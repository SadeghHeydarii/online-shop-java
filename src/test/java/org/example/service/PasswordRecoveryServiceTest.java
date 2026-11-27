package org.example.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.example.exception.AuthenticationException;
import org.example.exception.InvalidInputException;
import org.example.infrastructure.persistence.InMemoryCredentialRepository;
import org.example.infrastructure.persistence.InMemoryPasswordResetRepository;
import org.example.infrastructure.persistence.InMemorySessionRepository;
import org.example.infrastructure.persistence.InMemoryUserRepository;
import org.example.infrastructure.security.Sha256PasswordHasher;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.service.port.CredentialRepository;
import org.example.service.port.PasswordHasher;
import org.example.service.port.PasswordResetRepository;
import org.example.service.port.SessionRepository;
import org.example.service.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordRecoveryServiceTest {

    private UserRepository userRepository;
    private CredentialRepository credentialRepository;
    private SessionRepository sessionRepository;
    private PasswordResetRepository resetRepository;
    private PasswordHasher passwordHasher;
    private AtomicReference<Instant> currentInstant;
    private PasswordRecoveryService recoveryService;
    private User user;

    @BeforeEach
    void setUp() {
        currentInstant = new AtomicReference<>(Instant.parse("2026-11-25T15:00:00Z"));
        Clock mutableClock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return currentInstant.get();
            }
        };

        userRepository = new InMemoryUserRepository();
        credentialRepository = new InMemoryCredentialRepository();
        sessionRepository = new InMemorySessionRepository();
        resetRepository = new InMemoryPasswordResetRepository();
        passwordHasher = new Sha256PasswordHasher();

        String initialHash = passwordHasher.hash("OldPass1");
        user = new User("usr-1", "Reza", "reza@aut.ac.ir", "09121112233",
                initialHash, UserRole.CUSTOMER, LocalDateTime.of(2026, 11, 25, 14, 0));
        userRepository.save(user);
        credentialRepository.savePasswordHash("usr-1", initialHash);
        sessionRepository.saveSession("tok-active", "usr-1");

        recoveryService = new PasswordRecoveryService(
                userRepository,
                credentialRepository,
                sessionRepository,
                resetRepository,
                passwordHasher,
                () -> "482913",
                mutableClock
        );
    }

    @Test
    void resetsPasswordBeforeExpiryAndRevokesActiveSessions() {
        String code = recoveryService.requestPasswordReset("reza@aut.ac.ir");
        assertEquals("482913", code);

        // 9 minutes 59 seconds later: still valid
        currentInstant.set(Instant.parse("2026-11-25T15:09:59Z"));
        recoveryService.resetPassword("09121112233", code, "NewPass@2");

        String updatedHash = credentialRepository.findPasswordHashByUserId("usr-1").orElseThrow();
        assertTrue(passwordHasher.matches("NewPass@2", updatedHash));
        assertTrue(sessionRepository.findUserIdByToken("tok-active").isEmpty());
        assertTrue(resetRepository.findResetToken("usr-1").isEmpty());

        // Single-use token cannot be reused
        assertThrows(AuthenticationException.class,
                () -> recoveryService.resetPassword("reza@aut.ac.ir", code, "AnotherPass3"));
    }

    @Test
    void rejectsExpiredRecoveryCodeAtExactTenMinuteBoundary() {
        String code = recoveryService.requestPasswordReset("reza@aut.ac.ir");

        currentInstant.set(Instant.parse("2026-11-25T15:10:00Z"));
        assertThrows(AuthenticationException.class,
                () -> recoveryService.resetPassword("reza@aut.ac.ir", code, "NewPass@2"));
    }

    @Test
    void rejectsInvalidCodeWeakPasswordOrUnknownAccount() {
        recoveryService.requestPasswordReset("reza@aut.ac.ir");

        assertThrows(AuthenticationException.class,
                () -> recoveryService.resetPassword("reza@aut.ac.ir", "000000", "ValidPass1"));
        assertThrows(InvalidInputException.class,
                () -> recoveryService.resetPassword("reza@aut.ac.ir", "482913", "short"));
        assertThrows(AuthenticationException.class,
                () -> recoveryService.requestPasswordReset("nobody@aut.ac.ir"));
    }
}
