package org.example.infrastructure.catalog;

import java.util.Comparator;
import org.example.model.entity.Product;
import org.example.service.port.ProductSortStrategy;

/**
 * Default alphabetical sort strategy ordering products ascending by name.
 */
public class DefaultNameSort implements ProductSortStrategy {

    private static final Comparator<Product> BY_NAME = Comparator.comparing(Product::getName);

    @Override
    public String key() {
        return "default";
    }

    @Override
    public Comparator<Product> comparator() {
        return BY_NAME;
    }
}
