package org.example.infrastructure.persistence;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.service.port.CredentialRepository;

/**
 * Thread-safe in-memory implementation of {@link CredentialRepository}.
 */
public class InMemoryCredentialRepository implements CredentialRepository {

    private final Map<String, String> store;

    /**
     * Constructs an empty {@code InMemoryCredentialRepository}.
     */
    public InMemoryCredentialRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemoryCredentialRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by user ID
     */
    public InMemoryCredentialRepository(Map<String, String> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public void savePasswordHash(String userId, String passwordHash) {
        String id = RepositoryPreconditions.requireNonBlank(userId, "userId");
        String hash = RepositoryPreconditions.requireNonBlank(passwordHash, "passwordHash");
        store.put(id, hash);
    }

    @Override
    public Optional<String> findPasswordHashByUserId(String userId) {
        String id = RepositoryPreconditions.requireNonBlank(userId, "userId");
        return Optional.ofNullable(store.get(id));
    }
}
