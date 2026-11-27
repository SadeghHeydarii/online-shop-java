package org.example.cli.menu.customer;

import java.util.List;
import java.util.Objects;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.exception.ShopException;
import org.example.model.entity.Cart;
import org.example.model.entity.Order;
import org.example.model.entity.User;
import org.example.model.enums.PaymentMethod;
import org.example.model.vo.Address;
import org.example.service.api.CartUseCase;
import org.example.service.api.CustomerOrderUseCase;
import org.example.service.api.PaymentUseCase;

/**
 * Customer CLI handler dedicated to checkout, address/coupon selection, and payment initiation (SRP, ISP, DIP).
 */
public class CheckoutMenuHandler {

    private final CartUseCase cartUseCase;
    private final CustomerOrderUseCase customerOrderUseCase;
    private final PaymentUseCase paymentUseCase;
    private final ProfileMenuHandler profileHandler;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs a {@code CheckoutMenuHandler}.
     *
     * @param cartUseCase          shopping cart port
     * @param customerOrderUseCase order checkout port
     * @param paymentUseCase       payment authorization port
     * @param profileHandler       profile/address lookup and creation handler
     * @param in                   narrow console input port
     * @param out                  narrow console output port
     */
    public CheckoutMenuHandler(CartUseCase cartUseCase,
                               CustomerOrderUseCase customerOrderUseCase,
                               PaymentUseCase paymentUseCase,
                               ProfileMenuHandler profileHandler,
                               InputReader in,
                               OutputWriter out) {
        this.cartUseCase = Objects.requireNonNull(cartUseCase, "cartUseCase cannot be null");
        this.customerOrderUseCase = Objects.requireNonNull(customerOrderUseCase, "customerOrderUseCase cannot be null");
        this.paymentUseCase = Objects.requireNonNull(paymentUseCase, "paymentUseCase cannot be null");
        this.profileHandler = Objects.requireNonNull(profileHandler, "profileHandler cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    public void handleCheckout(User customer) {
        Objects.requireNonNull(customer, "customer cannot be null");
        out.printHeader("Order Checkout");
        Cart cart = cartUseCase.getCart(customer.getId());
        if (cart.isEmpty()) {
            out.printWarning("Your shopping cart is empty. Add products before proceeding to checkout.");
            return;
        }

        CartMenuHandler.printCart(out, cart);

        List<Address> addresses = resolveDeliveryAddresses(customer.getId());
        if (addresses.isEmpty()) {
            return;
        }

        String addressId = selectDeliveryAddressId(addresses);
        String couponCode = in.readLine("Enter discount coupon code (optional, leave blank to skip)");
        if (couponCode.isBlank()) {
            couponCode = null;
        }

        Order order;
        try {
            order = customerOrderUseCase.checkout(customer.getId(), addressId, couponCode);
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Checkout failed: " + e.getMessage());
            return;
        }

        printOrderSummary(order);
        promptAndProcessPayment(customer.getId(), order);
    }

    private List<Address> resolveDeliveryAddresses(String userId) {
        List<Address> addresses = profileHandler.loadProfile(userId).getAddresses();
        if (!addresses.isEmpty()) {
            return addresses;
        }

        out.printWarning("You do not have any registered delivery addresses.");
        if (!in.readBoolean("Would you like to register a delivery address now?")) {
            out.printError("Checkout aborted: cannot proceed without a delivery address.");
            return List.of();
        }

        profileHandler.promptAddAddress(userId);
        List<Address> refreshed = profileHandler.loadProfile(userId).getAddresses();
        if (refreshed.isEmpty()) {
            out.printError("Checkout aborted: delivery address required.");
        }
        return refreshed;
    }

    private String selectDeliveryAddressId(List<Address> addresses) {
        out.println();
        out.printHeader("Select Delivery Address");
        for (int i = 0; i < addresses.size(); i++) {
            Address a = addresses.get(i);
            String tag = a.isDefault() ? " [DEFAULT]" : "";
            out.println((i + 1) + ". " + a.title() + tag + " -> " + a.fullAddressText() + " (Postal: " + a.postalCode() + ")");
        }

        int addrChoice = in.readInt("Choose delivery address number (or enter 0 for default address)", 0, addresses.size());
        return addrChoice > 0 ? addresses.get(addrChoice - 1).addressId() : null;
    }

    private void printOrderSummary(Order order) {
        out.println();
        out.printHeader("Order Summary - Order #" + order.getOrderId());
        out.println("Status:          " + order.getStatus());
        out.println("Delivery To:     " + order.getDeliveryAddress().recipientName() + " (" + order.getDeliveryAddress().title() + ")");
        out.println("Address Details: " + order.getDeliveryAddress().fullAddressText());
        out.println("Items Subtotal:  " + order.getItemsSubtotal().amount() + " " + order.getItemsSubtotal().currency());
        out.println("Discount:       -" + order.getDiscountAmount().amount() + " " + order.getDiscountAmount().currency());
        out.println("Shipping Fee:   +" + order.getShippingFee().amount() + " " + order.getShippingFee().currency());
        out.printDivider();
        out.println("PAYABLE AMOUNT:  " + order.getPayableAmount().amount() + " " + order.getPayableAmount().currency());
    }

    private void promptAndProcessPayment(String userId, Order order) {
        if (!in.readBoolean("Proceed to payment simulation now?")) {
            out.printInfo("Order #" + order.getOrderId() + " registered with status PENDING. You may pay later via 'My Orders'.");
            return;
        }

        out.println();
        out.println("Select Payment Method:");
        out.println("  1. Online Payment Gateway (ONLINE_GATEWAY)");
        out.println("  2. Cash on Delivery (CASH_ON_DELIVERY)");
        out.println("  3. Digital Wallet (WALLET)");
        int methodChoice = in.readInt("Choose method", 1, 3);
        PaymentMethod method = switch (methodChoice) {
            case 2 -> PaymentMethod.CASH_ON_DELIVERY;
            case 3 -> PaymentMethod.WALLET;
            default -> PaymentMethod.ONLINE_GATEWAY;
        };

        boolean simulateSuccess = in.readBoolean("Simulate successful payment authorization?");
        try {
            boolean success = paymentUseCase.processPayment(userId, order.getOrderId(), method, simulateSuccess);
            if (success) {
                out.printSuccess("Payment authorization accepted! Order #" + order.getOrderId() + " status is now PAID.");
                out.printInfo("Stock deducted. Unchanged purchased cart items were removed; later edits were kept.");
            } else {
                out.printError("Payment simulation failed or was rejected. Order marked as PAYMENT_FAILED.");
                out.printInfo("Your cart items have been preserved so you can retry payment at your convenience.");
            }
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Payment processing error: " + e.getMessage());
        }
    }
}
