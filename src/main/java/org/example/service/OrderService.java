package org.example.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.InvalidCouponException;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Cart;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.vo.Address;
import org.example.model.vo.CartItem;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.api.CartCheckoutPort;
import org.example.service.api.ProductAvailabilityPort;
import org.example.service.api.ProductInventoryPort;
import org.example.service.api.UserLookupPort;
import org.example.service.port.CheckoutSnapshotRepository;
import org.example.service.port.CouponRepository;
import org.example.service.port.DiscountPolicy;
import org.example.service.port.IdGenerator;
import org.example.service.port.OrderRepository;
import org.example.service.port.PaymentGateway;
import org.example.service.port.PaymentGatewayRegistry;
import org.example.service.port.PaymentResult;
import org.example.service.port.ShippingPolicy;

/**
 * Deprecated transitional facade delegating all operations to cohesive SOLID order services
 * ({@link OrderPricingService}, {@link OrderCheckoutService}, {@link OrderPaymentService},
 * and {@link OrderFulfillmentService}).
 *
 * @deprecated Scheduled for removal once callers migrate directly to the segregated
 *             use-case APIs in Phase 5.
 */
@Deprecated
public class OrderService {

    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final OrderCheckoutService checkoutService;
    private final OrderPaymentService paymentService;
    private final OrderFulfillmentService fulfillmentService;

