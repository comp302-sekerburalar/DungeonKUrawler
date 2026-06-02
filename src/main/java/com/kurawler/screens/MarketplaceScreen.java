package com.kurawler.screens;

import com.kurawler.model.UserStore;
import com.kurawler.util.SpriteRenderer;
import com.kurawler.util.ImageCache;
import com.kurawler.wave.MarketItem;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.util.*;

/**
 * Merchant Marketplace — redesigned as a fantasy RPG merchant inventory.
 *
 * Visual design matches the Loadout backpack aesthetic:
 * ● Dark medieval dungeon palette
 * ● Wooden-frame item cards with pixel sprite icons from game sprite sheets
 * ● Gold coin counter with animated purchase flash
 * ● Left tab strip for category navigation
 * ● Right detail panel that slides in on card click
 * ● Owned/quantity badges on every card
 *
 * Sprite icon mapping (all from items_x2.png & weapons_x2.png at 32×32):
 * Potions : items_x2 [0,1]=red [0,2]=blue [0,3]=green
 * Keys : items_x2 [0,0]
 * Rings : items_x2 [1,0]=ring1 [1,1]=ring2 [1,2]=ring3
 * Gem/Scroll : items_x2 [1,4] [3,2]
 * Armour : items_x2 [2,4]=leather [2,5]=chain
 * Weapons : weapons_x2 [0,0]=sword [0,1]=dagger [0,2]=axe [4,0]=greatsword
 * Coins : items_x2 [3,1]
 */
public class MarketplaceScreen extends BaseScreen {

    // ── Item image filenames — direct lookup, no sprite sheet math ─────────────
    /** Maps item id → exact image filename in /resources/images/ */
    private static final Map<String, String> ITEM_IMAGES = new LinkedHashMap<>();
    static {
        // Potions (dedicated standalone images)
        ITEM_IMAGES.put("hp_small", "potion_hp.png");
        ITEM_IMAGES.put("hp_large", "potion_hp.png");
        ITEM_IMAGES.put("mana_pot", "potion_mana.png");
        ITEM_IMAGES.put("energy_pot", "potion_energy.png");
        // Power-ups (use ring/coin images)
        ITEM_IMAGES.put("max_hp_up", "potion_hp.png");
        ITEM_IMAGES.put("str_up", "ring_blue.png");
        ITEM_IMAGES.put("def_up", "ring_green.png");
        ITEM_IMAGES.put("energy_up", "ring_blue.png");
        // Weapons — real weapon sprites from weapons_x3.png
        ITEM_IMAGES.put("w_dagger", "weapon_dagger.png");
        ITEM_IMAGES.put("w_sword", "weapon_sword.png");
        ITEM_IMAGES.put("w_axe", "weapon_axe.png");
        ITEM_IMAGES.put("w_greatsword", "weapon_greatsword.png");
        // Armour — use coin/key images
        ITEM_IMAGES.put("armor_leather", "coins.png");
        ITEM_IMAGES.put("armor_chain", "coins.png");
        ITEM_IMAGES.put("armor_plate", "coins.png");
        // Skins
        ITEM_IMAGES.put("skin_red", "ring_blue.png");
        ITEM_IMAGES.put("skin_blue", "ring_green.png");
        ITEM_IMAGES.put("skin_green", "ring_blue.png");
        ITEM_IMAGES.put("skin_gold", "coins.png");
    }

    /**
     * Public accessor so other screens (LoadoutScreen) can use the same mapping.
     */
    public static String getItemImage(String itemId) {
        return ITEM_IMAGES.get(itemId);
    }

    // ── Category tabs ─────────────────────────────────────────────────────────
    private record Tab(String id, String label, String emoji, MarketItem.Category filter) {
    }

