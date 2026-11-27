package org.example.service;

import java.util.Comparator;
import java.util.List;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.OrderNotFoundException;
import org.example.exception.UnauthorizedActionException;
import org.example.model.entity.Order;
import org.example.model.enums.OrderStatus;
import org.example.model.vo.OrderItem;
import org.example.service.api.ProductInventoryPort;
import org.example.service.port.CheckoutSnapshotRepository;
import org.example.service.port.OrderRepository;

/**
 * Package-private shared helper centralizing order lookup, ownership checks, cancellation
 * restocking, and deterministic newest-first sorting across cohesive order services.
 */
final class OrderLifecycleSupport {

    private static final Comparator<Order> NEWEST_FIRST =
            Comparator.comparing(Order::getCreatedAt).reversed().thenComparing(Order::getOrderId);

    private OrderLifecycleSupport() {
    }

    static String requireNonBlank(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(fieldLabel + " cannot be null or blank");
        }
        return value.trim();
    }

    static Order requireOrder(OrderRepository orderRepository, String orderId) {
        String id = requireNonBlank(orderId, "Order ID");
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID '" + orderId + "' was not found"));
    }

    static Order requireOwnedOrder(OrderRepository orderRepository, String userId, String orderId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be blank");
        }
        Order order = requireOrder(orderRepository, orderId);
        if (!order.getUserId().equals(userId.trim())) {
            throw new UnauthorizedActionException("This order does not belong to the current user");
        }
        return order;
    }

    static boolean cancelOrder(OrderRepository orderRepository,
                               CheckoutSnapshotRepository snapshotRepository,
                               ProductInventoryPort inventoryPort,
                               Order order) {
        if (!order.canBeCancelled()) {
            throw new InvalidOrderStateException(
                    "Order '" + order.getOrderId() + "' in status " + order.getStatus() + " cannot be cancelled"
            );
        }

        OrderStatus priorStatus = order.getStatus();
        order.cancel();
        orderRepository.save(order);
        snapshotRepository.removeSnapshot(order.getOrderId());

        if (priorStatus == OrderStatus.PAID) {
            for (OrderItem item : order.getItems()) {
                inventoryPort.adjustStock(item.productId(), item.quantity().value());
            }
        }
        return true;
    }

    static List<Order> sortNewestFirst(List<Order> orders) {
        return orders.stream()
                .sorted(NEWEST_FIRST)
                .toList();
    }
}
