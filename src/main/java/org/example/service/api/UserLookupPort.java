package org.example.service.api;

import org.example.exception.InvalidInputException;
import org.example.exception.UserNotFoundException;
import org.example.model.entity.User;

/**
 * Inbound read-only port for resolving customer accounts and delivery addresses.
 *
 * <p>Segregates user lookup from profile-mutation operations so checkout services depend only
 * on user retrieval (ISP).
 */
public interface UserLookupPort {

    /**
     * Retrieves the user entity for the given identifier.
     *
     * @param userId unique non-blank user identifier
     * @return matching {@link User}
     * @throws InvalidInputException if {@code userId} is null or blank
     * @throws UserNotFoundException if no user exists for {@code userId}
     */
    User findUserById(String userId);
}
