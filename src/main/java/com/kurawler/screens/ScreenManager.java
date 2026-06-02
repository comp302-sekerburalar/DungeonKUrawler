package com.kurawler.screens;

import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import com.kurawler.engine.GridMap;
import com.kurawler.model.UserStore;

public class ScreenManager {

    private final Stage stage;
    private final StackPane root;
    private final UserStore userStore;
    private final double screenW;
    private final double screenH;

    // current hero name (set at login, used by marketplace and game)
    private String currentHero = "GUEST";
    private boolean loggedIn = false;

    // Singleton screens
    private MainMenuScreen mainMenu;
    private LoginScreen login;
    private RegisterScreen register;
    private WelcomeScreen welcome;
    private HelpScreen help;
    private WaveSurvivalScreen waveSurvival;

    public ScreenManager(Stage stage, double w, double h) {
        this.stage = stage;
        this.root = new StackPane();
        this.userStore = new UserStore();
        this.screenW = w;
        this.screenH = h;
        mainMenu = new MainMenuScreen(this);
        login = new LoginScreen(this);
        register = new RegisterScreen(this);
        welcome = new WelcomeScreen(this);
        help = new HelpScreen(this);
        waveSurvival = new WaveSurvivalScreen(this);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public void showMainMenu() {
        set(mainMenu);
    }

    public void showHelp() {
        set(help);
    }

    public void showWaveSurvival() {
        set(waveSurvival);
    }

    public void showLogin() {
        login.reset();
        set(login);
    }

    public void showRegister() {
        register.reset();
        set(register);
    }

    public void showWelcome(String heroName) {
        this.currentHero = heroName;
        welcome.setHeroName(heroName);
        set(welcome);
    }

    public void showMapSelection(String heroName) {
        this.currentHero = heroName;
        set(new MapSelectionScreen(this, heroName));
    }

    public void showMapEditor(String heroName) {
        set(new MapEditorScreen(this, heroName));
    }

    public void showMapEditor(String heroName, GridMap map, String mapName) {
        set(new MapEditorScreen(this, heroName, map, mapName));
    }

    /** Open the standalone Marketplace from the main menu. */
    public void showMarketplace() {
        if (currentHero == null || currentHero.isBlank()) {
            currentHero = "GUEST";
        }
        set(new MarketplaceScreen(this, currentHero));
    }

    public void startGame(String heroName) {
        this.currentHero = heroName;
        set(new GameScreen(this, heroName, null));
    }

    public void startGame(String heroName, GridMap map) {
        this.currentHero = heroName;
        set(new GameScreen(this, heroName, map));
    }

    public void startTeamMatch(String heroName, GridMap map) {
        this.currentHero = heroName;
        set(new GameScreen(this, heroName, map, com.kurawler.engine.GameEngine.Mode.TEAM_MATCH));
    }

    public void startWaveSurvival(String difficulty) {
        set(new WaveSurvivalGameScreen(this, difficulty));
    }

    public void exitGame() {
        stage.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void set(BaseScreen screen) {
        root.getChildren().setAll(screen.getView());
    }

    public StackPane getRoot() {
        return root;
    }

    public Stage getStage() {
        return stage;
    }

    public UserStore getUserStore() {
        return userStore;
    }

    public double getWidth() {
        return screenW;
    }

    public double getHeight() {
        return screenH;
    }

    public String getCurrentHero() {
        return currentHero;
    }

    public void loginUser(String username) {
        currentHero = username;
        loggedIn = true;
    }

    public void logoutUser() {
        currentHero = "GUEST";
        loggedIn = false;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void showLoadout() {
        set(new LoadoutScreen(this));
    }
}
