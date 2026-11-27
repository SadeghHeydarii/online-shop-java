package org.example.infrastructure.seed;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.model.vo.Category;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.service.api.CatalogAdminUseCase;
import org.example.service.port.CategoryRepository;
import org.example.service.port.CouponRepository;
import org.example.service.port.CredentialRepository;
import org.example.service.port.PasswordHasher;
import org.example.service.port.UserRepository;

/**
 * Decoupled initial sample data seeder depending strictly on outbound repositories,
 * {@link PasswordHasher}, {@link CatalogAdminUseCase}, and an injectable {@link Clock} (DIP &amp; OCP).
 */
public final class ApplicationDataSeeder {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogAdminUseCase catalogAdmin;
    private final CouponRepository couponRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    /**
     * Constructs an {@code ApplicationDataSeeder} with all required ports.
     *
     * @param userRepository       repository for seeded user accounts
     * @param credentialRepository repository for seeded password hashes
     * @param categoryRepository   repository for seeded catalog categories
     * @param catalogAdmin         admin catalog API for registering initial products
     * @param couponRepository     repository for seeded promotional coupons
     * @param passwordHasher       password hashing strategy
     * @param clock                clock controlling creation and coupon expiry dates
     */
    public ApplicationDataSeeder(UserRepository userRepository,
                                 CredentialRepository credentialRepository,
                                 CategoryRepository categoryRepository,
                                 CatalogAdminUseCase catalogAdmin,
                                 CouponRepository couponRepository,
                                 PasswordHasher passwordHasher,
                                 Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "credentialRepository cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository cannot be null");
        this.catalogAdmin = Objects.requireNonNull(catalogAdmin, "catalogAdmin cannot be null");
        this.couponRepository = Objects.requireNonNull(couponRepository, "couponRepository cannot be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /**
     * Convenience static entry point creating and executing an {@code ApplicationDataSeeder}.
     */
    public static void seed(UserRepository userRepository,
                            CredentialRepository credentialRepository,
                            CategoryRepository categoryRepository,
                            CatalogAdminUseCase catalogAdmin,
                            CouponRepository couponRepository,
                            PasswordHasher passwordHasher,
                            Clock clock) {
        new ApplicationDataSeeder(
                userRepository, credentialRepository, categoryRepository,
                catalogAdmin, couponRepository, passwordHasher, clock
        ).seed();
    }

    /**
     * Populates baseline admin/customer users, categories, sample products, and active coupons.
     */
    public void seed() {
        seedUsers();
        seedCategories();
        seedProducts();
        seedCoupons();
    }

    private void seedUsers() {
        LocalDateTime now = LocalDateTime.now(clock);

        String adminHash = passwordHasher.hash("Admin@123");
        User admin = new User(
                "usr-admin",
                "Administrator System",
                "admin@shop.com",
                "09120000000",
                adminHash,
                UserRole.ADMIN,
                now
        );
        userRepository.save(admin);
        credentialRepository.savePasswordHash(admin.getId(), adminHash);

        String customerHash = passwordHasher.hash("Pass@123");
        User customer = new User(
                "usr-customer",
                "Mohammad Rezaei",
                "customer@shop.com",
                "09121112233",
                customerHash,
                UserRole.CUSTOMER,
                now
        );
        customer.addAddress(new Address(
                "addr-home-1",
                "Mohammad Rezaei",
                "09121112233",
                "Tehran",
                "Tehran",
                "Hafez St, No. 42, Apt 3",
                "1591634311",
                "Home",
                true
        ));
        customer.addAddress(new Address(
                "addr-work-1",
                "Mohammad Rezaei",
                "09121112233",
                "Tehran",
                "Tehran",
                "Amirkabir University of Technology, CE Dept",
                "1591634312",
                "Work",
                false
        ));
        userRepository.save(customer);
        credentialRepository.savePasswordHash(customer.getId(), customerHash);
    }

    private void seedCategories() {
        categoryRepository.save(new Category(
                "CAT-DIGITAL",
                "Digital & Electronics",
                "Laptops, computer hardware, gadgets and technical accessories"
        ));
        categoryRepository.save(new Category(
                "CAT-BOOKS",
                "Books & Literature",
                "Software engineering, computer science and academic textbooks"
        ));
        categoryRepository.save(new Category(
                "CAT-FASHION",
                "Clothing & Apparel",
                "University apparel, hoodies, casual clothing and accessories"
        ));
        categoryRepository.save(new Category(
                "CAT-HOME",
                "Home & Office",
                "Ergonomic furniture, desk gear and workplace essentials"
        ));
    }

    private void seedProducts() {
        catalogAdmin.addProduct(
                "ThinkPad X1 Carbon",
                "Flagship developer ultrabook with 32GB RAM and 1TB NVMe SSD",
                new Money(new BigDecimal("85000000.00"), "IRR"),
                10,
                "CAT-DIGITAL"
        );
        catalogAdmin.addProduct(
                "Mechanical Gaming Keyboard",
                "RGB backlit mechanical keyboard with tactile blue switches",
                new Money(new BigDecimal("3500000.00"), "IRR"),
                25,
                "CAT-DIGITAL"
        );
        catalogAdmin.addProduct(
                "Clean Code: Handbook of Agile Software",
                "Classic software craftsmanship guide by Robert C. Martin",
                new Money(new BigDecimal("1200000.00"), "IRR"),
                50,
                "CAT-BOOKS"
        );
        catalogAdmin.addProduct(
                "Design Patterns: Elements of Reusable Object-Oriented Software",
                "The iconic Gang of Four (GoF) book on architectural design patterns",
                new Money(new BigDecimal("1800000.00"), "IRR"),
                30,
                "CAT-BOOKS"
        );
        catalogAdmin.addProduct(
                "AUT Engineering Hoodie",
                "Amirkabir University of Technology official heavyweight cotton hoodie",
                new Money(new BigDecimal("2200000.00"), "IRR"),
                40,
                "CAT-FASHION"
        );
        catalogAdmin.addProduct(
                "Ergonomic Office Chair",
                "Breathable mesh back ergonomic desk chair with lumbar support",
                new Money(new BigDecimal("14500000.00"), "IRR"),
                15,
                "CAT-HOME"
        );
    }

    private void seedCoupons() {
        LocalDate expiry = LocalDate.now(clock).plusMonths(3);
        couponRepository.save(new Coupon(
                "WELCOME20",
                20,
                new Money(new BigDecimal("500000.00"), "IRR"),
                new Money(new BigDecimal("1000000.00"), "IRR"),
                expiry
        ));
        couponRepository.save(new Coupon(
                "AUT_SPECIAL",
                15,
                new Money(new BigDecimal("300000.00"), "IRR"),
                new Money(new BigDecimal("500000.00"), "IRR"),
                expiry
        ));
    }
}
