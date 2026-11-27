package org.example.cli.menu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.cli.ConsoleIO;
import org.example.cli.view.ProductTablePrinter;
import org.example.infrastructure.catalog.DefaultNameSort;
import org.example.infrastructure.catalog.MapProductSortRegistry;
import org.example.infrastructure.catalog.NewestSort;
import org.example.infrastructure.catalog.PopularSort;
import org.example.infrastructure.catalog.PriceAscendingSort;
import org.example.infrastructure.catalog.PriceDescendingSort;
import org.example.infrastructure.catalog.RatingSort;
import org.example.infrastructure.persistence.InMemoryCategoryRepository;
import org.example.infrastructure.persistence.InMemoryCredentialRepository;
import org.example.infrastructure.persistence.InMemoryOrderRepository;
import org.example.infrastructure.persistence.InMemoryPasswordResetRepository;
import org.example.infrastructure.persistence.InMemoryProductRepository;
import org.example.infrastructure.persistence.InMemoryReviewRepository;
import org.example.infrastructure.persistence.InMemorySessionRepository;
import org.example.infrastructure.persistence.InMemoryUserRepository;
import org.example.infrastructure.security.RandomVerificationCodeGenerator;
import org.example.infrastructure.security.Sha256PasswordHasher;
import org.example.infrastructure.security.UuidIdGenerator;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.service.AuthenticationService;
import org.example.service.CatalogManagementService;
import org.example.service.CatalogQueryService;
import org.example.service.PasswordRecoveryService;
import org.example.service.ReviewService;
import org.example.service.api.PurchaseHistoryQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GuestMenu Tests")
class GuestMenuTest {

    private InMemoryOrderRepository orderRepository;
    private AuthenticationService authService;
    private PasswordRecoveryService recoveryService;
    private CatalogQueryService catalogQueryService;
    private ReviewService reviewService;
    private Product laptop;
    private Product mouse;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.systemDefaultZone();
        var idGenerator = new UuidIdGenerator();
        var passwordHasher = new Sha256PasswordHasher();
        var userRepository = new InMemoryUserRepository();
        var credentialRepository = new InMemoryCredentialRepository();
        var sessionRepository = new InMemorySessionRepository();
        var categoryRepository = new InMemoryCategoryRepository();
        var productRepository = new InMemoryProductRepository();
        orderRepository = new InMemoryOrderRepository();

        authService = new AuthenticationService(
                userRepository, credentialRepository, sessionRepository, passwordHasher, idGenerator, clock);
        recoveryService = new PasswordRecoveryService(
                userRepository, credentialRepository, sessionRepository, new InMemoryPasswordResetRepository(),
                passwordHasher, new RandomVerificationCodeGenerator(), clock);
        var sortRegistry = new MapProductSortRegistry(
                new DefaultNameSort(),
                new PriceAscendingSort(),
                new PriceDescendingSort(),
                new RatingSort(),
                new NewestSort(),
                new PopularSort()
        );
        catalogQueryService = new CatalogQueryService(productRepository, categoryRepository, sortRegistry);
        var catalogManagement = new CatalogManagementService(productRepository, categoryRepository, idGenerator, clock);
        PurchaseHistoryQuery purchaseHistoryQuery = new PurchaseHistoryQuery() {
            @Override
            public List<Order> getOrderHistory(String userId) {
                return orderRepository.findByUserId(userId);
            }

            @Override
            public boolean hasPurchasedProduct(String userId, String productId) {
                return orderRepository.findByUserId(userId).stream()
                        .filter(o -> o.getStatus() == OrderStatus.PAID
                                || o.getStatus() == OrderStatus.PROCESSING
                                || o.getStatus() == OrderStatus.SHIPPED
                                || o.getStatus() == OrderStatus.DELIVERED)
                        .flatMap(o -> o.getItems().stream())
                        .anyMatch(item -> item.productId().equals(productId));
            }
        };
        reviewService = new ReviewService(
                new InMemoryReviewRepository(), catalogQueryService, purchaseHistoryQuery, idGenerator, clock);

        Category electronics = new Category("CAT-ELEC", "Electronics", "Gadgets & Hardware");
        categoryRepository.save(electronics);

        Money laptopPrice = new Money(new BigDecimal("15000000.00"), "IRR");
        Money mousePrice = new Money(new BigDecimal("500000.00"), "IRR");

        catalogManagement.addProduct("Gaming Laptop", "High performance laptop", laptopPrice, 5, "CAT-ELEC");
        catalogManagement.addProduct("Wireless Mouse", "Ergonomic 2.4G mouse", mousePrice, 15, "CAT-ELEC");

