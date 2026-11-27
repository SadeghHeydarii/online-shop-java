package org.example.service.port;

import java.util.List;
import java.util.Optional;
import org.example.model.vo.Category;

/**
 * Outbound persistence port for managing {@link Category} value objects.
 */
public interface CategoryRepository {

    /**
     * Retrieves a category by its unique identifier.
     *
     * @param categoryId non-blank category identifier
     * @return matching category if present, otherwise empty
     * @throws IllegalArgumentException if {@code categoryId} is null or blank
     */
    Optional<Category> findById(String categoryId);

    /**
     * Returns a snapshot list of all stored categories.
     *
     * @return unmodifiable list of categories
     */
    List<Category> findAll();

    /**
     * Persists a category.
     *
     * @param category non-null category value object
     */
    void save(Category category);
}
