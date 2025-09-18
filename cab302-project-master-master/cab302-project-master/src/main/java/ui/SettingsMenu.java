package ui;

import app.demo.Settings;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SettingsMenu {

    /**
     * Show settings dialog with LIVE preview.
     * @param owner the parent stage
     * @param onChange a callback (e.g., your applySettings()) called whenever values change
     */
    public static void show(Stage owner, Runnable onChange) {
        // Remember old values so Cancel can revert
        double oldBrightness = Settings.getBrightness(); // expected 0.0..2.0 (1.0 = neutral)
        double oldVolume     = Settings.getVolume();     // 0.0..1.0

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("java.demo.Settings");

        // --- Brightness ---
        Label brightLabel = new Label("Brightness");
        Slider brightSlider = new Slider(0.0, 2.0, Settings.getBrightness());
        brightSlider.setShowTickLabels(true);
        brightSlider.setMajorTickUnit(0.5);
        brightSlider.valueProperty().addListener((obs, ov, nv) -> {
            Settings.setBrightness(nv.doubleValue());
            if (onChange != null) onChange.run();     // LIVE apply
        });

        // --- Volume ---
        Label volLabel = new Label("Volume");
        Slider volSlider = new Slider(0.0, 1.0, Settings.getVolume());
        volSlider.setShowTickLabels(true);
        volSlider.setMajorTickUnit(0.25);
        volSlider.valueProperty().addListener((obs, ov, nv) -> {
            Settings.setVolume(nv.doubleValue());
            if (onChange != null) onChange.run();     // LIVE apply
        });

        // Buttons
        Button ok = new Button("OK");
        ok.setOnAction(e -> dialog.close());

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> {
            // Revert to old values and re-apply
            Settings.setBrightness(oldBrightness);
            Settings.setVolume(oldVolume);
            if (onChange != null) onChange.run();
            dialog.close();
        });

        Button reset = new Button("Reset");
        reset.setOnAction(e -> {
            brightSlider.setValue(1.0);  // neutral brightness
            volSlider.setValue(1.0);     // full volume
            // listeners above already apply + call onChange
        });

        HBox buttons = new HBox(10, reset, cancel, ok);
        VBox root = new VBox(12,
                brightLabel, brightSlider,
                volLabel, volSlider,
                buttons
        );
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #222; -fx-text-fill: white;");

        dialog.setScene(new Scene(root, 340, 200));
        dialog.show(); // non-blocking; live updates still work with showAndWait()
    }
}
