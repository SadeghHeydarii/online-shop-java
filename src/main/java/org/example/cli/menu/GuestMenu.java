package org.example.cli.menu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.example.cli.ConsoleIO;
import org.example.exception.AuthenticationException;
import org.example.exception.DuplicateUserException;
import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.model.vo.Money;
import org.example.model.vo.Review;
import org.example.service.AuthService;
import org.example.service.ProductService;
import org.example.service.ReviewService;

/**
 * Console interactive menu for guest users prior to authentication.
 *
 * <p>Coordinates user interactions for guest capabilities specified in Section 6.2 and
 * Figure 3 of the authoritative domain specification, covering User Stories US 01, US 02,
 * and US 08 through US 14:
 * <ul>
 *   <li>US 01: Customer account registration.</li>
 *   <li>US 02: User sign-in authentication.</li>
 *   <li>US 08: Browsing available active product catalog.</li>
 *   <li>US 09: Searching products by keyword.</li>
 *   <li>US 10, US 11, US 12: Filtering products by category/price range and sorting.</li>
 *   <li>US 13, US 14: Viewing detailed product specifications and community customer reviews.</li>
 * </ul>
 */
public class GuestMenu {

    private final AuthService authService;
    private final ProductService productService;
    private final ReviewService reviewService;
    private final ConsoleIO io;

    /**
     * Constructs a {@code GuestMenu} without review browsing capabilities.
     *
     * @param authService    authentication service for registration and login
     * @param productService catalog service for product exploration
     * @param io             console I/O handler
     */
    public GuestMenu(AuthService authService, ProductService productService, ConsoleIO io) {
        this(authService, productService, null, io);
    }

    /**
     * Constructs a {@code GuestMenu} with full product and review browsing capabilities.
     *
     * @param authService    authentication service
     * @param productService product catalog service
     * @param reviewService  review service for displaying customer feedback
     * @param io             console I/O handler
     */
    public GuestMenu(AuthService authService, ProductService productService, ReviewService reviewService, ConsoleIO io) {
        this.authService = Objects.requireNonNull(authService, "authService cannot be null");
        this.productService = Objects.requireNonNull(productService, "productService cannot be null");
        this.reviewService = reviewService;
        this.io = Objects.requireNonNull(io, "io cannot be null");
    }

    /**
     * Runs the interactive Guest Menu loop until a user logs in successfully or chooses to exit.
     *
     * @return the authenticated {@link User} upon successful login, or {@code null} if the guest exits
     */
    public User showMenu() {
        while (true) {
            io.printHeader("Online Shop - Guest Menu");
            io.println("1. Browse All Products");
            io.println("2. Search Products by Keyword");
            io.println("3. Filter & Sort Products");
            io.println("4. View Product Details & Reviews");
            io.println("5. Register New Customer Account");
            io.println("6. Login");
            io.println("0. Exit Application");
            io.printDivider();

            try {
                int choice = io.readInt("Select an option", 0, 6);
                switch (choice) {
                    case 1 -> handleBrowseProducts();
                    case 2 -> handleSearchProducts();
                    case 3 -> handleFilterAndSort();
                    case 4 -> handleViewProductDetails();
                    case 5 -> handleRegister();
                    case 6 -> {
                        User loggedInUser = handleLogin();
                        if (loggedInUser != null) {
                            return loggedInUser;
                        }
                    }
                    case 0 -> {
                        io.println("Exiting application. Goodbye!");
                        return null;
                    }
                    default -> io.printError("Invalid option selected.");
                }
            } catch (NoSuchElementException e) {
                // Handle end of input stream (e.g. EOF in automated test or pipe)
                io.println();
                return null;
            }
        }
    }

    // =========================================================================
    // MENU HANDLERS
    // =========================================================================

    /**
     * Displays all active products currently available in the catalog (US 08).
     */
    public void handleBrowseProducts() {
        io.printHeader("Active Products Catalog");
        List<Product> products = productService.getAllActiveProducts();
        displayProductTable(products);
    }

    /**
     * Prompts for search terms and displays matching products (US 09).
     */
    public void handleSearchProducts() {
        io.printHeader("Search Products");
        String keyword = io.readString("Enter search keyword");
        List<Product> results = productService.searchProducts(keyword);
        displayProductTable(results);
    }

