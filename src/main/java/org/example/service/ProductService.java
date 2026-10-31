package org.example.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.CategoryNotFoundException;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;

/**
 * Application service managing the product catalog, category classifications,
 * catalog search, faceted filtering and sorting, and administrative inventory adjustments.
 *
 * <p>Specification compliance:
 * <ul>
 *   <li>Conforms strictly to Section 5.3 and Figure 2 of domain-model-specification.</li>
 *   <li>Supports catalog browsing, active category filtering, image URL management, and stock validation.</li>
 *   <li>In-memory data structures managed via {@link ConcurrentHashMap}.</li>
 * </ul>
 */
public class ProductService {

    public static final int MAX_SEARCH_LENGTH = 100;

    private final Map<String, Product> productStore;
    private final Map<String, Category> categoryStore;

    /**
     * Default constructor initializing in-memory thread-safe concurrent stores.
     */
    public ProductService() {
        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    /**
     * Parameterized constructor enabling dependency injection for testing and data seeding.
     *
     * @param productStore  in-memory store mapping product IDs to Product entities
     * @param categoryStore in-memory store mapping category IDs to Category objects
     */
    public ProductService(Map<String, Product> productStore, Map<String, Category> categoryStore) {
        this.productStore = Objects.requireNonNull(productStore, "productStore cannot be null");
        this.categoryStore = Objects.requireNonNull(categoryStore, "categoryStore cannot be null");
    }

    /**
     * Retrieves all published, active products in active categories, sorted deterministically by name and ID.
     *
     * @return unmodifiable list of visible active products
     */
    public List<Product> getAllActiveProducts() {
        return productStore.values().stream()
                .filter(this::isVisible)
                .sorted(Comparator.comparing(Product::getName).thenComparing(Product::getId))
                .toList();
    }

    /**
     * Retrieves all products in the catalog regardless of active status (for administrative management).
     *
     * @return list of all catalog products
     */
    public List<Product> getAllProducts() {
        return productStore.values().stream()
                .sorted(Comparator.comparing(Product::getName).thenComparing(Product::getId))
                .toList();
    }

    /**
     * Retrieves a single product by its unique identifier.
     *
     * @param productId the identifier of the product to look up
     * @return the requested {@link Product}
     * @throws InvalidInputException   if {@code productId} is null or blank
     * @throws ProductNotFoundException if no product exists with the given ID
     */
    public Product getProductById(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new InvalidInputException("Product ID cannot be null or blank");
        }
        Product product = productStore.get(productId.trim());
        if (product == null) {
            throw new ProductNotFoundException("Product with ID '" + productId + "' was not found");
        }
        return product;
    }

    /**
     * Performs a case-insensitive search across names and descriptions of visible active products.
     *
     * @param query the search keyword or phrase (up to {@value #MAX_SEARCH_LENGTH} characters)
     * @return list of matching active products
     * @throws InvalidInputException if {@code query} is null or exceeds maximum allowed length
     */
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

    /**
     * Filters active products by category and optional price range, then sorts the results.
     *
     * @param categoryId optional category filter (null, blank, or "ALL" ignores category filtering)
     * @param minPrice   optional minimum inclusive price threshold (null ignores lower bound)
     * @param maxPrice   optional maximum inclusive price threshold (null ignores upper bound)
     * @param sortBy     sort criterion: "price_asc", "price_desc", "rating", "newest", "popular", or null/empty
     * @return filtered and sorted list of active products
     * @throws InvalidInputException if minPrice exceeds maxPrice or if sortBy option is unrecognized
     */
    public List<Product> filterAndSort(String categoryId, Money minPrice, Money maxPrice, String sortBy) {
        if (minPrice != null && maxPrice != null && maxPrice.isLessThan(minPrice)) {
            throw new InvalidInputException("Minimum price (" + minPrice + ") cannot exceed maximum price (" + maxPrice + ")");
        }

        Comparator<Product> comparator = Comparator.comparing(Product::getName);
        if (sortBy != null && !sortBy.isBlank()) {
            comparator = switch (sortBy.trim().toLowerCase(Locale.ROOT)) {
                case "price_asc" -> Comparator.comparing(product -> product.getPrice().amount());
                case "price_desc" -> Comparator.comparing((Product product) -> product.getPrice().amount()).reversed();
                case "rating" -> Comparator.comparingDouble(Product::getAverageRating).reversed();
                case "newest" -> Comparator.comparing(Product::getCreatedAt).reversed();
                case "popular" -> Comparator.comparingLong(Product::getSoldCount).reversed();
                default -> throw new InvalidInputException("Unrecognized sort option: '" + sortBy
                        + "'. Valid options: price_asc, price_desc, rating, newest, popular");
            };
        }

        var stream = getAllActiveProducts().stream();

        if (categoryId != null && !categoryId.isBlank() && !categoryId.trim().equalsIgnoreCase("ALL")) {
            Category category = requireActiveCategory(categoryId);
            stream = stream.filter(product -> product.getCategoryId().equals(category.id()));
        }

        if (minPrice != null) {
            stream = stream.filter(product -> !product.getPrice().isLessThan(minPrice));
        }

        if (maxPrice != null) {
            stream = stream.filter(product -> !maxPrice.isLessThan(product.getPrice()));
        }

        return stream.sorted(comparator.thenComparing(Product::getId)).toList();
    }

