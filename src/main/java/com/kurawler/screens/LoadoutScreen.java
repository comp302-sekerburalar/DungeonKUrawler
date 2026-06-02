package com.kurawler.screens;

import com.kurawler.model.UserStore;
import com.kurawler.util.ImageCache;
import com.kurawler.util.GameRenderer;
import com.kurawler.wave.MarketItem;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.util.*;

/**
 * Loadout screen — visually styled as a medieval backpack.
 *
 * The bag_-_empty.png is rendered as the background.
 * Inventory slots are overlaid precisely onto the bag's 4×4 grid.
 * Owned items occupy slots with emoji icons.
 * Clicking a slot shows a detail panel on the right.
 * Equipped items are always shown in a left sidebar.
 */
public class LoadoutScreen extends BaseScreen {

    // ── Bag image grid calibration (in 480×528 display space) ────────────────
    // Original image: 590×648. Display at 480×528 → scale 0.814
    private static final double BAG_W = 480;
    private static final double BAG_H = 528;
    // Grid origin and slot size measured from pixel analysis
    private static final double GRID_X = 76; // left edge of first slot column
    private static final double GRID_Y = 96; // top edge of first slot row
    private static final double SLOT_W = 83; // slot width
    private static final double SLOT_H = 76; // slot height
    private static final double SLOT_GAP_X = 83; // distance between slot left edges (includes divider)
    private static final double SLOT_GAP_Y = 76; // distance between slot top edges

    private static final int COLS = 4;
    private static final int ROWS = 4;

    // ── State ─────────────────────────────────────────────────────────────────
    private final String heroName;
    private final UserStore store;
    private final List<MarketItem> catalogue = MarketItem.buildCatalogue();

    /**
     * All items the player has (owned permanents + consumables as pseudo-items).
     */
    private final List<SlotItem> allItems = new ArrayList<>();
    private SlotItem selectedItem = null;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private StackPane[][] slotPanes;
    private VBox detailPanel;
    private Label equippedWeaponLbl, equippedArmorLbl, equippedSkinLbl;

    // =========================================================================
    public LoadoutScreen(ScreenManager manager) {
        super(manager, true);
        this.heroName = manager.getCurrentHero();
        this.store = manager.getUserStore();
        buildItemList();
        initView();
    }

    // ── Collect all owned items into a flat list ──────────────────────────────
    private void buildItemList() {
        allItems.clear();
        Set<String> owned = store.getOwnedItems(heroName);
        // Permanents
        for (MarketItem m : catalogue) {
            if (!m.isConsumable() && owned.contains(m.getId())) {
                allItems.add(new SlotItem(m, 0));
            }
        }
        // Consumables (with quantity)
        Map<String, Integer> cons = store.getAllConsumables(heroName);
        for (MarketItem m : catalogue) {
            if (m.isConsumable()) {
                int qty = cons.getOrDefault(m.getId(), 0);
                if (qty > 0)
                    allItems.add(new SlotItem(m, qty));
            }
        }
    }

    // =========================================================================
    @Override
    protected Pane buildUI() {
        double W = W(), H = H();

        // Root: dark dungeon background
        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W, H);

        // ── Main layout: left equipped strip | centre bag | right detail ──────
        HBox main = new HBox(0);
        main.setAlignment(Pos.CENTER);
        main.setMaxWidth(W);
        main.setMaxHeight(H);

        VBox leftPanel = buildEquippedPanel();
        StackPane bagArea = buildBagArea();
        VBox rightPanel = buildDetailPanel();

        main.getChildren().addAll(leftPanel, bagArea, rightPanel);
        root.getChildren().add(main);

        // Back button floating top-left
        Button btnBack = new Button("◄  MAIN MENU");
        btnBack.getStyleClass().add("dungeon-btn");
        btnBack.setOnAction(e -> manager.showMainMenu());
        StackPane.setAlignment(btnBack, Pos.TOP_LEFT);
        btnBack.setTranslateX(20);
        btnBack.setTranslateY(20);
        root.getChildren().add(btnBack);

