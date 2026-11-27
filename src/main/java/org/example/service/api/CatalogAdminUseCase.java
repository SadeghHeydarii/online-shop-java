package org.example.service.api;

import java.util.List;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;

/**
 * Administrative catalog mutation and inventory administration API (ISP).
 */
public interface CatalogAdminUseCase {

    /**
     * Returns every product in the catalog regardless of active status.
     *
     * @return unmodifiable list of all catalog products
     */
    List<Product> getAllProducts();

    /**
     * Creates and stores a new catalog product under an active category.
     *
     * @param name        product display name
     * @param desc        product description
     * @param price       product monetary price
     * @param stock       initial non-negative warehouse stock
     * @param categoryId  active category identifier
     */
    void addProduct(String name, String desc, Money price, int stock, String categoryId);

    /**
     * Updates editable details of an existing product.
     *
     * @param productId  non-blank product identifier
     * @param name       updated product name
     * @param desc       updated description
     * @param price      updated monetary price
     * @param categoryId updated active category identifier
     */
    void updateProduct(String productId, String name, String desc, Money price, String categoryId);

    /**
     * Deactivates a product so it disappears from active storefront views.
     *
     * @param productId non-blank product identifier
     */
    void deactivateProduct(String productId);

    /**
     * Adjusts available stock by signed delta (positive restocks, negative deducts).
     *
     * @param productId non-blank product identifier
     * @param delta     signed quantity delta
     */
    void adjustStock(String productId, int delta);

    /**
     * Replaces registered image URLs for an existing product.
     *
     * @param productId non-blank product identifier
     * @param imageUrls list of validated HTTP/HTTPS image URLs
     */
    void setProductImages(String productId, List<String> imageUrls);

    /**
     * Registers or updates a product category.
     *
     * @param category non-null category value object
     */
    void registerCategory(Category category);

    /**
     * Registers a pre-built product entity (for seeding or test fixtures).
     *
     * @param product non-null product entity
     */
    void registerProduct(Product product);
}