    /**
     * Registers a new product into the catalog under an active existing category.
     *
     * @param name        the product display name
     * @param desc        the detailed product description
     * @param price       the monetary price of the product
     * @param stock       initial inventory count (non-negative)
     * @param categoryId  the category to assign the product to (must exist and be active)
     * @throws InvalidInputException     if input arguments violate domain invariants
     * @throws CategoryNotFoundException if categoryId does not exist or is inactive
     */
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
        if (categoryId == null || categoryId.isBlank()) {
            throw new InvalidInputException("Category ID cannot be null or blank");
        }

        String targetCatId = categoryId.trim();
        if (!categoryStore.containsKey(targetCatId) || !categoryStore.get(targetCatId).active()) {
            throw new CategoryNotFoundException("Category with ID '" + targetCatId + "' does not exist or is inactive");
        }

        String productId = "prod-" + UUID.randomUUID().toString();
        Product product = new Product(productId, name.trim(), desc.trim(), price, stock, targetCatId);
        productStore.put(product.getId(), product);
    }

    /**
     * Updates editable details of an existing product.
     *
     * @param productId  unique identifier of the product to update
     * @param name       updated product name
     * @param desc       updated product description
     * @param price      updated monetary price
     * @param categoryId updated category identifier (must exist and be active)
     * @throws InvalidInputException     if any parameter is invalid
     * @throws ProductNotFoundException  if product does not exist
     * @throws CategoryNotFoundException if categoryId does not exist or is inactive
     */
    public void updateProduct(String productId, String name, String desc, Money price, String categoryId) {
        Product product = getProductById(productId);

        if (categoryId == null || categoryId.isBlank()) {
            throw new InvalidInputException("Category ID cannot be null or blank");
        }
        String targetCatId = categoryId.trim();
        if (!categoryStore.containsKey(targetCatId) || !categoryStore.get(targetCatId).active()) {
            throw new CategoryNotFoundException("Category with ID '" + targetCatId + "' does not exist or is inactive");
        }

        product.updateDetails(name, desc, price, targetCatId);
    }

    /**
     * Deactivates a product, removing it from active storefront catalog views.
     *
     * @param productId unique identifier of the product to deactivate
     * @throws InvalidInputException   if productId is null or blank
     * @throws ProductNotFoundException if product does not exist
     */
    public void deactivateProduct(String productId) {
        Product product = getProductById(productId);
        product.setActive(false);
    }

    /**
     * Adjusts the available inventory level of a product.
     * Positive delta increases stock (restock), negative delta decreases stock (deduct).
     *
     * @param productId unique identifier of the product
     * @param delta     number of units to add (positive) or deduct (negative)
     * @throws InvalidInputException   if productId is null or blank
     * @throws ProductNotFoundException if product does not exist
     * @throws IllegalStateException    if stock is insufficient for a negative delta
     */
    public void adjustStock(String productId, int delta) {
        Product product = getProductById(productId);
        if (delta > 0) {
            product.restock(new Quantity(delta));
        } else if (delta < 0) {
            product.deductStock(new Quantity(-delta));
        }
    }

    /**
     * Returns all active categories sorted alphabetically by name.
     *
     * @return list of active categories
     */
    public List<Category> getActiveCategories() {
        return categoryStore.values().stream()
                .filter(Category::active)
                .sorted(Comparator.comparing(Category::name).thenComparing(Category::id))
                .toList();
    }

    /**
     * Retrieves all active products belonging to the specified active category.
     *
     * @param categoryId the category identifier
     * @return list of active products in the category
     */
    public List<Product> getProductsByCategory(String categoryId) {
        Category category = requireActiveCategory(categoryId);
        return getAllActiveProducts().stream()
                .filter(product -> product.getCategoryId().equals(category.id()))
                .toList();
    }

    /**
     * Updates the registered image URLs for a product.
     *
     * @param productId the identifier of the product
     * @param imageUrls the list of HTTP/HTTPS image URLs
     */
    public void setProductImages(String productId, List<String> imageUrls) {
        getProductById(productId).setImageUrls(imageUrls);
    }

    /**
     * Validates that the requested product exists, is visible in an active category, and has sufficient stock.
     *
     * @param productId the identifier of the product
     * @param quantity  the required quantity
     * @return the available {@link Product}
     * @throws InvalidInputException      if the product is inactive or unavailable
     * @throws InsufficientStockException if available stock is less than required quantity
     */
    public Product requireAvailableProduct(String productId, Quantity quantity) {
        Product product = getProductById(productId);
        if (!isVisible(product)) {
            throw new InvalidInputException("Product is inactive or its category is unavailable: " + productId);
        }
        if (!product.isAvailable(quantity)) {
            throw new InsufficientStockException("Insufficient stock for product '" + product.getName() + "'");
        }
        return product;
    }

    private boolean isVisible(Product product) {
        Category category = categoryStore.get(product.getCategoryId());
        return product.isActive() && category != null && category.active();
    }

    private Category requireActiveCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new InvalidInputException("Category ID cannot be blank");
        }
        Category category = categoryStore.get(categoryId.trim());
        if (category == null || !category.active()) {
            throw new CategoryNotFoundException("Active category not found: " + categoryId);
        }
        return category;
    }

    // =========================================================================
    // Package-private seeding helpers (strictly internal to package, not public domain API)
    // =========================================================================

    void registerCategory(Category category) {
        Objects.requireNonNull(category, "Category cannot be null");
        categoryStore.put(category.id(), category);
    }

    void registerProduct(Product product) {
        Objects.requireNonNull(product, "Product cannot be null");
        productStore.put(product.getId(), product);
    }
}
