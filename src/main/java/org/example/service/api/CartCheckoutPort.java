package org.example.service.api;

import java.util.List;
import org.example.model.entity.Cart;
import org.example.model.vo.CartItem;

/**
 * Narrow port exposing shopping-cart operations required during checkout and post-payment cleanup (ISP).
 */
public interface CartCheckoutPort {

    /**
     * Retrieves the customer's active shopping cart.
     *
     * @param userId non-blank user identifier
     * @return user's cart
     */
    Cart getCart(String userId);

    /**
     * Selectively removes checked-out snapshot items from the user's cart.
     *
     * @param userId   non-blank user identifier
     * @param snapshot checked-out cart item snapshot
     */
    void removeCheckedOutItems(String userId, List<CartItem> snapshot);

    /**
     * Clears all items from the customer's cart.
     *
     * @param userId non-blank user identifier
     */
    void clearCart(String userId);
}
