package org.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.cli.ConsoleIO;
import org.example.cli.MainMenuController;
import org.example.cli.menu.AdminMenu;
import org.example.cli.menu.CustomerMenu;
import org.example.cli.menu.GuestMenu;
import org.example.model.entity.Cart;
import org.example.model.vo.Category;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.enums.PaymentStatus;
import org.example.model.vo.PaymentTransaction;
import org.example.model.enums.UserRole;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.model.vo.Review;
import org.example.service.AuthService;
import org.example.service.CartService;
import org.example.service.OrderService;
import org.example.service.ProductService;
import org.example.service.ReviewService;
import org.example.service.UserService;
import org.example.util.DataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration and system verification test suite.
 *
 * <p>Validates complete multi-layered workflows matching the authoritative domain
 * model specification and course requirements:
 * <ul>
 *   <li>Customer purchase lifecycle: Registration -> Auth -> Catalog Search -> Cart ->
 *       Checkout with Coupon -> Payment -> Stock Deduction -> Verified Review Submission.</li>
 *   <li>Administrative catalog maintenance and order fulfillment through the state machine.</li>
 *   <li>Payment failure and retry semantics preserving cart items.</li>
 *   <li>Order cancellation restocking inventory back into active catalog.</li>
 *   <li>Interactive CLI session simulation through {@link MainMenuController}.</li>
 * </ul>
 */
@DisplayName("End-to-End System Integration Tests")
class EndToEndFlowTest {

    private Map<String, User> userStore;
    private Map<String, String> credentialsStore;
    private Map<String, String> activeSessions;
    private Map<String, Category> categoryStore;
    private Map<String, Product> productStore;
    private Map<String, Cart> cartStore;
    private Map<String, Order> orderStore;
    private Map<String, Coupon> couponStore;
    private Map<String, List<Review>> reviewStore;

    private AuthService authService;
    private UserService userService;
    private ProductService productService;
    private CartService cartService;
    private OrderService orderService;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        userStore = new ConcurrentHashMap<>();
        credentialsStore = new ConcurrentHashMap<>();
        activeSessions = new ConcurrentHashMap<>();
        categoryStore = new ConcurrentHashMap<>();
        productStore = new ConcurrentHashMap<>();
        cartStore = new ConcurrentHashMap<>();
        orderStore = new ConcurrentHashMap<>();
        couponStore = new ConcurrentHashMap<>();
        reviewStore = new ConcurrentHashMap<>();

        authService = new AuthService(userStore, credentialsStore, activeSessions);
        userService = new UserService(userStore);
        productService = new ProductService(productStore, categoryStore);
        cartService = new CartService(cartStore, productService);
        orderService = new OrderService(orderStore, couponStore, cartService, productService, userService);
        reviewService = new ReviewService(reviewStore, orderService, productService);

