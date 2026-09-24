package viz.fxpanel;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import viz.DensiTree;
import viz.Node;
import viz.ccd.AbstractCCD;
import viz.ccd.BitSet;
import viz.ccd.CCD1;
import viz.ccd.FilteredCCD;
import viz.ccd.RogueDetection;
import viz.ccd.Tree;

public class RoguePanel extends GridPane implements ChangeListener {

    public static final String HELP_COMBO_BOX = "After calculating rogues (via the 'calc rogues' button) select \n"
            + "the list is populated with rogues in decreasing order of impact on the entropy.\n"
            + "Select an item to remove all parts of the tree that include the selected rogue + all of the\n"
            + "rogues above.";
    public static final String HELP_CALC_ROGUES = "Calculate rogue taxa and populate combobox above.\n"
            + "This may take a while.\n"
            + "After that, remove unselected taxa from the tree set.";
    public static final String HELP_DROP_SET_SIZE = "Drop set size determines the maximum clade size to consider to be dropped.\n"
            + "Smaller is faster. Usually, small drop sets are sufficient.";

    private final ComboBox<String> comboBoxBottom;
    private final Button calcRoguesButton;
    private final TextField textField;
    private final DensiTree m_dt;

    public RoguePanel(DensiTree dt) {
        this.m_dt = dt;
        this.m_dt.addChangeListener(this);

        // Configure GridPane spacing and padding
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        // 1. Rogue Selection ComboBox (Row 0)
        comboBoxBottom = new ComboBox<>();
        comboBoxBottom.getItems().setAll("<not initialised>");
        comboBoxBottom.getSelectionModel().selectFirst();
        comboBoxBottom.setMaxWidth(Double.MAX_VALUE);
        comboBoxBottom.setTooltip(createTooltip(HELP_COMBO_BOX));
        comboBoxBottom.setOnAction(e -> {
            Platform.runLater(this::unselectRogues);
        });
        GridPane.setColumnSpan(comboBoxBottom, 2);
        GridPane.setHgrow(comboBoxBottom, Priority.ALWAYS);
        add(comboBoxBottom, 0, 0);

        // 2. Calc / Remove Rogues Button (Row 1)
        calcRoguesButton = new Button("Calc rogues");
        calcRoguesButton.setMaxWidth(Double.MAX_VALUE);
        calcRoguesButton.setDisable(true);
        calcRoguesButton.setTooltip(createTooltip(HELP_CALC_ROGUES));
        calcRoguesButton.setOnAction(e -> {
            if (calcRoguesButton.getText().equals("Calc rogues")) {
                calcRoguesButton.setText("Processing");
            }
            Platform.runLater(this::processRogues);
        });
        GridPane.setColumnSpan(calcRoguesButton, 2);
        GridPane.setHgrow(calcRoguesButton, Priority.ALWAYS);
        add(calcRoguesButton, 0, 1);

        // 3. Drop Set Size Label & TextField (Row 2)
        Label lblBurnIn = new Label("Drop set size");
        lblBurnIn.setTooltip(createTooltip(HELP_DROP_SET_SIZE));
        GridPane.setHalignment(lblBurnIn, HPos.RIGHT);
        add(lblBurnIn, 0, 2);

        textField = new TextField("1");
        textField.setPrefColumnCount(4);
        textField.setTooltip(createTooltip(HELP_DROP_SET_SIZE));
        GridPane.setHgrow(textField, Priority.ALWAYS);
        add(textField, 1, 2);
    }

    private void unselectRogues() {
        int index = comboBoxBottom.getSelectionModel().getSelectedIndex();
        if (index < 0 || m_dt.m_treeData == null || m_dt.m_treeData.m_bSelection == null) {
            return;
        }

        boolean[] selection = m_dt.m_treeData.m_bSelection;
        Arrays.fill(selection, true);

        for (int i = 1; i <= index; i++) {
            String label = comboBoxBottom.getItems().get(i);
            String taxon = label.split(" : ")[0];
            int j = 0;
            while (j < m_dt.m_settings.m_sLabels.size() && !m_dt.m_settings.m_sLabels.get(j).equals(taxon)) {
                j++;
            }
            if (j < selection.length) {
                selection[j] = false;
            }
        }

        m_dt.calcLines();
        m_dt.makeDirty();
    }

