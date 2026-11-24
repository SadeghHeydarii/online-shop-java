package org.example.infrastructure.security;

import org.example.service.port.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract Design-by-Contract (LSP) test suite for {@link PasswordHasher} implementations.
 */
public abstract class PasswordHasherContractTest {

    protected abstract PasswordHasher createHasher();

    private PasswordHasher hasher;

    @BeforeEach
    void initContract() {
        hasher = createHasher();
    }

    @Test
    void hashAndMatchesFulfillVerificationContract() {
        String hash = hasher.hash("Secret@123");
        assertNotNull(hash);
        assertFalse(hash.isBlank());
        assertNotEquals("Secret@123", hash);

        assertTrue(hasher.matches("Secret@123", hash));
        assertFalse(hasher.matches("WrongPass", hash));
        assertFalse(hasher.matches(null, hash));
        assertFalse(hasher.matches("Secret@123", null));
        assertFalse(hasher.matches(" ", hash));
    }

    @Test
    void hashRejectsNullOrBlankPasswords() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash("   "));
    }
}
