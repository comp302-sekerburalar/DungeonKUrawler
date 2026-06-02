package com.kurawler.screens;

import com.kurawler.engine.*;
import com.kurawler.game.action.Action;
import com.kurawler.game.entity.*;
import com.kurawler.game.objects.*;
import com.kurawler.util.SpriteRenderer;
import com.kurawler.util.HeroAnimator;
import com.kurawler.util.KnightAnimator;
import com.kurawler.util.SorcererAnimator;
import com.kurawler.util.GameRenderer;
import com.kurawler.util.ImageCache;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class GameScreen extends BaseScreen {

    private static final int TILE = 36; // rendered tile size in pixels
    private static final int MAP_W = 20;
    private static final int MAP_H = 15;

    private final GameEngine engine;
    private final String heroName;

    // ── UI components ──
    private Canvas mapCanvas;
    private Label lblHP, lblEnergy, lblMana, lblStr, lblDef;
    private ProgressBar barHP, barEnergy, barMana;
    private VBox inventoryGrid;
    private VBox actionPanel;
    private TextArea messageLog;
    private Label lblAiStatus;
    private Label lblTargetRelic;
    private Label lblWave;

    private GameObject selectedObject;
    private Enemy selectedEnemy;

    // ── Hero walk animation ──
    private final HeroAnimator heroAnimator = new HeroAnimator();
    // ── Knight animation (one per screen, reused for all knights) ──
    private final KnightAnimator knightAnimator = new KnightAnimator();
    private final SorcererAnimator sorcererAnimator = new SorcererAnimator();

    // ── floor tile image (walls sheet row 0 col 0 = floor) ──
    private static final int FLOOR_SC = 0, FLOOR_SR = 0;
    private static final int WALL_SC = 0, WALL_SR = 0; // first wall tile

    // ── character sprites (characters_x2.png rows) ──
    // Row 0: wizards/sorcerers, Row 1: knights, Row 2: heroes, Row 3: mages
    private static final int HERO_SC = 0, HERO_SR = 2;
    private static final int KNIGHT_SC = 0, KNIGHT_SR = 1;
    private static final int SORCERER_SC = 0, SORCERER_SR = 0;

    /** Play with a randomly generated map. */
    public GameScreen(ScreenManager manager, String heroName) {
        this(manager, heroName, null);
    }

    /** Play with a pre-designed map (may be null for random). */
    public GameScreen(ScreenManager manager, String heroName,
            com.kurawler.engine.GridMap existingMap) {
        super(manager, true);
        this.heroName = heroName;
        this.engine = new GameEngine(heroName, existingMap);
        initView();
        wireEngine();
        engine.start();
        Platform.runLater(this::refreshAll);
    }

    // =========================================================================
    // UI construction
    // =========================================================================
    @Override
    protected Pane buildUI() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W(), H());

        // Top HUD
        root.setTop(buildHUD());

        // Map canvas
        mapCanvas = new Canvas(TILE * MAP_W, TILE * MAP_H);
        ScrollPane scroll = new ScrollPane(mapCanvas);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setStyle("-fx-background-color:#110808; -fx-border-color:transparent;");
        scroll.setPrefSize(550, 535);
        root.setCenter(scroll);

        // Right sidebar
        root.setRight(buildSidebar());

        // Input
        mapCanvas.setOnMouseClicked(e -> handleMapClick(e.getX(), e.getY()));
        root.setOnKeyPressed(e -> handleKey(e.getCode()));
        root.setFocusTraversable(true);
        return root;
    }

    // ── HUD (top bar) ──
    private HBox buildHUD() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#1e0e0e; -fx-border-color:#6b3a2a; -fx-border-width:0 0 2 0;");

        // Hero sprite in HUD
        Canvas heroIcon = new Canvas(32, 32);
        drawCharacterIcon(heroIcon.getGraphicsContext2D(), HERO_SC, HERO_SR);

        Label heroLbl = makeLabel("♦ " + heroName, "#c9a227", 11, true);

        // Target relic display
        lblTargetRelic = makeLabel("FIND: " + engine.getTargetRelicName(), "#7ee8fa", 10, true);

        VBox statsLeft = new VBox(2);
        statsLeft.setAlignment(Pos.CENTER_LEFT);

        HBox hpRow = statRow("♥ HP", "#e74c3c");
        barHP = statBar("#e74c3c");
        lblHP = statValueLabel();
        hpRow.getChildren().addAll(barHP, lblHP);

        HBox enRow = statRow("⚡ EN", "#2ecc71");
        barEnergy = statBar("#2ecc71");
        lblEnergy = statValueLabel();
        enRow.getChildren().addAll(barEnergy, lblEnergy);

        HBox manaRow = statRow("✦ MP", "#3498db");
        barMana = statBar("#3498db");
        lblMana = statValueLabel();
        manaRow.getChildren().addAll(barMana, lblMana);

        statsLeft.getChildren().addAll(hpRow, enRow, manaRow);

        lblStr = makeLabel("STR:--", "#f39c12", 10, false);
        lblDef = makeLabel("DEF:--", "#9b59b6", 10, false);
        VBox statsRight = new VBox(4, lblStr, lblDef);
        statsRight.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnMenu = new Button("MENU");
        btnMenu.getStyleClass().add("dungeon-btn");
        btnMenu.setOnAction(e -> {
            engine.pause();
            showInGameMenu();
        });

        bar.getChildren().addAll(heroIcon, heroLbl, lblTargetRelic, statsLeft, statsRight, spacer, btnMenu);
        return bar;
    }

    private void drawCharacterIcon(GraphicsContext gc, int sc, int sr) {
        SpriteRenderer.drawTile(gc, "characters_x2", sc, sr, 0, 0, 32, 32);
    }

    // ── Sidebar ──
    private VBox buildSidebar() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(250);
        panel.setStyle("-fx-background-color:#1e0e0e; -fx-border-color:#6b3a2a; -fx-border-width:0 0 0 2;");

        // Inventory
        panel.getChildren().add(sectionHeader("INVENTORY (2×4)"));
        inventoryGrid = new VBox(3);
        inventoryGrid.setPadding(new Insets(5, 8, 5, 8));
        rebuildInventoryGrid();
        panel.getChildren().add(inventoryGrid);

        panel.getChildren().add(divider());
        panel.getChildren().add(sectionHeader("ACTIONS"));
        actionPanel = new VBox(4);
        actionPanel.setPadding(new Insets(5, 8, 5, 8));
        Label noSel = grayLabel("Click an adjacent object");
        actionPanel.getChildren().add(noSel);
        panel.getChildren().add(actionPanel);

        panel.getChildren().add(divider());
        panel.getChildren().add(sectionHeader("ENEMY AI"));
        lblAiStatus = new Label("Waiting...");
        lblAiStatus.setFont(Font.font("Courier New", 9));
        lblAiStatus.setTextFill(Color.web("#8a7060"));
        lblAiStatus.setWrapText(true);
        lblAiStatus.setPadding(new Insets(3, 8, 3, 8));
        panel.getChildren().add(lblAiStatus);

        panel.getChildren().add(divider());
        panel.getChildren().add(sectionHeader("LOG"));
        messageLog = new TextArea();
        messageLog.setEditable(false);
        messageLog.setWrapText(true);
        messageLog.setPrefRowCount(6);
        messageLog.setStyle(
                "-fx-control-inner-background:#110808; -fx-text-fill:#8a7060;" +
                        "-fx-font-family:'Courier New'; -fx-font-size:9px; -fx-border-color:transparent;");
        VBox.setVgrow(messageLog, Priority.ALWAYS);
        panel.getChildren().add(messageLog);

        return panel;
    }

    // =========================================================================
    // Engine wiring
    // =========================================================================
    private void wireEngine() {
        engine.setOnMapChanged(() -> Platform.runLater(this::drawMap));
        engine.setOnStatsChanged(() -> Platform.runLater(this::updateStats));
        engine.setOnInventoryChanged(() -> Platform.runLater(this::rebuildInventoryGrid));
        engine.setOnMessage(msg -> Platform.runLater(() -> appendLog(msg)));
        engine.setOnAiLog(log -> Platform.runLater(() -> {
            lblAiStatus.setText(log);
            boolean chasing = log.contains("CHASING");
            lblAiStatus.setTextFill(chasing ? Color.web("#e74c3c") : Color.web("#8a7060"));
        }));
        engine.setOnGameOver(() -> Platform.runLater(this::showGameOverScreen));
        engine.setOnVictory(() -> Platform.runLater(this::showVictoryScreen));
    }

    private void refreshAll() {
        drawMap();
        updateStats();
        rebuildInventoryGrid();
    }

    // =========================================================================
    // Map rendering
    // =========================================================================
    private void drawMap() {
        GridMap grid = engine.getMap();
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();

        // Background
        gc.setFill(Color.web("#110808"));
        gc.fillRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

        // Tiles — use GameRenderer which maps to actual image files
        for (int c = 0; c < grid.getCols(); c++) {
            for (int r = 0; r < grid.getRows(); r++) {
                TileType t = grid.getTile(new Vec2(c, r));
                double px = c * TILE, py = r * TILE;
                GameRenderer.drawTile(gc, t, c, r, px, py, TILE);
            }
        }

        // 3×3 interaction highlight around hero
        Vec2 heroPos = engine.getHero().getPos();
        gc.setFill(Color.web("#c9a22712"));
        for (int dc = -1; dc <= 1; dc++) {
            for (int dr = -1; dr <= 1; dr++) {
                Vec2 cell = heroPos.add(dc, dr);
                if (cell.inBounds(grid.getCols(), grid.getRows()))
                    gc.fillRect(cell.col() * TILE, cell.row() * TILE, TILE - 1, TILE - 1);
            }
        }

        // Game objects
        for (var list : grid.allObjects()) {
            for (GameObject obj : list)
                drawObject(gc, obj);
        }

        // Projectiles
        for (Projectile p : engine.getProjectiles()) {
            if (!p.isActive())
                continue;
            GameRenderer.drawProjectile(gc, p.getPos().col() * TILE, p.getPos().row() * TILE, TILE);
        }

        // Enemies
        for (Enemy e : engine.getEnemies())
            drawEnemy(gc, e);

        // Hero
        drawHero(gc, heroPos);
    }

    private void drawObject(GraphicsContext gc, GameObject obj) {
        double px = obj.getPos().col() * TILE;
        double py = obj.getPos().row() * TILE;
        if ("SEARCH_WALL".equals(obj.renderTag())) {
            GameRenderer.drawSearchableWall(gc, px, py, TILE);
        } else if ("WEAPON".equals(obj.renderTag())) {
            GameRenderer.drawWeapon(gc, px, py, TILE);
        } else if ("ARMOR".equals(obj.renderTag())) {
            GameRenderer.drawArmor(gc, px, py, TILE);
        } else {
            GameRenderer.drawObject(gc, obj, px, py, TILE);
        }
        // Selection highlight
        if (obj == selectedObject) {
            gc.setStroke(Color.web("#c9a227"));
            gc.setLineWidth(2);
            gc.strokeRect(px + 1, py + 1, TILE - 3, TILE - 3);
        }
    }

    private void drawHero(GraphicsContext gc, Vec2 pos) {
        double px = pos.col() * TILE, py = pos.row() * TILE;
        heroAnimator.draw(gc, px, py, TILE, TILE);
        // Weapon equipped indicator
        if (engine.getHero().hasWeaponEquipped()) {
            gc.setFill(Color.web("#c9a227", 0.8));
            gc.fillRect(px + TILE - 8, py, 8, 8);
        }
    }

    private void drawEnemy(GraphicsContext gc, Enemy enemy) {
        double px = enemy.getPos().col() * TILE;
        double py = enemy.getPos().row() * TILE;
        // Step knight animation when enemy is chasing (visual feedback)
        if (enemy.getType() == Enemy.Type.KNIGHT && enemy.getState() == EnemyState.CHASING) {
            knightAnimator.step();
        } else if (enemy.getType() == Enemy.Type.SORCERER) {
            sorcererAnimator.step();
        }
        GameRenderer.drawEnemy(gc, enemy, px, py, TILE, knightAnimator, sorcererAnimator, enemy == selectedEnemy);
    }

    // =========================================================================
    // Stats bar update
    // =========================================================================
    private void updateStats() {
        Hero h = engine.getHero();
        int hp = h.getStat(StatType.HP), maxHp = h.getStatMax(StatType.HP);
        int en = h.getStat(StatType.ENERGY), maxEn = h.getStatMax(StatType.ENERGY);
        int mp = h.getStat(StatType.MANA), maxMp = h.getStatMax(StatType.MANA);

        lblHP.setText(hp + "/" + maxHp);
        lblEnergy.setText(en + "/" + maxEn);
        lblMana.setText(mp + "/" + maxMp);
        lblStr.setText("STR:" + h.getStat(StatType.STR));
        lblDef.setText("DEF:" + h.getStat(StatType.DEF));

        barHP.setProgress((double) hp / maxHp);
        barEnergy.setProgress((double) en / maxEn);
        barMana.setProgress((double) mp / maxMp);
    }

    // =========================================================================
    // Inventory panel — uses sprite icons
    // =========================================================================
    private void rebuildInventoryGrid() {
        if (inventoryGrid == null)
            return;
        inventoryGrid.getChildren().clear();
        Inventory inv = engine.getHero().getInventory();

        for (int row = 0; row < Inventory.ROWS; row++) {
            HBox rowBox = new HBox(3);
            for (int col = 0; col < Inventory.COLS; col++) {
                int idx = row * Inventory.COLS + col;
                GameObject item = inv.get(idx);
                rowBox.getChildren().add(buildInventorySlot(item));
            }
            inventoryGrid.getChildren().add(rowBox);
        }
    }

    private Pane buildInventorySlot(GameObject item) {
        StackPane slot = new StackPane();
        slot.setPrefSize(112, 30);
        slot.setStyle("-fx-background-color:" +
                (item == null ? "#110808" : "#2a1a1a") +
                "; -fx-border-color:" +
                (item == null ? "#2a1a1a" : "#6b3a2a") +
                "; -fx-border-width:1; -fx-cursor:" +
                (item == null ? "default" : "hand") + ";");

        if (item != null) {
            // Sprite icon
            Canvas icon = new Canvas(24, 24);
            GraphicsContext gc = icon.getGraphicsContext2D();
            String sheet = item.getSpriteSheet();
            if (sheet != null && !sheet.isEmpty()) {
                SpriteRenderer.drawTile(gc, sheet, item.getSpriteCol(), item.getSpriteRow(), 0, 0, 24, 24);
            }
            // Name label
            Label name = new Label(item.getName().length() > 8
                    ? item.getName().substring(0, 7) + "…"
                    : item.getName());
            name.setFont(Font.font("Courier New", 8));
            name.setTextFill(Color.web("#e8d5b0"));

            // Equipped indicator
            boolean equipped = item == engine.getHero().getEquippedWeapon();
            if (equipped)
                slot.setStyle(slot.getStyle() + "-fx-border-color:#c9a227; -fx-border-width:2;");

            HBox content = new HBox(4, icon, name);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(0, 3, 0, 3));
            slot.getChildren().add(content);

            final GameObject captured = item;
            slot.setOnMouseClicked(e -> showInventoryActions(captured));
        }
        return slot;
    }

    private void showInventoryActions(GameObject item) {
        selectedObject = item;
        actionPanel.getChildren().clear();
        Label header = makeLabel(item.getName(), "#c9a227", 10, true);
        actionPanel.getChildren().add(header);

        for (Action action : item.getActions()) {
            Button btn = actionBtn(action.getLabel(), () -> {
                engine.executeAction(action, item);
                actionPanel.getChildren().clear();
                selectedObject = null;
                drawMap();
            });
            actionPanel.getChildren().add(btn);
        }
        // DISCARD (spec §2.4.2)
        Button discard = actionBtn("Discard " + item.getName(), () -> {
            engine.getHero().getInventory().remove(item);
            if (engine.getHero().getEquippedWeapon() == item)
                engine.getHero().unequipWeapon();
            engine.postMessage("Discarded: " + item.getName());
            engine.notifyInventoryChanged();
            actionPanel.getChildren().clear();
            selectedObject = null;
        });
        discard.setStyle(discard.getStyle() + "-fx-text-fill:#e74c3c;");
        actionPanel.getChildren().add(discard);
    }

    // =========================================================================
    // Map click — objects and enemies
    // =========================================================================
    private void handleMapClick(double mouseX, double mouseY) {
        int col = (int) (mouseX / TILE);
        int row = (int) (mouseY / TILE);
        Vec2 clicked = new Vec2(col, row);

        // Check if clicked on an enemy
        for (Enemy e : engine.getEnemies()) {
            if (e.getPos().equals(clicked)) {
                selectedEnemy = e;
                selectedObject = null;
                showEnemyActions(e);
                drawMap();
                return;
            }
        }
        selectedEnemy = null;

        List<GameObject> objs = engine.getMap().objectsAt(clicked);
        if (objs.isEmpty()) {
            actionPanel.getChildren().clear();
            actionPanel.getChildren().add(grayLabel("Nothing here."));
            selectedObject = null;
            drawMap();
            return;
        }

        GameObject obj = objs.get(0);
        selectedObject = obj;
        showObjectActions(obj);
        drawMap();
    }

    private void showObjectActions(GameObject obj) {
        List<Action> actions = engine.getActionsFor(obj);
        actionPanel.getChildren().clear();
        actionPanel.getChildren().add(makeLabel(obj.getName(), "#c9a227", 10, true));

        if (actions.isEmpty()) {
            boolean adj = engine.getMap().isAdjacent(engine.getHero().getPos(), obj.getPos());
            Label info = grayLabel(adj ? "No actions." : "Too far away — move closer.");
            if (!adj)
                info.setTextFill(Color.web("#e74c3c"));
            actionPanel.getChildren().add(info);
        } else {
            for (Action action : actions) {
                Button btn = actionBtn(action.getLabel(), () -> {
                    engine.executeAction(action, obj);
                    actionPanel.getChildren().clear();
                    selectedObject = null;
                    drawMap();
                });
                actionPanel.getChildren().add(btn);
            }
        }
    }

    private void showEnemyActions(Enemy enemy) {
        actionPanel.getChildren().clear();
        actionPanel.getChildren().add(makeLabel(
                enemy.getType() + " #" + enemy.getId(), "#e74c3c", 10, true));
        actionPanel.getChildren().add(grayLabel("HP: " + enemy.getStat(StatType.HP)));
        actionPanel.getChildren().add(grayLabel("State: " + enemy.getState()));

        if (engine.getHero().hasWeaponEquipped() &&
                engine.getMap().isAdjacent(engine.getHero().getPos(), enemy.getPos())) {
            Button btnAtk = actionBtn("Attack " + enemy.getType(), () -> {
                engine.heroAttackEnemy(enemy);
                selectedEnemy = null;
                actionPanel.getChildren().clear();
                drawMap();
            });
            btnAtk.setStyle(btnAtk.getStyle() + "-fx-border-color:#e74c3c;");
            actionPanel.getChildren().add(btnAtk);
        } else {
            String msg = engine.getHero().hasWeaponEquipped()
                    ? "Move adjacent to attack."
                    : "Equip a weapon to attack.";
            actionPanel.getChildren().add(grayLabel(msg));
        }
    }

    // =========================================================================
    // Keyboard
    // =========================================================================
    private void handleKey(KeyCode code) {
        if (engine.isGameOver())
            return;
        switch (code) {
            case UP, W -> {
                if (engine.moveHero(0, -1))
                    heroAnimator.step();
            }
            case DOWN, S -> {
                if (engine.moveHero(0, 1))
                    heroAnimator.step();
            }
            case LEFT, A -> {
                if (engine.moveHero(-1, 0))
                    heroAnimator.step();
            }
            case RIGHT, D -> {
                if (engine.moveHero(1, 0))
                    heroAnimator.step();
            }
            case ESCAPE -> {
                engine.pause();
                showInGameMenu();
            }
            default -> {
            }
        }
    }

    // =========================================================================
    // In-game menu, game over, victory
    // =========================================================================
    private void showInGameMenu() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Pause");
        alert.setHeaderText("GAME PAUSED");
        ButtonType resume = new ButtonType("Resume");
        ButtonType mainMenu = new ButtonType("Main Menu");
        alert.getButtonTypes().setAll(resume, mainMenu);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == mainMenu) {
                engine.stop();
                manager.showMainMenu();
            } else
                engine.resume();
        });
    }

    private void showGameOverScreen() {
        engine.stop();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Game Over");
        a.setHeaderText("YOU HAVE FALLEN");
        a.setContentText("The dungeon claims another victim.\nReturn to the main menu?");
        ButtonType retry = new ButtonType("Try Again");
        ButtonType menu = new ButtonType("Main Menu");
        a.getButtonTypes().setAll(retry, menu);
        a.showAndWait().ifPresent(btn -> {
            if (btn == retry)
                manager.startGame(heroName);
            else
                manager.showMainMenu();
        });
    }

    private void showVictoryScreen() {
        engine.stop();
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Victory!");
        a.setHeaderText("YOU FOUND THE " + engine.getTargetRelicName().toUpperCase() + "!");
        a.setContentText("The dungeon yields its secret. You are victorious!");
        ButtonType menu = new ButtonType("Main Menu");
        a.getButtonTypes().setAll(menu);
        a.showAndWait();
        manager.showMainMenu();
    }

    // =========================================================================
    // Message log
    // =========================================================================
    private void appendLog(String msg) {
        messageLog.appendText(msg + "\n");
    }

    // =========================================================================
    // UI helpers
    // =========================================================================
    private Label makeLabel(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setFont(Font.font("Courier New", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.web(color));
        return l;
    }

    private Label grayLabel(String text) {
        return makeLabel(text, "#8a7060", 9, false);
    }

    private Label statValueLabel() {
        Label l = new Label("--/--");
        l.setFont(Font.font("Courier New", 9));
        l.setTextFill(Color.web("#e8d5b0"));
        return l;
    }

    private HBox statRow(String label, String color) {
        Label lbl = makeLabel(label, color, 9, true);
        lbl.setMinWidth(42);
        HBox row = new HBox(4, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private ProgressBar statBar(String color) {
        ProgressBar bar = new ProgressBar(1.0);
        bar.setPrefWidth(80);
        bar.setPrefHeight(8);
        bar.setStyle("-fx-accent: " + color + ";");
        return bar;
    }

    private Label sectionHeader(String text) {
        Label l = new Label("  " + text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(4, 0, 3, 0));
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        l.setTextFill(Color.web("#c9a227"));
        l.setStyle("-fx-background-color:#2a1a1a; -fx-border-color:#6b3a2a; -fx-border-width:1 0 1 0;");
        return l;
    }

    private Region divider() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:#3d2a2a;");
        return r;
    }

    private Button actionBtn(String label, Runnable onClick) {
        Button btn = new Button(label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        String base = "-fx-background-color:#3d2a2a; -fx-border-color:#6b3a2a; -fx-border-width:1;" +
                "-fx-text-fill:#e8d5b0; -fx-padding:4 6 4 6; -fx-background-radius:0;" +
                "-fx-border-radius:0; -fx-cursor:hand;";
        String hover = "-fx-background-color:#6b3a2a; -fx-border-color:#c9a227; -fx-border-width:1;" +
                "-fx-text-fill:#c9a227; -fx-padding:4 6 4 6; -fx-background-radius:0;" +
                "-fx-border-radius:0; -fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnAction(e -> onClick.run());
        return btn;
    }
}
