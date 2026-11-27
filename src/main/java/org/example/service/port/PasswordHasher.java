package org.example.service.port;

/**
 * Outbound security port abstracting password hashing and verification algorithms.
 *
 * <p>Allows upgrading cryptographic hashing (e.g., from SHA-256 to BCrypt/Argon2)
 * without modifying authentication or seeding policies (OCP &amp; DIP).
 */
public interface PasswordHasher {

    /**
     * Computes a cryptographic hash for a raw plain-text password.
     *
     * @param rawPassword non-null, non-blank plain-text password
     * @return non-blank hashed password representation
     * @throws IllegalArgumentException if {@code rawPassword} is null or blank
     */
    String hash(String rawPassword);

    /**
     * Verifies whether a candidate plain-text password matches a stored hash.
     *
     * @param rawPassword candidate plain-text password
     * @param storedHash  stored cryptographic hash
     * @return {@code true} if matching; {@code false} if mismatched or if either input is null/blank
     */
    boolean matches(String rawPassword, String storedHash);
}
