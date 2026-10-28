package org.example.model.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value object representing a non-negative monetary value with currency.
 *
 * <p>Abstraction Function:
 * AF(m) = a monetary value of m.amount in currency m.currency.
 *
 * <p>Representation Invariant:
 * RI: amount != null && currency != null && !currency.isBlank()
 *     && amount.scale() == 2 && amount.signum() >= 0.
 *
 * @param amount   The monetary amount, scaled to 2 decimal places.
 * @param currency The ISO or domain currency code (e.g., "IRR", "TOMAN").
 */
public record Money(BigDecimal amount, String currency) {

    /**
     * Default zero money constant in Iranian Rials ("IRR").
     */
    public static final Money ZERO = new Money(BigDecimal.ZERO, "IRR");

    /**
     * Compact constructor enforcing immutability and financial invariants.
     *
     * @param amount   The monetary amount.
     * @param currency The currency code.
     * @throws NullPointerException if amount or currency is null.
     * @throws IllegalArgumentException if currency is blank or amount is negative.
     */
    public Money {
        Objects.requireNonNull(amount, "Monetary amount cannot be null");
        Objects.requireNonNull(currency, "Currency code cannot be null");

        String normalizedCurrency = currency.trim().toUpperCase();
        if (normalizedCurrency.isBlank()) {
            throw new IllegalArgumentException("Currency code cannot be blank");
        }

        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Monetary amount cannot be negative, but received: " + amount);
        }

        // Standardize monetary scale to 2 decimal places using HALF_UP rounding
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        currency = normalizedCurrency;
    }

    /**
     * Static factory method for creating Money from a long integer amount.
     *
     * @param amount   The integer monetary amount.
     * @param currency The currency code.
     * @return A valid immutable Money instance.
     */
    public static Money of(long amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    /**
     * Static factory method for creating Money from a BigDecimal amount.
     *
     * @param amount   The BigDecimal amount.
     * @param currency The currency code.
     * @return A valid immutable Money instance.
     */
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    /**
     * Static factory method for creating Money in Tomans.
     *
     * @param amount The integer amount in Tomans.
     * @return A valid Money instance with currency "TOMAN".
     */
    public static Money ofTomans(long amount) {
        return new Money(BigDecimal.valueOf(amount), "TOMAN");
    }

    /**
     * Static factory method for creating Money in Iranian Rials.
     *
     * @param amount The integer amount in Rials.
     * @return A valid Money instance with currency "IRR".
     */
    public static Money ofRials(long amount) {
        return new Money(BigDecimal.valueOf(amount), "IRR");
    }

    /**
     * Produces a new Money object representing the sum of this and another.
     *
     * <p>REQUIRES: other != null, this.currency.equals(other.currency).
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns new Money(this.amount + other.amount, this.currency).
     *
     * @param other The money to add.
     * @return A new Money representing the combined sum.
     * @throws NullPointerException if other is null.
     * @throws IllegalArgumentException if currencies do not match.
     */
    public Money add(Money other) {
        Objects.requireNonNull(other, "Cannot add null Money");
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Produces a new Money object representing the difference (this - other).
     *
     * <p>REQUIRES: other != null, this.currency.equals(other.currency),
     *           this.amount.compareTo(other.amount) >= 0.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns new Money(this.amount - other.amount, this.currency);
     *              throws IllegalArgumentException if other > this or currencies mismatch.
     *
     * @param other The money to subtract.
     * @return A new Money representing the remaining amount.
     * @throws NullPointerException if other is null.
     * @throws IllegalArgumentException if currencies mismatch or if other > this.
     */
    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Cannot subtract null Money");
        validateSameCurrency(other);
        if (this.amount.compareTo(other.amount) < 0) {
            throw new IllegalArgumentException(
                    "Cannot subtract larger amount (" + other.amount
                            + ") from smaller amount (" + this.amount + ")");
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Multiplies this monetary amount by a discrete Quantity.
     *
     * <p>REQUIRES: quantity != null.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns new Money(this.amount * quantity.value(), this.currency).
     *
     * @param quantity The quantity multiplier.
     * @return A new Money scaled by the given quantity.
     * @throws NullPointerException if quantity is null.
     */
    public Money multiply(Quantity quantity) {
        Objects.requireNonNull(quantity, "Cannot multiply by null Quantity");
        BigDecimal multiplier = BigDecimal.valueOf(quantity.value());
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    /**
     * Compares this money value with another to determine whether it is strictly less.
     *
     * <p>REQUIRES: other != null, this.currency.equals(other.currency).
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns true if this.amount < other.amount; false otherwise.
     *
     * @param other The money to compare against.
     * @return True if this money is strictly less than other; false otherwise.
     * @throws NullPointerException if other is null.
     * @throws IllegalArgumentException if currencies mismatch.
     */
    public boolean isLessThan(Money other) {
        Objects.requireNonNull(other, "Cannot compare with null Money");
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Checks whether this monetary amount is strictly negative.
     *
     * <p>REQUIRES: none.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns true if amount.signum() < 0; false otherwise.
     *
     * @return True if negative, false otherwise (always false for valid instances).
     */
    public boolean isNegative() {
        return this.amount.signum() < 0;
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: expected " + this.currency
                            + " but received " + other.currency);
        }
    }
}
