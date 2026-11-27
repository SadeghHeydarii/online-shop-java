package org.example.service.api;

import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.model.entity.Product;
import org.example.model.vo.Quantity;

/**
 * Narrow read-only port verifying catalog product availability and stock sufficiency (ISP).
 */
public interface ProductAvailabilityPort {

    /**
     * Verifies that a product exists, is active inside an active category, and has at least {@code quantity} in stock.
     *
     * @param productId non-blank product identifier
     * @param quantity  required quantity
     * @return available {@link Product}
     * @throws InvalidInputException      if product ID is blank or product is inactive
     * @throws ProductNotFoundException   if product does not exist
     * @throws InsufficientStockException if available stock is insufficient
     */
    Product requireAvailableProduct(String productId, Quantity quantity);
}
