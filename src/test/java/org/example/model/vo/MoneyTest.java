package org.example.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Money Value Object Tests")
class MoneyTest {

    @Test
    @DisplayName("Should create valid Money with standardized scale of 2 decimal places")
    void shouldCreateValidMoneyWithStandardScale() {
        Money m1 = new Money(new BigDecimal("100"), "IRR");
        Money m2 = Money.of(2500, "toman");

        assertEquals(new BigDecimal("100.00"), m1.amount());
        assertEquals("IRR", m1.currency());

        assertEquals(new BigDecimal("2500.00"), m2.amount());
        assertEquals("TOMAN", m2.currency(), "Currency should be normalized to uppercase");
    }

    @Test
    @DisplayName("Should verify ZERO constant properties")
    void shouldVerifyZeroConstant() {
        assertEquals(new BigDecimal("0.00"), Money.ZERO.amount());
        assertEquals("IRR", Money.ZERO.currency());
        assertFalse(Money.ZERO.isNegative());
    }

    @Test
    @DisplayName("Should reject null amount or null currency")
    void shouldRejectNullValues() {
        assertThrows(NullPointerException.class, () -> new Money(null, "IRR"));
        assertThrows(NullPointerException.class, () -> new Money(BigDecimal.TEN, null));
    }

    @Test
    @DisplayName("Should reject blank or whitespace-only currency")
    void shouldRejectBlankCurrency() {
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.TEN, ""));
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.TEN, "   "));
    }

    @Test
    @DisplayName("Should reject negative monetary amounts")
    void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Money(new BigDecimal("-1.00"), "IRR")
        );
    }

    @Test
    @DisplayName("Should correctly add same-currency Money without modifying originals")
    void shouldAddMoneyCorrectly() {
        Money m100 = Money.ofTomans(100);
        Money m250 = Money.ofTomans(250);

        Money sum = m100.add(m250);

        assertEquals(new BigDecimal("350.00"), sum.amount());
        assertEquals("TOMAN", sum.currency());
        assertEquals(new BigDecimal("100.00"), m100.amount(), "Original m100 must be unchanged");
    }

    @Test
    @DisplayName("Should reject adding different currencies with IllegalArgumentException")
    void shouldRejectAdditionOfMismatchedCurrencies() {
        Money tomans = Money.ofTomans(100);
        Money rials = Money.ofRials(1000);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> tomans.add(rials)
        );
        assertTrue(ex.getMessage().contains("Currency mismatch"));
    }

    @Test
    @DisplayName("Should correctly subtract smaller amount from larger amount")
    void shouldSubtractMoneyCorrectly() {
        Money m500 = Money.ofTomans(500);
        Money m200 = Money.ofTomans(200);

        Money diff = m500.subtract(m200);

        assertEquals(new BigDecimal("300.00"), diff.amount());
        assertEquals("TOMAN", diff.currency());
    }

    @Test
    @DisplayName("Should reject subtraction if other is greater than this")
    void shouldRejectSubtractionWhenInsufficient() {
        Money m100 = Money.ofTomans(100);
        Money m200 = Money.ofTomans(200);

        assertThrows(IllegalArgumentException.class, () -> m100.subtract(m200));
    }

    @Test
    @DisplayName("Should correctly multiply Money by Quantity")
    void shouldMultiplyMoneyByQuantity() {
        Money unitPrice = Money.ofTomans(15000);
        Quantity qty = Quantity.of(3);

        Money total = unitPrice.multiply(qty);

        assertEquals(new BigDecimal("45000.00"), total.amount());
        assertEquals("TOMAN", total.currency());
    }

    @Test
    @DisplayName("Should compare Money values using isLessThan correctly")
    void shouldCompareMoneyValues() {
        Money cheap = Money.ofTomans(1000);
        Money expensive = Money.ofTomans(5000);

        assertTrue(cheap.isLessThan(expensive));
        assertFalse(expensive.isLessThan(cheap));
        assertFalse(cheap.isLessThan(cheap));
    }

    @Test
    @DisplayName("Should verify structural equality and hashCode for Money")
    void shouldVerifyStructuralEquality() {
        Money a = Money.ofTomans(500);
        Money b = new Money(new BigDecimal("500.00"), "TOMAN");
        Money c = Money.ofTomans(600);
        Money d = Money.ofRials(500);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, d, "Different currencies must not be equal");
    }
}
