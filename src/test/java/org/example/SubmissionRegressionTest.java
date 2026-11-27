package org.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.example.cli.ConsoleIO;
import org.example.cli.MainMenuController;
import org.example.cli.menu.AdminMenu;
import org.example.cli.menu.AuthenticatedSession;
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
import org.example.exception.AuthenticationException;
import org.example.exception.CategoryNotFoundException;
import org.example.exception.DuplicateUserException;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.UnauthorizedActionException;
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
import org.example.model.vo.Address;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SubmissionRegressionTest {

    private final Clock clock = Clock.systemDefaultZone();
    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final Sha256PasswordHasher passwordHasher = new Sha256PasswordHasher();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final InMemoryCredentialRepository credentialRepository = new InMemoryCredentialRepository();
    private final InMemorySessionRepository sessionRepository = new InMemorySessionRepository();
    private final InMemoryPasswordResetRepository passwordResetRepository = new InMemoryPasswordResetRepository();
    private final InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
    private final InMemoryProductRepository productRepository = new InMemoryProductRepository();
    private final InMemoryCartRepository cartRepository = new InMemoryCartRepository();
    private final InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
    private final InMemoryCheckoutSnapshotRepository snapshotRepository = new InMemoryCheckoutSnapshotRepository();
    private final InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
    private final InMemoryReviewRepository reviewRepository = new InMemoryReviewRepository();

    private final AuthenticationService auth = new AuthenticationService(
            userRepository, credentialRepository, sessionRepository, passwordHasher, idGenerator, clock);
    private final PasswordRecoveryService recoveryService = new PasswordRecoveryService(
            userRepository, credentialRepository, sessionRepository, passwordResetRepository,
            passwordHasher, new RandomVerificationCodeGenerator(), clock);
    private final UserProfileService profileService = new UserProfileService(userRepository);
    private final CatalogQueryService catalogQueryService = new CatalogQueryService(
            productRepository,
            categoryRepository,
            new MapProductSortRegistry(
                    new DefaultNameSort(),
                    new PriceAscendingSort(),
                    new PriceDescendingSort(),
                    new RatingSort(),
                    new NewestSort(),
                    new PopularSort()
            )
    );
    private final CatalogManagementService catalogManagementService = new CatalogManagementService(
            productRepository, categoryRepository, idGenerator, clock);
    private final CartService cartService = new CartService(cartRepository, catalogQueryService);
    private final OrderPricingService pricingService = new OrderPricingService(
            new CouponDiscountPolicy(couponRepository), new FreeShippingPolicy(), clock);
    private final OrderCheckoutService checkoutService = new OrderCheckoutService(
            orderRepository, snapshotRepository, cartService, profileService,
            catalogQueryService, catalogManagementService, pricingService, idGenerator, clock);
    private final OrderPaymentService paymentService = new OrderPaymentService(
            orderRepository,
            snapshotRepository,
            cartService,
            catalogQueryService,
            catalogManagementService,
            new MapPaymentGatewayRegistry(
                    new OnlinePaymentGateway(idGenerator),
                    new CashOnDeliveryPaymentGateway(idGenerator),
                    new WalletPaymentGateway(idGenerator)
            ),
            idGenerator,
            clock
    );
    private final OrderFulfillmentService fulfillmentService = new OrderFulfillmentService(
            orderRepository, snapshotRepository, catalogManagementService);
    private final ReviewService reviewService = new ReviewService(
            reviewRepository, catalogQueryService, checkoutService, idGenerator, clock);

    private User alice;
    private User bob;
    private Product laptop;
    private Product keyboard;

    @BeforeEach
    void setUp() {
        new ApplicationDataSeeder(
                userRepository, credentialRepository, categoryRepository,
                catalogManagementService, couponRepository, passwordHasher, clock
        ).seed();
        alice = userRepository.findById("usr-customer").orElseThrow();
        bob = auth.register("Bob", "bob@example.com", "09123334444", "Bobpass1");
        bob.addAddress(new Address("addr-bob", "Bob", "09123334444", "Tehran", "Tehran",
                "Bob Street", "1234567890", "Home", true));
        laptop = catalogQueryService.searchProducts("ThinkPad").getFirst();
        keyboard = catalogQueryService.searchProducts("Keyboard").getFirst();
    }

    private Order checkout(User user) {
        cartService.addItem(user.getId(), laptop.getId(), new Quantity(1));
        return checkoutService.checkout(user.getId(), null, null);
    }

    private ConsoleIO io(String input, ByteArrayOutputStream output) {
        return new ConsoleIO(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    private CatalogMenu catalogMenu(ConsoleIO io) {
        return new CatalogMenu(catalogQueryService, reviewService, new ProductTablePrinter(io), io);
    }

    private GuestMenu guestMenu(ConsoleIO io) {
        return new GuestMenu(auth, recoveryService, catalogMenu(io), io, io);
    }

    private CustomerMenu customerMenu(ConsoleIO io) {
        ProfileMenuHandler profileHandler = new ProfileMenuHandler(profileService, profileService, idGenerator, io, io);
        return new CustomerMenu(
                catalogMenu(io),
                new CartMenuHandler(cartService, catalogQueryService, new ProductTablePrinter(io), io, io),
                new CheckoutMenuHandler(cartService, checkoutService, paymentService, profileHandler, io, io),
                new OrderHistoryMenuHandler(checkoutService, paymentService, io, io),
                new ReviewMenuHandler(reviewService, io, io),
                profileHandler,
                io,
                io
        );
    }

    private AdminMenu adminMenu(ConsoleIO io) {
        return new AdminMenu(
                new AdminCatalogHandler(catalogManagementService, catalogQueryService, new ProductTablePrinter(io), io, io),
                new AdminOrderHandler(fulfillmentService, io, io),
                io,
                io
        );
    }

    @Test
    void emailOnlyRegistrationAndProfileEditWork() {
        User user = auth.register("Carol", "carol@example.com", "", "Carolpass1");
        profileService.updateProfile(user.getId(), "Carol Updated", "");
        assertEquals("Carol Updated", user.getFullName());
        assertEquals("", user.getPhone());
        assertNotNull(auth.login("CAROL@EXAMPLE.COM", "Carolpass1"));
    }

    @Test
    void mobileOnlyRegistrationAndLoginWork() {
        User user = auth.register("Carol", "", "09126667777", "Carolpass1");
        assertEquals("", user.getEmail());
        assertEquals(user, auth.getAuthenticatedUser(auth.login("09126667777", "Carolpass1")));
        assertThrows(InvalidInputException.class, () -> profileService.updateProfile(user.getId(), "Carol", ""));
    }

    @Test
    void registrationRequiresAtLeastOneIdentifier() {
        assertThrows(InvalidInputException.class, () -> auth.register("Carol", "", "", "Carolpass1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"@", "a@", "a@b", ".a@example.com", "a..b@example.com", "a@-example.com", "a b@example.com"})
    void malformedEmailIsRejectedEvenWhenMobileIsValid(String email) {
        assertThrows(InvalidInputException.class, () -> auth.register("Carol", email, "09126667777", "Carolpass1"));
    }

    @Test
    void duplicateMobileIsRejectedDuringRegistrationAndProfileUpdate() {
        assertThrows(DuplicateUserException.class, () -> auth.register("Carol", "carol@example.com", bob.getPhone(), "Carolpass1"));
        assertThrows(DuplicateUserException.class, () -> profileService.updateProfile(alice.getId(), "Alice", bob.getPhone()));
        assertEquals("09121112233", alice.getPhone());
    }

    @Test
    void passwordResetChangesCredentialsInvalidatesSessionsAndCannotBeReused() {
        String token = auth.login("customer@shop.com", "Pass@123");
        String code = recoveryService.requestPasswordReset(alice.getPhone());
        assertThrows(AuthenticationException.class, () -> recoveryService.resetPassword(alice.getPhone(), "invalid", "Newpass1"));
        recoveryService.resetPassword(alice.getPhone(), code, "Newpass1");
        assertNull(auth.getAuthenticatedUser(token));
        assertThrows(AuthenticationException.class, () -> auth.login("customer@shop.com", "Pass@123"));
        assertNotNull(auth.login(alice.getPhone(), "Newpass1"));
        assertThrows(AuthenticationException.class, () -> recoveryService.resetPassword(alice.getPhone(), code, "Otherpass1"));
    }

    @Test
    void recoveryRequiresAnExistingActiveAccount() {
        assertThrows(AuthenticationException.class, () -> recoveryService.requestPasswordReset("missing@example.com"));
        alice.deactivate();
        assertThrows(AuthenticationException.class, () -> recoveryService.requestPasswordReset(alice.getPhone()));
    }

    @Test
    void replacementRecoveryCodeInvalidatesThePreviousCode() {
        String oldCode = recoveryService.requestPasswordReset(alice.getEmail());
        String newCode = recoveryService.requestPasswordReset(alice.getEmail());
        if (!oldCode.equals(newCode)) {
            assertThrows(AuthenticationException.class, () -> recoveryService.resetPassword(alice.getEmail(), oldCode, "Newpass1"));
        }
        recoveryService.resetPassword(alice.getEmail(), newCode, "Newpass1");
        assertNotNull(auth.login(alice.getEmail(), "Newpass1"));
    }

    @Test
    void customerCannotReadPayOrCancelAnotherUsersOrder() {
        Order order = checkout(alice);
        int stock = laptop.getAvailableStock();
        assertThrows(UnauthorizedActionException.class, () -> checkoutService.getOrderDetails(bob.getId(), order.getOrderId()));
        assertThrows(UnauthorizedActionException.class, () -> paymentService.processPayment(bob.getId(), order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertThrows(UnauthorizedActionException.class, () -> checkoutService.cancelOrder(bob.getId(), order.getOrderId()));
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(stock, laptop.getAvailableStock());
    }

    @Test
    void customerMenuRejectsAllCrossUserOrderActions() {
        Order victim = checkout(alice);
        checkout(bob);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String script = "1\n" + victim.getOrderId() + "\n2\n" + victim.getOrderId() + "\n3\n" + victim.getOrderId() + "\n0\n";
        customerMenu(io(script, output)).handleOrderHistory(bob);
        String displayed = output.toString(StandardCharsets.UTF_8);
        assertFalse(displayed.contains("Hafez St, No. 42, Apt 3"));
        assertTrue(displayed.contains("does not belong"));
        assertEquals(OrderStatus.PENDING, victim.getStatus());
    }

    @Test
    void ownOrderCanStillBeViewedPaidAndCancelled() {
        Order order = checkout(alice);
        assertEquals(order, checkoutService.getOrderDetails(alice.getId(), order.getOrderId()));
        assertTrue(paymentService.processPayment(alice.getId(), order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertTrue(checkoutService.cancelOrder(alice.getId(), order.getOrderId()));
    }

    @Test
    void inactiveProductCannotBeCheckedOutFromAnExistingCart() {
        cartService.addItem(alice.getId(), laptop.getId(), new Quantity(1));
        catalogManagementService.deactivateProduct(laptop.getId());
        assertThrows(InvalidInputException.class, () -> checkoutService.checkout(alice.getId(), null, null));
        assertTrue(checkoutService.getOrderHistory(alice.getId()).isEmpty());
    }

    @Test
    void productDeactivatedAfterCheckoutCannotBePaid() {
        Order order = checkout(alice);
        int stock = laptop.getAvailableStock();
        catalogManagementService.deactivateProduct(laptop.getId());
        assertThrows(InvalidInputException.class, () -> paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertEquals(stock, laptop.getAvailableStock());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertTrue(order.getTransactions().isEmpty());
    }

    @Test
    void paymentDoesNotDeductAnyStockWhenOneOrderItemBecomesUnavailable() {
        cartService.addItem(alice.getId(), laptop.getId(), new Quantity(1));
        cartService.addItem(alice.getId(), keyboard.getId(), new Quantity(1));
        Order order = checkoutService.checkout(alice.getId(), null, null);
        int laptopStock = laptop.getAvailableStock();
        catalogManagementService.deactivateProduct(keyboard.getId());
        assertThrows(InvalidInputException.class, () -> paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertEquals(laptopStock, laptop.getAvailableStock());
    }

    @Test
    void payingAnOldOrderPreservesUnrelatedLaterCartItems() {
        Order order = checkout(alice);
        cartService.addItem(alice.getId(), keyboard.getId(), new Quantity(2));
        paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        Cart remaining = cartService.getCart(alice.getId());
        assertEquals(0, remaining.quantityOf(laptop.getId()));
        assertEquals(2, remaining.quantityOf(keyboard.getId()));
    }

    @Test
    void payingAnOldOrderPreservesAReplacedCartLine() {
        Order order = checkout(alice);
        cartService.clearCart(alice.getId());
        cartService.addItem(alice.getId(), laptop.getId(), new Quantity(1));
        paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        assertEquals(1, cartService.getCart(alice.getId()).quantityOf(laptop.getId()));
    }

    @Test
    void payingAnOldOrderPreservesALineWhoseQuantityWasEdited() {
        Order order = checkout(alice);
        cartService.updateItemQuantity(alice.getId(), laptop.getId(), new Quantity(3));
        paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        assertEquals(3, cartService.getCart(alice.getId()).quantityOf(laptop.getId()));
    }

    @Test
    void repeatedFailedPaymentsRemainRetryableWithoutChangingStock() {
        Order order = checkout(alice);
        int stock = laptop.getAvailableStock();
        assertFalse(paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, false));
        assertFalse(paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, false));
        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        assertEquals(stock, laptop.getAvailableStock());
        assertEquals(2, order.getTransactions().size());
        assertTrue(paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertEquals(stock - 1, laptop.getAvailableStock());
        assertEquals(3, order.getTransactions().size());
    }

    @Test
    void cancellationThroughStatusUpdateRestocksExactlyOnce() {
        Order order = checkout(alice);
        int stock = laptop.getAvailableStock();
        paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        fulfillmentService.updateOrderStatus(order.getOrderId(), OrderStatus.CANCELLED);
        assertEquals(stock, laptop.getAvailableStock());
        assertThrows(InvalidOrderStateException.class, () -> fulfillmentService.cancelOrder(order.getOrderId()));
        assertEquals(stock, laptop.getAvailableStock());
    }

    @Test
    void negativeFilterPriceDisplaysAnErrorWithoutEscapingTheMenu() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GuestMenu menu = guestMenu(io("\n-1\n\n1\n", output));
        assertDoesNotThrow(menu::handleFilterAndSort);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[ERROR]"));
    }

    @Test
    void invalidAdminTransitionDisplaysAnErrorWithoutChangingTheOrder() {
        Order order = checkout(alice);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AdminMenu menu = adminMenu(io(order.getOrderId() + "\n2\n", output));
        assertDoesNotThrow(menu::handleProcessOrders);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[ERROR]"));
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void negativeAdminEditPriceDoesNotCrashOrChangeTheProduct() {
        Money oldPrice = laptop.getPrice();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AdminMenu menu = adminMenu(io(laptop.getId() + "\n\n\n-1\n", output));
        assertDoesNotThrow(menu::handleEditProduct);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[ERROR]"));
        assertEquals(oldPrice, laptop.getPrice());
    }

    @Test
    void controllerLogoutRevokesTheActualLoginSession() {
        ConsoleIO console = io("customer@shop.com\nPass@123\n", new ByteArrayOutputStream());
        GuestMenu guest = guestMenu(console);
        AuthenticatedSession session = guest.handleLogin().orElseThrow();
        String token = session.sessionToken();
        MainMenuController controller = new MainMenuController(
                guest, List.of(customerMenu(console), adminMenu(console)), auth, console);
        controller.setCurrentSession(session);
        controller.handleLogout();
        assertNull(controller.getCurrentUser());
        assertNull(controller.getCurrentSessionToken());
        assertNull(auth.getAuthenticatedUser(token));
    }

    @Test
    void inactiveAccountCannotKeepAnExistingSession() {
        String token = auth.login(alice.getEmail(), "Pass@123");
        alice.deactivate();
        assertNull(auth.getAuthenticatedUser(token));
    }

    @Test
    void activeCategoryBrowsingExcludesInactiveCategoriesAndProducts() {
        categoryRepository.save(new Category("CAT-HIDDEN", "Hidden", "Hidden category", false));
        assertFalse(catalogQueryService.getActiveCategories().stream().anyMatch(category -> category.id().equals("CAT-HIDDEN")));
        assertFalse(catalogQueryService.getProductsByCategory("CAT-DIGITAL").isEmpty());
        assertThrows(CategoryNotFoundException.class, () -> catalogQueryService.getProductsByCategory("CAT-HIDDEN"));
        assertThrows(CategoryNotFoundException.class, () -> catalogQueryService.filterAndSort("missing", null, null, null));
        catalogManagementService.deactivateProduct(laptop.getId());
        assertFalse(catalogQueryService.getProductsByCategory("CAT-DIGITAL").contains(laptop));
    }

    @Test
    void newestSortingUsesProductCreationTime() {
        Product older = new Product("older", "Older", "Older product", Money.ZERO, 1, "CAT-BOOKS", LocalDateTime.of(2020, 1, 1, 0, 0));
        Product newer = new Product("newer", "Newer", "Newer product", Money.ZERO, 1, "CAT-BOOKS", LocalDateTime.of(2090, 1, 1, 0, 0));
        productRepository.save(older);
        productRepository.save(newer);
        List<Product> sorted = catalogQueryService.filterAndSort("CAT-BOOKS", null, null, "newest");
        assertEquals(newer, sorted.getFirst());
        assertEquals(older, sorted.getLast());
    }

    @Test
    void popularSortingUsesSuccessfulPaidUnits() {
        Order order = checkout(alice);
        paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        assertEquals(1, laptop.getSoldCount());
        assertEquals(laptop, catalogQueryService.filterAndSort(null, null, null, "popular").getFirst());
        assertThrows(InvalidOrderStateException.class, () -> paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertEquals(1, laptop.getSoldCount());
    }

    @Test
    void imageManagementValidatesUrlsAndReturnsAnImmutableList() {
        catalogManagementService.setProductImages(laptop.getId(), List.of("https://example.com/laptop.png"));
        assertThrows(UnsupportedOperationException.class, () -> laptop.getImageUrls().add("https://example.com/other.png"));
        assertThrows(IllegalArgumentException.class, () -> catalogManagementService.setProductImages(laptop.getId(), List.of("javascript:alert(1)")));
        assertEquals(List.of("https://example.com/laptop.png"), laptop.getImageUrls());
    }

    @Test
    void productDetailsIncludeImageUrls() {
        catalogManagementService.setProductImages(laptop.getId(), List.of("https://example.com/laptop.png"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        catalogMenu(io(laptop.getId() + "\n", output)).handleViewProductDetails();
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("https://example.com/laptop.png"));
    }

    @Test
    void searchAndReviewLengthLimitsAcceptTheirBoundaries() {
        assertDoesNotThrow(() -> catalogQueryService.searchProducts("x".repeat(100)));
        assertThrows(InvalidInputException.class, () -> catalogQueryService.searchProducts("x".repeat(101)));
        Order order = checkout(alice);
        paymentService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        reviewService.addReview(alice.getId(), alice.getFullName(), laptop.getId(), 5, "x".repeat(2000));
        assertThrows(InvalidInputException.class, () -> reviewService.addReview(alice.getId(), alice.getFullName(), laptop.getId(), 5, "x".repeat(2001)));
        assertEquals(1, reviewService.getProductReviews(laptop.getId()).size());
    }

    @Test
    void addressEditingPreservesItsIdAndUpdatesTheOwnedAddress() {
        Address current = alice.getDefaultAddress().orElseThrow();
        Address updated = new Address(current.addressId(), current.recipientName(), current.phone(),
                current.province(), current.city(), "Updated Street", current.postalCode(), current.title(), true);
        profileService.updateAddress(alice.getId(), updated);
        assertEquals(2, alice.getAddresses().size());
        assertEquals("Updated Street", alice.getDefaultAddress().orElseThrow().streetAddress());
        assertThrows(InvalidInputException.class, () -> profileService.updateAddress(bob.getId(), updated));
    }

    @Test
    void addressEditingIsReachableFromTheCustomerMenu() {
        String script = "5\naddr-home-1\n\n\n\n\nNew Street\n\n\ny\n0\n";
        customerMenu(io(script, new ByteArrayOutputStream())).handleProfileManagement(alice);
        assertEquals("New Street", alice.getDefaultAddress().orElseThrow().streetAddress());
    }

    @Test
    void invalidAddressDataIsReportedInsteadOfCrashing() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String script = "2\nAlice\n09121112233\nTehran\nTehran\nStreet\ninvalid\nHome\nn\n0\n";
        assertDoesNotThrow(() -> customerMenu(io(script, output)).handleProfileManagement(alice));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Failed to add address"));
        assertEquals(2, alice.getAddresses().size());
    }

    @Test
    void adminCanListAllOrdersAndFilterByStatus() {
        Order pending = checkout(alice);
        Order paid = checkout(bob);
        paymentService.processPayment(paid.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        assertEquals(2, fulfillmentService.getAllOrders().size());
        assertEquals(List.of(pending), fulfillmentService.getOrdersByStatus(OrderStatus.PENDING));
        assertEquals(List.of(paid), fulfillmentService.getOrdersByStatus(OrderStatus.PAID));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        adminMenu(io("PAID\n", output)).handleListOrders();
        assertTrue(output.toString(StandardCharsets.UTF_8).contains(paid.getOrderId()));
        assertFalse(output.toString(StandardCharsets.UTF_8).contains(pending.getOrderId()));
    }

    @Test
    void invalidAdminStatusFilterIsHandled() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> adminMenu(io("unknown\n", output)).handleListOrders());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Unknown order status"));
    }

    @Test
    void adminInventoryIncludesInactiveProducts() {
        catalogManagementService.deactivateProduct(laptop.getId());
        assertTrue(catalogManagementService.getAllProducts().contains(laptop));
        assertFalse(catalogQueryService.getAllActiveProducts().contains(laptop));
    }

    @Test
    void nonAdminCannotEnterTheAdminMenu() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        adminMenu(io("0\n", output)).showMenu(bob);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Administrator access required"));
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("Administration & Warehouse Panel"));
    }

    @Test
    void removingAMissingCartProductIsRejected() {
        assertThrows(InvalidInputException.class, () -> cartService.removeItem(alice.getId(), laptop.getId()));
    }
}
