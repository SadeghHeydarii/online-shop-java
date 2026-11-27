package org.example.infrastructure.seed;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.infrastructure.catalog.DefaultNameSort;
import org.example.infrastructure.catalog.MapProductSortRegistry;
import org.example.infrastructure.persistence.InMemoryCategoryRepository;
import org.example.infrastructure.persistence.InMemoryCouponRepository;
import org.example.infrastructure.persistence.InMemoryCredentialRepository;
import org.example.infrastructure.persistence.InMemoryProductRepository;
import org.example.infrastructure.persistence.InMemoryUserRepository;
import org.example.infrastructure.security.Sha256PasswordHasher;
import org.example.model.entity.User;
import org.example.service.CatalogManagementService;
import org.example.service.CatalogQueryService;
import org.example.service.port.CategoryRepository;
import org.example.service.port.CouponRepository;
import org.example.service.port.CredentialRepository;
import org.example.service.port.PasswordHasher;
import org.example.service.port.ProductRepository;
import org.example.service.port.UserRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationDataSeederTest {

    @Test
    void seedsUsersCategoriesProductsAndCouponsThroughPortsDeterministically() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);
        AtomicInteger seq = new AtomicInteger(1);

        UserRepository userRepo = new InMemoryUserRepository();
        CredentialRepository credentialRepo = new InMemoryCredentialRepository();
        CategoryRepository categoryRepo = new InMemoryCategoryRepository();
        ProductRepository productRepo = new InMemoryProductRepository();
        CouponRepository couponRepo = new InMemoryCouponRepository();
        PasswordHasher passwordHasher = new Sha256PasswordHasher();

        CatalogManagementService catalogAdmin = new CatalogManagementService(
                productRepo, categoryRepo, prefix -> prefix + "-" + seq.getAndIncrement(), fixedClock
        );
        CatalogQueryService catalogQuery = new CatalogQueryService(
                productRepo, categoryRepo, new MapProductSortRegistry(new DefaultNameSort())
        );

        ApplicationDataSeeder.seed(
                userRepo, credentialRepo, categoryRepo, catalogAdmin, couponRepo, passwordHasher, fixedClock
        );

        User admin = userRepo.findByIdentifier("admin@shop.com").orElseThrow();
        User customer = userRepo.findByIdentifier("customer@shop.com").orElseThrow();

        assertEquals(LocalDateTime.of(2026, 11, 25, 12, 0), admin.getCreatedAt());
        assertTrue(passwordHasher.matches("Admin@123",
                credentialRepo.findPasswordHashByUserId(admin.getId()).orElseThrow()));
        assertTrue(passwordHasher.matches("Pass@123",
                credentialRepo.findPasswordHashByUserId(customer.getId()).orElseThrow()));
        assertEquals(2, customer.getAddresses().size());
        assertEquals(4, catalogQuery.getActiveCategories().size());
        assertEquals(6, catalogQuery.getAllActiveProducts().size());
        assertEquals(LocalDate.of(2027, 2, 25),
                couponRepo.findByCode("WELCOME20").orElseThrow().expiryDate());
        assertTrue(couponRepo.findByCode("AUT_SPECIAL").isPresent());
    }

    @Test
    void constructorRejectsNullDependencies() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);
        UserRepository userRepo = new InMemoryUserRepository();
        CredentialRepository credentialRepo = new InMemoryCredentialRepository();
        CategoryRepository categoryRepo = new InMemoryCategoryRepository();
        ProductRepository productRepo = new InMemoryProductRepository();
        CouponRepository couponRepo = new InMemoryCouponRepository();
        PasswordHasher passwordHasher = new Sha256PasswordHasher();
        CatalogManagementService catalogAdmin = new CatalogManagementService(
                productRepo, categoryRepo, prefix -> prefix + "-1", fixedClock);

        assertThrows(NullPointerException.class, () -> new ApplicationDataSeeder(
                null, credentialRepo, categoryRepo, catalogAdmin, couponRepo, passwordHasher, fixedClock));
        assertThrows(NullPointerException.class, () -> new ApplicationDataSeeder(
                userRepo, null, categoryRepo, catalogAdmin, couponRepo, passwordHasher, fixedClock));
        assertThrows(NullPointerException.class, () -> new ApplicationDataSeeder(
                userRepo, credentialRepo, null, catalogAdmin, couponRepo, passwordHasher, fixedClock));
        assertThrows(NullPointerException.class, () -> new ApplicationDataSeeder(
                userRepo, credentialRepo, categoryRepo, null, couponRepo, passwordHasher, fixedClock));
        assertThrows(NullPointerException.class, () -> new ApplicationDataSeeder(
                userRepo, credentialRepo, categoryRepo, catalogAdmin, null, passwordHasher, fixedClock));
        assertThrows(NullPointerException.class, () -> new ApplicationDataSeeder(
                userRepo, credentialRepo, categoryRepo, catalogAdmin, couponRepo, null, fixedClock));
        assertThrows(NullPointerException.class, () -> new ApplicationDataSeeder(
                userRepo, credentialRepo, categoryRepo, catalogAdmin, couponRepo, passwordHasher, null));
    }
}
