package org.example.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Quantity Value Object Tests")
class QuantityTest {

    @Test
    @DisplayName("Should create valid Quantity for strictly positive integers")
    void shouldCreateValidQuantityForPositiveValues() {
        Quantity q1 = new Quantity(1);
        Quantity q5 = new Quantity(5);
        Quantity q100 = new Quantity(100);

        assertEquals(1, q1.value());
        assertEquals(5, q5.value());
        assertEquals(100, q100.value());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, -100})
    @DisplayName("Should reject non-positive values with IllegalArgumentException")
    void shouldRejectNonPositiveValues(int nonPositiveValue) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Quantity(nonPositiveValue)
        );
        assertTrue(exception.getMessage().contains("strictly positive"));
    }

    @Test
    @DisplayName("Should correctly add two quantities without modifying originals (Producer)")
    void shouldAddQuantitiesCorrectly() {
        Quantity q3 = new Quantity(3);
        Quantity q4 = new Quantity(4);

        Quantity sum = q3.add(q4);

        assertEquals(7, sum.value());
        assertEquals(3, q3.value(), "Original q3 must remain unchanged");
        assertEquals(4, q4.value(), "Original q4 must remain unchanged");
    }

    @Test
    @DisplayName("Should reject adding null quantity with NullPointerException")
    void shouldRejectAddingNullQuantity() {
        Quantity q1 = new Quantity(1);
        assertThrows(NullPointerException.class, () -> q1.add(null));
    }

    @Test
    @DisplayName("Should correctly subtract a smaller quantity from a larger one")
    void shouldSubtractQuantitiesCorrectly() {
        Quantity q10 = new Quantity(10);
        Quantity q4 = new Quantity(4);

        Quantity diff = q10.subtract(q4);

        assertEquals(6, diff.value());
        assertEquals(10, q10.value(), "Original q10 must remain unchanged");
    }

    @Test
    @DisplayName("Should reject subtraction if other is equal to or greater than this")
    void shouldRejectSubtractionResultingInNonPositive() {
        Quantity q5 = new Quantity(5);
        Quantity q5Duplicate = new Quantity(5);
        Quantity q10 = new Quantity(10);

        // Equal subtraction (results in 0) -> rejected
        assertThrows(IllegalArgumentException.class, () -> q5.subtract(q5Duplicate));

        // Greater subtraction (results in negative) -> rejected
        assertThrows(IllegalArgumentException.class, () -> q5.subtract(q10));
    }

    @Test
    @DisplayName("Should evaluate isGreaterThan correctly")
    void shouldEvaluateIsGreaterThanCorrectly() {
        Quantity q5 = new Quantity(5);
        Quantity q3 = new Quantity(3);
        Quantity q5Duplicate = new Quantity(5);

        assertTrue(q5.isGreaterThan(q3));
        assertFalse(q3.isGreaterThan(q5));
        assertFalse(q5.isGreaterThan(q5Duplicate));
    }

    @Test
    @DisplayName("Should verify structural equality and hashcode for records")
    void shouldVerifyStructuralEquality() {
        Quantity a = new Quantity(42);
        Quantity b = new Quantity(42);
        Quantity c = new Quantity(99);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
