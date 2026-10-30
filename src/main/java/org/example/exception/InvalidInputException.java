package org.example.exception;

/**
 * Thrown when an input argument provided to a service operation fails domain validation.
 */
public class InvalidInputException extends IllegalArgumentException {

    /**
     * Constructs a new invalid input exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidInputException(String message) {
        super(message);
    }
}
