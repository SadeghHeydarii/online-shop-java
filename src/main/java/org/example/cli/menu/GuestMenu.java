package org.example.cli.menu;

import java.util.NoSuchElementException;
import java.util.Objects;
import org.example.cli.ConsoleIO;
import org.example.exception.AuthenticationException;
import org.example.exception.DuplicateUserException;
import org.example.exception.InvalidInputException;
import org.example.model.entity.User;
import org.example.service.AuthService;
import org.example.service.ProductService;
import org.example.service.ReviewService;

/**
 * Interactive console menu for unauthenticated guests.
 *
 * <p>Coordinates guest interactions defined in Section 6.2 and Figure 3 of the domain specification,
 * covering User Stories US 01 through US 04, US 08 through US 14, and password recovery:
 * <ul>
 *   <li>US 01: Customer account registration (via email and/or mobile phone).</li>
 *   <li>US 02: User login and session initiation.</li>
 *   <li>US 04: Password recovery and simulated reset codes.</li>
 *   <li>US 08 - US 14: Product catalog browsing, keyword search, filtering, and reviews via {@link CatalogMenu}.</li>
 * </ul>
 */
public class GuestMenu {

    private final AuthService authService;
    private final ConsoleIO io;
    private final CatalogMenu catalogMenu;
    private String sessionToken;

    /**
     * Constructs a {@code GuestMenu} without an explicit review service.
     *
     * @param authService    authentication service for registration and login
     * @param productService catalog service for product discovery
     * @param io             console input/output utility
     */
    public GuestMenu(AuthService authService, ProductService productService, ConsoleIO io) {
        this(authService, productService, null, io);
    }

    /**
     * Constructs a {@code GuestMenu} with injected authentication, catalog, and review services.
     *
     * @param authService    authentication service for registration and login
     * @param productService catalog service for product discovery
     * @param reviewService  review service for displaying customer feedback
     * @param io             console input/output utility
     */
    public GuestMenu(AuthService authService, ProductService productService, ReviewService reviewService, ConsoleIO io) {
        this.authService = Objects.requireNonNull(authService, "authService cannot be null");
        this.io = Objects.requireNonNull(io, "io cannot be null");
        this.catalogMenu = new CatalogMenu(productService, reviewService, io);
    }

    /**
     * Runs the interactive Guest Menu loop until successful login or application exit.
     *
     * @return the authenticated {@link User} entity upon successful login, or {@code null} if the guest exits
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
            io.println("7. Browse Categories");
            io.println("8. Recover Password");
            io.println("0. Exit Application");
            io.printDivider();

            try {
                int choice = io.readInt("Select an option", 0, 8);
                switch (choice) {
                    case 1 -> handleBrowseProducts();
                    case 2 -> handleSearchProducts();
                    case 3 -> handleFilterAndSort();
                    case 4 -> handleViewProductDetails();
                    case 5 -> handleRegister();
                    case 7 -> catalogMenu.handleBrowseCategories();
                    case 8 -> handlePasswordRecovery();
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
                io.println();
                return null;
            } catch (IllegalArgumentException | IllegalStateException e) {
                io.printError(e.getMessage());
            }
        }
    }

    /**
     * Handles browsing all published products in the catalog.
     */
    public void handleBrowseProducts() {
        catalogMenu.handleBrowseProducts();
    }

    /**
     * Handles keyword-based product catalog search.
     */
    public void handleSearchProducts() {
        catalogMenu.handleSearchProducts();
    }

    /**
     * Handles category filtering, price bounding, and product sorting.
     */
    public void handleFilterAndSort() {
        catalogMenu.handleFilterAndSort();
    }

    /**
     * Handles viewing full product details and reviews.
     */
    public void handleViewProductDetails() {
        catalogMenu.handleViewProductDetails();
    }

    /**
     * Handles customer registration supporting email and/or mobile phone.
     */
    public void handleRegister() {
        io.printHeader("Register New Customer Account");
        String fullName = io.readString("Full Name");
        String email = io.readLine("Email Address (optional if mobile is supplied)");
        String phone = io.readLine("Mobile Number (optional if email is supplied)");
        String password = io.readPassword("Password");

        try {
            User user = authService.register(fullName, email, phone, password);
            io.printSuccess("Registration successful! Account created for " + user.getFullName()
                    + " (" + (user.getEmail().isEmpty() ? user.getPhone() : user.getEmail()) + ").");
            io.printInfo("You may now log in using option 6.");
        } catch (DuplicateUserException | InvalidInputException e) {
            io.printError("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Handles customer login with either email or mobile number.
     *
     * @return the authenticated {@link User} entity or {@code null} on failure
     */
    public User handleLogin() {
        io.printHeader("Sign In to Online Shop");
        String identifier = io.readString("Email Address or Mobile Number");
        String password = io.readPassword("Password");
        try {
            logout();
            sessionToken = authService.login(identifier, password);
            User user = authService.getAuthenticatedUser(sessionToken);
            io.printSuccess("Login successful! Welcome back, " + user.getFullName() + " [" + user.getRole() + "]");
            return user;
        } catch (AuthenticationException | IllegalArgumentException e) {
            io.printError("Login failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Handles console-simulated password recovery workflow.
     */
    public void handlePasswordRecovery() {
        io.printHeader("Password Recovery");
        String identifier = io.readString("Registered Email or Mobile Number");
        try {
            String code = authService.requestPasswordReset(identifier);
            io.printInfo("Console simulation: recovery code " + code + " (valid for 10 minutes)");
            String enteredCode = io.readString("Recovery Code");
            String password = io.readPassword("New Password");
            authService.resetPassword(identifier, enteredCode, password);
            io.printSuccess("Password changed. Please log in again.");
        } catch (AuthenticationException | InvalidInputException e) {
            io.printError("Password recovery failed: " + e.getMessage());
        }
    }

    /**
     * Returns the active session token string if authenticated, or {@code null}.
     *
     * @return session token
     */
    public String getSessionToken() {
        return sessionToken;
    }

    /**
     * Terminates the current active session.
     */
    public void logout() {
        if (sessionToken != null) {
            authService.logout(sessionToken);
            sessionToken = null;
        }
    }
}