    private void processRogues() {
        if (calcRoguesButton.getText().equals("Calc rogues") || calcRoguesButton.getText().equals("Processing")) {
            calcRogues();
        } else if (calcRoguesButton.getText().equals("Remove rogues")) {
            removeRogues();
        }
    }

    private Object removeRogues() {
        try {
            boolean[] taxaToInclude = m_dt.m_treeData.m_bSelection;
            try (PrintStream out = new PrintStream("tmp.clipboard")) {
                for (Node root : m_dt.m_treeData.m_trees) {
                    out.println(toNewick(root, taxaToInclude, new double[]{0}) + ";");
                }
            }
            m_dt.init("tmp.clipboard");
            m_dt.calcLines();
            m_dt.fitToScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String toNewick(Node node, boolean[] taxaToInclude, double[] len) {
        final StringBuilder buf = new StringBuilder();
        if (node.m_left != null) {
            double[] leftLen = new double[]{0};
            double[] rightLen = new double[]{0};
            String leftStr = toNewick(node.m_left, taxaToInclude, leftLen);
            String rightStr = toNewick(node.m_right, taxaToInclude, rightLen);
            if (leftStr != null && rightStr != null) {
                buf.append("(");
                buf.append(leftStr);
                buf.append(":").append(leftLen[0]);
                buf.append(",");
                buf.append(rightStr);
                buf.append(":").append(rightLen[0]);
                buf.append(")");
                len[0] = node.m_fLength;
            } else if (leftStr != null) {
                len[0] = leftLen[0] + node.m_fLength;
                return leftStr;
            } else if (rightStr != null) {
                len[0] = rightLen[0] + node.m_fLength;
                return rightStr;
            } else {
                return null;
            }
        } else {
            if (taxaToInclude[node.getNr()]) {
                buf.append(m_dt.m_settings.m_sLabels.get(node.getNr()));
                len[0] = node.m_fLength;
                if (node.getMetaData() != null) {
                    buf.append("[&").append(node.getMetaData()).append(']');
                }
            } else {
                return null;
            }
        }

        return buf.toString();
    }

    private Object calcRogues() {
        List<Tree> trees = new ArrayList<>();
        for (Node tree : m_dt.m_treeData.m_trees) {
            trees.add(new Tree(tree, m_dt.m_treeData));
        }
        CCD1 ccd = new CCD1(trees, 0);

        int dropSetSize = 1;
        try {
            dropSetSize = Integer.parseInt(textField.getText().trim());
            if (dropSetSize < 1) {
                dropSetSize = 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        RogueDetection.TerminationStrategy tStrategy = RogueDetection.TerminationStrategy.NumRogues;
        tStrategy.setThreshold(m_dt.m_settings.m_sLabels.size());
        List<AbstractCCD> ccds = RogueDetection.detectRoguesWhileImproving(
                ccd,
                dropSetSize,
                RogueDetection.RogueDetectionStrategy.Entropy,
                tStrategy
        );

        List<String> rogues = new ArrayList<>();
        rogues.add("<none>");
        boolean[] done = new boolean[ccd.getSomeBaseTree().getNodeCount()];
        int i = ccds.size() - 1;
        DecimalFormat f = new DecimalFormat("#.##");
        while (i > 0) {
            AbstractCCD ccdi = ccds.get(i);
            if (ccdi instanceof FilteredCCD) {
                BitSet mask = ((FilteredCCD) ccdi).getRemovedTaxaMask();
                for (int j = mask.nextSetBit(0); j >= 0; j = mask.nextSetBit(j + 1)) {
                    if (!done[j]) {
                        String label = ccdi.getSomeBaseTree().getID(j);
                        label += " : " + f.format(ccdi.getEntropy());
                        rogues.add(1, label);
                        done[j] = true;
                    }
                }
            }
            i--;
        }

        comboBoxBottom.getItems().setAll(rogues);
        comboBoxBottom.getSelectionModel().selectFirst();
        calcRoguesButton.setText("Remove rogues");
        return null;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (m_dt.m_treeData == null || !m_dt.m_treeData.m_bMetaDataReady) {
            calcRoguesButton.setText("Calc rogues");
            calcRoguesButton.setDisable(true);
            comboBoxBottom.getItems().setAll("<not initialised>");
            comboBoxBottom.getSelectionModel().selectFirst();
        } else {
            calcRoguesButton.setDisable(false);
        }
    }

    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }
}