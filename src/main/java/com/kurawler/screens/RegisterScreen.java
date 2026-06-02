package com.kurawler.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import com.kurawler.components.DungeonButton;
import com.kurawler.components.PixelBorder;
import com.kurawler.components.PixelTextField;
import com.kurawler.model.UserStore;

/**
 * Hero registration screen.
 * Validates input, creates a new account in UserStore, then navigates to WelcomeScreen.
 */
public class RegisterScreen extends BaseScreen {

    private PixelTextField tfUsername;
    private PixelTextField tfPassword;
    private PixelTextField tfConfirm;
    private Label          lblError;

    public RegisterScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    protected Pane buildUI() {
        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(800, 600);

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(440);

        // Mini title
        Text miniTitle = new Text("KURAWLER");
        miniTitle.getStyleClass().add("title-line2-small");
        VBox titleWrap = new VBox(miniTitle);
        titleWrap.setAlignment(Pos.CENTER);
        titleWrap.setPadding(new Insets(0, 0, 16, 0));

        // Tab header
        HBox tabs = new HBox(0);
        tabs.setAlignment(Pos.CENTER_LEFT);
        tabs.setMaxWidth(440);

        Button tabLogin = new Button("LOGIN");
        tabLogin.getStyleClass().add("tab-btn");
        tabLogin.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tabLogin, Priority.ALWAYS);
        tabLogin.setOnAction(e -> manager.showLogin());

        Button tabRegister = new Button("REGISTER");
        tabRegister.getStyleClass().addAll("tab-btn", "tab-active");
        tabRegister.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tabRegister, Priority.ALWAYS);

        tabs.getChildren().addAll(tabLogin, tabRegister);

        // Stone top
        Pane stoneTop = PixelBorder.stoneTop(440);

        // Form panel
        VBox form = new VBox(14);
        form.setPadding(new Insets(24, 32, 24, 32));
        form.getStyleClass().add("panel-surface");
        form.setMaxWidth(440);

        // Error label
        lblError = new Label("");
        lblError.getStyleClass().add("error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblError.setMaxWidth(Double.MAX_VALUE);
        lblError.setWrapText(true);

        // Username
        VBox userGroup = fieldGroup("CHOOSE HERO NAME");
        tfUsername = new PixelTextField("ENTER NAME...");
        userGroup.getChildren().add(tfUsername);

        // Password
        VBox passGroup = fieldGroup("SET PASSWORD");
        tfPassword = new PixelTextField("••••••••", true);
        passGroup.getChildren().add(tfPassword);

        // Confirm
        VBox confirmGroup = fieldGroup("CONFIRM PASSWORD");
        tfConfirm = new PixelTextField("••••••••", true);
        confirmGroup.getChildren().add(tfConfirm);

        // Submit
        DungeonButton btnCreate = new DungeonButton("CREATE HERO", true);
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        btnCreate.setOnAction(e -> attemptRegister());

        form.getChildren().addAll(lblError, userGroup, passGroup, confirmGroup, btnCreate);

        // Stone bottom
        Pane stoneBottom = PixelBorder.stoneBottom(440);

        // Back link
        Button btnBack = new Button("◄  BACK TO MAIN MENU");
        btnBack.getStyleClass().add("link-btn");
        VBox backWrap = new VBox(btnBack);
        backWrap.setAlignment(Pos.CENTER);
        backWrap.setPadding(new Insets(12, 0, 0, 0));
        btnBack.setOnAction(e -> manager.showMainMenu());

        center.getChildren().addAll(titleWrap, tabs, stoneTop, form, stoneBottom, backWrap);
        root.getChildren().add(center);

        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER  -> attemptRegister();
                case ESCAPE -> manager.showMainMenu();
                default -> {}
            }
        });
        root.setFocusTraversable(true);

        return root;
    }

    // ---------- Logic ----------

    private void attemptRegister() {
        String user  = tfUsername.getText().trim();
        String pass  = tfPassword.getRawText();
        String conf  = tfConfirm.getRawText();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("ALL FIELDS ARE REQUIRED.");
            return;
        }
        if (user.length() < 3) {
            showError("HERO NAME MUST BE AT LEAST 3 CHARACTERS.");
            return;
        }
        if (!pass.equals(conf)) {
            showError("PASSWORDS DO NOT MATCH.");
            return;
        }
        if (pass.length() < 4) {
            showError("PASSWORD MUST BE AT LEAST 4 CHARACTERS.");
            return;
        }

        UserStore store = manager.getUserStore();
        if (store.exists(user)) {
            showError("HERO NAME ALREADY TAKEN.");
            return;
        }

        store.register(user, pass);
        hideError();
        manager.showWelcome(user.toUpperCase());
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    public void reset() {
        if (tfUsername != null) tfUsername.clear();
        if (tfPassword != null) tfPassword.clear();
        if (tfConfirm  != null) tfConfirm.clear();
        hideError();
    }

    // ---------- Helpers ----------

    private VBox fieldGroup(String labelText) {
        VBox group = new VBox(6);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("field-label");
        group.getChildren().add(lbl);
        return group;
    }
}
