package org.example.infrastructure.pricing;

import java.math.BigDecimal;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.service.port.ShippingPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FreeShippingPolicyTest extends ShippingPolicyContractTest {

    @Override
    protected ShippingPolicy createPolicy() {
        return new FreeShippingPolicy();
    }

    @Test
    void calculatesZeroFee() {
        Address address = new Address("addr-1", "Ali", "09121112233", "Tehran", "Tehran",
                "Hafez St", "1234567890", "Home", true);
        Money fee = createPolicy().calculateShipping(new Money(new BigDecimal("500000.00"), "IRR"), address);
        assertEquals(0, fee.amount().compareTo(BigDecimal.ZERO));
    }
}
