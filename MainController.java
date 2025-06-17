package com.passwordmanager.client.gui;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.passwordmanager.client.PasswordManagerClient;
import com.passwordmanager.common.PasswordEntry;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller for the MainView.fxml, handling password management operations
 * such as adding, updating, deleting, listing, and searching passwords.
 * It interacts with the PasswordManagerClient to communicate with the RMI server
 * and provides visual feedback to the user through the UI.
 */
public class MainController {

    @FXML
    private TableView<PasswordEntry> passwordTable;
    @FXML
    private TableColumn<PasswordEntry, String> websiteColumn;
    @FXML
    private TableColumn<PasswordEntry, String> usernameColumn;
    @FXML
    private TableColumn<PasswordEntry, String> passwordColumn;
    @FXML
    private TextField websiteField;
    @FXML
    private TextField entryUsernameField;
    @FXML
    private TextField entryPasswordField;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;

    private PasswordManagerClient client;
    private String loggedInUsername;
    private ObservableList<PasswordEntry> passwordList;

    /**
     * Sets the username of the currently logged-in user and initializes the client
     * and loads passwords for this user.
     * @param username The username of the logged-in user.
     */
    public void setLoggedInUsername(String username) {
        this.loggedInUsername = username;
        initializeClient();
        loadPasswords();
    }

    /**
     * Initializes the controller after its root element has been completely processed.
     * This method is automatically called by the FXMLLoader.
     * It sets up table column cell value factories, customizes the password column
     * for visibility toggle and copy functionality, and adds listeners for UI interactions.
     */
    @FXML
    public void initialize() {
        // Configure table columns
        websiteColumn.setCellValueFactory(new PropertyValueFactory<>("website"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        // Custom cell factory for password column
        passwordColumn.setCellValueFactory(new PropertyValueFactory<>("password"));
        // Gère l'affichage des mots de passe (masqués ou visibles)
        passwordColumn.setCellFactory(column -> {
            return new javafx.scene.control.TableCell<PasswordEntry, String>() {
                private final Label maskedLabel = new Label();
                private final Label visibleLabel = new Label();
                private final javafx.scene.control.Button toggleVisibilityButton = new javafx.scene.control.Button();
                private final javafx.scene.control.Button copyButton = new javafx.scene.control.Button();
                private final HBox container = new HBox(5);

                private boolean passwordVisible = false;

                {
                    toggleVisibilityButton.setText("👁️"); // Eye icon
                    toggleVisibilityButton.getStyleClass().add("password-action-button");
                    toggleVisibilityButton.setOnAction(event -> {
                        passwordVisible = !passwordVisible;
                        updateItem(getItem(), isEmpty()); // Re-render the cell
                    });
                    // Action sur le bouton copier
                    copyButton.setText("📋"); // Copy icon
                    copyButton.getStyleClass().add("password-action-button");
                    copyButton.setOnAction(event -> {
                        final Clipboard clipboard = Clipboard.getSystemClipboard();
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(getItem()); // Get the actual password
                        clipboard.setContent(content);
                        statusLabel.setText("Password copied to clipboard!");
                        statusLabel.getStyleClass().add("success-label");
                    });

                    container.setAlignment(Pos.CENTER_LEFT);
                    container.getStyleClass().add("password-cell");
                }

                @Override
                protected void updateItem(String password, boolean empty) {
                    super.updateItem(password, empty);
                    if (empty || password == null) {
                        setGraphic(null);
                    } else {
                        if (passwordVisible) {
                            visibleLabel.setText(password);
                            setGraphic(new HBox(5, visibleLabel, toggleVisibilityButton, copyButton));
                        } else {
                            maskedLabel.setText("•".repeat(password.length()));
                            setGraphic(new HBox(5, maskedLabel, toggleVisibilityButton, copyButton));
                        }
                    }
                }
            };
        });
        // Initialise la liste observable et la lie à la table
        passwordList = FXCollections.observableArrayList();
        passwordTable.setItems(passwordList);

        // Sélection d'une ligne dans la table remplit les champs de saisie
        passwordTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        websiteField.setText(newSelection.getWebsite());
                        entryUsernameField.setText(newSelection.getUsername());
                        entryPasswordField.setText(newSelection.getPassword());
                        clearInputStyling(); // Clear error styling on selection
                    }
                });

