package org.example.infrastructure.catalog;

import java.util.Comparator;
import org.example.model.entity.Product;
import org.example.service.port.ProductSortStrategy;

/**
 * Sort strategy ordering products by ascending price amount.
 */
public class PriceAscendingSort implements ProductSortStrategy {

    private static final Comparator<Product> BY_PRICE_ASC =
            Comparator.comparing(product -> product.getPrice().amount());

    @Override
    public String key() {
        return "price_asc";
    }

    @Override
    public Comparator<Product> comparator() {
        return BY_PRICE_ASC;
    }
}
