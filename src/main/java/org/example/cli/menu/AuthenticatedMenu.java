package org.example.cli.menu;

import org.example.model.entity.User;
import org.example.model.enums.UserRole;

/**
 * Polymorphic menu contract dispatched Open-Closed by {@link UserRole} (OCP, LSP, DIP).
 *
 * <p>Contract invariants for all implementations:
 * <ul>
 *   <li>{@link #supportedRole()} returns a non-null role handled by this menu.</li>
 *   <li>{@link #showMenu(User)} rejects a {@code null} user via {@link NullPointerException}.</li>
 *   <li>If {@code user.getRole() != supportedRole()}, the menu prints an error and returns without throwing.</li>
 * </ul>
 */
public interface AuthenticatedMenu {

    /**
     * Declares the {@link UserRole} serviced by this menu.
     *
     * @return non-null supported role
     */
    UserRole supportedRole();

    /**
     * Runs the role-specific dashboard loop for the authenticated user.
     *
     * @param user authenticated user (non-null)
     */
    void showMenu(User user);
}
