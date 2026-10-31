package org.example.service;

import java.security.SecureRandom;
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
import org.example.model.vo.ContactInfo;
import org.example.util.PasswordUtil;

/**
 * Service managing user authentication, registration, session lifecycles, and credential verification.
 *
 * <p>Encapsulates in-memory storage maps for registered users, active session tokens, credential hashes,
 * and expiring password reset tokens. Coordinates domain scenarios DS 01 (Registration), DS 02 (Authentication & Login),
 * DS 03 (Logout & Session Termination), and console password recovery.
 */
public class AuthService {

    private final Map<String, User> userStore;
    private final Map<String, String> activeSessions;
    private final Map<String, String> credentialsStore;
    private final Map<String, PasswordReset> passwordResets = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private record PasswordReset(String tokenHash, LocalDateTime expiresAt) {
    }

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
     *   <li><b>REQUIRES:</b> {@code fullName} non-blank; valid {@link ContactInfo} (at least one of email or phone);
     *       {@code password} length &gt;= 6; email and phone not already registered by another account.</li>
     *   <li><b>MODIFIES:</b> {@code userStore}, {@code credentialsStore}</li>
     *   <li><b>EFFECTS:</b> Hashes password, constructs new {@link User} in active status,
     *       stores it in {@code userStore}, and returns the created user entity.</li>
     * </ul>
     *
     * @param fullName the customer's full name
     * @param email    the customer's email address (optional if phone provided)
     * @param phone    the contact telephone number (optional if email provided)
     * @param password the plain-text password (minimum 6 characters)
     * @return the newly registered {@link User} entity
     * @throws InvalidInputException  if any parameter is invalid or password is too short
     * @throws DuplicateUserException if a user with the same email or phone already exists
     */
    public User register(String fullName, String email, String phone, String password) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidInputException("Full name cannot be null or blank");
        }
        validatePassword(password);
        ContactInfo contact;
        try {
            contact = new ContactInfo(email, phone);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(e.getMessage());
        }

        for (User existing : userStore.values()) {
            if (!contact.email().isEmpty() && existing.getEmail().equalsIgnoreCase(contact.email())) {
                throw new DuplicateUserException("Email is already registered: " + contact.email());
            }
            if (!contact.phone().isEmpty() && existing.getPhone().equals(contact.phone())) {
                throw new DuplicateUserException("Mobile number is already registered: " + contact.phone());
            }
        }

        String userId = "usr-" + UUID.randomUUID().toString().substring(0, 8);
        String passwordHash = PasswordUtil.hashPassword(password);

        User user = new User(userId, fullName.trim(), contact.email(), contact.phone(),
                passwordHash, UserRole.CUSTOMER, LocalDateTime.now());

        userStore.put(user.getId(), user);
        credentialsStore.put(user.getId(), passwordHash);
        return user;
    }

    /**
     * Authenticates a user by email address or mobile phone and password, establishing an active session upon success.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> {@code identifier != null}, {@code password != null}</li>
     *   <li><b>MODIFIES:</b> {@code activeSessions}</li>
     *   <li><b>EFFECTS:</b> Resolves user by matching email or mobile number; verifies credentials;
     *       if valid and account is active, generates a unique session token, stores the mapping in
     *       {@code activeSessions}, and returns the token. Throws {@link AuthenticationException}
     *       if verification fails or account is inactive.</li>
     * </ul>
     *
     * @param identifier the registered email address or mobile telephone number
     * @param password   the plain-text candidate password
     * @return the generated session token string
     * @throws IllegalArgumentException if identifier or password is null
     * @throws AuthenticationException  if credentials fail verification or the account is inactive
     */
    public String login(String identifier, String password) {
        if (identifier == null || password == null) {
            throw new IllegalArgumentException("Identifier and password cannot be null");
        }

        User user = findByIdentifier(identifier);
        if (user == null || !PasswordUtil.verifyPassword(password, credentialsStore.get(user.getId()))) {
            throw new AuthenticationException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("User account is inactive or suspended");
        }

        String sessionToken = "tok-" + UUID.randomUUID();
        activeSessions.put(sessionToken, user.getId());
        return sessionToken;
    }

    /**
     * Terminates an active user session.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> none (handles null gracefully)</li>
     *   <li><b>MODIFIES:</b> {@code activeSessions}</li>
     *   <li><b>EFFECTS:</b> Removes {@code sessionToken} from {@code activeSessions} if present.</li>
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
     * Resolves the {@link User} associated with an active session token, invalidating inactive sessions.
     *
     * @param sessionToken the active session token
     * @return the authenticated {@link User} entity, or {@code null} if the token is invalid or expired
     */
    public User getAuthenticatedUser(String sessionToken) {
        if (sessionToken == null) {
            return null;
        }
        String userId = activeSessions.get(sessionToken);
        User user = userId == null ? null : userStore.get(userId);
        if (user == null || !user.isActive()) {
            activeSessions.remove(sessionToken);
            return null;
        }
        return user;
    }

    /**
     * Initiates a password reset process by generating and storing a 6-digit recovery code valid for 10 minutes.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> {@code identifier != null && !identifier.isBlank()}</li>
     *   <li><b>MODIFIES:</b> {@code passwordResets}</li>
     *   <li><b>EFFECTS:</b> Generates a 6-digit random code, securely hashes it, records it in {@code passwordResets}
     *       with 10-minute expiration, and returns the raw code for console simulation.</li>
     * </ul>
     *
     * @param identifier the registered email address or mobile phone number
     * @return the generated 6-digit recovery code
     * @throws AuthenticationException if no active user matches the identifier
     */
    public String requestPasswordReset(String identifier) {
        User user = findByIdentifier(identifier);
        if (user == null || !user.isActive()) {
            throw new AuthenticationException("No active account matches that email or mobile number");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        passwordResets.put(user.getId(), new PasswordReset(
                PasswordUtil.hashPassword(code), LocalDateTime.now().plusMinutes(10)));
        return code;
    }

    /**
     * Resets a user's password given a valid, unexpired recovery code.
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li><b>REQUIRES:</b> {@code identifier}, {@code code}, and {@code newPassword} non-blank; {@code newPassword} length &gt;= 6</li>
     *   <li><b>MODIFIES:</b> {@code userStore}, {@code credentialsStore}, {@code passwordResets}, {@code activeSessions}</li>
     *   <li><b>EFFECTS:</b> Verifies the reset code; updates user password hash; terminates all active sessions for the user.</li>
     * </ul>
     *
     * @param identifier  the registered email address or mobile phone number
     * @param code        the 6-digit recovery code
     * @param newPassword the new candidate password (minimum 6 characters)
     * @throws InvalidInputException   if the new password fails validation
     * @throws AuthenticationException if the recovery code is invalid, expired, or user not found
     */
    public void resetPassword(String identifier, String code, String newPassword) {
        validatePassword(newPassword);
        synchronized (userStore) {
            User user = findByIdentifier(identifier);
            PasswordReset reset = user == null ? null : passwordResets.get(user.getId());
            if (user == null || !user.isActive() || reset == null
                    || !LocalDateTime.now().isBefore(reset.expiresAt())
                    || !PasswordUtil.verifyPassword(code, reset.tokenHash())) {
                throw new AuthenticationException("Invalid or expired recovery code");
            }
            String hash = PasswordUtil.hashPassword(newPassword);
            user.changePassword(hash);
            credentialsStore.put(user.getId(), hash);
            passwordResets.remove(user.getId());
            activeSessions.entrySet().removeIf(entry -> entry.getValue().equals(user.getId()));
        }
    }

    private User findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        return userStore.values().stream()
                .filter(user -> new ContactInfo(user.getEmail(), user.getPhone()).matches(identifier))
                .findFirst().orElse(null);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 6) {
            throw new InvalidInputException("Password must contain at least 6 characters");
        }
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
