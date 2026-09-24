package viz.fxpanel;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Color;
import javax.swing.JColorChooser;

import viz.DensiTree;
import viz.GridDrawer.GridMode;
import viz.graphics.JFontChooser;

public class GridPanel extends GridPane {

    public static final String HELP_GRID = "Show lines indicating timescale. Options are to show none, " +
            "short lines at the side of the panel, or full lines over the complete tree set.";
    public static final String HELP_DIGITS = "Set number of significant digits for the grid labels.";
    public static final String HELP_REVERSE = "By setting reverse, the time scale will be drawn forward in time. " +
            "By default, time scale is drawn backward in time, so that the height of a tree is a positive " +
            "number. Also, set 'origin' to the date of the youngest tip.";
    public static final String HELP_FONT = "Set font of the grid labels.";
    public static final String HELP_COLOR = "Set colour of the grid labels.";
    public static final String HELP_ORIGIN = "Set date of the youngest tip.";
    public static final String HELP_AUTOMATIC = "Automatically determine the number of ticks.";
    public static final String HELP_TICKS = "Interval between two ticks.";
    public static final String HELP_OFFSET = "Time added to the ticks. This can be useful when the youngest tip of the " +
            "tree is on a number that is not quite a round number, for example 2003.4. Setting the offset to " +
            "-3.4 ensures the grid lines will be drawn on 2000 instead of through 2003.4.";
    public static final String HELP_SCALE = "Scale time, which can be handy when the tree is in substitutions " +
            "and a clock rate is available from the literature. A negative scale has the same effect as " +
            "selecting 'reverse' with a positive scale.";

    private final DensiTree m_dt;
    private final ToggleGroup m_modeGroup = new ToggleGroup();
    private final TextField m_originTextField;
    private TextField m_ticksTextField = null;
    private TextField m_offsetTextField = null;
    private final Spinner<Integer> spinner;
    private final TextField txtScale;

