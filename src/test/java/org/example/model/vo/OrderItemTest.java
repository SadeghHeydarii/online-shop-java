package org.example.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderItem Value Object Tests")
class OrderItemTest {

    @Test
    @DisplayName("Should create valid OrderItem with secondary 4-arg constructor")
    void shouldCreateValidOrderItemWithSecondaryConstructor() {
        Money unitPrice = new Money(new BigDecimal("50000"), "TOMAN");
        Quantity qty = new Quantity(2);

        OrderItem item = new OrderItem("PROD-99", "USB Cable", unitPrice, qty);

        assertEquals("PROD-99", item.productId());
        assertEquals("USB Cable", item.productName());
        assertEquals(unitPrice, item.unitPrice());
        assertEquals(qty, item.quantity());

        Money expectedLineTotal = new Money(new BigDecimal("100000"), "TOMAN");
        assertEquals(expectedLineTotal, item.lineTotal());
    }

    @Test
    @DisplayName("Should create valid OrderItem with canonical 5-arg constructor when RI holds")
    void shouldCreateValidOrderItemWithCanonicalConstructor() {
        Money unitPrice = new Money(new BigDecimal("25000"), "TOMAN");
        Quantity qty = new Quantity(4);
        Money lineTotal = new Money(new BigDecimal("100000"), "TOMAN");

        OrderItem item = new OrderItem("PROD-99", "USB Cable", unitPrice, qty, lineTotal);

        assertEquals("PROD-99", item.productId());
        assertEquals(lineTotal, item.lineTotal());
    }

    @Test
    @DisplayName("Should reject canonical construction if lineTotal != unitPrice * quantity")
    void shouldRejectInconsistentLineTotal() {
        Money unitPrice = new Money(new BigDecimal("25000"), "TOMAN");
        Quantity qty = new Quantity(4);
        Money mismatchedLineTotal = new Money(new BigDecimal("99999"), "TOMAN");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new OrderItem("PROD-99", "USB Cable", unitPrice, qty, mismatchedLineTotal)
        );
        assertTrue(ex.getMessage().contains("must equal unitPrice * quantity"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject blank or null productId")
    void shouldRejectInvalidProductId(String invalidId) {
        Money price = new Money(BigDecimal.TEN, "IRR");
        Quantity qty = new Quantity(1);
        assertThrows(RuntimeException.class, () -> new OrderItem(invalidId, "Product", price, qty));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject blank or null productName")
    void shouldRejectInvalidProductName(String invalidName) {
        Money price = new Money(BigDecimal.TEN, "IRR");
        Quantity qty = new Quantity(1);
        assertThrows(RuntimeException.class, () -> new OrderItem("PROD-1", invalidName, price, qty));
    }

    @Test
    @DisplayName("Should reject null unitPrice or null quantity")
    void shouldRejectNullPriceOrQuantity() {
        Money price = new Money(BigDecimal.TEN, "IRR");
        Quantity qty = new Quantity(1);

        assertThrows(NullPointerException.class, () -> new OrderItem("PROD-1", "Product", null, qty));
        assertThrows(NullPointerException.class, () -> new OrderItem("PROD-1", "Product", price, null));
    }

    @Test
    @DisplayName("Should verify structural equality and hashCode for OrderItem")
    void shouldVerifyStructuralEquality() {
        Money price = new Money(new BigDecimal("100"), "IRR");
        Quantity qty = new Quantity(2);

        OrderItem a = new OrderItem("P-1", "Item", price, qty);
        OrderItem b = new OrderItem("P-1", "Item", price, qty);
        OrderItem c = new OrderItem("P-2", "Item", price, qty);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
