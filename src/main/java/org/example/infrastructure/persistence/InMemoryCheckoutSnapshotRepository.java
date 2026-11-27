package org.example.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.model.vo.CartItem;
import org.example.service.port.CheckoutSnapshotRepository;

/**
 * Thread-safe in-memory implementation of {@link CheckoutSnapshotRepository}.
 */
public class InMemoryCheckoutSnapshotRepository implements CheckoutSnapshotRepository {

    private final Map<String, List<CartItem>> store;

    /**
     * Constructs an empty {@code InMemoryCheckoutSnapshotRepository}.
     */
    public InMemoryCheckoutSnapshotRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemoryCheckoutSnapshotRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by order ID
     */
    public InMemoryCheckoutSnapshotRepository(Map<String, List<CartItem>> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public void saveSnapshot(String orderId, List<CartItem> items) {
        String id = RepositoryPreconditions.requireNonBlank(orderId, "orderId");
        Objects.requireNonNull(items, "items cannot be null");
        store.put(id, List.copyOf(items));
    }

    @Override
    public Optional<List<CartItem>> removeSnapshot(String orderId) {
        String id = RepositoryPreconditions.requireNonBlank(orderId, "orderId");
        return Optional.ofNullable(store.remove(id));
    }
}
