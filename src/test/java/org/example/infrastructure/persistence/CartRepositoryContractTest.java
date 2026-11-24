package org.example.infrastructure.persistence;

import org.example.model.entity.Cart;
import org.example.service.port.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract Design-by-Contract (LSP) test suite for {@link CartRepository} implementations.
 */
public abstract class CartRepositoryContractTest {

    protected abstract CartRepository createRepository();

    private CartRepository repository;

    @BeforeEach
    void initContract() {
        repository = createRepository();
    }

    @Test
    void getOrCreateAndFindByUserIdMaintainOwnership() {
        assertTrue(repository.findByUserId("usr-1").isEmpty());

        Cart created = repository.getOrCreate("usr-1");
        assertEquals("usr-1", created.getUserId());
        assertSame(created, repository.getOrCreate("usr-1"));
        assertSame(created, repository.findByUserId("usr-1").orElseThrow());

        Cart explicit = new Cart("cart-2", "usr-2");
        repository.save(explicit);
        assertSame(explicit, repository.findByUserId("usr-2").orElseThrow());
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> repository.findByUserId(""));
        assertThrows(IllegalArgumentException.class, () -> repository.getOrCreate(null));
        assertThrows(NullPointerException.class, () -> repository.save(null));
    }
}