        // Title floating top-centre
        Label title = new Label("LOADOUT");
        title.setFont(Font.font("Courier New", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#c9a227"));
        title.setStyle("-fx-effect:dropshadow(one-pass-box,#8a6b15,6,0,2,2);");
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        title.setTranslateY(24);
        root.getChildren().add(title);

        return root;
    }

    // ── Left panel: equipped items always visible ─────────────────────────────
    private VBox buildEquippedPanel() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(200);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(90, 12, 20, 20));

        Label hdr = panelHeader("EQUIPPED");
        panel.getChildren().add(hdr);

        // Equipped weapon
        String wId = store.getEquippedWeapon(heroName);
        String aId = store.getEquippedArmor(heroName);
        String sId = store.getEquippedSkin(heroName);

        equippedWeaponLbl = equippedLine("⚔", wId, "#e74c3c");
        equippedArmorLbl = equippedLine("🛡", aId, "#3498db");
        equippedSkinLbl = equippedLine("👑", sId, "#f39c12");

        panel.getChildren().addAll(
                spacer(12),
                equippedWeaponLbl,
                spacer(6),
                equippedArmorLbl,
                spacer(6),
                equippedSkinLbl);

        // Separator
        panel.getChildren().add(spacer(20));
        panel.getChildren().add(divider());
        panel.getChildren().add(spacer(16));

        // Consumables overview
        Label conHdr = panelHeader("POTIONS");
        panel.getChildren().add(conHdr);
        panel.getChildren().add(spacer(8));

        Map<String, Integer> cons = store.getAllConsumables(heroName);
        if (cons.isEmpty()) {
            panel.getChildren().add(mutedLabel("None — buy in Shop"));
        } else {
            for (Map.Entry<String, Integer> e : cons.entrySet()) {
                MarketItem item = findById(e.getKey());
                if (item == null)
                    continue;
                String emoji = itemEmoji(item);
                Label l = new Label(emoji + " " + item.getName() + " x" + e.getValue());
                l.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
                l.setTextFill(Color.web("#2ecc71"));
                l.setWrapText(true);
                l.setMaxWidth(170);
                panel.getChildren().add(l);
            }
        }

        // Bottom: coin balance
        panel.getChildren().add(spacer(20));
        panel.getChildren().add(divider());
        panel.getChildren().add(spacer(12));
        int coinBalance = store.getCoins(heroName);
        Label coinLbl = new Label("💰 " + coinBalance + " coins");
        coinLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        coinLbl.setTextFill(Color.web("#f1c40f"));
        panel.getChildren().add(coinLbl);

