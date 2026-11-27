package org.example.infrastructure.payment;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.example.exception.InvalidInputException;
import org.example.model.enums.PaymentMethod;
import org.example.service.port.PaymentGateway;
import org.example.service.port.PaymentGatewayRegistry;

/**
 * Map-backed implementation of {@link PaymentGatewayRegistry} supporting Open-Closed registration
 * of {@link PaymentGateway} implementations without conditional switching.
 */
public class MapPaymentGatewayRegistry implements PaymentGatewayRegistry {

    private final Map<PaymentMethod, PaymentGateway> gateways;

    /**
     * Constructs a registry from the provided collection of payment gateways.
     *
     * @param gateways collection of non-null gateways with unique payment methods
     */
    public MapPaymentGatewayRegistry(Collection<? extends PaymentGateway> gateways) {
        Objects.requireNonNull(gateways, "gateways cannot be null");
        Map<PaymentMethod, PaymentGateway> map = new EnumMap<>(PaymentMethod.class);
        for (PaymentGateway gateway : gateways) {
            Objects.requireNonNull(gateway, "gateway entry cannot be null");
            PaymentMethod method = Objects.requireNonNull(gateway.method(), "gateway.method() cannot be null");
            if (map.containsKey(method)) {
                throw new IllegalArgumentException("Duplicate payment gateway registered for method: " + method);
            }
            map.put(method, gateway);
        }
        this.gateways = Map.copyOf(map);
    }

    /**
     * Convenience constructor accepting gateway instances directly.
     *
     * @param gateways gateway instances to register
     */
    public MapPaymentGatewayRegistry(PaymentGateway... gateways) {
        this(List.of(Objects.requireNonNull(gateways, "gateways cannot be null")));
    }

    @Override
    public PaymentGateway resolve(PaymentMethod method) {
        if (method == null) {
            throw new InvalidInputException("Payment method cannot be null");
        }
        PaymentGateway gateway = gateways.get(method);
        if (gateway == null) {
            throw new InvalidInputException("No payment gateway registered for method: " + method);
        }
        return gateway;
    }
}
