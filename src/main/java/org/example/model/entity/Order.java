package org.example.model.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.example.model.enums.OrderStatus;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import java.util.ArrayList;
import org.example.model.vo.OrderItem;
import org.example.model.vo.PaymentTransaction;

/**
 * Represents an immutable customer purchase order entity and aggregate root.
 *
 * <p>An order captures an immutable snapshot of line items ({@link OrderItem}),
 * delivery address, item subtotal, discount, shipping fee, and calculated payable
 * amount at the point of checkout. The order's lifecycle progresses through discrete
 * states defined by {@link OrderStatus} in accordance with the domain statechart.
 *
 * <p><b>Abstraction Function (AF):</b>
 * AF(orderId, userId, items, deliveryAddress, itemsSubtotal, discountAmount, shippingFee,
 * payableAmount, status, createdAt, paidAt) =
 * A legally binding commercial order identified by {@code orderId}, submitted by customer
 * {@code userId}, shipping to {@code deliveryAddress}, containing items {@code items},
 * totaling {@code itemsSubtotal} with discount {@code discountAmount} and shipping {@code shippingFee}
 * for net payable {@code payableAmount}, currently in lifecycle state {@code status}, created at
 * {@code createdAt}, and paid at {@code paidAt} (if completed).
 *
 * <p><b>Representation Invariant (RI):</b>
 * <ul>
 *   <li>{@code orderId != null && !orderId.isBlank()}</li>
 *   <li>{@code userId != null && !userId.isBlank()}</li>
 *   <li>{@code items != null && !items.isEmpty()} and contains no null elements</li>
 *   <li>{@code deliveryAddress != null}</li>
 *   <li>{@code itemsSubtotal != null && !itemsSubtotal.isNegative()}</li>
 *   <li>{@code discountAmount != null && !discountAmount.isNegative()}</li>
 *   <li>{@code shippingFee != null && !shippingFee.isNegative()}</li>
 *   <li>{@code payableAmount != null && !payableAmount.isNegative()}</li>
 *   <li>All four money components have identical currency codes</li>
 *   <li>{@code payableAmount.equals(itemsSubtotal.subtract(discountAmount).add(shippingFee))}</li>
 *   <li>{@code status != null}</li>
 *   <li>{@code createdAt != null}</li>
 *   <li>If {@code status} is PAID, PROCESSING, SHIPPED, or DELIVERED, then {@code paidAt != null}</li>
 * </ul>
 *
 * <p><b>Safety from Representation Exposure:</b>
 * The list of {@link OrderItem} snapshots is captured defensively via
 * {@link List#copyOf(java.util.Collection)} in the constructor and returned directly
 * as an unmodifiable list in {@link #getItems()}.
 */
public class Order {

    private final String orderId;
    private final String userId;
    private final List<OrderItem> items;
    private final Address deliveryAddress;
    private final Money itemsSubtotal;
    private final Money discountAmount;
    private final Money shippingFee;
    private final Money payableAmount;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private final List<PaymentTransaction> transactions;

