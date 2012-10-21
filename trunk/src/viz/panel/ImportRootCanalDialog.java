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

public class ImportRootCanalDialog extends JPanel {
	private static final long serialVersionUID = 1L;
	
	DensiTree m_dt;
	ButtonGroup group;
	JRadioButton b1 = new JRadioButton("Newick tree:");
	JRadioButton b2 = new JRadioButton("Use summary_tree");
	private JTextField txtNewick;

	public ImportRootCanalDialog(DensiTree dt) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		
		GridBagConstraints gbc_rdbtnNewRadioButton = new GridBagConstraints();
		gbc_rdbtnNewRadioButton.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRadioButton.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNewRadioButton.gridx = 0;
		gbc_rdbtnNewRadioButton.gridy = 0;
		add(b1, gbc_rdbtnNewRadioButton);
		b1.setSelected(true);
		
		txtNewick = new JTextField();
		txtNewick.setText("newick");
		GridBagConstraints gbc_txtNewick = new GridBagConstraints();
		gbc_txtNewick.insets = new Insets(0, 0, 5, 0);
		gbc_txtNewick.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtNewick.gridx = 1;
		gbc_txtNewick.gridy = 0;
		add(txtNewick, gbc_txtNewick);
		txtNewick.setColumns(10);
		
		
		GridBagConstraints gbc_rdbtnNewRadioButton_1 = new GridBagConstraints();
		gbc_rdbtnNewRadioButton_1.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRadioButton_1.insets = new Insets(0, 0, 0, 5);
		gbc_rdbtnNewRadioButton_1.gridx = 0;
		gbc_rdbtnNewRadioButton_1.gridy = 1;
		add(b2, gbc_rdbtnNewRadioButton_1);
		
		m_dt = dt;
		group = new ButtonGroup();
		group.add(b1);
		group.add(b2);

	
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
				 try {
				      String line;
				      Process p = Runtime.getRuntime().exec("summary_tree " + m_dt.m_sFileName);
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
				
			}
			if (newick != null) {
				TreeFileParser parser = new TreeFileParser(m_dt.m_sLabels, null, null, 0);
				try {
					Node tree = parser.parseNewick(newick);
					tree.sort();
					tree.labelInternalNodes(m_dt.m_sLabels.size());
					float fTreeHeight = m_dt.positionHeight(tree, 0);
					m_dt.offsetHeight(tree, m_dt.m_fHeight - fTreeHeight);
					m_dt.calcCladeIDForNode(tree, m_dt.mapCladeToIndex);
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
