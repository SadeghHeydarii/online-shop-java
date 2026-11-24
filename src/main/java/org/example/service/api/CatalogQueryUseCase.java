package org.example.service.api;

import java.util.List;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;

/**
 * Read-only storefront catalog browsing, searching, and faceted sorting API (ISP).
 */
public interface CatalogQueryUseCase {

    /**
     * Returns all active products belonging to active categories, ordered deterministically.
     *
     * @return unmodifiable list of visible active products
     */
    List<Product> getAllActiveProducts();

    /**
     * Retrieves a product by its unique identifier.
     *
     * @param productId non-blank product identifier
     * @return matching {@link Product}
     */
    Product getProductById(String productId);

    /**
     * Searches active products case-insensitively by name or description.
     *
     * @param query non-null search keyword within length bounds
     * @return unmodifiable list of matching active products
     */
    List<Product> searchProducts(String query);

    /**
     * Filters active products by category and price range and orders them via registered sort strategy.
     *
     * @param categoryId optional category filter (null, blank, or "ALL" disables category filter)
     * @param minPrice   optional lower inclusive bound
     * @param maxPrice   optional upper inclusive bound
     * @param sortBy     optional sort strategy key
     * @return filtered and sorted list of active products
     */
    List<Product> filterAndSort(String categoryId, Money minPrice, Money maxPrice, String sortBy);

    /**
     * Returns all active categories sorted alphabetically.
     *
     * @return unmodifiable list of active categories
     */
    List<Category> getActiveCategories();

    /**
     * Returns active products belonging to an active category.
     *
     * @param categoryId non-blank active category identifier
     * @return unmodifiable list of matching active products
     */
    List<Product> getProductsByCategory(String categoryId);
}
