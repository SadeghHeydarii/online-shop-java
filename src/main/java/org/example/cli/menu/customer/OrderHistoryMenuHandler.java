package org.example.cli.menu.customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.exception.ShopException;
import org.example.model.entity.Order;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.vo.OrderItem;
import org.example.model.vo.PaymentTransaction;
import org.example.service.api.CustomerOrderUseCase;
import org.example.service.api.PaymentUseCase;

/**
 * Customer CLI handler dedicated to order history inspection, retry payment, and cancellation (SRP, ISP, DIP).
 */
public class OrderHistoryMenuHandler {

    private final CustomerOrderUseCase customerOrderUseCase;
    private final PaymentUseCase paymentUseCase;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs an {@code OrderHistoryMenuHandler}.
     *
     * @param customerOrderUseCase customer order history and lifecycle port
     * @param paymentUseCase       payment authorization port
     * @param in                   narrow console input port
     * @param out                  narrow console output port
     */
    public OrderHistoryMenuHandler(CustomerOrderUseCase customerOrderUseCase,
                                   PaymentUseCase paymentUseCase,
                                   InputReader in,
                                   OutputWriter out) {
        this.customerOrderUseCase = Objects.requireNonNull(customerOrderUseCase, "customerOrderUseCase cannot be null");
        this.paymentUseCase = Objects.requireNonNull(paymentUseCase, "paymentUseCase cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    /**
     * Displays the customer's order history and interactive order actions.
     *
     * @param customer authenticated customer
     */
    public void handleOrderHistory(User customer) {
        Objects.requireNonNull(customer, "customer cannot be null");
        while (true) {
            out.printHeader("My Order History");
            List<Order> orders = customerOrderUseCase.getOrderHistory(customer.getId());
            if (orders.isEmpty()) {
                out.printInfo("You have not placed any orders yet.");
                return;
            }

            printOrdersSummary(orders);

            out.println();
            out.println("Order Actions:");
            out.println("  1. View Order Details");
            out.println("  2. Pay Pending / Failed Order");
            out.println("  3. Cancel Order");
            out.println("  0. Back to Customer Menu");

            int action = in.readInt("Select order action", 0, 3);
            switch (action) {
                case 1 -> handleViewDetails(customer.getId());
                case 2 -> handleRetryPayment(customer.getId());
                case 3 -> handleCancelOrder(customer.getId());
                case 0 -> { return; }
                default -> out.printError("Invalid choice.");
            }
        }
    }

    private void printOrdersSummary(List<Order> orders) {
        List<String> headers = List.of("Order ID", "Date", "Status", "Items", "Payable Total");
        List<List<String>> rows = new ArrayList<>();
        for (Order o : orders) {
            rows.add(List.of(
                    o.getOrderId(),
                    o.getCreatedAt().toLocalDate().toString(),
                    o.getStatus().name(),
                    String.valueOf(o.getItems().size()),
                    o.getPayableAmount().amount() + " " + o.getPayableAmount().currency()
            ));
        }
        out.printTable(headers, rows);
    }

    private void handleViewDetails(String userId) {
        String oid = in.readString("Enter Order ID to inspect");
        try {
            Order o = customerOrderUseCase.getOrderDetails(userId, oid);
            displayOrderDetails(o);
        } catch (ShopException | IllegalArgumentException e) {
            out.printError(e.getMessage());
        }
    }

    private void handleRetryPayment(String userId) {
        String oid = in.readString("Enter Order ID to pay");
        try {
            Order o = customerOrderUseCase.getOrderDetails(userId, oid);
            if (o.getStatus() != OrderStatus.PENDING && o.getStatus() != OrderStatus.PAYMENT_FAILED) {
                out.printWarning("Order is currently in status " + o.getStatus() + " and cannot be paid.");
                return;
            }
            boolean success = paymentUseCase.processPayment(userId, oid, PaymentMethod.ONLINE_GATEWAY, true);
            if (success) {
                out.printSuccess("Payment completed! Order is now PAID.");
            } else {
                out.printError("Payment failed.");
            }
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Payment error: " + e.getMessage());
        }
    }

    private void handleCancelOrder(String userId) {
        String oid = in.readString("Enter Order ID to cancel");
        try {
            boolean cancelled = customerOrderUseCase.cancelOrder(userId, oid);
            if (cancelled) {
                out.printSuccess("Order #" + oid + " has been successfully CANCELLED.");
            }
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Cancellation failed: " + e.getMessage());
        }
    }

    private void displayOrderDetails(Order o) {
        out.println();
        out.printHeader("Details for Order #" + o.getOrderId());
        out.println("Status:       " + o.getStatus());
        out.println("Created At:   " + o.getCreatedAt());
        out.println("Paid At:      " + o.getPaidAt().map(Object::toString).orElse("Not paid yet"));
        out.println("Recipient:    " + o.getDeliveryAddress().recipientName() + " (" + o.getDeliveryAddress().phone() + ")");
        out.println("Address:      " + o.getDeliveryAddress().fullAddressText());
        out.println();
        out.println("Items:");

        List<String> headers = List.of("Product ID", "Name", "Unit Price", "Qty", "Total");
        List<List<String>> rows = new ArrayList<>();
        for (OrderItem item : o.getItems()) {
            rows.add(List.of(
                    item.productId(),
                    item.productName(),
                    item.unitPrice().amount() + " " + item.unitPrice().currency(),
                    String.valueOf(item.quantity().value()),
                    item.lineTotal().amount() + " " + item.lineTotal().currency()
            ));
        }
        out.printTable(headers, rows);

        out.println("Subtotal:     " + o.getItemsSubtotal().amount() + " " + o.getItemsSubtotal().currency());
        out.println("Discount:    -" + o.getDiscountAmount().amount() + " " + o.getDiscountAmount().currency());
        out.println("Shipping:    +" + o.getShippingFee().amount() + " " + o.getShippingFee().currency());
        out.println("TOTAL:        " + o.getPayableAmount().amount() + " " + o.getPayableAmount().currency());

        if (!o.getTransactions().isEmpty()) {
            out.println();
            out.println("Payment Transactions:");
            List<String> txHeaders = List.of("Txn ID", "Method", "Status", "Amount", "Reference", "Timestamp");
            List<List<String>> txRows = new ArrayList<>();
            for (PaymentTransaction tx : o.getTransactions()) {
                txRows.add(List.of(
                        tx.transactionId(),
                        tx.method().name(),
                        tx.status().name(),
                        tx.amount().amount() + " " + tx.amount().currency(),
                        tx.referenceCode() != null ? tx.referenceCode() : "-",
                        tx.timestamp().toString()
                ));
            }
            out.printTable(txHeaders, txRows);
        }
    }
}
