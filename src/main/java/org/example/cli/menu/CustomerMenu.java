package org.example.cli.menu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import org.example.cli.ConsoleIO;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidCouponException;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.OrderNotFoundException;
import org.example.exception.ProductNotFoundException;
import org.example.exception.UnauthorizedActionException;
import org.example.model.entity.Cart;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.vo.Address;
import org.example.model.vo.CartItem;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.PaymentTransaction;
import org.example.model.vo.Quantity;
import org.example.service.CartService;
import org.example.service.OrderService;
import org.example.service.ProductService;
import org.example.service.ReviewService;
import org.example.service.UserService;

/**
 * Interactive console menu for authenticated customers.
 *
 * <p>Coordinates customer workflows specified in Section 6.3 and Figure 3 of the authoritative
 * domain specification, covering User Stories US 05-07, US 15-28, and US 30-32:
 * <ul>
 *   <li>US 05, US 06, US 07: Profile viewing/updating and delivery address book management.</li>
 *   <li>US 15, US 16, US 17, US 18, US 19: Cart viewing, line-item adjustments, and clearance.</li>
 *   <li>US 20, US 21, US 22, US 23, US 24, US 25: Multi-step checkout, coupon discounts, and payment simulation.</li>
 *   <li>US 26, US 27, US 28: Order history inspection, status tracking, and order cancellation.</li>
 *   <li>US 30, US 31, US 32: Submission of verified product reviews and ratings.</li>
 * </ul>
 */
public class CustomerMenu {

    private final UserService userService;
    private final CartService cartService;
    private final OrderService orderService;
    private final ProductService productService;
    private final ReviewService reviewService;
    private final ConsoleIO io;
    private final CatalogMenu catalogMenu;

    /**
     * Constructs a {@code CustomerMenu} with all required coordinating domain services.
     *
     * @param userService    service managing customer accounts and addresses
     * @param cartService    service managing shopping cart operations
     * @param orderService   service coordinating orders, checkout, and payments
     * @param productService service managing catalog browsing and stock checks
     * @param reviewService  service managing customer feedback and ratings
     * @param io             console input/output utility
     */
    public CustomerMenu(UserService userService,
                        CartService cartService,
                        OrderService orderService,
                        ProductService productService,
                        ReviewService reviewService,
                        ConsoleIO io) {
        this.userService = Objects.requireNonNull(userService, "userService cannot be null");
        this.cartService = Objects.requireNonNull(cartService, "cartService cannot be null");
        this.orderService = Objects.requireNonNull(orderService, "orderService cannot be null");
        this.productService = Objects.requireNonNull(productService, "productService cannot be null");
        this.reviewService = reviewService;
        this.io = Objects.requireNonNull(io, "io cannot be null");
        this.catalogMenu = new CatalogMenu(productService, reviewService, io);
    }

    /**
     * Runs the interactive Customer Menu loop for the authenticated user until logout.
     *
     * @param user the authenticated customer
     */
    public void showMenu(User user) {
        Objects.requireNonNull(user, "User cannot be null");

        while (true) {
            User current = getLatestProfile(user.getId());
            io.printHeader("Customer Dashboard - Welcome, " + current.getFullName());
            io.println("1. Browse Catalog & Add to Cart");
            io.println("2. View & Manage Shopping Cart");
            io.println("3. Proceed to Checkout");
            io.println("4. My Order History & Tracking");
            io.println("5. Submit Product Review");
            io.println("6. Profile & Delivery Address Book");
            io.println("7. Search, Sort, Categories & Product Details");
            io.println("0. Log Out");
            io.printDivider();

            try {
                int choice = io.readInt("Select an option", 0, 7);
                switch (choice) {
                    case 1 -> handleBrowseAndAddToCart(current);
                    case 2 -> handleCartManagement(current);
                    case 3 -> handleCheckout(current);
                    case 4 -> handleOrderHistory(current);
                    case 5 -> handleSubmitReview(current);
                    case 6 -> handleProfileManagement(current);
                    case 7 -> catalogMenu.showMenu();
                    case 0 -> {
                        io.println("Logging out of customer session. Goodbye!");
                        return;
                    }
                    default -> io.printError("Invalid option selected.");
                }
            } catch (NoSuchElementException e) {
                io.println();
                return;
            } catch (IllegalArgumentException | IllegalStateException e) {
                io.printError(e.getMessage());
            }
        }
    }