    /**
     * Constructs a new {@code Order} entity in {@link OrderStatus#PENDING} status.
     *
     * @param orderId         the unique order identifier
     * @param userId          the identifier of the purchasing customer
     * @param items           the non-empty list of snapshot order items
     * @param deliveryAddress the destination physical delivery address
     * @param itemsSubtotal   the sum of item line totals
     * @param discountAmount  the discount applied to the order
     * @param shippingFee     the shipping and logistics fee
     * @param payableAmount   the final calculated payable amount
     * @param createdAt       the timestamp when the order was placed
     * @throws IllegalArgumentException if any argument violates invariants or algebraic consistency
     */
    public Order(String orderId, String userId, List<OrderItem> items,
                 Address deliveryAddress, Money itemsSubtotal, Money discountAmount,
                 Money shippingFee, Money payableAmount, LocalDateTime createdAt) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order items list cannot be null or empty");
        }
        for (OrderItem item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Order items cannot contain null elements");
            }
        }
        if (deliveryAddress == null) {
            throw new IllegalArgumentException("Delivery address cannot be null");
        }
        if (itemsSubtotal == null || itemsSubtotal.isNegative()) {
            throw new IllegalArgumentException("Items subtotal must be non-null and non-negative");
        }
        if (discountAmount == null || discountAmount.isNegative()) {
            throw new IllegalArgumentException("Discount amount must be non-null and non-negative");
        }
        if (shippingFee == null || shippingFee.isNegative()) {
            throw new IllegalArgumentException("Shipping fee must be non-null and non-negative");
        }
        if (payableAmount == null || payableAmount.isNegative()) {
            throw new IllegalArgumentException("Payable amount must be non-null and non-negative");
        }

        String baseCurrency = itemsSubtotal.currency();
        if (!discountAmount.currency().equals(baseCurrency)
                || !shippingFee.currency().equals(baseCurrency)
                || !payableAmount.currency().equals(baseCurrency)) {
            throw new IllegalArgumentException("All monetary amounts in an order must use the same currency: "
                    + baseCurrency);
        }

        Money expectedPayable = itemsSubtotal.subtract(discountAmount).add(shippingFee);
        if (!payableAmount.equals(expectedPayable)) {
            throw new IllegalArgumentException("Payable amount does not match expected formula "
                    + "(itemsSubtotal - discountAmount + shippingFee): expected "
                    + expectedPayable + ", got " + payableAmount);
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("Creation timestamp cannot be null");
        }

        this.orderId = orderId;
        this.userId = userId;
        this.items = List.copyOf(items);
        this.deliveryAddress = deliveryAddress;
        this.itemsSubtotal = itemsSubtotal;
        this.discountAmount = discountAmount;
        this.shippingFee = shippingFee;
        this.payableAmount = payableAmount;
        this.status = OrderStatus.PENDING;
        this.createdAt = createdAt;
        this.paidAt = null;
        this.transactions = new ArrayList<>();
        checkRep();
    }

    /**
     * Asserts the representation invariant of the Order entity.
     */
    private void checkRep() {
        assert orderId != null && !orderId.isBlank() : "orderId cannot be null or blank";
        assert userId != null && !userId.isBlank() : "userId cannot be null or blank";
        assert items != null && !items.isEmpty() : "items cannot be null or empty";
        for (OrderItem item : items) {
            assert item != null : "item in items list cannot be null";
        }
        assert deliveryAddress != null : "deliveryAddress cannot be null";
        assert itemsSubtotal != null && !itemsSubtotal.isNegative() : "itemsSubtotal must be non-negative";
        assert discountAmount != null && !discountAmount.isNegative() : "discountAmount must be non-negative";
        assert shippingFee != null && !shippingFee.isNegative() : "shippingFee must be non-negative";
        assert payableAmount != null && !payableAmount.isNegative() : "payableAmount must be non-negative";

        String cur = itemsSubtotal.currency();
        assert discountAmount.currency().equals(cur) : "discount currency mismatch";
        assert shippingFee.currency().equals(cur) : "shipping currency mismatch";
        assert payableAmount.currency().equals(cur) : "payable currency mismatch";

        Money expected = itemsSubtotal.subtract(discountAmount).add(shippingFee);
        assert payableAmount.equals(expected) : "payable amount invariant violated";

        assert status != null : "status cannot be null";
        assert createdAt != null : "createdAt cannot be null";
        assert transactions != null : "transactions list cannot be null";
        for (PaymentTransaction tx : transactions) {
            assert tx != null : "transaction in transactions list cannot be null";
            assert orderId.equals(tx.orderId()) : "transaction orderId mismatch";
        }

        if (status == OrderStatus.PAID
                || status == OrderStatus.PROCESSING
                || status == OrderStatus.SHIPPED
                || status == OrderStatus.DELIVERED) {
            assert paidAt != null : "paidAt must be recorded for paid/fulfilled orders";
        }
    }

    /**
     * Returns the unique order identifier.
     *
     * @return order ID
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Returns the identifier of the purchasing user.
     *
     * @return user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the current lifecycle status of the order.
     *
     * @return order status
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Returns the final payable monetary amount of this order.
     *
     * @return payable amount
     */
    public Money getPayableAmount() {
        return payableAmount;
    }

    /**
     * Returns the subtotal monetary amount of the items in this order.
     *
     * @return items subtotal
     */
    public Money getItemsSubtotal() {
        return itemsSubtotal;
    }

    /**
     * Returns the discount amount deducted from the subtotal.
     *
     * @return discount amount
     */
    public Money getDiscountAmount() {
        return discountAmount;
    }

    /**
     * Returns the shipping fee charged for this order.
     *
     * @return shipping fee
     */
    public Money getShippingFee() {
        return shippingFee;
    }

    /**
     * Returns an unmodifiable snapshot list of items in this order.
     *
     * @return unmodifiable list of order items
     */
    public List<OrderItem> getItems() {
        return items;
    }

    /**
     * Returns the delivery address for this order.
     *
     * @return delivery address
     */
    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    /**
     * Returns the creation timestamp of this order.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the timestamp when payment was recorded, if paid.
     *
     * @return an {@link Optional} containing the payment timestamp, or empty if unpaid
     */
    public Optional<LocalDateTime> getPaidAt() {
        return Optional.ofNullable(paidAt);
    }

    /**
     * Returns an unmodifiable snapshot list of payment transactions recorded for this order.
     *
     * @return unmodifiable list of payment transactions
     */
    public List<PaymentTransaction> getTransactions() {
        return List.copyOf(transactions);
    }

    /**
     * Records a payment transaction associated with this order.
     *
     * @param transaction the payment transaction to record (must not be null and must match this order's ID)
     * @throws IllegalArgumentException if transaction is null or does not match this order's ID
     */
    public void recordTransaction(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (!transaction.orderId().equals(this.orderId)) {
            throw new IllegalArgumentException("Transaction order ID " + transaction.orderId()
                    + " does not match order ID " + this.orderId);
        }
        this.transactions.add(transaction);
        checkRep();
    }

    /**
     * Checks whether the order is currently in a state where cancellation is permitted.
     *
     * <p>According to the order statechart, orders may be cancelled while in
     * {@link OrderStatus#PENDING}, {@link OrderStatus#PAYMENT_FAILED}, or
     * {@link OrderStatus#PAID} (prior to warehouse processing).
     *
     * @return {@code true} if cancellable; {@code false} otherwise
     */
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING
                || status == OrderStatus.PAYMENT_FAILED
                || status == OrderStatus.PAID;
    }

    /**
     * Records successful payment of the order, transitioning status to {@link OrderStatus#PAID}.
     *
     * @param paymentTime the timestamp of successful payment confirmation
     * @throws IllegalArgumentException if paymentTime is null
     * @throws IllegalStateException    if the order is not in PENDING or PAYMENT_FAILED status
     */
    public void markAsPaid(LocalDateTime paymentTime) {
        if (paymentTime == null) {
            throw new IllegalArgumentException("Payment timestamp cannot be null");
        }
        if (status != OrderStatus.PENDING && status != OrderStatus.PAYMENT_FAILED) {
            throw new IllegalStateException("Cannot mark order as paid from status: " + status);
        }
        this.status = OrderStatus.PAID;
        this.paidAt = paymentTime;
        checkRep();
    }

    /**
     * Records payment failure for this order, transitioning status to {@link OrderStatus#PAYMENT_FAILED}.
     *
     * @throws IllegalStateException if the order is not in PENDING status
     */
    public void markAsPaymentFailed() {
        if (status != OrderStatus.PENDING && status != OrderStatus.PAYMENT_FAILED) {
            throw new IllegalStateException("Cannot mark payment failed from status: " + status);
        }
        this.status = OrderStatus.PAYMENT_FAILED;
        checkRep();
    }

    /**
     * Updates the lifecycle status of the order in accordance with the statechart.
     *
     * <p>Allowed sequential fulfillment transitions:
     * <ul>
     *   <li>{@code PAID -> PROCESSING} (warehouse preparation)</li>
     *   <li>{@code PROCESSING -> SHIPPED} (dispatched to carrier)</li>
     *   <li>{@code SHIPPED -> DELIVERED} (received by customer)</li>
     * </ul>
     * Cancellation via this method delegates to {@link #cancel()}.
     *
     * @param newStatus the new status to advance to
     * @throws IllegalArgumentException if newStatus is null
     * @throws IllegalStateException    if the requested transition is not permitted
     */
    public void updateStatus(OrderStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }

        if (newStatus == OrderStatus.CANCELLED) {
            cancel();
            return;
        }

        boolean validTransition = switch (this.status) {
            case PAID -> newStatus == OrderStatus.PROCESSING;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            default -> false;
        };

        if (!validTransition) {
            throw new IllegalStateException("Invalid order status transition from "
                    + this.status + " to " + newStatus);
        }

        this.status = newStatus;
        checkRep();
    }

    /**
     * Cancels the order if it is in a cancellable state.
     *
     * @throws IllegalStateException if the order is not in a cancellable state
     */
    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Order in status " + status + " cannot be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        checkRep();
    }

    /**
     * Compares this order to another object for equality based on entity identity ({@code orderId}).
     *
     * @param o the object to compare with
     * @return {@code true} if both are Order entities with identical IDs; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order other)) {
            return false;
        }
        return this.orderId.equals(other.orderId);
    }

    /**
     * Returns the hash code based on entity identity ({@code orderId}).
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "Order[orderId=" + orderId
                + ", userId=" + userId
                + ", status=" + status
                + ", itemsCount=" + items.size()
                + ", payableAmount=" + payableAmount
                + ", createdAt=" + createdAt
                + "]";
    }
}
