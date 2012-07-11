package viz.panel;

import javax.swing.JPanel;

import java.awt.Cursor;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import javax.swing.JTextField;

import viz.DensiTree;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BurninPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private JTextField textField;
	DensiTree m_dt;
	
	public BurninPanel(DensiTree dt) {
		m_dt = dt;
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JLabel lblBurnIn = new JLabel("Burn in");
		GridBagConstraints gbc_lblBurnIn = new GridBagConstraints();
		gbc_lblBurnIn.insets = new Insets(0, 0, 0, 5);
		gbc_lblBurnIn.anchor = GridBagConstraints.EAST;
		gbc_lblBurnIn.gridx = 0;
		gbc_lblBurnIn.gridy = 0;
		add(lblBurnIn, gbc_lblBurnIn);
		
		textField = new JTextField(m_dt.m_nBurnIn + "");
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		add(textField, gbc_textField);
		textField.setColumns(4);
		textField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					m_dt.m_nBurnIn = Integer.parseInt(textField.getText());
					//setCursor(new Cursor(Cursor.WAIT_CURSOR));
					m_dt.init(m_dt.m_sFileName);
					m_dt.calcLines();
					m_dt.fitToScreen();
					//setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				} catch (Exception e2) {}
			}
		});

	}
}
