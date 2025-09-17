package app.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class LoginMain extends Application {

    public static final String TITLE = "Login Screen";
    public static final int WIDTH = 640;
    public static final int HEIGHT = 360;
    @Override
    public void start(Stage stage) throws IOException {
        System.out.println(LoginMain.class.getResource("/app/demo/login_Screen.fxml"));
        AccountDAO.initialiseDatabase();
        FXMLLoader fxmlLoader = new FXMLLoader(LoginMain.class.getResource("/app/demo/login_Screen.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);
        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();
    }


    public static void main(String[] args) {
        launch();
    }
}