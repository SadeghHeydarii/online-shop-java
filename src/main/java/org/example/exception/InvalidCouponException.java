package org.example.exception;

/**
 * Domain exception thrown when an applied discount coupon is invalid, expired,
 * or does not meet minimum order requirements.
 */
public class InvalidCouponException extends RuntimeException {

    /**
     * Constructs a new InvalidCouponException with the specified detail message.
     *
     * @param message descriptive message detailing the coupon invalidity
     */
    public InvalidCouponException(String message) {
        super(message);
    }
}
