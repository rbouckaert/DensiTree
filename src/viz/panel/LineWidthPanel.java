package viz.panel;


import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.Insets;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import viz.DensiTree;
import viz.graphics.BranchDrawer;
import viz.graphics.TrapeziumBranchDrawer;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JSpinner;
import javax.swing.border.TitledBorder;
import java.awt.GridLayout;

public class LineWidthPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTextField textField_1;
	private JTextField textField_3;
	SpinnerNumberModel topOfBranchModel;
	SpinnerNumberModel bottomOfBranchModel;
	
	DensiTree m_dt;
	
	public LineWidthPanel(DensiTree dt) {
		m_dt = dt;
		GridBagLayout gridBagLayout = new GridBagLayout();
//		gridBagLayout.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 0.0};
//		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
//		gridBagLayout.columnWeights = new double[]{1.0, 0.0, 0.0};
//		gridBagLayout.columnWidths = new int[]{0, 0, 0};
//		gridBagLayout.columnWidths = new int[]{0, 0, 0};
//		gridBagLayout.rowHeights = new int[]{0, 0, 156, 0, 0};
//		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
//		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JCheckBox chckbxNewCheckBox = new JCheckBox("Use metadata for line width");
		chckbxNewCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				boolean bPrev = m_dt.m_bMetaDataForLineWidth;
				JCheckBox useMetaBox = (JCheckBox) e.getSource();
				m_dt.m_bMetaDataForLineWidth = useMetaBox.isSelected();
			if (bPrev != m_dt.m_bMetaDataForLineWidth) {
				if (m_dt.m_bMetaDataForLineWidth) {
					m_dt.m_treeDrawer.setBranchDrawer(new TrapeziumBranchDrawer());
				} else {
					m_dt.m_treeDrawer.setBranchDrawer(new BranchDrawer());
				}
				m_dt.m_fLineWidth = null;
				m_dt.m_fCLineWidth = null;
				m_dt.m_fRLineWidth = null;
				m_dt.m_fTopLineWidth = null;
				m_dt.m_fTopCLineWidth = null;
				m_dt.m_fRTopLineWidth = null;
				m_dt.calcLines();
				m_dt.makeDirty();
			}

			}
		});
		chckbxNewCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.gridwidth = 2;
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.gridx = 0;
		gbc_chckbxNewCheckBox.gridy = 0;
		add(chckbxNewCheckBox, gbc_chckbxNewCheckBox);
		
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(null, "ID of item", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.gridwidth = 2;
		gbc_panel.insets = new Insets(0, 0, 5, 5);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 1;
		add(panel, gbc_panel);
		panel.setLayout(new GridLayout(0, 2, 0, 0));
		
		JLabel lblNumberOfItem_1 = new JLabel("top of branch");
		panel.add(lblNumberOfItem_1);
		bottomOfBranchModel = new SpinnerNumberModel(m_dt.m_iPatternForBottom + 1, 1, 100, 1);
		JSpinner spinner = new JSpinner(bottomOfBranchModel);
		panel.add(spinner);
		spinner.setMaximumSize(new Dimension(30,20));
		
		JLabel lblNumberOfItem = new JLabel("bottom of branch");
		panel.add(lblNumberOfItem);
		topOfBranchModel = new SpinnerNumberModel(m_dt.m_iPatternForTop + 1, 0, 100, 1);
		JSpinner spinner_1 = new JSpinner(topOfBranchModel);
		panel.add(spinner_1);
		spinner_1.setToolTipText("when 0, top will be egual to bottom");
		spinner_1.setMaximumSize(new Dimension(3,20));
		
		bottomOfBranchModel.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				m_dt.m_iPatternForBottom = (Integer) bottomOfBranchModel.getValue();
				if (m_dt.m_iPatternForBottom < 0) {
					m_dt.m_iPatternForBottom = 0;
				}
				if (m_dt.m_bMetaDataForLineWidth) {
					m_dt.m_pattern = m_dt.createPattern();
					m_dt.calcLines();
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
				if (m_dt.m_bMetaDataForLineWidth) {
					m_dt.m_pattern = m_dt.createPattern();
					m_dt.calcLines();
					m_dt.makeDirty();
				}
			}
		});
		
		GridBagConstraints gbc_lblMetaDataScale = new GridBagConstraints();
		gbc_lblMetaDataScale.anchor = GridBagConstraints.EAST;
		gbc_lblMetaDataScale.insets = new Insets(0, 0, 5, 5);
		gbc_lblMetaDataScale.gridx = 0;
		gbc_lblMetaDataScale.gridy = 2;
		JLabel lblMetaDataScale =  new JLabel("Meta data scale");
		add(lblMetaDataScale, gbc_lblMetaDataScale);
				GridBagConstraints gbc_textField_3 = new GridBagConstraints();
				gbc_textField_3.fill = GridBagConstraints.HORIZONTAL;
				gbc_textField_3.gridwidth = 1;
				gbc_textField_3.insets = new Insets(0, 0, 5, 5);
				gbc_textField_3.gridx = 1;
				gbc_textField_3.gridy = 2;
				textField_3 = new JTextField(m_dt.m_treeDrawer.LINE_WIDTH_SCALE + "");
				add(textField_3, gbc_textField_3);
				textField_3.setColumns(5);
				textField_3.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							m_dt.m_treeDrawer.LINE_WIDTH_SCALE = Float.parseFloat(textField_3.getText());
							if (m_dt.m_bMetaDataForLineWidth) {
								m_dt.makeDirty();
							}
						} catch (Exception ex) {}
					}
				});
		
				JLabel lblMetaDataPattern = new JLabel("Meta data pattern");
				GridBagConstraints gbc_lblMetaDataPattern = new GridBagConstraints();
				gbc_lblMetaDataPattern.anchor = GridBagConstraints.EAST;
				gbc_lblMetaDataPattern.insets = new Insets(0, 0, 5, 5);
				gbc_lblMetaDataPattern.gridx = 0;
				gbc_lblMetaDataPattern.gridy = 3;
				add(lblMetaDataPattern, gbc_lblMetaDataPattern);
		
		textField_1 = new JTextField(m_dt.m_sPattern);
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.gridwidth = 3;
		gbc_textField_1.insets = new Insets(0, 0, 5, 0);
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 0;
		gbc_textField_1.gridy = 4;
		add(textField_1, gbc_textField_1);
		textField_1.setColumns(10);
		textField_1.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					m_dt.m_sPattern = textField_1.getText();
					m_dt.m_pattern = m_dt.createPattern();
					if (m_dt.m_bMetaDataForLineWidth) {
						m_dt.calcLines();
						m_dt.makeDirty();
					}
				} catch (Exception ex) {}
			}
		});

		
		
		
		JCheckBox chckbxCorrectTopOf = new JCheckBox("Correct top of branch");
		chckbxCorrectTopOf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_bCorrectTopOfBranch;
				m_dt.m_bCorrectTopOfBranch = ((JCheckBox) e.getSource()).isVisible();
				if (bPrev != m_dt.m_bCorrectTopOfBranch && m_dt.m_bMetaDataForLineWidth) {
					m_dt.calcLines();
					m_dt.makeDirty();
				}
			}
		});
		chckbxCorrectTopOf.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_chckbxCorrectTopOf = new GridBagConstraints();
		gbc_chckbxCorrectTopOf.anchor = GridBagConstraints.WEST;
		gbc_chckbxCorrectTopOf.gridwidth = 3;
		gbc_chckbxCorrectTopOf.gridx = 0;
		gbc_chckbxCorrectTopOf.gridy = 5;
		add(chckbxCorrectTopOf, gbc_chckbxCorrectTopOf);
	}
}
