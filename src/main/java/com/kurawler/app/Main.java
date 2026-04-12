package com.kurawler.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.kurawler.screens.ScreenManager;

/**
 * Entry point.
 * Initializes the JavaFX stage and hands control to the ScreenManager.
 */
public class Main extends Application {

    public static final String GAME_TITLE = "Dungeon KUrawler";
    public static final int WINDOW_W = 800;
    public static final int WINDOW_H = 600;

    @Override
    public void start(Stage primaryStage) {
        ScreenManager manager = new ScreenManager(primaryStage);

        Scene scene = new Scene(manager.getRoot(), WINDOW_W, WINDOW_H);
        scene.getStylesheets().add(
                getClass().getResource("/css/dungeon.css").toExternalForm());

        primaryStage.setTitle(GAME_TITLE);
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();

        manager.showMainMenu();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
