package com.passwordmanager.server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

import com.passwordmanager.common.PasswordManagerService;

public class PasswordManagerServer {
    public static void main(String[] args) {
        try {
            // Create the RMI registry on port 1099
            LocateRegistry.createRegistry(1099);
            System.out.println("RMI Registry created on port 1099");

            // Create an instance of the service implementation
            PasswordManagerService service = new PasswordManagerServiceImpl();

            // Bind the service implementation to the RMI registry
            Naming.rebind("rmi://localhost:1099/PasswordManagerService", service);
            System.out.println("PasswordManagerService bound in registry.");
            System.out.println("Server is ready. Press Enter to stop.");

            // Keep the server running until a key is pressed
            System.in.read();

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
} 