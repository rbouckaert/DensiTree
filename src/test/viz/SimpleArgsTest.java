package test.viz;

import static org.fest.swing.edt.GuiActionRunner.execute;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.fest.swing.annotation.RunsInEDT;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.junit.testcase.FestSwingJUnitTestCase;
import org.junit.Test;

import viz.DensiTree;

public class SimpleArgsTest extends FestSwingJUnitTestCase {

	protected FrameFixture dtFrame;
	protected DensiTree densitree;

	@Override
	protected void onSetUp() {
		dtFrame = new FrameFixture(robot(), createNewEditor());
		dtFrame.show();
	}

	@RunsInEDT
	private static JFrame createNewEditor() {
		return execute(new GuiQuery<JFrame>() {
			@Override
			protected JFrame executeInEDT() throws Throwable {
				DensiTree densitree = DensiTree.startNew(new String[] {"examples/ape.trees"});
				JFrame frame = (JFrame) SwingUtilities.getRoot(densitree);
				return frame;
			}
		});
	}
	
	@Test
	public void simpleArgTest() throws Exception {
		// nothing to do, just test we get through initialisation
	}

}
