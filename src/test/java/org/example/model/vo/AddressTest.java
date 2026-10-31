package org.example.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Address Value Object Tests")
class AddressTest {

    @Test
    @DisplayName("Should create valid Address when all invariants are satisfied")
    void shouldCreateValidAddress() {
        Address addr = new Address(
                "ADDR-1",
                "Ali Rezaei",
                "09121234567",
                "Tehran",
                "Tehran",
                "Valiasr Ave, No 123",
                "1234567890",
                "Home",
                true);

        assertEquals("ADDR-1", addr.addressId());
        assertEquals("Ali Rezaei", addr.recipientName());
        assertEquals("09121234567", addr.phone());
        assertEquals("Tehran", addr.province());
        assertEquals("Tehran", addr.city());
        assertEquals("Valiasr Ave, No 123", addr.streetAddress());
        assertEquals("1234567890", addr.postalCode());
        assertEquals("Home", addr.title());
        assertTrue(addr.isDefault());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject blank or null addressId")
    void shouldRejectInvalidAddressId(String invalidId) {
        assertThrows(
                RuntimeException.class,
                () -> new Address(
                        invalidId, "Ali", "09121234567", "Tehran", "Tehran", "Street", "1234567890", "Home", false));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Should reject blank or null recipientName")
    void shouldRejectInvalidRecipientName(String invalidName) {
        assertThrows(
                RuntimeException.class,
                () -> new Address(
                        "ADDR-1", invalidName, "09121234567", "Tehran", "Tehran", "Street", "1234567890", "Home", false));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Should reject blank or null streetAddress")
    void shouldRejectInvalidStreetAddress(String invalidStreet) {
        assertThrows(
                RuntimeException.class,
                () -> new Address(
                        "ADDR-1", "Ali", "09121234567", "Tehran", "Tehran", invalidStreet, "1234567890", "Home", false));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345", "123456789", "12345678901", "letters123"})
    @DisplayName("Should reject invalid or non-10-digit postal code")
    void shouldRejectInvalidPostalCode(String invalidPostal) {
        assertThrows(
                RuntimeException.class,
                () -> new Address(
                        "ADDR-1", "Ali", "09121234567", "Tehran", "Tehran", "Street", invalidPostal, "Home", false));
    }

    @Test
    @DisplayName("Should produce a new Address instance with updated isDefault flag")
    void shouldProduceNewAddressWithDefault() {
        Address original = new Address(
                "ADDR-1", "Ali", "09121234567", "Tehran", "Tehran", "Street", "1234567890", "Home", false);

        Address modified = original.withDefault(true);

        assertFalse(original.isDefault(), "Original address must remain unchanged");
        assertTrue(modified.isDefault(), "Modified address must have updated isDefault flag");
        assertEquals(original.addressId(), modified.addressId());
        assertEquals(original.recipientName(), modified.recipientName());
        assertEquals(original.streetAddress(), modified.streetAddress());
    }

    @Test
    @DisplayName("Should generate accurate full address text containing all key details")
    void shouldGenerateAccurateFullAddressText() {
        Address addr = new Address(
                "ADDR-1",
                "Ali Rezaei",
                "09121234567",
                "Tehran",
                "Tehran",
                "Valiasr Ave, No 123",
                "1234567890",
                "Home",
                true);

        String fullText = addr.fullAddressText();

        assertTrue(fullText.contains("Tehran"));
        assertTrue(fullText.contains("Valiasr Ave, No 123"));
        assertTrue(fullText.contains("1234567890"));
        assertTrue(fullText.contains("Ali Rezaei"));
        assertTrue(fullText.contains("09121234567"));
        assertTrue(fullText.contains("Home"));
    }

    @Test
    @DisplayName("Should verify structural equality and hashCode for Address records")
    void shouldVerifyStructuralEquality() {
        Address a1 = new Address(
                "ADDR-1", "Ali", "09121234567", "Tehran", "Tehran", "Street", "1234567890", "Home", false);
        Address a2 = new Address(
                "ADDR-1", "Ali", "09121234567", "Tehran", "Tehran", "Street", "1234567890", "Home", false);
        Address a3 = new Address(
                "ADDR-2", "Ali", "09121234567", "Tehran", "Tehran", "Street", "1234567890", "Home", false);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, a3);
    }
}
