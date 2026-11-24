package org.example.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.model.entity.Order;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.service.port.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract Design-by-Contract (LSP) test suite for {@link OrderRepository} implementations.
 */
public abstract class OrderRepositoryContractTest {

    protected abstract OrderRepository createRepository();

    private OrderRepository repository;
    private Order order1;
    private Order order2;

    @BeforeEach
    void initContract() {
        repository = createRepository();
        Address address = new Address("addr-1", "Ali", "09121112233", "Tehran", "Tehran",
                "Hafez St", "1234567890", "Home", true);
        Money unitPrice = new Money(new BigDecimal("100000.00"), "IRR");
        OrderItem item = new OrderItem("prod-1", "Item", unitPrice, new Quantity(1));
        Money zero = new Money(BigDecimal.ZERO, "IRR");

        order1 = new Order("ord-1", "usr-1", List.of(item), address, unitPrice, zero, zero, unitPrice, LocalDateTime.now());
        order2 = new Order("ord-2", "usr-2", List.of(item), address, unitPrice, zero, zero, unitPrice, LocalDateTime.now());
    }

    @Test
    void saveAndQueriesHonorIsolationAndUnmodifiability() {
        repository.save(order1);
        repository.save(order2);

        assertEquals(order1, repository.findById("ord-1").orElseThrow());
        assertTrue(repository.findById("missing").isEmpty());
        assertEquals(List.of(order1), repository.findByUserId("usr-1"));
        assertEquals(2, repository.findAll().size());
        assertThrows(UnsupportedOperationException.class, () -> repository.findAll().add(order1));
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> repository.findById(" "));
        assertThrows(IllegalArgumentException.class, () -> repository.findByUserId(null));
        assertThrows(NullPointerException.class, () -> repository.save(null));
    }
}
