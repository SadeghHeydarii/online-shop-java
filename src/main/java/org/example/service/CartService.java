package org.example.service;

import java.util.List;
import java.util.Objects;
import org.example.exception.InsufficientStockException;
import org.example.exception.InvalidInputException;
import org.example.model.entity.Cart;
import org.example.model.entity.Product;
import org.example.model.vo.CartItem;
import org.example.model.vo.Quantity;
import org.example.service.api.CartCheckoutPort;
import org.example.service.api.CartUseCase;
import org.example.service.api.ProductAvailabilityPort;
import org.example.service.port.CartRepository;

/**
 * Application service coordinating customer shopping carts against persistence and
 * narrow product-availability ports (SRP, ISP, DIP).
 */
public class CartService implements CartUseCase, CartCheckoutPort {

    private final CartRepository cartRepository;
    private final ProductAvailabilityPort productAvailabilityPort;

    /**
     * Constructs a {@code CartService} backed by segregated repository and availability ports.
     *
     * @param cartRepository          outbound repository port for {@link Cart} aggregates
     * @param productAvailabilityPort narrow product-availability port validating stock and visibility
     */
    public CartService(CartRepository cartRepository, ProductAvailabilityPort productAvailabilityPort) {
        this.cartRepository = Objects.requireNonNull(cartRepository, "cartRepository cannot be null");
        this.productAvailabilityPort = Objects.requireNonNull(
                productAvailabilityPort, "productAvailabilityPort cannot be null"
        );
    }

    @Override
    public Cart getCart(String userId) {
        String normalizedUserId = requireNonBlank(userId, "User ID");
        Cart cart = cartRepository.getOrCreate(normalizedUserId);
        if (!normalizedUserId.equals(cart.getUserId())) {
            throw new IllegalStateException("Cart ownership invariant violated: expected owner "
                    + normalizedUserId + " but found " + cart.getUserId());
        }
        return cart;
    }

    @Override
    public void addItem(String userId, String productId, Quantity quantity) {
        String targetUserId = requireNonBlank(userId, "User ID");
        String targetProductId = requireNonBlank(productId, "Product ID");
        if (quantity == null || quantity.value() <= 0) {
            throw new InvalidInputException("Quantity must be non-null and positive");
        }

        Cart cart = getCart(targetUserId);
        int currentQty = cart.quantityOf(targetProductId);
        int totalRequested = currentQty + quantity.value();

        Product product = productAvailabilityPort.requireAvailableProduct(
                targetProductId, new Quantity(totalRequested)
        );
        cart.addProduct(product, quantity);
        cartRepository.save(cart);
    }

    @Override
    public void updateItemQuantity(String userId, String productId, Quantity newQuantity) {
        String targetUserId = requireNonBlank(userId, "User ID");
        String targetProductId = requireNonBlank(productId, "Product ID");
        if (newQuantity == null) {
            throw new InvalidInputException("Quantity cannot be null");
        }

        Cart cart = getCart(targetUserId);
        if (cart.quantityOf(targetProductId) == 0) {
            throw new InvalidInputException("Product is not present in this cart");
        }

        if (newQuantity.value() <= 0) {
            cart.removeProduct(targetProductId);
            cartRepository.save(cart);
            return;
        }

        productAvailabilityPort.requireAvailableProduct(targetProductId, newQuantity);
        cart.updateQuantity(targetProductId, newQuantity);
        cartRepository.save(cart);
    }

    @Override
    public void removeItem(String userId, String productId) {
        String targetUserId = requireNonBlank(userId, "User ID");
        String targetProductId = requireNonBlank(productId, "Product ID");

        Cart cart = getCart(targetUserId);
        if (cart.quantityOf(targetProductId) == 0) {
            throw new InvalidInputException("Product is not present in this cart");
        }
        cart.removeProduct(targetProductId);
        cartRepository.save(cart);
    }

    @Override
    public void clearCart(String userId) {
        String targetUserId = requireNonBlank(userId, "User ID");
        Cart cart = getCart(targetUserId);
        cart.clear();
        cartRepository.save(cart);
    }

    @Override
    public void removeCheckedOutItems(String userId, List<CartItem> snapshot) {
        Cart cart = getCart(userId);
        cart.removeCheckedOutItems(snapshot);
        cartRepository.save(cart);
    }

    private static String requireNonBlank(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(fieldLabel + " cannot be null or blank");
        }
        return value.trim();
    }
}
