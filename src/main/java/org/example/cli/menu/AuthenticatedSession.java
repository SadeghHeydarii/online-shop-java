package org.example.cli.menu;

import java.util.Objects;
import org.example.model.entity.User;

/**
 * Immutable authentication outcome pairing the signed-in {@link User} with its active session token.
 */
public record AuthenticatedSession(User user, String sessionToken) {

    public AuthenticatedSession {
        Objects.requireNonNull(user, "user cannot be null");
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new IllegalArgumentException("sessionToken cannot be null or blank");
        }
    }
}
