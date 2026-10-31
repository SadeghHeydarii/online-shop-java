package org.example.cli.menu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.example.cli.ConsoleIO;
import org.example.exception.CategoryNotFoundException;
import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.Review;
import org.example.service.ProductService;
import org.example.service.ReviewService;

/**
 * Reusable console presentation controller encapsulating product catalog exploration workflows.
 *
 * <p>Provides standardized interactions for product catalog browsing, keyword search,
 * category and price range faceted filtering, sorting, category hierarchy inspection,
 * and comprehensive product details with customer reviews. Shared across {@link GuestMenu}
 * and {@link CustomerMenu}.
 */
public class CatalogMenu {

    private final ProductService productService;
    private final ReviewService reviewService;
    private final ConsoleIO io;

    /**
     * Constructs a {@code CatalogMenu} with injected product catalog and review services.
     *
     * @param productService the product catalog service
     * @param reviewService  the customer reviews service (may be null if reviews unavailable)
     * @param io             the console I/O helper
     */
    public CatalogMenu(ProductService productService, ReviewService reviewService, ConsoleIO io) {
        this.productService = Objects.requireNonNull(productService, "productService cannot be null");
        this.reviewService = reviewService;
        this.io = Objects.requireNonNull(io, "io cannot be null");
    }

    /**
     * Displays the interactive catalog tools menu loop.
     */
    public void showMenu() {
        while (true) {
            io.printHeader("Catalog Tools");
            io.println("1. Browse Products");
            io.println("2. Search Products");
            io.println("3. Filter & Sort Products");
            io.println("4. Product Details & Reviews");
            io.println("5. Browse Categories");
            io.println("0. Back");
            try {
                switch (io.readInt("Select an option", 0, 5)) {
                    case 1 -> handleBrowseProducts();
                    case 2 -> handleSearchProducts();
                    case 3 -> handleFilterAndSort();
                    case 4 -> handleViewProductDetails();
                    case 5 -> handleBrowseCategories();
                    case 0 -> { return; }
                    default -> io.printError("Invalid option");
                }
            } catch (NoSuchElementException e) {
                return;
            } catch (IllegalArgumentException | IllegalStateException e) {
                io.printError(e.getMessage());
            }
        }
    }

    /**
     * Handles displaying all active products in the storefront catalog.
     */
    public void handleBrowseProducts() {
        io.printHeader("Active Products Catalog");
        List<Product> products = productService.getAllActiveProducts();
        displayProductTable(products);
    }

    /**
     * Handles searching active products by name or description keywords.
     */
    public void handleSearchProducts() {
        io.printHeader("Search Products");
        String keyword = io.readString("Enter search keyword");
        try {
            displayProductTable(productService.searchProducts(keyword));
        } catch (InvalidInputException e) {
            io.printError(e.getMessage());
        }
    }

    /**
     * Handles filtering active products by category, price bounds, and sort order.
     */
    public void handleFilterAndSort() {
        io.printHeader("Filter and Sort Products");
        String categoryId = io.readLine("Category ID (leave blank for any)");
        if (categoryId.isBlank()) {
            categoryId = null;
        }

        BigDecimal minPrice = null;
        String minRaw = io.readLine("Minimum Price in IRR (leave blank for none)");
        if (!minRaw.isBlank()) {
            try {
                minPrice = new BigDecimal(minRaw);
            } catch (NumberFormatException e) {
                io.printWarning("Invalid minimum price format. Ignoring minimum price filter.");
            }
        }

        BigDecimal maxPrice = null;
        String maxRaw = io.readLine("Maximum Price in IRR (leave blank for none)");
        if (!maxRaw.isBlank()) {
            try {
                maxPrice = new BigDecimal(maxRaw);
            } catch (NumberFormatException e) {
                io.printWarning("Invalid maximum price format. Ignoring maximum price filter.");
            }
        }

        io.println("Sort options:");
        io.println("  1. Default (None)");
        io.println("  2. Price: Low to High (price_asc)");
        io.println("  3. Price: High to Low (price_desc)");
        io.println("  4. Rating: Highest First (rating)");
        io.println("  5. Newest First (newest)");
        io.println("  6. Most Popular (popular)");
        int sortChoice = io.readInt("Choose sort option", 1, 6);
        String sortBy = switch (sortChoice) {
            case 2 -> "price_asc";
            case 3 -> "price_desc";
            case 4 -> "rating";
            case 5 -> "newest";
            case 6 -> "popular";
            default -> null;
        };

        try {
            Money minMoney = minPrice != null ? new Money(minPrice, "IRR") : null;
            Money maxMoney = maxPrice != null ? new Money(maxPrice, "IRR") : null;
            List<Product> results = productService.filterAndSort(categoryId, minMoney, maxMoney, sortBy);
            displayProductTable(results);
        } catch (CategoryNotFoundException | IllegalArgumentException e) {
            io.printError("Invalid filter criteria: " + e.getMessage());
        }
    }

