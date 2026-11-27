package org.example.infrastructure.catalog;

import java.util.Comparator;
import org.example.model.entity.Product;
import org.example.service.port.ProductSortStrategy;

/**
 * Sort strategy ordering products by descending customer average rating.
 */
public class RatingSort implements ProductSortStrategy {

    private static final Comparator<Product> BY_RATING =
            Comparator.comparingDouble(Product::getAverageRating).reversed();

    @Override
    public String key() {
        return "rating";
    }

    @Override
    public Comparator<Product> comparator() {
        return BY_RATING;
    }
}
