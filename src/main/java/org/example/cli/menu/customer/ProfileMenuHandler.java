package org.example.cli.menu.customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.exception.ShopException;
import org.example.model.entity.User;
import org.example.model.vo.Address;
import org.example.service.api.AddressBookUseCase;
import org.example.service.api.UserProfileUseCase;
import org.example.service.port.IdGenerator;

/**
 * Customer CLI handler dedicated to profile updates and delivery address management (SRP, ISP, DIP).
 */
public class ProfileMenuHandler {

    private final UserProfileUseCase profileUseCase;
    private final AddressBookUseCase addressBookUseCase;
    private final IdGenerator idGenerator;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs a {@code ProfileMenuHandler}.
     *
     * @param profileUseCase     profile read/update port
     * @param addressBookUseCase address management port
     * @param idGenerator        opaque address identifier generator
     * @param in                 narrow console input port
     * @param out                narrow console output port
     */
    public ProfileMenuHandler(UserProfileUseCase profileUseCase,
                              AddressBookUseCase addressBookUseCase,
                              IdGenerator idGenerator,
                              InputReader in,
                              OutputWriter out) {
        this.profileUseCase = Objects.requireNonNull(profileUseCase, "profileUseCase cannot be null");
        this.addressBookUseCase = Objects.requireNonNull(addressBookUseCase, "addressBookUseCase cannot be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    /**
     * Loads the latest persisted profile state for the given customer.
     *
     * @param userId non-blank user identifier
     * @return current {@link User} snapshot
     */
    public User loadProfile(String userId) {
        return profileUseCase.getProfile(userId);
    }

    public void handleProfileManagement(User customer) {
        Objects.requireNonNull(customer, "customer cannot be null");
        while (true) {
            User current = loadProfile(customer.getId());
            out.printHeader("My Profile & Address Book");
            out.println("Full Name:    " + current.getFullName());
            out.println("Email:        " + current.getEmail());
            out.println("Phone:        " + current.getPhone());
            out.println("Role:         " + current.getRole());
            out.println("Created At:   " + current.getCreatedAt().toLocalDate());

            Address def = current.getDefaultAddress().orElse(null);
            out.println("Default Addr: " + (def != null ? def.title() + " -> " + def.fullAddressText() : "None selected"));

            out.println();
            out.println("Profile Actions:");
            out.println("  1. Edit Personal Profile (Name & Phone)");
            out.println("  2. Add Delivery Address");
            out.println("  3. Remove Delivery Address");
            out.println("  4. Set Default Delivery Address");
            out.println("  5. Edit Delivery Address");
            out.println("  0. Back to Main Menu");

            int action = in.readInt("Choose profile action", 0, 5);
            switch (action) {
                case 1 -> promptEditProfile(customer.getId(), current);
                case 2 -> promptAddAddress(customer.getId());
                case 3 -> promptRemoveAddress(customer.getId(), current.getAddresses());
                case 4 -> promptSetDefaultAddress(customer.getId(), current.getAddresses());
                case 5 -> promptEditAddress(customer.getId());
                case 0 -> { return; }
                default -> out.printError("Invalid action.");
            }
        }
    }

    void promptAddAddress(String userId) {
        out.println();
        out.printHeader("Add New Delivery Address");
        String recipient = in.readString("Recipient Name");
        String phone = in.readString("Recipient Phone");
        String province = in.readString("Province");
        String city = in.readString("City");
        String street = in.readString("Street Address");
        String postalCode = in.readString("Postal Code (10 digits)");
        String title = in.readString("Address Label/Title (e.g. Home, Office)");
        boolean isDefault = in.readBoolean("Set this as default delivery address?");

        String addressId = idGenerator.nextId("addr");
        try {
            Address address = new Address(addressId, recipient, phone, province, city, street, postalCode, title, isDefault);
            addressBookUseCase.addAddress(userId, address);
            out.printSuccess("Address '" + title + "' added successfully.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Failed to add address: " + e.getMessage());
        }
    }

    private void promptEditProfile(String userId, User current) {
        String newName = in.readString("New Full Name");
        String newPhone = readOrKeep("New Phone Number", current.getPhone());
        try {
            profileUseCase.updateProfile(userId, newName, newPhone);
            out.printSuccess("Profile updated successfully.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Profile update failed: " + e.getMessage());
        }
    }

    private void promptRemoveAddress(String userId, List<Address> addrs) {
        if (addrs.isEmpty()) {
            out.printInfo("Address book is empty.");
            return;
        }
        displayAddresses(addrs);
        String aid = in.readString("Enter Address ID to remove");
        try {
            addressBookUseCase.removeAddress(userId, aid);
            out.printSuccess("Address removed.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Failed to remove address: " + e.getMessage());
        }
    }

    private void promptSetDefaultAddress(String userId, List<Address> addrs) {
        if (addrs.isEmpty()) {
            out.printInfo("Address book is empty.");
            return;
        }
        displayAddresses(addrs);
        String aid = in.readString("Enter Address ID to set as default");
        try {
            addressBookUseCase.setDefaultAddress(userId, aid);
            out.printSuccess("Default address updated.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Failed to set default address: " + e.getMessage());
        }
    }

    private void promptEditAddress(String userId) {
        List<Address> addresses = loadProfile(userId).getAddresses();
        if (addresses.isEmpty()) {
            out.printInfo("Address book is empty.");
            return;
        }
        displayAddresses(addresses);
        String addressId = in.readString("Address ID to edit");
        Address current = addresses.stream()
                .filter(address -> address.addressId().equals(addressId))
                .findFirst()
                .orElse(null);
        if (current == null) {
            out.printError("Address does not belong to this user");
            return;
        }
        String recipient = readOrKeep("Recipient", current.recipientName());
        String phone = readOrKeep("Phone", current.phone());
        String province = readOrKeep("Province", current.province());
        String city = readOrKeep("City", current.city());
        String street = readOrKeep("Street", current.streetAddress());
        String postalCode = readOrKeep("Postal Code", current.postalCode());
        String title = readOrKeep("Title", current.title());
        boolean isDefault = in.readBoolean("Set as default address?");
        try {
            addressBookUseCase.updateAddress(
                    userId,
                    new Address(current.addressId(), recipient, phone, province, city, street, postalCode, title, isDefault));
            out.printSuccess("Address updated successfully.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Address update failed: " + e.getMessage());
        }
    }

    private void displayAddresses(List<Address> addresses) {
        List<String> headers = List.of("Address ID", "Title", "Recipient", "Details", "Default");
        List<List<String>> rows = new ArrayList<>();
        for (Address a : addresses) {
            rows.add(List.of(
                    a.addressId(),
                    a.title(),
                    a.recipientName(),
                    a.fullAddressText(),
                    a.isDefault() ? "YES" : "NO"
            ));
        }
        out.printTable(headers, rows);
    }

    private String readOrKeep(String label, String current) {
        String value = in.readLine(label + " (blank keeps '" + current + "')");
        return value.isBlank() ? current : value;
    }
}
