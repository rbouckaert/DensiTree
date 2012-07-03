package viz.panel;


import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.GridBagLayout;

import javax.swing.JCheckBox;

import viz.DensiTree;
import viz.DensiTree.LineColorMode;
import viz.DensiTree.MetaDataType;

import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import java.awt.Insets;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class ColorPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	DensiTree m_dt;
	JComboBox comboBox;
	private JTextField txtPattern;
	private JButton btnLineColors;
	private JCheckBox chckbxShowLegend;
	
	public ColorPanel(DensiTree dt) {
		m_dt = dt;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE, 0.0};
		setLayout(gridBagLayout);
		
		
		List<String> selection = new ArrayList<String>();
		selection.add(LineColorMode.DEFAULT.toString());
		selection.add(LineColorMode.COLOR_BY_CLADE.toString());
		selection.add(LineColorMode.BY_METADATA_PATTERN.toString());
		for (int i = 0; i < m_dt.m_metaDataTags.size(); i++) {
			if (!m_dt.m_metaDataTypes.get(i).equals(MetaDataType.SET)) {
				selection.add(m_dt.m_metaDataTags.get(i));				
			}
		}
		
		JCheckBox chckbxMultiColorConsensus = new JCheckBox("<html>Multi color cons. trees</html>");
		chckbxMultiColorConsensus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_bViewMultiColor = ((JCheckBox) e.getSource()).isSelected();
				m_dt.makeDirty();
			}
		});
		
		comboBox = new JComboBox(selection.toArray(new String[0]));
		comboBox.setSelectedItem(m_dt.m_lineColorMode);
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						String selected = comboBox.getSelectedItem().toString();
						LineColorMode oldMode = m_dt.m_lineColorMode; 
						String oldTag = m_dt.m_lineColorTag;
						if (selected.equals(LineColorMode.DEFAULT.toString())) {
							m_dt.m_lineColorMode = LineColorMode.DEFAULT;
						} else if (selected.equals(LineColorMode.BY_METADATA_PATTERN.toString())) {
							m_dt.m_lineColorMode = LineColorMode.BY_METADATA_PATTERN;
						} else if (selected.equals(LineColorMode.COLOR_BY_CLADE.toString())) {
							m_dt.m_lineColorMode = LineColorMode.COLOR_BY_CLADE;
						} else {
							m_dt.m_lineColorTag = selected; 
							m_dt.m_lineColorMode = LineColorMode.COLOR_BY_METADATA_TAG;
						}
						txtPattern.setEnabled(m_dt.m_lineColorMode == LineColorMode.BY_METADATA_PATTERN);
						chckbxShowLegend.setEnabled(m_dt.m_lineColorMode == LineColorMode.BY_METADATA_PATTERN 
								|| m_dt.m_lineColorMode == LineColorMode.COLOR_BY_METADATA_TAG);
						if (m_dt.m_lineColorMode != oldMode || m_dt.m_lineColorTag != oldTag) {
							m_dt.calcColors(false);
							m_dt.makeDirty();
						}
					}
				});
			}
		});
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.gridwidth = 2;
		gbc_comboBox.insets = new Insets(0, 0, 5, 5);
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 0;
		gbc_comboBox.gridy = 0;
		add(comboBox, gbc_comboBox);
		
		chckbxShowLegend = new JCheckBox("Show legend");
		chckbxShowLegend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_showLegend = !m_dt.m_showLegend;
				m_dt.makeDirty();
			}
		});
		chckbxShowLegend.setEnabled(false);
		GridBagConstraints gbc_chckbxShowLegend = new GridBagConstraints();
		gbc_chckbxShowLegend.anchor = GridBagConstraints.WEST;
		gbc_chckbxShowLegend.gridwidth = 2;
		gbc_chckbxShowLegend.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxShowLegend.gridx = 0;
		gbc_chckbxShowLegend.gridy = 1;
		add(chckbxShowLegend, gbc_chckbxShowLegend);
		GridBagConstraints gbc_chckbxMultiColorConsensus = new GridBagConstraints();
		gbc_chckbxMultiColorConsensus.anchor = GridBagConstraints.WEST;
		gbc_chckbxMultiColorConsensus.gridwidth = 3;
		gbc_chckbxMultiColorConsensus.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxMultiColorConsensus.gridx = 0;
		gbc_chckbxMultiColorConsensus.gridy = 2;
		add(chckbxMultiColorConsensus, gbc_chckbxMultiColorConsensus);
		JLabel lblPattern = new JLabel("pattern");
		GridBagConstraints gbc_lblPattern = new GridBagConstraints();
		gbc_lblPattern.anchor = GridBagConstraints.EAST;
		gbc_lblPattern.insets = new Insets(0, 0, 5, 5);
		gbc_lblPattern.gridx = 0;
		gbc_lblPattern.gridy = 3;
		add(lblPattern, gbc_lblPattern);
		
		txtPattern = new JTextField(m_dt.m_sLineColorPattern);
		txtPattern.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String oldPattern = m_dt.m_sLineColorPattern;
				m_dt.m_sLineColorPattern = txtPattern.getText();
				if (oldPattern.equals(m_dt.m_sLineColorPattern)) {
					m_dt.calcColors(false);
					m_dt.makeDirty();
				}
			}
		});
		GridBagConstraints gbc_txtPattern = new GridBagConstraints();
		gbc_txtPattern.insets = new Insets(0, 0, 5, 5);
		gbc_txtPattern.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPattern.gridx = 1;
		gbc_txtPattern.gridy = 3;
		add(txtPattern, gbc_txtPattern);
		txtPattern.setColumns(10);
		txtPattern.setEnabled(false);
		
		btnLineColors = new RoundedButton("Line colors");
		btnLineColors.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new ColorDialog(m_dt);
			}
		});
		GridBagConstraints gbc_btnLineColors = new GridBagConstraints();
		gbc_btnLineColors.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnLineColors.insets = new Insets(0, 0, 0, 5);
		gbc_btnLineColors.gridx = 1;
		gbc_btnLineColors.gridy = 4;
		add(btnLineColors, gbc_btnLineColors);
		
	}
}
