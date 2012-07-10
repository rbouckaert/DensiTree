package test.viz;

import static org.fest.swing.finder.JFileChooserFinder.findFileChooser;

import java.io.File;


import org.fest.swing.fixture.JFileChooserFixture;
import org.junit.Test;

public class DensiTreeSimpleTest extends DensiTreeBase {

	
	@Test
	public void simpleTest() throws Exception {
		dtFrame.menuItemWithPath("File", "Load").click();
		JFileChooserFixture fileChooser = findFileChooser().using(robot());
		fileChooser.setCurrentDirectory(new File(""));
		fileChooser.selectFile(new File("ape.trees")).approve();
	}

}
