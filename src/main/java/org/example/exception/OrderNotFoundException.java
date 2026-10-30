package org.example.exception;

/**
 * Domain exception thrown when a requested order cannot be found.
 */
public class OrderNotFoundException extends RuntimeException {

    /**
     * Constructs a new OrderNotFoundException with the specified detail message.
     *
     * @param message descriptive message detailing the missing order
     */
    public OrderNotFoundException(String message) {
        super(message);
    }
}
