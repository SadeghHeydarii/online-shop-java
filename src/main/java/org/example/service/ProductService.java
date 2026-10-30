package org.example.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.CategoryNotFoundException;
import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.model.vo.Category;
import org.example.model.entity.Product;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;

/**
 * Application service managing the product catalog, category classifications,
 * catalog search, faceted filtering and sorting, and administrative inventory adjustments.
 *
 * <p>Specification compliance:
 * <ul>
 *   <li>Conforms strictly to Section 5.3 and Figure 2 of domain-model-specification.</li>
 *   <li>Case A: Implements the exact 8 approved public methods without inventing unapproved APIs.</li>
 *   <li>In-memory data structures managed via {@link ConcurrentHashMap}.</li>
 * </ul>
 */
public class ProductService {

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
     * Retrieves all published, active products in the catalog.
     *
     * @return unmodifiable list of active products
     */
    public List<Product> getAllActiveProducts() {
        return productStore.values().stream()
                .filter(Product::isActive)
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
     * Performs a case-insensitive search across names and descriptions of active products.
     *
     * @param query the search keyword or phrase (non-null)
     * @return list of matching active products
     * @throws InvalidInputException if {@code query} is null
     */
    public List<Product> searchProducts(String query) {
        if (query == null) {
            throw new InvalidInputException("Search query cannot be null");
        }
        String term = query.trim().toLowerCase();
        if (term.isEmpty()) {
            return getAllActiveProducts();
        }
        return productStore.values().stream()
                .filter(Product::isActive)
                .filter(p -> p.getName().toLowerCase().contains(term)
                        || p.getDescription().toLowerCase().contains(term))
                .toList();
    }

    /**
     * Filters active products by category and optional price range, then sorts the results.
     *
     * @param categoryId optional category filter (null, blank, or "ALL" ignores category filtering)
     * @param minPrice   optional minimum inclusive price threshold (null ignores lower bound)
     * @param maxPrice   optional maximum inclusive price threshold (null ignores upper bound)
     * @param sortBy     sort criterion: "price_asc", "price_desc", "rating", or null/empty
     * @return filtered and sorted list of active products
     * @throws InvalidInputException if minPrice exceeds maxPrice or if sortBy option is unrecognized
     */
    public List<Product> filterAndSort(String categoryId, Money minPrice, Money maxPrice, String sortBy) {
        if (minPrice != null && maxPrice != null && maxPrice.isLessThan(minPrice)) {
            throw new InvalidInputException("Minimum price (" + minPrice + ") cannot exceed maximum price (" + maxPrice + ")");
        }

        Comparator<Product> comparator = null;
        if (sortBy != null && !sortBy.isBlank()) {
            String normSort = sortBy.trim().toLowerCase();
            comparator = switch (normSort) {
                case "price_asc" -> Comparator.comparing(p -> p.getPrice().amount());
                case "price_desc" -> Comparator.comparing((Product p) -> p.getPrice().amount()).reversed();
                case "rating" -> Comparator.comparingDouble(Product::getAverageRating).reversed();
                default -> throw new InvalidInputException("Unrecognized sort option: '" + sortBy + "'. Valid options: price_asc, price_desc, rating");
            };
        }

        var stream = productStore.values().stream()
                .filter(Product::isActive);

        if (categoryId != null && !categoryId.isBlank() && !categoryId.equalsIgnoreCase("ALL")) {
            String targetCatId = categoryId.trim();
            stream = stream.filter(p -> p.getCategoryId().equals(targetCatId));
        }

        if (minPrice != null) {
            stream = stream.filter(p -> !p.getPrice().isLessThan(minPrice));
        }

        if (maxPrice != null) {
            stream = stream.filter(p -> !maxPrice.isLessThan(p.getPrice()));
        }

        if (comparator != null) {
            stream = stream.sorted(comparator);
        }

        return stream.toList();
    }

    /**
     * Registers a new product into the catalog under an existing category.
     *
     * @param name        the product display name
     * @param desc        the detailed product description
     * @param price       the monetary price of the product
     * @param stock       initial inventory count (non-negative)
     * @param categoryId  the category to assign the product to (must exist in categoryStore)
     * @throws InvalidInputException     if input arguments violate domain invariants
     * @throws CategoryNotFoundException if categoryId does not exist in the categoryStore
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
        if (!categoryStore.containsKey(targetCatId)) {
            throw new CategoryNotFoundException("Category with ID '" + targetCatId + "' does not exist");
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
     * @param categoryId updated category identifier (must exist in categoryStore)
     * @throws InvalidInputException     if any parameter is invalid
     * @throws ProductNotFoundException  if product does not exist
     * @throws CategoryNotFoundException if categoryId does not exist
     */
    public void updateProduct(String productId, String name, String desc, Money price, String categoryId) {
        Product product = getProductById(productId);

        if (categoryId == null || categoryId.isBlank()) {
            throw new InvalidInputException("Category ID cannot be null or blank");
        }
        String targetCatId = categoryId.trim();
        if (!categoryStore.containsKey(targetCatId)) {
            throw new CategoryNotFoundException("Category with ID '" + targetCatId + "' does not exist");
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
