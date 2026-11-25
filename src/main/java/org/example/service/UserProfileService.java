package org.example.service;

import java.util.Objects;
import org.example.exception.DuplicateUserException;
import org.example.exception.InvalidInputException;
import org.example.exception.UserNotFoundException;
import org.example.model.entity.User;
import org.example.model.vo.Address;
import org.example.model.vo.ContactInfo;
import org.example.service.api.AddressBookUseCase;
import org.example.service.api.UserLookupPort;
import org.example.service.api.UserProfileUseCase;
import org.example.service.port.UserRepository;

/**
 * Cohesive customer profile and delivery address-book service backed strictly by
 * {@link UserRepository} (SRP, ISP, DIP).
 */
public class UserProfileService implements UserProfileUseCase, AddressBookUseCase, UserLookupPort {

    private final UserRepository userRepository;

    /**
     * Constructs a {@code UserProfileService} backed by the specified user repository.
     *
     * @param userRepository outbound persistence port for user aggregates
     */
    public UserProfileService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
    }

    @Override
    public User findUserById(String userId) {
        return getProfile(userId);
    }

    @Override
    public User getProfile(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be null or blank");
        }
        return userRepository.findById(userId.trim())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    @Override
    public synchronized void updateProfile(String userId, String fullName, String phone) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidInputException("Full name cannot be blank");
        }
        User user = getProfile(userId);
        ContactInfo contact;
        try {
            contact = new ContactInfo(user.getEmail(), phone);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(e.getMessage());
        }
        if (userRepository.existsByPhoneExcludingUser(contact.phone(), user.getId())) {
            throw new DuplicateUserException("Mobile number is already registered");
        }
        user.updateProfile(fullName.trim(), contact.phone());
        userRepository.save(user);
    }

    @Override
    public void addAddress(String userId, Address address) {
        if (address == null) {
            throw new InvalidInputException("Address cannot be null");
        }
        User user = getProfile(userId);
        user.addAddress(address);
        userRepository.save(user);
    }

    @Override
    public void updateAddress(String userId, Address address) {
        if (address == null) {
            throw new InvalidInputException("Address cannot be null");
        }
        User user = getProfile(userId);
        boolean owned = user.getAddresses().stream()
                .anyMatch(existing -> existing.addressId().equals(address.addressId()));
        if (!owned) {
            throw new InvalidInputException("Address does not belong to this user");
        }
        user.addAddress(address);
        userRepository.save(user);
    }

    @Override
    public void removeAddress(String userId, String addressId) {
        if (addressId == null || addressId.isBlank()) {
            throw new InvalidInputException("Address ID cannot be null or blank");
        }
        User user = getProfile(userId);
        user.removeAddress(addressId);
        userRepository.save(user);
    }

    @Override
    public void setDefaultAddress(String userId, String addressId) {
        if (addressId == null || addressId.isBlank()) {
            throw new InvalidInputException("Address ID cannot be null or blank");
        }
        User user = getProfile(userId);
        user.setDefaultAddress(addressId);
        userRepository.save(user);
    }
}
