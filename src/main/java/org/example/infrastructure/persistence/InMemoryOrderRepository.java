package org.example.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.model.entity.Order;
import org.example.service.port.OrderRepository;

/**
 * Thread-safe in-memory implementation of {@link OrderRepository}.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> store;

    /**
     * Constructs an empty {@code InMemoryOrderRepository}.
     */
    public InMemoryOrderRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemoryOrderRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by order ID
     */
    public InMemoryOrderRepository(Map<String, Order> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public Optional<Order> findById(String orderId) {
        String id = RepositoryPreconditions.requireNonBlank(orderId, "orderId");
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findByUserId(String userId) {
        String uid = RepositoryPreconditions.requireNonBlank(userId, "userId");
        return store.values().stream()
                .filter(order -> uid.equals(order.getUserId()))
                .toList();
    }

    @Override
    public List<Order> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void save(Order order) {
        Objects.requireNonNull(order, "order cannot be null");
        store.put(order.getOrderId(), order);
    }
}
