package org.example.model.enums;

/**
 * Represents the lifecycle stages of an order.
 */
public enum OrderStatus {
    PENDING,
    PAYMENT_FAILED,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
