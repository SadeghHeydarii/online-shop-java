package org.example.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.OrderNotFoundException;
import org.example.infrastructure.persistence.InMemoryCheckoutSnapshotRepository;
import org.example.infrastructure.persistence.InMemoryOrderRepository;
import org.example.model.entity.Order;
import org.example.model.enums.OrderStatus;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.service.api.ProductInventoryPort;
import org.example.service.port.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderFulfillmentServiceTest {

    private OrderRepository orderRepository;
    private Map<String, Integer> restockedUnits;
    private OrderFulfillmentService fulfillmentService;
    private Order paidOrder;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        restockedUnits = new HashMap<>();

        Address address = new Address("addr-1", "Ali", "09121112233", "Tehran", "Tehran",
                "Hafez St", "1234567890", "Home", true);
        Money price = new Money(new BigDecimal("100000.00"), "IRR");
        Money zero = new Money(BigDecimal.ZERO, "IRR");
        OrderItem item = new OrderItem("prod-1", "Cable", price, new Quantity(3));

        paidOrder = new Order("ord-paid", "usr-1", List.of(item), address,
                new Money(new BigDecimal("300000.00"), "IRR"), zero, zero,
                new Money(new BigDecimal("300000.00"), "IRR"), LocalDateTime.of(2026, 11, 24, 10, 0));
        paidOrder.markAsPaid(LocalDateTime.of(2026, 11, 24, 10, 5));

        pendingOrder = new Order("ord-pending", "usr-2", List.of(item), address,
                new Money(new BigDecimal("300000.00"), "IRR"), zero, zero,
                new Money(new BigDecimal("300000.00"), "IRR"), LocalDateTime.of(2026, 11, 24, 11, 0));

        orderRepository.save(paidOrder);
        orderRepository.save(pendingOrder);

        ProductInventoryPort inventoryPort = new ProductInventoryPort() {
            @Override
            public void adjustStock(String productId, int delta) {
                restockedUnits.merge(productId, delta, Integer::sum);
            }

            @Override
            public void recordSale(String productId, Quantity quantity) {
            }
        };

        fulfillmentService = new OrderFulfillmentService(
                orderRepository,
                new InMemoryCheckoutSnapshotRepository(),
                inventoryPort
        );
    }

    @Test
    void advancesOrderThroughLifecycleAndFiltersByStatus() {
        fulfillmentService.updateOrderStatus("ord-paid", OrderStatus.PROCESSING);
        fulfillmentService.updateOrderStatus("ord-paid", OrderStatus.SHIPPED);
        fulfillmentService.updateOrderStatus("ord-paid", OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, fulfillmentService.getOrderDetails("ord-paid").getStatus());
        assertEquals(1, fulfillmentService.getOrdersByStatus(OrderStatus.DELIVERED).size());
        assertEquals(2, fulfillmentService.getAllOrders().size());
        assertEquals("ord-pending", fulfillmentService.getAllOrders().getFirst().getOrderId());
    }

    @Test
    void cancellingPaidOrderViaStatusUpdateRestocksInventory() {
        fulfillmentService.updateOrderStatus("ord-paid", OrderStatus.CANCELLED);
        assertEquals(OrderStatus.CANCELLED, paidOrder.getStatus());
        assertEquals(3, restockedUnits.get("prod-1"));

        fulfillmentService.cancelOrder("ord-pending");
        assertEquals(3, restockedUnits.get("prod-1"));
    }

    @Test
    void rejectsInvalidTransitionsAndMissingOrders() {
        assertThrows(InvalidOrderStateException.class,
                () -> fulfillmentService.updateOrderStatus("ord-pending", OrderStatus.SHIPPED));
        assertThrows(OrderNotFoundException.class,
                () -> fulfillmentService.getOrderDetails("missing"));
    }
}
