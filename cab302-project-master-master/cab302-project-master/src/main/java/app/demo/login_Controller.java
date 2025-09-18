package app.demo;

import app.demo.data.AccountDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import start.start;

public class login_Controller {

    private final AccountDAO dao = new AccountDAO();

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button Return;

    @FXML
    private void handleLogin() {
        // sanity check: FXML wiring
        if (usernameField == null || passwordField == null) {
            showAlert(Alert.AlertType.ERROR, "Wiring Error",
                    "usernameField or passwordField is null. Check fx:id/controller.");
            return;
        }

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (!dao.ConfrimLogin(username, password)) {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
            return;
        }

        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            new start().start(stage); // go to Start page class
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not open the start menu.\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @FXML
    private void handleNewAccount() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Account Creation Failed",
                    "You need a valid username and password.");
            return;
        }

        if (dao.usernameExists(username)) {
            showAlert(Alert.AlertType.ERROR, "Account Creation Failed",
                    "That username is already taken.");
            return;
        }


        boolean created = dao.register(username, password); // <-- raw password; DAO hashes it
        if (created) {
            showAlert(Alert.AlertType.INFORMATION, "Account Created",
                    "Your account has been created. You can now log in.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Account Creation Failed",
                    "Could not create the account. Please try a different username.");
        }
    }

    @FXML
    private void handleReturn() {
        Stage stage = (Stage) Return.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        var alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
