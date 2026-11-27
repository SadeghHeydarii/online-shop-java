package org.example.service.port;

import java.util.List;
import java.util.Optional;
import org.example.model.entity.Order;

/**
 * Outbound persistence port for managing {@link Order} aggregate roots.
 */
public interface OrderRepository {

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param orderId non-blank order identifier
     * @return matching order if present, otherwise empty
     * @throws IllegalArgumentException if {@code orderId} is null or blank
     */
    Optional<Order> findById(String orderId);

    /**
     * Retrieves all orders placed by the specified user.
     *
     * @param userId non-blank user identifier
     * @return unmodifiable list of the user's orders
     * @throws IllegalArgumentException if {@code userId} is null or blank
     */
    List<Order> findByUserId(String userId);

    /**
     * Returns a snapshot list of all stored orders.
     *
     * @return unmodifiable list of all orders
     */
    List<Order> findAll();

    /**
     * Persists a new or updated {@link Order} aggregate.
     *
     * @param order non-null order entity
     */
    void save(Order order);
}
