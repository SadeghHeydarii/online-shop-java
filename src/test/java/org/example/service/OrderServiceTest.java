package org.example.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidCouponException;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.OrderNotFoundException;
import org.example.model.entity.Category;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderService Tests")
class OrderServiceTest {

    private ProductService productService;
    private CartService cartService;
    private java.util.Map<String, User> userStore;
    private UserService userService;
    private OrderService orderService;

    private User customer;
    private Address homeAddress;
    private Address workAddress;
    private Product laptop;
    private Product mouse;
    private Coupon discountCoupon;
    private Coupon expiredCoupon;

    @BeforeEach
    void setUp() {
        productService = new ProductService();
        cartService = new CartService(productService);
        userStore = new java.util.concurrent.ConcurrentHashMap<>();
        userService = new UserService(userStore);
        orderService = new OrderService(cartService, productService, userService);

        // Seed Category & Products
        Category tech = new Category("CAT-TECH", "Technology", "Hardware gear");
        productService.registerCategory(tech);

        Money laptopPrice = new Money(new BigDecimal("1000000.00"), "IRR");
        Money mousePrice = new Money(new BigDecimal("100000.00"), "IRR");
        productService.addProduct("Laptop", "Developer laptop", laptopPrice, 10, "CAT-TECH");
        productService.addProduct("Mouse", "Ergonomic mouse", mousePrice, 20, "CAT-TECH");

        laptop = productService.searchProducts("Laptop").getFirst();
        mouse = productService.searchProducts("Mouse").getFirst();

        // Seed User & Addresses
        customer = new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", "09121112233", "hashedPass", UserRole.CUSTOMER, LocalDateTime.now());
        userStore.put(customer.getId(), customer);

        homeAddress = new Address("addr-home", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St", "1234567890", "Home", true);
        workAddress = new Address("addr-work", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Valiasr Ave", "9876543210", "Work", false);
        userService.addAddress("usr-1", homeAddress);
        userService.addAddress("usr-1", workAddress);

        // Seed Coupons
        Money maxDisc = new Money(new BigDecimal("200000.00"), "IRR");
        Money minOrder = new Money(new BigDecimal("500000.00"), "IRR");
        discountCoupon = new Coupon("SAVE10", 10, maxDisc, minOrder, LocalDate.now().plusDays(10));
        expiredCoupon = new Coupon("EXPIRED", 15, maxDisc, minOrder, LocalDate.now().minusDays(1));

        orderService.registerCoupon(discountCoupon);
        orderService.registerCoupon(expiredCoupon);
    }

    @Nested
    @DisplayName("checkout Tests")
    class CheckoutTests {

        @BeforeEach
        void fillCart() {
            cartService.addItem("usr-1", laptop.getId(), new Quantity(1));
            cartService.addItem("usr-1", mouse.getId(), new Quantity(2));
            // subtotal = 1,000,000 + (100,000 * 2) = 1,200,000 IRR
        }

        @Test
        @DisplayName("Successful checkout without coupon builds PENDING order with correct totals")
        void checkoutWithoutCouponSuccess() {
            Order order = orderService.checkout("usr-1", "addr-home", null);

            assertNotNull(order);
            assertTrue(order.getOrderId().startsWith("ord-"));
            assertEquals("usr-1", order.getUserId());
            assertEquals(OrderStatus.PENDING, order.getStatus());
            assertEquals(2, order.getItems().size());
            assertEquals("addr-home", order.getDeliveryAddress().addressId());

            Money expectedSubtotal = new Money(new BigDecimal("1200000.00"), "IRR");
            Money expectedDiscount = new Money(BigDecimal.ZERO, "IRR");
            assertEquals(expectedSubtotal, order.getItemsSubtotal());
            assertEquals(expectedDiscount, order.getDiscountAmount());
            assertEquals(expectedSubtotal, order.getPayableAmount());

            // Order must be stored
            assertEquals(order, orderService.getOrderDetails(order.getOrderId()));
        }

        @Test
        @DisplayName("Checkout using valid coupon applies calculated discount")
        void checkoutWithValidCouponSuccess() {
            // Subtotal = 1,200,000. 10% = 120,000 (below maxDiscount 200,000)
            Order order = orderService.checkout("usr-1", "addr-work", "SAVE10");

            Money expectedSubtotal = new Money(new BigDecimal("1200000.00"), "IRR");
            Money expectedDiscount = new Money(new BigDecimal("120000.00"), "IRR");
            Money expectedPayable = new Money(new BigDecimal("1080000.00"), "IRR");

            assertEquals(expectedSubtotal, order.getItemsSubtotal());
            assertEquals(expectedDiscount, order.getDiscountAmount());
            assertEquals(expectedPayable, order.getPayableAmount());
            assertEquals("addr-work", order.getDeliveryAddress().addressId());
        }

        @Test
        @DisplayName("Checkout with null addressId defaults to user's default delivery address")
        void checkoutDefaultAddress() {
            Order order = orderService.checkout("usr-1", null, null);
            assertEquals("addr-home", order.getDeliveryAddress().addressId());
            assertTrue(order.getDeliveryAddress().isDefault());
        }

        @Test
        @DisplayName("Checkout with unowned address throws InvalidInputException")
        void checkoutUnownedAddressThrows() {
            assertThrows(InvalidInputException.class, () ->
                    orderService.checkout("usr-1", "foreign-addr-id", null));
        }

        @Test
        @DisplayName("Checkout with expired coupon throws InvalidCouponException")
        void checkoutExpiredCouponThrows() {
            assertThrows(InvalidCouponException.class, () ->
                    orderService.checkout("usr-1", "addr-home", "EXPIRED"));
        }

        @Test
        @DisplayName("Checkout with non-existent coupon code throws InvalidCouponException")
        void checkoutUnknownCouponThrows() {
            assertThrows(InvalidCouponException.class, () ->
                    orderService.checkout("usr-1", "addr-home", "NONEXISTENT"));
        }

        @Test
        @DisplayName("Checkout with empty cart throws InvalidOrderStateException")
        void checkoutEmptyCartThrows() {
            cartService.clearCart("usr-1");
            assertThrows(InvalidOrderStateException.class, () ->
                    orderService.checkout("usr-1", "addr-home", null));
        }

        @Test
        @DisplayName("Checkout when stock became insufficient throws InsufficientStockException")
        void checkoutInsufficientStockThrows() {
            // Deduct stock externally before checkout
            productService.adjustStock(laptop.getId(), -10); // now stock is 0

            assertThrows(InsufficientStockException.class, () ->
                    orderService.checkout("usr-1", "addr-home", null));
        }
    }

    @Nested
    @DisplayName("processPayment Tests")
    class ProcessPaymentTests {

        private Order pendingOrder;

        @BeforeEach
        void createOrder() {
            cartService.addItem("usr-1", laptop.getId(), new Quantity(2));
            pendingOrder = orderService.checkout("usr-1", "addr-home", null);
        }

        @Test
        @DisplayName("Successful payment transitions to PAID, deducts stock, clears cart and returns true")
        void paymentSuccess() {
            int originalStock = laptop.getAvailableStock(); // 10

            boolean success = orderService.processPayment(pendingOrder.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);

            assertTrue(success);
            Order paidOrder = orderService.getOrderDetails(pendingOrder.getOrderId());
            assertEquals(OrderStatus.PAID, paidOrder.getStatus());
            assertTrue(paidOrder.getPaidAt().isPresent());

            // Stock deducted (10 - 2 = 8)
            assertEquals(originalStock - 2, laptop.getAvailableStock());

            // User's cart cleared
            assertTrue(cartService.getCart("usr-1").isEmpty());
        }

        @Test
        @DisplayName("Failed payment transitions to PAYMENT_FAILED, leaves stock and cart intact and returns false")
        void paymentFailure() {
            int originalStock = laptop.getAvailableStock();

            boolean success = orderService.processPayment(pendingOrder.getOrderId(), PaymentMethod.ONLINE_GATEWAY, false);

            assertFalse(success);
            Order failedOrder = orderService.getOrderDetails(pendingOrder.getOrderId());
            assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.getStatus());
            assertTrue(failedOrder.getPaidAt().isEmpty());

            // Stock NOT deducted
            assertEquals(originalStock, laptop.getAvailableStock());

            // Cart remains intact for user retry (US 24)
            assertFalse(cartService.getCart("usr-1").isEmpty());
        }

        @Test
        @DisplayName("Payment can be retried successfully after prior PAYMENT_FAILED")
        void paymentRetrySuccess() {
            orderService.processPayment(pendingOrder.getOrderId(), PaymentMethod.ONLINE_GATEWAY, false);
            assertEquals(OrderStatus.PAYMENT_FAILED, pendingOrder.getStatus());

            boolean retrySuccess = orderService.processPayment(pendingOrder.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
            assertTrue(retrySuccess);
            assertEquals(OrderStatus.PAID, pendingOrder.getStatus());
        }

        @Test
        @DisplayName("Processing payment on already PAID order throws InvalidOrderStateException")
        void paymentOnPaidOrderThrows() {
            orderService.processPayment(pendingOrder.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);

            assertThrows(InvalidOrderStateException.class, () ->
                    orderService.processPayment(pendingOrder.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        }
    }

    @Nested
    @DisplayName("cancelOrder Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Cancelling PENDING order marks CANCELLED without touching stock")
        void cancelPendingOrder() {
            cartService.addItem("usr-1", mouse.getId(), new Quantity(2));
            Order order = orderService.checkout("usr-1", "addr-home", null);

            int stockBefore = mouse.getAvailableStock();
            boolean cancelled = orderService.cancelOrder(order.getOrderId());

            assertTrue(cancelled);
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
            assertEquals(stockBefore, mouse.getAvailableStock());
        }

        @Test
        @DisplayName("Cancelling PAID order marks CANCELLED and restocks deducted inventory")
        void cancelPaidOrderRestocks() {
            cartService.addItem("usr-1", mouse.getId(), new Quantity(3));
            Order order = orderService.checkout("usr-1", "addr-home", null);
            orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);

            assertEquals(17, mouse.getAvailableStock()); // 20 - 3

            boolean cancelled = orderService.cancelOrder(order.getOrderId());
            assertTrue(cancelled);
            assertEquals(OrderStatus.CANCELLED, order.getStatus());

            // Stock restocked back to 20
            assertEquals(20, mouse.getAvailableStock());
        }

        @Test
        @DisplayName("Cancelling order in PROCESSING status throws InvalidOrderStateException")
        void cancelProcessingThrows() {
            cartService.addItem("usr-1", mouse.getId(), new Quantity(1));
            Order order = orderService.checkout("usr-1", "addr-home", null);
            orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
            orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PROCESSING);

            assertThrows(InvalidOrderStateException.class, () ->
                    orderService.cancelOrder(order.getOrderId()));
        }
    }

    @Nested
    @DisplayName("Queries and Status Transitions")
    class QueryAndStatusTests {

        private Order createSampleOrder(String orderId, String userId, OrderStatus targetStatus) {
            Money price = new Money(new BigDecimal("100000.00"), "IRR");
            OrderItem item = new OrderItem("prod-1", "Sample", price, new Quantity(1), price);
            Money zero = Money.ZERO;
            Order order = new Order(
                    orderId,
                    userId,
                    List.of(item),
                    homeAddress,
                    price,
                    zero,
                    zero,
                    price,
                    LocalDateTime.now()
            );

            if (targetStatus == OrderStatus.PAID) {
                order.markAsPaid(LocalDateTime.now());
            } else if (targetStatus == OrderStatus.PROCESSING) {
                order.markAsPaid(LocalDateTime.now());
                order.updateStatus(OrderStatus.PROCESSING);
            } else if (targetStatus == OrderStatus.SHIPPED) {
                order.markAsPaid(LocalDateTime.now());
                order.updateStatus(OrderStatus.PROCESSING);
                order.updateStatus(OrderStatus.SHIPPED);
            } else if (targetStatus == OrderStatus.DELIVERED) {
                order.markAsPaid(LocalDateTime.now());
                order.updateStatus(OrderStatus.PROCESSING);
                order.updateStatus(OrderStatus.SHIPPED);
                order.updateStatus(OrderStatus.DELIVERED);
            } else if (targetStatus == OrderStatus.PAYMENT_FAILED) {
                order.markAsPaymentFailed();
            } else if (targetStatus == OrderStatus.CANCELLED) {
                order.cancel();
            }

            return order;
        }

        @Test
        @DisplayName("getOrderHistory filters orders by userId properly")
        void getOrderHistoryFiltering() {
            Order o1 = createSampleOrder("ord-1", "u1", OrderStatus.PAID);
            Order o2 = createSampleOrder("ord-2", "u1", OrderStatus.DELIVERED);
            Order o3 = createSampleOrder("ord-3", "u2", OrderStatus.PAID);

            orderService.recordOrder(o1);
            orderService.recordOrder(o2);
            orderService.recordOrder(o3);

            List<Order> u1Orders = orderService.getOrderHistory("u1");
            assertEquals(2, u1Orders.size());

            List<Order> u2Orders = orderService.getOrderHistory("u2");
            assertEquals(1, u2Orders.size());

            List<Order> u3Orders = orderService.getOrderHistory("u3");
            assertTrue(u3Orders.isEmpty());
        }

        @Test
        @DisplayName("getOrderDetails throws OrderNotFoundException for non-existent order")
        void nonExistentOrderThrows() {
            assertThrows(OrderNotFoundException.class, () ->
                    orderService.getOrderDetails("ghost-order"));
        }

        @Test
        @DisplayName("updateOrderStatus advances status across valid lifecycle transitions")
        void updateOrderStatusTransitions() {
            Order order = createSampleOrder("ord-50", "u1", OrderStatus.PAID);
            orderService.recordOrder(order);

            orderService.updateOrderStatus("ord-50", OrderStatus.PROCESSING);
            assertEquals(OrderStatus.PROCESSING, order.getStatus());

            orderService.updateOrderStatus("ord-50", OrderStatus.SHIPPED);
            assertEquals(OrderStatus.SHIPPED, order.getStatus());

            orderService.updateOrderStatus("ord-50", OrderStatus.DELIVERED);
            assertEquals(OrderStatus.DELIVERED, order.getStatus());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Blank or null orderId throws InvalidInputException")
        void invalidOrderIdThrows(String orderId) {
            assertThrows(InvalidInputException.class, () -> orderService.getOrderDetails(orderId));
        }
    }
}
