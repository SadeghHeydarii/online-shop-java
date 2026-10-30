package org.example.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.InvalidInputException;
import org.example.exception.UserNotFoundException;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private Map<String, User> userStore;
    private UserService userService;
    private User testUser;
    private Address address1;
    private Address address2;

    @BeforeEach
    void setUp() {
        userStore = new ConcurrentHashMap<>();
        userService = new UserService(userStore);

        testUser = new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", "09121112233",
                "hash_abc", UserRole.CUSTOMER, LocalDateTime.now());
        userStore.put(testUser.getId(), testUser);

        address1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St", "1234567890", "Home", true);
        address2 = new Address("addr-2", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Valiasr St", "1234567891", "Work", false);
    }

    @Test
    @DisplayName("getProfile() retrieves existing user profile")
    void testGetProfileSuccess() {
        User retrieved = userService.getProfile("usr-1");
        assertNotNull(retrieved);
        assertEquals("usr-1", retrieved.getId());
        assertEquals("Ali Rezaei", retrieved.getFullName());
    }

    @Test
    @DisplayName("getProfile() throws UserNotFoundException for non-existent userId")
    void testGetProfileNotFound() {
        assertThrows(UserNotFoundException.class, () -> userService.getProfile("non-existent"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("getProfile() throws InvalidInputException on invalid userId")
    void testGetProfileInvalidId(String invalidId) {
        assertThrows(InvalidInputException.class, () -> userService.getProfile(invalidId));
    }

    @Test
    @DisplayName("updateProfile() updates user full name and phone number")
    void testUpdateProfileSuccess() {
        userService.updateProfile("usr-1", "Ali R. Hosseini", "09129998877");

        User updated = userService.getProfile("usr-1");
        assertEquals("Ali R. Hosseini", updated.getFullName());
        assertEquals("09129998877", updated.getPhone());
    }

    @Test
    @DisplayName("updateProfile() throws UserNotFoundException for unknown userId")
    void testUpdateProfileUnknownUser() {
        assertThrows(UserNotFoundException.class, () ->
                userService.updateProfile("non-existent", "Name", "09120000000"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("updateProfile() rejects invalid full name")
    void testUpdateProfileInvalidName(String invalidName) {
        assertThrows(InvalidInputException.class, () ->
                userService.updateProfile("usr-1", invalidName, "09129998877"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("updateProfile() rejects invalid phone")
    void testUpdateProfileInvalidPhone(String invalidPhone) {
        assertThrows(InvalidInputException.class, () ->
                userService.updateProfile("usr-1", "Ali R.", invalidPhone));
    }

    @Test
    @DisplayName("addAddress() adds delivery address to user profile")
    void testAddAddressSuccess() {
        userService.addAddress("usr-1", address1);

        User user = userService.getProfile("usr-1");
        assertEquals(1, user.getAddresses().size());
        assertEquals("addr-1", user.getAddresses().get(0).addressId());
        assertTrue(user.getDefaultAddress().isPresent());
        assertEquals("addr-1", user.getDefaultAddress().get().addressId());
    }

    @Test
    @DisplayName("addAddress() throws UserNotFoundException for unknown user")
    void testAddAddressUnknownUser() {
        assertThrows(UserNotFoundException.class, () ->
                userService.addAddress("non-existent", address1));
    }

    @Test
    @DisplayName("addAddress() throws InvalidInputException for null address")
    void testAddAddressNull() {
        assertThrows(InvalidInputException.class, () ->
                userService.addAddress("usr-1", null));
    }

    @Test
    @DisplayName("removeAddress() removes address from user address book")
    void testRemoveAddressSuccess() {
        userService.addAddress("usr-1", address1);
        userService.addAddress("usr-1", address2);
        assertEquals(2, userService.getProfile("usr-1").getAddresses().size());

        userService.removeAddress("usr-1", "addr-1");
        assertEquals(1, userService.getProfile("usr-1").getAddresses().size());
        assertEquals("addr-2", userService.getProfile("usr-1").getAddresses().get(0).addressId());
    }

    @Test
    @DisplayName("removeAddress() throws UserNotFoundException for unknown user")
    void testRemoveAddressUnknownUser() {
        assertThrows(UserNotFoundException.class, () ->
                userService.removeAddress("non-existent", "addr-1"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("removeAddress() rejects invalid address ID")
    void testRemoveAddressInvalidId(String invalidId) {
        assertThrows(InvalidInputException.class, () ->
                userService.removeAddress("usr-1", invalidId));
    }

    @Test
    @DisplayName("setDefaultAddress() updates default address across address list")
    void testSetDefaultAddressSuccess() {
        userService.addAddress("usr-1", address1); // default
        userService.addAddress("usr-1", address2); // non-default

        userService.setDefaultAddress("usr-1", "addr-2");

        User user = userService.getProfile("usr-1");
        assertTrue(user.getDefaultAddress().isPresent());
        assertEquals("addr-2", user.getDefaultAddress().get().addressId());

        Address updatedAddr1 = user.getAddresses().stream()
                .filter(a -> a.addressId().equals("addr-1")).findFirst().orElseThrow();
        assertFalse(updatedAddr1.isDefault());
    }

    @Test
    @DisplayName("setDefaultAddress() throws IllegalArgumentException if addressId does not exist in user addresses")
    void testSetDefaultAddressNonExistentAddress() {
        userService.addAddress("usr-1", address1);

        assertThrows(IllegalArgumentException.class, () ->
                userService.setDefaultAddress("usr-1", "addr-999"));
    }

    @Test
    @DisplayName("setDefaultAddress() throws UserNotFoundException for unknown user")
    void testSetDefaultAddressUnknownUser() {
        assertThrows(UserNotFoundException.class, () ->
                userService.setDefaultAddress("non-existent", "addr-1"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("setDefaultAddress() rejects invalid address ID")
    void testSetDefaultAddressInvalidId(String invalidId) {
        assertThrows(InvalidInputException.class, () ->
                userService.setDefaultAddress("usr-1", invalidId));
    }
}
