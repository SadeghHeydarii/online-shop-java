package org.example.model.vo;

/**
 * Immutable value object representing a product classification category.
 *
 * <p>Abstraction Function:
 * AF(c) = a product catalog category identified by {@code c.id}, named {@code c.name},
 *         described by {@code c.description}, with active availability status {@code c.active}.
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
 * @param active      whether the category is currently active in the catalog
 */
public record Category(String id, String name, String description, boolean active) {

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

    /**
     * Backward-compatible convenience constructor defaulting active status to {@code true}.
     *
     * @param id          unique category identifier (non-null, non-blank)
     * @param name        descriptive display name of the category (non-null, non-blank)
     * @param description explanatory summary of the category (non-null, non-blank)
     */
    public Category(String id, String name, String description) {
        this(id, name, description, true);
    }
}
