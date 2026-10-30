package org.example.model.entity;

import java.math.BigDecimal;
import java.util.List;
import org.example.model.vo.CartItem;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartTest {

    private Cart cart;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        cart = new Cart("cart-usr-1");
        product1 = new Product("prod-1", "Gaming Mouse", "Ergonomic gaming mouse",
                new Money(new BigDecimal("50000.00"), "IRR"), 20, "cat-accessories");
        product2 = new Product("prod-2", "Mechanical Keyboard", "RGB mechanical keyboard",
                new Money(new BigDecimal("150000.00"), "IRR"), 10, "cat-accessories");
    }

    @Test
    @DisplayName("Cart constructor initializes empty cart with valid identifier")
    void testValidCartCreation() {
        assertEquals("cart-usr-1", cart.getCartId());
        assertTrue(cart.isEmpty());
        assertEquals(0, cart.getItems().size());
        assertEquals(0, cart.quantityOf("prod-1"));
        assertEquals(Money.ZERO, cart.calculateSubtotal());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Cart constructor rejects null or blank cartId")
    void testInvalidCartId(String invalidId) {
        assertThrows(IllegalArgumentException.class, () -> new Cart(invalidId));
    }

    @Test
    @DisplayName("addProduct() adds new line item and calculates subtotal correctly")
    void testAddSingleProduct() {
        cart.addProduct(product1, new Quantity(2));

        assertFalse(cart.isEmpty());
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.quantityOf("prod-1"));
        assertEquals(new Money(new BigDecimal("100000.00"), "IRR"), cart.calculateSubtotal());

        CartItem item = cart.getItems().get(0);
        assertEquals("prod-1", item.productId());
        assertEquals("Gaming Mouse", item.productName());
        assertEquals(new Quantity(2), item.quantity());
    }

    @Test
    @DisplayName("addProduct() accumulates quantity when adding already present product")
    void testAddExistingProductAccumulatesQuantity() {
        cart.addProduct(product1, new Quantity(2));
        cart.addProduct(product1, new Quantity(3));

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.quantityOf("prod-1"));
        assertEquals(new Money(new BigDecimal("250000.00"), "IRR"), cart.calculateSubtotal());
    }

    @Test
    @DisplayName("addProduct() maintains distinct lines for different products")
    void testAddMultipleDistinctProducts() {
        cart.addProduct(product1, new Quantity(2)); // 50,000 * 2 = 100,000
        cart.addProduct(product2, new Quantity(1)); // 150,000 * 1 = 150,000

        assertEquals(2, cart.getItems().size());
        assertEquals(2, cart.quantityOf("prod-1"));
        assertEquals(1, cart.quantityOf("prod-2"));
        assertEquals(new Money(new BigDecimal("250000.00"), "IRR"), cart.calculateSubtotal());
    }

    @Test
    @DisplayName("addProduct() rejects null product or null quantity")
    void testAddProductNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> cart.addProduct(null, new Quantity(1)));
        assertThrows(IllegalArgumentException.class, () -> cart.addProduct(product1, null));
    }

    @Test
    @DisplayName("updateQuantity() changes line item quantity and updates subtotal")
    void testUpdateQuantitySuccess() {
        cart.addProduct(product1, new Quantity(2));
        cart.updateQuantity("prod-1", new Quantity(5));

        assertEquals(5, cart.quantityOf("prod-1"));
        assertEquals(new Money(new BigDecimal("250000.00"), "IRR"), cart.calculateSubtotal());
    }

    @Test
    @DisplayName("updateQuantity() rejects product not present in cart")
    void testUpdateQuantityNonExistent() {
        assertThrows(IllegalArgumentException.class, () ->
                cart.updateQuantity("prod-999", new Quantity(2)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("updateQuantity() rejects invalid product ID")
    void testUpdateQuantityInvalidId(String invalidId) {
        assertThrows(IllegalArgumentException.class, () ->
                cart.updateQuantity(invalidId, new Quantity(2)));
    }

    @Test
    @DisplayName("updateQuantity() rejects null quantity")
    void testUpdateQuantityNullQuantity() {
        cart.addProduct(product1, new Quantity(1));
        assertThrows(IllegalArgumentException.class, () ->
                cart.updateQuantity("prod-1", null));
    }

    @Test
    @DisplayName("removeProduct() removes existing line item")
    void testRemoveProductSuccess() {
        cart.addProduct(product1, new Quantity(2));
        cart.addProduct(product2, new Quantity(1));
        assertEquals(2, cart.getItems().size());

        cart.removeProduct("prod-1");
        assertEquals(1, cart.getItems().size());
        assertEquals(0, cart.quantityOf("prod-1"));
        assertEquals(1, cart.quantityOf("prod-2"));
        assertEquals(new Money(new BigDecimal("150000.00"), "IRR"), cart.calculateSubtotal());
    }

    @Test
    @DisplayName("removeProduct() on non-existent product does not alter cart")
    void testRemoveProductNonExistent() {
        cart.addProduct(product1, new Quantity(2));
        cart.removeProduct("prod-non-existent");
        assertEquals(1, cart.getItems().size());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("removeProduct() rejects invalid product ID")
    void testRemoveProductInvalidId(String invalidId) {
        assertThrows(IllegalArgumentException.class, () -> cart.removeProduct(invalidId));
    }

    @Test
    @DisplayName("clear() removes all items and resets subtotal to ZERO")
    void testClearCart() {
        cart.addProduct(product1, new Quantity(2));
        cart.addProduct(product2, new Quantity(3));
        assertFalse(cart.isEmpty());

        cart.clear();
        assertTrue(cart.isEmpty());
        assertEquals(0, cart.getItems().size());
        assertEquals(Money.ZERO, cart.calculateSubtotal());
    }

    @Test
    @DisplayName("quantityOf() returns 0 for null productId")
    void testQuantityOfNull() {
        assertEquals(0, cart.quantityOf(null));
    }

    @Test
    @DisplayName("Representation exposure: getItems() returns unmodifiable copy")
    void testGetItemsImmutability() {
        cart.addProduct(product1, new Quantity(1));
        List<CartItem> items = cart.getItems();

        CartItem extraItem = new CartItem("prod-9", "Extra",
                new Money(new BigDecimal("1000.00"), "IRR"), new Quantity(1));

        assertThrows(UnsupportedOperationException.class, () -> items.add(extraItem));
    }

    @Test
    @DisplayName("Cart equality is strictly based on identity cartId")
    void testCartEquality() {
        Cart cartSameId = new Cart("cart-usr-1");
        cartSameId.addProduct(product2, new Quantity(5));

        Cart cartDiffId = new Cart("cart-usr-2");

        assertEquals(cart, cartSameId);
        assertEquals(cart.hashCode(), cartSameId.hashCode());
        assertNotEquals(cart, cartDiffId);
        assertNotEquals(cart, null);
        assertNotEquals(cart, "not-a-cart");
    }

    @Test
    @DisplayName("toString() includes cartId and itemCount")
    void testToString() {
        cart.addProduct(product1, new Quantity(2));
        String str = cart.toString();
        assertTrue(str.contains("cart-usr-1"));
        assertTrue(str.contains("itemCount=1"));
    }
}
