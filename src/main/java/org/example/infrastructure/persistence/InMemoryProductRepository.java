package org.example.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.model.entity.Product;
import org.example.service.port.ProductRepository;

/**
 * Thread-safe in-memory implementation of {@link ProductRepository}.
 */
public class InMemoryProductRepository implements ProductRepository {

    private final Map<String, Product> store;

    /**
     * Constructs an empty {@code InMemoryProductRepository}.
     */
    public InMemoryProductRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemoryProductRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by product ID
     */
    public InMemoryProductRepository(Map<String, Product> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public Optional<Product> findById(String productId) {
        String id = RepositoryPreconditions.requireNonBlank(productId, "productId");
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void save(Product product) {
        Objects.requireNonNull(product, "product cannot be null");
        store.put(product.getId(), product);
    }
}
