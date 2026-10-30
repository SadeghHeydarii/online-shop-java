package org.example.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Review Value Object Tests")
class ReviewTest {

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    @DisplayName("Should create valid Review for allowed star ratings (1 to 5)")
    void shouldCreateValidReview() {
        Review r1 = new Review("REV-1", "USR-10", "Reza", "PROD-5", 5, "Excellent product!", now);
        Review r2 = new Review("REV-2", "USR-11", "Sara", "PROD-5", 1, "Poor quality", now);

        assertEquals("REV-1", r1.reviewId());
        assertEquals("USR-10", r1.userId());
        assertEquals("Reza", r1.authorName());
        assertEquals("PROD-5", r1.productId());
        assertEquals(5, r1.rating());
        assertEquals("Excellent product!", r1.commentText());
        assertEquals(now, r1.createdAt());

        assertEquals(1, r2.rating());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, 6, 7, 10})
    @DisplayName("Should reject ratings outside 1..5 range")
    void shouldRejectInvalidRatings(int invalidRating) {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Review("REV-1", "USR-10", "Reza", "PROD-5", invalidRating, "Comment", now)
        );
        assertTrue(ex.getMessage().contains("Rating must be between 1 and 5"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject blank or null commentText")
    void shouldRejectInvalidComment(String invalidComment) {
        assertThrows(
                RuntimeException.class,
                () -> new Review("REV-1", "USR-10", "Reza", "PROD-5", 4, invalidComment, now)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject blank or null reviewId")
    void shouldRejectInvalidReviewId(String invalidId) {
        assertThrows(
                RuntimeException.class,
                () -> new Review(invalidId, "USR-10", "Reza", "PROD-5", 4, "Comment", now)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject blank or null userId")
    void shouldRejectInvalidUserId(String invalidUserId) {
        assertThrows(
                RuntimeException.class,
                () -> new Review("REV-1", invalidUserId, "Reza", "PROD-5", 4, "Comment", now)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject blank or null authorName")
    void shouldRejectInvalidAuthorName(String invalidAuthor) {
        assertThrows(
                RuntimeException.class,
                () -> new Review("REV-1", "USR-10", invalidAuthor, "PROD-5", 4, "Comment", now)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject blank or null productId")
    void shouldRejectInvalidProductId(String invalidProdId) {
        assertThrows(
                RuntimeException.class,
                () -> new Review("REV-1", "USR-10", "Reza", invalidProdId, 4, "Comment", now)
        );
    }

    @Test
    @DisplayName("Should reject null createdAt")
    void shouldRejectNullCreatedAt() {
        assertThrows(
                NullPointerException.class,
                () -> new Review("REV-1", "USR-10", "Reza", "PROD-5", 4, "Comment", null)
        );
    }

    @Test
    @DisplayName("Should verify structural equality and hashCode for Review")
    void shouldVerifyStructuralEquality() {
        Review r1 = new Review("REV-1", "USR-10", "Reza", "PROD-5", 5, "Great", now);
        Review r2 = new Review("REV-1", "USR-10", "Reza", "PROD-5", 5, "Great", now);
        Review r3 = new Review("REV-2", "USR-10", "Reza", "PROD-5", 5, "Great", now);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3);
    }
}
