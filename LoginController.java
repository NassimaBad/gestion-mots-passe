package com.passwordmanager.client.gui;

import java.util.Objects;

import com.passwordmanager.client.PasswordManagerClient;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the LoginView.fxml, handling user authentication and registration.
 * It manages the UI elements for username, password, and status messages, and interacts
 * with the PasswordManagerClient to perform login and registration operations.
 */
public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;

    private PasswordManagerClient client;

    /**
     * Constructor for LoginController. Initializes the PasswordManagerClient.
     * This client is used to communicate with the RMI server for authentication and registration.
     */
    public LoginController() {
        // Initialize the client in the constructor. This ensures it's ready when the controller is created.
        client = new PasswordManagerClient();
    }

    /**
     * Handles the login action when the login button is pressed.
     * It validates user input, attempts to log in via the client, and updates the UI accordingly.
     * @param event The ActionEvent that triggered this method (e.g., button click).
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        clearErrorStyling(); // Clear previous errors
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty()) {
            messageLabel.setText("Le nom d'utilisateur ne peut pas être vide.");
            usernameField.getStyleClass().add("error-field");
            return;
        }
        if (password.isEmpty()) {
            messageLabel.setText("Le mot de passe ne peut pas être vide.");
            passwordField.getStyleClass().add("error-field");
            return;
        }

        try {
            if (client.login(username, password)) {
                messageLabel.setText("Connexion réussie !");
                messageLabel.getStyleClass().remove("error-label");
                messageLabel.getStyleClass().add("success-label");
                System.out.println("Login successful for user: " + username);
                
                // Navigate to the main application view
                Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/com/passwordmanager/gui/MainView.fxml")));
                Parent root = loader.load();

                MainController mainController = loader.getController();
                mainController.setLoggedInUsername(username);

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Gestionnaire de Mots de Passe");
                stage.show();

            } else {
                messageLabel.setText("Nom d'utilisateur ou mot de passe invalide.");
                messageLabel.getStyleClass().remove("success-label");
                messageLabel.getStyleClass().add("error-label");
                usernameField.getStyleClass().add("error-field");
                passwordField.getStyleClass().add("error-field");
            }
        } catch (Exception e) {
            messageLabel.setText("Erreur lors de la connexion : " + e.getMessage());
            messageLabel.getStyleClass().remove("success-label");
            messageLabel.getStyleClass().add("error-label");
            e.printStackTrace();
        }
    }

    /**
     * Handles the registration action when the register button is pressed.
     * It validates user input based on defined rules, attempts to register a new user
     * via the client, and provides feedback to the user.
     * @param event The ActionEvent that triggered this method (e.g., button click).
     */
    @FXML
    private void handleRegister(ActionEvent event) {
        clearErrorStyling(); // Clear previous errors
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty()) {
            messageLabel.setText("Le nom d'utilisateur ne peut pas être vide.");
            usernameField.getStyleClass().add("error-field");
            return;
        }
        if (password.isEmpty()) {
            messageLabel.setText("Le mot de passe ne peut pas être vide.");
            passwordField.getStyleClass().add("error-field");
            return;
        }

        // Username validation
        if (username.length() < 3) {
            messageLabel.setText("Le nom d'utilisateur doit contenir au moins 3 caractères.");
            usernameField.getStyleClass().add("error-field");
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            messageLabel.setText("Le nom d'utilisateur ne peut contenir que des lettres, des chiffres et des underscores.");
            usernameField.getStyleClass().add("error-field");
            return;
        }

        // Password validation
        if (password.length() < 8) {
            messageLabel.setText("Le mot de passe doit contenir au moins 8 caractères.");
            passwordField.getStyleClass().add("error-field");
            return;
        }
        if (!password.matches(".*[A-Z].*")) {
            messageLabel.setText("Le mot de passe doit contenir au moins une lettre majuscule.");
            passwordField.getStyleClass().add("error-field");
            return;
        }
        if (!password.matches(".*[a-z].*")) {
            messageLabel.setText("Le mot de passe doit contenir au moins une lettre minuscule.");
            passwordField.getStyleClass().add("error-field");
            return;
        }
        if (!password.matches(".*[0-9].*")) {
            messageLabel.setText("Le mot de passe doit contenir au moins un chiffre.");
            passwordField.getStyleClass().add("error-field");
            return;
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            messageLabel.setText("Le mot de passe doit contenir au moins un caractère spécial.");
            passwordField.getStyleClass().add("error-field");
            return;
        }

        try {
            if (client.register(username, password)) {
                messageLabel.setText("Enregistrement réussi ! Vous pouvez maintenant vous connecter.");
                messageLabel.getStyleClass().remove("error-label");
                messageLabel.getStyleClass().add("success-label");
                System.out.println("Registration successful for user: " + username);
            } else {
                messageLabel.setText("Échec de l'enregistrement : Le nom d'utilisateur existe déjà.");
                messageLabel.getStyleClass().remove("success-label");
                messageLabel.getStyleClass().add("error-label");
                usernameField.getStyleClass().add("error-field");
            }
        } catch (Exception e) {
            messageLabel.setText("Erreur lors de l'enregistrement : " + e.getMessage());
            messageLabel.getStyleClass().remove("success-label");
            messageLabel.getStyleClass().add("error-label");
            e.printStackTrace();
        }
    }

    /**
     * Clears any error styling from the username and password fields,
     * and resets the message label content and styling.
     */
    private void clearErrorStyling() {
        usernameField.getStyleClass().remove("error-field");
        passwordField.getStyleClass().remove("error-field");
        messageLabel.getStyleClass().remove("error-label");
        messageLabel.getStyleClass().remove("success-label");
        messageLabel.setText(""); // Clear message text
    }
} 