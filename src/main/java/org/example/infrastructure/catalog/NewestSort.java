package org.example.infrastructure.catalog;

import java.util.Comparator;
import org.example.model.entity.Product;
import org.example.service.port.ProductSortStrategy;

/**
 * Sort strategy ordering products from newest to oldest catalog creation timestamp.
 */
public class NewestSort implements ProductSortStrategy {

    private static final Comparator<Product> BY_NEWEST =
            Comparator.comparing(Product::getCreatedAt).reversed();

    @Override
    public String key() {
        return "newest";
    }

    @Override
    public Comparator<Product> comparator() {
        return BY_NEWEST;
    }
}
