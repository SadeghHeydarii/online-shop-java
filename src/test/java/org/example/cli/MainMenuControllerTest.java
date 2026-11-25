package org.example.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.example.cli.menu.AdminMenu;
import org.example.cli.menu.AuthenticatedMenu;
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
import org.example.infrastructure.payment.MapPaymentGatewayRegistry;
import org.example.infrastructure.payment.OnlinePaymentGateway;
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
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MainMenuController Unit Tests")
class MainMenuControllerTest {

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
    private UuidIdGenerator idGenerator;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.systemDefaultZone();
        idGenerator = new UuidIdGenerator();
        var passwordHasher = new Sha256PasswordHasher();
        var userRepository = new InMemoryUserRepository();
        var credentialRepository = new InMemoryCredentialRepository();
        var sessionRepository = new InMemorySessionRepository();
        var productRepository = new InMemoryProductRepository();
        var categoryRepository = new InMemoryCategoryRepository();
        var orderRepository = new InMemoryOrderRepository();
        var snapshotRepository = new InMemoryCheckoutSnapshotRepository();

        authService = new AuthenticationService(
                userRepository, credentialRepository, sessionRepository, passwordHasher, idGenerator, clock);
        recoveryService = new PasswordRecoveryService(
                userRepository, credentialRepository, sessionRepository, new InMemoryPasswordResetRepository(),
                passwordHasher, new RandomVerificationCodeGenerator(), clock);
        profileService = new UserProfileService(userRepository);
        catalogQueryService = new CatalogQueryService(
                productRepository, categoryRepository, new MapProductSortRegistry(new DefaultNameSort()));
        catalogManagementService = new CatalogManagementService(
                productRepository, categoryRepository, idGenerator, clock);
        cartService = new CartService(new InMemoryCartRepository(), catalogQueryService);
        var pricingService = new OrderPricingService(
                new CouponDiscountPolicy(new InMemoryCouponRepository()), new FreeShippingPolicy(), clock);
        checkoutService = new OrderCheckoutService(
                orderRepository, snapshotRepository, cartService, profileService,
                catalogQueryService, catalogManagementService, pricingService, idGenerator, clock);
        paymentService = new OrderPaymentService(
                orderRepository, snapshotRepository, cartService,
                catalogQueryService, catalogManagementService,
                new MapPaymentGatewayRegistry(new OnlinePaymentGateway(idGenerator)), idGenerator, clock);
        fulfillmentService = new OrderFulfillmentService(orderRepository, snapshotRepository, catalogManagementService);
        reviewService = new ReviewService(
                new InMemoryReviewRepository(), catalogQueryService, checkoutService, idGenerator, clock);
    }

    private ConsoleIO createConsoleIO(String script, ByteArrayOutputStream out) {
        ByteArrayInputStream in = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        return new ConsoleIO(in, new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    private GuestMenu createGuestMenu(ConsoleIO io) {
        ProductTablePrinter printer = new ProductTablePrinter(io);
        CatalogMenu catalogMenu = new CatalogMenu(catalogQueryService, reviewService, printer, io);
        return new GuestMenu(authService, recoveryService, catalogMenu, io, io);
    }

    private List<AuthenticatedMenu> createRoleMenus(ConsoleIO io) {
        ProductTablePrinter printer = new ProductTablePrinter(io);
        CatalogMenu catalogMenu = new CatalogMenu(catalogQueryService, reviewService, printer, io);
        ProfileMenuHandler profileHandler = new ProfileMenuHandler(profileService, profileService, idGenerator, io, io);
        CustomerMenu customerMenu = new CustomerMenu(
                catalogMenu,
                new CartMenuHandler(cartService, catalogQueryService, printer, io, io),
                new CheckoutMenuHandler(cartService, checkoutService, paymentService, profileHandler, io, io),
                new OrderHistoryMenuHandler(checkoutService, paymentService, io, io),
                new ReviewMenuHandler(reviewService, io, io),
                profileHandler,
                io,
                io
        );
        AdminMenu adminMenu = new AdminMenu(
                new AdminCatalogHandler(catalogManagementService, catalogQueryService, printer, io, io),
                new AdminOrderHandler(fulfillmentService, io, io),
                io,
                io
        );
        return List.of(customerMenu, adminMenu);
    }

    @Test
    @DisplayName("Constructor enforces non-null and unique role-menu parameters")
    void constructorNullGuards() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO io = createConsoleIO("", out);
        GuestMenu guestMenu = createGuestMenu(io);
        List<AuthenticatedMenu> roleMenus = createRoleMenus(io);

        assertThrows(NullPointerException.class, () -> new MainMenuController(null, roleMenus, authService, io));
        assertThrows(NullPointerException.class, () -> new MainMenuController(guestMenu, null, authService, io));
        assertThrows(NullPointerException.class, () -> new MainMenuController(guestMenu, roleMenus, null, io));
        assertThrows(NullPointerException.class, () -> new MainMenuController(guestMenu, roleMenus, authService, null));
    }

    @Test
    @DisplayName("start immediately exits when guest chooses option 0")
    void guestImmediateExit() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO io = createConsoleIO("0\n", out);
        MainMenuController controller = new MainMenuController(createGuestMenu(io), createRoleMenus(io), authService, io);
        controller.start();

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("WELCOME TO AMIRKABIR ONLINE SHOP (AUT)"));
        assertTrue(output.contains("Thank you for using Amirkabir Online Shop. Goodbye!"));
        assertNull(controller.getCurrentUser());
    }

    @Test
    @DisplayName("start routes pre-authenticated admin user and handles logout")
    void adminRoutingAndLogout() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO io = createConsoleIO("0\n0\n", out);
        MainMenuController controller = new MainMenuController(createGuestMenu(io), createRoleMenus(io), authService, io);
        User admin = new User("adm-1", "Admin Boss", "boss@aut.ac.ir", "09121111111", "hash", UserRole.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(admin);

        controller.start();

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Administration & Warehouse Panel"));
        assertTrue(output.contains("Logging out of administrative session"));
        assertNull(controller.getCurrentUser());
    }

    @Test
    @DisplayName("start denies administrative access to the storekeeper role")
    void storekeeperRouting() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO io = createConsoleIO("0\n0\n", out);
        MainMenuController controller = new MainMenuController(createGuestMenu(io), createRoleMenus(io), authService, io);
        User keeper = new User("sk-1", "Store Keeper", "keeper@aut.ac.ir", "09122222222", "hash", UserRole.STOREKEEPER, LocalDateTime.now());
        controller.setCurrentUser(keeper);

        controller.start();

        String output = out.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains("Administration & Warehouse Panel"));
        assertTrue(output.contains("This role cannot access the shop menus"));
        assertNull(controller.getCurrentUser());
    }

    @Test
    @DisplayName("start dispatches Open-Closed custom AuthenticatedMenu implementation without changing controller")
    void customRoleMenuDispatch_OcpVerification() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO io = createConsoleIO("0\n", out);
        AuthenticatedMenu keeperMenu = new AuthenticatedMenu() {
            @Override
            public UserRole supportedRole() {
                return UserRole.STOREKEEPER;
            }

            @Override
            public void showMenu(User user) {
                io.println("Storekeeper Panel - " + user.getFullName());
            }
        };

        MainMenuController controller = new MainMenuController(createGuestMenu(io), List.of(keeperMenu), authService, io);
        User keeper = new User("sk-1", "Ali Keeper", "keeper@aut.ac.ir", "09122222222", "hash", UserRole.STOREKEEPER, LocalDateTime.now());
        controller.setCurrentUser(keeper);
        controller.start();

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Storekeeper Panel - Ali Keeper"));
        assertNull(controller.getCurrentUser());
    }
}
