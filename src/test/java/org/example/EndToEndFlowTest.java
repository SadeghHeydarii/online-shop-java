package org.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import org.example.cli.ConsoleIO;
import org.example.cli.MainMenuController;
import org.example.cli.menu.AdminMenu;
import org.example.cli.menu.CatalogMenu;
import org.example.cli.menu.CustomerMenu;
import org.example.cli.menu.GuestMenu;
import org.example.cli.menu.admin.AdminCatalogHandler;
import org.example.cli.menu.admin.AdminOrderHandler;
import org.example.cli.menu.customer.CartMenuHandler;
import org.example.cli.menu.customer.CheckoutMenuHandler;
import org.example.cli.menu.customer.OrderHistoryMenuHandler;
import org.example.cli.menu.customer.ProfileMenuHandler;
import org.example.cli.menu.customer.ReviewMenuHandler;
import org.example.cli.view.ProductTablePrinter;
import org.example.infrastructure.catalog.DefaultNameSort;
import org.example.infrastructure.catalog.MapProductSortRegistry;
import org.example.infrastructure.catalog.NewestSort;
import org.example.infrastructure.catalog.PopularSort;
import org.example.infrastructure.catalog.PriceAscendingSort;
import org.example.infrastructure.catalog.PriceDescendingSort;
import org.example.infrastructure.catalog.RatingSort;
import org.example.infrastructure.payment.CashOnDeliveryPaymentGateway;
import org.example.infrastructure.payment.MapPaymentGatewayRegistry;
import org.example.infrastructure.payment.OnlinePaymentGateway;
import org.example.infrastructure.payment.WalletPaymentGateway;
import org.example.infrastructure.persistence.InMemoryCartRepository;
import org.example.infrastructure.persistence.InMemoryCategoryRepository;
import org.example.infrastructure.persistence.InMemoryCheckoutSnapshotRepository;
import org.example.infrastructure.persistence.InMemoryCouponRepository;
import org.example.infrastructure.persistence.InMemoryCredentialRepository;
import org.example.infrastructure.persistence.InMemoryOrderRepository;
import org.example.infrastructure.persistence.InMemoryPasswordResetRepository;
import org.example.infrastructure.persistence.InMemoryProductRepository;
import org.example.infrastructure.persistence.InMemoryReviewRepository;
import org.example.infrastructure.persistence.InMemorySessionRepository;
import org.example.infrastructure.persistence.InMemoryUserRepository;
import org.example.infrastructure.pricing.CouponDiscountPolicy;
import org.example.infrastructure.pricing.FreeShippingPolicy;
import org.example.infrastructure.security.RandomVerificationCodeGenerator;
import org.example.infrastructure.security.Sha256PasswordHasher;
import org.example.infrastructure.security.UuidIdGenerator;
import org.example.infrastructure.seed.ApplicationDataSeeder;
import org.example.model.entity.Cart;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.enums.PaymentStatus;
import org.example.model.enums.UserRole;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.model.vo.Review;
import org.example.service.AuthenticationService;
import org.example.service.CartService;
import org.example.service.CatalogManagementService;
import org.example.service.CatalogQueryService;
import org.example.service.OrderCheckoutService;
import org.example.service.OrderFulfillmentService;
import org.example.service.OrderPaymentService;
import org.example.service.OrderPricingService;
import org.example.service.PasswordRecoveryService;
import org.example.service.ReviewService;
import org.example.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("End-to-End System Integration Tests")
class EndToEndFlowTest {

    private UuidIdGenerator idGenerator;
    private InMemoryUserRepository userRepository;

    private AuthenticationService authService;
    private PasswordRecoveryService recoveryService;
    private UserProfileService profileService;
    private CatalogQueryService catalogQueryService;
    private CatalogManagementService catalogManagementService;
    private CartService cartService;
    private OrderCheckoutService checkoutService;
    private OrderPaymentService paymentService;
    private OrderFulfillmentService fulfillmentService;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.systemDefaultZone();
        idGenerator = new UuidIdGenerator();
        var passwordHasher = new Sha256PasswordHasher();

        userRepository = new InMemoryUserRepository();
        var credentialRepository = new InMemoryCredentialRepository();
        var sessionRepository = new InMemorySessionRepository();
        var passwordResetRepository = new InMemoryPasswordResetRepository();
        var categoryRepository = new InMemoryCategoryRepository();
        var productRepository = new InMemoryProductRepository();
        var cartRepository = new InMemoryCartRepository();
        var orderRepository = new InMemoryOrderRepository();
        var snapshotRepository = new InMemoryCheckoutSnapshotRepository();
        var couponRepository = new InMemoryCouponRepository();
        var reviewRepository = new InMemoryReviewRepository();

