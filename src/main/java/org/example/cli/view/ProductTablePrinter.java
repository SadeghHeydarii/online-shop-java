package org.example.cli.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.example.cli.io.OutputWriter;
import org.example.model.entity.Product;

/**
 * Shared console view component rendering catalog product tables without duplication (SRP, DRY, ISP).
 */
public class ProductTablePrinter {

    private final OutputWriter out;

    /**
     * Constructs a {@code ProductTablePrinter} writing tables through {@link OutputWriter}.
     *
     * @param out narrow console output port
     */
    public ProductTablePrinter(OutputWriter out) {
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    /**
     * Prints a formatted table of catalog products and a summary row count.
     *
     * @param products list of products to render
     */
    public void printProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            out.printInfo("No products found matching the criteria.");
            return;
        }

        List<String> headers = List.of("ID", "Name", "Category", "Price", "Stock", "Rating");
        List<List<String>> rows = new ArrayList<>();

        for (Product p : products) {
            String priceStr = p.getPrice().amount() + " " + p.getPrice().currency();
            String ratingStr = String.format("%.1f \u2605 (%d)", p.getAverageRating(), p.getReviewCount());
            rows.add(List.of(
                    p.getId(),
                    p.getName(),
                    p.getCategoryId(),
                    priceStr,
                    String.valueOf(p.getAvailableStock()),
                    ratingStr
            ));
        }

        out.printTable(headers, rows);
        out.println("Total products: " + products.size());
    }
}
