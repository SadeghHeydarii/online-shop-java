package org.example.model.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;

/**
 * Represents a user account entity in the online shop system.
 *
 * <p>A user possesses a unique identity ({@code id}), credentials, profile attributes,
 * an assigned system role, an activation status flag, account creation timestamp,
 * and an encapsulated list of associated physical delivery addresses.
 *
 * <p><b>Abstraction Function (AF):</b>
 * AF(id, fullName, email, phone, passwordHash, role, active, createdAt, addresses) =
 * A registered customer, administrator, or storekeeper in the system identified by {@code id},
 * reachable at {@code email} and {@code phone}, with security credentials {@code passwordHash},
 * having role {@code role}, active state {@code active}, registered at {@code createdAt},
 * and maintaining delivery addresses {@code addresses}.
 *
 * <p><b>Representation Invariant (RI):</b>
 * <ul>
 *   <li>{@code id != null && !id.isBlank()}</li>
 *   <li>{@code fullName != null && !fullName.isBlank()}</li>
 *   <li>{@code email != null && !email.isBlank()}</li>
 *   <li>{@code phone != null && !phone.isBlank()}</li>
 *   <li>{@code passwordHash != null && !passwordHash.isBlank()}</li>
 *   <li>{@code role != null}</li>
 *   <li>{@code createdAt != null}</li>
 *   <li>{@code addresses != null} and contains no null elements</li>
 *   <li>At most one address in {@code addresses} satisfies {@code isDefault() == true}</li>
 * </ul>
 *
 * <p><b>Safety from Representation Exposure:</b>
 * All fields are private. Mutable address collections are protected by returning
 * an unmodifiable copy via {@link List#copyOf(java.util.Collection)} in {@link #getAddresses()}.
 */
public class User {

    private final String id;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private UserRole role;
    private boolean active;
    private final LocalDateTime createdAt;
    private final List<Address> addresses;

