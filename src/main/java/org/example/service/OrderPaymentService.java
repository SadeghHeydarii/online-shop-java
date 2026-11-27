package org.example.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.example.exception.InvalidInputException;
import org.example.exception.InvalidOrderStateException;
import org.example.model.entity.Order;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.enums.PaymentStatus;
import org.example.model.vo.CartItem;
import org.example.model.vo.OrderItem;
import org.example.model.vo.PaymentTransaction;
import org.example.service.api.CartCheckoutPort;
import org.example.service.api.PaymentUseCase;
import org.example.service.api.ProductAvailabilityPort;
import org.example.service.api.ProductInventoryPort;
import org.example.service.port.CheckoutSnapshotRepository;
import org.example.service.port.IdGenerator;
import org.example.service.port.OrderRepository;
import org.example.service.port.PaymentGateway;
import org.example.service.port.PaymentGatewayRegistry;
import org.example.service.port.PaymentResult;

/**
 * Cohesive payment execution service authorizing transactions via {@link PaymentGatewayRegistry},
 * finalizing inventory deductions, and clearing checked-out cart snapshots (SRP, OCP, DIP).
 */
public class OrderPaymentService implements PaymentUseCase {

    private final OrderRepository orderRepository;
    private final CheckoutSnapshotRepository snapshotRepository;
    private final CartCheckoutPort cartCheckoutPort;
    private final ProductAvailabilityPort productAvailabilityPort;
    private final ProductInventoryPort productInventoryPort;
    private final PaymentGatewayRegistry paymentGatewayRegistry;
    private final IdGenerator idGenerator;
    private final Clock clock;

    /**
     * Constructs an {@code OrderPaymentService} with all required ports and gateway registry.
     *
     * @param orderRepository         persistence port for order aggregates
     * @param snapshotRepository      persistence port for checkout cart-item snapshots
     * @param cartCheckoutPort        cart port for post-payment cart cleanup
     * @param productAvailabilityPort product port verifying stock before deduction
     * @param productInventoryPort    inventory port deducting stock and recording sales
     * @param paymentGatewayRegistry  registry resolving {@link PaymentGateway} strategies by method
     * @param idGenerator             identifier generator for transaction IDs
     * @param clock                   clock for payment transaction timestamps
     */
    public OrderPaymentService(OrderRepository orderRepository,
                               CheckoutSnapshotRepository snapshotRepository,
                               CartCheckoutPort cartCheckoutPort,
                               ProductAvailabilityPort productAvailabilityPort,
                               ProductInventoryPort productInventoryPort,
                               PaymentGatewayRegistry paymentGatewayRegistry,
                               IdGenerator idGenerator,
                               Clock clock) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository cannot be null");
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository cannot be null");
        this.cartCheckoutPort = Objects.requireNonNull(cartCheckoutPort, "cartCheckoutPort cannot be null");
        this.productAvailabilityPort = Objects.requireNonNull(productAvailabilityPort, "productAvailabilityPort cannot be null");
        this.productInventoryPort = Objects.requireNonNull(productInventoryPort, "productInventoryPort cannot be null");
        this.paymentGatewayRegistry = Objects.requireNonNull(paymentGatewayRegistry, "paymentGatewayRegistry cannot be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public synchronized boolean processPayment(String orderId, PaymentMethod method, boolean simulateSuccess) {
        OrderLifecycleSupport.requireNonBlank(orderId, "Order ID");
        if (method == null) {
            throw new InvalidInputException("Payment method cannot be null");
        }

        Order order = OrderLifecycleSupport.requireOrder(orderRepository, orderId);
        OrderStatus currentStatus = order.getStatus();
        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.PAYMENT_FAILED) {
            throw new InvalidOrderStateException(
                    "Order '" + orderId + "' cannot be processed for payment because it is in status " + currentStatus
            );
        }

        PaymentGateway gateway = paymentGatewayRegistry.resolve(method);

        if (simulateSuccess) {
            for (OrderItem item : order.getItems()) {
                productAvailabilityPort.requireAvailableProduct(item.productId(), item.quantity());
            }
        }

        PaymentResult result = gateway.charge(order, order.getPayableAmount(), simulateSuccess);
        LocalDateTime paymentTime = LocalDateTime.now(clock);

        if (result.success()) {
            for (OrderItem item : order.getItems()) {
                productInventoryPort.adjustStock(item.productId(), -item.quantity().value());
                productInventoryPort.recordSale(item.productId(), item.quantity());
            }

            PaymentTransaction tx = new PaymentTransaction(
                    idGenerator.nextId("txn"),
                    order.getOrderId(),
                    order.getPayableAmount(),
                    method,
                    PaymentStatus.SUCCESS,
                    paymentTime,
                    result.referenceCode()
            );
            order.recordTransaction(tx);
            order.markAsPaid(paymentTime);
            orderRepository.save(order);

            List<CartItem> snapshot = snapshotRepository.removeSnapshot(order.getOrderId()).orElse(null);
            if (snapshot != null) {
                cartCheckoutPort.removeCheckedOutItems(order.getUserId(), snapshot);
            } else {
                cartCheckoutPort.clearCart(order.getUserId());
            }
            return true;
        }

        PaymentTransaction failedTx = new PaymentTransaction(
                idGenerator.nextId("txn"),
                order.getOrderId(),
                order.getPayableAmount(),
                method,
                PaymentStatus.FAILED,
                paymentTime,
                result.referenceCode()
        );
        order.recordTransaction(failedTx);
        order.markAsPaymentFailed();
        orderRepository.save(order);
        return false;
    }

    @Override
    public synchronized boolean processPayment(String userId, String orderId, PaymentMethod method, boolean simulateSuccess) {
        OrderLifecycleSupport.requireOwnedOrder(orderRepository, userId, orderId);
        return processPayment(orderId, method, simulateSuccess);
    }
}
