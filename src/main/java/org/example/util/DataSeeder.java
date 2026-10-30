package org.example.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import org.example.model.entity.Category;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.service.ProductService;

/**
 * Utility class providing initial sample data seeding for the online shop system.
 *
 * <p>Populates baseline demo entities and value objects required by Section 7 of the
 * authoritative domain model specification:
 * <ul>
 *   <li>Default Administrator account: {@code admin@shop.com} / {@code Admin@123}.</li>
 *   <li>Default Customer account: {@code customer@shop.com} / {@code Pass@123} with pre-registered
 *       "Home" and "Work" delivery addresses.</li>
 *   <li>Catalog categories and at least five diverse products with valid prices and stock.</li>
 *   <li>Active promotional discount coupons (e.g., {@code WELCOME20} and {@code AUT_SPECIAL}).</li>
 * </ul>
 */
public final class DataSeeder {

    private DataSeeder() {
        // Utility class: prevent instantiation
    }

    /**
     * Seeds initial sample data into the provided storage maps and services.
     *
     * @param userStore        the backing storage map for user accounts
     * @param credentialsStore the backing storage map for password hashes
     * @param categoryStore    the backing storage map for product categories
     * @param productService   the product service managing product catalog creation
     * @param couponStore      the backing storage map for promotional coupons
     */
    public static void seed(Map<String, User> userStore,
                            Map<String, String> credentialsStore,
                            Map<String, Category> categoryStore,
                            ProductService productService,
                            Map<String, Coupon> couponStore) {
        Objects.requireNonNull(userStore, "userStore cannot be null");
        Objects.requireNonNull(credentialsStore, "credentialsStore cannot be null");
        Objects.requireNonNull(categoryStore, "categoryStore cannot be null");
        Objects.requireNonNull(productService, "productService cannot be null");
        Objects.requireNonNull(couponStore, "couponStore cannot be null");

        seedUsers(userStore, credentialsStore);
        seedCategories(categoryStore);
        seedProducts(productService);
        seedCoupons(couponStore);
    }

    private static void seedUsers(Map<String, User> userStore, Map<String, String> credentialsStore) {
        // 1. Seed Default Administrator Account
        String adminPasswordHash = PasswordUtil.hashPassword("Admin@123");
        User admin = new User(
                "usr-admin",
                "Administrator System",
                "admin@shop.com",
                "09120000000",
                adminPasswordHash,
                UserRole.ADMIN,
                LocalDateTime.now()
        );
        userStore.put(admin.getId(), admin);
        credentialsStore.put(admin.getId(), adminPasswordHash);

        // 2. Seed Default Customer Account with Address Book
        String customerPasswordHash = PasswordUtil.hashPassword("Pass@123");
        User customer = new User(
                "usr-customer",
                "Mohammad Rezaei",
                "customer@shop.com",
                "09121112233",
                customerPasswordHash,
                UserRole.CUSTOMER,
                LocalDateTime.now()
        );

        Address homeAddress = new Address(
                "addr-home-1",
                "Mohammad Rezaei",
                "09121112233",
                "Tehran",
                "Tehran",
                "Hafez St, No. 42, Apt 3",
                "1591634311",
                "Home",
                true
        );

        Address workAddress = new Address(
                "addr-work-1",
                "Mohammad Rezaei",
                "09121112233",
                "Tehran",
                "Tehran",
                "Amirkabir University of Technology, CE Dept",
                "1591634312",
                "Work",
                false
        );

        customer.addAddress(homeAddress);
        customer.addAddress(workAddress);

        userStore.put(customer.getId(), customer);
        credentialsStore.put(customer.getId(), customerPasswordHash);
    }

    private static void seedCategories(Map<String, Category> categoryStore) {
        Category digital = new Category(
                "CAT-DIGITAL",
                "Digital & Electronics",
                "Laptops, computer hardware, gadgets and technical accessories"
        );
        Category books = new Category(
                "CAT-BOOKS",
                "Books & Literature",
                "Software engineering, computer science and academic textbooks"
        );
        Category fashion = new Category(
                "CAT-FASHION",
                "Clothing & Apparel",
                "University apparel, hoodies, casual clothing and accessories"
        );
        Category home = new Category(
                "CAT-HOME",
                "Home & Office",
                "Ergonomic furniture, desk gear and workplace essentials"
        );

        categoryStore.put(digital.id(), digital);
        categoryStore.put(books.id(), books);
        categoryStore.put(fashion.id(), fashion);
        categoryStore.put(home.id(), home);
    }

    private static void seedProducts(ProductService productService) {
        // Digital & Electronics
        productService.addProduct(
                "ThinkPad X1 Carbon",
                "Flagship developer ultrabook with 32GB RAM and 1TB NVMe SSD",
                new Money(new BigDecimal("85000000.00"), "IRR"),
                10,
                "CAT-DIGITAL"
        );

        productService.addProduct(
                "Mechanical Gaming Keyboard",
                "RGB backlit mechanical keyboard with tactile blue switches",
                new Money(new BigDecimal("3500000.00"), "IRR"),
                25,
                "CAT-DIGITAL"
        );

        // Books & Literature
        productService.addProduct(
                "Clean Code: Handbook of Agile Software",
                "Classic software craftsmanship guide by Robert C. Martin",
                new Money(new BigDecimal("1200000.00"), "IRR"),
                50,
                "CAT-BOOKS"
        );

        productService.addProduct(
                "Design Patterns: Elements of Reusable Object-Oriented Software",
                "The iconic Gang of Four (GoF) book on architectural design patterns",
                new Money(new BigDecimal("1800000.00"), "IRR"),
                30,
                "CAT-BOOKS"
        );

        // Clothing & Apparel
        productService.addProduct(
                "AUT Engineering Hoodie",
                "Amirkabir University of Technology official heavyweight cotton hoodie",
                new Money(new BigDecimal("2200000.00"), "IRR"),
                40,
                "CAT-FASHION"
        );

        // Home & Office
        productService.addProduct(
                "Ergonomic Office Chair",
                "Breathable mesh back ergonomic desk chair with lumbar support",
                new Money(new BigDecimal("14500000.00"), "IRR"),
                15,
                "CAT-HOME"
        );
    }

    private static void seedCoupons(Map<String, Coupon> couponStore) {
        Coupon welcome20 = new Coupon(
                "WELCOME20",
                20,
                new Money(new BigDecimal("500000.00"), "IRR"),
                new Money(new BigDecimal("1000000.00"), "IRR"),
                LocalDate.now().plusMonths(3)
        );

        Coupon autSpecial = new Coupon(
                "AUT_SPECIAL",
                15,
                new Money(new BigDecimal("300000.00"), "IRR"),
                new Money(new BigDecimal("500000.00"), "IRR"),
                LocalDate.now().plusMonths(3)
        );

        couponStore.put(welcome20.code(), welcome20);
        couponStore.put(autSpecial.code(), autSpecial);
    }
}
