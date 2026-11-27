package org.example.service.port;

import java.util.List;
import java.util.Optional;
import org.example.model.vo.CartItem;

/**
 * Outbound port for tracking checkout cart-item snapshots associated with pending orders,
 * allowing selective post-payment cart cleanup without discarding concurrent cart edits.
 */
public interface CheckoutSnapshotRepository {

    /**
     * Stores an immutable snapshot of checked-out cart items for an order.
     *
     * @param orderId non-blank order identifier
     * @param items   non-null list of checked-out cart items
     */
    void saveSnapshot(String orderId, List<CartItem> items);

    /**
     * Removes and returns the checkout snapshot for an order, if present.
     *
     * @param orderId non-blank order identifier
     * @return snapshot item list if present, otherwise empty
     */
    Optional<List<CartItem>> removeSnapshot(String orderId);
}
