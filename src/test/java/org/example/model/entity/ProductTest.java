package org.example.model.entity;

import java.math.BigDecimal;
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

class ProductTest {

    private Money samplePrice;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        samplePrice = new Money(new BigDecimal("150000.00"), "IRR");
        sampleProduct = new Product("prod-1", "Mechanical Keyboard",
                "High quality tactile switches", samplePrice, 10, "cat-electronics");
    }

    @Test
    @DisplayName("Product constructor sets valid initial state")
    void testValidProductCreation() {
        assertEquals("prod-1", sampleProduct.getId());
        assertEquals("Mechanical Keyboard", sampleProduct.getName());
        assertEquals("High quality tactile switches", sampleProduct.getDescription());
        assertEquals(samplePrice, sampleProduct.getPrice());
        assertEquals(10, sampleProduct.getAvailableStock());
        assertEquals("cat-electronics", sampleProduct.getCategoryId());
        assertTrue(sampleProduct.isActive());
        assertEquals(0.0, sampleProduct.getAverageRating());
        assertEquals(0, sampleProduct.getReviewCount());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Product constructor rejects invalid ID")
    void testInvalidId(String invalidId) {
        assertThrows(IllegalArgumentException.class, () ->
                new Product(invalidId, "Keyboard", "Desc", samplePrice, 5, "cat-1"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Product constructor rejects invalid name")
    void testInvalidName(String invalidName) {
        assertThrows(IllegalArgumentException.class, () ->
                new Product("prod-1", invalidName, "Desc", samplePrice, 5, "cat-1"));
    }

    @Test
    @DisplayName("Product constructor rejects null description")
    void testNullDescription() {
        assertThrows(IllegalArgumentException.class, () ->
                new Product("prod-1", "Keyboard", null, samplePrice, 5, "cat-1"));
    }

    @Test
    @DisplayName("Product constructor rejects null price")
    void testNullPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                new Product("prod-1", "Keyboard", "Desc", null, 5, "cat-1"));
    }

    @Test
    @DisplayName("Product constructor rejects negative stock")
    void testNegativeStock() {
        assertThrows(IllegalArgumentException.class, () ->
                new Product("prod-1", "Keyboard", "Desc", samplePrice, -1, "cat-1"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Product constructor rejects invalid category ID")
    void testInvalidCategoryId(String invalidCat) {
        assertThrows(IllegalArgumentException.class, () ->
                new Product("prod-1", "Keyboard", "Desc", samplePrice, 5, invalidCat));
    }

    @Test
    @DisplayName("isAvailable() returns true when available stock is sufficient")
    void testIsAvailableSufficientStock() {
        assertTrue(sampleProduct.isAvailable(new Quantity(5)));
        assertTrue(sampleProduct.isAvailable(new Quantity(10)));
        assertFalse(sampleProduct.isAvailable(new Quantity(11)));
    }

    @Test
    @DisplayName("isAvailable() throws IllegalArgumentException on null quantity")
    void testIsAvailableNull() {
        assertThrows(IllegalArgumentException.class, () -> sampleProduct.isAvailable(null));
    }

    @Test
    @DisplayName("updateDetails() updates product attributes")
    void testUpdateDetailsSuccess() {
        Money newPrice = new Money(new BigDecimal("180000.00"), "IRR");
        sampleProduct.updateDetails("RGB Keyboard", "New RGB backlight", newPrice, "cat-gaming");

        assertEquals("RGB Keyboard", sampleProduct.getName());
        assertEquals("New RGB backlight", sampleProduct.getDescription());
        assertEquals(newPrice, sampleProduct.getPrice());
        assertEquals("cat-gaming", sampleProduct.getCategoryId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("updateDetails() rejects invalid name")
    void testUpdateDetailsInvalidName(String invalidName) {
        assertThrows(IllegalArgumentException.class, () ->
                sampleProduct.updateDetails(invalidName, "Desc", samplePrice, "cat-1"));
    }

    @Test
    @DisplayName("updateDetails() rejects null description")
    void testUpdateDetailsNullDesc() {
        assertThrows(IllegalArgumentException.class, () ->
                sampleProduct.updateDetails("Name", null, samplePrice, "cat-1"));
    }

    @Test
    @DisplayName("updateDetails() rejects null price")
    void testUpdateDetailsNullPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                sampleProduct.updateDetails("Name", "Desc", null, "cat-1"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("updateDetails() rejects invalid category ID")
    void testUpdateDetailsInvalidCategory(String invalidCat) {
        assertThrows(IllegalArgumentException.class, () ->
                sampleProduct.updateDetails("Name", "Desc", samplePrice, invalidCat));
    }

    @Test
    @DisplayName("setActive() updates publication status")
    void testSetActive() {
        assertTrue(sampleProduct.isActive());
        sampleProduct.setActive(false);
        assertFalse(sampleProduct.isActive());
        sampleProduct.setActive(true);
        assertTrue(sampleProduct.isActive());
    }

    @Test
    @DisplayName("deductStock() decreases stock on sufficient inventory")
    void testDeductStockSuccess() {
        sampleProduct.deductStock(new Quantity(4));
        assertEquals(6, sampleProduct.getAvailableStock());

        sampleProduct.deductStock(new Quantity(6));
        assertEquals(0, sampleProduct.getAvailableStock());
    }

    @Test
    @DisplayName("deductStock() throws IllegalStateException when stock is insufficient")
    void testDeductStockInsufficient() {
        assertThrows(IllegalStateException.class, () ->
                sampleProduct.deductStock(new Quantity(15)));
        assertEquals(10, sampleProduct.getAvailableStock());
    }

    @Test
    @DisplayName("deductStock() throws IllegalArgumentException on null quantity")
    void testDeductStockNull() {
        assertThrows(IllegalArgumentException.class, () -> sampleProduct.deductStock(null));
    }

    @Test
    @DisplayName("restock() increases available stock")
    void testRestockSuccess() {
        sampleProduct.restock(new Quantity(5));
        assertEquals(15, sampleProduct.getAvailableStock());
    }

    @Test
    @DisplayName("restock() throws IllegalArgumentException on null quantity")
    void testRestockNull() {
        assertThrows(IllegalArgumentException.class, () -> sampleProduct.restock(null));
    }

    @Test
    @DisplayName("updateRatingStats() calculates correct cumulative average rating")
    void testUpdateRatingStats() {
        sampleProduct.updateRatingStats(5);
        assertEquals(1, sampleProduct.getReviewCount());
        assertEquals(5.0, sampleProduct.getAverageRating(), 0.001);

        sampleProduct.updateRatingStats(3);
        assertEquals(2, sampleProduct.getReviewCount());
        assertEquals(4.0, sampleProduct.getAverageRating(), 0.001);

        sampleProduct.updateRatingStats(4);
        assertEquals(3, sampleProduct.getReviewCount());
        assertEquals(4.0, sampleProduct.getAverageRating(), 0.001);

        sampleProduct.updateRatingStats(1);
        assertEquals(4, sampleProduct.getReviewCount());
        assertEquals(3.25, sampleProduct.getAverageRating(), 0.001);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 6, 10})
    @DisplayName("updateRatingStats() rejects ratings outside [1, 5]")
    void testUpdateRatingStatsInvalid(int invalidRating) {
        assertThrows(IllegalArgumentException.class, () ->
                sampleProduct.updateRatingStats(invalidRating));
    }

    @Test
    @DisplayName("Product equality is strictly based on identity ID")
    void testEntityIdentityEquality() {
        Product prodSameId = new Product("prod-1", "Different Keyboard", "Different desc",
                new Money(new BigDecimal("999999.00"), "IRR"), 99, "cat-other");

        Product prodDiffId = new Product("prod-2", "Mechanical Keyboard",
                "High quality tactile switches", samplePrice, 10, "cat-electronics");

        assertEquals(sampleProduct, prodSameId);
        assertEquals(sampleProduct.hashCode(), prodSameId.hashCode());
        assertNotEquals(sampleProduct, prodDiffId);
        assertNotEquals(sampleProduct, null);
        assertNotEquals(sampleProduct, "not-a-product");
    }

    @Test
    @DisplayName("toString() includes key product information")
    void testToString() {
        String str = sampleProduct.toString();
        assertTrue(str.contains("prod-1"));
        assertTrue(str.contains("Mechanical Keyboard"));
        assertTrue(str.contains("cat-electronics"));
    }
}
