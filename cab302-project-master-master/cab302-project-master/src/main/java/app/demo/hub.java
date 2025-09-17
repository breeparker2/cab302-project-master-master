package app.demo;// package java.demo.hub;  // <-- uncomment & set if this file is inside a package folder

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.Region;

/* ----------------- Helper classes (top-level, package-private) ----------------- */

final class MarketEvent {
    final String type;
    final String country;
    final String detail;
    final LocalDateTime when;
    MarketEvent(String type, String country, String detail) {
        this.type = type; this.country = country; this.detail = detail; this.when = LocalDateTime.now();
    }
    @Override public String toString() { return type + " [" + country + "]: " + detail; }
}

final class EventBus {
    private final List<Consumer<MarketEvent>> listeners = new ArrayList<>();
    void subscribe(Consumer<MarketEvent> listener) { listeners.add(listener); }
    void publish(MarketEvent evt) { Platform.runLater(() -> listeners.forEach(l -> l.accept(evt))); }
}

final class TaskManager {
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final Set<String> dayKeys = new HashSet<>();  // what we've already added today

    ObservableList<Task> getTasks() { return tasks; }

    // Call this at the START of each new in-game day
    void resetForNewDay() {
        tasks.clear();
        dayKeys.clear();
    }

    // Optional: if you want to keep finished items until end of day
    void markAllDone() { tasks.forEach(Task::markDone); tasks.setAll(new java.util.ArrayList<>(tasks)); }
    void clear() { tasks.clear(); dayKeys.clear(); }

    void onEvent(MarketEvent evt) {
        // Use a stable key so we don't add duplicates for repeats
        String key = evt.type + "|" + evt.detail;

        if (dayKeys.contains(key)) return;  // already on tablet for today

        // Make the task an action the player should take
        String label = makePlayerAction(evt);

        tasks.add(new Task(label));
        dayKeys.add(key);

        // (No auto-remove here — task persists until resetForNewDay())
        if (tasks.size() > 20) tasks.remove(0);
    }

    private String makePlayerAction(MarketEvent evt) {
        return switch (evt.type) {
            case "OPEC"      -> "Energy: adjust OIL positioning — " + evt.detail;
            case "EARNINGS"  -> "Decide BHP move on earnings — " + evt.detail;
            case "RATE_HIKE" -> "Rebalance rate-sensitive names — " + evt.detail;
            case "NEWS"      -> "Review positions affected — " + evt.detail;
            default          -> "Respond: " + evt.detail;
        };
    }
}

/* --- NPC helpers ------------------------------------------------------------- */
enum Department { BOSS, ENERGY, SOCIALITE }

final class Npc {
    final String name;
    final String imgPath;
    final Department dept;
    final double fitWidth;
    Npc(String name, String imgPath, Department dept, double fitWidth) {
        this.name = name; this.imgPath = imgPath; this.dept = dept; this.fitWidth = fitWidth;
    }
}

/* ----------------- Your app ----------------- */

public class hub extends Application {

    private static final boolean DEBUG = true;

    // background view
    private ImageView bgView;

    // java.demo.hub-only buttons group
    private Pane hubButtons;

    // tasks tablet UI + logic
    private VBox tasksPanel;
    private boolean tasksOpen = false;
    private EventBus eventBus;
    private TaskManager taskManager;

    // End-of-day overlay
    private Pane eodLayer;
    private ImageView eodView;
    private Button eodContinueBtn;
    private static final String EOD_IMG = "/day report.png";

    private static final double TRUST_ROW_Y = 500;
    private static final double RIGHT_MARGIN = 140;
    private static final int    EOD_REP_FONT_SIZE = 40;
    private Label eodRepDeltaLbl;

    // --- Workday timer (9:00 → 5:00 where 1s = 1min) ---
    private Label clockLabel;
    private VBox clockBox;
    private Timeline workdayTimer;
    private LocalTime simTime = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(17, 0);
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("h:mm a");

    // resource constants
    private static final String HUB_IMG = "/menu-1.png_3-1.png_3.png";
    private static final String COUNTRY_IMG = "/country.png";
    private static final String EVENTS_IMG = "/events.png";
    private static final String STOCKS_IMG = "/Stock.png";
    private static final String START_IMG = "/start.png";

    // java.demo.hub ambient player
    private MediaPlayer hubAmbient;

    // brightness effect for the whole java.demo.hub
    private ColorAdjust adjust;

    // ---------- Dialogue / NPC overlay ----------
    private ImageView npcView;
    private VBox dialogueBox;
    private Label speakerLbl, textLbl;
    private StackPane overlayLayer;
    private HBox dialogueContainer;
    private boolean briefingShownToday = false;

    // Choice UI
    private HBox choiceBar;
    private Button yesBtn, noBtn;

    // --- NPC registry + dialogue pools ---
    private Map<Department, Npc> npcs = new HashMap<>();
    private Map<Department, List<String>> idleLines = new HashMap<>();
    private Map<String, List<String>> eventLines = new HashMap<>();
    private final Random rng = new Random();

    private List<EventDef> todaysDefs = new ArrayList<>();
    // Track if we're on java.demo.hub; timed visits are java.demo.hub-only
    private boolean onHub = true;

