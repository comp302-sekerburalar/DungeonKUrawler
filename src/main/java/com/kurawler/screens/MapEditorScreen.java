package com.kurawler.screens;

import com.kurawler.engine.*;
import com.kurawler.game.objects.*;
import com.kurawler.util.SpriteRenderer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.util.*;

/**
 * In-game level editor (spec §4.2).
 *
 * Toolbar at top: drag tiles/objects from the palette onto the map grid.
 * Buttons (spec §4.2):
 * 1. Save map (JSON)
 * 2. Load map (open saved map in editor)
 * 3. Run map (play the designed map)
 * 4. Add 5 random items + 1 hidden item
 * 5. Clear map
 * 6. Exit to main menu
 *
 * Left-click = place selected palette item
 * Right-click = erase tile / object
 * Mouse drag = paint walls continuously
 */
public class MapEditorScreen extends BaseScreen {

    // Grid constants
    private static final int COLS = 20;
    private static final int ROWS = 15;
    private static final int TILE = 32;

    private final String heroName;
    private final GridMap map;
    private String currentMapName;

    // Editor state
    private String selectedPaletteItem = "WALL"; // what the brush paints
    private boolean isDragging = false;

    // UI
    private Canvas mapCanvas;
    private Label lblStatus;
    private Label lblSelected;

    // ── Palette definition ───────────────────────────────────────────────────
    // Each entry: [id, displayLabel, spriteSheet, spriteCol, spriteRow]
    private static final String[][] PALETTE = {
            // Structural
            { "WALL", "Wall", "walls_and_statics_x2", "0", "0" },
            { "FLOOR", "Floor", "", "-1", "-1" },
            { "COLUMN", "Column", "walls_and_statics_x2", "1", "2" },
            { "CRATE", "Crate", "containers_x2", "0", "2" },
            { "BREAK_CRATE", "B.Crate", "containers_x2", "1", "0" },
            { "CHEST", "Chest", "containers_x2", "0", "0" },
            { "SEARCH_WALL", "S.Wall", "walls_and_statics_x2", "3", "1" },
            // Items
            { "KEY", "Key", "items_x2", "0", "0" },
            { "GEM", "Gem", "items_x2", "4", "1" },
            { "RING", "Ring", "items_x2", "0", "1" },
            { "POTION_RED", "H.Potion", "items_x2", "1", "0" },
            { "POTION_BLUE", "M.Potion", "items_x2", "2", "0" },
            { "POTION_GREEN", "E.Potion", "items_x2", "3", "0" },
            { "BOOK", "Spell Book", "items_x2", "3", "2" },
            // Weapons
            { "WEAPON_SWORD", "Sword", "weapons_x2", "0", "0" },
            { "WEAPON_DAGGER", "Dagger", "weapons_x2", "1", "0" },
            { "WEAPON_AXE", "Axe", "weapons_x2", "2", "0" },
            { "WEAPON_BOW", "Bow", "weapons_x2", "8", "9" },
            { "WEAPON_GREATSWORD", "G.Sword", "weapons_x2", "0", "4" },
            // Armour
            { "ARMOR_LEATHER", "L.Armor", "items_x2", "4", "2" },
            { "ARMOR_CHAIN", "C.Armor", "items_x2", "5", "2" },
    };

    // ── Constructors ─────────────────────────────────────────────────────────

    /** New blank map. */
    public MapEditorScreen(ScreenManager manager, String heroName) {
        super(manager, true);
        this.heroName = heroName;
        this.currentMapName = "untitled";
        this.map = new GridMap(COLS, ROWS);
        map.buildBorderWalls();
        initView();
        Platform.runLater(this::redraw);
    }

    /** Load existing map for editing. */
    public MapEditorScreen(ScreenManager manager, String heroName, GridMap existingMap, String mapName) {
        super(manager, true);
        this.heroName = heroName;
        this.currentMapName = mapName;
        this.map = existingMap;
        initView();
        Platform.runLater(this::redraw);
    }

