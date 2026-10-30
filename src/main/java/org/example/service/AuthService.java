package org.example.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.AuthenticationException;
import org.example.exception.DuplicateUserException;
import org.example.exception.InvalidInputException;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.util.PasswordUtil;

/**
 * Service managing user authentication, registration, session lifecycles, and credential verification.
 *
 * <p>Encapsulates in-memory storage maps for registered users and active session tokens,
 * coordinating domain scenarios DS 01 (Registration), DS 02 (Authentication & Login),
 * and DS 03 (Logout & Session Termination).
 */
public class AuthService {

    private final Map<String, User> userStore;
    private final Map<String, String> activeSessions;
    private final Map<String, String> credentialsStore;

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
     * Constructs an {@code AuthService} with injected user, credentials, and active session storage maps.
     *
     * @param userStore        the backing map for user entities
     * @param credentialsStore the backing map for user credentials hashes
     * @param activeSessions   the backing map for session tokens to user IDs
     */
    public AuthService(Map<String, User> userStore,
                       Map<String, String> credentialsStore,
                       Map<String, String> activeSessions) {
        this.userStore = Objects.requireNonNull(userStore, "userStore cannot be null");
        this.credentialsStore = Objects.requireNonNull(credentialsStore, "credentialsStore cannot be null");
        this.activeSessions = Objects.requireNonNull(activeSessions, "activeSessions cannot be null");
    }

    /**
     * Registers a new customer account in the system.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> {@code fullName}, {@code email}, and {@code phone} non-blank;
     *       valid email format containing '@'; {@code password} length &gt;= 6;
     *       email not already registered.</li>
     *   <li><b>MODIFIES:</b> {@code userStore}, {@code credentialsStore}</li>
     *   <li><b>EFFECTS:</b> Hashes password, constructs new {@link User} in active status,
     *       stores it in {@code userStore}, and returns the created user entity.</li>
     * </ul>
     *
     * @param fullName the customer's full name
     * @param email    the customer's email address
     * @param phone    the contact telephone number
     * @param password the plain-text password (minimum 6 characters)
     * @return the newly registered {@link User} entity
     * @throws InvalidInputException  if any parameter is blank or password is too short
     * @throws DuplicateUserException if a user with the same email already exists
     */
    public User register(String fullName, String email, String phone, String password) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidInputException("Full name cannot be null or blank");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new InvalidInputException("A valid email address containing '@' is required");
        }
        if (phone == null || phone.isBlank()) {
            throw new InvalidInputException("Phone number cannot be null or blank");
        }
        if (password == null || password.length() < 6) {
            throw new InvalidInputException("Password must be at least 6 characters long");
        }

        String normalizedEmail = email.trim().toLowerCase();
        for (User existing : userStore.values()) {
            if (existing.getEmail().equalsIgnoreCase(normalizedEmail)) {
                throw new DuplicateUserException("Email is already registered: " + email);
            }
        }

        String userId = "usr-" + UUID.randomUUID().toString().substring(0, 8);
        String passwordHash = PasswordUtil.hashPassword(password);

        User user = new User(userId, fullName.trim(), normalizedEmail, phone.trim(),
                passwordHash, UserRole.CUSTOMER, LocalDateTime.now());

        userStore.put(user.getId(), user);
        credentialsStore.put(user.getId(), passwordHash);
        return user;
    }

    /**
     * Authenticates a user by email and password, establishing an active session upon success.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> {@code email != null}, {@code password != null}</li>
     *   <li><b>MODIFIES:</b> {@code activeSessions}</li>
     *   <li><b>EFFECTS:</b> Verifies user credentials; if valid and account is active, generates
     *       a unique session token, stores the mapping in {@code activeSessions}, and returns the token.
     *       Throws {@link AuthenticationException} if credentials do not match or user is inactive.</li>
     * </ul>
     *
     * @param email    the user's registered email address
     * @param password the plain-text candidate password
     * @return the generated session token string
     * @throws IllegalArgumentException if email or password is null
     * @throws AuthenticationException  if credentials fail verification or the account is inactive
     */
    public String login(String email, String password) {
        if (email == null || password == null) {
            throw new IllegalArgumentException("Email and password cannot be null");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User targetUser = null;
        for (User u : userStore.values()) {
            if (u.getEmail().equalsIgnoreCase(normalizedEmail)) {
                targetUser = u;
                break;
            }
        }

        if (targetUser == null) {
            throw new AuthenticationException("Invalid email or password");
        }

        String storedHash = credentialsStore.get(targetUser.getId());
        if (storedHash == null || !PasswordUtil.verifyPassword(password, storedHash)) {
            throw new AuthenticationException("Invalid email or password");
        }

        if (!targetUser.isActive()) {
            throw new AuthenticationException("User account is inactive or suspended");
        }

        String sessionToken = "tok-" + UUID.randomUUID().toString();
        activeSessions.put(sessionToken, targetUser.getId());
        return sessionToken;
    }

    /**
     * Terminates an active user session.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> {@code sessionToken != null}</li>
     *   <li><b>MODIFIES:</b> {@code activeSessions}</li>
     *   <li><b>EFFECTS:</b> Removes {@code sessionToken} from {@code activeSessions}.</li>
     * </ul>
     *
     * @param sessionToken the session token to terminate
     */
    public void logout(String sessionToken) {
        if (sessionToken != null) {
            activeSessions.remove(sessionToken);
        }
    }

    /**
     * Resolves the {@link User} associated with an active session token.
     *
     * @param sessionToken the active session token
     * @return the authenticated {@link User} entity, or {@code null} if the token is invalid or expired
     */
    public User getAuthenticatedUser(String sessionToken) {
        if (sessionToken == null || !activeSessions.containsKey(sessionToken)) {
            return null;
        }
        String userId = activeSessions.get(sessionToken);
        return userStore.get(userId);
    }

    /**
     * Internal helper to record pre-seeded credentials for testing or data initialization.
     *
     * @param userId       the identifier of the user
     * @param passwordHash the cryptographic password hash
     */
    void recordCredentials(String userId, String passwordHash) {
        if (userId != null && passwordHash != null) {
            credentialsStore.put(userId, passwordHash);
        }
    }
}
