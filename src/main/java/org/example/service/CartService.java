package org.example.service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Cart;
import org.example.model.entity.Product;
import org.example.model.vo.Quantity;

/**
 * Application service coordinating customer shopping carts with the product catalog and warehouse inventory.
 *
 * <p>Specification compliance:
 * <ul>
 *   <li>Conforms strictly to Section 5.4 and Figure 2 of domain-model-specification.</li>
 *   <li>Case A: Implements the exact 5 approved public methods without inventing unapproved APIs.</li>
 *   <li>Validates available warehouse stock before admitting items into carts.</li>
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

        Product product = productService.getProductById(targetProductId);
        if (!product.isActive()) {
            throw new InvalidInputException("Product '" + targetProductId + "' is currently inactive and cannot be purchased");
        }

        Cart cart = getCart(targetUserId);
        int currentQty = cart.quantityOf(targetProductId);
        int totalRequested = currentQty + quantity.value();

        if (!product.isAvailable(new Quantity(totalRequested))) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + product.getName() + "': available "
                            + product.getAvailableStock() + ", total requested " + totalRequested
            );
        }

        cart.addProduct(product, quantity);
    }

    /**
     * Updates the quantity of a specific product line item in the user's cart.
     * If the new quantity is zero or less, the item is removed from the cart.
     *
     * @param userId      unique identifier of the customer
     * @param productId   unique identifier of the product
     * @param newQuantity the updated quantity desired
     * @throws InvalidInputException      if inputs are null or blank
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

        if (newQuantity.value() <= 0) {
            cart.removeProduct(targetProductId);
            return;
        }

        Product product = productService.getProductById(targetProductId);
        if (!product.isAvailable(newQuantity)) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + product.getName() + "': available "
                            + product.getAvailableStock() + ", requested " + newQuantity.value()
            );
        }

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
}
