package org.example.cli.menu;

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
import org.example.model.vo.Category;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.service.CartService;
import org.example.service.OrderService;
import org.example.service.ProductService;
import org.example.service.ReviewService;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CustomerMenu Tests")
class CustomerMenuTest {

    private Map<String, User> userStore;
    private Map<String, Category> categoryStore;
    private Map<String, Order> orderStore;
    private Map<String, Coupon> couponStore;

    private UserService userService;
    private ProductService productService;
    private CartService cartService;
    private OrderService orderService;
    private ReviewService reviewService;

    private User customer;
    private Product laptop;
    private Product mouse;
    private Address defaultAddress;

    @BeforeEach
    void setUp() {
        userStore = new ConcurrentHashMap<>();
        categoryStore = new ConcurrentHashMap<>();
        orderStore = new ConcurrentHashMap<>();
        couponStore = new ConcurrentHashMap<>();

        userService = new UserService(userStore);
        productService = new ProductService(new ConcurrentHashMap<>(), categoryStore);
        cartService = new CartService(productService);
        orderService = new OrderService(orderStore, couponStore, cartService, productService, userService);
        reviewService = new ReviewService(orderService, productService);

        // Seed Category & Products
        Category tech = new Category("CAT-TECH", "Technology", "Tech equipment");
        categoryStore.put(tech.id(), tech);

        Money laptopPrice = new Money(new BigDecimal("10000000.00"), "IRR");
        Money mousePrice = new Money(new BigDecimal("200000.00"), "IRR");
        productService.addProduct("Laptop", "Developer laptop", laptopPrice, 5, "CAT-TECH");
        productService.addProduct("Mouse", "Optical mouse", mousePrice, 20, "CAT-TECH");

        laptop = productService.searchProducts("Laptop").getFirst();
        mouse = productService.searchProducts("Mouse").getFirst();

        // Seed User & Address
        customer = new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", "09121112233", "passHash", UserRole.CUSTOMER, LocalDateTime.now());
        userStore.put(customer.getId(), customer);

        defaultAddress = new Address("addr-1", "Ali Rezaei", "09121112233", "Tehran", "Tehran", "Hafez St", "1234567890", "Home", true);
        userService.addAddress(customer.getId(), defaultAddress);

        // Seed Coupon
        Coupon coupon = new Coupon("SAVE10", 10, new Money(new BigDecimal("2000000.00"), "IRR"), new Money(new BigDecimal("500000.00"), "IRR"), java.time.LocalDate.now().plusDays(5));
        couponStore.put(coupon.code(), coupon);
    }

