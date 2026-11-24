package org.example.service.api;

import org.example.model.enums.PaymentMethod;

/**
 * Inbound use-case API for executing order payments through configured gateways (ISP).
 */
public interface PaymentUseCase {

    /**
     * Processes payment for the specified order.
     *
     * @param orderId         non-blank order identifier
     * @param method          selected payment method
     * @param simulateSuccess whether the simulated external gateway authorizes the charge
     * @return {@code true} if payment succeeded; {@code false} if declined
     */
    boolean processPayment(String orderId, PaymentMethod method, boolean simulateSuccess);

    /**
     * Processes payment after verifying that the order belongs to {@code userId}.
     *
     * @param userId          non-blank customer identifier
     * @param orderId         non-blank order identifier
     * @param method          selected payment method
     * @param simulateSuccess whether the simulated external gateway authorizes the charge
     * @return {@code true} if payment succeeded; {@code false} if declined
     */
    boolean processPayment(String userId, String orderId, PaymentMethod method, boolean simulateSuccess);
}
