package com.kurawler.screens;

import com.kurawler.engine.GameEngine;
import com.kurawler.engine.GridMap;
import com.kurawler.engine.TileType;
import com.kurawler.engine.Vec2;
import com.kurawler.game.action.Action;
import com.kurawler.game.entity.*;
import com.kurawler.game.objects.GameObject;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * The main gameplay screen.
 *
 * Layout:
 *   ┌─────────────────────────────────────────────────┐
 *   │  STATS BAR (HP / Energy / Mana / STR / DEF)     │
 *   ├───────────────────────┬─────────────────────────┤
 *   │                       │  INVENTORY (2×4)         │
 *   │   MAP CANVAS          ├─────────────────────────┤
 *   │                       │  ACTION MENU             │
 *   │                       ├─────────────────────────┤
 *   │                       │  AI / MESSAGE LOG        │
 *   └───────────────────────┴─────────────────────────┘
 */
public class GameScreen extends BaseScreen {

    // Rendering constants
    private static final int TILE_PX  = 36;   // pixels per grid cell

    private final GameEngine engine;
    private final String     heroName;

    // UI components refreshed on state changes
    private Canvas      mapCanvas;
    private Label       lblHP, lblEnergy, lblMana, lblStr, lblDef;
    private VBox        inventoryGrid;
    private VBox        actionPanel;
    private TextArea    messageLog;
    private Label       lblAiStatus;

    // Currently selected map object for action menu
    private GameObject  selectedObject;

    public GameScreen(ScreenManager manager, String heroName) {
        // Deferred build: engine must be assigned BEFORE buildUI() runs,
        // because buildUI() -> buildRightPanel() -> rebuildInventoryGrid() -> engine.getHero()
        super(manager, true);
        this.heroName = heroName;
        this.engine   = new GameEngine();   // assigned BEFORE initView()

        initView();      // now safe: all fields are ready
        wireEngine();
        engine.start();
        refresh();
    }

    // =========================================================================
    //  UI construction
    // =========================================================================

    @Override
    protected Pane buildUI() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(800, 600);

        // ---- Top stats bar ----
        root.setTop(buildStatsBar());

        // ---- Map canvas (centre) ----
        mapCanvas = new Canvas(TILE_PX * 20, TILE_PX * 15);
        StackPane canvasWrap = new StackPane(mapCanvas);
        canvasWrap.setStyle("-fx-background-color: #110808;");
        // Scroll if map is larger than window
        ScrollPane scroll = new ScrollPane(canvasWrap);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setStyle("-fx-background-color: #110808; -fx-border-color: transparent;");
        scroll.setPrefSize(540, 520);
        root.setCenter(scroll);

        // ---- Right panel ----
        VBox rightPanel = buildRightPanel();
        rightPanel.setPrefWidth(245);
        root.setRight(rightPanel);

        // ---- Canvas click → object selection ----
        mapCanvas.setOnMouseClicked(e -> handleMapClick(e.getX(), e.getY()));

        // ---- Keyboard movement ----
        root.setOnKeyPressed(ev -> handleKey(ev.getCode()));
        root.setFocusTraversable(true);

