package org.example.infrastructure.payment;

import org.example.model.enums.PaymentMethod;
import org.example.service.port.IdGenerator;

/**
 * Simulated payment gateway adapter for {@link PaymentMethod#WALLET}.
 */
public class WalletPaymentGateway extends AbstractSimulatedPaymentGateway {

    /**
     * Constructs a {@code WalletPaymentGateway} using the provided ID generator.
     *
     * @param idGenerator generator used to produce wallet transaction reference codes
     */
    public WalletPaymentGateway(IdGenerator idGenerator) {
        super(PaymentMethod.WALLET, "WAL", idGenerator);
    }
}
