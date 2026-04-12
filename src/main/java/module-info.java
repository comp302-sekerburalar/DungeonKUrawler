module com.kurawler {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens com.kurawler.app      to javafx.graphics;
    opens com.kurawler.screens  to javafx.graphics;
    opens com.kurawler.components to javafx.graphics;
    opens com.kurawler.model    to javafx.graphics;

    exports com.kurawler.app;
    exports com.kurawler.screens;
    exports com.kurawler.components;
    exports com.kurawler.model;
}
