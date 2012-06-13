package viz.panel;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;

import viz.DensiTree;

import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import java.awt.Insets;

import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.SwingConstants;

public class GeoPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	DensiTree m_dt;
	private JTextField textField;
	
	public GeoPanel(DensiTree dt) {
		m_dt = dt;
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JCheckBox chckbxShowGeoInfo = new JCheckBox("Show geo info (if any)");
		chckbxShowGeoInfo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_bDrawGeo;
				m_dt.m_bDrawGeo = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_bDrawGeo) {
					m_dt.makeDirty();
				}
			}
		});
		GridBagConstraints gbc_chckbxShowGeoInfo = new GridBagConstraints();
		gbc_chckbxShowGeoInfo.gridwidth = 2;
		gbc_chckbxShowGeoInfo.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxShowGeoInfo.gridx = 0;
		gbc_chckbxShowGeoInfo.gridy = 0;
		add(chckbxShowGeoInfo, gbc_chckbxShowGeoInfo);
		
		JButton btnLoadLocations = new JButton("Load locations");
		btnLoadLocations.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.a_loadkml.actionPerformed(e);
			}
		});
		GridBagConstraints gbc_btnLoadLocations = new GridBagConstraints();
		gbc_btnLoadLocations.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnLoadLocations.insets = new Insets(0, 0, 5, 0);
		gbc_btnLoadLocations.gridx = 1;
		gbc_btnLoadLocations.gridy = 1;
		add(btnLoadLocations, gbc_btnLoadLocations);
		
		JLabel lblLineWidth = new JLabel("Line width");
		GridBagConstraints gbc_lblLineWidth = new GridBagConstraints();
		gbc_lblLineWidth.anchor = GridBagConstraints.EAST;
		gbc_lblLineWidth.insets = new Insets(0, 0, 5, 5);
		gbc_lblLineWidth.gridx = 0;
		gbc_lblLineWidth.gridy = 2;
		add(lblLineWidth, gbc_lblLineWidth);
		
		textField = new JTextField(m_dt.m_nGeoWidth + "");
		textField.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.anchor = GridBagConstraints.WEST;
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 2;
		add(textField, gbc_textField);
		textField.setColumns(5);
		textField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {update();}
			@Override
			public void insertUpdate(DocumentEvent e) {update();}
			@Override
			public void changedUpdate(DocumentEvent e) {update();}
			private void update() {
				try {
					m_dt.m_nGeoWidth = Integer.parseInt(textField.getText());
					m_dt.m_Panel.clearImage();
					m_dt.repaint();
				} catch (Exception e) {}
				
			}
			
		});
		
		JButton btnLineColor = new JButton("Line color");
		btnLineColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(m_dt.m_Panel, getName(), m_dt.m_color[DensiTree.GEOCOLOR]);
				if (newColor != null) {
					m_dt.m_color[DensiTree.GEOCOLOR] = newColor;
					m_dt.makeDirty();
				}
				m_dt.repaint();
			}
		});
		GridBagConstraints gbc_btnLineColor = new GridBagConstraints();
		gbc_btnLineColor.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnLineColor.gridx = 1;
		gbc_btnLineColor.gridy = 3;
		add(btnLineColor, gbc_btnLineColor);
		
	}
}
