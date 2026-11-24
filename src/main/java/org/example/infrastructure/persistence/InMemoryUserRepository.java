package org.example.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.model.entity.User;
import org.example.model.vo.ContactInfo;
import org.example.service.port.UserRepository;

/**
 * Thread-safe in-memory implementation of {@link UserRepository}.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> store;

    /**
     * Constructs an empty {@code InMemoryUserRepository}.
     */
    public InMemoryUserRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemoryUserRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by user ID
     */
    public InMemoryUserRepository(Map<String, User> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public Optional<User> findById(String userId) {
        String id = RepositoryPreconditions.requireNonBlank(userId, "userId");
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<User> findByIdentifier(String identifier) {
        String target = RepositoryPreconditions.requireNonBlank(identifier, "identifier");
        return store.values().stream()
                .filter(user -> new ContactInfo(user.getEmail(), user.getPhone()).matches(target))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalized = email.trim();
        return store.values().stream()
                .anyMatch(user -> !user.getEmail().isEmpty() && user.getEmail().equalsIgnoreCase(normalized));
    }

    @Override
    public boolean existsByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String normalized = phone.trim();
        return store.values().stream()
                .anyMatch(user -> !user.getPhone().isEmpty() && user.getPhone().equals(normalized));
    }

    @Override
    public boolean existsByPhoneExcludingUser(String phone, String excludedUserId) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String normalized = phone.trim();
        String excluded = excludedUserId == null ? "" : excludedUserId.trim();
        return store.values().stream()
                .anyMatch(user -> !user.getId().equals(excluded)
                        && !user.getPhone().isEmpty()
                        && user.getPhone().equals(normalized));
    }

    @Override
    public void save(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        store.put(user.getId(), user);
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(store.values());
    }
}
