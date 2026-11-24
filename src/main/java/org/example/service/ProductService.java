package org.example.service;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.port.CategoryRepository;
import org.example.service.port.ProductRepository;
import org.example.service.port.ProductSortRegistry;
import org.example.service.port.ProductSortStrategy;

/**
 * Deprecated transitional facade delegating all catalog query and administration operations to
 * {@link CatalogQueryService} and {@link CatalogManagementService}.
 *
 * @deprecated Scheduled for removal once menus and seeder migrate to segregated catalog APIs in Phase 5.
 */
@Deprecated
public class ProductService {

    public static final int MAX_SEARCH_LENGTH = CatalogQueryService.MAX_SEARCH_LENGTH;

    private final CatalogQueryService queryService;
    private final CatalogManagementService managementService;

    /**
     * Default constructor initializing in-memory thread-safe concurrent stores.
     */
    public ProductService() {
        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    /**
     * Parameterized constructor bridging legacy concurrent maps to segregated catalog services.
     *
     * @param productStore  in-memory store mapping product IDs to Product entities
     * @param categoryStore in-memory store mapping category IDs to Category objects
     */
    public ProductService(Map<String, Product> productStore, Map<String, Category> categoryStore) {
        Objects.requireNonNull(productStore, "productStore cannot be null");
        Objects.requireNonNull(categoryStore, "categoryStore cannot be null");

        ProductRepository productRepository = new MapBackedProductRepository(productStore);
        CategoryRepository categoryRepository = new MapBackedCategoryRepository(categoryStore);

        this.queryService = new CatalogQueryService(productRepository, categoryRepository, createLegacySortRegistry());
        this.managementService = new CatalogManagementService(
                productRepository,
                categoryRepository,
                prefix -> prefix + "-" + UUID.randomUUID(),
                Clock.systemDefaultZone()
        );
    }

    public List<Product> getAllActiveProducts() {
        return queryService.getAllActiveProducts();
    }

    public List<Product> getAllProducts() {
        return managementService.getAllProducts();
    }

    public Product getProductById(String productId) {
        return queryService.getProductById(productId);
    }

    public List<Product> searchProducts(String query) {
        return queryService.searchProducts(query);
    }

    public List<Product> filterAndSort(String categoryId, Money minPrice, Money maxPrice, String sortBy) {
        return queryService.filterAndSort(categoryId, minPrice, maxPrice, sortBy);
    }

    public void addProduct(String name, String desc, Money price, int stock, String categoryId) {
        managementService.addProduct(name, desc, price, stock, categoryId);
    }

    public void updateProduct(String productId, String name, String desc, Money price, String categoryId) {
        managementService.updateProduct(productId, name, desc, price, categoryId);
    }

    public void deactivateProduct(String productId) {
        managementService.deactivateProduct(productId);
    }

    public void adjustStock(String productId, int delta) {
        managementService.adjustStock(productId, delta);
    }

    public List<Category> getActiveCategories() {
        return queryService.getActiveCategories();
    }

    public List<Product> getProductsByCategory(String categoryId) {
        return queryService.getProductsByCategory(categoryId);
    }

    public void setProductImages(String productId, List<String> imageUrls) {
        managementService.setProductImages(productId, imageUrls);
    }

    public Product requireAvailableProduct(String productId, Quantity quantity) {
        return queryService.requireAvailableProduct(productId, quantity);
    }

    void registerCategory(Category category) {
        managementService.registerCategory(category);
    }

    void registerProduct(Product product) {
        managementService.registerProduct(product);
    }

    // =========================================================================
    // Legacy transition adapters (removed along with ProductService in Phase 5)
    // =========================================================================

    private static ProductSortRegistry createLegacySortRegistry() {
        ProductSortStrategy defaultSort = new SimpleSort("default", Comparator.comparing(Product::getName));
        Map<String, ProductSortStrategy> sorts = Map.of(
                "price_asc", new SimpleSort("price_asc", Comparator.comparing(p -> p.getPrice().amount())),
                "price_desc", new SimpleSort("price_desc", Comparator.comparing((Product p) -> p.getPrice().amount()).reversed()),
                "rating", new SimpleSort("rating", Comparator.comparingDouble(Product::getAverageRating).reversed()),
                "newest", new SimpleSort("newest", Comparator.comparing(Product::getCreatedAt).reversed()),
                "popular", new SimpleSort("popular", Comparator.comparingLong(Product::getSoldCount).reversed())
        );

        return new ProductSortRegistry() {
            @Override
            public ProductSortStrategy resolve(String sortKey) {
                if (sortKey == null || sortKey.isBlank()) {
                    return defaultSort;
                }
                ProductSortStrategy strategy = sorts.get(sortKey.trim().toLowerCase(Locale.ROOT));
                if (strategy == null) {
                    throw new InvalidInputException("Unrecognized sort option: '" + sortKey
                            + "'. Valid options: price_asc, price_desc, rating, newest, popular");
                }
                return strategy;
            }

            @Override
            public List<String> availableKeys() {
                return List.of("price_asc", "price_desc", "rating", "newest", "popular");
            }
        };
    }

    private record SimpleSort(String key, Comparator<Product> comparator) implements ProductSortStrategy {
    }

    private record MapBackedProductRepository(Map<String, Product> store) implements ProductRepository {
        @Override
        public Optional<Product> findById(String productId) {
            return Optional.ofNullable(store.get(productId.trim()));
        }

        @Override
        public List<Product> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public void save(Product product) {
            store.put(product.getId(), product);
        }
    }

    private record MapBackedCategoryRepository(Map<String, Category> store) implements CategoryRepository {
        @Override
        public Optional<Category> findById(String categoryId) {
            return Optional.ofNullable(store.get(categoryId.trim()));
        }

        @Override
        public List<Category> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public void save(Category category) {
            store.put(category.id(), category);
        }
    }
}
