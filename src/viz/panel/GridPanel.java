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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import viz.DensiTree;
import viz.DensiTree.GridMode;
import viz.graphics.JFontChooser;
import javax.swing.BoxLayout;
import javax.swing.border.LineBorder;

public class GridPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JTextField m_offsetTextField;
	DensiTree m_dt;
	ButtonGroup m_modeGroup = new ButtonGroup();

	/**
	 * Create the panel.
	 */
	public GridPanel(DensiTree dt) {
		//setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		m_dt = dt;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{10, 20, 0, 0};
		gridBagLayout.rowWeights = new double[]{1.0, 0.0, 0.0, 0.0, 0.0};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, 0.0, 0.0};
		setLayout(gridBagLayout);
				
				JPanel panel = new JPanel();
				panel.setBorder(new LineBorder(Color.gray));
				GridBagConstraints gbc_panel = new GridBagConstraints();
				gbc_panel.weightx = 0.95;
				gbc_panel.gridwidth = 2;
				gbc_panel.gridheight = 3;
				gbc_panel.insets = new Insets(0, 0, 5, 5);
				gbc_panel.fill = GridBagConstraints.BOTH;
				gbc_panel.gridx = 0;
				gbc_panel.gridy = 1;
				add(panel, gbc_panel);
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				
				JRadioButton rdbtnNewRadioButton_1 = new JRadioButton("No grid");
				panel.add(rdbtnNewRadioButton_1);
				rdbtnNewRadioButton_1.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						m_dt.m_nGridMode = GridMode.NONE;
						m_dt.makeDirty();
					}
				});
				
						m_modeGroup.add(rdbtnNewRadioButton_1);
						
								JRadioButton rdbtnNewRadioButton = new JRadioButton("Short grid");
								panel.add(rdbtnNewRadioButton);
								rdbtnNewRadioButton.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										m_dt.m_nGridMode = GridMode.SHORT;
										m_dt.makeDirty();
									}
								});
								m_modeGroup.add(rdbtnNewRadioButton);
								
										JRadioButton radioButton_2 = new JRadioButton("Full grid");
										panel.add(radioButton_2);
										radioButton_2.addActionListener(new ActionListener() {
											public void actionPerformed(ActionEvent e) {
												m_dt.m_nGridMode = GridMode.FULL;
												m_dt.makeDirty();
											}
										});
										m_modeGroup.add(radioButton_2);
										m_modeGroup.setSelected(rdbtnNewRadioButton_1.getModel(), true);
								
										JCheckBox reverseGrid = new JCheckBox("Reverse");
										GridBagConstraints c5 = new GridBagConstraints();
										c5.insets = new Insets(0, 0, 5, 0);
										c5.gridx = 2; 
										c5.gridy = 1; 
										c5.weightx = 0.5; 
										c5.gridwidth = 2; 
										c5.fill = GridBagConstraints.HORIZONTAL;
										add(reverseGrid, c5);
										reverseGrid.addActionListener(new ActionListener() {
											@Override
											public void actionPerformed(ActionEvent e) {
												JCheckBox button = (JCheckBox) e.getSource();
												m_dt.m_bReverseGrid = button.isSelected();
												m_dt.m_Panel.clearImage();
												m_dt.repaint();
											}
										});
								
										JButton btnGridFont = new RoundedButton("Font");
										GridBagConstraints c7 = new GridBagConstraints();
										c7.gridx = 2; 
										c7.gridy = 2; 
										c7.weightx = 0.5;
										c7.insets = new Insets(3, 3, 5, 5); 
										c7.fill = GridBagConstraints.HORIZONTAL;
										add(btnGridFont, c7);
										btnGridFont.addActionListener(new ActionListener() {
											
											@Override
											public void actionPerformed(ActionEvent ae) {
												JFontChooser fontChooser = new JFontChooser();
												if (m_dt.m_gridfont != null) {
													fontChooser.setSelectedFont(m_dt.m_gridfont);
												}
												int result = fontChooser.showDialog(null);
												if (result == JFontChooser.OK_OPTION) {
													m_dt.m_gridfont = fontChooser.getSelectedFont();
													m_dt.makeDirty();
													m_dt.repaint();
												}
											} // actionPerformed
										});
						
								JButton btnGridColor = new RoundedButton("Color");
								GridBagConstraints c6 = new GridBagConstraints();
								c6.gridx = 2; 
								c6.gridy = 3; 
								c6.weightx = 0.5;
								c6.insets = new Insets(3, 3, 5, 5); 
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
						
						
						//JPanel panel2 = new JPanel();
						//add(panel2, c);

						JLabel lblOffset = new JLabel("Offset");
						GridBagConstraints c4 = new GridBagConstraints();
						c4.insets = new Insets(0, 0, 0, 5);
						c4.gridx = 0; 
						c4.gridy = 4; 
						c4.weightx = 0.5; 
						c4.gridwidth = 1;
						c4.fill = GridBagConstraints.HORIZONTAL;
						add(lblOffset, c4);
								
										m_offsetTextField = new JTextField();
										m_offsetTextField.setText(m_dt.m_fGridOffset+"");
										GridBagConstraints c8 = new GridBagConstraints();
										c8.fill = GridBagConstraints.HORIZONTAL;
										c8.gridwidth = 2;
										c8.insets = new Insets(0, 0, 0, 5);
										c8.gridx = 1; 
										c8.gridy = 4;
										add(m_offsetTextField, c8);
										m_offsetTextField.setColumns(5);
								m_offsetTextField.getDocument().addDocumentListener(new DocumentListener() {
									@Override
									public void removeUpdate(DocumentEvent e) {updateOffset();}
									
									@Override
									public void insertUpdate(DocumentEvent e) {updateOffset();}
									
									@Override
									public void changedUpdate(DocumentEvent e) {updateOffset();}

									private void updateOffset() {
										try {
											m_dt.m_fGridOffset = Float.parseFloat(m_offsetTextField.getText());
											m_dt.m_Panel.clearImage();
											m_dt.repaint();
										} catch (NumberFormatException e) {
											// ignore
										}
									}

								});
	}

}
