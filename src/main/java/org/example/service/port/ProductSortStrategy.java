package org.example.service.port;

import java.util.Comparator;
import org.example.model.entity.Product;

/**
 * Outbound Open-Closed Strategy port defining a catalog product ordering rule (OCP).
 */
public interface ProductSortStrategy {

    /**
     * Returns the canonical identifier for this sort strategy (e.g., "price_asc", "rating").
     *
     * @return non-blank lowercase sort key
     */
    String key();

    /**
     * Returns a non-null primary comparator ordering {@link Product} instances.
     *
     * @return primary product comparator
     */
    Comparator<Product> comparator();
}
