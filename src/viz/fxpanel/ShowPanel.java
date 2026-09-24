package viz.fxpanel;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.ArrayList;
import java.util.List;

import viz.DensiTree;

public class ShowPanel extends GridPane implements ChangeListener {

    public static final String HELP_CONSENSUS_TREES = "Display consensus trees. There is one consensus tree for every topology " +
            "in the tree set. The height of the nodes are the average of the heights for that topology.";
    public static final String HELP_ALL_TREES = "Show all trees in the tree set.";
    public static final String HELP_ROOT_CANAL = "Show root canal tree. This is a single summary tree representing the complete tree set. " +
            "There are many ways to construct a summary tree.";
    public static final String HELP_ROOT_CANAL_NUMBER = "Select root canal tree to display.";
    public static final String HELP_IMPORT = "Import root canal tree from Newick or from the summary_tree program.";
    public static final String HELP_ROOT_AT_TOP = "Display the root at the top of the display instead of on the left hand side.";
    public static final String HELP_EDIT_TREE = "Display edit tree for manipulating order of tree and position of internal nodes. " +
            "Works only with default drawing style.";

    private final DensiTree m_dt;
    private final CheckBox chckbxShowEditTree = new CheckBox("Edit Tree");
    private final ComboBox<String> comboBox = new ComboBox<>();
    private final CheckBox checkBoxShowRotoCanal = new CheckBox("Root Canal");
    private final Button btnImport = new Button("import");

