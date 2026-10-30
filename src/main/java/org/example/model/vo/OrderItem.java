package org.example.model.vo;

import java.util.Objects;

/**
 * Immutable value object representing a historical snapshot of an item in a placed order.
 *
 * <p>Abstraction Function:
 * AF(item) = a finalized order line snapshot for product item.productId (named item.productName),
 *            purchased at fixed unit price item.unitPrice, with item.quantity count,
 *            totalling item.lineTotal.
 *
 * <p>Representation Invariant:
 * RI: productId != null && !productId.isBlank()
 *     && productName != null && !productName.isBlank()
 *     && unitPrice != null
 *     && quantity != null
 *     && lineTotal != null
 *     && lineTotal.equals(unitPrice.multiply(quantity)).
 *
 * @param productId   The unique identifier of the product.
 * @param productName The finalized product name.
 * @param unitPrice   The finalized unit price at checkout.
 * @param quantity    The purchased quantity.
 * @param lineTotal   The total line cost (must equal unitPrice * quantity).
 */
public record OrderItem(
        String productId,
        String productName,
        Money unitPrice,
        Quantity quantity,
        Money lineTotal) {

    /**
     * Compact constructor validating non-null invariants and mathematical consistency.
     */
    public OrderItem {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(productName, "productName cannot be null");
        Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
        Objects.requireNonNull(quantity, "quantity cannot be null");
        Objects.requireNonNull(lineTotal, "lineTotal cannot be null");

        if (productId.isBlank()) {
            throw new IllegalArgumentException("productId cannot be blank");
        }
        if (productName.isBlank()) {
            throw new IllegalArgumentException("productName cannot be blank");
        }

        Money expectedTotal = unitPrice.multiply(quantity);
        if (!lineTotal.equals(expectedTotal)) {
            throw new IllegalArgumentException(
                    "lineTotal (" + lineTotal + ") must equal unitPrice * quantity ("
                            + expectedTotal + ")");
        }
    }

    /**
     * Secondary constructor computing lineTotal automatically from unitPrice and quantity.
     *
     * <p>REQUIRES: All parameters non-null, strings non-blank.
     * <br>MODIFIES: this.
     * <br>EFFECTS: Constructs an OrderItem with lineTotal = unitPrice.multiply(quantity).
     *
     * @param productId   The product ID.
     * @param productName The product name.
     * @param unitPrice   The unit price.
     * @param quantity    The purchased quantity.
     */
    public OrderItem(String productId, String productName, Money unitPrice, Quantity quantity) {
        this(
                productId,
                productName,
                unitPrice,
                quantity,
                Objects.requireNonNull(unitPrice, "unitPrice cannot be null")
                        .multiply(Objects.requireNonNull(quantity, "quantity cannot be null")));
    }
}
