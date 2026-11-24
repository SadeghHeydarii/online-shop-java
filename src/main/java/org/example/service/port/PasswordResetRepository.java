package org.example.service.port;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound persistence port for expiring single-use password recovery tokens.
 */
public interface PasswordResetRepository {

    /**
     * Immutable value record capturing a hashed recovery code and its expiration timestamp.
     *
     * @param tokenHash cryptographic hash of the recovery code
     * @param expiresAt expiration timestamp
     */
    record ResetToken(String tokenHash, LocalDateTime expiresAt) {
        public ResetToken {
            if (tokenHash == null || tokenHash.isBlank()) {
                throw new IllegalArgumentException("tokenHash cannot be null or blank");
            }
            Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        }
    }

    /**
     * Stores or replaces the active recovery token for a user.
     *
     * @param userId non-blank user identifier
     * @param token  non-null recovery token record
     */
    void saveResetToken(String userId, ResetToken token);

    /**
     * Retrieves the active recovery token for a user, if any.
     *
     * @param userId non-blank user identifier
     * @return active recovery token if present, otherwise empty
     */
    Optional<ResetToken> findResetToken(String userId);

    /**
     * Removes any active recovery token for a user.
     *
     * @param userId non-blank user identifier
     */
    void removeResetToken(String userId);
}
