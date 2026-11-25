package org.example.cli.menu;

import java.util.Optional;

/**
 * Entry-point menu contract for unauthenticated storefront visitors (ISP, DIP).
 */
public interface UnauthenticatedMenu {

    /**
     * Runs the guest interaction loop until the user logs in or chooses to exit.
     *
     * @return authenticated session if login succeeds, or empty if exiting the application
     */
    Optional<AuthenticatedSession> showMenu();
}
