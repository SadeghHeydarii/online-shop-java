package org.example.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Coupon Value Object Tests")
class CouponTest {

    private final Money defaultMaxDiscount = new Money(new BigDecimal("50000"), "TOMAN");
    private final Money defaultMinOrder = new Money(new BigDecimal("100000"), "TOMAN");
    private final LocalDate futureDate = LocalDate.now().plusMonths(1);
    private final LocalDate pastDate = LocalDate.now().minusDays(1);

    @Test
    @DisplayName("Should create valid Coupon with standardized upper-case code")
    void shouldCreateValidCoupon() {
        Coupon coupon = new Coupon("welcome20", 20, defaultMaxDiscount, defaultMinOrder, futureDate);

        assertEquals("WELCOME20", coupon.code());
        assertEquals(20, coupon.discountPercent());
        assertEquals(defaultMaxDiscount, coupon.maxDiscount());
        assertEquals(defaultMinOrder, coupon.minOrderAmount());
        assertEquals(futureDate, coupon.expiryDate());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject blank or null coupon code")
    void shouldRejectInvalidCode(String invalidCode) {
        assertThrows(
                RuntimeException.class,
                () -> new Coupon(invalidCode, 20, defaultMaxDiscount, defaultMinOrder, futureDate));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, 101, 150})
    @DisplayName("Should reject discount percentage outside 1..100")
    void shouldRejectInvalidDiscountPercentage(int invalidPercent) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Coupon("SAVE", invalidPercent, defaultMaxDiscount, defaultMinOrder, futureDate));
    }

    @Test
    @DisplayName("Should reject currency mismatch between maxDiscount and minOrderAmount")
    void shouldRejectCurrencyMismatch() {
        Money maxDiscountTomans = new Money(new BigDecimal("50000"), "TOMAN");
        Money minOrderRials = new Money(new BigDecimal("500000"), "IRR");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Coupon("SAVE", 10, maxDiscountTomans, minOrderRials, futureDate));
        assertTrue(ex.getMessage().contains("Currency mismatch"));
    }

    @Test
    @DisplayName("Should return zero discount if subtotal is less than minOrderAmount")
    void shouldReturnZeroDiscountWhenBelowMinOrderAmount() {
        Coupon coupon = new Coupon("DISCOUNT20", 20, defaultMaxDiscount, defaultMinOrder, futureDate);
        Money smallSubtotal = new Money(new BigDecimal("80000"), "TOMAN");

        Money discount = coupon.calculateDiscount(smallSubtotal);

        assertEquals(new Money(BigDecimal.ZERO, "TOMAN"), discount);
    }

    @Test
    @DisplayName("Should calculate exact percentage discount when below maxDiscount cap")
    void shouldCalculateExactPercentageBelowCap() {
        Coupon coupon = new Coupon("DISCOUNT10", 10, defaultMaxDiscount, defaultMinOrder, futureDate);
        // subtotal = 200,000 -> 10% = 20,000 <= 50,000
        Money subtotal = new Money(new BigDecimal("200000"), "TOMAN");

        Money discount = coupon.calculateDiscount(subtotal);

        assertEquals(new Money(new BigDecimal("20000.00"), "TOMAN"), discount);
    }

    @Test
    @DisplayName("Should cap discount at maxDiscount when calculated percentage exceeds cap")
    void shouldCapDiscountAtMaxDiscount() {
        Coupon coupon = new Coupon("DISCOUNT20", 20, defaultMaxDiscount, defaultMinOrder, futureDate);
        // subtotal = 400,000 -> 20% = 80,000 > maxDiscount (50,000)
        Money subtotal = new Money(new BigDecimal("400000"), "TOMAN");

        Money discount = coupon.calculateDiscount(subtotal);

        assertEquals(defaultMaxDiscount, discount);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if subtotal currency mismatches coupon currency")
    void shouldRejectCurrencyMismatchOnCalculate() {
        Coupon coupon = new Coupon("DISCOUNT20", 20, defaultMaxDiscount, defaultMinOrder, futureDate);
        Money rialsSubtotal = new Money(new BigDecimal("1000000"), "IRR");

        assertThrows(IllegalArgumentException.class, () -> coupon.calculateDiscount(rialsSubtotal));
    }

    @Test
    @DisplayName("Should validate isValid correctly based on expiryDate, subtotal and currency")
    void shouldValidateCorrectly() {
        Coupon coupon = new Coupon("DISCOUNT20", 20, defaultMaxDiscount, defaultMinOrder, futureDate);
        LocalDate today = LocalDate.now();

        Money validSubtotal = new Money(new BigDecimal("150000"), "TOMAN");
        Money smallSubtotal = new Money(new BigDecimal("50000"), "TOMAN");
        Money rialsSubtotal = new Money(new BigDecimal("1500000"), "IRR");

        // Valid conditions
        assertTrue(coupon.isValid(validSubtotal, today));

        // Subtotal below minimum
        assertFalse(coupon.isValid(smallSubtotal, today));

        // Expired date
        LocalDate wayInFuture = futureDate.plusDays(10);
        assertFalse(coupon.isValid(validSubtotal, wayInFuture));

        // Currency mismatch
        assertFalse(coupon.isValid(rialsSubtotal, today));

        // Null inputs
        assertFalse(coupon.isValid(null, today));
        assertFalse(coupon.isValid(validSubtotal, null));
    }

    @Test
    @DisplayName("Should verify structural equality and hashCode for Coupon")
    void shouldVerifyStructuralEquality() {
        Coupon c1 = new Coupon("SAVE10", 10, defaultMaxDiscount, defaultMinOrder, futureDate);
        Coupon c2 = new Coupon("save10", 10, defaultMaxDiscount, defaultMinOrder, futureDate);
        Coupon c3 = new Coupon("SAVE20", 20, defaultMaxDiscount, defaultMinOrder, futureDate);

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1, c3);
    }
}
