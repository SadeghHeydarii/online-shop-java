package org.example.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.api.CatalogAdminUseCase;
import org.example.service.api.ProductInventoryPort;
import org.example.service.port.CategoryRepository;
import org.example.service.port.IdGenerator;
import org.example.service.port.ProductRepository;

/**
 * Cohesive administrative catalog service managing product creation, updates,
 * deactivation, category registration, and inventory/sales mutations (SRP, ISP, DIP).
 */
public class CatalogManagementService implements CatalogAdminUseCase, ProductInventoryPort {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    /**
     * Constructs a {@code CatalogManagementService} with all required repositories and generators.
     *
     * @param productRepository  repository for catalog products
     * @param categoryRepository repository for product categories
     * @param idGenerator        identifier generator for new products
     * @param clock              clock for deterministic creation timestamps
     */
    public CatalogManagementService(ProductRepository productRepository,
                                    CategoryRepository categoryRepository,
                                    IdGenerator idGenerator,
                                    Clock clock) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository cannot be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getName).thenComparing(Product::getId))
                .toList();
    }

    @Override
    public void addProduct(String name, String desc, Money price, int stock, String categoryId) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Product name cannot be null or blank");
        }
        if (desc == null || desc.isBlank()) {
            throw new InvalidInputException("Product description cannot be null or blank");
        }
        if (price == null) {
            throw new InvalidInputException("Product price cannot be null");
        }
        if (price.isNegative()) {
            throw new InvalidInputException("Product price cannot be negative");
        }
        if (stock < 0) {
            throw new InvalidInputException("Initial stock cannot be negative: " + stock);
        }

        Category category = CatalogLookupSupport.requireActiveCategory(categoryRepository, categoryId);
        String productId = idGenerator.nextId("prod");
        Product product = new Product(
                productId,
                name.trim(),
                desc.trim(),
                price,
                stock,
                category.id(),
                LocalDateTime.now(clock)
        );
        productRepository.save(product);
    }

    @Override
    public void updateProduct(String productId, String name, String desc, Money price, String categoryId) {
        Product product = CatalogLookupSupport.requireProduct(productRepository, productId);
        Category category = CatalogLookupSupport.requireActiveCategory(categoryRepository, categoryId);
        product.updateDetails(name, desc, price, category.id());
        productRepository.save(product);
    }

    @Override
    public void deactivateProduct(String productId) {
        Product product = CatalogLookupSupport.requireProduct(productRepository, productId);
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    public void adjustStock(String productId, int delta) {
        Product product = CatalogLookupSupport.requireProduct(productRepository, productId);
        if (delta > 0) {
            product.restock(new Quantity(delta));
        } else if (delta < 0) {
            product.deductStock(new Quantity(-delta));
        }
        productRepository.save(product);
    }

    @Override
    public void recordSale(String productId, Quantity quantity) {
        Product product = CatalogLookupSupport.requireProduct(productRepository, productId);
        product.recordSale(quantity);
        productRepository.save(product);
    }

    @Override
    public void setProductImages(String productId, List<String> imageUrls) {
        Product product = CatalogLookupSupport.requireProduct(productRepository, productId);
        product.setImageUrls(imageUrls);
        productRepository.save(product);
    }

    @Override
    public void registerCategory(Category category) {
        Objects.requireNonNull(category, "Category cannot be null");
        categoryRepository.save(category);
    }

    @Override
    public void registerProduct(Product product) {
        Objects.requireNonNull(product, "Product cannot be null");
        productRepository.save(product);
    }
}
