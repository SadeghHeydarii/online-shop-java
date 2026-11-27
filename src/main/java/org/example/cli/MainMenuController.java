package org.example.cli;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.example.cli.io.OutputWriter;
import org.example.cli.menu.AuthenticatedMenu;
import org.example.cli.menu.AuthenticatedSession;
import org.example.cli.menu.UnauthenticatedMenu;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.service.api.AuthUseCase;

/**
 * Central CLI coordinator dispatching unauthenticated and role-based menus polymorphically (SRP, OCP, LSP, DIP).
 *
 * <p>Role routing uses a polymorphic {@code Map<UserRole, AuthenticatedMenu>} registry so adding
 * new authenticated roles does not modify controller branching logic.
 */
public class MainMenuController {

    private final UnauthenticatedMenu unauthenticatedMenu;
    private final Map<UserRole, AuthenticatedMenu> roleMenus;
    private final AuthUseCase authUseCase;
    private final OutputWriter out;

    private User currentUser;
    private String currentSessionToken;

    /**
     * Constructs a {@code MainMenuController} with polymorphic role menus and session lifecycle control.
     *
     * @param unauthenticatedMenu entry menu for unauthenticated visitors
     * @param authenticatedMenus  registered role menus indexed by {@link AuthenticatedMenu#supportedRole()}
     * @param authUseCase         session validation and logout port
     * @param out                 narrow console output port
     */
    public MainMenuController(UnauthenticatedMenu unauthenticatedMenu,
                              Collection<? extends AuthenticatedMenu> authenticatedMenus,
                              AuthUseCase authUseCase,
                              OutputWriter out) {
        this.unauthenticatedMenu = Objects.requireNonNull(unauthenticatedMenu, "unauthenticatedMenu cannot be null");
        this.authUseCase = Objects.requireNonNull(authUseCase, "authUseCase cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");

        Objects.requireNonNull(authenticatedMenus, "authenticatedMenus cannot be null");
        Map<UserRole, AuthenticatedMenu> map = new EnumMap<>(UserRole.class);
        for (AuthenticatedMenu menu : authenticatedMenus) {
            Objects.requireNonNull(menu, "authenticatedMenus entry cannot be null");
            UserRole role = Objects.requireNonNull(menu.supportedRole(), "supportedRole cannot be null");
            if (map.containsKey(role)) {
                throw new IllegalArgumentException("Duplicate menu registration for role: " + role);
            }
            map.put(role, menu);
        }
        this.roleMenus = Collections.unmodifiableMap(map);
    }

    /**
     * Runs the top-level menu dispatch loop until exit or EOF.
     */
    public void start() {
        printWelcomeBanner();

        while (true) {
            try {
                if (currentUser == null) {
                    Optional<AuthenticatedSession> session = unauthenticatedMenu.showMenu();
                    if (session.isEmpty()) {
                        break;
                    }
                    setCurrentSession(session.get());
                }

                if (currentSessionToken != null && authUseCase.getAuthenticatedUser(currentSessionToken) == null) {
                    handleLogout();
                    continue;
                }

                AuthenticatedMenu roleMenu = roleMenus.get(currentUser.getRole());
                if (roleMenu != null) {
                    roleMenu.showMenu(currentUser);
                } else {
                    out.printError("This role cannot access the shop menus: " + currentUser.getRole());
                }

                handleLogout();
            } catch (NoSuchElementException e) {
                out.println();
                break;
            }
        }

        printGoodbyeBanner();
    }

    /**
     * Revokes any active session token in {@link AuthUseCase} and clears controller session state.
     */
    public void handleLogout() {
        if (currentSessionToken != null) {
            authUseCase.logout(currentSessionToken);
            currentSessionToken = null;
        }
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentSessionToken() {
        return currentSessionToken;
    }

    public void setCurrentSession(AuthenticatedSession session) {
        Objects.requireNonNull(session, "session cannot be null");
        this.currentUser = session.user();
        this.currentSessionToken = session.sessionToken();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.currentSessionToken = null;
    }

    private void printWelcomeBanner() {
        out.println("================================================================================");
        out.println("                   WELCOME TO AMIRKABIR ONLINE SHOP (AUT)                      ");
        out.println("               Object-Oriented Software Design Project - Fall 2026              ");
        out.println("================================================================================");
    }

    private void printGoodbyeBanner() {
        out.println();
        out.println("================================================================================");
        out.println("              Thank you for using Amirkabir Online Shop. Goodbye!               ");
        out.println("================================================================================");
    }
}
