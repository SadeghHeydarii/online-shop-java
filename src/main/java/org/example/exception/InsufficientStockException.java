package org.example.exception;

/**
 * Domain exception thrown when an operation cannot proceed due to insufficient available inventory.
 */
public class InsufficientStockException extends RuntimeException {

    /**
     * Constructs a new InsufficientStockException with the specified detail message.
     *
     * @param message descriptive message detailing the stock deficit
     */
    public InsufficientStockException(String message) {
        super(message);
    }
}
