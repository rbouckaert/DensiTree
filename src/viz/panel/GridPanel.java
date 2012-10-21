package viz.panel;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import viz.DensiTree;
import viz.GridDrawer.GridMode;
import viz.graphics.JFontChooser;
import javax.swing.BoxLayout;
import javax.swing.border.LineBorder;
import javax.swing.JSpinner;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class GridPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	DensiTree m_dt;
	ButtonGroup m_modeGroup = new ButtonGroup();
	private JTextField m_offsetTextField;
	private JTextField m_ticksTextField;
	private JTextField m_originTextField;
	SpinnerNumberModel significantDigitsModel;
	/**
	 * Create the panel.
	 */
	public GridPanel(DensiTree dt) {
		// setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null,
		// null));
		m_dt = dt;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 10, 0, 20, 0, 0 };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, 1.0, 0.0, 0.0 };
		setLayout(gridBagLayout);

		JPanel panel = new JPanel();
		panel.setBorder(new LineBorder(Color.gray));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.weightx = 0.95;
		gbc_panel.gridwidth = 3;
		gbc_panel.gridheight = 3;
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 1;
		add(panel, gbc_panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JRadioButton rdbtnNewRadioButton_1 = new JRadioButton("No grid");
		panel.add(rdbtnNewRadioButton_1);
		rdbtnNewRadioButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_gridDrawer.m_nGridMode = GridMode.NONE;
				m_dt.makeDirty();
			}
		});

		m_modeGroup.add(rdbtnNewRadioButton_1);

		JRadioButton rdbtnNewRadioButton = new JRadioButton("Short grid");
		panel.add(rdbtnNewRadioButton);
		rdbtnNewRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_gridDrawer.m_nGridMode = GridMode.SHORT;
				m_dt.makeDirty();
			}
		});
		m_modeGroup.add(rdbtnNewRadioButton);

		JRadioButton radioButton_2 = new JRadioButton("Full grid");
		panel.add(radioButton_2);
		radioButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_dt.m_gridDrawer.m_nGridMode = GridMode.FULL;
				m_dt.makeDirty();
			}
		});
		m_modeGroup.add(radioButton_2);
		m_modeGroup.setSelected(rdbtnNewRadioButton_1.getModel(), true);
		
		JLabel lblDigits = new JLabel("Digits");
		GridBagConstraints gbc_lblDigits = new GridBagConstraints();
		gbc_lblDigits.anchor = GridBagConstraints.SOUTHWEST;
		gbc_lblDigits.insets = new Insets(0, 0, 5, 0);
		gbc_lblDigits.gridx = 0;
		gbc_lblDigits.gridy = 4;
		add(lblDigits, gbc_lblDigits);
		
		significantDigitsModel = new SpinnerNumberModel(m_dt.m_gridDrawer.m_nGridDigits, 0, 5, 1);
		JSpinner spinner = new JSpinner(significantDigitsModel);
		significantDigitsModel.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				m_dt.m_gridDrawer.m_nGridDigits = (Integer) significantDigitsModel.getValue();
				m_dt.makeDirty();
			}
		});
		GridBagConstraints gbc_spinner = new GridBagConstraints();
		gbc_spinner.anchor = GridBagConstraints.WEST;
		gbc_spinner.gridwidth = 2;
		gbc_spinner.insets = new Insets(0, 0, 5, 0);
		gbc_spinner.gridx = 1;
		gbc_spinner.gridy = 4;
		add(spinner, gbc_spinner);

		JCheckBox reverseGrid = new JCheckBox("Reverse");
		GridBagConstraints c5 = new GridBagConstraints();
		c5.insets = new Insets(0, 0, 5, 0);
		c5.gridx = 0;
		c5.gridy = 5;
		c5.weightx = 0.5;
		c5.gridwidth = 3;
		c5.fill = GridBagConstraints.HORIZONTAL;
		add(reverseGrid, c5);
		reverseGrid.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox button = (JCheckBox) e.getSource();
				m_dt.m_gridDrawer.m_bReverseGrid = button.isSelected();
				m_dt.m_Panel.clearImage();
				m_dt.repaint();
			}
		});

		JButton btnGridFont = new RoundedButton("Font");
		GridBagConstraints c7 = new GridBagConstraints();
		c7.gridwidth = 2;
		c7.gridx = 0;
		c7.gridy = 6;
		c7.weightx = 0.5;
		//c7.insets = new Insets(3, 3, 5, 5);
		c7.fill = GridBagConstraints.HORIZONTAL;
		add(btnGridFont, c7);
		btnGridFont.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
				JFontChooser fontChooser = new JFontChooser();
				if (m_dt.m_gridDrawer.m_gridfont != null) {
					fontChooser.setSelectedFont(m_dt.m_gridDrawer.m_gridfont);
				}
				int result = fontChooser.showDialog(null);
				if (result == JFontChooser.OK_OPTION) {
					m_dt.m_gridDrawer.m_gridfont = fontChooser.getSelectedFont();
					m_dt.makeDirty();
					m_dt.repaint();
				}
			} // actionPerformed
		});

		JButton btnGridColor = new RoundedButton("Color");
		GridBagConstraints c6 = new GridBagConstraints();
		c6.gridwidth = 2;
		c6.gridx = 2;
		c6.gridy = 6;
		c6.weightx = 0.5;
		//c6.insets = new Insets(3, 3, 5, 5);
		c6.fill = GridBagConstraints.HORIZONTAL;
		add(btnGridColor, c6);
		btnGridColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				Color newColor = JColorChooser.showDialog(m_dt.m_Panel, getName(), m_dt.m_color[DensiTree.HEIGHTCOLOR]);
				if (newColor != null) {
					m_dt.m_color[DensiTree.HEIGHTCOLOR] = newColor;
					m_dt.makeDirty();
				}
				m_dt.repaint();
			}
		});

		JLabel lblOffset = new JLabel("Offset");
		GridBagConstraints c4 = new GridBagConstraints();
		c4.insets = new Insets(0, 0, 5, 0);
		c4.gridx = 0;
		c4.gridy = 7;
		c4.weightx = 0.5;
		c4.gridwidth = 1;
		c4.fill = GridBagConstraints.HORIZONTAL;
		add(lblOffset, c4);

		m_offsetTextField = new JTextField();
		m_offsetTextField.setText(m_dt.m_gridDrawer.m_fGridOffset + "");
		GridBagConstraints c8 = new GridBagConstraints();
		c8.gridwidth = 2;
		c8.fill = GridBagConstraints.HORIZONTAL;
		c8.insets = new Insets(0, 0, 5, 0);
		c8.gridx = 1;
		c8.gridy = 7;
		add(m_offsetTextField, c8);
		m_offsetTextField.setColumns(4);

		JCheckBox chckbxAutomatic = new JCheckBox("Automatic");
		chckbxAutomatic.setSelected(m_dt.m_gridDrawer.m_bAutoGrid);
		chckbxAutomatic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_gridDrawer.m_bAutoGrid;
				m_dt.m_gridDrawer.m_bAutoGrid = ((JCheckBox) e.getSource()).isSelected();
				m_ticksTextField.setEnabled(!m_dt.m_gridDrawer.m_bAutoGrid);
				m_originTextField.setEnabled(!m_dt.m_gridDrawer.m_bAutoGrid);
				if (bPrev != m_dt.m_gridDrawer.m_bAutoGrid) {
					m_dt.makeDirty();
					m_dt.repaint();
				}
			}
		});
		GridBagConstraints gbc_chckbxAutomatic = new GridBagConstraints();
		gbc_chckbxAutomatic.gridwidth = 3;
		gbc_chckbxAutomatic.anchor = GridBagConstraints.WEST;
		gbc_chckbxAutomatic.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxAutomatic.gridx = 0;
		gbc_chckbxAutomatic.gridy = 8;
		add(chckbxAutomatic, gbc_chckbxAutomatic);

		JLabel lblTicks = new JLabel("Ticks");
		GridBagConstraints gbc_lblTicks = new GridBagConstraints();
		gbc_lblTicks.anchor = GridBagConstraints.WEST;
		gbc_lblTicks.insets = new Insets(0, 0, 5, 0);
		gbc_lblTicks.gridx = 0;
		gbc_lblTicks.gridy = 9;
		add(lblTicks, gbc_lblTicks);

		m_ticksTextField = new JTextField();
		m_ticksTextField.setText(m_dt.m_gridDrawer.m_fGridTicks+"");
		m_ticksTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					m_dt.m_gridDrawer.m_fGridTicks = Float.parseFloat(m_ticksTextField.getText());
					m_dt.makeDirty();
					m_dt.repaint();
				} catch (Exception ex) {
				}
			}
		});
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 2;
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 9;
		add(m_ticksTextField, gbc_textField);
		m_ticksTextField.setColumns(4);

		JLabel lblOrigin = new JLabel("Origin");
		GridBagConstraints gbc_lblOrigin = new GridBagConstraints();
		gbc_lblOrigin.anchor = GridBagConstraints.WEST;
		gbc_lblOrigin.insets = new Insets(0, 0, 0, 5);
		gbc_lblOrigin.gridx = 0;
		gbc_lblOrigin.gridy = 10;
		add(lblOrigin, gbc_lblOrigin);

		m_originTextField = new JTextField();
		m_originTextField.setText(m_dt.m_gridDrawer.m_fGridOrigin+"");
		m_originTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					m_dt.m_gridDrawer.m_fGridOrigin = Float.parseFloat(m_originTextField.getText());
					m_dt.makeDirty();
					m_dt.repaint();
				} catch (Exception ex) {
				}
			}
		});
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.gridwidth = 2;
		gbc_textField_1.insets = new Insets(0, 0, 0, 5);
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 1;
		gbc_textField_1.gridy = 10;
		add(m_originTextField, gbc_textField_1);
		m_originTextField.setColumns(4);
		m_offsetTextField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateOffset();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				updateOffset();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateOffset();
			}

			private void updateOffset() {
				try {
					m_dt.m_gridDrawer.m_fGridOffset = Float.parseFloat(m_offsetTextField.getText());
					m_dt.m_Panel.clearImage();
					m_dt.repaint();
				} catch (NumberFormatException e) {
					// ignore
				}
			}

		});
		m_ticksTextField.setEnabled(!m_dt.m_gridDrawer.m_bAutoGrid);
		m_originTextField.setEnabled(!m_dt.m_gridDrawer.m_bAutoGrid);
	}

}