package org.example.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidInputException;
import org.example.infrastructure.catalog.DefaultNameSort;
import org.example.infrastructure.catalog.MapProductSortRegistry;
import org.example.infrastructure.persistence.InMemoryCartRepository;
import org.example.infrastructure.persistence.InMemoryCategoryRepository;
import org.example.infrastructure.persistence.InMemoryProductRepository;
import org.example.infrastructure.security.UuidIdGenerator;
import org.example.model.entity.Cart;
import org.example.model.vo.Category;
import org.example.model.entity.Product;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.port.CartRepository;
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

    private CatalogQueryService catalogQueryService;
    private CatalogManagementService catalogManagementService;
    private CartService cartService;
    private Product activeProduct;
    private Product lowStockProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        var productRepository = new InMemoryProductRepository();
        var categoryRepository = new InMemoryCategoryRepository();
        catalogQueryService = new CatalogQueryService(
                productRepository,
                categoryRepository,
                new MapProductSortRegistry(new DefaultNameSort())
        );
        catalogManagementService = new CatalogManagementService(
                productRepository,
                categoryRepository,
                new UuidIdGenerator(),
                Clock.systemDefaultZone()
        );
        cartService = new CartService(new InMemoryCartRepository(), catalogQueryService);

        Category tech = new Category("CAT-TECH", "Technology", "Tech gear");
        catalogManagementService.registerCategory(tech);

        Money price = new Money(new BigDecimal("100000.00"), "IRR");
        catalogManagementService.addProduct("Wireless Mouse", "Optical mouse", price, 20, "CAT-TECH");
        catalogManagementService.addProduct("Rare Cable", "Limited edition cable", price, 2, "CAT-TECH");
        catalogManagementService.addProduct("Old Keyboard", "Legacy model", price, 10, "CAT-TECH");

        activeProduct = catalogQueryService.searchProducts("Wireless Mouse").getFirst();
        lowStockProduct = catalogQueryService.searchProducts("Rare Cable").getFirst();
        inactiveProduct = catalogQueryService.searchProducts("Old Keyboard").getFirst();
        catalogManagementService.deactivateProduct(inactiveProduct.getId());
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
            assertEquals("user-1", cart1.getUserId());

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

    @Nested
    @DisplayName("Multi-User Cart Isolation Tests")
    class MultiUserIsolationTests {

        @Test
        @DisplayName("Distinct users receive independent cart instances with correct owner user ID")
        void usersReceiveDistinctIndependentCarts() {
            Cart cartA = cartService.getCart("user-alice");
            Cart cartB = cartService.getCart("user-bob");

            assertNotNull(cartA);
            assertNotNull(cartB);
            assertNotSame(cartA, cartB);
            assertEquals("user-alice", cartA.getUserId());
            assertEquals("user-bob", cartB.getUserId());
            assertNotEquals(cartA.getCartId(), cartB.getCartId());
        }

        @Test
        @DisplayName("Modifying user A's cart does not affect user B's cart")
        void modificationsToCartADoNotAffectCartB() {
            Cart cartBob = cartService.getCart("user-bob");
            assertTrue(cartBob.isEmpty());

            cartService.addItem("user-alice", activeProduct.getId(), new Quantity(3));

            Cart cartAlice = cartService.getCart("user-alice");
            assertEquals(1, cartAlice.getItems().size());
            assertEquals(3, cartAlice.quantityOf(activeProduct.getId()));

            // User B's cart remains completely untouched and isolated
            Cart reloadedBob = cartService.getCart("user-bob");
            assertTrue(reloadedBob.isEmpty());
            assertEquals(0, reloadedBob.quantityOf(activeProduct.getId()));
        }

        @Test
        @DisplayName("Users can independently add different quantities of the same product")
        void crossUserCartModificationIsolation() {
            cartService.addItem("user-alice", activeProduct.getId(), new Quantity(2));
            cartService.addItem("user-bob", activeProduct.getId(), new Quantity(5));

            assertEquals(2, cartService.getCart("user-alice").quantityOf(activeProduct.getId()));
            assertEquals(5, cartService.getCart("user-bob").quantityOf(activeProduct.getId()));

            cartService.removeItem("user-alice", activeProduct.getId());
            assertTrue(cartService.getCart("user-alice").isEmpty());
            assertEquals(5, cartService.getCart("user-bob").quantityOf(activeProduct.getId()));
        }

        @Test
        @DisplayName("Subsequent retrieval preserves cart identity without creating new carts")
        void subsequentRetrievalPreservesIdentityWithoutCreatingNewCarts() {
            Cart firstRetrieval = cartService.getCart("user-alice");
            cartService.addItem("user-alice", activeProduct.getId(), new Quantity(1));

            Cart secondRetrieval = cartService.getCart("user-alice");
            assertSame(firstRetrieval, secondRetrieval);
            assertEquals(1, secondRetrieval.quantityOf(activeProduct.getId()));
        }

        @Test
        @DisplayName("Mismatched cart owner in storage throws IllegalStateException")
        void mismatchedCartOwnerInStoreThrowsIllegalStateException() {
            Map<String, Cart> tamperedStore = new HashMap<>();
            Cart corruptedCart = new Cart("cart-123", "user-different");
            tamperedStore.put("user-target", corruptedCart);

            CartRepository tamperedRepo = new CartRepository() {
                @Override
                public Optional<Cart> findByUserId(String userId) {
                    return Optional.ofNullable(tamperedStore.get(userId));
                }

                @Override
                public Cart getOrCreate(String userId) {
                    return tamperedStore.computeIfAbsent(userId, id -> new Cart(id, id));
                }

                @Override
                public void save(Cart cart) {
                    tamperedStore.put(cart.getUserId(), cart);
                }
            };

            CartService customService = new CartService(tamperedRepo, catalogQueryService);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    customService.getCart("user-target")
            );
            assertTrue(ex.getMessage().contains("Cart ownership invariant violated"));
        }
    }
}
