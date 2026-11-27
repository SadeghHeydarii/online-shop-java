package org.example.infrastructure.catalog;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.example.exception.InvalidInputException;
import org.example.service.port.ProductSortRegistry;
import org.example.service.port.ProductSortStrategy;

/**
 * Map-backed implementation of {@link ProductSortRegistry} supporting Open-Closed registration
 * of {@link ProductSortStrategy} options without {@code switch} statements.
 */
public class MapProductSortRegistry implements ProductSortRegistry {

    private final ProductSortStrategy defaultStrategy;
    private final Map<String, ProductSortStrategy> strategies;

    /**
     * Constructs a registry with a fallback default strategy and explicit named strategies.
     *
     * @param defaultStrategy strategy selected when {@code sortKey} is null or blank
     * @param strategies      registered selectable sort strategies
     */
    public MapProductSortRegistry(ProductSortStrategy defaultStrategy,
                                  Collection<? extends ProductSortStrategy> strategies) {
        this.defaultStrategy = Objects.requireNonNull(defaultStrategy, "defaultStrategy cannot be null");
        Objects.requireNonNull(strategies, "strategies cannot be null");

        Map<String, ProductSortStrategy> map = new LinkedHashMap<>();
        for (ProductSortStrategy strategy : strategies) {
            Objects.requireNonNull(strategy, "sort strategy entry cannot be null");
            String key = normalizeKey(strategy.key());
            if (map.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate sort strategy key: " + key);
            }
            map.put(key, strategy);
        }
        this.strategies = Collections.unmodifiableMap(map);
    }

    /**
     * Convenience constructor accepting varargs strategies.
     *
     * @param defaultStrategy default strategy when sort key is null or blank
     * @param strategies      selectable sort strategies
     */
    public MapProductSortRegistry(ProductSortStrategy defaultStrategy, ProductSortStrategy... strategies) {
        this(defaultStrategy, List.of(Objects.requireNonNull(strategies, "strategies cannot be null")));
    }

    @Override
    public ProductSortStrategy resolve(String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            return defaultStrategy;
        }
        String normalized = sortKey.trim().toLowerCase(Locale.ROOT);
        ProductSortStrategy strategy = strategies.get(normalized);
        if (strategy == null) {
            throw new InvalidInputException("Unrecognized sort option: '" + sortKey
                    + "'. Valid options: " + String.join(", ", strategies.keySet()));
        }
        return strategy;
    }

    @Override
    public List<String> availableKeys() {
        return List.copyOf(strategies.keySet());
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Sort strategy key cannot be null or blank");
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
