package org.example.model.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Immutable value object representing a promotional discount coupon.
 *
 * <p>Abstraction Function:
 * AF(c) = a discount voucher with code c.code offering c.discountPercent% discount,
 *         capped at c.maxDiscount, applicable on orders of at least c.minOrderAmount,
 *         expiring at the end of c.expiryDate.
 *
 * <p>Representation Invariant:
 * RI: code != null && !code.isBlank()
 *     && discountPercent > 0 && discountPercent <= 100
 *     && maxDiscount != null
 *     && minOrderAmount != null
 *     && maxDiscount.currency().equals(minOrderAmount.currency())
 *     && expiryDate != null.
 *
 * @param code            The promotional coupon code (e.g., "WELCOME20").
 * @param discountPercent The percentage discount (1 to 100).
 * @param maxDiscount     The maximum discount cap.
 * @param minOrderAmount  The minimum order subtotal required to qualify.
 * @param expiryDate      The expiration date.
 */
public record Coupon(
        String code,
        int discountPercent,
        Money maxDiscount,
        Money minOrderAmount,
        LocalDate expiryDate) {

    /**
     * Compact constructor enforcing coupon invariants.
     */
    public Coupon {
        Objects.requireNonNull(code, "code cannot be null");
        Objects.requireNonNull(maxDiscount, "maxDiscount cannot be null");
        Objects.requireNonNull(minOrderAmount, "minOrderAmount cannot be null");
        Objects.requireNonNull(expiryDate, "expiryDate cannot be null");

        code = code.trim().toUpperCase();
        if (code.isBlank()) {
            throw new IllegalArgumentException("code cannot be blank");
        }
        if (discountPercent <= 0 || discountPercent > 100) {
            throw new IllegalArgumentException(
                    "discountPercent must be between 1 and 100, but received: " + discountPercent);
        }
        if (!maxDiscount.currency().equals(minOrderAmount.currency())) {
            throw new IllegalArgumentException(
                    "Currency mismatch between maxDiscount (" + maxDiscount.currency()
                            + ") and minOrderAmount (" + minOrderAmount.currency() + ")");
        }
    }

    /**
     * Calculates the applicable monetary discount for a given order subtotal.
     *
     * <p>REQUIRES: subtotal != null, subtotal.currency().equals(maxDiscount.currency()).
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: If subtotal < minOrderAmount, returns Money.ZERO with the subtotal currency.
     *              Otherwise, returns min(subtotal * discountPercent / 100, maxDiscount).
     *
     * @param subtotal The order items subtotal.
     * @return The calculated discount Money.
     * @throws NullPointerException if subtotal is null.
     * @throws IllegalArgumentException if currencies mismatch.
     */
    public Money calculateDiscount(Money subtotal) {
        Objects.requireNonNull(subtotal, "subtotal cannot be null");
        if (!subtotal.currency().equals(maxDiscount.currency())) {
            throw new IllegalArgumentException(
                    "Currency mismatch: expected " + maxDiscount.currency()
                            + " but received " + subtotal.currency());
        }

        if (subtotal.isLessThan(minOrderAmount)) {
            return new Money(BigDecimal.ZERO, subtotal.currency());
        }

        BigDecimal percentage = BigDecimal.valueOf(discountPercent);
        BigDecimal rawDiscount = subtotal.amount()
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Money calculatedDiscount = new Money(rawDiscount, subtotal.currency());
        if (maxDiscount.isLessThan(calculatedDiscount)) {
            return maxDiscount;
        }
        return calculatedDiscount;
    }

    /**
     * Checks whether this coupon is valid for the given order subtotal and date.
     *
     * <p>REQUIRES: none.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns true if subtotal is non-null, currency matches,
     *              currentDate is non-null and not after expiryDate,
     *              and subtotal is not less than minOrderAmount; false otherwise.
     *
     * @param subtotal    The order items subtotal.
     * @param currentDate The current evaluation date.
     * @return True if the coupon is currently valid for this subtotal; false otherwise.
     */
    public boolean isValid(Money subtotal, LocalDate currentDate) {
        if (subtotal == null || currentDate == null) {
            return false;
        }
        if (!subtotal.currency().equals(maxDiscount.currency())) {
            return false;
        }
        if (currentDate.isAfter(expiryDate)) {
            return false;
        }
        return !subtotal.isLessThan(minOrderAmount);
    }
}
