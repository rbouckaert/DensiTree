package viz.fxpanel;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.ArrayList;
import java.util.List;

import viz.DensiTree;
import viz.DensiTree.LineColorMode;
import viz.DensiTree.MetaDataType;

public class ColorPanel extends GridPane implements ChangeListener {

    public static final String HELP_LINE_COLOR = "Determines line color for the complete tree set. " +
            "So, this does not affect the consensus trees or root canal tree.\n" +
            "DEFAULT: color 1 for most frequently occurring topology, color 2 for the second most popular, " +
            "color 3 for the third, and color 4 for the remaining trees. Colors can be changed using the " +
            "line colors button.\n" +
            "COLOR_BY_CLADE: draw clades in one color.\n" +
            "COLOR_BY_META_DATA_PATTERN: draw trees matching the regular expression specified in the pattern entry below.\n" +
            "meta data attribute: only available if any meta data attribute is specified. Use value of the attribute to color branches.";
    public static final String HELP_SHOW_LEGEND = "Show legend mapping colors to attribute values in the DensiTree. " +
            "This only works when a discrete attribute is selected for line coloring.";
    public static final String HELP_MULTI_COLOR_CONSENSUS_TREES = "Use different colours for consensus trees instead of the " +
            "standard color.";
    public static final String HELP_CATEGORICAL = "Interpret value of attribute as categorical data.";
    public static final String HELP_PATTERN = "Regular expression used for coloring trees when COLOR_BY_META_DATA_PATTERN " +
            "is chosen. The string of the pattern between brackets is selected as value.";
    public static final String HELP_LINE_COLORS = "Specify custom colors.";

    private final DensiTree m_dt;
    private final ComboBox<String> comboBox = new ComboBox<>();
    private TextField txtPattern = null;
    private final Button btnLineColors;
    private CheckBox chckbxShowLegend = null;
    private CheckBox chckbxCategorical = null;
    private final CheckBox chckbxMultiColorConsensus;

