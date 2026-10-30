package org.example.exception;

/**
 * Domain exception thrown when a requested product cannot be found in the catalog.
 */
public class ProductNotFoundException extends RuntimeException {

    /**
     * Constructs a new ProductNotFoundException with the specified detail message.
     *
     * @param message descriptive message detailing the missing product
     */
    public ProductNotFoundException(String message) {
        super(message);
    }
}
