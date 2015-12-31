package viz.panel;


import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import viz.DensiTree;
import viz.Node;
import viz.TreeFileParser;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.swing.JTextField;
import javax.swing.JSeparator;
import javax.swing.JComboBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;

public class ImportRootCanalDialog extends JPanel {
	private static final long serialVersionUID = 1L;
	
	class STOption {
		public STOption(String sDisplay, String sOptions, boolean bHasExtraOptions) {
			m_sDisplay = sDisplay;
			m_sOptions = sOptions;
			m_bHasExtraOptions = bHasExtraOptions;
		}
		String m_sDisplay;
		String m_sOptions;
		boolean m_bHasExtraOptions;
		
		@Override
		public String toString() {
			return m_sDisplay;
		}
	}
	
	DensiTree m_dt;
	ButtonGroup group;
	JRadioButton b1 = new JRadioButton("Newick tree:");
	JRadioButton b2 = new JRadioButton("Use summary_tree");
	private JTextField txtNewick;
	private final JSeparator separator = new JSeparator();
	private final JComboBox<STOption> comboBox = new JComboBox<>();
	private final JLabel lblTopTrees = new JLabel("# top trees");
	private final JTextField textField = new JTextField();
	private final JLabel lblTimeLimitseconds = new JLabel("time limit (seconds)");
	private final JTextField textField_1 = new JTextField();

