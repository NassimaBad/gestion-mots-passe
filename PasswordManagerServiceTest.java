package com.passwordmanager.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.passwordmanager.common.PasswordEntry;
import com.passwordmanager.common.PasswordManagerService;

public class PasswordManagerServiceTest {

    private PasswordManagerService service;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpass";

    // This method will run once before all tests in this class.
    // It ensures the RMI server is started for integration tests.
    @BeforeEach
    public void setUp() throws Exception {
        // Connect to the RMI registry where the server is expected to be running.
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        service = (PasswordManagerService) registry.lookup("PasswordManagerService");
        assertNotNull(service, "RMI service should not be null");

        // Clean up any existing test user before each test
        try {
            service.deletePassword(TEST_USERNAME, "test.com"); // Clean up a potential lingering entry
        } catch (Exception ignored) {
            // Ignore if password doesn't exist, as it's a cleanup step
        }
        try {
            // Attempt to register and then deregister if exists (no deregister method, so just try to register)
            // For tests, we'll try to register, if it fails, assume it's already there and proceed.
            service.register(TEST_USERNAME, TEST_PASSWORD);
        } catch (Exception e) {
            // User might already exist from previous test run if server wasn't fully reset.
            // We'll proceed assuming the server handles existing users gracefully.
            System.out.println("Test user registration might have failed, possibly due to existing user: " + e.getMessage());
        }
    }

    @AfterEach
    public void tearDown() throws RemoteException {
        // Clean up the test user's passwords after each test
        List<PasswordEntry> entries = service.listPasswords(TEST_USERNAME);
        for (PasswordEntry entry : entries) {
            service.deletePassword(TEST_USERNAME, entry.getWebsite());
        }
    }

    @Test
    public void testUserRegistrationAndLogin() throws Exception {
        String newUsername = "newUser" + System.currentTimeMillis(); // Ensure unique username
        String newPassword = "newPass123";

        // Test registration
        assertTrue(service.register(newUsername, newPassword), "New user should be registered successfully");
        assertFalse(service.register(newUsername, newPassword), "Re-registering existing user should fail");

        // Test login
        assertTrue(service.login(newUsername, newPassword), "Login with correct credentials should be successful");
        assertFalse(service.login(newUsername, "wrongpass"), "Login with wrong password should fail");
        assertFalse(service.login("nonexistent", "anypass"), "Login with nonexistent username should fail");
    }

    @Test
    public void testAddAndUpdatePassword() throws Exception {
        // Use the default test user
        String website = "test.com";
        String username = "testuser_account";
        String password = "secret123";

        // Test add password
        service.addPassword(TEST_USERNAME, new PasswordEntry(website, username, password));
        List<PasswordEntry> entries = service.listPasswords(TEST_USERNAME);
        assertEquals(1, entries.size(), "Should have 1 password entry after adding");
        PasswordEntry addedEntry = entries.get(0);
        assertEquals(website, addedEntry.getWebsite());
        assertEquals(username, addedEntry.getUsername());
        assertEquals(password, addedEntry.getPassword()); // Password should be decrypted when listed

        // Test update password
        String updatedPassword = "newsecret456";
        service.updatePassword(TEST_USERNAME, new PasswordEntry(website, username, updatedPassword));
        entries = service.listPasswords(TEST_USERNAME);
        assertEquals(1, entries.size(), "Should still have 1 password entry after update");
        PasswordEntry updatedEntry = entries.get(0);
        assertEquals(website, updatedEntry.getWebsite());
        assertEquals(username, updatedEntry.getUsername());
        assertEquals(updatedPassword, updatedEntry.getPassword());
    }

    @Test
    public void testDeletePassword() throws Exception {
        // Use the default test user
        String website = "todelete.com";
        String username = "deleteuser";
        String password = "deletepass";

        service.addPassword(TEST_USERNAME, new PasswordEntry(website, username, password));
        assertEquals(1, service.listPasswords(TEST_USERNAME).size(), "Should have 1 entry before deletion");

        service.deletePassword(TEST_USERNAME, website);
        assertEquals(0, service.listPasswords(TEST_USERNAME).size(), "Should have 0 entries after deletion");

        // Test deleting non-existent password
        assertThrows(RemoteException.class, () -> service.deletePassword(TEST_USERNAME, "nonexistent.com"),
                "Deleting a non-existent password should throw RemoteException");
    }

    @Test
    public void testListPasswords() throws Exception {
        // Use the default test user
        List<PasswordEntry> initialEntries = service.listPasswords(TEST_USERNAME);
        assertEquals(0, initialEntries.size(), "Should have no passwords initially for a clean test user");

        service.addPassword(TEST_USERNAME, new PasswordEntry("site1.com", "u1", "p1"));
        service.addPassword(TEST_USERNAME, new PasswordEntry("site2.com", "u2", "p2"));

        List<PasswordEntry> entries = service.listPasswords(TEST_USERNAME);
        assertEquals(2, entries.size(), "Should list all added passwords");

        // Verify content
        assertTrue(entries.stream().anyMatch(e -> e.getWebsite().equals("site1.com") && e.getUsername().equals("u1") && e.getPassword().equals("p1")));
        assertTrue(entries.stream().anyMatch(e -> e.getWebsite().equals("site2.com") && e.getUsername().equals("u2") && e.getPassword().equals("p2")));
    }
} 