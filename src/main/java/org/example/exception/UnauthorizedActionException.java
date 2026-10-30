package org.example.exception;

/**
 * Domain exception thrown when a user attempts an unauthorized operation,
 * such as submitting a review for a product they have not purchased in a paid order.
 */
public class UnauthorizedActionException extends RuntimeException {

    /**
     * Constructs a new UnauthorizedActionException with the specified detail message.
     *
     * @param message descriptive message detailing the unauthorized operation
     */
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
