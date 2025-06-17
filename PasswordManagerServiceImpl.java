package com.passwordmanager.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.passwordmanager.common.PasswordEncryptor;
import com.passwordmanager.common.PasswordEntry;
import com.passwordmanager.common.PasswordHasher;
import com.passwordmanager.common.PasswordManagerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;

public class PasswordManagerServiceImpl extends UnicastRemoteObject implements PasswordManagerService {

    private static final long serialVersionUID = 1L;
    private static final String USERS_FILE = "users.json";
    private static final String PASSWORDS_FILE = "passwords.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Stores hashed passwords and salts: Map<username, [hashedPassword, salt]>
    private final Map<String, String[]> userCredentials = new ConcurrentHashMap<>();
    // Stores password entries for each user: Map<username, Map<website, PasswordEntry>>
    private final Map<String, Map<String, PasswordEntry>> userPasswords = new ConcurrentHashMap<>();

    public PasswordManagerServiceImpl() throws RemoteException {
        super();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print JSON
        loadData();

        // Add example users and passwords ONLY if no data was loaded
        if (userCredentials.isEmpty()) {
            String salt1 = PasswordHasher.generateSalt();
            String hashedPassword1 = PasswordHasher.hashPassword("password123", salt1);
            userCredentials.put("user1", new String[]{hashedPassword1, salt1});
            userPasswords.put("user1", new ConcurrentHashMap<>());

            String salt2 = PasswordHasher.generateSalt();
            String hashedPassword2 = PasswordHasher.hashPassword("adminpass", salt2);
            userCredentials.put("admin", new String[]{hashedPassword2, salt2});
            userPasswords.put("admin", new ConcurrentHashMap<>());

            String encryptedGooglePass = PasswordEncryptor.encrypt("googlepass");
            userPasswords.get("user1").put("google.com", new PasswordEntry("google.com", "user1_google", encryptedGooglePass));
            String encryptedFbPass = PasswordEncryptor.encrypt("fbpass");
            userPasswords.get("user1").put("facebook.com", new PasswordEntry("facebook.com", "user1_fb", encryptedFbPass));

            saveData(); // Save initial data
        }
    }

    private void loadData() {
        File usersFile = new File(USERS_FILE);
        File passwordsFile = new File(PASSWORDS_FILE);

        if (usersFile.exists() && passwordsFile.exists()) {
            try {
                // Load user credentials
                Map<String, String[]> loadedCredentials = objectMapper.readValue(usersFile,
                        new TypeReference<Map<String, String[]>>() {});
                userCredentials.putAll(loadedCredentials);

                // Load password entries
                Map<String, Map<String, PasswordEntry>> loadedPasswords = objectMapper.readValue(passwordsFile,
                        new TypeReference<Map<String, Map<String, PasswordEntry>>>() {});
                userPasswords.putAll(loadedPasswords);

                System.out.println("Data loaded from JSON files.");
            } catch (IOException e) {
                System.err.println("Error loading data from JSON files: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No existing data files found. Starting with empty or example data.");
        }
    }

    private void saveData() {
        try {
            objectMapper.writeValue(new File(USERS_FILE), userCredentials);
            objectMapper.writeValue(new File(PASSWORDS_FILE), userPasswords);
            System.out.println("Data saved to JSON files.");
        } catch (IOException e) {
            System.err.println("Error saving data to JSON files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean register(String username, String password) throws RemoteException {
        if (userCredentials.containsKey(username)) {
            System.out.println("Registration failed: User " + username + " already exists.");
            return false; // User already exists
        } else {
            String salt = PasswordHasher.generateSalt();
            String hashedPassword = PasswordHasher.hashPassword(password, salt);
            userCredentials.put(username, new String[]{hashedPassword, salt});
            userPasswords.put(username, new ConcurrentHashMap<>());
            saveData(); // Save data after registration
            System.out.println("User " + username + " registered successfully.");
            return true;
        }
    }

    @Override
    public boolean login(String username, String password) throws RemoteException {
        String[] storedCreds = userCredentials.get(username);
        if (storedCreds != null) {
            String storedHash = storedCreds[0];
            String storedSalt = storedCreds[1];
            if (PasswordHasher.verifyPassword(password, storedHash, storedSalt)) {
                System.out.println("Login successful for user: " + username);
                return true;
            }
        }
        System.out.println("Login failed for user: " + username + ". Invalid credentials.");
        return false;
    }

    @Override
    public void addPassword(String username, PasswordEntry entry) throws RemoteException {
        // Encrypt the password before storing
        String encryptedPassword = PasswordEncryptor.encrypt(entry.getPassword());
        PasswordEntry encryptedEntry = new PasswordEntry(entry.getWebsite(), entry.getUsername(), encryptedPassword);
        userPasswords.computeIfAbsent(username, k -> new ConcurrentHashMap<>()).put(encryptedEntry.getWebsite(), encryptedEntry);
        saveData(); // Save data after adding password
        System.out.println("Password added for " + username + ": " + encryptedEntry.getWebsite());
    }

    @Override
    public void updatePassword(String username, PasswordEntry entry) throws RemoteException {
        if (userPasswords.containsKey(username) && userPasswords.get(username).containsKey(entry.getWebsite())) {
            // Encrypt the password before updating
            String encryptedPassword = PasswordEncryptor.encrypt(entry.getPassword());
            PasswordEntry encryptedEntry = new PasswordEntry(entry.getWebsite(), entry.getUsername(), encryptedPassword);
            userPasswords.get(username).put(encryptedEntry.getWebsite(), encryptedEntry);
            saveData(); // Save data after updating password
            System.out.println("Password updated for " + username + ": " + encryptedEntry.getWebsite());
        } else {
            throw new RemoteException("Password entry not found for update.");
        }
    }

    @Override
    public void deletePassword(String username, String website) throws RemoteException {
        if (userPasswords.containsKey(username) && userPasswords.get(username).containsKey(website)) {
            userPasswords.get(username).remove(website);
            saveData(); // Save data after deleting password
            System.out.println("Password deleted for " + username + ": " + website);
        } else {
            throw new RemoteException("Password entry not found for deletion.");
        }
    }

    @Override
    public List<PasswordEntry> listPasswords(String username) throws RemoteException {
        // Decrypt passwords before returning to the client
        return userPasswords.getOrDefault(username, new ConcurrentHashMap<>()).values().stream()
                .map(entry -> new PasswordEntry(entry.getWebsite(), entry.getUsername(), PasswordEncryptor.decrypt(entry.getPassword())))
                .collect(Collectors.toList());
    }
} 