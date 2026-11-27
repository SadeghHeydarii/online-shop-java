package org.example.service.api;

import java.util.List;
import org.example.model.vo.Review;

/**
 * Inbound use-case API for submitting verified-purchase reviews and querying product reviews (ISP).
 */
public interface ReviewUseCase {

    /**
     * Submits a new product review and rating after verifying the customer purchased the product.
     *
     * @param userId     non-blank reviewing customer ID
     * @param authorName non-blank display name
     * @param productId  non-blank product ID
     * @param rating     star score between 1 and 5
     * @param comment    non-blank feedback within maximum length limits
     */
    void addReview(String userId, String authorName, String productId, int rating, String comment);

    /**
     * Returns all reviews recorded for the given product.
     *
     * @param productId non-blank product ID
     * @return unmodifiable list of reviews
     */
    List<Review> getProductReviews(String productId);
}
