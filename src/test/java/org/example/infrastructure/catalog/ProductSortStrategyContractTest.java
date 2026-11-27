package org.example.infrastructure.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.model.entity.Product;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.port.ProductSortStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract Design-by-Contract (LSP) test suite for {@link ProductSortStrategy} implementations.
 */
public abstract class ProductSortStrategyContractTest {

    protected abstract ProductSortStrategy createStrategy();

    private ProductSortStrategy strategy;
    private Product first;
    private Product second;

    @BeforeEach
    void initContract() {
        strategy = createStrategy();
        first = new Product("p-1", "Alpha", "First",
                new Money(new BigDecimal("100000.00"), "IRR"), 10, "CAT-1",
                LocalDateTime.of(2026, 11, 20, 10, 0));
        first.updateRatingStats(5);
        first.recordSale(new Quantity(10));

        second = new Product("p-2", "Beta", "Second",
                new Money(new BigDecimal("200000.00"), "IRR"), 10, "CAT-1",
                LocalDateTime.of(2026, 11, 22, 10, 0));
        second.updateRatingStats(3);
        second.recordSale(new Quantity(2));
    }

    @Test
    void keyIsNonBlankAndLowercase() {
        String key = strategy.key();
        assertNotNull(key);
        assertFalse(key.isBlank());
        assertEquals(key.trim().toLowerCase(), key);
    }

    @Test
    void comparatorIsNonNullAntisymmetricAndConsistentWithItself() {
        var cmp = strategy.comparator();
        assertNotNull(cmp);
        assertEquals(0, cmp.compare(first, first));
        int ab = cmp.compare(first, second);
        int ba = cmp.compare(second, first);
        assertEquals(Integer.signum(ab), -Integer.signum(ba));
        assertDoesNotThrow(() -> List.of(second, first).stream().sorted(cmp).toList());
    }
}