    // NPC2 timed visit (11:30) and lines
    private boolean npc2VisitShownToday = false;
    private static final String[] NPC2_PITCH_LINES = {
            "I’ve got a high-beta pair that sings if vol pops. Small size, big convexity.",
            "Utilities are sleepy—rotate into cyclicals with teeth. I’ll set tight stops.",
            "Two words: earnings momentum. Let me lean in while the tape’s forgiving.",
            "Desk chatter says a squeeze is brewing. I want a flyer with defined risk.",
            "We’re under-risked. Give me ammo and I’ll hunt mispriced upside."
    };

    // --- Socialite (NPC3) party tiers ---
    private enum PartyTier { BASIC, ELITE, ULTRA }

    private static final int BASIC_REP_REQ = 0;
    private static final int ELITE_REP_REQ = 20;
    private static final int ULTRA_REP_REQ = 50;

    private static final int BASIC_COST = 50;
    private static final int ELITE_COST = 200;
    private static final int ULTRA_COST = 1000;

    private static final int BASIC_REP_GAIN = 10;
    private static final int ELITE_REP_GAIN = 30;
    private static final int ULTRA_REP_GAIN = 80;

    // --- Simple player state + socialite (3:00 PM) ---
    private int cash = 200;

    // Reputation + daily total
    private int reputation = 0;
    private int dailyRepDelta = 0;

    private Label repLbl;
    private Label cashUnderLbl;

    private static final LocalTime SOCIALITE_TIME = LocalTime.of(15, 0);
    private boolean socialiteShownToday = false;

    private long bossCooldownUntil = 0;
    private static final long BOSS_COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes
    private int bossDailyCount = 0;
    private static final int BOSS_MAX_DAILY = 3;

    private boolean gambledToday = false;            // set to true when you take NPC2's gamble
    private boolean gambledSinceLastAudit = false;   // tracks if you've gambled since the last audit
    private boolean auditScheduledToday = false;     // if an audit is queued for today
    private LocalTime auditTime = null;              // time-of-day for today's audit
    private int daysUntilNextAudit = 0;

    // --- Today’s events (for morning brief) ---
    private final List<MarketEvent> todaysEvents = new ArrayList<>();

    // ===== Backend bridge fields =====
    private Market market;
    private EventEngine engine;
    private Timeline eventBridgeTimer;
    private final Set<String> liveKeys = new HashSet<>();

