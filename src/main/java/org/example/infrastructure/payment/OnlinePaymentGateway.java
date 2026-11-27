package org.example.infrastructure.payment;

import org.example.model.enums.PaymentMethod;
import org.example.service.port.IdGenerator;

/**
 * Simulated payment gateway adapter for {@link PaymentMethod#ONLINE_GATEWAY}.
 */
public class OnlinePaymentGateway extends AbstractSimulatedPaymentGateway {

    /**
     * Constructs an {@code OnlinePaymentGateway} using the provided ID generator.
     *
     * @param idGenerator generator used to produce gateway reference codes
     */
    public OnlinePaymentGateway(IdGenerator idGenerator) {
        super(PaymentMethod.ONLINE_GATEWAY, "REF", idGenerator);
    }
}
