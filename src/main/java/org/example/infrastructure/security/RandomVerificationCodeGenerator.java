package org.example.infrastructure.security;

import java.security.SecureRandom;
import org.example.service.port.VerificationCodeGenerator;

/**
 * {@link SecureRandom}-backed implementation of {@link VerificationCodeGenerator}.
 */
public class RandomVerificationCodeGenerator implements VerificationCodeGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String nextSixDigitCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
