package org.example.model.vo;

import java.util.Objects;

/**
 * Immutable value object representing a customer delivery address.
 *
 * <p>Abstraction Function:
 * AF(a) = a physical delivery destination identified locally by a.addressId,
 *         located at a.streetAddress, a.city, a.province (postal code: a.postalCode),
 *         titled a.title, intended for recipient a.recipientName (phone: a.phone),
 *         with default shipping status a.isDefault.
 *
 * <p>Representation Invariant:
 * RI: addressId != null && !addressId.isBlank()
 *     && recipientName != null && !recipientName.isBlank()
 *     && phone != null && !phone.isBlank()
 *     && province != null && !province.isBlank()
 *     && city != null && !city.isBlank()
 *     && streetAddress != null && !streetAddress.isBlank()
 *     && postalCode != null && !postalCode.isBlank()
 *     && title != null && !title.isBlank().
 *
 * @param addressId     The local address identifier (e.g., "ADDR-1").
 * @param recipientName The recipient's full name.
 * @param phone         The recipient's contact phone number.
 * @param province      The destination province.
 * @param city          The destination city.
 * @param streetAddress The detailed street and building address.
 * @param postalCode    The postal code.
 * @param title         The address title (e.g., "Home", "Work").
 * @param isDefault     Flag indicating whether this is the default address.
 */
public record Address(
        String addressId,
        String recipientName,
        String phone,
        String province,
        String city,
        String streetAddress,
        String postalCode,
        String title,
        boolean isDefault) {

    /**
     * Compact constructor enforcing non-null and non-blank invariants on all string fields.
     */
    public Address {
        Objects.requireNonNull(addressId, "addressId cannot be null");
        Objects.requireNonNull(recipientName, "recipientName cannot be null");
        Objects.requireNonNull(phone, "phone cannot be null");
        Objects.requireNonNull(province, "province cannot be null");
        Objects.requireNonNull(city, "city cannot be null");
        Objects.requireNonNull(streetAddress, "streetAddress cannot be null");
        Objects.requireNonNull(postalCode, "postalCode cannot be null");
        Objects.requireNonNull(title, "title cannot be null");

        if (addressId.isBlank()) {
            throw new IllegalArgumentException("addressId cannot be blank");
        }
        if (recipientName.isBlank()) {
            throw new IllegalArgumentException("recipientName cannot be blank");
        }
        if (phone.isBlank()) {
            throw new IllegalArgumentException("phone cannot be blank");
        }
        if (province.isBlank()) {
            throw new IllegalArgumentException("province cannot be blank");
        }
        if (city.isBlank()) {
            throw new IllegalArgumentException("city cannot be blank");
        }
        if (streetAddress.isBlank()) {
            throw new IllegalArgumentException("streetAddress cannot be blank");
        }
        if (postalCode.isBlank()) {
            throw new IllegalArgumentException("postalCode cannot be blank");
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("title cannot be blank");
        }
    }

    /**
     * Produces a new Address instance with an updated isDefault flag.
     *
     * <p>REQUIRES: none.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns a new Address instance identical to this, but with the specified
     *              default status.
     *
     * @param isDefault True if the new instance should be marked default; false otherwise.
     * @return A new Address instance with the updated default flag.
     */
    public Address withDefault(boolean isDefault) {
        return new Address(
                this.addressId,
                this.recipientName,
                this.phone,
                this.province,
                this.city,
                this.streetAddress,
                this.postalCode,
                this.title,
                isDefault);
    }

    /**
     * Formats the complete physical address as a single descriptive text string for invoices.
     *
     * <p>REQUIRES: none.
     * <br>MODIFIES: nothing.
     * <br>EFFECTS: Returns a formatted representation of the recipient and postal address.
     *
     * @return The formatted address string.
     */
    public String fullAddressText() {
        return province + ", " + city + ", " + streetAddress
                + " (" + title + ") [Postal Code: " + postalCode
                + "] - Recipient: " + recipientName + " (" + phone + ")";
    }
}