    public ShowPanel(DensiTree dt) {
        this.m_dt = dt;
        this.m_dt.addChangeListener(this);

        // Configure GridPane spacing and padding
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        // Row 0: Consensus Trees
        CheckBox checkBox_1 = new CheckBox("Consensus Trees");
        checkBox_1.setSelected(m_dt.m_bViewCTrees);
        checkBox_1.setTooltip(createTooltip(HELP_CONSENSUS_TREES));
        checkBox_1.setOnAction(e -> {
            boolean bPrev = m_dt.m_bViewCTrees;
            m_dt.m_bViewCTrees = checkBox_1.isSelected();
            if (bPrev != m_dt.m_bViewCTrees) {
                m_dt.makeDirty();
            }
        });
        GridPane.setColumnSpan(checkBox_1, 2);
        GridPane.setHalignment(checkBox_1, HPos.LEFT);
        add(checkBox_1, 0, 0);

        // Row 1: All Trees
        CheckBox checkBox = new CheckBox("All Trees");
        checkBox.setSelected(m_dt.m_bViewAllTrees);
        checkBox.setTooltip(createTooltip(HELP_ALL_TREES));
        checkBox.setOnAction(e -> {
            boolean bPrev = m_dt.m_bViewAllTrees;
            m_dt.m_bViewAllTrees = checkBox.isSelected();
            if (bPrev != m_dt.m_bViewAllTrees) {
                m_dt.makeDirty();
            }
        });
        GridPane.setColumnSpan(checkBox, 2);
        GridPane.setHalignment(checkBox, HPos.LEFT);
        add(checkBox, 0, 1);

        // Row 2: Separator 1
        Separator separator = new Separator();
        GridPane.setColumnSpan(separator, 2);
        GridPane.setHgrow(separator, Priority.ALWAYS);
        add(separator, 0, 2);

        // Row 3: Root Canal CheckBox
        checkBoxShowRotoCanal.setSelected(m_dt.m_settings.m_bShowRootCanalTopology);
        checkBoxShowRotoCanal.setTooltip(createTooltip(HELP_ROOT_CANAL));
        checkBoxShowRotoCanal.setOnAction(e -> {
            boolean bPrev = m_dt.m_settings.m_bShowRootCanalTopology;
            m_dt.m_settings.m_bShowRootCanalTopology = checkBoxShowRotoCanal.isSelected();
            if (bPrev != m_dt.m_settings.m_bShowRootCanalTopology) {
                m_dt.makeDirty();
            }
        });
        GridPane.setColumnSpan(checkBoxShowRotoCanal, 2);
        GridPane.setHalignment(checkBoxShowRotoCanal, HPos.LEFT);
        add(checkBoxShowRotoCanal, 0, 3);

        // Row 4: Root Canal Number ComboBox & Import Button
        List<String> labels = new ArrayList<>();
        if (m_dt.m_treeData != null && m_dt.m_treeData.m_summaryTree != null) {
            for (int i = 0; i < m_dt.m_treeData.m_summaryTree.size(); i++) {
                labels.add(String.valueOf(i + 1));
            }
        }
        comboBox.getItems().setAll(labels);
        comboBox.setTooltip(createTooltip(HELP_ROOT_CANAL_NUMBER));
        comboBox.setOnAction(e -> {
            int i = comboBox.getSelectionModel().getSelectedIndex();
            if (m_dt.m_treeData != null && m_dt.m_treeData.m_summaryTree != null) {
                if (i >= 0 && i < m_dt.m_treeData.m_summaryTree.size()) {
                    m_dt.m_treeData.m_rootcanaltree = m_dt.m_treeData.m_summaryTree.get(i);
                    m_dt.calcLines();
                    m_dt.makeDirty();
                }
            }
        });
        GridPane.setHgrow(comboBox, Priority.ALWAYS);
        add(comboBox, 0, 4);

        btnImport.setTooltip(createTooltip(HELP_IMPORT));
        btnImport.setOnAction(e -> {
            ImportRootCanalDialog dlg = new ImportRootCanalDialog(m_dt);
            if (dlg.showDialog(this)) {
                comboBox.getItems().add(String.valueOf(comboBox.getItems().size() + 1));
                comboBox.getSelectionModel().selectLast();
                m_dt.calcPositions();
                m_dt.calcLines();
                m_dt.makeDirty();
            }
        });
        add(btnImport, 1, 4);

        // Row 5: Separator 2
        Separator separator_1 = new Separator();
        GridPane.setColumnSpan(separator_1, 2);
        GridPane.setHgrow(separator_1, Priority.ALWAYS);
        add(separator_1, 0, 5);

        // Row 6: Root At Top
        CheckBox checkBox_3 = new CheckBox("Root At Top");
        checkBox_3.setSelected(m_dt.m_treeDrawer.m_bRootAtTop);
        checkBox_3.setTooltip(createTooltip(HELP_ROOT_AT_TOP));
        checkBox_3.setOnAction(e -> {
            boolean bPrev = m_dt.m_treeDrawer.m_bRootAtTop;
            m_dt.m_treeDrawer.m_bRootAtTop = checkBox_3.isSelected();
            if (bPrev != m_dt.m_treeDrawer.m_bRootAtTop) {
                m_dt.fitToScreen();
            }
        });
        GridPane.setColumnSpan(checkBox_3, 2);
        GridPane.setHalignment(checkBox_3, HPos.LEFT);
        add(checkBox_3, 0, 6);

        // Row 7: Edit Tree
        chckbxShowEditTree.setSelected(m_dt.m_settings.m_bViewEditTree);
        chckbxShowEditTree.setTooltip(createTooltip(HELP_EDIT_TREE));
        chckbxShowEditTree.setOnAction(e -> {
            boolean bPrev = m_dt.m_settings.m_bViewEditTree;
            m_dt.m_settings.m_bViewEditTree = chckbxShowEditTree.isSelected();
            if (bPrev != m_dt.m_settings.m_bViewEditTree) {
                m_dt.makeDirty();
            }
        });
        GridPane.setColumnSpan(chckbxShowEditTree, 2);
        GridPane.setHalignment(chckbxShowEditTree, HPos.LEFT);
        add(chckbxShowEditTree, 0, 7);

        // Initialize state
        stateChanged(null);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        chckbxShowEditTree.setDisable(m_dt.m_settings.m_Xmode != 0);

        if (m_dt.m_treeData != null && m_dt.m_treeData.m_summaryTree != null && !m_dt.m_treeData.m_summaryTree.isEmpty()) {
            int summarySize = m_dt.m_treeData.m_summaryTree.size();

            if (comboBox.getItems().size() != summarySize) {
                List<String> items = new ArrayList<>();
                for (int i = 0; i < summarySize; i++) {
                    items.add(String.valueOf(i + 1));
                }
                comboBox.getItems().setAll(items);
            }

            for (int i = 0; i < summarySize && i < comboBox.getItems().size(); i++) {
                if (m_dt.m_treeData.m_rootcanaltree == m_dt.m_treeData.m_summaryTree.get(i)) {
                    comboBox.getSelectionModel().select(i);
                }
            }
        }

        boolean hasRootCanal = (m_dt.m_treeData != null && m_dt.m_treeData.m_rootcanaltree != null);
        comboBox.setDisable(!hasRootCanal);
        checkBoxShowRotoCanal.setDisable(!hasRootCanal);
        btnImport.setDisable(!hasRootCanal);
        System.err.println("rootcanaltree = " + hasRootCanal);
    }

    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }
}