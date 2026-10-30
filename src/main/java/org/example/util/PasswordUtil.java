package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class providing cryptographic password hashing and verification services.
 *
 * <p>Uses the standard SHA-256 hashing algorithm and constant-time string comparison
 * to safeguard stored user credentials against tampering and side-channel timing attacks.
 */
public final class PasswordUtil {

    private static final String HASH_ALGORITHM = "SHA-256";

    private PasswordUtil() {
        // Prevent instantiation of utility class
    }

    /**
     * Computes the hexadecimal SHA-256 cryptographic hash of a raw password string.
     *
     * @param rawPassword the plain-text password to hash
     * @return the lowercase 64-character hexadecimal hash
     * @throws IllegalArgumentException if rawPassword is null or blank
     */
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Required cryptographic algorithm unavailable: " + HASH_ALGORITHM, e);
        }
    }

    /**
     * Verifies whether a candidate plain-text password matches a stored cryptographic hash.
     *
     * @param rawPassword the candidate plain-text password
     * @param storedHash  the expected stored hexadecimal hash
     * @return {@code true} if the computed hash matches the stored hash; {@code false} otherwise
     */
    public static boolean verifyPassword(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || rawPassword.isBlank() || storedHash.isBlank()) {
            return false;
        }
        String candidateHash = hashPassword(rawPassword);
        byte[] candidateBytes = candidateHash.getBytes(StandardCharsets.UTF_8);
        byte[] storedBytes = storedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(candidateBytes, storedBytes);
    }
}
