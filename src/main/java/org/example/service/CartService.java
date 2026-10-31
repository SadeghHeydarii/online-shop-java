package org.example.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Cart;
import org.example.model.entity.Product;
import org.example.model.vo.CartItem;
import org.example.model.vo.Quantity;

/**
 * Application service coordinating customer shopping carts with the product catalog and warehouse inventory.
 *
 * <p>Specification compliance:
 * <ul>
 *   <li>Conforms strictly to Section 5.4 and Figure 2 of domain-model-specification.</li>
 *   <li>Validates available warehouse stock before admitting items into carts.</li>
 *   <li>Supports selective removal of checked-out items upon successful checkout orchestration.</li>
 * </ul>
 */
public class CartService {

    private final Map<String, Cart> cartStore;
    private final ProductService productService;

    /**
     * Constructs a CartService with new concurrent storage and injected product service.
     *
     * @param productService product service for inventory and price validation
     */
    public CartService(ProductService productService) {
        this(new ConcurrentHashMap<>(), productService);
    }

    /**
     * Parameterized constructor enabling dependency injection for testing.
     *
     * @param cartStore      in-memory store mapping user IDs to their Cart aggregates
     * @param productService product catalog service
     */
    public CartService(Map<String, Cart> cartStore, ProductService productService) {
        this.cartStore = Objects.requireNonNull(cartStore, "cartStore cannot be null");
        this.productService = Objects.requireNonNull(productService, "productService cannot be null");
    }

    /**
     * Retrieves the active shopping cart for the specified user, creating a new empty cart if none exists.
     *
     * @param userId unique identifier of the customer
     * @return the user's mutable {@link Cart} aggregate
     * @throws InvalidInputException if {@code userId} is null or blank
     */
    public Cart getCart(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be null or blank");
        }
        String normalizedUserId = userId.trim();
        Cart cart = cartStore.computeIfAbsent(normalizedUserId, uid -> new Cart(uid, uid));
        if (!normalizedUserId.equals(cart.getUserId())) {
            throw new IllegalStateException("Cart ownership invariant violated: expected owner "
                    + normalizedUserId + " but found " + cart.getUserId());
        }
        return cart;
    }

    /**
     * Adds a quantity of a product to the user's cart after validating stock availability.
     *
     * <p>Contract:
     * <ul>
     *   <li>REQUIRES: userId != null, productId exists and active, quantity != null and positive</li>
     *   <li>MODIFIES: cartStore</li>
     *   <li>EFFECTS: Validates stock, adds to Cart; throws {@link InsufficientStockException} if unavailable.</li>
     * </ul>
     *
     * @param userId    unique identifier of the customer
     * @param productId unique identifier of the product to add
     * @param quantity  number of units to add
     * @throws InvalidInputException       if any argument violates basic input validation
     * @throws InsufficientStockException  if the warehouse stock cannot fulfill the cumulative requested quantity
     */
    public void addItem(String userId, String productId, Quantity quantity) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be null or blank");
        }
        if (productId == null || productId.isBlank()) {
            throw new InvalidInputException("Product ID cannot be null or blank");
        }
        if (quantity == null || quantity.value() <= 0) {
            throw new InvalidInputException("Quantity must be non-null and positive");
        }

        String targetUserId = userId.trim();
        String targetProductId = productId.trim();

        Cart cart = getCart(targetUserId);
        int currentQty = cart.quantityOf(targetProductId);
        int totalRequested = currentQty + quantity.value();

        Product product = productService.requireAvailableProduct(targetProductId, new Quantity(totalRequested));
        cart.addProduct(product, quantity);
    }

    /**
     * Updates the quantity of a specific product line item in the user's cart.
     * If the new quantity is zero or less, the item is removed from the cart.
     *
     * @param userId      unique identifier of the customer
     * @param productId   unique identifier of the product
     * @param newQuantity the updated quantity desired
     * @throws InvalidInputException      if inputs are null or blank, or product not present in cart
     * @throws InsufficientStockException if the requested new quantity exceeds available warehouse stock
     */
    public void updateItemQuantity(String userId, String productId, Quantity newQuantity) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be null or blank");
        }
        if (productId == null || productId.isBlank()) {
            throw new InvalidInputException("Product ID cannot be null or blank");
        }
        if (newQuantity == null) {
            throw new InvalidInputException("Quantity cannot be null");
        }

        String targetUserId = userId.trim();
        String targetProductId = productId.trim();
        Cart cart = getCart(targetUserId);

        if (cart.quantityOf(targetProductId) == 0) {
            throw new InvalidInputException("Product is not present in this cart");
        }

        if (newQuantity.value() <= 0) {
            cart.removeProduct(targetProductId);
            return;
        }

        productService.requireAvailableProduct(targetProductId, newQuantity);
        cart.updateQuantity(targetProductId, newQuantity);
    }

    /**
     * Removes a product line item entirely from the user's cart.
     *
     * @param userId    unique identifier of the customer
     * @param productId unique identifier of the product to remove
     * @throws InvalidInputException if inputs are null or blank
     */
    public void removeItem(String userId, String productId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be null or blank");
        }
        if (productId == null || productId.isBlank()) {
            throw new InvalidInputException("Product ID cannot be null or blank");
        }
        Cart cart = getCart(userId.trim());
        if (cart.quantityOf(productId.trim()) == 0) {
            throw new InvalidInputException("Product is not present in this cart");
        }
        cart.removeProduct(productId.trim());
    }

    /**
     * Clears all items from the user's cart.
     *
     * @param userId unique identifier of the customer
     * @throws InvalidInputException if userId is null or blank
     */
    public void clearCart(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be null or blank");
        }
        Cart cart = getCart(userId.trim());
        cart.clear();
    }

    /**
     * Selectively removes checked-out snapshot items from the user's cart.
     *
     * @param userId   unique identifier of the customer
     * @param snapshot the list of checked-out cart items to remove
     * @throws InvalidInputException if userId is null or blank
     * @throws NullPointerException  if snapshot is null
     */
    public void removeCheckedOutItems(String userId, List<CartItem> snapshot) {
        getCart(userId).removeCheckedOutItems(snapshot);
    }
}
