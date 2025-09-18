

module demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    requires javafx.graphics;
    requires javafx.media;
    requires org.xerial.sqlitejdbc;


    requires java.desktop;


    opens app.demo to javafx.fxml;
    exports app.demo;
    exports core;
    opens core to javafx.fxml;
    exports events;
    opens events to javafx.fxml;
    exports npc;
    opens npc to javafx.fxml;
    exports ui;
    opens ui to javafx.fxml;
    exports app.demo.data;
    opens app.demo.data to javafx.fxml;
    exports start;
    opens start to javafx.fxml;
}
