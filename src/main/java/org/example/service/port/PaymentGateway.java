package org.example.service.port;

import org.example.exception.InvalidInputException;
import org.example.model.entity.Order;
import org.example.model.enums.PaymentMethod;
import org.example.model.vo.Money;

/**
 * Outbound Open-Closed Strategy port for executing payment authorizations per payment channel (OCP &amp; DIP).
 */
public interface PaymentGateway {

    /**
     * Returns the payment channel handled by this gateway implementation.
     *
     * @return non-null supported {@link PaymentMethod}
     */
    PaymentMethod method();

    /**
     * Attempts to authorize payment for an order amount.
     *
     * @param order           non-null order being paid
     * @param amount          non-null, non-negative amount to charge
     * @param simulateSuccess simulation flag indicating authorization outcome
     * @return non-null {@link PaymentResult}
     * @throws InvalidInputException if {@code order} or {@code amount} is null, or {@code amount} is negative
     */
    PaymentResult charge(Order order, Money amount, boolean simulateSuccess);
}
