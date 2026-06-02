package com.kurawler.components;

import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

/**
 * Unified pixel-art text input that can operate as a plain TextField
 * or as a PasswordField (characters masked).
 *
 * Use getText() / getRawText() to retrieve the value.
 * clear() wipes the field.
 */
public class PixelTextField extends StackPane {

    private final TextField     plain;
    private final PasswordField secret;
    private final boolean       isPassword;

    /** Plain text field */
    public PixelTextField(String placeholder) {
        this(placeholder, false);
    }

    /** Plain or password field depending on the isPassword flag */
    public PixelTextField(String placeholder, boolean isPassword) {
        this.isPassword = isPassword;

        plain  = new TextField();
        secret = new PasswordField();

        plain.setPromptText(placeholder);
        secret.setPromptText(placeholder);

        plain.getStyleClass().add("pixel-input");
        secret.getStyleClass().add("pixel-input");

        if (isPassword) {
            getChildren().add(secret);
        } else {
            getChildren().add(plain);
        }

        setMaxWidth(Double.MAX_VALUE);
    }

    /** The visible text (masked for passwords). Use getRawText() for validation. */
    public String getText() {
        return isPassword ? secret.getText() : plain.getText();
    }

    /** Raw unmasked text – use this for password comparison. */
    public String getRawText() {
        return isPassword ? secret.getText() : plain.getText();
    }

    /** Clear the field. */
    public void clear() {
        plain.clear();
        secret.clear();
    }

    /** Allow setting text programmatically (e.g. remembered username). */
    public void setText(String value) {
        if (isPassword) {
            secret.setText(value);
        } else {
            plain.setText(value);
        }
    }

    /** Request focus on the underlying input. */
    @Override
    public void requestFocus() {
        if (isPassword) {
            secret.requestFocus();
        } else {
            plain.requestFocus();
        }
    }
}
