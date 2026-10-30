package org.example.exception;

/**
 * Domain exception thrown when a requested product category cannot be found.
 */
public class CategoryNotFoundException extends RuntimeException {

    /**
     * Constructs a new CategoryNotFoundException with the specified detail message.
     *
     * @param message descriptive message detailing the missing category
     */
    public CategoryNotFoundException(String message) {
        super(message);
    }
}
