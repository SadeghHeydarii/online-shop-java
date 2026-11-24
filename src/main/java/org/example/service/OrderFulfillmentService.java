package org.example.service;

import java.util.List;
import java.util.Objects;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.model.entity.Order;
import org.example.model.enums.OrderStatus;
import org.example.service.api.OrderFulfillmentUseCase;
import org.example.service.api.ProductInventoryPort;
import org.example.service.port.CheckoutSnapshotRepository;
import org.example.service.port.OrderRepository;

/**
 * Cohesive administrative fulfillment service managing order lifecycle status transitions,
 * system cancellations, and catalog-wide order queries (SRP, ISP, DIP).
 */
public class OrderFulfillmentService implements OrderFulfillmentUseCase {

    private final OrderRepository orderRepository;
    private final CheckoutSnapshotRepository snapshotRepository;
    private final ProductInventoryPort productInventoryPort;

    /**
     * Constructs an {@code OrderFulfillmentService} with required repositories and inventory port.
     *
     * @param orderRepository      persistence port for order aggregates
     * @param snapshotRepository   persistence port for checkout cart-item snapshots
     * @param productInventoryPort inventory port for restocking paid cancelled orders
     */
    public OrderFulfillmentService(OrderRepository orderRepository,
                                   CheckoutSnapshotRepository snapshotRepository,
                                   ProductInventoryPort productInventoryPort) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository cannot be null");
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository cannot be null");
        this.productInventoryPort = Objects.requireNonNull(productInventoryPort, "productInventoryPort cannot be null");
    }

    @Override
    public synchronized boolean cancelOrder(String orderId) {
        Order order = OrderLifecycleSupport.requireOrder(orderRepository, orderId);
        return OrderLifecycleSupport.cancelOrder(orderRepository, snapshotRepository, productInventoryPort, order);
    }

    @Override
    public synchronized void updateOrderStatus(String orderId, OrderStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidInputException("New order status cannot be null");
        }
        if (newStatus == OrderStatus.CANCELLED) {
            cancelOrder(orderId);
            return;
        }
        Order order = OrderLifecycleSupport.requireOrder(orderRepository, orderId);
        try {
            order.updateStatus(newStatus);
            orderRepository.save(order);
        } catch (IllegalStateException e) {
            throw new InvalidOrderStateException(e.getMessage());
        }
    }

    @Override
    public Order getOrderDetails(String orderId) {
        return OrderLifecycleSupport.requireOrder(orderRepository, orderId);
    }

    @Override
    public List<Order> getAllOrders() {
        return getOrdersByStatus(null);
    }

    @Override
    public List<Order> getOrdersByStatus(OrderStatus status) {
        List<Order> filtered = orderRepository.findAll().stream()
                .filter(order -> status == null || order.getStatus() == status)
                .toList();
        return OrderLifecycleSupport.sortNewestFirst(filtered);
    }
}
