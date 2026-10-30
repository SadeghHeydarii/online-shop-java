package org.example.model.vo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable value object representing a customer review and rating on a product.
 *
 * <p>Abstraction Function:
 * AF(r) = a customer product review identified by r.reviewId, authored by user r.userId
 *         (display name r.authorName), rating product r.productId with r.rating stars (1-5),
 *         commentary r.commentText, recorded at r.createdAt.
 *
 * <p>Representation Invariant:
 * RI: reviewId != null && !reviewId.isBlank()
 *     && userId != null && !userId.isBlank()
 *     && authorName != null && !authorName.isBlank()
 *     && productId != null && !productId.isBlank()
 *     && rating >= 1 && rating <= 5
 *     && commentText != null && !commentText.isBlank()
 *     && createdAt != null.
 *
 * @param reviewId    The unique review identifier.
 * @param userId      The reviewer user's unique identifier.
 * @param authorName  The reviewer's display name.
 * @param productId   The reviewed product's identifier.
 * @param rating      The integer star rating from 1 to 5.
 * @param commentText The written feedback text.
 * @param createdAt   The creation timestamp.
 */
public record Review(
        String reviewId,
        String userId,
        String authorName,
        String productId,
        int rating,
        String commentText,
        LocalDateTime createdAt) {

    /**
     * Compact constructor validating review invariants.
     */
    public Review {
        Objects.requireNonNull(reviewId, "reviewId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(authorName, "authorName cannot be null");
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(commentText, "commentText cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");

        if (reviewId.isBlank()) {
            throw new IllegalArgumentException("reviewId cannot be blank");
        }
        if (userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be blank");
        }
        if (authorName.isBlank()) {
            throw new IllegalArgumentException("authorName cannot be blank");
        }
        if (productId.isBlank()) {
            throw new IllegalArgumentException("productId cannot be blank");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "Rating must be between 1 and 5, but received: " + rating);
        }
        if (commentText.isBlank()) {
            throw new IllegalArgumentException("commentText cannot be blank");
        }
    }
}
