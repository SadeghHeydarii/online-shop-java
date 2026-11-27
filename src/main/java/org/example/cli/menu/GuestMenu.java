package org.example.cli.menu;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.exception.ShopException;
import org.example.model.entity.User;
import org.example.service.api.AuthUseCase;
import org.example.service.api.PasswordRecoveryUseCase;

/**
 * Interactive console menu for unauthenticated visitors implementing {@link UnauthenticatedMenu} (SRP, ISP, DIP).
 *
 * <p>Delivers login outcomes as stateless {@link AuthenticatedSession} values so session lifecycle
 * ownership resides in {@link org.example.cli.MainMenuController} rather than the guest view.
 */
public class GuestMenu implements UnauthenticatedMenu {

    private final AuthUseCase authUseCase;
    private final PasswordRecoveryUseCase passwordRecoveryUseCase;
    private final CatalogMenu catalogMenu;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs a {@code GuestMenu} with segregated authentication, recovery, catalog, and CLI I/O ports.
     *
     * @param authUseCase             registration and login port
     * @param passwordRecoveryUseCase account password reset port
     * @param catalogMenu             reusable storefront catalog exploration menu
     * @param in                      narrow console input port
     * @param out                     narrow console output port
     */
    public GuestMenu(AuthUseCase authUseCase,
                     PasswordRecoveryUseCase passwordRecoveryUseCase,
                     CatalogMenu catalogMenu,
                     InputReader in,
                     OutputWriter out) {
        this.authUseCase = Objects.requireNonNull(authUseCase, "authUseCase cannot be null");
        this.passwordRecoveryUseCase = Objects.requireNonNull(passwordRecoveryUseCase, "passwordRecoveryUseCase cannot be null");
        this.catalogMenu = Objects.requireNonNull(catalogMenu, "catalogMenu cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    @Override
    public Optional<AuthenticatedSession> showMenu() {
        while (true) {
            out.printHeader("Online Shop - Guest Menu");
            out.println("1. Browse All Products");
            out.println("2. Search Products by Keyword");
            out.println("3. Filter & Sort Products");
            out.println("4. View Product Details & Reviews");
            out.println("5. Register New Customer Account");
            out.println("6. Login");
            out.println("7. Browse Categories");
            out.println("8. Recover Password");
            out.println("0. Exit Application");
            out.printDivider();

            try {
                int choice = in.readInt("Select an option", 0, 8);
                switch (choice) {
                    case 1 -> handleBrowseProducts();
                    case 2 -> handleSearchProducts();
                    case 3 -> handleFilterAndSort();
                    case 4 -> handleViewProductDetails();
                    case 5 -> handleRegister();
                    case 7 -> catalogMenu.handleBrowseCategories();
                    case 8 -> handlePasswordRecovery();
                    case 6 -> {
                        Optional<AuthenticatedSession> session = handleLogin();
                        if (session.isPresent()) {
                            return session;
                        }
                    }
                    case 0 -> {
                        out.println("Exiting application. Goodbye!");
                        return Optional.empty();
                    }
                    default -> out.printError("Invalid option selected.");
                }
            } catch (NoSuchElementException e) {
                out.println();
                return Optional.empty();
            } catch (ShopException | IllegalArgumentException e) {
                out.printError(e.getMessage());
            }
        }
    }

    public void handleBrowseProducts() {
        catalogMenu.handleBrowseProducts();
    }

    public void handleSearchProducts() {
        catalogMenu.handleSearchProducts();
    }

    public void handleFilterAndSort() {
        catalogMenu.handleFilterAndSort();
    }

    public void handleViewProductDetails() {
        catalogMenu.handleViewProductDetails();
    }

    public void handleRegister() {
        out.printHeader("Register New Customer Account");
        String fullName = in.readString("Full Name");
        String email = in.readLine("Email Address (optional if mobile is supplied)");
        String phone = in.readLine("Mobile Number (optional if email is supplied)");
        String password = in.readPassword("Password");

        try {
            User user = authUseCase.register(fullName, email, phone, password);
            out.printSuccess("Registration successful! Account created for " + user.getFullName()
                    + " (" + (user.getEmail().isEmpty() ? user.getPhone() : user.getEmail()) + ").");
            out.printInfo("You may now log in using option 6.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Registration failed: " + e.getMessage());
        }
    }

    public Optional<AuthenticatedSession> handleLogin() {
        out.printHeader("Sign In to Online Shop");
        String identifier = in.readString("Email Address or Mobile Number");
        String password = in.readPassword("Password");
        try {
            String token = authUseCase.login(identifier, password);
            User user = authUseCase.getAuthenticatedUser(token);
            out.printSuccess("Login successful! Welcome back, " + user.getFullName() + " [" + user.getRole() + "]");
            return Optional.of(new AuthenticatedSession(user, token));
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Login failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void handlePasswordRecovery() {
        out.printHeader("Password Recovery");
        String identifier = in.readString("Registered Email or Mobile Number");
        try {
            String code = passwordRecoveryUseCase.requestPasswordReset(identifier);
            out.printInfo("Console simulation: recovery code " + code + " (valid for 10 minutes)");
            String enteredCode = in.readString("Recovery Code");
            String password = in.readPassword("New Password");
            passwordRecoveryUseCase.resetPassword(identifier, enteredCode, password);
            out.printSuccess("Password changed. Please log in again.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Password recovery failed: " + e.getMessage());
        }
    }
}
