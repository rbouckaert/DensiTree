package viz.fxpanel;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import viz.DensiTree;

import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.JColorChooser;

public class GeoPanel extends GridPane {

    public static final String HELP_SHOW_GEO_INFO = "Show lines linking tip labels with geographic location " +
            "of tip sample. Line only show up if geographic locations are specified using the 'load locations' " +
            "function.";
    public static final String HELP_LINE_WIDTH = "Width of the line used to link tips with geo locations.";
    public static final String HELP_LOAD_LOCATIONS = "Load locations from KML file. The locations can be specified " +
            "in google earth and saved in a KML file.";
    public static final String HELP_COLOR = "Color of the line used to link tips with geo locations.";

    private final DensiTree m_dt;
    private final Spinner<Integer> spinner;

    public GeoPanel(DensiTree dt) {
        this.m_dt = dt;

        // Configure GridPane spacing and padding
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        // 1. Show Geo Info CheckBox (Row 0)
        CheckBox chckbxShowGeoInfo = new CheckBox("Show geo info\n(if any)");
        chckbxShowGeoInfo.setSelected(m_dt.m_settings.m_bDrawGeo);
        chckbxShowGeoInfo.setTooltip(createTooltip(HELP_SHOW_GEO_INFO));
        chckbxShowGeoInfo.setOnAction(e -> {
            boolean bPrev = m_dt.m_settings.m_bDrawGeo;
            m_dt.m_settings.m_bDrawGeo = chckbxShowGeoInfo.isSelected();
            if (bPrev != m_dt.m_settings.m_bDrawGeo) {
                m_dt.makeDirty();
            }
        });
        GridPane.setColumnSpan(chckbxShowGeoInfo, 2);
        GridPane.setHalignment(chckbxShowGeoInfo, HPos.LEFT);
        add(chckbxShowGeoInfo, 0, 0);

        // 2. Line Width Label & Spinner (Row 1)
        Label lblLineWidth = new Label("Line width");
        lblLineWidth.setTooltip(createTooltip(HELP_LINE_WIDTH));
        GridPane.setHalignment(lblLineWidth, HPos.RIGHT);
        add(lblLineWidth, 0, 1);

        spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, m_dt.m_settings.m_nGeoWidth, 1));
        spinner.setPrefWidth(75);
        spinner.setTooltip(createTooltip(HELP_LINE_WIDTH));
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                try {
                    m_dt.m_settings.m_nGeoWidth = newValue;
                    m_dt.m_Panel.clearImage();
                    m_dt.repaint();
                } catch (Exception ex) {
                    // Ignore
                }
            }
        });
        GridPane.setHalignment(spinner, HPos.RIGHT);
        add(spinner, 1, 1);

        // 3. Load Locations Button (Row 2)
        Button btnLoadLocations = new Button("Load locations");
        btnLoadLocations.setMaxWidth(Double.MAX_VALUE);
        btnLoadLocations.setTooltip(createTooltip(HELP_LOAD_LOCATIONS));
        btnLoadLocations.setOnAction(e -> {
            if (m_dt.a_loadkml != null) {
                m_dt.a_loadkml.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        });
        GridPane.setColumnSpan(btnLoadLocations, 2);
        GridPane.setHgrow(btnLoadLocations, Priority.ALWAYS);
        add(btnLoadLocations, 0, 2);

        // 4. Line Color Button (Row 3)
        Button btnLineColor = new Button("Color");
        btnLineColor.setMaxWidth(Double.MAX_VALUE);
        btnLineColor.setTooltip(createTooltip(HELP_COLOR));
        btnLineColor.setOnAction(e -> {
            Color newColor = JColorChooser.showDialog(
                    m_dt.m_Panel,
                    "Geo Line Color",
                    m_dt.m_settings.m_color[DensiTree.GEOCOLOR]
            );
            if (newColor != null) {
                m_dt.m_settings.m_color[DensiTree.GEOCOLOR] = newColor;
                m_dt.makeDirty();
            }
            m_dt.repaint();
        });
        GridPane.setColumnSpan(btnLineColor, 2);
        GridPane.setHgrow(btnLineColor, Priority.ALWAYS);
        add(btnLineColor, 0, 3);
    }

    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }
}