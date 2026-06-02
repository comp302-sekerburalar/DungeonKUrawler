module com.kurawler {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;

    opens com.kurawler.app to javafx.graphics;
    opens com.kurawler.screens to javafx.graphics;
    opens com.kurawler.components to javafx.graphics;
    opens com.kurawler.model to javafx.graphics;
    opens com.kurawler.engine to javafx.graphics;
    opens com.kurawler.util to javafx.graphics;
    opens com.kurawler.game.entity to javafx.graphics;
    opens com.kurawler.game.objects to javafx.graphics;
    opens com.kurawler.game.action to javafx.graphics;
    opens com.kurawler.game.effect to javafx.graphics;

    exports com.kurawler.app;
    exports com.kurawler.screens;
    exports com.kurawler.components;
    exports com.kurawler.model;
    exports com.kurawler.engine;
    exports com.kurawler.util;
    exports com.kurawler.game.entity;
    exports com.kurawler.game.objects;
    exports com.kurawler.game.action;
    exports com.kurawler.game.effect;
    exports com.kurawler.wave;

    opens com.kurawler.wave to javafx.graphics;
}
