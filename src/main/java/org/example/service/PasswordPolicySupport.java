package org.example.service;

import org.example.exception.InvalidInputException;

/**
 * Package-private shared password strength validation helper preventing duplication
 * between registration and password-recovery policies.
 */
final class PasswordPolicySupport {

    static final int MIN_PASSWORD_LENGTH = 6;

    private PasswordPolicySupport() {
    }

    static void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidInputException("Password must contain at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }
}
