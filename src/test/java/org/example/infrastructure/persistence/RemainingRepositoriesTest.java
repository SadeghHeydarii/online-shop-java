package org.example.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.model.entity.Product;
import org.example.model.vo.CartItem;
import org.example.model.vo.Category;
import org.example.model.vo.Coupon;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.model.vo.Review;
import org.example.service.port.PasswordResetRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RemainingRepositoriesTest {

    @Test
    void productAndCategoryRepositoriesSaveAndQueryCleanly() {
        InMemoryCategoryRepository catRepo = new InMemoryCategoryRepository();
        Category cat = new Category("CAT-1", "Books", "Academic books");
        catRepo.save(cat);
        assertEquals(cat, catRepo.findById("CAT-1").orElseThrow());
        assertEquals(1, catRepo.findAll().size());

        InMemoryProductRepository prodRepo = new InMemoryProductRepository();
        Product product = new Product("prod-1", "Clean Code", "Book", new Money(BigDecimal.TEN, "IRR"), 5, "CAT-1");
        prodRepo.save(product);
        assertEquals(product, prodRepo.findById("prod-1").orElseThrow());
        assertEquals(1, prodRepo.findAll().size());
    }

    @Test
    void couponReviewAndSnapshotRepositoriesWorkCorrectly() {
        InMemoryCouponRepository couponRepo = new InMemoryCouponRepository();
        Coupon coupon = new Coupon("SAVE10", 10, new Money(BigDecimal.TEN, "IRR"),
                new Money(BigDecimal.ONE, "IRR"), LocalDate.now().plusDays(1));
        couponRepo.save(coupon);
        assertEquals(coupon, couponRepo.findByCode(" save10 ").orElseThrow());

        InMemoryReviewRepository reviewRepo = new InMemoryReviewRepository();
        assertTrue(reviewRepo.findByProductId("prod-1").isEmpty());
        Review review = new Review("rev-1", "usr-1", "Ali", "prod-1", 5, "Great!", LocalDateTime.now());
        reviewRepo.addReview(review);
        assertEquals(List.of(review), reviewRepo.findByProductId("prod-1"));

        InMemoryCheckoutSnapshotRepository snapRepo = new InMemoryCheckoutSnapshotRepository();
        CartItem item = new CartItem("prod-1", "Clean Code", new Money(BigDecimal.TEN, "IRR"), new Quantity(2));
        snapRepo.saveSnapshot("ord-1", List.of(item));
        assertEquals(List.of(item), snapRepo.removeSnapshot("ord-1").orElseThrow());
        assertTrue(snapRepo.removeSnapshot("ord-1").isEmpty());
    }

    @Test
    void credentialSessionAndPasswordResetRepositoriesWorkCorrectly() {
        InMemoryCredentialRepository credRepo = new InMemoryCredentialRepository();
        credRepo.savePasswordHash("usr-1", "hash123");
        assertEquals("hash123", credRepo.findPasswordHashByUserId("usr-1").orElseThrow());
        assertTrue(credRepo.findPasswordHashByUserId("missing").isEmpty());

        InMemorySessionRepository sessionRepo = new InMemorySessionRepository();
        sessionRepo.saveSession("tok-1", "usr-1");
        sessionRepo.saveSession("tok-2", "usr-1");
        sessionRepo.saveSession("tok-3", "usr-2");
        assertEquals("usr-1", sessionRepo.findUserIdByToken("tok-1").orElseThrow());
        assertTrue(sessionRepo.findUserIdByToken(" ").isEmpty());
        sessionRepo.removeSession("tok-1");
        assertTrue(sessionRepo.findUserIdByToken("tok-1").isEmpty());
        sessionRepo.removeAllSessionsForUser("usr-1");
        assertTrue(sessionRepo.findUserIdByToken("tok-2").isEmpty());
        assertEquals("usr-2", sessionRepo.findUserIdByToken("tok-3").orElseThrow());

        InMemoryPasswordResetRepository resetRepo = new InMemoryPasswordResetRepository();
        PasswordResetRepository.ResetToken token = new PasswordResetRepository.ResetToken("h", LocalDateTime.now().plusMinutes(10));
        resetRepo.saveResetToken("usr-1", token);
        assertEquals(token, resetRepo.findResetToken("usr-1").orElseThrow());
        resetRepo.removeResetToken("usr-1");
        assertTrue(resetRepo.findResetToken("usr-1").isEmpty());
    }
}
