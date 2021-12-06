package viz.panel;





import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.Insets;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import viz.DensiTree;
import viz.DensiTree.LineWidthMode;
import viz.DensiTree.MetaDataType;
import viz.util.Util;

import javax.swing.JSpinner;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JSeparator;

public class LineWidthPanel extends JPanel implements ChangeListener {
	final static public String HELP_LINE_WIDTH = "Determine line width of trees of both tree set and consensus trees." +
			"For consensus trees, the average value for the topology is used.\n" +
			"DEFAULT: all lines are same width.\n" +
			"BY_META_DATA_PATTERN: use value of pattern specified below.\n" +
			"BY_META_DATA_NUMBER: use the N-th attribute value in the meta data.\n" +
			"meta data attribute: only available if any meta data attribute is specified. Use value of the attribute for line width of branches.";
	final static public String HELP_PATTERN = "Regular expression used for width of branches when BY_META_DATA_PATTERN " +
			"is chosen. The string of the pattern between brackets is selected as value.;";
	final static public String HELP_TOP = "Specifies N-th meta data attribute for top of branch when BY_META_DATA_NUMBER is seleced.";
	final static public String HELP_LINE_WIDTH_BOTTOM = "Line width at bottom of branch.\n" +
			"same as top: use same specification as for top of branch.\n" +
			"Fit to bottom: adjust bottom widths so they fit to top of branch below.\n" +
			"BY_META_DATA_PATTERN: use value of pattern specified below.\n" +
			"BY_META_DATA_NUMBER: use the N-th attribute value in the meta data.\n" +
			"meta data attribute: only available if any meta data attribute is specified. Use value of the attribute for line width of branches.";
	final static public String HELP_BOTTOM = "Specifies N-th meta data attribute for top of branch when BY_META_DATA_NUMBER is seleced.";
	final static public String HELP_ZERO_BASED = "If selected, the minimum value is zero, otherwise the minimum value of the range of " +
			"whatever value is used.";
	final static public String HELP_SCALE = "Scale width with this number.";
	
	
	
	private static final long serialVersionUID = 1L;
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_3;
	JSpinner spinner;
	JSpinner spinner_1;
	SpinnerNumberModel topOfBranchModel;
	SpinnerNumberModel bottomOfBranchModel;
	JComboBox<String> comboBoxBottom;
	JComboBox<String> comboBoxTop= new JComboBox<>();

	final static String SAME_AS_BOTTOM = "Same as top";
	final static String MAKE_FIT_BOTTOM = "Make fit to bottom";
	
	DensiTree m_dt;
	private JSeparator separator;
	private JSeparator separator_1;
	JCheckBox chckbxZeroBased;
	
