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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import viz.DensiTree;
import viz.DensiTree.LineWidthMode;
import viz.DensiTree.MetaDataType;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JSpinner;
import javax.swing.border.TitledBorder;
import java.util.ArrayList;

import javax.swing.JComboBox;

public class LineWidthPanel extends JPanel implements ChangeListener {
	private static final long serialVersionUID = 1L;
	private JTextField textField_1;
	private JTextField textField_3;
	SpinnerNumberModel topOfBranchModel;
	SpinnerNumberModel bottomOfBranchModel;
	JComboBox comboBox;
	JPanel panel;
	
	DensiTree m_dt;
	
	public LineWidthPanel(DensiTree dt) {
		m_dt = dt;
		m_dt.addChangeListener(this);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[]{1.0, 0.0, 0.0};
		setLayout(gridBagLayout);
		
		comboBox = new JComboBox();
		stateChanged(null);
		comboBox.setPreferredSize(new Dimension(130,20));
		comboBox.setMaximumSize(new Dimension(130,200));
		comboBox.setSelectedItem(m_dt.m_lineWidthMode);
		
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						String selected = comboBox.getSelectedItem().toString();
						LineWidthMode oldMode = m_dt.m_lineWidthMode; 
						String oldTag = m_dt.m_lineWidthTag;
						if (selected.equals(LineWidthMode.DEFAULT.toString())) {
							m_dt.m_lineWidthMode = LineWidthMode.DEFAULT;
						} else if (selected.equals(LineWidthMode.BY_METADATA_PATTERN.toString())) {
							m_dt.m_lineWidthMode = LineWidthMode.BY_METADATA_PATTERN;
						} else if (selected.equals(LineWidthMode.BY_METADATA_NUMBER.toString())) {
							m_dt.m_lineWidthMode = LineWidthMode.BY_METADATA_NUMBER;
						} else {
							m_dt.m_lineWidthTag = selected; 
							m_dt.m_lineWidthMode = LineWidthMode.BY_METADATA_TAG;
						}
						m_dt.resetStyle();
//						txtPattern.setEnabled(m_dt.m_lineColorMode == LineColorMode.BY_METADATA_PATTERN);
//						chckbxShowLegend.setEnabled(m_dt.m_lineColorMode == LineColorMode.BY_METADATA_PATTERN 
//								|| m_dt.m_lineColorMode == LineColorMode.COLOR_BY_METADATA_TAG);
						if (m_dt.m_lineWidthMode != oldMode || m_dt.m_lineWidthTag != oldTag) {
							m_dt.calcLineWidths(true);
							m_dt.makeDirty();
						}
						textField_1.setEnabled(m_dt.m_lineWidthMode == LineWidthMode.BY_METADATA_PATTERN);
						panel.setVisible(m_dt.m_lineWidthMode == LineWidthMode.BY_METADATA_NUMBER);
						repaint();
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
		
		
		textField_1 = new JTextField(m_dt.m_sLineWidthPattern);
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.insets = new Insets(0, 0, 5, 0);
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 0;
		gbc_textField_1.gridy = 1;
		gbc_textField_1.gridwidth= 2;
		add(textField_1, gbc_textField_1);
		textField_1.setColumns(10);
		textField_1.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					m_dt.m_sLineWidthPattern = textField_1.getText();
					if (m_dt.m_lineWidthMode!= LineWidthMode.DEFAULT) {
						m_dt.calcLineWidths(true);
						m_dt.makeDirty();
					}
				} catch (Exception ex) {}
			}
		});
		textField_1.setEnabled(false);
		
		GridBagConstraints gbc_lblMetaDataScale = new GridBagConstraints();
		gbc_lblMetaDataScale.anchor = GridBagConstraints.EAST;
		gbc_lblMetaDataScale.insets = new Insets(0, 0, 5, 5);
		gbc_lblMetaDataScale.gridx = 0;
		gbc_lblMetaDataScale.gridy = 2;
		JLabel lblMetaDataScale =  new JLabel("Scale");
		add(lblMetaDataScale, gbc_lblMetaDataScale);
		GridBagConstraints gbc_textField_3 = new GridBagConstraints();
		gbc_textField_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_3.gridwidth = 1;
		gbc_textField_3.insets = new Insets(0, 0, 5, 5);
		gbc_textField_3.gridx = 1;
		gbc_textField_3.gridy = 2;
		textField_3 = new JTextField(m_dt.m_treeDrawer.LINE_WIDTH_SCALE + "");
		add(textField_3, gbc_textField_3);
		textField_3.setColumns(3);
		textField_3.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					m_dt.m_treeDrawer.LINE_WIDTH_SCALE = Float.parseFloat(textField_3.getText());
					if (m_dt.m_lineWidthMode!= LineWidthMode.DEFAULT) {
						m_dt.makeDirty();
					}
				} catch (Exception ex) {}
			}
		});
		bottomOfBranchModel = new SpinnerNumberModel(m_dt.m_iPatternForBottom, 1, 100, 1);
		topOfBranchModel = new SpinnerNumberModel(m_dt.m_iPatternForTop, 0, 100, 1);
		
		bottomOfBranchModel.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				m_dt.m_iPatternForBottom = (Integer) bottomOfBranchModel.getValue();
				if (m_dt.m_iPatternForBottom < 1) {
					m_dt.m_iPatternForBottom = 1;
				}
				if (m_dt.m_lineWidthMode!= LineWidthMode.DEFAULT) {
					m_dt.m_pattern = m_dt.createPattern();
					m_dt.calcLineWidths(true);
					m_dt.makeDirty();
				}
			}
		});
		
		topOfBranchModel.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				m_dt.m_iPatternForTop = (Integer) topOfBranchModel.getValue();
				if (m_dt.m_iPatternForTop < 0) {
					m_dt.m_iPatternForTop = 0;
				}
				if (m_dt.m_lineWidthMode!= LineWidthMode.DEFAULT) {
					m_dt.m_pattern = m_dt.createPattern();
					m_dt.calcLineWidths(true);
					m_dt.makeDirty();
				}
			}
		});
		