    /**
     * Prompts for category and price filter criteria and sorts catalog items (US 10, 11, 12).
     */
    public void handleFilterAndSort() {
        io.printHeader("Filter and Sort Products");
        String categoryId = io.readLine("Category ID (leave blank for any)");
        if (categoryId.isBlank()) {
            categoryId = null;
        }

        BigDecimal minPrice = null;
        String minRaw = io.readLine("Minimum Price in IRR (leave blank for none)");
        if (!minRaw.isBlank()) {
            try {
                minPrice = new BigDecimal(minRaw);
            } catch (NumberFormatException e) {
                io.printWarning("Invalid minimum price format. Ignoring minimum price filter.");
            }
        }

        BigDecimal maxPrice = null;
        String maxRaw = io.readLine("Maximum Price in IRR (leave blank for none)");
        if (!maxRaw.isBlank()) {
            try {
                maxPrice = new BigDecimal(maxRaw);
            } catch (NumberFormatException e) {
                io.printWarning("Invalid maximum price format. Ignoring maximum price filter.");
            }
        }

        io.println("Sort options:");
        io.println("  1. Default (None)");
        io.println("  2. Price: Low to High (price_asc)");
        io.println("  3. Price: High to Low (price_desc)");
        io.println("  4. Rating: Highest First (rating)");
        int sortChoice = io.readInt("Choose sort option", 1, 4);
        String sortBy = switch (sortChoice) {
            case 2 -> "price_asc";
            case 3 -> "price_desc";
            case 4 -> "rating";
            default -> null;
        };

        Money minMoney = minPrice != null ? new Money(minPrice, "IRR") : null;
        Money maxMoney = maxPrice != null ? new Money(maxPrice, "IRR") : null;

        try {
            List<Product> results = productService.filterAndSort(categoryId, minMoney, maxMoney, sortBy);
            displayProductTable(results);
        } catch (InvalidInputException e) {
            io.printError("Invalid filter criteria: " + e.getMessage());
        }
    }

    /**
     * Prompts for a product identifier and displays detailed specifications and reviews (US 13, 14).
     */
    public void handleViewProductDetails() {
        io.printHeader("Product Details & Customer Reviews");
        String productId = io.readString("Enter Product ID");

        try {
            Product product = productService.getProductById(productId);
            io.println();
            io.println("ID:             " + product.getId());
            io.println("Name:           " + product.getName());
            io.println("Description:    " + product.getDescription());
            io.println("Category ID:    " + product.getCategoryId());
            io.println("Price:          " + product.getPrice().amount() + " " + product.getPrice().currency());
            io.println("Stock:          " + product.getAvailableStock() + " units available");
            io.println("Status:         " + (product.isActive() ? "ACTIVE" : "INACTIVE"));
            io.println("Average Rating: " + String.format("%.1f ★ (%d reviews)", product.getAverageRating(), product.getReviewCount()));

            if (reviewService != null) {
                io.println();
                io.printHeader("Customer Reviews for: " + product.getName());
                List<Review> reviews = reviewService.getProductReviews(product.getId());
                if (reviews.isEmpty()) {
                    io.printInfo("No customer reviews for this product yet.");
                } else {
                    for (int i = 0; i < reviews.size(); i++) {
                        Review r = reviews.get(i);
                        io.println((i + 1) + ". Rating: " + "★".repeat(r.rating()) + " (" + r.rating() + "/5)"
                                + " | Author: " + r.authorName()
                                + " | Date: " + r.createdAt().toLocalDate());
                        io.println("   Feedback: " + r.commentText());
                        io.println();
                    }
                }
            }
        } catch (ProductNotFoundException e) {
            io.printError("Product not found: " + e.getMessage());
        }
    }

    /**
     * Prompts user for registration details and creates a new customer account (US 01).
     */
    public void handleRegister() {
        io.printHeader("Register New Customer Account");
        String fullName = io.readString("Full Name");
        String email = io.readString("Email Address");
        String phone = io.readString("Phone Number");
        String password = io.readPassword("Password");

        try {
            User user = authService.register(fullName, email, phone, password);
            io.printSuccess("Registration successful! Account created for " + user.getFullName() + " (" + user.getEmail() + ").");
            io.printInfo("You may now log in using option 6.");
        } catch (DuplicateUserException | InvalidInputException e) {
            io.printError("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Prompts for login credentials and authenticates user against {@link AuthService} (US 02).
     *
     * @return the authenticated {@link User} on success, or {@code null} on authentication failure
     */
    public User handleLogin() {
        io.printHeader("Sign In to Online Shop");
        String email = io.readString("Email Address");
        String password = io.readPassword("Password");

        try {
            String sessionToken = authService.login(email, password);
            User user = authService.getAuthenticatedUser(sessionToken);
            io.printSuccess("Login successful! Welcome back, " + user.getFullName() + " [" + user.getRole() + "]");
            return user;
        } catch (AuthenticationException | IllegalArgumentException e) {
            io.printError("Login failed: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void displayProductTable(List<Product> products) {
        if (products == null || products.isEmpty()) {
            io.printInfo("No products found matching the criteria.");
            return;
        }

        List<String> headers = List.of("ID", "Name", "Category", "Price", "Stock", "Rating");
        List<List<String>> rows = new ArrayList<>();

        for (Product p : products) {
            String priceStr = p.getPrice().amount() + " " + p.getPrice().currency();
            String ratingStr = String.format("%.1f ★ (%d)", p.getAverageRating(), p.getReviewCount());
            rows.add(List.of(
                    p.getId(),
                    p.getName(),
                    p.getCategoryId(),
                    priceStr,
                    String.valueOf(p.getAvailableStock()),
                    ratingStr
            ));
        }

        io.printTable(headers, rows);
        io.println("Total products: " + products.size());
    }
}
