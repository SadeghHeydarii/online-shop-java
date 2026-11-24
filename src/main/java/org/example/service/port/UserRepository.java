package org.example.service.port;

import java.util.List;
import java.util.Optional;
import org.example.model.entity.User;

/**
 * Outbound persistence port for managing {@link User} entities.
 *
 * <p>Owned by the application policy layer in accordance with the Dependency Inversion Principle (DIP).
 */
public interface UserRepository {

    /**
     * Finds a user by their unique identifier.
     *
     * @param userId non-null, non-blank user identifier
     * @return matching user if present, otherwise empty
     * @throws IllegalArgumentException if {@code userId} is null or blank
     */
    Optional<User> findById(String userId);

    /**
     * Finds a user by email address (case-insensitive) or mobile phone number.
     *
     * @param identifier non-null, non-blank email or phone identifier
     * @return matching user if present, otherwise empty
     * @throws IllegalArgumentException if {@code identifier} is null or blank
     */
    Optional<User> findByIdentifier(String identifier);

    /**
     * Checks whether any account is registered with the given normalized email.
     *
     * @param email non-null email string (empty string returns {@code false})
     * @return {@code true} if an account already uses the email
     */
    boolean existsByEmail(String email);

    /**
     * Checks whether any account is registered with the given phone number.
     *
     * @param phone non-null phone string (empty string returns {@code false})
     * @return {@code true} if an account already uses the phone number
     */
    boolean existsByPhone(String phone);

    /**
     * Checks whether any account other than {@code excludedUserId} uses the given phone number.
     *
     * @param phone          non-null phone string
     * @param excludedUserId user identifier to exclude from the check
     * @return {@code true} if another user holds the phone number
     */
    boolean existsByPhoneExcludingUser(String phone, String excludedUserId);

    /**
     * Persists a new or updated {@link User} entity.
     *
     * @param user non-null user entity
     */
    void save(User user);

    /**
     * Returns a snapshot list of all stored users.
     *
     * @return unmodifiable list of users
     */
    List<User> findAll();
}
