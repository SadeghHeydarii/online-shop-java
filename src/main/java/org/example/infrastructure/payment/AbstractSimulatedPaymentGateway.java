package org.example.infrastructure.payment;

import java.util.Locale;
import java.util.Objects;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Order;
import org.example.model.enums.PaymentMethod;
import org.example.model.vo.Money;
import org.example.service.port.IdGenerator;
import org.example.service.port.PaymentGateway;
import org.example.service.port.PaymentResult;

/**
 * Shared base class for simulated {@link PaymentGateway} adapters eliminating duplicate
 * precondition checks and tracking-reference generation logic.
 */
public abstract class AbstractSimulatedPaymentGateway implements PaymentGateway {

    private final PaymentMethod method;
    private final String referencePrefix;
    private final IdGenerator idGenerator;

    /**
     * Constructs a simulated gateway for a fixed {@link PaymentMethod}.
     *
     * @param method          supported payment channel
     * @param referencePrefix non-blank tracking reference prefix (e.g., "REF", "WAL", "COD")
     * @param idGenerator     identifier generator for tracking codes
     */
    protected AbstractSimulatedPaymentGateway(PaymentMethod method,
                                              String referencePrefix,
                                              IdGenerator idGenerator) {
        this.method = Objects.requireNonNull(method, "method cannot be null");
        if (referencePrefix == null || referencePrefix.isBlank()) {
            throw new IllegalArgumentException("referencePrefix cannot be null or blank");
        }
        this.referencePrefix = referencePrefix.trim().toUpperCase(Locale.ROOT);
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator cannot be null");
    }

    @Override
    public PaymentMethod method() {
        return method;
    }

    @Override
    public PaymentResult charge(Order order, Money amount, boolean simulateSuccess) {
        if (order == null) {
            throw new InvalidInputException("Order cannot be null");
        }
        if (amount == null || amount.isNegative()) {
            throw new InvalidInputException("Payment amount must be non-null and non-negative");
        }
        if (!simulateSuccess) {
            return PaymentResult.declined();
        }
        String referenceCode = idGenerator.nextId(referencePrefix).toUpperCase(Locale.ROOT);
        return PaymentResult.approved(referenceCode);
    }
}
