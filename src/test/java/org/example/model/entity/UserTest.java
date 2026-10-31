package org.example.model.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("User Entity Invariant and Encapsulation Tests")
class UserTest {

    private User sampleUser;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.of(2026, 10, 30, 10, 0);
        sampleUser = new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", "09121112233",
                "hashed_secret_password", UserRole.CUSTOMER, testTime);
    }

    @Test
    @DisplayName("Constructor sets valid initial state")
    void testConstructorValid() {
        assertEquals("usr-1", sampleUser.getId());
        assertEquals("Ali Rezaei", sampleUser.getFullName());
        assertEquals("ali@aut.ac.ir", sampleUser.getEmail());
        assertEquals("09121112233", sampleUser.getPhone());
        assertEquals(UserRole.CUSTOMER, sampleUser.getRole());
        assertTrue(sampleUser.isActive());
        assertEquals(testTime, sampleUser.getCreatedAt());
        assertTrue(sampleUser.getAddresses().isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("User constructor rejects invalid ID")
    void testInvalidId(String invalidId) {
        assertThrows(IllegalArgumentException.class, () ->
                new User(invalidId, "Ali Rezaei", "ali@aut.ac.ir", "09121112233",
                        "hash", UserRole.CUSTOMER, testTime));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("User constructor rejects invalid full name")
    void testInvalidFullName(String invalidName) {
        assertThrows(IllegalArgumentException.class, () ->
                new User("usr-1", invalidName, "ali@aut.ac.ir", "09121112233",
                        "hash", UserRole.CUSTOMER, testTime));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("User constructor rejects invalid email when no phone is provided")
    void testInvalidEmail(String invalidEmail) {
        assertThrows(IllegalArgumentException.class, () ->
                new User("usr-1", "Ali Rezaei", invalidEmail, "",
                        "hash", UserRole.CUSTOMER, testTime));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("User constructor rejects invalid phone when no email is provided")
    void testInvalidPhone(String invalidPhone) {
        assertThrows(IllegalArgumentException.class, () ->
                new User("usr-1", "Ali Rezaei", "", invalidPhone,
                        "hash", UserRole.CUSTOMER, testTime));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("User constructor rejects invalid password hash")
    void testInvalidPasswordHash(String invalidHash) {
        assertThrows(IllegalArgumentException.class, () ->
                new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", "09121112233",
                        invalidHash, UserRole.CUSTOMER, testTime));
    }

    @Test
    @DisplayName("User constructor rejects null role")
    void testNullRole() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", "09121112233",
                        "hash", null, testTime));
    }

    @Test
    @DisplayName("User constructor rejects null creation timestamp")
    void testNullCreatedAt() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", "09121112233",
                        "hash", UserRole.CUSTOMER, null));
    }

    @Test
    @DisplayName("activate() and deactivate() toggle active status")
    void testActivateDeactivate() {
        assertTrue(sampleUser.isActive());
        sampleUser.deactivate();
        assertFalse(sampleUser.isActive());
        sampleUser.activate();
        assertTrue(sampleUser.isActive());
    }

    @Test
    @DisplayName("updateProfile() updates full name and phone number")
    void testUpdateProfileSuccess() {
        sampleUser.updateProfile("Mohammad Rezaei", "09129998877");
        assertEquals("Mohammad Rezaei", sampleUser.getFullName());
        assertEquals("09129998877", sampleUser.getPhone());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("updateProfile() rejects invalid full name")
    void testUpdateProfileInvalidName(String invalidName) {
        assertThrows(IllegalArgumentException.class, () ->
                sampleUser.updateProfile(invalidName, "09129998877"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "letters", "0912x112233"})
    @DisplayName("updateProfile() rejects malformed phone")
    void testUpdateProfileInvalidPhone(String invalidPhone) {
        assertThrows(IllegalArgumentException.class, () ->
                sampleUser.updateProfile("Mohammad Rezaei", invalidPhone));
    }

    @Test
    @DisplayName("changePassword() updates password hash successfully")
    void testChangePasswordSuccess() {
        sampleUser.changePassword("new_hashed_password_123");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("changePassword() rejects invalid password hash")
    void testChangePasswordInvalid(String invalidHash) {
        assertThrows(IllegalArgumentException.class, () ->
                sampleUser.changePassword(invalidHash));
    }

    @Test
    @DisplayName("addAddress() adds addresses and maintains default address invariant")
    void testAddAddressAndDefaultInvariant() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Valiasr St No 1", "1234567890", "Home", false);
        sampleUser.addAddress(addr1);
        assertEquals(1, sampleUser.getAddresses().size());
        assertFalse(sampleUser.getDefaultAddress().isPresent());

        // Adding a default address
        Address addr2 = new Address("addr-2", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Enghelab St No 2", "1234567891", "Work", true);
        sampleUser.addAddress(addr2);

        assertEquals(2, sampleUser.getAddresses().size());
        Optional<Address> defaultAddr = sampleUser.getDefaultAddress();
        assertTrue(defaultAddr.isPresent());
        assertEquals("addr-2", defaultAddr.get().addressId());

        Address addr3 = new Address("addr-3", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Azadi St No 3", "1234567892", "Other", true);
        sampleUser.addAddress(addr3);

        assertEquals(3, sampleUser.getAddresses().size());
        assertEquals("addr-3", sampleUser.getDefaultAddress().get().addressId());

        Address updatedAddr2 = sampleUser.getAddresses().stream()
                .filter(a -> a.addressId().equals("addr-2"))
                .findFirst().orElseThrow();
        assertFalse(updatedAddr2.isDefault(), "Earlier default address must become non-default");
    }

    @Test
    @DisplayName("addAddress() updates existing address by ID")
    void testAddAddressReplacesExisting() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Old Street", "1234567890", "Home", false);
        sampleUser.addAddress(addr1);

        Address addr1Updated = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "New Street", "1234567890", "Home", false);
        sampleUser.addAddress(addr1Updated);

        assertEquals(1, sampleUser.getAddresses().size());
        assertEquals("New Street", sampleUser.getAddresses().get(0).streetAddress());
    }

    @Test
    @DisplayName("addAddress() rejects null address")
    void testAddAddressNull() {
        assertThrows(IllegalArgumentException.class, () -> sampleUser.addAddress(null));
    }

    @Test
    @DisplayName("removeAddress() removes specified address")
    void testRemoveAddress() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Valiasr St No 1", "1234567890", "Home", true);
        Address addr2 = new Address("addr-2", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Enghelab St No 2", "1234567891", "Work", false);
        sampleUser.addAddress(addr1);
        sampleUser.addAddress(addr2);

        sampleUser.removeAddress("addr-1");
        assertEquals(1, sampleUser.getAddresses().size());
        assertEquals("addr-2", sampleUser.getAddresses().get(0).addressId());
        assertFalse(sampleUser.getDefaultAddress().isPresent());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("removeAddress() rejects invalid address ID")
    void testRemoveAddressInvalid(String invalidId) {
        assertThrows(IllegalArgumentException.class, () -> sampleUser.removeAddress(invalidId));
    }

    @Test
    @DisplayName("setDefaultAddress() selects target address and unsets previous default")
    void testSetDefaultAddress() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Valiasr St No 1", "1234567890", "Home", true);
        Address addr2 = new Address("addr-2", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Enghelab St No 2", "1234567891", "Work", false);
        sampleUser.addAddress(addr1);
        sampleUser.addAddress(addr2);

        sampleUser.setDefaultAddress("addr-2");
        assertEquals("addr-2", sampleUser.getDefaultAddress().get().addressId());

        Address updatedAddr1 = sampleUser.getAddresses().stream()
                .filter(a -> a.addressId().equals("addr-1"))
                .findFirst().orElseThrow();
        assertFalse(updatedAddr1.isDefault());
    }

    @Test
    @DisplayName("setDefaultAddress() throws for non-existent address ID")
    void testSetDefaultAddressNotFound() {
        assertThrows(IllegalArgumentException.class, () -> sampleUser.setDefaultAddress("non-existent"));
    }

    @Test
    @DisplayName("getAddresses() returns unmodifiable copy protecting internal rep")
    void testGetAddressesRepExposure() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Valiasr St No 1", "1234567890", "Home", true);
        sampleUser.addAddress(addr1);

        List<Address> addresses = sampleUser.getAddresses();
        Address addr2 = new Address("addr-2", "Ali", "09121112233", "Tehran", "Tehran", "Street", "1234567890", "Work", false);
        assertThrows(UnsupportedOperationException.class, () ->
                addresses.add(addr2));
    }

    @Test
    @DisplayName("equals and hashCode based on entity identity (id)")
    void testEqualsAndHashCode() {
        User sameIdUser = new User("usr-1", "Different Name", "other@aut.ac.ir", "09120000000",
                "different_hash", UserRole.ADMIN, testTime.plusDays(1));
        User diffIdUser = new User("usr-2", "Ali Rezaei", "ali@aut.ac.ir", "09121112233",
                "hashed_secret_password", UserRole.CUSTOMER, testTime);

        assertEquals(sampleUser, sampleUser);
        assertEquals(sampleUser, sameIdUser);
        assertEquals(sampleUser.hashCode(), sameIdUser.hashCode());

        assertNotEquals(sampleUser, diffIdUser);
        assertNotEquals(sampleUser, null);
        assertNotEquals(sampleUser, "not-a-user");
    }

    @Test
    @DisplayName("toString contains relevant entity details")
    void testToString() {
        String str = sampleUser.toString();
        assertNotNull(str);
        assertTrue(str.contains("usr-1"));
        assertTrue(str.contains("Ali Rezaei"));
        assertTrue(str.contains("CUSTOMER"));
    }
}