        laptop = catalogQueryService.searchProducts("Gaming Laptop").getFirst();
        mouse = catalogQueryService.searchProducts("Wireless Mouse").getFirst();
    }

    private ConsoleIO createConsoleIO(String input, ByteArrayOutputStream out) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(out, true, StandardCharsets.UTF_8);
        return new ConsoleIO(in, ps);
    }

    private GuestMenu createGuestMenu(ConsoleIO io) {
        CatalogMenu catalogMenu = new CatalogMenu(catalogQueryService, reviewService, new ProductTablePrinter(io), io);
        return new GuestMenu(authService, recoveryService, catalogMenu, io, io);
    }

    @Nested
    @DisplayName("Product Exploration Tests")
    class ProductExplorationTests {

        @Test
        @DisplayName("handleBrowseProducts prints active products table")
        void browseProductsDisplaysTable() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("", out));

            menu.handleBrowseProducts();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Active Products Catalog"));
            assertTrue(output.contains("Gaming Laptop"));
            assertTrue(output.contains("Wireless Mouse"));
            assertTrue(output.contains("Total products: 2"));
        }

        @Test
        @DisplayName("handleSearchProducts filters catalog by keyword")
        void searchProductsSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("Laptop\n", out));

            menu.handleSearchProducts();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Search Products"));
            assertTrue(output.contains("Gaming Laptop"));
            assertFalse(output.contains("Wireless Mouse"));
        }

        @Test
        @DisplayName("handleFilterAndSort applies criteria and sorting")
        void filterAndSortSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("\n1000000\n\n2\n", out));

            menu.handleFilterAndSort();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Gaming Laptop"));
            assertFalse(output.contains("Wireless Mouse"));
        }

        @Test
        @DisplayName("handleViewProductDetails displays specs and customer reviews")
        void viewProductDetailsWithReviews() {
            User customer = authService.register("Tester", "test@aut.ac.ir", "09121112233", "pass123");
            Address addr = new Address("a1", "Tester", "09121112233", "Tehran", "Tehran", "St", "1234567890", "Home", true);
            OrderItem item = new OrderItem(laptop.getId(), laptop.getName(), laptop.getPrice(), new Quantity(1), laptop.getPrice());
            Order paidOrder = new Order("ord-rev", customer.getId(), List.of(item), addr,
                    laptop.getPrice(), Money.ZERO, Money.ZERO, laptop.getPrice(), LocalDateTime.now());
            paidOrder.markAsPaid(LocalDateTime.now());
            orderRepository.save(paidOrder);

            reviewService.addReview(customer.getId(), customer.getFullName(), laptop.getId(), 5, "Outstanding build quality!");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO(laptop.getId() + "\n", out));

            menu.handleViewProductDetails();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Name:           " + laptop.getName()));
            assertTrue(output.contains("15000000.00 IRR"));
            assertTrue(output.contains("Outstanding build quality!"));
            assertTrue(output.contains("(5/5)"));
        }

        @Test
        @DisplayName("handleViewProductDetails with non-existent ID reports error")
        void viewProductDetailsNotFound() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("non-existent-id\n", out));

            menu.handleViewProductDetails();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("[ERROR] Product not found"));
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("handleRegister creates customer account in AuthUseCase")
        void registerCustomerSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("Sara Mohammadi\nsara@aut.ac.ir\n09123334455\nsecurePass123\n", out));

            menu.handleRegister();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("[SUCCESS] Registration successful!"));

            String token = authService.login("sara@aut.ac.ir", "securePass123");
            User created = authService.getAuthenticatedUser(token);
            assertNotNull(created);
            assertEquals("Sara Mohammadi", created.getFullName());
            assertEquals(UserRole.CUSTOMER, created.getRole());
        }

        @Test
        @DisplayName("handleRegister with duplicate email prints error")
        void registerDuplicateEmailFails() {
            authService.register("Existing User", "existing@aut.ac.ir", "09120000000", "password123");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("Another User\nexisting@aut.ac.ir\n09121111111\npassword234\n", out));

            menu.handleRegister();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("[ERROR] Registration failed: Email is already registered: existing@aut.ac.ir"));
        }

        @Test
        @DisplayName("handleLogin with correct credentials returns authenticated session")
        void loginSuccess() {
            authService.register("Reza Rad", "reza@aut.ac.ir", "09129998877", "mypassword");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("reza@aut.ac.ir\nmypassword\n", out));

            Optional<AuthenticatedSession> session = menu.handleLogin();

            assertTrue(session.isPresent());
            assertEquals("reza@aut.ac.ir", session.get().user().getEmail());
            assertEquals("Reza Rad", session.get().user().getFullName());
            assertTrue(out.toString(StandardCharsets.UTF_8).contains("[SUCCESS] Login successful!"));
        }

        @Test
        @DisplayName("handleLogin with invalid password returns empty and reports error")
        void loginInvalidPasswordFails() {
            authService.register("Reza Rad", "reza2@aut.ac.ir", "09129998877", "correctPass");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("reza2@aut.ac.ir\nwrongPass\n", out));

            Optional<AuthenticatedSession> session = menu.handleLogin();

            assertTrue(session.isEmpty());
            assertTrue(out.toString(StandardCharsets.UTF_8).contains("[ERROR] Login failed: Invalid email or password"));
        }
    }

    @Nested
    @DisplayName("Menu Navigation and Loop Control")
    class MenuLoopTests {

        @Test
        @DisplayName("showMenu option 0 exits and returns empty session")
        void exitReturnsEmpty() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("0\n", out));

            Optional<AuthenticatedSession> result = menu.showMenu();

            assertTrue(result.isEmpty());
            assertTrue(out.toString(StandardCharsets.UTF_8).contains("Exiting application. Goodbye!"));
        }

        @Test
        @DisplayName("showMenu option 6 logs in and returns authenticated session")
        void loginBreaksLoopAndReturnsSession() {
            authService.register("Ali User", "ali@aut.ac.ir", "09121234567", "secretKey");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("6\nali@aut.ac.ir\nsecretKey\n", out));

            Optional<AuthenticatedSession> result = menu.showMenu();

            assertTrue(result.isPresent());
            assertEquals("ali@aut.ac.ir", result.get().user().getEmail());
            assertNotNull(authService.getAuthenticatedUser(result.get().sessionToken()));
        }

        @Test
        @DisplayName("showMenu handles EOF input gracefully without throwing exception")
        void eofReturnsEmptyGracefully() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GuestMenu menu = createGuestMenu(createConsoleIO("", out));

            Optional<AuthenticatedSession> result = menu.showMenu();
            assertTrue(result.isEmpty());
        }
    }
}
