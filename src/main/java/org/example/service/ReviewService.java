package org.example.service;

import java.time.Clock;
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
import org.example.service.api.ProductLookupPort;
import org.example.service.api.PurchaseHistoryQuery;
import org.example.service.api.ReviewUseCase;
import org.example.service.port.IdGenerator;
import org.example.service.port.ReviewRepository;

/**
 * Application service managing product ratings and verified-purchase customer reviews
 * via segregated review, product-lookup, purchase-history, ID, and time ports (SRP, ISP, DIP).
 */
public class ReviewService implements ReviewUseCase {

    public static final int MAX_REVIEW_LENGTH = 2000;

    private final ReviewRepository reviewRepository;
    private final ProductLookupPort productLookupPort;
    private final PurchaseHistoryQuery purchaseHistoryQuery;
    private final IdGenerator idGenerator;
    private final Clock clock;

    /**
     * Constructs a {@code ReviewService} with all required segregated ports and clock.
     *
     * @param reviewRepository     persistence port for review value objects
     * @param productLookupPort    narrow lookup port verifying product existence
     * @param purchaseHistoryQuery narrow read-only query checking verified customer purchases
     * @param idGenerator          identifier generator for review IDs
     * @param clock                clock for deterministic review timestamps
     */
    public ReviewService(ReviewRepository reviewRepository,
                         ProductLookupPort productLookupPort,
                         PurchaseHistoryQuery purchaseHistoryQuery,
                         IdGenerator idGenerator,
                         Clock clock) {
        this.reviewRepository = Objects.requireNonNull(reviewRepository, "reviewRepository cannot be null");
        this.productLookupPort = Objects.requireNonNull(productLookupPort, "productLookupPort cannot be null");
        this.purchaseHistoryQuery = Objects.requireNonNull(purchaseHistoryQuery, "purchaseHistoryQuery cannot be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /**
     * Deprecated convenience constructor for legacy callers until Phase 5 migration.
     *
     * @param orderService   legacy order facade
     * @param productService legacy product facade
     * @deprecated Use {@link #ReviewService(ReviewRepository, ProductLookupPort, PurchaseHistoryQuery, IdGenerator, Clock)}.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public ReviewService(OrderService orderService, ProductService productService) {
        this(new ConcurrentHashMap<>(), orderService, productService);
    }

    /**
     * Deprecated bridge constructor accepting raw map storage for legacy callers until Phase 5.
     *
     * @param productReviewsStore in-memory map keyed by product ID
     * @param orderService        legacy order facade
     * @param productService      legacy product facade
     * @deprecated Use {@link #ReviewService(ReviewRepository, ProductLookupPort, PurchaseHistoryQuery, IdGenerator, Clock)}.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public ReviewService(Map<String, List<Review>> productReviewsStore,
                         OrderService orderService,
                         ProductService productService) {
        this(
                new MapBackedReviewRepository(Objects.requireNonNull(productReviewsStore, "productReviewsStore cannot be null")),
                Objects.requireNonNull(productService, "productService cannot be null")::getProductById,
                new LegacyPurchaseHistoryAdapter(Objects.requireNonNull(orderService, "orderService cannot be null")),
                prefix -> prefix + "-" + UUID.randomUUID(),
                Clock.systemDefaultZone()
        );
    }

    @Override
    public void addReview(String userId, String authorName, String productId, int rating, String comment) {
        String targetUserId = requireNonBlank(userId, "User ID");
        String targetAuthor = requireNonBlank(authorName, "Author name");
        String targetProductId = requireNonBlank(productId, "Product ID");
        if (rating < 1 || rating > 5) {
            throw new InvalidInputException("Rating must be between 1 and 5 inclusive, got: " + rating);
        }
        if (comment == null || comment.isBlank() || comment.length() > MAX_REVIEW_LENGTH) {
            throw new InvalidInputException("Review text must contain 1 to " + MAX_REVIEW_LENGTH + " characters");
        }

        Product product = productLookupPort.getProductById(targetProductId);

        if (!purchaseHistoryQuery.hasPurchasedProduct(targetUserId, targetProductId)) {
            throw new UnauthorizedActionException(
                    "User '" + targetUserId + "' cannot review product '" + targetProductId
                            + "' because they have not purchased it in a completed or paid order"
            );
        }

        Review review = new Review(
                idGenerator.nextId("rev"),
                targetUserId,
                targetAuthor,
                targetProductId,
                rating,
                comment.trim(),
                LocalDateTime.now(clock)
        );

        reviewRepository.addReview(review);
        product.updateRatingStats(rating);
    }

    @Override
    public List<Review> getProductReviews(String productId) {
        String targetProductId = requireNonBlank(productId, "Product ID");
        return reviewRepository.findByProductId(targetProductId);
    }

    private static String requireNonBlank(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(fieldLabel + " cannot be null or blank");
        }
        return value.trim();
    }

    // Legacy transition adapters removed in Phase 5 along with deprecated constructors
    private record MapBackedReviewRepository(Map<String, List<Review>> store) implements ReviewRepository {
        @Override
        public void addReview(Review review) {
            store.computeIfAbsent(review.productId(), k -> new CopyOnWriteArrayList<>()).add(review);
        }

        @Override
        public List<Review> findByProductId(String productId) {
            List<Review> list = store.get(productId.trim());
            return (list == null || list.isEmpty()) ? List.of() : List.copyOf(list);
        }
    }

    @SuppressWarnings("deprecation")
    private record LegacyPurchaseHistoryAdapter(OrderService orderService) implements PurchaseHistoryQuery {
        @Override
        public List<Order> getOrderHistory(String userId) {
            return orderService.getOrderHistory(userId);
        }

        @Override
        public boolean hasPurchasedProduct(String userId, String productId) {
            return orderService.getOrderHistory(userId).stream()
                    .filter(ReviewService::isPaidOrCompletedOrder)
                    .flatMap(order -> order.getItems().stream())
                    .anyMatch(item -> item.productId().equals(productId));
        }
    }

    private static boolean isPaidOrCompletedOrder(Order order) {
        OrderStatus status = order.getStatus();
        return status == OrderStatus.PAID
                || status == OrderStatus.PROCESSING
                || status == OrderStatus.SHIPPED
                || status == OrderStatus.DELIVERED;
    }
}
