package org.example.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Cart;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.service.port.DiscountPolicy;
import org.example.service.port.ShippingPolicy;

/**
 * Cohesive pricing service computing order subtotals, promotional discounts,
 * shipping fees, and payable totals via injected Open-Closed pricing strategies (SRP &amp; OCP).
 */
public class OrderPricingService {

    /**
     * Immutable financial breakdown computed during order checkout.
     *
     * @param subtotal      sum of line-item totals
     * @param discount      promotional discount applied
     * @param shippingFee   delivery charge
     * @param payableAmount final amount payable ({@code subtotal - discount + shippingFee})
     */
    public record PriceBreakdown(Money subtotal, Money discount, Money shippingFee, Money payableAmount) {
        public PriceBreakdown {
            Objects.requireNonNull(subtotal, "subtotal cannot be null");
            Objects.requireNonNull(discount, "discount cannot be null");
            Objects.requireNonNull(shippingFee, "shippingFee cannot be null");
            Objects.requireNonNull(payableAmount, "payableAmount cannot be null");
        }
    }

    private final DiscountPolicy discountPolicy;
    private final ShippingPolicy shippingPolicy;
    private final Clock clock;

    /**
     * Constructs an {@code OrderPricingService} with injected pricing policies and clock.
     *
     * @param discountPolicy promotional discount calculation strategy
     * @param shippingPolicy shipping fee calculation strategy
     * @param clock          clock used for deterministic coupon expiration evaluation
     */
    public OrderPricingService(DiscountPolicy discountPolicy, ShippingPolicy shippingPolicy, Clock clock) {
        this.discountPolicy = Objects.requireNonNull(discountPolicy, "discountPolicy cannot be null");
        this.shippingPolicy = Objects.requireNonNull(shippingPolicy, "shippingPolicy cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /**
     * Calculates the complete monetary breakdown for a non-empty cart and delivery address.
     *
     * @param cart            non-null non-empty shopping cart
     * @param deliveryAddress resolved destination address
     * @param couponCode      optional coupon code
     * @return immutable {@link PriceBreakdown}
     * @throws InvalidInputException if cart is null/empty or deliveryAddress is null
     */
    public PriceBreakdown calculateOrderTotals(Cart cart, Address deliveryAddress, String couponCode) {
        if (cart == null || cart.isEmpty()) {
            throw new InvalidInputException("Cart must be non-null and non-empty to calculate order totals");
        }
        if (deliveryAddress == null) {
            throw new InvalidInputException("Delivery address cannot be null");
        }

        Money subtotal = cart.calculateSubtotal();
        Money discount = discountPolicy.calculateDiscount(subtotal, couponCode, LocalDate.now(clock));
        Money shippingFee = shippingPolicy.calculateShipping(subtotal, deliveryAddress);
        Money payableAmount = subtotal.subtract(discount).add(shippingFee);
        return new PriceBreakdown(subtotal, discount, shippingFee, payableAmount);
    }
}
