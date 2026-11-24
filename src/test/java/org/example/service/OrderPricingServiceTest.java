package org.example.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.example.exception.InvalidCouponException;
import org.example.exception.InvalidInputException;
import org.example.infrastructure.persistence.InMemoryCouponRepository;
import org.example.infrastructure.pricing.CouponDiscountPolicy;
import org.example.infrastructure.pricing.FreeShippingPolicy;
import org.example.model.entity.Cart;
import org.example.model.entity.Product;
import org.example.model.vo.Address;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.OrderPricingService.PriceBreakdown;
import org.example.service.port.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderPricingServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);
    private Address address;
    private Cart nonEmptyCart;
    private CouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        address = new Address("addr-1", "Ali", "09121112233", "Tehran", "Tehran",
                "Hafez St", "1234567890", "Home", true);
        Product laptop = new Product("prod-1", "Laptop", "Dev laptop",
                new Money(new BigDecimal("1000000.00"), "IRR"), 5, "CAT-1");
        nonEmptyCart = new Cart("cart-1", "usr-1");
        nonEmptyCart.addProduct(laptop, new Quantity(1));

        couponRepository = new InMemoryCouponRepository();
        couponRepository.save(new Coupon(
                "SAVE10",
                10,
                new Money(new BigDecimal("200000.00"), "IRR"),
                new Money(new BigDecimal("500000.00"), "IRR"),
                LocalDate.of(2026, 11, 24)
        ));
    }

    @Test
    void calculatesTotalsWithValidCouponAndFreeShipping() {
        OrderPricingService pricingService = new OrderPricingService(
                new CouponDiscountPolicy(couponRepository),
                new FreeShippingPolicy(),
                fixedClock
        );

        PriceBreakdown breakdown = pricingService.calculateOrderTotals(nonEmptyCart, address, "SAVE10");
        assertEquals(0, breakdown.subtotal().amount().compareTo(new BigDecimal("1000000.00")));
        assertEquals(0, breakdown.discount().amount().compareTo(new BigDecimal("100000.00")));
        assertEquals(0, breakdown.shippingFee().amount().compareTo(BigDecimal.ZERO));
        assertEquals(0, breakdown.payableAmount().amount().compareTo(new BigDecimal("900000.00")));
    }

    @Test
    void supportsNewShippingPolicyWithoutModifyingService_OcpVerification() {
        OrderPricingService pricingService = new OrderPricingService(
                new CouponDiscountPolicy(couponRepository),
                (subtotal, dest) -> new Money(new BigDecimal("25000.00"), subtotal.currency()),
                fixedClock
        );

        PriceBreakdown breakdown = pricingService.calculateOrderTotals(nonEmptyCart, address, null);
        assertEquals(0, breakdown.shippingFee().amount().compareTo(new BigDecimal("25000.00")));
        assertEquals(0, breakdown.payableAmount().amount().compareTo(new BigDecimal("1025000.00")));
    }

    @Test
    void rejectsExpiredCouponAccordingToInjectedClock() {
        Clock nextDayClock = Clock.fixed(Instant.parse("2026-11-25T08:00:00Z"), ZoneOffset.UTC);
        OrderPricingService pricingService = new OrderPricingService(
                new CouponDiscountPolicy(couponRepository),
                new FreeShippingPolicy(),
                nextDayClock
        );

        assertThrows(InvalidCouponException.class,
                () -> pricingService.calculateOrderTotals(nonEmptyCart, address, "SAVE10"));
    }

    @Test
    void rejectsEmptyCartOrNullAddress() {
        OrderPricingService pricingService = new OrderPricingService(
                new CouponDiscountPolicy(couponRepository),
                new FreeShippingPolicy(),
                fixedClock
        );
        Cart emptyCart = new Cart("cart-2", "usr-1");
        assertThrows(InvalidInputException.class, () -> pricingService.calculateOrderTotals(emptyCart, address, null));
        assertThrows(InvalidInputException.class, () -> pricingService.calculateOrderTotals(nonEmptyCart, null, null));
    }
}
