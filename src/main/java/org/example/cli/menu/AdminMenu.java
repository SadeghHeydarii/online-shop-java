package org.example.cli.menu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.example.cli.ConsoleIO;
import org.example.exception.CategoryNotFoundException;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.OrderNotFoundException;
import org.example.exception.ProductNotFoundException;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.vo.Money;
import org.example.service.OrderService;
import org.example.service.ProductService;

/**
 * Interactive console menu for administrators and storekeepers.
 *
 * <p>Coordinates catalog maintenance, inventory management, and order fulfillment workflows
 * specified in Section 6.4 and Figure 3 of the authoritative domain specification, covering
 * User Stories US 29, US 33 through US 36:
 * <ul>
 *   <li>US 33: Creating new products in the catalog.</li>
 *   <li>US 34: Updating existing product specifications, pricing, or categories.</li>
 *   <li>US 35: Deactivating obsolete products from the active catalog.</li>
 *   <li>US 36: Restocking and adjusting warehouse inventory levels.</li>
 *   <li>US 29: Order fulfillment and status advancement (PROCESSING, SHIPPED, DELIVERED).</li>
 * </ul>
 */
public class AdminMenu {

    private final ProductService productService;
    private final OrderService orderService;
    private final ConsoleIO io;

    /**
     * Constructs an {@code AdminMenu} with injected product catalog and order fulfillment services.
     *
     * @param productService catalog management and inventory service
     * @param orderService   order status and fulfillment service
     * @param io             console input/output utility
     */
    public AdminMenu(ProductService productService, OrderService orderService, ConsoleIO io) {
        this.productService = Objects.requireNonNull(productService, "productService cannot be null");
        this.orderService = Objects.requireNonNull(orderService, "orderService cannot be null");
        this.io = Objects.requireNonNull(io, "io cannot be null");
    }

    /**
     * Runs the interactive Admin Menu loop for the authenticated administrator/storekeeper until logout.
     *
     * @param admin the authenticated admin user
     */
    public void showMenu(User admin) {
        Objects.requireNonNull(admin, "Admin user cannot be null");

        while (true) {
            io.printHeader("Administration & Warehouse Panel - " + admin.getFullName() + " [" + admin.getRole() + "]");
            io.println("1. View Active Products");
            io.println("2. Add New Product to Catalog");
            io.println("3. Update Product Specifications");
            io.println("4. Deactivate Product");
            io.println("5. Manage Inventory & Restock");
            io.println("6. Inspect & Advance Order Status");
            io.println("0. Log Out");
            io.printDivider();

            try {
                int choice = io.readInt("Select an option", 0, 6);
                switch (choice) {
                    case 1 -> handleViewAllProducts();
                    case 2 -> handleAddProduct();
                    case 3 -> handleEditProduct();
                    case 4 -> handleDeactivateProduct();
                    case 5 -> handleManageStock();
                    case 6 -> handleProcessOrders();
                    case 0 -> {
                        io.println("Logging out of administrative session. Goodbye!");
                        return;
                    }
                    default -> io.printError("Invalid option selected.");
                }
            } catch (NoSuchElementException e) {
                // End of input stream reached (e.g. pipe or automated test)
                io.println();
                return;
            }
        }
    }

    // =========================================================================
    // 1. VIEW ACTIVE PRODUCTS
    // =========================================================================

    public void handleViewAllProducts() {
        io.printHeader("Catalog Products List");
        List<Product> products = productService.getAllActiveProducts();
        if (products.isEmpty()) {
            io.printInfo("No active products currently in catalog.");
            return;
        }

        displayProductTable(products);
    }

    // =========================================================================
    // 2. ADD PRODUCT (US 33)
    // =========================================================================

    public void handleAddProduct() {
        io.printHeader("Add New Product to Catalog (US 33)");
        String name = io.readString("Product Name");
        String description = io.readString("Description");
        BigDecimal priceAmount = io.readBigDecimal("Price (in IRR)");
        int initialStock = io.readInt("Initial Stock Quantity", 0, 100000);
        String categoryId = io.readString("Category ID");

        try {
            Money price = new Money(priceAmount, "IRR");
            productService.addProduct(name, description, price, initialStock, categoryId);
            io.printSuccess("Product '" + name + "' successfully created and added to catalog!");
        } catch (InvalidInputException | CategoryNotFoundException e) {
            io.printError("Failed to add product: " + e.getMessage());
        }
    }

    // =========================================================================
    // 3. EDIT PRODUCT (US 34)
    // =========================================================================

