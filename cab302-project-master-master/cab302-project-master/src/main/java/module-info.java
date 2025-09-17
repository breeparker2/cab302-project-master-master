

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
}
