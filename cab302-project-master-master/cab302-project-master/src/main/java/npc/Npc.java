package npc;

import events.MarketEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * All NPC types, dialogue UI, and NPC-related game logic
 * are centralized here so hub.java can stay slim.
 */
public final class Npc {


    public enum Department { BOSS, ENERGY, SOCIALITE }
    public enum PartyTier   { BASIC, ELITE, ULTRA }

    // simple data for a single NPC
    public static final class Info {
        public final String name;
        public final String imgPath;
        public final Department dept;
        public final double fitWidth;
        public Info(String name, String imgPath, Department dept, double fitWidth) {
            this.name = name; this.imgPath = imgPath; this.dept = dept; this.fitWidth = fitWidth;
        }
    }

    //  Controller (UI + behavior)
    /** Owns all NPC UI, lines, and event logic. */
    public static final class Controller {

        // UI
        private ImageView npcView;
        private VBox      dialogueBox;
        private Label     speakerLbl, textLbl;
        private StackPane overlayLayer;
        private HBox      dialogueContainer;
        private HBox      choiceBar;
        private Button    yesBtn, noBtn;

        // external UI we need to poke
        private Pane hubButtons;

        // state/registry
        private final Map<Department, Info> npcs = new HashMap<>();
        private final Map<Department, List<String>> idleLines = new HashMap<>();
        private final Map<String, List<String>> eventLines    = new HashMap<>();
        private final Random rng = new Random();

        // day flags
        public boolean briefingShownToday   = false;
        public boolean npc2VisitShownToday  = false;
        public boolean socialiteShownToday  = false;

        // audit state
        public boolean gambledToday = false;
        public boolean gambledSinceLastAudit = false;
        public boolean auditScheduledToday = false;
        public LocalTime auditTime = null;
        public int daysUntilNextAudit = 0;

        // constants copied from hub.java
        public static final int  BASIC_REP_REQ = 0, ELITE_REP_REQ = 20, ULTRA_REP_REQ = 50;
        public static final int  BASIC_COST = 50, ELITE_COST = 200, ULTRA_COST = 1000;
        public static final int  BASIC_REP_GAIN = 10, ELITE_REP_GAIN = 30, ULTRA_REP_GAIN = 80;


        public Controller() { }

        public void initUI(Pane root, Pane hubButtons) {
            this.hubButtons = hubButtons;

            overlayLayer = new StackPane();
            overlayLayer.setPickOnBounds(false);
            overlayLayer.prefWidthProperty().bind(root.widthProperty());
            overlayLayer.prefHeightProperty().bind(root.heightProperty());
            root.getChildren().add(overlayLayer);

            dialogueContainer = new HBox(0);
            dialogueContainer.setAlignment(Pos.TOP_LEFT);
            dialogueContainer.setVisible(false);
            dialogueContainer.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);

            npcView = new ImageView(load("/npc1.png"));
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
            yesBtn = new Button("Yes");
            noBtn  = new Button("No");
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

        public void initRegistryAndDialogue() {
            npcs.put(Department.BOSS,      new Info("Floor Boss", "/npc1.png", Department.BOSS, 500));
            npcs.put(Department.ENERGY,    new Info("Energy Analyst", "/npc2.png", Department.ENERGY, 500));
            npcs.put(Department.SOCIALITE, new Info("Socialite Secretary", "/npc3.png", Department.SOCIALITE, 500));

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

        //  Public helpers the hub calls on timer ticks

        public boolean isDialogueVisible() { return dialogueContainer != null && dialogueContainer.isVisible(); }

        public void morningBrief(List<MarketEvent> todaysEvents) {
            if (briefingShownToday) return;
            briefingShownToday = true;

            Info boss = npcs.get(Department.BOSS);

            String hint;
            if (todaysEvents == null || todaysEvents.isEmpty()) {
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
                    shown++; if (shown == 3) break;
                }
                if (todaysEvents.size() > 3) sb.append(", …");
                sb.append(".");
                hint = sb.toString();
            }
            showNpcDialogue(boss, "Morning. Big day on the tape. " + hint + " Keep the gossip column close.");
        }

