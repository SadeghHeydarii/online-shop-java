package org.example.model.vo;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Immutable value object representing a user's contact information (email address and/or mobile telephone number).
 *
 * <p>Enforces format validation for RFC-compliant email addresses and international/local mobile telephone numbers.
 * Supports accounts identified by email, mobile phone, or both.
 *
 * <p><b>Abstraction Function (AF):</b>
 * AF(email, phone) = The contact and identification details of an account reachable by electronic mail at
 * {@code email} (normalized to lowercase) and/or telephone at {@code phone}.
 *
 * <p><b>Representation Invariant (RI):</b>
 * <ul>
 *   <li>{@code email != null} and {@code phone != null}</li>
 *   <li>At least one of {@code email} or {@code phone} is non-empty</li>
 *   <li>If {@code email} is non-empty, it matches the standard email format and length &lt;= 254</li>
 *   <li>If {@code phone} is non-empty, it matches 7 to 15 numeric digits with an optional leading '+'</li>
 * </ul>
 *
 * <p><b>Safety from Representation Exposure:</b>
 * All fields are immutable {@link String} components of a Java record, ensuring complete immutability.
 */
public record ContactInfo(String email, String phone) {

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");
    private static final Pattern PHONE = Pattern.compile("\\+?[0-9]{7,15}");

    /**
     * Compact constructor validating and normalizing email and telephone number representations.
     *
     * @param email customer email address (optional if phone is provided)
     * @param phone contact telephone number (optional if email is provided)
     * @throws IllegalArgumentException if both email and phone are empty, or if either format is invalid
     */
    public ContactInfo {
        email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        phone = phone == null ? "" : phone.trim();
        if (email.isEmpty() && phone.isEmpty()) {
            throw new IllegalArgumentException("An email address or mobile number is required");
        }
        if (!email.isEmpty()) {
            String local = email.substring(0, Math.max(0, email.indexOf('@')));
            if (email.length() > 254 || !EMAIL.matcher(email).matches()
                    || local.startsWith(".") || local.endsWith(".") || local.contains("..")) {
                throw new IllegalArgumentException("Invalid email address");
            }
        }
        if (!phone.isEmpty() && !PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("Phone number must contain 7 to 15 digits, with an optional leading +");
        }
    }

    /**
     * Determines whether the specified identifier matches either the email or the mobile phone number.
     *
     * @param identifier the input email address or telephone number to match
     * @return {@code true} if matching; {@code false} otherwise
     */
    public boolean matches(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }
        String value = identifier.trim();
        return (!email.isEmpty() && email.equalsIgnoreCase(value))
                || (!phone.isEmpty() && phone.equals(value));
    }
}
