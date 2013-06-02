package viz.panel;


import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import viz.DensiTree;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JSeparator;

public class ShowPanel extends JPanel implements ChangeListener {
	final public static String HELP_CONSENSUS_TREES = "Display consensus trees. There is one consensus tree for every topology " +
			"in the tree set. The height of the nodes are the average of the heights for that topology.";
	final public static String HELP_ALL_TREES = "Show all trees in the tree set.";
	final public static String HELP_ROOT_CANAL = "Show root canal tree. This is a single summary tree representing the complete tree set. " +
			"There are many ways to construct a summary tree.";
	final public static String HELP_ROOT_CANAL_NUMBER = "Select root canal tree to display.";
	final public static String HELP_IMPORT = "Import root canal tree from Newick or from the summary_tree program.";
	final public static String HELP_ROOT_AT_TOP_ = "Display the root at the top of the display instead of on the left hand side.";
	final public static String HELP_EDIT_TREE = "Display edit tree for manipulating order of tree and position of internal nodes. " +
			"Works only with default drawing style.";

	private static final long serialVersionUID = 1L;
	
	DensiTree m_dt;
	JCheckBox chckbxShowEditTree = new JCheckBox("Edit Tree");
	JComboBox comboBox = new JComboBox();
	JCheckBox checkBoxShowRotoCanal;
	RoundedButton btnImport;
	