        public void energyGamble(IntSupplier getCash,
                                 IntConsumer setCash,
                                 IntConsumer changeRep,
                                 Runnable refreshStatus) {
            if (npc2VisitShownToday) return;
            npc2VisitShownToday = true;

            final int stake   = 100;
            final int winGain = 200;
            final int repWin  = 2;
            final int repLoss = -2;
            final double winProb = 0.50;

            Info npc2 = npcs.get(Department.ENERGY);
            setNpcImage(npc2);
            speakerLbl.setText(npc2.name);
            textLbl.setText("Here’s a play: double down on whispers. Put $" + stake +
                    " on the line. Win and clear $" + winGain + " (+2 rep). Lose and you’ll take the heat (-2 rep). Roll with it?");
            setTwoButtons("Pass", "Take the gamble ($" + stake + ")");

            yesBtn.setOnAction(e -> {
                int cash = getCash.getAsInt();
                if (cash < stake) {
                    changeRep.accept(-2);
                    textLbl.setText("No cash? The street eats hesitation. (-2 rep)");
                } else {
                    gambledToday = true;
                    gambledSinceLastAudit = true;

                    setCash.accept(cash - stake);
                    boolean win = rng.nextDouble() < winProb;
                    if (win) {
                        setCash.accept(getCash.getAsInt() + winGain);
                        changeRep.accept(repWin);
                        textLbl.setText("Bang on. Quick cash, quick clout. (+2 rep)");
                    } else {
                        changeRep.accept(repLoss);
                        textLbl.setText("The tape turned against us. That’s the game. (-2 rep)");
                    }
                }
                refreshStatus.run();
                choiceBar.setVisible(false);
                new Timeline(new KeyFrame(Duration.seconds(5), ev -> advanceOrCloseDialogue())).play();
            });

            noBtn.setOnAction(e -> {
                changeRep.accept(-2);
                textLbl.setText("Safe play.  (-2 rep)");
                refreshStatus.run();
                choiceBar.setVisible(false);
                yesBtn.setDisable(true);
                noBtn.setDisable(true);
                new Timeline(new KeyFrame(Duration.seconds(4), ev -> advanceOrCloseDialogue())).play();
            });

            choiceBar.setVisible(true);
            dialogueContainer.setVisible(true);
            if (hubButtons != null) hubButtons.setMouseTransparent(true);
        }

        public PartyTier determinePartyOffer(int reputation, int cash) {
            if (reputation >= ULTRA_REP_REQ && cash >= ULTRA_COST) return PartyTier.ULTRA;
            if (reputation >= ELITE_REP_REQ && cash >= ELITE_COST) return PartyTier.ELITE;
            return PartyTier.BASIC;
        }

        public void socialInvite(int reputation,
                                 IntSupplier getCash,
                                 IntConsumer setCash,
                                 IntConsumer changeRep,
                                 Runnable refreshStatus) {
            socialiteShownToday = true;

            Info npc = npcs.get(Department.SOCIALITE);
            setNpcImage(npc);

            PartyTier tier = determinePartyOffer(reputation, getCash.getAsInt());

            final int cost;
            final int repGain;
            final String pitch;

            switch (tier) {
                case ULTRA -> {
                    cost = ULTRA_COST; repGain = ULTRA_REP_GAIN;
                    pitch = "I’m curating something exquisite tonight — very private, very selective. "
                            + "Captains of industry, a couple of ministers, and an art auction that might get spicy. "
                            + "Entry is $" + cost + ". Interested?";
                }
                case ELITE -> {
                    cost = ELITE_COST; repGain = ELITE_REP_GAIN;
                    pitch = "Closed-door reception tonight — boutique funds and a few journalists you’ll want to know. "
                            + "Cover’s $" + cost + ". Shall I put your name down?";
                }
                default -> {
                    cost = BASIC_COST; repGain = BASIC_REP_GAIN;
                    pitch = "Hey fancy a little soirée after work? Decent crowd, you'll get to introduction yourself to some big names. "
                            + "It’s $" + cost + " to get in. Interested?";
                }
            }

            speakerLbl.setText(npc.name);
            textLbl.setText(pitch);

            yesBtn.setOnAction(null);
            noBtn.setOnAction(null);

            yesBtn.setText("Go ($" + cost + ")");
            noBtn.setText("Maybe later");

            yesBtn.setOnAction(e -> {
                int cash = getCash.getAsInt();
                if (cash >= cost) {
                    setCash.accept(cash - cost);
                    changeRep.accept(repGain);
                    textLbl.setText(switch (tier) {
                        case ULTRA -> "Fabulous choice.";
                        case ELITE -> "Excellent!.";
                        default    -> "Lovely! You’re on the list.";
                    });
                } else {
                    changeRep.accept(-5);
                    textLbl.setText("Oh… you don’t have the money? That’s a shame. People will notice.");
                }
                refreshStatus.run();
                choiceBar.setVisible(false);
                new Timeline(new KeyFrame(Duration.seconds(5), ev -> advanceOrCloseDialogue())).play();
            });

            noBtn.setOnAction(e -> {
                changeRep.accept(-10);
                textLbl.setText("Oh, what a shame. Another time, then.");
                refreshStatus.run();
                choiceBar.setVisible(false);
                yesBtn.setDisable(true);
                noBtn.setDisable(true);
                new Timeline(new KeyFrame(Duration.seconds(5), ev -> advanceOrCloseDialogue())).play();
            });

            choiceBar.setVisible(true);
            dialogueContainer.setVisible(true);
            if (hubButtons != null) hubButtons.setMouseTransparent(true);
        }

