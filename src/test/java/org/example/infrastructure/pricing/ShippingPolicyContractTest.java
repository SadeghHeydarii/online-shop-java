package org.example.infrastructure.pricing;

import java.math.BigDecimal;
import org.example.exception.InvalidInputException;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.service.port.ShippingPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract Design-by-Contract (LSP) test suite for {@link ShippingPolicy} implementations.
 */
public abstract class ShippingPolicyContractTest {

    protected abstract ShippingPolicy createPolicy();

    private ShippingPolicy policy;
    private Money subtotal;
    private Address address;

    @BeforeEach
    void initContract() {
        policy = createPolicy();
        subtotal = new Money(new BigDecimal("250000.00"), "IRR");
        address = new Address("addr-1", "Ali", "09121112233", "Tehran", "Tehran",
                "Hafez St", "1234567890", "Home", true);
    }

    @Test
    void returnsNonNegativeMoneyInSubtotalCurrency() {
        Money fee = policy.calculateShipping(subtotal, address);
        assertNotNull(fee);
        assertFalse(fee.isNegative());
        assertEquals("IRR", fee.currency());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(InvalidInputException.class, () -> policy.calculateShipping(null, address));
        assertThrows(InvalidInputException.class, () -> policy.calculateShipping(subtotal, null));
    }
}
