package org.example.infrastructure.security;

import java.util.UUID;
import org.example.service.port.IdGenerator;

/**
 * UUID-based implementation of {@link IdGenerator}.
 */
public class UuidIdGenerator implements IdGenerator {

    @Override
    public String nextId(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix cannot be null or blank");
        }
        return prefix.trim() + "-" + UUID.randomUUID();
    }
}
