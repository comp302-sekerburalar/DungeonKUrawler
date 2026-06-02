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
 * Login screen.
 * Validates credentials against UserStore and navigates to WelcomeScreen on success.
 */
public class LoginScreen extends BaseScreen {

    private PixelTextField tfUsername;
    private PixelTextField tfPassword;
    private Label          lblError;

    public LoginScreen(ScreenManager manager) {
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

        // Tab-style header
        HBox tabs = new HBox(0);
        tabs.setAlignment(Pos.CENTER_LEFT);
        tabs.setMaxWidth(440);

        Button tabLogin = new Button("LOGIN");
        tabLogin.getStyleClass().addAll("tab-btn", "tab-active");
        tabLogin.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tabLogin, Priority.ALWAYS);

        Button tabRegister = new Button("REGISTER");
        tabRegister.getStyleClass().add("tab-btn");
        tabRegister.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tabRegister, Priority.ALWAYS);

        tabRegister.setOnAction(e -> manager.showRegister());
        tabLogin.setOnAction(e -> {});   // already here

        tabs.getChildren().addAll(tabLogin, tabRegister);

        // Stone top
        Pane stoneTop = PixelBorder.stoneTop(440);

        // Form panel
        VBox form = new VBox(14);
        form.setPadding(new Insets(24, 32, 24, 32));
        form.getStyleClass().add("panel-surface");
        form.setMaxWidth(440);

        // Error label (hidden by default)
        lblError = new Label("");
        lblError.getStyleClass().add("error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblError.setMaxWidth(Double.MAX_VALUE);
        lblError.setWrapText(true);

        // Username
        VBox userGroup = fieldGroup("HERO NAME");
        tfUsername = new PixelTextField("ENTER NAME...");
        userGroup.getChildren().add(tfUsername);

        // Password
        VBox passGroup = fieldGroup("PASSWORD");
        tfPassword = new PixelTextField("••••••••", true);
        passGroup.getChildren().add(tfPassword);

        // Remember me checkbox
        HBox rememberRow = new HBox(10);
        rememberRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox rememberBox = new CheckBox("REMEMBER ME");
        rememberBox.getStyleClass().add("pixel-checkbox");
        rememberRow.getChildren().add(rememberBox);

        // Submit button
        DungeonButton btnEnter = new DungeonButton("ENTER THE DUNGEON", true);
        btnEnter.setMaxWidth(Double.MAX_VALUE);
        btnEnter.setOnAction(e -> attemptLogin());

        form.getChildren().addAll(lblError, userGroup, passGroup, rememberRow, btnEnter);

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

        // ENTER key submits
        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER  -> attemptLogin();
                case ESCAPE -> manager.showMainMenu();
                default -> {}
            }
        });
        root.setFocusTraversable(true);

        return root;
    }

    // ---------- Logic ----------

    private void attemptLogin() {
        String user = tfUsername.getText().trim();
        String pass = tfPassword.getRawText();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("ENTER YOUR HERO NAME AND PASSWORD.");
            return;
        }

        UserStore store = manager.getUserStore();
        if (store.authenticate(user, pass)) {
            hideError();
            manager.showWelcome(user.toUpperCase());
        } else {
            showError("INVALID CREDENTIALS. TRY AGAIN.");
        }
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

    /** Called by ScreenManager before showing this screen. */
    public void reset() {
        if (tfUsername != null) tfUsername.clear();
        if (tfPassword != null) tfPassword.clear();
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
