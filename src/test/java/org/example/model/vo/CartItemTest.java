package org.example.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CartItem Value Object Tests")
class CartItemTest {

    @Test
    @DisplayName("Should create valid CartItem and compute line total accurately")
    void shouldCreateValidCartItemAndComputeLineTotal() {
        Money unitPrice = new Money(new BigDecimal("15000"), "TOMAN");
        Quantity qty = new Quantity(3);

        CartItem item = new CartItem("PROD-101", "Wireless Mouse", unitPrice, qty);

        assertEquals("PROD-101", item.productId());
        assertEquals("Wireless Mouse", item.productName());
        assertEquals(unitPrice, item.unitPrice());
        assertEquals(qty, item.quantity());

        Money expectedTotal = new Money(new BigDecimal("45000"), "TOMAN");
        assertEquals(expectedTotal, item.getLineTotal());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject blank or null productId")
    void shouldRejectInvalidProductId(String invalidId) {
        Money price = new Money(BigDecimal.TEN, "IRR");
        Quantity qty = new Quantity(1);
        assertThrows(RuntimeException.class, () -> new CartItem(invalidId, "Product", price, qty));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject blank or null productName")
    void shouldRejectInvalidProductName(String invalidName) {
        Money price = new Money(BigDecimal.TEN, "IRR");
        Quantity qty = new Quantity(1);
        assertThrows(RuntimeException.class, () -> new CartItem("PROD-1", invalidName, price, qty));
    }

    @Test
    @DisplayName("Should reject null unitPrice or null quantity")
    void shouldRejectNullPriceOrQuantity() {
        Money price = new Money(BigDecimal.TEN, "IRR");
        Quantity qty = new Quantity(1);

        assertThrows(NullPointerException.class, () -> new CartItem("PROD-1", "Product", null, qty));
        assertThrows(NullPointerException.class, () -> new CartItem("PROD-1", "Product", price, null));
    }

    @Test
    @DisplayName("Should produce a new CartItem instance with updated quantity (withQuantity)")
    void shouldProduceNewCartItemWithUpdatedQuantity() {
        Money price = new Money(new BigDecimal("20000"), "TOMAN");
        CartItem original = new CartItem("PROD-1", "Keyboard", price, new Quantity(1));

        CartItem updated = original.withQuantity(new Quantity(4));

        assertEquals(new Quantity(1), original.quantity(), "Original quantity must remain 1");
        assertEquals(new Quantity(4), updated.quantity(), "Updated quantity must be 4");
        assertEquals(original.productId(), updated.productId());
        assertEquals(original.productName(), updated.productName());
        assertEquals(original.unitPrice(), updated.unitPrice());
        assertEquals(new Money(new BigDecimal("80000"), "TOMAN"), updated.getLineTotal());
    }

    @Test
    @DisplayName("Should reject withQuantity with null parameter")
    void shouldRejectWithQuantityNull() {
        CartItem item = new CartItem(
                "PROD-1", "Keyboard", new Money(BigDecimal.TEN, "IRR"), new Quantity(1));
        assertThrows(NullPointerException.class, () -> item.withQuantity(null));
    }

    @Test
    @DisplayName("Should verify structural equality and hashCode for CartItem")
    void shouldVerifyStructuralEquality() {
        Money p1 = new Money(new BigDecimal("100"), "IRR");
        Quantity q1 = new Quantity(2);

        CartItem a = new CartItem("P-1", "Item", p1, q1);
        CartItem b = new CartItem("P-1", "Item", p1, q1);
        CartItem c = new CartItem("P-2", "Item", p1, q1);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