        // Listen for text changes in input fields to clear status messages
        websiteField.textProperty().addListener((obs, oldText, newText) -> clearStatusMessage());
        entryUsernameField.textProperty().addListener((obs, oldText, newText) -> clearStatusMessage());
        entryPasswordField.textProperty().addListener((obs, oldText, newText) -> clearStatusMessage());
        searchField.textProperty().addListener((obs, oldText, newText) -> clearStatusMessage());
    }

    /**
     * Initializes the PasswordManagerClient. This method ensures that the client
     * is ready to communicate with the RMI server. It's called when the logged-in
     * username is set.
     */
    private void initializeClient() {
        // This client should already be connected via LoginController, but we ensure it here.
        // In a more robust app, we would pass the client instance from the login scene.
        if (client == null) {
            client = new PasswordManagerClient();
        }
    }

    /**
     * Loads password entries for the logged-in user from the RMI server
     * and populates the password table. Updates the status label based on the outcome.
     */
    private void loadPasswords() {
        if (loggedInUsername == null || client == null) {
            statusLabel.setText("Error: User not logged in or client not initialized.");
            statusLabel.getStyleClass().add("error-label");
            System.err.println("Error: User not logged in or client not initialized during password loading.");
            return;
        }
        try {
            List<PasswordEntry> entries = client.listPasswords(loggedInUsername);
            passwordList.setAll(entries);
            statusLabel.setText("Passwords loaded.");
            statusLabel.getStyleClass().remove("error-label");
            statusLabel.getStyleClass().add("success-label");
            System.out.println("Passwords successfully loaded for user: " + loggedInUsername);
        } catch (Exception e) {
            statusLabel.setText("Error loading passwords: " + e.getMessage());
            statusLabel.getStyleClass().add("error-label");
            System.err.println("Error loading passwords for user " + loggedInUsername + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the search action, filtering password entries based on the text
     * entered in the search field. The search is performed on website and username fields.
     */
    @FXML
    private void handleSearch() {
        clearInputStyling(); // Clear any previous error styling
        final String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            loadPasswords(); // If search text is empty, load all passwords
        } else {
            try {
                final List<PasswordEntry> allEntries = client.listPasswords(loggedInUsername);
                final ObservableList<PasswordEntry> filteredList = FXCollections.observableArrayList(allEntries.stream()
                        .filter(entry -> entry.getWebsite().toLowerCase().contains(searchText) ||
                                         entry.getUsername().toLowerCase().contains(searchText))
                        .collect(Collectors.toList()));
                passwordList.setAll(filteredList);
                statusLabel.setText("Search results for '" + searchText + "'.");
                statusLabel.getStyleClass().remove("error-label");
                statusLabel.getStyleClass().add("success-label");
                System.out.println("Search performed for '" + searchText + "'. Found " + filteredList.size() + " entries.");
            } catch (Exception e) {
                statusLabel.setText("Error during search: " + e.getMessage());
                statusLabel.getStyleClass().add("error-label");
                System.err.println("Error during password search: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles the action for adding a new password entry.
     * It validates input fields, sends the new entry to the server, and updates the UI.
     */
    @FXML
    private void handleAddPassword() {
        clearInputStyling(); // Clear previous error styling
        final String website = websiteField.getText().trim();
        final String username = entryUsernameField.getText().trim();
        final String password = entryPasswordField.getText().trim();

        boolean hasError = false;
        if (website.isEmpty()) {
            websiteField.getStyleClass().add("error-field");
            hasError = true;
        }
        if (username.isEmpty()) {
            entryUsernameField.getStyleClass().add("error-field");
            hasError = true;
        }
        if (password.isEmpty()) {
            entryPasswordField.getStyleClass().add("error-field");
            hasError = true;
        }

        if (hasError) {
            statusLabel.setText("Tous les champs sont requis pour ajouter un mot de passe.");
            statusLabel.getStyleClass().add("error-label");
            System.err.println("Validation failed for adding password: Empty fields.");
            return;
        }
        try {
            client.addPassword(loggedInUsername, new PasswordEntry(website, username, password));
            statusLabel.setText("Mot de passe ajouté avec succès.");
            statusLabel.getStyleClass().remove("error-label");
            statusLabel.getStyleClass().add("success-label");
            clearFields();
            loadPasswords();
            System.out.println("Password added successfully for user " + loggedInUsername + " for website " + website);
        } catch (Exception e) {
            statusLabel.setText("Erreur lors de l'ajout du mot de passe : " + e.getMessage());
            statusLabel.getStyleClass().add("error-label");
            System.err.println("Error adding password for user " + loggedInUsername + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the action for updating an existing password entry.
     * It validates input fields, sends the updated entry to the server, and refreshes the UI.
     */
    @FXML
    private void handleUpdatePassword() {
        clearInputStyling(); // Clear previous error styling
        final String website = websiteField.getText().trim();
        final String username = entryUsernameField.getText().trim();
        final String password = entryPasswordField.getText().trim();

        boolean hasError = false;
        if (website.isEmpty()) {
            websiteField.getStyleClass().add("error-field");
            hasError = true;
        }
        if (username.isEmpty()) {
            entryUsernameField.getStyleClass().add("error-field");
            hasError = true;
        }
        if (password.isEmpty()) {
            entryPasswordField.getStyleClass().add("error-field");
            hasError = true;
        }

        if (hasError) {
            statusLabel.setText("Tous les champs sont requis pour mettre à jour un mot de passe.");
            statusLabel.getStyleClass().add("error-label");
            System.err.println("Validation failed for updating password: Empty fields.");
            return;
        }
        try {
            client.updatePassword(loggedInUsername, new PasswordEntry(website, username, password));
            statusLabel.setText("Mot de passe mis à jour avec succès.");
            statusLabel.getStyleClass().remove("error-label");
            statusLabel.getStyleClass().add("success-label");
            clearFields();
            loadPasswords();
            System.out.println("Password updated successfully for user " + loggedInUsername + " for website " + website);
        } catch (Exception e) {
            statusLabel.setText("Erreur lors de la mise à jour du mot de passe : " + e.getMessage());
            statusLabel.getStyleClass().add("error-label");
            System.err.println("Error updating password for user " + loggedInUsername + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the action for deleting a password entry.
     * It validates the website field, sends the delete request to the server, and refreshes the UI.
     */
    @FXML
    private void handleDeletePassword() {
        clearInputStyling(); // Clear previous error styling
        final String website = websiteField.getText().trim();
        if (website.isEmpty()) {
            statusLabel.setText("Le champ Site Web est requis pour supprimer un mot de passe.");
            websiteField.getStyleClass().add("error-field");
            statusLabel.getStyleClass().add("error-label");
            System.err.println("Validation failed for deleting password: Empty website field.");
            return;
        }
        try {
            client.deletePassword(loggedInUsername, website);
            statusLabel.setText("Mot de passe supprimé avec succès.");
            statusLabel.getStyleClass().remove("error-label");
            statusLabel.getStyleClass().add("success-label");
            clearFields();
            loadPasswords();
            System.out.println("Password deleted successfully for user " + loggedInUsername + " for website " + website);
        } catch (Exception e) {
            statusLabel.setText("Erreur lors de la suppression du mot de passe : " + e.getMessage());
            statusLabel.getStyleClass().add("error-label");
            System.err.println("Error deleting password for user " + loggedInUsername + " for website " + website + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the refresh action, reloading all password entries for the logged-in user.
     */
    @FXML
    private void handleRefresh() {
        clearInputStyling();
        loadPasswords();
        System.out.println("Passwords refreshed for user: " + loggedInUsername);
    }

    /**
     * Clears all input fields (website, username, password).
     */
    private void clearFields() {
        websiteField.clear();
        entryUsernameField.clear();
        entryPasswordField.clear();
        System.out.println("Input fields cleared.");
    }

    /**
     * Clears any error styling applied to the input fields.
     */
    private void clearInputStyling() {
        websiteField.getStyleClass().remove("error-field");
        entryUsernameField.getStyleClass().remove("error-field");
        entryPasswordField.getStyleClass().remove("error-field");
        clearStatusMessage();
        System.out.println("Input field styling cleared.");
    }

    /**
     * Clears the status message and removes any success or error styling from it.
     */
    private void clearStatusMessage() {
        statusLabel.setText("");
        statusLabel.getStyleClass().remove("error-label");
        statusLabel.getStyleClass().remove("success-label");
        System.out.println("Status message cleared.");
    }

    /**
     * Handles the logout action, clearing user session data and navigating back to the login view.
     * @param event The ActionEvent that triggered this method (e.g., button click).
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Clear any session-related data (if applicable)
            loggedInUsername = null;
            client = null; // Disconnect client from server (optional, re-initializes on login)
            passwordList.clear();
            clearInputStyling(); // Clear any lingering styling on logout
            System.out.println("User logged out successfully.");

            // Navigate back to LoginView
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/com/passwordmanager/gui/LoginView.fxml")));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Password Manager Login");
            stage.show();
        } catch (Exception e) {
            statusLabel.setText("Erreur lors de la déconnexion : " + e.getMessage());
            statusLabel.getStyleClass().add("error-label");
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 