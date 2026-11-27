package org.example.service.api;

import java.util.List;
import org.example.model.entity.Order;
import org.example.model.enums.OrderStatus;

/**
 * Administrative inbound use-case API for order fulfillment lifecycle management and queries (ISP).
 */
public interface OrderFulfillmentUseCase {

    /**
     * Cancels an order from an administrative/system context, restocking paid items if applicable.
     *
     * @param orderId non-blank order identifier
     * @return {@code true} upon successful cancellation
     */
    boolean cancelOrder(String orderId);

    /**
     * Advances or transitions an order's lifecycle status according to the domain statechart.
     *
     * @param orderId   non-blank order identifier
     * @param newStatus target order status
     */
    void updateOrderStatus(String orderId, OrderStatus newStatus);

    /**
     * Retrieves full details for any order by identifier.
     *
     * @param orderId non-blank order identifier
     * @return matching {@link Order}
     */
    Order getOrderDetails(String orderId);

    /**
     * Returns all orders in the system, sorted newest-first.
     *
     * @return unmodifiable list of all orders
     */
    List<Order> getAllOrders();

    /**
     * Returns orders matching {@code status}, or all orders when {@code status} is {@code null}.
     *
     * @param status optional status filter
     * @return unmodifiable list of matching orders sorted newest-first
     */
    List<Order> getOrdersByStatus(OrderStatus status);
}
