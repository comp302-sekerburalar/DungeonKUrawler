package com.kurawler.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import com.kurawler.components.*;

public class LoginScreen extends BaseScreen {

    private PixelTextField tfUser, tfPass;
    private Label lblError;

    public LoginScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    protected Pane buildUI() {
        StackPane root = new StackPane();
        root.getStyleClass().add("dungeon-bg");
        root.setPrefSize(W(), H());

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(440);

        Text mini = new Text("KURAWLER");
        mini.getStyleClass().add("title-line2-small");
        VBox tw = new VBox(mini);
        tw.setAlignment(Pos.CENTER);
        tw.setPadding(new Insets(0, 0, 16, 0));

        HBox tabs = new HBox(0);
        tabs.setMaxWidth(440);
        Button tLogin = new Button("LOGIN");
        tLogin.getStyleClass().addAll("tab-btn", "tab-active");
        tLogin.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tLogin, Priority.ALWAYS);
        Button tReg = new Button("REGISTER");
        tReg.getStyleClass().add("tab-btn");
        tReg.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tReg, Priority.ALWAYS);
        tReg.setOnAction(e -> manager.showRegister());
        tabs.getChildren().addAll(tLogin, tReg);

        Pane stoneTop = PixelBorder.stoneTop(440);

        VBox form = new VBox(14);
        form.setPadding(new Insets(24, 32, 24, 32));
        form.getStyleClass().add("panel-surface");
        form.setMaxWidth(440);

        lblError = new Label("");
        lblError.getStyleClass().add("error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblError.setMaxWidth(Double.MAX_VALUE);
        lblError.setWrapText(true);

        VBox ug = fieldGroup("HERO NAME");
        tfUser = new PixelTextField("ENTER NAME...");
        ug.getChildren().add(tfUser);
        VBox pg = fieldGroup("PASSWORD");
        tfPass = new PixelTextField("••••••••", true);
        pg.getChildren().add(tfPass);

        CheckBox rem = new CheckBox("REMEMBER ME");
        rem.getStyleClass().add("pixel-checkbox");
        HBox remRow = new HBox(10, rem);
        remRow.setAlignment(Pos.CENTER_LEFT);

        DungeonButton btnEnter = new DungeonButton("ENTER THE DUNGEON", true);
        btnEnter.setMaxWidth(Double.MAX_VALUE);
        btnEnter.setOnAction(e -> attemptLogin());

        form.getChildren().addAll(lblError, ug, pg, remRow, btnEnter);
        Pane stoneBottom = PixelBorder.stoneBottom(440);

        Button btnBack = new Button("◄  BACK TO MAIN MENU");
        btnBack.getStyleClass().add("link-btn");
        VBox bw = new VBox(btnBack);
        bw.setAlignment(Pos.CENTER);
        bw.setPadding(new Insets(12, 0, 0, 0));
        btnBack.setOnAction(e -> manager.showMainMenu());

        center.getChildren().addAll(tw, tabs, stoneTop, form, stoneBottom, bw);
        root.getChildren().add(center);

        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> attemptLogin();
                case ESCAPE -> manager.showMainMenu();
                default -> {
                }
            }
        });
        root.setFocusTraversable(true);
        return root;
    }

    private void attemptLogin() {
        String user = tfUser.getText().trim(), pass = tfPass.getRawText();
        if (user.isEmpty() || pass.isEmpty()) {
            showError("ENTER HERO NAME AND PASSWORD.");
            return;
        }
        if (manager.getUserStore().authenticate(user, pass)) {
            hideError();

            manager.loginUser(user.toUpperCase());

            manager.showWelcome(user.toUpperCase());
        } else {
            showError("INVALID CREDENTIALS. TRY AGAIN.");
        }
    }

    private void showError(String m) {
        lblError.setText(m);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    public void reset() {
        if (tfUser != null)
            tfUser.clear();
        if (tfPass != null)
            tfPass.clear();
        hideError();
    }

    private VBox fieldGroup(String lbl) {
        VBox g = new VBox(6);
        Label l = new Label(lbl);
        l.getStyleClass().add("field-label");
        g.getChildren().add(l);
        return g;
    }
}
