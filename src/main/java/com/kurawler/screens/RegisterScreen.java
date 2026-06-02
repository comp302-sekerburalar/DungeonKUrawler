package com.kurawler.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import com.kurawler.components.*;

public class RegisterScreen extends BaseScreen {

    private PixelTextField tfUser, tfPass, tfConf;
    private Label lblError;

    public RegisterScreen(ScreenManager manager) {
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
        tLogin.getStyleClass().add("tab-btn");
        tLogin.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tLogin, Priority.ALWAYS);
        tLogin.setOnAction(e -> manager.showLogin());
        Button tReg = new Button("REGISTER");
        tReg.getStyleClass().addAll("tab-btn", "tab-active");
        tReg.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tReg, Priority.ALWAYS);
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

        VBox ug = fg("CHOOSE HERO NAME");
        tfUser = new PixelTextField("ENTER NAME...");
        ug.getChildren().add(tfUser);
        VBox pg = fg("SET PASSWORD");
        tfPass = new PixelTextField("••••••••", true);
        pg.getChildren().add(tfPass);
        VBox cg = fg("CONFIRM PASSWORD");
        tfConf = new PixelTextField("••••••••", true);
        cg.getChildren().add(tfConf);

        DungeonButton btn = new DungeonButton("CREATE HERO", true);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> attemptRegister());
        form.getChildren().addAll(lblError, ug, pg, cg, btn);

        Pane stoneBottom = PixelBorder.stoneBottom(440);
        Button back = new Button("◄  BACK TO MAIN MENU");
        back.getStyleClass().add("link-btn");
        VBox bw = new VBox(back);
        bw.setAlignment(Pos.CENTER);
        bw.setPadding(new Insets(12, 0, 0, 0));
        back.setOnAction(e -> manager.showMainMenu());

        center.getChildren().addAll(tw, tabs, stoneTop, form, stoneBottom, bw);
        root.getChildren().add(center);
        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> attemptRegister();
                case ESCAPE -> manager.showMainMenu();
                default -> {
                }
            }
        });
        root.setFocusTraversable(true);
        return root;
    }

    private void attemptRegister() {
        String u = tfUser.getText().trim(), p = tfPass.getRawText(), c = tfConf.getRawText();
        if (u.isEmpty() || p.isEmpty()) {
            showErr("ALL FIELDS REQUIRED.");
            return;
        }
        if (u.length() < 3) {
            showErr("NAME MUST BE 3+ CHARS.");
            return;
        }
        if (!p.equals(c)) {
            showErr("PASSWORDS DO NOT MATCH.");
            return;
        }
        if (p.length() < 4) {
            showErr("PASSWORD MUST BE 4+ CHARS.");
            return;
        }
        if (manager.getUserStore().exists(u)) {
            showErr("HERO NAME TAKEN.");
            return;
        }
        manager.getUserStore().register(u, p);

        manager.loginUser(u.toUpperCase());

        manager.showWelcome(u.toUpperCase());
    }

    private void showErr(String m) {
        lblError.setText(m);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideErr() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    public void reset() {
        if (tfUser != null)
            tfUser.clear();
        if (tfPass != null)
            tfPass.clear();
        if (tfConf != null)
            tfConf.clear();
        hideErr();
    }

    private VBox fg(String lbl) {
        VBox g = new VBox(6);
        Label l = new Label(lbl);
        l.getStyleClass().add("field-label");
        g.getChildren().add(l);
        return g;
    }
}