    private static final Tab[] TABS = {
            new Tab("all", "ALL", "⊞", null),
            new Tab("potion", "POTIONS", "🧪", MarketItem.Category.CONSUMABLE),
            new Tab("weapon", "WEAPONS", "⚔", MarketItem.Category.WEAPON),
            new Tab("armor", "ARMOUR", "🛡", MarketItem.Category.ARMOR),
            new Tab("power", "POWER-UPS", "⚡", MarketItem.Category.POWER_UP),
            new Tab("skin", "SKINS", "👑", MarketItem.Category.SKIN),
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private final String heroName;
    private final UserStore store;
    private final List<MarketItem> catalogue = MarketItem.buildCatalogue();

    private Tab activeTab = TABS[0];
    private MarketItem selectedItem = null;
    private Label lblCoins;
    private Label lblNotify;
    private FlowPane cardGrid;
    private VBox detailPanel;
    private Label[] tabLabels;

    // Coin flash animation target
    private StackPane coinFlash;

    // =========================================================================
    public MarketplaceScreen(ScreenManager manager, String heroName) {
        super(manager, true);
        this.heroName = (heroName == null || heroName.isBlank()) ? "GUEST" : heroName;
        this.store = manager.getUserStore();
        initView();
    }

    // =========================================================================
    @Override
    protected Pane buildUI() {
        double W = W(), H = H();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W, H);

        root.setTop(buildTopBar(W));
        root.setLeft(buildTabStrip());
        root.setCenter(buildCentre());
        root.setRight(buildDetailPanel());

        return root;
    }

