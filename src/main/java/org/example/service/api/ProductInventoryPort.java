package org.example.service.api;

import org.example.model.vo.Quantity;

/**
 * Narrow inventory-mutation port used during payment finalization and paid-order cancellation (ISP).
 */
public interface ProductInventoryPort {

    /**
     * Adjusts available stock by {@code delta} (positive to restock, negative to deduct).
     *
     * @param productId non-blank product identifier
     * @param delta     signed stock adjustment
     */
    void adjustStock(String productId, int delta);

    /**
     * Records confirmed unit sales for a product after successful payment.
     *
     * @param productId non-blank product identifier
     * @param quantity  sold unit quantity
     */
    void recordSale(String productId, Quantity quantity);
}
