package org.example.infrastructure.payment;

import org.example.model.enums.PaymentMethod;
import org.example.service.port.IdGenerator;

/**
 * Simulated payment gateway adapter for {@link PaymentMethod#CASH_ON_DELIVERY}.
 */
public class CashOnDeliveryPaymentGateway extends AbstractSimulatedPaymentGateway {

    /**
     * Constructs a {@code CashOnDeliveryPaymentGateway} using the provided ID generator.
     *
     * @param idGenerator generator used to produce COD tracking reference codes
     */
    public CashOnDeliveryPaymentGateway(IdGenerator idGenerator) {
        super(PaymentMethod.CASH_ON_DELIVERY, "COD", idGenerator);
    }
}
