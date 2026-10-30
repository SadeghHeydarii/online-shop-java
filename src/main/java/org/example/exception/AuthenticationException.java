package org.example.exception;

/**
 * Thrown to indicate an authentication failure, such as invalid credentials,
 * expired session tokens, or inactive account access attempts.
 */
public class AuthenticationException extends RuntimeException {

    /**
     * Constructs a new authentication exception with the specified detail message.
     *
     * @param message the detail message
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Constructs a new authentication exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
