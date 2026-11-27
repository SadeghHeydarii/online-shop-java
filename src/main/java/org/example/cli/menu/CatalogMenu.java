package org.example.cli.menu;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.cli.io.TerminalIO;
import org.example.cli.view.ProductTablePrinter;
import org.example.exception.ShopException;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.Review;
import org.example.service.api.CatalogQueryUseCase;
import org.example.service.api.ReviewUseCase;

/**
 * Reusable console presentation controller encapsulating product catalog exploration workflows (SRP, OCP, DIP).
 */
public class CatalogMenu {

    private static final Map<String, String> SORT_DISPLAY_LABELS = Map.of(
            "price_asc", "Price: Low to High (price_asc)",
            "price_desc", "Price: High to Low (price_desc)",
            "rating", "Rating: Highest First (rating)",
            "newest", "Newest First (newest)",
            "popular", "Most Popular (popular)"
    );

    private final CatalogQueryUseCase catalogQuery;
    private final ReviewUseCase reviewUseCase;
    private final ProductTablePrinter tablePrinter;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs a {@code CatalogMenu} with segregated catalog/review ports and console views.
     *
     * @param catalogQuery catalog browsing and filtering use-case port
     * @param reviewUseCase optional customer reviews use-case port (may be null)
     * @param tablePrinter shared product table view renderer
     * @param in           narrow input reader port
     * @param out          narrow output writer port
     */
    public CatalogMenu(CatalogQueryUseCase catalogQuery,
                       ReviewUseCase reviewUseCase,
                       ProductTablePrinter tablePrinter,
                       InputReader in,
                       OutputWriter out) {
        this.catalogQuery = Objects.requireNonNull(catalogQuery, "catalogQuery cannot be null");
        this.reviewUseCase = reviewUseCase;
        this.tablePrinter = Objects.requireNonNull(tablePrinter, "tablePrinter cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    /**
     * Convenience constructor accepting a unified {@link TerminalIO}.
     */
    public CatalogMenu(CatalogQueryUseCase catalogQuery,
                       ReviewUseCase reviewUseCase,
                       ProductTablePrinter tablePrinter,
                       TerminalIO io) {
        this(catalogQuery, reviewUseCase, tablePrinter,
                Objects.requireNonNull(io, "io cannot be null"), io);
    }

    /**
     * Displays the interactive catalog tools menu loop.
     */
    public void showMenu() {
        while (true) {
            out.printHeader("Catalog Tools");
            out.println("1. Browse Products");
            out.println("2. Search Products");
            out.println("3. Filter & Sort Products");
            out.println("4. Product Details & Reviews");
            out.println("5. Browse Categories");
            out.println("0. Back");
            try {
                switch (in.readInt("Select an option", 0, 5)) {
                    case 1 -> handleBrowseProducts();
                    case 2 -> handleSearchProducts();
                    case 3 -> handleFilterAndSort();
                    case 4 -> handleViewProductDetails();
                    case 5 -> handleBrowseCategories();
                    case 0 -> { return; }
                    default -> out.printError("Invalid option");
                }
            } catch (NoSuchElementException e) {
                return;
            } catch (ShopException | IllegalArgumentException e) {
                out.printError(e.getMessage());
            }
        }
    }

    /**
     * Handles displaying all active products in the storefront catalog.
     */
    public void handleBrowseProducts() {
        out.printHeader("Active Products Catalog");
        tablePrinter.printProducts(catalogQuery.getAllActiveProducts());
    }

    /**
     * Handles searching active products by name or description keywords.
     */
    public void handleSearchProducts() {
        out.printHeader("Search Products");
        String keyword = in.readString("Enter search keyword");
        try {
            tablePrinter.printProducts(catalogQuery.searchProducts(keyword));
        } catch (ShopException | IllegalArgumentException e) {
            out.printError(e.getMessage());
        }
    }

    /**
     * Handles filtering active products by category, price bounds, and registered dynamic sort options.
     */
    public void handleFilterAndSort() {
        out.printHeader("Filter and Sort Products");
        String categoryId = in.readLine("Category ID (leave blank for any)");
        if (categoryId.isBlank()) {
            categoryId = null;
        }

        BigDecimal minPrice = parseOptionalPrice("Minimum Price in IRR (leave blank for none)", "minimum");
        BigDecimal maxPrice = parseOptionalPrice("Maximum Price in IRR (leave blank for none)", "maximum");

        List<String> sortOptions = catalogQuery.getAvailableSortOptions();
        out.println("Sort options:");
        out.println("  1. Default (None)");
        for (int i = 0; i < sortOptions.size(); i++) {
            String key = sortOptions.get(i);
            out.println("  " + (i + 2) + ". " + SORT_DISPLAY_LABELS.getOrDefault(key, key));
        }

        int sortChoice = in.readInt("Choose sort option", 1, sortOptions.size() + 1);
        String sortBy = sortChoice == 1 ? null : sortOptions.get(sortChoice - 2);

        try {
            Money minMoney = minPrice != null ? new Money(minPrice, "IRR") : null;
            Money maxMoney = maxPrice != null ? new Money(maxPrice, "IRR") : null;
            List<Product> results = catalogQuery.filterAndSort(categoryId, minMoney, maxMoney, sortBy);
            tablePrinter.printProducts(results);
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Invalid filter criteria: " + e.getMessage());
        }
    }

    /**
     * Handles viewing full product details, image URLs, and submitted customer reviews.
     */
    public void handleViewProductDetails() {
        out.printHeader("Product Details & Customer Reviews");
        String productId = in.readString("Enter Product ID");

        try {
            Product product = catalogQuery.getProductById(productId);
            out.println();
            out.println("ID:             " + product.getId());
            out.println("Name:           " + product.getName());
            out.println("Description:    " + product.getDescription());
            out.println("Category ID:    " + product.getCategoryId());
            out.println("Price:          " + product.getPrice().amount() + " " + product.getPrice().currency());
            out.println("Stock:          " + product.getAvailableStock() + " units available");
            out.println("Status:         " + (product.isActive() ? "ACTIVE" : "INACTIVE"));
            out.println("Images:         " + (product.getImageUrls().isEmpty() ? "No images" : String.join(", ", product.getImageUrls())));
            out.println("Average Rating: " + String.format("%.1f \u2605 (%d reviews)", product.getAverageRating(), product.getReviewCount()));

            if (reviewUseCase != null) {
                out.println();
                out.printHeader("Customer Reviews for: " + product.getName());
                List<Review> reviews = reviewUseCase.getProductReviews(product.getId());
                if (reviews.isEmpty()) {
                    out.printInfo("No customer reviews for this product yet.");
                } else {
                    for (int i = 0; i < reviews.size(); i++) {
                        Review r = reviews.get(i);
                        out.println((i + 1) + ". Rating: " + "\u2605".repeat(r.rating()) + " (" + r.rating() + "/5)"
                                + " | Author: " + r.authorName()
                                + " | Date: " + r.createdAt().toLocalDate());
                        out.println("   Feedback: " + r.commentText());
                        out.println();
                    }
                }
            }
        } catch (ShopException e) {
            out.printError("Product not found: " + e.getMessage());
        }
    }

    /**
     * Handles displaying all active product categories and browsing products within them.
     */
    public void handleBrowseCategories() {
        out.printHeader("Active Categories");
        List<Category> categories = catalogQuery.getActiveCategories();
        if (categories.isEmpty()) {
            out.printInfo("No active categories available");
            return;
        }
        List<List<String>> rows = categories.stream()
                .map(category -> List.of(category.id(), category.name(), category.description())).toList();
        out.printTable(List.of("ID", "Name", "Description"), rows);
        String categoryId = in.readLine("Category ID to browse (leave blank to go back)");
        if (!categoryId.isBlank()) {
            try {
                tablePrinter.printProducts(catalogQuery.getProductsByCategory(categoryId));
            } catch (ShopException | IllegalArgumentException e) {
                out.printError(e.getMessage());
            }
        }
    }

    private BigDecimal parseOptionalPrice(String prompt, String boundLabel) {
        String raw = in.readLine(prompt);
        if (raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            out.printWarning("Invalid " + boundLabel + " price format. Ignoring " + boundLabel + " price filter.");
            return null;
        }
    }
}
