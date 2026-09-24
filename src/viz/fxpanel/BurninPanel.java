package viz.fxpanel;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import viz.DensiTree;

public class BurninPanel extends GridPane {

    public static final String HELP_BURNIN = "Specifies the set of trees at the beginning of the set that are removed " +
            "from the tree set. When the tree set represents a sample from an MCMC run, typically about 10% of the trees " +
            "are sampled while the chain is in burn-in, and are not representative for the tree distribution.\n" +
            "Focus and press enter to reload file with adjusted burn-in settings.";
    public static final String HELP_PERCENTAGE = "If selected, the burn-in is interpreted as a percentage, hence should be in " +
            "between 0 and 100. If the burn-in falls outside that range, burn-in is reset to 10.";
    public static final String HELP_NUMBER_OF_TREES = "If selected, the burn-in is interpreted as the number of trees at the " +
            "start of the set that should be removed. If burn-in is larger than the number of trees in the set, " +
            "burn-in is reset to 0.";

    private final TextField textField;
    private final ToggleGroup m_group = new ToggleGroup();
    private final DensiTree m_dt;

    public BurninPanel(DensiTree dt) {
        this.m_dt = dt;

        // Configure GridPane spacing and padding
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        // 1. Label
        Label lblBurnIn = new Label("Burn in");
        lblBurnIn.setTooltip(createTooltip(HELP_BURNIN));
        GridPane.setHalignment(lblBurnIn, HPos.RIGHT);
        add(lblBurnIn, 0, 0);

        // 2. TextField
        textField = new TextField(String.valueOf(m_dt.m_nBurnIn));
        textField.setPrefColumnCount(4);
        textField.setTooltip(createTooltip(HELP_BURNIN));
        GridPane.setHgrow(textField, Priority.ALWAYS);
        add(textField, 1, 0);

        // 3. Percentage RadioButton
        RadioButton rdbtnPercentage = new RadioButton("percentage");
        rdbtnPercentage.setSelected(true);
        rdbtnPercentage.setToggleGroup(m_group);
        rdbtnPercentage.setTooltip(createTooltip(HELP_PERCENTAGE));
        rdbtnPercentage.setOnAction(e -> {
            m_dt.m_bBurnInIsPercentage = true;
        });
        GridPane.setColumnSpan(rdbtnPercentage, 2);
        add(rdbtnPercentage, 0, 1);

        // 4. #trees RadioButton
        RadioButton rdbtnTrees = new RadioButton("#trees");
        rdbtnTrees.setToggleGroup(m_group);
        rdbtnTrees.setTooltip(createTooltip(HELP_NUMBER_OF_TREES));
        rdbtnTrees.setOnAction(e -> {
            m_dt.m_bBurnInIsPercentage = false;
        });
        GridPane.setColumnSpan(rdbtnTrees, 2);
        add(rdbtnTrees, 0, 2);

        // Action event for TextField (fires when Enter is pressed)
        textField.setOnAction(e -> {
            try {
                m_dt.m_nBurnIn = Integer.parseInt(textField.getText().trim());
                m_dt.init(m_dt.m_sFileName);
                m_dt.calcLines();
                m_dt.fitToScreen();
                // make sure the textfield is up to date
                textField.setText(String.valueOf(m_dt.m_nBurnIn));
            } catch (Exception ex) {
                // Ignore parsing/initialization errors
            }
        });
    }

    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }
}