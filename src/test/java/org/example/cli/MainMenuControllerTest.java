package org.example.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.cli.menu.AdminMenu;
import org.example.cli.menu.CustomerMenu;
import org.example.cli.menu.GuestMenu;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.service.AuthService;
import org.example.service.CartService;
import org.example.service.OrderService;
import org.example.service.ProductService;
import org.example.service.ReviewService;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MainMenuController Unit Tests")
class MainMenuControllerTest {

    private AuthService authService;
    private UserService userService;
    private ProductService productService;
    private CartService cartService;
    private OrderService orderService;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        Map<String, User> userStore = new ConcurrentHashMap<>();
        authService = new AuthService(userStore);
        userService = new UserService(userStore);
        productService = new ProductService();
        cartService = new CartService(productService);
        orderService = new OrderService();
        reviewService = new ReviewService(orderService, productService);
    }

    private ConsoleIO createConsoleIO(String script, ByteArrayOutputStream out) {
        ByteArrayInputStream in = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        return new ConsoleIO(in, new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Constructor enforces non-null parameters")
    void constructorNullGuards() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO io = createConsoleIO("", out);
        GuestMenu guestMenu = new GuestMenu(authService, productService, io);
        CustomerMenu customerMenu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);
        AdminMenu adminMenu = new AdminMenu(productService, orderService, io);

        assertThrows(NullPointerException.class, () -> new MainMenuController(null, customerMenu, adminMenu, io));
        assertThrows(NullPointerException.class, () -> new MainMenuController(guestMenu, null, adminMenu, io));
        assertThrows(NullPointerException.class, () -> new MainMenuController(guestMenu, customerMenu, null, io));
        assertThrows(NullPointerException.class, () -> new MainMenuController(guestMenu, customerMenu, adminMenu, null));
    }

    @Test
    @DisplayName("start immediately exits when guest chooses option 0")
    void guestImmediateExit() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 0 in Guest Menu -> Exit
        ConsoleIO io = createConsoleIO("0\n", out);

        GuestMenu guestMenu = new GuestMenu(authService, productService, io);
        CustomerMenu customerMenu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);
        AdminMenu adminMenu = new AdminMenu(productService, orderService, io);

        MainMenuController controller = new MainMenuController(guestMenu, customerMenu, adminMenu, io);
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
        // Admin menu option 0 (Logout) -> then Guest Menu option 0 (Exit)
        ConsoleIO io = createConsoleIO("0\n0\n", out);

        GuestMenu guestMenu = new GuestMenu(authService, productService, io);
        CustomerMenu customerMenu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);
        AdminMenu adminMenu = new AdminMenu(productService, orderService, io);

        MainMenuController controller = new MainMenuController(guestMenu, customerMenu, adminMenu, io);
        User admin = new User("adm-1", "Admin Boss", "boss@aut.ac.ir", "09121111111", "hash", UserRole.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(admin);

        controller.start();

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Administration & Warehouse Panel"));
        assertTrue(output.contains("Logging out of administrative session"));
        assertNull(controller.getCurrentUser());
    }

    @Test
    @DisplayName("start routes storekeeper role to admin menu")
    void storekeeperRouting() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO io = createConsoleIO("0\n0\n", out);

        GuestMenu guestMenu = new GuestMenu(authService, productService, io);
        CustomerMenu customerMenu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);
        AdminMenu adminMenu = new AdminMenu(productService, orderService, io);

        MainMenuController controller = new MainMenuController(guestMenu, customerMenu, adminMenu, io);
        User keeper = new User("sk-1", "Store Keeper", "keeper@aut.ac.ir", "09122222222", "hash", UserRole.STOREKEEPER, LocalDateTime.now());
        controller.setCurrentUser(keeper);

        controller.start();

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Administration & Warehouse Panel"));
        assertNull(controller.getCurrentUser());
    }
}
