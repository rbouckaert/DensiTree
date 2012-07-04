package viz.panel;

import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import viz.DensiTree;
import viz.graphics.JFontChooser;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class LabelPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private JTextField textField;
	DensiTree m_dt;
	
	public LabelPanel(DensiTree dt) {
		//setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		m_dt = dt;
		GridBagLayout layout = new GridBagLayout();
		//layout.setHgap(30);
		setLayout(layout);
//		JPanel panel = new JPanel();
//		add(panel, gbc_panel);
		
		JLabel lblWidth = new JLabel("Width");
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.anchor = GridBagConstraints.WEST;
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		add(lblWidth, gbc_panel);
		
		textField = new JTextField();
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try{
				m_dt.m_nLabelWidth = Integer.parseInt(textField.getText());
				} catch (Exception ex) {
				}
				m_dt.fitToScreen();
			}
		});
		textField.setText(m_dt.m_nLabelWidth+"");
		GridBagConstraints gbc_width = new GridBagConstraints();
		gbc_width.anchor = GridBagConstraints.WEST;
		gbc_width.insets = new Insets(0, 0, 5, 0);
		gbc_width.gridx = 1;
		gbc_width.gridy = 0;
		add(textField, gbc_width);
		textField.setColumns(5);
		
		JCheckBox chckbxRotate = new JCheckBox("Rotate");
		chckbxRotate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JCheckBox button = (JCheckBox) e.getSource();
				m_dt.m_bRotateTextWhenRootAtTop = button.isSelected();
				m_dt.fitToScreen();
			}
		});
		GridBagConstraints gbc_chckbxRotate = new GridBagConstraints();
		gbc_chckbxRotate.anchor = GridBagConstraints.WEST;
		gbc_chckbxRotate.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxRotate.gridx = 1;
		gbc_chckbxRotate.gridy = 1;
		add(chckbxRotate, gbc_chckbxRotate);
		
		JButton btnColor = new RoundedButton("Color");
		btnColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(m_dt.m_Panel, getName(), m_dt.m_color[DensiTree.LABELCOLOR]);
				if (newColor != null) {
					m_dt.m_color[DensiTree.LABELCOLOR] = newColor;
					m_dt.makeDirty();
				}
				m_dt.repaint();
			}
		});
		
		JButton btnFont = new RoundedButton("Font");
		btnFont.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFontChooser fontChooser = new JFontChooser();
				if (m_dt.m_font != null) {
					fontChooser.setSelectedFont(m_dt.m_font);
				}
				int result = fontChooser.showDialog(null);
				if (result == JFontChooser.OK_OPTION) {
					m_dt.m_font = fontChooser.getSelectedFont();
					m_dt.makeDirty();
					m_dt.repaint();
				}
			}
		});
		
		JCheckBox chckbxAlign = new JCheckBox("Align");
		chckbxAlign.setSelected(m_dt.m_bAlignLabels);
		chckbxAlign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_bAlignLabels = ((JCheckBox) e.getSource()).isSelected();
				m_dt.makeDirty();
			}
		});
		GridBagConstraints gbc_chckbxAlign = new GridBagConstraints();
		gbc_chckbxAlign.anchor = GridBagConstraints.WEST;
		gbc_chckbxAlign.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxAlign.gridx = 1;
		gbc_chckbxAlign.gridy = 2;
		add(chckbxAlign, gbc_chckbxAlign);
		GridBagConstraints gbc_btnFont = new GridBagConstraints();
		gbc_btnFont.insets = new Insets(0, 0, 0, 5);
		gbc_btnFont.gridx = 0;
		gbc_btnFont.gridy = 3;
		add(btnFont, gbc_btnFont);
		GridBagConstraints gbc_btnColor = new GridBagConstraints();
		gbc_btnColor.gridx = 1;
		gbc_btnColor.gridy = 3;
		add(btnColor, gbc_btnColor);
	}
}
