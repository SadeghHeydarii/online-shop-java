package org.example.model.vo;

import java.util.Objects;

/**
 * Immutable value object representing a single line item in a customer's shopping cart.
 *
 * <p>Abstraction Function:
 * AF(item) = a shopping cart line item referencing product item.productId (named item.productName),
 *            priced at unit price item.unitPrice, with requested item.quantity items.
 *
 * <p>Representation Invariant:
 * RI: productId != null && !productId.isBlank()
 *     && productName != null && !productName.isBlank()
 *     && unitPrice != null
 *     && quantity != null.
 *
 * @param productId   The unique identifier of the product.
 * @param productName The name of the product at the time of addition.
 * @param unitPrice   The unit price of the product.
 * @param quantity    The requested quantity.
 */
public record CartItem(
        String productId,
        String productName,
        Money unitPrice,
        Quantity quantity) {

    /**
     * Compact constructor validating non-null and non-blank invariants.
     */
    public CartItem {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(productName, "productName cannot be null");
        Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
        Objects.requireNonNull(quantity, "quantity cannot be null");

        if (productId.isBlank()) {
            throw new IllegalArgumentException("productId cannot be blank");
        }
        if (productName.isBlank()) {
            throw new IllegalArgumentException("productName cannot be blank");
        }
    }

    /**
     * Calculates the total monetary value of this cart line item (unitPrice * quantity).
     *
     * <p>REQUIRES: none.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns unitPrice.multiply(quantity).
     *
     * @return The total price for this line item.
     */
    public Money getLineTotal() {
        return unitPrice.multiply(quantity);
    }

    /**
     * Produces a new CartItem instance with an updated quantity.
     *
     * <p>REQUIRES: newQuantity != null.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns a new CartItem with the new quantity.
     *
     * @param newQuantity The new quantity.
     * @return A new CartItem instance with the updated quantity.
     * @throws NullPointerException if newQuantity is null.
     */
    public CartItem withQuantity(Quantity newQuantity) {
        Objects.requireNonNull(newQuantity, "newQuantity cannot be null");
        return new CartItem(this.productId, this.productName, this.unitPrice, newQuantity);
    }
}
