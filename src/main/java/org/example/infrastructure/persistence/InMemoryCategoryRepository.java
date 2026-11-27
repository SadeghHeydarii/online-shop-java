package org.example.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.model.vo.Category;
import org.example.service.port.CategoryRepository;

/**
 * Thread-safe in-memory implementation of {@link CategoryRepository}.
 */
public class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<String, Category> store;

    /**
     * Constructs an empty {@code InMemoryCategoryRepository}.
     */
    public InMemoryCategoryRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemoryCategoryRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by category ID
     */
    public InMemoryCategoryRepository(Map<String, Category> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public Optional<Category> findById(String categoryId) {
        String id = RepositoryPreconditions.requireNonBlank(categoryId, "categoryId");
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Category> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void save(Category category) {
        Objects.requireNonNull(category, "category cannot be null");
        store.put(category.id(), category);
    }
}
