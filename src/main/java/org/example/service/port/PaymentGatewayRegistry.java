package org.example.service.port;

import org.example.exception.InvalidInputException;
import org.example.model.enums.PaymentMethod;

/**
 * Outbound registry port resolving the {@link PaymentGateway} strategy for a {@link PaymentMethod} (OCP).
 */
public interface PaymentGatewayRegistry {

    /**
     * Resolves the registered gateway responsible for {@code method}.
     *
     * @param method non-null payment method
     * @return matching {@link PaymentGateway}
     * @throws InvalidInputException if {@code method} is null or has no registered gateway
     */
    PaymentGateway resolve(PaymentMethod method);
}
