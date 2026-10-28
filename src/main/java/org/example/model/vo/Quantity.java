package org.example.model.vo;

import java.util.Objects;

/**
 * Immutable value object representing a strictly positive discrete quantity of items.
 *
 * <p>Abstraction Function:
 * AF(q) = a strictly positive integer item count equal to q.value.
 *
 * <p>Representation Invariant:
 * RI: value > 0.
 *
 * @param value The underlying strictly positive integer count.
 */
public record Quantity(int value) {

    /**
     * Compact constructor enforcing the representation invariant (value > 0).
     *
     * @param value The discrete count to validate.
     * @throws IllegalArgumentException if value is less than or equal to zero.
     */
    public Quantity {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be strictly positive, but received: " + value);
        }
    }

    /**
     * Static factory method for creating a Quantity instance.
     *
     * @param value The discrete count.
     * @return A valid immutable Quantity instance.
     * @throws IllegalArgumentException if value <= 0.
     */
    public static Quantity of(int value) {
        return new Quantity(value);
    }

    /**
     * Produces a new Quantity representing the sum of this quantity and another.
     *
     * <p>REQUIRES: other != null.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns new Quantity(this.value + other.value).
     *
     * @param other The quantity to add.
     * @return A new Quantity with the combined total.
     * @throws NullPointerException if other is null.
     */
    public Quantity add(Quantity other) {
        Objects.requireNonNull(other, "Cannot add null Quantity");
        return new Quantity(this.value + other.value);
    }

    /**
     * Produces a new Quantity representing the difference (this - other).
     *
     * <p>REQUIRES: other != null and this.value > other.value.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns new Quantity(this.value - other.value);
     *              throws IllegalArgumentException if other.value >= this.value.
     *
     * @param other The quantity to subtract.
     * @return A new Quantity with the subtracted count.
     * @throws NullPointerException if other is null.
     * @throws IllegalArgumentException if other is greater than or equal to this.
     */
    public Quantity subtract(Quantity other) {
        Objects.requireNonNull(other, "Cannot subtract null Quantity");
        if (other.value >= this.value) {
            throw new IllegalArgumentException(
                    "Subtraction would result in non-positive quantity: "
                            + this.value + " - " + other.value);
        }
        return new Quantity(this.value - other.value);
    }

    /**
     * Compares this quantity with another to determine whether it is strictly greater.
     *
     * <p>REQUIRES: other != null.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns true if this.value > other.value; false otherwise.
     *
     * @param other The quantity to compare against.
     * @return True if this quantity is strictly greater than other; false otherwise.
     * @throws NullPointerException if other is null.
     */
    public boolean isGreaterThan(Quantity other) {
        Objects.requireNonNull(other, "Cannot compare with null Quantity");
        return this.value > other.value;
    }
}
