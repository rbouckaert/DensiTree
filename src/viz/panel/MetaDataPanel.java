package viz.panel;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.Insets;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import viz.DensiTree;
import viz.graphics.BranchDrawer;
import viz.graphics.TrapeziumBranchDrawer;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class MetaDataPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;
	private JTextField textField_3;

	DensiTree m_dt;
	
	public MetaDataPanel(DensiTree dt) {
		m_dt = dt;
		GridBagLayout gridBagLayout = new GridBagLayout();
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
				m_dt.m_fTopLineWidth = null;
				m_dt.m_fTopCLineWidth = null;
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
		
		JLabel lblNumberOfItem = new JLabel("ID of item for bottom of branch");
		GridBagConstraints gbc_lblNumberOfItem = new GridBagConstraints();
		gbc_lblNumberOfItem.gridwidth = 2;
		gbc_lblNumberOfItem.insets = new Insets(0, 0, 5, 5);
		gbc_lblNumberOfItem.gridx = 0;
		gbc_lblNumberOfItem.gridy = 1;
		add(lblNumberOfItem, gbc_lblNumberOfItem);
		
		textField = new JTextField((m_dt.m_iPatternForBottom + 1) + "");
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 2;
		gbc_textField.gridy = 1;
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
					m_dt.m_iPatternForBottom = Integer.parseInt(textField.getText()) - 1;
					if (m_dt.m_iPatternForBottom < 0) {
						m_dt.m_iPatternForBottom = 0;
					}
					m_dt.m_pattern = m_dt.createPattern();
					m_dt.calcLines();
					m_dt.makeDirty();
				} catch (Exception e) {}
				
			}
			
		});
		
		JLabel lblNumberOfItem_1 = new JLabel("ID of item for top of branch");
		GridBagConstraints gbc_lblNumberOfItem_1 = new GridBagConstraints();
		gbc_lblNumberOfItem_1.gridwidth = 2; 
		gbc_lblNumberOfItem_1.anchor = GridBagConstraints.EAST;
		gbc_lblNumberOfItem_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNumberOfItem_1.gridx = 0;
		gbc_lblNumberOfItem_1.gridy = 2;
		add(lblNumberOfItem_1, gbc_lblNumberOfItem_1);
		
		textField_2 = new JTextField((m_dt.m_iPatternForTop + 1) + "");
		textField_2.setToolTipText("when 0, top will be egual to bottom");
		GridBagConstraints gbc_textField_2 = new GridBagConstraints();
		gbc_textField_2.insets = new Insets(0, 0, 5, 5);
		gbc_textField_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_2.gridx = 2;
		gbc_textField_2.gridy = 2;
		add(textField_2, gbc_textField_2);
		textField_2.setColumns(5);
		textField_2.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {update();}
			@Override
			public void insertUpdate(DocumentEvent e) {update();}
			@Override
			public void changedUpdate(DocumentEvent e) {update();}
			private void update() {
				try {
					m_dt.m_iPatternForTop = Integer.parseInt(textField_2.getText()) - 1;
					if (m_dt.m_iPatternForTop < 0) {
						m_dt.m_iPatternForTop = 0;
					}
					m_dt.m_pattern = m_dt.createPattern();
					m_dt.calcLines();
					m_dt.makeDirty();
				} catch (Exception e) {}
			}
		});
		
		GridBagConstraints gbc_lblMetaDataScale = new GridBagConstraints();
		gbc_lblMetaDataScale.gridwidth = 2;
		gbc_lblMetaDataScale.anchor = GridBagConstraints.EAST;
		gbc_lblMetaDataScale.insets = new Insets(0, 0, 5, 5);
		gbc_lblMetaDataScale.gridx = 0;
		gbc_lblMetaDataScale.gridy = 3;
		JLabel lblMetaDataScale =  new JLabel("Meta data scale");
		add(lblMetaDataScale, gbc_lblMetaDataScale);
		GridBagConstraints gbc_textField_3 = new GridBagConstraints();
		gbc_textField_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_3.gridwidth = 1;
		gbc_textField_3.insets = new Insets(0, 0, 5, 5);
		gbc_textField_3.gridx = 2;
		gbc_textField_3.gridy = 3;
		textField_3 = new JTextField(m_dt.m_treeDrawer.LINE_WIDTH_SCALE + "");
		add(textField_3, gbc_textField_3);
		textField_3.setColumns(5);
		textField_3.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {update();}
			@Override
			public void insertUpdate(DocumentEvent e) {update();}
			@Override
			public void changedUpdate(DocumentEvent e) {update();}
			private void update() {
				try {
					m_dt.m_treeDrawer.LINE_WIDTH_SCALE = Float.parseFloat(textField_3.getText());
					m_dt.makeDirty();
				} catch (Exception e) {}
			}
		});

		JLabel lblMetaDataPattern = new JLabel("Meta data pattern");
		GridBagConstraints gbc_lblMetaDataPattern = new GridBagConstraints();
		gbc_lblMetaDataPattern.anchor = GridBagConstraints.EAST;
		gbc_lblMetaDataPattern.insets = new Insets(0, 0, 5, 5);
		gbc_lblMetaDataPattern.gridx = 0;
		gbc_lblMetaDataPattern.gridy = 4;
		add(lblMetaDataPattern, gbc_lblMetaDataPattern);
		
		textField_1 = new JTextField(m_dt.m_sPattern);
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.gridwidth = 2;
		gbc_textField_1.insets = new Insets(0, 0, 5, 5);
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 1;
		gbc_textField_1.gridy = 4;
		add(textField_1, gbc_textField_1);
		textField_1.setColumns(10);
		textField_1.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {update();}
			@Override
			public void insertUpdate(DocumentEvent e) {update();}
			@Override
			public void changedUpdate(DocumentEvent e) {update();}
			private void update() {
				try {
					m_dt.m_sPattern = textField_1.getText();
					m_dt.m_pattern = m_dt.createPattern();
					m_dt.calcLines();
					m_dt.makeDirty();
				} catch (Exception e) {}
			}
		});

		
		
		
		JCheckBox chckbxCorrectTopOf = new JCheckBox("Correct top of branch");
		chckbxCorrectTopOf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_bCorrectTopOfBranch;
				m_dt.m_bCorrectTopOfBranch = ((JCheckBox) e.getSource()).isVisible();
				if (bPrev != m_dt.m_bCorrectTopOfBranch) {
					m_dt.calcLines();
					m_dt.makeDirty();
				}
			}
		});
		chckbxCorrectTopOf.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_chckbxCorrectTopOf = new GridBagConstraints();
		gbc_chckbxCorrectTopOf.anchor = GridBagConstraints.WEST;
		gbc_chckbxCorrectTopOf.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxCorrectTopOf.gridwidth = 3;
		gbc_chckbxCorrectTopOf.gridx = 0;
		gbc_chckbxCorrectTopOf.gridy = 5;
		add(chckbxCorrectTopOf, gbc_chckbxCorrectTopOf);
	}
}