        return root;
    }

    // ---------- Stats bar ----------

    private HBox buildStatsBar() {
        HBox bar = new HBox(18);
        bar.setPadding(new Insets(8, 14, 8, 14));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #1e0e0e; -fx-border-color: #6b3a2a; -fx-border-width: 0 0 2 0;");

        Label title = statLabel("♦ " + heroName, "#c9a227");

        lblHP     = statLabel("HP: --",     "#e74c3c");
        lblEnergy = statLabel("EN: --",     "#2ecc71");
        lblMana   = statLabel("MANA: --",   "#3498db");
        lblStr    = statLabel("STR: --",    "#e8d5b0");
        lblDef    = statLabel("DEF: --",    "#f39c12");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnMenu = new Button("MENU");
        btnMenu.getStyleClass().add("dungeon-btn");
        btnMenu.setOnAction(e -> {
            engine.pause();
            manager.showMainMenu();
        });

        bar.getChildren().addAll(title, lblHP, lblEnergy, lblMana, lblStr, lblDef, spacer, btnMenu);
        return bar;
    }

    private Label statLabel(String text, String color) {
        Label l = new Label(text);
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        l.setTextFill(Color.web(color));
        return l;
    }

    // ---------- Right panel ----------

    private VBox buildRightPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #1e0e0e; -fx-border-color: #6b3a2a; -fx-border-width: 0 0 0 2;");

        // Inventory section
        Label invTitle = sectionHeader("INVENTORY  (2×4)");
        inventoryGrid = new VBox(3);
        inventoryGrid.setPadding(new Insets(6, 8, 6, 8));
        rebuildInventoryGrid();

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #3d2a2a;");

        // Action menu section
        Label actTitle = sectionHeader("ACTIONS");
        actionPanel = new VBox(4);
        actionPanel.setPadding(new Insets(6, 8, 6, 8));
        Label noSel = new Label("Click an adjacent object");
        noSel.setFont(Font.font("Courier New", 10));
        noSel.setTextFill(Color.web("#8a7060"));
        noSel.setWrapText(true);
        actionPanel.getChildren().add(noSel);

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: #3d2a2a;");

        // AI log section
        Label aiTitle = sectionHeader("AI STATUS");
        lblAiStatus = new Label("Waiting for enemies...");
        lblAiStatus.setFont(Font.font("Courier New", 9));
        lblAiStatus.setTextFill(Color.web("#8a7060"));
        lblAiStatus.setWrapText(true);
        lblAiStatus.setPadding(new Insets(4, 8, 4, 8));

        Separator sep3 = new Separator();
        sep3.setStyle("-fx-background-color: #3d2a2a;");

        // Message log
        Label msgTitle = sectionHeader("LOG");
        messageLog = new TextArea();
        messageLog.setEditable(false);
        messageLog.setWrapText(true);
        messageLog.setPrefRowCount(5);
        messageLog.setStyle(
            "-fx-control-inner-background: #110808;" +
            "-fx-text-fill: #8a7060;" +
            "-fx-font-family: 'Courier New';" +
            "-fx-font-size: 9px;" +
            "-fx-border-color: transparent;"
        );
        VBox.setVgrow(messageLog, Priority.ALWAYS);

        panel.getChildren().addAll(
            invTitle, inventoryGrid, sep1,
            actTitle, actionPanel,   sep2,
            aiTitle,  lblAiStatus,   sep3,
            msgTitle, messageLog
        );
        return panel;
    }

    private Label sectionHeader(String text) {
        Label l = new Label("  " + text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(5, 0, 4, 0));
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        l.setTextFill(Color.web("#c9a227"));
        l.setStyle("-fx-background-color: #2a1a1a; -fx-border-color: #6b3a2a; -fx-border-width: 1 0 1 0;");
        return l;
    }

    // =========================================================================
    //  Engine wiring
    // =========================================================================

    private void wireEngine() {
        engine.setOnMapChanged(   () -> Platform.runLater(this::drawMap));
        engine.setOnStatsChanged( () -> Platform.runLater(this::updateStats));
        engine.setOnMessage(  msg -> Platform.runLater(() -> appendLog(msg)));
        engine.setOnAiLog(    log -> Platform.runLater(() -> {
            lblAiStatus.setText(log);
            lblAiStatus.setTextFill(
                log.contains("CHASING") ? Color.web("#e74c3c") : Color.web("#8a7060")
            );
        }));
    }

    private void refresh() {
        Platform.runLater(() -> {
            drawMap();
            updateStats();
            rebuildInventoryGrid();
        });
    }

    // =========================================================================
    //  Map rendering
    // =========================================================================

    private void drawMap() {
        GridMap grid = engine.getMap();
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();

        gc.setFill(Color.web("#110808"));
        gc.fillRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

        // ---- Base tiles ----
        for (int c = 0; c < grid.getCols(); c++) {
            for (int r = 0; r < grid.getRows(); r++) {
                TileType t = grid.getTile(new Vec2(c, r));
                Color fill = switch (t) {
                    case WALL  -> Color.web("#3d2a2a");
                    case FLOOR -> Color.web("#1e1010");
                    default    -> Color.web("#1e1010");
                };
                gc.setFill(fill);
                gc.fillRect(c * TILE_PX, r * TILE_PX, TILE_PX - 1, TILE_PX - 1);

                // Wall border highlight
                if (t == TileType.WALL) {
                    gc.setStroke(Color.web("#6b3a2a"));
                    gc.setLineWidth(1);
                    gc.strokeRect(c * TILE_PX, r * TILE_PX, TILE_PX - 1, TILE_PX - 1);
                }
            }
        }

        // ---- Highlight 3×3 area around hero ----
        Vec2 heroPos = engine.getHero().getPos();
        for (int dc = -1; dc <= 1; dc++) {
            for (int dr = -1; dr <= 1; dr++) {
                Vec2 cell = heroPos.add(dc, dr);
                if (cell.inBounds(grid.getCols(), grid.getRows())) {
                    gc.setFill(Color.web("#c9a22710"));
                    gc.fillRect(cell.col() * TILE_PX, cell.row() * TILE_PX, TILE_PX - 1, TILE_PX - 1);
                }
            }
        }

        // ---- Game objects ----
        for (var list : engine.getMap().allObjects()) {
            for (GameObject obj : list) {
                drawObject(gc, obj);
            }
        }

        // ---- Enemies ----
        for (var enemy : engine.getEnemies()) {
            drawEnemy(gc, enemy);
        }

        // ---- Hero ----
        drawHero(gc, heroPos);
    }

    private void drawObject(GraphicsContext gc, GameObject obj) {
        int px = obj.getPos().col() * TILE_PX;
        int py = obj.getPos().row() * TILE_PX;

        Color bg = switch (obj.renderTag()) {
            case "WALL"   -> Color.web("#3d2a2a");
            case "CRATE"  -> Color.web("#8B4513");
            case "KEY"    -> Color.web("#c9a227");
            case "GEM"    -> Color.web("#3498db");
            case "POTION" -> Color.web("#c0392b");
            default       -> Color.web("#666666");
        };

        if (obj.blocksMovement()) {
            // Solid fill for static objects
            gc.setFill(bg);
            gc.fillRect(px + 2, py + 2, TILE_PX - 5, TILE_PX - 5);
            gc.setStroke(bg.brighter());
            gc.setLineWidth(1.5);
            gc.strokeRect(px + 2, py + 2, TILE_PX - 5, TILE_PX - 5);
        } else {
            // Circular for items (passable)
            gc.setFill(bg);
            double r = TILE_PX / 3.0;
            gc.fillOval(px + TILE_PX / 2.0 - r, py + TILE_PX / 2.0 - r, r * 2, r * 2);
        }

        // Label
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 8));
        String label = switch (obj.renderTag()) {
            case "CRATE"  -> "CR";
            case "KEY"    -> "K";
            case "GEM"    -> "G";
            case "POTION" -> "P";
            default       -> obj.getName().substring(0, Math.min(2, obj.getName().length())).toUpperCase();
        };
        gc.fillText(label, px + 4, py + TILE_PX - 6);

        // Highlight if selected
        if (obj == selectedObject) {
            gc.setStroke(Color.web("#c9a227"));
            gc.setLineWidth(2);
            gc.strokeRect(px + 1, py + 1, TILE_PX - 3, TILE_PX - 3);
        }
    }

    private void drawHero(GraphicsContext gc, Vec2 pos) {
        int px = pos.col() * TILE_PX;
        int py = pos.row() * TILE_PX;
        gc.setFill(Color.web("#c9a227"));
        double[] xp = {px + TILE_PX/2.0, px + 4, px + TILE_PX - 4};
        double[] yp = {py + 4, py + TILE_PX - 4, py + TILE_PX - 4};
        gc.fillPolygon(xp, yp, 3);
        gc.setStroke(Color.web("#fff8dc"));
        gc.setLineWidth(1.5);
        gc.strokePolygon(xp, yp, 3);
    }

    private void drawEnemy(GraphicsContext gc, Enemy enemy) {
        int px = enemy.getPos().col() * TILE_PX;
        int py = enemy.getPos().row() * TILE_PX;
        Color col = enemy.getState() == EnemyState.CHASING ?
            Color.web("#e74c3c") : Color.web("#8e44ad");
        gc.setFill(col);
        gc.fillRect(px + 4, py + 4, TILE_PX - 9, TILE_PX - 9);
        gc.setStroke(col.brighter());
        gc.setLineWidth(1.5);
        gc.strokeRect(px + 4, py + 4, TILE_PX - 9, TILE_PX - 9);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 7));
        gc.fillText(enemy.getType() == Enemy.Type.KNIGHT ? "KN" : "SC", px + 6, py + TILE_PX - 8);
    }

    // =========================================================================
    //  Stats update
    // =========================================================================

    private void updateStats() {
        Hero h = engine.getHero();
        lblHP.setText("HP: "     + h.getStat(StatType.HP)     + "/" + h.getStatMax(StatType.HP));
        lblEnergy.setText("EN: " + h.getStat(StatType.ENERGY) + "/" + h.getStatMax(StatType.ENERGY));
        lblMana.setText("MANA: " + h.getStat(StatType.MANA)   + "/" + h.getStatMax(StatType.MANA));
        lblStr.setText("STR: "   + h.getStat(StatType.STR));
        lblDef.setText("DEF: "   + h.getStat(StatType.DEF));
    }

    // =========================================================================
    //  Inventory panel
    // =========================================================================

    private void rebuildInventoryGrid() {
        if (inventoryGrid == null) return;
        inventoryGrid.getChildren().clear();

        Inventory inv = engine.getHero().getInventory();
        // 2 columns × 4 rows
        for (int row = 0; row < Inventory.ROWS; row++) {
            HBox rowBox = new HBox(3);
            for (int col = 0; col < Inventory.COLS; col++) {
                int index = row * Inventory.COLS + col;
                GameObject item = inv.get(index);
                Button slot = buildInventorySlot(item);
                rowBox.getChildren().add(slot);
            }
            inventoryGrid.getChildren().add(rowBox);
        }
    }

    private Button buildInventorySlot(GameObject item) {
        Button btn = new Button(item == null ? "" : item.getName().substring(0, Math.min(6, item.getName().length())));
        btn.setPrefSize(106, 28);
        btn.setFont(Font.font("Courier New", FontWeight.BOLD, 8));
        if (item == null) {
            btn.setStyle(
                "-fx-background-color: #110808; -fx-border-color: #3d2a2a; -fx-border-width: 1;" +
                "-fx-text-fill: #3d2a2a; -fx-background-radius: 0; -fx-border-radius: 0;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: #3d2a2a; -fx-border-color: #c9a227; -fx-border-width: 1;" +
                "-fx-text-fill: #e8d5b0; -fx-background-radius: 0; -fx-border-radius: 0; -fx-cursor: hand;"
            );
            final GameObject captured = item;
            btn.setOnAction(e -> showInventoryActions(captured));
        }
        return btn;
    }

    private void showInventoryActions(GameObject item) {
        selectedObject = item;
        actionPanel.getChildren().clear();

        Label header = new Label(item.getName());
        header.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        header.setTextFill(Color.web("#c9a227"));
        actionPanel.getChildren().add(header);

        for (Action action : item.getActions()) {
            Button btn = actionButton(action.getLabel(), () -> {
                engine.executeAction(action, item);
                rebuildInventoryGrid();
                actionPanel.getChildren().clear();
                selectedObject = null;
            });
            actionPanel.getChildren().add(btn);
        }

        // DISCARD action (spec §2.4.2)
        Button discard = actionButton("Discard " + item.getName(), () -> {
            engine.getHero().getInventory().remove(item);
            engine.postMessage("Discarded: " + item.getName());
            engine.notifyStatsChanged();
            rebuildInventoryGrid();
            actionPanel.getChildren().clear();
            selectedObject = null;
        });
        discard.setStyle(discard.getStyle() + "-fx-text-fill: #e74c3c;");
        actionPanel.getChildren().add(discard);
    }

    // =========================================================================
    //  Map click → action menu
    // =========================================================================

    private void handleMapClick(double mouseX, double mouseY) {
        int col = (int)(mouseX / TILE_PX);
        int row = (int)(mouseY / TILE_PX);
        Vec2 clicked = new Vec2(col, row);

        // Check for objects at the clicked cell
        List<GameObject> objs = engine.getMap().objectsAt(clicked);
        if (objs.isEmpty()) {
            actionPanel.getChildren().clear();
            Label lbl = new Label("Nothing there.");
            lbl.setFont(Font.font("Courier New", 10));
            lbl.setTextFill(Color.web("#8a7060"));
            actionPanel.getChildren().add(lbl);
            selectedObject = null;
            drawMap();
            return;
        }

        GameObject obj = objs.get(0);
        selectedObject = obj;

        List<Action> actions = engine.getActionsFor(obj);
        actionPanel.getChildren().clear();

        Label header = new Label(obj.getName());
        header.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        header.setTextFill(Color.web("#c9a227"));
        actionPanel.getChildren().add(header);

        if (actions.isEmpty()) {
            boolean adjacent = engine.getMap().isAdjacent(engine.getHero().getPos(), obj.getPos());
            Label info = new Label(adjacent ? "No actions available." : "Too far away! Move closer.");
            info.setFont(Font.font("Courier New", 9));
            info.setTextFill(adjacent ? Color.web("#8a7060") : Color.web("#e74c3c"));
            info.setWrapText(true);
            actionPanel.getChildren().add(info);
        } else {
            for (Action action : actions) {
                Button btn = actionButton(action.getLabel(), () -> {
                    engine.executeAction(action, obj);
                    rebuildInventoryGrid();
                    actionPanel.getChildren().clear();
                    selectedObject = null;
                    drawMap();
                });
                actionPanel.getChildren().add(btn);
            }
        }
        drawMap();
    }

    private Button actionButton(String label, Runnable onClick) {
        Button btn = new Button(label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        btn.setStyle(
            "-fx-background-color: #3d2a2a; -fx-border-color: #6b3a2a; -fx-border-width: 1;" +
            "-fx-text-fill: #e8d5b0; -fx-padding: 5 8 5 8; -fx-background-radius: 0;" +
            "-fx-border-radius: 0; -fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #6b3a2a; -fx-border-color: #c9a227; -fx-border-width: 1;" +
            "-fx-text-fill: #c9a227; -fx-padding: 5 8 5 8; -fx-background-radius: 0;" +
            "-fx-border-radius: 0; -fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #3d2a2a; -fx-border-color: #6b3a2a; -fx-border-width: 1;" +
            "-fx-text-fill: #e8d5b0; -fx-padding: 5 8 5 8; -fx-background-radius: 0;" +
            "-fx-border-radius: 0; -fx-cursor: hand;"
        ));
        btn.setOnAction(e -> onClick.run());
        return btn;
    }

    // =========================================================================
    //  Keyboard input
    // =========================================================================

    private void handleKey(KeyCode code) {
        boolean moved = switch (code) {
            case UP,    W -> engine.moveHero( 0, -1);
            case DOWN,  S -> engine.moveHero( 0,  1);
            case LEFT,  A -> engine.moveHero(-1,  0);
            case RIGHT, D -> engine.moveHero( 1,  0);
            default       -> false;
        };
        if (moved) {
            drawMap();
            updateStats();
            rebuildInventoryGrid();
        }
    }

    // =========================================================================
    //  Message log
    // =========================================================================

    private void appendLog(String msg) {
        messageLog.appendText(msg + "\n");
    }
}
