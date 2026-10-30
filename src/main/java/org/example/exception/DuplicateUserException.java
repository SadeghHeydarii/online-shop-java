package org.example.exception;

/**
 * Thrown when attempting to register a user with an email address or identity
 * that already exists in the system.
 */
public class DuplicateUserException extends IllegalArgumentException {

    /**
     * Constructs a new duplicate user exception with the specified detail message.
     *
     * @param message the detail message
     */
    public DuplicateUserException(String message) {
        super(message);
    }
}
