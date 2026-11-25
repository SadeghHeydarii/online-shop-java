package org.example.cli.menu.customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.cli.view.ProductTablePrinter;
import org.example.exception.ShopException;
import org.example.model.entity.Cart;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.vo.CartItem;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.api.CartUseCase;
import org.example.service.api.CatalogQueryUseCase;

/**
 * Customer CLI handler dedicated to catalog add-to-cart and shopping cart management (SRP, ISP, DIP).
 */
public class CartMenuHandler {

    private final CartUseCase cartUseCase;
    private final CatalogQueryUseCase catalogQuery;
    private final ProductTablePrinter tablePrinter;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs a {@code CartMenuHandler}.
     *
     * @param cartUseCase  shopping cart port
     * @param catalogQuery read-only catalog query port
     * @param tablePrinter shared product table view
     * @param in           narrow console input port
     * @param out          narrow console output port
     */
    public CartMenuHandler(CartUseCase cartUseCase,
                           CatalogQueryUseCase catalogQuery,
                           ProductTablePrinter tablePrinter,
                           InputReader in,
                           OutputWriter out) {
        this.cartUseCase = Objects.requireNonNull(cartUseCase, "cartUseCase cannot be null");
        this.catalogQuery = Objects.requireNonNull(catalogQuery, "catalogQuery cannot be null");
        this.tablePrinter = Objects.requireNonNull(tablePrinter, "tablePrinter cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    public void handleBrowseAndAddToCart(User customer) {
        Objects.requireNonNull(customer, "customer cannot be null");
        out.printHeader("Active Products Catalog");
        List<Product> products = catalogQuery.getAllActiveProducts();
        if (products.isEmpty()) {
            out.printInfo("No active products available in catalog.");
            return;
        }

        tablePrinter.printProducts(products);

        boolean add = in.readBoolean("Would you like to add an item to your cart?");
        if (add) {
            String productId = in.readString("Enter Product ID");
            int qty = in.readInt("Enter Quantity", 1, 100);

            try {
                cartUseCase.addItem(customer.getId(), productId, new Quantity(qty));
                out.printSuccess("Added " + qty + " item(s) to your shopping cart!");
            } catch (ShopException | IllegalArgumentException e) {
                out.printError("Failed to add product to cart: " + e.getMessage());
            }
        }
    }

    public void handleCartManagement(User customer) {
        Objects.requireNonNull(customer, "customer cannot be null");
        while (true) {
            out.printHeader("My Shopping Cart");
            Cart cart = cartUseCase.getCart(customer.getId());
            printCart(out, cart);

            out.println();
            out.println("Cart Actions:");
            out.println("  1. Add Product by ID");
            out.println("  2. Update Item Quantity");
            out.println("  3. Remove Item from Cart");
            out.println("  4. Clear Entire Cart");
            out.println("  0. Back to Main Menu");

            int action = in.readInt("Choose cart action", 0, 4);
            switch (action) {
                case 1 -> handleAddById(customer.getId());
                case 2 -> handleUpdateQuantity(customer.getId(), cart);
                case 3 -> handleRemoveItem(customer.getId(), cart);
                case 4 -> handleClearCart(customer.getId(), cart);
                case 0 -> { return; }
                default -> out.printError("Invalid action.");
            }
        }
    }

    static void printCart(OutputWriter out, Cart cart) {
        List<CartItem> items = cart.getItems();
        if (items.isEmpty()) {
            out.printInfo("Your shopping cart is currently empty.");
            return;
        }

        List<String> headers = List.of("Product ID", "Product Name", "Unit Price", "Qty", "Line Total");
        List<List<String>> rows = new ArrayList<>();

        for (CartItem item : items) {
            String unitPrice = item.unitPrice().amount() + " " + item.unitPrice().currency();
            String lineTotal = item.getLineTotal().amount() + " " + item.getLineTotal().currency();
            rows.add(List.of(
                    item.productId(),
                    item.productName(),
                    unitPrice,
                    String.valueOf(item.quantity().value()),
                    lineTotal
            ));
        }

        out.printTable(headers, rows);
        Money subtotal = cart.calculateSubtotal();
        out.println("Cart Items Subtotal: " + subtotal.amount() + " " + subtotal.currency());
    }

    private void handleAddById(String userId) {
        String pid = in.readString("Product ID");
        int q = in.readInt("Quantity", 1, 100);
        try {
            cartUseCase.addItem(userId, pid, new Quantity(q));
            out.printSuccess("Product added to cart.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Add to cart failed: " + e.getMessage());
        }
    }

    private void handleUpdateQuantity(String userId, Cart cart) {
        if (cart.isEmpty()) {
            out.printWarning("Your cart is empty.");
            return;
        }
        String pid = in.readString("Product ID to update");
        int q = in.readInt("New Quantity (enter positive quantity)", 1, 100);
        try {
            cartUseCase.updateItemQuantity(userId, pid, new Quantity(q));
            out.printSuccess("Item quantity updated.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Update quantity failed: " + e.getMessage());
        }
    }

    private void handleRemoveItem(String userId, Cart cart) {
        if (cart.isEmpty()) {
            out.printWarning("Your cart is empty.");
            return;
        }
        String pid = in.readString("Product ID to remove");
        try {
            cartUseCase.removeItem(userId, pid);
            out.printSuccess("Item removed from cart.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Remove failed: " + e.getMessage());
        }
    }

    private void handleClearCart(String userId, Cart cart) {
        if (cart.isEmpty()) {
            out.printInfo("Cart is already empty.");
            return;
        }
        if (in.readBoolean("Are you sure you want to clear all items in your cart?")) {
            cartUseCase.clearCart(userId);
            out.printSuccess("Cart cleared successfully.");
        }
    }
}
