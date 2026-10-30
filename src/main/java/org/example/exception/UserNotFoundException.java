package org.example.exception;

/**
 * Thrown when a requested user entity cannot be found by their identifier.
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Constructs a new user not found exception with the specified detail message.
     *
     * @param message the detail message
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
