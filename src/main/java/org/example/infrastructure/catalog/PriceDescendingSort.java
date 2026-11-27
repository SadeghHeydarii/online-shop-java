package org.example.infrastructure.catalog;

import java.util.Comparator;
import org.example.model.entity.Product;
import org.example.service.port.ProductSortStrategy;

/**
 * Sort strategy ordering products by descending price amount.
 */
public class PriceDescendingSort implements ProductSortStrategy {

    private static final Comparator<Product> BY_PRICE_DESC =
            Comparator.comparing((Product product) -> product.getPrice().amount()).reversed();

    @Override
    public String key() {
        return "price_desc";
    }

    @Override
    public Comparator<Product> comparator() {
        return BY_PRICE_DESC;
    }
}
