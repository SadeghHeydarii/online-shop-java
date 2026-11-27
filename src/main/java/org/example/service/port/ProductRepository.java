package org.example.service.port;

import java.util.List;
import java.util.Optional;
import org.example.model.entity.Product;

/**
 * Outbound persistence port for managing {@link Product} catalog entities.
 */
public interface ProductRepository {

    /**
     * Retrieves a product by its unique identifier.
     *
     * @param productId non-blank product identifier
     * @return matching product if present, otherwise empty
     * @throws IllegalArgumentException if {@code productId} is null or blank
     */
    Optional<Product> findById(String productId);

    /**
     * Returns a snapshot list of all stored products (active and inactive).
     *
     * @return unmodifiable list of all products
     */
    List<Product> findAll();

    /**
     * Persists a new or updated {@link Product} entity.
     *
     * @param product non-null product entity
     */
    void save(Product product);
}
