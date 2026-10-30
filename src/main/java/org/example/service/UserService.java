package org.example.service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.InvalidInputException;
import org.example.exception.UserNotFoundException;
import org.example.model.entity.User;
import org.example.model.vo.Address;

/**
 * Service managing customer profile information and delivery address books.
 *
 * <p>Coordinates use cases DS 04 (Profile and Address Management), US 05 (View/Edit Profile),
 * US 06 (Add/Remove Addresses), and US 07 (Set Default Address).
 */
public class UserService {

    private final Map<String, User> userStore;

    /**
     * Constructs a {@code UserService} with a dedicated concurrent in-memory user map.
     */
    public UserService() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs a {@code UserService} backed by the provided shared user storage map.
     *
     * @param userStore the backing map for user entities
     */
    public UserService(Map<String, User> userStore) {
        this.userStore = Objects.requireNonNull(userStore, "userStore cannot be null");
    }

    /**
     * Retrieves the profile of a registered user.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> {@code userId != null}</li>
     *   <li><b>MODIFIES:</b> nothing</li>
     *   <li><b>EFFECTS:</b> Returns {@link User} instance; throws {@link UserNotFoundException}
     *       if the user does not exist.</li>
     * </ul>
     *
     * @param userId the unique identifier of the user
     * @return the requested {@link User} entity
     * @throws InvalidInputException if userId is null or blank
     * @throws UserNotFoundException if no user with the given ID exists
     */
    public User getProfile(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be null or blank");
        }
        User user = userStore.get(userId);
        if (user == null) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
        return user;
    }

    /**
     * Updates personal details (full name and phone number) in the user's profile.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> user exists, {@code fullName} non-blank, {@code phone} non-blank</li>
     *   <li><b>MODIFIES:</b> {@code userStore}</li>
     *   <li><b>EFFECTS:</b> Retrieves user and calls {@code user.updateProfile(fullName, phone)}.</li>
     * </ul>
     *
     * @param userId   the unique identifier of the user
     * @param fullName the new full name
     * @param phone    the new telephone contact number
     * @throws InvalidInputException if any argument is null or blank
     * @throws UserNotFoundException if user does not exist
     */
    public void updateProfile(String userId, String fullName, String phone) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidInputException("Full name cannot be null or blank");
        }
        if (phone == null || phone.isBlank()) {
            throw new InvalidInputException("Phone cannot be null or blank");
        }
        User user = getProfile(userId);
        user.updateProfile(fullName, phone);
    }

    /**
     * Adds a delivery address to the user's registered addresses.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> user exists, {@code address != null}</li>
     *   <li><b>MODIFIES:</b> {@code userStore}</li>
     *   <li><b>EFFECTS:</b> Calls {@code user.addAddress(address)}.</li>
     * </ul>
     *
     * @param userId  the unique identifier of the user
     * @param address the delivery address to add
     * @throws InvalidInputException if address is null
     * @throws UserNotFoundException if user does not exist
     */
    public void addAddress(String userId, Address address) {
        if (address == null) {
            throw new InvalidInputException("Address cannot be null");
        }
        User user = getProfile(userId);
        user.addAddress(address);
    }

    /**
     * Removes an address from the user's address book by its identifier.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> user exists, {@code addressId != null}</li>
     *   <li><b>MODIFIES:</b> {@code userStore}</li>
     *   <li><b>EFFECTS:</b> Calls {@code user.removeAddress(addressId)}.</li>
     * </ul>
     *
     * @param userId    the unique identifier of the user
     * @param addressId the identifier of the address to remove
     * @throws InvalidInputException if addressId is null or blank
     * @throws UserNotFoundException if user does not exist
     */
    public void removeAddress(String userId, String addressId) {
        if (addressId == null || addressId.isBlank()) {
            throw new InvalidInputException("Address ID cannot be null or blank");
        }
        User user = getProfile(userId);
        user.removeAddress(addressId);
    }

    /**
     * Designates a specific address as the user's default delivery address.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> user exists, {@code addressId} exists in user addresses</li>
     *   <li><b>MODIFIES:</b> {@code userStore}</li>
     *   <li><b>EFFECTS:</b> Calls {@code user.setDefaultAddress(addressId)}.</li>
     * </ul>
     *
     * @param userId    the unique identifier of the user
     * @param addressId the identifier of the address to designate as default
     * @throws InvalidInputException   if addressId is null or blank
     * @throws UserNotFoundException   if user does not exist
     * @throws IllegalArgumentException if addressId does not exist in user's addresses
     */
    public void setDefaultAddress(String userId, String addressId) {
        if (addressId == null || addressId.isBlank()) {
            throw new InvalidInputException("Address ID cannot be null or blank");
        }
        User user = getProfile(userId);
        user.setDefaultAddress(addressId);
    }
}