	public ShowPanel(DensiTree dt) {
		m_dt = dt;
		m_dt.addChangeListener(this);
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[]{0.0, 1.0};
//		gridBagLayout.columnWidths = new int[]{93, 129, 129, 0};
//		gridBagLayout.rowHeights = new int[]{23, 0};
//		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
//		gridBagLayout.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JCheckBox checkBox_1 = new JCheckBox("Consenus Trees");
		checkBox_1.setSelected(m_dt.m_bViewCTrees);
		checkBox_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_bViewCTrees;
				m_dt.m_bViewCTrees = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_bViewCTrees) {
					m_dt.makeDirty();
				}
			}
		});
		GridBagConstraints gbc_checkBox_1 = new GridBagConstraints();
		gbc_checkBox_1.gridwidth = 2;
		gbc_checkBox_1.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox_1.anchor = GridBagConstraints.NORTHWEST;
		gbc_checkBox_1.gridx = 0;
		gbc_checkBox_1.gridy = 0;
		add(checkBox_1, gbc_checkBox_1);
		
		JCheckBox checkBox = new JCheckBox("All Trees");
		checkBox.setSelected(m_dt.m_bViewAllTrees);
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_bViewAllTrees;
				m_dt.m_bViewAllTrees = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_bViewAllTrees) {
					m_dt.makeDirty();
				}
			}
		});
		GridBagConstraints gbc_checkBox = new GridBagConstraints();
		gbc_checkBox.gridwidth = 2;
		gbc_checkBox.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox.anchor = GridBagConstraints.NORTHWEST;
		gbc_checkBox.gridx = 0;
		gbc_checkBox.gridy = 1;
		add(checkBox, gbc_checkBox);

		checkBoxShowRotoCanal = new JCheckBox("Root Canal");
		checkBoxShowRotoCanal.setSelected(m_dt.m_bShowRootCanalTopology);
		checkBoxShowRotoCanal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_bShowRootCanalTopology;
				m_dt.m_bShowRootCanalTopology = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_bShowRootCanalTopology) {
					m_dt.makeDirty();
				}
			}
		});
		
		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.gridwidth = 2;
		gbc_separator.insets = new Insets(0, 0, 5, 0);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 2;
		add(separator, gbc_separator);
		GridBagConstraints gbc_checkBox_2 = new GridBagConstraints();
		gbc_checkBox_2.gridwidth = 2;
		gbc_checkBox_2.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox_2.anchor = GridBagConstraints.NORTHWEST;
		gbc_checkBox_2.gridx = 0;
		gbc_checkBox_2.gridy = 3;
		add(checkBoxShowRotoCanal, gbc_checkBox_2);

		JCheckBox checkBox_3 = new JCheckBox("Root At Top");
		checkBox_3.setSelected(m_dt.m_treeDrawer.m_bRootAtTop);
		checkBox_3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_treeDrawer.m_bRootAtTop;
				m_dt.m_treeDrawer.m_bRootAtTop = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_treeDrawer.m_bRootAtTop) {
					m_dt.fitToScreen();
				}
			}
		});
		
		btnImport = new RoundedButton("import");
		btnImport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ImportRootCanalDialog dlg = new ImportRootCanalDialog(m_dt);
				if (dlg.showDialog(null)) {
					DefaultComboBoxModel model = (DefaultComboBoxModel) comboBox.getModel();
					model.addElement((comboBox.getItemCount() + 1) + "");
					// setting last added item, this should trigger an ActionEvent handled below
					comboBox.setSelectedIndex(comboBox.getItemCount() - 1);
					m_dt.calcPositions();
					m_dt.calcLines();
					m_dt.makeDirty();
				}
			}
		});
		
		
		List<String> labels = new ArrayList<String>();
		for (int i = 0; i < m_dt.m_summaryTree.size(); i++) {
			labels.add("" + (i+1));
		}		
		comboBox = new JComboBox(labels.toArray());
		comboBox.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				int i = comboBox.getSelectedIndex();
				if (m_dt.m_summaryTree != null) {
					m_dt.m_rootcanaltree = m_dt.m_summaryTree.get(i);					
					m_dt.calcLines();
					m_dt.makeDirty();
				}
			}
		});
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.insets = new Insets(0, 0, 5, 5);
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 0;
		gbc_comboBox.gridy = 4;
		add(comboBox, gbc_comboBox);
		GridBagConstraints gbc_btnImport = new GridBagConstraints();
		gbc_btnImport.insets = new Insets(0, 0, 5, 0);
		gbc_btnImport.gridx = 1;
		gbc_btnImport.gridy = 4;
		add(btnImport, gbc_btnImport);
		
		JSeparator separator_1 = new JSeparator();
		GridBagConstraints gbc_separator_1 = new GridBagConstraints();
		gbc_separator_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_1.gridwidth = 2;
		gbc_separator_1.insets = new Insets(0, 0, 5, 5);
		gbc_separator_1.gridx = 0;
		gbc_separator_1.gridy = 5;
		add(separator_1, gbc_separator_1);
		GridBagConstraints gbc_checkBox_3 = new GridBagConstraints();
		gbc_checkBox_3.gridwidth = 2;
		gbc_checkBox_3.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox_3.anchor = GridBagConstraints.NORTHWEST;
		gbc_checkBox_3.gridx = 0;
		gbc_checkBox_3.gridy = 6;
		add(checkBox_3, gbc_checkBox_3);
		
		chckbxShowEditTree.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_bViewEditTree;
				m_dt.m_bViewEditTree = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_bViewEditTree) {
					m_dt.makeDirty();
				}
			}
		});
		GridBagConstraints gbc_chckbxShowEditTree = new GridBagConstraints();
		gbc_chckbxShowEditTree.gridwidth = 2;
		gbc_chckbxShowEditTree.anchor = GridBagConstraints.WEST;
		gbc_chckbxShowEditTree.gridx = 0;
		gbc_chckbxShowEditTree.gridy = 7;
		add(chckbxShowEditTree, gbc_chckbxShowEditTree);
		
		stateChanged(null);
		
		checkBox_1.setToolTipText(DensiTree.formatToolTip(HELP_CONSENSUS_TREES));
		checkBox.setToolTipText(DensiTree.formatToolTip(HELP_ALL_TREES));
		checkBox_3.setToolTipText(DensiTree.formatToolTip(HELP_ROOT_AT_TOP_));
		chckbxShowEditTree.setToolTipText(DensiTree.formatToolTip(HELP_EDIT_TREE));
		comboBox.setToolTipText(DensiTree.formatToolTip(HELP_ROOT_CANAL_NUMBER));
		checkBoxShowRotoCanal.setToolTipText(DensiTree.formatToolTip(HELP_ROOT_CANAL));
		btnImport.setToolTipText(DensiTree.formatToolTip(HELP_IMPORT));

	}

	@Override
	public void stateChanged(ChangeEvent e) {
		chckbxShowEditTree.setEnabled(m_dt.m_Xmode == 0);
		if (m_dt.m_summaryTree != null && m_dt.m_summaryTree.size() > 0) {
	        for (int i = 0; i < m_dt.m_summaryTree.size() && i < comboBox.getItemCount(); i++) {
	            if (m_dt.m_rootcanaltree == m_dt.m_summaryTree.get(i)) {
	                    comboBox.setSelectedIndex(i);
	            }
	        }
	        if (comboBox.getItemCount() != m_dt.m_summaryTree.size()) {
	        	DefaultComboBoxModel model = (DefaultComboBoxModel) comboBox.getModel();
				model.removeAllElements();
				for (int i = 0; i < m_dt.m_summaryTree.size(); i++) {
					model.addElement("" + (i+1));
				}
	        }
		}
		comboBox.setEnabled(m_dt.m_rootcanaltree != null);
		checkBoxShowRotoCanal.setEnabled(m_dt.m_rootcanaltree != null);
		btnImport.setEnabled(m_dt.m_rootcanaltree != null);
		System.err.println("rootcanaltree = " + (m_dt.m_rootcanaltree != null));
	}
}
