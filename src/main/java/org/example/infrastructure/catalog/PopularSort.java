package org.example.infrastructure.catalog;

import java.util.Comparator;
import org.example.model.entity.Product;
import org.example.service.port.ProductSortStrategy;

/**
 * Sort strategy ordering products by descending cumulative units sold.
 */
public class PopularSort implements ProductSortStrategy {

    private static final Comparator<Product> BY_POPULAR =
            Comparator.comparingLong(Product::getSoldCount).reversed();

    @Override
    public String key() {
        return "popular";
    }

    @Override
    public Comparator<Product> comparator() {
        return BY_POPULAR;
    }
}
