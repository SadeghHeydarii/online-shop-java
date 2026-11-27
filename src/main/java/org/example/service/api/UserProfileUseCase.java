package org.example.service.api;

import org.example.model.entity.User;

/**
 * Inbound use-case API for reading and updating customer profile identity details (ISP).
 */
public interface UserProfileUseCase {

    /**
     * Retrieves the profile of a registered user.
     *
     * @param userId non-blank user identifier
     * @return matching {@link User}
     */
    User getProfile(String userId);

    /**
     * Updates the customer's full name and mobile phone number after verifying uniqueness.
     *
     * @param userId   non-blank user identifier
     * @param fullName updated display name
     * @param phone    updated unique mobile phone number
     */
    void updateProfile(String userId, String fullName, String phone);
}