    /**
     * Default constructor initializing in-memory concurrent stores for backward compatibility.
     */
    public OrderService() {
        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), null, null, null);
    }

    /**
     * Constructor accepting an explicit order store for legacy test compatibility.
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
     * Parameterized constructor bridging legacy stores and service references to cohesive services.
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
        Objects.requireNonNull(orderStore, "orderStore cannot be null");
        Objects.requireNonNull(couponStore, "couponStore cannot be null");

        this.orderRepository = new MapBackedOrderRepository(orderStore);
        this.couponRepository = new MapBackedCouponRepository(couponStore);
        CheckoutSnapshotRepository snapshotRepository = new MapBackedSnapshotRepository();
        IdGenerator idGenerator = prefix -> prefix + "-" + UUID.randomUUID();
        Clock clock = Clock.systemDefaultZone();

        CartCheckoutPort cartPort = createLegacyCartPort(cartService, productService, userService);
        UserLookupPort userPort = createLegacyUserPort(cartService, productService, userService);
        ProductAvailabilityPort availabilityPort = createLegacyAvailabilityPort(productService);
        ProductInventoryPort inventoryPort = createLegacyInventoryPort(productService);

        OrderPricingService pricingService = new OrderPricingService(
                new LegacyCouponDiscountPolicy(couponRepository),
                new LegacyFreeShippingPolicy(),
                clock
        );
        PaymentGatewayRegistry gatewayRegistry = method -> {
            if (method == null) {
                throw new InvalidInputException("Payment method cannot be null");
            }
            return new PaymentGateway() {
                @Override
                public PaymentMethod method() {
                    return method;
                }

                @Override
                public PaymentResult charge(Order order, Money amount, boolean simulateSuccess) {
                    if (!simulateSuccess) {
                        return PaymentResult.declined();
                    }
                    String refPrefix = switch (method) {
                        case ONLINE_GATEWAY -> "REF-";
                        case WALLET -> "WAL-";
                        case CASH_ON_DELIVERY -> "COD-";
                    };
                    String refCode = refPrefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
                    return PaymentResult.approved(refCode);
                }
            };
        };

        this.checkoutService = new OrderCheckoutService(
                orderRepository, snapshotRepository, cartPort, userPort,
                availabilityPort, inventoryPort, pricingService, idGenerator, clock
        );
        this.paymentService = new OrderPaymentService(
                orderRepository, snapshotRepository, cartPort, availabilityPort,
                inventoryPort, gatewayRegistry, idGenerator, clock
        );
        this.fulfillmentService = new OrderFulfillmentService(orderRepository, snapshotRepository, inventoryPort);
    }

    public Order checkout(String userId, String addressId, String couponCode) {
        return checkoutService.checkout(userId, addressId, couponCode);
    }

    public boolean processPayment(String orderId, PaymentMethod method, boolean simulateSuccess) {
        return paymentService.processPayment(orderId, method, simulateSuccess);
    }

    public boolean processPayment(String userId, String orderId, PaymentMethod method, boolean simulateSuccess) {
        return paymentService.processPayment(userId, orderId, method, simulateSuccess);
    }

    public boolean cancelOrder(String orderId) {
        return fulfillmentService.cancelOrder(orderId);
    }

    public boolean cancelOrder(String userId, String orderId) {
        return checkoutService.cancelOrder(userId, orderId);
    }

    public List<Order> getOrderHistory(String userId) {
        return checkoutService.getOrderHistory(userId);
    }

    public Order getOrderDetails(String orderId) {
        return fulfillmentService.getOrderDetails(orderId);
    }

    public Order getOrderDetails(String userId, String orderId) {
        return checkoutService.getOrderDetails(userId, orderId);
    }

    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        fulfillmentService.updateOrderStatus(orderId, newStatus);
    }

    public List<Order> getAllOrders() {
        return fulfillmentService.getAllOrders();
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return fulfillmentService.getOrdersByStatus(status);
    }

    void recordOrder(Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        orderRepository.save(order);
    }

    void registerCoupon(Coupon coupon) {
        Objects.requireNonNull(coupon, "Coupon cannot be null");
        couponRepository.save(coupon);
    }

    // =========================================================================
    // Legacy transition adapters (removed along with OrderService in Phase 5)
    // =========================================================================

    private static void requireLegacyDependencies(CartService cartService,
                                                  ProductService productService,
                                                  UserService userService) {
        if (cartService == null || productService == null || userService == null) {
            throw new IllegalStateException(
                    "OrderService dependencies (cartService, productService, userService) must be provided"
            );
        }
    }

    private static CartCheckoutPort createLegacyCartPort(CartService cartService,
                                                         ProductService productService,
                                                         UserService userService) {
        return new CartCheckoutPort() {
            @Override
            public Cart getCart(String userId) {
                requireLegacyDependencies(cartService, productService, userService);
                return cartService.getCart(userId);
            }

            @Override
            public void removeCheckedOutItems(String userId, List<CartItem> snapshot) {
                if (cartService != null) {
                    cartService.removeCheckedOutItems(userId, snapshot);
                }
            }

            @Override
            public void clearCart(String userId) {
                if (cartService != null) {
                    cartService.clearCart(userId);
                }
            }
        };
    }

    private static UserLookupPort createLegacyUserPort(CartService cartService,
                                                     ProductService productService,
                                                     UserService userService) {
        return userId -> {
            requireLegacyDependencies(cartService, productService, userService);
            return userService.getProfile(userId);
        };
    }

    private static ProductAvailabilityPort createLegacyAvailabilityPort(ProductService productService) {
        return (productId, quantity) -> {
            if (productService == null) {
                return null;
            }
            return productService.requireAvailableProduct(productId, quantity);
        };
    }

    private static ProductInventoryPort createLegacyInventoryPort(ProductService productService) {
        return new ProductInventoryPort() {
            @Override
            public void adjustStock(String productId, int delta) {
                if (productService != null) {
                    productService.adjustStock(productId, delta);
                }
            }

            @Override
            public void recordSale(String productId, Quantity quantity) {
                if (productService != null) {
                    Product product = productService.getProductById(productId);
                    product.recordSale(quantity);
                }
            }
        };
    }

    private record MapBackedOrderRepository(Map<String, Order> store) implements OrderRepository {
        @Override
        public Optional<Order> findById(String orderId) {
            return Optional.ofNullable(store.get(orderId.trim()));
        }

        @Override
        public List<Order> findByUserId(String userId) {
            String target = userId.trim();
            return store.values().stream()
                    .filter(order -> order.getUserId().equals(target))
                    .toList();
        }

        @Override
        public List<Order> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public void save(Order order) {
            store.put(order.getOrderId(), order);
        }
    }

    private record MapBackedCouponRepository(Map<String, Coupon> store) implements CouponRepository {
        @Override
        public Optional<Coupon> findByCode(String code) {
            return Optional.ofNullable(store.get(code.trim().toUpperCase(Locale.ROOT)));
        }

        @Override
        public void save(Coupon coupon) {
            store.put(coupon.code(), coupon);
        }
    }

    private static final class MapBackedSnapshotRepository implements CheckoutSnapshotRepository {
        private final Map<String, List<CartItem>> store = new ConcurrentHashMap<>();

        @Override
        public void saveSnapshot(String orderId, List<CartItem> items) {
            store.put(orderId, List.copyOf(items));
        }

        @Override
        public Optional<List<CartItem>> removeSnapshot(String orderId) {
            return Optional.ofNullable(store.remove(orderId));
        }
    }

    private record LegacyCouponDiscountPolicy(CouponRepository couponRepository) implements DiscountPolicy {
        @Override
        public Money calculateDiscount(Money subtotal, String couponCode, LocalDate evaluationDate) {
            if (couponCode == null || couponCode.isBlank()) {
                return new Money(BigDecimal.ZERO, subtotal.currency());
            }
            Coupon coupon = couponRepository.findByCode(couponCode.trim())
                    .filter(c -> c.isValid(subtotal, evaluationDate))
                    .orElseThrow(() -> new InvalidCouponException(
                            "Coupon '" + couponCode + "' is invalid, expired, or order subtotal does not meet requirements"
                    ));
            return coupon.calculateDiscount(subtotal);
        }
    }

    private static final class LegacyFreeShippingPolicy implements ShippingPolicy {
        @Override
        public Money calculateShipping(Money subtotal, Address deliveryAddress) {
            return new Money(BigDecimal.ZERO, subtotal.currency());
        }
    }
}