    private ConsoleIO createConsoleIO(String input, ByteArrayOutputStream out) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(out, true, StandardCharsets.UTF_8);
        return new ConsoleIO(in, ps);
    }

    @Nested
    @DisplayName("Catalog Browsing & Direct Add to Cart")
    class CatalogBrowsingTests {

        @Test
        @DisplayName("handleBrowseAndAddToCart displays catalog and adds item to cart")
        void browseAndAddSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // y -> Product ID -> Quantity
            ConsoleIO io = createConsoleIO("y\n" + laptop.getId() + "\n2\n", out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

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
            ConsoleIO io = createConsoleIO("n\n", out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

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
            // 1 (Add) -> mouse ID -> 3
            // 2 (Update) -> mouse ID -> 5
            // 3 (Remove) -> mouse ID
            // 0 (Back)
            String script = "1\n" + mouse.getId() + "\n3\n"
                    + "2\n" + mouse.getId() + "\n5\n"
                    + "3\n" + mouse.getId() + "\n"
                    + "0\n";
            ConsoleIO io = createConsoleIO(script, out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

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
            // 4 (Clear) -> y -> 0 (Back)
            ConsoleIO io = createConsoleIO("4\ny\n0\n", out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

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
            ConsoleIO io = createConsoleIO("", out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

            menu.handleCheckout(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Your shopping cart is empty."));
        }

        @Test
        @DisplayName("handleCheckout with valid cart, coupon, and payment succeeds")
        void checkoutAndPaySuccess() {
            cartService.addItem(customer.getId(), laptop.getId(), new Quantity(1));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Address choice: 0 (default)
            // Coupon: SAVE10
            // Pay now: y
            // Method: 1 (ONLINE_GATEWAY)
            // Simulate success: y
            String script = "0\nSAVE10\ny\n1\ny\n";
            ConsoleIO io = createConsoleIO(script, out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

            menu.handleCheckout(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Order Summary - Order #"));
            assertTrue(output.contains("Discount:       -1000000.00 IRR"));
            assertTrue(output.contains("PAYABLE AMOUNT:  9000000.00 IRR"));
            assertTrue(output.contains("Payment authorization accepted!"));
            assertTrue(output.contains("status is now PAID"));

            // Verify stock deducted & cart cleared
            assertEquals(4, laptop.getAvailableStock()); // 5 - 1
            assertTrue(cartService.getCart(customer.getId()).isEmpty());
        }

        @Test
        @DisplayName("handleCheckout with failed payment keeps cart intact")
        void checkoutPaymentFailureKeepsCart() {
            cartService.addItem(customer.getId(), mouse.getId(), new Quantity(2));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Address: 0 -> Coupon: (blank) -> Pay now: y -> Method: 1 -> Simulate success: n
            String script = "0\n\ny\n1\nn\n";
            ConsoleIO io = createConsoleIO(script, out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

            menu.handleCheckout(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Payment simulation failed or was rejected."));
            assertTrue(output.contains("Your cart items have been preserved"));

            // Cart still has the items
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
            // Seed a paid order
            OrderItem item = new OrderItem(laptop.getId(), laptop.getName(), laptop.getPrice(), new Quantity(1), laptop.getPrice());
            Order order = new Order("ord-99", customer.getId(), List.of(item), defaultAddress,
                    laptop.getPrice(), Money.ZERO, Money.ZERO, laptop.getPrice(), LocalDateTime.now());
            order.markAsPaid(LocalDateTime.now());
            orderStore.put(order.getOrderId(), order);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // 1 (View Details) -> ord-99 -> 0 (Back)
            String script = "1\nord-99\n0\n";
            ConsoleIO io = createConsoleIO(script, out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

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
            // Seed order
            OrderItem item = new OrderItem(laptop.getId(), laptop.getName(), laptop.getPrice(), new Quantity(1), laptop.getPrice());
            Order order = new Order("ord-rev-ok", customer.getId(), List.of(item), defaultAddress,
                    laptop.getPrice(), Money.ZERO, Money.ZERO, laptop.getPrice(), LocalDateTime.now());
            order.markAsPaid(LocalDateTime.now());
            orderStore.put(order.getOrderId(), order);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Product ID -> Rating -> Comment
            String script = laptop.getId() + "\n5\nFantastic laptop, blazing fast!\n";
            ConsoleIO io = createConsoleIO(script, out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

            menu.handleSubmitReview(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Your verified review and 5-star rating have been recorded."));
            assertEquals(1, reviewService.getProductReviews(laptop.getId()).size());
        }

        @Test
        @DisplayName("handleSubmitReview fails when product was never purchased")
        void submitReviewUnpurchasedItemFails() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Product ID -> Rating -> Comment
            String script = mouse.getId() + "\n4\nNice mouse\n";
            ConsoleIO io = createConsoleIO(script, out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

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
            // 1 (Edit Profile) -> New Name -> New Phone -> 0 (Back)
            String script = "1\nAli R. Mohammadi\n09129990011\n0\n";
            ConsoleIO io = createConsoleIO(script, out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

            menu.handleProfileManagement(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Profile updated successfully."));
            assertEquals("Ali R. Mohammadi", userService.getProfile(customer.getId()).getFullName());
            assertEquals("09129990011", userService.getProfile(customer.getId()).getPhone());
        }

        @Test
        @DisplayName("handleProfileManagement adds delivery address")
        void addDeliveryAddressSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // 2 (Add Address) -> Recipient -> Phone -> Province -> City -> Street -> Postal -> Title -> isDefault -> 0 (Back)
            String script = "2\nAli Work\n09121112233\nTehran\nTehran\nValiasr St\n9876543210\nOffice\nn\n0\n";
            ConsoleIO io = createConsoleIO(script, out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

            menu.handleProfileManagement(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Address 'Office' added successfully."));
            assertEquals(2, userService.getProfile(customer.getId()).getAddresses().size());
        }
    }

    @Nested
    @DisplayName("Menu Loop Control")
    class MenuLoopControlTests {

        @Test
        @DisplayName("showMenu option 0 exits loop and logs out")
        void exitLogsOut() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("0\n", out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

            menu.showMenu(customer);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Logging out of customer session. Goodbye!"));
        }

        @Test
        @DisplayName("showMenu handles EOF gracefully")
        void eofExitsGracefully() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("", out);
            CustomerMenu menu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);

            menu.showMenu(customer);
            // Completed without throwing unhandled exception
        }
    }
}