	public LineWidthPanel(DensiTree dt) {
		m_dt = dt;
		m_dt.addChangeListener(this);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		gridBagLayout.columnWeights = new double[]{1.0, 0.0};
		setLayout(gridBagLayout);
		
		comboBoxBottom = new JComboBox<>();
		stateChanged(null);
		comboBoxBottom.setPreferredSize(new Dimension(130,20));
		comboBoxBottom.setMaximumSize(new Dimension(130,200));
		comboBoxBottom.setSelectedItem(m_dt.m_settings.m_lineWidthMode);

		comboBoxBottom.addActionListener(e-> {
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						String selected = comboBoxBottom.getSelectedItem().toString();
						LineWidthMode oldMode = m_dt.m_settings.m_lineWidthMode; 
						String oldTag = m_dt.m_settings.m_lineWidthTag;
						if (selected.equals(LineWidthMode.DEFAULT.toString())) {
							m_dt.m_settings.m_lineWidthMode = LineWidthMode.DEFAULT;
						} else if (selected.equals(LineWidthMode.BY_METADATA_PATTERN.toString())) {
							m_dt.m_settings.m_lineWidthMode = LineWidthMode.BY_METADATA_PATTERN;
						} else if (selected.equals(LineWidthMode.BY_METADATA_NUMBER.toString())) {
							m_dt.m_settings.m_lineWidthMode = LineWidthMode.BY_METADATA_NUMBER;
						} else {
							m_dt.m_settings.m_lineWidthTag = selected; 
							m_dt.m_settings.m_lineWidthMode = LineWidthMode.BY_METADATA_TAG;
						}
						m_dt.resetStyle();
//						txtPattern.setEnabled(m_dt.m_lineColorMode == LineColorMode.BY_METADATA_PATTERN);
//						chckbxShowLegend.setEnabled(m_dt.m_lineColorMode == LineColorMode.BY_METADATA_PATTERN 
//								|| m_dt.m_lineColorMode == LineColorMode.COLOR_BY_METADATA_TAG);
						if (m_dt.m_settings.m_lineWidthMode != oldMode || (m_dt.m_settings.m_lineWidthTag != null && !m_dt.m_settings.m_lineWidthTag.equals(oldTag))) {
							m_dt.calcLineWidths(true);
							m_dt.makeDirty();
						}
						updateEnabled();
						repaint();
					}

				});
			});
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.gridwidth = 2;
		gbc_comboBox.insets = new Insets(0, 0, 5, 0);
		//gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 0;
		gbc_comboBox.gridy = 2;
		add(comboBoxBottom, gbc_comboBox);
		
		
		textField_1 = new JTextField(m_dt.m_settings.m_sLineWidthPattern);
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.insets = new Insets(0, 0, 5, 0);
		gbc_textField_1.gridx = 0;
		gbc_textField_1.gridy = 3;
		gbc_textField_1.gridwidth= 2;
		add(textField_1, gbc_textField_1);
		textField_1.setColumns(10);
		textField_1.addActionListener(e-> {
				try {
					m_dt.m_settings.m_sLineWidthPattern = textField_1.getText();
					if (m_dt.m_settings.m_lineWidthMode!= LineWidthMode.DEFAULT) {
						m_dt.calcLineWidths(true);
						m_dt.makeDirty();
					}
				} catch (Exception ex) {}
			});
		bottomOfBranchModel = new SpinnerNumberModel(m_dt.m_settings.m_iPatternForBottom, 1, 100, 1);
		topOfBranchModel = new SpinnerNumberModel(m_dt.m_settings.m_iPatternForTop, 0, 100, 1);
		
		bottomOfBranchModel.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				m_dt.m_settings.m_iPatternForBottom = (Integer) bottomOfBranchModel.getValue();
				if (m_dt.m_settings.m_iPatternForBottom < 1) {
					m_dt.m_settings.m_iPatternForBottom = 1;
				}
				if (m_dt.m_settings.m_lineWidthMode!= LineWidthMode.DEFAULT) {
					m_dt.m_settings.m_pattern = m_dt.createPattern();
					m_dt.calcLineWidths(true);
					m_dt.makeDirty();
				}
			}
		});
		
		topOfBranchModel.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				m_dt.m_settings.m_iPatternForTop = (Integer) topOfBranchModel.getValue();
				if (m_dt.m_settings.m_iPatternForTop < 0) {
					m_dt.m_settings.m_iPatternForTop = 0;
				}
				if (m_dt.m_settings.m_lineWidthMode!= LineWidthMode.DEFAULT) {
					m_dt.m_settings.m_pattern = m_dt.createPattern();
					m_dt.calcLineWidths(true);
					m_dt.makeDirty();
				}
			}
		});
		
		
		JLabel lblNumberOfItem_1 = new JLabel("top");
		GridBagConstraints gbc_lblNumberOfItem_1 = new GridBagConstraints();
		gbc_lblNumberOfItem_1.anchor = GridBagConstraints.EAST;
		gbc_lblNumberOfItem_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNumberOfItem_1.gridx = 0;
		gbc_lblNumberOfItem_1.gridy = 4;
		add(lblNumberOfItem_1, gbc_lblNumberOfItem_1);
		spinner = new JSpinner(bottomOfBranchModel);
		GridBagConstraints gbc_spinner = new GridBagConstraints();
		gbc_spinner.insets = new Insets(0, 0, 5, 0);
		gbc_spinner.gridx = 1;
		gbc_spinner.gridy = 4;
		add(spinner, gbc_spinner);
		spinner.setMaximumSize(new Dimension(1,20));
		
		separator_1 = new JSeparator();
		GridBagConstraints gbc_separator_1 = new GridBagConstraints();
		gbc_separator_1.insets = new Insets(0, 0, 5, 5);
		gbc_separator_1.gridx = 0;
		gbc_separator_1.gridy = 5;
		add(separator_1, gbc_separator_1);

		GridBagConstraints gbc_comboBoxTop = new GridBagConstraints();
		gbc_comboBoxTop.gridwidth = 2;
		gbc_comboBoxTop.insets = new Insets(0, 0, 5, 0);
		gbc_comboBoxTop.gridx = 0;
		gbc_comboBoxTop.gridy = 6;
		add(comboBoxTop, gbc_comboBoxTop);
		
				comboBoxTop.setPreferredSize(new Dimension(130,20));
				comboBoxTop.setMaximumSize(new Dimension(130,200));
				comboBoxTop.setSelectedItem(m_dt.m_settings.m_lineWidthMode);
				comboBoxTop.addActionListener(ea-> {
							SwingUtilities.invokeLater(new Runnable() {
								
								@Override
								public void run() {
									String selected = comboBoxTop.getSelectedItem().toString();

									LineWidthMode oldMode = m_dt.m_settings.m_lineWidthModeTop; 
									String oldTag = m_dt.m_settings.m_lineWidthTagTop;
									boolean oldCorrectTopOfBranch = m_dt.m_settings.m_bCorrectTopOfBranch;
									if (selected.equals(SAME_AS_BOTTOM)) {
										m_dt.m_settings.m_lineWidthModeTop = LineWidthMode.DEFAULT;
									} else if (selected.equals(MAKE_FIT_BOTTOM)) {
										m_dt.m_settings.m_lineWidthModeTop = LineWidthMode.DEFAULT;
										m_dt.m_settings.m_bCorrectTopOfBranch = true;
									} else if (selected.equals(LineWidthMode.BY_METADATA_PATTERN.toString())) {
										m_dt.m_settings.m_lineWidthModeTop = LineWidthMode.BY_METADATA_PATTERN;
									} else if (selected.equals(LineWidthMode.BY_METADATA_NUMBER.toString())) {
										m_dt.m_settings.m_lineWidthModeTop = LineWidthMode.BY_METADATA_NUMBER;
									} else {
										m_dt.m_settings.m_lineWidthTagTop = selected; 
										m_dt.m_settings.m_lineWidthModeTop = LineWidthMode.BY_METADATA_TAG;
									}
									m_dt.m_settings.m_bCorrectTopOfBranch = selected.equals(MAKE_FIT_BOTTOM);
									m_dt.resetStyle();
//							txtPattern.setEnabled(m_dt.m_lineColorMode == LineColorMode.BY_METADATA_PATTERN);
//							chckbxShowLegend.setEnabled(m_dt.m_lineColorMode == LineColorMode.BY_METADATA_PATTERN 
//									|| m_dt.m_lineColorMode == LineColorMode.COLOR_BY_METADATA_TAG);
									if (m_dt.m_settings.m_lineWidthModeTop != oldMode || (m_dt.m_settings.m_lineWidthTag != null && !m_dt.m_settings.m_lineWidthTagTop.equals(oldTag))
											|| oldCorrectTopOfBranch != m_dt.m_settings.m_bCorrectTopOfBranch) {
										m_dt.calcLineWidths(true);
										m_dt.makeDirty();
									}
									updateEnabled();
									repaint();

								}
						});
					});
		
		textField = new JTextField(m_dt.m_settings.m_sLineWidthPatternTop);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridwidth = 2;
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.gridx = 0;
		gbc_textField.gridy = 7;
		add(textField, gbc_textField);
		textField.addActionListener(ea-> {
				try {
					m_dt.m_settings.m_sLineWidthPatternTop = textField.getText();
					m_dt.calcLineWidths(true);
					m_dt.makeDirty();
				} catch (Exception ex) {}
			});
		textField.setColumns(10);
		
		JLabel lblNumberOfItem = new JLabel("bottom");
		GridBagConstraints gbc_lblNumberOfItem = new GridBagConstraints();
		gbc_lblNumberOfItem.anchor = GridBagConstraints.EAST;
		gbc_lblNumberOfItem.insets = new Insets(0, 0, 5, 5);
		gbc_lblNumberOfItem.gridx = 0;
		gbc_lblNumberOfItem.gridy = 8;
		add(lblNumberOfItem, gbc_lblNumberOfItem);
		spinner_1 = new JSpinner(topOfBranchModel);
		GridBagConstraints gbc_spinner_1 = new GridBagConstraints();
		gbc_spinner_1.insets = new Insets(0, 0, 5, 0);
		gbc_spinner_1.gridx = 1;
		gbc_spinner_1.gridy = 8;
		add(spinner_1, gbc_spinner_1);
		spinner_1.setToolTipText("when 0, top will be egual to bottom");
		spinner_1.setMaximumSize(new Dimension(1,20));
		
		chckbxZeroBased = new JCheckBox("Zero based");
		chckbxZeroBased.setSelected(m_dt.m_settings.m_bWidthsAreZeroBased);
		chckbxZeroBased.addActionListener(ea-> {
				boolean bPrev = m_dt.m_settings.m_bCorrectTopOfBranch;
				m_dt.m_settings.m_bWidthsAreZeroBased = ((JCheckBox) ea.getSource()).isSelected();
				if (bPrev != m_dt.m_settings.m_bWidthsAreZeroBased) {
					m_dt.calcLineWidths(true);
					m_dt.makeDirty();
				}
			});
		
		separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.insets = new Insets(0, 0, 5, 5);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 9;
		add(separator, gbc_separator);
		GridBagConstraints gbc_chckbxZeroBased = new GridBagConstraints();
		gbc_chckbxZeroBased.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxZeroBased.gridwidth = 2;
		gbc_chckbxZeroBased.anchor = GridBagConstraints.WEST;
		gbc_chckbxZeroBased.gridx = 0;
		gbc_chckbxZeroBased.gridy = 10;
		add(chckbxZeroBased, gbc_chckbxZeroBased);
		
		GridBagConstraints gbc_lblMetaDataScale = new GridBagConstraints();
		gbc_lblMetaDataScale.anchor = GridBagConstraints.EAST;
		gbc_lblMetaDataScale.insets = new Insets(0, 0, 0, 5);
		gbc_lblMetaDataScale.gridx = 0;
		gbc_lblMetaDataScale.gridy = 11;
		JLabel lblMetaDataScale = new JLabel("Scale");
		add(lblMetaDataScale, gbc_lblMetaDataScale);
		GridBagConstraints gbc_textField_3 = new GridBagConstraints();
		gbc_textField_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_3.gridwidth = 1;
		gbc_textField_3.gridx = 1;
		gbc_textField_3.gridy = 11;
		textField_3 = new JTextField(m_dt.m_treeDrawer.LINE_WIDTH_SCALE + "");
		add(textField_3, gbc_textField_3);
		textField_3.setColumns(3);
		textField_3.addActionListener(ea-> {
				try {
					m_dt.m_treeDrawer.LINE_WIDTH_SCALE = Float.parseFloat(textField_3.getText());
					if (m_dt.m_settings.m_lineWidthMode!= LineWidthMode.DEFAULT) {
						m_dt.makeDirty();
					}
				} catch (Exception ex) {}
			});
		updateEnabled();

