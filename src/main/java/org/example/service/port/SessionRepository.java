package org.example.service.port;

import java.util.Optional;

/**
 * Outbound persistence port for managing active authentication session tokens.
 */
public interface SessionRepository {

    /**
     * Records an active session mapping from token to user identifier.
     *
     * @param sessionToken non-blank session token
     * @param userId       non-blank authenticated user identifier
     * @throws IllegalArgumentException if either parameter is null or blank
     */
    void saveSession(String sessionToken, String userId);

    /**
     * Resolves the user identifier associated with an active session token.
     *
     * @param sessionToken candidate session token (null/blank returns empty)
     * @return associated user identifier if active, otherwise empty
     */
    Optional<String> findUserIdByToken(String sessionToken);

    /**
     * Terminates a single active session token if present.
     *
     * @param sessionToken session token to remove (null ignored safely)
     */
    void removeSession(String sessionToken);

    /**
     * Terminates all active sessions belonging to the specified user.
     *
     * @param userId non-blank user identifier
     */
    void removeAllSessionsForUser(String userId);
}
