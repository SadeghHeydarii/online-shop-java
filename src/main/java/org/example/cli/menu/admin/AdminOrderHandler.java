package org.example.cli.menu.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.exception.ShopException;
import org.example.model.entity.Order;
import org.example.model.enums.OrderStatus;
import org.example.service.api.OrderFulfillmentUseCase;

/**
 * Admin CLI handler dedicated to store order inspection and lifecycle transitions (SRP, ISP, DIP).
 */
public class AdminOrderHandler {

    private final OrderFulfillmentUseCase orderFulfillment;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs an {@code AdminOrderHandler}.
     *
     * @param orderFulfillment administrative order management port
     * @param in               narrow console input port
     * @param out              narrow console output port
     */
    public AdminOrderHandler(OrderFulfillmentUseCase orderFulfillment, InputReader in, OutputWriter out) {
        this.orderFulfillment = Objects.requireNonNull(orderFulfillment, "orderFulfillment cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    public void handleProcessOrders() {
        out.printHeader("Order Fulfillment & Status Advancement (US 29)");
        String orderId = in.readString("Enter Order ID to process");

        try {
            Order order = orderFulfillment.getOrderDetails(orderId);
            out.println("Order ID:       " + order.getOrderId());
            out.println("User ID:        " + order.getUserId());
            out.println("Current Status: " + order.getStatus());
            out.println("Total Amount:   " + order.getPayableAmount().amount() + " " + order.getPayableAmount().currency());
            out.printDivider();

            out.println("Available Transitions:");
            out.println("  1. Advance to PROCESSING");
            out.println("  2. Advance to SHIPPED");
            out.println("  3. Advance to DELIVERED");
            out.println("  4. Cancel Order");
            out.println("  0. Back");

            int choice = in.readInt("Choose action", 0, 4);
            switch (choice) {
                case 1 -> {
                    orderFulfillment.updateOrderStatus(orderId, OrderStatus.PROCESSING);
                    out.printSuccess("Order #" + orderId + " is now in PROCESSING state.");
                }
                case 2 -> {
                    orderFulfillment.updateOrderStatus(orderId, OrderStatus.SHIPPED);
                    out.printSuccess("Order #" + orderId + " is now SHIPPED.");
                }
                case 3 -> {
                    orderFulfillment.updateOrderStatus(orderId, OrderStatus.DELIVERED);
                    out.printSuccess("Order #" + orderId + " marked as DELIVERED.");
                }
                case 4 -> {
                    orderFulfillment.cancelOrder(orderId);
                    out.printSuccess("Order #" + orderId + " has been CANCELLED.");
                }
                case 0 -> { return; }
                default -> out.printError("Invalid action.");
            }
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Order processing error: " + e.getMessage());
        }
    }

    public void handleListOrders() {
        out.printHeader("All Store Orders");
        out.println("Statuses: " + Arrays.toString(OrderStatus.values()));
        String raw = in.readLine("Status filter (blank for all)");
        try {
            OrderStatus status = raw.isBlank() ? null : OrderStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            List<Order> orders = status == null
                    ? orderFulfillment.getAllOrders()
                    : orderFulfillment.getOrdersByStatus(status);
            List<List<String>> rows = orders.stream().map(order -> List.of(
                    order.getOrderId(),
                    order.getUserId(),
                    order.getStatus().name(),
                    order.getCreatedAt().toString(),
                    order.getPayableAmount().toString()
            )).toList();
            out.printTable(List.of("Order ID", "Customer", "Status", "Created", "Total"), rows);
        } catch (IllegalArgumentException e) {
            out.printError("Unknown order status");
        } catch (ShopException e) {
            out.printError("Failed to list orders: " + e.getMessage());
        }
    }
}
