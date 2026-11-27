package org.example.service.api;

import org.example.model.entity.Cart;
import org.example.model.vo.Quantity;

/**
 * Customer-facing shopping cart management API (ISP).
 */
public interface CartUseCase {

    /**
     * Retrieves or creates the active cart owned by {@code userId}.
     *
     * @param userId non-blank customer identifier
     * @return customer's {@link Cart}
     */
    Cart getCart(String userId);

    /**
     * Adds {@code quantity} of a product to the customer's cart after validating stock availability.
     *
     * @param userId    non-blank customer identifier
     * @param productId non-blank product identifier
     * @param quantity  positive quantity to add
     */
    void addItem(String userId, String productId, Quantity quantity);

    /**
     * Updates the quantity of an existing cart line item, removing it if {@code newQuantity} is zero or less.
     *
     * @param userId      non-blank customer identifier
     * @param productId   non-blank product identifier
     * @param newQuantity updated desired quantity
     */
    void updateItemQuantity(String userId, String productId, Quantity newQuantity);

    /**
     * Removes a product line item from the customer's cart.
     *
     * @param userId    non-blank customer identifier
     * @param productId non-blank product identifier
     */
    void removeItem(String userId, String productId);

    /**
     * Removes all items from the customer's cart.
     *
     * @param userId non-blank customer identifier
     */
    void clearCart(String userId);
}
