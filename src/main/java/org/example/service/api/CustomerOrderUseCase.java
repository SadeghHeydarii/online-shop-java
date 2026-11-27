package org.example.service.api;

import java.util.List;
import org.example.model.entity.Order;

/**
 * Customer-facing inbound use-case API for checkout, personal order history, and customer cancellation (ISP).
 */
public interface CustomerOrderUseCase {

    /**
     * Creates a new pending order by snapshotting the customer's cart and computing totals.
     *
     * @param userId     non-blank customer identifier
     * @param addressId  optional delivery address ID (null/blank selects default address)
     * @param couponCode optional promotional coupon code
     * @return newly persisted {@link Order}
     */
    Order checkout(String userId, String addressId, String couponCode);

    /**
     * Cancels a customer-owned order when permitted by its current status.
     *
     * @param userId  non-blank customer identifier
     * @param orderId non-blank order identifier
     * @return {@code true} upon successful cancellation
     */
    boolean cancelOrder(String userId, String orderId);

    /**
     * Retrieves all orders belonging to the specified customer, newest-first.
     *
     * @param userId non-blank customer identifier
     * @return unmodifiable list of customer orders
     */
    List<Order> getOrderHistory(String userId);

    /**
     * Retrieves details of a single order after verifying customer ownership.
     *
     * @param userId  non-blank customer identifier
     * @param orderId non-blank order identifier
     * @return matching {@link Order}
     */
    Order getOrderDetails(String userId, String orderId);
}
