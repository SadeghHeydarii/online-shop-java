package org.example.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.model.entity.User;
import org.example.service.port.CredentialRepository;
import org.example.service.port.IdGenerator;
import org.example.service.port.PasswordHasher;
import org.example.service.port.PasswordResetRepository;
import org.example.service.port.SessionRepository;
import org.example.service.port.UserRepository;
import org.example.service.port.VerificationCodeGenerator;
import org.example.util.PasswordUtil;

/**
 * Deprecated transitional facade delegating authentication and password recovery to
 * {@link AuthenticationService} and {@link PasswordRecoveryService}.
 *
 * @deprecated Scheduled for removal once menus and {@code Main} migrate to {@code AuthUseCase}
 *             and {@code PasswordRecoveryUseCase} in Phase 5.
 */
@Deprecated
public class AuthService {

    private final CredentialRepository credentialRepository;
    private final AuthenticationService authenticationService;
    private final PasswordRecoveryService passwordRecoveryService;

    /**
     * Constructs a new {@code AuthService} with independent concurrent in-memory storage maps.
     */
    public AuthService() {
        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code AuthService} sharing the specified user storage map.
     *
     * @param userStore the backing map for user entities
     */
    public AuthService(Map<String, User> userStore) {
        this(userStore, new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code AuthService} with injected user and active session storage maps.
     *
     * @param userStore      the backing map for user entities
     * @param activeSessions the backing map for session tokens to user IDs
     */
    public AuthService(Map<String, User> userStore, Map<String, String> activeSessions) {
        this(userStore, new ConcurrentHashMap<>(), activeSessions);
    }

    /**
     * Constructs an {@code AuthService} bridging legacy maps to segregated auth &amp; recovery services.
     *
     * @param userStore        the backing map for user entities
     * @param credentialsStore the backing map for user credentials hashes
     * @param activeSessions   the backing map for session tokens to user IDs
     */
    public AuthService(Map<String, User> userStore,
                       Map<String, String> credentialsStore,
                       Map<String, String> activeSessions) {
        Objects.requireNonNull(userStore, "userStore cannot be null");
        Objects.requireNonNull(credentialsStore, "credentialsStore cannot be null");
        Objects.requireNonNull(activeSessions, "activeSessions cannot be null");

        UserRepository userRepository = new MapBackedUserRepository(userStore);
        this.credentialRepository = new MapBackedCredentialRepository(credentialsStore);
        SessionRepository sessionRepository = new MapBackedSessionRepository(activeSessions);
        PasswordResetRepository resetRepository = new MapBackedResetRepository();
        PasswordHasher passwordHasher = new LegacyPasswordHasher();
        IdGenerator idGenerator = prefix -> "usr".equals(prefix)
                ? "usr-" + UUID.randomUUID().toString().substring(0, 8)
                : prefix + "-" + UUID.randomUUID();
        SecureRandom random = new SecureRandom();
        VerificationCodeGenerator codeGenerator = () -> String.format("%06d", random.nextInt(1_000_000));
        Clock clock = Clock.systemDefaultZone();

        this.authenticationService = new AuthenticationService(
                userRepository, credentialRepository, sessionRepository, passwordHasher, idGenerator, clock
        );
        this.passwordRecoveryService = new PasswordRecoveryService(
                userRepository, credentialRepository, sessionRepository, resetRepository,
                passwordHasher, codeGenerator, clock
        );
    }

    public User register(String fullName, String email, String phone, String password) {
        return authenticationService.register(fullName, email, phone, password);
    }

    public String login(String identifier, String password) {
        return authenticationService.login(identifier, password);
    }

    public void logout(String sessionToken) {
        authenticationService.logout(sessionToken);
    }

    public User getAuthenticatedUser(String sessionToken) {
        return authenticationService.getAuthenticatedUser(sessionToken);
    }

    public String requestPasswordReset(String identifier) {
        return passwordRecoveryService.requestPasswordReset(identifier);
    }

    public void resetPassword(String identifier, String code, String newPassword) {
        passwordRecoveryService.resetPassword(identifier, code, newPassword);
    }

    void recordCredentials(String userId, String passwordHash) {
        if (userId != null && !userId.isBlank() && passwordHash != null && !passwordHash.isBlank()) {
            credentialRepository.savePasswordHash(userId, passwordHash);
        }
    }

    // =========================================================================
    // Legacy transition adapters (removed along with AuthService in Phase 5)
    // =========================================================================

    private static final class LegacyPasswordHasher implements PasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return PasswordUtil.hashPassword(rawPassword);
        }

        @Override
        public boolean matches(String rawPassword, String storedHash) {
            return PasswordUtil.verifyPassword(rawPassword, storedHash);
        }
    }

    private record MapBackedUserRepository(Map<String, User> store) implements UserRepository {
        @Override
        public Optional<User> findById(String userId) {
            return Optional.ofNullable(store.get(userId.trim()));
        }

        @Override
        public Optional<User> findByIdentifier(String identifier) {
            return store.values().stream()
                    .filter(user -> user.matchesContact(identifier))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            if (email == null || email.isBlank()) {
                return false;
            }
            String norm = email.trim();
            return store.values().stream()
                    .anyMatch(u -> !u.getEmail().isEmpty() && u.getEmail().equalsIgnoreCase(norm));
        }

        @Override
        public boolean existsByPhone(String phone) {
            if (phone == null || phone.isBlank()) {
                return false;
            }
            String norm = phone.trim();
            return store.values().stream()
                    .anyMatch(u -> !u.getPhone().isEmpty() && u.getPhone().equals(norm));
        }

        @Override
        public boolean existsByPhoneExcludingUser(String phone, String excludedUserId) {
            if (phone == null || phone.isBlank()) {
                return false;
            }
            String norm = phone.trim();
            return store.values().stream()
                    .anyMatch(u -> !u.getId().equals(excludedUserId) && !u.getPhone().isEmpty() && u.getPhone().equals(norm));
        }

        @Override
        public void save(User user) {
            store.put(user.getId(), user);
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(store.values());
        }
    }

    private record MapBackedCredentialRepository(Map<String, String> store) implements CredentialRepository {
        @Override
        public void savePasswordHash(String userId, String passwordHash) {
            store.put(userId.trim(), passwordHash);
        }

        @Override
        public Optional<String> findPasswordHashByUserId(String userId) {
            return Optional.ofNullable(store.get(userId.trim()));
        }
    }

    private record MapBackedSessionRepository(Map<String, String> store) implements SessionRepository {
        @Override
        public void saveSession(String sessionToken, String userId) {
            store.put(sessionToken.trim(), userId.trim());
        }

        @Override
        public Optional<String> findUserIdByToken(String sessionToken) {
            if (sessionToken == null || sessionToken.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(store.get(sessionToken.trim()));
        }

        @Override
        public void removeSession(String sessionToken) {
            if (sessionToken != null) {
                store.remove(sessionToken);
            }
        }

        @Override
        public void removeAllSessionsForUser(String userId) {
            store.entrySet().removeIf(entry -> entry.getValue().equals(userId));
        }
    }

    private static final class MapBackedResetRepository implements PasswordResetRepository {
        private final Map<String, ResetToken> store = new ConcurrentHashMap<>();

        @Override
        public void saveResetToken(String userId, ResetToken token) {
            store.put(userId, token);
        }

        @Override
        public Optional<ResetToken> findResetToken(String userId) {
            return Optional.ofNullable(store.get(userId));
        }

        @Override
        public void removeResetToken(String userId) {
            store.remove(userId);
        }
    }
}
