package viz.panel;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import viz.ccd.BitSet;
import viz.ccd.FilteredCCD;
import viz.ccd.RogueDetection;
import viz.ccd.AbstractCCD;
import viz.DensiTree;
import viz.Node;
import viz.ccd.CCD1;
import viz.ccd.Tree;
import viz.util.Util;

public class RoguePanel extends JPanel implements ChangeListener {
	final static String HELP_COMBO_BOX = "After calculating rogues (via the 'calc rogues' button) select \n"
			+ "the list is populated with rogues in decreasing order of impact on the entropy.\n"
			+ "Select an item to remove all parts of the tree that include the selected rogue + all of the\n"
			+ "rogues above.";
	final static String HELP_CALC_ROGUES = "Calculate rogue taxa and populate combobox above.\n"
			+ "This may take a while.\n" 
			+ "After that, remove unselected taxa from the tree set.";
	final static String HELP_DROP_SET_SIZE = "Drop set size determines the maximum clade size to consider to be dropped.\n"
			+ "Smaller is faster. Usually, small drop sets are sufficient.";
	
	private static final long serialVersionUID = 1L;
	JComboBox<String> comboBoxBottom;
	JButton calcRoguesButton;
	JTextField textField;
	
	DensiTree m_dt;
	
	public RoguePanel(DensiTree dt) {
		m_dt = dt;
		m_dt.addChangeListener(this);

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0};
		gridBagLayout.columnWeights = new double[]{1.0, 0.0};
		setLayout(gridBagLayout);

		comboBoxBottom = new JComboBox<>();
		ComboBoxModel<String> model = new DefaultComboBoxModel<>(new String[]{"<not initialised>"});
		comboBoxBottom.setModel(model);
		comboBoxBottom.setPreferredSize(new Dimension(130,20));
		//comboBoxBottom.setMaximumSize(new Dimension(130,200));
		comboBoxBottom.setSelectedItem(0);
		comboBoxBottom.addActionListener(e-> {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					unselectRogues();
				}
			});
		});
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.gridwidth = 2;
		gbc_comboBox.insets = new Insets(0, 0, 5, 0);
		//gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 0;
		gbc_comboBox.gridy = 0;
		add(comboBoxBottom, gbc_comboBox);
		comboBoxBottom.setToolTipText(Util.formatToolTipAsHtml(HELP_COMBO_BOX));

		calcRoguesButton = new JButton("Calc rogues");
		GridBagConstraints gbc_calcRoguesButton = new GridBagConstraints();
		gbc_calcRoguesButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_calcRoguesButton.insets = new Insets(0, 0, 5, 0);
		gbc_calcRoguesButton.gridx = 0;
		gbc_calcRoguesButton.gridy = 1;
		gbc_calcRoguesButton.gridwidth= 2;
		add(calcRoguesButton, gbc_calcRoguesButton);
		calcRoguesButton.addActionListener(e-> {
					if (calcRoguesButton.getText().equals("Calc rogues")) {
						calcRoguesButton.setText("Processing");
					}
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							processRogues();
						}
					});
				});
		calcRoguesButton.setEnabled(false);
		calcRoguesButton.setToolTipText(Util.formatToolTipAsHtml(HELP_CALC_ROGUES));
		
		JLabel lblBurnIn = new JLabel("Drop set size");
		lblBurnIn.setToolTipText(Util.formatToolTipAsHtml(HELP_DROP_SET_SIZE));
		GridBagConstraints gbc_lblBurnIn = new GridBagConstraints();
		gbc_lblBurnIn.insets = new Insets(0, 0, 5, 5);
		gbc_lblBurnIn.anchor = GridBagConstraints.EAST;
		gbc_lblBurnIn.gridx = 0;
		gbc_lblBurnIn.gridy = 2;
		add(lblBurnIn, gbc_lblBurnIn);
		
		textField = new JTextField("1");
		textField.setToolTipText(Util.formatToolTipAsHtml(HELP_DROP_SET_SIZE));
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 2;
		add(textField, gbc_textField);
		textField.setColumns(4);
		
