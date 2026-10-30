package org.example;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.cli.ConsoleIO;
import org.example.cli.MainMenuController;
import org.example.cli.menu.AdminMenu;
import org.example.cli.menu.CustomerMenu;
import org.example.cli.menu.GuestMenu;
import org.example.model.entity.Cart;
import org.example.model.entity.Category;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.vo.Coupon;
import org.example.model.vo.Review;
import org.example.service.AuthService;
import org.example.service.CartService;
import org.example.service.OrderService;
import org.example.service.ProductService;
import org.example.service.ReviewService;
import org.example.service.UserService;
import org.example.util.DataSeeder;

/**
 * Application bootstrap and entry point for the Object-Oriented Online Shop.
 *
 * <p>Fulfills the three core startup responsibilities defined in Section 7 of the
 * authoritative domain model specification:
 * <ol>
 *   <li><b>Manual Dependency Injection:</b> Centralized instantiation and wiring of shared
 *       thread-safe in-memory stores and coordinating domain services.</li>
 *   <li><b>Initial Sample Data Seeding:</b> Automated population of default administrative
 *       and customer accounts, structured product catalog categories, products, and discount coupons.</li>
 *   <li><b>Interactive Terminal Bootstrap:</b> Handover of control to {@link MainMenuController}
 *       to start the role-routed CLI presentation loop.</li>
 * </ol>
 */
public class Main {

    /**
     * Main executable entry point.
     *
     * @param args runtime command-line arguments (not required)
     */
    public static void main(String[] args) {
        // 1. Initialize Shared In-Memory Concurrent Stores
        Map<String, User> userStore = new ConcurrentHashMap<>();
        Map<String, String> credentialsStore = new ConcurrentHashMap<>();
        Map<String, String> activeSessions = new ConcurrentHashMap<>();
        Map<String, Category> categoryStore = new ConcurrentHashMap<>();
        Map<String, Product> productStore = new ConcurrentHashMap<>();
        Map<String, Cart> cartStore = new ConcurrentHashMap<>();
        Map<String, Order> orderStore = new ConcurrentHashMap<>();
        Map<String, Coupon> couponStore = new ConcurrentHashMap<>();
        Map<String, List<Review>> reviewStore = new ConcurrentHashMap<>();

        // 2. Instantiate and Wire Domain Services (Manual Dependency Injection)
        AuthService authService = new AuthService(userStore, credentialsStore, activeSessions);
        UserService userService = new UserService(userStore);
        ProductService productService = new ProductService(productStore, categoryStore);
        CartService cartService = new CartService(cartStore, productService);
        OrderService orderService = new OrderService(orderStore, couponStore, cartService, productService, userService);
        ReviewService reviewService = new ReviewService(reviewStore, orderService, productService);

        // 3. Seed Initial Sample Data (Categories, Products, Users, Coupons)
        DataSeeder.seed(userStore, credentialsStore, categoryStore, productService, couponStore);

        // 4. Initialize CLI Presentation Components
        ConsoleIO io = new ConsoleIO();
        GuestMenu guestMenu = new GuestMenu(authService, productService, reviewService, io);
        CustomerMenu customerMenu = new CustomerMenu(userService, cartService, orderService, productService, reviewService, io);
        AdminMenu adminMenu = new AdminMenu(productService, orderService, io);

        MainMenuController controller = new MainMenuController(guestMenu, customerMenu, adminMenu, authService, io);

        // 5. Start the Application Interaction Loop
        controller.start();
    }
}
