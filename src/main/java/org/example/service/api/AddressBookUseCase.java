package org.example.service.api;

import org.example.model.vo.Address;

/**
 * Inbound use-case API for managing a customer's delivery address book (ISP).
 */
public interface AddressBookUseCase {

    /**
     * Adds a delivery address to the user's address book.
     *
     * @param userId  non-blank user identifier
     * @param address non-null address value object
     */
    void addAddress(String userId, Address address);

    /**
     * Updates an existing owned delivery address.
     *
     * @param userId  non-blank user identifier
     * @param address updated address sharing an existing {@code addressId}
     */
    void updateAddress(String userId, Address address);

    /**
     * Removes a delivery address by its identifier.
     *
     * @param userId    non-blank user identifier
     * @param addressId non-blank address identifier
     */
    void removeAddress(String userId, String addressId);

    /**
     * Designates an owned address as the customer's default delivery address.
     *
     * @param userId    non-blank user identifier
     * @param addressId non-blank address identifier
     */
    void setDefaultAddress(String userId, String addressId);
}
