package org.example.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.UnauthorizedActionException;
import org.example.infrastructure.persistence.InMemoryCheckoutSnapshotRepository;
import org.example.infrastructure.persistence.InMemoryCouponRepository;
import org.example.infrastructure.persistence.InMemoryOrderRepository;
import org.example.infrastructure.pricing.CouponDiscountPolicy;
import org.example.infrastructure.pricing.FreeShippingPolicy;
import org.example.model.entity.Cart;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.model.vo.CartItem;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.api.CartCheckoutPort;
import org.example.service.api.ProductInventoryPort;
import org.example.service.port.CheckoutSnapshotRepository;
import org.example.service.port.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderCheckoutServiceTest {

    private OrderRepository orderRepository;
    private CheckoutSnapshotRepository snapshotRepository;
    private Cart cart;
    private User user;
    private Product product;
    private Map<String, Integer> restockDeltas;
    private OrderCheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);
        orderRepository = new InMemoryOrderRepository();
        snapshotRepository = new InMemoryCheckoutSnapshotRepository();

        product = new Product("prod-1", "Keyboard", "Mechanical",
                new Money(new BigDecimal("500000.00"), "IRR"), 10, "CAT-1");
        cart = new Cart("cart-1", "usr-1");
        cart.addProduct(product, new Quantity(2));

        user = new User("usr-1", "Ali", "ali@aut.ac.ir", "09121112233", "hash",
                UserRole.CUSTOMER, LocalDateTime.now(clock));
        user.addAddress(new Address("addr-1", "Ali", "09121112233", "Tehran", "Tehran",
                "Hafez", "1234567890", "Home", true));

        restockDeltas = new HashMap<>();
        AtomicInteger seq = new AtomicInteger(1);

        CartCheckoutPort cartPort = new CartCheckoutPort() {
            @Override
            public Cart getCart(String userId) {
                return cart;
            }

            @Override
            public void removeCheckedOutItems(String userId, List<CartItem> snapshot) {
                cart.removeCheckedOutItems(snapshot);
            }

            @Override
            public void clearCart(String userId) {
                cart.clear();
            }
        };

        ProductInventoryPort inventoryPort = new ProductInventoryPort() {
            @Override
            public void adjustStock(String productId, int delta) {
                restockDeltas.merge(productId, delta, Integer::sum);
            }

            @Override
            public void recordSale(String productId, Quantity quantity) {
            }
        };

        OrderPricingService pricingService = new OrderPricingService(
                new CouponDiscountPolicy(new InMemoryCouponRepository()),
                new FreeShippingPolicy(),
                clock
        );

        checkoutService = new OrderCheckoutService(
                orderRepository,
                snapshotRepository,
                cartPort,
                uid -> user,
                (pid, qty) -> product,
                inventoryPort,
                pricingService,
                prefix -> prefix + "-" + seq.getAndIncrement(),
                clock
        );
    }

    @Test
    void checkoutSavesOrderAndSnapshotUsingDefaultAddress() {
        Order order = checkoutService.checkout("usr-1", null, null);

        assertEquals("ord-1", order.getOrderId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(0, order.getPayableAmount().amount().compareTo(new BigDecimal("1000000.00")));
        assertEquals(order, orderRepository.findById("ord-1").orElseThrow());
        assertFalse(checkoutService.hasPurchasedProduct("usr-1", "prod-1"));
    }

    @Test
    void paidOrderAppearsInPurchaseHistoryAndRestocksOnCustomerCancellation() {
        Order order = checkoutService.checkout("usr-1", "addr-1", null);
        order.markAsPaid(LocalDateTime.of(2026, 11, 24, 12, 5));
        orderRepository.save(order);

        assertTrue(checkoutService.hasPurchasedProduct("usr-1", "prod-1"));
        assertEquals(1, checkoutService.getOrderHistory("usr-1").size());

        assertTrue(checkoutService.cancelOrder("usr-1", "ord-1"));
        assertEquals(OrderStatus.CANCELLED, orderRepository.findById("ord-1").orElseThrow().getStatus());
        assertEquals(2, restockDeltas.get("prod-1"));
        assertFalse(checkoutService.hasPurchasedProduct("usr-1", "prod-1"));
    }

    @Test
    void enforcesOwnershipAndNonEmptyCart() {
        Order order = checkoutService.checkout("usr-1", "addr-1", null);
        assertThrows(UnauthorizedActionException.class,
                () -> checkoutService.getOrderDetails("other-user", order.getOrderId()));

        cart.clear();
        assertThrows(InvalidOrderStateException.class,
                () -> checkoutService.checkout("usr-1", "addr-1", null));
        assertThrows(InvalidInputException.class,
                () -> checkoutService.getOrderDetails("usr-1", "   "));
    }
}
