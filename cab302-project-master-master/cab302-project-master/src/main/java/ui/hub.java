package ui;

import app.demo.HubGateway;
import app.demo.Settings;
import app.demo.TaskManager;
import ui.TaskPanelView;
import events.MarketEventsManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import npc.Npc;
import start.start;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class hub extends Application {

    private static final boolean DEBUG = true;

    private ImageView bgView;
    private Pane hubButtons;

    private TaskPanelView taskPanelView;
    private TaskManager taskManager;

    private MarketEventsManager events;

    // End-of-day overlay
    private Pane eodLayer;
    private ImageView eodView;
    private Button eodContinueBtn;
    private static final String EOD_IMG = "/day report.png";
    private static final double TRUST_ROW_Y = 500;
    private static final double RIGHT_MARGIN = 140;
    private static final int EOD_REP_FONT_SIZE = 40;
    private Label eodRepDeltaLbl;

    // Workday
    private Label clockLabel;
    private VBox clockBox;
    private Timeline workdayTimer;
    private LocalTime simTime = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(17, 0);
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("h:mm a");

    // Resources
    private static final String HUB_IMG = "/menu-1.png_3-1.png_3.png";
    private static final String COUNTRY_IMG = "/country.png";
    private static final String EVENTS_IMG = "/events.png";
    private static final String STOCKS_IMG = "/Stock.png";

    private MediaPlayer hubAmbient;
    private ColorAdjust adjust;

    private boolean onHub = true;

    private void toggleTo(String resourcePath) {
        Stage stage = (Stage) clockLabel.getScene().getWindow();
        if (onHub) {
            goTo(stage, resourcePath, false);   // go to page
        } else {
            goTo(stage, HUB_IMG, false);        // return to hub
        }
    }

    // Simple player state
    private int cash = 200;
    private int reputation = 0;
    private int dailyRepDelta = 0;

    private Label repLbl;
    private Label cashUnderLbl;

    private static final LocalTime SOCIALITE_TIME = LocalTime.of(15, 0);

    // NPC controller
    private Npc.Controller npc;

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        root.setStyle("-fx-background-color: #2b2b2b;");
        adjust = new ColorAdjust();
        root.setEffect(adjust);

        bgView = new ImageView();
        root.getChildren().add(0, bgView);

        setBackground(null, HUB_IMG);
        onHub = true;

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.setResizable(false);

        // Tasks + Events
        taskManager = new TaskManager();

        events = new MarketEventsManager();
        events.subscribe(taskManager::onEvent);
        HubGateway.register(events.publisher());
        events.start();

        // Tasks panel (left tablet)
        taskPanelView = new TaskPanelView(taskManager, DEBUG);
        root.getChildren().add(taskPanelView.getRoot());
        taskPanelView.getRoot().setLayoutX(36);
        taskPanelView.getRoot().setLayoutY(470);

        // Hub buttons
        hubButtons = new Pane();
        root.getChildren().add(hubButtons);

        hubButtons.getChildren().add(makeHotspot(40, 588, 80, 120, "TASKS", () -> {
            if (taskPanelView.isOpen()) {
                taskPanelView.close();
                hubButtons.toFront();                 // bring buttons back on top
            } else {
                taskPanelView.open();
                taskPanelView.getRoot().toFront();    // bring tablet on top
            }
        }));
        hubButtons.getChildren().add(makeHotspot(578, 520, 175, 34, "COUNTRY",
                () -> toggleTo(COUNTRY_IMG)));
        hubButtons.getChildren().add(makeHotspot(578, 554, 146, 34, "EVENTS",
                () -> toggleTo(EVENTS_IMG)));
        hubButtons.getChildren().add(makeHotspot(578, 588, 128, 34, "STOCKS",
                () -> toggleTo(STOCKS_IMG)));
        hubButtons.getChildren().add(makeHotspot(578, 622, 174, 34, "PORTFOLIO", () -> {
            System.out.println("Portfolio clicked");
            taskPanelView.close();
            hubButtons.toFront();
        }));
        hubButtons.getChildren().add(makeHotspot(670, 223, 135, 48, "EXIT", () -> {
            if (hubAmbient != null) {
                hubAmbient.stop();
                hubAmbient.dispose();
                hubAmbient = null;
            }
            if (workdayTimer != null) workdayTimer.stop();
            if (eodLayer != null) eodLayer.setVisible(false);
            try { new start().start(primaryStage); } catch (Exception ex) { ex.printStackTrace(); }
        }));

        // portfolio label under PORTFOLIO
        cashUnderLbl = new Label();
        cashUnderLbl.setTextFill(Color.web("#57ff89"));
        cashUnderLbl.setFont(javafx.scene.text.Font.font("Segoe UI",
                javafx.scene.text.FontWeight.BOLD, 20));
        cashUnderLbl.setStyle(
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 6, 0.2, 0, 0);"
        );
        double portX = 578, portY = 622, portW = 174, portH = 34;
        cashUnderLbl.setLayoutX(portX);
        cashUnderLbl.setLayoutY(portY + portH + 16);
        cashUnderLbl.setPrefWidth(portW);
        cashUnderLbl.setMinWidth(Region.USE_PREF_SIZE);
        cashUnderLbl.setMaxWidth(Region.USE_PREF_SIZE);
        cashUnderLbl.setWrapText(false);
        cashUnderLbl.setAlignment(Pos.CENTER_LEFT);
        cashUnderLbl.setTextOverrun(OverrunStyle.CLIP);
        ((Pane) bgView.getParent()).getChildren().add(cashUnderLbl);
        cashUnderLbl.toFront();

        // clock + start day
        initClock(root);
        refreshStatus();
        startWorkdayTimer();

        // NPCs
        npc = new Npc.Controller();
        npc.initUI(root, hubButtons);
        npc.initRegistryAndDialogue();

        hubButtons.setVisible(true);

        primaryStage.setTitle("Hub Page");
        primaryStage.show();

        // ambience + settings
        if (hubAmbient == null) {
            hubAmbient = loopMusic("/sfx/general-chatter-in-bar-14816.mp3", Settings.getVolume());
        } else {
            hubAmbient.play();
        }
        applySettings();

        // Morning briefing at 9:00
        if (!npc.briefingShownToday && simTime.equals(LocalTime.of(9, 0))) {
            Timeline t = new Timeline(new KeyFrame(
                    Duration.millis(400),
                    e -> npc.morningBrief(events.getTodaysEvents())));
            t.setCycleCount(1);
            t.play();
        }

        // keys
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case SPACE -> {
                    if (!npc.isDialogueVisible()) {
                        SettingsMenu.show(primaryStage, this::applySettings);
                    }
                    e.consume();
                }
                case ENTER -> {
                    if (npc.isDialogueVisible()) {
                        npc.daysUntilNextAudit = new Random().nextInt(24) + 7;
                        e.consume();
                    }
                }
                default -> {}
            }
        });
    }

    private void startWorkdayTimer() {
        workdayTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!npc.briefingShownToday && simTime.equals(LocalTime.of(9, 0))) {
                npc.morningBrief(events.getTodaysEvents());
            }

            if (onHub && !npc.npc2VisitShownToday && simTime.equals(LocalTime.of(11, 30))) {
                npc.energyGamble(
                        (IntSupplier) () -> cash,
                        (IntConsumer) v -> { cash = v; refreshStatus(); },
                        (IntConsumer) this::changeReputation,
                        this::refreshStatus
                );
            }

            if (onHub && !npc.socialiteShownToday && simTime.equals(SOCIALITE_TIME) && !npc.isDialogueVisible()) {
                npc.socialInvite(
                        reputation,
                        (IntSupplier) () -> cash,
                        (IntConsumer) v -> { cash = v; refreshStatus(); },
                        (IntConsumer) this::changeReputation,
                        this::refreshStatus
                );
            }

            if (!npc.auditScheduledToday && npc.daysUntilNextAudit <= 0 && npc.npc2VisitShownToday && npc.gambledToday) {
                npc.scheduleAuditForToday();
            }
            if (npc.auditScheduledToday && simTime.equals(npc.auditTime) && !npc.isDialogueVisible()) {
                npc.showAudit((IntConsumer) this::changeReputation, this::refreshStatus);
            }

            simTime = simTime.plusMinutes(1);
            clockLabel.setText(CLOCK_FMT.format(simTime));

            if (!simTime.isBefore(END_TIME)) {
                workdayTimer.stop();
                clockLabel.setText(CLOCK_FMT.format(END_TIME));
                showEndOfDayReport();
            }
        }));
        workdayTimer.setCycleCount(Timeline.INDEFINITE);
        workdayTimer.playFromStart();
    }

    private void showEndOfDayReport() {
        if (eodLayer == null) {
            eodLayer = new Pane();
            eodLayer.setPickOnBounds(true);
            eodLayer.setMouseTransparent(false);
            eodLayer.setStyle("-fx-background-color: rgba(0,0,0,0.0);");

            eodView = new ImageView();
            eodView.setPreserveRatio(false);

            eodContinueBtn = new Button();
            eodContinueBtn.setStyle("-fx-background-color: transparent;");
            eodContinueBtn.setOnAction(e -> continueToNextDay());

            eodRepDeltaLbl = new Label();
            eodRepDeltaLbl.setStyle(
                    "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
                            "-fx-font-size: " + EOD_REP_FONT_SIZE + ";" +
                            "-fx-text-fill: #f6b233;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 4, 0.2, 0, 1);"
            );

            eodLayer.getChildren().addAll(eodView, eodRepDeltaLbl, eodContinueBtn);

            Pane root = (Pane) bgView.getParent();
            root.getChildren().add(eodLayer);
        }

        eodLayer.setVisible(true);
        eodLayer.toFront();

        URL url = Objects.requireNonNull(getClass().getResource(EOD_IMG), "Resource '" + EOD_IMG + "' not found.");
        Image img = new Image(url.toExternalForm());
        eodView.setImage(img);

        Pane root = (Pane) bgView.getParent();
        double w = root.getWidth();
        double h = root.getHeight();

        eodLayer.resizeRelocate(0, 0, w, h);
        eodView.setFitWidth(w);
        eodView.setFitHeight(h);

        double btnX = 80, btnY = h - 90, btnW = w - 160, btnH = 70;
        eodContinueBtn.setLayoutX(btnX);
        eodContinueBtn.setLayoutY(btnY);
        eodContinueBtn.setPrefSize(btnW, btnH);

        eodRepDeltaLbl.setText((dailyRepDelta >= 0 ? "+" : "") + dailyRepDelta);
        eodRepDeltaLbl.applyCss();
        eodRepDeltaLbl.layout();
        double repWidth = eodRepDeltaLbl.getBoundsInLocal().getWidth();
        double repX = w - RIGHT_MARGIN - repWidth;
        eodRepDeltaLbl.setLayoutX(repX);
        eodRepDeltaLbl.setLayoutY(TRUST_ROW_Y);

        if (hubButtons != null) hubButtons.setVisible(false);
        taskPanelView.close();

        eodLayer.setVisible(true);
        eodLayer.toFront();

        // reset NPC flags
        npc.briefingShownToday = false;
        npc.npc2VisitShownToday = false;
        npc.socialiteShownToday = false;
        npc.gambledToday = false;
        npc.auditScheduledToday = false;
        npc.auditTime = null;
        if (npc.daysUntilNextAudit > 0) npc.daysUntilNextAudit--;
    }

    private void continueToNextDay() {
        if (eodLayer != null) eodLayer.setVisible(false);

        simTime = LocalTime.of(9, 0);
        clockLabel.setText(CLOCK_FMT.format(simTime));
        startWorkdayTimer();

        setBackground((Stage) clockLabel.getScene().getWindow(), HUB_IMG);
        if (hubButtons != null) hubButtons.setVisible(true);

        dailyRepDelta = 0;

        taskManager.resetForNewDay();
        events.nextDay();

        if (!npc.briefingShownToday && simTime.equals(LocalTime.of(9, 0))) {
            Timeline t = new Timeline(new KeyFrame(Duration.millis(500),
                    e -> npc.morningBrief(events.getTodaysEvents())));
            t.setCycleCount(1);
            t.play();
        }
    }

    private void goTo(Stage stage, String resourcePath, boolean showTasks) {
        setBackground(stage, resourcePath);

        boolean onHubNow = HUB_IMG.equals(resourcePath);
        onHub = onHubNow;

        if (onHubNow) {
            if (hubAmbient == null)
                hubAmbient = loopMusic("/sfx/general-chatter-in-bar-14816.mp3", Settings.getVolume());
            else hubAmbient.play();
        } else {
            if (hubAmbient != null) hubAmbient.stop();
        }

        // Only show the clickable hotspots on the hub
        hubButtons.setVisible(onHubNow);
        if (onHubNow) hubButtons.toFront();

        if (clockBox != null) clockBox.setVisible(onHubNow);
        if (cashUnderLbl != null) cashUnderLbl.setVisible(onHubNow);

        // close the tasks panel when leaving hub, optionally open if asked when on hub
        if (showTasks && onHubNow) taskPanelView.open();
        else taskPanelView.close();
    }
    private void initClock(Pane root) {
        clockLabel = new Label(CLOCK_FMT.format(simTime));
        clockLabel.setStyle("""
                -fx-font-family: Consolas, 'Courier New', monospace;
                -fx-font-size: 24;
                -fx-text-fill: #57ff89;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 8, 0.2, 0, 0);
                """);

        repLbl = new Label();
        repLbl.setStyle("-fx-text-fill: #cde8d0; -fx-font-size: 14;");

        clockBox = new VBox(2, clockLabel, repLbl);
        clockBox.setPadding(new Insets(6, 10, 6, 10));
        clockBox.setStyle("""
                -fx-background-color: rgba(10,10,12,0.75);
                -fx-border-color: #57ff89;
                -fx-border-width: 2;
                -fx-background-radius: 6;
                -fx-border-radius: 6;
                """);

        clockBox.setTranslateX(-15);
        root.getChildren().add(clockBox);
        clockBox.setLayoutY(12);
        clockBox.layoutXProperty().bind(root.widthProperty().subtract(clockBox.widthProperty()).subtract(12));
    }

    private void refreshStatus() {
        if (repLbl != null) repLbl.setText("Reputation: " + reputation);
        if (cashUnderLbl != null) cashUnderLbl.setText("Portfolio: $" + cash);
    }

    private void setBackground(Stage stage, String resourcePath) {
        try {
            URL url = Objects.requireNonNull(getClass().getResource(resourcePath),
                    "Resource '" + resourcePath + "' not found on classpath.");
            Image img = new Image(url.toExternalForm());

            bgView.setImage(img);
            bgView.setPreserveRatio(false);
            bgView.setFitWidth(0);
            bgView.setFitHeight(0);

            Pane root = (Pane) bgView.getParent();
            root.setPrefSize(img.getWidth(), img.getHeight());
            root.setMinSize(img.getWidth(), img.getHeight());
            root.setMaxSize(img.getWidth(), img.getHeight());

            if (stage != null && stage.getScene() != null) {
                stage.sizeToScene();
                stage.centerOnScreen();
            }
        } catch (NullPointerException npe) {
            new Alert(Alert.AlertType.ERROR,
                    "Could not find '" + resourcePath + "' in resources.\n" +
                            "Make sure the PNG is in your 'resources' folder.").show();
        }
    }

    private void changeReputation(int delta) {
        reputation += delta;
        dailyRepDelta += delta;
        refreshStatus();
    }

    private Button makeHotspot(double x, double y, double w, double h,
                               String ariaLabel, Runnable onClick) {
        Button b = new Button();
        b.setLayoutX(x); b.setLayoutY(y);
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
        URL url = Objects.requireNonNull(getClass().getResource(resourcePath), "Missing audio: " + resourcePath);
        MediaPlayer mp = new MediaPlayer(new Media(url.toExternalForm()));
        mp.setCycleCount(MediaPlayer.INDEFINITE);
        mp.setVolume(volume);
        mp.play();
        return mp;
    }

    private void applySettings() {
        if (adjust != null) adjust.setBrightness(Settings.getBrightness() - 1.0);
        if (hubAmbient != null) hubAmbient.setVolume(Settings.getVolume());
    }

    public static void main(String[] args) { launch(args); }
}
