package start;// package java.demo.hub;

import app.demo.Settings;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import ui.SettingsMenu;
import ui.hub;

import java.net.URL;
import java.util.Objects;

public class start extends Application {

    private static final boolean DEBUG = true; // true shows green hotspots
    private static final String START_IMG = "/start.png";

    private ImageView bgView;
    private MediaPlayer startMusic;
    private ColorAdjust adjust;

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        root.setStyle("-fx-background-color: black;");

        // brightness control
        adjust = new ColorAdjust();
        root.setEffect(adjust);

        // background
        bgView = new ImageView();
        root.getChildren().add(0, bgView);
        setBackground(primaryStage, START_IMG);

        // music (loops)
        startMusic = loopMusic("/sfx/BeepBox-Song.wav", Settings.getVolume());

        // --- HOTSPOTS ---
        root.getChildren().add(makeHotspot(465, 338, 290, 55, "NEW GAME", () -> {
            System.out.println("New Game pressed");
            if (startMusic != null) startMusic.stop();
            try { new hub().start(primaryStage); } catch (Exception ex) { ex.printStackTrace(); }
        }));

        root.getChildren().add(makeHotspot(470, 398, 280, 47, "LOAD GAME", () ->
                System.out.println("Load Game clicked (TODO)")
        ));


        root.getChildren().add(makeHotspot(557, 450, 124, 45, "EXIT", () -> {
            if (startMusic != null) startMusic.stop();
            primaryStage.close();
        }));

        // scene + key handler
        Scene scene = new Scene(root);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                e.consume(); // stop buttons from seeing SPACE
                SettingsMenu.show(primaryStage, this::applySettings);
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Start Menu");
        primaryStage.setResizable(false);
        primaryStage.show();

        applySettings(); // apply current java.demo.Settings on show
    }

    private void setBackground(Stage stage, String resourcePath) {
        try {
            URL url = Objects.requireNonNull(
                    getClass().getResource(resourcePath),
                    "Resource '" + resourcePath + "' not found."
            );
            Image img = new Image(url.toExternalForm());

            bgView.setImage(img);
            bgView.setPreserveRatio(false);
            bgView.setFitWidth(img.getWidth());
            bgView.setFitHeight(img.getHeight());

            Pane root = (Pane) bgView.getParent();
            root.setPrefSize(img.getWidth(), img.getHeight());

            if (stage != null && stage.getScene() != null) {
                stage.sizeToScene();
                stage.centerOnScreen();
            }
        } catch (NullPointerException npe) {
            new Alert(Alert.AlertType.ERROR,
                    "Could not find '" + resourcePath + "' in resources.\n" +
                            "Make sure the PNG is in your resources folder.").show();
        }
    }

    private Button makeHotspot(double x, double y, double w, double h,
                               String ariaLabel, Runnable onClick) {
        Button b = new Button();
        b.setLayoutX(x);
        b.setLayoutY(y);
        b.setPrefSize(w, h);
        b.setPickOnBounds(true);
        b.setCursor(Cursor.HAND);
        b.setAccessibleText(ariaLabel);

        b.setFocusTraversable(false);
        b.setMnemonicParsing(false);

        b.setStyle(DEBUG
                ? "-fx-background-color: rgba(0,0,0,0.25); -fx-border-color: #00ff00; -fx-border-width: 1;"
                : "-fx-background-color: transparent;");
        b.setOnAction(e -> onClick.run());
        return b;
    }


    private MediaPlayer loopMusic(String resourcePath, double volume) {
        var url = Objects.requireNonNull(
                getClass().getResource(resourcePath),
                "Missing audio: " + resourcePath);
        MediaPlayer mp = new MediaPlayer(new Media(url.toExternalForm()));
        mp.setCycleCount(MediaPlayer.INDEFINITE);
        mp.setVolume(volume);
        mp.play();
        return mp;
    }

    // single applySettings()
    private void applySettings() {
        if (adjust != null) {
            adjust.setBrightness(Settings.getBrightness() - 1.0);
        }
        if (startMusic != null) {
            startMusic.setVolume(Settings.getVolume());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
