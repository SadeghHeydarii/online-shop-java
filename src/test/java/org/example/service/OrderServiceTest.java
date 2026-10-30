package org.example.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.exception.InvalidInputException;
import org.example.exception.OrderNotFoundException;
import org.example.model.entity.Order;
import org.example.model.enums.OrderStatus;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderService Tests")
class OrderServiceTest {

    private OrderService orderService;
    private Address sampleAddress;

    @BeforeEach
    void setUp() {
        orderService = new OrderService();
        sampleAddress = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St", "1234567890", "Home", true);
    }

    private Order createTestOrder(String orderId, String userId, OrderStatus targetStatus) {
        Money price = new Money(new BigDecimal("100000.00"), "IRR");
        OrderItem item = new OrderItem("prod-1", "Sample Product", price, new Quantity(1), price);
        Money zero = Money.ZERO;
        Order order = new Order(
                orderId,
                userId,
                List.of(item),
                sampleAddress,
                price,
                zero,
                zero,
                price,
                LocalDateTime.now()
        );

        if (targetStatus == OrderStatus.PAID) {
            order.markAsPaid(LocalDateTime.now());
        } else if (targetStatus == OrderStatus.PROCESSING) {
            order.markAsPaid(LocalDateTime.now());
            order.updateStatus(OrderStatus.PROCESSING);
        } else if (targetStatus == OrderStatus.SHIPPED) {
            order.markAsPaid(LocalDateTime.now());
            order.updateStatus(OrderStatus.PROCESSING);
            order.updateStatus(OrderStatus.SHIPPED);
        } else if (targetStatus == OrderStatus.DELIVERED) {
            order.markAsPaid(LocalDateTime.now());
            order.updateStatus(OrderStatus.PROCESSING);
            order.updateStatus(OrderStatus.SHIPPED);
            order.updateStatus(OrderStatus.DELIVERED);
        } else if (targetStatus == OrderStatus.PAYMENT_FAILED) {
            order.markAsPaymentFailed();
        } else if (targetStatus == OrderStatus.CANCELLED) {
            order.cancel();
        }

        return order;
    }

    @Test
    @DisplayName("getOrderHistory returns only orders placed by the user")
    void getOrderHistoryFiltering() {
        Order o1 = createTestOrder("ord-1", "user-1", OrderStatus.PAID);
        Order o2 = createTestOrder("ord-2", "user-1", OrderStatus.DELIVERED);
        Order o3 = createTestOrder("ord-3", "user-2", OrderStatus.PAID);

        orderService.recordOrder(o1);
        orderService.recordOrder(o2);
        orderService.recordOrder(o3);

        List<Order> user1Orders = orderService.getOrderHistory("user-1");
        assertEquals(2, user1Orders.size());
        assertTrue(user1Orders.stream().anyMatch(o -> o.getOrderId().equals("ord-1")));
        assertTrue(user1Orders.stream().anyMatch(o -> o.getOrderId().equals("ord-2")));

        List<Order> user2Orders = orderService.getOrderHistory("user-2");
        assertEquals(1, user2Orders.size());
        assertEquals("ord-3", user2Orders.getFirst().getOrderId());

        List<Order> user3Orders = orderService.getOrderHistory("user-3");
        assertTrue(user3Orders.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Blank or null userId in getOrderHistory throws InvalidInputException")
    void invalidUserIdThrows(String userId) {
        assertThrows(InvalidInputException.class, () -> orderService.getOrderHistory(userId));
    }

    @Test
    @DisplayName("getOrderDetails returns matching order")
    void getOrderDetailsSuccess() {
        Order order = createTestOrder("ord-99", "user-5", OrderStatus.PAID);
        orderService.recordOrder(order);

        Order fetched = orderService.getOrderDetails("ord-99");
        assertEquals("ord-99", fetched.getOrderId());
        assertEquals("user-5", fetched.getUserId());
    }

    @Test
    @DisplayName("getOrderDetails for non-existent order throws OrderNotFoundException")
    void nonExistentOrderThrows() {
        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderDetails("missing-order-id"));
    }

    @Test
    @DisplayName("updateOrderStatus successfully updates status")
    void updateOrderStatusSuccess() {
        Order order = createTestOrder("ord-10", "user-1", OrderStatus.PAID);
        orderService.recordOrder(order);

        orderService.updateOrderStatus("ord-10", OrderStatus.PROCESSING);
        assertEquals(OrderStatus.PROCESSING, orderService.getOrderDetails("ord-10").getStatus());
    }

    @Test
    @DisplayName("updateOrderStatus with null status throws InvalidInputException")
    void nullStatusThrows() {
        Order order = createTestOrder("ord-11", "user-1", OrderStatus.PAID);
        orderService.recordOrder(order);

        assertThrows(InvalidInputException.class, () -> orderService.updateOrderStatus("ord-11", null));
    }
}
