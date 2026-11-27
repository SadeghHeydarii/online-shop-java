package org.example.infrastructure.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Product;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.port.ProductSortStrategy;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductSortStrategiesAndRegistryTest {

    @Nested class DefaultSortContract extends ProductSortStrategyContractTest {
        @Override protected ProductSortStrategy createStrategy() { return new DefaultNameSort(); }
    }

    @Nested class PriceAscSortContract extends ProductSortStrategyContractTest {
        @Override protected ProductSortStrategy createStrategy() { return new PriceAscendingSort(); }
    }

    @Nested class PriceDescSortContract extends ProductSortStrategyContractTest {
        @Override protected ProductSortStrategy createStrategy() { return new PriceDescendingSort(); }
    }

    @Nested class RatingSortContract extends ProductSortStrategyContractTest {
        @Override protected ProductSortStrategy createStrategy() { return new RatingSort(); }
    }

    @Nested class NewestSortContract extends ProductSortStrategyContractTest {
        @Override protected ProductSortStrategy createStrategy() { return new NewestSort(); }
    }

    @Nested class PopularSortContract extends ProductSortStrategyContractTest {
        @Override protected ProductSortStrategy createStrategy() { return new PopularSort(); }
    }

    @Test
    void registryResolvesAllRegisteredKeysAndFallsBackToDefaultOnBlank() {
        DefaultNameSort defaultSort = new DefaultNameSort();
        PriceAscendingSort priceAsc = new PriceAscendingSort();
        PriceDescendingSort priceDesc = new PriceDescendingSort();
        RatingSort rating = new RatingSort();
        NewestSort newest = new NewestSort();
        PopularSort popular = new PopularSort();

        MapProductSortRegistry registry = new MapProductSortRegistry(
                defaultSort, priceAsc, priceDesc, rating, newest, popular);

        assertSame(defaultSort, registry.resolve(null));
        assertSame(defaultSort, registry.resolve("   "));
        assertSame(priceAsc, registry.resolve("PRICE_ASC"));
        assertSame(priceDesc, registry.resolve("price_desc"));
        assertSame(rating, registry.resolve("rating"));
        assertSame(newest, registry.resolve("newest"));
        assertSame(popular, registry.resolve("popular"));
        assertEquals(List.of("price_asc", "price_desc", "rating", "newest", "popular"), registry.availableKeys());

        assertThrows(InvalidInputException.class, () -> registry.resolve("unknown_sort"));
        assertThrows(IllegalArgumentException.class, () -> new MapProductSortRegistry(defaultSort, priceAsc, priceAsc));
    }

    @Test
    void strategiesOrderSampleProductsCorrectly() {
        Product cheapOldLow = new Product("p1", "Beta", "Desc",
                new Money(new BigDecimal("100.00"), "IRR"), 5, "CAT-1",
                LocalDateTime.of(2026, 11, 10, 8, 0));
        cheapOldLow.updateRatingStats(2);
        cheapOldLow.recordSale(new Quantity(50));

        Product midNewHigh = new Product("p2", "Alpha", "Desc",
                new Money(new BigDecimal("300.00"), "IRR"), 5, "CAT-1",
                LocalDateTime.of(2026, 11, 20, 8, 0));
        midNewHigh.updateRatingStats(5);
        midNewHigh.recordSale(new Quantity(10));

        List<Product> products = List.of(cheapOldLow, midNewHigh);

        assertEquals(List.of(midNewHigh, cheapOldLow),
                products.stream().sorted(new DefaultNameSort().comparator()).toList());
        assertEquals(List.of(cheapOldLow, midNewHigh),
                products.stream().sorted(new PriceAscendingSort().comparator()).toList());
        assertEquals(List.of(midNewHigh, cheapOldLow),
                products.stream().sorted(new PriceDescendingSort().comparator()).toList());
        assertEquals(List.of(midNewHigh, cheapOldLow),
                products.stream().sorted(new RatingSort().comparator()).toList());
        assertEquals(List.of(midNewHigh, cheapOldLow),
                products.stream().sorted(new NewestSort().comparator()).toList());
        assertEquals(List.of(cheapOldLow, midNewHigh),
                products.stream().sorted(new PopularSort().comparator()).toList());
    }
}
