package org.example.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.service.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract Design-by-Contract (LSP) test suite for {@link UserRepository} implementations.
 */
public abstract class UserRepositoryContractTest {

    protected abstract UserRepository createRepository();

    private UserRepository repository;
    private User alice;
    private User bob;

    @BeforeEach
    void initContract() {
        repository = createRepository();
        alice = new User("usr-1", "Alice", "alice@example.com", "09121112233", "hash1", UserRole.CUSTOMER, LocalDateTime.now());
        bob = new User("usr-2", "Bob", "bob@example.com", "09124445566", "hash2", UserRole.CUSTOMER, LocalDateTime.now());
    }

    @Test
    void saveAndFindByIdAndIdentifierWorkConsistently() {
        repository.save(alice);
        repository.save(bob);

        assertEquals(alice, repository.findById("usr-1").orElseThrow());
        assertEquals(alice, repository.findByIdentifier("ALICE@EXAMPLE.COM").orElseThrow());
        assertEquals(bob, repository.findByIdentifier("09124445566").orElseThrow());
        assertTrue(repository.findById("missing").isEmpty());
        assertTrue(repository.findByIdentifier("missing@example.com").isEmpty());
    }

    @Test
    void uniquenessChecksHonorExclusionContract() {
        repository.save(alice);
        repository.save(bob);

        assertTrue(repository.existsByEmail("ALICE@example.com"));
        assertFalse(repository.existsByEmail("none@example.com"));
        assertTrue(repository.existsByPhone("09121112233"));
        assertFalse(repository.existsByPhone("09120000000"));

        assertFalse(repository.existsByPhoneExcludingUser("09121112233", "usr-1"));
        assertTrue(repository.existsByPhoneExcludingUser("09121112233", "usr-2"));
    }

    @Test
    void findAllReturnsUnmodifiableSnapshotAndRejectsInvalidPreconditions() {
        repository.save(alice);
        List<User> all = repository.findAll();
        assertEquals(1, all.size());
        assertThrows(UnsupportedOperationException.class, () -> all.add(bob));
        assertThrows(IllegalArgumentException.class, () -> repository.findById(" "));
        assertThrows(IllegalArgumentException.class, () -> repository.findByIdentifier(null));
        assertThrows(NullPointerException.class, () -> repository.save(null));
    }
}
