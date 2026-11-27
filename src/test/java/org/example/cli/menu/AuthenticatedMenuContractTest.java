package org.example.cli.menu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.example.cli.ConsoleIO;
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
import org.example.infrastructure.persistence.InMemoryOrderRepository;
import org.example.infrastructure.persistence.InMemoryProductRepository;
import org.example.infrastructure.persistence.InMemoryReviewRepository;
import org.example.infrastructure.persistence.InMemoryUserRepository;
import org.example.infrastructure.pricing.CouponDiscountPolicy;
import org.example.infrastructure.pricing.FreeShippingPolicy;
import org.example.infrastructure.security.UuidIdGenerator;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.service.CartService;
import org.example.service.CatalogManagementService;
import org.example.service.CatalogQueryService;
import org.example.service.OrderCheckoutService;
import org.example.service.OrderFulfillmentService;
import org.example.service.OrderPaymentService;
import org.example.service.OrderPricingService;
import org.example.service.ReviewService;
import org.example.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AuthenticatedMenu LSP Contract Tests")
class AuthenticatedMenuContractTest {

    static Stream<AuthenticatedMenu> menuImplementations() {
        Clock clock = Clock.systemDefaultZone();
        var idGenerator = new UuidIdGenerator();
        var userRepository = new InMemoryUserRepository();
        var categoryRepository = new InMemoryCategoryRepository();
        var productRepository = new InMemoryProductRepository();
        var orderRepository = new InMemoryOrderRepository();
        var snapshotRepository = new InMemoryCheckoutSnapshotRepository();

        var profileService = new UserProfileService(userRepository);
        var catalogQuery = new CatalogQueryService(
                productRepository, categoryRepository, new MapProductSortRegistry(new DefaultNameSort()));
        var catalogMgmt = new CatalogManagementService(productRepository, categoryRepository, idGenerator, clock);
        var cartService = new CartService(new InMemoryCartRepository(), catalogQuery);
        var pricingService = new OrderPricingService(
                new CouponDiscountPolicy(new InMemoryCouponRepository()), new FreeShippingPolicy(), clock);
        var checkoutService = new OrderCheckoutService(
                orderRepository, snapshotRepository, cartService, profileService,
                catalogQuery, catalogMgmt, pricingService, idGenerator, clock);
        var paymentService = new OrderPaymentService(
                orderRepository, snapshotRepository, cartService, catalogQuery, catalogMgmt,
                new MapPaymentGatewayRegistry(new OnlinePaymentGateway(idGenerator)), idGenerator, clock);
        var fulfillmentService = new OrderFulfillmentService(orderRepository, snapshotRepository, catalogMgmt);
        var reviewService = new ReviewService(
                new InMemoryReviewRepository(), catalogQuery, checkoutService, idGenerator, clock);

        ConsoleIO io = new ConsoleIO(
                new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)
        );
        ProductTablePrinter printer = new ProductTablePrinter(io);
        CatalogMenu catalogMenu = new CatalogMenu(catalogQuery, reviewService, printer, io);
        ProfileMenuHandler profileHandler = new ProfileMenuHandler(profileService, profileService, idGenerator, io, io);

        CustomerMenu customerMenu = new CustomerMenu(
                catalogMenu,
                new CartMenuHandler(cartService, catalogQuery, printer, io, io),
                new CheckoutMenuHandler(cartService, checkoutService, paymentService, profileHandler, io, io),
                new OrderHistoryMenuHandler(checkoutService, paymentService, io, io),
                new ReviewMenuHandler(reviewService, io, io),
                profileHandler,
                io,
                io
        );
        AdminMenu adminMenu = new AdminMenu(
                new AdminCatalogHandler(catalogMgmt, catalogQuery, printer, io, io),
                new AdminOrderHandler(fulfillmentService, io, io),
                io,
                io
        );
        return Stream.of(customerMenu, adminMenu);
    }

    @ParameterizedTest
    @MethodSource("menuImplementations")
    @DisplayName("All AuthenticatedMenu implementations declare supportedRole and enforce consistent null/role invariants")
    void enforcesUniformLspContract(AuthenticatedMenu menu) {
        assertNotNull(menu.supportedRole());
        assertThrows(NullPointerException.class, () -> menu.showMenu(null));

        User wrongRoleUser = new User(
                "usr-wrong",
                "Wrong Role",
                "wrong@aut.ac.ir",
                "09121234567",
                "hash",
                UserRole.STOREKEEPER,
                LocalDateTime.now()
        );
        assertDoesNotThrow(() -> menu.showMenu(wrongRoleUser));
        assertTrue(menu.supportedRole() != UserRole.STOREKEEPER);
    }
}
