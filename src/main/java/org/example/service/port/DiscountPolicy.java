package org.example.service.port;

import java.time.LocalDate;
import org.example.exception.InvalidCouponException;
import org.example.exception.InvalidInputException;
import org.example.model.vo.Money;

/**
 * Outbound pricing strategy port for computing order discounts (OCP &amp; DIP).
 */
public interface DiscountPolicy {

    /**
     * Computes the monetary discount for an order subtotal under the supplied optional coupon code.
     *
     * <p>Contract (LSP):
     * <ul>
     *   <li>If {@code couponCode} is null or blank, returns zero {@link Money} in {@code subtotal.currency()}.</li>
     *   <li>If {@code couponCode} is non-blank and inapplicable/invalid, throws {@link InvalidCouponException}.</li>
     *   <li>Returned discount is non-negative, shares {@code subtotal.currency()}, and never exceeds {@code subtotal}.</li>
     * </ul>
     *
     * @param subtotal       non-null order items subtotal
     * @param couponCode     optional discount coupon code
     * @param evaluationDate non-null date on which eligibility is evaluated
     * @return non-null discount amount in {@code subtotal.currency()}
     * @throws InvalidInputException  if {@code subtotal} or {@code evaluationDate} is null
     * @throws InvalidCouponException if a non-blank {@code couponCode} is invalid, expired, or ineligible
     */
    Money calculateDiscount(Money subtotal, String couponCode, LocalDate evaluationDate);
}