    public void setTodaysEvents(List<MarketEvent> events) {
        todaysEvents.clear();
        if (events != null) todaysEvents.addAll(events);
        for (MarketEvent evt : todaysEvents) { eventBus.publish(evt); }
    }

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

        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.SPACE) {
                if (isDialogueVisible()) {
                    advanceOrCloseDialogue();
                } else {
                    SettingsMenu.show(primaryStage, this::applySettings);
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                if (isDialogueVisible()) {
                    advanceOrCloseDialogue();
                    e.consume();
                    daysUntilNextAudit = rng.nextInt(24) + 7;
                }
            }
        });

        // --- Event + task plumbing ---
        eventBus = new EventBus();
        taskManager = new TaskManager();
        eventBus.subscribe(taskManager::onEvent);
        HubGateway.register(eventBus::publish);

        // ---- Backend market + engine ----
        market = new Market();
        market.add(new Stock("iron", 12.00));
        market.add(new Stock("oil", 75.00));
        market.add(new Stock("bhp", 24.00));

        List<EventDef> defs = Arrays.asList(
                new EventDef("Trade Dispute", "Tariffs escalate; demand weak", "iron", -8.0, 10_000),
                new EventDef("OPEC Guidance", "Production guidance due",       "oil",  +5.0, 10_000),
                new EventDef("Earnings Beat", "Strong results pre-market",     "bhp",  +4.0, 10_000)
        );

        Consumer<String> logFn = s -> System.out.println("[EVENT] " + s);
        Consumer<String> toastFn = s -> {};
        Runnable refreshFn = () -> {};

        engine = new EventEngine(market, defs, new Random(), logFn, toastFn, refreshFn);
        engine.start();

        // Pick a fresh pool for *today* and load it into the engine
        rollTodaysDefs();
        // Mirror current active events (usually none at 9:00)
        refreshTasksFromEngine();

        // ---- Bridge timer: detect new/ended engine events → publish into your java.demo.EventBus
        eventBridgeTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> syncEventsToHub()));
        eventBridgeTimer.setCycleCount(Timeline.INDEFINITE);
        eventBridgeTimer.play();

        // (Optional) seed morning docket into tasks if you pre-populate todaysEvents
        taskManager.clear();
        for (MarketEvent evt : todaysEvents) taskManager.onEvent(evt);

        // --- Tasks panel (left tablet) ---
        tasksPanel = buildTasksPanel();
        tasksPanel.setLayoutX(36);
        tasksPanel.setLayoutY(470);
        root.getChildren().add(tasksPanel);

        // --- Hub buttons group ---
        hubButtons = new Pane();
        root.getChildren().add(hubButtons);

        hubButtons.getChildren().add(makeHotspot(40, 588, 80, 120, "TASKS", this::toggleTasksPanel));
        hubButtons.getChildren().add(makeHotspot(578, 520, 175, 34, "COUNTRY", () -> {
            goTo(primaryStage, COUNTRY_IMG, false);
        }));
        hubButtons.getChildren().add(makeHotspot(578, 554, 146, 34, "EVENTS", () -> {
            goTo(primaryStage, EVENTS_IMG, false);
        }));
        hubButtons.getChildren().add(makeHotspot(578, 588, 128, 34, "STOCKS", () -> {
            goTo(primaryStage, STOCKS_IMG, false);
        }));
        hubButtons.getChildren().add(makeHotspot(578, 622, 174, 34, "PORTFOLIO", () -> {
            System.out.println("Portfolio clicked");
            hubButtons.setVisible(false);
            closeTasksPanel();
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

        // ---- "Cash" label under the PORTFOLIO button ----
        cashUnderLbl = new Label();
        cashUnderLbl.setTextFill(Color.web("#57ff89"));
        cashUnderLbl.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        cashUnderLbl.setStyle("""
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 6, 0.2, 0, 0);
            -fx-font-family: Consolas, 'Courier New', monospace;
            """);

        // The PORTFOLIO hotspot is at (x=578, y=622, w=174, h=34)
        double portX = 578, portY = 622, portW = 174, portH = 34;

        // Place & center the label under the button
        cashUnderLbl.setLayoutX(portX);
        cashUnderLbl.setLayoutY(portY + portH + 6);

        // Prevent ellipsis by fixing width and clipping instead of overrun
        cashUnderLbl.setPrefWidth(portW);
        cashUnderLbl.setMinWidth(Region.USE_PREF_SIZE);
        cashUnderLbl.setMaxWidth(Region.USE_PREF_SIZE);
        cashUnderLbl.setWrapText(false);
        cashUnderLbl.setAlignment(Pos.CENTER_LEFT);
        cashUnderLbl.setTextOverrun(OverrunStyle.CLIP);

        ((Pane) bgView.getParent()).getChildren().add(cashUnderLbl);
        cashUnderLbl.toFront();

        // --- Digital clock (top-right on java.demo.hub) ---
        initClock(root);
        refreshStatus();
        startWorkdayTimer();

        // --- Dialogue/NPC overlay ---
        initMorningBriefingUI(root);

        // NPC registry & event listener
        initNpcRegistryAndDialogue();
        eventBus.subscribe(this::onMarketEventForNpc);

        // Visible on java.demo.hub
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
        if (!briefingShownToday && simTime.equals(LocalTime.of(9, 0))) {
            Timeline t = new Timeline(new KeyFrame(Duration.millis(400), e -> startBriefing()));
            t.setCycleCount(1);
            t.play();
        }
    }

    /* ---------------------- Bridge: java.demo.EventEngine → java.demo.EventBus ---------------------- */

    private void syncEventsToHub() {
        Set<String> now = new HashSet<>();
        for (EventEngine.ActiveEvent ae : engine.getActiveEvents()) {
            String key = ae.def.stockCode + "|" + ae.def.title;
            now.add(key);
            if (!liveKeys.contains(key)) {
                MarketEvent evt = mapToMarketEvent(ae.def);
                eventBus.publish(evt);
                if (todaysEvents.size() < 5) todaysEvents.add(evt);
            }
        }
        liveKeys.clear();
        liveKeys.addAll(now);

        // Rebuild tasks from the *current* active events (adds & removes)
        refreshTasksFromEngine();
    }

    private MarketEvent mapToMarketEvent(EventDef def) {
        String title = def.title.toLowerCase();
        String type;

        if (title.contains("pipeline") || title.contains("refinery") ||
                title.contains("throughput") || title.contains("outage")) {
            type = "OIL_SUPPLY";
        } else if (title.contains("opec") || def.stockCode.equalsIgnoreCase("oil")) {
            type = "OPEC";
        } else if (title.contains("rate") || title.contains("hike")) {
            type = "RATE_HIKE";
        } else if (title.contains("earning") || title.contains("beat")) {
            type = "EARNINGS";
        } else {
            type = "NEWS";
        }

        String detail = def.title + " (" + def.stockCode.toUpperCase() + ", " +
                (def.shiftPct >= 0 ? "+" : "") + String.format("%.2f", def.shiftPct) + "%)";
        return new MarketEvent(type, "GLOBAL", detail);
    }

    /* ---------------------- Workday timer & EOD ---------------------- */

    private void startWorkdayTimer() {
        workdayTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!briefingShownToday && simTime.equals(LocalTime.of(9, 0))) startBriefing();

            if (onHub && !npc2VisitShownToday && simTime.equals(LocalTime.of(11, 30))) {
                startNpc2Pitch();
            }

            if (onHub && !socialiteShownToday && simTime.equals(SOCIALITE_TIME) && !isDialogueVisible()) {
                showSocialInvite();
            }

            // === Anti-Corruption audit checks ===
            if (!auditScheduledToday && daysUntilNextAudit <= 0 && npc2VisitShownToday && gambledToday) {
                scheduleAuditForToday();
            }

            if (auditScheduledToday && simTime.equals(auditTime) && !isDialogueVisible()) {
                showAntiCorruptionAudit();
            }

            // Advance the clock
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

        // daily resets happen here
        briefingShownToday = false;
        npc2VisitShownToday = false;
        socialiteShownToday = false;

        // reset boss limits for the next day
        bossDailyCount = 0;
        bossCooldownUntil = 0;

        URL url = Objects.requireNonNull(getClass().getResource(EOD_IMG),
                "Resource '" + EOD_IMG + "' not found.");
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
        closeTasksPanel();

        eodLayer.setVisible(true);
        eodLayer.toFront();

        briefingShownToday = false;
        npc2VisitShownToday = false;
        socialiteShownToday = false;
    }

    private void continueToNextDay() {
        if (eodLayer != null) eodLayer.setVisible(false);

        simTime = LocalTime.of(9, 0);
        clockLabel.setText(CLOCK_FMT.format(simTime));
        startWorkdayTimer();

        setBackground((Stage) clockLabel.getScene().getWindow(), HUB_IMG);
        if (hubButtons != null) hubButtons.setVisible(true);

        briefingShownToday = false;
        npc2VisitShownToday = false;
        socialiteShownToday = false;
        dailyRepDelta = 0;

        bossDailyCount = 0;
        bossCooldownUntil = 0;

        // ✅ Daily resets for the gambling/audit system
        gambledToday = false;
        auditScheduledToday = false;
        auditTime = null;

        // ✅ Move the audit window forward one day
        if (daysUntilNextAudit > 0) daysUntilNextAudit--;

        // Start new day with a clean tablet
        taskManager.resetForNewDay();

        setTodaysEvents(List.of(
                new MarketEvent("EARNINGS", "US", "MegaCap results pre-market")
        ));

        if (!briefingShownToday && simTime.equals(LocalTime.of(9, 0))) {
            Timeline t = new Timeline(new KeyFrame(Duration.millis(500), e -> startBriefing()));
            t.setCycleCount(1);
            t.play();
            engine.resetDaily();
        }
    }

    /* ---------------------- Screen switching ---------------------- */

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
            if (isDialogueVisible()) advanceOrCloseDialogue();
        }

        hubButtons.setVisible(onHubNow);
        if (clockBox != null) clockBox.setVisible(onHubNow);
        if (cashUnderLbl != null) cashUnderLbl.setVisible(onHubNow);

        if (showTasks && onHubNow) openTasksPanel();
        else closeTasksPanel();
    }

    /* ---------------------- Clock helpers ---------------------- */

    private void initClock(Pane root) {
        clockLabel = new Label(CLOCK_FMT.format(simTime));
        clockLabel.setStyle("""
                -fx-font-family: Consolas, 'Courier New', monospace;
                -fx-font-size: 24;
                -fx-text-fill: #57ff89;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 8, 0.2, 0, 0);
                """);

        repLbl  = new Label();
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
       // if (cashLbl != null) cashLbl.setText("Cash: $" + cash);
        if (repLbl  != null) repLbl.setText("Reputation: " + reputation);
        if (cashUnderLbl != null) cashUnderLbl.setText("Portfolio: $" + cash);
    }

    /* ---------------------- Background swapping ---------------------- */

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

    /* ---------------------- Tasks tablet UI ---------------------- */

    private VBox buildTasksPanel() {
        VBox panel = new VBox(6);
        panel.setPadding(new Insets(8));
        panel.setPrefSize(220, 230);
        panel.setMinSize(180, 200);
        panel.setStyle("""
                -fx-background-color: rgba(15,15,20,0.92);
                -fx-border-color: #57ff89;
                -fx-border-width: 2;
                -fx-background-radius: 4;
                -fx-border-radius: 4;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0.0, 0, 4);
                """);

        Label title = new Label("Tasks");
        title.setStyle("-fx-text-fill: #57ff89; -fx-font-weight: bold; -fx-font-size: 12;");

        ListView<Task> list = new ListView<>(taskManager.getTasks());
        list.setStyle("-fx-control-inner-background: #111;");
        VBox.setVgrow(list, Priority.ALWAYS);

        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setTextFill(Color.web("#cde8d0"));
                setStyle("-fx-background-color: #111; -fx-padding: 2 6 2 6; -fx-wrap-text: true;");
                prefWidthProperty().bind(list.widthProperty().subtract(18));
                setMaxWidth(Double.MAX_VALUE);
            }
        });

        ScrollBar miniScroll = new ScrollBar();
        miniScroll.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        miniScroll.setPrefHeight(10);
        miniScroll.setMin(0);
        miniScroll.setMax(Math.max(0, list.getItems().size() - 1));
        miniScroll.setUnitIncrement(1);
        miniScroll.setBlockIncrement(3);

        miniScroll.valueProperty().addListener((obs, oldV, newV) -> list.scrollTo(newV.intValue()));
        taskManager.getTasks().addListener((ListChangeListener<Task>) c ->
                miniScroll.setMax(Math.max(0, list.getItems().size() - 1))
        );

        Button ok = new Button("OK");
        ok.setOnAction(e -> closeTasksPanel());

        HBox bottom = new HBox(8, miniScroll, ok);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(miniScroll, Priority.ALWAYS);

        panel.setVisible(false);
        panel.setTranslateX(180);

        panel.getChildren().addAll(title, list, bottom);
        return panel;
    }

    private void openTasksPanel() {
        tasksPanel.setVisible(true);
        tasksPanel.setMouseTransparent(false);
        tasksPanel.toFront();
        if (hubButtons != null) hubButtons.setMouseTransparent(true);

        TranslateTransition tt = new TranslateTransition(Duration.millis(220), tasksPanel);
        tt.setFromX(tasksPanel.getTranslateX());
        tt.setToX(0);
        tt.setOnFinished(e -> tasksOpen = true);
        tt.play();
    }

    private void closeTasksPanel() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(180), tasksPanel);
        tt.setFromX(tasksPanel.getTranslateX());
        tt.setToX(180);
        tt.setOnFinished(e -> {
            tasksPanel.setVisible(false);
            tasksPanel.setMouseTransparent(true);
            tasksOpen = false;
            if (hubButtons != null) {
                hubButtons.setMouseTransparent(false);
                hubButtons.toFront();
            }
        });
        tt.play();
    }

    private void refreshTasksFromEngine() {
        taskManager.clear();
        for (EventEngine.ActiveEvent ev : engine.getActiveEvents()) {
            String label = ev.def.title + " (" + ev.def.stockCode.toUpperCase() + ")";
            taskManager.getTasks().add(new Task(label));
        }
    }

    // at java.demo.start-of-day pick the pool for *today* and (re)load the engine
    private void rollTodaysDefs() {
        List<EventDef> all = Arrays.asList(
                new EventDef("Trade Dispute",   "Tariffs escalate; demand weak", "iron", -8.0, 10_000),
                new EventDef("OPEC Guidance",   "Production guidance due",        "oil",  +5.0, 10_000),
                new EventDef("Earnings Beat",   "Strong results pre-market",      "bhp",  +4.0, 10_000),
                new EventDef("Pipeline Snag",   "Throughput cut weighs on supply","oil",  -3.0, 10_000),
                new EventDef("China Stimulus",  "Infrastructure push lifting steel","iron",+6.0, 10_000),
                new EventDef("Mine Safety Audit","Temporary halt; output risk",   "bhp",  -4.0, 10_000)
        );

        Collections.shuffle(all, rng);
        todaysDefs = new ArrayList<>(all.subList(0, Math.min(3, all.size()))); // any 3

        if (engine != null) {
            engine.setEventDefs(todaysDefs);
            engine.resetDaily();
        }
    }

    private void toggleTasksPanel() { if (tasksOpen) closeTasksPanel(); else openTasksPanel(); }

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

    // loop audio
    private MediaPlayer loopMusic(String resourcePath, double volume) {
        URL url = Objects.requireNonNull(getClass().getResource(resourcePath),
                "Missing audio: " + resourcePath);
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

    /* ===================== Dialogue UI & Logic ===================== */

    private void initMorningBriefingUI(Pane root) {
        overlayLayer = new StackPane();
        overlayLayer.setPickOnBounds(false);
        overlayLayer.prefWidthProperty().bind(root.widthProperty());
        overlayLayer.prefHeightProperty().bind(root.heightProperty());
        root.getChildren().add(overlayLayer);

        dialogueContainer = new HBox(0);
        dialogueContainer.setAlignment(Pos.TOP_LEFT);
        dialogueContainer.setVisible(false);
        dialogueContainer.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);

        npcView = new ImageView(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/npc1.png"),
                        "Missing /npc1.png in resources")));
        npcView.setPreserveRatio(true);
        npcView.setFitWidth(500);
        npcView.setVisible(true);

        dialogueBox = new VBox(6);
        dialogueBox.setPadding(new Insets(10));
        dialogueBox.setStyle("-fx-background-color: #c6ad7b; -fx-border-color: #2a2622; -fx-border-width: 2px;");
        dialogueBox.setPrefWidth(300);
        dialogueBox.setMaxWidth(300);
        dialogueBox.setMaxHeight(VBox.USE_PREF_SIZE);
        HBox.setMargin(dialogueBox, new Insets(0, 0, 30, -80));
        dialogueBox.setTranslateX(-80);
        dialogueBox.setTranslateY(40);

        speakerLbl = new Label();
        speakerLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a2622; -fx-font-size: 18px;");
        textLbl = new Label();
        textLbl.setWrapText(true);
        textLbl.setStyle("-fx-text-fill: #2a2622; -fx-font-size: 16px;");

        choiceBar = new HBox(8);
        choiceBar.setAlignment(Pos.CENTER_RIGHT);
        yesBtn = new Button("Go ($50)");
        noBtn  = new Button("Maybe later");
        choiceBar.getChildren().addAll(noBtn, yesBtn);
        choiceBar.setVisible(false);

        dialogueBox.getChildren().addAll(speakerLbl, textLbl, choiceBar);
        dialogueContainer.getChildren().addAll(npcView, dialogueBox);

        StackPane.setAlignment(dialogueContainer, Pos.BOTTOM_LEFT);
        StackPane.setMargin(dialogueContainer, new Insets(0, 0, 130, -5));
        overlayLayer.getChildren().add(dialogueContainer);

        overlayLayer.setOnMouseClicked(e -> {
            if (isDialogueVisible() && !choiceBar.isVisible()) advanceOrCloseDialogue();
        });
    }

    private boolean isDialogueVisible() { return dialogueContainer != null && dialogueContainer.isVisible(); }

    private void startBriefing() {
        if (briefingShownToday) return;
        briefingShownToday = true;

        Npc boss = npcs.get(Department.BOSS);

        String hint;
        if (todaysEvents.isEmpty()) {
            hint = "Calendar looks quiet, but keep your board close.";
        } else {
            StringBuilder sb = new StringBuilder("On the docket: ");
            int shown = 0;
            for (MarketEvent evt : todaysEvents) {
                if (shown > 0) sb.append(", ");
                sb.append(switch (evt.type) {
                    case "RATE_HIKE" -> "rate chatter (" + evt.country + ")";
                    case "OPEC"      -> "OPEC signals";
                    case "EARNINGS"  -> "earnings (" + evt.detail + ")";
                    case "NEWS"      -> "headline risk (" + evt.country + ")";
                    default          -> evt.type.toLowerCase();
                });
                shown++;
                if (shown == 3) break;
            }
            if (todaysEvents.size() > 3) sb.append(", …");
            sb.append(".");
            hint = sb.toString();
        }

        String text = "Morning. Big day on the tape. " + hint + " Keep the gossip column close.";
        showNpcDialogue(boss, text);
    }

    private void showNpcDialogue(Npc npc, String text) {
        npcView.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(npc.imgPath),
                "Missing " + npc.imgPath + " in resources")));
        npcView.setFitWidth(npc.fitWidth);

        speakerLbl.setText(npc.name);
        textLbl.setText(text);
        choiceBar.setVisible(false);

        dialogueContainer.setVisible(true);
        if (hubButtons != null) hubButtons.setMouseTransparent(true);
    }

    private void advanceOrCloseDialogue() {
        dialogueContainer.setVisible(false);
        choiceBar.setVisible(false);
        if (npcView != null) {
            Timeline t = new Timeline(
                    new KeyFrame(Duration.millis(0), e -> npcView.setOpacity(1)),
                    new KeyFrame(Duration.millis(300), e -> npcView.setOpacity(0))
            );
            t.setOnFinished(e -> npcView.setOpacity(1));
            t.play();
        }
        if (hubButtons != null) {
            hubButtons.setMouseTransparent(false);
            hubButtons.toFront();
        }
    }

    /* ===== NPC system ===== */

    private void initNpcRegistryAndDialogue() {
        npcs.put(Department.BOSS, new Npc("Floor Boss", "/npc1.png", Department.BOSS, 500));
        npcs.put(Department.ENERGY, new Npc("Energy Analyst", "/npc2.png", Department.ENERGY, 500));
        npcs.put(Department.SOCIALITE, new Npc("Socialite Secretary", "/npc3.png", Department.SOCIALITE, 500));

        idleLines.put(Department.BOSS, List.of(
                "Keep an eye on tape speed—it’s picking up.",
                "Liquidity is thin. Don’t chase—make it come to you."
        ));
        idleLines.put(Department.ENERGY, List.of(
                "Refinery margins look fat this week.",
                "Crack spread’s telling a story—watch diesel."
        ));

        eventLines.put("OPEC", List.of(
                "Cartel chatter again. Watch front-month and refiners.",
                "Production signals are messy—expect whipsaw in crude."
        ));
        eventLines.put("RATE_HIKE", List.of(
                "Higher rates bite growth—fade froth on the open.",
                "Financials first, then credit—position accordingly."
        ));
        eventLines.put("EARNINGS", List.of(
                "Guidance beats matter more than EPS today.",
                "Watch gross margin commentary—tells you the cycle."
        ));
    }

    private void onMarketEventForNpc(MarketEvent evt) {
        if (!onHub || isDialogueVisible()) return;

        long now = System.currentTimeMillis();

        // Only apply cooldown/daily limits for the boss
        if (evt.type.equals("OPEC") || evt.type.equals("EARNINGS") || evt.type.equals("RATE_HIKE")) {
            if (bossDailyCount >= BOSS_MAX_DAILY || now < bossCooldownUntil) return;
            bossDailyCount++;
            bossCooldownUntil = now + BOSS_COOLDOWN_MS;
        }

        // More useful dialogue lines with details
        String line = switch (evt.type) {
            case "EARNINGS"  -> "Earnings in: " + evt.detail;
            case "RATE_HIKE" -> "Rates chatter: " + evt.detail;
            case "OPEC"      -> "Oil watch: " + evt.detail;
            default          -> "News just hit: " + evt.detail;
        };

        Npc npc = switch (evt.type) {
            case "OPEC"      -> npcs.get(Department.ENERGY);
            default          -> npcs.get(Department.BOSS);
        };

        showNpcDialogue(npc, line);
    }

    // --- NPC2 Gambling profiles ---
    private enum RiskTier { SAFE, EVEN, WILD }

    private static final class BetProfile {
        final String name;
        final double winProb;     // chance to win
        final double payoutMult;  // total payout relative to stake; net gain = (payoutMult - 1) * stake
        BetProfile(String name, double winProb, double payoutMult) {
            this.name = name; this.winProb = winProb; this.payoutMult = payoutMult;
        }
    }

    // House-leaning but fun: SAFE has slight edge cost, WILD has fat upside at lower odds
    private static final Map<RiskTier, BetProfile> BETS = Map.of(
            RiskTier.SAFE, new BetProfile("SAFE", 0.55, 1.80),  // ~55% to get 1.8x (net +0.8x)
            RiskTier.EVEN, new BetProfile("EVEN", 0.50, 2.00),  // 50% for 2x (classic coin flip)
            RiskTier.WILD, new BetProfile("WILD", 0.33, 3.00)   // ~33% for 3x (swingy)
    );

    // Stake options we’ll cycle through; we’ll cap by cash at runtime
    private static final int[] STAKE_OPTIONS = {10, 25, 50, 100, 200, 500};

    private void startNpc2Pitch() {
        if (!onHub || npc2VisitShownToday) return;
        npc2VisitShownToday = true;

        // make sure the two-button bar is alive and clickable
        resetChoiceBarForTwoOptions(); // <- add this helper if you haven’t already (shown earlier)

        Npc npc2 = npcs.get(Department.ENERGY);

        // simple 50/50 gamble
        final int stake   = 100;   // cost to play
        final int winGain = 200;   // amount you receive on a win (net profit = +100)
        final int repWin  = 2;     // reputation delta on win
        final int repLoss = -2;    // reputation delta on loss
        final double winProb = 0.50;

        npcView.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(npc2.imgPath),
                "Missing " + npc2.imgPath + " in resources")));
        npcView.setFitWidth(npc2.fitWidth);

        speakerLbl.setText(npc2.name);
        textLbl.setText(
                "Here’s a play: double down on whispers. Put $" + stake + " on the line. " +
                        "Win and clear $" + winGain + " (+2 rep). Lose and you’ll take the heat (-2 rep). Roll with it?"
        );

        yesBtn.setText("Take the gamble ($" + stake + ")");
        noBtn.setText("Pass");

        // fresh handlers
        yesBtn.setOnAction(e -> {
            System.out.println("[NPC2] Gamble clicked. cash=" + cash + ", rep=" + reputation);

            if (cash < stake) {
                changeReputation(-2);
                textLbl.setText("No cash? The street eats hesitation. (-2 rep)");
            } else {
                // record that you gambled today (for audits)
                gambledToday = true;
                gambledSinceLastAudit = true;

                cash -= stake;
                boolean win = rng.nextDouble() < winProb;
                if (win) {
                    cash += winGain;
                    changeReputation(repWin);
                    textLbl.setText("Bang on. Quick cash, quick clout. (+2 rep)");
                    System.out.println("[NPC2] WIN: +$" + (winGain - stake) + ", rep +" + repWin +
                            " -> cash=" + cash + ", rep=" + reputation);
                } else {
                    changeReputation(repLoss);
                    textLbl.setText("The tape turned against us. That’s the game. (-2 rep)");
                    System.out.println("[NPC2] LOSS: -$" + stake + ", rep " + repLoss +
                            " -> cash=" + cash + ", rep=" + reputation);
                }
            }

            refreshStatus();                  // updates “Portfolio: $ ..."and top-right rep
            choiceBar.setVisible(false);      // hide buttons so you don’t double-click
            Timeline t = new Timeline(new KeyFrame(Duration.seconds(5), ev -> advanceOrCloseDialogue()));
            t.setCycleCount(1);
            t.play();
        });

        noBtn.setOnAction(e -> {
            changeReputation(-2);
            textLbl.setText("Safe play. But safe doesn’t get remembered. (-2 rep)");
            refreshStatus();
            choiceBar.setVisible(false);
            yesBtn.setDisable(true);
            noBtn.setDisable(true);
            Timeline t = new Timeline(new KeyFrame(Duration.seconds(4), ev -> advanceOrCloseDialogue()));
            t.setCycleCount(1);
            t.play();
        });

        choiceBar.setVisible(true);
        choiceBar.setDisable(false);
        choiceBar.setMouseTransparent(false);
        choiceBar.toFront();

        dialogueContainer.setVisible(true);
        if (hubButtons != null) hubButtons.setMouseTransparent(true);
    }





    private PartyTier determinePartyOffer() {
        if (reputation >= ULTRA_REP_REQ && cash >= ULTRA_COST) return PartyTier.ULTRA;
        if (reputation >= ELITE_REP_REQ && cash >= ELITE_COST) return PartyTier.ELITE;
        return PartyTier.BASIC;
    }

    private void changeReputation(int delta) {
        reputation += delta;
        dailyRepDelta += delta;
        refreshStatus();
    }

    // --- Socialite invite (3:00 PM), tiered by Reputation + Portfolio ---
