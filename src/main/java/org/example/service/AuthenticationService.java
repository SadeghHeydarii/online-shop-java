package org.example.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.example.exception.AuthenticationException;
import org.example.exception.DuplicateUserException;
import org.example.exception.InvalidInputException;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.model.vo.ContactInfo;
import org.example.service.api.AuthUseCase;
import org.example.service.port.CredentialRepository;
import org.example.service.port.IdGenerator;
import org.example.service.port.PasswordHasher;
import org.example.service.port.SessionRepository;
import org.example.service.port.UserRepository;

/**
 * Cohesive authentication and session-management service separated from password recovery
 * and profile administration (SRP, ISP, DIP).
 */
public class AuthenticationService implements AuthUseCase {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    /**
     * Constructs an {@code AuthenticationService} with segregated persistence, security, and time ports.
     *
     * @param userRepository       repository for user accounts
     * @param credentialRepository repository for password hashes
     * @param sessionRepository    repository for active session tokens
     * @param passwordHasher       cryptographic password hashing port
     * @param idGenerator          identifier generator for user IDs and session tokens
     * @param clock                clock for user registration timestamps
     */
    public AuthenticationService(UserRepository userRepository,
                                 CredentialRepository credentialRepository,
                                 SessionRepository sessionRepository,
                                 PasswordHasher passwordHasher,
                                 IdGenerator idGenerator,
                                 Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "credentialRepository cannot be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository cannot be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher cannot be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public synchronized User register(String fullName, String email, String phone, String password) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidInputException("Full name cannot be null or blank");
        }
        PasswordPolicySupport.validatePassword(password);

        ContactInfo contact;
        try {
            contact = new ContactInfo(email, phone);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(e.getMessage());
        }

        if (!contact.email().isEmpty() && userRepository.existsByEmail(contact.email())) {
            throw new DuplicateUserException("Email is already registered: " + contact.email());
        }
        if (!contact.phone().isEmpty() && userRepository.existsByPhone(contact.phone())) {
            throw new DuplicateUserException("Mobile number is already registered: " + contact.phone());
        }

        String userId = idGenerator.nextId("usr");
        String passwordHash = passwordHasher.hash(password);
        User user = new User(
                userId,
                fullName.trim(),
                contact.email(),
                contact.phone(),
                passwordHash,
                UserRole.CUSTOMER,
                LocalDateTime.now(clock)
        );

        userRepository.save(user);
        credentialRepository.savePasswordHash(user.getId(), passwordHash);
        return user;
    }

    @Override
    public String login(String identifier, String password) {
        if (identifier == null || password == null) {
            throw new IllegalArgumentException("Identifier and password cannot be null");
        }
        if (identifier.isBlank()) {
            throw new AuthenticationException("Invalid email or password");
        }

        User user = userRepository.findByIdentifier(identifier).orElse(null);
        String storedHash = user == null
                ? null
                : credentialRepository.findPasswordHashByUserId(user.getId()).orElse(null);

        if (user == null || storedHash == null || !passwordHasher.matches(password, storedHash)) {
            throw new AuthenticationException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("User account is inactive or suspended");
        }

        String sessionToken = idGenerator.nextId("tok");
        sessionRepository.saveSession(sessionToken, user.getId());
        return sessionToken;
    }

    @Override
    public void logout(String sessionToken) {
        sessionRepository.removeSession(sessionToken);
    }

    @Override
    public User getAuthenticatedUser(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return null;
        }
        String userId = sessionRepository.findUserIdByToken(sessionToken).orElse(null);
        if (userId == null) {
            return null;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            sessionRepository.removeSession(sessionToken);
            return null;
        }
        return user;
    }
}
