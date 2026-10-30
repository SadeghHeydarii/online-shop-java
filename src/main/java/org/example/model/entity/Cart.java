package org.example.model.entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.example.model.vo.CartItem;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;

/**
 * Represents a customer's shopping cart entity and aggregate root.
 *
 * <p>A shopping cart holds line items ({@link CartItem}) accumulated by a customer
 * during a shopping session, keyed by product identifier. It embodies the domain association
 * {@code User "1" --> "0..1" Cart : owns}, recording the owning customer's identifier
 * ({@code userId}). It provides domain operations to add items, update quantities, remove
 * items, clear contents, compute total value, and query line count and emptiness.
 *
 * <p><b>Abstraction Function (AF):</b>
 * AF(cartId, userId, items) = A mutable shopping cart identified by {@code cartId}
 * belonging to customer {@code userId}, containing the active collection of product line
 * items mapped in {@code items}.
 *
 * <p><b>Representation Invariant (RI):</b>
 * <ul>
 *   <li>{@code cartId != null && !cartId.isBlank()}</li>
 *   <li>{@code userId != null && !userId.isBlank()}</li>
 *   <li>{@code items != null}</li>
 *   <li>For every entry in {@code items}: key and value are non-null, and
 *       {@code entry.getKey().equals(entry.getValue().productId())}</li>
 *   <li>Every cart item has {@code quantity().value() > 0}</li>
 * </ul>
 *
 * <p><b>Safety from Representation Exposure:</b>
 * The internal {@link Map} representation is completely encapsulated.
 * {@link #getItems()} returns an unmodifiable defensive list copy via
 * {@link List#copyOf(java.util.Collection)}.
 */
public class Cart {

    private final String cartId;
    private final String userId;
    private final Map<String, CartItem> items;

    /**
     * Constructs a new empty {@code Cart} entity with explicit cart and owner identifiers.
     *
     * @param cartId the unique identifier of the cart
     * @param userId the unique identifier of the customer who owns this cart
     * @throws IllegalArgumentException if cartId or userId is null or blank
     */
    public Cart(String cartId, String userId) {
        if (cartId == null || cartId.isBlank()) {
            throw new IllegalArgumentException("Cart ID cannot be null or blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        this.cartId = cartId.trim();
        this.userId = userId.trim();
        this.items = new LinkedHashMap<>();
        checkRep();
    }

    /**
     * Constructs a new empty {@code Cart} entity where the cart identifier defaults to the user identifier.
     *
     * @param userId the unique identifier of the owning customer
     * @throws IllegalArgumentException if userId is null or blank
     */
    public Cart(String userId) {
        this(userId, userId);
    }

    /**
     * Asserts the representation invariant of the Cart entity.
     */
    private void checkRep() {
        assert cartId != null && !cartId.isBlank() : "cartId cannot be null or blank";
        assert userId != null && !userId.isBlank() : "userId cannot be null or blank";
        assert items != null : "items map cannot be null";
        for (Map.Entry<String, CartItem> entry : items.entrySet()) {
            assert entry.getKey() != null : "null map key in cart items";
            assert entry.getValue() != null : "null cart item in cart items";
            assert entry.getKey().equals(entry.getValue().productId())
                    : "map key (" + entry.getKey() + ") must match item productId ("
                    + entry.getValue().productId() + ")";
            assert entry.getValue().quantity().value() > 0
                    : "cart item quantity must be positive";
        }
    }

    /**
     * Returns the unique identifier of this cart.
     *
     * @return cart identifier
     */
    public String getCartId() {
        return cartId;
    }

    /**
     * Returns the unique identifier of the customer who owns this cart.
     *
     * @return owning customer user identifier
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Adds a product and quantity to the cart.
     *
     * <p>If the product is already present in the cart, the added quantity is
     * accumulated onto the existing line item. Otherwise, a new cart item is created.
     *
     * @param product  the product to add
     * @param quantity the quantity of the product to add
     * @throws IllegalArgumentException if product or quantity is null
     */
    public void addProduct(Product product, Quantity quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }

        String productId = product.getId();
        if (items.containsKey(productId)) {
            CartItem existing = items.get(productId);
            Quantity updatedQuantity = existing.quantity().add(quantity);
            items.put(productId, existing.withQuantity(updatedQuantity));
        } else {
            CartItem newItem = new CartItem(productId, product.getName(), product.getPrice(), quantity);
            items.put(productId, newItem);
        }
        checkRep();
    }

    /**
     * Updates the quantity of a product currently present in the cart.
     *
     * @param productId   the identifier of the product to update
     * @param newQuantity the new desired quantity (must be positive)
     * @throws IllegalArgumentException if productId is null/blank, newQuantity is null,
     *                                  or the product is not in the cart
     */
    public void updateQuantity(String productId, Quantity newQuantity) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
        if (newQuantity == null) {
            throw new IllegalArgumentException("New quantity cannot be null");
        }
        if (!items.containsKey(productId)) {
            throw new IllegalArgumentException("Product with ID " + productId + " not found in cart");
        }

        CartItem existing = items.get(productId);
        items.put(productId, existing.withQuantity(newQuantity));
        checkRep();
    }

    /**
     * Removes a product line item from the cart if present.
     *
     * @param productId the identifier of the product to remove
     * @throws IllegalArgumentException if productId is null or blank
     */
    public void removeProduct(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
        items.remove(productId);
        checkRep();
    }

    /**
     * Clears all items from the shopping cart.
     */
    public void clear() {
        items.clear();
        checkRep();
    }

    /**
     * Returns the quantity of the specified product currently in the cart.
     *
     * @param productId the product identifier
     * @return the quantity count if present; 0 otherwise
     */
    public int quantityOf(String productId) {
        if (productId == null || !items.containsKey(productId)) {
            return 0;
        }
        return items.get(productId).quantity().value();
    }

    /**
     * Calculates the subtotal monetary amount of all items currently in the cart.
     *
     * @return sum of line totals, or {@link Money#ZERO} if the cart is empty
     */
    public Money calculateSubtotal() {
        if (items.isEmpty()) {
            return Money.ZERO;
        }
        Money total = null;
        for (CartItem item : items.values()) {
            if (total == null) {
                total = item.getLineTotal();
            } else {
                total = total.add(item.getLineTotal());
            }
        }
        return total;
    }

    /**
     * Checks whether the cart currently contains no items.
     *
     * @return {@code true} if empty; {@code false} otherwise
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns an unmodifiable snapshot of the line items currently in the cart.
     *
     * @return unmodifiable list of cart items
     */
    public List<CartItem> getItems() {
        return List.copyOf(items.values());
    }

    /**
     * Compares this cart to another object for equality based on entity identity ({@code cartId}).
     *
     * @param o the object to compare with
     * @return {@code true} if both are Cart entities with identical IDs; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cart other)) {
            return false;
        }
        return this.cartId.equals(other.cartId);
    }

    /**
     * Returns the hash code based on entity identity ({@code cartId}).
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(cartId);
    }

    @Override
    public String toString() {
        return "Cart[cartId=" + cartId
                + ", userId=" + userId
                + ", itemCount=" + items.size()
                + ", subtotal=" + calculateSubtotal()
                + "]";
    }
}