    // ── Top bar ───────────────────────────────────────────────────────────────
    private HBox buildTopBar(double W) {
        HBox bar = new HBox(14);
        bar.setPadding(new Insets(12, 20, 12, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
                "-fx-background-color:#0d0808;" +
                        "-fx-border-color:#6b3a2a; -fx-border-width:0 0 3 0;");

        // Merchant title
        Label icon = label("🏪", 24, "#c9a227", true);
        Label title = label("MERCHANT  SHOP", 20, "#c9a227", true);
        title.setStyle("-fx-effect:dropshadow(one-pass-box,#8a6b15,5,0,2,2);");
        Label sub = label("Trade your hard-earned coins for equipment and supplies", 9, "#8a7060", false);
        VBox titleBox = new VBox(2, title, sub);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Coin counter with sprite icon
        Canvas coinIcon = new Canvas(28, 28);
        SpriteRenderer.drawTile(coinIcon.getGraphicsContext2D(), "items_x2", 1, 3, 0, 0, 28, 28);

        int coins = store.getCoins(heroName);
        lblCoins = label(String.valueOf(coins), 18, "#f1c40f", true);
        lblCoins.setStyle("-fx-effect:dropshadow(one-pass-box,#8a6000,3,0,1,1);");

        coinFlash = new StackPane();
        coinFlash.setStyle("-fx-background-color:transparent;");
        coinFlash.setOpacity(0);
        coinFlash.setPrefSize(60, 28);

        HBox coinRow = new HBox(6, coinIcon, lblCoins, coinFlash);
        coinRow.setAlignment(Pos.CENTER);
        coinRow.setPadding(new Insets(6, 14, 6, 14));
        coinRow.setStyle(
                "-fx-background-color:#1a0e00;" +
                        "-fx-border-color:#8a6000; -fx-border-width:2;");

        // Back
        Button btnBack = new Button("◄  MAIN MENU");
        btnBack.getStyleClass().add("dungeon-btn");
        btnBack.setOnAction(e -> manager.showMainMenu());

        // Notification bar
        lblNotify = label("", 10, "#2ecc71", false);
        HBox notifyWrap = new HBox(lblNotify);
        notifyWrap.setAlignment(Pos.CENTER);
        notifyWrap.setPadding(new Insets(0, 16, 0, 16));
        HBox.setHgrow(notifyWrap, Priority.SOMETIMES);

        bar.getChildren().addAll(icon, titleBox, spacer, notifyWrap, coinRow, btnBack);
        return bar;
    }

    // ── Left tab strip ────────────────────────────────────────────────────────
    private VBox buildTabStrip() {
        VBox strip = new VBox(0);
        strip.setPrefWidth(130);
        strip.setStyle(
                "-fx-background-color:#0d0808;" +
                        "-fx-border-color:#6b3a2a; -fx-border-width:0 2 0 0;");

        Label hdr = label("  CATEGORIES", 9, "#6b3a2a", true);
        hdr.setPadding(new Insets(14, 0, 10, 0));
        hdr.setMaxWidth(Double.MAX_VALUE);
        strip.getChildren().add(hdr);

        tabLabels = new Label[TABS.length];
        for (int i = 0; i < TABS.length; i++) {
            Tab tab = TABS[i];
            Label lbl = new Label(tab.emoji() + "  " + tab.label());
            lbl.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
            lbl.setPadding(new Insets(10, 14, 10, 14));
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setCursor(Cursor.HAND);
            tabLabels[i] = lbl;
            applyTabStyle(lbl, tab == activeTab);

            lbl.setOnMouseClicked(e -> {
                activeTab = tab;
                refreshTabs();
                rebuildCards();
            });
            lbl.setOnMouseEntered(e -> {
                if (tab != activeTab)
                    lbl.setStyle(tabHoverStyle());
            });
            lbl.setOnMouseExited(e -> applyTabStyle(lbl, tab == activeTab));
            strip.getChildren().add(lbl);
        }

        // Bottom: owned count
        Region spring = new Region();
        VBox.setVgrow(spring, Priority.ALWAYS);
        strip.getChildren().add(spring);

        int owned = store.getOwnedItems(heroName).size();
        Label ownedLbl = label("  Owned: " + owned, 9, "#8a7060", false);
        ownedLbl.setPadding(new Insets(10, 0, 10, 0));
        ownedLbl.setStyle("-fx-border-color:#3d2a2a; -fx-border-width:1 0 0 0;");
        ownedLbl.setMaxWidth(Double.MAX_VALUE);
        strip.getChildren().add(ownedLbl);

        return strip;
    }

    private void applyTabStyle(Label lbl, boolean active) {
        if (active) {
            lbl.setStyle("-fx-background-color:#1e0e0e; -fx-text-fill:#c9a227; " +
                    "-fx-border-color:#c9a227; -fx-border-width:0 3 0 0; -fx-cursor:hand;");
            lbl.setTextFill(Color.web("#c9a227"));
        } else {
            lbl.setStyle("-fx-background-color:transparent; -fx-text-fill:#8a7060; " +
                    "-fx-border-color:transparent; -fx-cursor:hand;");
            lbl.setTextFill(Color.web("#8a7060"));
        }
    }

    private String tabHoverStyle() {
        return "-fx-background-color:#1a0e0e; -fx-text-fill:#e8d5b0; -fx-border-color:transparent; -fx-cursor:hand;";
    }

    private void refreshTabs() {
        for (int i = 0; i < TABS.length; i++)
            applyTabStyle(tabLabels[i], TABS[i] == activeTab);
    }

    // ── Centre: card grid ─────────────────────────────────────────────────────
    private ScrollPane buildCentre() {
        cardGrid = new FlowPane();
        cardGrid.setHgap(14);
        cardGrid.setVgap(14);
        cardGrid.setPadding(new Insets(20, 20, 20, 20));
        cardGrid.setStyle("-fx-background-color:#140a0a;");
        rebuildCards();

        ScrollPane sp = new ScrollPane(cardGrid);
        sp.setFitToWidth(true);
        sp.setFitToHeight(false);
        sp.setStyle("-fx-background-color:#140a0a; -fx-border-color:transparent;");
        return sp;
    }

    private void rebuildCards() {
        cardGrid.getChildren().clear();
        for (MarketItem item : catalogue) {
            if (activeTab.filter() != null && item.getCategory() != activeTab.filter())
                continue;
            cardGrid.getChildren().add(buildCard(item));
        }
        if (cardGrid.getChildren().isEmpty()) {
            Label empty = label("Nothing available.", 12, "#3d2a2a", false);
            empty.setPadding(new Insets(40));
            cardGrid.getChildren().add(empty);
        }
    }

    // ── Item card ─────────────────────────────────────────────────────────────
    private VBox buildCard(MarketItem item) {
        boolean owned = !item.isConsumable() && store.ownsItem(heroName, item.getId());
        boolean canBuy = !owned && store.getCoins(heroName) >= item.getPrice();
        int qty = item.isConsumable() ? store.getConsumableQty(heroName, item.getId()) : 0;
        String accent = categoryAccent(item);
        boolean selected = item == selectedItem;

        VBox card = new VBox(0);
        card.setPrefWidth(150);
        card.setPrefHeight(190);
        card.setCursor(Cursor.HAND);
        card.setStyle(cardStyle(accent, owned, selected));

        // ── Top: sprite icon ──────────────────────────────────────────────────
        StackPane iconArea = new StackPane();
        iconArea.setPrefHeight(80);
        iconArea.setStyle(
                "-fx-background-color:" + accent + "11;" +
                        "-fx-border-color:" + accent + "33; -fx-border-width:0 0 1 0;");

        Canvas spriteCanvas = new Canvas(48, 48);
        drawItemSprite(spriteCanvas.getGraphicsContext2D(), item, 48);
        iconArea.getChildren().add(spriteCanvas);

        // Owned badge (top-left)
        if (owned) {
            Label badge = label("✓ OWNED", 7, "#2ecc71", true);
            badge.setStyle("-fx-background-color:#0a2a0a; -fx-padding:2 5 2 5; " +
                    "-fx-border-color:#2ecc71; -fx-border-width:1;");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            badge.setTranslateX(4);
            badge.setTranslateY(4);
            iconArea.getChildren().add(badge);
        }
        // Qty badge for consumables
        if (qty > 0) {
            Label qtyBadge = label("x" + qty, 8, "#f1c40f", true);
            qtyBadge.setStyle("-fx-background-color:#1a1000; -fx-padding:2 5 2 5; " +
                    "-fx-border-color:#8a6000; -fx-border-width:1;");
            StackPane.setAlignment(qtyBadge, Pos.TOP_RIGHT);
            qtyBadge.setTranslateX(-4);
            qtyBadge.setTranslateY(4);
            iconArea.getChildren().add(qtyBadge);
        }

        card.getChildren().add(iconArea);

        // ── Body: name + description ──────────────────────────────────────────
        VBox body = new VBox(3);
        body.setPadding(new Insets(7, 8, 4, 8));
        VBox.setVgrow(body, Priority.ALWAYS);

        Label nameLbl = label(item.getName(), 10, owned ? "#6a6a6a" : "#e8d5b0", true);
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(140);
        Label descLbl = label(item.getDescription(), 8, "#8a7060", false);
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(140);

        body.getChildren().addAll(nameLbl, descLbl);
        card.getChildren().add(body);

        // ── Footer: price + buy ───────────────────────────────────────────────
        HBox footer = new HBox(4);
        footer.setPadding(new Insets(4, 8, 7, 8));
        footer.setAlignment(Pos.CENTER_LEFT);

        // Coin icon
        Canvas coinIcon = new Canvas(16, 16);
        SpriteRenderer.drawTile(coinIcon.getGraphicsContext2D(), "items_x2", 1, 3, 0, 0, 16, 16);
        Label priceLbl = label(String.valueOf(item.getPrice()), 10,
                owned ? "#3d2a2a" : canBuy ? "#f1c40f" : "#7a5050", true);

        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);

        Button buyBtn;
        if (owned) {
            buyBtn = footerBtn("OWNED", "#2ecc71", true);
            buyBtn.setDisable(true);
        } else if (canBuy) {
            buyBtn = footerBtn(item.isConsumable() ? "+1" : "BUY", accent, false);
            buyBtn.setOnAction(e -> doPurchase(item));
        } else {
            buyBtn = footerBtn(item.isConsumable() ? "+1" : "BUY", "#3d2a2a", true);
            buyBtn.setDisable(true);
        }

        footer.getChildren().addAll(coinIcon, priceLbl, fSpacer, buyBtn);
        card.getChildren().add(footer);

        // ── Hover / click ─────────────────────────────────────────────────────
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(accent, 0.6));
        glow.setRadius(12);
        glow.setSpread(0.2);

