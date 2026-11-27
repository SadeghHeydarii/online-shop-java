package org.example.cli.menu;

import java.util.NoSuchElementException;
import java.util.Objects;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.cli.menu.admin.AdminCatalogHandler;
import org.example.cli.menu.admin.AdminOrderHandler;
import org.example.exception.ShopException;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;

/**
 * Lean administrative dashboard coordinator dispatching catalog and order handlers (SRP, OCP, LSP, DIP).
 */
public class AdminMenu implements AuthenticatedMenu {

    private final AdminCatalogHandler catalogHandler;
    private final AdminOrderHandler orderHandler;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs an {@code AdminMenu} with decomposed catalog and order handlers.
     *
     * @param catalogHandler admin catalog/stock/image handler
     * @param orderHandler   admin order fulfillment handler
     * @param in             narrow console input port
     * @param out            narrow console output port
     */
    public AdminMenu(AdminCatalogHandler catalogHandler,
                     AdminOrderHandler orderHandler,
                     InputReader in,
                     OutputWriter out) {
        this.catalogHandler = Objects.requireNonNull(catalogHandler, "catalogHandler cannot be null");
        this.orderHandler = Objects.requireNonNull(orderHandler, "orderHandler cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    @Override
    public UserRole supportedRole() {
        return UserRole.ADMIN;
    }

    @Override
    public void showMenu(User admin) {
        Objects.requireNonNull(admin, "Admin user cannot be null");

        if (admin.getRole() != supportedRole()) {
            out.printError("Administrator access required");
            return;
        }

        while (true) {
            out.printHeader("Administration & Warehouse Panel - " + admin.getFullName() + " [" + admin.getRole() + "]");
            out.println("1. View All Products");
            out.println("2. Add New Product to Catalog");
            out.println("3. Update Product Specifications");
            out.println("4. Deactivate Product");
            out.println("5. Manage Inventory & Restock");
            out.println("6. Inspect & Advance Order Status");
            out.println("7. Manage Product Images");
            out.println("8. List Orders / Filter by Status");
            out.println("0. Log Out");
            out.printDivider();

            try {
                int choice = in.readInt("Select an option", 0, 8);
                switch (choice) {
                    case 1 -> handleViewAllProducts();
                    case 2 -> handleAddProduct();
                    case 3 -> handleEditProduct();
                    case 4 -> handleDeactivateProduct();
                    case 5 -> handleManageStock();
                    case 6 -> handleProcessOrders();
                    case 7 -> handleProductImages();
                    case 8 -> handleListOrders();
                    case 0 -> {
                        out.println("Logging out of administrative session. Goodbye!");
                        return;
                    }
                    default -> out.printError("Invalid option selected.");
                }
            } catch (NoSuchElementException e) {
                out.println();
                return;
            } catch (ShopException | IllegalArgumentException e) {
                out.printError(e.getMessage());
            }
        }
    }

    public void handleViewAllProducts() {
        catalogHandler.handleViewAllProducts();
    }

    public void handleAddProduct() {
        catalogHandler.handleAddProduct();
    }

    public void handleEditProduct() {
        catalogHandler.handleEditProduct();
    }

    public void handleDeactivateProduct() {
        catalogHandler.handleDeactivateProduct();
    }

    public void handleManageStock() {
        catalogHandler.handleManageStock();
    }

    public void handleProcessOrders() {
        orderHandler.handleProcessOrders();
    }

    public void handleProductImages() {
        catalogHandler.handleProductImages();
    }

    public void handleListOrders() {
        orderHandler.handleListOrders();
    }
}