        var sortRegistry = new MapProductSortRegistry(
                new DefaultNameSort(),
                new PriceAscendingSort(),
                new PriceDescendingSort(),
                new RatingSort(),
                new NewestSort(),
                new PopularSort()
        );
        var paymentGatewayRegistry = new MapPaymentGatewayRegistry(
                new OnlinePaymentGateway(idGenerator),
                new CashOnDeliveryPaymentGateway(idGenerator),
                new WalletPaymentGateway(idGenerator)
        );
        var pricingService = new OrderPricingService(
                new CouponDiscountPolicy(couponRepository), new FreeShippingPolicy(), clock);

        authService = new AuthenticationService(
                userRepository, credentialRepository, sessionRepository, passwordHasher, idGenerator, clock);
        recoveryService = new PasswordRecoveryService(
                userRepository, credentialRepository, sessionRepository, passwordResetRepository,
                passwordHasher, new RandomVerificationCodeGenerator(), clock);
        profileService = new UserProfileService(userRepository);
        catalogQueryService = new CatalogQueryService(productRepository, categoryRepository, sortRegistry);
        catalogManagementService = new CatalogManagementService(
                productRepository, categoryRepository, idGenerator, clock);
        cartService = new CartService(cartRepository, catalogQueryService);
        checkoutService = new OrderCheckoutService(
                orderRepository, snapshotRepository, cartService, profileService,
                catalogQueryService, catalogManagementService, pricingService, idGenerator, clock);
        paymentService = new OrderPaymentService(
                orderRepository, snapshotRepository, cartService,
                catalogQueryService, catalogManagementService, paymentGatewayRegistry, idGenerator, clock);
        fulfillmentService = new OrderFulfillmentService(orderRepository, snapshotRepository, catalogManagementService);
        reviewService = new ReviewService(
                reviewRepository, catalogQueryService, checkoutService, idGenerator, clock);

