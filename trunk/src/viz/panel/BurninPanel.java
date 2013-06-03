package viz.panel;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import javax.swing.JTextField;

import viz.DensiTree;
import viz.util.Util;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JRadioButton;

public class BurninPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	public static String HELP_BURNIN = "Specifies the set of trees at the beginning of the set that are removed " +
			"from the tree set. When the tree set represents a sample from an MCMC run, typically about 10% of the trees " +
			"are sampled while the chain is in burn-in, and are not representative for the tree distribution.\n" +
			"Focus and press enter to reload file with adjusted burn-in settings.";
	public static String HELP_PERCENTAGE = "If selected, the burn-in is interpreted as a percentage, hence should be in " +
			"between 0 and 100. If the burn-in falls outside that range, burn-in is reset to 10.";
	public static String HELP_NUMBER_OF_TREES = "If selected, the burn-in is interpreted as the number of trees at the " +
			"start of the set that should be removed. If burn-in is larger than the number of trees in the set, " +
			"burn-in is reset to 0.";
	
	private JTextField textField;
	ButtonGroup m_group = new ButtonGroup();
	DensiTree m_dt;

	public BurninPanel(DensiTree dt) {
		m_dt = dt;
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JLabel lblBurnIn = new JLabel("Burn in");
		lblBurnIn.setToolTipText(Util.formatToolTipAsHtml(HELP_BURNIN));
		GridBagConstraints gbc_lblBurnIn = new GridBagConstraints();
		gbc_lblBurnIn.insets = new Insets(0, 0, 5, 5);
		gbc_lblBurnIn.anchor = GridBagConstraints.EAST;
		gbc_lblBurnIn.gridx = 0;
		gbc_lblBurnIn.gridy = 0;
		add(lblBurnIn, gbc_lblBurnIn);
		
		textField = new JTextField(m_dt.m_nBurnIn + "");
		textField.setToolTipText(Util.formatToolTipAsHtml(HELP_BURNIN));
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		add(textField, gbc_textField);
		textField.setColumns(4);
		
		JRadioButton rdbtnPercentage = new JRadioButton("percentage");
		rdbtnPercentage.setSelected(true);
		rdbtnPercentage.setToolTipText(Util.formatToolTipAsHtml(HELP_PERCENTAGE));
		rdbtnPercentage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_bBurnInIsPercentage = true;
			}
		});
		m_group.add(rdbtnPercentage);
		GridBagConstraints gbc_rdbtnPercentage = new GridBagConstraints();
		gbc_rdbtnPercentage.gridwidth = 2;
		gbc_rdbtnPercentage.anchor = GridBagConstraints.WEST;
		gbc_rdbtnPercentage.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnPercentage.gridx = 0;
		gbc_rdbtnPercentage.gridy = 1;
		add(rdbtnPercentage, gbc_rdbtnPercentage);
		
		JRadioButton rdbtnTrees = new JRadioButton("#trees");
		rdbtnTrees.setToolTipText(Util.formatToolTipAsHtml(HELP_NUMBER_OF_TREES));
		rdbtnTrees.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_bBurnInIsPercentage = false;
			}
		});
		m_group.add(rdbtnTrees);
		GridBagConstraints gbc_rdbtntrees = new GridBagConstraints();
		gbc_rdbtntrees.gridwidth = 2;
		gbc_rdbtntrees.anchor = GridBagConstraints.WEST;
		gbc_rdbtntrees.gridx = 0;
		gbc_rdbtntrees.gridy = 2;
		add(rdbtnTrees, gbc_rdbtntrees);
		textField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					m_dt.m_nBurnIn = Integer.parseInt(textField.getText());
					m_dt.init(m_dt.m_sFileName);
					m_dt.calcLines();
					m_dt.fitToScreen();
					// make sure the textfield is up to date
					textField.setText(m_dt.m_nBurnIn + "");
				} catch (Exception e2) {}
			}
		});

	}
}
