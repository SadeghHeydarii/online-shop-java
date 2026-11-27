package org.example.service.api;

/**
 * Inbound use-case API for initiating and completing password recovery (SRP &amp; ISP).
 */
public interface PasswordRecoveryUseCase {

    /**
     * Issues a single-use 6-digit recovery code for an active account matching {@code identifier}.
     *
     * @param identifier registered email address or mobile phone number
     * @return generated 6-digit recovery code
     */
    String requestPasswordReset(String identifier);

    /**
     * Verifies a recovery code, updates the user's password, and revokes active sessions.
     *
     * @param identifier  registered email address or mobile phone number
     * @param code        6-digit recovery code
     * @param newPassword replacement plain-text password meeting strength policy
     */
    void resetPassword(String identifier, String code, String newPassword);
}
