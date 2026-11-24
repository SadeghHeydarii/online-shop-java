package org.example.infrastructure.persistence;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.service.port.PasswordResetRepository;

/**
 * Thread-safe in-memory implementation of {@link PasswordResetRepository}.
 */
public class InMemoryPasswordResetRepository implements PasswordResetRepository {

    private final Map<String, ResetToken> store;

    /**
     * Constructs an empty {@code InMemoryPasswordResetRepository}.
     */
    public InMemoryPasswordResetRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemoryPasswordResetRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by user ID
     */
    public InMemoryPasswordResetRepository(Map<String, ResetToken> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public void saveResetToken(String userId, ResetToken token) {
        String uid = RepositoryPreconditions.requireNonBlank(userId, "userId");
        Objects.requireNonNull(token, "token cannot be null");
        store.put(uid, token);
    }

    @Override
    public Optional<ResetToken> findResetToken(String userId) {
        String uid = RepositoryPreconditions.requireNonBlank(userId, "userId");
        return Optional.ofNullable(store.get(uid));
    }

    @Override
    public void removeResetToken(String userId) {
        String uid = RepositoryPreconditions.requireNonBlank(userId, "userId");
        store.remove(uid);
    }
}
