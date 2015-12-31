package viz.panel;


import javax.swing.JPanel;

import java.awt.Color;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.Insets;

import javax.swing.JColorChooser;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.JButton;

import viz.DensiTree;
import viz.graphics.JFontChooser;
import viz.util.Util;

import javax.swing.JSpinner;
import javax.swing.JSeparator;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class CladePanel extends JPanel implements ChangeListener {
	public final static String HELP_SHOW_CLADES = "Show information for individual clades. This is only " +
			"activated when the drawing style is not default. The default style is not clade based, so " +
			"there is no information to position clade information.";
	public final static String HELP_SELECTED_ONLY = "Show information only for selected clades. Clades can " +
			"be selected by clicking them in the DensiTree, or selecting them from the clade-bar (at botom " +
			"of the screen).";
	public final static String HELP_MEAN = "Show mean height of clades as line or and/as text.";
	public final static String HELP_95HPD = "Show 95% highest probability density interval of the height of clades as bar and/or as text.";
	public final static String HELP_SUPPORT = "Show support of clade as cricle and/or as text. The support is the fraction of " +
			"trees in the tree set that contain the clade.";
	public final static String HELP_DIGITS = "Number of significant digits to show clade information as text.";
	public final static String HELP_FONT = "Font used to show clade information as text.";
	public final static String HELP_COLOR  = "Color used to show clade information as text.";

	
	private static final long serialVersionUID = 1L;
	DensiTree m_dt;
	
	SpinnerNumberModel significantDigitsModel;
	JCheckBox chckbxSelectionOnly = new JCheckBox("Selected only");
	JCheckBox chckbxShowClades = new JCheckBox("Show clades");
	JCheckBox chckbxMean = new JCheckBox("");
	JCheckBox checkBox = new JCheckBox("");
	JCheckBox chckbxhpd = new JCheckBox("");
	JCheckBox checkBox_1 = new JCheckBox("");
	JCheckBox chckbxSupport = new JCheckBox("");
	JCheckBox checkBox_2 = new JCheckBox("");
	JButton btnFont;
	JSpinner spinner;
	JButton btnColor;
	
	public CladePanel(DensiTree dt) {
		m_dt = dt;
		m_dt.addChangeListener(this);
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		chckbxSelectionOnly.setSelected(m_dt.m_cladeDrawer.m_bSelectedOnly);
		chckbxSelectionOnly.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_cladeDrawer.m_bSelectedOnly;
				m_dt.m_cladeDrawer.m_bSelectedOnly = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_cladeDrawer.m_bSelectedOnly) {
					m_dt.makeDirty();
				}				
			}
		});
		
		chckbxShowClades.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_bViewClades;
				m_dt.m_bViewClades = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_bViewClades) {
					m_dt.makeDirty();
				}
			}
		});
		GridBagConstraints gbc_chckbxShowClades = new GridBagConstraints();
		gbc_chckbxShowClades.anchor = GridBagConstraints.WEST;
		gbc_chckbxShowClades.gridwidth = 3;
		gbc_chckbxShowClades.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxShowClades.gridx = 0;
		gbc_chckbxShowClades.gridy = 0;
		add(chckbxShowClades, gbc_chckbxShowClades);
		chckbxSelectionOnly.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_chckbxSelectionOnly = new GridBagConstraints();
		gbc_chckbxSelectionOnly.gridwidth = 3;
		gbc_chckbxSelectionOnly.anchor = GridBagConstraints.WEST;
		gbc_chckbxSelectionOnly.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxSelectionOnly.gridx = 0;
		gbc_chckbxSelectionOnly.gridy = 1;
		add(chckbxSelectionOnly, gbc_chckbxSelectionOnly);
		
		JLabel btnDraw = new JLabel("draw");
		GridBagConstraints gbc_btnDraw = new GridBagConstraints();
		gbc_btnDraw.anchor = GridBagConstraints.EAST;
		gbc_btnDraw.gridwidth = 2;
		gbc_btnDraw.insets = new Insets(0, 0, 5, 0);
		//gbc_btnDraw.insets = new Insets(0, 0, 5, 0);
		gbc_btnDraw.gridx = 0;
		gbc_btnDraw.gridy = 2;
		add(btnDraw, gbc_btnDraw);
		
		JLabel btnText = new JLabel("text");
		GridBagConstraints gbc_btnText = new GridBagConstraints();
		gbc_btnText.insets = new Insets(0, 0, 5, 0);
		gbc_btnText.gridx = 2;
		gbc_btnText.gridy = 2;
		add(btnText, gbc_btnText);
		
		JLabel lblMean = new JLabel("Mean");
		lblMean.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblMean = new GridBagConstraints();
		gbc_lblMean.anchor = GridBagConstraints.EAST;
		gbc_lblMean.insets = new Insets(0, 0, 5, 0);
		gbc_lblMean.gridx = 0;
		gbc_lblMean.gridy = 3;
		add(lblMean, gbc_lblMean);
		
		chckbxMean.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_cladeDrawer.m_bDrawMean;
				m_dt.m_cladeDrawer.m_bDrawMean = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_cladeDrawer.m_bDrawMean) {
					m_dt.makeDirty();
				}				
			}
		});
		chckbxMean.setSelected(m_dt.m_cladeDrawer.m_bDrawMean);
		GridBagConstraints gbc_chckbxMean = new GridBagConstraints();
		gbc_chckbxMean.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxMean.gridx = 1;
		gbc_chckbxMean.gridy = 3;
		add(chckbxMean, gbc_chckbxMean);
		
		checkBox.setSelected(m_dt.m_cladeDrawer.m_bTextMean);
		checkBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_cladeDrawer.m_bTextMean;
				m_dt.m_cladeDrawer.m_bTextMean = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_cladeDrawer.m_bTextMean) {
					m_dt.makeDirty();
				}				
			}
		});
		checkBox.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_checkBox = new GridBagConstraints();
		gbc_checkBox.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox.gridx = 2;
		gbc_checkBox.gridy = 3;
		add(checkBox, gbc_checkBox);
		
		JLabel lblhpd = new JLabel("95%HPD");
		GridBagConstraints gbc_lblhpd = new GridBagConstraints();
		gbc_lblhpd.anchor = GridBagConstraints.EAST;
		gbc_lblhpd.insets = new Insets(0, 0, 5, 0);
		gbc_lblhpd.gridx = 0;
		gbc_lblhpd.gridy = 4;
		add(lblhpd, gbc_lblhpd);
		
		chckbxhpd.setSelected(m_dt.m_cladeDrawer.m_bDraw95HPD);
		chckbxhpd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_cladeDrawer.m_bDraw95HPD;
				m_dt.m_cladeDrawer.m_bDraw95HPD = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_cladeDrawer.m_bDraw95HPD) {
					m_dt.makeDirty();
				}				
			}
		});
		GridBagConstraints gbc_chckbxhpd = new GridBagConstraints();
		gbc_chckbxhpd.anchor = GridBagConstraints.NORTH;
		gbc_chckbxhpd.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxhpd.gridx = 1;
		gbc_chckbxhpd.gridy = 4;
		add(chckbxhpd, gbc_chckbxhpd);
		
		checkBox_1.setSelected(m_dt.m_cladeDrawer.m_bText95HPD);
		checkBox_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_cladeDrawer.m_bText95HPD;
				m_dt.m_cladeDrawer.m_bText95HPD = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_cladeDrawer.m_bText95HPD) {
					m_dt.makeDirty();
				}				
			}
		});
		checkBox_1.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_checkBox_1 = new GridBagConstraints();
		gbc_checkBox_1.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox_1.gridx = 2;
		gbc_checkBox_1.gridy = 4;
		add(checkBox_1, gbc_checkBox_1);
		
		
		JLabel lblSupport = new JLabel("Support");
		GridBagConstraints gbc_lblSupport = new GridBagConstraints();
		gbc_lblSupport.anchor = GridBagConstraints.EAST;
		gbc_lblSupport.insets = new Insets(0, 0, 5, 0);
		gbc_lblSupport.gridx = 0;
		gbc_lblSupport.gridy = 5;
		add(lblSupport, gbc_lblSupport);
		
		chckbxSupport.setSelected(m_dt.m_cladeDrawer.m_bDrawSupport);
		chckbxSupport.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_cladeDrawer.m_bDrawSupport;
				m_dt.m_cladeDrawer.m_bDrawSupport = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_cladeDrawer.m_bDrawSupport) {
					m_dt.makeDirty();
				}				
			}
		});
		GridBagConstraints gbc_chckbxSupport = new GridBagConstraints();
		gbc_chckbxSupport.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxSupport.gridx = 1;
		gbc_chckbxSupport.gridy = 5;
		add(chckbxSupport, gbc_chckbxSupport);
		
		checkBox_2.setSelected(m_dt.m_cladeDrawer.m_bTextSupport);
		checkBox_2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean bPrev = m_dt.m_cladeDrawer.m_bTextSupport;
				m_dt.m_cladeDrawer.m_bTextSupport = ((JCheckBox) e.getSource()).isSelected();
				if (bPrev != m_dt.m_cladeDrawer.m_bTextSupport) {
					m_dt.makeDirty();
				}				
			}
		});
		checkBox_2.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_checkBox_2 = new GridBagConstraints();
		gbc_checkBox_2.insets = new Insets(0, 0, 5, 0);
		gbc_checkBox_2.gridx = 2;
		gbc_checkBox_2.gridy = 5;
		add(checkBox_2, gbc_checkBox_2);
		
		
		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.gridwidth = 4;
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.insets = new Insets(0, 0, 5, 0);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 6;
		add(separator, gbc_separator);
		
		JLabel lblDigits = new JLabel("Sign. Digits");
		GridBagConstraints gbc_lblDigits = new GridBagConstraints();
		gbc_lblDigits.gridwidth = 2;
		gbc_lblDigits.anchor = GridBagConstraints.EAST;
		gbc_lblDigits.insets = new Insets(0, 0, 5, 0);
		gbc_lblDigits.gridx = 0;
		gbc_lblDigits.gridy = 7;
		add(lblDigits, gbc_lblDigits);
		
		significantDigitsModel = new SpinnerNumberModel(m_dt.m_cladeDrawer.m_nSignificantDigits, 0, 100, 1);
		significantDigitsModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				m_dt.m_cladeDrawer.m_nSignificantDigits = (Integer) significantDigitsModel.getValue();
				m_dt.makeDirty();
			}
		});
		
		btnFont = new RoundedButton("Font");
		btnFont.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFontChooser fontChooser = new JFontChooser();
				if (m_dt.m_cladeDrawer.m_font != null) {
					fontChooser.setSelectedFont(m_dt.m_cladeDrawer.m_font);
				}
				int result = fontChooser.showDialog(null);
				if (result == JFontChooser.OK_OPTION) {
					m_dt.m_cladeDrawer.m_font = fontChooser.getSelectedFont();
					m_dt.makeDirty();
					m_dt.repaint();
				}
			}
		});
		spinner = new JSpinner(significantDigitsModel);
		GridBagConstraints gbc_spinner = new GridBagConstraints();
		gbc_spinner.anchor = GridBagConstraints.WEST;
		gbc_spinner.insets = new Insets(0, 0, 5, 0);
		gbc_spinner.gridx = 2;
		gbc_spinner.gridy = 7;
		add(spinner, gbc_spinner);
		GridBagConstraints gbc_btnFont = new GridBagConstraints();
		gbc_btnFont.insets = new Insets(0, 0, 5, 0);
		gbc_btnFont.gridx = 0;
		gbc_btnFont.gridy = 8;
		add(btnFont, gbc_btnFont);
		
		btnColor = new RoundedButton("Color");
		btnColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(m_dt.m_Panel, getName(), m_dt.m_cladeDrawer.m_color);
				if (newColor != null) {
					m_dt.m_cladeDrawer.m_color = newColor;
					m_dt.makeDirty();
				}
				m_dt.repaint();
			}
		});
		GridBagConstraints gbc_btnColor = new GridBagConstraints();
		gbc_btnColor.gridwidth = 2;
		gbc_btnColor.insets = new Insets(0, 0, 5, 0);
		gbc_btnColor.gridx = 1;
		gbc_btnColor.gridy = 8;
		add(btnColor, gbc_btnColor);
		
		stateChanged(null);
		
		setToolTipText(Util.formatToolTipAsHtml(HELP_SELECTED_ONLY));
		chckbxSelectionOnly.setToolTipText(Util.formatToolTipAsHtml(HELP_SELECTED_ONLY));
		chckbxShowClades.setToolTipText(Util.formatToolTipAsHtml(HELP_SHOW_CLADES));
		chckbxMean.setToolTipText(Util.formatToolTipAsHtml(HELP_MEAN));
		chckbxhpd.setToolTipText(Util.formatToolTipAsHtml(HELP_95HPD));
		chckbxSupport.setToolTipText(Util.formatToolTipAsHtml(HELP_SUPPORT));
		checkBox.setToolTipText(Util.formatToolTipAsHtml(HELP_MEAN));
		checkBox_1.setToolTipText(Util.formatToolTipAsHtml(HELP_95HPD));
		checkBox_2.setToolTipText(Util.formatToolTipAsHtml(HELP_SUPPORT));
		btnFont.setToolTipText(Util.formatToolTipAsHtml(HELP_FONT));
		spinner.setToolTipText(Util.formatToolTipAsHtml(HELP_DIGITS));
		btnColor.setToolTipText(Util.formatToolTipAsHtml(HELP_COLOR));
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		this.setEnabled(m_dt.m_Xmode != 0);
		chckbxSelectionOnly.setEnabled(m_dt.m_Xmode != 0);
		chckbxShowClades.setEnabled(m_dt.m_Xmode != 0);
		chckbxMean.setEnabled(m_dt.m_Xmode != 0);
		checkBox.setEnabled(m_dt.m_Xmode != 0);
		chckbxhpd.setEnabled(m_dt.m_Xmode != 0);
		checkBox_1.setEnabled(m_dt.m_Xmode != 0);
		chckbxSupport.setEnabled(m_dt.m_Xmode != 0);
		checkBox_2.setEnabled(m_dt.m_Xmode != 0);
		btnFont.setEnabled(m_dt.m_Xmode != 0);;
		spinner.setEnabled(m_dt.m_Xmode != 0);;
		btnColor.setEnabled(m_dt.m_Xmode != 0);;
	}
}