    // =========================================================================
    // 1. PRODUCT BROWSING & DIRECT ADD-TO-CART
    // =========================================================================

    public void handleBrowseAndAddToCart(User user) {
        io.printHeader("Active Products Catalog");
        List<Product> products = productService.getAllActiveProducts();
        if (products.isEmpty()) {
            io.printInfo("No active products available in catalog.");
            return;
        }

        displayProductTable(products);

        boolean add = io.readBoolean("Would you like to add an item to your cart?");
        if (add) {
            String productId = io.readString("Enter Product ID");
            int qty = io.readInt("Enter Quantity", 1, 100);

            try {
                cartService.addItem(user.getId(), productId, new Quantity(qty));
                io.printSuccess("Added " + qty + " item(s) to your shopping cart!");
            } catch (InvalidInputException | InsufficientStockException | ProductNotFoundException e) {
                io.printError("Failed to add product to cart: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // 2. SHOPPING CART MANAGEMENT
    // =========================================================================

    public void handleCartManagement(User user) {
        while (true) {
            io.printHeader("My Shopping Cart");
            Cart cart = cartService.getCart(user.getId());
            displayCart(cart);

            io.println();
            io.println("Cart Actions:");
            io.println("  1. Add Product by ID");
            io.println("  2. Update Item Quantity");
            io.println("  3. Remove Item from Cart");
            io.println("  4. Clear Entire Cart");
            io.println("  0. Back to Main Menu");

            int action = io.readInt("Choose cart action", 0, 4);
            switch (action) {
                case 1 -> {
                    String pid = io.readString("Product ID");
                    int q = io.readInt("Quantity", 1, 100);
                    try {
                        cartService.addItem(user.getId(), pid, new Quantity(q));
                        io.printSuccess("Product added to cart.");
                    } catch (RuntimeException e) {
                        io.printError("Add to cart failed: " + e.getMessage());
                    }
                }
                case 2 -> {
                    if (cart.isEmpty()) {
                        io.printWarning("Your cart is empty.");
                        break;
                    }
                    String pid = io.readString("Product ID to update");
                    int q = io.readInt("New Quantity (enter positive quantity)", 1, 100);
                    try {
                        cartService.updateItemQuantity(user.getId(), pid, new Quantity(q));
                        io.printSuccess("Item quantity updated.");
                    } catch (RuntimeException e) {
                        io.printError("Update quantity failed: " + e.getMessage());
                    }
                }
                case 3 -> {
                    if (cart.isEmpty()) {
                        io.printWarning("Your cart is empty.");
                        break;
                    }
                    String pid = io.readString("Product ID to remove");
                    try {
                        cartService.removeItem(user.getId(), pid);
                        io.printSuccess("Item removed from cart.");
                    } catch (RuntimeException e) {
                        io.printError("Remove failed: " + e.getMessage());
                    }
                }
                case 4 -> {
                    if (cart.isEmpty()) {
                        io.printInfo("Cart is already empty.");
                        break;
                    }
                    boolean confirm = io.readBoolean("Are you sure you want to clear all items in your cart?");
                    if (confirm) {
                        cartService.clearCart(user.getId());
                        io.printSuccess("Cart cleared successfully.");
                    }
                }
                case 0 -> {
                    return;
                }
                default -> io.printError("Invalid action.");
            }
        }
    }

    private void displayCart(Cart cart) {
        List<CartItem> items = cart.getItems();
        if (items.isEmpty()) {
            io.printInfo("Your shopping cart is currently empty.");
            return;
        }

        List<String> headers = List.of("Product ID", "Product Name", "Unit Price", "Qty", "Line Total");
        List<List<String>> rows = new ArrayList<>();

        for (CartItem item : items) {
            String unitPrice = item.unitPrice().amount() + " " + item.unitPrice().currency();
            String lineTotal = item.getLineTotal().amount() + " " + item.getLineTotal().currency();
            rows.add(List.of(
                    item.productId(),
                    item.productName(),
                    unitPrice,
                    String.valueOf(item.quantity().value()),
                    lineTotal
            ));
        }

        io.printTable(headers, rows);
        Money subtotal = cart.calculateSubtotal();
        io.println("Cart Items Subtotal: " + subtotal.amount() + " " + subtotal.currency());
    }

    // =========================================================================
    // 3. CHECKOUT & PAYMENT ORCHESTRATION
    // =========================================================================

    public void handleCheckout(User user) {
        io.printHeader("Order Checkout");
        Cart cart = cartService.getCart(user.getId());
        if (cart.isEmpty()) {
            io.printWarning("Your shopping cart is empty. Add products before proceeding to checkout.");
            return;
        }

        displayCart(cart);

        // Address selection
        User current = getLatestProfile(user.getId());
        List<Address> addresses = current.getAddresses();
        if (addresses.isEmpty()) {
            io.printWarning("You do not have any registered delivery addresses.");
            boolean addNow = io.readBoolean("Would you like to register a delivery address now?");
            if (addNow) {
                promptAddAddress(user.getId());
                current = getLatestProfile(user.getId());
                addresses = current.getAddresses();
                if (addresses.isEmpty()) {
                    io.printError("Checkout aborted: delivery address required.");
                    return;
                }
            } else {
                io.printError("Checkout aborted: cannot proceed without a delivery address.");
                return;
            }
        }

        io.println();
        io.printHeader("Select Delivery Address");
        for (int i = 0; i < addresses.size(); i++) {
            Address a = addresses.get(i);
            String tag = a.isDefault() ? " [DEFAULT]" : "";
            io.println((i + 1) + ". " + a.title() + tag + " -> " + a.fullAddressText() + " (Postal: " + a.postalCode() + ")");
        }

        String addressId = null;
        int addrChoice = io.readInt("Choose delivery address number (or enter 0 for default address)", 0, addresses.size());
        if (addrChoice > 0) {
            addressId = addresses.get(addrChoice - 1).addressId();
        }

        // Optional coupon code
        String couponCode = io.readLine("Enter discount coupon code (optional, leave blank to skip)");
        if (couponCode.isBlank()) {
            couponCode = null;
        }

        // Checkout transaction
        Order order;
        try {
            order = orderService.checkout(user.getId(), addressId, couponCode);
        } catch (InsufficientStockException | InvalidCouponException | InvalidOrderStateException | InvalidInputException e) {
            io.printError("Checkout failed: " + e.getMessage());
            return;
        }

        io.println();
        io.printHeader("Order Summary - Order #" + order.getOrderId());
        io.println("Status:          " + order.getStatus());
        io.println("Delivery To:     " + order.getDeliveryAddress().recipientName() + " (" + order.getDeliveryAddress().title() + ")");
        io.println("Address Details: " + order.getDeliveryAddress().fullAddressText());
        io.println("Items Subtotal:  " + order.getItemsSubtotal().amount() + " " + order.getItemsSubtotal().currency());
        io.println("Discount:       -" + order.getDiscountAmount().amount() + " " + order.getDiscountAmount().currency());
        io.println("Shipping Fee:   +" + order.getShippingFee().amount() + " " + order.getShippingFee().currency());
        io.printDivider();
        io.println("PAYABLE AMOUNT:  " + order.getPayableAmount().amount() + " " + order.getPayableAmount().currency());

        boolean payNow = io.readBoolean("Proceed to payment simulation now?");
        if (!payNow) {
            io.printInfo("Order #" + order.getOrderId() + " registered with status PENDING. You may pay later via 'My Orders'.");
            return;
        }

        io.println();
        io.println("Select Payment Method:");
        io.println("  1. Online Payment Gateway (ONLINE_GATEWAY)");
        io.println("  2. Cash on Delivery (CASH_ON_DELIVERY)");
        io.println("  3. Digital Wallet (WALLET)");
        int methodChoice = io.readInt("Choose method", 1, 3);
        PaymentMethod method = switch (methodChoice) {
            case 2 -> PaymentMethod.CASH_ON_DELIVERY;
            case 3 -> PaymentMethod.WALLET;
            default -> PaymentMethod.ONLINE_GATEWAY;
        };

        boolean simulateSuccess = io.readBoolean("Simulate successful payment authorization?");

        try {
            boolean success = orderService.processPayment(user.getId(), order.getOrderId(), method, simulateSuccess);
            if (success) {
                io.printSuccess("Payment authorization accepted! Order #" + order.getOrderId() + " status is now PAID.");
                io.printInfo("Stock deducted. Unchanged purchased cart items were removed; later edits were kept.");
            } else {
                io.printError("Payment simulation failed or was rejected. Order marked as PAYMENT_FAILED.");
                io.printInfo("Your cart items have been preserved so you can retry payment at your convenience.");
            }
        } catch (RuntimeException e) {
            io.printError("Payment processing error: " + e.getMessage());
        }
    }

    // =========================================================================
    // 4. ORDER HISTORY & TRACKING
    // =========================================================================

    public void handleOrderHistory(User user) {
        while (true) {
            io.printHeader("My Order History");
            List<Order> orders = orderService.getOrderHistory(user.getId());
            if (orders.isEmpty()) {
                io.printInfo("You have not placed any orders yet.");
                return;
            }

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
            io.printTable(headers, rows);

            io.println();
            io.println("Order Actions:");
            io.println("  1. View Order Details");
            io.println("  2. Pay Pending / Failed Order");
            io.println("  3. Cancel Order");
            io.println("  0. Back to Customer Menu");

            int action = io.readInt("Select order action", 0, 3);
            switch (action) {
                case 1 -> {
                    String oid = io.readString("Enter Order ID to inspect");
                    try {
                        Order o = orderService.getOrderDetails(user.getId(), oid);
                        displayOrderDetails(o);
                    } catch (OrderNotFoundException | UnauthorizedActionException e) {
                        io.printError(e.getMessage());
                    }
                }
                case 2 -> {
                    String oid = io.readString("Enter Order ID to pay");
                    try {
                        Order o = orderService.getOrderDetails(user.getId(), oid);
                        if (o.getStatus() != OrderStatus.PENDING && o.getStatus() != OrderStatus.PAYMENT_FAILED) {
                            io.printWarning("Order is currently in status " + o.getStatus() + " and cannot be paid.");
                            break;
                        }
                        boolean success = orderService.processPayment(user.getId(), oid, PaymentMethod.ONLINE_GATEWAY, true);
                        if (success) {
                            io.printSuccess("Payment completed! Order is now PAID.");
                        } else {
                            io.printError("Payment failed.");
                        }
                    } catch (RuntimeException e) {
                        io.printError("Payment error: " + e.getMessage());
                    }
                }
                case 3 -> {
                    String oid = io.readString("Enter Order ID to cancel");
                    try {
                        boolean cancelled = orderService.cancelOrder(user.getId(), oid);
                        if (cancelled) {
                            io.printSuccess("Order #" + oid + " has been successfully CANCELLED.");
                        }
                    } catch (RuntimeException e) {
                        io.printError("Cancellation failed: " + e.getMessage());
                    }
                }
                case 0 -> {
                    return;
                }
                default -> io.printError("Invalid choice.");
            }
        }
    }

    private void displayOrderDetails(Order o) {
        io.println();
        io.printHeader("Details for Order #" + o.getOrderId());
        io.println("Status:       " + o.getStatus());
        io.println("Created At:   " + o.getCreatedAt());
        io.println("Paid At:      " + o.getPaidAt().map(Object::toString).orElse("Not paid yet"));
        io.println("Recipient:    " + o.getDeliveryAddress().recipientName() + " (" + o.getDeliveryAddress().phone() + ")");
        io.println("Address:      " + o.getDeliveryAddress().fullAddressText());
        io.println();
        io.println("Items:");

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
        io.printTable(headers, rows);

        io.println("Subtotal:     " + o.getItemsSubtotal().amount() + " " + o.getItemsSubtotal().currency());
        io.println("Discount:    -" + o.getDiscountAmount().amount() + " " + o.getDiscountAmount().currency());
        io.println("Shipping:    +" + o.getShippingFee().amount() + " " + o.getShippingFee().currency());
        io.println("TOTAL:        " + o.getPayableAmount().amount() + " " + o.getPayableAmount().currency());

        if (!o.getTransactions().isEmpty()) {
            io.println();
            io.println("Payment Transactions:");
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
            io.printTable(txHeaders, txRows);
        }
    }

    // =========================================================================
    // 5. PRODUCT REVIEWS SUBMISSION
    // =========================================================================

    public void handleSubmitReview(User user) {
        io.printHeader("Submit Product Rating & Review");
        if (reviewService == null) {
            io.printWarning("Review service is not currently available.");
            return;
        }

        String productId = io.readString("Enter Product ID");
        int rating = io.readInt("Rating (1 to 5 stars)", 1, 5);
        String comment = io.readString("Written feedback comment");

        try {
            reviewService.addReview(user.getId(), user.getFullName(), productId, rating, comment);
            io.printSuccess("Thank you! Your verified review and " + rating + "-star rating have been recorded.");
        } catch (UnauthorizedActionException e) {
            io.printError("Review submission rejected: " + e.getMessage());
            io.printInfo("Note: You must have purchased and paid for this item in a previous order before reviewing it.");
        } catch (RuntimeException e) {
            io.printError("Failed to submit review: " + e.getMessage());
        }
    }

    // =========================================================================
    // 6. PROFILE & DELIVERY ADDRESS MANAGEMENT
    // =========================================================================

    public void handleProfileManagement(User user) {
        while (true) {
            User current = getLatestProfile(user.getId());
            io.printHeader("My Profile & Address Book");
            io.println("Full Name:    " + current.getFullName());
            io.println("Email:        " + current.getEmail());
            io.println("Phone:        " + current.getPhone());
            io.println("Role:         " + current.getRole());
            io.println("Created At:   " + current.getCreatedAt().toLocalDate());

            Address def = current.getDefaultAddress().orElse(null);
            io.println("Default Addr: " + (def != null ? def.title() + " -> " + def.fullAddressText() : "None selected"));

            io.println();
            io.println("Profile Actions:");
            io.println("  1. Edit Personal Profile (Name & Phone)");
            io.println("  2. Add Delivery Address");
            io.println("  3. Remove Delivery Address");
            io.println("  4. Set Default Delivery Address");
            io.println("  5. Edit Delivery Address");
            io.println("  0. Back to Main Menu");

            int action = io.readInt("Choose profile action", 0, 5);
            switch (action) {
                case 1 -> {
                    String newName = io.readString("New Full Name");
                    String newPhone = readOrKeep("New Phone Number", current.getPhone());
                    try {
                        userService.updateProfile(user.getId(), newName, newPhone);
                        io.printSuccess("Profile updated successfully.");
                    } catch (RuntimeException e) {
                        io.printError("Profile update failed: " + e.getMessage());
                    }
                }
                case 2 -> promptAddAddress(user.getId());
                case 5 -> promptEditAddress(user.getId());
                case 3 -> {
                    List<Address> addrs = current.getAddresses();
                    if (addrs.isEmpty()) {
                        io.printInfo("Address book is empty.");
                        break;
                    }
                    displayAddresses(addrs);
                    String aid = io.readString("Enter Address ID to remove");
                    try {
                        userService.removeAddress(user.getId(), aid);
                        io.printSuccess("Address removed.");
                    } catch (RuntimeException e) {
                        io.printError("Failed to remove address: " + e.getMessage());
                    }
                }
                case 4 -> {
                    List<Address> addrs = current.getAddresses();
                    if (addrs.isEmpty()) {
                        io.printInfo("Address book is empty.");
                        break;
                    }
                    displayAddresses(addrs);
                    String aid = io.readString("Enter Address ID to set as default");
                    try {
                        userService.setDefaultAddress(user.getId(), aid);
                        io.printSuccess("Default address updated.");
                    } catch (RuntimeException e) {
                        io.printError("Failed to set default address: " + e.getMessage());
                    }
                }
                case 0 -> {
                    return;
                }
                default -> io.printError("Invalid action.");
            }
        }
    }

    private void promptAddAddress(String userId) {
        io.println();
        io.printHeader("Add New Delivery Address");
        String recipient = io.readString("Recipient Name");
        String phone = io.readString("Recipient Phone");
        String province = io.readString("Province");
        String city = io.readString("City");
        String street = io.readString("Street Address");
        String postalCode = io.readString("Postal Code (10 digits)");
        String title = io.readString("Address Label/Title (e.g. Home, Office)");
        boolean isDefault = io.readBoolean("Set this as default delivery address?");

        String addressId = "addr-" + UUID.randomUUID();
        try {
            Address address = new Address(addressId, recipient, phone, province, city, street, postalCode, title, isDefault);
            userService.addAddress(userId, address);
            io.printSuccess("Address '" + title + "' added successfully.");
        } catch (RuntimeException e) {
            io.printError("Failed to add address: " + e.getMessage());
        }
    }

    private void displayAddresses(List<Address> addresses) {
        List<String> headers = List.of("Address ID", "Title", "Recipient", "Details", "Default");
        List<List<String>> rows = new ArrayList<>();
        for (Address a : addresses) {
            rows.add(List.of(
                    a.addressId(),
                    a.title(),
                    a.recipientName(),
                    a.fullAddressText(),
                    a.isDefault() ? "YES" : "NO"
            ));
        }
        io.printTable(headers, rows);
    }

    private User getLatestProfile(String userId) {
        return userService.getProfile(userId);
    }

    private void promptEditAddress(String userId) {
        List<Address> addresses = userService.getProfile(userId).getAddresses();
        if (addresses.isEmpty()) {
            io.printInfo("Address book is empty.");
            return;
        }
        displayAddresses(addresses);
        String addressId = io.readString("Address ID to edit");
        Address current = addresses.stream().filter(address -> address.addressId().equals(addressId))
                .findFirst().orElse(null);
        if (current == null) {
            io.printError("Address does not belong to this user");
            return;
        }
        String recipient = readOrKeep("Recipient", current.recipientName());
        String phone = readOrKeep("Phone", current.phone());
        String province = readOrKeep("Province", current.province());
        String city = readOrKeep("City", current.city());
        String street = readOrKeep("Street", current.streetAddress());
        String postalCode = readOrKeep("Postal Code", current.postalCode());
        String title = readOrKeep("Title", current.title());
        boolean isDefault = io.readBoolean("Set as default address?");
        try {
            userService.updateAddress(userId, new Address(current.addressId(), recipient, phone, province, city, street, postalCode, title, isDefault));
            io.printSuccess("Address updated successfully.");
        } catch (RuntimeException e) {
            io.printError("Address update failed: " + e.getMessage());
        }
    }

    private String readOrKeep(String label, String current) {
        String value = io.readLine(label + " (blank keeps '" + current + "')");
        return value.isBlank() ? current : value;
    }


    private void displayProductTable(List<Product> products) {
        List<String> headers = List.of("ID", "Name", "Category", "Price", "Stock", "Rating");
        List<List<String>> rows = new ArrayList<>();
        for (Product p : products) {
            String priceStr = p.getPrice().amount() + " " + p.getPrice().currency();
            String ratingStr = String.format("%.1f Γÿà (%d)", p.getAverageRating(), p.getReviewCount());
            rows.add(List.of(
                    p.getId(),
                    p.getName(),
                    p.getCategoryId(),
                    priceStr,
                    String.valueOf(p.getAvailableStock()),
                    ratingStr
            ));
        }
        io.printTable(headers, rows);
    }
}
