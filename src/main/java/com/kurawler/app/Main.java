package com.kurawler.app;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.kurawler.screens.ScreenManager;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // ── Fullscreen setup ──────────────────────────────────────────────────
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double W = screen.getWidth();
        double H = screen.getHeight();

        ScreenManager manager = new ScreenManager(stage, W, H);

        Scene scene = new Scene(manager.getRoot(), W, H);
        scene.getStylesheets().add(
                getClass().getResource("/css/dungeon.css").toExternalForm());

        stage.setTitle("Dungeon KUrawler");
        stage.setScene(scene);
        stage.setX(screen.getMinX());
        stage.setY(screen.getMinY());
        stage.setWidth(W);
        stage.setHeight(H);
        stage.setMaximized(true); // borderless maximized
        stage.setResizable(true);
        stage.show();

        manager.showMainMenu();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
