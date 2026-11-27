package org.example.service.api;

import org.example.model.entity.User;

/**
 * Inbound use-case API for customer registration, login, logout, and session resolution (ISP).
 */
public interface AuthUseCase {

    /**
     * Registers a new customer account with unique contact details.
     *
     * @param fullName customer full name
     * @param email    optional email address (required if phone absent)
     * @param phone    optional mobile phone (required if email absent)
     * @param password plain-text password meeting strength policy
     * @return newly created {@link User}
     */
    User register(String fullName, String email, String phone, String password);

    /**
     * Authenticates a user by email/phone and password and starts an active session.
     *
     * @param identifier email or mobile phone identifier
     * @param password   candidate plain-text password
     * @return generated session token
     */
    String login(String identifier, String password);

    /**
     * Terminates an active session if present.
     *
     * @param sessionToken active session token (null ignored)
     */
    void logout(String sessionToken);

    /**
     * Resolves the currently authenticated active user for {@code sessionToken}.
     *
     * @param sessionToken active session token
     * @return authenticated {@link User}, or {@code null} if invalid/inactive
     */
    User getAuthenticatedUser(String sessionToken);
}