    /**
     * Handles viewing full product details, image URLs, and submitted customer reviews.
     */
    public void handleViewProductDetails() {
        io.printHeader("Product Details & Customer Reviews");
        String productId = io.readString("Enter Product ID");

        try {
            Product product = productService.getProductById(productId);
            io.println();
            io.println("ID:             " + product.getId());
            io.println("Name:           " + product.getName());
            io.println("Description:    " + product.getDescription());
            io.println("Category ID:    " + product.getCategoryId());
            io.println("Price:          " + product.getPrice().amount() + " " + product.getPrice().currency());
            io.println("Stock:          " + product.getAvailableStock() + " units available");
            io.println("Status:         " + (product.isActive() ? "ACTIVE" : "INACTIVE"));
            io.println("Images:         " + (product.getImageUrls().isEmpty() ? "No images" : String.join(", ", product.getImageUrls())));
            io.println("Average Rating: " + String.format("%.1f ★ (%d reviews)", product.getAverageRating(), product.getReviewCount()));

            if (reviewService != null) {
                io.println();
                io.printHeader("Customer Reviews for: " + product.getName());
                List<Review> reviews = reviewService.getProductReviews(product.getId());
                if (reviews.isEmpty()) {
                    io.printInfo("No customer reviews for this product yet.");
                } else {
                    for (int i = 0; i < reviews.size(); i++) {
                        Review r = reviews.get(i);
                        io.println((i + 1) + ". Rating: " + "★".repeat(r.rating()) + " (" + r.rating() + "/5)"
                                + " | Author: " + r.authorName()
                                + " | Date: " + r.createdAt().toLocalDate());
                        io.println("   Feedback: " + r.commentText());
                        io.println();
                    }
                }
            }
        } catch (ProductNotFoundException e) {
            io.printError("Product not found: " + e.getMessage());
        }
    }

    /**
     * Formats and prints a tabular listing of catalog products to the console.
     *
     * @param products list of products to display
     */
    public void displayProductTable(List<Product> products) {
        if (products == null || products.isEmpty()) {
            io.printInfo("No products found matching the criteria.");
            return;
        }

        List<String> headers = List.of("ID", "Name", "Category", "Price", "Stock", "Rating");
        List<List<String>> rows = new ArrayList<>();

        for (Product p : products) {
            String priceStr = p.getPrice().amount() + " " + p.getPrice().currency();
            String ratingStr = String.format("%.1f ★ (%d)", p.getAverageRating(), p.getReviewCount());
            rows.add(List.of(
                    p.getId(),
                    p.getName(),
                    p.getCategoryId(),
                    priceStr,
                    String.valueOf(p.getAvailableStock()),
                    ratingStr
            ));
        }

        io.printTable(headers, rows);
        io.println("Total products: " + products.size());
    }

    /**
     * Handles displaying all active product categories and browsing products within them.
     */
    public void handleBrowseCategories() {
        io.printHeader("Active Categories");
        List<Category> categories = productService.getActiveCategories();
        if (categories.isEmpty()) {
            io.printInfo("No active categories available");
            return;
        }
        List<List<String>> rows = categories.stream()
                .map(category -> List.of(category.id(), category.name(), category.description())).toList();
        io.printTable(List.of("ID", "Name", "Description"), rows);
        String categoryId = io.readLine("Category ID to browse (leave blank to go back)");
        if (!categoryId.isBlank()) {
            try {
                displayProductTable(productService.getProductsByCategory(categoryId));
            } catch (CategoryNotFoundException | InvalidInputException e) {
                io.printError(e.getMessage());
            }
        }
    }
}
