package org.example.service.port;

/**
 * Outbound port for generating short numeric verification codes (e.g., password recovery).
 */
public interface VerificationCodeGenerator {

    /**
     * Generates a 6-digit numeric verification code.
     *
     * @return 6-character string consisting only of digits {@code 0-9}
     */
    String nextSixDigitCode();
}
