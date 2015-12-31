package viz.panel;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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
import viz.util.Util;

import javax.swing.BoxLayout;
import javax.swing.border.LineBorder;
import javax.swing.JSpinner;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JSeparator;

public class GridPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	final public static String HELP_GRID = "Show lines indicating timescale. Options are to show none, " +
			"short lines at the side of the panel, or full lines over the complete tree set.";
	final public static String HELP_DIGITS = "Set number of significant digits for the grid labels.";
	final public static String HELP_REVERSE = "By setting reverse, the time scale will be drawn forward in time. " +
			"By default, time scale is drawn backward in time, so that the height of a tree is a positive " +
			"number. Also, set 'origin' to the date of the youngest tip.";
	final public static String HELP_FONT = "Set font of the grid labels.";
	final public static String HELP_COLOR = "Set colour of the grid labels.";
	final public static String HELP_ORIGIN = "Set date of the youngest tip.";
	final public static String HELP_AUTOMATIC = "Automatically determine the number of ticks.";
	final public static String HELP_TICKS = "Interval between two ticks.";
	final public static String HELP_OFFSET = "Time added to the ticks. This can be usefull when the youngest tip of the " +
			"tree is on a number that is not quite a round number, for example 2003.4. Setting the offset to " +
			"-3.4 ensures the grid lines will be drawn on 2000 instead of through 2003.4.";
	final public static String HELP_SCALE = "Scale time, which can be handy when the tree is in substitutions " +
			"and a clock rate is available from the literature. A negative scale has the same effect as " +
			"selecting 'reverse' with a positive scale.";

	

	DensiTree m_dt;
	ButtonGroup m_modeGroup = new ButtonGroup();
	private JTextField m_originTextField;
	private JTextField m_ticksTextField;
	private JTextField m_offsetTextField;
	SpinnerNumberModel significantDigitsModel;
	private JTextField txtScale;
	/**
	 * Create the panel.
	 */
	public GridPanel(DensiTree dt) {
		// setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null,
		// null));
		m_dt = dt;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 10, 0, 20, 0, 0 };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, 1.0, 0.0, 0.0 };
		setLayout(gridBagLayout);

		JPanel panel = new JPanel();
		panel.setBorder(new LineBorder(Color.gray));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.weightx = 0.95;
		gbc_panel.gridwidth = 3;
		gbc_panel.gridheight = 3;
		gbc_panel.insets = new Insets(0, 0, 5, 5);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 1;
		add(panel, gbc_panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JRadioButton rdbtnNewRadioButton_1 = new JRadioButton("No grid");
		panel.add(rdbtnNewRadioButton_1);
		rdbtnNewRadioButton_1.addActionListener(e-> {
				m_dt.m_gridDrawer.m_nGridMode = GridMode.NONE;
				m_dt.makeDirty();
			});

		m_modeGroup.add(rdbtnNewRadioButton_1);

		JRadioButton rdbtnNewRadioButton = new JRadioButton("Short grid");
		panel.add(rdbtnNewRadioButton);
		rdbtnNewRadioButton.addActionListener(e-> {
				m_dt.m_gridDrawer.m_nGridMode = GridMode.SHORT;
				m_dt.makeDirty();
		});
		m_modeGroup.add(rdbtnNewRadioButton);

		JRadioButton radioButton_2 = new JRadioButton("Full grid");
		panel.add(radioButton_2);
		radioButton_2.addActionListener(e-> {
				m_dt.m_gridDrawer.m_nGridMode = GridMode.FULL;
				m_dt.makeDirty();
			});
		m_modeGroup.add(radioButton_2);
		m_modeGroup.setSelected(rdbtnNewRadioButton_1.getModel(), true);
		
		rdbtnNewRadioButton.setToolTipText(Util.formatToolTipAsHtml(HELP_GRID));
		rdbtnNewRadioButton_1.setToolTipText(Util.formatToolTipAsHtml(HELP_GRID));
		radioButton_2.setToolTipText(Util.formatToolTipAsHtml(HELP_GRID));
		
		JLabel lblDigits = new JLabel("Digits");
		lblDigits.setToolTipText(Util.formatToolTipAsHtml(HELP_DIGITS));
		GridBagConstraints gbc_lblDigits = new GridBagConstraints();
		gbc_lblDigits.anchor = GridBagConstraints.SOUTHWEST;
		gbc_lblDigits.insets = new Insets(0, 0, 5, 5);
		gbc_lblDigits.gridx = 0;
		gbc_lblDigits.gridy = 4;
		add(lblDigits, gbc_lblDigits);
		
		significantDigitsModel = new SpinnerNumberModel(m_dt.m_gridDrawer.m_nGridDigits, 0, 5, 1);
		JSpinner spinner = new JSpinner(significantDigitsModel);
		spinner.setToolTipText(Util.formatToolTipAsHtml(HELP_DIGITS));
		significantDigitsModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				m_dt.m_gridDrawer.m_nGridDigits = (Integer) significantDigitsModel.getValue();
				m_dt.makeDirty();
			}
		});
		GridBagConstraints gbc_spinner = new GridBagConstraints();
		gbc_spinner.anchor = GridBagConstraints.WEST;
		gbc_spinner.gridwidth = 2;
		gbc_spinner.insets = new Insets(0, 0, 5, 5);
		gbc_spinner.gridx = 1;
		gbc_spinner.gridy = 4;
		add(spinner, gbc_spinner);

		JCheckBox reverseGrid = new JCheckBox("Reverse");
		reverseGrid.setToolTipText(Util.formatToolTipAsHtml(HELP_REVERSE));
		GridBagConstraints c5 = new GridBagConstraints();
		c5.insets = new Insets(0, 0, 5, 5);
		c5.gridx = 0;
		c5.gridy = 5;
		c5.weightx = 0.5;
		c5.gridwidth = 3;
		c5.fill = GridBagConstraints.HORIZONTAL;
		add(reverseGrid, c5);
		reverseGrid.addActionListener(e-> {
				JCheckBox button = (JCheckBox) e.getSource();
				m_dt.m_gridDrawer.m_bReverseGrid = button.isSelected();
				m_dt.m_Panel.clearImage();
				m_dt.repaint();
			});

		JButton btnGridFont = new RoundedButton("Font");
		btnGridFont.setToolTipText(Util.formatToolTipAsHtml(HELP_FONT));
		GridBagConstraints c7 = new GridBagConstraints();
		c7.insets = new Insets(0, 0, 5, 5);
		c7.gridwidth = 2;
		c7.gridx = 0;
		c7.gridy = 6;
		c7.weightx = 0.5;
		//c7.insets = new Insets(3, 3, 5, 5);
		c7.fill = GridBagConstraints.HORIZONTAL;
		add(btnGridFont, c7);
		btnGridFont.addActionListener(ae-> {
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
			}); // actionPerformed

		JButton btnGridColor = new RoundedButton("Color");
		btnGridColor.setToolTipText(Util.formatToolTipAsHtml(HELP_COLOR));
		GridBagConstraints c6 = new GridBagConstraints();
		c6.insets = new Insets(0, 0, 5, 5);
		c6.gridwidth = 2;
		c6.gridx = 2;
		c6.gridy = 6;
		c6.weightx = 0.5;
		//c6.insets = new Insets(3, 3, 5, 5);
		c6.fill = GridBagConstraints.HORIZONTAL;
		add(btnGridColor, c6);
		btnGridColor.addActionListener(ae-> {
				Color newColor = JColorChooser.showDialog(m_dt.m_Panel, getName(), m_dt.m_color[DensiTree.HEIGHTCOLOR]);
				if (newColor != null) {
					m_dt.m_color[DensiTree.HEIGHTCOLOR] = newColor;
					m_dt.makeDirty();
				}
				m_dt.repaint();
			});

		JLabel lblOffset = new JLabel("Origin");
		lblOffset.setToolTipText(Util.formatToolTipAsHtml(HELP_ORIGIN));
		GridBagConstraints c4 = new GridBagConstraints();
		c4.insets = new Insets(0, 0, 5, 5);
		c4.gridx = 0;
		c4.gridy = 7;
		c4.weightx = 0.5;
		c4.gridwidth = 1;
		c4.fill = GridBagConstraints.HORIZONTAL;
		add(lblOffset, c4);

		m_originTextField = new JTextField();
		m_originTextField.setToolTipText(Util.formatToolTipAsHtml(HELP_ORIGIN));
		m_originTextField.setText(m_dt.m_gridDrawer.m_fGridOrigin + "");
		GridBagConstraints c8 = new GridBagConstraints();
		c8.gridwidth = 2;
		c8.fill = GridBagConstraints.HORIZONTAL;
		c8.insets = new Insets(0, 0, 5, 5);
		c8.gridx = 1;
		c8.gridy = 7;
		add(m_originTextField, c8);
		m_originTextField.setColumns(4);

		JCheckBox chckbxAutomatic = new JCheckBox("Automatic");
		chckbxAutomatic.setToolTipText(Util.formatToolTipAsHtml(HELP_AUTOMATIC));
		chckbxAutomatic.setSelected(m_dt.m_gridDrawer.m_bAutoGrid);
		chckbxAutomatic.addActionListener(e-> {
				boolean bPrev = m_dt.m_gridDrawer.m_bAutoGrid;
				m_dt.m_gridDrawer.m_bAutoGrid = ((JCheckBox) e.getSource()).isSelected();
				m_ticksTextField.setEnabled(!m_dt.m_gridDrawer.m_bAutoGrid);
				m_offsetTextField.setEnabled(!m_dt.m_gridDrawer.m_bAutoGrid);
				if (bPrev != m_dt.m_gridDrawer.m_bAutoGrid) {
					m_dt.makeDirty();
					m_dt.repaint();
				}
			});
		GridBagConstraints gbc_chckbxAutomatic = new GridBagConstraints();
		gbc_chckbxAutomatic.gridwidth = 3;
		gbc_chckbxAutomatic.anchor = GridBagConstraints.WEST;
		gbc_chckbxAutomatic.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxAutomatic.gridx = 0;
		gbc_chckbxAutomatic.gridy = 8;
		add(chckbxAutomatic, gbc_chckbxAutomatic);

		JLabel lblTicks = new JLabel("Ticks");
		lblTicks.setToolTipText(Util.formatToolTipAsHtml(HELP_TICKS));
		GridBagConstraints gbc_lblTicks = new GridBagConstraints();
		gbc_lblTicks.anchor = GridBagConstraints.WEST;
		gbc_lblTicks.insets = new Insets(0, 0, 5, 5);
		gbc_lblTicks.gridx = 0;
		gbc_lblTicks.gridy = 9;
		add(lblTicks, gbc_lblTicks);

		m_ticksTextField = new JTextField();
		m_ticksTextField.setToolTipText(Util.formatToolTipAsHtml(HELP_TICKS));
		m_ticksTextField.setText(m_dt.m_gridDrawer.m_fGridTicks+"");
		m_ticksTextField.getDocument().addDocumentListener(new DocumentListener() {
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
					float fGridTicks = Float.parseFloat(m_ticksTextField.getText());
					if (fGridTicks > 0) {
						m_dt.m_gridDrawer.m_fGridTicks = fGridTicks;
						m_dt.makeDirty();
						m_dt.repaint();
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}

		});

		GridBagConstraints gbc_txtScale = new GridBagConstraints();
		gbc_txtScale.gridwidth = 2;
		gbc_txtScale.insets = new Insets(0, 0, 5, 5);
		gbc_txtScale.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtScale.gridx = 1;
		gbc_txtScale.gridy = 9;
		add(m_ticksTextField, gbc_txtScale);
		m_ticksTextField.setColumns(4);

		JLabel lblOrigin = new JLabel("Offset");
		lblOrigin.setToolTipText(Util.formatToolTipAsHtml(HELP_OFFSET));
		GridBagConstraints gbc_lblOrigin = new GridBagConstraints();
		gbc_lblOrigin.anchor = GridBagConstraints.WEST;
		gbc_lblOrigin.insets = new Insets(0, 0, 5, 5);
		gbc_lblOrigin.gridx = 0;
		gbc_lblOrigin.gridy = 10;
		add(lblOrigin, gbc_lblOrigin);

		m_offsetTextField = new JTextField();
		m_offsetTextField.setToolTipText(Util.formatToolTipAsHtml(HELP_OFFSET));
		m_offsetTextField.setText(m_dt.m_gridDrawer.m_fGridOffset+"");
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
					m_dt.makeDirty();
					m_dt.repaint();
				} catch (NumberFormatException e) {
					// ignore
				}
			}

		});

		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.gridwidth = 2;
		gbc_textField_1.insets = new Insets(0, 0, 5, 5);
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 1;
		gbc_textField_1.gridy = 10;
		add(m_offsetTextField, gbc_textField_1);
		m_offsetTextField.setColumns(4);
		m_originTextField.getDocument().addDocumentListener(new DocumentListener() {
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
					m_dt.m_gridDrawer.m_fGridOrigin = Float.parseFloat(m_originTextField.getText());
					m_dt.m_Panel.clearImage();
					m_dt.repaint();
				} catch (NumberFormatException e) {
					// ignore
				}
			}

		});
		m_ticksTextField.setEnabled(!m_dt.m_gridDrawer.m_bAutoGrid);
		m_offsetTextField.setEnabled(!m_dt.m_gridDrawer.m_bAutoGrid);
		
		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.gridwidth = 3;
		gbc_separator.insets = new Insets(0, 0, 5, 5);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 11;
		add(separator, gbc_separator);

	
		JLabel lblS = new JLabel("Scale");
		lblS.setToolTipText(Util.formatToolTipAsHtml(HELP_SCALE));
		GridBagConstraints gbc_lblS = new GridBagConstraints();
		gbc_lblS.anchor = GridBagConstraints.WEST;
		gbc_lblS.insets = new Insets(0, 0, 0, 5);
		gbc_lblS.gridx = 0;
		gbc_lblS.gridy = 12;
		add(lblS, gbc_lblS);
		
		txtScale = new JTextField();
		txtScale.addActionListener(e-> {
				try {
					m_dt.m_fUserScale = Float.parseFloat(txtScale.getText());
					m_dt.updateCladeModel();
					m_dt.makeDirty();
					m_dt.repaint();
				} catch (Exception ex) {
				}
			});
		txtScale.setText(m_dt.m_fUserScale + "");
		txtScale.setToolTipText(Util.formatToolTipAsHtml(HELP_SCALE));
		GridBagConstraints gbc_txtScale2 = new GridBagConstraints();
		gbc_txtScale2.gridwidth = 2;
		gbc_txtScale2.insets = new Insets(0, 0, 0, 5);
		gbc_txtScale2.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtScale2.gridx = 1;
		gbc_txtScale2.gridy = 12;
		add(txtScale, gbc_txtScale2);
		txtScale.setColumns(4);
	}

}
