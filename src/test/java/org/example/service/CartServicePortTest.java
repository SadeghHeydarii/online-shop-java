package org.example.service;

import java.math.BigDecimal;
import java.util.List;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidInputException;
import org.example.infrastructure.persistence.InMemoryCartRepository;
import org.example.model.entity.Cart;
import org.example.model.entity.Product;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;
import org.example.service.api.ProductAvailabilityPort;
import org.example.service.port.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartServicePortTest {

    private CartRepository cartRepository;
    private Product laptop;
    private Product mouse;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartRepository = new InMemoryCartRepository();
        laptop = new Product("p-laptop", "Laptop", "Workstation",
                new Money(new BigDecimal("1000000.00"), "IRR"), 5, "CAT-1");
        mouse = new Product("p-mouse", "Mouse", "Wireless",
                new Money(new BigDecimal("100000.00"), "IRR"), 10, "CAT-1");

        ProductAvailabilityPort availabilityPort = (productId, quantity) -> {
            Product target = "p-laptop".equals(productId) ? laptop : mouse;
            if (!target.isAvailable(quantity)) {
                throw new InsufficientStockException("Not enough stock for " + productId);
            }
            return target;
        };

        cartService = new CartService(cartRepository, availabilityPort);
    }

    @Test
    void addsUpdatesRemovesAndSelectivelyClearsSnapshotViaPorts() {
        cartService.addItem("usr-1", "p-laptop", new Quantity(2));
        cartService.addItem("usr-1", "p-mouse", new Quantity(1));

        Cart persisted = cartRepository.findByUserId("usr-1").orElseThrow();
        assertEquals(2, persisted.quantityOf("p-laptop"));

        cartService.updateItemQuantity("usr-1", "p-mouse", new Quantity(3));
        assertEquals(3, persisted.quantityOf("p-mouse"));

        cartService.removeCheckedOutItems("usr-1",
                persisted.getItems().stream().filter(i -> i.productId().equals("p-laptop")).toList());
        assertEquals(0, persisted.quantityOf("p-laptop"));
        assertEquals(3, persisted.quantityOf("p-mouse"));

        cartService.clearCart("usr-1");
        assertTrue(persisted.isEmpty());
    }

    @Test
    void rejectsExcessStockAndMissingCartItems() {
        assertThrows(InsufficientStockException.class,
                () -> cartService.addItem("usr-1", "p-laptop", new Quantity(6)));
        assertThrows(InvalidInputException.class,
                () -> cartService.removeItem("usr-1", "p-laptop"));
    }
}
