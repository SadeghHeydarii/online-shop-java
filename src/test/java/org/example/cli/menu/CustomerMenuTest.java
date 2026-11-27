package org.example.cli.menu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.cli.ConsoleIO;
import org.example.cli.menu.customer.CartMenuHandler;
import org.example.cli.menu.customer.CheckoutMenuHandler;
import org.example.cli.menu.customer.OrderHistoryMenuHandler;
import org.example.cli.menu.customer.ProfileMenuHandler;
import org.example.cli.menu.customer.ReviewMenuHandler;
import org.example.cli.view.ProductTablePrinter;
import org.example.infrastructure.catalog.DefaultNameSort;
import org.example.infrastructure.catalog.MapProductSortRegistry;
import org.example.infrastructure.payment.CashOnDeliveryPaymentGateway;
import org.example.infrastructure.payment.MapPaymentGatewayRegistry;
import org.example.infrastructure.payment.OnlinePaymentGateway;
import org.example.infrastructure.payment.WalletPaymentGateway;
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
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.model.vo.Category;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.service.CartService;
import org.example.service.CatalogManagementService;
import org.example.service.CatalogQueryService;
import org.example.service.OrderCheckoutService;
import org.example.service.OrderPaymentService;
import org.example.service.OrderPricingService;
import org.example.service.ReviewService;
import org.example.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CustomerMenu Tests")
class CustomerMenuTest {

    private InMemoryUserRepository userRepository;
    private InMemoryCategoryRepository categoryRepository;
    private InMemoryOrderRepository orderRepository;
    private InMemoryCouponRepository couponRepository;
    private UuidIdGenerator idGenerator;

    private UserProfileService profileService;
    private CatalogQueryService catalogQueryService;
    private CatalogManagementService catalogManagementService;
    private CartService cartService;
    private OrderCheckoutService checkoutService;
    private OrderPaymentService paymentService;
    private ReviewService reviewService;

    private User customer;
    private Product laptop;
    private Product mouse;
    private Address defaultAddress;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.systemDefaultZone();
        idGenerator = new UuidIdGenerator();
        userRepository = new InMemoryUserRepository();
        categoryRepository = new InMemoryCategoryRepository();
        orderRepository = new InMemoryOrderRepository();
        couponRepository = new InMemoryCouponRepository();
        var productRepository = new InMemoryProductRepository();
        var snapshotRepository = new InMemoryCheckoutSnapshotRepository();

        profileService = new UserProfileService(userRepository);
        catalogQueryService = new CatalogQueryService(
                productRepository, categoryRepository, new MapProductSortRegistry(new DefaultNameSort()));
        catalogManagementService = new CatalogManagementService(
                productRepository, categoryRepository, idGenerator, clock);
        cartService = new CartService(new InMemoryCartRepository(), catalogQueryService);
        var pricingService = new OrderPricingService(
                new CouponDiscountPolicy(couponRepository), new FreeShippingPolicy(), clock);
        checkoutService = new OrderCheckoutService(
                orderRepository, snapshotRepository, cartService, profileService,
                catalogQueryService, catalogManagementService, pricingService, idGenerator, clock);
        var gatewayRegistry = new MapPaymentGatewayRegistry(
                new OnlinePaymentGateway(idGenerator),
                new CashOnDeliveryPaymentGateway(idGenerator),
                new WalletPaymentGateway(idGenerator)
        );
        paymentService = new OrderPaymentService(
                orderRepository, snapshotRepository, cartService,
                catalogQueryService, catalogManagementService, gatewayRegistry, idGenerator, clock);
        reviewService = new ReviewService(
                new InMemoryReviewRepository(), catalogQueryService, checkoutService, idGenerator, clock);

        Category tech = new Category("CAT-TECH", "Technology", "Tech equipment");
        categoryRepository.save(tech);

        Money laptopPrice = new Money(new BigDecimal("10000000.00"), "IRR");
        Money mousePrice = new Money(new BigDecimal("200000.00"), "IRR");
        catalogManagementService.addProduct("Laptop", "Developer laptop", laptopPrice, 5, "CAT-TECH");
        catalogManagementService.addProduct("Mouse", "Optical mouse", mousePrice, 20, "CAT-TECH");

        laptop = catalogQueryService.searchProducts("Laptop").getFirst();
        mouse = catalogQueryService.searchProducts("Mouse").getFirst();

