package org.example.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.exception.AuthenticationException;
import org.example.exception.DuplicateUserException;
import org.example.exception.InvalidInputException;
import org.example.infrastructure.persistence.InMemoryCredentialRepository;
import org.example.infrastructure.persistence.InMemorySessionRepository;
import org.example.infrastructure.persistence.InMemoryUserRepository;
import org.example.infrastructure.security.Sha256PasswordHasher;
import org.example.model.entity.User;
import org.example.service.port.CredentialRepository;
import org.example.service.port.PasswordHasher;
import org.example.service.port.SessionRepository;
import org.example.service.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {

    private UserRepository userRepository;
    private CredentialRepository credentialRepository;
    private SessionRepository sessionRepository;
    private PasswordHasher passwordHasher;
    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T14:00:00Z"), ZoneOffset.UTC);
        AtomicInteger seq = new AtomicInteger(1);

        userRepository = new InMemoryUserRepository();
        credentialRepository = new InMemoryCredentialRepository();
        sessionRepository = new InMemorySessionRepository();
        passwordHasher = new Sha256PasswordHasher();

        authService = new AuthenticationService(
                userRepository,
                credentialRepository,
                sessionRepository,
                passwordHasher,
                prefix -> prefix + "-" + seq.getAndIncrement(),
                clock
        );
    }

    @Test
    void registersLogsInResolvesAndLogsOutUsingPorts() {
        User sara = authService.register("Sara Ahmadi", "sara@aut.ac.ir", "09121112233", "Secret@1");

        assertEquals("usr-1", sara.getId());
        assertEquals(LocalDateTime.of(2026, 11, 25, 14, 0), sara.getCreatedAt());
        assertTrue(passwordHasher.matches("Secret@1",
                credentialRepository.findPasswordHashByUserId("usr-1").orElseThrow()));

        String token = authService.login("sara@aut.ac.ir", "Secret@1");
        assertEquals("tok-2", token);
        assertEquals(sara, authService.getAuthenticatedUser(token));

        authService.logout(token);
        assertNull(authService.getAuthenticatedUser(token));
    }

    @Test
    void rejectsDuplicatesBadPasswordsAndInactiveAccounts() {
        User ali = authService.register("Ali", "ali@aut.ac.ir", "09123334455", "Secret@1");

        assertThrows(DuplicateUserException.class,
                () -> authService.register("Other", "ali@aut.ac.ir", "09129998877", "Secret@1"));
        assertThrows(DuplicateUserException.class,
                () -> authService.register("Other", "other@aut.ac.ir", "09123334455", "Secret@1"));
        assertThrows(InvalidInputException.class,
                () -> authService.register("Ali", "a@aut.ac.ir", "09120001122", "123"));
        assertThrows(AuthenticationException.class,
                () -> authService.login("ali@aut.ac.ir", "wrongPass"));

        String token = authService.login("09123334455", "Secret@1");
        ali.deactivate();
        userRepository.save(ali);

        assertNull(authService.getAuthenticatedUser(token));
        assertThrows(AuthenticationException.class,
                () -> authService.login("ali@aut.ac.ir", "Secret@1"));
    }
}
