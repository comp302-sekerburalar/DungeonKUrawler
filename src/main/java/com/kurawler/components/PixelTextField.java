package com.kurawler.components;

import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class PixelTextField extends StackPane {

    private final TextField plain;
    private final PasswordField secret;
    private final boolean isPassword;

    public PixelTextField(String placeholder) {
        this(placeholder, false);
    }

    public PixelTextField(String placeholder, boolean isPassword) {
        this.isPassword = isPassword;
        plain = new TextField();
        plain.setPromptText(placeholder);
        secret = new PasswordField();
        secret.setPromptText(placeholder);
        plain.getStyleClass().add("pixel-input");
        secret.getStyleClass().add("pixel-input");
        getChildren().add(isPassword ? secret : plain);
        setMaxWidth(Double.MAX_VALUE);
    }

    public String getText() {
        return isPassword ? secret.getText() : plain.getText();
    }

    public String getRawText() {
        return getText();
    }

    public void clear() {
        plain.clear();
        secret.clear();
    }

    public void setText(String v) {
        if (isPassword)
            secret.setText(v);
        else
            plain.setText(v);
    }

    @Override
    public void requestFocus() {
        if (isPassword)
            secret.requestFocus();
        else
            plain.requestFocus();
    }
}
