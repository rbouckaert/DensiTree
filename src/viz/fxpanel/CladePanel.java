package viz.fxpanel;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import viz.DensiTree;
import viz.graphics.JFontChooser;

public class CladePanel extends GridPane implements ChangeListener {

    public static final String HELP_SHOW_CLADES = "Show information for individual clades. This is only " +
            "activated when the drawing style is not default. The default style is not clade based, so " +
            "there is no information to position clade information.";
    public static final String HELP_SELECTED_ONLY = "Show information only for selected clades. Clades can " +
            "be selected by clicking them in the DensiTree, or selecting them from the clade-bar (at botom " +
            "of the screen).";
    public static final String HELP_MEAN = "Show mean height of clades as line or and/as text.";
    public static final String HELP_95HPD = "Show 95% highest probability density interval of the height of clades as bar and/or as text.";
    public static final String HELP_SUPPORT = "Show support of clade as cricle and/or as text. The support is the fraction of " +
            "trees in the tree set that contain the clade.";
    public static final String HELP_DIGITS = "Number of significant digits to show clade information as text.";
    public static final String HELP_FONT = "Font used to show clade information as text.";
    public static final String HELP_COLOR = "Color used to show clade information as text.";

    private final DensiTree m_dt;

    private final CheckBox chckbxSelectionOnly = new CheckBox("Selected only");
    private final CheckBox chckbxShowClades = new CheckBox("Show clades");
    private final CheckBox chckbxMean = new CheckBox("");
    private final CheckBox checkBox = new CheckBox("");
    private final CheckBox chckbxhpd = new CheckBox("");
    private final CheckBox checkBox_1 = new CheckBox("");
    private final CheckBox chckbxSupport = new CheckBox("");
    private final CheckBox checkBox_2 = new CheckBox("");
    private final Button btnFont = new Button("Font");
    private final Spinner<Integer> spinner;
    private final Button btnColor = new Button("Color");
    private final Label lblNewLabel = new Label("Smallest");
    private final TextField textFieldSmallestClade = new TextField();

