package org.example.infrastructure.pricing;

import java.math.BigDecimal;
import org.example.exception.InvalidInputException;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.service.port.ShippingPolicy;

/**
 * Default {@link ShippingPolicy} offering zero shipping fee while preserving the subtotal currency.
 */
public class FreeShippingPolicy implements ShippingPolicy {

    @Override
    public Money calculateShipping(Money subtotal, Address deliveryAddress) {
        if (subtotal == null) {
            throw new InvalidInputException("Subtotal cannot be null");
        }
        if (deliveryAddress == null) {
            throw new InvalidInputException("Delivery address cannot be null");
        }
        return new Money(BigDecimal.ZERO, subtotal.currency());
    }
}
