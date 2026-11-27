package org.example.infrastructure.persistence;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.model.entity.Cart;
import org.example.service.port.CartRepository;

/**
 * Thread-safe in-memory implementation of {@link CartRepository}.
 */
public class InMemoryCartRepository implements CartRepository {

    private final Map<String, Cart> store;

    /**
     * Constructs an empty {@code InMemoryCartRepository}.
     */
    public InMemoryCartRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemoryCartRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by user ID
     */
    public InMemoryCartRepository(Map<String, Cart> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public Optional<Cart> findByUserId(String userId) {
        String uid = RepositoryPreconditions.requireNonBlank(userId, "userId");
        return Optional.ofNullable(store.get(uid));
    }

    @Override
    public Cart getOrCreate(String userId) {
        String uid = RepositoryPreconditions.requireNonBlank(userId, "userId");
        Cart cart = store.computeIfAbsent(uid, id -> new Cart(id, id));
        if (!uid.equals(cart.getUserId())) {
            throw new IllegalStateException("Cart ownership invariant violated: expected owner "
                    + uid + " but found " + cart.getUserId());
        }
        return cart;
    }

    @Override
    public void save(Cart cart) {
        Objects.requireNonNull(cart, "cart cannot be null");
        store.put(cart.getUserId(), cart);
    }
}
