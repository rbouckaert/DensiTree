package viz.fxpanel;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.ArrayList;
import java.util.List;

import viz.DensiTree;
import viz.DensiTree.LineWidthMode;
import viz.DensiTree.MetaDataType;

public class LineWidthPanel extends GridPane implements ChangeListener {

    public static final String HELP_LINE_WIDTH = "Determine line width of trees of both tree set and consensus trees. " +
            "For consensus trees, the average value for the topology is used.\n" +
            "DEFAULT: all lines are same width.\n" +
            "BY_META_DATA_PATTERN: use value of pattern specified below.\n" +
            "BY_META_DATA_NUMBER: use the N-th attribute value in the meta data.\n" +
            "meta data attribute: only available if any meta data attribute is specified. Use value of the attribute for line width of branches.";
    public static final String HELP_PATTERN = "Regular expression used for width of branches when BY_META_DATA_PATTERN " +
            "is chosen. The string of the pattern between brackets is selected as value.";
    public static final String HELP_TOP = "Specifies N-th meta data attribute for top of branch when BY_META_DATA_NUMBER is selected.";
    public static final String HELP_LINE_WIDTH_BOTTOM = "Line width at bottom of branch.\n" +
            "same as top: use same specification as for top of branch.\n" +
            "Fit to bottom: adjust bottom widths so they fit to top of branch below.\n" +
            "BY_META_DATA_PATTERN: use value of pattern specified below.\n" +
            "BY_META_DATA_NUMBER: use the N-th attribute value in the meta data.\n" +
            "meta data attribute: only available if any meta data attribute is specified. Use value of the attribute for line width of branches.";
    public static final String HELP_BOTTOM = "Specifies N-th meta data attribute for top of branch when BY_META_DATA_NUMBER is selected.";
    public static final String HELP_ZERO_BASED = "If selected, the minimum value is zero, otherwise the minimum value of the range of " +
            "whatever value is used.";
    public static final String HELP_SCALE = "Scale width with this number.";

    public static final String SAME_AS_BOTTOM = "Same as top";
    public static final String MAKE_FIT_BOTTOM = "Make fit to bottom";

    private final DensiTree m_dt;
    private final TextField textField;
    private final TextField textField_1;
    private final TextField textField_3;
    private final Spinner<Integer> spinner;
    private final Spinner<Integer> spinner_1;
    private final ComboBox<String> comboBoxBottom = new ComboBox<>();
    private final ComboBox<String> comboBoxTop = new ComboBox<>();
    private final CheckBox chckbxZeroBased;

