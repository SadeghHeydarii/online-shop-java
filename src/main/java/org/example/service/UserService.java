package org.example.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.model.entity.User;
import org.example.model.vo.Address;
import org.example.service.port.UserRepository;

/**
 * Deprecated transitional facade delegating profile and address-book operations to
 * {@link UserProfileService}.
 *
 * @deprecated Scheduled for removal once {@code CustomerMenu} and {@code Main} migrate to
 *             {@code UserProfileUseCase} and {@code AddressBookUseCase} in Phase 5.
 */
@Deprecated
public class UserService {

    private final UserProfileService profileService;

    /**
     * Constructs a {@code UserService} with a dedicated concurrent in-memory user map.
     */
    public UserService() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs a {@code UserService} bridging a raw user storage map to {@link UserProfileService}.
     *
     * @param userStore the backing map for user entities
     */
    public UserService(Map<String, User> userStore) {
        Objects.requireNonNull(userStore, "userStore cannot be null");
        this.profileService = new UserProfileService(new MapBackedUserRepository(userStore));
    }

    public User getProfile(String userId) {
        return profileService.getProfile(userId);
    }

    public void updateProfile(String userId, String fullName, String phone) {
        profileService.updateProfile(userId, fullName, phone);
    }

    public void addAddress(String userId, Address address) {
        profileService.addAddress(userId, address);
    }

    public void updateAddress(String userId, Address address) {
        profileService.updateAddress(userId, address);
    }

    public void removeAddress(String userId, String addressId) {
        profileService.removeAddress(userId, addressId);
    }

    public void setDefaultAddress(String userId, String addressId) {
        profileService.setDefaultAddress(userId, addressId);
    }

    // Legacy transition adapter removed in Phase 5 along with UserService
    private record MapBackedUserRepository(Map<String, User> store) implements UserRepository {
        @Override
        public Optional<User> findById(String userId) {
            return Optional.ofNullable(store.get(userId.trim()));
        }

        @Override
        public Optional<User> findByIdentifier(String identifier) {
            return store.values().stream()
                    .filter(user -> user.matchesContact(identifier))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            if (email == null || email.isBlank()) {
                return false;
            }
            String norm = email.trim();
            return store.values().stream()
                    .anyMatch(u -> !u.getEmail().isEmpty() && u.getEmail().equalsIgnoreCase(norm));
        }

        @Override
        public boolean existsByPhone(String phone) {
            if (phone == null || phone.isBlank()) {
                return false;
            }
            String norm = phone.trim();
            return store.values().stream()
                    .anyMatch(u -> !u.getPhone().isEmpty() && u.getPhone().equals(norm));
        }

        @Override
        public boolean existsByPhoneExcludingUser(String phone, String excludedUserId) {
            if (phone == null || phone.isBlank()) {
                return false;
            }
            String norm = phone.trim();
            return store.values().stream()
                    .anyMatch(u -> !u.getId().equals(excludedUserId)
                            && !u.getPhone().isEmpty()
                            && u.getPhone().equals(norm));
        }

        @Override
        public void save(User user) {
            store.put(user.getId(), user);
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(store.values());
        }
    }
}
