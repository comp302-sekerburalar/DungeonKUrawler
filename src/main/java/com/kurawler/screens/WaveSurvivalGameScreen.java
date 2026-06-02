package com.kurawler.screens;

import com.kurawler.engine.*;
import com.kurawler.game.entity.*;
import com.kurawler.game.objects.GameObject;
import com.kurawler.util.SpriteRenderer;
import com.kurawler.util.HeroAnimator;
import com.kurawler.util.KnightAnimator;
import com.kurawler.util.SorcererAnimator;
import com.kurawler.util.GameRenderer;
import com.kurawler.util.ImageCache;
import com.kurawler.wave.*;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.util.*;

/**
 * Wave Survival game screen.
 *
 * Key bindings:
 * WASD / Arrows — move hero
 * 1 — use first consumable (HP potion)
 * 2 — use second consumable (Mana potion)
 * 3 — use third consumable (Energy potion)
 * ESC — pause menu
 *
 * Combat: click adjacent enemy → auto-attack if weapon equipped.
 */
public class WaveSurvivalGameScreen extends BaseScreen {

    private static final int TILE = 36, MAP_W = 20, MAP_H = 15;

    private final WaveEngine engine;
    private final String difficulty;

    private Canvas mapCanvas;
    private StackPane canvasStack;

    // HUD labels
    private Label lblWave, lblTimer, lblEnemiesLeft, lblScore, lblCoins, lblStreak;

    // Stats
    private ProgressBar barHp, barEnergy, barMana;
    private Label lblHpVal, lblEnVal, lblMpVal, lblStr, lblDef;

    // Sidebar
    private VBox actionPanel;
    private VBox consumablePanel;
    private TextArea logArea;
    private Label lblEnemyStatus;

    // Overlays
    private Label coinPop;
    private StackPane marketOverlay = null;

    private Enemy selectedEnemy = null;
    private final HeroAnimator heroAnimator = new HeroAnimator();
    private final KnightAnimator knightAnimator = new KnightAnimator();
    private final SorcererAnimator sorcererAnimator = new SorcererAnimator();

    // =========================================================================
    public WaveSurvivalGameScreen(ScreenManager manager, String difficulty) {
        super(manager, true);
        this.difficulty = difficulty;
        this.engine = new WaveEngine(
                manager.getCurrentHero(),
                WaveDifficulty.fromLabel(difficulty),
                manager.getUserStore());
        initView();
        wireEngine();
        Platform.runLater(() -> {
            redrawMap();
            updateStats();
            updateHUD();
            updateConsumablePanel();
            engine.startSession();
        });
    }