    // =========================================================================
    // UI construction
    // =========================================================================
    @Override
    protected Pane buildUI() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W(), H());

        // Top: palette + toolbar
        root.setTop(buildTopArea());

        // Center: map canvas
        mapCanvas = new Canvas(TILE * COLS, TILE * ROWS);
        ScrollPane scroll = new ScrollPane(mapCanvas);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setStyle("-fx-background-color:#110808; -fx-border-color:transparent;");

        StackPane canvasWrap = new StackPane(scroll);
        canvasWrap.setStyle("-fx-background-color:#110808;");
        root.setCenter(canvasWrap);

        // Bottom: status bar
        root.setBottom(buildStatusBar());

        // Mouse handlers
        mapCanvas.setOnMousePressed(e -> {
            isDragging = true;
            handleCanvasMouse(e.getX(), e.getY(), e.getButton());
        });
        mapCanvas.setOnMouseDragged(e -> {
            if (isDragging)
                handleCanvasMouse(e.getX(), e.getY(), e.getButton());
        });
        mapCanvas.setOnMouseReleased(e -> isDragging = false);

        return root;
    }

    // ── Top area: palette + action buttons ───────────────────────────────────

    private VBox buildTopArea() {
        VBox top = new VBox(0);
        top.setStyle("-fx-background-color:#1e0e0e; -fx-border-color:#6b3a2a; -fx-border-width:0 0 2 0;");

        // Row 1: action toolbar
        top.getChildren().add(buildToolbar());

        // Row 2: palette
        top.getChildren().add(buildPalette());

        return top;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(6);
        bar.setPadding(new Insets(6, 10, 5, 10));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-border-color:transparent transparent #3d2a2a transparent; -fx-border-width:0 0 1 0;");

        Text lbl = new Text("MAP EDITOR");
        lbl.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        lbl.setFill(Color.web("#c9a227"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // spec §4.2 buttons
        Button btnSave = toolBtn("💾 SAVE", "#c9a227");
        Button btnLoad = toolBtn("📂 LOAD", "#3498db");
        Button btnRun = toolBtn("▶ PLAY", "#2ecc71");
        Button btnTeam = toolBtn("TEAM", "#7ee8fa");
        Button btnRandom = toolBtn("⚄ POPULATE", "#9b59b6");
        Button btnClear = toolBtn("🗑 CLEAR", "#e74c3c");
        Button btnExit = toolBtn("✕ EXIT", "#8a7060");

        btnSave.setOnAction(e -> saveMap());
        btnLoad.setOnAction(e -> loadMap());
        btnRun.setOnAction(e -> runMap());
        btnTeam.setOnAction(e -> runTeamMatch());
        btnRandom.setOnAction(e -> populateRandom());
        btnClear.setOnAction(e -> clearMap());
        btnExit.setOnAction(e -> manager.showMainMenu());

        // Tooltip hints
        Tooltip.install(btnSave, new Tooltip("Save map to disk (JSON)"));
        Tooltip.install(btnLoad, new Tooltip("Load another saved map"));
        Tooltip.install(btnRun, new Tooltip("Play this map now"));
        Tooltip.install(btnTeam, new Tooltip("Run this map in Team Match mode"));
        Tooltip.install(btnRandom, new Tooltip("Add 5 random items + 1 hidden item (spec §4.2 #4)"));
        Tooltip.install(btnClear, new Tooltip("Remove all objects from the map"));
        Tooltip.install(btnExit, new Tooltip("Return to main menu"));

        bar.getChildren().addAll(lbl, spacer, btnSave, btnLoad, btnRun, btnTeam, btnRandom, btnClear, btnExit);
        return bar;
    }

    private HBox buildPalette() {
        HBox palette = new HBox(2);
        palette.setPadding(new Insets(5, 10, 5, 10));
        palette.setAlignment(Pos.CENTER_LEFT);

        Text lbl = new Text("BRUSH: ");
        lbl.setFont(Font.font("Courier New", 9));
        lbl.setFill(Color.web("#8a7060"));
        palette.getChildren().add(lbl);

        for (String[] item : PALETTE) {
            String id = item[0];
            String label = item[1];
            String sheet = item[2];
            int sc = Integer.parseInt(item[3]);
            int sr = Integer.parseInt(item[4]);

            StackPane cell = new StackPane();
            cell.setPrefSize(36, 36);
            cell.setStyle(
                    "-fx-background-color:#1e0e0e; -fx-border-color:#3d2a2a;" +
                            "-fx-border-width:1; -fx-cursor:hand;");
            cell.setUserData(id);

            // Sprite or coloured fill
            if (sc >= 0 && !sheet.isEmpty()) {
                Canvas icon = new Canvas(30, 30);
                SpriteRenderer.drawTile(icon.getGraphicsContext2D(), sheet, sc, sr, 0, 0, 30, 30);
                cell.getChildren().add(icon);
            } else {
                // FLOOR = dark rectangle
                javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle(24, 24, Color.web("#221212"));
                cell.getChildren().add(r);
            }

            // Tooltip with name
            Tooltip.install(cell, new Tooltip(label));

            cell.setOnMouseClicked(e -> selectPaletteItem(id, cell));
            cell.setOnMouseEntered(e -> {
                if (!id.equals(selectedPaletteItem))
                    cell.setStyle(
                            "-fx-background-color:#2a1a1a; -fx-border-color:#6b3a2a; -fx-border-width:1; -fx-cursor:hand;");
            });
            cell.setOnMouseExited(e -> {
                if (!id.equals(selectedPaletteItem))
                    cell.setStyle(
                            "-fx-background-color:#1e0e0e; -fx-border-color:#3d2a2a; -fx-border-width:1; -fx-cursor:hand;");
            });

            palette.getChildren().add(cell);
        }

        return palette;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(14);
        bar.setPadding(new Insets(4, 10, 4, 10));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#1e0e0e; -fx-border-color:#3d2a2a; -fx-border-width:1 0 0 0;");

        lblSelected = statusLabel("Brush: WALL");
        lblStatus = statusLabel("Left-click / drag to paint  |  Right-click to erase");

        Label hint = statusLabel("Map: " + currentMapName);

        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);
        bar.getChildren().addAll(lblSelected, lblStatus, s, hint);
        return bar;
    }

    // =========================================================================
    // Palette selection
    // =========================================================================

    private StackPane lastSelected = null;

    private void selectPaletteItem(String id, StackPane cell) {
        selectedPaletteItem = id;
        // Highlight selected
        if (lastSelected != null)
            lastSelected.setStyle(
                    "-fx-background-color:#1e0e0e; -fx-border-color:#3d2a2a; -fx-border-width:1; -fx-cursor:hand;");
        cell.setStyle("-fx-background-color:#3d2a2a; -fx-border-color:#c9a227; -fx-border-width:2; -fx-cursor:hand;");
        lastSelected = cell;
        lblSelected.setText("Brush: " + id.replace("_", " "));
    }

    // =========================================================================
    // Canvas mouse handling
    // =========================================================================

    private void handleCanvasMouse(double mouseX, double mouseY, MouseButton btn) {
        int col = (int) (mouseX / TILE);
        int row = (int) (mouseY / TILE);
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS)
            return;
        Vec2 pos = new Vec2(col, row);

        if (btn == MouseButton.SECONDARY) {
            // Right-click: erase
            erase(pos);
        } else {
            // Left-click: paint
            paint(pos);
        }
        redraw();
    }

    private void paint(Vec2 pos) {
        // Remove any existing objects at this cell first
        List<GameObject> existing = new ArrayList<>(map.objectsAt(pos));
        existing.forEach(map::removeObject);

        switch (selectedPaletteItem) {
            case "WALL" -> map.setTile(pos, TileType.WALL);
            case "FLOOR" -> map.setTile(pos, TileType.FLOOR);
            default -> {
                // Ensure the tile is FLOOR before placing an object
                if (map.getTile(pos) != TileType.WALL) {
                    GameObject obj = createObject(selectedPaletteItem, pos);
                    if (obj != null)
                        map.placeObject(obj);
                }
            }
        }
    }

    private void erase(Vec2 pos) {
        // Remove all objects
        List<GameObject> objs = new ArrayList<>(map.objectsAt(pos));
        objs.forEach(map::removeObject);
        // Reset tile to floor (unless it was a border wall)
        if (pos.col() > 0 && pos.col() < COLS - 1 && pos.row() > 0 && pos.row() < ROWS - 1)
            map.setTile(pos, TileType.FLOOR);
    }

    private GameObject createObject(String type, Vec2 pos) {
        return switch (type) {
            case "COLUMN" -> GameObjects.column(pos);
            case "CRATE" -> GameObjects.crate(pos);
            case "BREAK_CRATE" -> GameObjects.breakableCrate(pos);
            case "CHEST" -> GameObjects.chest(pos);
            case "SEARCH_WALL" -> GameObjects.searchableWall(pos, null);
            case "KEY" -> GameObjects.key(pos);
            case "GEM" -> GameObjects.gem(pos);
            case "RING" -> GameObjects.ring(pos);
            case "POTION_RED" -> GameObjects.redPotion(pos);
            case "POTION_BLUE" -> GameObjects.bluePotion(pos);
            case "POTION_GREEN" -> GameObjects.greenPotion(pos);
            case "BOOK" -> GameObjects.spellBook(pos);
            case "WEAPON_SWORD" -> GameObjects.sword(pos);
            case "WEAPON_DAGGER" -> GameObjects.dagger(pos);
            case "WEAPON_AXE" -> GameObjects.axe(pos);
            case "WEAPON_BOW" -> GameObjects.bow(pos);
            case "WEAPON_GREATSWORD" -> GameObjects.greatSword(pos);
            case "ARMOR_LEATHER" -> GameObjects.leatherArmor(pos);
            case "ARMOR_CHAIN" -> GameObjects.chainArmor(pos);
            default -> null;
        };
    }

    // =========================================================================
    // Map rendering
    // =========================================================================

    private void redraw() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();

        // Background
        gc.setFill(Color.web("#110808"));
        gc.fillRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

        // Tiles
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                double px = c * TILE, py = r * TILE;
                TileType t = map.getTile(new Vec2(c, r));

                if (t == TileType.WALL) {
                    SpriteRenderer.drawTile(gc, "walls_and_statics_x2",
                            (c + r) % 3 == 0 ? 1 : 0, 0, px, py, TILE, TILE);
                } else {
                    // Floor — checker pattern
                    gc.setFill((c + r) % 2 == 0 ? Color.web("#1e1010") : Color.web("#1a0e0e"));
                    gc.fillRect(px, py, TILE - 1, TILE - 1);
                    // Grid lines
                    gc.setStroke(Color.web("#2a1a1a", 0.4));
                    gc.setLineWidth(0.5);
                    gc.strokeRect(px, py, TILE - 1, TILE - 1);
                }
            }
        }

        // Objects
        for (var list : map.allObjects()) {
            for (GameObject obj : list)
                drawObj(gc, obj);
        }

        // Hero spawn indicator (always at 5,5)
        gc.setFill(Color.web("#c9a227", 0.25));
        gc.fillRect(5 * TILE + 1, 5 * TILE + 1, TILE - 3, TILE - 3);
        gc.setFill(Color.web("#c9a227", 0.7));
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 8));
        gc.fillText("HERO", 5 * TILE + 3, 5 * TILE + 20);
    }

    private void drawObj(GraphicsContext gc, GameObject obj) {
        double px = obj.getPos().col() * TILE;
        double py = obj.getPos().row() * TILE;
        String sheet = obj.getSpriteSheet();
        int sc = obj.getSpriteCol(), sr = obj.getSpriteRow();

        if (sheet != null && !sheet.isEmpty() && sc >= 0) {
            // blockers: full tile; passables: inset
            double off = obj.blocksMovement() ? 0 : 3;
            SpriteRenderer.drawTile(gc, sheet, sc, sr, px + off, py + off, TILE - off * 2, TILE - off * 2);
        } else {
            gc.setFill(Color.web("#555555"));
            gc.fillRect(px + 4, py + 4, TILE - 9, TILE - 9);
        }
    }

    // =========================================================================
    // Toolbar actions (spec §4.2)
    // =========================================================================

    /** Save the current map to JSON (spec §4.2 #1). */
    private void saveMap() {
        TextInputDialog dlg = new TextInputDialog(currentMapName);
        dlg.setTitle("Save Map");
        dlg.setHeaderText("SAVE MAP");
        dlg.setContentText("Map name:");
        dlg.showAndWait().ifPresent(name -> {
            if (name.isBlank())
                return;
            currentMapName = name.trim();
            try {
                MapSerializer.save(map, currentMapName);
                setStatus("Map saved: " + currentMapName);
            } catch (Exception e) {
                showErr("Save failed: " + e.getMessage());
            }
        });
    }

    /** Load another saved map in editor (spec §4.2 #2). */
    private void loadMap() {
        List<String[]> maps = MapSerializer.listSavedMaps();
        if (maps.isEmpty()) {
            setStatus("No saved maps found.");
            return;
        }

        List<String> names = maps.stream().map(m -> m[0] + "  (" + m[1] + ")").toList();
        ChoiceDialog<String> dlg = new ChoiceDialog<>(names.get(0), names);
        dlg.setTitle("Load Map");
        dlg.setHeaderText("SELECT A SAVED MAP");
        dlg.setContentText("Map:");
        dlg.showAndWait().ifPresent(chosen -> {
            String chosenName = chosen.split("  \\(")[0];
            try {
                GridMap loaded = MapSerializer.load(chosenName);
                manager.showMapEditor(heroName, loaded, chosenName);
            } catch (Exception e) {
                showErr("Load failed: " + e.getMessage());
            }
        });
    }

    /** Run the current map in play mode (spec §4.2 #3). */
    private void runMap() {
        // Validate: map needs at least border walls
        manager.startGame(heroName, map);
    }

    /** Run the current map in Team Match mode. */
    private void runTeamMatch() {
        manager.startTeamMatch(heroName, map);
    }

    /**
     * Add 5 random items + 1 hidden item (spec §4.2 #4).
     * Container items: if roll 8+ → put random item inside, repeat till < 8.
     */
    private void populateRandom() {
        Random rng = new Random();
        List<Vec2> floors = map.allFloorCells();
        if (floors.size() < 6) {
            setStatus("Not enough floor space!");
            return;
        }
        Collections.shuffle(floors, rng);

        int placed = 0;
        for (Vec2 p : floors) {
            if (placed >= 5)
                break;
            if (map.objectsAt(p).isEmpty() && !isHeroSpawn(p)) {
                GameObject item = randomItemForPopulate(rng, p);
                map.placeObject(item);
                placed++;
            }
        }

        // 1 hidden item in a searchable location (spec §4.2 #4 last bullet)
        List<Vec2> remaining = map.allFloorCells();
        remaining.removeIf(v -> !map.objectsAt(v).isEmpty() || isHeroSpawn(v));
        if (!remaining.isEmpty()) {
            Collections.shuffle(remaining, rng);
            Vec2 hiddenPos = remaining.get(0);
            GameObject hidden = GameObjects.gem(hiddenPos);
            map.placeObject(GameObjects.searchableWall(hiddenPos, hidden));
        }

        redraw();
        setStatus("Added 5 random items + 1 hidden item.");
    }

    private GameObject randomItemForPopulate(Random rng, Vec2 pos) {
        int roll = rng.nextInt(10);
        if (roll < 2)
            return GameObjects.redPotion(pos);
        if (roll < 4)
            return GameObjects.randomWeapon(pos);
        if (roll < 5)
            return GameObjects.randomArmor(pos);
        if (roll < 6)
            return GameObjects.key(pos);
        if (roll < 7)
            return GameObjects.bluePotion(pos);
        if (roll < 8)
            return GameObjects.gem(pos);
        return GameObjects.crate(pos); // container: chest mechanic
    }

    /**
     * Clear the map — removes all objects, resets interior tiles to FLOOR (spec
     * §4.2 #5).
     */
    private void clearMap() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Map");
        confirm.setHeaderText("CLEAR ALL OBJECTS?");
        confirm.setContentText("This removes all placed items and resets the interior to floor.");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                map.clearAll();
                // Reset interior tiles to floor, keep borders as wall
                for (int c = 1; c < COLS - 1; c++)
                    for (int r = 1; r < ROWS - 1; r++)
                        map.setTile(new Vec2(c, r), TileType.FLOOR);
                redraw();
                setStatus("Map cleared.");
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isHeroSpawn(Vec2 v) {
        return Math.abs(v.col() - 5) <= 1 && Math.abs(v.row() - 5) <= 1;
    }

    private void setStatus(String msg) {
        if (lblStatus != null)
            lblStatus.setText(msg);
    }

    private Button toolBtn(String label, String color) {
        Button btn = new Button(label);
        btn.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        String norm = "-fx-background-color:transparent; -fx-border-color:" + color
                + "; -fx-border-width:1; -fx-text-fill:" + color
                + "; -fx-padding:4 8 4 8; -fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;";
        String hov = "-fx-background-color:" + color + "22; -fx-border-color:" + color
                + "; -fx-border-width:1; -fx-text-fill:" + color
                + "; -fx-padding:4 8 4 8; -fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;";
        btn.setStyle(norm);
        btn.setOnMouseEntered(e -> btn.setStyle(hov));
        btn.setOnMouseExited(e -> btn.setStyle(norm));
        return btn;
    }

    private Label statusLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Courier New", 9));
        l.setTextFill(Color.web("#8a7060"));
        return l;
    }

    private void showErr(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Editor Error");
        a.setContentText(msg);
        a.showAndWait();
    }
}
