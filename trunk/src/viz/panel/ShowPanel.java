package viz.panel;

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
import javax.swing.JComboBox;

public class ShowPanel extends JPanel implements ChangeListener {
	private static final long serialVersionUID = 1L;
	
	DensiTree m_dt;
	JCheckBox chckbxShowEditTree = new JCheckBox("Edit Tree");
	JComboBox comboBox;
	
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
		gbc_checkBox_1.insets = new Insets(0, 0, 5, 5);
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
		gbc_checkBox.insets = new Insets(0, 0, 5, 5);
		gbc_checkBox.anchor = GridBagConstraints.NORTHWEST;
		gbc_checkBox.gridx = 0;
		gbc_checkBox.gridy = 1;
		add(checkBox, gbc_checkBox);

		JCheckBox checkBox_2 = new JCheckBox("Root Canal");
		checkBox_2.setSelected(m_dt.m_bShowRootCanalTopology);
		checkBox_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_bShowRootCanalTopology;
				m_dt.m_bShowRootCanalTopology = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_bShowRootCanalTopology) {
					m_dt.makeDirty();
				}
			}
		});
		GridBagConstraints gbc_checkBox_2 = new GridBagConstraints();
		gbc_checkBox_2.insets = new Insets(0, 0, 5, 5);
		gbc_checkBox_2.anchor = GridBagConstraints.NORTHWEST;
		gbc_checkBox_2.gridx = 0;
		gbc_checkBox_2.gridy = 2;
		add(checkBox_2, gbc_checkBox_2);

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
		
		comboBox = new JComboBox(new String[]{"1","2","3","4"});
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox comboBox = ((JComboBox)e.getSource());
				int i = comboBox.getSelectedIndex();
				if (m_dt.m_summaryTree != null) {
					m_dt.m_rootcanaltree = m_dt.m_summaryTree[i];
					m_dt.calcLines();
					m_dt.makeDirty();
				}
			}
		});
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.insets = new Insets(0, 0, 5, 0);
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 1;
		gbc_comboBox.gridy = 2;
		add(comboBox, gbc_comboBox);
		GridBagConstraints gbc_checkBox_3 = new GridBagConstraints();
		gbc_checkBox_3.insets = new Insets(0, 0, 5, 5);
		gbc_checkBox_3.anchor = GridBagConstraints.NORTHWEST;
		gbc_checkBox_3.gridx = 0;
		gbc_checkBox_3.gridy = 3;
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
		gbc_chckbxShowEditTree.anchor = GridBagConstraints.WEST;
		gbc_chckbxShowEditTree.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxShowEditTree.gridx = 0;
		gbc_chckbxShowEditTree.gridy = 4;
		add(chckbxShowEditTree, gbc_chckbxShowEditTree);
		
		stateChanged(null);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		chckbxShowEditTree.setEnabled(m_dt.m_Xmode == 0);
        for (int i = 0; i < m_dt.m_summaryTree.length; i++) {
            if (m_dt.m_rootcanaltree == m_dt.m_summaryTree[i]) {
                    comboBox.setSelectedIndex(i);
            }
        }
	}
}
