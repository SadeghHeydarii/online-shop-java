package org.example.service.port;

import java.util.Optional;
import org.example.model.entity.Cart;

/**
 * Outbound persistence port for managing customer {@link Cart} aggregates.
 */
public interface CartRepository {

    /**
     * Retrieves an existing shopping cart for the given user if present.
     *
     * @param userId non-blank user identifier
     * @return cart if found, otherwise empty
     * @throws IllegalArgumentException if {@code userId} is null or blank
     */
    Optional<Cart> findByUserId(String userId);

    /**
     * Retrieves the user's cart, or creates and stores a new empty cart if none exists.
     *
     * @param userId non-blank user identifier
     * @return existing or newly created {@link Cart} owned by {@code userId}
     * @throws IllegalArgumentException if {@code userId} is null or blank
     */
    Cart getOrCreate(String userId);

    /**
     * Persists a {@link Cart} aggregate keyed by its owner user identifier.
     *
     * @param cart non-null cart entity
     */
    void save(Cart cart);
}