// --- Socialite invite (3:00 PM), tiered by Reputation + Portfolio ---
    private void showSocialInvite() {
        socialiteShownToday = true;
        resetChoiceBarForTwoOptions();

        Npc npc = npcs.get(Department.SOCIALITE);
        npcView.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(npc.imgPath),
                "Missing " + npc.imgPath + " in resources")));
        npcView.setFitWidth(npc.fitWidth);

        // Decide which party tier to offer
        PartyTier tier = determinePartyOffer();

        final int cost;
        final int repGain;
        final String pitch;

        switch (tier) {
            case ULTRA -> {
                cost = ULTRA_COST;
                repGain = ULTRA_REP_GAIN;
                pitch = "I’m curating something exquisite tonight — very private, very selective. "
                        + "Captains of industry, a couple of ministers, and an art auction that might get spicy. "
                        + "Entry is $" + cost + ". Interested?";
            }
            case ELITE -> {
                cost = ELITE_COST;
                repGain = ELITE_REP_GAIN;
                pitch = "Closed-door reception tonight — boutique funds and a few journalists you’ll want to know. "
                        + "Cover’s $" + cost + ". Shall I put your name down?";
            }
            default -> {
                cost = BASIC_COST;           // BASIC_COST == 10
                repGain = BASIC_REP_GAIN;    // BASIC_REP_GAIN == 10
                pitch = "Hey—fancy a little soirée after work? Decent crowd, good introductions. "
                        + "It’s $" + cost + " to get in. Interested?";
            }
        }

        speakerLbl.setText(npc.name);
        textLbl.setText(pitch);

        // Prevent handler stacking
        yesBtn.setOnAction(null);
        noBtn.setOnAction(null);

        // Button labels reflect the active tier cost
        yesBtn.setText("Go ($" + cost + ")");
        noBtn.setText("Maybe later");

        yesBtn.setOnAction(e -> {
            if (cash >= cost) {
                cash -= cost;
                changeReputation(repGain);
                switch (tier) {
                    case ULTRA -> textLbl.setText("Fabulous choice. Expect a few doors to open tomorrow.");
                    case ELITE -> textLbl.setText("Excellent — keep your elevator pitch tight.");
                    default -> textLbl.setText("Lovely! You’re on the list. Let’s make sure the right people learn your name.");
                }
            } else {
                changeReputation(-5);  // small sting for not having funds
                textLbl.setText("Oh… you don’t have it? That’s a shame. People notice when you can’t make it.");
            }
            refreshStatus();
            choiceBar.setVisible(false);
            Timeline t = new Timeline(new KeyFrame(Duration.seconds(5), ev -> advanceOrCloseDialogue()));
            t.setCycleCount(1);
            t.play();
        });

        noBtn.setOnAction(e -> {
            changeReputation(-10);  // always -10 when declining
            textLbl.setText("Another time, then. The rooms that matter don’t stay open forever.");
            refreshStatus();
            choiceBar.setVisible(false);
            yesBtn.setDisable(true);
            noBtn.setDisable(true);
            Timeline t = new Timeline(new KeyFrame(Duration.seconds(5), ev -> advanceOrCloseDialogue()));
            t.setCycleCount(1);
            t.play();
        });

        choiceBar.setVisible(true);
        dialogueContainer.setVisible(true);
        if (hubButtons != null) hubButtons.setMouseTransparent(true);
    }

    private void scheduleAuditForToday() {
        int startMin = 12 * 60;                 // 12:00
        int endMin   = 16 * 60 + 30;            // 4:30 PM
        int minute   = startMin + rng.nextInt(Math.max(1, endMin - startMin));
        auditTime = LocalTime.of(minute / 60, minute % 60);
        auditScheduledToday = true;
    }

    // Show the audit dialogue and apply penalties if you've gambled since the last audit
    private void showAntiCorruptionAudit() {
        Npc boss = npcs.get(Department.BOSS);
        npcView.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(boss.imgPath),
                "Missing " + boss.imgPath + " in resources")));
        npcView.setFitWidth(boss.fitWidth);

        speakerLbl.setText("Compliance (via " + boss.name + ")");
        if (gambledSinceLastAudit) {
            int penalty = -6;
            changeReputation(penalty);
            textLbl.setText("Spot check. Unusual risk signatures around your book. "
                    + "We expect prudence. (Reputation " + penalty + ")");
        } else {
            textLbl.setText("Routine compliance sweep. Keep your positions tidy.");
        }

        // Prepare a single, enabled "Understood" button
        prepChoiceBarSingle("Understood");

        // Show overlay
        dialogueContainer.setVisible(true);
        if (hubButtons != null) hubButtons.setMouseTransparent(true);

        yesBtn.setOnAction(e -> {
            // Hide bar and close immediately
            choiceBar.setVisible(false);
            advanceOrCloseDialogue();

            // Restore normal 2-button layout for the next scene
            resetChoiceBarForTwoOptions();
        });

        // Consume today's audit
        gambledSinceLastAudit = false;
        if (daysUntilNextAudit <= 0) {
            daysUntilNextAudit = rng.nextInt(24) + 7; // next window in 7–30 days
        }
        auditScheduledToday = false;
        auditTime = null;
    }


    private void resetChoiceBarForTwoOptions() {
        yesBtn.setDisable(false);
        noBtn.setDisable(false);
        choiceBar.setDisable(false);
        choiceBar.setMouseTransparent(false);
        if (choiceBar.getChildren().size() != 2 || choiceBar.getChildren().get(0) != noBtn) {
            choiceBar.getChildren().setAll(noBtn, yesBtn);
        }
        yesBtn.setOnAction(null);
        noBtn.setOnAction(null);
    }

    private void prepChoiceBarSingle(String yesLabel) {
        yesBtn.setText(yesLabel);
        yesBtn.setDisable(false);
        noBtn.setDisable(true);
        yesBtn.setOnAction(null);
        noBtn.setOnAction(null);
        choiceBar.getChildren().setAll(yesBtn);
        choiceBar.setDisable(false);
        choiceBar.setMouseTransparent(false);
        choiceBar.setVisible(true);
        choiceBar.toFront();
    }

    private String pickRandom(List<String> choices) {
        if (choices == null || choices.isEmpty()) return "";
        return choices.get(rng.nextInt(choices.size()));
    }

    public static void main(String[] args) { launch(args); }
}