        customer = new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", "09121112233", "passHash", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(customer);

        defaultAddress = new Address("addr-1", "Ali Rezaei", "09121112233", "Tehran", "Tehran", "Hafez St", "1234567890", "Home", true);
        profileService.addAddress(customer.getId(), defaultAddress);

        Coupon coupon = new Coupon("SAVE10", 10, new Money(new BigDecimal("2000000.00"), "IRR"), new Money(new BigDecimal("500000.00"), "IRR"), LocalDate.now().plusDays(5));
        couponRepository.save(coupon);
    }

    private ConsoleIO createConsoleIO(String input, ByteArrayOutputStream out) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(out, true, StandardCharsets.UTF_8);
        return new ConsoleIO(in, ps);
    }

    private CustomerMenu createCustomerMenu(ConsoleIO io) {
        ProductTablePrinter printer = new ProductTablePrinter(io);
        CatalogMenu catalogMenu = new CatalogMenu(catalogQueryService, reviewService, printer, io);
        ProfileMenuHandler profileHandler = new ProfileMenuHandler(profileService, profileService, idGenerator, io, io);
        return new CustomerMenu(
                catalogMenu,
                new CartMenuHandler(cartService, catalogQueryService, printer, io, io),
                new CheckoutMenuHandler(cartService, checkoutService, paymentService, profileHandler, io, io),
                new OrderHistoryMenuHandler(checkoutService, paymentService, io, io),
                new ReviewMenuHandler(reviewService, io, io),
                profileHandler,
                io,
                io
        );
    }

    @Nested
    @DisplayName("Catalog Browsing & Direct Add to Cart")
    class BrowseAndAddTests {

        @Test
        @DisplayName("handleBrowseAndAddToCart adds selected product to cart when confirmed")
        void browseAndAddConfirmed() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO("y\n" + laptop.getId() + "\n2\n", out));

            menu.handleBrowseAndAddToCart(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Active Products Catalog"));
            assertTrue(output.contains("Laptop"));
            assertTrue(output.contains("Added 2 item(s) to your shopping cart!"));
            assertEquals(2, cartService.getCart(customer.getId()).quantityOf(laptop.getId()));
        }

        @Test
        @DisplayName("handleBrowseAndAddToCart declines adding item when answered 'n'")
        void browseAndDeclineAdd() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO("n\n", out));

            menu.handleBrowseAndAddToCart(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Active Products Catalog"));
            assertTrue(cartService.getCart(customer.getId()).isEmpty());
        }
    }

    @Nested
    @DisplayName("Shopping Cart Management")
    class CartManagementTests {

        @Test
        @DisplayName("handleCartManagement supports adding, updating, and removing items")
        void cartOperationsWorkflow() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String script = "1\n" + mouse.getId() + "\n3\n"
                    + "2\n" + mouse.getId() + "\n5\n"
                    + "3\n" + mouse.getId() + "\n"
                    + "0\n";
            CustomerMenu menu = createCustomerMenu(createConsoleIO(script, out));

            menu.handleCartManagement(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Product added to cart."));
            assertTrue(output.contains("Item quantity updated."));
            assertTrue(output.contains("Item removed from cart."));
            assertTrue(cartService.getCart(customer.getId()).isEmpty());
        }

        @Test
        @DisplayName("handleCartManagement clears cart when confirmed")
        void cartClearConfirmed() {
            cartService.addItem(customer.getId(), laptop.getId(), new Quantity(1));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO("4\ny\n0\n", out));

            menu.handleCartManagement(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Cart cleared successfully."));
            assertTrue(cartService.getCart(customer.getId()).isEmpty());
        }
    }

    @Nested
    @DisplayName("Checkout & Payment Orchestration")
    class CheckoutTests {

        @Test
        @DisplayName("handleCheckout with empty cart warns and aborts")
        void checkoutEmptyCartAborts() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO("", out));

            menu.handleCheckout(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Your shopping cart is empty."));
        }

        @Test
        @DisplayName("handleCheckout with valid cart, coupon, and payment succeeds")
        void checkoutAndPaySuccess() {
            cartService.addItem(customer.getId(), laptop.getId(), new Quantity(1));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO("0\nSAVE10\ny\n1\ny\n", out));

            menu.handleCheckout(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Order Summary - Order #"));
            assertTrue(output.contains("Discount:       -1000000.00 IRR"));
            assertTrue(output.contains("PAYABLE AMOUNT:  9000000.00 IRR"));
            assertTrue(output.contains("Payment authorization accepted!"));
            assertTrue(output.contains("status is now PAID"));

            assertEquals(4, laptop.getAvailableStock());
            assertTrue(cartService.getCart(customer.getId()).isEmpty());
        }

        @Test
        @DisplayName("handleCheckout with failed payment keeps cart intact")
        void checkoutPaymentFailureKeepsCart() {
            cartService.addItem(customer.getId(), mouse.getId(), new Quantity(2));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO("0\n\ny\n1\nn\n", out));

            menu.handleCheckout(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Payment simulation failed or was rejected."));
            assertTrue(output.contains("Your cart items have been preserved"));

            assertFalse(cartService.getCart(customer.getId()).isEmpty());
            assertEquals(20, mouse.getAvailableStock());
        }
    }

    @Nested
    @DisplayName("Order History & Reviews")
    class OrderHistoryAndReviewsTests {

        @Test
        @DisplayName("handleOrderHistory displays past orders and allows viewing details")
        void orderHistoryAndDetails() {
            OrderItem item = new OrderItem(laptop.getId(), laptop.getName(), laptop.getPrice(), new Quantity(1), laptop.getPrice());
            Order order = new Order("ord-99", customer.getId(), List.of(item), defaultAddress,
                    laptop.getPrice(), Money.ZERO, Money.ZERO, laptop.getPrice(), LocalDateTime.now());
            order.markAsPaid(LocalDateTime.now());
            orderRepository.save(order);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO("1\nord-99\n0\n", out));

            menu.handleOrderHistory(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("My Order History"));
            assertTrue(output.contains("ord-99"));
            assertTrue(output.contains("Details for Order #ord-99"));
            assertTrue(output.contains("Laptop"));
        }

        @Test
        @DisplayName("handleSubmitReview records review for purchased item")
        void submitReviewPurchasedItemSuccess() {
            OrderItem item = new OrderItem(laptop.getId(), laptop.getName(), laptop.getPrice(), new Quantity(1), laptop.getPrice());
            Order order = new Order("ord-rev-ok", customer.getId(), List.of(item), defaultAddress,
                    laptop.getPrice(), Money.ZERO, Money.ZERO, laptop.getPrice(), LocalDateTime.now());
            order.markAsPaid(LocalDateTime.now());
            orderRepository.save(order);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO(laptop.getId() + "\n5\nFantastic laptop, blazing fast!\n", out));

            menu.handleSubmitReview(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Your verified review and 5-star rating have been recorded."));
            assertEquals(1, reviewService.getProductReviews(laptop.getId()).size());
        }

        @Test
        @DisplayName("handleSubmitReview fails when product was never purchased")
        void submitReviewUnpurchasedItemFails() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO(mouse.getId() + "\n4\nNice mouse\n", out));

            menu.handleSubmitReview(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Review submission rejected:"));
        }
    }

    @Nested
    @DisplayName("Profile & Address Book Management")
    class ProfileManagementTests {

        @Test
        @DisplayName("handleProfileManagement updates name and phone")
        void editProfileSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO("1\nAli R. Mohammadi\n09129990011\n0\n", out));

            menu.handleProfileManagement(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Profile updated successfully."));
            assertEquals("Ali R. Mohammadi", profileService.getProfile(customer.getId()).getFullName());
            assertEquals("09129990011", profileService.getProfile(customer.getId()).getPhone());
        }

        @Test
        @DisplayName("handleProfileManagement adds delivery address")
        void addDeliveryAddressSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO(
                    "2\nAli Work\n09121112233\nTehran\nTehran\nValiasr St\n9876543210\nOffice\nn\n0\n", out));

            menu.handleProfileManagement(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Address 'Office' added successfully."));
            assertEquals(2, profileService.getProfile(customer.getId()).getAddresses().size());
        }
    }

    @Nested
    @DisplayName("Menu Loop Control")
    class MenuLoopControlTests {

        @Test
        @DisplayName("showMenu option 0 exits loop and logs out")
        void exitLogsOut() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO("0\n", out));

            menu.showMenu(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Logging out of customer session. Goodbye!"));
        }

        @Test
        @DisplayName("showMenu handles EOF gracefully")
        void eofExitsGracefully() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CustomerMenu menu = createCustomerMenu(createConsoleIO("", out));

            menu.showMenu(customer);
        }
    }
}
