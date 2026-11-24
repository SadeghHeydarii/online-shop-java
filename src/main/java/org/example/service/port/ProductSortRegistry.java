package org.example.service.port;

import java.util.List;
import org.example.exception.InvalidInputException;

/**
 * Outbound registry port resolving {@link ProductSortStrategy} implementations by sort key (OCP).
 */
public interface ProductSortRegistry {

    /**
     * Resolves the strategy for {@code sortKey}, returning the default strategy when {@code sortKey}
     * is null or blank.
     *
     * @param sortKey optional sort option key
     * @return resolved non-null {@link ProductSortStrategy}
     * @throws InvalidInputException if a non-blank {@code sortKey} is not registered
     */
    ProductSortStrategy resolve(String sortKey);

    /**
     * Returns the registered non-default sort keys available to catalog consumers.
     *
     * @return unmodifiable list of sort option keys
     */
    List<String> availableKeys();
}