    public LineWidthPanel(DensiTree dt) {
        this.m_dt = dt;
        this.m_dt.addChangeListener(this);

        // Configure GridPane
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        // 1. Bottom ComboBox (Row 0)
        comboBoxBottom.setMaxWidth(Double.MAX_VALUE);
        comboBoxBottom.setTooltip(createTooltip(HELP_LINE_WIDTH));
        comboBoxBottom.setOnAction(e -> {
            Platform.runLater(() -> {
                String selected = comboBoxBottom.getValue();
                if (selected == null) return;

                LineWidthMode oldMode = m_dt.m_settings.m_lineWidthMode;
                String oldTag = m_dt.m_settings.m_lineWidthTag;

                if (selected.equals(LineWidthMode.DEFAULT.toString())) {
                    m_dt.m_settings.m_lineWidthMode = LineWidthMode.DEFAULT;
                } else if (selected.equals(LineWidthMode.BY_METADATA_PATTERN.toString())) {
                    m_dt.m_settings.m_lineWidthMode = LineWidthMode.BY_METADATA_PATTERN;
                } else if (selected.equals(LineWidthMode.BY_METADATA_NUMBER.toString())) {
                    m_dt.m_settings.m_lineWidthMode = LineWidthMode.BY_METADATA_NUMBER;
                } else {
                    m_dt.m_settings.m_lineWidthTag = selected;
                    m_dt.m_settings.m_lineWidthMode = LineWidthMode.BY_METADATA_TAG;
                }

                m_dt.resetStyle();
                if (m_dt.m_settings.m_lineWidthMode != oldMode
                        || (m_dt.m_settings.m_lineWidthTag != null && !m_dt.m_settings.m_lineWidthTag.equals(oldTag))) {
                    m_dt.calcLineWidths(true);
                    m_dt.makeDirty();
                }
                updateEnabled();
                m_dt.repaint();
            });
        });
        GridPane.setColumnSpan(comboBoxBottom, 2);
        GridPane.setHgrow(comboBoxBottom, Priority.ALWAYS);
        add(comboBoxBottom, 0, 0);

        // 2. Bottom Pattern TextField (Row 1)
        textField_1 = new TextField(m_dt.m_settings.m_sLineWidthPattern);
        textField_1.setPrefColumnCount(10);
        textField_1.setTooltip(createTooltip(HELP_PATTERN));
        textField_1.setOnAction(e -> {
            try {
                m_dt.m_settings.m_sLineWidthPattern = textField_1.getText();
                if (m_dt.m_settings.m_lineWidthMode != LineWidthMode.DEFAULT) {
                    m_dt.calcLineWidths(true);
                    m_dt.makeDirty();
                }
            } catch (Exception ex) {
                // Ignore
            }
        });
        GridPane.setColumnSpan(textField_1, 2);
        GridPane.setHgrow(textField_1, Priority.ALWAYS);
        add(textField_1, 0, 1);

        // 3. Top Number Label & Spinner (Row 2)
        Label lblNumberOfItem_1 = new Label("top");
        lblNumberOfItem_1.setTooltip(createTooltip(HELP_TOP));
        GridPane.setHalignment(lblNumberOfItem_1, HPos.RIGHT);
        add(lblNumberOfItem_1, 0, 2);

        int initBottom = Math.max(1, m_dt.m_settings.m_iPatternForBottom);
        spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, initBottom, 1));
        spinner.setPrefWidth(70);
        spinner.setTooltip(createTooltip(HELP_TOP));
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                m_dt.m_settings.m_iPatternForBottom = Math.max(1, newValue);
                if (m_dt.m_settings.m_lineWidthMode != LineWidthMode.DEFAULT) {
                    m_dt.m_settings.m_pattern = m_dt.createPattern();
                    m_dt.calcLineWidths(true);
                    m_dt.makeDirty();
                }
            }
        });
        GridPane.setHalignment(spinner, HPos.RIGHT);
        add(spinner, 1, 2);

        // 4. Separator 1 (Row 3)
        Separator separator_1 = new Separator();
        GridPane.setColumnSpan(separator_1, 2);
        GridPane.setHgrow(separator_1, Priority.ALWAYS);
        add(separator_1, 0, 3);

        // 5. Top ComboBox (Row 4)
        comboBoxTop.setMaxWidth(Double.MAX_VALUE);
        comboBoxTop.setTooltip(createTooltip(HELP_LINE_WIDTH_BOTTOM));
        comboBoxTop.setOnAction(e -> {
            Platform.runLater(() -> {
                String selected = comboBoxTop.getValue();
                if (selected == null) return;

                LineWidthMode oldMode = m_dt.m_settings.m_lineWidthModeTop;
                String oldTag = m_dt.m_settings.m_lineWidthTagTop;
                boolean oldCorrectTopOfBranch = m_dt.m_settings.m_bCorrectTopOfBranch;

                if (selected.equals(SAME_AS_BOTTOM)) {
                    m_dt.m_settings.m_lineWidthModeTop = LineWidthMode.DEFAULT;
                } else if (selected.equals(MAKE_FIT_BOTTOM)) {
                    m_dt.m_settings.m_lineWidthModeTop = LineWidthMode.DEFAULT;
                    m_dt.m_settings.m_bCorrectTopOfBranch = true;
                } else if (selected.equals(LineWidthMode.BY_METADATA_PATTERN.toString())) {
                    m_dt.m_settings.m_lineWidthModeTop = LineWidthMode.BY_METADATA_PATTERN;
                } else if (selected.equals(LineWidthMode.BY_METADATA_NUMBER.toString())) {
                    m_dt.m_settings.m_lineWidthModeTop = LineWidthMode.BY_METADATA_NUMBER;
                } else {
                    m_dt.m_settings.m_lineWidthTagTop = selected;
                    m_dt.m_settings.m_lineWidthModeTop = LineWidthMode.BY_METADATA_TAG;
                }
                m_dt.m_settings.m_bCorrectTopOfBranch = selected.equals(MAKE_FIT_BOTTOM);
                m_dt.resetStyle();

                if (m_dt.m_settings.m_lineWidthModeTop != oldMode
                        || (m_dt.m_settings.m_lineWidthTag != null && !m_dt.m_settings.m_lineWidthTagTop.equals(oldTag))
                        || oldCorrectTopOfBranch != m_dt.m_settings.m_bCorrectTopOfBranch) {
                    m_dt.calcLineWidths(true);
                    m_dt.makeDirty();
                }
                updateEnabled();
                m_dt.repaint();
            });
        });
        GridPane.setColumnSpan(comboBoxTop, 2);
        GridPane.setHgrow(comboBoxTop, Priority.ALWAYS);
        add(comboBoxTop, 0, 4);

        // 6. Top Pattern TextField (Row 5)
        textField = new TextField(m_dt.m_settings.m_sLineWidthPatternTop);
        textField.setPrefColumnCount(10);
        textField.setTooltip(createTooltip(HELP_PATTERN));
        textField.setOnAction(e -> {
            try {
                m_dt.m_settings.m_sLineWidthPatternTop = textField.getText();
                m_dt.calcLineWidths(true);
                m_dt.makeDirty();
            } catch (Exception ex) {
                // Ignore
            }
        });
        GridPane.setColumnSpan(textField, 2);
        GridPane.setHgrow(textField, Priority.ALWAYS);
        add(textField, 0, 5);

        // 7. Bottom Number Label & Spinner (Row 6)
        Label lblNumberOfItem = new Label("bottom");
        lblNumberOfItem.setTooltip(createTooltip(HELP_BOTTOM));
        GridPane.setHalignment(lblNumberOfItem, HPos.RIGHT);
        add(lblNumberOfItem, 0, 6);

        int initTop = Math.max(0, m_dt.m_settings.m_iPatternForTop);
        spinner_1 = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, initTop, 1));
        spinner_1.setPrefWidth(70);
        spinner_1.setTooltip(createTooltip("when 0, top will be equal to bottom"));
        spinner_1.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                m_dt.m_settings.m_iPatternForTop = Math.max(0, newValue);
                if (m_dt.m_settings.m_lineWidthMode != LineWidthMode.DEFAULT) {
                    m_dt.m_settings.m_pattern = m_dt.createPattern();
                    m_dt.calcLineWidths(true);
                    m_dt.makeDirty();
                }
            }
        });
        GridPane.setHalignment(spinner_1, HPos.RIGHT);
        add(spinner_1, 1, 6);

        // 8. Separator (Row 7)
        Separator separator = new Separator();
        GridPane.setColumnSpan(separator, 2);
        GridPane.setHgrow(separator, Priority.ALWAYS);
        add(separator, 0, 7);

        // 9. Zero-based CheckBox (Row 8)
        chckbxZeroBased = new CheckBox("Zero based");
        chckbxZeroBased.setSelected(m_dt.m_settings.m_bWidthsAreZeroBased);
        chckbxZeroBased.setTooltip(createTooltip(HELP_ZERO_BASED));
        chckbxZeroBased.setOnAction(e -> {
            boolean bPrev = m_dt.m_settings.m_bWidthsAreZeroBased;
            m_dt.m_settings.m_bWidthsAreZeroBased = chckbxZeroBased.isSelected();
            if (bPrev != m_dt.m_settings.m_bWidthsAreZeroBased) {
                m_dt.calcLineWidths(true);
                m_dt.makeDirty();
            }
        });
        GridPane.setColumnSpan(chckbxZeroBased, 2);
        GridPane.setHalignment(chckbxZeroBased, HPos.LEFT);
        add(chckbxZeroBased, 0, 8);

        // 10. Scale Label & TextField (Row 9)
        Label lblMetaDataScale = new Label("Scale");
        GridPane.setHalignment(lblMetaDataScale, HPos.RIGHT);
        add(lblMetaDataScale, 0, 9);

        textField_3 = new TextField(String.valueOf(m_dt.m_treeDrawer.LINE_WIDTH_SCALE));
        textField_3.setPrefColumnCount(3);
        textField_3.setTooltip(createTooltip(HELP_SCALE));
        textField_3.setOnAction(e -> {
            try {
                m_dt.m_treeDrawer.LINE_WIDTH_SCALE = Float.parseFloat(textField_3.getText().trim());
                if (m_dt.m_settings.m_lineWidthMode != LineWidthMode.DEFAULT) {
                    m_dt.makeDirty();
                }
            } catch (Exception ex) {
                // Ignore
            }
        });
        GridPane.setHgrow(textField_3, Priority.ALWAYS);
        add(textField_3, 1, 9);

        // Initialize state
        stateChanged(null);
    }

    private void updateEnabled() {
        if (textField_1 == null) {
            return;
        }

        boolean notDefault = (m_dt.m_settings.m_lineWidthMode != LineWidthMode.DEFAULT);
        textField_3.setDisable(!notDefault);
        chckbxZeroBased.setDisable(!notDefault);

        textField_1.setDisable(m_dt.m_settings.m_lineWidthMode != LineWidthMode.BY_METADATA_PATTERN);
        spinner.setDisable(m_dt.m_settings.m_lineWidthMode != LineWidthMode.BY_METADATA_NUMBER);

        textField.setDisable(true);
        spinner_1.setDisable(true);

        if (!notDefault) {
            comboBoxTop.setDisable(true);
        } else {
            comboBoxTop.setDisable(false);
            if (m_dt.m_settings.m_lineWidthModeTop == LineWidthMode.BY_METADATA_PATTERN) {
                textField.setDisable(false);
            }
            if (m_dt.m_settings.m_lineWidthModeTop == LineWidthMode.BY_METADATA_NUMBER) {
                spinner_1.setDisable(false);
            }
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        List<String> selection = new ArrayList<>();
        List<String> selectionTop = new ArrayList<>();
        selection.add(LineWidthMode.DEFAULT.toString());
        selectionTop.add(SAME_AS_BOTTOM);
        selectionTop.add(MAKE_FIT_BOTTOM);

        if (m_dt.m_treeData != null && m_dt.m_treeData.m_bMetaDataReady) {
            selection.add(LineWidthMode.BY_METADATA_PATTERN.toString());
            selectionTop.add(LineWidthMode.BY_METADATA_PATTERN.toString());
            selection.add(LineWidthMode.BY_METADATA_NUMBER.toString());
            selectionTop.add(LineWidthMode.BY_METADATA_NUMBER.toString());
            if (m_dt.m_settings.m_metaDataTags != null) {
                for (int i = 0; i < m_dt.m_settings.m_metaDataTags.size(); i++) {
                    if (m_dt.m_settings.m_metaDataTypes.get(i).equals(MetaDataType.NUMERIC)) {
                        selection.add(m_dt.m_settings.m_metaDataTags.get(i));
                        selectionTop.add(m_dt.m_settings.m_metaDataTags.get(i));
                    }
                }
            }
        }

        comboBoxBottom.getItems().setAll(selection);
        comboBoxTop.getItems().setAll(selectionTop);

        if (m_dt.m_settings.m_lineWidthMode == LineWidthMode.DEFAULT) {
            comboBoxBottom.setValue(LineWidthMode.DEFAULT.toString());
        } else if (m_dt.m_settings.m_lineWidthMode == LineWidthMode.BY_METADATA_PATTERN) {
            comboBoxBottom.setValue(LineWidthMode.BY_METADATA_PATTERN.toString());
        } else if (m_dt.m_settings.m_lineWidthMode == LineWidthMode.BY_METADATA_NUMBER) {
            comboBoxBottom.setValue(LineWidthMode.BY_METADATA_NUMBER.toString());
        } else {
            comboBoxBottom.setValue(m_dt.m_settings.m_lineWidthTag);
        }

        if (m_dt.m_settings.m_lineWidthModeTop == LineWidthMode.DEFAULT) {
            if (m_dt.m_settings.m_bCorrectTopOfBranch) {
                comboBoxTop.setValue(MAKE_FIT_BOTTOM);
            } else {
                comboBoxTop.setValue(SAME_AS_BOTTOM);
            }
        } else if (m_dt.m_settings.m_lineWidthModeTop == LineWidthMode.BY_METADATA_PATTERN) {
            comboBoxTop.setValue(LineWidthMode.BY_METADATA_PATTERN.toString());
        } else if (m_dt.m_settings.m_lineWidthModeTop == LineWidthMode.BY_METADATA_NUMBER) {
            comboBoxTop.setValue(LineWidthMode.BY_METADATA_NUMBER.toString());
        } else {
            comboBoxTop.setValue(m_dt.m_settings.m_lineWidthTagTop);
        }

        updateEnabled();
    }

    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }
}