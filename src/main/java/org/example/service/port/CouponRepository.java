package org.example.service.port;

import java.util.Optional;
import org.example.model.vo.Coupon;

/**
 * Outbound persistence port for promotional {@link Coupon} value objects.
 */
public interface CouponRepository {

    /**
     * Finds a coupon by its code (case-insensitive, trimmed).
     *
     * @param code non-blank coupon code
     * @return matching coupon if present, otherwise empty
     * @throws IllegalArgumentException if {@code code} is null or blank
     */
    Optional<Coupon> findByCode(String code);

    /**
     * Persists a promotional coupon keyed by its normalized code.
     *
     * @param coupon non-null coupon value object
     */
    void save(Coupon coupon);
}
