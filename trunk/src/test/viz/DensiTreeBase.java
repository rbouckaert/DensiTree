package test.viz;


import static org.fest.swing.edt.GuiActionRunner.execute;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.fest.swing.annotation.RunsInEDT;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.junit.testcase.FestSwingJUnitTestCase;

import viz.DensiTree;


/**
 * Basic test methods for DensiTree  
 * 
 */
public class DensiTreeBase extends FestSwingJUnitTestCase {

	protected FrameFixture dtFrame;
	protected DensiTree densitree;

	protected void onSetUp() {
		dtFrame = new FrameFixture(robot(), createNewEditor());
		dtFrame.show();
	}

	@RunsInEDT
	private static JFrame createNewEditor() {
		return execute(new GuiQuery<JFrame>() {
			protected JFrame executeInEDT() throws Throwable {
				DensiTree densitree = DensiTree.startNew(new String[] {});
				JFrame frame = (JFrame) SwingUtilities.getRoot(densitree);
				return frame;
			}
		});
	}


}