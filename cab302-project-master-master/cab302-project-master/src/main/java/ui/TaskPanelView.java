package ui;

import app.demo.Task;
import app.demo.TaskManager;
import javafx.animation.TranslateTransition;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

final class TaskPanelView {

    private final TaskManager taskManager;
    private final boolean debugBorders;

    private final VBox root;       // the sliding panel
    private boolean open = false;

    TaskPanelView(TaskManager taskManager, boolean debugBorders) {
        this.taskManager = taskManager;
        this.debugBorders = debugBorders;
        this.root = build();
    }

    Node getRoot() { return root; }
    boolean isOpen() { return open; }

    void toggle() { if (open) close(); else open(); }

    void open() {
        root.setVisible(true);
        root.setMouseTransparent(false);
        root.toFront();
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), root);
        tt.setFromX(root.getTranslateX());
        tt.setToX(0);
        tt.setOnFinished(e -> open = true);
        tt.play();
    }

    void close() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(180), root);
        tt.setFromX(root.getTranslateX());
        tt.setToX(180);
        tt.setOnFinished(e -> {
            root.setVisible(false);
            root.setMouseTransparent(true);
            open = false;
        });
        tt.play();
    }

    private VBox build() {
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
            @Override protected void updateItem(Task item, boolean empty) {
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
        ok.setOnAction(e -> close());

        HBox bottom = new HBox(8, miniScroll, ok);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(miniScroll, Priority.ALWAYS);

        panel.setVisible(false);
        panel.setTranslateX(180);
        panel.getChildren().addAll(title, list, bottom);

        return panel;
    }
}
