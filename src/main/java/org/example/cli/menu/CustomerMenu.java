package org.example.cli.menu;

import java.util.NoSuchElementException;
import java.util.Objects;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.cli.menu.customer.CartMenuHandler;
import org.example.cli.menu.customer.CheckoutMenuHandler;
import org.example.cli.menu.customer.OrderHistoryMenuHandler;
import org.example.cli.menu.customer.ProfileMenuHandler;
import org.example.cli.menu.customer.ReviewMenuHandler;
import org.example.exception.ShopException;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;

/**
 * Lean presentation coordinator for the authenticated customer dashboard (SRP, OCP, LSP, DIP).
 */
public class CustomerMenu implements AuthenticatedMenu {

    private final CatalogMenu catalogMenu;
    private final CartMenuHandler cartHandler;
    private final CheckoutMenuHandler checkoutHandler;
    private final OrderHistoryMenuHandler orderHistoryHandler;
    private final ReviewMenuHandler reviewHandler;
    private final ProfileMenuHandler profileHandler;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs a {@code CustomerMenu} from cohesive role sub-handlers and console I/O ports.
     */
    public CustomerMenu(CatalogMenu catalogMenu,
                        CartMenuHandler cartHandler,
                        CheckoutMenuHandler checkoutHandler,
                        OrderHistoryMenuHandler orderHistoryHandler,
                        ReviewMenuHandler reviewHandler,
                        ProfileMenuHandler profileHandler,
                        InputReader in,
                        OutputWriter out) {
        this.catalogMenu = Objects.requireNonNull(catalogMenu, "catalogMenu cannot be null");
        this.cartHandler = Objects.requireNonNull(cartHandler, "cartHandler cannot be null");
        this.checkoutHandler = Objects.requireNonNull(checkoutHandler, "checkoutHandler cannot be null");
        this.orderHistoryHandler = Objects.requireNonNull(orderHistoryHandler, "orderHistoryHandler cannot be null");
        this.reviewHandler = Objects.requireNonNull(reviewHandler, "reviewHandler cannot be null");
        this.profileHandler = Objects.requireNonNull(profileHandler, "profileHandler cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    @Override
    public UserRole supportedRole() {
        return UserRole.CUSTOMER;
    }

    @Override
    public void showMenu(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        if (user.getRole() != supportedRole()) {
            out.printError("Customer access required (current role: " + user.getRole() + ").");
            return;
        }

        while (true) {
            User current = profileHandler.loadProfile(user.getId());
            out.printHeader("Customer Dashboard - Welcome, " + current.getFullName());
            out.println("1. Browse Catalog & Add to Cart");
            out.println("2. View & Manage Shopping Cart");
            out.println("3. Proceed to Checkout");
            out.println("4. My Order History & Tracking");
            out.println("5. Submit Product Review");
            out.println("6. Profile & Delivery Address Book");
            out.println("7. Search, Sort, Categories & Product Details");
            out.println("0. Log Out");
            out.printDivider();

            try {
                int choice = in.readInt("Select an option", 0, 7);
                switch (choice) {
                    case 1 -> handleBrowseAndAddToCart(current);
                    case 2 -> handleCartManagement(current);
                    case 3 -> handleCheckout(current);
                    case 4 -> handleOrderHistory(current);
                    case 5 -> handleSubmitReview(current);
                    case 6 -> handleProfileManagement(current);
                    case 7 -> catalogMenu.showMenu();
                    case 0 -> {
                        out.println("Logging out of customer session. Goodbye!");
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

    public void handleBrowseAndAddToCart(User user) {
        cartHandler.handleBrowseAndAddToCart(user);
    }

    public void handleCartManagement(User user) {
        cartHandler.handleCartManagement(user);
    }

    public void handleCheckout(User user) {
        checkoutHandler.handleCheckout(user);
    }

    public void handleOrderHistory(User user) {
        orderHistoryHandler.handleOrderHistory(user);
    }

    public void handleSubmitReview(User user) {
        reviewHandler.handleSubmitReview(user);
    }

    public void handleProfileManagement(User user) {
        profileHandler.handleProfileManagement(user);
    }
}