    public GridPanel(DensiTree dt) {
        this.m_dt = dt;

        // Configure GridPane spacing and padding
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        // 1. Grid Mode Radio Buttons Box (Row 0)
        VBox modeBox = new VBox(4);
        modeBox.setPadding(new Insets(4));
        modeBox.setStyle("-fx-border-color: gray; -fx-border-width: 1px;");

        RadioButton rdbtnNone = new RadioButton("No grid");
        rdbtnNone.setToggleGroup(m_modeGroup);
        rdbtnNone.setTooltip(createTooltip(HELP_GRID));
        rdbtnNone.setOnAction(e -> {
            m_dt.m_gridDrawer.m_nGridMode = GridMode.NONE;
            m_dt.makeDirty();
        });

        RadioButton rdbtnShort = new RadioButton("Short grid");
        rdbtnShort.setToggleGroup(m_modeGroup);
        rdbtnShort.setTooltip(createTooltip(HELP_GRID));
        rdbtnShort.setOnAction(e -> {
            m_dt.m_gridDrawer.m_nGridMode = GridMode.SHORT;
            m_dt.makeDirty();
        });

        RadioButton rdbtnFull = new RadioButton("Full grid");
        rdbtnFull.setToggleGroup(m_modeGroup);
        rdbtnFull.setTooltip(createTooltip(HELP_GRID));
        rdbtnFull.setOnAction(e -> {
            m_dt.m_gridDrawer.m_nGridMode = GridMode.FULL;
            m_dt.makeDirty();
        });

        // Initial selection
        if (m_dt.m_gridDrawer.m_nGridMode == GridMode.SHORT) {
            rdbtnShort.setSelected(true);
        } else if (m_dt.m_gridDrawer.m_nGridMode == GridMode.FULL) {
            rdbtnFull.setSelected(true);
        } else {
            rdbtnNone.setSelected(true);
        }

        modeBox.getChildren().addAll(rdbtnNone, rdbtnShort, rdbtnFull);
        GridPane.setColumnSpan(modeBox, 2);
        GridPane.setHgrow(modeBox, Priority.ALWAYS);
        add(modeBox, 0, 0);

        // 2. Digits Label & Spinner (Row 1)
        Label lblDigits = new Label("Digits");
        lblDigits.setTooltip(createTooltip(HELP_DIGITS));
        GridPane.setHalignment(lblDigits, HPos.LEFT);
        add(lblDigits, 0, 1);

        spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, m_dt.m_gridDrawer.m_nGridDigits, 1));
        spinner.setPrefWidth(70);
        spinner.setTooltip(createTooltip(HELP_DIGITS));
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                m_dt.m_gridDrawer.m_nGridDigits = newValue;
                m_dt.makeDirty();
            }
        });
        GridPane.setHalignment(spinner, HPos.RIGHT);
        add(spinner, 1, 1);

        // 3. Reverse CheckBox (Row 2)
        CheckBox reverseGrid = new CheckBox("Reverse");
        reverseGrid.setSelected(m_dt.m_gridDrawer.m_bReverseGrid);
        reverseGrid.setTooltip(createTooltip(HELP_REVERSE));
        reverseGrid.setOnAction(e -> {
            m_dt.m_gridDrawer.m_bReverseGrid = reverseGrid.isSelected();
            m_dt.m_Panel.clearImage();
            m_dt.repaint();
        });
        GridPane.setColumnSpan(reverseGrid, 2);
        add(reverseGrid, 0, 2);

        // 4. Font & Color Buttons (Row 3)
        Button btnGridFont = new Button("Font");
        btnGridFont.setMaxWidth(Double.MAX_VALUE);
        btnGridFont.setTooltip(createTooltip(HELP_FONT));
        btnGridFont.setOnAction(e -> {
            JFontChooser fontChooser = new JFontChooser();
            if (m_dt.m_gridDrawer.m_gridfont != null) {
                fontChooser.setSelectedFont(m_dt.m_gridDrawer.m_gridfont);
            }
            int result = fontChooser.showDialog(null);
            if (result == JFontChooser.OK_OPTION) {
                m_dt.m_gridDrawer.m_gridfont = fontChooser.getSelectedFont();
                m_dt.makeDirty();
                m_dt.repaint();
            }
        });
        GridPane.setHgrow(btnGridFont, Priority.ALWAYS);
        add(btnGridFont, 0, 3);

        Button btnGridColor = new Button("Color");
        btnGridColor.setMaxWidth(Double.MAX_VALUE);
        btnGridColor.setTooltip(createTooltip(HELP_COLOR));
        btnGridColor.setOnAction(e -> {
            Color newColor = JColorChooser.showDialog(
                    m_dt.m_Panel,
                    "Grid Color",
                    m_dt.m_settings.m_color[DensiTree.HEIGHTCOLOR]
            );
            if (newColor != null) {
                m_dt.m_settings.m_color[DensiTree.HEIGHTCOLOR] = newColor;
                m_dt.makeDirty();
            }
            m_dt.repaint();
        });
        GridPane.setHgrow(btnGridColor, Priority.ALWAYS);
        add(btnGridColor, 1, 3);

        // 5. Origin Label & TextField (Row 4)
        Label lblOffset = new Label("Origin");
        lblOffset.setTooltip(createTooltip(HELP_ORIGIN));
        add(lblOffset, 0, 4);

        m_originTextField = new TextField(String.valueOf(m_dt.m_gridDrawer.m_fGridOrigin));
        m_originTextField.setPrefColumnCount(4);
        m_originTextField.setTooltip(createTooltip(HELP_ORIGIN));
        m_originTextField.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                m_dt.m_gridDrawer.m_fGridOrigin = Float.parseFloat(newValue.trim());
                m_dt.m_Panel.clearImage();
                m_dt.repaint();
            } catch (NumberFormatException ex) {
                // Ignore while typing invalid numbers
            }
        });
        GridPane.setHgrow(m_originTextField, Priority.ALWAYS);
        add(m_originTextField, 1, 4);

        // 6. Automatic CheckBox (Row 5)
        CheckBox chckbxAutomatic = new CheckBox("Automatic");
        chckbxAutomatic.setSelected(m_dt.m_gridDrawer.m_bAutoGrid);
        chckbxAutomatic.setTooltip(createTooltip(HELP_AUTOMATIC));
        chckbxAutomatic.setOnAction(e -> {
            boolean bPrev = m_dt.m_gridDrawer.m_bAutoGrid;
            m_dt.m_gridDrawer.m_bAutoGrid = chckbxAutomatic.isSelected();
            m_ticksTextField.setDisable(m_dt.m_gridDrawer.m_bAutoGrid);
            m_offsetTextField.setDisable(m_dt.m_gridDrawer.m_bAutoGrid);
            if (bPrev != m_dt.m_gridDrawer.m_bAutoGrid) {
                m_dt.makeDirty();
                m_dt.repaint();
            }
        });
        GridPane.setColumnSpan(chckbxAutomatic, 2);
        add(chckbxAutomatic, 0, 5);

        // 7. Ticks Label & TextField (Row 6)
        Label lblTicks = new Label("Ticks");
        lblTicks.setTooltip(createTooltip(HELP_TICKS));
        add(lblTicks, 0, 6);

        m_ticksTextField = new TextField(String.valueOf(m_dt.m_gridDrawer.m_fGridTicks));
        m_ticksTextField.setPrefColumnCount(4);
        m_ticksTextField.setTooltip(createTooltip(HELP_TICKS));
        m_ticksTextField.setDisable(m_dt.m_gridDrawer.m_bAutoGrid);
        m_ticksTextField.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                float fGridTicks = Float.parseFloat(newValue.trim());
                if (fGridTicks > 0) {
                    m_dt.m_gridDrawer.m_fGridTicks = fGridTicks;
                    m_dt.makeDirty();
                    m_dt.repaint();
                }
            } catch (NumberFormatException ex) {
                // Ignore while typing invalid numbers
            }
        });
        GridPane.setHgrow(m_ticksTextField, Priority.ALWAYS);
        add(m_ticksTextField, 1, 6);

        // 8. Offset Label & TextField (Row 7)
        Label lblOrigin = new Label("Offset");
        lblOrigin.setTooltip(createTooltip(HELP_OFFSET));
        add(lblOrigin, 0, 7);

        m_offsetTextField = new TextField(String.valueOf(m_dt.m_gridDrawer.m_fGridOffset));
        m_offsetTextField.setPrefColumnCount(4);
        m_offsetTextField.setTooltip(createTooltip(HELP_OFFSET));
        m_offsetTextField.setDisable(m_dt.m_gridDrawer.m_bAutoGrid);
        m_offsetTextField.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                m_dt.m_gridDrawer.m_fGridOffset = Float.parseFloat(newValue.trim());
                m_dt.makeDirty();
                m_dt.repaint();
            } catch (NumberFormatException ex) {
                // Ignore while typing invalid numbers
            }
        });
        GridPane.setHgrow(m_offsetTextField, Priority.ALWAYS);
        add(m_offsetTextField, 1, 7);

        // 9. Separator (Row 8)
        Separator separator = new Separator();
        GridPane.setColumnSpan(separator, 2);
        GridPane.setHgrow(separator, Priority.ALWAYS);
        add(separator, 0, 8);

        // 10. Scale Label & TextField (Row 9)
        Label lblS = new Label("Scale");
        lblS.setTooltip(createTooltip(HELP_SCALE));
        add(lblS, 0, 9);

        txtScale = new TextField(String.valueOf(m_dt.m_fUserScale));
        txtScale.setPrefColumnCount(4);
        txtScale.setTooltip(createTooltip(HELP_SCALE));
        txtScale.setOnAction(e -> {
            try {
                m_dt.m_fUserScale = Float.parseFloat(txtScale.getText().trim());
                m_dt.updateCladeModel();
                m_dt.makeDirty();
                m_dt.repaint();
            } catch (Exception ex) {
                // Ignore
            }
        });
        GridPane.setHgrow(txtScale, Priority.ALWAYS);
        add(txtScale, 1, 9);
    }

    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }
}