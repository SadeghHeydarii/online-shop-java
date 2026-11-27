package org.example.cli.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.example.cli.ConsoleIO;
import org.example.model.entity.Product;
import org.example.model.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProductTablePrinter Tests")
class ProductTablePrinterTest {

    @Test
    @DisplayName("Constructor rejects null OutputWriter")
    void rejectsNullOutputWriter() {
        assertThrows(NullPointerException.class, () -> new ProductTablePrinter(null));
    }

    @Test
    @DisplayName("printProducts renders clean UTF-8 star symbol without mojibake across products")
    void printsProductTableWithUnicodeStar() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO io = new ConsoleIO(
                new ByteArrayInputStream(new byte[0]),
                new PrintStream(out, true, StandardCharsets.UTF_8)
        );
        Product product = new Product(
                "prod-1",
                "ThinkPad X1",
                "Developer laptop",
                new Money(new BigDecimal("1000000.00"), "IRR"),
                7,
                "CAT-DIGITAL",
                LocalDateTime.of(2026, 11, 25, 10, 0)
        );
        product.updateRatingStats(5);

        new ProductTablePrinter(io).printProducts(List.of(product));

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ThinkPad X1"));
        assertTrue(output.contains("5.0 \u2605 (1)"));
        assertFalse(output.contains("Γÿà"));
        assertTrue(output.contains("Total products: 1"));
    }
}
