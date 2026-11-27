package org.example.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.model.entity.Cart;
import org.example.model.entity.Order;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.vo.Address;
import org.example.model.vo.CartItem;
import org.example.model.vo.OrderItem;
import org.example.service.OrderPricingService.PriceBreakdown;
import org.example.service.api.CartCheckoutPort;
import org.example.service.api.CustomerOrderUseCase;
import org.example.service.api.ProductAvailabilityPort;
import org.example.service.api.ProductInventoryPort;
import org.example.service.api.PurchaseHistoryQuery;
import org.example.service.api.UserLookupPort;
import org.example.service.port.CheckoutSnapshotRepository;
import org.example.service.port.IdGenerator;
import org.example.service.port.OrderRepository;

/**
 * Cohesive customer-order service coordinating checkout orchestration, personal order queries,
 * purchase history verification, and customer-owned order cancellation (SRP, ISP, DIP).
 */
public class OrderCheckoutService implements CustomerOrderUseCase, PurchaseHistoryQuery {

    private final OrderRepository orderRepository;
    private final CheckoutSnapshotRepository snapshotRepository;
    private final CartCheckoutPort cartCheckoutPort;
    private final UserLookupPort userLookupPort;
    private final ProductAvailabilityPort productAvailabilityPort;
    private final ProductInventoryPort productInventoryPort;
    private final OrderPricingService pricingService;
    private final IdGenerator idGenerator;
    private final Clock clock;

    /**
     * Constructs an {@code OrderCheckoutService} with all required ports and collaborators.
     *
     * @param orderRepository         persistence port for order aggregates
     * @param snapshotRepository      persistence port for checkout cart-item snapshots
     * @param cartCheckoutPort        cart port for retrieving customer carts
     * @param userLookupPort          user port for delivery address resolution
     * @param productAvailabilityPort product port for stock validation
     * @param productInventoryPort    inventory port for paid-order cancellation restocking
     * @param pricingService          pricing service for subtotal, discount, shipping, and payable total
     * @param idGenerator             identifier generator for new order IDs
     * @param clock                   clock for order creation timestamps
     */
    public OrderCheckoutService(OrderRepository orderRepository,
                                CheckoutSnapshotRepository snapshotRepository,
                                CartCheckoutPort cartCheckoutPort,
                                UserLookupPort userLookupPort,
                                ProductAvailabilityPort productAvailabilityPort,
                                ProductInventoryPort productInventoryPort,
                                OrderPricingService pricingService,
                                IdGenerator idGenerator,
                                Clock clock) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository cannot be null");
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository cannot be null");
        this.cartCheckoutPort = Objects.requireNonNull(cartCheckoutPort, "cartCheckoutPort cannot be null");
        this.userLookupPort = Objects.requireNonNull(userLookupPort, "userLookupPort cannot be null");
        this.productAvailabilityPort = Objects.requireNonNull(productAvailabilityPort, "productAvailabilityPort cannot be null");
        this.productInventoryPort = Objects.requireNonNull(productInventoryPort, "productInventoryPort cannot be null");
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService cannot be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public synchronized Order checkout(String userId, String addressId, String couponCode) {
        String targetUserId = OrderLifecycleSupport.requireNonBlank(userId, "User ID");
        Cart cart = cartCheckoutPort.getCart(targetUserId);
        if (cart.isEmpty()) {
            throw new InvalidOrderStateException("Cannot checkout with an empty cart for user '" + targetUserId + "'");
        }

        User user = userLookupPort.findUserById(targetUserId);
        Address deliveryAddress = resolveDeliveryAddress(user, targetUserId, addressId);

        for (CartItem cartItem : cart.getItems()) {
            productAvailabilityPort.requireAvailableProduct(cartItem.productId(), cartItem.quantity());
        }

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

        PriceBreakdown breakdown = pricingService.calculateOrderTotals(cart, deliveryAddress, couponCode);
        Order order = new Order(
                idGenerator.nextId("ord"),
                targetUserId,
                orderItems,
                deliveryAddress,
                breakdown.subtotal(),
                breakdown.discount(),
                breakdown.shippingFee(),
                breakdown.payableAmount(),
                LocalDateTime.now(clock)
        );

        snapshotRepository.saveSnapshot(order.getOrderId(), cart.getItems());
        orderRepository.save(order);
        return order;
    }

    @Override
    public synchronized boolean cancelOrder(String userId, String orderId) {
        Order order = OrderLifecycleSupport.requireOwnedOrder(orderRepository, userId, orderId);
        return OrderLifecycleSupport.cancelOrder(orderRepository, snapshotRepository, productInventoryPort, order);
    }

    @Override
    public List<Order> getOrderHistory(String userId) {
        String targetUserId = OrderLifecycleSupport.requireNonBlank(userId, "User ID");
        return OrderLifecycleSupport.sortNewestFirst(orderRepository.findByUserId(targetUserId));
    }

    @Override
    public boolean hasPurchasedProduct(String userId, String productId) {
        String targetUserId = OrderLifecycleSupport.requireNonBlank(userId, "User ID");
        String targetProductId = OrderLifecycleSupport.requireNonBlank(productId, "Product ID");
        return orderRepository.findByUserId(targetUserId).stream()
                .filter(OrderCheckoutService::isQualifiedPurchaseStatus)
                .flatMap(order -> order.getItems().stream())
                .anyMatch(item -> item.productId().equals(targetProductId));
    }

    @Override
    public Order getOrderDetails(String userId, String orderId) {
        return OrderLifecycleSupport.requireOwnedOrder(orderRepository, userId, orderId);
    }

    private Address resolveDeliveryAddress(User user, String userId, String addressId) {
        if (addressId != null && !addressId.isBlank()) {
            String targetAddressId = addressId.trim();
            return user.getAddresses().stream()
                    .filter(a -> a.addressId().equals(targetAddressId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidInputException(
                            "Delivery address with ID '" + targetAddressId + "' does not belong to user '" + userId + "'"
                    ));
        }
        return user.getDefaultAddress()
                .orElseThrow(() -> new InvalidInputException(
                        "No delivery address specified and user '" + userId + "' has no default delivery address"
                ));
    }

    private static boolean isQualifiedPurchaseStatus(Order order) {
        OrderStatus status = order.getStatus();
        return status == OrderStatus.PAID
                || status == OrderStatus.PROCESSING
                || status == OrderStatus.SHIPPED
                || status == OrderStatus.DELIVERED;
    }
}
