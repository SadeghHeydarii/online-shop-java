package org.example.service.api;

import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.model.entity.Product;

/**
 * Narrow read-only product lookup port used by reviews and catalog consumers (ISP).
 */
public interface ProductLookupPort {

    /**
     * Retrieves a single catalog product by its unique identifier.
     *
     * @param productId non-blank product identifier
     * @return matching {@link Product}
     * @throws InvalidInputException    if {@code productId} is null or blank
     * @throws ProductNotFoundException if no product exists with {@code productId}
     */
    Product getProductById(String productId);
}
