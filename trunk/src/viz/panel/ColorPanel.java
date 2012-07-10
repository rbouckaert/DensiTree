package viz.panel;


import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

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

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ColorPanel extends JPanel implements ChangeListener {
	
	private static final long serialVersionUID = 1L;
	
	DensiTree m_dt;
	JComboBox comboBox;
	private JTextField txtPattern;
	private JButton btnLineColors;
	private JCheckBox chckbxShowLegend;
	
	public ColorPanel(DensiTree dt) {
		m_dt = dt;
		m_dt.addChangeListener(this);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE, 0.0};
		setLayout(gridBagLayout);

		
		
		comboBox = new JComboBox(); 
		stateChanged(null);
		comboBox.setSelectedItem(m_dt.m_lineColorMode);
		comboBox.setPreferredSize(new Dimension(130,20));
		comboBox.setMaximumSize(new Dimension(130,200));
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
		//gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
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
		
		
		
		JCheckBox chckbxMultiColorConsensus = new JCheckBox("<html>Multi color<br>cons-trees</html>");
		chckbxMultiColorConsensus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_bViewMultiColor = ((JCheckBox) e.getSource()).isSelected();
				m_dt.makeDirty();
			}
		});
		GridBagConstraints gbc_chckbxMultiColorConsensus = new GridBagConstraints();
		gbc_chckbxMultiColorConsensus.anchor = GridBagConstraints.WEST;
		gbc_chckbxMultiColorConsensus.gridwidth = 3;
		gbc_chckbxMultiColorConsensus.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxMultiColorConsensus.gridx = 0;
		gbc_chckbxMultiColorConsensus.gridy = 2;
		add(chckbxMultiColorConsensus, gbc_chckbxMultiColorConsensus);
		
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
		JLabel lblPattern = new JLabel("pattern:");
		GridBagConstraints gbc_lblPattern = new GridBagConstraints();
		gbc_lblPattern.anchor = GridBagConstraints.EAST;
		gbc_lblPattern.insets = new Insets(0, 0, 5, 5);
		gbc_lblPattern.gridx = 0;
		gbc_lblPattern.gridy = 3;
		add(lblPattern, gbc_lblPattern);
		GridBagConstraints gbc_txtPattern = new GridBagConstraints();
		gbc_txtPattern.gridwidth = 2;
		gbc_txtPattern.insets = new Insets(0, 0, 5, 5);
		gbc_txtPattern.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPattern.gridx = 0;
		gbc_txtPattern.gridy = 4;
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
		gbc_btnLineColors.gridwidth = 2;
		gbc_btnLineColors.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnLineColors.insets = new Insets(0, 0, 0, 5);
		gbc_btnLineColors.gridx = 0;
		gbc_btnLineColors.gridy = 5;
		add(btnLineColors, gbc_btnLineColors);
		
	}
	
	public class ColorDialog extends JDialog {
		private static final long serialVersionUID = 1L;
		
		DensiTree m_dt;
		
		
		public ColorDialog(DensiTree dt) {
			m_dt = dt;
			getContentPane().setLayout(new GridLayout(0, 3, 0, 0));
			
			
			addColorAction("Color 1", "Color of most popular topolgy", 0);
			addColorAction("Color 2", "Color of second most popular topolgy", 1);
			addColorAction("Color 3", "Color of third most popular topolgy", 2);
			addColorAction("Default", "Default color ", 3);
			addColorAction("Consensus", "Consensus tree color ", DensiTree.CONSCOLOR);
			addColorAction("Background", "Background color ", DensiTree.BGCOLOR);
			addColorAction("Root canal", "Root canal color ", DensiTree.ROOTCANALCOLOR);
					
			for (int k = 9; k < m_dt.m_color.length; k++) {
				addColorAction("Color " + k, "Custom line color " + k, k);
			}
			
			JButton button = new RoundedButton("Close");
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			add(button);
			
			setPreferredSize(new Dimension(300,200));
			setSize(new Dimension(300,200));
			setLocation(300, 200);
			setModal(true);
			setVisible(true);
		}
		
		
		private void addColorAction(String label, String tiptext, int colorID) {
			JButton button = new ColorButton(label, m_dt.m_color[colorID]);
			button.setToolTipText(tiptext);
			button.addActionListener(new ColorActionListener(colorID, tiptext));
			
			JButton btnColors = new JButton("colors");
			btnColors.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					new ColorDialog(m_dt);
				}
			});
			add(button);
		}
		
		class ColorButton extends RoundedButton {
			private static final long serialVersionUID = 1L;

			Color m_color;
			
			ColorButton(String label, Color color) {
				super(label);
				m_color = color;
			}
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setColor(m_color);
				g.fillRect(3, 3, 10, getHeight() - 6);
			}
			
		}

		class ColorActionListener implements ActionListener {
			int m_colorID;
			String m_sName;
			
			public ColorActionListener(int colorID, String name) {
				m_colorID = colorID;
				m_sName = name;
			}
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(m_dt.m_Panel, m_sName, m_dt.m_color[m_colorID]);
				if (newColor != null && m_dt.m_color[m_colorID] != newColor) {
					ColorButton button = ((ColorButton)e.getSource());
					button.m_color = newColor;
					button.repaint();
					m_dt.m_color[m_colorID] = newColor;
					m_dt.calcColors(true);
					m_dt.makeDirty();
				}
				m_dt.repaint();
			}
		}

	}

	@Override
	public void stateChanged(ChangeEvent e) {
		List<String> selection = new ArrayList<String>();
		selection.add(LineColorMode.DEFAULT.toString());
		if (m_dt.m_bMetaDataReady) {
			selection.add(LineColorMode.COLOR_BY_CLADE.toString());
			selection.add(LineColorMode.BY_METADATA_PATTERN.toString());
			for (int i = 0; i < m_dt.m_metaDataTags.size(); i++) {
				if (!m_dt.m_metaDataTypes.get(i).equals(MetaDataType.SET)) {
					selection.add(m_dt.m_metaDataTags.get(i));				
				}
			}
		}
		ComboBoxModel model = new DefaultComboBoxModel(selection.toArray(new String[0]));
		comboBox.setModel(model);
	}

}
