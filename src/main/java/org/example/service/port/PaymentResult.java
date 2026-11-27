package org.example.service.port;

/**
 * Immutable outcome of executing a charge attempt through a {@link PaymentGateway}.
 *
 * @param success       {@code true} if the payment gateway authorized the payment; {@code false} if declined
 * @param referenceCode non-blank gateway reference code when {@code success} is {@code true}, or {@code null} when declined
 */
public record PaymentResult(boolean success, String referenceCode) {

    /**
     * Compact constructor enforcing outcome invariants.
     */
    public PaymentResult {
        if (success) {
            if (referenceCode == null || referenceCode.isBlank()) {
                throw new IllegalArgumentException("Successful payment result must include a non-blank referenceCode");
            }
            referenceCode = referenceCode.trim();
        } else {
            referenceCode = (referenceCode == null || referenceCode.isBlank()) ? null : referenceCode.trim();
        }
    }

    /**
     * Creates a successful payment result with the given tracking reference.
     *
     * @param referenceCode non-blank gateway tracking code
     * @return approved {@link PaymentResult}
     */
    public static PaymentResult approved(String referenceCode) {
        return new PaymentResult(true, referenceCode);
    }

    /**
     * Creates a declined payment result with no tracking reference.
     *
     * @return declined {@link PaymentResult}
     */
    public static PaymentResult declined() {
        return new PaymentResult(false, null);
    }
}
