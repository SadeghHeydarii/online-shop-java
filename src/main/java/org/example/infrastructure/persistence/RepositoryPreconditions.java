package org.example.infrastructure.persistence;

/**
 * Package-private input guard helpers shared across in-memory repository implementations.
 */
final class RepositoryPreconditions {

    private RepositoryPreconditions() {
    }

    static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
