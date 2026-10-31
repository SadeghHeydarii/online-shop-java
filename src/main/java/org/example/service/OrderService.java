package org.example.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidCouponException;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.OrderNotFoundException;
import org.example.exception.UnauthorizedActionException;
import org.example.model.entity.Cart;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.enums.PaymentStatus;
import org.example.model.vo.Address;
import org.example.model.vo.CartItem;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.PaymentTransaction;

/**
 * Application service coordinating the end-to-end checkout workflow, discount coupons,
 * payment processing simulation, order state machine lifecycle, and order history queries.
 *
 * <p>Specification compliance:
 * <ul>
 *   <li>Conforms strictly to Section 5.5, Figure 2, and Figure 5 of domain-model-specification.</li>
 *   <li>Supports checkout, discount coupons, payment transaction recording, and order cancellation.</li>
 *   <li>Enforces step-wise procedural abstraction, transactional inventory consistency, and customer isolation.</li>
 * </ul>
 */
public class OrderService {

    private final Map<String, Order> orderStore;
    private final Map<String, Coupon> couponStore;
    private final Map<String, List<CartItem>> checkoutSnapshots = new ConcurrentHashMap<>();
    private final CartService cartService;
    private final ProductService productService;
    private final UserService userService;

    /**
     * Default constructor initializing in-memory concurrent stores for testing and backward compatibility.
     */
    public OrderService() {
        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), null, null, null);
    }

    /**
     * Constructor accepting an explicit order store for Phase 6 test compatibility.
     *
     * @param orderStore map of orders
     */
    public OrderService(Map<String, Order> orderStore) {
        this(orderStore, new ConcurrentHashMap<>(), null, null, null);
    }

    /**
     * Constructor initializing in-memory stores and injecting coordinating services.
     *
     * @param cartService    cart service for cart extraction and post-payment clearing
     * @param productService product service for inventory checking and deduction
     * @param userService    user service for address verification
     */
    public OrderService(CartService cartService, ProductService productService, UserService userService) {
        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), cartService, productService, userService);
    }

    /**
     * Parameterized constructor enabling full dependency injection for testing.
     *
     * @param orderStore     in-memory store mapping order IDs to Order entities
     * @param couponStore    in-memory store mapping coupon codes to Coupon value objects
     * @param cartService    cart coordination service
     * @param productService product catalog and inventory service
     * @param userService    user account and address service
     */
    public OrderService(Map<String, Order> orderStore,
                        Map<String, Coupon> couponStore,
                        CartService cartService,
                        ProductService productService,
                        UserService userService) {
        this.orderStore = Objects.requireNonNull(orderStore, "orderStore cannot be null");
        this.couponStore = Objects.requireNonNull(couponStore, "couponStore cannot be null");
        this.cartService = cartService;
        this.productService = productService;
        this.userService = userService;
    }

    /**
     * Executes the checkout process, snapshotting cart items into an immutable Order in PENDING status.
     *
     * <p>Contract:
     * <ul>
     *   <li>REQUIRES: cart is not empty, addressId is valid for user, all products in cart have sufficient stock</li>
     *   <li>MODIFIES: orderStore, checkoutSnapshots</li>
     *   <li>EFFECTS: Builds Order in PENDING status, calculates payable amount, saves to store and returns Order.</li>
     * </ul>
     *
     * @param userId     unique identifier of the customer
     * @param addressId  optional identifier of the delivery address (if null/blank, default address is used)
     * @param couponCode optional discount coupon code to apply (if null/blank, no discount is applied)
     * @return the newly constructed {@link Order} in {@link OrderStatus#PENDING} status
     * @throws InvalidInputException       if inputs violate basic invariants or address cannot be resolved
     * @throws InvalidOrderStateException  if the customer's cart is empty
     * @throws InsufficientStockException  if any cart item exceeds currently available warehouse stock
     * @throws InvalidCouponException     if a non-blank coupon code is invalid, expired, or subtotal is below minimum
     */
    public synchronized Order checkout(String userId, String addressId, String couponCode) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be null or blank");
        }
        ensureDependenciesConfigured();

        String targetUserId = userId.trim();
        Cart cart = cartService.getCart(targetUserId);
        if (cart.isEmpty()) {
            throw new InvalidOrderStateException("Cannot checkout with an empty cart for user '" + targetUserId + "'");
        }

        // 1. Resolve and validate delivery address
        User user = userService.getProfile(targetUserId);
        Address deliveryAddress;
        if (addressId != null && !addressId.isBlank()) {
            String targetAddressId = addressId.trim();
            deliveryAddress = user.getAddresses().stream()
                    .filter(a -> a.addressId().equals(targetAddressId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidInputException(
                            "Delivery address with ID '" + targetAddressId + "' does not belong to user '" + targetUserId + "'"
                    ));
        } else {
            deliveryAddress = user.getDefaultAddress()
                    .orElseThrow(() -> new InvalidInputException(
                            "No delivery address specified and user '" + targetUserId + "' has no default delivery address"
                    ));
        }

        // 2. Validate current stock availability for all cart items
        for (CartItem cartItem : cart.getItems()) {
            productService.requireAvailableProduct(cartItem.productId(), cartItem.quantity());
        }

        // 3. Snapshot cart items into immutable OrderItem instances
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            orderItems.add(new OrderItem(
                    ci.productId(),
                    ci.productName(),
                    ci.unitPrice(),
                    ci.quantity(),
                    ci.getLineTotal()
            ));
        }

        // 4. Calculate financial totals
        Money subtotal = cart.calculateSubtotal();
        Money discount;
        if (couponCode != null && !couponCode.isBlank()) {
            String normCode = couponCode.trim().toUpperCase();
            Coupon coupon = couponStore.get(normCode);
            if (coupon == null || !coupon.isValid(subtotal, LocalDate.now())) {
                throw new InvalidCouponException(
                        "Coupon '" + couponCode + "' is invalid, expired, or order subtotal does not meet requirements"
                );
            }
            discount = coupon.calculateDiscount(subtotal);
        } else {
            discount = new Money(BigDecimal.ZERO, subtotal.currency());
        }

        Money shippingFee = new Money(BigDecimal.ZERO, subtotal.currency());
        Money payableAmount = subtotal.subtract(discount).add(shippingFee);

        // 5. Construct and record the PENDING order
        String orderId = "ord-" + UUID.randomUUID().toString();
        Order order = new Order(
                orderId,
                targetUserId,
                orderItems,
                deliveryAddress,
                subtotal,
                discount,
                shippingFee,
                payableAmount,
                LocalDateTime.now()
        );

        checkoutSnapshots.put(order.getOrderId(), List.copyOf(cart.getItems()));
        orderStore.put(order.getOrderId(), order);
        return order;
    }

    /**
     * Processes customer payment for an order, simulating bank gateway interaction.
     *
     * <p>Contract:
     * <ul>
     *   <li>REQUIRES: order exists and in PENDING/PAYMENT_FAILED status</li>
     *   <li>MODIFIES: orderStore, productService, cartService, checkoutSnapshots</li>
     *   <li>EFFECTS: If success: permanently deducts stock, increments sales count, sets Order to PAID,
     *                removes checked-out items from cart, returns true; else sets status to PAYMENT_FAILED and returns false.</li>
     * </ul>
     *
     * @param orderId         unique identifier of the order
     * @param method          the chosen payment method
     * @param simulateSuccess flag indicating whether the gateway payment simulation succeeds
     * @return {@code true} if payment succeeded and order is PAID; {@code false} if payment failed
     * @throws InvalidInputException      if inputs are null or blank
     * @throws InvalidOrderStateException if the order is not in PENDING or PAYMENT_FAILED status
     * @throws InsufficientStockException if warehouse stock was depleted before payment finalization
     */
    public synchronized boolean processPayment(String orderId, PaymentMethod method, boolean simulateSuccess) {
        if (orderId == null || orderId.isBlank()) {
            throw new InvalidInputException("Order ID cannot be null or blank");
        }
        if (method == null) {
            throw new InvalidInputException("Payment method cannot be null");
        }

        Order order = getOrderDetails(orderId);
        OrderStatus currentStatus = order.getStatus();
        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.PAYMENT_FAILED) {
            throw new InvalidOrderStateException(
                    "Order '" + orderId + "' cannot be processed for payment because it is in status " + currentStatus
            );
        }

        if (simulateSuccess) {
            // Verify stock is still sufficient and active before permanently deducting
            if (productService != null) {
                for (OrderItem item : order.getItems()) {
                    productService.requireAvailableProduct(item.productId(), item.quantity());
                }
                // Permanently deduct inventory and record sales
                for (OrderItem item : order.getItems()) {
                    productService.adjustStock(item.productId(), -item.quantity().value());
                    Product product = productService.getProductById(item.productId());
                    product.recordSale(item.quantity());
                }
            }

            LocalDateTime paymentTime = LocalDateTime.now();
            String refCode = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            PaymentTransaction tx = new PaymentTransaction(
                    "txn-" + UUID.randomUUID().toString(),
                    order.getOrderId(),
                    order.getPayableAmount(),
                    method,
                    PaymentStatus.SUCCESS,
                    paymentTime,
                    refCode
            );
            order.recordTransaction(tx);

            // Mark order as PAID
            order.markAsPaid(paymentTime);

            // Clear checked-out items from user's cart
            List<CartItem> snapshot = checkoutSnapshots.remove(order.getOrderId());
            if (cartService != null) {
                if (snapshot != null) {
                    cartService.removeCheckedOutItems(order.getUserId(), snapshot);
                } else {
                    cartService.clearCart(order.getUserId());
                }
            }
            return true;
        } else {
            PaymentTransaction failedTx = new PaymentTransaction(
                    "txn-" + UUID.randomUUID().toString(),
                    order.getOrderId(),
                    order.getPayableAmount(),
                    method,
                    PaymentStatus.FAILED,
                    LocalDateTime.now(),
                    null
            );
            order.recordTransaction(failedTx);

            // Mark payment as failed; inventory and cart remain intact for retry
            order.markAsPaymentFailed();
            return false;
        }
    }

    /**
     * User-isolated payment processing overload verifying that the order belongs to the customer.
     *
     * @param userId          unique identifier of the customer
     * @param orderId         unique identifier of the order
     * @param method          the chosen payment method
     * @param simulateSuccess flag indicating whether payment simulation succeeds
     * @return {@code true} if payment succeeded; {@code false} otherwise
     */
    public boolean processPayment(String userId, String orderId, PaymentMethod method, boolean simulateSuccess) {
        getOrderDetails(userId, orderId);
        return processPayment(orderId, method, simulateSuccess);
    }

    /**
     * Cancels an order if permitted by its current lifecycle status, restocking items if paid.
     *
     * <p>Contract:
     * <ul>
     *   <li>REQUIRES: order exists and order.canBeCancelled() is true</li>
     *   <li>MODIFIES: orderStore, productService, checkoutSnapshots</li>
     *   <li>EFFECTS: Calls order.cancel(); if previously PAID, restocks items via productService; returns true.</li>
     * </ul>
     *
     * @param orderId unique identifier of the order to cancel
     * @return {@code true} upon successful cancellation
     * @throws InvalidInputException      if orderId is null or blank
     * @throws InvalidOrderStateException if the order is in a non-cancellable state
     */
    public synchronized boolean cancelOrder(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new InvalidInputException("Order ID cannot be null or blank");
        }

        Order order = getOrderDetails(orderId);
        if (!order.canBeCancelled()) {
            throw new InvalidOrderStateException(
                    "Order '" + orderId + "' in status " + order.getStatus() + " cannot be cancelled"
            );
        }

        OrderStatus priorStatus = order.getStatus();
        order.cancel();
        checkoutSnapshots.remove(order.getOrderId());

        // If the order was previously paid, restock the inventory units
        if (priorStatus == OrderStatus.PAID && productService != null) {
            for (OrderItem item : order.getItems()) {
                productService.adjustStock(item.productId(), item.quantity().value());
            }
        }
        return true;
    }

    /**
     * User-isolated order cancellation overload verifying that the order belongs to the customer.
     *
     * @param userId  unique identifier of the customer
     * @param orderId unique identifier of the order
     * @return {@code true} upon successful cancellation
     */
    public boolean cancelOrder(String userId, String orderId) {
        getOrderDetails(userId, orderId);
        return cancelOrder(orderId);
    }

    /**
     * Retrieves all orders placed by the specified user, sorted descending by creation time.
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
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed().thenComparing(Order::getOrderId))
                .toList();
    }

    /**
     * Retrieves the details of a single order by its unique order identifier.
     *
     * @param orderId unique order identifier (non-null, non-blank)
     * @return the requested {@link Order}
     * @throws InvalidInputException  if {@code orderId} is null or blank
     * @throws OrderNotFoundException if no order exists with the given ID
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
     * Retrieves the details of an order enforcing customer ownership isolation.
     *
     * @param userId  unique identifier of the customer
     * @param orderId unique order identifier
     * @return the requested {@link Order}
     * @throws InvalidInputException       if inputs are invalid
     * @throws UnauthorizedActionException if the order does not belong to the user
     */
    public Order getOrderDetails(String userId, String orderId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be blank");
        }
        Order order = getOrderDetails(orderId);
        if (!order.getUserId().equals(userId.trim())) {
            throw new UnauthorizedActionException("This order does not belong to the current user");
        }
        return order;
    }

    /**
     * Updates the status of an existing order following valid lifecycle transitions.
     *
     * @param orderId   unique order identifier
     * @param newStatus the new status to advance or transition to
     * @throws InvalidInputException      if {@code orderId} is null/blank or {@code newStatus} is null
     * @throws InvalidOrderStateException if the transition is prohibited by the statechart
     * @throws OrderNotFoundException     if no order exists with the given ID
     */
    public synchronized void updateOrderStatus(String orderId, OrderStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidInputException("New order status cannot be null");
        }
        if (newStatus == OrderStatus.CANCELLED) {
            cancelOrder(orderId);
            return;
        }
        Order order = getOrderDetails(orderId);
        try {
            order.updateStatus(newStatus);
        } catch (IllegalStateException e) {
            throw new InvalidOrderStateException(e.getMessage());
        }
    }

    /**
     * Retrieves all orders across the store (administrative query).
     *
     * @return list of all orders sorted by creation time descending
     */
    public List<Order> getAllOrders() {
        return getOrdersByStatus(null);
    }

    /**
     * Retrieves all orders matching a specified lifecycle status, or all orders if status is null.
     *
     * @param status optional status filter (null returns all orders)
     * @return list of matching orders sorted by creation time descending
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderStore.values().stream()
                .filter(order -> status == null || order.getStatus() == status)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed().thenComparing(Order::getOrderId))
                .toList();
    }

    // =========================================================================
    // Package-private seeding helpers (strictly internal to package, not public domain API)
    // =========================================================================

    void recordOrder(Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        orderStore.put(order.getOrderId(), order);
    }

    void registerCoupon(Coupon coupon) {
        Objects.requireNonNull(coupon, "Coupon cannot be null");
        couponStore.put(coupon.code(), coupon);
    }

    private void ensureDependenciesConfigured() {
        if (cartService == null || productService == null || userService == null) {
            throw new IllegalStateException("OrderService dependencies (cartService, productService, userService) must be provided");
        }
    }
}
