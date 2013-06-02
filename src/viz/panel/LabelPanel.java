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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LabelPanel extends JPanel {
	final public static String HELP_LABEL_WIDTH = "Width of the label.";
	final public static String HELP_ROTATE = "Rotate label -- is only effective when root at top.";
	final public static String HELP_ALIGN = "Align labels with label for youngest tip. This is only useful when tips are not all from the same date.";
	final public static String HELP_HIDE = "Hide labels.";
	final public static String HELP_FONT = "Font used for labels.";
	final public static String HELP_COLOR = "Color used for labels.";
	final public static String HELP_SEARCH = "Search for labels. Labels matching the search string will be selected/highlighted.";
	
	private static final long serialVersionUID = 1L;
	
	private JTextField textField;
	DensiTree m_dt;
	private JTextField textField_1;
	
	public LabelPanel(DensiTree dt) {
		//setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		m_dt = dt;
		GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[]{0.0, 1.0};
		//layout.setHgap(30);
		setLayout(layout);
//		JPanel panel = new JPanel();
//		add(panel, gbc_panel);
		
		JLabel lblWidth = new JLabel("Width");
		lblWidth.setToolTipText(DensiTree.formatToolTip(HELP_LABEL_WIDTH));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.anchor = GridBagConstraints.WEST;
		gbc_panel.insets = new Insets(0, 0, 5, 5);
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		add(lblWidth, gbc_panel);
		
		textField = new JTextField();
		textField.setToolTipText(DensiTree.formatToolTip(HELP_LABEL_WIDTH));
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
		chckbxRotate.setToolTipText(DensiTree.formatToolTip(HELP_ROTATE));
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
		btnColor.setToolTipText(DensiTree.formatToolTip(HELP_COLOR));
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
		btnFont.setToolTipText(DensiTree.formatToolTip(HELP_FONT));
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
		chckbxAlign.setToolTipText(DensiTree.formatToolTip(HELP_ALIGN));
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
		
		JCheckBox chckbxHide = new JCheckBox("Hide");
		chckbxHide.setToolTipText(DensiTree.formatToolTip(HELP_HIDE));
		chckbxHide.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_bHideLabels = ((JCheckBox) e.getSource()).isSelected();
				m_dt.makeDirty();
			}
		});
		GridBagConstraints gbc_chckbxHide = new GridBagConstraints();
		gbc_chckbxHide.anchor = GridBagConstraints.WEST;
		gbc_chckbxHide.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxHide.gridx = 1;
		gbc_chckbxHide.gridy = 3;
		add(chckbxHide, gbc_chckbxHide);
		GridBagConstraints gbc_btnFont = new GridBagConstraints();
		gbc_btnFont.insets = new Insets(0, 0, 5, 5);
		gbc_btnFont.gridx = 0;
		gbc_btnFont.gridy = 4;
		add(btnFont, gbc_btnFont);
		GridBagConstraints gbc_btnColor = new GridBagConstraints();
		gbc_btnColor.insets = new Insets(0, 0, 5, 0);
		gbc_btnColor.gridx = 1;
		gbc_btnColor.gridy = 4;
		add(btnColor, gbc_btnColor);
		
		JLabel lblSearch = new JLabel("Search");
		lblSearch.setToolTipText(DensiTree.formatToolTip(HELP_SEARCH));
		GridBagConstraints gbc_lblSearch = new GridBagConstraints();
		gbc_lblSearch.insets = new Insets(0, 0, 0, 5);
		gbc_lblSearch.anchor = GridBagConstraints.WEST;
		gbc_lblSearch.gridx = 0;
		gbc_lblSearch.gridy = 5;
		add(lblSearch, gbc_lblSearch);
		
		textField_1 = new JTextField();
		textField_1.setToolTipText(DensiTree.formatToolTip(HELP_SEARCH));
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 1;
		gbc_textField_1.gridy = 5;
		add(textField_1, gbc_textField_1);
		textField_1.setColumns(5);
		textField_1.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateSelection();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				updateSelection();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateSelection();
			}

			private void updateSelection() {
				try {
					String sPattern = ".*" + textField_1.getText() + ".*";
					Pattern pattern = Pattern.compile(sPattern);
					for (int i = 0; i < m_dt.m_sLabels.size(); i++) {
						Matcher m = pattern.matcher(m_dt.m_sLabels.get(i));
						m_dt.m_bSelection[i] = m.find();
					}
					//m_dt.m_bSelectionChanged = true;
					m_dt.m_Panel.repaint();
				} catch (NumberFormatException e) {
					// ignore
				}
			}

		});
	}
}