        new ApplicationDataSeeder(
                userRepository, credentialRepository, categoryRepository,
                catalogManagementService, couponRepository, passwordHasher, clock
        ).seed();
    }

    @Nested
    @DisplayName("Complete Customer Purchase, Payment & Review Journey")
    class CustomerPurchaseJourneyTests {

        @Test
        @DisplayName("Full flow: Sign-In -> Search -> Add to Cart -> Checkout with Coupon -> Pay -> Review")
        void fullCustomerPurchaseJourney() {
            String sessionToken = authService.login("customer@shop.com", "Pass@123");
            assertNotNull(sessionToken);
            User customer = authService.getAuthenticatedUser(sessionToken);
            assertNotNull(customer);
            assertEquals("customer@shop.com", customer.getEmail());
            assertEquals(UserRole.CUSTOMER, customer.getRole());

            List<Product> laptops = catalogQueryService.searchProducts("ThinkPad");
            assertFalse(laptops.isEmpty());
            Product thinkpad = laptops.getFirst();
            int initialLaptopStock = thinkpad.getAvailableStock();
            assertEquals(10, initialLaptopStock);

            List<Product> books = catalogQueryService.searchProducts("Clean Code");
            assertFalse(books.isEmpty());
            Product cleanCode = books.getFirst();
            int initialBookStock = cleanCode.getAvailableStock();
            assertEquals(50, initialBookStock);

            cartService.addItem(customer.getId(), thinkpad.getId(), new Quantity(1));
            cartService.addItem(customer.getId(), cleanCode.getId(), new Quantity(2));

            Cart cart = cartService.getCart(customer.getId());
            assertEquals(2, cart.getItems().size());
            assertEquals(1, cart.quantityOf(thinkpad.getId()));
            assertEquals(2, cart.quantityOf(cleanCode.getId()));

            Money subtotal = cart.calculateSubtotal();
            assertEquals(new BigDecimal("87400000.00"), subtotal.amount());

            Order pendingOrder = checkoutService.checkout(customer.getId(), "addr-home-1", "WELCOME20");
            assertNotNull(pendingOrder);
            assertEquals(OrderStatus.PENDING, pendingOrder.getStatus());
            assertEquals(customer.getId(), pendingOrder.getUserId());
            assertEquals(new BigDecimal("87400000.00"), pendingOrder.getItemsSubtotal().amount());
            assertEquals(new BigDecimal("500000.00"), pendingOrder.getDiscountAmount().amount());
            assertEquals(new BigDecimal("0.00"), pendingOrder.getShippingFee().amount());
            assertEquals(new BigDecimal("86900000.00"), pendingOrder.getPayableAmount().amount());

            assertEquals(initialLaptopStock, catalogQueryService.getProductById(thinkpad.getId()).getAvailableStock());

            boolean paymentSuccess = paymentService.processPayment(
                    pendingOrder.getOrderId(),
                    PaymentMethod.ONLINE_GATEWAY,
                    true
            );
            assertTrue(paymentSuccess);

            Order paidOrder = fulfillmentService.getOrderDetails(pendingOrder.getOrderId());
            assertEquals(OrderStatus.PAID, paidOrder.getStatus());
            assertTrue(paidOrder.getPaidAt().isPresent());
            assertEquals(1, paidOrder.getTransactions().size());
            assertEquals(PaymentStatus.SUCCESS, paidOrder.getTransactions().get(0).status());
            assertEquals(paidOrder.getPayableAmount(), paidOrder.getTransactions().get(0).amount());

            assertEquals(initialLaptopStock - 1, catalogQueryService.getProductById(thinkpad.getId()).getAvailableStock());
            assertEquals(initialBookStock - 2, catalogQueryService.getProductById(cleanCode.getId()).getAvailableStock());

            Cart clearedCart = cartService.getCart(customer.getId());
            assertTrue(clearedCart.isEmpty());

            reviewService.addReview(
                    customer.getId(),
                    customer.getFullName(),
                    thinkpad.getId(),
                    5,
                    "Exceptional build quality and Linux compatibility for development!"
            );

            List<Review> reviews = reviewService.getProductReviews(thinkpad.getId());
            assertEquals(1, reviews.size());
            assertEquals(5, reviews.getFirst().rating());
            assertEquals(customer.getId(), reviews.getFirst().userId());

            Product updatedThinkpad = catalogQueryService.getProductById(thinkpad.getId());
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
            String adminToken = authService.login("admin@shop.com", "Admin@123");
            assertNotNull(adminToken);
            User admin = authService.getAuthenticatedUser(adminToken);
            assertEquals(UserRole.ADMIN, admin.getRole());

            catalogManagementService.addProduct(
                    "4K USB-C Monitor",
                    "32-inch 4K IPS monitor with 90W power delivery",
                    new Money(new BigDecimal("28000000.00"), "IRR"),
                    5,
                    "CAT-DIGITAL"
            );

            Product monitor = catalogQueryService.searchProducts("4K USB-C Monitor").getFirst();
            assertEquals(5, monitor.getAvailableStock());

            catalogManagementService.adjustStock(monitor.getId(), 10);
            assertEquals(15, catalogQueryService.getProductById(monitor.getId()).getAvailableStock());

            User customer = userRepository.findById("usr-customer").orElseThrow();
            cartService.addItem(customer.getId(), monitor.getId(), new Quantity(2));
            Order order = checkoutService.checkout(customer.getId(), null, null);
            paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);

            assertEquals(OrderStatus.PAID, fulfillmentService.getOrderDetails(order.getOrderId()).getStatus());
            assertEquals(13, catalogQueryService.getProductById(monitor.getId()).getAvailableStock());

            fulfillmentService.updateOrderStatus(order.getOrderId(), OrderStatus.PROCESSING);
            assertEquals(OrderStatus.PROCESSING, fulfillmentService.getOrderDetails(order.getOrderId()).getStatus());

            fulfillmentService.updateOrderStatus(order.getOrderId(), OrderStatus.SHIPPED);
            assertEquals(OrderStatus.SHIPPED, fulfillmentService.getOrderDetails(order.getOrderId()).getStatus());

            fulfillmentService.updateOrderStatus(order.getOrderId(), OrderStatus.DELIVERED);
            assertEquals(OrderStatus.DELIVERED, fulfillmentService.getOrderDetails(order.getOrderId()).getStatus());
        }
    }

    @Nested
    @DisplayName("Payment Failure & Order Cancellation Invariant Tests")
    class TransactionalInvariantsTests {

        @Test
        @DisplayName("Payment failure sets PAYMENT_FAILED, preserves cart items, and allows successful retry")
        void paymentFailureAndRetryPreservesCart() {
            User customer = userRepository.findById("usr-customer").orElseThrow();
            Product book = catalogQueryService.searchProducts("Clean Code").getFirst();
            int initialStock = book.getAvailableStock();

            cartService.addItem(customer.getId(), book.getId(), new Quantity(1));
            Order order = checkoutService.checkout(customer.getId(), null, null);

            boolean failed = paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, false);
            assertFalse(failed);

            Order failedOrder = fulfillmentService.getOrderDetails(order.getOrderId());
            assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.getStatus());

            assertFalse(cartService.getCart(customer.getId()).isEmpty());
            assertEquals(initialStock, catalogQueryService.getProductById(book.getId()).getAvailableStock());

            boolean retrySuccess = paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
            assertTrue(retrySuccess);

            Order paidOrder = fulfillmentService.getOrderDetails(order.getOrderId());
            assertEquals(OrderStatus.PAID, paidOrder.getStatus());
            assertTrue(cartService.getCart(customer.getId()).isEmpty());
            assertEquals(initialStock - 1, catalogQueryService.getProductById(book.getId()).getAvailableStock());
        }

        @Test
        @DisplayName("Order cancellation automatically restocks inventory back to catalog")
        void orderCancellationRestocksInventory() {
            User customer = userRepository.findById("usr-customer").orElseThrow();
            Product hoodie = catalogQueryService.searchProducts("Hoodie").getFirst();
            int initialStock = hoodie.getAvailableStock();

            cartService.addItem(customer.getId(), hoodie.getId(), new Quantity(3));
            Order order = checkoutService.checkout(customer.getId(), null, null);
            paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);

            assertEquals(initialStock - 3, catalogQueryService.getProductById(hoodie.getId()).getAvailableStock());

            boolean cancelled = fulfillmentService.cancelOrder(order.getOrderId());
            assertTrue(cancelled);

            Order cancelledOrder = fulfillmentService.getOrderDetails(order.getOrderId());
            assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
            assertEquals(initialStock, catalogQueryService.getProductById(hoodie.getId()).getAvailableStock());
        }
    }

    @Nested
    @DisplayName("End-to-End Terminal CLI Session Simulation")
    class CliSessionSimulationTests {

        @Test
        @DisplayName("Complete CLI session: Guest Login -> Customer View -> Logout -> Guest Exit")
        void completeCliInteractiveSession() {
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
            ProductTablePrinter tablePrinter = new ProductTablePrinter(io);
            CatalogMenu catalogMenu = new CatalogMenu(catalogQueryService, reviewService, tablePrinter, io);
            ProfileMenuHandler profileHandler = new ProfileMenuHandler(profileService, profileService, idGenerator, io, io);

            GuestMenu guestMenu = new GuestMenu(authService, recoveryService, catalogMenu, io, io);
            CustomerMenu customerMenu = new CustomerMenu(
                    catalogMenu,
                    new CartMenuHandler(cartService, catalogQueryService, tablePrinter, io, io),
                    new CheckoutMenuHandler(cartService, checkoutService, paymentService, profileHandler, io, io),
                    new OrderHistoryMenuHandler(checkoutService, paymentService, io, io),
                    new ReviewMenuHandler(reviewService, io, io),
                    profileHandler,
                    io,
                    io
            );
            AdminMenu adminMenu = new AdminMenu(
                    new AdminCatalogHandler(catalogManagementService, catalogQueryService, tablePrinter, io, io),
                    new AdminOrderHandler(fulfillmentService, io, io),
                    io,
                    io
            );

            MainMenuController controller = new MainMenuController(
                    guestMenu, List.of(customerMenu, adminMenu), authService, io);
            controller.start();

            String consoleOutput = out.toString(StandardCharsets.UTF_8);

            assertTrue(consoleOutput.contains("WELCOME TO AMIRKABIR ONLINE SHOP (AUT)"));
            assertTrue(consoleOutput.contains("Sign In to Online Shop"));
            assertTrue(consoleOutput.contains("Login successful! Welcome back, Mohammad Rezaei [CUSTOMER]"));
            assertTrue(consoleOutput.contains("Customer Dashboard - Welcome, Mohammad Rezaei"));
            assertTrue(consoleOutput.contains("My Profile & Address Book"));
            assertTrue(consoleOutput.contains("Logging out of customer session. Goodbye!"));
            assertTrue(consoleOutput.contains("Thank you for using Amirkabir Online Shop. Goodbye!"));

            assertNull(controller.getCurrentUser());
            assertNull(controller.getCurrentSessionToken());
        }
    }
}
