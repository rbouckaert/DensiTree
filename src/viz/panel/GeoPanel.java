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
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.SwingConstants;
import javax.swing.JSpinner;

public class GeoPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	DensiTree m_dt;
	private JTextField textField;
	SpinnerNumberModel model;
	
	public GeoPanel(DensiTree dt) {
		m_dt = dt;
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JCheckBox chckbxShowGeoInfo = new JCheckBox("<html>Show geo info<br>(if any)</html>");
		chckbxShowGeoInfo.setSelected(m_dt.m_bDrawGeo);
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
		
		JLabel lblLineWidth = new JLabel("Line width");
		GridBagConstraints gbc_lblLineWidth = new GridBagConstraints();
		gbc_lblLineWidth.anchor = GridBagConstraints.EAST;
		gbc_lblLineWidth.insets = new Insets(0, 0, 5, 5);
		gbc_lblLineWidth.gridx = 0;
		gbc_lblLineWidth.gridy = 2;
		add(lblLineWidth, gbc_lblLineWidth);
		
		
		model =  new SpinnerNumberModel(m_dt.m_nGeoWidth, //initial value
		                               1, //min
		                               100, //max
		                               1); // stepsize 
		JSpinner spinner = new JSpinner(model);
		GridBagConstraints gbc_spinner = new GridBagConstraints();
		gbc_spinner.anchor = GridBagConstraints.WEST;
		gbc_spinner.insets = new Insets(0, 0, 5, 0);
		gbc_spinner.gridx = 1;
		gbc_spinner.gridy = 2;
		add(spinner, gbc_spinner);
		model.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				try {
					String str = model.getValue().toString();
					m_dt.m_nGeoWidth = Integer.parseInt(str);
					m_dt.m_Panel.clearImage();
					m_dt.repaint();
				} catch (Exception ex) {}
			}
		});
				
				JButton btnLoadLocations = new RoundedButton("<html>Load<br>locations</html>");
				btnLoadLocations.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						m_dt.a_loadkml.actionPerformed(e);
					}
				});
				GridBagConstraints gbc_btnLoadLocations = new GridBagConstraints();
				gbc_btnLoadLocations.fill = GridBagConstraints.HORIZONTAL;
				gbc_btnLoadLocations.insets = new Insets(0, 0, 5, 5);
				gbc_btnLoadLocations.gridx = 0;
				gbc_btnLoadLocations.gridy = 3;
				add(btnLoadLocations, gbc_btnLoadLocations);
				
				JButton btnLineColor = new RoundedButton("Color");
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
						gbc_btnLineColor.insets = new Insets(0, 0, 5, 0);
						gbc_btnLineColor.fill = GridBagConstraints.HORIZONTAL;
						gbc_btnLineColor.gridx = 1;
						gbc_btnLineColor.gridy = 3;
						add(btnLineColor, gbc_btnLineColor);
		
	}
}
