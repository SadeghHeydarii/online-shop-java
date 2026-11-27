package org.example.service.port;

import java.util.List;
import org.example.model.vo.Review;

/**
 * Outbound persistence port for storing and querying product {@link Review} value objects.
 */
public interface ReviewRepository {

    /**
     * Appends a customer review for its target product.
     *
     * @param review non-null review value object
     */
    void addReview(Review review);

    /**
     * Retrieves all recorded reviews for the given product.
     *
     * @param productId non-blank product identifier
     * @return unmodifiable list of reviews (empty if none exist)
     * @throws IllegalArgumentException if {@code productId} is null or blank
     */
    List<Review> findByProductId(String productId);
}