        return panel;
    }

    // ── Centre: backpack bag with overlaid slot grid ──────────────────────────
    private StackPane buildBagArea() {
        StackPane area = new StackPane();
        area.setPrefSize(BAG_W, BAG_H + 80);
        area.setAlignment(Pos.CENTER);

        // Bag image
        Image bagImg = new Image(getClass().getResourceAsStream("/images/bag_-_empty.png"));
        ImageView bagView = new ImageView(bagImg);
        bagView.setFitWidth(BAG_W);
        bagView.setFitHeight(BAG_H);
        bagView.setPreserveRatio(false);
        bagView.setSmooth(false); // pixel-art: no blurring

        // Slot grid overlay (transparent pane placed exactly over the bag slots)
        Pane gridOverlay = new Pane();
        gridOverlay.setPrefSize(BAG_W, BAG_H);
        slotPanes = new StackPane[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = row * COLS + col;
                SlotItem item = idx < allItems.size() ? allItems.get(idx) : null;

                StackPane slot = buildSlot(item, row, col);
                double x = GRID_X + col * SLOT_GAP_X;
                double y = GRID_Y + row * SLOT_GAP_Y;
                slot.setLayoutX(x);
                slot.setLayoutY(y);

                slotPanes[row][col] = slot;
                gridOverlay.getChildren().add(slot);
            }
        }

        area.getChildren().addAll(bagView, gridOverlay);
        StackPane.setAlignment(bagView, Pos.TOP_CENTER);
        StackPane.setAlignment(gridOverlay, Pos.TOP_CENTER);

        // Subtitle below bag
        Label sub = new Label("Click a slot to inspect · Equip from the panel");
        sub.setFont(Font.font("Courier New", 9));
        sub.setTextFill(Color.web("#8a7060"));
        StackPane.setAlignment(sub, Pos.BOTTOM_CENTER);
        sub.setTranslateY(-8);
        area.getChildren().add(sub);

        return area;
    }

    private StackPane buildSlot(SlotItem item, int row, int col) {
        StackPane slot = new StackPane();
        slot.setPrefSize(SLOT_W, SLOT_H);
        slot.setCursor(item != null ? Cursor.HAND : Cursor.DEFAULT);

        // Base: fully transparent — the bag image's slot already has the parchment
        // colour
        slot.setStyle("-fx-background-color:transparent;");

        if (item != null) {
            // Emoji icon
            Label icon = new Label(itemEmoji(item.item()));
            icon.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 22));
            icon.setAlignment(Pos.CENTER);

            // Quantity badge for consumables
            if (item.qty() > 0) {
                Label qty = new Label("x" + item.qty());
                qty.setFont(Font.font("Courier New", FontWeight.BOLD, 8));
                qty.setTextFill(Color.web("#f1c40f"));
                qty.setStyle("-fx-background-color:#2a1a1a; -fx-padding:1 3 1 3;");
                StackPane.setAlignment(qty, Pos.BOTTOM_RIGHT);
                qty.setTranslateX(-2);
                qty.setTranslateY(-2);
                slot.getChildren().addAll(icon, qty);
            } else {
                slot.getChildren().add(icon);
            }

            // Equipped badge
            boolean equipped = isEquipped(item.item());
            if (equipped) {
                Label badge = new Label("EQ");
                badge.setFont(Font.font("Courier New", FontWeight.BOLD, 7));
                badge.setTextFill(Color.web("#ffffff"));
                badge.setStyle("-fx-background-color:#c9a227; -fx-padding:1 3 1 3; -fx-background-radius:2;");
                StackPane.setAlignment(badge, Pos.TOP_RIGHT);
                badge.setTranslateX(-2);
                badge.setTranslateY(2);
                slot.getChildren().add(badge);
            }

            // ── Hover effects ──
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web(categoryGlow(item.item())));
            glow.setRadius(14);
            glow.setSpread(0.3);

            slot.setOnMouseEntered(e -> {
                slot.setEffect(glow);
                slot.setStyle("-fx-background-color: rgba(201,162,39,0.12); -fx-background-radius:4;");
                ScaleTransition st = new ScaleTransition(Duration.millis(100), slot);
                st.setToX(1.08);
                st.setToY(1.08);
                st.play();
            });
            slot.setOnMouseExited(e -> {
                slot.setEffect(selectedItem == item ? glow : null);
                slot.setStyle(selectedItem == item
                        ? "-fx-background-color:rgba(201,162,39,0.2); -fx-background-radius:4; -fx-border-color:#c9a227; -fx-border-width:1.5; -fx-border-radius:3;"
                        : "-fx-background-color:transparent;");
                ScaleTransition st = new ScaleTransition(Duration.millis(100), slot);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            });
            slot.setOnMouseClicked(e -> selectItem(item));
        } else {
            // Empty slot — subtle indicator
            slot.setStyle("-fx-background-color:rgba(0,0,0,0.0);");
        }

        return slot;
    }

    // ── Right panel: item detail ───────────────────────────────────────────────
    private VBox buildDetailPanel() {
        detailPanel = new VBox(10);
        detailPanel.setPrefWidth(220);
        detailPanel.setAlignment(Pos.TOP_LEFT);
        detailPanel.setPadding(new Insets(90, 20, 20, 12));

        showEmptyDetail();
        return detailPanel;
    }

    private void showEmptyDetail() {
        detailPanel.getChildren().clear();
        Label hdr = panelHeader("ITEM DETAILS");
        Label hint = mutedLabel("Select an item from\nyour backpack to\nview details.");
        detailPanel.getChildren().addAll(hdr, spacer(8), hint);
    }

    private void selectItem(SlotItem item) {
        // Deselect previous
        if (selectedItem != null) {
            int prevIdx = allItems.indexOf(selectedItem);
            if (prevIdx >= 0) {
                int r = prevIdx / COLS, c = prevIdx % COLS;
                if (r < ROWS && slotPanes[r][c] != null)
                    slotPanes[r][c].setStyle("-fx-background-color:transparent;");
            }
        }
        selectedItem = item;

        // Highlight selected slot
        int idx = allItems.indexOf(item);
        if (idx >= 0) {
            int r = idx / COLS, c = idx % COLS;
            if (r < ROWS && slotPanes[r][c] != null) {
                slotPanes[r][c].setStyle(
                        "-fx-background-color:rgba(201,162,39,0.2);" +
                                "-fx-background-radius:4;" +
                                "-fx-border-color:#c9a227; -fx-border-width:1.5; -fx-border-radius:3;");
            }
        }

        populateDetailPanel(item);

        // Bounce animation on detail panel
        ScaleTransition pop = new ScaleTransition(Duration.millis(120), detailPanel);
        pop.setFromX(0.95);
        pop.setToX(1.0);
        pop.setFromY(0.95);
        pop.setToY(1.0);
        pop.play();
    }

    private void populateDetailPanel(SlotItem slotItem) {
        detailPanel.getChildren().clear();
        MarketItem item = slotItem.item();
        boolean equipped = isEquipped(item);

        // Category colour
        String accent = categoryGlow(item);

        // Icon + name header
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(itemEmoji(item));
        iconLbl.setFont(Font.font("Segoe UI Emoji", 26));
        Label nameLbl = new Label(item.getName());
        nameLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
        nameLbl.setTextFill(Color.web(accent));
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(170);
        nameRow.getChildren().addAll(iconLbl, nameLbl);

        // Divider
        Region dv = new Region();
        dv.setPrefHeight(1);
        dv.setMaxWidth(Double.MAX_VALUE);
        dv.setStyle("-fx-background-color:" + accent + "44;");

        // Type badge
        Label typeLbl = new Label(item.getCategory().name().replace("_", " "));
        typeLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
        typeLbl.setTextFill(Color.web(accent));
        typeLbl.setStyle("-fx-background-color:" + accent + "22; -fx-padding:2 8 2 8;");

        // Description
        Label descLbl = new Label(item.getDescription());
        descLbl.setFont(Font.font("Courier New", 10));
        descLbl.setTextFill(Color.web("#8a7060"));
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(200);

        // Stats line
        String statLine = switch (item.getEffectType()) {
            case RESTORE_HP -> "Restores " + item.getEffectValue() + " HP";
            case RESTORE_MANA -> "Restores " + item.getEffectValue() + " Mana";
            case RESTORE_ENERGY -> "Restores " + item.getEffectValue() + " Energy";
            case BOOST_MAX_HP -> "Max HP  +" + item.getEffectValue();
            case BOOST_STR -> "STR  +" + item.getEffectValue();
            case BOOST_DEF -> "DEF  +" + item.getEffectValue();
            case BOOST_MAX_ENERGY -> "Max Energy  +" + item.getEffectValue();
            case GRANT_WEAPON -> "ATK Bonus  +" + item.getEffectValue();
            case SKIN_CHANGE -> "Cosmetic skin change";
        };
        Label statLbl = new Label("▸ " + statLine);
        statLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        statLbl.setTextFill(Color.web("#e8d5b0"));

        // Status
        String statusTxt = equipped ? "✓  EQUIPPED"
                : (item.isConsumable() ? "x" + slotItem.qty() + " in stash" : "OWNED");
        Color statusColor = equipped ? Color.web("#2ecc71") : Color.web("#8a7060");
        Label statusLbl = new Label(statusTxt);
        statusLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        statusLbl.setTextFill(statusColor);

        detailPanel.getChildren().addAll(nameRow, dv, typeLbl, spacer(4), descLbl, statLbl, statusLbl, spacer(8));

        // Action buttons
        if (item.getCategory() == MarketItem.Category.WEAPON && !equipped) {
            detailPanel.getChildren().add(actionBtn("⚔  EQUIP WEAPON", accent, () -> {
                store.equipWeapon(heroName, item.getId());
                refreshAll();
            }));
        } else if (item.getCategory() == MarketItem.Category.ARMOR && !equipped) {
            detailPanel.getChildren().add(actionBtn("🛡  EQUIP ARMOR", accent, () -> {
                store.equipArmor(heroName, item.getId());
                refreshAll();
            }));
        } else if (item.getCategory() == MarketItem.Category.SKIN && !equipped) {
            detailPanel.getChildren().add(actionBtn("👑  APPLY SKIN", accent, () -> {
                store.equipSkin(heroName, item.getId());
                refreshAll();
            }));
        } else if (item.isConsumable() && slotItem.qty() > 0) {
            detailPanel.getChildren().add(actionBtn("❤  USE NOW", "#2ecc71", () -> {
                store.useConsumable(heroName, item.getId());
                refreshAll();
            }));
        } else if (equipped) {
            Label eq = new Label("[ EQUIPPED ]");
            eq.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
            eq.setTextFill(Color.web("#2ecc71"));
            eq.setStyle(
                    "-fx-background-color:#0a2a0a; -fx-padding:6 16; -fx-border-color:#2ecc71; -fx-border-width:1;");
            detailPanel.getChildren().add(eq);
        }
    }

    // ── Refresh entire screen after equip/use ─────────────────────────────────
    private void refreshAll() {
        buildItemList();
        selectedItem = null;
        // Rebuild UI
        Pane newView = buildUI();
        manager.getRoot().getChildren().setAll(newView);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean isEquipped(MarketItem item) {
        return switch (item.getCategory()) {
            case WEAPON -> item.getId().equals(store.getEquippedWeapon(heroName));
            case ARMOR -> item.getId().equals(store.getEquippedArmor(heroName));
            case SKIN -> item.getId().equals(store.getEquippedSkin(heroName));
            default -> false;
        };
    }

    private String itemEmoji(MarketItem item) {
        return switch (item.getCategory()) {
            case WEAPON -> "⚔";
            case ARMOR -> "🛡";
            case SKIN -> "👑";
            case POWER_UP -> switch (item.getEffectType()) {
                case BOOST_STR -> "💪";
                case BOOST_DEF -> "🔰";
                default -> "⚡";
            };
            case CONSUMABLE -> switch (item.getEffectType()) {
                case RESTORE_HP -> "❤";
                case RESTORE_MANA -> "🔵";
                case RESTORE_ENERGY -> "⚡";
                default -> "🧪";
            };
        };
    }

    private String categoryGlow(MarketItem item) {
        return switch (item.getCategory()) {
            case WEAPON -> "#e74c3c";
            case ARMOR -> "#3498db";
            case SKIN -> "#f39c12";
            case POWER_UP -> "#9b59b6";
            case CONSUMABLE -> "#2ecc71";
        };
    }

    private MarketItem findById(String id) {
        return catalogue.stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
    }

    private Label equippedLine(String emoji, String itemId, String color) {
        String name = "None";
        if (itemId != null) {
            MarketItem m = findById(itemId);
            if (m != null)
                name = m.getName();
        }
        Label l = new Label(emoji + "  " + name);
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        l.setTextFill(Color.web(itemId != null ? color : "#3d2a2a"));
        l.setWrapText(true);
        l.setMaxWidth(180);
        return l;
    }

    private Button actionBtn(String label, String accent, Runnable action) {
        Button btn = new Button(label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        String norm = "-fx-background-color:" + accent + "22; -fx-border-color:" + accent + "; -fx-border-width:1.5;" +
                "-fx-text-fill:" + accent
                + "; -fx-padding:8 14; -fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;";
        String hov = "-fx-background-color:" + accent + "55; -fx-border-color:" + accent + "; -fx-border-width:1.5;" +
                "-fx-text-fill:#ffffff; -fx-padding:8 14; -fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;";
        btn.setStyle(norm);
        btn.setOnMouseEntered(e -> btn.setStyle(hov));
        btn.setOnMouseExited(e -> btn.setStyle(norm));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private Label panelHeader(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        l.setTextFill(Color.web("#c9a227"));
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(4, 0, 4, 0));
        l.setStyle("-fx-border-color:transparent transparent #6b3a2a transparent; -fx-border-width:0 0 1 0;");
        return l;
    }

    private Label mutedLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Courier New", 10));
        l.setTextFill(Color.web("#8a7060"));
        l.setWrapText(true);
        l.setMaxWidth(200);
        return l;
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setPrefHeight(h);
        return r;
    }

    private Region divider() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:#3d2a2a;");
        return r;
    }

    // ── Inner record ──────────────────────────────────────────────────────────
    /** Wraps a MarketItem with its quantity (0 for permanents). */
    private record SlotItem(MarketItem item, int qty) {
    }
}