    public CladePanel(DensiTree dt) {
        this.m_dt = dt;
        this.m_dt.addChangeListener(this);

        // Configure Grid layout spacing
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        // Row 0: Show clades
        chckbxShowClades.setSelected(m_dt.m_settings.m_bViewClades);
        chckbxShowClades.setTooltip(createTooltip(HELP_SHOW_CLADES));
        chckbxShowClades.setOnAction(e -> {
            boolean bPrev = m_dt.m_settings.m_bViewClades;
            m_dt.m_settings.m_bViewClades = chckbxShowClades.isSelected();
            if (bPrev != m_dt.m_settings.m_bViewClades) {
                m_dt.makeDirty();
            }
        });
        GridPane.setColumnSpan(chckbxShowClades, 3);
        GridPane.setHalignment(chckbxShowClades, HPos.LEFT);
        add(chckbxShowClades, 0, 0);

        // Row 1: Selected only
        chckbxSelectionOnly.setSelected(m_dt.m_cladeDrawer.m_bSelectedOnly);
        chckbxSelectionOnly.setTooltip(createTooltip(HELP_SELECTED_ONLY));
        chckbxSelectionOnly.setOnAction(e -> {
            boolean bPrev = m_dt.m_cladeDrawer.m_bSelectedOnly;
            m_dt.m_cladeDrawer.m_bSelectedOnly = chckbxSelectionOnly.isSelected();
            if (bPrev != m_dt.m_cladeDrawer.m_bSelectedOnly) {
                m_dt.makeDirty();
            }
        });
        GridPane.setColumnSpan(chckbxSelectionOnly, 3);
        GridPane.setHalignment(chckbxSelectionOnly, HPos.LEFT);
        add(chckbxSelectionOnly, 0, 1);

        // Row 2: Header Labels "draw" and "text"
        Label btnDraw = new Label("draw");
        GridPane.setColumnSpan(btnDraw, 2);
        GridPane.setHalignment(btnDraw, HPos.RIGHT);
        add(btnDraw, 0, 2);

        Label btnText = new Label("text");
        GridPane.setHalignment(btnText, HPos.CENTER);
        add(btnText, 2, 2);

        // Row 3: Mean
        Label lblMean = new Label("Mean");
        GridPane.setHalignment(lblMean, HPos.RIGHT);
        add(lblMean, 0, 3);

        chckbxMean.setSelected(m_dt.m_cladeDrawer.m_bDrawMean);
        chckbxMean.setTooltip(createTooltip(HELP_MEAN));
        chckbxMean.setOnAction(e -> {
            boolean bPrev = m_dt.m_cladeDrawer.m_bDrawMean;
            m_dt.m_cladeDrawer.m_bDrawMean = chckbxMean.isSelected();
            if (bPrev != m_dt.m_cladeDrawer.m_bDrawMean) {
                m_dt.makeDirty();
            }
        });
        GridPane.setHalignment(chckbxMean, HPos.CENTER);
        add(chckbxMean, 1, 3);

        checkBox.setSelected(m_dt.m_cladeDrawer.m_bTextMean);
        checkBox.setTooltip(createTooltip(HELP_MEAN));
        checkBox.setOnAction(e -> {
            boolean bPrev = m_dt.m_cladeDrawer.m_bTextMean;
            m_dt.m_cladeDrawer.m_bTextMean = checkBox.isSelected();
            if (bPrev != m_dt.m_cladeDrawer.m_bTextMean) {
                m_dt.makeDirty();
            }
        });
        GridPane.setHalignment(checkBox, HPos.RIGHT);
        add(checkBox, 2, 3);

        // Row 4: 95% HPD
        Label lblhpd = new Label("95%HPD");
        GridPane.setHalignment(lblhpd, HPos.RIGHT);
        add(lblhpd, 0, 4);

        chckbxhpd.setSelected(m_dt.m_cladeDrawer.m_bDraw95HPD);
        chckbxhpd.setTooltip(createTooltip(HELP_95HPD));
        chckbxhpd.setOnAction(e -> {
            boolean bPrev = m_dt.m_cladeDrawer.m_bDraw95HPD;
            m_dt.m_cladeDrawer.m_bDraw95HPD = chckbxhpd.isSelected();
            if (bPrev != m_dt.m_cladeDrawer.m_bDraw95HPD) {
                m_dt.makeDirty();
            }
        });
        GridPane.setValignment(chckbxhpd, VPos.TOP);
        GridPane.setHalignment(chckbxhpd, HPos.CENTER);
        add(chckbxhpd, 1, 4);

        checkBox_1.setSelected(m_dt.m_cladeDrawer.m_bText95HPD);
        checkBox_1.setTooltip(createTooltip(HELP_95HPD));
        checkBox_1.setOnAction(e -> {
            boolean bPrev = m_dt.m_cladeDrawer.m_bText95HPD;
            m_dt.m_cladeDrawer.m_bText95HPD = checkBox_1.isSelected();
            if (bPrev != m_dt.m_cladeDrawer.m_bText95HPD) {
                m_dt.makeDirty();
            }
        });
        GridPane.setHalignment(checkBox_1, HPos.RIGHT);
        add(checkBox_1, 2, 4);

        // Row 5: Support
        Label lblSupport = new Label("Support");
        GridPane.setHalignment(lblSupport, HPos.RIGHT);
        add(lblSupport, 0, 5);

        chckbxSupport.setSelected(m_dt.m_cladeDrawer.m_bDrawSupport);
        chckbxSupport.setTooltip(createTooltip(HELP_SUPPORT));
        chckbxSupport.setOnAction(e -> {
            boolean bPrev = m_dt.m_cladeDrawer.m_bDrawSupport;
            m_dt.m_cladeDrawer.m_bDrawSupport = chckbxSupport.isSelected();
            if (bPrev != m_dt.m_cladeDrawer.m_bDrawSupport) {
                m_dt.makeDirty();
            }
        });
        GridPane.setHalignment(chckbxSupport, HPos.CENTER);
        add(chckbxSupport, 1, 5);

        checkBox_2.setSelected(m_dt.m_cladeDrawer.m_bTextSupport);
        checkBox_2.setTooltip(createTooltip(HELP_SUPPORT));
        checkBox_2.setOnAction(e -> {
            boolean bPrev = m_dt.m_cladeDrawer.m_bTextSupport;
            m_dt.m_cladeDrawer.m_bTextSupport = checkBox_2.isSelected();
            if (bPrev != m_dt.m_cladeDrawer.m_bTextSupport) {
                m_dt.makeDirty();
            }
        });
        GridPane.setHalignment(checkBox_2, HPos.RIGHT);
        add(checkBox_2, 2, 5);

        // Row 6: Separator
        Separator separator = new Separator();
        GridPane.setColumnSpan(separator, 4);
        GridPane.setHgrow(separator, Priority.ALWAYS);
        add(separator, 0, 6);

        // Row 7: Significant Digits
        Label lblDigits = new Label("Sign. Digits");
        GridPane.setColumnSpan(lblDigits, 2);
        GridPane.setHalignment(lblDigits, HPos.RIGHT);
        add(lblDigits, 0, 7);

        spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, m_dt.m_cladeDrawer.m_nSignificantDigits));
        spinner.setPrefWidth(70);
        spinner.setTooltip(createTooltip(HELP_DIGITS));
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                m_dt.m_cladeDrawer.m_nSignificantDigits = newValue;
                m_dt.makeDirty();
            }
        });
        GridPane.setHalignment(spinner, HPos.RIGHT);
        add(spinner, 2, 7);

        // Row 8: Font & Color
        btnFont.setTooltip(createTooltip(HELP_FONT));
        btnFont.setOnAction(e -> {
            JFontChooser fontChooser = new JFontChooser();
            if (m_dt.m_cladeDrawer.m_font != null) {
                fontChooser.setSelectedFont(m_dt.m_cladeDrawer.m_font);
            }
            int result = fontChooser.showDialog(null);
            if (result == JFontChooser.OK_OPTION) {
                m_dt.m_cladeDrawer.m_font = fontChooser.getSelectedFont();
                m_dt.makeDirty();
                m_dt.repaint();
            }
        });
        add(btnFont, 0, 8);

        btnColor.setTooltip(createTooltip(HELP_COLOR));
        btnColor.setOnAction(e -> {
            java.awt.Color newColor = javax.swing.JColorChooser.showDialog(m_dt.m_Panel, "Choose Clade Text Color", m_dt.m_cladeDrawer.m_color);
            if (newColor != null) {
                m_dt.m_cladeDrawer.m_color = newColor;
                m_dt.makeDirty();
            }
            m_dt.repaint();
        });
        GridPane.setColumnSpan(btnColor, 2);
        add(btnColor, 1, 8);

        // Row 9: Smallest Clade Support
        lblNewLabel.setTooltip(createTooltip("Smallest clade support to be considered a clade"));
        add(lblNewLabel, 0, 9);

        textFieldSmallestClade.setText(String.valueOf(m_dt.m_settings.m_smallestCladeSupport));
        textFieldSmallestClade.setPrefColumnCount(3);
        textFieldSmallestClade.setOnAction(e -> {
            try {
                m_dt.m_settings.m_smallestCladeSupport = Double.parseDouble(textFieldSmallestClade.getText().trim());
                m_dt.updateCladeModel();
            } catch (Exception ex) {
                // Ignore parse errors
            }
        });
        GridPane.setColumnSpan(textFieldSmallestClade, 2);
        GridPane.setHgrow(textFieldSmallestClade, Priority.ALWAYS);
        add(textFieldSmallestClade, 1, 9);

        // Set initial enabled/disabled states
        stateChanged(null);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        boolean enabled = (m_dt.m_settings.m_Xmode != 0);
        this.setDisable(!enabled);
    }

    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }
}