package org.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.cli.ConsoleIO;
import org.example.cli.MainMenuController;
import org.example.cli.menu.AdminMenu;
import org.example.cli.menu.CatalogMenu;
import org.example.cli.menu.CustomerMenu;
import org.example.cli.menu.GuestMenu;
import org.example.exception.AuthenticationException;
import org.example.exception.CategoryNotFoundException;
import org.example.exception.DuplicateUserException;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.UnauthorizedActionException;
import org.example.model.entity.Cart;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.vo.Address;
import org.example.model.vo.Category;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SubmissionRegressionTest {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> credentials = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final Map<String, Category> categories = new ConcurrentHashMap<>();
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, Cart> carts = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, Coupon> coupons = new ConcurrentHashMap<>();
    private final Map<String, List<Review>> reviews = new ConcurrentHashMap<>();
    private final AuthService auth = new AuthService(users, credentials, sessions);
    private final UserService userService = new UserService(users);
    private final ProductService productService = new ProductService(products, categories);
    private final CartService cartService = new CartService(carts, productService);
    private final OrderService orderService = new OrderService(orders, coupons, cartService, productService, userService);
    private final ReviewService reviewService = new ReviewService(reviews, orderService, productService);
    private User alice;
    private User bob;
    private Product laptop;
    private Product keyboard;

    @BeforeEach
    void setUp() {
        DataSeeder.seed(users, credentials, categories, productService, coupons);
        alice = users.get("usr-customer");
        bob = auth.register("Bob", "bob@example.com", "09123334444", "Bobpass1");
        bob.addAddress(new Address("addr-bob", "Bob", "09123334444", "Tehran", "Tehran",
                "Bob Street", "1234567890", "Home", true));
        laptop = productService.searchProducts("ThinkPad").getFirst();
        keyboard = productService.searchProducts("Keyboard").getFirst();
    }

    private Order checkout(User user) {
        cartService.addItem(user.getId(), laptop.getId(), new Quantity(1));
        return orderService.checkout(user.getId(), null, null);
    }

    private ConsoleIO io(String input, ByteArrayOutputStream output) {
        return new ConsoleIO(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    private CustomerMenu customerMenu(ConsoleIO io) {
        return new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);
    }

    @Test
    void emailOnlyRegistrationAndProfileEditWork() {
        User user = auth.register("Carol", "carol@example.com", "", "Carolpass1");
        userService.updateProfile(user.getId(), "Carol Updated", "");
        assertEquals("Carol Updated", user.getFullName());
        assertEquals("", user.getPhone());
        assertNotNull(auth.login("CAROL@EXAMPLE.COM", "Carolpass1"));
    }

    @Test
    void mobileOnlyRegistrationAndLoginWork() {
        User user = auth.register("Carol", "", "09126667777", "Carolpass1");
        assertEquals("", user.getEmail());
        assertEquals(user, auth.getAuthenticatedUser(auth.login("09126667777", "Carolpass1")));
        assertThrows(InvalidInputException.class, () -> userService.updateProfile(user.getId(), "Carol", ""));
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
        assertThrows(DuplicateUserException.class, () -> userService.updateProfile(alice.getId(), "Alice", bob.getPhone()));
        assertEquals("09121112233", alice.getPhone());
    }

    @Test
    void passwordResetChangesCredentialsInvalidatesSessionsAndCannotBeReused() {
        String token = auth.login("customer@shop.com", "Pass@123");
        String code = auth.requestPasswordReset(alice.getPhone());
        assertThrows(AuthenticationException.class, () -> auth.resetPassword(alice.getPhone(), "invalid", "Newpass1"));
        auth.resetPassword(alice.getPhone(), code, "Newpass1");
        assertNull(auth.getAuthenticatedUser(token));
        assertThrows(AuthenticationException.class, () -> auth.login("customer@shop.com", "Pass@123"));
        assertNotNull(auth.login(alice.getPhone(), "Newpass1"));
        assertThrows(AuthenticationException.class, () -> auth.resetPassword(alice.getPhone(), code, "Otherpass1"));
    }

    @Test
    void recoveryRequiresAnExistingActiveAccount() {
        assertThrows(AuthenticationException.class, () -> auth.requestPasswordReset("missing@example.com"));
        alice.deactivate();
        assertThrows(AuthenticationException.class, () -> auth.requestPasswordReset(alice.getPhone()));
    }

    @Test
    void replacementRecoveryCodeInvalidatesThePreviousCode() {
        String oldCode = auth.requestPasswordReset(alice.getEmail());
        String newCode = auth.requestPasswordReset(alice.getEmail());
        if (!oldCode.equals(newCode)) {
            assertThrows(AuthenticationException.class, () -> auth.resetPassword(alice.getEmail(), oldCode, "Newpass1"));
        }
        auth.resetPassword(alice.getEmail(), newCode, "Newpass1");
        assertNotNull(auth.login(alice.getEmail(), "Newpass1"));
    }

    @Test
    void customerCannotReadPayOrCancelAnotherUsersOrder() {
        Order order = checkout(alice);
        int stock = laptop.getAvailableStock();
        assertThrows(UnauthorizedActionException.class, () -> orderService.getOrderDetails(bob.getId(), order.getOrderId()));
        assertThrows(UnauthorizedActionException.class, () -> orderService.processPayment(bob.getId(), order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertThrows(UnauthorizedActionException.class, () -> orderService.cancelOrder(bob.getId(), order.getOrderId()));
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
        assertEquals(order, orderService.getOrderDetails(alice.getId(), order.getOrderId()));
        assertTrue(orderService.processPayment(alice.getId(), order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertTrue(orderService.cancelOrder(alice.getId(), order.getOrderId()));
    }

    @Test
    void inactiveProductCannotBeCheckedOutFromAnExistingCart() {
        cartService.addItem(alice.getId(), laptop.getId(), new Quantity(1));
        productService.deactivateProduct(laptop.getId());
        assertThrows(InvalidInputException.class, () -> orderService.checkout(alice.getId(), null, null));
        assertTrue(orderService.getOrderHistory(alice.getId()).isEmpty());
    }

    @Test
    void productDeactivatedAfterCheckoutCannotBePaid() {
        Order order = checkout(alice);
        int stock = laptop.getAvailableStock();
        productService.deactivateProduct(laptop.getId());
        assertThrows(InvalidInputException.class, () -> orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertEquals(stock, laptop.getAvailableStock());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertTrue(order.getTransactions().isEmpty());
    }

    @Test
    void paymentDoesNotDeductAnyStockWhenOneOrderItemBecomesUnavailable() {
        cartService.addItem(alice.getId(), laptop.getId(), new Quantity(1));
        cartService.addItem(alice.getId(), keyboard.getId(), new Quantity(1));
        Order order = orderService.checkout(alice.getId(), null, null);
        int laptopStock = laptop.getAvailableStock();
        productService.deactivateProduct(keyboard.getId());
        assertThrows(InvalidInputException.class, () -> orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertEquals(laptopStock, laptop.getAvailableStock());
    }

    @Test
    void payingAnOldOrderPreservesUnrelatedLaterCartItems() {
        Order order = checkout(alice);
        cartService.addItem(alice.getId(), keyboard.getId(), new Quantity(2));
        orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        Cart remaining = cartService.getCart(alice.getId());
        assertEquals(0, remaining.quantityOf(laptop.getId()));
        assertEquals(2, remaining.quantityOf(keyboard.getId()));
    }

    @Test
    void payingAnOldOrderPreservesAReplacedCartLine() {
        Order order = checkout(alice);
        cartService.clearCart(alice.getId());
        cartService.addItem(alice.getId(), laptop.getId(), new Quantity(1));
        orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        assertEquals(1, cartService.getCart(alice.getId()).quantityOf(laptop.getId()));
    }

    @Test
    void payingAnOldOrderPreservesALineWhoseQuantityWasEdited() {
        Order order = checkout(alice);
        cartService.updateItemQuantity(alice.getId(), laptop.getId(), new Quantity(3));
        orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        assertEquals(3, cartService.getCart(alice.getId()).quantityOf(laptop.getId()));
    }

    @Test
    void repeatedFailedPaymentsRemainRetryableWithoutChangingStock() {
        Order order = checkout(alice);
        int stock = laptop.getAvailableStock();
        assertFalse(orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, false));
        assertFalse(orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, false));
        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        assertEquals(stock, laptop.getAvailableStock());
        assertEquals(2, order.getTransactions().size());
        assertTrue(orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertEquals(stock - 1, laptop.getAvailableStock());
        assertEquals(3, order.getTransactions().size());
    }

    @Test
    void cancellationThroughStatusUpdateRestocksExactlyOnce() {
        Order order = checkout(alice);
        int stock = laptop.getAvailableStock();
        orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CANCELLED);
        assertEquals(stock, laptop.getAvailableStock());
        assertThrows(InvalidOrderStateException.class, () -> orderService.cancelOrder(order.getOrderId()));
        assertEquals(stock, laptop.getAvailableStock());
    }

    @Test
    void negativeFilterPriceDisplaysAnErrorWithoutEscapingTheMenu() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GuestMenu menu = new GuestMenu(auth, productService, reviewService, io("\n-1\n\n1\n", output));
        assertDoesNotThrow(menu::handleFilterAndSort);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[ERROR]"));
    }

    @Test
    void invalidAdminTransitionDisplaysAnErrorWithoutChangingTheOrder() {
        Order order = checkout(alice);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AdminMenu menu = new AdminMenu(productService, orderService, io(order.getOrderId() + "\n2\n", output));
        assertDoesNotThrow(menu::handleProcessOrders);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[ERROR]"));
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void negativeAdminEditPriceDoesNotCrashOrChangeTheProduct() {
        Money oldPrice = laptop.getPrice();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AdminMenu menu = new AdminMenu(productService, orderService, io(laptop.getId() + "\n\n\n-1\n", output));
        assertDoesNotThrow(menu::handleEditProduct);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[ERROR]"));
        assertEquals(oldPrice, laptop.getPrice());
    }

    @Test
    void controllerLogoutRevokesTheActualLoginSession() {
        ConsoleIO console = io("customer@shop.com\nPass@123\n", new ByteArrayOutputStream());
        GuestMenu guest = new GuestMenu(auth, productService, reviewService, console);
        User loggedIn = guest.handleLogin();
        String token = guest.getSessionToken();
        MainMenuController controller = new MainMenuController(guest, customerMenu(console),
                new AdminMenu(productService, orderService, console), auth, console);
        controller.setCurrentUser(loggedIn);
        controller.handleLogout();
        assertNull(controller.getCurrentUser());
        assertNull(guest.getSessionToken());
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
        categories.put("CAT-HIDDEN", new Category("CAT-HIDDEN", "Hidden", "Hidden category", false));
        assertFalse(productService.getActiveCategories().stream().anyMatch(category -> category.id().equals("CAT-HIDDEN")));
        assertFalse(productService.getProductsByCategory("CAT-DIGITAL").isEmpty());
        assertThrows(CategoryNotFoundException.class, () -> productService.getProductsByCategory("CAT-HIDDEN"));
        assertThrows(CategoryNotFoundException.class, () -> productService.filterAndSort("missing", null, null, null));
        productService.deactivateProduct(laptop.getId());
        assertFalse(productService.getProductsByCategory("CAT-DIGITAL").contains(laptop));
    }

    @Test
    void newestSortingUsesProductCreationTime() {
        Product older = new Product("older", "Older", "Older product", Money.ZERO, 1, "CAT-BOOKS", LocalDateTime.of(2020, 1, 1, 0, 0));
        Product newer = new Product("newer", "Newer", "Newer product", Money.ZERO, 1, "CAT-BOOKS", LocalDateTime.of(2090, 1, 1, 0, 0));
        products.put(older.getId(), older);
        products.put(newer.getId(), newer);
        List<Product> sorted = productService.filterAndSort("CAT-BOOKS", null, null, "newest");
        assertEquals(newer, sorted.getFirst());
        assertEquals(older, sorted.getLast());
    }

    @Test
    void popularSortingUsesSuccessfulPaidUnits() {
        Order order = checkout(alice);
        orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        assertEquals(1, laptop.getSoldCount());
        assertEquals(laptop, productService.filterAndSort(null, null, null, "popular").getFirst());
        assertThrows(InvalidOrderStateException.class, () -> orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true));
        assertEquals(1, laptop.getSoldCount());
    }

    @Test
    void imageManagementValidatesUrlsAndReturnsAnImmutableList() {
        productService.setProductImages(laptop.getId(), List.of("https://example.com/laptop.png"));
        assertThrows(UnsupportedOperationException.class, () -> laptop.getImageUrls().add("https://example.com/other.png"));
        assertThrows(IllegalArgumentException.class, () -> productService.setProductImages(laptop.getId(), List.of("javascript:alert(1)")));
        assertEquals(List.of("https://example.com/laptop.png"), laptop.getImageUrls());
    }

    @Test
    void productDetailsIncludeImageUrls() {
        productService.setProductImages(laptop.getId(), List.of("https://example.com/laptop.png"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new CatalogMenu(productService, reviewService, io(laptop.getId() + "\n", output)).handleViewProductDetails();
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("https://example.com/laptop.png"));
    }

    @Test
    void searchAndReviewLengthLimitsAcceptTheirBoundaries() {
        assertDoesNotThrow(() -> productService.searchProducts("x".repeat(100)));
        assertThrows(InvalidInputException.class, () -> productService.searchProducts("x".repeat(101)));
        Order order = checkout(alice);
        orderService.processPayment(order.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        reviewService.addReview(alice.getId(), alice.getFullName(), laptop.getId(), 5, "x".repeat(2000));
        assertThrows(InvalidInputException.class, () -> reviewService.addReview(alice.getId(), alice.getFullName(), laptop.getId(), 5, "x".repeat(2001)));
        assertEquals(1, reviewService.getProductReviews(laptop.getId()).size());
    }

    @Test
    void addressEditingPreservesItsIdAndUpdatesTheOwnedAddress() {
        Address current = alice.getDefaultAddress().orElseThrow();
        Address updated = new Address(current.addressId(), current.recipientName(), current.phone(),
                current.province(), current.city(), "Updated Street", current.postalCode(), current.title(), true);
        userService.updateAddress(alice.getId(), updated);
        assertEquals(2, alice.getAddresses().size());
        assertEquals("Updated Street", alice.getDefaultAddress().orElseThrow().streetAddress());
        assertThrows(InvalidInputException.class, () -> userService.updateAddress(bob.getId(), updated));
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
        orderService.processPayment(paid.getOrderId(), PaymentMethod.ONLINE_GATEWAY, true);
        assertEquals(2, orderService.getAllOrders().size());
        assertEquals(List.of(pending), orderService.getOrdersByStatus(OrderStatus.PENDING));
        assertEquals(List.of(paid), orderService.getOrdersByStatus(OrderStatus.PAID));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new AdminMenu(productService, orderService, io("PAID\n", output)).handleListOrders();
        assertTrue(output.toString(StandardCharsets.UTF_8).contains(paid.getOrderId()));
        assertFalse(output.toString(StandardCharsets.UTF_8).contains(pending.getOrderId()));
    }

    @Test
    void invalidAdminStatusFilterIsHandled() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> new AdminMenu(productService, orderService, io("unknown\n", output)).handleListOrders());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Unknown order status"));
    }

    @Test
    void adminInventoryIncludesInactiveProducts() {
        productService.deactivateProduct(laptop.getId());
        assertTrue(productService.getAllProducts().contains(laptop));
        assertFalse(productService.getAllActiveProducts().contains(laptop));
    }

    @Test
    void nonAdminCannotEnterTheAdminMenu() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new AdminMenu(productService, orderService, io("0\n", output)).showMenu(bob);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Administrator access required"));
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("Administration & Warehouse Panel"));
    }

    @Test
    void removingAMissingCartProductIsRejected() {
        assertThrows(InvalidInputException.class, () -> cartService.removeItem(alice.getId(), laptop.getId()));
    }
}
