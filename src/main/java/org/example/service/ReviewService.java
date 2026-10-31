package org.example.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.example.exception.InvalidInputException;
import org.example.exception.UnauthorizedActionException;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.enums.OrderStatus;
import org.example.model.vo.Review;

/**
 * Application service managing product ratings and customer reviews.
 * Enforces verified-purchase constraints, character limits, and synchronizes aggregate rating metrics on products.
 *
 * <p>Specification compliance:
 * <ul>
 *   <li>Conforms strictly to Section 5.6 and Figure 2 of domain-model-specification.</li>
 *   <li>Requires users to have purchased the product in a paid or delivered order before leaving a review.</li>
 *   <li>Enforces comment text length boundaries (1 to {@value #MAX_REVIEW_LENGTH} characters).</li>
 *   <li>Automatically updates the product's running rating statistics via {@link Product#updateRatingStats(int)}.</li>
 * </ul>
 */
public class ReviewService {

    public static final int MAX_REVIEW_LENGTH = 2000;

    private final Map<String, List<Review>> productReviewsStore;
    private final OrderService orderService;
    private final ProductService productService;

    /**
     * Constructs a ReviewService with new concurrent storage and injected domain services.
     *
     * @param orderService   service managing user order history for verified purchase checks
     * @param productService service managing catalog products and aggregate rating updates
     */
    public ReviewService(OrderService orderService, ProductService productService) {
        this(new ConcurrentHashMap<>(), orderService, productService);
    }

    /**
     * Parameterized constructor enabling full dependency injection for testing.
     *
     * @param productReviewsStore in-memory store mapping product IDs to lists of customer reviews
     * @param orderService        order service for purchase history validation
     * @param productService      product service for product lookup and rating synchronization
     */
    public ReviewService(Map<String, List<Review>> productReviewsStore,
                         OrderService orderService,
                         ProductService productService) {
        this.productReviewsStore = Objects.requireNonNull(productReviewsStore, "productReviewsStore cannot be null");
        this.orderService = Objects.requireNonNull(orderService, "orderService cannot be null");
        this.productService = Objects.requireNonNull(productService, "productService cannot be null");
    }

    /**
     * Submits a new product review and rating after validating that the user is a verified purchaser.
     *
     * <p>Contract:
     * <ul>
     *   <li>REQUIRES: rating between 1 and 5, comment non-blank and &lt;= {@value #MAX_REVIEW_LENGTH} characters,
     *       userId has purchased product in a paid/delivered order.</li>
     *   <li>MODIFIES: productReviewsStore, product</li>
     *   <li>EFFECTS: Creates Review, appends to store, updates product rating statistics;
     *                throws {@link UnauthorizedActionException} if user did not purchase the product.</li>
     * </ul>
     *
     * @param userId     unique identifier of the reviewing user
     * @param authorName display name of the reviewer
     * @param productId  unique identifier of the product being reviewed
     * @param rating     integer star rating from 1 to 5 inclusive
     * @param comment    written feedback text (non-blank, up to {@value #MAX_REVIEW_LENGTH} characters)
     * @throws InvalidInputException       if any argument violates input validation or comment length
     * @throws UnauthorizedActionException if the user has not purchased the product in a qualified order
     */
    public void addReview(String userId, String authorName, String productId, int rating, String comment) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be null or blank");
        }
        if (authorName == null || authorName.isBlank()) {
            throw new InvalidInputException("Author name cannot be null or blank");
        }
        if (productId == null || productId.isBlank()) {
            throw new InvalidInputException("Product ID cannot be null or blank");
        }
        if (rating < 1 || rating > 5) {
            throw new InvalidInputException("Rating must be between 1 and 5 inclusive, got: " + rating);
        }
        if (comment == null || comment.isBlank() || comment.length() > MAX_REVIEW_LENGTH) {
            throw new InvalidInputException("Review text must contain 1 to " + MAX_REVIEW_LENGTH + " characters");
        }

        String targetUserId = userId.trim();
        String targetProductId = productId.trim();

        // 1. Verify the product exists in the catalog
        Product product = productService.getProductById(targetProductId);

        // 2. Verify that the user has purchased this product in a qualified completed order
        boolean hasPurchased = hasUserPurchasedProduct(targetUserId, targetProductId);
        if (!hasPurchased) {
            throw new UnauthorizedActionException(
                    "User '" + targetUserId + "' cannot review product '" + targetProductId
                            + "' because they have not purchased it in a completed or paid order"
            );
        }

        // 3. Create the immutable Review value object
        String reviewId = "rev-" + UUID.randomUUID().toString();
        Review review = new Review(
                reviewId,
                targetUserId,
                authorName.trim(),
                targetProductId,
                rating,
                comment.trim(),
                LocalDateTime.now()
        );

        // 4. Save review to thread-safe list in store
        productReviewsStore.computeIfAbsent(targetProductId, k -> new CopyOnWriteArrayList<>())
                .add(review);

        // 5. Update running rating metrics on the product entity
        product.updateRatingStats(rating);
    }

    /**
     * Retrieves all recorded customer reviews for a given product.
     *
     * @param productId unique identifier of the product
     * @return unmodifiable list of customer reviews for the product
     * @throws InvalidInputException if {@code productId} is null or blank
     */
    public List<Review> getProductReviews(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new InvalidInputException("Product ID cannot be null or blank");
        }
        List<Review> reviews = productReviewsStore.get(productId.trim());
        if (reviews == null || reviews.isEmpty()) {
            return List.of();
        }
        return List.copyOf(reviews);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private boolean hasUserPurchasedProduct(String userId, String productId) {
        List<Order> userOrders = orderService.getOrderHistory(userId);
        return userOrders.stream()
                .filter(this::isPaidOrCompletedOrder)
                .flatMap(order -> order.getItems().stream())
                .anyMatch(item -> item.productId().equals(productId));
    }

    private boolean isPaidOrCompletedOrder(Order order) {
        OrderStatus status = order.getStatus();
        return status == OrderStatus.PAID
                || status == OrderStatus.PROCESSING
                || status == OrderStatus.SHIPPED
                || status == OrderStatus.DELIVERED;
    }
}
