package org.example.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.exception.UnauthorizedActionException;
import org.example.model.vo.Category;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.enums.OrderStatus;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.model.vo.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReviewService Tests")
class ReviewServiceTest {

    private OrderService orderService;
    private ProductService productService;
    private ReviewService reviewService;
    private Product targetProduct;
    private Product otherProduct;
    private Address dummyAddress;

    @BeforeEach
    void setUp() {
        orderService = new OrderService();
        productService = new ProductService();
        reviewService = new ReviewService(orderService, productService);

        Category tech = new Category("CAT-TECH", "Technology", "Tech equipment");
        productService.registerCategory(tech);

        Money price = new Money(new BigDecimal("100000.00"), "IRR");
        productService.addProduct("Wireless Headphones", "Noise cancelling", price, 50, "CAT-TECH");
        productService.addProduct("Smart Watch", "Fitness tracker", price, 30, "CAT-TECH");

        targetProduct = productService.searchProducts("Wireless Headphones").getFirst();
        otherProduct = productService.searchProducts("Smart Watch").getFirst();
        dummyAddress = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Azadi Ave", "1122334455", "Home", true);
    }

    private void createPurchase(String orderId, String userId, String productId, OrderStatus targetStatus) {
        Money price = new Money(new BigDecimal("100000.00"), "IRR");
        OrderItem item = new OrderItem(productId, "Purchased Item", price, new Quantity(1), price);
        Money zero = Money.ZERO;
        Order order = new Order(
                orderId,
                userId,
                List.of(item),
                dummyAddress,
                price,
                zero,
                zero,
                price,
                LocalDateTime.now()
        );

        if (targetStatus == OrderStatus.PAID) {
            order.markAsPaid(LocalDateTime.now());
        } else if (targetStatus == OrderStatus.PROCESSING) {
            order.markAsPaid(LocalDateTime.now());
            order.updateStatus(OrderStatus.PROCESSING);
        } else if (targetStatus == OrderStatus.SHIPPED) {
            order.markAsPaid(LocalDateTime.now());
            order.updateStatus(OrderStatus.PROCESSING);
            order.updateStatus(OrderStatus.SHIPPED);
        } else if (targetStatus == OrderStatus.DELIVERED) {
            order.markAsPaid(LocalDateTime.now());
            order.updateStatus(OrderStatus.PROCESSING);
            order.updateStatus(OrderStatus.SHIPPED);
            order.updateStatus(OrderStatus.DELIVERED);
        } else if (targetStatus == OrderStatus.PAYMENT_FAILED) {
            order.markAsPaymentFailed();
        } else if (targetStatus == OrderStatus.CANCELLED) {
            order.cancel();
        }

        orderService.recordOrder(order);
    }

    @Nested
    @DisplayName("addReview Tests")
    class AddReviewTests {

        @Test
        @DisplayName("Verified purchaser with PAID order can submit review and update rating stats")
        void addReviewSuccessPaidOrder() {
            createPurchase("ord-1", "user-1", targetProduct.getId(), OrderStatus.PAID);

            reviewService.addReview("user-1", "Alice", targetProduct.getId(), 5, "Outstanding sound quality!");

            List<Review> reviews = reviewService.getProductReviews(targetProduct.getId());
            assertEquals(1, reviews.size());
            Review rev = reviews.getFirst();
            assertTrue(rev.reviewId().startsWith("rev-"));
            assertEquals("user-1", rev.userId());
            assertEquals("Alice", rev.authorName());
            assertEquals(targetProduct.getId(), rev.productId());
            assertEquals(5, rev.rating());
            assertEquals("Outstanding sound quality!", rev.commentText());

            // Product entity stats must update
            assertEquals(1, targetProduct.getReviewCount());
            assertEquals(5.0, targetProduct.getAverageRating(), 0.001);
        }

        @Test
        @DisplayName("Verified purchaser with DELIVERED order can submit review")
        void addReviewSuccessDeliveredOrder() {
            createPurchase("ord-2", "user-2", targetProduct.getId(), OrderStatus.DELIVERED);

            reviewService.addReview("user-2", "Bob", targetProduct.getId(), 4, "Good battery life");

            List<Review> reviews = reviewService.getProductReviews(targetProduct.getId());
            assertEquals(1, reviews.size());
            assertEquals(4, reviews.getFirst().rating());
            assertEquals(4.0, targetProduct.getAverageRating(), 0.001);
        }

        @Test
        @DisplayName("Multiple reviews incrementally update running average rating")
        void multipleReviewsAggregateStats() {
            createPurchase("ord-10", "u1", targetProduct.getId(), OrderStatus.PAID);
            createPurchase("ord-20", "u2", targetProduct.getId(), OrderStatus.PAID);

            reviewService.addReview("u1", "User One", targetProduct.getId(), 5, "Great!");
            reviewService.addReview("u2", "User Two", targetProduct.getId(), 3, "Average.");

            assertEquals(2, targetProduct.getReviewCount());
            assertEquals(4.0, targetProduct.getAverageRating(), 0.001);
        }

