package org.example.exception;

/**
 * Base runtime exception for domain and application rule violations in the online shop.
 *
 * <p>Provides a unified hierarchy for business exceptions so presentation layers can handle
 * domain failures explicitly without catching broad {@link RuntimeException} instances.
 */
public abstract class ShopException extends RuntimeException {

    /**
     * Constructs a new {@code ShopException} with the specified detail message.
     *
     * @param message descriptive explanation of the domain error
     */
    protected ShopException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ShopException} with the specified detail message and cause.
     *
     * @param message descriptive explanation of the domain error
     * @param cause   underlying cause
     */
    protected ShopException(String message, Throwable cause) {
        super(message, cause);
    }
}
