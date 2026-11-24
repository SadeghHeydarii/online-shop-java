package org.example.infrastructure.persistence;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.service.port.SessionRepository;

/**
 * Thread-safe in-memory implementation of {@link SessionRepository}.
 */
public class InMemorySessionRepository implements SessionRepository {

    private final Map<String, String> store;

    /**
     * Constructs an empty {@code InMemorySessionRepository}.
     */
    public InMemorySessionRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemorySessionRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by session token
     */
    public InMemorySessionRepository(Map<String, String> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public void saveSession(String sessionToken, String userId) {
        String token = RepositoryPreconditions.requireNonBlank(sessionToken, "sessionToken");
        String uid = RepositoryPreconditions.requireNonBlank(userId, "userId");
        store.put(token, uid);
    }

    @Override
    public Optional<String> findUserIdByToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(sessionToken.trim()));
    }

    @Override
    public void removeSession(String sessionToken) {
        if (sessionToken != null) {
            store.remove(sessionToken.trim());
        }
    }

    @Override
    public void removeAllSessionsForUser(String userId) {
        String uid = RepositoryPreconditions.requireNonBlank(userId, "userId");
        store.entrySet().removeIf(entry -> uid.equals(entry.getValue()));
    }
}