        // Seed default initial data
        DataSeeder.seed(userStore, credentialsStore, categoryStore, productService, couponStore);
    }

    @Nested
    @DisplayName("Complete Customer Purchase, Payment & Review Journey")
    class CustomerPurchaseJourneyTests {

        @Test
        @DisplayName("Full flow: Sign-In -> Search -> Add to Cart -> Checkout with Coupon -> Pay -> Review")
        void fullCustomerPurchaseJourney() {
            // 1. Authenticate seeded customer
            String sessionToken = authService.login("customer@shop.com", "Pass@123");
            assertNotNull(sessionToken);
            User customer = authService.getAuthenticatedUser(sessionToken);
            assertNotNull(customer);
            assertEquals("customer@shop.com", customer.getEmail());
            assertEquals(UserRole.CUSTOMER, customer.getRole());

            // 2. Discover products in catalog
            List<Product> laptops = productService.searchProducts("ThinkPad");
            assertFalse(laptops.isEmpty());
            Product thinkpad = laptops.getFirst();
            int initialLaptopStock = thinkpad.getAvailableStock();
            assertEquals(10, initialLaptopStock);

            List<Product> books = productService.searchProducts("Clean Code");
            assertFalse(books.isEmpty());
            Product cleanCode = books.getFirst();
            int initialBookStock = cleanCode.getAvailableStock();
            assertEquals(50, initialBookStock);

            // 3. Add items to user's cart
            cartService.addItem(customer.getId(), thinkpad.getId(), new Quantity(1));
            cartService.addItem(customer.getId(), cleanCode.getId(), new Quantity(2));

            Cart cart = cartService.getCart(customer.getId());
            assertEquals(2, cart.getItems().size());
            assertEquals(1, cart.quantityOf(thinkpad.getId()));
            assertEquals(2, cart.quantityOf(cleanCode.getId()));

            // Subtotal: (85,000,000 * 1) + (1,200,000 * 2) = 87,400,000 IRR
            Money subtotal = cart.calculateSubtotal();
            assertEquals(new BigDecimal("87400000.00"), subtotal.amount());

            // 4. Checkout with coupon WELCOME20 (20% up to 500,000 IRR discount)
            Order pendingOrder = orderService.checkout(customer.getId(), "addr-home-1", "WELCOME20");
            assertNotNull(pendingOrder);
            assertEquals(OrderStatus.PENDING, pendingOrder.getStatus());
            assertEquals(customer.getId(), pendingOrder.getUserId());
            assertEquals(new BigDecimal("87400000.00"), pendingOrder.getItemsSubtotal().amount());

            // Max discount cap is 500,000 IRR
            assertEquals(new BigDecimal("500000.00"), pendingOrder.getDiscountAmount().amount());
            // Shipping fee: 0.00 IRR (Free shipping)
            assertEquals(new BigDecimal("0.00"), pendingOrder.getShippingFee().amount());
            // Payable: 87,400,000 - 500,000 + 0 = 86,900,000 IRR
            assertEquals(new BigDecimal("86900000.00"), pendingOrder.getPayableAmount().amount());

            // In PENDING status, stock is not deducted yet
            assertEquals(initialLaptopStock, productService.getProductById(thinkpad.getId()).getAvailableStock());

            // 5. Process Payment successfully
            boolean paymentSuccess = orderService.processPayment(
                    pendingOrder.getOrderId(),
                    PaymentMethod.ONLINE_GATEWAY,
                    true
            );
            assertTrue(paymentSuccess);

            // Verify order transitioned to PAID
            Order paidOrder = orderService.getOrderDetails(pendingOrder.getOrderId());
            assertEquals(OrderStatus.PAID, paidOrder.getStatus());
            assertTrue(paidOrder.getPaidAt().isPresent());
            assertEquals(1, paidOrder.getTransactions().size());
            assertEquals(PaymentStatus.SUCCESS, paidOrder.getTransactions().get(0).status());
            assertEquals(paidOrder.getPayableAmount(), paidOrder.getTransactions().get(0).amount());

            // Verify stock permanently deducted
            assertEquals(initialLaptopStock - 1, productService.getProductById(thinkpad.getId()).getAvailableStock());
            assertEquals(initialBookStock - 2, productService.getProductById(cleanCode.getId()).getAvailableStock());

            // Verify cart cleared post-payment
            Cart clearedCart = cartService.getCart(customer.getId());
            assertTrue(clearedCart.isEmpty());

            // 6. Submit Review as verified purchaser
            reviewService.addReview(
                    customer.getId(),
                    customer.getFullName(),
                    thinkpad.getId(),
                    5,
                    "Exceptional build quality and Linux compatibility for development!"
            );

            // Verify review persisted and rating aggregate updated
            List<Review> reviews = reviewService.getProductReviews(thinkpad.getId());
            assertEquals(1, reviews.size());
            assertEquals(5, reviews.getFirst().rating());
            assertEquals(customer.getId(), reviews.getFirst().userId());

            Product updatedThinkpad = productService.getProductById(thinkpad.getId());
            assertEquals(5.0, updatedThinkpad.getAverageRating());
            assertEquals(1, updatedThinkpad.getReviewCount());
        }
    }

    @Nested
    @DisplayName("Administrative Catalog & Order Fulfillment Tests")
    class AdminOrderFulfillmentTests {

        @Test
        @DisplayName("Admin logs in, adds product, restocks, and fulfills customer order through state transitions")
        void adminFulfillmentLifecycle() {
            // 1. Admin login
            String adminToken = authService.login("admin@shop.com", "Admin@123");
            assertNotNull(adminToken);
            User admin = authService.getAuthenticatedUser(adminToken);
            assertEquals(UserRole.ADMIN, admin.getRole());

            // 2. Admin adds a new product to catalog
            productService.addProduct(
                    "4K USB-C Monitor",
                    "32-inch 4K IPS monitor with 90W power delivery",
                    new Money(new BigDecimal("28000000.00"), "IRR"),
                    5,
                    "CAT-DIGITAL"
            );

            Product monitor = productService.searchProducts("4K USB-C Monitor").getFirst();
            assertEquals(5, monitor.getAvailableStock());

            // 3. Admin restocks product
            productService.adjustStock(monitor.getId(), 10);
            assertEquals(15, productService.getProductById(monitor.getId()).getAvailableStock());

            // 4. Customer purchases the monitor
            User customer = userStore.get("usr-customer");
            cartService.addItem(customer.getId(), monitor.getId(), new Quantity(2));
            Order order = orderService.checkout(customer.getId(), null, null);
            orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);

            assertEquals(OrderStatus.PAID, orderService.getOrderDetails(order.getOrderId()).getStatus());
            assertEquals(13, productService.getProductById(monitor.getId()).getAvailableStock());

            // 5. Admin advances order through state transitions
            orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PROCESSING);
            assertEquals(OrderStatus.PROCESSING, orderService.getOrderDetails(order.getOrderId()).getStatus());

            orderService.updateOrderStatus(order.getOrderId(), OrderStatus.SHIPPED);
            assertEquals(OrderStatus.SHIPPED, orderService.getOrderDetails(order.getOrderId()).getStatus());

            orderService.updateOrderStatus(order.getOrderId(), OrderStatus.DELIVERED);
            assertEquals(OrderStatus.DELIVERED, orderService.getOrderDetails(order.getOrderId()).getStatus());
        }
    }

    @Nested
    @DisplayName("Payment Failure & Order Cancellation Invariant Tests")
    class TransactionalInvariantsTests {

        @Test
        @DisplayName("Payment failure sets PAYMENT_FAILED, preserves cart items, and allows successful retry")
        void paymentFailureAndRetryPreservesCart() {
            User customer = userStore.get("usr-customer");
            Product book = productService.searchProducts("Clean Code").getFirst();
            int initialStock = book.getAvailableStock();

            cartService.addItem(customer.getId(), book.getId(), new Quantity(1));
            Order order = orderService.checkout(customer.getId(), null, null);

            // Simulate payment failure
            boolean failed = orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, false);
            assertFalse(failed);

            Order failedOrder = orderService.getOrderDetails(order.getOrderId());
            assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.getStatus());

            // Cart still retains the item
            assertFalse(cartService.getCart(customer.getId()).isEmpty());
            // Stock is untouched
            assertEquals(initialStock, productService.getProductById(book.getId()).getAvailableStock());

            // Customer retries payment successfully
            boolean retrySuccess = orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
            assertTrue(retrySuccess);

            Order paidOrder = orderService.getOrderDetails(order.getOrderId());
            assertEquals(OrderStatus.PAID, paidOrder.getStatus());
            assertTrue(cartService.getCart(customer.getId()).isEmpty());
            assertEquals(initialStock - 1, productService.getProductById(book.getId()).getAvailableStock());
        }

        @Test
        @DisplayName("Order cancellation automatically restocks inventory back to catalog")
        void orderCancellationRestocksInventory() {
            User customer = userStore.get("usr-customer");
            Product hoodie = productService.searchProducts("Hoodie").getFirst();
            int initialStock = hoodie.getAvailableStock();

            cartService.addItem(customer.getId(), hoodie.getId(), new Quantity(3));
            Order order = orderService.checkout(customer.getId(), null, null);
            orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);

            assertEquals(initialStock - 3, productService.getProductById(hoodie.getId()).getAvailableStock());

            // Cancel the paid order
            boolean cancelled = orderService.cancelOrder(order.getOrderId());
            assertTrue(cancelled);

            Order cancelledOrder = orderService.getOrderDetails(order.getOrderId());
            assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());

            // Stock is restored to initial level
            assertEquals(initialStock, productService.getProductById(hoodie.getId()).getAvailableStock());
        }
    }

    @Nested
    @DisplayName("End-to-End Terminal CLI Session Simulation")
    class CliSessionSimulationTests {

        @Test
        @DisplayName("Complete CLI session: Guest Login -> Customer View -> Logout -> Guest Exit")
        void completeCliInteractiveSession() {
            // Simulated user inputs:
            // 6: Login in Guest Menu
            // customer@shop.com
            // Pass@123
            // In Customer Dashboard:
            // 2: View & Manage Cart
            // 0: Back
            // 6: Profile & Delivery Address Book
            // 0: Back
            // 0: Log Out from Customer Dashboard
            // In Guest Menu:
            // 0: Exit Application
            String input = String.join("\n",
                    "6",
                    "customer@shop.com",
                    "Pass@123",
                    "2",
                    "0",
                    "6",
                    "0",
                    "0",
                    "0",
                    ""
            );

            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = new ConsoleIO(in, new PrintStream(out, true, StandardCharsets.UTF_8));

            GuestMenu guestMenu = new GuestMenu(authService, productService, reviewService, io);
            CustomerMenu customerMenu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);
            AdminMenu adminMenu = new AdminMenu(productService, orderService, io);

            MainMenuController controller = new MainMenuController(guestMenu, customerMenu, adminMenu, authService, io);
            controller.start();

            String consoleOutput = out.toString(StandardCharsets.UTF_8);

            // Verify key milestones appeared in terminal output
            assertTrue(consoleOutput.contains("WELCOME TO AMIRKABIR ONLINE SHOP (AUT)"));
            assertTrue(consoleOutput.contains("Sign In to Online Shop"));
            assertTrue(consoleOutput.contains("Login successful! Welcome back, Mohammad Rezaei [CUSTOMER]"));
            assertTrue(consoleOutput.contains("Customer Dashboard - Welcome, Mohammad Rezaei"));
            assertTrue(consoleOutput.contains("My Profile & Address Book"));
            assertTrue(consoleOutput.contains("Logging out of customer session. Goodbye!"));
            assertTrue(consoleOutput.contains("Thank you for using Amirkabir Online Shop. Goodbye!"));

            // Session state is fully cleaned up
            assertNull(controller.getCurrentUser());
        }
    }
}
