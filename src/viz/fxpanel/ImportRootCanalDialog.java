package viz.fxpanel;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;

import viz.DensiTree;
import viz.TreeFileParser;

public class ImportRootCanalDialog extends GridPane {

    public static class STOption {
        String m_sDisplay;
        String m_sOptions;
        boolean m_bHasExtraOptions;

        public STOption(String sDisplay, String sOptions, boolean bHasExtraOptions) {
            this.m_sDisplay = sDisplay;
            this.m_sOptions = sOptions;
            this.m_bHasExtraOptions = bHasExtraOptions;
        }

        @Override
        public String toString() {
            return m_sDisplay;
        }
    }

    private final DensiTree m_dt;
    private final ToggleGroup group = new ToggleGroup();
    private final RadioButton b1 = new RadioButton("Newick tree:");
    private final RadioButton b2 = new RadioButton("Use summary_tree");
    private final TextField txtNewick = new TextField();
    private final Separator separator = new Separator();
    private final ComboBox<STOption> comboBox = new ComboBox<>();
    private final Label lblTopTrees = new Label("# top trees");
    private final TextField textField = new TextField();
    private final Label lblTimeLimitseconds = new Label("time limit (seconds)");
    private final TextField textField_1 = new TextField();

    public ImportRootCanalDialog(DensiTree dt) {
        this.m_dt = dt;

        // Configure GridPane spacing and padding
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(10));

        textField_1.setText("-1");
        textField_1.setPrefColumnCount(10);
        textField.setText("1");
        textField.setPrefColumnCount(10);

        // Group radio buttons
        b1.setToggleGroup(group);
        b2.setToggleGroup(group);
        b1.setSelected(true);

        b1.setOnAction(e -> update());
        b2.setOnAction(e -> update());

        // Row 0: Newick option
        GridPane.setHalignment(b1, HPos.LEFT);
        add(b1, 0, 0);

        txtNewick.setText("newick");
        txtNewick.setPrefColumnCount(10);
        GridPane.setColumnSpan(txtNewick, 2);
        GridPane.setHgrow(txtNewick, Priority.ALWAYS);
        add(txtNewick, 1, 0);

        // Row 1: Separator
        GridPane.setColumnSpan(separator, 3);
        GridPane.setHgrow(separator, Priority.ALWAYS);
        add(separator, 0, 1);

        // Row 2: summary_tree option & ComboBox
        GridPane.setHalignment(b2, HPos.LEFT);
        add(b2, 0, 2);

        comboBox.getItems().addAll(
                new STOption("Taxon partitions", "--method taxon-partitions", false),
                new STOption("Clade ca", "--method clade-ca", false),
                new STOption("Min. distance by height score", "--method min-distance  --distance-method heights-score", true),
                new STOption("Min. distance by heights only", "--method min-distance  --distance-method heights-only", true),
                new STOption("Min. distance by branch score", "--method min-distance  --distance-method branch-score", true)
        );
        comboBox.getSelectionModel().selectFirst();
        comboBox.setOnAction(e -> update());
        comboBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setColumnSpan(comboBox, 2);
        GridPane.setHgrow(comboBox, Priority.ALWAYS);
        add(comboBox, 1, 2);

        // Row 3: # Top Trees
        GridPane.setHalignment(lblTopTrees, HPos.LEFT);
        add(lblTopTrees, 1, 3);

        GridPane.setHgrow(textField, Priority.ALWAYS);
        add(textField, 2, 3);

        // Row 4: Time Limit
        GridPane.setHalignment(lblTimeLimitseconds, HPos.LEFT);
        add(lblTimeLimitseconds, 1, 4);

        GridPane.setHgrow(textField_1, Priority.ALWAYS);
        add(textField_1, 2, 4);

        update();
    }

    private void update() {
        if (b2.isSelected()) {
            STOption option = comboBox.getValue();
            boolean hasExtra = (option != null && option.m_bHasExtraOptions);
            textField_1.setDisable(!hasExtra);
            textField.setDisable(!hasExtra);
            lblTopTrees.setDisable(!hasExtra);
            lblTimeLimitseconds.setDisable(!hasExtra);
            comboBox.setDisable(false);
            txtNewick.setDisable(true);
        } else {
            textField_1.setDisable(true);
            textField.setDisable(true);
            lblTopTrees.setDisable(true);
            lblTimeLimitseconds.setDisable(true);
            comboBox.setDisable(true);
            txtNewick.setDisable(false);
        }
    }

    public boolean showDialog() {
        return showDialog(null);
    }

    /** Returns true if a new root canal tree was successfully imported **/
    public boolean showDialog(Object parent) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Import root canal tree");

        if (parent instanceof Window window) {
            dialog.initOwner(window);
        } else if (parent instanceof Node node && node.getScene() != null) {
            dialog.initOwner(node.getScene().getWindow());
        }

        dialog.getDialogPane().setContent(this);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return false;
        }

        String newick = null;
        if (b1.isSelected()) {
            newick = txtNewick.getText();
        } else if (b2.isSelected()) {
            m_dt.setWaitCursor();
            try {
                String line;
                double fBurnIn;
                if (m_dt.m_bBurnInIsPercentage) {
                    fBurnIn = m_dt.m_nBurnIn;
                } else {
                    fBurnIn = 100.0 * m_dt.m_nBurnIn / (m_dt.m_nBurnIn + m_dt.m_treeData.m_trees.length);
                }

                STOption option = comboBox.getValue();
                String sCmd = "summary_tree --burnin " + fBurnIn + " " + option.m_sOptions;
                if (option.m_bHasExtraOptions) {
                    sCmd += " --ntops " + textField.getText().trim() + " --limit " + textField_1.getText().trim();
                }
                sCmd += " " + m_dt.m_sFileName;
                System.err.println("Trying to execute: " + sCmd);

                Process p = Runtime.getRuntime().exec(sCmd);
                try (BufferedReader pout = new BufferedReader(new InputStreamReader(p.getInputStream()));
                     BufferedReader perr = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                    while ((line = pout.readLine()) != null) {
                        newick = line;
                    }
                    while ((line = perr.readLine()) != null) {
                        System.err.println(line);
                    }
                }
                p.waitFor();
            } catch (Exception err) {
                err.printStackTrace();
            } finally {
                m_dt.setDefaultCursor();
            }
        }

        if (newick != null && !newick.isBlank()) {
            TreeFileParser parser = new TreeFileParser(m_dt.m_settings.m_sLabels, null, null, 0);
            try {
                viz.Node tree = parser.parseNewick(newick);
                tree.sort();

                System.err.println("labelInternalNodes");
                tree.labelInternalNodes(m_dt.m_settings.m_sLabels.size());

                System.err.println("positionHeight");
                float fTreeHeight = m_dt.positionHeight(tree, 0);

                System.err.println("offsetHeight");
                m_dt.offsetHeight(tree, m_dt.m_fHeight - fTreeHeight);

                System.err.println("calcCladeIDForNode");
                m_dt.calcCladeIDForNode(tree, m_dt.m_treeData.mapCladeToIndex);

                System.err.println("resetCladeNr");
                m_dt.resetCladeNr(tree, m_dt.m_treeData.reverseindex);

                m_dt.m_treeData.m_summaryTree.add(tree);
                return true;
            } catch (Exception e) {
                System.err.println("ImportRootCanalDialog: " + e.getMessage());
            }
        }

        return false;
    }
}