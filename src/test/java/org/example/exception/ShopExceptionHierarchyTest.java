package org.example.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShopException Hierarchy Tests")
class ShopExceptionHierarchyTest {

    @Test
    @DisplayName("All domain business exceptions extend ShopException while InvalidInputException preserves IllegalArgumentException")
    void exceptionHierarchyMatchesContracts() {
        assertTrue(ShopException.class.isAssignableFrom(AuthenticationException.class));
        assertTrue(ShopException.class.isAssignableFrom(CategoryNotFoundException.class));
        assertTrue(ShopException.class.isAssignableFrom(DuplicateUserException.class));
        assertTrue(ShopException.class.isAssignableFrom(InsufficientStockException.class));
        assertTrue(ShopException.class.isAssignableFrom(InvalidCouponException.class));
        assertTrue(ShopException.class.isAssignableFrom(InvalidOrderStateException.class));
        assertTrue(ShopException.class.isAssignableFrom(OrderNotFoundException.class));
        assertTrue(ShopException.class.isAssignableFrom(ProductNotFoundException.class));
        assertTrue(ShopException.class.isAssignableFrom(UnauthorizedActionException.class));
        assertTrue(ShopException.class.isAssignableFrom(UserNotFoundException.class));

        assertTrue(IllegalArgumentException.class.isAssignableFrom(InvalidInputException.class));

        Throwable cause = new IllegalStateException("root");
        AuthenticationException withCause = new AuthenticationException("auth failed", cause);
        assertEquals("auth failed", withCause.getMessage());
        assertSame(cause, withCause.getCause());
    }
}
