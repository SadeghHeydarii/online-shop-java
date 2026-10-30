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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private LocalDateTime testTime;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.of(2026, 10, 30, 12, 0);
        sampleUser = new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", "09121112233",
                "hashed_pwd_secret", UserRole.CUSTOMER, testTime);
    }

    @Test
    @DisplayName("User creation with valid arguments succeeds with initial state")
    void testValidUserCreation() {
        assertEquals("usr-1", sampleUser.getId());
        assertEquals("Ali Rezaei", sampleUser.getFullName());
        assertEquals("ali@aut.ac.ir", sampleUser.getEmail());
        assertEquals("09121112233", sampleUser.getPhone());
        assertEquals(UserRole.CUSTOMER, sampleUser.getRole());
        assertTrue(sampleUser.isActive());
        assertEquals(testTime, sampleUser.getCreatedAt());
        assertTrue(sampleUser.getAddresses().isEmpty());
        assertTrue(sampleUser.getDefaultAddress().isEmpty());
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
    @DisplayName("User constructor rejects invalid email")
    void testInvalidEmail(String invalidEmail) {
        assertThrows(IllegalArgumentException.class, () ->
                new User("usr-1", "Ali Rezaei", invalidEmail, "09121112233",
                        "hash", UserRole.CUSTOMER, testTime));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("User constructor rejects invalid phone")
    void testInvalidPhone(String invalidPhone) {
        assertThrows(IllegalArgumentException.class, () ->
                new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", invalidPhone,
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
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("updateProfile() rejects invalid phone")
    void testUpdateProfileInvalidPhone(String invalidPhone) {
        assertThrows(IllegalArgumentException.class, () ->
                sampleUser.updateProfile("Mohammad Rezaei", invalidPhone));
    }

    @Test
    @DisplayName("changePassword() updates password hash successfully")
    void testChangePasswordSuccess() {
        sampleUser.changePassword("new_hashed_password_123");
        // Verification via no exception thrown and state remains valid
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
    @DisplayName("addAddress() adds addresses and manages default address correctly")
    void testAddAddress() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St No 1", "1234567890", "Home", false);
        sampleUser.addAddress(addr1);

        assertEquals(1, sampleUser.getAddresses().size());
        assertEquals(addr1, sampleUser.getAddresses().get(0));
        assertTrue(sampleUser.getDefaultAddress().isEmpty());

        Address addr2 = new Address("addr-2", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Valiasr St No 2", "1234567891", "Work", true);
        sampleUser.addAddress(addr2);

        assertEquals(2, sampleUser.getAddresses().size());
        Optional<Address> defaultAddr = sampleUser.getDefaultAddress();
        assertTrue(defaultAddr.isPresent());
        assertEquals("addr-2", defaultAddr.get().addressId());

        // Adding another default address should make addr2 non-default
        Address addr3 = new Address("addr-3", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Azadi St No 3", "1234567892", "Other", true);
        sampleUser.addAddress(addr3);

        assertEquals(3, sampleUser.getAddresses().size());
        assertEquals("addr-3", sampleUser.getDefaultAddress().get().addressId());
        // Verify addr2 is no longer default
        Address updatedAddr2 = sampleUser.getAddresses().stream()
                .filter(a -> a.addressId().equals("addr-2"))
                .findFirst().orElseThrow();
        assertFalse(updatedAddr2.isDefault());
    }

    @Test
    @DisplayName("addAddress() with existing ID replaces address")
    void testAddAddressReplacesExistingId() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St No 1", "1234567890", "Home", false);
        sampleUser.addAddress(addr1);

        Address addr1Updated = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St No 10", "1234567890", "Home", true);
        sampleUser.addAddress(addr1Updated);

        assertEquals(1, sampleUser.getAddresses().size());
        assertEquals("Hafez St No 10", sampleUser.getAddresses().get(0).streetAddress());
        assertTrue(sampleUser.getAddresses().get(0).isDefault());
    }

    @Test
    @DisplayName("addAddress() rejects null address")
    void testAddNullAddress() {
        assertThrows(IllegalArgumentException.class, () -> sampleUser.addAddress(null));
    }

    @Test
    @DisplayName("removeAddress() removes existing address")
    void testRemoveAddress() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St No 1", "1234567890", "Home", false);
        Address addr2 = new Address("addr-2", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Valiasr St No 2", "1234567891", "Work", false);
        sampleUser.addAddress(addr1);
        sampleUser.addAddress(addr2);
        assertEquals(2, sampleUser.getAddresses().size());

        sampleUser.removeAddress("addr-1");
        assertEquals(1, sampleUser.getAddresses().size());
        assertEquals("addr-2", sampleUser.getAddresses().get(0).addressId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("removeAddress() rejects invalid address ID")
    void testRemoveAddressInvalidId(String invalidId) {
        assertThrows(IllegalArgumentException.class, () -> sampleUser.removeAddress(invalidId));
    }

    @Test
    @DisplayName("setDefaultAddress() sets target address as default and clears others")
    void testSetDefaultAddressSuccess() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St No 1", "1234567890", "Home", true);
        Address addr2 = new Address("addr-2", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Valiasr St No 2", "1234567891", "Work", false);
        sampleUser.addAddress(addr1);
        sampleUser.addAddress(addr2);

        sampleUser.setDefaultAddress("addr-2");

        Address updated1 = sampleUser.getAddresses().stream()
                .filter(a -> a.addressId().equals("addr-1")).findFirst().orElseThrow();
        Address updated2 = sampleUser.getAddresses().stream()
                .filter(a -> a.addressId().equals("addr-2")).findFirst().orElseThrow();

        assertFalse(updated1.isDefault());
        assertTrue(updated2.isDefault());
        assertEquals("addr-2", sampleUser.getDefaultAddress().get().addressId());
    }

    @Test
    @DisplayName("setDefaultAddress() rejects non-existent address ID")
    void testSetDefaultAddressNonExistent() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St No 1", "1234567890", "Home", false);
        sampleUser.addAddress(addr1);

        assertThrows(IllegalArgumentException.class, () -> sampleUser.setDefaultAddress("non-existent"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("setDefaultAddress() rejects invalid address ID")
    void testSetDefaultAddressInvalidId(String invalidId) {
        assertThrows(IllegalArgumentException.class, () -> sampleUser.setDefaultAddress(invalidId));
    }

    @Test
    @DisplayName("Representation exposure: getAddresses() returns unmodifiable copy")
    void testAddressesImmutability() {
        Address addr1 = new Address("addr-1", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Hafez St No 1", "1234567890", "Home", false);
        sampleUser.addAddress(addr1);

        List<Address> addresses = sampleUser.getAddresses();
        Address addr2 = new Address("addr-2", "Ali Rezaei", "09121112233",
                "Tehran", "Tehran", "Valiasr St No 2", "1234567891", "Work", false);

        assertThrows(UnsupportedOperationException.class, () -> addresses.add(addr2));
    }

    @Test
    @DisplayName("Entity equality is strictly based on identity ID")
    void testEntityIdentityEquality() {
        User userSameId = new User("usr-1", "Different Name", "other@aut.ac.ir", "09998887766",
                "different_hash", UserRole.ADMIN, testTime.plusDays(1));

        User userDifferentId = new User("usr-2", "Ali Rezaei", "ali@aut.ac.ir", "09121112233",
                "hashed_pwd_secret", UserRole.CUSTOMER, testTime);

        assertEquals(sampleUser, userSameId);
        assertEquals(sampleUser.hashCode(), userSameId.hashCode());
        assertNotEquals(sampleUser, userDifferentId);
        assertNotEquals(sampleUser, null);
        assertNotEquals(sampleUser, "not-a-user");
    }

    @Test
    @DisplayName("toString() does not expose raw password hash")
    void testToStringSecurity() {
        String str = sampleUser.toString();
        assertTrue(str.contains("usr-1"));
        assertTrue(str.contains("Ali Rezaei"));
        assertTrue(str.contains("ali@aut.ac.ir"));
        assertFalse(str.contains("hashed_pwd_secret"));
    }
}
