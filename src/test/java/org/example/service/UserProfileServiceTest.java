package org.example.service;

import java.time.LocalDateTime;
import org.example.exception.DuplicateUserException;
import org.example.exception.InvalidInputException;
import org.example.exception.UserNotFoundException;
import org.example.infrastructure.persistence.InMemoryUserRepository;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.service.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileServiceTest {

    private UserRepository userRepository;
    private UserProfileService service;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        userRepository.save(new User("usr-1", "Ali Rezaei", "ali@aut.ac.ir", "09121112233",
                "hash1", UserRole.CUSTOMER, LocalDateTime.of(2026, 11, 25, 10, 0)));
        userRepository.save(new User("usr-2", "Sara Rahimi", "sara@aut.ac.ir", "09124445566",
                "hash2", UserRole.CUSTOMER, LocalDateTime.of(2026, 11, 25, 10, 0)));
        service = new UserProfileService(userRepository);
    }

    @Test
    void looksUpAndUpdatesProfileWhileEnforcingPhoneUniqueness() {
        assertEquals("Ali Rezaei", service.findUserById("usr-1").getFullName());

        service.updateProfile("usr-1", "Ali H. Rezaei", "09127778899");
        assertEquals("Ali H. Rezaei", service.getProfile("usr-1").getFullName());
        assertEquals("09127778899", service.getProfile("usr-1").getPhone());

        assertThrows(DuplicateUserException.class,
                () -> service.updateProfile("usr-1", "Ali", "09124445566"));
        assertThrows(UserNotFoundException.class, () -> service.getProfile("missing"));
    }

    @Test
    void managesOwnedAddressesThroughAddressBookUseCase() {
        Address home = new Address("addr-1", "Ali", "09121112233", "Tehran", "Tehran",
                "Hafez St", "1234567890", "Home", true);
        Address work = new Address("addr-2", "Ali", "09121112233", "Tehran", "Tehran",
                "Valiasr", "9876543210", "Work", false);

        service.addAddress("usr-1", home);
        service.addAddress("usr-1", work);
        service.setDefaultAddress("usr-1", "addr-2");

        assertEquals("addr-2", service.getProfile("usr-1").getDefaultAddress().orElseThrow().addressId());

        Address updatedWork = new Address("addr-2", "Ali", "09121112233", "Tehran", "Tehran",
                "Valiasr Updated", "9876543210", "Work", true);
        service.updateAddress("usr-1", updatedWork);
        assertEquals("Valiasr Updated",
                service.getProfile("usr-1").getDefaultAddress().orElseThrow().streetAddress());

        assertThrows(InvalidInputException.class,
                () -> service.updateAddress("usr-1", new Address("foreign", "Ali", "09121112233",
                        "Tehran", "Tehran", "St", "1234567890", "Other", false)));

        service.removeAddress("usr-1", "addr-1");
        assertEquals(1, service.getProfile("usr-1").getAddresses().size());
    }
}
