package org.example.infrastructure.payment;

import org.example.exception.InvalidInputException;
import org.example.infrastructure.security.UuidIdGenerator;
import org.example.model.enums.PaymentMethod;
import org.example.service.port.IdGenerator;
import org.example.service.port.PaymentGateway;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentGatewaysAndRegistryTest {

    private final IdGenerator idGenerator = new UuidIdGenerator();

    @Nested
    class OnlineGatewayContract extends PaymentGatewayContractTest {
        @Override
        protected PaymentGateway createGateway() {
            return new OnlinePaymentGateway(idGenerator);
        }

        @Test
        void usesOnlineMethodAndRefPrefix() {
            PaymentGateway gw = createGateway();
            assertEquals(PaymentMethod.ONLINE_GATEWAY, gw.method());
        }
    }

    @Nested
    class WalletGatewayContract extends PaymentGatewayContractTest {
        @Override
        protected PaymentGateway createGateway() {
            return new WalletPaymentGateway(idGenerator);
        }

        @Test
        void usesWalletMethod() {
            assertEquals(PaymentMethod.WALLET, createGateway().method());
        }
    }

    @Nested
    class CashOnDeliveryGatewayContract extends PaymentGatewayContractTest {
        @Override
        protected PaymentGateway createGateway() {
            return new CashOnDeliveryPaymentGateway(idGenerator);
        }

        @Test
        void usesCashOnDeliveryMethod() {
            assertEquals(PaymentMethod.CASH_ON_DELIVERY, createGateway().method());
        }
    }

    @Test
    void registryResolvesAllRegisteredGatewaysAndRejectsMissingOrDuplicate() {
        OnlinePaymentGateway online = new OnlinePaymentGateway(idGenerator);
        WalletPaymentGateway wallet = new WalletPaymentGateway(idGenerator);
        CashOnDeliveryPaymentGateway cod = new CashOnDeliveryPaymentGateway(idGenerator);

        MapPaymentGatewayRegistry registry = new MapPaymentGatewayRegistry(online, wallet, cod);
        assertSame(online, registry.resolve(PaymentMethod.ONLINE_GATEWAY));
        assertSame(wallet, registry.resolve(PaymentMethod.WALLET));
        assertSame(cod, registry.resolve(PaymentMethod.CASH_ON_DELIVERY));
        assertThrows(InvalidInputException.class, () -> registry.resolve(null));

        MapPaymentGatewayRegistry partialRegistry = new MapPaymentGatewayRegistry(online);
        assertThrows(InvalidInputException.class, () -> partialRegistry.resolve(PaymentMethod.WALLET));
        assertThrows(IllegalArgumentException.class, () -> new MapPaymentGatewayRegistry(online, online));
    }
}
