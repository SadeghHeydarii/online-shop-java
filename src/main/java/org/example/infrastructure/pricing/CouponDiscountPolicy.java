package org.example.infrastructure.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.example.exception.InvalidCouponException;
import org.example.exception.InvalidInputException;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.service.port.CouponRepository;
import org.example.service.port.DiscountPolicy;

/**
 * {@link DiscountPolicy} implementation validating and applying promotional {@link Coupon} codes
 * backed by a {@link CouponRepository}.
 */
public class CouponDiscountPolicy implements DiscountPolicy {

    private final CouponRepository couponRepository;

    /**
     * Constructs a {@code CouponDiscountPolicy} using the specified coupon repository.
     *
     * @param couponRepository non-null repository for promotional coupon lookup
     */
    public CouponDiscountPolicy(CouponRepository couponRepository) {
        this.couponRepository = Objects.requireNonNull(couponRepository, "couponRepository cannot be null");
    }

    @Override
    public Money calculateDiscount(Money subtotal, String couponCode, LocalDate evaluationDate) {
        if (subtotal == null) {
            throw new InvalidInputException("Subtotal cannot be null");
        }
        if (evaluationDate == null) {
            throw new InvalidInputException("Evaluation date cannot be null");
        }
        if (couponCode == null || couponCode.isBlank()) {
            return new Money(BigDecimal.ZERO, subtotal.currency());
        }

        Coupon coupon = couponRepository.findByCode(couponCode.trim())
                .filter(c -> c.isValid(subtotal, evaluationDate))
                .orElseThrow(() -> new InvalidCouponException(
                        "Coupon '" + couponCode + "' is invalid, expired, or order subtotal does not meet requirements"
                ));

        return coupon.calculateDiscount(subtotal);
    }
}
