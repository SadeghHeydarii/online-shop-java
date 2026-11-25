package org.example.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.example.exception.AuthenticationException;
import org.example.model.entity.User;
import org.example.service.api.PasswordRecoveryUseCase;
import org.example.service.port.CredentialRepository;
import org.example.service.port.PasswordHasher;
import org.example.service.port.PasswordResetRepository;
import org.example.service.port.PasswordResetRepository.ResetToken;
import org.example.service.port.SessionRepository;
import org.example.service.port.UserRepository;
import org.example.service.port.VerificationCodeGenerator;

/**
 * Cohesive password-recovery service managing timed verification code issuance,
 * password resets, and active session revocation (SRP, OCP, DIP).
 */
public class PasswordRecoveryService implements PasswordRecoveryUseCase {

    public static final int RESET_TTL_MINUTES = 10;

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final SessionRepository sessionRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordHasher passwordHasher;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final Clock clock;

    /**
     * Constructs a {@code PasswordRecoveryService} with segregated persistence, security, and time ports.
     *
     * @param userRepository            repository for resolving user accounts
     * @param credentialRepository      repository for updating password hashes
     * @param sessionRepository         repository for revoking active sessions after reset
     * @param passwordResetRepository   repository for expiring recovery tokens
     * @param passwordHasher            cryptographic hashing port
     * @param verificationCodeGenerator 6-digit recovery code generator
     * @param clock                     clock controlling token expiration timestamps
     */
    public PasswordRecoveryService(UserRepository userRepository,
                                   CredentialRepository credentialRepository,
                                   SessionRepository sessionRepository,
                                   PasswordResetRepository passwordResetRepository,
                                   PasswordHasher passwordHasher,
                                   VerificationCodeGenerator verificationCodeGenerator,
                                   Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "credentialRepository cannot be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository cannot be null");
        this.passwordResetRepository = Objects.requireNonNull(
                passwordResetRepository, "passwordResetRepository cannot be null"
        );
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher cannot be null");
        this.verificationCodeGenerator = Objects.requireNonNull(
                verificationCodeGenerator, "verificationCodeGenerator cannot be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public String requestPasswordReset(String identifier) {
        User user = findActiveUser(identifier);
        if (user == null) {
            throw new AuthenticationException("No active account matches that email or mobile number");
        }
        String code = verificationCodeGenerator.nextSixDigitCode();
        ResetToken token = new ResetToken(
                passwordHasher.hash(code),
                LocalDateTime.now(clock).plusMinutes(RESET_TTL_MINUTES)
        );
        passwordResetRepository.saveResetToken(user.getId(), token);
        return code;
    }

    @Override
    public synchronized void resetPassword(String identifier, String code, String newPassword) {
        PasswordPolicySupport.validatePassword(newPassword);

        User user = findActiveUser(identifier);
        ResetToken reset = user == null
                ? null
                : passwordResetRepository.findResetToken(user.getId()).orElse(null);

        if (user == null
                || reset == null
                || !LocalDateTime.now(clock).isBefore(reset.expiresAt())
                || !passwordHasher.matches(code, reset.tokenHash())) {
            throw new AuthenticationException("Invalid or expired recovery code");
        }

        String hash = passwordHasher.hash(newPassword);
        user.changePassword(hash);
        userRepository.save(user);
        credentialRepository.savePasswordHash(user.getId(), hash);
        passwordResetRepository.removeResetToken(user.getId());
        sessionRepository.removeAllSessionsForUser(user.getId());
    }

    private User findActiveUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        return userRepository.findByIdentifier(identifier)
                .filter(User::isActive)
                .orElse(null);
    }
}
