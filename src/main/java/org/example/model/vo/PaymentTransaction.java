package org.example.model.vo;

import java.time.LocalDateTime;
import org.example.model.enums.PaymentMethod;
import org.example.model.enums.PaymentStatus;

/**
 * Immutable value object representing a financial payment transaction for an order.
 *
 * <p>Captures the immutable transaction outcome (successful, failed, or cancelled) processed
 * via an external payment gateway, customer wallet, or cash on delivery.
 *
 * <p><b>Abstraction Function (AF):</b>
 * AF(transactionId, orderId, amount, method, status, timestamp, referenceCode) =
 * A recorded financial transaction identified by {@code transactionId}, executed for order
 * {@code orderId} with monetary value {@code amount}, processed using payment channel {@code method},
 * yielding transaction outcome {@code status} at timestamp {@code timestamp}, with tracking reference
 * code {@code referenceCode}.
 *
 * <p><b>Representation Invariant (RI):</b>
 * <ul>
 *   <li>{@code transactionId != null && !transactionId.isBlank()}</li>
 *   <li>{@code orderId != null && !orderId.isBlank()}</li>
 *   <li>{@code amount != null && !amount.isNegative()}</li>
 *   <li>{@code method != null}</li>
 *   <li>{@code status != null}</li>
 *   <li>{@code timestamp != null}</li>
 * </ul>
 *
 * <p><b>Safety from Representation Exposure:</b>
 * All fields are immutable primitives, {@link String}, {@link LocalDateTime}, or immutable
 * value objects ({@link Money}, enum constants).
 *
 * @param transactionId unique transaction tracking identifier
 * @param orderId       associated order identifier
 * @param amount        monetary amount of the transaction
 * @param method        payment method used
 * @param status        outcome status of the transaction
 * @param timestamp     exact date and time of transaction execution
 * @param referenceCode bank or gateway tracking reference code (optional, may be null)
 */
public record PaymentTransaction(
        String transactionId,
        String orderId,
        Money amount,
        PaymentMethod method,
        PaymentStatus status,
        LocalDateTime timestamp,
        String referenceCode
) {

    /**
     * Compact constructor enforcing non-null and representation invariants.
     *
     * @throws IllegalArgumentException if any mandatory field violates invariants
     */
    public PaymentTransaction {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or blank");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (amount == null || amount.isNegative()) {
            throw new IllegalArgumentException("Transaction amount must be non-null and non-negative");
        }
        if (method == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Payment status cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Transaction timestamp cannot be null");
        }
        transactionId = transactionId.trim();
        orderId = orderId.trim();
        referenceCode = (referenceCode != null && !referenceCode.isBlank()) ? referenceCode.trim() : null;
    }
}
