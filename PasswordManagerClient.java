package com.passwordmanager.client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import com.passwordmanager.common.PasswordEntry;
import com.passwordmanager.common.PasswordManagerService;

public class PasswordManagerClient {

    private PasswordManagerService service;

    public PasswordManagerClient() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            service = (PasswordManagerService) registry.lookup("PasswordManagerService");
            System.out.println("Connected to PasswordManagerService.");
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public boolean login(String username, String password) throws Exception {
        return service.login(username, password);
    }

    public boolean register(String username, String password) throws Exception {
        return service.register(username, password);
    }

    public void addPassword(String username, PasswordEntry entry) throws Exception {
        service.addPassword(username, entry);
    }

    public void updatePassword(String username, PasswordEntry entry) throws Exception {
        service.updatePassword(username, entry);
    }

    public void deletePassword(String username, String website) throws Exception {
        service.deletePassword(username, website);
    }

    public List<PasswordEntry> listPasswords(String username) throws Exception {
        return service.listPasswords(username);
    }

    // Main method for testing the client connection
    public static void main(String[] args) {
        PasswordManagerClient client = new PasswordManagerClient();
        if (client.service != null) {
            try {
                System.out.println("Attempting to authenticate user1 with pass1...");
                boolean authenticated = client.login("user1", "pass1");
                System.out.println("Authentication successful: " + authenticated);

                if (authenticated) {
                    System.out.println("Adding a test password...");
                    client.addPassword("user1", new PasswordEntry("test.com", "testuser", "testpass"));

                    System.out.println("Listing passwords...");
                    List<PasswordEntry> entries = client.listPasswords("user1");
                    entries.forEach(entry -> System.out.println("- " + entry));
                }

            } catch (Exception e) {
                System.err.println("Client operation failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
} 