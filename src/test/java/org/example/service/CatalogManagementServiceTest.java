package org.example.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.exception.CategoryNotFoundException;
import org.example.exception.InvalidInputException;
import org.example.infrastructure.persistence.InMemoryCategoryRepository;
import org.example.infrastructure.persistence.InMemoryProductRepository;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.port.CategoryRepository;
import org.example.service.port.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogManagementServiceTest {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private CatalogManagementService managementService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T09:30:00Z"), ZoneOffset.UTC);
        AtomicInteger seq = new AtomicInteger(1);

        productRepository = new InMemoryProductRepository();
        categoryRepository = new InMemoryCategoryRepository();
        categoryRepository.save(new Category("CAT-TECH", "Technology", "Hardware", true));
        categoryRepository.save(new Category("CAT-OFF", "Disabled", "Off", false));

        managementService = new CatalogManagementService(
                productRepository,
                categoryRepository,
                prefix -> prefix + "-" + seq.getAndIncrement(),
                clock
        );
    }

    @Test
    void addsUpdatesDeactivatesAndManagesStockViaPorts() {
        Money initialPrice = new Money(new BigDecimal("100000.00"), "IRR");
        managementService.addProduct("Mouse", "Optical", initialPrice, 10, "CAT-TECH");

        Product saved = productRepository.findById("prod-1").orElseThrow();
        assertEquals(LocalDateTime.of(2026, 11, 25, 9, 30), saved.getCreatedAt());

        Money updatedPrice = new Money(new BigDecimal("120000.00"), "IRR");
        managementService.updateProduct("prod-1", "Pro Mouse", "Laser", updatedPrice, "CAT-TECH");
        managementService.adjustStock("prod-1", 5);
        managementService.adjustStock("prod-1", -3);
        managementService.recordSale("prod-1", new Quantity(3));
        managementService.setProductImages("prod-1", List.of("https://shop.local/img/mouse.png"));

        assertEquals("Pro Mouse", saved.getName());
        assertEquals(12, saved.getAvailableStock());
        assertEquals(3, saved.getSoldCount());
        assertEquals(List.of("https://shop.local/img/mouse.png"), saved.getImageUrls());

        managementService.deactivateProduct("prod-1");
        assertFalse(saved.isActive());
        assertEquals(1, managementService.getAllProducts().size());
    }

    @Test
    void rejectsInactiveCategoriesAndInvalidInputs() {
        Money price = new Money(new BigDecimal("50000.00"), "IRR");
        assertThrows(CategoryNotFoundException.class,
                () -> managementService.addProduct("Item", "Desc", price, 5, "CAT-OFF"));
        assertThrows(InvalidInputException.class,
                () -> managementService.addProduct(" ", "Desc", price, 5, "CAT-TECH"));
    }
}
