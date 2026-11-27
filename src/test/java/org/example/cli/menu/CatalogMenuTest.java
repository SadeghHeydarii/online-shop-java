package org.example.cli.menu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Comparator;
import org.example.cli.ConsoleIO;
import org.example.cli.view.ProductTablePrinter;
import org.example.infrastructure.catalog.DefaultNameSort;
import org.example.infrastructure.catalog.MapProductSortRegistry;
import org.example.infrastructure.persistence.InMemoryCategoryRepository;
import org.example.infrastructure.persistence.InMemoryProductRepository;
import org.example.infrastructure.security.UuidIdGenerator;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.service.CatalogManagementService;
import org.example.service.CatalogQueryService;
import org.example.service.port.ProductSortStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CatalogMenu Dynamic Sort & OCP Tests")
class CatalogMenuTest {

    @Test
    @DisplayName("handleFilterAndSort discovers newly registered sort strategies dynamically without modifying CatalogMenu")
    void rendersCustomSortStrategyDynamically() {
        var productRepo = new InMemoryProductRepository();
        var categoryRepo = new InMemoryCategoryRepository();
        categoryRepo.save(new Category("CAT-TECH", "Technology", "Hardware"));

        Clock clock = Clock.systemDefaultZone();
        var catalogAdmin = new CatalogManagementService(productRepo, categoryRepo, new UuidIdGenerator(), clock);
        catalogAdmin.addProduct("Item A", "Low stock item", new Money(new BigDecimal("1000.00"), "IRR"), 2, "CAT-TECH");
        catalogAdmin.addProduct("Item B", "High stock item", new Money(new BigDecimal("2000.00"), "IRR"), 50, "CAT-TECH");

        ProductSortStrategy stockDesc = new ProductSortStrategy() {
            @Override
            public String key() {
                return "stock_desc";
            }

            @Override
            public Comparator<Product> comparator() {
                return Comparator.comparingInt(Product::getAvailableStock).reversed();
            }
        };

        var catalogQuery = new CatalogQueryService(
                productRepo, categoryRepo, new MapProductSortRegistry(new DefaultNameSort(), stockDesc));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO io = new ConsoleIO(
                new ByteArrayInputStream("\n\n\n2\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8)
        );

        CatalogMenu menu = new CatalogMenu(catalogQuery, null, new ProductTablePrinter(io), io);
        menu.handleFilterAndSort();

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("2. stock_desc"));
        assertTrue(output.indexOf("Item B") < output.indexOf("Item A"));
    }
}
