package org.example.infrastructure.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.exception.InvalidCouponException;
import org.example.infrastructure.persistence.InMemoryCouponRepository;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.service.port.CouponRepository;
import org.example.service.port.DiscountPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CouponDiscountPolicyTest extends DiscountPolicyContractTest {

    private CouponRepository couponRepository;
    private CouponDiscountPolicy policy;
    private final LocalDate today = LocalDate.of(2026, 11, 24);

    @Override
    protected DiscountPolicy createPolicy() {
        couponRepository = new InMemoryCouponRepository();
        couponRepository.save(new Coupon(
                "SAVE10",
                10,
                new Money(new BigDecimal("200000.00"), "IRR"),
                new Money(new BigDecimal("500000.00"), "IRR"),
                today.plusDays(10)
        ));
        couponRepository.save(new Coupon(
                "OLD20",
                20,
                new Money(new BigDecimal("200000.00"), "IRR"),
                new Money(new BigDecimal("100000.00"), "IRR"),
                today.minusDays(1)
        ));
        return new CouponDiscountPolicy(couponRepository);
    }

    @BeforeEach
    void initConcrete() {
        policy = (CouponDiscountPolicy) createPolicy();
    }

    @Test
    void appliesValidCouponAndRespectsCap() {
        Money subtotal = new Money(new BigDecimal("1200000.00"), "IRR");
        Money discount = policy.calculateDiscount(subtotal, " save10 ", today);
        assertEquals(0, discount.amount().compareTo(new BigDecimal("120000.00")));
        assertEquals("IRR", discount.currency());
    }

    @Test
    void rejectsExpiredUnknownOrBelowMinimumCoupons() {
        Money eligible = new Money(new BigDecimal("600000.00"), "IRR");
        Money tooSmall = new Money(new BigDecimal("200000.00"), "IRR");

        assertThrows(InvalidCouponException.class, () -> policy.calculateDiscount(eligible, "OLD20", today));
        assertThrows(InvalidCouponException.class, () -> policy.calculateDiscount(eligible, "MISSING", today));
        assertThrows(InvalidCouponException.class, () -> policy.calculateDiscount(tooSmall, "SAVE10", today));
    }
}
