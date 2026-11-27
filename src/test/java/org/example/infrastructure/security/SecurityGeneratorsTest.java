package org.example.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityGeneratorsTest {

    @Test
    void uuidIdGeneratorProducesUniquePrefixedIds() {
        UuidIdGenerator generator = new UuidIdGenerator();
        String id1 = generator.nextId("prod");
        String id2 = generator.nextId("prod");

        assertTrue(id1.startsWith("prod-"));
        assertTrue(id2.startsWith("prod-"));
        assertNotEquals(id1, id2);
        assertThrows(IllegalArgumentException.class, () -> generator.nextId(" "));
        assertThrows(IllegalArgumentException.class, () -> generator.nextId(null));
    }

    @Test
    void randomVerificationCodeGeneratorProducesSixDigits() {
        RandomVerificationCodeGenerator generator = new RandomVerificationCodeGenerator();
        for (int i = 0; i < 20; i++) {
            String code = generator.nextSixDigitCode();
            assertTrue(code.matches("[0-9]{6}"), "Expected 6 digits but got: " + code);
        }
    }
}
