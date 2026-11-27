package org.example.cli.menu.admin;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.cli.view.ProductTablePrinter;
import org.example.exception.ShopException;
import org.example.model.entity.Product;
import org.example.model.vo.Money;
import org.example.service.api.CatalogAdminUseCase;
import org.example.service.api.ProductLookupPort;

/**
 * Admin CLI handler dedicated to catalog product CRUD, stock, and image management (SRP, ISP, DIP).
 */
public class AdminCatalogHandler {

    private final CatalogAdminUseCase catalogAdmin;
    private final ProductLookupPort productLookup;
    private final ProductTablePrinter tablePrinter;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs an {@code AdminCatalogHandler}.
     *
     * @param catalogAdmin  catalog mutation port
     * @param productLookup narrow read-by-ID port
     * @param tablePrinter  shared product table renderer
     * @param in            narrow console input port
     * @param out           narrow console output port
     */
    public AdminCatalogHandler(CatalogAdminUseCase catalogAdmin,
                               ProductLookupPort productLookup,
                               ProductTablePrinter tablePrinter,
                               InputReader in,
                               OutputWriter out) {
        this.catalogAdmin = Objects.requireNonNull(catalogAdmin, "catalogAdmin cannot be null");
        this.productLookup = Objects.requireNonNull(productLookup, "productLookup cannot be null");
        this.tablePrinter = Objects.requireNonNull(tablePrinter, "tablePrinter cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    public void handleViewAllProducts() {
        out.printHeader("Catalog Products List");
        List<Product> products = catalogAdmin.getAllProducts();
        if (products.isEmpty()) {
            out.printInfo("No products currently in catalog.");
            return;
        }
        tablePrinter.printProducts(products);
    }

    public void handleAddProduct() {
        out.printHeader("Add New Product to Catalog (US 33)");
        String name = in.readString("Product Name");
        String description = in.readString("Product Description");
        BigDecimal priceAmount = in.readBigDecimal("Price (in IRR)");
        int initialStock = in.readInt("Initial Stock Quantity", 0, 100000);
        String categoryId = in.readString("Category ID");

        try {
            Money price = new Money(priceAmount, "IRR");
            catalogAdmin.addProduct(name, description, price, initialStock, categoryId);
            out.printSuccess("Product '" + name + "' successfully created and added to catalog!");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Failed to add product: " + e.getMessage());
        }
    }

    public void handleEditProduct() {
        out.printHeader("Update Product Specifications (US 34)");
        String productId = in.readString("Enter Product ID to update");

        try {
            Product current = productLookup.getProductById(productId);
            out.println("Current Name:        " + current.getName());
            out.println("Current Description: " + current.getDescription());
            out.println("Current Price:       " + current.getPrice().amount() + " " + current.getPrice().currency());
            out.println("Current Category:    " + current.getCategoryId());
            out.printDivider();

            String newName = readOrDefault("New Name (leave blank to keep '" + current.getName() + "')", current.getName());
            String newDesc = readOrDefault("New Description (leave blank to keep current)", current.getDescription());
            Money newPrice = parsePriceOrDefault(
                    "New Price in IRR (leave blank to keep " + current.getPrice().amount() + ")",
                    current.getPrice());
            String newCatId = readOrDefault("New Category ID (leave blank to keep '" + current.getCategoryId() + "')", current.getCategoryId());

            catalogAdmin.updateProduct(productId, newName, newDesc, newPrice, newCatId);
            out.printSuccess("Product '" + productId + "' updated successfully.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Product update failed: " + e.getMessage());
        }
    }

    public void handleDeactivateProduct() {
        out.printHeader("Deactivate Product from Catalog (US 35)");
        String productId = in.readString("Enter Product ID to deactivate");

        try {
            Product current = productLookup.getProductById(productId);
            if (!current.isActive()) {
                out.printInfo("Product '" + current.getName() + "' is already inactive.");
                return;
            }

            boolean confirm = in.readBoolean("Are you sure you want to deactivate '" + current.getName() + "'?");
            if (confirm) {
                catalogAdmin.deactivateProduct(productId);
                out.printSuccess("Product '" + current.getName() + "' has been deactivated.");
            }
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Product not found: " + e.getMessage());
        }
    }

    public void handleManageStock() {
        out.printHeader("Manage Inventory & Restock (US 36)");
        String productId = in.readString("Enter Product ID");

        try {
            Product current = productLookup.getProductById(productId);
            out.println("Product:         " + current.getName());
            out.println("Current Stock:   " + current.getAvailableStock() + " units");
            out.println("Status:          " + (current.isActive() ? "ACTIVE" : "INACTIVE"));
            out.printDivider();

            out.println("Inventory Action:");
            out.println("  1. Restock (Add Quantity)");
            out.println("  2. Deduct (Remove Quantity)");
            out.println("  0. Cancel");

            int action = in.readInt("Select action", 0, 2);
            if (action == 0) {
                return;
            }

            int amount = in.readInt("Enter quantity", 1, 100000);
            int delta = (action == 1) ? amount : -amount;

            catalogAdmin.adjustStock(productId, delta);
            Product updated = productLookup.getProductById(productId);
            out.printSuccess("Stock adjusted successfully! New available stock: " + updated.getAvailableStock() + " units.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Inventory adjustment failed: " + e.getMessage());
        }
    }

    public void handleProductImages() {
        String productId = in.readString("Product ID");
        String raw = in.readLine("Image URLs separated by commas (blank clears images)");
        List<String> urls = raw.isBlank() ? List.of()
                : Arrays.stream(raw.split(",")).map(String::trim).toList();
        try {
            catalogAdmin.setProductImages(productId, urls);
            out.printSuccess("Product images updated.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Image update failed: " + e.getMessage());
        }
    }

    private String readOrDefault(String prompt, String fallback) {
        String raw = in.readLine(prompt);
        return raw.isBlank() ? fallback : raw;
    }

    private Money parsePriceOrDefault(String prompt, Money currentPrice) {
        String priceStr = in.readLine(prompt);
        if (priceStr.isBlank()) {
            return currentPrice;
        }
        try {
            return new Money(new BigDecimal(priceStr), "IRR");
        } catch (NumberFormatException e) {
            out.printWarning("Invalid price format. Keeping existing price.");
            return currentPrice;
        }
    }
}
