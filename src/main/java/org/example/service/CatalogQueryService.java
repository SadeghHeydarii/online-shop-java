package org.example.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.api.CatalogQueryUseCase;
import org.example.service.api.ProductAvailabilityPort;
import org.example.service.api.ProductLookupPort;
import org.example.service.port.CategoryRepository;
import org.example.service.port.ProductRepository;
import org.example.service.port.ProductSortRegistry;

/**
 * Cohesive read-only catalog query service handling storefront browsing, search,
 * faceted filtering, and Open-Closed strategy sorting (SRP, OCP, ISP, DIP).
 */
public class CatalogQueryService implements CatalogQueryUseCase, ProductAvailabilityPort, ProductLookupPort {

    public static final int MAX_SEARCH_LENGTH = 100;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSortRegistry sortRegistry;

    /**
     * Constructs a {@code CatalogQueryService} backed by segregated catalog ports and sort registry.
     *
     * @param productRepository  repository for catalog products
     * @param categoryRepository repository for product categories
     * @param sortRegistry       Open-Closed registry resolving sort strategies by key
     */
    public CatalogQueryService(ProductRepository productRepository,
                               CategoryRepository categoryRepository,
                               ProductSortRegistry sortRegistry) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository cannot be null");
        this.sortRegistry = Objects.requireNonNull(sortRegistry, "sortRegistry cannot be null");
    }

    @Override
    public List<Product> getAllActiveProducts() {
        return productRepository.findAll().stream()
                .filter(product -> CatalogLookupSupport.isVisible(product, categoryRepository))
                .sorted(Comparator.comparing(Product::getName).thenComparing(Product::getId))
                .toList();
    }

    @Override
    public Product getProductById(String productId) {
        return CatalogLookupSupport.requireProduct(productRepository, productId);
    }

    @Override
    public List<Product> searchProducts(String query) {
        if (query == null || query.length() > MAX_SEARCH_LENGTH) {
            throw new InvalidInputException("Search text must be at most " + MAX_SEARCH_LENGTH + " characters");
        }
        String term = query.trim().toLowerCase(Locale.ROOT);
        return getAllActiveProducts().stream()
                .filter(product -> product.getName().toLowerCase(Locale.ROOT).contains(term)
                        || product.getDescription().toLowerCase(Locale.ROOT).contains(term))
                .toList();
    }

    @Override
    public List<Product> filterAndSort(String categoryId, Money minPrice, Money maxPrice, String sortBy) {
        if (minPrice != null && maxPrice != null && maxPrice.isLessThan(minPrice)) {
            throw new InvalidInputException("Minimum price (" + minPrice + ") cannot exceed maximum price (" + maxPrice + ")");
        }

        Comparator<Product> primaryComparator = sortRegistry.resolve(sortBy).comparator();
        var stream = getAllActiveProducts().stream();

        if (categoryId != null && !categoryId.isBlank() && !categoryId.trim().equalsIgnoreCase("ALL")) {
            Category category = CatalogLookupSupport.requireActiveCategory(categoryRepository, categoryId);
            stream = stream.filter(product -> product.getCategoryId().equals(category.id()));
        }

        if (minPrice != null) {
            stream = stream.filter(product -> !product.getPrice().isLessThan(minPrice));
        }

        if (maxPrice != null) {
            stream = stream.filter(product -> !maxPrice.isLessThan(product.getPrice()));
        }

        return stream.sorted(primaryComparator.thenComparing(Product::getId)).toList();
    }

    @Override
    public List<Category> getActiveCategories() {
        return categoryRepository.findAll().stream()
                .filter(Category::active)
                .sorted(Comparator.comparing(Category::name).thenComparing(Category::id))
                .toList();
    }

    @Override
    public List<Product> getProductsByCategory(String categoryId) {
        Category category = CatalogLookupSupport.requireActiveCategory(categoryRepository, categoryId);
        return getAllActiveProducts().stream()
                .filter(product -> product.getCategoryId().equals(category.id()))
                .toList();
    }

    @Override
    public List<String> getAvailableSortOptions() {
        return sortRegistry.availableKeys();
    }

    @Override
    public Product requireAvailableProduct(String productId, Quantity quantity) {
        Product product = getProductById(productId);
        if (!CatalogLookupSupport.isVisible(product, categoryRepository)) {
            throw new InvalidInputException("Product is inactive or its category is unavailable: " + productId);
        }
        if (!product.isAvailable(quantity)) {
            throw new InsufficientStockException("Insufficient stock for product '" + product.getName() + "'");
        }
        return product;
    }
}