//				JLabel lblMetaDataPattern = new JLabel("pattern");
//				GridBagConstraints gbc_lblMetaDataPattern = new GridBagConstraints();
//				gbc_lblMetaDataPattern.anchor = GridBagConstraints.EAST;
//				gbc_lblMetaDataPattern.insets = new Insets(0, 0, 5, 5);
//				gbc_lblMetaDataPattern.gridx = 0;
//				gbc_lblMetaDataPattern.gridy = 4;
//				add(lblMetaDataPattern, gbc_lblMetaDataPattern);

		
		
		
		JCheckBox chckbxCorrectTopOf = new JCheckBox("Top correction");
		chckbxCorrectTopOf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_bCorrectTopOfBranch;
				m_dt.m_bCorrectTopOfBranch = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_bCorrectTopOfBranch) {// && m_dt.m_lineWidthMode!= LineWidthMode.DEFAULT) {
					m_dt.calcLineWidths(true);
					m_dt.makeDirty();
				}
			}
		});
		
		
		
		panel = new JPanel();
		panel.setVisible(false);
		panel.setBorder(new TitledBorder(null, "Number", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.gridwidth = 2;
		gbc_panel.insets = new Insets(0, 0, 5, 5);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 3;
		add(panel, gbc_panel);
		panel.setLayout(new GridBagLayout());
		
		JLabel lblNumberOfItem_1 = new JLabel("top");
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.anchor = GridBagConstraints.WEST;
		gbc_label.gridx = 0;
		gbc_label.gridy = 1;
		panel.add(lblNumberOfItem_1, gbc_label);
		JSpinner spinner = new JSpinner(bottomOfBranchModel);
		
				GridBagConstraints gbc_spinner = new GridBagConstraints();
				gbc_spinner.gridx = 1;
				gbc_spinner.gridy = 1;
				panel.add(spinner, gbc_spinner);
				spinner.setMaximumSize(new Dimension(1,20));
				
				JLabel lblNumberOfItem = new JLabel("bottom");
				GridBagConstraints gbc_label2 = new GridBagConstraints();
				gbc_label2.anchor = GridBagConstraints.WEST;
				gbc_label2.gridx = 0;
				gbc_label2.gridy = 2;
				panel.add(lblNumberOfItem, gbc_label2);
				JSpinner spinner_1 = new JSpinner(topOfBranchModel);
				GridBagConstraints gbc_spinner2 = new GridBagConstraints();
				gbc_spinner2.gridx = 1;
				gbc_spinner2.gridy = 2;
				panel.add(spinner_1, gbc_spinner2);
				spinner_1.setToolTipText("when 0, top will be egual to bottom");
				spinner_1.setMaximumSize(new Dimension(1,20));
		
		chckbxCorrectTopOf.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_chckbxCorrectTopOf = new GridBagConstraints();
		gbc_chckbxCorrectTopOf.anchor = GridBagConstraints.WEST;
		gbc_chckbxCorrectTopOf.gridwidth = 3;
		gbc_chckbxCorrectTopOf.gridx = 0;
		gbc_chckbxCorrectTopOf.gridy = 7;
		add(chckbxCorrectTopOf, gbc_chckbxCorrectTopOf);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		List<String> selection = new ArrayList<String>();
		selection.add(LineWidthMode.DEFAULT.toString());
		if (m_dt.m_bMetaDataReady) {
			selection.add(LineWidthMode.BY_METADATA_PATTERN.toString());
			selection.add(LineWidthMode.BY_METADATA_NUMBER.toString());
			for (int i = 0; i < m_dt.m_metaDataTags.size(); i++) {
				if (m_dt.m_metaDataTypes.get(i).equals(MetaDataType.NUMERIC)) {
					selection.add(m_dt.m_metaDataTags.get(i));				
				}
			}
		}
		ComboBoxModel model = new DefaultComboBoxModel(selection.toArray(new String[0]));
		comboBox.setModel(model);
		if (m_dt.m_lineWidthMode == LineWidthMode.DEFAULT) {
			comboBox.setSelectedItem(LineWidthMode.DEFAULT.toString());
		} else if (m_dt.m_lineWidthMode == LineWidthMode.BY_METADATA_PATTERN) {
			comboBox.setSelectedItem(LineWidthMode.BY_METADATA_PATTERN.toString());
		} else if (m_dt.m_lineWidthMode == LineWidthMode.BY_METADATA_NUMBER) {
			comboBox.setSelectedItem(LineWidthMode.BY_METADATA_NUMBER.toString());
		} else {
			comboBox.setSelectedItem(m_dt.m_lineWidthTag);
		}
	}
}
