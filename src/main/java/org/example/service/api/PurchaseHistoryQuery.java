package org.example.service.api;

import java.util.List;
import org.example.model.entity.Order;

/**
 * Segregated read-only query port exposing verified customer purchase history (ISP).
 */
public interface PurchaseHistoryQuery {

    /**
     * Returns all orders placed by the user, ordered newest-first.
     *
     * @param userId non-blank customer identifier
     * @return unmodifiable list of the user's orders
     */
    List<Order> getOrderHistory(String userId);

    /**
     * Checks whether the customer has purchased the given product in a paid or fulfilled order.
     *
     * @param userId    non-blank customer identifier
     * @param productId non-blank product identifier
     * @return {@code true} if a qualifying purchase exists
     */
    boolean hasPurchasedProduct(String userId, String productId);
}
