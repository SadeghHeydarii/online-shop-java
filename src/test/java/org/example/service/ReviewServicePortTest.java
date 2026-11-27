package org.example.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.exception.UnauthorizedActionException;
import org.example.infrastructure.persistence.InMemoryReviewRepository;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.vo.Money;
import org.example.model.vo.Review;
import org.example.service.api.ProductLookupPort;
import org.example.service.api.PurchaseHistoryQuery;
import org.example.service.port.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReviewServicePortTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-25T10:15:00Z"), ZoneOffset.UTC);
    private ReviewRepository reviewRepository;
    private Product product;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewRepository = new InMemoryReviewRepository();
        product = new Product("prod-1", "Clean Code", "Book",
                new Money(new BigDecimal("150000.00"), "IRR"), 20, "CAT-BOOK");
        AtomicInteger seq = new AtomicInteger(1);

        ProductLookupPort productLookup = id -> {
            if ("prod-1".equals(id)) {
                return product;
            }
            throw new ProductNotFoundException("Not found: " + id);
        };

        PurchaseHistoryQuery purchaseHistory = new PurchaseHistoryQuery() {
            @Override
            public List<Order> getOrderHistory(String userId) {
                return List.of();
            }

            @Override
            public boolean hasPurchasedProduct(String userId, String productId) {
                return "verified-buyer".equals(userId) && "prod-1".equals(productId);
            }
        };

        reviewService = new ReviewService(
                reviewRepository,
                productLookup,
                purchaseHistory,
                prefix -> prefix + "-" + seq.getAndIncrement(),
                fixedClock
        );
    }

    @Test
    void recordsReviewUpdatesProductStatsAndUsesClockTimestamp() {
        reviewService.addReview("verified-buyer", "Sara", "prod-1", 5, "Excellent book!");

        List<Review> stored = reviewService.getProductReviews("prod-1");
        assertEquals(1, stored.size());
        assertEquals("rev-1", stored.getFirst().reviewId());
        assertEquals(LocalDateTime.of(2026, 11, 25, 10, 15), stored.getFirst().createdAt());
        assertEquals(5.0, product.getAverageRating(), 1e-6);
        assertEquals(1, product.getReviewCount());
    }

    @Test
    void enforcesVerifiedPurchaseAndValidInputsViaPorts() {
        assertThrows(UnauthorizedActionException.class,
                () -> reviewService.addReview("stranger", "Bob", "prod-1", 4, "Nice"));
        assertThrows(ProductNotFoundException.class,
                () -> reviewService.addReview("verified-buyer", "Sara", "missing", 5, "Great"));
        assertThrows(InvalidInputException.class,
                () -> reviewService.addReview("verified-buyer", "Sara", "prod-1", 6, "Out of range"));
    }
}
