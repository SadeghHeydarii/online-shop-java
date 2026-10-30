package org.example.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.InvalidInputException;
import org.example.exception.OrderNotFoundException;
import org.example.model.entity.Order;
import org.example.model.enums.OrderStatus;

/**
 * Application service managing order records, order querying, and order status transitions.
 *
 * <p>Phase 6 foundation implementation:
 * <ul>
 *   <li>Manages in-memory order storage ({@code orderStore}).</li>
 *   <li>Provides {@link #getOrderHistory(String)} for user purchase verification required by {@link ReviewService}.</li>
 *   <li>Full multi-step checkout, coupon evaluation, and payment gateway coordination will be integrated in Phase 7.</li>
 * </ul>
 */
public class OrderService {

    private final Map<String, Order> orderStore;

    /**
     * Default constructor initializing in-memory concurrent order store.
     */
    public OrderService() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Parameterized constructor enabling dependency injection for testing and cross-service coordination.
     *
     * @param orderStore in-memory map storing orders by their unique order IDs
     */
    public OrderService(Map<String, Order> orderStore) {
        this.orderStore = Objects.requireNonNull(orderStore, "orderStore cannot be null");
    }

    /**
     * Retrieves all orders placed by the specified user.
     *
     * @param userId unique user identifier (non-null, non-blank)
     * @return unmodifiable list of orders associated with the user
     * @throws InvalidInputException if {@code userId} is null or blank
     */
    public List<Order> getOrderHistory(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be null or blank");
        }
        String targetUserId = userId.trim();
        return orderStore.values().stream()
                .filter(o -> o.getUserId().equals(targetUserId))
                .toList();
    }

    /**
     * Retrieves the details of a single order by its unique order identifier.
     *
     * @param orderId unique order identifier (non-null, non-blank)
     * @return the requested {@link Order}
     * @throws InvalidInputException   if {@code orderId} is null or blank
     * @throws OrderNotFoundException  if no order exists with the given ID
     */
    public Order getOrderDetails(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new InvalidInputException("Order ID cannot be null or blank");
        }
        Order order = orderStore.get(orderId.trim());
        if (order == null) {
            throw new OrderNotFoundException("Order with ID '" + orderId + "' was not found");
        }
        return order;
    }

    /**
     * Updates the status of an existing order following valid lifecycle transitions.
     *
     * @param orderId   unique order identifier
     * @param newStatus the new status to advance or transition to
     * @throws InvalidInputException   if {@code orderId} is null/blank or {@code newStatus} is null
     * @throws OrderNotFoundException  if no order exists with the given ID
     */
    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidInputException("New order status cannot be null");
        }
        Order order = getOrderDetails(orderId);
        order.updateStatus(newStatus);
    }

    // =========================================================================
    // Package-private seeding helpers (strictly internal to package, not public domain API)
    // =========================================================================

    void recordOrder(Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        orderStore.put(order.getOrderId(), order);
    }
}
