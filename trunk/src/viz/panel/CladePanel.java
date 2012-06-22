package viz.panel;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.Insets;
import javax.swing.SwingConstants;
import javax.swing.JButton;

import viz.DensiTree;
import javax.swing.JToggleButton;
import javax.swing.JSpinner;
import javax.swing.JSeparator;

public class CladePanel extends JPanel {
	DensiTree m_dt;
	
	public CladePanel(DensiTree dt) {
		m_dt = dt;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JLabel btnDraw = new JLabel("draw");
		GridBagConstraints gbc_btnDraw = new GridBagConstraints();
		gbc_btnDraw.anchor = GridBagConstraints.EAST;
		gbc_btnDraw.gridwidth = 2;
		gbc_btnDraw.insets = new Insets(0, 0, 5, 0);
		//gbc_btnDraw.insets = new Insets(0, 0, 5, 0);
		gbc_btnDraw.gridx = 0;
		gbc_btnDraw.gridy = 1;
		add(btnDraw, gbc_btnDraw);
		
		JLabel btnText = new JLabel("text");
		GridBagConstraints gbc_btnText = new GridBagConstraints();
		gbc_btnText.anchor = GridBagConstraints.EAST;
		gbc_btnText.insets = new Insets(0, 0, 5, 0);
		gbc_btnText.gridx = 2;
		gbc_btnText.gridy = 1;
		add(btnText, gbc_btnText);
		
		JLabel btnslctdonly = new JLabel("<html>slctd<br>only</html>");
		GridBagConstraints gbc_btnslctdonly = new GridBagConstraints();
		gbc_btnslctdonly.insets = new Insets(0, 0, 5, 0);
		gbc_btnslctdonly.gridx = 3;
		gbc_btnslctdonly.gridy = 1;
		add(btnslctdonly, gbc_btnslctdonly);
		
		JLabel lblMean = new JLabel("Mean");
		lblMean.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblMean = new GridBagConstraints();
		gbc_lblMean.anchor = GridBagConstraints.EAST;
		gbc_lblMean.insets = new Insets(0, 0, 5, 0);
		gbc_lblMean.gridx = 0;
		gbc_lblMean.gridy = 2;
		add(lblMean, gbc_lblMean);
		
		JCheckBox chckbxMean = new JCheckBox("");
		GridBagConstraints gbc_chckbxMean = new GridBagConstraints();
		gbc_chckbxMean.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxMean.gridx = 1;
		gbc_chckbxMean.gridy = 2;
		add(chckbxMean, gbc_chckbxMean);
		
		JCheckBox checkBox = new JCheckBox("");
		checkBox.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_checkBox = new GridBagConstraints();
		gbc_checkBox.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox.gridx = 2;
		gbc_checkBox.gridy = 2;
		add(checkBox, gbc_checkBox);
		
		JCheckBox chckbxSelectionOnly = new JCheckBox("");
		chckbxSelectionOnly.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_chckbxSelectionOnly = new GridBagConstraints();
		gbc_chckbxSelectionOnly.anchor = GridBagConstraints.WEST;
		gbc_chckbxSelectionOnly.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxSelectionOnly.gridx = 3;
		gbc_chckbxSelectionOnly.gridy = 2;
		add(chckbxSelectionOnly, gbc_chckbxSelectionOnly);
		
		JLabel lblhpd = new JLabel("95%HPD");
		GridBagConstraints gbc_lblhpd = new GridBagConstraints();
		gbc_lblhpd.anchor = GridBagConstraints.EAST;
		gbc_lblhpd.insets = new Insets(0, 0, 5, 0);
		gbc_lblhpd.gridx = 0;
		gbc_lblhpd.gridy = 3;
		add(lblhpd, gbc_lblhpd);
		
		JCheckBox chckbxhpd = new JCheckBox("");
		GridBagConstraints gbc_chckbxhpd = new GridBagConstraints();
		gbc_chckbxhpd.anchor = GridBagConstraints.NORTH;
		gbc_chckbxhpd.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxhpd.gridx = 1;
		gbc_chckbxhpd.gridy = 3;
		add(chckbxhpd, gbc_chckbxhpd);
		
		JCheckBox checkBox_1 = new JCheckBox("");
		checkBox_1.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_checkBox_1 = new GridBagConstraints();
		gbc_checkBox_1.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox_1.gridx = 2;
		gbc_checkBox_1.gridy = 3;
		add(checkBox_1, gbc_checkBox_1);
		
		JCheckBox checkBox_3 = new JCheckBox("");
		checkBox_3.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_checkBox_3 = new GridBagConstraints();
		gbc_checkBox_3.anchor = GridBagConstraints.WEST;
		gbc_checkBox_3.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox_3.gridx = 3;
		gbc_checkBox_3.gridy = 3;
		add(checkBox_3, gbc_checkBox_3);
		
		JLabel lblSupport = new JLabel("Support");
		GridBagConstraints gbc_lblSupport = new GridBagConstraints();
		gbc_lblSupport.anchor = GridBagConstraints.EAST;
		gbc_lblSupport.insets = new Insets(0, 0, 5, 0);
		gbc_lblSupport.gridx = 0;
		gbc_lblSupport.gridy = 4;
		add(lblSupport, gbc_lblSupport);
		
		JCheckBox chckbxSupport = new JCheckBox("");
		GridBagConstraints gbc_chckbxSupport = new GridBagConstraints();
		gbc_chckbxSupport.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxSupport.gridx = 1;
		gbc_chckbxSupport.gridy = 4;
		add(chckbxSupport, gbc_chckbxSupport);
		
		JCheckBox checkBox_2 = new JCheckBox("");
		checkBox_2.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_checkBox_2 = new GridBagConstraints();
		gbc_checkBox_2.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox_2.gridx = 2;
		gbc_checkBox_2.gridy = 4;
		add(checkBox_2, gbc_checkBox_2);
		
		JCheckBox checkBox_4 = new JCheckBox("");
		checkBox_4.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_checkBox_4 = new GridBagConstraints();
		gbc_checkBox_4.anchor = GridBagConstraints.WEST;
		gbc_checkBox_4.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox_4.gridx = 3;
		gbc_checkBox_4.gridy = 4;
		add(checkBox_4, gbc_checkBox_4);
		
		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.gridwidth = 4;
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.insets = new Insets(0, 0, 5, 0);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 5;
		add(separator, gbc_separator);
		
		JLabel lblDigits = new JLabel("Sign. Digits");
		GridBagConstraints gbc_lblDigits = new GridBagConstraints();
		gbc_lblDigits.gridwidth = 3;
		gbc_lblDigits.anchor = GridBagConstraints.EAST;
		gbc_lblDigits.insets = new Insets(0, 0, 5, 0);
		gbc_lblDigits.gridx = 0;
		gbc_lblDigits.gridy = 6;
		add(lblDigits, gbc_lblDigits);
		
		JSpinner spinner = new JSpinner();
		GridBagConstraints gbc_spinner = new GridBagConstraints();
		gbc_spinner.anchor = GridBagConstraints.WEST;
		gbc_spinner.insets = new Insets(0, 0, 5, 0);
		gbc_spinner.gridx = 3;
		gbc_spinner.gridy = 6;
		add(spinner, gbc_spinner);
		
//		JButton btnT = new RoundedButton("");
//		GridBagConstraints gbc_btnT = new GridBagConstraints();
//		//gbc_btnT.insets = new Insets(0, 0, 5, 0);
//		gbc_btnT.gridx = 1;
//		gbc_btnT.gridy = 5;
//		add(btnT, gbc_btnT);
//		
//		JButton button = new RoundedButton("");
//		GridBagConstraints gbc_button = new GridBagConstraints();
//		//gbc_button.insets = new Insets(0, 0, 5, 0);
//		gbc_button.gridx = 2;
//		gbc_button.gridy = 5;
//		add(button, gbc_button);
//		
//		JButton btnT3 = new RoundedButton("");
//		GridBagConstraints gbc_btnT3 = new GridBagConstraints();
//		//gbc_btnT3.insets = new Insets(0, 0, 5, 0);
//		gbc_btnT3.gridx = 3;
//		gbc_btnT3.gridy = 5;
//		add(btnT3, gbc_btnT3);

		JButton btnFont = new RoundedButton("Font");
		GridBagConstraints gbc_btnFont = new GridBagConstraints();
		gbc_btnFont.insets = new Insets(0, 0, 5, 0);
		gbc_btnFont.gridx = 0;
		gbc_btnFont.gridy = 7;
		add(btnFont, gbc_btnFont);
		
		JButton btnColor = new RoundedButton("Color");
		GridBagConstraints gbc_btnColor = new GridBagConstraints();
		gbc_btnColor.gridwidth = 3;
		gbc_btnColor.insets = new Insets(0, 0, 5, 0);
		gbc_btnColor.gridx = 1;
		gbc_btnColor.gridy = 7;
		add(btnColor, gbc_btnColor);
	}
}