    public ColorPanel(DensiTree dt) {
        this.m_dt = dt;
        this.m_dt.addChangeListener(this);

        // Configure GridPane
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        // 1. Combo Box (Row 0)
        comboBox.setPrefWidth(130);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setTooltip(createTooltip(HELP_LINE_COLOR));
        stateChanged(null);

        comboBox.setOnAction(e -> {
            Platform.runLater(() -> {
                String selected = comboBox.getValue();
                if (selected == null) return;

                LineColorMode oldMode = m_dt.m_settings.m_lineColorMode;
                String oldTag = m_dt.m_settings.m_lineColorTag;

                if (selected.equals(LineColorMode.DEFAULT.toString())) {
                    m_dt.m_settings.m_lineColorMode = LineColorMode.DEFAULT;
                } else if (selected.equals(LineColorMode.BY_METADATA_PATTERN.toString())) {
                    m_dt.m_settings.m_lineColorMode = LineColorMode.BY_METADATA_PATTERN;
                } else if (selected.equals(LineColorMode.COLOR_BY_CLADE.toString())) {
                    m_dt.m_settings.m_lineColorMode = LineColorMode.COLOR_BY_CLADE;
                } else {
                    m_dt.m_settings.m_lineColorTag = selected;
                    m_dt.m_settings.m_lineColorMode = LineColorMode.COLOR_BY_METADATA_TAG;
                }

                txtPattern.setDisable(m_dt.m_settings.m_lineColorMode != LineColorMode.BY_METADATA_PATTERN);
                chckbxShowLegend.setDisable(m_dt.m_settings.m_lineColorMode != LineColorMode.BY_METADATA_PATTERN
                        && m_dt.m_settings.m_lineColorMode != LineColorMode.COLOR_BY_METADATA_TAG);
                chckbxCategorical.setDisable(m_dt.m_settings.m_lineColorMode != LineColorMode.COLOR_BY_METADATA_TAG);

                if (m_dt.m_settings.m_lineColorMode != oldMode
                        || (m_dt.m_settings.m_lineColorTag != null && !m_dt.m_settings.m_lineColorTag.equals(oldTag))) {
                    m_dt.calcColors(false);
                    m_dt.makeDirty();
                }
            });
        });
        GridPane.setColumnSpan(comboBox, 2);
        GridPane.setHgrow(comboBox, Priority.ALWAYS);
        add(comboBox, 0, 0);

        // 2. Show Legend (Row 1)
        chckbxShowLegend = new CheckBox("Show legend");
        chckbxShowLegend.setSelected(m_dt.m_settings.m_showLegend);
        chckbxShowLegend.setTooltip(createTooltip(HELP_SHOW_LEGEND));
        chckbxShowLegend.setDisable(true);
        chckbxShowLegend.setOnAction(e -> {
            m_dt.m_settings.m_showLegend = !m_dt.m_settings.m_showLegend;
            m_dt.makeDirty();
        });
        GridPane.setColumnSpan(chckbxShowLegend, 2);
        GridPane.setHalignment(chckbxShowLegend, HPos.LEFT);
        add(chckbxShowLegend, 0, 1);

        // 3. Multi-color Consensus Trees (Row 2)
        chckbxMultiColorConsensus = new CheckBox("Multi color\ncons-trees");
        chckbxMultiColorConsensus.setSelected(m_dt.m_settings.m_bViewMultiColor);
        chckbxMultiColorConsensus.setTooltip(createTooltip(HELP_MULTI_COLOR_CONSENSUS_TREES));
        chckbxMultiColorConsensus.setOnAction(e -> {
            m_dt.m_settings.m_bViewMultiColor = chckbxMultiColorConsensus.isSelected();
            m_dt.makeDirty();
        });
        GridPane.setColumnSpan(chckbxMultiColorConsensus, 3);
        GridPane.setHalignment(chckbxMultiColorConsensus, HPos.LEFT);
        add(chckbxMultiColorConsensus, 0, 2);

        // 4. Categorical Checkbox (Row 3)
        chckbxCategorical = new CheckBox("categorical");
        chckbxCategorical.setSelected(m_dt.m_settings.m_bColorByCategory);
        chckbxCategorical.setTooltip(createTooltip(HELP_CATEGORICAL));
        chckbxCategorical.setDisable(true);
        chckbxCategorical.setOnAction(e -> {
            m_dt.m_settings.m_bColorByCategory = chckbxCategorical.isSelected();
            m_dt.calcColors(true);
            m_dt.makeDirty();
        });
        GridPane.setColumnSpan(chckbxCategorical, 2);
        GridPane.setHalignment(chckbxCategorical, HPos.LEFT);
        add(chckbxCategorical, 0, 3);

        // 5. Pattern Label (Row 4)
        Label lblPattern = new Label("pattern:");
        GridPane.setHalignment(lblPattern, HPos.LEFT);
        add(lblPattern, 0, 4);

        // 6. Pattern Text Field (Row 5)
        txtPattern = new TextField(m_dt.m_settings.m_sLineColorPattern);
        txtPattern.setPrefColumnCount(10);
        txtPattern.setTooltip(createTooltip(HELP_PATTERN));
        txtPattern.setDisable(true);
        txtPattern.setOnAction(e -> {
            String oldPattern = m_dt.m_settings.m_sLineColorPattern;
            m_dt.m_settings.m_sLineColorPattern = txtPattern.getText();
            if (!m_dt.m_settings.m_sLineColorPattern.equals(oldPattern)) {
                m_dt.calcColors(false);
                m_dt.makeDirty();
            }
        });
        GridPane.setColumnSpan(txtPattern, 2);
        GridPane.setHgrow(txtPattern, Priority.ALWAYS);
        add(txtPattern, 0, 5);

        // 7. Line Colors Button (Row 6)
        btnLineColors = new Button("Line colors");
        btnLineColors.setMaxWidth(Double.MAX_VALUE);
        btnLineColors.setTooltip(createTooltip(HELP_LINE_COLORS));
        btnLineColors.setOnAction(e -> new ColorDialog(m_dt).show());
        GridPane.setColumnSpan(btnLineColors, 2);
        GridPane.setHgrow(btnLineColors, Priority.ALWAYS);
        add(btnLineColors, 0, 6);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        List<String> selection = new ArrayList<>();
        selection.add(LineColorMode.DEFAULT.toString());
        if (m_dt.m_treeData != null && m_dt.m_treeData.m_bMetaDataReady) {
            selection.add(LineColorMode.COLOR_BY_CLADE.toString());
            selection.add(LineColorMode.BY_METADATA_PATTERN.toString());
            if (m_dt.m_settings.m_metaDataTags != null) {
                for (int i = 0; i < m_dt.m_settings.m_metaDataTags.size(); i++) {
                    if (!m_dt.m_settings.m_metaDataTypes.get(i).equals(MetaDataType.SET)) {
                        selection.add(m_dt.m_settings.m_metaDataTags.get(i));
                    }
                }
            }
        }
        comboBox.getItems().setAll(selection);

        if (m_dt.m_settings.m_lineColorMode == LineColorMode.DEFAULT) {
            comboBox.setValue(LineColorMode.DEFAULT.toString());
        } else if (m_dt.m_settings.m_lineColorMode == LineColorMode.BY_METADATA_PATTERN) {
            comboBox.setValue(LineColorMode.BY_METADATA_PATTERN.toString());
        } else if (m_dt.m_settings.m_lineColorMode == LineColorMode.COLOR_BY_CLADE) {
            comboBox.setValue(LineColorMode.COLOR_BY_CLADE.toString());
        } else {
            comboBox.setValue(m_dt.m_settings.m_lineColorTag);
        }
    }

    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }

    // --- Color Dialog Implementation in JavaFX ---
    public static class ColorDialog extends Stage {
        private final DensiTree m_dt;

        public ColorDialog(DensiTree dt) {
            this.m_dt = dt;
            initModality(Modality.APPLICATION_MODAL);
            setTitle("Line Colors");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(8);
            grid.setPadding(new Insets(10));

            int[] row = {0};
            addColorAction(grid, row, "Color 1", "Color of most popular topology", 0);
            addColorAction(grid, row, "Color 2", "Color of second most popular topology", 1);
            addColorAction(grid, row, "Color 3", "Color of third most popular topology", 2);
            addColorAction(grid, row, "Default", "Default color", 3);
            addColorAction(grid, row, "Consensus", "Consensus tree color", DensiTree.CONSCOLOR);
            addColorAction(grid, row, "Background", "Background color", DensiTree.BGCOLOR);
            addColorAction(grid, row, "Root canal", "Root canal color", DensiTree.ROOTCANALCOLOR);

            if (m_dt.m_settings.m_color != null) {
                for (int k = 9; k < m_dt.m_settings.m_color.length; k++) {
                    addColorAction(grid, row, "Color " + k, "Custom line color " + k, k);
                }
            }

            Button btnClose = new Button("Close");
            btnClose.setMaxWidth(Double.MAX_VALUE);
            btnClose.setOnAction(e -> close());

            VBox root = new VBox(10);
            root.setPadding(new Insets(10));
            root.setAlignment(Pos.CENTER);

            ScrollPane scrollPane = new ScrollPane(grid);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(400);

            root.getChildren().addAll(scrollPane, btnClose);

            setScene(new Scene(root, 320, 480));
        }

        private void addColorAction(GridPane grid, int[] row, String labelText, String tipText, int colorID) {
            Label label = new Label(labelText);
            label.setTooltip(new Tooltip(tipText));

            java.awt.Color awtColor = m_dt.m_settings.m_color[colorID];
            javafx.scene.paint.Color initialFxColor = awtToFxColor(awtColor);

            ColorPicker colorPicker = new ColorPicker(initialFxColor);
            colorPicker.setTooltip(new Tooltip(tipText));
            colorPicker.setOnAction(e -> {
                javafx.scene.paint.Color newFxColor = colorPicker.getValue();
                if (newFxColor != null) {
                    m_dt.m_settings.m_color[colorID] = fxToAwtColor(newFxColor);
                    m_dt.calcColors(true);
                    m_dt.makeDirty();
                    m_dt.repaint();
                }
            });

            grid.add(label, 0, row[0]);
            grid.add(colorPicker, 1, row[0]);
            row[0]++;
        }

        private static javafx.scene.paint.Color awtToFxColor(java.awt.Color c) {
            if (c == null) return javafx.scene.paint.Color.BLACK;
            return javafx.scene.paint.Color.rgb(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0);
        }

        private static java.awt.Color fxToAwtColor(javafx.scene.paint.Color c) {
            if (c == null) return java.awt.Color.BLACK;
            return new java.awt.Color((float) c.getRed(), (float) c.getGreen(), (float) c.getBlue(), (float) c.getOpacity());
        }
    }
}