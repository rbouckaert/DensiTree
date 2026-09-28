package viz.fxpanel;
import java.awt.GraphicsEnvironment;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

// =========================================================================
// Custom Font Chooser Dialog using java.awt.Font
// =========================================================================
public class FontChooserDialog extends Dialog<java.awt.Font> {

    private final ListView<String> familyList;
    private final ListView<String> styleList;
    private final ListView<Integer> sizeList;
    private final Label previewLabel;

    public FontChooserDialog(java.awt.Font initialFont) {
        setTitle("Font Chooser (AWT)");
        setHeaderText("Select Font Family, Style, and Size");

        // Standard Dialog buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 1. AWT Font Families
        String[] fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        familyList = new ListView<>(FXCollections.observableArrayList(fontFamilies));
        familyList.setPrefSize(180, 160);

        // 2. Font Styles
        styleList = new ListView<>(FXCollections.observableArrayList(
                "Regular", "Bold", "Italic", "Bold Italic"
        ));
        styleList.setPrefSize(110, 160);

        // 3. Font Sizes
        sizeList = new ListView<>(FXCollections.observableArrayList(
                8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72
        ));
        sizeList.setPrefSize(70, 160);

        // 4. Live Preview Label
        previewLabel = new Label("AaBbYyZz 123");
        previewLabel.setAlignment(Pos.CENTER);
        previewLabel.setPrefHeight(60);
        previewLabel.setStyle("-fx-border-color: lightgray; -fx-padding: 8;");

        // Select initial values
        selectInitialFont(initialFont);

        // Listeners to update preview dynamically
        familyList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updatePreview());
        styleList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updatePreview());
        sizeList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updatePreview());

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Font:"), 0, 0);
        grid.add(familyList, 0, 1);

        grid.add(new Label("Style:"), 1, 0);
        grid.add(styleList, 1, 1);

        grid.add(new Label("Size:"), 2, 0);
        grid.add(sizeList, 2, 1);

        grid.add(new Label("Preview:"), 0, 2);
        grid.add(previewLabel, 0, 3, 3, 1);
        previewLabel.setMaxWidth(Double.MAX_VALUE);

        getDialogPane().setContent(grid);

        // Convert Dialog Result to java.awt.Font
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return getSelectedFont();
            }
            return null;
        });
    }

    private void selectInitialFont(java.awt.Font font) {
        if (font == null) {
            font = new java.awt.Font(java.awt.Font.DIALOG, java.awt.Font.PLAIN, 12);
        }

        // Family
        familyList.getSelectionModel().select(font.getFamily());
        familyList.scrollTo(font.getFamily());

        // Style
        if (font.isBold() && font.isItalic()) {
            styleList.getSelectionModel().select("Bold Italic");
        } else if (font.isBold()) {
            styleList.getSelectionModel().select("Bold");
        } else if (font.isItalic()) {
            styleList.getSelectionModel().select("Italic");
        } else {
            styleList.getSelectionModel().select("Regular");
        }

        // Size
        sizeList.getSelectionModel().select(Integer.valueOf(font.getSize()));
        sizeList.scrollTo(Integer.valueOf(font.getSize()));

        updatePreview();
    }

    public java.awt.Font getSelectedFont() {
        String family = familyList.getSelectionModel().getSelectedItem();
        String styleStr = styleList.getSelectionModel().getSelectedItem();
        Integer size = sizeList.getSelectionModel().getSelectedItem();

        if (family == null) family = java.awt.Font.DIALOG;
        if (size == null) size = 12;

        int style = java.awt.Font.PLAIN;
        if ("Bold".equals(styleStr)) {
            style = java.awt.Font.BOLD;
        } else if ("Italic".equals(styleStr)) {
            style = java.awt.Font.ITALIC;
        } else if ("Bold Italic".equals(styleStr)) {
            style = java.awt.Font.BOLD | java.awt.Font.ITALIC;
        }

        return new java.awt.Font(family, style, size);
    }

    private void updatePreview() {
        java.awt.Font awtFont = getSelectedFont();
        // Convert to JavaFX Font for preview rendering
        FontWeight weight = awtFont.isBold() ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = awtFont.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;
        previewLabel.setFont(javafx.scene.text.Font.font(awtFont.getFamily(), weight, posture, awtFont.getSize()));
    }
}