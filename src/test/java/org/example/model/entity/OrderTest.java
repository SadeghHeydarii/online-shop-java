package org.example.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.enums.PaymentStatus;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.model.vo.PaymentTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    private Address sampleAddress;
    private OrderItem item1;
    private OrderItem item2;
    private List<OrderItem> sampleItems;
    private Money subtotal;
    private Money discount;
    private Money shipping;
    private Money payable;
    private LocalDateTime orderTime;
    private Order order;

    @BeforeEach
    void setUp() {
        sampleAddress = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St", "1234567890", "Home", true);

        // item1: 50,000 * 2 = 100,000
        item1 = new OrderItem("prod-1", "Mouse",
                new Money(new BigDecimal("50000.00"), "IRR"), new Quantity(2));
        // item2: 150,000 * 1 = 150,000
        item2 = new OrderItem("prod-2", "Keyboard",
                new Money(new BigDecimal("150000.00"), "IRR"), new Quantity(1));

        sampleItems = new ArrayList<>(List.of(item1, item2));

        subtotal = new Money(new BigDecimal("250000.00"), "IRR");
        discount = new Money(new BigDecimal("25000.00"), "IRR");
        shipping = new Money(new BigDecimal("15000.00"), "IRR");
        // payable: 250,000 - 25,000 + 15,000 = 240,000
        payable = new Money(new BigDecimal("240000.00"), "IRR");

        orderTime = LocalDateTime.of(2026, 10, 30, 14, 0);

        order = new Order("ord-100", "usr-1", sampleItems, sampleAddress,
                subtotal, discount, shipping, payable, orderTime);
    }

    @Test
    @DisplayName("Order constructor initializes valid pending order with matching fields")
    void testValidOrderCreation() {
        assertEquals("ord-100", order.getOrderId());
        assertEquals("usr-1", order.getUserId());
        assertEquals(2, order.getItems().size());
        assertEquals(sampleAddress, order.getDeliveryAddress());
        assertEquals(subtotal, order.getItemsSubtotal());
        assertEquals(discount, order.getDiscountAmount());
        assertEquals(shipping, order.getShippingFee());
        assertEquals(payable, order.getPayableAmount());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(orderTime, order.getCreatedAt());
        assertTrue(order.getPaidAt().isEmpty());
        assertTrue(order.canBeCancelled());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Order constructor rejects invalid orderId")
    void testInvalidOrderId(String invalidId) {
        assertThrows(IllegalArgumentException.class, () ->
                new Order(invalidId, "usr-1", sampleItems, sampleAddress,
                        subtotal, discount, shipping, payable, orderTime));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Order constructor rejects invalid userId")
    void testInvalidUserId(String invalidId) {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ord-1", invalidId, sampleItems, sampleAddress,
                        subtotal, discount, shipping, payable, orderTime));
    }

    @Test
    @DisplayName("Order constructor rejects null or empty items list")
    void testNullOrEmptyItems() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ord-1", "usr-1", null, sampleAddress,
                        subtotal, discount, shipping, payable, orderTime));

        assertThrows(IllegalArgumentException.class, () ->
                new Order("ord-1", "usr-1", List.of(), sampleAddress,
                        subtotal, discount, shipping, payable, orderTime));
    }

    @Test
    @DisplayName("Order constructor rejects items containing null element")
    void testNullElementInItems() {
        List<OrderItem> itemsWithNull = new ArrayList<>();
        itemsWithNull.add(item1);
        itemsWithNull.add(null);

        assertThrows(IllegalArgumentException.class, () ->
                new Order("ord-1", "usr-1", itemsWithNull, sampleAddress,
                        subtotal, discount, shipping, payable, orderTime));
    }

    @Test
    @DisplayName("Order constructor rejects null delivery address")
    void testNullDeliveryAddress() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ord-1", "usr-1", sampleItems, null,
                        subtotal, discount, shipping, payable, orderTime));
    }

    @Test
    @DisplayName("Order constructor rejects currency mismatch among financial amounts")
    void testCurrencyMismatch() {
        Money dollarDiscount = new Money(new BigDecimal("10.00"), "USD");
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ord-1", "usr-1", sampleItems, sampleAddress,
                        subtotal, dollarDiscount, shipping, payable, orderTime));
    }

    @Test
    @DisplayName("Order constructor rejects payable amount that violates algebraic invariant")
    void testAlgebraicMismatch() {
        // Correct is 240,000, passing 200,000 should be rejected
        Money wrongPayable = new Money(new BigDecimal("200000.00"), "IRR");
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ord-1", "usr-1", sampleItems, sampleAddress,
                        subtotal, discount, shipping, wrongPayable, orderTime));
    }

    @Test
    @DisplayName("Order constructor rejects null createdAt")
    void testNullCreatedAt() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ord-1", "usr-1", sampleItems, sampleAddress,
                        subtotal, discount, shipping, payable, null));
    }

    @Test
    @DisplayName("markAsPaid() transitions PENDING order to PAID and records paidAt")
    void testMarkAsPaidFromPending() {
        LocalDateTime payTime = orderTime.plusMinutes(10);
        order.markAsPaid(payTime);

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertTrue(order.getPaidAt().isPresent());
        assertEquals(payTime, order.getPaidAt().get());
        assertTrue(order.canBeCancelled()); // Paid orders can be cancelled prior to processing
    }

    @Test
    @DisplayName("markAsPaid() transitions PAYMENT_FAILED order to PAID upon successful retry")
    void testMarkAsPaidFromPaymentFailed() {
        order.markAsPaymentFailed();
        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());

        LocalDateTime payTime = orderTime.plusMinutes(15);
        order.markAsPaid(payTime);

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(payTime, order.getPaidAt().get());
    }

    @Test
    @DisplayName("markAsPaid() rejects null timestamp or invalid prior status")
    void testMarkAsPaidInvalid() {
        assertThrows(IllegalArgumentException.class, () -> order.markAsPaid(null));

        order.markAsPaid(orderTime.plusMinutes(5));
        // Calling markAsPaid again on already PAID order throws IllegalStateException
        assertThrows(IllegalStateException.class, () -> order.markAsPaid(orderTime.plusMinutes(10)));
    }

    @Test
    @DisplayName("markAsPaymentFailed() transitions PENDING order to PAYMENT_FAILED")
    void testMarkAsPaymentFailedFromPending() {
        order.markAsPaymentFailed();
        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        assertTrue(order.canBeCancelled());
    }

    @Test
    @DisplayName("markAsPaymentFailed() throws IllegalStateException from non-PENDING status")
    void testMarkAsPaymentFailedFromInvalidStatus() {
        order.markAsPaid(orderTime.plusMinutes(5));
        assertThrows(IllegalStateException.class, () -> order.markAsPaymentFailed());
    }

    @Test
    @DisplayName("updateStatus() guides full order fulfillment flow (PAID -> PROCESSING -> SHIPPED -> DELIVERED)")
    void testSequentialFulfillmentFlow() {
        order.markAsPaid(orderTime.plusMinutes(5));
        assertEquals(OrderStatus.PAID, order.getStatus());

        order.updateStatus(OrderStatus.PROCESSING);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertFalse(order.canBeCancelled()); // Once processing begins, cancellation is locked

        order.updateStatus(OrderStatus.SHIPPED);
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertFalse(order.canBeCancelled());

        order.updateStatus(OrderStatus.DELIVERED);
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertFalse(order.canBeCancelled());
    }

    @Test
    @DisplayName("updateStatus() rejects invalid status skips and backward transitions")
    void testInvalidTransitions() {
        // PENDING cannot jump directly to PROCESSING
        assertThrows(IllegalStateException.class, () -> order.updateStatus(OrderStatus.PROCESSING));
        // PENDING cannot jump directly to SHIPPED
        assertThrows(IllegalStateException.class, () -> order.updateStatus(OrderStatus.SHIPPED));
        // PENDING cannot jump directly to DELIVERED
        assertThrows(IllegalStateException.class, () -> order.updateStatus(OrderStatus.DELIVERED));

        order.markAsPaid(orderTime.plusMinutes(5));
        // PAID cannot skip PROCESSING to SHIPPED
        assertThrows(IllegalStateException.class, () -> order.updateStatus(OrderStatus.SHIPPED));

        order.updateStatus(OrderStatus.PROCESSING);
        // PROCESSING cannot go backwards to PAID
        assertThrows(IllegalStateException.class, () -> order.updateStatus(OrderStatus.PAID));

        assertThrows(IllegalArgumentException.class, () -> order.updateStatus(null));
    }

    @Test
    @DisplayName("updateStatus(CANCELLED) successfully delegates to cancel()")
    void testUpdateStatusCancelledDelegation() {
        order.updateStatus(OrderStatus.CANCELLED);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("cancel() succeeds from PENDING, PAYMENT_FAILED, and PAID states")
    void testCancelAllowedStates() {
        // 1. Cancel from PENDING
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        // 2. Cancel from PAYMENT_FAILED
        Order orderFailed = new Order("ord-101", "usr-1", sampleItems, sampleAddress,
                subtotal, discount, shipping, payable, orderTime);
        orderFailed.markAsPaymentFailed();
        orderFailed.cancel();
        assertEquals(OrderStatus.CANCELLED, orderFailed.getStatus());

        // 3. Cancel from PAID
        Order orderPaid = new Order("ord-102", "usr-1", sampleItems, sampleAddress,
                subtotal, discount, shipping, payable, orderTime);
        orderPaid.markAsPaid(orderTime.plusMinutes(5));
        orderPaid.cancel();
        assertEquals(OrderStatus.CANCELLED, orderPaid.getStatus());
    }

    @Test
    @DisplayName("cancel() fails when order is in non-cancellable state")
    void testCancelForbiddenStates() {
        order.markAsPaid(orderTime.plusMinutes(5));
        order.updateStatus(OrderStatus.PROCESSING);

        // In PROCESSING, cannot cancel
        assertThrows(IllegalStateException.class, () -> order.cancel());

        order.updateStatus(OrderStatus.SHIPPED);
        // In SHIPPED, cannot cancel
        assertThrows(IllegalStateException.class, () -> order.cancel());

        order.updateStatus(OrderStatus.DELIVERED);
        // In DELIVERED, cannot cancel
        assertThrows(IllegalStateException.class, () -> order.cancel());
    }

    @Test
    @DisplayName("Representation exposure: getItems() is unmodifiable and defensive")
    void testItemsImmutability() {
        List<OrderItem> items = order.getItems();
        OrderItem extra = new OrderItem("prod-3", "Screen",
                new Money(new BigDecimal("10000.00"), "IRR"), new Quantity(1));

        assertThrows(UnsupportedOperationException.class, () -> items.add(extra));

        // Mutating external sampleItems after creation does not affect order.getItems()
        sampleItems.add(extra);
        assertEquals(2, order.getItems().size());
    }

    @Test
    @DisplayName("Initial transactions list is empty and defensively unmodifiable")
    void testTransactionsInitialAndUnmodifiable() {
        assertTrue(order.getTransactions().isEmpty());

        PaymentTransaction tx = new PaymentTransaction(
                "txn-1", order.getOrderId(), order.getPayableAmount(),
                PaymentMethod.ONLINE_GATEWAY, PaymentStatus.SUCCESS, LocalDateTime.now(), "REF-1"
        );
        assertThrows(UnsupportedOperationException.class, () -> order.getTransactions().add(tx));
    }

    @Test
    @DisplayName("recordTransaction successfully adds transactions and validates inputs")
    void testRecordTransaction() {
        PaymentTransaction failedTx = new PaymentTransaction(
                "txn-1", order.getOrderId(), order.getPayableAmount(),
                PaymentMethod.ONLINE_GATEWAY, PaymentStatus.FAILED, LocalDateTime.now(), null
        );
        order.recordTransaction(failedTx);
        assertEquals(1, order.getTransactions().size());
        assertEquals("txn-1", order.getTransactions().get(0).transactionId());
        assertEquals(PaymentStatus.FAILED, order.getTransactions().get(0).status());

        PaymentTransaction successTx = new PaymentTransaction(
                "txn-2", order.getOrderId(), order.getPayableAmount(),
                PaymentMethod.WALLET, PaymentStatus.SUCCESS, LocalDateTime.now(), "REF-2"
        );
        order.recordTransaction(successTx);
        assertEquals(2, order.getTransactions().size());
        assertEquals("txn-2", order.getTransactions().get(1).transactionId());
        assertEquals(PaymentStatus.SUCCESS, order.getTransactions().get(1).status());

        assertThrows(IllegalArgumentException.class, () -> order.recordTransaction(null));

        PaymentTransaction mismatchTx = new PaymentTransaction(
                "txn-3", "ord-different", order.getPayableAmount(),
                PaymentMethod.WALLET, PaymentStatus.SUCCESS, LocalDateTime.now(), "REF-3"
        );
        assertThrows(IllegalArgumentException.class, () -> order.recordTransaction(mismatchTx));
    }

    @Test
    @DisplayName("Order equality is strictly based on identity orderId")
    void testOrderEquality() {
        Order orderSameId = new Order("ord-100", "usr-different", sampleItems, sampleAddress,
                subtotal, discount, shipping, payable, orderTime.plusDays(1));

        Order orderDiffId = new Order("ord-999", "usr-1", sampleItems, sampleAddress,
                subtotal, discount, shipping, payable, orderTime);

        assertEquals(order, orderSameId);
        assertEquals(order.hashCode(), orderSameId.hashCode());
        assertNotEquals(order, orderDiffId);
        assertNotEquals(order, null);
        assertNotEquals(order, "not-an-order");
    }

    @Test
    @DisplayName("toString() contains orderId and userId")
    void testToString() {
        String str = order.toString();
        assertTrue(str.contains("ord-100"));
        assertTrue(str.contains("usr-1"));
    }
}