	public ImportRootCanalDialog(DensiTree dt) {
		textField_1.setText("-1");
		textField_1.setColumns(10);
		textField.setText("1");
		textField.setColumns(10);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 203, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 1.0};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		
		GridBagConstraints gbc_rdbtnNewRadioButton = new GridBagConstraints();
		gbc_rdbtnNewRadioButton.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRadioButton.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNewRadioButton.gridx = 0;
		gbc_rdbtnNewRadioButton.gridy = 0;
		b1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				update();
			}
		});
		add(b1, gbc_rdbtnNewRadioButton);
		b1.setSelected(true);
		
		txtNewick = new JTextField();
		txtNewick.setText("newick");
		GridBagConstraints gbc_txtNewick = new GridBagConstraints();
		gbc_txtNewick.insets = new Insets(0, 0, 5, 0);
		gbc_txtNewick.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtNewick.gridx = 1;
		gbc_txtNewick.gridy = 0;
		gbc_txtNewick.gridwidth = 2;
		add(txtNewick, gbc_txtNewick);
		txtNewick.setColumns(10);
		
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.gridwidth = 2;
		gbc_separator.insets = new Insets(0, 0, 5, 5);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 1;
		add(separator, gbc_separator);
		
		
		GridBagConstraints gbc_rdbtnNewRadioButton_1 = new GridBagConstraints();
		gbc_rdbtnNewRadioButton_1.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRadioButton_1.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNewRadioButton_1.gridx = 0;
		gbc_rdbtnNewRadioButton_1.gridy = 2;
		add(b2, gbc_rdbtnNewRadioButton_1);
		b2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				update();
			}
		});
		
		m_dt = dt;
		group = new ButtonGroup();
		group.add(b1);
		group.add(b2);
		
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.gridwidth = 2;
		gbc_comboBox.insets = new Insets(0, 0, 5, 0);
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 1;
		gbc_comboBox.gridy = 2;
		comboBox.addItem(new STOption("Taxon partitions", "--method taxon-partitions", false));
		comboBox.addItem(new STOption("Clade ca", "--method clade-ca", false));
		comboBox.addItem(new STOption("Min. distance by height score", "--method min-distance  --distance-method heights-score", true));
		comboBox.addItem(new STOption("Min. distance by heights only", "--method min-distance  --distance-method heights-only", true));
		comboBox.addItem(new STOption("Min. distance by branch score", "--method min-distance  --distance-method branch-score", true));
		
		comboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				update();
			}
		});
		add(comboBox, gbc_comboBox);
		
		GridBagConstraints gbc_lblTopTrees = new GridBagConstraints();
		gbc_lblTopTrees.insets = new Insets(0, 0, 5, 5);
		gbc_lblTopTrees.anchor = GridBagConstraints.WEST;
		gbc_lblTopTrees.gridx = 1;
		gbc_lblTopTrees.gridy = 3;
		add(lblTopTrees, gbc_lblTopTrees);
		
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 2;
		gbc_textField.gridy = 3;
		add(textField, gbc_textField);
		
		GridBagConstraints gbc_lblTimeLimitseconds = new GridBagConstraints();
		gbc_lblTimeLimitseconds.anchor = GridBagConstraints.WEST;
		gbc_lblTimeLimitseconds.insets = new Insets(0, 0, 0, 5);
		gbc_lblTimeLimitseconds.gridx = 1;
		gbc_lblTimeLimitseconds.gridy = 4;
		add(lblTimeLimitseconds, gbc_lblTimeLimitseconds);
		
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 2;
		gbc_textField_1.gridy = 4;
		add(textField_1, gbc_textField_1);

		update();
	}
		
	private void update() {
		if (b2.getModel() == group.getSelection()) {
			STOption option = (STOption) comboBox.getSelectedItem();
			textField_1.setEnabled(option.m_bHasExtraOptions);
			textField.setEnabled(option.m_bHasExtraOptions);
			lblTopTrees.setEnabled(option.m_bHasExtraOptions);
			lblTimeLimitseconds.setEnabled(option.m_bHasExtraOptions);
			comboBox.setEnabled(true);
			txtNewick.setEnabled(false);
		} else {
			textField_1.setEnabled(false);
			textField.setEnabled(false);
			lblTopTrees.setEnabled(false);
			lblTimeLimitseconds.setEnabled(false);			
			comboBox.setEnabled(false);
			txtNewick.setEnabled(true);
		}
	}

	/** returns true if a new root canal tree was successfully imported **/
	public boolean showDialog(JComponent parent) {

			JOptionPane optionPane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
					null, new String[] { "Cancel", "OK" }, "OK");
			optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

			final JDialog dialog = optionPane.createDialog(parent, "Import root canal tree");
			dialog.setName("ImportRootCanal");
			dialog.pack();
			dialog.setVisible(true);
			if (!optionPane.getValue().equals("OK")) {
				return false;
			}
			
			String newick = null;
			if (b1.getModel() == group.getSelection()) {
				newick = txtNewick.getText();
			}
			if (b2.getModel() == group.getSelection()) {
				 m_dt.setWaitCursor();
				 try {
				      String line;
				      double fBurnIn;
				      if (m_dt.m_bBurnInIsPercentage) {
				    	  fBurnIn = m_dt.m_nBurnIn;
				      } else {
				    	  fBurnIn = 100.0 * m_dt.m_nBurnIn / (m_dt.m_nBurnIn  + m_dt.m_trees.length);
				      }
				      STOption option = (STOption) comboBox.getSelectedItem();
				      String sCmd = "summary_tree --burnin " + fBurnIn + " " +  option.m_sOptions;
				      if (option.m_bHasExtraOptions) {
				    	  sCmd += " --ntops " + textField.getText() + " --limit " + textField_1.getText();
				      }
				      sCmd += " " + m_dt.m_sFileName;
				      System.err.println("Trying to execute: " + sCmd);
				      
				      
				      Process p = Runtime.getRuntime().exec(sCmd);
				      BufferedReader pout = new BufferedReader((new InputStreamReader(p.getInputStream())));
				      BufferedReader perr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				      while ((line = pout.readLine()) != null) {
				    	  newick = line; 
				      }
				      pout.close();
				      while ((line = perr.readLine()) != null) {
				        System.err.println(line);
				      }
				      perr.close();
				      p.waitFor();
				    }
				    catch (Exception err) {
				      err.printStackTrace();
				    }
				 	m_dt.setDefaultCursor();				
			}
			if (newick != null) {
				TreeFileParser parser = new TreeFileParser(m_dt.m_sLabels, null, null, 0);
				try {
					Node tree = parser.parseNewick(newick);
					tree.sort();

					System.err.println("labelInternalNodes");
					tree.labelInternalNodes(m_dt.m_sLabels.size());

					System.err.println("positionHeight");
					float fTreeHeight = m_dt.positionHeight(tree, 0);

					System.err.println("offsetHeight");
					m_dt.offsetHeight(tree, m_dt.m_fHeight - fTreeHeight);
					
					System.err.println("calcCladeIDForNode");
					m_dt.calcCladeIDForNode(tree, m_dt.mapCladeToIndex);

					System.err.println("resetCladeNr");
					m_dt.resetCladeNr(tree, m_dt.reverseindex);

					m_dt.m_summaryTree.add(tree);
					return true;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.err.println("ImportRootCanalDialog: " + e.getMessage());
				}
			}
			
			return false;
	}
}
