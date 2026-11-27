package org.example.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.exception.InvalidOrderStateException;
import org.example.exception.UnauthorizedActionException;
import org.example.infrastructure.payment.MapPaymentGatewayRegistry;
import org.example.infrastructure.payment.OnlinePaymentGateway;
import org.example.infrastructure.persistence.InMemoryCheckoutSnapshotRepository;
import org.example.infrastructure.persistence.InMemoryOrderRepository;
import org.example.model.entity.Cart;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.PaymentMethod;
import org.example.model.enums.PaymentStatus;
import org.example.model.vo.Address;
import org.example.model.vo.CartItem;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.service.api.CartCheckoutPort;
import org.example.service.api.ProductInventoryPort;
import org.example.service.port.CheckoutSnapshotRepository;
import org.example.service.port.IdGenerator;
import org.example.service.port.OrderRepository;
import org.example.service.port.PaymentGateway;
import org.example.service.port.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderPaymentServiceTest {

    private OrderRepository orderRepository;
    private CheckoutSnapshotRepository snapshotRepository;
    private Product product;
    private Cart cart;
    private Order pendingOrder;
    private IdGenerator idGenerator;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);
        AtomicInteger seq = new AtomicInteger(1);
        idGenerator = prefix -> prefix + "-" + seq.getAndIncrement();

        orderRepository = new InMemoryOrderRepository();
        snapshotRepository = new InMemoryCheckoutSnapshotRepository();

        Money unitPrice = new Money(new BigDecimal("150000.00"), "IRR");
        Money zero = new Money(BigDecimal.ZERO, "IRR");
        product = new Product("prod-1", "Mouse", "Wireless", unitPrice, 5, "CAT-1");
        cart = new Cart("cart-1", "usr-1");
        cart.addProduct(product, new Quantity(2));

        Address address = new Address("addr-1", "Ali", "09121112233", "Tehran", "Tehran",
                "Hafez St", "1234567890", "Home", true);
        OrderItem item = new OrderItem("prod-1", "Mouse", unitPrice, new Quantity(2));
        Money total = new Money(new BigDecimal("300000.00"), "IRR");
        pendingOrder = new Order("ord-1", "usr-1", List.of(item), address, total, zero, zero, total, LocalDateTime.now(clock));

        orderRepository.save(pendingOrder);
        snapshotRepository.saveSnapshot("ord-1", cart.getItems());
    }

    private OrderPaymentService createPaymentService(PaymentGateway gateway) {
        CartCheckoutPort cartPort = new CartCheckoutPort() {
            @Override
            public Cart getCart(String userId) {
                return cart;
            }

            @Override
            public void removeCheckedOutItems(String userId, List<CartItem> snapshot) {
                cart.removeCheckedOutItems(snapshot);
            }

            @Override
            public void clearCart(String userId) {
                cart.clear();
            }
        };

        ProductInventoryPort inventoryPort = new ProductInventoryPort() {
            @Override
            public void adjustStock(String productId, int delta) {
                if (delta > 0) {
                    product.restock(new Quantity(delta));
                } else if (delta < 0) {
                    product.deductStock(new Quantity(-delta));
                }
            }

            @Override
            public void recordSale(String productId, Quantity quantity) {
                product.recordSale(quantity);
            }
        };

        return new OrderPaymentService(
                orderRepository,
                snapshotRepository,
                cartPort,
                (pid, qty) -> product,
                inventoryPort,
                new MapPaymentGatewayRegistry(gateway),
                idGenerator,
                clock
        );
    }

    @Test
    void approvesPaymentDeductsStockRecordsSaleAndClearsSnapshotItems() {
        OrderPaymentService service = createPaymentService(new OnlinePaymentGateway(idGenerator));

        boolean paid = service.processPayment("usr-1", "ord-1", PaymentMethod.ONLINE_GATEWAY, true);
        assertTrue(paid);
        assertEquals(OrderStatus.PAID, pendingOrder.getStatus());
        assertEquals(3, product.getAvailableStock());
        assertEquals(2, product.getSoldCount());
        assertTrue(cart.isEmpty());
        assertEquals(PaymentStatus.SUCCESS, pendingOrder.getTransactions().getFirst().status());
        assertThrows(InvalidOrderStateException.class,
                () -> service.processPayment("ord-1", PaymentMethod.ONLINE_GATEWAY, true));
    }

    @Test
    void declinedPaymentPreservesStockAndAllowsRetry() {
        OrderPaymentService service = createPaymentService(new OnlinePaymentGateway(idGenerator));

        assertFalse(service.processPayment("ord-1", PaymentMethod.ONLINE_GATEWAY, false));
        assertEquals(OrderStatus.PAYMENT_FAILED, pendingOrder.getStatus());
        assertEquals(5, product.getAvailableStock());

        assertTrue(service.processPayment("ord-1", PaymentMethod.ONLINE_GATEWAY, true));
        assertEquals(OrderStatus.PAID, pendingOrder.getStatus());
        assertEquals(2, pendingOrder.getTransactions().size());
    }

    @Test
    void acceptsCustomGatewayImplementationWithoutModifyingService_OcpVerification() {
        PaymentGateway customWalletGateway = new PaymentGateway() {
            @Override
            public PaymentMethod method() {
                return PaymentMethod.WALLET;
            }

            @Override
            public PaymentResult charge(Order order, Money amount, boolean simulateSuccess) {
                return PaymentResult.approved("CUSTOM-WAL-777");
            }
        };

        OrderPaymentService service = createPaymentService(customWalletGateway);
        assertTrue(service.processPayment("usr-1", "ord-1", PaymentMethod.WALLET, true));
        assertEquals("CUSTOM-WAL-777", pendingOrder.getTransactions().getFirst().referenceCode());
        assertThrows(UnauthorizedActionException.class,
                () -> service.processPayment("other", "ord-1", PaymentMethod.WALLET, true));
    }
}