        card.setOnMouseEntered(e -> {
            if (!selected) {
                card.setStyle(cardHoverStyle(accent, owned));
                card.setEffect(glow);
            }
            ScaleTransition st = new ScaleTransition(Duration.millis(90), card);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(cardStyle(accent, owned, item == selectedItem));
            card.setEffect(item == selectedItem ? glow : null);
            ScaleTransition st = new ScaleTransition(Duration.millis(90), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        card.setOnMouseClicked(e -> selectItem(item));

        if (selected)
            card.setEffect(glow);

        return card;
    }

    // Draw item image into a canvas at given size — uses exact filename
    private void drawItemSprite(GraphicsContext gc, MarketItem item, double size) {
        String filename = ITEM_IMAGES.get(item.getId());
        if (filename != null) {
            com.kurawler.util.ImageCache.draw(gc, filename, 0, 0, size, size);
        } else {
            // Coloured circle fallback
            gc.setFill(Color.web(categoryAccent(item)));
            double r = size * 0.36;
            gc.fillOval(size / 2 - r, size / 2 - r, r * 2, r * 2);
        }
    }

    // ── Detail panel ──────────────────────────────────────────────────────────
    private VBox buildDetailPanel() {
        detailPanel = new VBox(0);
        detailPanel.setPrefWidth(240);
        detailPanel.setStyle(
                "-fx-background-color:#0d0808;" +
                        "-fx-border-color:#6b3a2a; -fx-border-width:0 0 0 2;");
        showEmptyDetail();
        return detailPanel;
    }

    private void showEmptyDetail() {
        detailPanel.getChildren().clear();
        Label hdr = sectionHeader("ITEM  DETAILS");
        detailPanel.getChildren().add(hdr);
        VBox hint = new VBox(6);
        hint.setPadding(new Insets(20, 16, 16, 16));
        hint.getChildren().add(label("Click any item card\nto view details\nand purchase.", 10, "#8a7060", false));
        detailPanel.getChildren().add(hint);
    }

    private void selectItem(MarketItem item) {
        selectedItem = item;
        rebuildCards();
        populateDetail(item);
        FadeTransition ft = new FadeTransition(Duration.millis(150), detailPanel);
        ft.setFromValue(0.6);
        ft.setToValue(1.0);
        ft.play();
    }

    private void populateDetail(MarketItem item) {
        detailPanel.getChildren().clear();

        boolean owned = !item.isConsumable() && store.ownsItem(heroName, item.getId());
        boolean canBuy = !owned && store.getCoins(heroName) >= item.getPrice();
        int qty = item.isConsumable() ? store.getConsumableQty(heroName, item.getId()) : 0;
        String accent = categoryAccent(item);

        // Header
        detailPanel.getChildren().add(sectionHeader("ITEM  DETAILS"));

        // Icon area
        StackPane iconWrap = new StackPane();
        iconWrap.setPrefHeight(110);
        iconWrap.setStyle("-fx-background-color:" + accent + "11;");
        Canvas bigSprite = new Canvas(72, 72);
        drawItemSprite(bigSprite.getGraphicsContext2D(), item, 72);
        iconWrap.getChildren().add(bigSprite);
        detailPanel.getChildren().add(iconWrap);

        // Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 16, 14, 16));

        // Name
        Label nameLbl = label(item.getName(), 14, accent, true);
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(210);
        nameLbl.setStyle("-fx-effect:dropshadow(one-pass-box," + accent + ",3,0,1,1);");

        // Category badge
        Label catBadge = label(item.getCategory().name().replace("_", " "), 8, accent, true);
        catBadge.setStyle("-fx-background-color:" + accent + "22; -fx-padding:2 8; " +
                "-fx-border-color:" + accent + "55; -fx-border-width:1;");

        // Description
        Label descLbl = label(item.getDescription(), 10, "#8a7060", false);
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(210);

        // Stat line
        String statStr = switch (item.getEffectType()) {
            case RESTORE_HP -> "❤  Restores +" + item.getEffectValue() + " HP";
            case RESTORE_MANA -> "🔵  Restores +" + item.getEffectValue() + " Mana";
            case RESTORE_ENERGY -> "⚡  Restores +" + item.getEffectValue() + " Energy";
            case BOOST_MAX_HP -> "❤  Max HP +" + item.getEffectValue();
            case BOOST_STR -> "💪  STR +" + item.getEffectValue();
            case BOOST_DEF -> "🔰  DEF +" + item.getEffectValue();
            case BOOST_MAX_ENERGY -> "⚡  Max Energy +" + item.getEffectValue();
            case GRANT_WEAPON -> "⚔  ATK +" + item.getEffectValue();
            case SKIN_CHANGE -> "👑  Cosmetic change";
        };
        Label statLbl = label(statStr, 11, "#e8d5b0", true);

        // Type
        String typeStr = item.isConsumable() ? "Consumable  (stackable)" : "Permanent unlock";
        Label typeLbl = label(typeStr, 9, "#8a7060", false);

        // Ownership
        String ownedStr = owned ? "✓  Already owned" : item.isConsumable() && qty > 0 ? "Owned:  x" + qty : "Not owned";
        Color ownedClr = owned ? Color.web("#2ecc71") : qty > 0 ? Color.web("#f1c40f") : Color.web("#8a7060");
        Label ownedLbl = new Label(ownedStr);
        ownedLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        ownedLbl.setTextFill(ownedClr);

        // Price row with coin sprite
        HBox priceRow = new HBox(6);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Canvas coinIco = new Canvas(20, 20);
        SpriteRenderer.drawTile(coinIco.getGraphicsContext2D(), "items_x2", 1, 3, 0, 0, 20, 20);
        Label priceLbl = label(item.getPrice() + " coins", 12, canBuy ? "#f1c40f" : "#7a5050", true);
        priceRow.getChildren().addAll(coinIco, priceLbl);

        body.getChildren().addAll(nameLbl, catBadge, descLbl, statLbl, typeLbl, ownedLbl);
        detailPanel.getChildren().add(body);

        // Divider
        detailPanel.getChildren().add(hline());

        // Price + buy section
        VBox buySection = new VBox(10);
        buySection.setPadding(new Insets(14, 16, 14, 16));
        buySection.getChildren().add(priceRow);

        if (owned) {
            Label ownedBtn = new Label("✓  PURCHASED");
            ownedBtn.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
            ownedBtn.setTextFill(Color.web("#2ecc71"));
            ownedBtn.setMaxWidth(Double.MAX_VALUE);
            ownedBtn.setAlignment(Pos.CENTER);
            ownedBtn.setStyle("-fx-background-color:#0a2a0a; -fx-padding:10 0; " +
                    "-fx-border-color:#2ecc71; -fx-border-width:1;");
            buySection.getChildren().add(ownedBtn);
        } else if (canBuy) {
            Button buyBtn = new Button(item.isConsumable() ? "BUY  +1" : "BUY  NOW");
            buyBtn.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
            buyBtn.setMaxWidth(Double.MAX_VALUE);
            String bs = "-fx-background-color:" + accent + "22; -fx-border-color:" + accent + "; " +
                    "-fx-border-width:2; -fx-text-fill:" + accent + "; -fx-padding:10 0; " +
                    "-fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;";
            String bh = "-fx-background-color:" + accent + "55; -fx-border-color:" + accent + "; " +
                    "-fx-border-width:2; -fx-text-fill:#ffffff; -fx-padding:10 0; " +
                    "-fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;";
            buyBtn.setStyle(bs);
            buyBtn.setOnMouseEntered(e -> buyBtn.setStyle(bh));
            buyBtn.setOnMouseExited(e -> buyBtn.setStyle(bs));
            buyBtn.setOnAction(e -> doPurchase(item));
            buySection.getChildren().add(buyBtn);
        } else {
            Label cantLbl = label("Not enough coins", 10, "#7a5050", false);
            cantLbl.setMaxWidth(Double.MAX_VALUE);
            cantLbl.setAlignment(Pos.CENTER);
            cantLbl.setStyle("-fx-background-color:#1a0808; -fx-padding:10 0; " +
                    "-fx-border-color:#3d2a2a; -fx-border-width:1;");
            buySection.getChildren().add(cantLbl);
        }

        detailPanel.getChildren().add(buySection);
    }

    // ── Purchase ──────────────────────────────────────────────────────────────
    private void doPurchase(MarketItem item) {
        if (!item.isConsumable() && store.ownsItem(heroName, item.getId())) {
            notify("Already owned!", "#8a7060");
            return;
        }
        boolean ok = store.buyItem(heroName, item.getId(), item.getPrice(), item.isConsumable());
        if (!ok) {
            notify("Not enough coins!", "#e74c3c");
            return;
        }

        // Auto-equip permanent gear
        if (item.getCategory() == MarketItem.Category.WEAPON)
            store.equipWeapon(heroName, item.getId());
        else if (item.getCategory() == MarketItem.Category.ARMOR)
            store.equipArmor(heroName, item.getId());
        else if (item.getCategory() == MarketItem.Category.SKIN)
            store.equipSkin(heroName, item.getId());

        // Update coin display
        int newCoins = store.getCoins(heroName);
        lblCoins.setText(String.valueOf(newCoins));

        // Coin flash animation
        animateCoinSpend();

        // Notify
        String msg = item.isConsumable()
                ? "Bought " + item.getName() + " x" + store.getConsumableQty(heroName, item.getId())
                : "Purchased: " + item.getName();
        notify(msg, "#2ecc71");

        // Refresh
        rebuildCards();
        populateDetail(item);
    }

    private void animateCoinSpend() {
        // Quick flash: lblCoins turns red then back to gold
        lblCoins.setTextFill(Color.web("#e74c3c"));
        ScaleTransition sc = new ScaleTransition(Duration.millis(100), lblCoins);
        sc.setFromX(1.2);
        sc.setToX(1.0);
        sc.setFromY(1.2);
        sc.setToY(1.0);
        sc.play();
        PauseTransition pause = new PauseTransition(Duration.millis(350));
        pause.setOnFinished(e -> lblCoins.setTextFill(Color.web("#f1c40f")));
        pause.play();
    }

    private void notify(String msg, String color) {
        lblNotify.setText(msg);
        lblNotify.setTextFill(Color.web(color));
        FadeTransition ft = new FadeTransition(Duration.seconds(3), lblNotify);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            lblNotify.setText("");
            lblNotify.setOpacity(1.0);
        });
        ft.play();
    }

