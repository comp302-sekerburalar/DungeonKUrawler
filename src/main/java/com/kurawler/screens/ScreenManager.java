package com.kurawler.screens;

import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import com.kurawler.model.UserStore;
import com.kurawler.screens.GameScreen;

/**
 * Central controller for navigating between screens.
 * All screens reference this manager to trigger transitions.
 */
public class ScreenManager {

    private final Stage     stage;
    private final StackPane root;
    private final UserStore userStore;

    private MainMenuScreen   mainMenuScreen;
    private LoginScreen      loginScreen;
    private RegisterScreen   registerScreen;
    private WelcomeScreen    welcomeScreen;
    private HelpScreen       helpScreen;

    public ScreenManager(Stage stage) {
        this.stage     = stage;
        this.root      = new StackPane();
        this.userStore = new UserStore();

        initScreens();
    }

    private void initScreens() {
        mainMenuScreen  = new MainMenuScreen(this);
        loginScreen     = new LoginScreen(this);
        registerScreen  = new RegisterScreen(this);
        welcomeScreen   = new WelcomeScreen(this);
        helpScreen      = new HelpScreen(this);
    }

    // ---------- Navigation ----------

    public void showMainMenu() {
        setScreen(mainMenuScreen);
    }

    public void showLogin() {
        loginScreen.reset();
        setScreen(loginScreen);
    }

    public void showRegister() {
        registerScreen.reset();
        setScreen(registerScreen);
    }

    /**
     * Called after successful login or registration.
     * @param heroName the authenticated / newly created hero name
     */
    public void showWelcome(String heroName) {
        welcomeScreen.setHeroName(heroName);
        setScreen(welcomeScreen);
    }

    public void showHelp() {
        setScreen(helpScreen);
    }

    /** Launches the gameplay screen. */
    public void startGame(String heroName) {
        GameScreen gameScreen = new GameScreen(this, heroName);
        setScreen(gameScreen);
    }

    public void exitGame() {
        stage.close();
    }

    // ---------- Helpers ----------

    private void setScreen(BaseScreen screen) {
        root.getChildren().setAll(screen.getView());
    }

    public StackPane getRoot()      { return root; }
    public Stage     getStage()     { return stage; }
    public UserStore getUserStore() { return userStore; }
}
