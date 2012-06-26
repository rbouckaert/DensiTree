package viz.panel;

import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;

import viz.DensiTree;
import viz.DensiTree.LineColorMode;

import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import java.awt.Insets;
import javax.swing.JComboBox;

public class ColorPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	DensiTree m_dt;
	JComboBox comboBox;
	
	public ColorPanel(DensiTree dt) {
		m_dt = dt;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JCheckBox chckbxMultiColorConsensus = new JCheckBox("<html>Multi color<br>cons. trees</html>");
		chckbxMultiColorConsensus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_bViewMultiColor = ((JCheckBox) e.getSource()).isSelected();
				m_dt.makeDirty();
			}
		});
		GridBagConstraints gbc_chckbxMultiColorConsensus = new GridBagConstraints();
		gbc_chckbxMultiColorConsensus.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxMultiColorConsensus.gridx = 0;
		gbc_chckbxMultiColorConsensus.gridy = 0;
		add(chckbxMultiColorConsensus, gbc_chckbxMultiColorConsensus);
		
		comboBox = new JComboBox(DensiTree.LineColorMode.values());
		comboBox.setSelectedItem(m_dt.m_lineColorMode);
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						String selected = comboBox.getSelectedItem().toString();
						LineColorMode oldMode = m_dt.m_lineColorMode; 
						if (selected.equals(LineColorMode.COLOR_DEFAULT.toString())) {
							m_dt.m_lineColorMode = LineColorMode.COLOR_DEFAULT;
						} else if (selected.equals(LineColorMode.COLOR_BY_METADATA.toString())) {
							m_dt.m_lineColorMode = LineColorMode.COLOR_BY_METADATA;
						} else if (selected.equals(LineColorMode.COLOR_BY_CLADE.toString())) {
							m_dt.m_lineColorMode = LineColorMode.COLOR_BY_CLADE;
						}
						if (m_dt.m_lineColorMode != oldMode) {
							m_dt.calcColors();
							m_dt.makeDirty();
						}
					}
				});
			}
		});
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.insets = new Insets(0, 0, 5, 0);
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 0;
		gbc_comboBox.gridy = 1;
		add(comboBox, gbc_comboBox);

		int k = 2;
		addColorAction("Color 1", "Color of most popular topolgy", 0, k++);
		addColorAction("Color 2", "Color of second most popular topolgy", 1, k++);
		addColorAction("Color 3", "Color of third most popular topolgy", 2, k++);
		addColorAction("Default", "Default color ", 3, k++);
		addColorAction("Consensus", "Consensus tree color ", DensiTree.CONSCOLOR, k++);
		addColorAction("Background", "Background color ", DensiTree.BGCOLOR, k++);
		addColorAction("Root canal", "Root canal color ", DensiTree.ROOTCANALCOLOR, k++);
	}

	private void addColorAction(String label, String tiptext, int colorID, int posy) {
		JButton button = new RoundedButton(label);
		button.setToolTipText(tiptext);
		button.addActionListener(new ColorActionListener(colorID, tiptext));
		
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.fill = GridBagConstraints.HORIZONTAL;
		gbc_button.gridx = 0;
		gbc_button.gridy = posy;
		gbc_button.insets = new Insets(3, 3, 3, 3);
		add(button, gbc_button);
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
			if (newColor != null) {
				m_dt.m_color[m_colorID] = newColor;
				m_dt.makeDirty();
			}
			m_dt.repaint();
		}
	}
}