        @Test
        @DisplayName("User who has not purchased product throws UnauthorizedActionException")
        void unpurchasedThrowsUnauthorized() {
            assertThrows(UnauthorizedActionException.class, () ->
                    reviewService.addReview("user-stranger", "Mallory", targetProduct.getId(), 1, "Spam review"));

            assertTrue(reviewService.getProductReviews(targetProduct.getId()).isEmpty());
            assertEquals(0, targetProduct.getReviewCount());
        }

        @Test
        @DisplayName("User with only PENDING order throws UnauthorizedActionException")
        void pendingOrderThrowsUnauthorized() {
            createPurchase("ord-3", "user-3", targetProduct.getId(), OrderStatus.PENDING);

            assertThrows(UnauthorizedActionException.class, () ->
                    reviewService.addReview("user-3", "Charlie", targetProduct.getId(), 4, "Not paid yet"));
        }

        @Test
        @DisplayName("User with only PAYMENT_FAILED order throws UnauthorizedActionException")
        void paymentFailedThrowsUnauthorized() {
            createPurchase("ord-4", "user-4", targetProduct.getId(), OrderStatus.PAYMENT_FAILED);

            assertThrows(UnauthorizedActionException.class, () ->
                    reviewService.addReview("user-4", "Dave", targetProduct.getId(), 4, "Payment declined"));
        }

        @Test
        @DisplayName("User with CANCELLED order throws UnauthorizedActionException")
        void cancelledOrderThrowsUnauthorized() {
            createPurchase("ord-5", "user-5", targetProduct.getId(), OrderStatus.CANCELLED);

            assertThrows(UnauthorizedActionException.class, () ->
                    reviewService.addReview("user-5", "Eve", targetProduct.getId(), 2, "Cancelled order"));
        }

        @Test
        @DisplayName("User who purchased different product throws UnauthorizedActionException")
        void differentProductPurchasedThrows() {
            createPurchase("ord-6", "user-6", otherProduct.getId(), OrderStatus.PAID);

            assertThrows(UnauthorizedActionException.class, () ->
                    reviewService.addReview("user-6", "Frank", targetProduct.getId(), 5, "Wrong product"));
        }

        @Test
        @DisplayName("Reviewing non-existent product throws ProductNotFoundException")
        void nonExistentProductThrows() {
            createPurchase("ord-7", "user-7", "ghost-prod", OrderStatus.PAID);

            assertThrows(ProductNotFoundException.class, () ->
                    reviewService.addReview("user-7", "Grace", "ghost-prod", 5, "Ghost item"));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 6, 10})
        @DisplayName("Rating outside [1, 5] throws InvalidInputException")
        void invalidRatingThrows(int invalidRating) {
            createPurchase("ord-8", "user-8", targetProduct.getId(), OrderStatus.PAID);

            assertThrows(InvalidInputException.class, () ->
                    reviewService.addReview("user-8", "Heidi", targetProduct.getId(), invalidRating, "Invalid rating"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Blank or null comment throws InvalidInputException")
        void invalidCommentThrows(String comment) {
            createPurchase("ord-9", "user-9", targetProduct.getId(), OrderStatus.PAID);

            assertThrows(InvalidInputException.class, () ->
                    reviewService.addReview("user-9", "Ivan", targetProduct.getId(), 4, comment));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Blank or null authorName throws InvalidInputException")
        void invalidAuthorNameThrows(String author) {
            createPurchase("ord-11", "user-10", targetProduct.getId(), OrderStatus.PAID);

            assertThrows(InvalidInputException.class, () ->
                    reviewService.addReview("user-10", author, targetProduct.getId(), 4, "Valid comment"));
        }
    }

    @Nested
    @DisplayName("getProductReviews Tests")
    class GetProductReviewsTests {

        @Test
        @DisplayName("Product with no reviews returns empty list")
        void noReviewsReturnsEmpty() {
            List<Review> reviews = reviewService.getProductReviews(targetProduct.getId());
            assertNotNull(reviews);
            assertTrue(reviews.isEmpty());
        }

        @Test
        @DisplayName("Returned list is defensive copy and cannot mutate internal store")
        void defensiveCopy() {
            createPurchase("ord-1", "user-1", targetProduct.getId(), OrderStatus.PAID);
            reviewService.addReview("user-1", "Alice", targetProduct.getId(), 5, "Superb");

            List<Review> reviews = reviewService.getProductReviews(targetProduct.getId());
            assertEquals(1, reviews.size());

            assertThrows(UnsupportedOperationException.class, () ->
                    reviews.add(new Review("r-bad", "u-bad", "Bad", targetProduct.getId(), 1, "Fake", LocalDateTime.now())));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Blank or null product ID throws InvalidInputException")
        void invalidProductIdThrows(String productId) {
            assertThrows(InvalidInputException.class, () -> reviewService.getProductReviews(productId));
        }
    }
}