    /**
     * Constructs a new {@code User} entity with active status and an empty address list.
     *
     * @param id           the unique immutable identifier of the user
     * @param fullName     the full name of the user
     * @param email        the unique email address
     * @param phone        the telephone contact number
     * @param passwordHash the secure cryptographic hash of the password
     * @param role         the user role (CUSTOMER, ADMIN, STOREKEEPER)
     * @param createdAt    the account registration timestamp
     * @throws IllegalArgumentException if any string argument is null/blank, or role/createdAt is null
     */
    public User(String id, String fullName, String email, String phone,
                String passwordHash, UserRole role, LocalDateTime createdAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("User role cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Creation timestamp cannot be null");
        }

        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
        this.createdAt = createdAt;
        this.addresses = new ArrayList<>();
        checkRep();
    }

    /**
     * Asserts the representation invariant of the User entity.
     */
    private void checkRep() {
        assert id != null && !id.isBlank() : "User id cannot be null or blank";
        assert fullName != null && !fullName.isBlank() : "User fullName cannot be null or blank";
        assert email != null && !email.isBlank() : "User email cannot be null or blank";
        assert phone != null && !phone.isBlank() : "User phone cannot be null or blank";
        assert passwordHash != null && !passwordHash.isBlank() : "User passwordHash cannot be null or blank";
        assert role != null : "User role cannot be null";
        assert createdAt != null : "User createdAt cannot be null";
        assert addresses != null : "User addresses list cannot be null";
        int defaultCount = 0;
        for (Address addr : addresses) {
            assert addr != null : "Address in list cannot be null";
            if (addr.isDefault()) {
                defaultCount++;
            }
        }
        assert defaultCount <= 1 : "At most one default address allowed";
    }

    /**
     * Returns the unique immutable identifier of this user.
     *
     * @return user ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the full name of this user.
     *
     * @return user full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Returns the registered email address of this user.
     *
     * @return email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the telephone contact number of this user.
     *
     * @return phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Returns the assigned role of this user.
     *
     * @return user role
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Checks whether this user account is active.
     *
     * @return {@code true} if active; {@code false} if deactivated
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the timestamp when this user account was created.
     *
     * @return account creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Activates this user account.
     */
    public void activate() {
        this.active = true;
        checkRep();
    }

    /**
     * Deactivates this user account.
     */
    public void deactivate() {
        this.active = false;
        checkRep();
    }

    /**
     * Updates profile personal details (full name and phone number).
     *
     * @param newFullName the new full name (non-blank)
     * @param newPhone    the new phone number (non-blank)
     * @throws IllegalArgumentException if either parameter is null or blank
     */
    public void updateProfile(String newFullName, String newPhone) {
        if (newFullName == null || newFullName.isBlank()) {
            throw new IllegalArgumentException("New full name cannot be null or blank");
        }
        if (newPhone == null || newPhone.isBlank()) {
            throw new IllegalArgumentException("New phone cannot be null or blank");
        }
        this.fullName = newFullName;
        this.phone = newPhone;
        checkRep();
    }

    /**
     * Updates the password hash of this user.
     *
     * @param newPasswordHash the new password hash (non-blank)
     * @throws IllegalArgumentException if the parameter is null or blank
     */
    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("New password hash cannot be null or blank");
        }
        this.passwordHash = newPasswordHash;
        checkRep();
    }

    /**
     * Adds an address to the user's registered delivery addresses.
     *
     * <p>If the added address is marked as default, all other addresses currently
     * in the user's list are updated to be non-default. If an address with the
     * same {@code addressId} already exists, it is replaced.
     *
     * @param address the address to add
     * @throws IllegalArgumentException if address is null
     */
    public void addAddress(Address address) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }

        if (address.isDefault()) {
            for (int i = 0; i < addresses.size(); i++) {
                Address existing = addresses.get(i);
                if (existing.isDefault()) {
                    addresses.set(i, existing.withDefault(false));
                }
            }
        }

        int existingIndex = -1;
        for (int i = 0; i < addresses.size(); i++) {
            if (addresses.get(i).addressId().equals(address.addressId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            addresses.set(existingIndex, address);
        } else {
            addresses.add(address);
        }
        checkRep();
    }

    /**
     * Removes an address by its identifier.
     *
     * @param addressId the identifier of the address to remove
     * @throws IllegalArgumentException if addressId is null or blank
     */
    public void removeAddress(String addressId) {
        if (addressId == null || addressId.isBlank()) {
            throw new IllegalArgumentException("Address ID cannot be null or blank");
        }
        addresses.removeIf(a -> a.addressId().equals(addressId));
        checkRep();
    }

    /**
     * Sets the specified address as the default delivery address.
     *
     * <p>The address matching {@code addressId} is set to default, and all other
     * addresses in the list are set to non-default.
     *
     * @param addressId the identifier of the address to set as default
     * @throws IllegalArgumentException if addressId is null/blank or not found in addresses
     */
    public void setDefaultAddress(String addressId) {
        if (addressId == null || addressId.isBlank()) {
            throw new IllegalArgumentException("Address ID cannot be null or blank");
        }

        boolean found = false;
        for (Address addr : addresses) {
            if (addr.addressId().equals(addressId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Address with ID " + addressId + " not found in user addresses");
        }

        for (int i = 0; i < addresses.size(); i++) {
            Address existing = addresses.get(i);
            if (existing.addressId().equals(addressId)) {
                addresses.set(i, existing.withDefault(true));
            } else if (existing.isDefault()) {
                addresses.set(i, existing.withDefault(false));
            }
        }
        checkRep();
    }

    /**
     * Returns the user's default delivery address, if one is designated.
     *
     * @return an {@link Optional} containing the default address, or empty if none is designated
     */
    public Optional<Address> getDefaultAddress() {
        return addresses.stream()
                .filter(Address::isDefault)
                .findFirst();
    }

    /**
     * Returns an unmodifiable snapshot of the registered delivery addresses.
     *
     * @return unmodifiable list of delivery addresses
     */
    public List<Address> getAddresses() {
        return List.copyOf(this.addresses);
    }

    /**
     * Compares this user to another object for equality based on entity identity ({@code id}).
     *
     * @param o the object to compare with
     * @return {@code true} if both are User entities with identical IDs; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    /**
     * Returns the hash code based on entity identity ({@code id}).
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User[id=" + id
                + ", fullName=" + fullName
                + ", email=" + email
                + ", phone=" + phone
                + ", role=" + role
                + ", active=" + active
                + ", createdAt=" + createdAt
                + ", addressesCount=" + addresses.size()
                + "]";
    }
}
