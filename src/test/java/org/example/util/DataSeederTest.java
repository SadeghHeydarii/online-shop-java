package org.example.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.model.vo.Category;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.model.vo.Coupon;
import org.example.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DataSeeder Unit Tests")
class DataSeederTest {

    @Test
    @DisplayName("seed populates administrative, customer, catalog and promotional data")
    void seedSuccess() {
        Map<String, User> userStore = new ConcurrentHashMap<>();
        Map<String, String> credentialsStore = new ConcurrentHashMap<>();
        Map<String, Category> categoryStore = new ConcurrentHashMap<>();
        Map<String, Product> productStore = new ConcurrentHashMap<>();
        Map<String, Coupon> couponStore = new ConcurrentHashMap<>();
        ProductService productService = new ProductService(productStore, categoryStore);

        DataSeeder.seed(userStore, credentialsStore, categoryStore, productService, couponStore);

        // Verify Users
        User admin = userStore.get("usr-admin");
        assertNotNull(admin);
        assertEquals("admin@shop.com", admin.getEmail());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(PasswordUtil.verifyPassword("Admin@123", credentialsStore.get("usr-admin")));

        User customer = userStore.get("usr-customer");
        assertNotNull(customer);
        assertEquals("customer@shop.com", customer.getEmail());
        assertEquals(UserRole.CUSTOMER, customer.getRole());
        assertEquals(2, customer.getAddresses().size());
        assertTrue(customer.getDefaultAddress().isPresent());
        assertTrue(PasswordUtil.verifyPassword("Pass@123", credentialsStore.get("usr-customer")));

        // Verify Categories
        assertEquals(4, categoryStore.size());
        assertTrue(categoryStore.containsKey("CAT-DIGITAL"));
        assertTrue(categoryStore.containsKey("CAT-BOOKS"));
        assertTrue(categoryStore.containsKey("CAT-FASHION"));
        assertTrue(categoryStore.containsKey("CAT-HOME"));

        // Verify Products
        assertTrue(productStore.size() >= 5);
        assertFalse(productService.searchProducts("ThinkPad").isEmpty());
        assertFalse(productService.searchProducts("Clean Code").isEmpty());

        // Verify Coupons
        assertEquals(2, couponStore.size());
        assertTrue(couponStore.containsKey("WELCOME20"));
        assertTrue(couponStore.containsKey("AUT_SPECIAL"));
    }

    @Test
    @DisplayName("seed throws NullPointerException if any store argument is null")
    void seedNullValidation() {
        Map<String, User> userStore = new ConcurrentHashMap<>();
        Map<String, String> credentialsStore = new ConcurrentHashMap<>();
        Map<String, Category> categoryStore = new ConcurrentHashMap<>();
        Map<String, Product> productStore = new ConcurrentHashMap<>();
        Map<String, Coupon> couponStore = new ConcurrentHashMap<>();
        ProductService productService = new ProductService(productStore, categoryStore);

        assertThrows(NullPointerException.class, () ->
                DataSeeder.seed(null, credentialsStore, categoryStore, productService, couponStore));
        assertThrows(NullPointerException.class, () ->
                DataSeeder.seed(userStore, null, categoryStore, productService, couponStore));
        assertThrows(NullPointerException.class, () ->
                DataSeeder.seed(userStore, credentialsStore, null, productService, couponStore));
        assertThrows(NullPointerException.class, () ->
                DataSeeder.seed(userStore, credentialsStore, categoryStore, null, couponStore));
        assertThrows(NullPointerException.class, () ->
                DataSeeder.seed(userStore, credentialsStore, categoryStore, productService, null));
    }
}
