package test.viz;


import static org.fest.swing.finder.JFileChooserFinder.findFileChooser;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;

import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.image.ScreenshotTaker;
import org.junit.Test;

import viz.DensiTree;
import viz.panel.BurninPanel;
import viz.panel.CladePanel;
import viz.panel.ColorPanel;
import viz.panel.ExpandablePanel;
import viz.panel.GeoPanel;
import viz.panel.GridPanel;
import viz.panel.LabelPanel;
import viz.panel.LineWidthPanel;
import viz.panel.ShowPanel;
import viz.util.Util;

public class DocumentationCreator extends DensiTreeBase {

	@Test
	public void simpleTest() throws Exception {
		dtFrame.menuItemWithPath("File", "Load").click();
		JFileChooserFixture fileChooser = findFileChooser().using(robot());
		fileChooser.setCurrentDirectory(new File("examples"));
		fileChooser.selectFile(new File("ape.trees")).approve();
		
		Thread.sleep(3000);
		
		ScreenshotTaker screenshotTaker = new ScreenshotTaker();
		(new File("doc/screenshots/ShowPanel.png")).delete();
		(new File("doc/screenshots/GridPanel.png")).delete();
		(new File("doc/screenshots/LabelPanel.png")).delete();
		(new File("doc/screenshots/GeoPanel.png")).delete();
		(new File("doc/screenshots/LineWidthPanel.png")).delete();
		(new File("doc/screenshots/ColorPanel.png")).delete();
		(new File("doc/screenshots/BurninPanel.png")).delete();
		(new File("doc/screenshots/CladePanel.png")).delete();
		
		(new File("doc/screenshots/densitree.png")).delete();

		screenshotTaker.saveComponentAsPng(dtFrame.target, "doc/screenshots/densitree.png");		

		PrintStream out = new PrintStream(new File("doc/panels.tex"));
		createScreenShotOf("ShowPanel", screenshotTaker, out);
		createScreenShotOf("GridPanel", screenshotTaker, out);
		createScreenShotOf("LabelPanel", screenshotTaker, out);
		createScreenShotOf("GeoPanel", screenshotTaker, out);
		createScreenShotOf("LineWidthPanel", screenshotTaker, out);
		createScreenShotOf("ColorPanel", screenshotTaker, out);
		createScreenShotOf("BurninPanel", screenshotTaker, out);
		createScreenShotOf("CladePanel", screenshotTaker, out);

	}

	private void createScreenShotOf(String string, ScreenshotTaker screenshotTaker, PrintStream out) throws Exception {
		dtFrame.button(string + "Button").click();
		screenshotTaker.saveComponentAsPng(dtFrame.panel(string).target, "doc/screenshots/" + string + ".png");		
		dtFrame.button(string + "Button").click();
		createDocumentation(((ExpandablePanel) dtFrame.panel(string).panel().target).m_panel, out);
		
	}

	void createDocumentation(PrintStream out) throws Exception {
		DensiTree dt = new DensiTree();
		
		createDocumentation(new ShowPanel(dt), out);
		createDocumentation(new GridPanel(dt), out);
		createDocumentation(new LabelPanel(dt), out);
		createDocumentation(new GeoPanel(dt), out);
		createDocumentation(new LineWidthPanel(dt), out);
		createDocumentation(new ColorPanel(dt), out);
		createDocumentation(new BurninPanel(dt), out);
		createDocumentation(new CladePanel(dt), out);
	}
	
	void createDocumentation(Object o, PrintStream out) throws Exception {
		String _class = o.getClass().getName();
		_class = _class.substring(_class.lastIndexOf('.') + 1);
		_class = _class.replace("Panel", " Panel");
		out.println("\\section{" + _class + "}");
		out.println("\\begin{center}");
		out.println("\\includegraphics[width=0.4\\textwidth]{screenshots/" + _class.replaceAll(" ", "") + "}");
		out.println("\\end{center}");
		
		Field [] fields = o.getClass().getFields();
		for (Field field : fields) {
			String name = field.getName();
			if (name.startsWith("HELP_")) {
				name = name.substring(4).toLowerCase();
				for (int i = 0; i < name.length(); i++) {
					if (name.charAt(i) == '_') {
						i++;
						out.print(' ');
						char a = name.charAt(i);
						if (a >= 'a' && a <= 'z') {
							a = (char) (a+'A'-'a');
						}
						out.print(a);
					} else {
						out.print(name.charAt(i));
					}
				}
				
				out.println(": ");
				out.println(Util.formatToolTipAsTeX(field.get(o).toString()));
				out.println();
				
			}
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		DocumentationCreator dc = new DocumentationCreator();
		dc.createDocumentation(System.out);
		System.err.println("Run as junit test to create screen shots");
	}

}