    public void handleEditProduct() {
        io.printHeader("Update Product Specifications (US 34)");
        String productId = io.readString("Enter Product ID to update");

        try {
            Product current = productService.getProductById(productId);
            io.println("Current Name:        " + current.getName());
            io.println("Current Description: " + current.getDescription());
            io.println("Current Price:       " + current.getPrice().amount() + " " + current.getPrice().currency());
            io.println("Current Category:    " + current.getCategoryId());
            io.printDivider();

            String newName = io.readLine("New Name (leave blank to keep '" + current.getName() + "')");
            if (newName.isBlank()) {
                newName = current.getName();
            }

            String newDesc = io.readLine("New Description (leave blank to keep current)");
            if (newDesc.isBlank()) {
                newDesc = current.getDescription();
            }

            String priceStr = io.readLine("New Price in IRR (leave blank to keep " + current.getPrice().amount() + ")");
            Money newPrice = current.getPrice();
            if (!priceStr.isBlank()) {
                try {
                    newPrice = new Money(new BigDecimal(priceStr), "IRR");
                } catch (NumberFormatException e) {
                    io.printWarning("Invalid price format. Keeping existing price.");
                }
            }

            String newCatId = io.readLine("New Category ID (leave blank to keep '" + current.getCategoryId() + "')");
            if (newCatId.isBlank()) {
                newCatId = current.getCategoryId();
            }

            productService.updateProduct(productId, newName, newDesc, newPrice, newCatId);
            io.printSuccess("Product '" + productId + "' updated successfully.");
        } catch (ProductNotFoundException | CategoryNotFoundException | InvalidInputException e) {
            io.printError("Product update failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 4. DEACTIVATE PRODUCT (US 35)
    // =========================================================================

    public void handleDeactivateProduct() {
        io.printHeader("Deactivate Product from Catalog (US 35)");
        String productId = io.readString("Enter Product ID to deactivate");

        try {
            Product current = productService.getProductById(productId);
            if (!current.isActive()) {
                io.printInfo("Product '" + current.getName() + "' is already inactive.");
                return;
            }

            boolean confirm = io.readBoolean("Are you sure you want to deactivate '" + current.getName() + "'?");
            if (confirm) {
                productService.deactivateProduct(productId);
                io.printSuccess("Product '" + current.getName() + "' has been deactivated.");
            }
        } catch (ProductNotFoundException e) {
            io.printError("Product not found: " + e.getMessage());
        }
    }

    // =========================================================================
    // 5. MANAGE INVENTORY STOCK (US 36)
    // =========================================================================

    public void handleManageStock() {
        io.printHeader("Warehouse Inventory Adjustment (US 36)");
        String productId = io.readString("Enter Product ID");

        try {
            Product product = productService.getProductById(productId);
            int currentStock = product.getAvailableStock();
            io.println("Product:         " + product.getName());
            io.println("Available Stock: " + currentStock + " units");
            io.printDivider();

            io.println("Adjustment options:");
            io.println("  1. Restock (Add inventory)");
            io.println("  2. Deduct (Reduce inventory)");
            int opt = io.readInt("Choose option", 1, 2);

            int delta;
            if (opt == 1) {
                int addQty = io.readInt("Quantity to add", 1, 10000);
                delta = addQty;
            } else {
                if (currentStock == 0) {
                    io.printWarning("Stock is already 0. Cannot deduct further.");
                    return;
                }
                int deductQty = io.readInt("Quantity to deduct", 1, currentStock);
                delta = -deductQty;
            }

            productService.adjustStock(productId, delta);
            Product updated = productService.getProductById(productId);
            io.printSuccess("Stock adjusted successfully! New available stock: " + updated.getAvailableStock() + " units.");
        } catch (ProductNotFoundException | InvalidInputException e) {
            io.printError("Inventory adjustment failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 6. PROCESS CUSTOMER ORDERS (US 29)
    // =========================================================================

    public void handleProcessOrders() {
        io.printHeader("Order Fulfillment & Status Advancement (US 29)");
        String orderId = io.readString("Enter Order ID to process");

        try {
            Order order = orderService.getOrderDetails(orderId);
            io.println("Order ID:     " + order.getOrderId());
            io.println("User ID:      " + order.getUserId());
            io.println("Current Status: " + order.getStatus());
            io.println("Total Amount: " + order.getPayableAmount().amount() + " " + order.getPayableAmount().currency());
            io.printDivider();

            io.println("Available Transitions:");
            io.println("  1. Advance to PROCESSING");
            io.println("  2. Advance to SHIPPED");
            io.println("  3. Advance to DELIVERED");
            io.println("  4. Cancel Order");
            io.println("  0. Back");

            int choice = io.readInt("Choose action", 0, 4);
            switch (choice) {
                case 1 -> {
                    orderService.updateOrderStatus(orderId, OrderStatus.PROCESSING);
                    io.printSuccess("Order #" + orderId + " is now in PROCESSING state.");
                }
                case 2 -> {
                    orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED);
                    io.printSuccess("Order #" + orderId + " is now SHIPPED.");
                }
                case 3 -> {
                    orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);
                    io.printSuccess("Order #" + orderId + " marked as DELIVERED.");
                }
                case 4 -> {
                    orderService.cancelOrder(orderId);
                    io.printSuccess("Order #" + orderId + " has been CANCELLED.");
                }
                case 0 -> {
                    return;
                }
                default -> io.printError("Invalid action.");
            }
        } catch (OrderNotFoundException | InvalidOrderStateException | InvalidInputException e) {
            io.printError("Order processing error: " + e.getMessage());
        }
    }

    private void displayProductTable(List<Product> products) {
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
    }
}
