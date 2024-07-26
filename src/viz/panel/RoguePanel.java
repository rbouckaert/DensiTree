package viz.panel;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import viz.ccd.BitSet;
import viz.ccd.FilteredCCD;
import viz.ccd.RogueDetection;
import viz.ccd.AbstractCCD;
import viz.DensiTree;
import viz.Node;
import viz.ccd.CCD1;
import viz.ccd.Tree;
import viz.util.Util;

public class RoguePanel extends JPanel implements ChangeListener {
	final static String HELP_COMBO_BOX = "After calculating rogues (via the 'calc rogues' button) select \n"
			+ "the list is populated with rogues in decreasing order of impact on the entropy.\n"
			+ "Select an item to remove all parts of the tree that include the selected rogue + all of the\n"
			+ "rogues above.";
	final static String HELP_CALC_ROGUES = "Calculate rogue taxa and populate combobox above.\n"
			+ "This may take a while.";
	final static String HELP_REMOVE_ROGUES = "Remove unselected taxa from the tree set.";
	
		private static final long serialVersionUID = 1L;
		JComboBox<String> comboBoxBottom;
		JButton calcRoguesButton;
		JButton removeRoguesButton;
		
		DensiTree m_dt;
		
		public RoguePanel(DensiTree dt) {
			m_dt = dt;
			m_dt.addChangeListener(this);

			GridBagLayout gridBagLayout = new GridBagLayout();
			gridBagLayout.columnWidths = new int[]{0, 0};
			gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0};
			gridBagLayout.columnWeights = new double[]{1.0, 0.0};
			setLayout(gridBagLayout);

			comboBoxBottom = new JComboBox<>();
			ComboBoxModel<String> model = new DefaultComboBoxModel<>(new String[]{"<not initialised>"});
			comboBoxBottom.setModel(model);
			comboBoxBottom.setPreferredSize(new Dimension(130,20));
			//comboBoxBottom.setMaximumSize(new Dimension(130,200));
			comboBoxBottom.setSelectedItem(0);
			comboBoxBottom.addActionListener(e-> {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						unselectRogues();
					}
				});
			});
			GridBagConstraints gbc_comboBox = new GridBagConstraints();
			gbc_comboBox.gridwidth = 2;
			gbc_comboBox.insets = new Insets(0, 0, 5, 0);
			//gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_comboBox.gridx = 0;
			gbc_comboBox.gridy = 0;
			add(comboBoxBottom, gbc_comboBox);
			comboBoxBottom.setToolTipText(Util.formatToolTipAsHtml(HELP_COMBO_BOX));

			calcRoguesButton = new JButton("Calc rogues");
			GridBagConstraints gbc_calcRoguesButton = new GridBagConstraints();
			gbc_calcRoguesButton.fill = GridBagConstraints.HORIZONTAL;
			gbc_calcRoguesButton.insets = new Insets(0, 0, 5, 0);
			gbc_calcRoguesButton.gridx = 0;
			gbc_calcRoguesButton.gridy = 1;
			gbc_calcRoguesButton.gridwidth= 2;
			add(calcRoguesButton, gbc_calcRoguesButton);
			calcRoguesButton.addActionListener(e-> {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								calcRogues();
							}
						});
					});
			calcRoguesButton.setEnabled(false);
			calcRoguesButton.setToolTipText(Util.formatToolTipAsHtml(HELP_CALC_ROGUES));

			removeRoguesButton = new JButton("Remove rogues");
			GridBagConstraints gbc_removeRoguesButton = new GridBagConstraints();
			gbc_removeRoguesButton.fill = GridBagConstraints.HORIZONTAL;
			gbc_removeRoguesButton.insets = new Insets(0, 0, 5, 0);
			gbc_removeRoguesButton.gridx = 0;
			gbc_removeRoguesButton.gridy = 2;
			gbc_removeRoguesButton.gridwidth= 2;
			add(removeRoguesButton, gbc_removeRoguesButton);
			removeRoguesButton.addActionListener(e-> {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						removeRogues();
					}
				});
			});
			removeRoguesButton.setEnabled(false);
			removeRoguesButton.setToolTipText(Util.formatToolTipAsHtml(HELP_REMOVE_ROGUES));
		}

		
		private void unselectRogues() {
			// TODO Auto-generated method stub
			
		}
		
		
		private Object removeRogues() {
			// TODO Auto-generated method stub
			return null;
		}

		private Object calcRogues() {
			
			List<Tree> trees = new ArrayList<>();
			for (Node tree : m_dt.m_treeData.m_trees) {
				trees.add(new Tree(tree, m_dt.m_treeData));
			}
			CCD1 ccd = new CCD1(trees, 0);
			
	        List<viz.ccd.AbstractCCD> ccds = RogueDetection.detectRoguesWhileImproving(
	                ccd,
	                1,
	                RogueDetection.RogueDetectionStrategy.Entropy,
	                RogueDetection.TerminationStrategy.NumRogues
	                );
	        
			List<String> rogues = new ArrayList<>();
        	for (AbstractCCD ccdi : ccds) {
        		if (ccdi instanceof FilteredCCD) {
	        		BitSet mask = ((FilteredCCD) ccdi).getRemovedTaxaMask();
	                for (int j = mask.nextSetBit(0); j >= 0; j = mask.nextSetBit(j + 1)) {
	                	String taxon = ccdi.getSomeBaseTree().getID(j);
	                	rogues.add(taxon);
	                }
        		}
        	}
			ComboBoxModel<String> model = new DefaultComboBoxModel<>(rogues.toArray(new String[]{}));
			comboBoxBottom.setModel(model);

			removeRoguesButton.setEnabled(true);
			return null;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			// deal with m_dt state changes
			calcRoguesButton.setEnabled(m_dt.m_treeData.m_bMetaDataReady);
			if (!m_dt.m_treeData.m_bMetaDataReady) {
				removeRoguesButton.setEnabled(false);
				ComboBoxModel<String> model = new DefaultComboBoxModel<>(new String[]{"<not initialised>"});
				comboBoxBottom.setModel(model);
			}
		}
		
}
