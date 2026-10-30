package org.example.service;

import java.math.BigDecimal;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Cart;
import org.example.model.entity.Category;
import org.example.model.entity.Product;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CartService Tests")
class CartServiceTest {

    private ProductService productService;
    private CartService cartService;
    private Product activeProduct;
    private Product lowStockProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        productService = new ProductService();
        cartService = new CartService(productService);

        Category tech = new Category("CAT-TECH", "Technology", "Tech gear");
        productService.registerCategory(tech);

        Money price = new Money(new BigDecimal("100000.00"), "IRR");
        productService.addProduct("Wireless Mouse", "Optical mouse", price, 20, "CAT-TECH");
        productService.addProduct("Rare Cable", "Limited edition cable", price, 2, "CAT-TECH");
        productService.addProduct("Old Keyboard", "Legacy model", price, 10, "CAT-TECH");

        activeProduct = productService.searchProducts("Wireless Mouse").getFirst();
        lowStockProduct = productService.searchProducts("Rare Cable").getFirst();
        inactiveProduct = productService.searchProducts("Old Keyboard").getFirst();
        productService.deactivateProduct(inactiveProduct.getId());
    }

    @Nested
    @DisplayName("getCart Tests")
    class GetCartTests {

        @Test
        @DisplayName("getCart retrieves new empty cart on first access and caches it")
        void getCartNewAndSubsequent() {
            Cart cart1 = cartService.getCart("user-1");
            assertNotNull(cart1);
            assertTrue(cart1.isEmpty());
            assertEquals("user-1", cart1.getCartId());

            Cart cart2 = cartService.getCart("user-1");
            assertSame(cart1, cart2);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Blank or null userId throws InvalidInputException")
        void invalidUserIdThrows(String userId) {
            assertThrows(InvalidInputException.class, () -> cartService.getCart(userId));
        }
    }

    @Nested
    @DisplayName("addItem Tests")
    class AddItemTests {

        @Test
        @DisplayName("Adding available active product succeeds and accumulates quantity on repeat")
        void addItemSuccessAndAccumulate() {
            cartService.addItem("user-1", activeProduct.getId(), new Quantity(3));
            Cart cart = cartService.getCart("user-1");
            assertEquals(1, cart.getItems().size());
            assertEquals(3, cart.quantityOf(activeProduct.getId()));

            cartService.addItem("user-1", activeProduct.getId(), new Quantity(2));
            assertEquals(1, cart.getItems().size());
            assertEquals(5, cart.quantityOf(activeProduct.getId()));
        }

        @Test
        @DisplayName("Adding more than available stock throws InsufficientStockException")
        void addExceedingStockThrows() {
            assertThrows(InsufficientStockException.class, () ->
                    cartService.addItem("user-1", lowStockProduct.getId(), new Quantity(5)));

            assertTrue(cartService.getCart("user-1").isEmpty());
        }

        @Test
        @DisplayName("Adding quantity that accumulates past available stock throws InsufficientStockException")
        void accumulatingExceedingStockThrows() {
            cartService.addItem("user-1", lowStockProduct.getId(), new Quantity(2));

            assertThrows(InsufficientStockException.class, () ->
                    cartService.addItem("user-1", lowStockProduct.getId(), new Quantity(1)));

            assertEquals(2, cartService.getCart("user-1").quantityOf(lowStockProduct.getId()));
        }

        @Test
        @DisplayName("Adding inactive product throws InvalidInputException")
        void addInactiveProductThrows() {
            assertThrows(InvalidInputException.class, () ->
                    cartService.addItem("user-1", inactiveProduct.getId(), new Quantity(1)));
        }

        @Test
        @DisplayName("Adding null or non-positive quantity throws InvalidInputException")
        void invalidQuantityThrows() {
            assertThrows(InvalidInputException.class, () ->
                    cartService.addItem("user-1", activeProduct.getId(), null));
        }
    }

    @Nested
    @DisplayName("updateItemQuantity Tests")
    class UpdateItemQuantityTests {

        @BeforeEach
        void seedCart() {
            cartService.addItem("user-1", activeProduct.getId(), new Quantity(5));
        }

        @Test
        @DisplayName("Valid update changes line item quantity")
        void updateQuantitySuccess() {
            cartService.updateItemQuantity("user-1", activeProduct.getId(), new Quantity(8));
            assertEquals(8, cartService.getCart("user-1").quantityOf(activeProduct.getId()));
        }

        @Test
        @DisplayName("Attempting to create zero or negative quantity throws IllegalArgumentException")
        void nonPositiveQuantityThrows() {
            assertThrows(IllegalArgumentException.class, () -> new Quantity(0));
            assertThrows(IllegalArgumentException.class, () -> new Quantity(-1));
        }

        @Test
        @DisplayName("Updating to quantity greater than available stock throws InsufficientStockException")
        void updateExceedingStockThrows() {
            assertThrows(InsufficientStockException.class, () ->
                    cartService.updateItemQuantity("user-1", activeProduct.getId(), new Quantity(50)));
            assertEquals(5, cartService.getCart("user-1").quantityOf(activeProduct.getId()));
        }

        @Test
        @DisplayName("Updating null quantity throws InvalidInputException")
        void updateNullQuantityThrows() {
            assertThrows(InvalidInputException.class, () ->
                    cartService.updateItemQuantity("user-1", activeProduct.getId(), null));
        }
    }

    @Nested
    @DisplayName("removeItem and clearCart Tests")
    class RemoveAndClearTests {

        @BeforeEach
        void seedCart() {
            cartService.addItem("user-1", activeProduct.getId(), new Quantity(2));
            cartService.addItem("user-1", lowStockProduct.getId(), new Quantity(1));
        }

        @Test
        @DisplayName("removeItem removes specified item only")
        void removeItemSuccess() {
            cartService.removeItem("user-1", activeProduct.getId());
            Cart cart = cartService.getCart("user-1");
            assertEquals(1, cart.getItems().size());
            assertEquals(0, cart.quantityOf(activeProduct.getId()));
            assertEquals(1, cart.quantityOf(lowStockProduct.getId()));
        }

        @Test
        @DisplayName("clearCart empties all cart items")
        void clearCartSuccess() {
            cartService.clearCart("user-1");
            assertTrue(cartService.getCart("user-1").isEmpty());
        }
    }
}
