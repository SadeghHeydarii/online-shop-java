package org.example.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.exception.AuthenticationException;
import org.example.exception.DuplicateUserException;
import org.example.exception.InvalidInputException;
import org.example.model.entity.User;
import org.example.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private Map<String, User> userStore;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userStore = new ConcurrentHashMap<>();
        authService = new AuthService(userStore);
    }

    @Test
    @DisplayName("register() successfully creates user with active status and CUSTOMER role")
    void testRegisterSuccess() {
        User user = authService.register("Sara Ahmadi", "sara@aut.ac.ir", "09121113344", "secret123");

        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals("Sara Ahmadi", user.getFullName());
        assertEquals("sara@aut.ac.ir", user.getEmail());
        assertEquals("09121113344", user.getPhone());
        assertEquals(UserRole.CUSTOMER, user.getRole());
        assertTrue(user.isActive());
        assertTrue(userStore.containsKey(user.getId()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("register() rejects invalid full name")
    void testRegisterInvalidFullName(String invalidName) {
        assertThrows(InvalidInputException.class, () ->
                authService.register(invalidName, "user@aut.ac.ir", "09121113344", "secret123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalid-email", "noatsign.com"})
    @DisplayName("register() rejects a missing or invalid sole email identifier")
    void testRegisterInvalidEmail(String invalidEmail) {
        assertThrows(InvalidInputException.class, () ->
                authService.register("Sara Ahmadi", invalidEmail, "", "secret123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("register() rejects a missing or invalid sole mobile identifier")
    void testRegisterInvalidPhone(String invalidPhone) {
        assertThrows(InvalidInputException.class, () ->
                authService.register("Sara Ahmadi", "", invalidPhone, "secret123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345", "abc", "     "})
    @DisplayName("register() rejects passwords shorter than 6 characters")
    void testRegisterShortPassword(String shortPassword) {
        assertThrows(InvalidInputException.class, () ->
                authService.register("Sara Ahmadi", "user@aut.ac.ir", "09121113344", shortPassword));
    }

    @Test
    @DisplayName("register() rejects already registered email address (case-insensitive)")
    void testRegisterDuplicateEmail() {
        authService.register("Sara Ahmadi", "sara@aut.ac.ir", "09121113344", "secret123");

        assertThrows(DuplicateUserException.class, () ->
                authService.register("Sara Second", "SARA@aut.ac.ir", "09129998877", "another123"));
    }

    @Test
    @DisplayName("login() returns valid session token upon matching credentials")
    void testLoginSuccess() {
        authService.register("Sara Ahmadi", "sara@aut.ac.ir", "09121113344", "secret123");

        String sessionToken = authService.login("sara@aut.ac.ir", "secret123");
        assertNotNull(sessionToken);
        assertFalse(sessionToken.isBlank());

        User authenticated = authService.getAuthenticatedUser(sessionToken);
        assertNotNull(authenticated);
        assertEquals("sara@aut.ac.ir", authenticated.getEmail());
    }

    @Test
    @DisplayName("login() accepts email with case insensitivity")
    void testLoginCaseInsensitiveEmail() {
        authService.register("Sara Ahmadi", "sara@aut.ac.ir", "09121113344", "secret123");

        String sessionToken = authService.login("SARA@AUT.AC.IR", "secret123");
        assertNotNull(sessionToken);
        assertEquals("sara@aut.ac.ir", authService.getAuthenticatedUser(sessionToken).getEmail());
    }

    @Test
    @DisplayName("login() throws AuthenticationException on incorrect password")
    void testLoginWrongPassword() {
        authService.register("Sara Ahmadi", "sara@aut.ac.ir", "09121113344", "secret123");

        assertThrows(AuthenticationException.class, () ->
                authService.login("sara@aut.ac.ir", "wrongpassword"));
    }

    @Test
    @DisplayName("login() throws AuthenticationException on non-existent email")
    void testLoginNonExistentEmail() {
        assertThrows(AuthenticationException.class, () ->
                authService.login("unknown@aut.ac.ir", "secret123"));
    }

    @Test
    @DisplayName("login() throws IllegalArgumentException on null email or password")
    void testLoginNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> authService.login(null, "secret123"));
        assertThrows(IllegalArgumentException.class, () -> authService.login("sara@aut.ac.ir", null));
    }

    @Test
    @DisplayName("login() throws AuthenticationException when user account is deactivated")
    void testLoginDeactivatedAccount() {
        User user = authService.register("Sara Ahmadi", "sara@aut.ac.ir", "09121113344", "secret123");
        user.deactivate();

        assertThrows(AuthenticationException.class, () ->
                authService.login("sara@aut.ac.ir", "secret123"));
    }

    @Test
    @DisplayName("logout() terminates active session")
    void testLogoutSuccess() {
        authService.register("Sara Ahmadi", "sara@aut.ac.ir", "09121113344", "secret123");
        String token = authService.login("sara@aut.ac.ir", "secret123");

        assertNotNull(authService.getAuthenticatedUser(token));

        authService.logout(token);
        assertNull(authService.getAuthenticatedUser(token));
    }

    @Test
    @DisplayName("logout() handles null gracefully")
    void testLogoutNull() {
        authService.logout(null); // Should not throw
    }

    @Test
    @DisplayName("getAuthenticatedUser() returns null for unknown session token")
    void testGetAuthenticatedUserUnknownToken() {
        assertNull(authService.getAuthenticatedUser("invalid-token-123"));
        assertNull(authService.getAuthenticatedUser(null));
    }
}