        public void scheduleAuditForToday() {
            int startMin = 12 * 60;                 // 12:00
            int endMin   = 16 * 60 + 30;            // 4:30 PM
            int minute   = startMin + rng.nextInt(Math.max(1, endMin - startMin));
            auditTime = LocalTime.of(minute / 60, minute % 60);
            auditScheduledToday = true;
        }

        public void showAudit(IntConsumer changeRep, Runnable refreshStatus) {
            Info boss = npcs.get(Department.BOSS);
            setNpcImage(boss);
            speakerLbl.setText("Compliance (via " + boss.name + ")");

            if (gambledSinceLastAudit) {
                int penalty = -6;
                changeRep.accept(penalty);
                textLbl.setText("Spot check. Unusual risk signatures around your book. "
                        + "We expect prudence. (Reputation " + penalty + ")");
            } else {
                textLbl.setText("Routine compliance sweep. Keep your positions tidy.");
            }

            prepChoiceBarSingle("Understood");

            dialogueContainer.setVisible(true);
            if (hubButtons != null) hubButtons.setMouseTransparent(true);

            yesBtn.setOnAction(e -> {
                choiceBar.setVisible(false);
                advanceOrCloseDialogue();
                resetChoiceBarForTwoOptions();
            });

            gambledSinceLastAudit = false;
            if (daysUntilNextAudit <= 0) {
                daysUntilNextAudit = rng.nextInt(24) + 7; // next window in 7–30 days
            }
            auditScheduledToday = false;
            auditTime = null;
            refreshStatus.run();
        }

        //  small UI helpers

        private static Image load(String path) {
            InputStream in = Npc.class.getResourceAsStream(path);
            if (in == null) throw new IllegalArgumentException("Missing " + path + " in resources");
            return new Image(in);
        }

        private void setTwoButtons(String noLabel, String yesLabel) {
            noBtn.setText(noLabel);
            yesBtn.setText(yesLabel);

            yesBtn.setDisable(false);
            noBtn.setDisable(false);
            choiceBar.setVisible(true);
            choiceBar.setDisable(false);
            choiceBar.setMouseTransparent(false);

            if (choiceBar.getChildren().size() != 2 || choiceBar.getChildren().get(0) != noBtn) {
                choiceBar.getChildren().setAll(noBtn, yesBtn);
            }

            yesBtn.setOnAction(null);
            noBtn.setOnAction(null);
        }

        private void setNpcImage(Info npc) {
            npcView.setImage(load(npc.imgPath));
            npcView.setFitWidth(npc.fitWidth);
        }

        private void showNpcDialogue(Info npc, String text) {
            setNpcImage(npc);
            speakerLbl.setText(npc.name);
            textLbl.setText(text);
            choiceBar.setVisible(false);
            dialogueContainer.setVisible(true);
            if (hubButtons != null) hubButtons.setMouseTransparent(true);
        }

        public void advanceOrCloseDialogue() {
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
    }
}
