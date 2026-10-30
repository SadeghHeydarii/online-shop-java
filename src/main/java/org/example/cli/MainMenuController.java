package org.example.cli;

import java.util.NoSuchElementException;
import java.util.Objects;
import org.example.cli.menu.AdminMenu;
import org.example.cli.menu.CustomerMenu;
import org.example.cli.menu.GuestMenu;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.service.AuthService;

/**
 * Central CLI controller coordinating top-level navigation, session routing, and menu transitions.
 *
 * <p>Implements the application control flow defined in Figure 3 and Section 6 of the authoritative
 * domain specification:
 * <ul>
 *   <li>Presents the unauthenticated {@link GuestMenu} when no active user session exists.</li>
 *   <li>Routes authenticated users to {@link CustomerMenu} or {@link AdminMenu} according to their {@link UserRole}.</li>
 *   <li>Handles logout transitions gracefully, returning the user to the guest menu until application shutdown.</li>
 * </ul>
 */
public class MainMenuController {

    private final GuestMenu guestMenu;
    private final CustomerMenu customerMenu;
    private final AdminMenu adminMenu;
    private final AuthService authService;
    private final ConsoleIO io;

    private User currentUser;

    /**
     * Constructs a {@code MainMenuController} without an explicit authentication service reference.
     *
     * @param guestMenu    menu handling guest actions and authentication
     * @param customerMenu menu handling customer interactions
     * @param adminMenu    menu handling administrative and storekeeper interactions
     * @param io           console I/O handler
     */
    public MainMenuController(GuestMenu guestMenu,
                              CustomerMenu customerMenu,
                              AdminMenu adminMenu,
                              ConsoleIO io) {
        this(guestMenu, customerMenu, adminMenu, null, io);
    }

    /**
     * Constructs a {@code MainMenuController} with all injected menus and services.
     *
     * @param guestMenu    menu handling guest actions and authentication
     * @param customerMenu menu handling customer interactions
     * @param adminMenu    menu handling administrative and storekeeper interactions
     * @param authService  authentication service for session state synchronization
     * @param io           console I/O handler
     */
    public MainMenuController(GuestMenu guestMenu,
                              CustomerMenu customerMenu,
                              AdminMenu adminMenu,
                              AuthService authService,
                              ConsoleIO io) {
        this.guestMenu = Objects.requireNonNull(guestMenu, "guestMenu cannot be null");
        this.customerMenu = Objects.requireNonNull(customerMenu, "customerMenu cannot be null");
        this.adminMenu = Objects.requireNonNull(adminMenu, "adminMenu cannot be null");
        this.authService = authService;
        this.io = Objects.requireNonNull(io, "io cannot be null");
    }

    /**
     * Starts the main application interaction loop, routing users through menus according to session state.
     */
    public void start() {
        printWelcomeBanner();

        while (true) {
            try {
                if (currentUser == null) {
                    // Route to Guest Menu (Browsing, Search, Registration, Login, Exit)
                    currentUser = guestMenu.showMenu();
                    if (currentUser == null) {
                        // User chose to exit the application from Guest Menu
                        break;
                    }
                }

                // Route authenticated user based on role
                UserRole role = currentUser.getRole();
                if (role == UserRole.CUSTOMER) {
                    customerMenu.showMenu(currentUser);
                } else if (role == UserRole.ADMIN || role == UserRole.STOREKEEPER) {
                    adminMenu.showMenu(currentUser);
                } else {
                    io.printError("Unrecognized user role: " + role);
                }

                // After customer or admin menu returns, the user chose to Log Out (Option 0)
                handleLogout();

            } catch (NoSuchElementException e) {
                // Input stream closed or terminated (e.g. automated pipe or test execution)
                io.println();
                break;
            }
        }

        printGoodbyeBanner();
    }

    /**
     * Clears current authenticated user session state.
     */
    public void handleLogout() {
        if (currentUser != null) {
            currentUser = null;
        }
    }

    /**
     * Returns the currently active authenticated user, or {@code null} if in guest mode.
     *
     * @return current active user or {@code null}
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Explicitly sets the current authenticated user (primarily used for test session setup).
     *
     * @param user the authenticated user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private void printWelcomeBanner() {
        io.println("================================================================================");
        io.println("                   WELCOME TO AMIRKABIR ONLINE SHOP (AUT)                      ");
        io.println("               Object-Oriented Software Design Project - Fall 2026              ");
        io.println("================================================================================");
    }

    private void printGoodbyeBanner() {
        io.println();
        io.println("================================================================================");
        io.println("              Thank you for using Amirkabir Online Shop. Goodbye!               ");
        io.println("================================================================================");
    }
}