//      layout debugging
//		for (Component c : getComponents()) {
//			if (c instanceof JComponent) {
//				((JComponent)c).setBorder(BorderFactory.createLineBorder(Color.black));
//			}
//		}
		
		textField.setToolTipText(Util.formatToolTipAsHtml(HELP_PATTERN));
		textField_1.setToolTipText(Util.formatToolTipAsHtml(HELP_PATTERN));
		textField_3.setToolTipText(Util.formatToolTipAsHtml(HELP_SCALE));
		spinner.setToolTipText(Util.formatToolTipAsHtml(HELP_TOP));
		spinner_1.setToolTipText(Util.formatToolTipAsHtml(HELP_BOTTOM));
		lblNumberOfItem_1.setToolTipText(Util.formatToolTipAsHtml(HELP_TOP));
		lblNumberOfItem.setToolTipText(Util.formatToolTipAsHtml(HELP_BOTTOM));
		comboBoxBottom.setToolTipText(Util.formatToolTipAsHtml(HELP_LINE_WIDTH));
		comboBoxTop.setToolTipText(Util.formatToolTipAsHtml(HELP_LINE_WIDTH_BOTTOM));
		chckbxZeroBased.setToolTipText(Util.formatToolTipAsHtml(HELP_ZERO_BASED));
	}
			
	private void updateEnabled() {
		if (textField_1 == null) {
			return;
		}
		textField_3.setEnabled(m_dt.m_settings.m_lineWidthMode != LineWidthMode.DEFAULT);
		chckbxZeroBased.setEnabled(m_dt.m_settings.m_lineWidthMode != LineWidthMode.DEFAULT);
		
		textField_1.setEnabled(m_dt.m_settings.m_lineWidthMode == LineWidthMode.BY_METADATA_PATTERN);
		spinner.setEnabled(m_dt.m_settings.m_lineWidthMode == LineWidthMode.BY_METADATA_NUMBER);

		textField.setEnabled(false);
		spinner_1.setEnabled(false);
		if (m_dt.m_settings.m_lineWidthMode == LineWidthMode.DEFAULT) {
			comboBoxTop.setEnabled(false);
		} else {
			comboBoxTop.setEnabled(true);
			if (m_dt.m_settings.m_lineWidthModeTop == LineWidthMode.BY_METADATA_PATTERN) {
				textField.setEnabled(true);
			}
			if (m_dt.m_settings.m_lineWidthModeTop == LineWidthMode.BY_METADATA_NUMBER) {
				spinner_1.setEnabled(true);
			}
		}
	}
			

	@Override
	public void stateChanged(ChangeEvent e) {
		List<String> selection = new ArrayList<String>();
		List<String> selectionTop = new ArrayList<String>();
		selection.add(LineWidthMode.DEFAULT.toString());
		selectionTop.add(SAME_AS_BOTTOM);
		selectionTop.add(MAKE_FIT_BOTTOM);
		if (m_dt.m_treeData.m_bMetaDataReady) {
			selection.add(LineWidthMode.BY_METADATA_PATTERN.toString());
			selectionTop.add(LineWidthMode.BY_METADATA_PATTERN.toString());
			selection.add(LineWidthMode.BY_METADATA_NUMBER.toString());
			selectionTop.add(LineWidthMode.BY_METADATA_NUMBER.toString());			
			for (int i = 0; i < m_dt.m_settings.m_metaDataTags.size(); i++) {
				if (m_dt.m_settings.m_metaDataTypes.get(i).equals(MetaDataType.NUMERIC)) {
					selection.add(m_dt.m_settings.m_metaDataTags.get(i));				
					selectionTop.add(m_dt.m_settings.m_metaDataTags.get(i));			
				}
			}
		}
		ComboBoxModel<String> model = new DefaultComboBoxModel<>(selection.toArray(new String[0]));
		comboBoxBottom.setModel(model);
		
		model = new DefaultComboBoxModel<>(selectionTop.toArray(new String[0]));
		comboBoxTop.setModel(model);
		
		if (m_dt.m_settings.m_lineWidthMode == LineWidthMode.DEFAULT) {
			comboBoxBottom.setSelectedItem(LineWidthMode.DEFAULT.toString());
		} else if (m_dt.m_settings.m_lineWidthMode == LineWidthMode.BY_METADATA_PATTERN) {
			comboBoxBottom.setSelectedItem(LineWidthMode.BY_METADATA_PATTERN.toString());
		} else if (m_dt.m_settings.m_lineWidthMode == LineWidthMode.BY_METADATA_NUMBER) {
			comboBoxBottom.setSelectedItem(LineWidthMode.BY_METADATA_NUMBER.toString());
		} else {
			comboBoxBottom.setSelectedItem(m_dt.m_settings.m_lineWidthTag);
		}
		if (m_dt.m_settings.m_lineWidthModeTop == LineWidthMode.DEFAULT) {
			if (m_dt.m_settings.m_bCorrectTopOfBranch) {
				comboBoxTop.setSelectedItem(MAKE_FIT_BOTTOM);
			} else {
				comboBoxTop.setSelectedItem(SAME_AS_BOTTOM);				
			}
		} else if (m_dt.m_settings.m_lineWidthModeTop == LineWidthMode.BY_METADATA_PATTERN) {
			comboBoxTop.setSelectedItem(LineWidthMode.BY_METADATA_PATTERN.toString());
		} else if (m_dt.m_settings.m_lineWidthModeTop == LineWidthMode.BY_METADATA_NUMBER) {
			comboBoxTop.setSelectedItem(LineWidthMode.BY_METADATA_NUMBER.toString());
		} else {
			comboBoxTop.setSelectedItem(m_dt.m_settings.m_lineWidthTagTop);
		}

		updateEnabled();
	}
}
