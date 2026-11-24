package org.example.service.port;

/**
 * Outbound port for generating prefixed unique identifiers.
 */
public interface IdGenerator {

    /**
     * Generates a unique identifier beginning with {@code prefix + "-"}.
     *
     * @param prefix non-blank identifier prefix (e.g., "usr", "prod", "ord", "txn", "rev", "tok")
     * @return generated non-blank identifier
     * @throws IllegalArgumentException if {@code prefix} is null or blank
     */
    String nextId(String prefix);
}
