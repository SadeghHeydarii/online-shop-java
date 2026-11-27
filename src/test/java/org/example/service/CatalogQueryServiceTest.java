package org.example.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.example.exception.CategoryNotFoundException;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.infrastructure.catalog.DefaultNameSort;
import org.example.infrastructure.catalog.MapProductSortRegistry;
import org.example.infrastructure.catalog.PriceAscendingSort;
import org.example.infrastructure.catalog.RatingSort;
import org.example.infrastructure.persistence.InMemoryCategoryRepository;
import org.example.infrastructure.persistence.InMemoryProductRepository;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.port.CategoryRepository;
import org.example.service.port.ProductRepository;
import org.example.service.port.ProductSortStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogQueryServiceTest {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private CatalogQueryService queryService;
    private Product laptop;
    private Product book;
    private Product hidden;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        categoryRepository = new InMemoryCategoryRepository();
        categoryRepository.save(new Category("CAT-TECH", "Technology", "Gear", true));
        categoryRepository.save(new Category("CAT-BOOK", "Books", "Reading", true));
        categoryRepository.save(new Category("CAT-OLD", "Archive", "Inactive", false));

        laptop = new Product("p-laptop", "ThinkPad", "Developer laptop",
                new Money(new BigDecimal("900000.00"), "IRR"), 5, "CAT-TECH",
                LocalDateTime.of(2026, 11, 20, 10, 0));
        laptop.updateRatingStats(4);

        book = new Product("p-book", "Clean Code", "Craftsmanship handbook",
                new Money(new BigDecimal("150000.00"), "IRR"), 12, "CAT-BOOK",
                LocalDateTime.of(2026, 11, 22, 10, 0));
        book.updateRatingStats(5);

        hidden = new Product("p-old", "Legacy Item", "Inactive category item",
                new Money(new BigDecimal("50000.00"), "IRR"), 10, "CAT-OLD",
                LocalDateTime.of(2026, 11, 15, 10, 0));

        productRepository.save(laptop);
        productRepository.save(book);
        productRepository.save(hidden);

        queryService = new CatalogQueryService(
                productRepository,
                categoryRepository,
                new MapProductSortRegistry(new DefaultNameSort(), new PriceAscendingSort(), new RatingSort())
        );
    }

    @Test
    void queriesExcludeInactiveProductsAndFilterCorrectly() {
        assertEquals(List.of(book, laptop), queryService.getAllActiveProducts());
        assertEquals(List.of(book), queryService.searchProducts("craftsmanship"));
        assertEquals(List.of(laptop), queryService.getProductsByCategory("CAT-TECH"));
        assertEquals(2, queryService.getActiveCategories().size());
        assertThrows(CategoryNotFoundException.class, () -> queryService.getProductsByCategory("CAT-OLD"));
    }

    @Test
    void validatesAvailabilityAndMissingProducts() {
        assertSame(laptop, queryService.requireAvailableProduct("p-laptop", new Quantity(3)));
        assertThrows(InsufficientStockException.class,
                () -> queryService.requireAvailableProduct("p-laptop", new Quantity(99)));
        assertThrows(InvalidInputException.class,
                () -> queryService.requireAvailableProduct("p-old", new Quantity(1)));
        assertThrows(ProductNotFoundException.class, () -> queryService.getProductById("missing"));
    }

    @Test
    void acceptsCustomSortStrategyWithoutModifyingQueryService_OcpVerification() {
        ProductSortStrategy stockDescSort = new ProductSortStrategy() {
            @Override
            public String key() {
                return "stock_desc";
            }

            @Override
            public Comparator<Product> comparator() {
                return Comparator.comparingInt(Product::getAvailableStock).reversed();
            }
        };

        CatalogQueryService extensibleQuery = new CatalogQueryService(
                productRepository,
                categoryRepository,
                new MapProductSortRegistry(new DefaultNameSort(), stockDescSort)
        );

        assertEquals(List.of("price_asc", "rating"), queryService.getAvailableSortOptions());
        assertEquals(List.of(book, laptop), extensibleQuery.filterAndSort(null, null, null, "stock_desc"));
        assertEquals(List.of("stock_desc"), extensibleQuery.getAvailableSortOptions());
        assertThrows(InvalidInputException.class,
                () -> extensibleQuery.filterAndSort(null, null, null, "nonexistent"));
    }
}
