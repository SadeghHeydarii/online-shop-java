package org.example.infrastructure.security;

import org.example.service.port.PasswordHasher;
import org.example.util.PasswordUtil;

/**
 * SHA-256 implementation of {@link PasswordHasher} delegating to {@link PasswordUtil}.
 */
public class Sha256PasswordHasher implements PasswordHasher {

    @Override
    public String hash(String rawPassword) {
        return PasswordUtil.hashPassword(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        return PasswordUtil.verifyPassword(rawPassword, storedHash);
    }
}
