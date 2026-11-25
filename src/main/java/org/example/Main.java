package org.example;

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

/**
 * Sole Composition Root assembling infrastructure adapters, Open-Closed registries,
 * cohesive application services, deterministic seed data, and polymorphic CLI menus (DIP).
 */
public class Main {

    public static void main(String[] args) {
        Clock clock = Clock.systemDefaultZone();
        var idGenerator = new UuidIdGenerator();
        var passwordHasher = new Sha256PasswordHasher();
        var verificationCodeGenerator = new RandomVerificationCodeGenerator();

        // Outbound in-memory repository adapters
        var userRepository = new InMemoryUserRepository();
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

        // Open-Closed strategy registries and pricing policies
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
        var discountPolicy = new CouponDiscountPolicy(couponRepository);
        var shippingPolicy = new FreeShippingPolicy();

        // Cohesive application services wired strictly to ports
        var authService = new AuthenticationService(
                userRepository, credentialRepository, sessionRepository, passwordHasher, idGenerator, clock);
        var recoveryService = new PasswordRecoveryService(
                userRepository, credentialRepository, sessionRepository, passwordResetRepository,
                passwordHasher, verificationCodeGenerator, clock);
        var profileService = new UserProfileService(userRepository);
        var catalogQueryService = new CatalogQueryService(productRepository, categoryRepository, sortRegistry);
        var catalogManagementService = new CatalogManagementService(
                productRepository, categoryRepository, idGenerator, clock);
        var cartService = new CartService(cartRepository, catalogQueryService);
        var pricingService = new OrderPricingService(discountPolicy, shippingPolicy, clock);
        var checkoutService = new OrderCheckoutService(
                orderRepository, snapshotRepository, cartService, profileService,
                catalogQueryService, catalogManagementService, pricingService, idGenerator, clock);
        var paymentService = new OrderPaymentService(
                orderRepository, snapshotRepository, cartService,
                catalogQueryService, catalogManagementService, paymentGatewayRegistry, idGenerator, clock);
        var fulfillmentService = new OrderFulfillmentService(
                orderRepository, snapshotRepository, catalogManagementService);
        var reviewService = new ReviewService(
                reviewRepository, catalogQueryService, checkoutService, idGenerator, clock);

        // Initial storefront seed data
        new ApplicationDataSeeder(
                userRepository, credentialRepository, categoryRepository,
                catalogManagementService, couponRepository, passwordHasher, clock
        ).seed();

        // Segregated terminal I/O, shared views, role handlers, and polymorphic menus
        ConsoleIO io = new ConsoleIO();
        ProductTablePrinter tablePrinter = new ProductTablePrinter(io);
        CatalogMenu catalogMenu = new CatalogMenu(catalogQueryService, reviewService, tablePrinter, io);

        CartMenuHandler cartHandler = new CartMenuHandler(cartService, catalogQueryService, tablePrinter, io, io);
        ProfileMenuHandler profileHandler = new ProfileMenuHandler(profileService, profileService, idGenerator, io, io);
        CheckoutMenuHandler checkoutHandler = new CheckoutMenuHandler(
                cartService, checkoutService, paymentService, profileHandler, io, io);
        OrderHistoryMenuHandler orderHistoryHandler = new OrderHistoryMenuHandler(
                checkoutService, paymentService, io, io);
        ReviewMenuHandler reviewHandler = new ReviewMenuHandler(reviewService, io, io);

        AdminCatalogHandler adminCatalogHandler = new AdminCatalogHandler(
                catalogManagementService, catalogQueryService, tablePrinter, io, io);
        AdminOrderHandler adminOrderHandler = new AdminOrderHandler(fulfillmentService, io, io);

        GuestMenu guestMenu = new GuestMenu(authService, recoveryService, catalogMenu, io, io);
        CustomerMenu customerMenu = new CustomerMenu(
                catalogMenu, cartHandler, checkoutHandler, orderHistoryHandler, reviewHandler, profileHandler, io, io);
        AdminMenu adminMenu = new AdminMenu(adminCatalogHandler, adminOrderHandler, io, io);

        MainMenuController controller = new MainMenuController(
                guestMenu, List.of(customerMenu, adminMenu), authService, io);
        controller.start();
    }
}
