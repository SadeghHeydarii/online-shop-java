package org.example.model.entity;

/**
 * Immutable domain concept representing a product classification category.
 *
 * <p>Abstraction Function:
 * AF(c) = a product catalog category identified by {@code c.id}, named {@code c.name},
 *         and described by {@code c.description}.
 *
 * <p>Representation Invariant:
 * <ul>
 *   <li>{@code id != null && !id.isBlank()}</li>
 *   <li>{@code name != null && !name.isBlank()}</li>
 *   <li>{@code description != null && !description.isBlank()}</li>
 * </ul>
 *
 * @param id          unique category identifier (non-null, non-blank)
 * @param name        descriptive display name of the category (non-null, non-blank)
 * @param description explanatory summary of the category (non-null, non-blank)
 */
public record Category(String id, String name, String description) {

    /**
     * Compact constructor enforcing non-null and non-blank invariants.
     *
     * @throws IllegalArgumentException if any parameter is null or blank
     */
    public Category {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Category ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be null or blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Category description cannot be null or blank");
        }
        id = id.trim();
        name = name.trim();
        description = description.trim();
    }
}
