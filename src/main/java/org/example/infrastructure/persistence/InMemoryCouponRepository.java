package org.example.infrastructure.persistence;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.model.vo.Coupon;
import org.example.service.port.CouponRepository;

/**
 * Thread-safe in-memory implementation of {@link CouponRepository}.
 */
public class InMemoryCouponRepository implements CouponRepository {

    private final Map<String, Coupon> store;

    /**
     * Constructs an empty {@code InMemoryCouponRepository}.
     */
    public InMemoryCouponRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemoryCouponRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by uppercase coupon code
     */
    public InMemoryCouponRepository(Map<String, Coupon> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        String normalized = RepositoryPreconditions.requireNonBlank(code, "code").toUpperCase(Locale.ROOT);
        return Optional.ofNullable(store.get(normalized));
    }

    @Override
    public void save(Coupon coupon) {
        Objects.requireNonNull(coupon, "coupon cannot be null");
        store.put(coupon.code(), coupon);
    }
}