    @Override
    protected Pane buildUI() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W(), H());

        root.setTop(buildHUD());
        root.setRight(buildSidebar());

        mapCanvas = new Canvas(TILE * MAP_W, TILE * MAP_H);
        canvasStack = new StackPane(mapCanvas);
        canvasStack.setStyle("-fx-background-color:#110808;");
        ScrollPane scroll = new ScrollPane(canvasStack);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setStyle("-fx-background-color:#110808; -fx-border-color:transparent;");
        scroll.setPrefSize(552, 535);
        root.setCenter(scroll);

        coinPop = new Label("");
        coinPop.setFont(Font.font("Courier New", FontWeight.BOLD, 16));
        coinPop.setTextFill(Color.web("#f1c40f"));
        coinPop.setVisible(false);
        canvasStack.getChildren().add(coinPop);
        StackPane.setAlignment(coinPop, Pos.CENTER);

        mapCanvas.setOnMouseClicked(e -> handleClick(e.getX(), e.getY()));
        root.setOnKeyPressed(e -> handleKey(e.getCode()));
        root.setFocusTraversable(true);
        return root;
    }

    // ── HUD ──────────────────────────────────────────────────────────────────
    private HBox buildHUD() {
        HBox bar = new HBox(0);
        bar.setStyle(
                "-fx-background-color:#2a1a1a;" +
                        "-fx-border-color:#8a5a2a; -fx-border-width:0 0 3 0;" +
                        "-fx-effect:dropshadow(gaussian,black,8,0.4,0,4);");
        bar.setMinHeight(72);

        // Hero portrait
        javafx.scene.image.ImageView portrait = new javafx.scene.image.ImageView();
        javafx.scene.image.Image pi = com.kurawler.util.ImageCache.get("hero_portrait.png");
        if (pi != null)
            portrait.setImage(pi);
        portrait.setFitWidth(68);
        portrait.setFitHeight(68);
        portrait.setPreserveRatio(false);
        portrait.setStyle("-fx-effect:dropshadow(gaussian,black,4,0.5,0,0);");
        HBox portraitWrap = new HBox(portrait);
        portraitWrap.setPadding(new Insets(2, 8, 2, 6));
        portraitWrap.setAlignment(Pos.CENTER);
        portraitWrap.setStyle("-fx-background-color:#1a0e0e;");

        // Wave counter block
        lblWave = hbl("WAVE 0", "#7ee8fa", true);
        lblTimer = hbl("0:00", "#8a7060", false);
        lblEnemiesLeft = hbl("0/0", "#e74c3c", true);
        lblScore = hbl("0", "#c9a227", true);
        lblCoins = hbl("0", "#f1c40f", true);
        lblStreak = hbl("x0", "#9b59b6", true);

        bar.getChildren().addAll(
                portraitWrap, hudSep(),
                hudBlock(lblWave, lblTimer), hudSep(),
                hudBlock(hbl("ENEMIES", "#8a7060", false), lblEnemiesLeft), hudSep(),
                hudBlock(hbl("SCORE", "#8a7060", false), lblScore), hudSep(),
                hudBlock(hbl("COINS", "#8a7060", false), lblCoins), hudSep(),
                hudBlock(hbl("STREAK", "#8a7060", false), lblStreak));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Menu image button
        javafx.scene.image.ImageView menuImg = new javafx.scene.image.ImageView();
        javafx.scene.image.Image mi = com.kurawler.util.ImageCache.get("menu_button.png");
        if (mi != null)
            menuImg.setImage(mi);
        menuImg.setFitWidth(60);
        menuImg.setFitHeight(60);
        menuImg.setPreserveRatio(true);
        menuImg.setCursor(javafx.scene.Cursor.HAND);
        menuImg.setOnMouseClicked(e -> {
            engine.pause();
            showPauseMenu();
        });
        menuImg.setOnMouseEntered(e -> menuImg.setOpacity(0.8));
        menuImg.setOnMouseExited(e -> menuImg.setOpacity(1.0));
        HBox menuWrap = new HBox(menuImg);
        menuWrap.setPadding(new Insets(4, 8, 4, 4));
        menuWrap.setAlignment(Pos.CENTER);

        bar.getChildren().addAll(spacer, menuWrap);
        return bar;
    }

    private VBox hudBlock(Label... labels) {
        VBox v = new VBox(1);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(5, 16, 5, 16));
        v.setStyle("-fx-border-color:transparent #3d2a2a transparent transparent; -fx-border-width:0 1 0 0;");
        v.getChildren().addAll(labels);
        return v;
    }

    private Region hudSep() {
        Region r = new Region();
        r.setPrefWidth(1);
        r.setStyle("-fx-background-color:#3d2a2a;");
        return r;
    }

    private Label hbl(String t, String c, boolean bold) {
        Label l = new Label(t);
        l.setFont(Font.font("Courier New", bold ? FontWeight.BOLD : FontWeight.NORMAL, bold ? 13 : 9));
        l.setTextFill(Color.web(c));
        return l;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private VBox buildSidebar() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(248);
        panel.setStyle("-fx-background-color:#1e0e0e; -fx-border-color:#6b3a2a; -fx-border-width:0 0 0 2;");

        panel.getChildren().add(secHead("STATS"));
        VBox sbox = new VBox(4);
        sbox.setPadding(new Insets(6, 8, 6, 8));
        barHp = sbar("#e74c3c");
        lblHpVal = sv();
        barEnergy = sbar("#2ecc71");
        lblEnVal = sv();
        barMana = sbar("#3498db");
        lblMpVal = sv();
        sbox.getChildren().addAll(
                srow("♥ HP", "#e74c3c", barHp, lblHpVal),
                srow("⚡ EN", "#2ecc71", barEnergy, lblEnVal),
                srow("✦ MP", "#3498db", barMana, lblMpVal));
        lblStr = gl("STR:--");
        lblDef = gl("DEF:--");
        HBox sd = new HBox(12, lblStr, lblDef);
        sd.setPadding(new Insets(2, 8, 2, 8));
        sbox.getChildren().add(sd);
        panel.getChildren().add(sbox);

        // Consumables panel
        panel.getChildren().addAll(div(), secHead("POTIONS  [1][2][3]"));
        consumablePanel = new VBox(3);
        consumablePanel.setPadding(new Insets(5, 8, 5, 8));
        panel.getChildren().add(consumablePanel);

        panel.getChildren().addAll(div(), secHead("ACTIONS"));
        actionPanel = new VBox(4);
        actionPanel.setPadding(new Insets(5, 8, 5, 8));
        actionPanel.getChildren().add(gl("Click enemy to attack"));
        panel.getChildren().add(actionPanel);

        panel.getChildren().addAll(div(), secHead("AI STATUS"));
        lblEnemyStatus = new Label("Waiting...");
        lblEnemyStatus.setFont(Font.font("Courier New", 9));
        lblEnemyStatus.setTextFill(Color.web("#8a7060"));
        lblEnemyStatus.setWrapText(true);
        lblEnemyStatus.setPadding(new Insets(3, 8, 3, 8));
        panel.getChildren().add(lblEnemyStatus);

        panel.getChildren().addAll(div(), secHead("LOG"));
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(6);
        logArea.setStyle(
                "-fx-control-inner-background:#110808;-fx-text-fill:#8a7060;-fx-font-family:'Courier New';-fx-font-size:9px;-fx-border-color:transparent;");
        VBox.setVgrow(logArea, Priority.ALWAYS);
        panel.getChildren().add(logArea);
        return panel;
    }

    // ── Wire ──────────────────────────────────────────────────────────────────
    private void wireEngine() {
        engine.setOnMapChanged(() -> Platform.runLater(this::redrawMap));
        engine.setOnStatsChanged(() -> Platform.runLater(this::updateStats));
        engine.setOnStateChanged(() -> Platform.runLater(this::updateHUD));
        engine.setOnMessage(m -> Platform.runLater(() -> addLog(m)));
        engine.setOnCoinDrop(n -> Platform.runLater(() -> showCoinPop(n)));
        engine.setOnWaveComplete(() -> Platform.runLater(this::showMarketOverlay));
        engine.setOnMarketClose(() -> Platform.runLater(this::closeMarketOverlay));
        engine.setOnGameOver(() -> Platform.runLater(this::showGameOver));
        engine.setOnInventoryChanged(() -> Platform.runLater(this::updateConsumablePanel));
    }

    // ── Map rendering ─────────────────────────────────────────────────────────
    private void redrawMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#110808"));
        gc.fillRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

        GridMap grid = engine.getMap();
        for (int c = 0; c < MAP_W; c++)
            for (int r = 0; r < MAP_H; r++) {
                double px = c * TILE, py = r * TILE;
                GameRenderer.drawTile(gc, grid.getTile(new Vec2(c, r)), c, r, px, py, TILE);
            }

        Vec2 h = engine.getHero().getPos();
        gc.setFill(Color.web("#c9a22710"));
        for (int dc = -1; dc <= 1; dc++)
            for (int dr = -1; dr <= 1; dr++) {
                Vec2 c = h.add(dc, dr);
                if (c.inBounds(MAP_W, MAP_H))
                    gc.fillRect(c.col() * TILE, c.row() * TILE, TILE - 1, TILE - 1);
            }

        for (Projectile p : engine.getProjectiles()) {
            if (!p.isActive())
                continue;
            GameRenderer.drawProjectile(gc, p.getPos().col() * TILE, p.getPos().row() * TILE, TILE);
        }

        for (Enemy e : engine.getEnemies()) {
            double px = e.getPos().col() * TILE, py = e.getPos().row() * TILE;
            if (e.getType() == Enemy.Type.KNIGHT && e.getState() == EnemyState.CHASING)
                knightAnimator.step();
            else if (e.getType() == Enemy.Type.SORCERER)
                sorcererAnimator.step();
            GameRenderer.drawEnemy(gc, e, px, py, TILE, knightAnimator, sorcererAnimator, e == selectedEnemy);
        }

        int sk = engine.getSkinIndex();
        javafx.scene.paint.Color skinTint = switch (sk) {
            case 1 -> Color.web("#e74c3c", 0.4);
            case 2 -> Color.web("#3498db", 0.4);
            case 3 -> Color.web("#2ecc71", 0.4);
            case 4 -> Color.web("#f1c40f", 0.4);
            default -> null;
        };
        heroAnimator.draw(gc, h.col() * TILE, h.row() * TILE, TILE, TILE, skinTint);
        if (engine.getHero().hasWeaponEquipped()) {
            gc.setFill(Color.web("#c9a227", 0.9));
            gc.fillRect(h.col() * TILE + TILE - 8, h.row() * TILE, 8, 8);
        }
    }

    // ── HUD update ────────────────────────────────────────────────────────────
    private void updateHUD() {
        WaveState s = engine.getState();
        WaveDifficulty d = engine.getDiff();
        int wave = s.getCurrentWave();
        lblWave.setText("WAVE " + wave);
        lblWave.setTextFill(Color.web(wave <= 3 ? "#7ee8fa" : wave <= 6 ? "#c9a227" : "#e74c3c"));
        // Show BOSS on boss waves
        if (wave > 0 && wave % 5 == 0) {
            lblWave.setText("WAVE " + wave + " 👑");
            lblWave.setTextFill(Color.web("#f1c40f"));
        }
        double sec = s.getWaveTimerSec();
        lblTimer.setText(String.format("%d:%02d", (int) sec / 60, (int) sec % 60));
        lblEnemiesLeft.setText(s.getEnemiesLeft() + "/" + s.getEnemiesThisWave());
        lblScore.setText(String.format("%,d", s.getTotalScore()));
        // Show real-time UserStore coins
        int realCoins = engine.getUserStore().getCoins(engine.getHero().getName());
        lblCoins.setText(realCoins + " \uD83E\uDE99");
        int streak = s.getCurrentStreak();
        lblStreak.setText("x" + streak);
        lblStreak.setTextFill(streak >= d.streakBonusThreshold ? Color.web("#f1c40f") : Color.web("#9b59b6"));

        if (!engine.getEnemies().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Enemy e : engine.getEnemies())
                sb.append(e.getType()).append(" #").append(e.getId()).append(" ").append(e.getState()).append("\n");
            lblEnemyStatus.setText(sb.toString().trim());
            lblEnemyStatus.setTextFill(engine.getEnemies().stream().anyMatch(e -> e.getState() == EnemyState.CHASING)
                    ? Color.web("#e74c3c")
                    : Color.web("#8a7060"));
        } else {
            lblEnemyStatus.setText(s.isMarketOpen() ? "Market open!" : "Wave clear!");
            lblEnemyStatus.setTextFill(Color.web("#2ecc71"));
        }

        if (s.isMarketOpen() && actionPanel != null) {
            actionPanel.getChildren().clear();
            Label l = new Label("SHOP CLOSES IN " + (int) Math.ceil(s.getMarketTimerSec()) + "s");
            l.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
            l.setTextFill(Color.web("#f1c40f"));
            actionPanel.getChildren().add(l);
        }
    }

    private void updateStats() {
        Hero h = engine.getHero();
        int hp = h.getStat(StatType.HP), mhp = h.getStatMax(StatType.HP);
        int en = h.getStat(StatType.ENERGY), men = h.getStatMax(StatType.ENERGY);
        int mp = h.getStat(StatType.MANA), mmp = h.getStatMax(StatType.MANA);
        barHp.setProgress((double) hp / mhp);
        lblHpVal.setText(hp + "/" + mhp);
        barEnergy.setProgress((double) en / men);
        lblEnVal.setText(en + "/" + men);
        barMana.setProgress((double) mp / mmp);
        lblMpVal.setText(mp + "/" + mmp);
        lblStr.setText("STR:" + h.getStat(StatType.STR));
        lblDef.setText("DEF:" + h.getStat(StatType.DEF));
    }

    /** Rebuild the consumable inventory display (keys 1/2/3). */
    private void updateConsumablePanel() {
        if (consumablePanel == null)
            return;
        consumablePanel.getChildren().clear();
        Map<String, Integer> stash = engine.getUserStore().getAllConsumables(engine.getHero().getName());
        if (stash.isEmpty()) {
            consumablePanel.getChildren().add(gl("No potions  (buy in Shop)"));
            return;
        }
        int slot = 1;
        for (Map.Entry<String, Integer> entry : stash.entrySet()) {
            if (slot > 3)
                break; // max 3 bound slots
            String id = entry.getKey();
            int qty = entry.getValue();
            // Find display name from catalogue
            String displayName = engine.getShopInventory().stream()
                    .filter(m -> m.getId().equals(id)).map(MarketItem::getName).findFirst().orElse(id);
            Label l = new Label("[" + slot + "] " + displayName + " x" + qty);
            l.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
            l.setTextFill(Color.web(qty > 0 ? "#2ecc71" : "#5a5a5a"));
            l.setMaxWidth(Double.MAX_VALUE);
            l.setPadding(new Insets(2, 0, 2, 0));
            consumablePanel.getChildren().add(l);
            slot++;
        }
        if (stash.size() > 3) {
            Label more = gl("+" + (stash.size() - 3) + " more in shop");
            consumablePanel.getChildren().add(more);
        }
    }

    // ── Market overlay ────────────────────────────────────────────────────────
    private void showMarketOverlay() {
        if (marketOverlay != null)
            return;
        marketOverlay = new StackPane();
        marketOverlay.setStyle("-fx-background-color:rgba(0,0,0,0.80);");
        VBox panel = buildMarketPanel();
        panel.setTranslateY(400);
        marketOverlay.getChildren().add(panel);
        canvasStack.getChildren().add(marketOverlay);
        TranslateTransition tt = new TranslateTransition(Duration.millis(280), panel);
        tt.setToY(0);
        tt.play();
    }

    private void closeMarketOverlay() {
        if (marketOverlay == null)
            return;
        canvasStack.getChildren().remove(marketOverlay);
        marketOverlay = null;
        updateConsumablePanel();
    }

    private VBox buildMarketPanel() {
        VBox panel = new VBox(0);
        panel.setMaxWidth(680);
        panel.setMaxHeight(500);
        panel.setStyle("-fx-background-color:#1a0e0e; -fx-border-color:#c9a227; -fx-border-width:2;");

        HBox hdr = new HBox(10);
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setPadding(new Insets(10, 16, 10, 16));
        hdr.setStyle(
                "-fx-background-color:#2a1a1a; -fx-border-color:transparent transparent #6b3a2a transparent; -fx-border-width:0 0 1 0;");
        Label title = new Label("\u2694  WAVE SHOP  \u2694");
        title.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
        title.setTextFill(Color.web("#c9a227"));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label coinLbl = new Label(
                "\uD83D\uDCB0 " + engine.getUserStore().getCoins(engine.getHero().getName()) + " coins");
        coinLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        coinLbl.setTextFill(Color.web("#f1c40f"));
        Button skip = new Button("NEXT WAVE");
        skip.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        skip.setStyle(
                "-fx-background-color:#7a2318;-fx-border-color:#c0392b;-fx-border-width:1;-fx-text-fill:#ffccc0;-fx-padding:4 10;-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;");
        skip.setOnAction(e -> {
            engine.getState().closeMarket();
            closeMarketOverlay();
        });
        hdr.getChildren().addAll(title, sp, coinLbl, skip);
        panel.getChildren().add(hdr);

        HBox tabs = new HBox(0);
        tabs.setStyle("-fx-background-color:#1e0e0e;");
        String[] cats = { "ALL", "CONSUMABLE", "POWER_UP", "WEAPON", "ARMOR", "SKIN" };
        Label[] tabBtns = new Label[cats.length];
        VBox grid = buildItemGrid(null, coinLbl);

        for (int i = 0; i < cats.length; i++) {
            final String cat = cats[i];
            Label tb = new Label(cat.equals("ALL") ? "ALL" : cat.replace("_", " "));
            tb.setFont(Font.font("Courier New", FontWeight.BOLD, 8));
            tb.setPadding(new Insets(6, 12, 6, 12));
            tb.setStyle(i == 0
                    ? "-fx-background-color:#2a1a1a;-fx-text-fill:#c9a227;-fx-cursor:hand;-fx-border-color:transparent transparent #c9a227 transparent;-fx-border-width:0 0 2 0;"
                    : "-fx-background-color:#1e0e0e;-fx-text-fill:#8a7060;-fx-cursor:hand;");
            tabBtns[i] = tb;
            tb.setOnMouseClicked(e -> {
                for (Label lb : tabBtns)
                    lb.setStyle("-fx-background-color:#1e0e0e;-fx-text-fill:#8a7060;-fx-cursor:hand;");
                tb.setStyle(
                        "-fx-background-color:#2a1a1a;-fx-text-fill:#c9a227;-fx-cursor:hand;-fx-border-color:transparent transparent #c9a227 transparent;-fx-border-width:0 0 2 0;");
                MarketItem.Category filter = cat.equals("ALL") ? null : MarketItem.Category.valueOf(cat);
                grid.getChildren().clear();
                grid.getChildren().addAll(buildItemGrid(filter, coinLbl).getChildren());
            });
            tabs.getChildren().add(tb);
        }
        panel.getChildren().add(tabs);

        ScrollPane sp2 = new ScrollPane(grid);
        sp2.setFitToWidth(true);
        sp2.setPrefHeight(370);
        sp2.setStyle("-fx-background-color:#1a0e0e;-fx-border-color:transparent;");
        VBox.setVgrow(sp2, Priority.ALWAYS);
        panel.getChildren().add(sp2);
        return panel;
    }

    private VBox buildItemGrid(MarketItem.Category filter, Label coinLbl) {
        VBox grid = new VBox(1);
        grid.setPadding(new Insets(4, 8, 4, 8));
        for (MarketItem item : engine.getShopInventory()) {
            if (filter != null && item.getCategory() != filter)
                continue;
            // Permanent items already owned: skip
            if (!item.isConsumable() && engine.isPurchased(item.getId()))
                continue;
            grid.getChildren().add(makeItemRow(item, coinLbl, grid, filter));
        }
        if (grid.getChildren().isEmpty()) {
            Label e = gl("  Nothing here.");
            e.setPadding(new Insets(8, 0, 8, 0));
            grid.getChildren().add(e);
        }
        return grid;
    }

    private HBox makeItemRow(MarketItem item, Label coinLbl, VBox grid, MarketItem.Category filter) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(7, 10, 7, 10));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-background-color:#1e0e0e;-fx-border-color:transparent transparent #2a1a1a transparent;-fx-border-width:0 0 1 0;");

        String cc = switch (item.getCategory()) {
            case CONSUMABLE -> "#2ecc71";
            case WEAPON -> "#e74c3c";
            case ARMOR -> "#3498db";
            case POWER_UP -> "#9b59b6";
            case SKIN -> "#f39c12";
        };
        Label sw = new Label("■");
        sw.setFont(Font.font("Courier New", 14));
        sw.setTextFill(Color.web(cc));

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        // Show qty for consumables
        String nameTxt = item.getName();
        if (item.isConsumable()) {
            int qty = engine.getUserStore().getConsumableQty(engine.getHero().getName(), item.getId());
            if (qty > 0)
                nameTxt += " (owned: " + qty + ")";
        }
        Label nm = new Label(nameTxt);
        nm.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        nm.setTextFill(Color.web("#e8d5b0"));
        Label ds = new Label(item.getDescription());
        ds.setFont(Font.font("Courier New", 9));
        ds.setTextFill(Color.web("#8a7060"));
        info.getChildren().addAll(nm, ds);

        boolean can = engine.canAfford(item);
        Label pr = new Label("\uD83D\uDCB0 " + item.getPrice());
        pr.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        pr.setTextFill(can ? Color.web("#f1c40f") : Color.web("#7a5050"));

        Button buy = new Button(can ? (item.isConsumable() ? "BUY +1" : "BUY") : "NEED " + item.getPrice());
        buy.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        buy.setStyle(can
                ? "-fx-background-color:#1a3a1a;-fx-border-color:#2ecc71;-fx-border-width:1;-fx-text-fill:#2ecc71;-fx-padding:4 10;-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;"
                : "-fx-background-color:#1e0e0e;-fx-border-color:#3d2a2a;-fx-border-width:1;-fx-text-fill:#3d2a2a;-fx-padding:4 10;-fx-background-radius:0;-fx-border-radius:0;");
        buy.setDisable(!can);
        buy.setOnAction(e -> {
            if (engine.purchase(item)) {
                coinLbl.setText(
                        "\uD83D\uDCB0 " + engine.getUserStore().getCoins(engine.getHero().getName()) + " coins");
                grid.getChildren().clear();
                grid.getChildren().addAll(buildItemGrid(filter, coinLbl).getChildren());
                updateConsumablePanel();
            }
        });

        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color:#2a1a1a;-fx-border-color:transparent transparent #2a1a1a transparent;-fx-border-width:0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color:#1e0e0e;-fx-border-color:transparent transparent #2a1a1a transparent;-fx-border-width:0 0 1 0;"));
        row.getChildren().addAll(sw, info, pr, buy);
        return row;
    }

    // ── Input ─────────────────────────────────────────────────────────────────
    private void handleClick(double mx, double my) {
        int col = (int) (mx / TILE), row = (int) (my / TILE);
        Vec2 clicked = new Vec2(col, row);
        for (Enemy e : engine.getEnemies()) {
            if (e.getPos().equals(clicked)) {
                // Auto-attack if adjacent and armed
                if (engine.getHero().hasWeaponEquipped()
                        && engine.getMap().isAdjacent(engine.getHero().getPos(), e.getPos())) {
                    engine.heroAttackEnemy(e);
                    selectedEnemy = null;
                    actionPanel.getChildren().clear();
                    actionPanel.getChildren().add(gl("Click enemy to attack"));
                    redrawMap();
                    return;
                }
                selectedEnemy = e;
                showEnemyPanel(e);
                redrawMap();
                return;
            }
        }
        selectedEnemy = null;
        actionPanel.getChildren().clear();
        actionPanel.getChildren().add(gl("Click enemy to attack"));
        redrawMap();
    }

    private void showEnemyPanel(Enemy e) {
        actionPanel.getChildren().clear();
        Label hdr = new Label(e.getType() + " #" + e.getId());
        hdr.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        hdr.setTextFill(Color.web("#e74c3c"));
        actionPanel.getChildren().addAll(hdr, gl("HP: " + e.getStat(StatType.HP)), gl("State: " + e.getState()));
        if (!engine.getHero().hasWeaponEquipped())
            actionPanel.getChildren().add(gl("No weapon equipped!"));
        else if (!engine.getMap().isAdjacent(engine.getHero().getPos(), e.getPos()))
            actionPanel.getChildren().add(gl("Move closer to attack."));
    }

    private void handleKey(KeyCode code) {
        if (engine.getState().isSessionOver())
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
            case DIGIT1, NUMPAD1 -> engine.useConsumable(1);
            case DIGIT2, NUMPAD2 -> engine.useConsumable(2);
            case DIGIT3, NUMPAD3 -> engine.useConsumable(3);
            case ESCAPE -> {
                engine.pause();
                showPauseMenu();
            }
            default -> {
            }
        }
    }

    // ── Coin pop ──────────────────────────────────────────────────────────────
    private void showCoinPop(int n) {
        coinPop.setText("\uD83D\uDCB0 +" + n);
        coinPop.setVisible(true);
        coinPop.setTranslateY(0);
        coinPop.setOpacity(1);
        FadeTransition ft = new FadeTransition(Duration.millis(1400), coinPop);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> coinPop.setVisible(false));
        TranslateTransition tt = new TranslateTransition(Duration.millis(1400), coinPop);
        tt.setFromY(0);
        tt.setToY(-50);
        tt.setOnFinished(e -> coinPop.setTranslateY(0));
        new ParallelTransition(ft, tt).play();
    }

    // ── Game over / pause ──────────────────────────────────────────────────────
    private void showGameOver() {
        engine.stop();
        WaveState s = engine.getState();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Game Over");
        a.setHeaderText("YOU HAVE FALLEN");
        a.setContentText(String.format("Wave reached: %d\nEnemies slain: %d\nFinal score: %,d\nMax streak: x%d",
                s.getCurrentWave(), s.getTotalKills(), s.getTotalScore(), s.getMaxStreak()));
        ButtonType retry = new ButtonType("Try Again"), menu = new ButtonType("Main Menu");
        a.getButtonTypes().setAll(retry, menu);
        a.showAndWait().ifPresent(bt -> {
            if (bt == retry)
                manager.startWaveSurvival(difficulty);
            else
                manager.showMainMenu();
        });
    }

    private void showPauseMenu() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Paused");
        a.setHeaderText("GAME PAUSED");
        ButtonType res = new ButtonType("Resume"), menu = new ButtonType("Main Menu");
        a.getButtonTypes().setAll(res, menu);
        a.showAndWait().ifPresent(bt -> {
            if (bt == menu) {
                engine.stop();
                manager.showMainMenu();
            } else
                engine.resume();
        });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private void addLog(String m) {
        if (logArea != null)
            logArea.appendText(m + "\n");
    }

    private Label gl(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("Courier New", 9));
        l.setTextFill(Color.web("#8a7060"));
        return l;
    }

    private Label sv() {
        Label l = new Label("--/--");
        l.setFont(Font.font("Courier New", 9));
        l.setTextFill(Color.web("#e8d5b0"));
        return l;
    }

    private ProgressBar sbar(String c) {
        ProgressBar b = new ProgressBar(1);
        b.setPrefWidth(78);
        b.setPrefHeight(7);
        b.setStyle("-fx-accent:" + c + ";");
        return b;
    }

    private HBox srow(String lbl, String c, ProgressBar bar, Label val) {
        Label l = new Label(lbl);
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        l.setTextFill(Color.web(c));
        l.setMinWidth(36);
        HBox r = new HBox(4, l, bar, val);
        r.setAlignment(Pos.CENTER_LEFT);
        return r;
    }

    private Label secHead(String t) {
        Label l = new Label("  " + t);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(4, 0, 3, 0));
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        l.setTextFill(Color.web("#c9a227"));
        l.setStyle("-fx-background-color:#2a1a1a;-fx-border-color:#6b3a2a;-fx-border-width:1 0 1 0;");
        return l;
    }

    private Region div() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:#3d2a2a;");
        return r;
    }
}
