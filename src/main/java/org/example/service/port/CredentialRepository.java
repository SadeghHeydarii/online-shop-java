package org.example.service.port;

import java.util.Optional;

/**
 * Outbound persistence port for storing and retrieving user password hashes.
 */
public interface CredentialRepository {

    /**
     * Stores or updates the cryptographic password hash for a user.
     *
     * @param userId       non-blank user identifier
     * @param passwordHash non-blank cryptographic hash
     * @throws IllegalArgumentException if either parameter is null or blank
     */
    void savePasswordHash(String userId, String passwordHash);

    /**
     * Retrieves the stored password hash for the specified user.
     *
     * @param userId non-blank user identifier
     * @return stored password hash if present, otherwise empty
     */
    Optional<String> findPasswordHashByUserId(String userId);
}