    // ── Style helpers ─────────────────────────────────────────────────────────
    private String cardStyle(String accent, boolean owned, boolean selected) {
        String bg = owned ? "#111111" : "#1a0e0e";
        String border = selected ? accent : owned ? "#2a2a2a" : "#3d2a2a";
        String bw = selected ? "2" : "1";
        return "-fx-background-color:" + bg + "; -fx-border-color:" + border + "; -fx-border-width:" + bw + ";";
    }

    private String cardHoverStyle(String accent, boolean owned) {
        return "-fx-background-color:" + (owned ? "#161616" : "#1e1010") + "; " +
                "-fx-border-color:" + accent + "; -fx-border-width:1.5;";
    }

    private String categoryAccent(MarketItem item) {
        return switch (item.getCategory()) {
            case CONSUMABLE -> "#2ecc71";
            case WEAPON -> "#e74c3c";
            case ARMOR -> "#3498db";
            case POWER_UP -> "#9b59b6";
            case SKIN -> "#f39c12";
        };
    }

    private Button footerBtn(String text, String accent, boolean muted) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Courier New", FontWeight.BOLD, 8));
        btn.setStyle(
                "-fx-background-color:" + (muted ? "#1a1a1a" : accent + "22") + "; " +
                        "-fx-border-color:" + (muted ? "#3d2a2a" : accent) + "; -fx-border-width:1; " +
                        "-fx-text-fill:" + (muted ? "#3d2a2a" : accent) + "; " +
                        "-fx-padding:3 8; -fx-background-radius:0; -fx-border-radius:0;" +
                        (muted ? "" : " -fx-cursor:hand;"));
        return btn;
    }

    // ── UI building helpers ───────────────────────────────────────────────────
    private Label label(String t, double sz, String color, boolean bold) {
        Label l = new Label(t);
        l.setFont(Font.font("Courier New", bold ? FontWeight.BOLD : FontWeight.NORMAL, sz));
        l.setTextFill(Color.web(color));
        return l;
    }

    private Label sectionHeader(String t) {
        Label l = new Label("  " + t);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(10, 0, 8, 0));
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        l.setTextFill(Color.web("#c9a227"));
        l.setStyle("-fx-background-color:#1a0e0e; -fx-border-color:#6b3a2a; -fx-border-width:0 0 1 0;");
        return l;
    }

    private Region hline() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:#3d2a2a;");
        return r;
    }
}