//		removeRoguesButton = new JButton("Remove rogues");
//		GridBagConstraints gbc_removeRoguesButton = new GridBagConstraints();
//		gbc_removeRoguesButton.fill = GridBagConstraints.HORIZONTAL;
//		gbc_removeRoguesButton.insets = new Insets(0, 0, 5, 0);
//		gbc_removeRoguesButton.gridx = 0;
//		gbc_removeRoguesButton.gridy = 2;
//		gbc_removeRoguesButton.gridwidth= 2;
//		add(removeRoguesButton, gbc_removeRoguesButton);
//		removeRoguesButton.addActionListener(e-> {
//			SwingUtilities.invokeLater(new Runnable() {
//				@Override
//				public void run() {
//					removeRogues();
//				}
//			});
//		});
//		removeRoguesButton.setEnabled(false);
//		removeRoguesButton.setToolTipText(Util.formatToolTipAsHtml(HELP_REMOVE_ROGUES));
	}

	
	private void unselectRogues() {
		ComboBoxModel<String> model = comboBoxBottom.getModel();
		int index = comboBoxBottom.getSelectedIndex();
		boolean[] selection = m_dt.m_treeData.m_bSelection;
		Arrays.fill(selection, true);
		for (int i = 1; i <= index; i++) {
			String taxon = model.getElementAt(i);
			int j = 0;
			while (j < m_dt.m_settings.m_sLabels.size() && !m_dt.m_settings.m_sLabels.get(j).equals(taxon)) {
				j++;
			}
			selection[j] = false;
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
			boolean [] taxaToInclude = m_dt.m_treeData.m_bSelection;
			PrintStream out = new PrintStream("tmp.clipboard");
			for (Node root : m_dt.m_treeData.m_trees) {
				out.println(toNewick(root, taxaToInclude, new double[]{0}) + ";");
			}
			out.close();
			m_dt.init("tmp.clipboard");
			m_dt.calcLines();
			m_dt.fitToScreen();
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	
	public String toNewick(Node node, boolean[] taxaToInclude, double[] len) {
		final StringBuilder buf = new StringBuilder();
		if (node.m_left != null) {
			double[] leftLen = new double[] { 0 };
			double[] rightLen = new double[] { 0 };
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
			} else if (leftStr != null) { // rightStr == null
				len[0] = leftLen[0] + node.m_fLength;
				return leftStr;
			} else if (rightStr != null) { // leftStr == null
				len[0] = rightLen[0] + node.m_fLength;
				return rightStr;
			} else { // leftStr =rightStr == null
				return null;
			}
		} else {
			if (taxaToInclude[node.getNr()]) {
				buf.append(m_dt.m_settings.m_sLabels.get(node.getNr()));
				len[0] = node.m_fLength;
				if (node.getMetaData() != null) {
					buf.append("[&" + node.getMetaData() + ']');
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
			dropSetSize = Integer.parseInt(textField.getText());
			if (dropSetSize < 1) {
				dropSetSize = 1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        List<viz.ccd.AbstractCCD> ccds = RogueDetection.detectRoguesWhileImproving(
                ccd,
                dropSetSize,
                RogueDetection.RogueDetectionStrategy.Entropy,
                RogueDetection.TerminationStrategy.NumRogues
                );
        
		List<String> rogues = new ArrayList<>();
		rogues.add("<none>");
		boolean [] done = new boolean[ccd.getSomeBaseTree().getNodeCount()];
    	for (AbstractCCD ccdi : ccds) {
    		if (ccdi instanceof FilteredCCD) {
        		BitSet mask = ((FilteredCCD) ccdi).getRemovedTaxaMask();
                for (int j = mask.nextSetBit(0); j >= 0; j = mask.nextSetBit(j + 1)) {
                	if (!done[j]) {
                		String taxon = ccdi.getSomeBaseTree().getID(j);
                		rogues.add(taxon);
                		done[j] = true;
                	}
                }
    		}
    	}
		ComboBoxModel<String> model = new DefaultComboBoxModel<>(rogues.toArray(new String[]{}));
		comboBoxBottom.setModel(model);

		calcRoguesButton.setText("Remove rogues");			
		return null;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// deal with m_dt state changes
		if (!m_dt.m_treeData.m_bMetaDataReady) {
			calcRoguesButton.setText("Calc rogues");
			calcRoguesButton.setEnabled(false);
			ComboBoxModel<String> model = new DefaultComboBoxModel<>(new String[]{"<not initialised>"});
			comboBoxBottom.setModel(model);
		} else {
			calcRoguesButton.setEnabled(true);
		}
	}
	
}
