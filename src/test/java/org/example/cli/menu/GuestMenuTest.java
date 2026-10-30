package org.example.cli.menu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.example.cli.ConsoleIO;
import org.example.model.vo.Category;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.service.AuthService;
import org.example.service.OrderService;
import org.example.service.ProductService;
import org.example.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GuestMenu Tests")
class GuestMenuTest {

    private java.util.Map<String, Category> categoryStore;
    private java.util.Map<String, Order> orderStore;
    private AuthService authService;
    private ProductService productService;
    private ReviewService reviewService;
    private OrderService orderService;
    private Product laptop;
    private Product mouse;

    @BeforeEach
    void setUp() {
        categoryStore = new java.util.concurrent.ConcurrentHashMap<>();
        orderStore = new java.util.concurrent.ConcurrentHashMap<>();
        authService = new AuthService();
        productService = new ProductService(new java.util.concurrent.ConcurrentHashMap<>(), categoryStore);
        orderService = new OrderService(orderStore);
        reviewService = new ReviewService(orderService, productService);

        // Seed Category and Products
        Category electronics = new Category("CAT-ELEC", "Electronics", "Gadgets & Hardware");
        categoryStore.put(electronics.id(), electronics);

        Money laptopPrice = new Money(new BigDecimal("15000000.00"), "IRR");
        Money mousePrice = new Money(new BigDecimal("500000.00"), "IRR");

        productService.addProduct("Gaming Laptop", "High performance laptop", laptopPrice, 5, "CAT-ELEC");
        productService.addProduct("Wireless Mouse", "Ergonomic 2.4G mouse", mousePrice, 15, "CAT-ELEC");

        laptop = productService.searchProducts("Gaming Laptop").getFirst();
        mouse = productService.searchProducts("Wireless Mouse").getFirst();
    }

    private ConsoleIO createConsoleIO(String input, ByteArrayOutputStream out) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(out, true, StandardCharsets.UTF_8);
        return new ConsoleIO(in, ps);
    }

    @Nested
    @DisplayName("Product Exploration Tests")
    class ProductExplorationTests {

        @Test
        @DisplayName("handleBrowseProducts prints active products table")
        void browseProductsDisplaysTable() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            menu.handleBrowseProducts();

            String output = out.toString();
            assertTrue(output.contains("Active Products Catalog"));
            assertTrue(output.contains("Gaming Laptop"));
            assertTrue(output.contains("Wireless Mouse"));
            assertTrue(output.contains("Total products: 2"));
        }

        @Test
        @DisplayName("handleSearchProducts filters catalog by keyword")
        void searchProductsSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("Laptop\n", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            menu.handleSearchProducts();

            String output = out.toString();
            assertTrue(output.contains("Search Products"));
            assertTrue(output.contains("Gaming Laptop"));
            assertFalse(output.contains("Wireless Mouse"));
        }

        @Test
        @DisplayName("handleFilterAndSort applies criteria and sorting")
        void filterAndSortSuccess() {
            // Category: blank, Min: 1000000, Max: blank, Sort: 2 (price_asc)
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("\n1000000\n\n2\n", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            menu.handleFilterAndSort();

            String output = out.toString();
            assertTrue(output.contains("Gaming Laptop")); // 15,000,000 >= 1,000,000
            assertFalse(output.contains("Wireless Mouse")); // 500,000 < 1,000,000
        }

        @Test
        @DisplayName("handleViewProductDetails displays specs and customer reviews")
        void viewProductDetailsWithReviews() {
            // Seed a verified review
            User customer = authService.register("Tester", "test@aut.ac.ir", "09121112233", "pass123");
            Address addr = new Address("a1", "Tester", "09121112233", "Tehran", "Tehran", "St", "1234567890", "Home", true);
            OrderItem item = new OrderItem(laptop.getId(), laptop.getName(), laptop.getPrice(), new Quantity(1), laptop.getPrice());
            Order paidOrder = new Order("ord-rev", customer.getId(), List.of(item), addr,
                    laptop.getPrice(), Money.ZERO, Money.ZERO, laptop.getPrice(), LocalDateTime.now());
            paidOrder.markAsPaid(LocalDateTime.now());
            orderStore.put(paidOrder.getOrderId(), paidOrder);

            reviewService.addReview(customer.getId(), customer.getFullName(), laptop.getId(), 5, "Outstanding build quality!");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO(laptop.getId() + "\n", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

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
            ConsoleIO io = createConsoleIO("non-existent-id\n", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            menu.handleViewProductDetails();

            String output = out.toString();
            assertTrue(output.contains("[ERROR] Product not found"));
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("handleRegister creates customer account in AuthService")
        void registerCustomerSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // FullName, Email, Phone, Password
            ConsoleIO io = createConsoleIO("Sara Mohammadi\nsara@aut.ac.ir\n09123334455\nsecurePass123\n", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            menu.handleRegister();

            String output = out.toString();
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
            ConsoleIO io = createConsoleIO("Another User\nexisting@aut.ac.ir\n09121111111\npassword234\n", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            menu.handleRegister();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("[ERROR] Registration failed: Email is already registered: existing@aut.ac.ir"));
        }

        @Test
        @DisplayName("handleLogin with correct credentials returns authenticated user")
        void loginSuccess() {
            authService.register("Reza Rad", "reza@aut.ac.ir", "09129998877", "mypassword");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("reza@aut.ac.ir\nmypassword\n", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            User loggedIn = menu.handleLogin();

            assertNotNull(loggedIn);
            assertEquals("reza@aut.ac.ir", loggedIn.getEmail());
            assertEquals("Reza Rad", loggedIn.getFullName());
            assertTrue(out.toString().contains("[SUCCESS] Login successful!"));
        }

        @Test
        @DisplayName("handleLogin with invalid password returns null and reports error")
        void loginInvalidPasswordFails() {
            authService.register("Reza Rad", "reza2@aut.ac.ir", "09129998877", "correctPass");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("reza2@aut.ac.ir\nwrongPass\n", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            User loggedIn = menu.handleLogin();

            assertNull(loggedIn);
            assertTrue(out.toString().contains("[ERROR] Login failed: Invalid email or password"));
        }
    }

    @Nested
    @DisplayName("Menu Navigation and Loop Control")
    class MenuLoopTests {

        @Test
        @DisplayName("showMenu option 0 exits and returns null")
        void exitReturnsNull() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("0\n", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            User result = menu.showMenu();

            assertNull(result);
            assertTrue(out.toString().contains("Exiting application. Goodbye!"));
        }

        @Test
        @DisplayName("showMenu option 6 logs in and returns authenticated user")
        void loginBreaksLoopAndReturnsUser() {
            authService.register("Ali User", "ali@aut.ac.ir", "09121234567", "secretKey");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Option 6 -> Email -> Password
            ConsoleIO io = createConsoleIO("6\nali@aut.ac.ir\nsecretKey\n", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            User result = menu.showMenu();

            assertNotNull(result);
            assertEquals("ali@aut.ac.ir", result.getEmail());
        }

        @Test
        @DisplayName("showMenu handles EOF input gracefully without throwing exception")
        void eofReturnsNullGracefully() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("", out);
            GuestMenu menu = new GuestMenu(authService, productService, reviewService, io);

            User result = menu.showMenu();
            assertNull(result);
        }
    }
}
