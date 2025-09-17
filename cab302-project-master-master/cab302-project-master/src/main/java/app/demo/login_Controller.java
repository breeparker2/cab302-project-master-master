package app.demo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class login_Controller {
    AccountDAO dao = new AccountDAO();

    // Link to the fx:id fields in your FXML
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button Return;

    // Handle the Login button
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (dao.ConfrimLogin(username, password)){
            Account account = dao.getAccount(username, password);
            showAlert(AlertType.INFORMATION, "Login Successful",
                    "Welcome, " + account.getUsername() + "!");
            /*
            try{
                Stage currentStage = (Stage) usernameField.getScene().getWindow();
                new java.demo.start().java.demo.start(currentStage);
            } catch (IOException e){
                e.printStackTrace();
            }

             */



        }
        else{
            showAlert(AlertType.ERROR, "Login Failed", "Invalid username or password.");
        }
    }
    /*
    @FXML
    private void nextScreen(String fxmlFile, String title) throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(java.demo.Main.class.getResource(fxmlFile));
        Scene scene = new Scene(fxmlLoader.load(), java.demo.Main.WIDTH, java.demo.Main.HEIGHT);
        Stage stage = (Stage) PLACEHOLDER.getScene().getWindow();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
     */


    @FXML
    private void handleNewAccount(){
        String username = usernameField.getText();
        String password = passwordField.getText();
        Account account = new Account(username, password);
        // here is where new password gets hashed
        if ((username == "") | (password == "")){
            showAlert(AlertType.ERROR, "Account Creation Failed", "You need a valid username and password");
        }

        else if (dao.getAccount(username, password) == null) {
            dao.CreateAccount(account);
        }
        else{
            showAlert(AlertType.ERROR, "Account Creation Failed", "This Account Already Exists");
        }

    }
    // Handle the Cancel button
    @FXML
    private void handleReturn() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), LoginMain.WIDTH, LoginMain.HEIGHT);
        Stage stage = (Stage) Return.getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    // Utility method for alerts
    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}