package org.example.service.port;

import org.example.exception.InvalidInputException;
import org.example.model.vo.Address;
import org.example.model.vo.Money;

/**
 * Outbound pricing strategy port for computing delivery shipping fees (OCP &amp; DIP).
 */
public interface ShippingPolicy {

    /**
     * Calculates the shipping fee for an order subtotal and destination address.
     *
     * <p>Contract (LSP): Returns a non-null, non-negative {@link Money} with {@code subtotal.currency()}.
     *
     * @param subtotal        non-null items subtotal
     * @param deliveryAddress non-null destination address
     * @return non-null, non-negative shipping fee in {@code subtotal.currency()}
     * @throws InvalidInputException if {@code subtotal} or {@code deliveryAddress} is null
     */
    Money calculateShipping(Money subtotal, Address deliveryAddress);
}
