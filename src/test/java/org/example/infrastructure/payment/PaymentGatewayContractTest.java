package org.example.infrastructure.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Order;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.service.port.PaymentGateway;
import org.example.service.port.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract Design-by-Contract (LSP) test suite for {@link PaymentGateway} implementations.
 */
public abstract class PaymentGatewayContractTest {

    protected abstract PaymentGateway createGateway();

    private PaymentGateway gateway;
    private Order sampleOrder;
    private Money amount;

    @BeforeEach
    void initContract() {
        gateway = createGateway();
        Address address = new Address("addr-1", "Ali", "09121112233", "Tehran", "Tehran",
                "Hafez St", "1234567890", "Home", true);
        amount = new Money(new BigDecimal("100000.00"), "IRR");
        Money zero = new Money(BigDecimal.ZERO, "IRR");
        OrderItem item = new OrderItem("prod-1", "Mouse", amount, new Quantity(1));
        sampleOrder = new Order("ord-1", "usr-1", List.of(item), address, amount, zero, zero, amount, LocalDateTime.now());
    }

    @Test
    void methodReturnsNonNullSupportedChannel() {
        assertNotNull(gateway.method());
    }

    @Test
    void chargeApprovedReturnsSuccessWithNonBlankReferenceCode() {
        PaymentResult result = gateway.charge(sampleOrder, amount, true);
        assertTrue(result.success());
        assertNotNull(result.referenceCode());
        assertFalse(result.referenceCode().isBlank());
    }

    @Test
    void chargeDeclinedReturnsFailureWithoutReferenceCode() {
        PaymentResult result = gateway.charge(sampleOrder, amount, false);
        assertFalse(result.success());
        assertNull(result.referenceCode());
    }

    @Test
    void chargeRejectsNullArgumentsPerContract() {
        assertThrows(InvalidInputException.class, () -> gateway.charge(null, amount, true));
        assertThrows(InvalidInputException.class, () -> gateway.charge(sampleOrder, null, true));
    }
}
