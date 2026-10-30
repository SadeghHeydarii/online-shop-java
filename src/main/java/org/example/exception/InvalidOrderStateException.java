package org.example.exception;

/**
 * Domain exception thrown when an operation violates order lifecycle state transitions.
 */
public class InvalidOrderStateException extends RuntimeException {

    /**
     * Constructs a new InvalidOrderStateException with the specified detail message.
     *
     * @param message descriptive message detailing the invalid state transition
     */
    public InvalidOrderStateException(String message) {
        super(message);
    }
}
