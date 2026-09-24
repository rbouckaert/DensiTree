package viz.fxpanel;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JColorChooser;

import viz.DensiTree;
import viz.graphics.JFontChooser;

public class LabelPanel extends GridPane {

    public static final String HELP_LABEL_WIDTH = "Width of the label.";
    public static final String HELP_ROTATE = "Rotate label -- is only effective when root at top.";
    public static final String HELP_ALIGN = "Align labels with label for youngest tip. This is only useful when tips are not all from the same date.";
    public static final String HELP_HIDE = "Hide labels.";
    public static final String HELP_FONT = "Font used for labels.";
    public static final String HELP_COLOR = "Color used for labels.";
    public static final String HELP_SEARCH = "Search for labels. Labels matching the search string will be selected/highlighted.";
    public static final String HELP_LOAD = "Load images for all taxa.";
    public static final String HELP_IMAGE_SIZE = "Size of images used in labels.";

    private final DensiTree m_dt;
    private final TextField textField;
    private final TextField textField_1;
    private final TextField textField_2;

    public LabelPanel(DensiTree dt) {
        this.m_dt = dt;

        // Configure GridPane spacing and padding
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        // Row 0: Width
        Label lblWidth = new Label("Width");
        lblWidth.setTooltip(createTooltip(HELP_LABEL_WIDTH));
        GridPane.setHalignment(lblWidth, HPos.LEFT);
        add(lblWidth, 0, 0);

        textField = new TextField(String.valueOf(m_dt.m_settings.m_nLabelWidth));
        textField.setPrefColumnCount(5);
        textField.setTooltip(createTooltip(HELP_LABEL_WIDTH));
        textField.setOnAction(e -> {
            try {
                m_dt.m_settings.m_nLabelWidth = Integer.parseInt(textField.getText().trim());
            } catch (Exception ex) {
                // Ignore parse errors
            }
            m_dt.fitToScreen();
        });
        GridPane.setHgrow(textField, Priority.ALWAYS);
        add(textField, 1, 0);

        // Row 1: Indent
        Label lblIndent = new Label("Indent");
        GridPane.setHalignment(lblIndent, HPos.LEFT);
        add(lblIndent, 0, 1);

        textField_2 = new TextField(String.valueOf(m_dt.m_settings.m_fLabelIndent));
        textField_2.setPrefColumnCount(5);
        textField_2.setOnAction(e -> {
            try {
                m_dt.m_settings.m_fLabelIndent = Float.parseFloat(textField_2.getText().trim());
            } catch (Exception ex) {
                m_dt.m_settings.m_fLabelIndent = 0.0f;
            }
            m_dt.fitToScreen();
        });
        GridPane.setHgrow(textField_2, Priority.ALWAYS);
        add(textField_2, 1, 1);

        // Row 2: Rotate CheckBox
        CheckBox chckbxRotate = new CheckBox("Rotate");
        chckbxRotate.setSelected(m_dt.m_settings.m_bRotateTextWhenRootAtTop);
        chckbxRotate.setTooltip(createTooltip(HELP_ROTATE));
        chckbxRotate.setOnAction(e -> {
            m_dt.m_settings.m_bRotateTextWhenRootAtTop = chckbxRotate.isSelected();
            m_dt.fitToScreen();
        });
        GridPane.setHalignment(chckbxRotate, HPos.LEFT);
        add(chckbxRotate, 1, 2);

        // Row 3: Align CheckBox
        CheckBox chckbxAlign = new CheckBox("Align");
        chckbxAlign.setSelected(m_dt.m_bAlignLabels);
        chckbxAlign.setTooltip(createTooltip(HELP_ALIGN));
        chckbxAlign.setOnAction(e -> {
            m_dt.m_bAlignLabels = chckbxAlign.isSelected();
            m_dt.makeDirty();
        });
        GridPane.setHalignment(chckbxAlign, HPos.LEFT);
        add(chckbxAlign, 1, 3);

        // Row 4: Hide CheckBox
        CheckBox chckbxHide = new CheckBox("Hide");
        chckbxHide.setSelected(m_dt.m_settings.m_bHideLabels);
        chckbxHide.setTooltip(createTooltip(HELP_HIDE));
        chckbxHide.setOnAction(e -> {
            m_dt.m_settings.m_bHideLabels = chckbxHide.isSelected();
            m_dt.makeDirty();
        });
        GridPane.setHalignment(chckbxHide, HPos.LEFT);
        add(chckbxHide, 1, 4);

        // Row 5: Font & Color Buttons
        Button btnFont = new Button("Font");
        btnFont.setMaxWidth(Double.MAX_VALUE);
        btnFont.setTooltip(createTooltip(HELP_FONT));
        btnFont.setOnAction(e -> {
            JFontChooser fontChooser = new JFontChooser();
            if (m_dt.m_font != null) {
                fontChooser.setSelectedFont(m_dt.m_font);
            }
            int result = fontChooser.showDialog(null);
            if (result == JFontChooser.OK_OPTION) {
                m_dt.m_font = fontChooser.getSelectedFont();
                m_dt.makeDirty();
                m_dt.repaint();
            }
        });
        GridPane.setHgrow(btnFont, Priority.ALWAYS);
        add(btnFont, 0, 5);

        Button btnColor = new Button("Color");
        btnColor.setMaxWidth(Double.MAX_VALUE);
        btnColor.setTooltip(createTooltip(HELP_COLOR));
        btnColor.setOnAction(e -> {
            Color newColor = JColorChooser.showDialog(
                    m_dt.m_Panel,
                    "Label Color",
                    m_dt.m_settings.m_color[DensiTree.LABELCOLOR]
            );
            if (newColor != null) {
                m_dt.m_settings.m_color[DensiTree.LABELCOLOR] = newColor;
                m_dt.makeDirty();
            }
            m_dt.repaint();
        });
        GridPane.setHgrow(btnColor, Priority.ALWAYS);
        add(btnColor, 1, 5);

        // Row 6: Search
        Label lblSearch = new Label("Search");
        lblSearch.setTooltip(createTooltip(HELP_SEARCH));
        GridPane.setHalignment(lblSearch, HPos.LEFT);
        add(lblSearch, 0, 6);

        textField_1 = new TextField();
        textField_1.setPrefColumnCount(5);
        textField_1.setTooltip(createTooltip(HELP_SEARCH));
        textField_1.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                String sPattern = ".*" + (newValue != null ? newValue : "") + ".*";
                Pattern pattern = Pattern.compile(sPattern);
                if (m_dt.m_settings.m_sLabels != null && m_dt.m_treeData != null && m_dt.m_treeData.m_bSelection != null) {
                    for (int i = 0; i < m_dt.m_settings.m_sLabels.size(); i++) {
                        Matcher m = pattern.matcher(m_dt.m_settings.m_sLabels.get(i));
                        m_dt.m_treeData.m_bSelection[i] = m.find();
                    }
                    if (m_dt.m_Panel != null) {
                        m_dt.m_Panel.repaint();
                    }
                }
            } catch (Exception ex) {
                // Ignore regex compilation errors while typing
            }
        });
        GridPane.setHgrow(textField_1, Priority.ALWAYS);
        add(textField_1, 1, 6);

        // Row 7: Load image map Button
        Button btnLoad = new Button("Load image map");
        btnLoad.setMaxWidth(Double.MAX_VALUE);
        btnLoad.setTooltip(createTooltip(HELP_LOAD));
        btnLoad.setOnAction(e -> m_dt.loadImages());
        GridPane.setColumnSpan(btnLoad, 2);
        GridPane.setHgrow(btnLoad, Priority.ALWAYS);
        add(btnLoad, 0, 7);
    }

    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }
}