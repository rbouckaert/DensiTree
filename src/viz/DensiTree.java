/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
/*
 * DensiTree.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 - 2026 
 */

package viz;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import viz.GridDrawer.GridMode;
import viz.graphics.*;
import viz.fxpanel.BurninPanel;
import viz.fxpanel.CladePanel;
import viz.fxpanel.ColorPanel;
import viz.fxpanel.ExpandablePanel;
import viz.fxpanel.GeoPanel;
import viz.fxpanel.GridPanel;
import viz.fxpanel.LabelPanel;
import viz.fxpanel.LineWidthPanel;
import viz.fxpanel.RoguePanel;
import viz.fxpanel.ShowPanel;
import viz.util.Util;

public class DensiTree extends BorderPane {
	final static String VERSION = "3.2.0";
	final static String FRAME_TITLE = "DensiTree - Tree Set Visualizer";
	final static String CITATION = "Remco R. Bouckaert & Joseph Heled\n" +
			"DensiTree 2: Seeing Trees Through the Forest\n" +
			"bioRxiv, 2014,\n" +
			"http://dx.doi.org/10.1101/012401\n";
	public static int instances = 0;

	public Settings m_settings = new Settings();
	public TreeData m_treeData = new TreeData(this, m_settings);
	public TreeData m_treeData2;

	public int[] m_mirrorCladeToIDMap = null, m_cladeToIDMap = null;
	static public float GEO_OFFSET = 3.0f;

	public String m_sOptFile = null;
	public int m_iOptTree = -1;
	public Node m_optTree = null;
	public String m_sOptTree = null;

	static int B = 1;
	public Stage stage;

	public static final String ICONPATH = "viz/icons/";
	final static double DEFAULT_LENGTH = 0.001f;

	public float m_fHeight = 0;
	float m_fScaleX = 10;
	float m_fScaleY = 10;
	float m_fScaleGX = 10;
	float m_fScaleGY = 10;
	float m_fScale = 1.0f;
	public float m_fUserScale = 1.0f;

	public float m_fTreeOffset = 0;
	public float m_fTreeScale = 1;

	public GridDrawer m_gridDrawer;
	public CladeDrawer m_cladeDrawer;

	boolean m_bLeafCladeSelection = false;
	boolean m_bInitializing;

	public Rectangle[] m_bLabelRectangle;
	public Rectangle[] m_bGeoRectangle;
	public Rectangle m_nSelectedRect = null;

	public int m_nBurnIn = 10;
	public int m_nThin = 1;
	public boolean m_bBurnInIsPercentage = true;

	public double m_w = 0;

	public static int HEIGHTCOLOR = 6,
			CONSCOLOR = 4,
			LABELCOLOR = 5,
			BGCOLOR = 7,
			GEOCOLOR = 8,
			ROOTCANALCOLOR = 9;

	public BufferedImage m_bgImage;
	double[] m_fBGImageBox = { -180, -90, 180, 90 };
	BufferedImage m_rotate;
	final static String DEFAULT_PATTERN = ".*location=\"([^\"]*).*";

	public String m_sFileName;
	public String m_sFileName2;
	String m_asPDF = null, m_cladeComparisonAsPDF = null;
	Thread thread = null;

	private boolean isExporting = false;
	boolean isExporting() {
		return isExporting;
	}

	public void setWaitCursor() {
		Platform.runLater(() -> {
			if (stage != null && stage.getScene() != null) {
				stage.getScene().setCursor(Cursor.WAIT);
			}
		});
	}

	public void setDefaultCursor() {
		Platform.runLater(() -> {
			if (stage != null && stage.getScene() != null) {
				stage.getScene().setCursor(Cursor.DEFAULT);
			}
		});
	}

	/* Swing components wrapped inside SwingNodes */
	public TreeSetPanel m_Panel;
	public CladeSetComparisonPanel m_cladeSetComparisonPanel;
	public SwingNode m_panelSwingNode = new SwingNode();
	public SwingNode m_cladeComparisonSwingNode = new SwingNode();

	/* JavaFX UI Components */
	public ScrollPane m_jScrollPane;
	public MenuBar m_menuBar;
	public ToolBar m_tbTools = new ToolBar();
	public VBox m_tbTools2 = new VBox();
	public BorderPane m_cladeToolsPane = new BorderPane();
	public Label m_jStatusBar = new Label("Status bar");

	private SplitPane m_mainSplitPane;
	private SplitPane m_centerSplitPane;

	public Font m_font = new Font("sansserif", Font.PLAIN, 12);
	public boolean m_bAlignLabels = false;

	public boolean m_bViewCTrees = false;
	public boolean m_bViewAllTrees = true;
	public double m_fExponent = 1.0;

	boolean m_bAnimateOverwrite = false;
	int m_iAnimateTree;
	int m_nAnimationDelay = 100;
	boolean m_bAutoRefresh = true;
	boolean m_bIsDirty = true;

	public enum ViewMode {
		DRAW, ANIMATE, BROWSE
	}

	public ViewMode m_viewMode = ViewMode.DRAW;
	public TreeDrawer m_treeDrawer = new TreeDrawer();
	int m_nStyle = 0;

	public CheckMenuItem m_viewEditTree;
	public CheckMenuItem m_viewClades;

	public DensiTree() {
		m_gridDrawer = new GridDrawer(this);
		m_cladeDrawer = new CladeDrawer(this);
		instances++;

		m_treeData.m_bSelection = new boolean[0];
		m_settings.m_nRevOrder = new int[0];
		m_treeData.m_cTrees = new Node[0];
		m_treeData.m_trees = new Node[0];
		initColors();

		m_Panel = new TreeSetPanel(this);
		m_cladeSetComparisonPanel = new CladeSetComparisonPanel(this);

		SwingUtilities.invokeLater(() -> {
			m_panelSwingNode.setContent(m_Panel);
			m_cladeComparisonSwingNode.setContent(m_cladeSetComparisonPanel);
		});

		m_jScrollPane = new ScrollPane(m_panelSwingNode);
		m_jScrollPane.setPannable(true);
		m_jScrollPane.setFitToWidth(false);
		m_jScrollPane.setFitToHeight(false);

		makeToolbar();
		m_menuBar = makeMenuBar();

		m_centerSplitPane = new SplitPane();
		m_centerSplitPane.setOrientation(Orientation.HORIZONTAL);
		m_centerSplitPane.getItems().addAll(m_jScrollPane);

		m_mainSplitPane = new SplitPane();
		m_mainSplitPane.setOrientation(Orientation.VERTICAL);
		m_mainSplitPane.getItems().add(m_centerSplitPane);

		VBox topContainer = new VBox(m_menuBar, m_tbTools);
		setTop(topContainer);
		setCenter(m_mainSplitPane);
		setRight(m_tbTools2);

		HBox statusContainer = new HBox(m_jStatusBar);
		statusContainer.setPadding(new Insets(2, 5, 2, 5));
		setBottom(statusContainer);

		widthProperty().addListener((obs, oldV, newV) -> fitToScreen());
		heightProperty().addListener((obs, oldV, newV) -> fitToScreen());

		URL tempURL = ClassLoader.getSystemResource(ICONPATH + "rotate.png");
		if (tempURL != null) {
			try {
				m_rotate = ImageIO.read(tempURL);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public DensiTree(String[] args) {
		this();
		System.out.println(banner());
		parseArgs(args);
		m_settings.m_pattern = createPattern();
	}

	public static Image getFxIcon(String sIcon) {
		URL url = ClassLoader.getSystemResource(ICONPATH + sIcon + ".png");
		if (url != null) {
			return new Image(url.toExternalForm());
		}
		return null;
	}

	public static ImageView getFxIconView(String sIcon) {
		Image img = getFxIcon(sIcon);
		if (img != null) {
			ImageView iv = new ImageView(img);
			iv.setFitWidth(18);
			iv.setFitHeight(18);
			return iv;
		}
		return null;
	}

	public void repaint() {
		SwingUtilities.invokeLater(() -> {
			if (m_Panel != null) m_Panel.repaint();
			if (m_cladeSetComparisonPanel != null) m_cladeSetComparisonPanel.repaint();
		});
	}

	public void makeDirty() {
		m_treeData.m_rotationPoints = null;
		if (m_treeData2 != null) {
			m_treeData2.m_rotationPoints = null;
		}
		if (m_bAutoRefresh) {
			SwingUtilities.invokeLater(() -> m_Panel.clearImage());
		} else {
			m_bIsDirty = true;
		}
		repaint();
	}

	public void fitToScreen() {
		if (m_settings.m_sLabels == null) {
			return;
		}
		m_fScaleX = 10;
		m_fScaleY = 10;
		int nW = (int) getWidth();
		int nH = (int) getHeight() - 24;
		if (nW <= 0) nW = 1000;
		if (nH <= 0) nH = 750;

		if (m_treeDrawer.m_bRootAtTop) {
			m_fScaleX = (nW + 0.0f) / m_settings.m_sLabels.size();
			m_fScaleGX = (nW + 0.0f) / (m_settings.m_fMaxLong - m_settings.m_fMinLong);
			if (m_fHeight > 0) {
				if (m_settings.m_bRotateTextWhenRootAtTop) {
					m_fScaleY = (nH - m_settings.m_nLabelWidth - 0.0f) / m_fHeight;
					m_fScaleGY = (nH - m_settings.m_nLabelWidth - 0.0f) / (m_settings.m_fMaxLat - m_settings.m_fMinLat);
				} else {
					m_fScaleY = (nH - 10.0f) / m_fHeight;
					m_fScaleGY = (nH - 10.0f) / (m_settings.m_fMaxLat - m_settings.m_fMinLat);
				}
			}
		} else {
			if (m_settings.m_sLabels != null && m_settings.m_sLabels.size() > 0) {
				m_fScaleY = (nH + 0.0f) / m_settings.m_sLabels.size();
				m_fScaleGY = (nH + 0.0f) / (m_settings.m_fMaxLat - m_settings.m_fMinLat);
			}
			if (m_fHeight > 0) {
				m_fScaleX = (nW - m_settings.m_nLabelWidth + 0.0f) / m_fHeight;
				m_fScaleGX = (nW - m_settings.m_nLabelWidth + 0.0f) / (m_settings.m_fMaxLong - m_settings.m_fMinLong);
			}
		}

		final int panelW = (int) (nW * m_fScale);
		final int panelH = (int) (nH * m_fScale);
		SwingUtilities.invokeLater(() -> m_Panel.setPreferredSize(new Dimension(panelW, panelH)));

		m_fScaleX *= m_fScale;
		m_fScaleY *= m_fScale;

		if (m_treeData2 != null) {
			m_fScaleX /= 2.0;
		}
		makeDirty();
	}

	public Pattern createPattern() {
		StringBuilder sPattern = new StringBuilder();
		for (int i = 0; i < m_settings.m_iPatternForBottom; i++) {
			sPattern.append("[0-9\\.Ee-]+[^0-9]+");
		}
		sPattern.append("([0-9\\.Ee-]+)");
		if (m_settings.m_iPatternForTop > m_settings.m_iPatternForBottom) {
			for (int i = m_settings.m_iPatternForBottom + 1; i < m_settings.m_iPatternForTop; i++) {
				sPattern.append("[^0-9]+[0-9\\.Ee-]+");
			}
			sPattern.append("[^0-9]+([0-9\\.Ee-]+)");
		}
		return Pattern.compile(sPattern.toString());
	}

	void initColors() {
		m_settings.m_color = new Color[10 + 11 + 50];
		m_settings.m_color[0] = Color.getColor("color.1", Color.blue);
		m_settings.m_color[1] = Color.getColor("color.2", Color.red);
		m_settings.m_color[2] = Color.getColor("color.3", Color.green);
		m_settings.m_color[3] = Color.getColor("color.default", new Color(0, 100, 25));
		m_settings.m_color[CONSCOLOR] = Color.getColor("color.cons", Color.blue);
		m_settings.m_color[LABELCOLOR] = Color.getColor("color.label", Color.blue);
		m_settings.m_color[HEIGHTCOLOR] = Color.getColor("color.height", Color.gray);
		m_settings.m_color[BGCOLOR] = Color.getColor("color.bg", Color.white);
		m_settings.m_color[GEOCOLOR] = Color.getColor("color.bg", Color.orange);
		m_settings.m_color[ROOTCANALCOLOR] = Color.getColor("color.rootcanal", Color.blue);

		int k = GEOCOLOR + 1;
		m_settings.m_color[k++] = Color.blue;
		m_settings.m_color[k++] = Color.green;
		m_settings.m_color[k++] = Color.red;
		m_settings.m_color[k++] = Color.gray;
		m_settings.m_color[k++] = Color.orange;
		m_settings.m_color[k++] = Color.yellow;
		m_settings.m_color[k++] = Color.pink;
		m_settings.m_color[k++] = Color.black;
		m_settings.m_color[k++] = Color.cyan;
		m_settings.m_color[k++] = Color.darkGray;
		m_settings.m_color[k++] = Color.magenta;
		m_settings.m_color[k++] = new Color(100, 200, 25);

		for (float saturation = 0.9f; saturation >= 0.0f; saturation -= 0.2) {
			for (float hue = 0.0f; hue < 1.0f; hue += 0.1) {
				m_settings.m_color[k++] = new Color(Color.HSBtoRGB(hue + saturation / 10, saturation, 0.9f));
			}
		}
	}

	void parseArgs(String[] args) {
		File cfgFile = new File(".densitree");
		if (cfgFile.exists()) {
			List<String> cfgArgs = new ArrayList<>();
			try (BufferedReader fin = new BufferedReader(new FileReader(cfgFile))) {
				String sStr;
				while ((sStr = fin.readLine()) != null) {
					if (sStr.length() > 0 && !sStr.matches("^\\s*$")) {
						cfgArgs.add(sStr);
					}
				}
				Collections.addAll(cfgArgs, args);
				args = cfgArgs.toArray(new String[0]);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				System.err.println("WARNING: could not process cfg file");
			}
		}

		int i = 0;
		try {
			while (i < args.length) {
				int iOld = i;
				if (i < args.length - 1) {
					if (args[i].equals("")) {
						i += 1;
					} else if (args[i].equals("-c")) {
						m_settings.m_fCTreeIntensity = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-i")) {
						m_settings.m_fTreeIntensity = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-j")) {
						m_settings.m_nJitter = (int) Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-w")) {
						m_settings.m_nCTreeWidth = (int) Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-v")) {
						m_settings.m_nTreeWidth = (int) Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-f")) {
						m_nAnimationDelay = (int) Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-t")) {
						m_Panel.m_nDrawThreads = (int) Float.parseFloat(args[i + 1]);
						if (m_Panel.m_nDrawThreads < 1) {
							m_Panel.m_nDrawThreads = 1;
						}
						i += 2;
					} else if (args[i].equals("-b")) {
						m_nBurnIn = (int) Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-thin")) {
						m_nThin = (int) Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-geo")) {
						String[] sStrs = args[i + 1].split("x");
						int nWidth = Integer.parseInt(sStrs[0]);
						int nHeight = Integer.parseInt(sStrs[1]);
						setPrefSize(nWidth, nHeight);
						i += 2;
					} else if (args[i].equals("-geooffset")) {
						GEO_OFFSET = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-invertLongitude")) {
						m_settings.m_bInvertLongitude = true;
						i += 1;
					} else if (args[i].equals("-scalemode")) {
						String sMode = args[i + 1].toLowerCase();
						if (sMode.equals("none")) {
							m_gridDrawer.m_nGridMode = GridMode.NONE;
						} else if (sMode.equals("short")) {
							m_gridDrawer.m_nGridMode = GridMode.SHORT;
						} else if (sMode.equals("full")) {
							m_gridDrawer.m_nGridMode = GridMode.FULL;
						} else {
							throw new Exception("expected scalemode to be NONE, SHORT or FULL");
						}
						i += 2;
					} else if (args[i].equals("-li") || args[i].equals("-label.indent")) {
						m_settings.m_fLabelIndent = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-label.width")) {
						m_settings.m_nLabelWidth = Integer.parseInt(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-label.hide")) {
						m_settings.m_bHideLabels = true;
						i += 1;
					} else if (args[i].equals("-o")) {
						m_settings.m_sOutputFile = args[i + 1];
						i += 2;
					} else if (args[i].equals("-kml")) {
						m_settings.m_sKMLFile = args[i + 1];
						i += 2;
					} else if (args[i].equals("-geowidth")) {
						m_settings.m_nGeoWidth = Integer.parseInt(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-geocolor")) {
						m_settings.m_color[GEOCOLOR] = Color.decode(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-bg")) {
						try {
							loadBGImage(args[i + 1]);
						} catch (Exception e) {
							System.err.println("Error loading file: " + e.getMessage());
							return;
						}
						i += 2;
					} else if (args[i].equals("-bd")) {
						BranchDrawer bd = (BranchDrawer) Class.forName(args[i + 1]).newInstance();
						m_treeDrawer.setBranchDrawer(bd);
						i += 2;
					} else if (args[i].equals("-pattern")) {
						m_settings.m_sPattern = args[i + 1];
						i += 2;
					} else if (args[i].equals("-colorpattern")) {
						m_settings.m_sColorPattern = args[i + 1];
						i += 2;
					} else if (args[i].equals("-linecolortag")) {
						m_settings.m_lineColorTag = args[i + 1];
						m_settings.m_lineColorMode = LineColorMode.COLOR_BY_METADATA_TAG;
						i += 2;
					} else if (args[i].equals("-linecolorlegend")) {
						m_settings.m_showLegend = true;
						i++;
					} else if (args[i].equals("-singlechild")) {
						m_settings.m_bAllowSingleChild = Boolean.parseBoolean(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-rotatetext")) {
						m_settings.m_bRotateTextWhenRootAtTop = true;
						i++;
					} else if (args[i].equals("-transform")) {
						m_settings.m_bUseLogScale = true;
						m_fExponent = Double.parseDouble(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-allowLeafsToBeMovedIKnowThisMessesUpInternalCladePositions")) {
						m_bLeafCladeSelection = true;
						i += 1;
					} else if (args[i].equals("-optfile")) {
						m_sOptFile = args[i + 1];
						i += 2;
					} else if (args[i].equals("-rootcanaltree")) {
						try {
							m_iOptTree = Integer.parseInt(args[i + 1]);
						} catch (NumberFormatException e) {
							m_sOptTree = args[i + 1];
						}
						i += 2;
					} else if (args[i].equals("-rawrootcanaltree")) {
						m_sOptTree = args[i + 1];
						m_settings.m_bOptimiseRootCanalTree = false;
						i += 2;
					} else if (args[i].equals("-asPDF")) {
						m_asPDF = args[i + 1];
						i += 2;
					} else if (args[i].equals("-cladeComparisonAsPDF")) {
						m_cladeComparisonAsPDF = args[i + 1];
						i += 2;
					} else if (args[i].equals("-mirror")) {
						m_sFileName2 = args[i + 1];
						i += 2;
					} else if (args[i].equals("-viewCladeComparison")) {
						setCladeComparisonVisible(true);
						i += 1;
					} else if (args[i].equals("-cladeThreshold")) {
						m_settings.m_cladeThreshold = Double.parseDouble(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-r")) {
						m_settings.m_bDrawReverse = true;
						i += 1;
					} else if (args[i].equals("-order")) {
						m_settings.m_sOrderFile = args[i + 1];
						i += 2;
					}

					if (i == iOld) {
						if (new File(args[i]).exists()) {
							init(args[i++]);
							calcLines();

							if (i != args.length) {
								String[] args2 = new String[args.length - 1];
								for (int k = 0; k < i - 1; k++) {
									args2[k] = args[k];
								}
								for (int k = i; k < args.length; k++) {
									args2[k - 1] = args[k];
								}
								startNew(args2);
							}
							return;
						}
						throw new Exception("Wrong argument: " + (i < args.length ? args[i] : i + ""));
					}
				} else {
					init(args[i++]);
					calcLines();
				}
			}

			if (m_asPDF != null || m_cladeComparisonAsPDF != null) {
				new Thread(() -> {
					try {
						Thread.sleep(5000);
					} catch (Exception e) {
						e.printStackTrace();
					}
					while (!m_treeData.m_bMetaDataReady) {
						try {
							Thread.sleep(100);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (m_cladeComparisonAsPDF != null) {
						while (m_treeData2 == null || !m_treeData2.m_bMetaDataReady) {
							try {
								Thread.sleep(100);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						exportPDF(m_cladeComparisonAsPDF, m_cladeSetComparisonPanel);
					}
					if (m_asPDF != null) {
						exportPDF(m_asPDF, m_Panel);
					}
					System.exit(0);
				}).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
			showErrorAlert("Error parsing command line arguments: " + Arrays.toString(args)
					+ "\nArguments ignored\n\n" + getStatus());
		}
	}

	String banner() {
		return "DensiTree - Tree Set Visualizer\nVersion " + VERSION + "\n\n" + "Remco Bouckaert\n"
				+ "r.bouckaert@auckland.ac.nz\nrrb@xm.co.nz\n" + "(c) 2010-2026\n\n\n"
				+ "Key shortcuts:\n" + "c/Ctrl-c decrease/increase consensus tree intensity\n"
				+ "i/Ctrl-i decrease/increase tree intensity\n"
				+ "j/Ctrl-j decrease/increase jitter on trees (not consensus trees)\n"
				+ "w/Ctrl-w decrease/increase consensus tree line width\n"
				+ "v/Ctrl-v decrease/increase tree line width\n"
				+ "f/Ctrl-f decrease/increase animation time delay - shorter delay = faster animation\n"
				+ "t/Ctrl-t decrease/increase number of drawing threads for drawing tree set\n\n"
				+ "Arrow keys & Page-Up/Down to scroll\n";
	}

	String formatColor(int iColor) {
		return " 0x" + Integer.toHexString(m_settings.m_color[iColor].getRGB()).substring(2) + ' ';
	}

	String getStatus() {
		int nSelected = 0;
		if (m_treeData.m_bSelection != null) {
			for (boolean b : m_treeData.m_bSelection) {
				if (b) nSelected++;
			}
		}
		return "\n\nCurrent status:\n" + m_treeData.m_trees.length + " trees with " + m_treeData.m_cTrees.length + " topologies " +
				m_settings.m_sLabels.size() + " taxa " + nSelected + " selected \n"
				+ "Tree intensity: " + m_settings.m_fTreeIntensity + "\n" + "Consensus Tree intensity: " + m_settings.m_fCTreeIntensity
				+ "\n" + "Tree width: " + m_settings.m_nTreeWidth + "\n" + "Consensus Tree width: " + m_settings.m_nCTreeWidth + "\n"
				+ "Jitter: " + m_settings.m_nJitter + "\n" + "Animation delay: " + m_nAnimationDelay + "\n" + "Height: "
				+ m_fHeight + "\n" + "Zoom: " + m_fScale + "\n" + "Number of drawing threads: " + m_Panel.m_nDrawThreads + "\n"
				+ "Burn in: " + m_nBurnIn + "\n\nColor 1:" + formatColor(0) + "\tColor 2:" + formatColor(1)
				+ "\tColor 3:" + formatColor(2) + "\tDefault Color:" + formatColor(3) + "\nConsensus Color:"
				+ formatColor(CONSCOLOR) + "\tLabel color:" + formatColor(LABELCOLOR) + "\tBackground color:"
				+ formatColor(BGCOLOR) + "\tHeight color:" + formatColor(HEIGHTCOLOR);
	}

	class MetaDataThread extends Thread {
		TreeData treeData;
		DensiTree m_dt;

		MetaDataThread(TreeData treeData, DensiTree dt) {
			this.treeData = treeData;
			this.m_dt = dt;
		}

		@Override
		public void run() {
			updateStatus("Calculating clades");
			treeData.calcClades();
			treeData.m_bCladesReady = true;

			if (treeData.drawMode == TreeData.MODE_RIGHT) {
				m_cladeToIDMap = new int[m_dt.m_treeData.m_clades.size()];
				for (int i = 0; i < m_dt.m_treeData.m_clades.size(); i++) {
					m_cladeToIDMap[i] = findClade(m_dt.m_treeData2, m_dt.m_treeData.m_clades.get(i));
				}
				m_mirrorCladeToIDMap = new int[m_dt.m_treeData2.m_clades.size()];
				for (int i = 0; i < m_dt.m_treeData2.m_clades.size(); i++) {
					m_mirrorCladeToIDMap[i] = findClade(m_dt.m_treeData, m_dt.m_treeData2.m_clades.get(i));
				}
			}

			updateStatus("Optimising node order");
			int[] oldOrder = m_settings.m_nOrder.clone();
			if (!m_settings.m_bAllowSingleChild && treeData.drawMode != TreeData.MODE_RIGHT) {
				reshuffle(NodeOrderer.SORT_BY_ROOT_CANAL_LENGTH);
			}
			if (!m_settings.m_bAllowSingleChild) {
				calcPositions();
				calcLines();
				notifyChangeListeners();
				if (orderChanged(oldOrder)) {
					System.err.println("Node order changed");
					makeDirty();
				}
			}
			String statusMsg = "Parsing metadata";
			for (int k = 0; k < treeData.m_trees.length; k++) {
				parseMetaData(treeData.m_trees[k]);
				if (k % 100 == 0) {
					statusMsg += ".";
					updateStatus(statusMsg);
					setWaitCursor();
				}
			}
			if (!m_settings.m_bAllowSingleChild && treeData.drawMode != TreeData.MODE_RIGHT) {
				m_settings.m_metaDataTags = new ArrayList<>();
				m_settings.m_metaDataTypes = new ArrayList<>();
				collectMetaDataTags(treeData.m_trees[0]);
				if (m_settings.m_metaDataTags.size() > 0) {
					calcPositions();
					calcLines();
					makeDirty();
				}
			}
			treeData.m_bMetaDataReady = true;
			notifyChangeListeners();
			updateStatus("Done parsing metadata");
			thread = null;
		}

		private void parseMetaData(Node node) {
			node.parseMetaData();
			if (!node.isLeaf()) {
				parseMetaData(node.m_left);
				if (node.m_right != null) {
					parseMetaData(node.m_right);
				}
			}
		}
	}

	public void updateStatus(String msg) {
		Platform.runLater(() -> m_jStatusBar.setText(msg));
	}

	@SuppressWarnings("deprecation")
	public void init(String sFile) throws Exception {
		setWaitCursor();
		m_treeData2 = null;
		updateStatus("Initializing...");
		m_sFileName = sFile;
		m_bInitializing = true;
		m_viewMode = ViewMode.DRAW;
		m_settings.m_prevLineColorMode = null;
		LineColorMode orgLineColorMode = m_settings.m_lineColorMode;
		m_settings.m_lineColorMode = LineColorMode.DEFAULT;
		m_settings.m_prevLineWidthMode = null;
		m_settings.m_lineWidthMode = LineWidthMode.DEFAULT;

		System.err.print("Initializing...");
		m_iAnimateTree = 0;
		m_fHeight = 0;
		m_fScaleX = 10;
		m_fScaleY = 10;
		m_fScale = 1;
		m_fTreeScale = 1;
		m_fTreeOffset = 0;
		m_doActions = new Vector<>();
		m_iUndo = 0;
		m_settings.m_random = new Random();
		m_Panel.m_drawThread = new Thread[2][m_Panel.m_nDrawThreads];
		m_treeData.m_rootcanaltree = null;

		try {
			if (thread != null) {
				try {
					thread.stop();
				} catch (Exception ignored) {
				}
			}
			m_settings.m_sLabels = new Vector<>();
			m_settings.m_fLongitude = new Vector<>();
			m_settings.m_fLatitude = new Vector<>();
			m_settings.m_fMinLat = 360;
			m_settings.m_fMinLong = 360;
			m_settings.m_fMaxLat = 0;
			m_settings.m_fMaxLong = 0;
			m_settings.m_nOrder = null;

			m_treeData.loadFromFile(sFile, true);

			Node tree = m_treeData.m_trees[0];
			m_settings.m_nOrder = new int[m_settings.m_sLabels.size()];
			m_settings.m_nRevOrder = new int[m_settings.m_sLabels.size()];
			initOrder(tree, 0);

			int nSum = 0;
			for (int i = 0; i < m_settings.m_nOrder.length; i++) {
				nSum += m_settings.m_nOrder[i];
			}
			if (nSum != m_settings.m_nNrOfLabels * (m_settings.m_nNrOfLabels - 1) / 2) {
				showWarningAlert("The tree set possibly contains non-binary trees. Expect that not all nodes are shown.");
			}

			reshuffle(NodeOrderer.DEFAULT);
			calcPositions();

			m_treeData.m_bMetaDataReady = false;
			thread = new MetaDataThread(m_treeData, this);
			thread.start();

			m_settings.m_metaDataTags = new ArrayList<>();
			m_settings.m_metaDataTypes = new ArrayList<>();
			collectMetaDataTags(m_treeData.m_trees[0]);
			notifyChangeListeners();

			if (orgLineColorMode != LineColorMode.DEFAULT) {
				while (!m_treeData.m_bMetaDataReady) {
					Thread.sleep(100);
				}
				m_settings.m_lineColorMode = orgLineColorMode;
				calcColors(false);
				makeDirty();
			}
		} catch (OutOfMemoryError e) {
			clear();
			showErrorAlert("Not enough memory is reserved for java to process this tree. "
					+ "Try starting DensiTree with more memory\n\n(for example "
					+ "use:\njava -Xmx3g DensiTree.jar\nfrom " + "the command line) where DensiTree is in the path\n"
					+ "or subsample your tree set to create a smaller tree file.");
			setDefaultCursor();
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			clear();
			setDefaultCursor();
			throw e;
		}
		m_bInitializing = false;
		if (m_settings.m_sColorPattern != null) {
			calcColorPattern();
		}

		addAction(new DoAction());
		Platform.runLater(() -> {
			if (stage != null) {
				stage.setTitle(FRAME_TITLE + " " + sFile);
			}
		});
		if (m_settings.m_sKMLFile != null) {
			loadKML();
		}

		if (m_sFileName2 != null && new File(m_sFileName2).exists()) {
			doOpenMirror(m_sFileName2);
		}

		System.err.println("Done");
	}

	public float positionHeight(Node node, int fOffSet) {
		return m_treeData.positionHeight(node, fOffSet);
	}

	public void calcLines() {
		m_treeData.calcLines();
		if (m_treeData2 != null) {
			m_treeData2.calcLines();
		}
	}

	public void calcColors(boolean forceRecalc) {
		m_treeData.calcColors(forceRecalc);
		if (m_treeData2 != null) {
			m_treeData2.calcColors(forceRecalc);
		}
	}

	public void calcPositions() {
		m_treeData.calcPositions();
		if (m_treeData2 != null) {
			m_treeData2.calcPositions();
		}
	}

	public void calcLineWidths(boolean forceRecalc) {
		m_treeData.calcLineWidths(forceRecalc);
		if (m_treeData2 != null) {
			m_treeData2.calcLineWidths(forceRecalc);
		}
	}

	float positionRest(Node node) {
		return m_treeData.positionRest(node);
	}

	private void getPosition(Node node, float[] fPosX) {
		m_treeData.getPosition(node, fPosX);
	}

	private void setPosition(Node node, float[] fPosX) {
		m_treeData.setPosition(node, fPosX);
	}

	void notifyChangeListeners() {
		for (ChangeListener listener : m_changeListeners) {
			listener.stateChanged(null);
		}
	}

	private boolean orderChanged(int[] oldOrder) {
		for (int i = 0; i < oldOrder.length; i++) {
			if (oldOrder[i] != m_settings.m_nOrder[i]) {
				return true;
			}
		}
		return false;
	}

	private void collectMetaDataTags(Node node) {
		Map<String, Object> metaDataMap = node.getMetaDataSet();
		if (metaDataMap != null) {
			for (String key : metaDataMap.keySet()) {
				if (!m_settings.m_metaDataTags.contains(key)) {
					m_settings.m_metaDataTags.add(key);
					Object o = metaDataMap.get(key);
					if (o instanceof Double) {
						m_settings.m_metaDataTypes.add(MetaDataType.NUMERIC);
					} else {
						String s = o.toString();
						if (s.length() > 0 && s.charAt(0) == '{') {
							m_settings.m_metaDataTypes.add(MetaDataType.SET);
						} else {
							m_settings.m_metaDataTypes.add(MetaDataType.STRING);
						}
					}
				}
			}
		}
		if (!node.isLeaf()) {
			collectMetaDataTags(node.m_left);
			if (node.m_right != null) {
				collectMetaDataTags(node.m_right);
			}
		}
	}

	public void updateCladeModel() {
		m_treeData.updateCladeModel();
		if (m_treeData2 != null) {
			m_treeData2.updateCladeModel();
		}
	}

	public void resetCladeSelection() {
		m_treeData.resetCladeSelection();
		if (m_treeData2 != null) {
			m_treeData2.resetCladeSelection();
		}
	}

	public void calcCladeIDForNode(Node tree, Map<String, Integer> mapCladeToIndex) {
		m_treeData.calcCladeIDForNode(tree, mapCladeToIndex);
	}

	public void resetCladeNr(Node tree, Integer[] reverseindex) {
		m_treeData.resetCladeNr(tree, reverseindex);
	}

	void calcColorPattern() {
		m_settings.m_iColor = new int[m_settings.m_sLabels.size()];
		Pattern pattern = Pattern.compile(".*" + m_settings.m_sColorPattern + ".*");
		List<String> sPatterns = new ArrayList<>();
		for (int i = 0; i < m_settings.m_sLabels.size(); i++) {
			String sLabel = m_settings.m_sLabels.get(i);
			Matcher matcher = pattern.matcher(sLabel);
			if (matcher.find()) {
				String sMatch = matcher.group(1);
				if (!sPatterns.contains(sMatch)) {
					sPatterns.add(sMatch);
				}
				m_settings.m_iColor[i] = sPatterns.indexOf(sMatch);
			}
		}
	}

	void loadKML() {
		String sFileName = m_settings.m_sKMLFile;
		HashMap<String, Vector<Double>> mapLabel2X = new HashMap<>();
		HashMap<String, Vector<Double>> mapLabel2Y = new HashMap<>();

		if (!(new File(sFileName)).exists()) {
			showWarningAlert("Tried to read geo info, but could not find file " + sFileName);
			return;
		}

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			org.w3c.dom.Document doc = null;
			if (sFileName.toLowerCase().endsWith(".kmz")) {
				ZipFile zf = new ZipFile(sFileName);
				Enumeration<?> entries = zf.entries();
				while (entries.hasMoreElements()) {
					ZipEntry ze = (ZipEntry) entries.nextElement();
					if (ze.getName().toLowerCase().equals("doc.kml")) {
						doc = factory.newDocumentBuilder().parse(zf.getInputStream(ze));
					}
				}
				zf.close();
			} else {
				doc = factory.newDocumentBuilder().parse(new File(sFileName));
			}
			doc.normalize();

			HashMap<String, Integer> mapStyleToColor = new HashMap<>();
			org.w3c.dom.NodeList oStyles = doc.getElementsByTagName("Style");
			for (int iNode = 0; iNode < oStyles.getLength(); iNode++) {
				org.w3c.dom.Node oStyle = oStyles.item(iNode);
				String sID = oStyle.getAttributes().getNamedItem("id").getTextContent();
				XPath xpath = XPathFactory.newInstance().newXPath();
				String expression = ".//PolyStyle/color";
				org.w3c.dom.Node oColor = (org.w3c.dom.Node) xpath.evaluate(expression, oStyles.item(iNode),
						XPathConstants.NODE);
				if (oColor != null) {
					String sColor = oColor.getTextContent().substring(2);
					mapStyleToColor.put(sID, Integer.parseInt(sColor, 16));
				}
			}

			org.w3c.dom.NodeList oPlacemarks = doc.getElementsByTagName("Placemark");
			for (int iNode = 0; iNode < oPlacemarks.getLength(); iNode++) {
				String sPlacemarkName = "";
				Vector<Double> nX = new Vector<>();
				Vector<Double> nY = new Vector<>();
				org.w3c.dom.Node node = oPlacemarks.item(iNode);
				org.w3c.dom.NodeList oChildren = node.getChildNodes();
				for (int iChild = 0; iChild < oChildren.getLength(); iChild++) {
					org.w3c.dom.Node oChild = oChildren.item(iChild);
					if (oChild.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
						String sName = oChild.getNodeName();
						if (sName.equals("name")) {
							sPlacemarkName = oChild.getTextContent().trim();
						} else if (sName.equals("Polygon") || sName.equals("Point") || sName.equals("LineString")) {
							XPath xpath = XPathFactory.newInstance().newXPath();
							String expression = ".//coordinates";
							org.w3c.dom.Node oCoords = (org.w3c.dom.Node) xpath.evaluate(expression, oChild,
									XPathConstants.NODE);
							String sCoord = oCoords.getTextContent();
							String[] sCoords = sCoord.split("\\s+");
							for (String str : sCoords) {
								String[] sStrs = str.split(",");
								if (sStrs.length > 1) {
									nX.add(Double.parseDouble(sStrs[0]));
									nY.add(Double.parseDouble(sStrs[1]));
								}
							}
						}
					}
				}
				if (nX.size() > 0) {
					mapLabel2X.put(sPlacemarkName.toLowerCase(), nX);
					mapLabel2Y.put(sPlacemarkName.toLowerCase(), nY);
					sPlacemarkName = sPlacemarkName.replaceAll("[-_]", "");
					if (!mapLabel2X.containsKey(sPlacemarkName)) {
						mapLabel2X.put(sPlacemarkName.toLowerCase(), nX);
						mapLabel2Y.put(sPlacemarkName.toLowerCase(), nY);
					}
				}
			}
		} catch (Exception e) {
			try (BufferedReader fin = new BufferedReader(new FileReader(sFileName))) {
				m_settings.m_fMinLat = 90;
				m_settings.m_fMinLong = 180;
				m_settings.m_fMaxLat = -90;
				m_settings.m_fMaxLong = -180;

				String sStr = fin.readLine();
				while ((sStr = fin.readLine()) != null) {
					String[] sStrs = sStr.split("\\s+");
					if (sStrs.length >= 3) {
						try {
							String sPlacemarkName = sStrs[0];
							Vector<Double> nX = new Vector<>();
							Vector<Double> nY = new Vector<>();
							nX.add(Double.parseDouble(sStrs[2]));
							nY.add(Double.parseDouble(sStrs[1]));
							mapLabel2X.put(sPlacemarkName.toLowerCase(), nX);
							mapLabel2Y.put(sPlacemarkName.toLowerCase(), nY);
							sPlacemarkName = sPlacemarkName.replaceAll("[-_]", "");
							if (!mapLabel2X.containsKey(sPlacemarkName)) {
								mapLabel2X.put(sPlacemarkName.toLowerCase(), nX);
								mapLabel2Y.put(sPlacemarkName.toLowerCase(), nY);
							}
						} catch (Exception ignored) {
						}
					}
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}

		try {
			m_settings.m_fMinLat = 90;
			m_settings.m_fMinLong = 180;
			m_settings.m_fMaxLat = -90;
			m_settings.m_fMaxLong = -180;
			for (int iLabel = 0; iLabel < m_settings.m_nNrOfLabels; iLabel++) {
				String sTaxon = m_settings.m_sLabels.get(iLabel).toLowerCase();
				String sTaxon2 = sTaxon.replaceAll("[-_]", "");
				if (mapLabel2X.containsKey(sTaxon) || mapLabel2X.containsKey(sTaxon2)) {
					if (!mapLabel2X.containsKey(sTaxon)) {
						sTaxon = sTaxon2;
					}
					Vector<Double> nX = mapLabel2X.get(sTaxon);
					Vector<Double> nY = mapLabel2Y.get(sTaxon);
					double fX = 0, fY = 0;
					for (Double f : nX) fX += f;
					fX /= nX.size();
					for (Double f : nY) fY += f;
					fY /= nY.size();
					while (m_settings.m_fLatitude.size() <= iLabel) {
						m_settings.m_fLatitude.add(0f);
						m_settings.m_fLongitude.add(0f);
					}
					m_settings.m_fLatitude.set(iLabel, (float) fY);
					m_settings.m_fLongitude.set(iLabel, (float) fX);
					m_settings.m_fMinLat = Math.min(m_settings.m_fMinLat, (float) fY);
					m_settings.m_fMaxLat = Math.max(m_settings.m_fMaxLat, (float) fY);
					m_settings.m_fMinLong = Math.min(m_settings.m_fMinLong, (float) fX);
					m_settings.m_fMaxLong = Math.max(m_settings.m_fMaxLong, (float) fX);
				} else {
					while (m_settings.m_fLatitude.size() <= iLabel) {
						m_settings.m_fLatitude.add(0f);
						m_settings.m_fLongitude.add(0f);
					}
					m_settings.m_fLatitude.set(iLabel, 0f);
					m_settings.m_fLongitude.set(iLabel, 0f);
				}
			}
			float fOffset = GEO_OFFSET;
			m_settings.m_fMaxLong += fOffset;
			m_settings.m_fMaxLat += fOffset;
			m_settings.m_fMinLong -= fOffset;
			m_settings.m_fMinLat -= fOffset;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void clear() {
		m_treeData.m_trees = new Node[0];
		m_treeData.m_cTrees = new Node[0];
		m_treeData.m_fLinesX = null;
		m_treeData.m_fLinesY = null;
		m_treeData.m_fCLinesX = null;
		m_treeData.m_fCLinesY = null;
		m_bInitializing = false;
	}

	void reshuffle(int nMethod) {
		int[] oldOrder = m_settings.m_nOrder.clone();
		m_settings.m_nShuffleMode = nMethod;
		setWaitCursor();

		if (m_settings.m_sOrderFile != null) {
			if (new File(m_settings.m_sOrderFile).exists()) {
				nMethod = NodeOrderer.MANUAL;
			} else {
				showWarningAlert("Could not find file " + m_settings.m_sOrderFile + " for reading");
			}
		}

		try {
			switch (nMethod) {
				case NodeOrderer.DEFAULT:
					initOrder(m_treeData.m_trees[0], 0);
					break;
				case NodeOrderer.MANUAL: {
					StringBuilder buf = new StringBuilder();
					for (int i = 0; i < m_settings.m_sLabels.size(); i++) {
						buf.append(m_settings.m_sLabels.elementAt(m_settings.m_nRevOrder[i])).append(" ");
					}
					buf.deleteCharAt(buf.length() - 1);

					String[] sIndex;
					if (m_settings.m_sOrderFile == null) {
						String sOrder = showInputDialog("New node order:", buf.toString());
						if (sOrder == null) return;
						sIndex = sOrder.split(" ");
					} else {
						List<String> labels = new ArrayList<>();
						try (BufferedReader fin = new BufferedReader(new FileReader(m_settings.m_sOrderFile))) {
							String str;
							while ((str = fin.readLine()) != null) {
								if (!str.startsWith("#") && str.trim().length() > 0) {
									labels.add(str.trim());
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
							showErrorAlert("Something went wrong with file " + m_settings.m_sOrderFile + ": " + e.getMessage());
						}
						sIndex = labels.size() == 1 ? labels.get(0).split("\\s") : labels.toArray(new String[0]);
					}

					if (sIndex.length != m_settings.m_nNrOfLabels) {
						System.err.println("Number of labels/taxa " + sIndex.length + " differs from given labels " + m_settings.m_nNrOfLabels);
						return;
					}
					int[] nOrder = new int[m_settings.m_nOrder.length];
					int[] nRevOrder = new int[m_settings.m_nRevOrder.length];
					for (int i = 0; i < sIndex.length; i++) {
						int j = 0;
						String sTarget = sIndex[i];
						while ((j < m_settings.m_sLabels.size()) && !(m_settings.m_sLabels.elementAt(j).equals(sTarget))) {
							j++;
						}
						if (j == m_settings.m_sLabels.size()) {
							System.err.println("Label \"" + sTarget + "\" not found among labels");
							return;
						}
						nOrder[j] = i;
						nRevOrder[i] = j;
					}
					m_settings.m_nOrder = nOrder;
					m_settings.m_nRevOrder = nRevOrder;
					break;
				}
				case NodeOrderer.META_ALL:
				case NodeOrderer.META_SUM:
				case NodeOrderer.META_AVERAGE:
				case NodeOrderer.GEOINFO:
					break;
				default:
					NodeOrderer h = new NodeOrderer(nMethod);
					int[] nOrder = h.calcOrder(m_settings.m_nNrOfLabels, m_treeData.m_trees, m_treeData.m_cTrees, m_treeData.m_rootcanaltree, m_treeData.m_fTreeWeight, m_treeData.m_clades, m_treeData.m_cladeWeight);
					m_settings.m_nOrder = nOrder;
					for (int i = 0; i < m_settings.m_nNrOfLabels; i++) {
						m_settings.m_nRevOrder[m_settings.m_nOrder[i]] = i;
					}
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		int nNodes = getNrOfNodes(m_treeData.m_trees[0]);
		if (nMethod < NodeOrderer.META_ALL) {
			m_settings.m_bShowBounds = false;
			calcPositions();
			calcLines();
			makeDirty();
			addAction(new DoAction());
		} else {
			m_settings.m_bShowBounds = true;
			m_settings.m_pattern = Pattern.compile(m_settings.m_sPattern);
			switch (nMethod) {
				case NodeOrderer.META_ALL: {
					double fMaxX = 0;
					for (Node tree : m_treeData.m_trees) fMaxX = Math.max(fMaxX, positionMetaAll(tree));
					for (Node cTree : m_treeData.m_cTrees) fMaxX = Math.max(fMaxX, positionMetaAll(cTree));
					fMaxX = m_settings.m_nNrOfLabels / fMaxX;
					for (Node tree : m_treeData.m_trees) scaleX(tree, fMaxX);
					for (Node cTree : m_treeData.m_cTrees) scaleX(cTree, fMaxX);
					calcLines();
					break;
				}
				case NodeOrderer.META_SUM:
				case NodeOrderer.META_AVERAGE: {
					for (int i = 0; i < m_treeData.m_trees.length; i++) {
						float[] fHeights = new float[m_settings.m_nNrOfLabels * 2 - 1];
						float[] fMetas = new float[m_settings.m_nNrOfLabels * 2 - 1];
						int[] nCounts = new int[m_settings.m_nNrOfLabels * 2 - 1];
						collectHeights(m_treeData.m_trees[i], fHeights, 0);
						Arrays.sort(fHeights);
						m_treeData.collectMetaData(m_treeData.m_trees[i], fHeights, 0.0f, 0, fMetas, nCounts);
						m_treeData.m_fLinesX[i] = new float[nNodes * 2 + 2];
						m_treeData.m_fLinesY[i] = new float[nNodes * 2 + 2];
						for (int j = 0; j < fMetas.length - 1; j++) {
							m_treeData.m_fLinesX[i][j * 2] = fMetas[j];
							m_treeData.m_fLinesY[i][j * 2] = (fHeights[j] - m_fTreeOffset) * m_fTreeScale;
							m_treeData.m_fLinesX[i][j * 2 + 1] = fMetas[j + 1];
							m_treeData.m_fLinesY[i][j * 2 + 1] = (fHeights[j + 1] - m_fTreeOffset) * m_fTreeScale;
						}
						if (nMethod == NodeOrderer.META_AVERAGE) {
							for (int j = 0; j < fMetas.length - 1; j++) {
								if (nCounts[j] > 0) m_treeData.m_fLinesX[i][j * 2] = fMetas[j] / nCounts[j];
								if (nCounts[j + 1] > 0) m_treeData.m_fLinesX[i][j * 2 + 1] = fMetas[j + 1] / nCounts[j + 1];
							}
						}
					}
					for (int i = 0; i < m_treeData.m_cTrees.length; i++) {
						float[] fHeights = new float[m_settings.m_nNrOfLabels * 2 - 1];
						float[] fMetas = new float[m_settings.m_nNrOfLabels * 2 - 1];
						int[] nCounts = new int[m_settings.m_nNrOfLabels * 2 - 1];
						collectHeights(m_treeData.m_cTrees[i], fHeights, 0);
						Arrays.sort(fHeights);
						m_treeData.collectMetaData(m_treeData.m_cTrees[i], fHeights, 0.0f, 0, fMetas, nCounts);
						m_treeData.m_fCLinesX[i] = new float[nNodes * 2 + 2];
						m_treeData.m_fCLinesY[i] = new float[nNodes * 2 + 2];
						for (int j = 0; j < fMetas.length - 1; j++) {
							m_treeData.m_fCLinesX[i][j * 2] = fMetas[j];
							m_treeData.m_fCLinesY[i][j * 2] = (fHeights[j] - m_fTreeOffset) * m_fTreeScale;
							m_treeData.m_fCLinesX[i][j * 2 + 1] = fMetas[j + 1];
							m_treeData.m_fCLinesY[i][j * 2 + 1] = (fHeights[j + 1] - m_fTreeOffset) * m_fTreeScale;
						}
					}
					float fMaxX = 0;
					for (float[] fXs : m_treeData.m_fLinesX) {
						for (float f : fXs) fMaxX = Math.max(f, fMaxX);
					}
					for (float[] fXs : m_treeData.m_fCLinesX) {
						for (float f : fXs) fMaxX = Math.max(f, fMaxX);
					}
					float fScale = m_settings.m_nNrOfLabels / fMaxX;
					for (float[] fXs : m_treeData.m_fCLinesX) {
						for (int j = 0; j < fXs.length; j++) fXs[j] *= fScale;
					}
					for (float[] fXs : m_treeData.m_fLinesX) {
						for (int j = 0; j < fXs.length; j++) fXs[j] *= fScale;
					}
					break;
				}
			}
			if (orderChanged(oldOrder)) {
				makeDirty();
			}
		}
		setDefaultCursor();
	}

	void rotateAround(int iRotationPoint) {
		Vector<Integer> iLeafs = new Vector<>();
		getRotationLeafs(m_treeData.m_cTrees[0], -1, iLeafs, iRotationPoint);

		int iMin = m_settings.m_nOrder.length;
		int iMax = 0;
		for (Integer i : iLeafs) {
			int j = m_settings.m_nOrder[i];
			iMin = Math.min(j, iMin);
			iMax = Math.max(j, iMax);
		}
		for (int i = 0; i < (iMax - iMin) / 2 + 1; i++) {
			int nTmp = m_settings.m_nRevOrder[iMin + i];
			m_settings.m_nRevOrder[iMin + i] = m_settings.m_nRevOrder[iMax - i];
			m_settings.m_nRevOrder[iMax - i] = nTmp;
		}

		for (int i = 0; i < m_settings.m_sLabels.size(); i++) {
			m_settings.m_nOrder[m_settings.m_nRevOrder[i]] = i;
		}

		calcPositions();
		calcLines();
		makeDirty();
		addAction(new DoAction());
	}

	void moveRotationPoint(int iRotationPoint, float fdH) {
		Vector<Integer> iLeafs = new Vector<>();
		getRotationLeafs(m_treeData.m_cTrees[0], -1, iLeafs, iRotationPoint);
		boolean[] bSelection = m_treeData.m_bSelection;
		m_treeData.m_bSelection = new boolean[m_settings.m_sLabels.size()];
		for (int i : iLeafs) {
			m_treeData.m_bSelection[i] = true;
		}

		for (Node tree : m_treeData.m_trees) moveInternalNode(fdH, tree, iLeafs.size());
		for (Node cTree : m_treeData.m_cTrees) moveInternalNode(fdH, cTree, iLeafs.size());
		m_treeData.m_bSelection = bSelection;
		calcLines();
		makeDirty();
	}

	int moveInternalNode(float fdH, Node node, int nSelected) {
		if (node.isLeaf()) {
			return (m_treeData.m_bSelection[node.getNr()] ? 1 : 0);
		} else {
			int i = moveInternalNode(fdH, node.m_left, nSelected);
			i += moveInternalNode(fdH, node.m_right, nSelected);
			if (i == nSelected) {
				node.m_fPosX += fdH;
				i++;
			}
			return i;
		}
	}

	int getRotationLeafs(Node node, int iPos, Vector<Integer> iLeafs, int iRotationPoint) {
		if (node.isLeaf()) {
			iLeafs.add(node.getNr());
		} else {
			iPos = getRotationLeafs(node.m_left, iPos, iLeafs, iRotationPoint);
			if (iPos == iRotationPoint) return iPos;
			Vector<Integer> iLeafsR = new Vector<>();
			if (node.m_right != null) {
				iPos = getRotationLeafs(node.m_right, iPos, iLeafsR, iRotationPoint);
				if (iPos == iRotationPoint) {
					iLeafs.removeAllElements();
					iLeafs.addAll(iLeafsR);
					return iPos;
				}
			}
			iPos++;
			iLeafs.addAll(iLeafsR);
		}
		return iPos;
	}

	int getNrOfNodes(Node node) {
		if (node.isLeaf()) {
			return 1;
		} else {
			int nNodes = getNrOfNodes(node.m_left);
			if (node.m_right != null) {
				nNodes += getNrOfNodes(node.m_right);
			} else {
				nNodes++;
			}
			return nNodes + 1;
		}
	}

	public enum LineWidthMode {BY_METADATA_PATTERN, BY_METADATA_NUMBER, DEFAULT, BY_METADATA_TAG}
	public enum LineColorMode {COLOR_BY_CLADE, BY_METADATA_PATTERN, DEFAULT, COLOR_BY_METADATA_TAG}
	public enum MetaDataType {NUMERIC, STRING, SET}

	int initOrder(Node node, int iNr) throws Exception {
		if (node.isLeaf()) {
			m_settings.m_nOrder[node.m_iLabel] = iNr;
			m_settings.m_nRevOrder[iNr] = node.m_iLabel;
			return iNr + 1;
		} else {
			iNr = initOrder(node.m_left, iNr);
			if (node.m_right != null) {
				iNr = initOrder(node.m_right, iNr);
			}
		}
		return iNr;
	}

	void checkSelection() {
		m_treeData.checkSelection();
	}

	boolean moveSanityChek() {
		int nSelected = m_treeData.selectionSize();
		if (nSelected > 0 && nSelected < m_settings.m_nRevOrder.length - 1) {
			return true;
		}
		showErrorAlert("To move labels, select at least one, but not all of the labels");
		return false;
	}

	void moveSelectedLabelsDown() {
		for (int i = 1; i < m_settings.m_nRevOrder.length; i++) {
			if (m_treeData.m_bSelection[m_settings.m_nRevOrder[i]] && !m_treeData.m_bSelection[m_settings.m_nRevOrder[i - 1]]) {
				int h = m_settings.m_nRevOrder[i];
				m_settings.m_nRevOrder[i] = m_settings.m_nRevOrder[i - 1];
				m_settings.m_nRevOrder[i - 1] = h;
			}
		}
		for (int i = 0; i < m_settings.m_nRevOrder.length; i++) {
			m_settings.m_nOrder[m_settings.m_nRevOrder[i]] = i;
		}
		calcPositions();
		addAction(new DoAction());
	}

	void moveSelectedLabelsUp() {
		for (int i = m_settings.m_nRevOrder.length - 2; i >= 0; i--) {
			if (m_treeData.m_bSelection[m_settings.m_nRevOrder[i]] && !m_treeData.m_bSelection[m_settings.m_nRevOrder[i + 1]]) {
				int h = m_settings.m_nRevOrder[i];
				m_settings.m_nRevOrder[i] = m_settings.m_nRevOrder[i + 1];
				m_settings.m_nRevOrder[i + 1] = h;
			}
		}
		for (int i = 0; i < m_settings.m_nRevOrder.length; i++) {
			m_settings.m_nOrder[m_settings.m_nRevOrder[i]] = i;
		}
		calcPositions();
		addAction(new DoAction());
	}

	float getMetaData(Node node) {
		try {
			Matcher matcher = m_settings.m_pattern.matcher(node.getMetaData());
			matcher.find();
			int nGroup = 1;
			if (nGroup > matcher.groupCount()) {
				nGroup = 1;
			}
			return Float.parseFloat(matcher.group(nGroup));
		} catch (Exception ignored) {
		}
		return 1f;
	}

	int getMetaDataCategory(Node node) {
		try {
			Matcher matcher = m_settings.m_pattern.matcher(node.getMetaData());
			matcher.find();
			int nGroup = 1;
			if (nGroup > matcher.groupCount()) {
				nGroup = 1;
			}
			String match = matcher.group(nGroup);
			if (m_settings.m_colorMetaDataCategories.get(match) == null) {
				m_settings.m_colorMetaDataCategories.put(match, m_settings.m_colorMetaDataCategories.size());
			}
			return m_settings.m_colorMetaDataCategories.get(match);
		} catch (Exception ignored) {
		}
		return 0;
	}

	double positionMetaAll(Node node) {
		node.m_fPosX = getMetaData(node);
		if (!node.isLeaf()) {
			double fX1 = positionMetaAll(node.m_left);
			double fX2 = positionMetaAll(node.m_right);
			return Math.max(fX1, Math.max(fX2, node.m_fPosX));
		}
		return node.m_fPosX;
	}

	void scaleX(Node node, double fScale) {
		node.m_fPosX = (float) (m_settings.m_sLabels.size() - node.m_fPosX * fScale);
		if (!node.isLeaf()) {
			scaleX(node.m_left, fScale);
			scaleX(node.m_right, fScale);
		}
	}

	int collectHeights(Node node, float[] fHeights, int iPos) {
		fHeights[iPos++] = node.m_fPosY;
		if (!node.isLeaf()) {
			iPos = collectHeights(node.m_left, fHeights, iPos);
			iPos = collectHeights(node.m_right, fHeights, iPos);
		}
		return iPos;
	}

	public void offsetHeight(Node node, float f) {
		if (!node.isLeaf()) {
			offsetHeight(node.m_left, f);
			if (node.m_right != null) {
				offsetHeight(node.m_right, f);
			}
		}
		node.m_fPosY += f;
	}

	void drawLabels(Node node, Graphics2D g, TreeData treeData) {
		if (m_settings.m_bHideLabels || treeData.reverse()) {
			return;
		}
		g.setFont(m_font);
		if (m_settings.m_bShowBounds) {
			return;
		}
		if (Util.isAppleWithJava17() >= 1 && !isExporting()) {
			g.setTransform(new AffineTransform(2, 0, 0, 2, 0, 0));
		}
		if (node.isLeaf()) {
			if (treeData.m_bSelection[node.m_iLabel]) {
				if (m_settings.m_iColor == null) {
					g.setColor(m_settings.m_color[LABELCOLOR]);
				} else {
					g.setColor(m_settings.m_color[GEOCOLOR + m_settings.m_iColor[node.m_iLabel] % (m_settings.m_color.length - GEOCOLOR)]);
				}
			} else {
				g.setColor(Color.GRAY);
			}
			if (m_treeDrawer.m_bRootAtTop) {
				if (m_settings.m_bRotateTextWhenRootAtTop) {
					int x = (int) (node.m_fPosX * m_fScaleX) - g.getFontMetrics().getHeight() / 3;
					int y = getPosY(((m_bAlignLabels ? m_fHeight : node.m_fPosY) + m_settings.m_fLabelIndent - m_fTreeOffset) * m_fTreeScale) + 2;
					g.rotate(Math.PI / 2.0);
					g.translate(y, -x);
					g.drawString(m_settings.m_sLabels.elementAt(node.m_iLabel), 0, 0);
					g.translate(-y, x);
					g.rotate(-Math.PI / 2.0);
					Rectangle r = m_bLabelRectangle[node.m_iLabel];
					r.x = x;
					r.y = y - 10;
					r.height = 10;
					r.width = m_settings.m_nLabelWidth;
					drawImage(g, x, y, node.m_iLabel);
				} else {
					String sLabel = m_settings.m_sLabels.elementAt(node.m_iLabel);
					int x = (int) (node.m_fPosX * m_fScaleX) - g.getFontMetrics().stringWidth(sLabel) / 2;
					int y = getPosY(((m_bAlignLabels ? m_fHeight : node.m_fPosY) + m_settings.m_fLabelIndent - m_fTreeOffset) * m_fTreeScale)
							+ g.getFontMetrics().getHeight() + 2;
					g.drawString(sLabel, x, y);
					Rectangle r = m_bLabelRectangle[node.m_iLabel];
					r.x = x;
					r.y = y - 10;
					r.height = 10;
					r.width = m_settings.m_nLabelWidth;
					drawImage(g, x, y, node.m_iLabel);
				}
			} else {
				int y = (int) (node.m_fPosX * m_fScaleY) + g.getFontMetrics().getHeight() / 3;
				int x = getPosX(((m_bAlignLabels ? m_fHeight : node.m_fPosY) + m_settings.m_fLabelIndent - m_fTreeOffset) * m_fTreeScale) + 1;
				if (m_settings.m_bDrawReverse) {
					g.scale(-1.0, 1.0);
					String text = m_settings.m_sLabels.elementAt(node.m_iLabel);
					g.drawString(text, -x - g.getFontMetrics().stringWidth(text), y);
					g.scale(-1.0, 1.0);
				} else {
					switch (treeData.drawMode) {
						case TreeData.MODE_CENTRE:
							g.drawString(m_settings.m_sLabels.elementAt(node.m_iLabel), x, y);
							break;
						case TreeData.MODE_LEFT:
							String text = m_settings.m_sLabels.elementAt(node.m_iLabel);
							g.drawString(text, (int) getWidth() / 2 - g.getFontMetrics().stringWidth(text) / 2, y);
							break;
						case TreeData.MODE_RIGHT:
							break;
					}
				}
				Rectangle r = m_bLabelRectangle[node.m_iLabel];
				r.x = x;
				r.y = y - 10;
				r.height = 10;
				r.width = m_settings.m_nLabelWidth;
				drawImage(g, x, y, node.m_iLabel);
			}
		} else {
			drawLabels(node.m_left, g, treeData);
			if (node.m_right != null) {
				drawLabels(node.m_right, g, treeData);
			}
		}
	}

	private void drawImage(Graphics g, int x, int y, int iLabel) {
		if (m_settings.m_LabelImages != null && m_settings.m_LabelImages[iLabel] != null) {
			BufferedImage img = m_settings.m_LabelImages[iLabel];
			g.drawImage(img, x, y - m_settings.m_nImageSize, x + m_settings.m_nImageSize, y, 0, 0, img.getWidth(), img.getHeight(), null);
		}
	}

	void drawGeo(Node node, Graphics g) {
		if (node.isLeaf()) {
			if (m_treeData.m_bSelection[node.m_iLabel]) {
				if (m_settings.m_fLongitude.elementAt(node.m_iLabel) == 0 && m_settings.m_fLatitude.elementAt(node.m_iLabel) == 0) {
					return;
				}
				int gx = (int) ((m_settings.m_fLongitude.elementAt(node.m_iLabel) - m_settings.m_fMinLong) * m_fScaleGX * m_fScale);
				int gy = (int) ((m_settings.m_fMaxLat - m_settings.m_fLatitude.elementAt(node.m_iLabel)) * m_fScaleGY * m_fScale);
				g.setColor(m_settings.m_color[GEOCOLOR]);
				if (m_treeDrawer.m_bRootAtTop) {
					int x = (int) (node.m_fPosX * m_fScaleX * m_fScale) + 10;
					int y = getPosY(node.m_fPosY * m_fScale);
					g.drawLine(x, y, gx, gy);
					g.setColor(Color.BLACK);
					g.drawOval(gx - 1, gy - 1, 3, 3);
				} else {
					int y = (int) (node.m_fPosX * m_fScaleY * m_fScale);
					int x = getPosX(node.m_fPosY * m_fScale);
					if (m_settings.m_bInvertLongitude) {
						x = 0;
					}
					g.drawLine(x, y, gx, gy);
					g.setColor(Color.BLACK);
					g.drawOval(gx - 1, gy - 1, 3, 3);
				}
				Rectangle r = m_bGeoRectangle[node.m_iLabel];
				r.x = gx - 2;
				r.y = gy - 2;
				r.width = 5;
				r.height = 5;
			}
		} else {
			drawGeo(node.m_left, g);
			drawGeo(node.m_right, g);
		}
	}

	int getPosY(float fHeight) {
		if (m_settings.m_bUseLogScale) {
			return (int) (m_fHeight / Math.log(m_fHeight + 1.0) * m_fScaleY * (Math.log(m_fHeight + 1.0) - Math.log(m_fHeight - fHeight + 1.0)));
		}
		return (int) (fHeight * m_fScaleY);
	}

	float screenPosToHeight(int nX, int nY) {
		if (m_settings.m_bUseLogScale) {
			return Float.NaN;
		}
		if (m_treeDrawer.m_bRootAtTop) {
			return (m_fHeight - ((nY / m_fScaleY) + m_fTreeOffset)) * m_fUserScale;
		} else {
			return (m_fHeight - ((nX / m_fScaleX) + m_fTreeOffset)) * m_fUserScale;
		}
	}

	int getPosX(float fHeight) {
		if (m_settings.m_bUseLogScale) {
			return (int) ((m_fHeight / Math.log(m_fHeight + 1.0) * m_fScaleX * (Math.log(m_fHeight + 1.0) - Math
					.log(m_fHeight - fHeight + 1.0))));
		}
		return (int) (fHeight * m_fScaleX);
	}

	void selectMode(int nXmode) {
		switch (nXmode) {
			case 0:
				m_settings.m_Xmode = 0;
				m_settings.m_bUseAngleCorrection = false;
				m_treeDrawer.m_bViewBlockTree = false;
				m_viewClades.setDisable(true);
				m_viewEditTree.setDisable(false);
				break;
			case 1:
				m_settings.m_Xmode = 2;
				m_settings.m_bUseAngleCorrection = false;
				m_treeDrawer.m_bViewBlockTree = false;
				m_viewClades.setDisable(false);
				m_viewEditTree.setDisable(true);
				break;
			case 2:
				m_settings.m_Xmode = 1;
				m_settings.m_bUseAngleCorrection = false;
				m_treeDrawer.m_bViewBlockTree = false;
				m_viewClades.setDisable(false);
				m_viewEditTree.setDisable(true);
				break;
			case 3:
				m_settings.m_Xmode = 1;
				m_settings.m_bUseAngleCorrection = true;
				m_treeDrawer.m_bViewBlockTree = false;
				m_viewClades.setDisable(false);
				m_viewEditTree.setDisable(true);
				break;
		}

		calcPositions();
		calcLines();
		makeDirty();
		notifyChangeListeners();
	}

	public void resetStyle() {
		setStyle(m_nStyle);
	}

	void setStyle(int nStyle) {
		m_nStyle = nStyle;
		BranchDrawer bd = null;
		switch (nStyle) {
			case 0:
				bd = (m_settings.m_lineWidthMode != LineWidthMode.DEFAULT) ? new TrapeziumBranchDrawer() : new BranchDrawer();
				m_treeDrawer.m_bViewBlockTree = false;
				break;
			case 1:
				bd = (m_settings.m_lineWidthMode != LineWidthMode.DEFAULT) ? new TrapeziumBranchDrawer() : new BranchDrawer();
				m_treeDrawer.m_bViewBlockTree = true;
				break;
			case 2:
				bd = new ArcBranchDrawer();
				m_treeDrawer.m_bViewBlockTree = false;
				break;
			case 3:
				bd = new SteepArcBranchDrawer();
				m_treeDrawer.m_bViewBlockTree = false;
				break;
		}
		if (bd != null) {
			m_treeDrawer.setBranchDrawer(bd);
			makeDirty();
		}
	}

	int m_iUndo = 0;
	Vector<DoAction> m_doActions = new Vector<>();

	class DoAction {
		int[] m_nOrder2;
		int[] m_nRevOrder2;
		float[] m_fPosX;

		DoAction() {
			m_nOrder2 = m_settings.m_nOrder.clone();
			m_nRevOrder2 = m_settings.m_nRevOrder.clone();
			m_fPosX = new float[m_settings.m_sLabels.size()];
			getPosition(m_treeData.m_trees[0], m_fPosX);
		}

		void doThisAction() {
			m_settings.m_nOrder = m_nOrder2.clone();
			m_settings.m_nRevOrder = m_nRevOrder2.clone();
			for (Node tree : m_treeData.m_trees) {
				setPosition(tree, m_fPosX);
				positionRest(tree);
			}
			for (Node cTree : m_treeData.m_cTrees) {
				setPosition(cTree, m_fPosX);
				positionRest(cTree);
			}
			calcLines();
			makeDirty();
		}
	}

	void addAction(DoAction action) {
		while (m_iUndo < m_doActions.size()) {
			m_doActions.remove(m_iUndo);
		}
		m_doActions.add(action);
		m_iUndo++;
	}

	public void setCladeComparisonVisible(boolean visible) {
		Platform.runLater(() -> {
			if (visible) {
				if (!m_centerSplitPane.getItems().contains(m_cladeComparisonSwingNode)) {
					m_centerSplitPane.getItems().add(0, m_cladeComparisonSwingNode);
					m_centerSplitPane.setDividerPositions(0.4);
				}
			} else {
				m_centerSplitPane.getItems().remove(m_cladeComparisonSwingNode);
			}
		});
		SwingUtilities.invokeLater(() -> m_cladeSetComparisonPanel.setVisible(visible));
	}

	public void setCladeToolsVisible(boolean visible) {
		Platform.runLater(() -> {
			if (visible) {
				if (!m_mainSplitPane.getItems().contains(m_cladeToolsPane)) {
					m_mainSplitPane.getItems().add(m_cladeToolsPane);
					m_mainSplitPane.setDividerPositions(0.8);
				}
			} else {
				m_mainSplitPane.getItems().remove(m_cladeToolsPane);
			}
		});
	}

	Button createToolbarButton(String iconName, String tooltipText, Runnable action) {
		Button btn = new Button();
		btn.setPadding(new Insets(2));
		btn.setFocusTraversable(false);
		ImageView iv = getFxIconView(iconName);
		if (iv != null) btn.setGraphic(iv);
		if (tooltipText != null) btn.setTooltip(new Tooltip(tooltipText));
		btn.setOnAction(e -> action.run());
		return btn;
	}

	void makeToolbar() {
		m_tbTools.getItems().addAll(
				createToolbarButton("open", "Load tree set", () -> doOpenAction()),
				new Separator(),
				createToolbarButton("redraw", "Draw Tree Set", () -> {
					m_viewMode = ViewMode.DRAW;
					if (m_bIsDirty) calcLines();
					SwingUtilities.invokeLater(() -> m_Panel.clearImage());
					repaint();
				}),
				new Separator(),
				createToolbarButton("browsefirst", "Browse First", () -> {
					m_viewMode = ViewMode.BROWSE;
					m_iAnimateTree = 0;
					repaint();
				}),
				createToolbarButton("browseprev", "Browse Prev", () -> {
					m_viewMode = ViewMode.BROWSE;
					m_iAnimateTree = Math.max(0, m_iAnimateTree - 1);
					repaint();
				}),
				createToolbarButton("start", "Start Animation", () -> {
					if (m_viewMode == ViewMode.ANIMATE) {
						m_viewMode = ViewMode.BROWSE;
					} else {
						if (m_viewMode != ViewMode.BROWSE) m_iAnimateTree = 0;
						m_viewMode = ViewMode.ANIMATE;
					}
					repaint();
				}),
				createToolbarButton("browsenext", "Browse Next", () -> {
					m_viewMode = ViewMode.BROWSE;
					m_iAnimateTree = Math.min(m_treeData.m_nTopologies - 1, m_iAnimateTree + 1);
					repaint();
				}),
				createToolbarButton("browselast", "Browse Last", () -> {
					m_viewMode = ViewMode.BROWSE;
					m_iAnimateTree = m_treeData.m_nTopologies - 1;
					repaint();
				}),
				new Separator(),
				createToolbarButton("intensityup", "Increase Tree Intensity", () -> {
					m_settings.m_fTreeIntensity *= 1.1;
					makeDirty();
				}),
				createToolbarButton("intensitydown", "Decrease Tree Intensity", () -> {
					m_settings.m_fTreeIntensity /= 1.1;
					makeDirty();
				}),
				createToolbarButton("cintensityup", "Increase Consensus Tree Intensity", () -> {
					m_settings.m_fCTreeIntensity *= 1.1;
					makeDirty();
				}),
				createToolbarButton("cintensitydown", "Decrease Consensus Tree Intensity", () -> {
					m_settings.m_fCTreeIntensity /= 1.1;
					makeDirty();
				}),
				new Separator(),
				createToolbarButton("treewidthup", "Increase Tree Width", () -> {
					m_settings.m_nTreeWidth++;
					makeDirty();
				}),
				createToolbarButton("treewidthdown", "Decrease Tree Width", () -> {
					m_settings.m_nTreeWidth = Math.max(1, m_settings.m_nTreeWidth - 1);
					makeDirty();
				}),
				createToolbarButton("ctreewidthup", "Increase Consensus Tree Width", () -> {
					m_settings.m_nCTreeWidth++;
					makeDirty();
				}),
				createToolbarButton("ctreewidthdown", "Decrease Consensus Tree Width", () -> {
					m_settings.m_nCTreeWidth = Math.max(1, m_settings.m_nCTreeWidth - 1);
					makeDirty();
				}),
				new Separator(),
				createToolbarButton("aspeeddown", "Decrease Animation Speed", () -> m_nAnimationDelay += 1 + m_nAnimationDelay / 10),
				createToolbarButton("aspeedup", "Increase Animation Speed", () -> {
					if (m_nAnimationDelay > 0) m_nAnimationDelay -= 1 + m_nAnimationDelay / 10;
				}),
				new Separator(),
				createToolbarButton("jitterup", "Increase Jitter", () -> {
					m_settings.m_nJitter++;
					makeDirty();
				}),
				createToolbarButton("jitterdown", "Decrease Jitter", () -> {
					m_settings.m_nJitter = Math.max(0, m_settings.m_nJitter - 1);
					makeDirty();
				}),
				new Separator(),
				createToolbarButton("help", "Help", () -> showHelpDialog())
		);

		TilePane typePanel = new TilePane();
		typePanel.setPrefColumns(2);
		typePanel.setPadding(new Insets(3, B, 5, B));
		typePanel.setHgap(4);
		typePanel.setVgap(4);
		typePanel.getChildren().addAll(
				createToolbarButton("modedefault", "Default Mode", () -> selectMode(0)),
				createToolbarButton("modestar", "Star Mode", () -> selectMode(1)),
				createToolbarButton("modecentralised", "Centralised Mode", () -> selectMode(2)),
				createToolbarButton("modeanglecorrected", "Angle Corrected Mode", () -> selectMode(3))
		);

		TilePane stylePanel = new TilePane();
		stylePanel.setPrefColumns(2);
		stylePanel.setPadding(new Insets(3, B, 5, B));
		stylePanel.setHgap(4);
		stylePanel.setVgap(4);
		stylePanel.getChildren().addAll(
				createToolbarButton("stylestraight", "Straight Style", () -> setStyle(0)),
				createToolbarButton("styleblock", "Block Style", () -> setStyle(1)),
				createToolbarButton("stylearced", "Arc Style", () -> setStyle(2)),
				createToolbarButton("stylesteep", "Steep Arc Style", () -> setStyle(3))
		);

		VBox sidebarVBox = new VBox(2);
		sidebarVBox.setPadding(new Insets(5));
		sidebarVBox.setFillWidth(true);
		sidebarVBox.getChildren().addAll(
				new ExpandablePanel("Type", typePanel, true),
				new ExpandablePanel("Style", stylePanel, true),
				new ExpandablePanel("Show", new ShowPanel(this)),
				new ExpandablePanel("Grid", new GridPanel(this)),
				new ExpandablePanel("Label", new LabelPanel(this)),
				new ExpandablePanel("Geography", new GeoPanel(this)),
				new ExpandablePanel("Line Width", new LineWidthPanel(this)),
				new ExpandablePanel("Line Color", new ColorPanel(this)),
				new ExpandablePanel("Burn in", new BurninPanel(this)),
				new ExpandablePanel("Rogues", new RoguePanel(this)),
				new ExpandablePanel("Clades", new CladePanel(this))
		);

		ScrollPane sidebarScroll = new ScrollPane(sidebarVBox);
		sidebarScroll.setFitToWidth(true);
		sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		m_tbTools2.getChildren().add(sidebarScroll);

		m_treeData.m_cladelist = new ListView<>(m_treeData.m_cladelistmodel);
		m_treeData.m_cladelist.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		m_treeData.m_cladelist.getSelectionModel().selectedItemProperty().addListener(e -> {
			if (m_treeData.m_bAllowCladeSelection) {
				m_treeData.getCladeSelection().clear();
				if (m_treeData2 != null) {
					m_treeData2.getCladeSelection().clear();
				}
				for (int i : m_treeData.m_cladelist.getSelectionModel().getSelectedIndices()) {
					if (m_treeData.m_cladeWeight.get(i) > 0.01 && ((m_settings.m_Xmode == 1 && m_treeData.m_clades.get(i).length > 1) || (m_settings.m_Xmode == 2 && m_treeData.m_clades.get(i).length == 1))) {
						addCladeToSelection(i, false);
					}
				}
				resetCladeSelection();
				repaint();
			}
		});

		m_cladeToolsPane.setCenter(m_treeData.m_cladelist);
	}

	public void removeCladeFromselection(int i, boolean reverse) {
		if (!reverse) {
			m_treeData.getCladeSelection().remove(i);
			int[] clade = m_treeData.m_clades.get(i);
			int j = findClade(m_treeData2, clade);
			if (j >= 0) m_treeData2.getCladeSelection().remove(j);
		} else {
			m_treeData2.getCladeSelection().remove(i);
			int[] clade2 = m_treeData2.m_clades.get(i);
			int j = findClade(m_treeData, clade2);
			if (j >= 0) m_treeData.getCladeSelection().remove(j);
		}
	}

	public void addCladeToSelection(int i, boolean reverse) {
		if (!reverse) {
			m_treeData.getCladeSelection().add(i);
			int[] clade = m_treeData.m_clades.get(i);
			int j = findClade(m_treeData2, clade);
			if (j >= 0) m_treeData2.getCladeSelection().add(j);
		} else {
			m_treeData2.getCladeSelection().add(i);
			int[] clade2 = m_treeData2.m_clades.get(i);
			int j = findClade(m_treeData, clade2);
			if (j >= 0) m_treeData.getCladeSelection().add(j);
		}
	}

	private int findClade(TreeData data, int[] clade2) {
		if (data == null) return -1;
		for (int j = 0; j < data.m_clades.size(); j++) {
			int[] clade1 = data.m_clades.get(j);
			if (clade1.length == clade2.length) {
				boolean matches = true;
				for (int k = 0; k < clade1.length; k++) {
					if (clade1[k] != clade2[k]) {
						matches = false;
						break;
					}
				}
				if (matches) return j;
			}
		}
		return -1;
	}

	MenuItem createMenuItem(String text, String iconName, KeyCombination accelerator, Runnable action) {
		MenuItem item = new MenuItem(text);
		if (iconName != null) item.setGraphic(getFxIconView(iconName));
		if (accelerator != null) item.setAccelerator(accelerator);
		item.setOnAction(e -> action.run());
		return item;
	}

	MenuBar makeMenuBar() {
		MenuBar mb = new MenuBar();

		Menu fileMenu = new Menu("File");
		fileMenu.getItems().addAll(
				createMenuItem("New", "new", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), () -> startNew(new String[0])),
				createMenuItem("Load", "open", new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), () -> doOpenAction()),
				createMenuItem("Load mirror set", "open", null, () -> doOpenMirrorAction()),
				createMenuItem("Save as", "save", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), () -> doSaveAsAction()),
				createMenuItem("Background image", "bgimage", null, () -> doLoadBgImageAction()),
				new SeparatorMenuItem(),
				createMenuItem("Print", "print", new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN), () -> doPrintAction()),
				createMenuItem("Export", "export", null, () -> doExportAction(m_Panel, "Export DensiTree As")),
				createMenuItem("Export comparison", "exportcc", null, () -> doExportAction(m_cladeSetComparisonPanel, "Export Clade Comparison As")),
				new SeparatorMenuItem(),
				createMenuItem("Exit", "exit", null, () -> System.exit(0))
		);

		Menu editMenu = new Menu("Edit");
		editMenu.getItems().addAll(
				createMenuItem("Undo", "udno", new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), () -> {
					if (m_iUndo > 0) {
						m_iUndo--;
						m_doActions.elementAt(m_iUndo - 1).doThisAction();
						repaint();
					}
				}),
				createMenuItem("Redo", "reno", new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN), () -> {
					if (m_iUndo < m_doActions.size()) {
						m_iUndo++;
						m_doActions.elementAt(m_iUndo - 1).doThisAction();
						repaint();
					}
				}),
				createMenuItem("Select All", "selectall", new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN), () -> {
					for (int i = 0; i < m_treeData.m_bSelection.length; i++) m_treeData.m_bSelection[i] = true;
					if (m_treeData2 != null) {
						for (int i = 0; i < m_treeData2.m_bSelection.length; i++) m_treeData2.m_bSelection[i] = true;
					}
					repaint();
				}),
				createMenuItem("Unselect All", "unselectall", new KeyCodeCombination(KeyCode.U, KeyCombination.SHORTCUT_DOWN), () -> {
					for (int i = 0; i < m_treeData.m_bSelection.length; i++) m_treeData.m_bSelection[i] = false;
					repaint();
				}),
				createMenuItem("Delete", "del", null, () -> deleteSelected()),
				createMenuItem("Paste", "paste", new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), () -> pasteFromClipboard()),
				createMenuItem("Move labels up", "moveup", new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN), () -> {
					if (!moveSanityChek()) return;
					moveSelectedLabelsUp();
					calcLines();
					SwingUtilities.invokeLater(() -> m_Panel.clearImage());
					repaint();
				}),
				createMenuItem("Move labels down", "movedown", new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN), () -> {
					if (!moveSanityChek()) return;
					moveSelectedLabelsDown();
					calcLines();
					SwingUtilities.invokeLater(() -> m_Panel.clearImage());
					repaint();
				})
		);

		m_viewEditTree = new CheckMenuItem("Show Edit Tree");
		m_viewEditTree.setSelected(m_settings.m_bViewEditTree);
		m_viewEditTree.setOnAction(e -> {
			m_settings.m_bViewEditTree = m_viewEditTree.isSelected();
			makeDirty();
		});

		m_viewClades = new CheckMenuItem("Show Clades");
		m_viewClades.setSelected(m_settings.m_bViewClades);
		m_viewClades.setDisable(true);
		m_viewClades.setOnAction(e -> {
			m_settings.m_bViewClades = m_viewClades.isSelected();
			makeDirty();
		});

		editMenu.getItems().addAll(m_viewEditTree, m_viewClades);

		Menu shuffleMenu = new Menu("Shuffle");
		shuffleMenu.getItems().addAll(
				createMenuItem("Most Frequent", null, null, () -> reshuffle(NodeOrderer.DEFAULT)),
				createMenuItem("SPQ", null, new KeyCodeCombination(KeyCode.Q, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.SPQ)),
				createMenuItem("Closest Outside First", null, new KeyCodeCombination(KeyCode.S, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.CLOSEST_OUTSIDE_FIRST)),
				createMenuItem("Optimised root canal tree", null, new KeyCodeCombination(KeyCode.O, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.OPTIMISE)),
				createMenuItem("Sorted root canal tree", null, new KeyCodeCombination(KeyCode.R, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.SORT_BY_ROOT_CANAL_LENGTH)),
				createMenuItem("Closest First", null, new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.CLOSEST_FIRST)),
				createMenuItem("Single link", null, new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.SINGLE)),
				createMenuItem("Complete link", null, new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.COMPLETE)),
				createMenuItem("Average link", null, new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.AVERAGE)),
				createMenuItem("Mean link", null, new KeyCodeCombination(KeyCode.DIGIT5, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.MEAN)),
				createMenuItem("Adjusted complete link", null, new KeyCodeCombination(KeyCode.DIGIT6, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.ADJCOMLPETE)),
				new SeparatorMenuItem(),
				createMenuItem("Manual", null, null, () -> reshuffle(NodeOrderer.MANUAL)),
				createMenuItem("By Geography", null, null, () -> reshuffle(NodeOrderer.GEOINFO)),
				createMenuItem("By meta data, all", null, new KeyCodeCombination(KeyCode.DIGIT7, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.META_ALL)),
				createMenuItem("By meta data, sum", null, new KeyCodeCombination(KeyCode.DIGIT8, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.META_SUM)),
				createMenuItem("By meta data, mean", null, new KeyCodeCombination(KeyCode.DIGIT9, KeyCombination.ALT_DOWN), () -> reshuffle(NodeOrderer.META_AVERAGE))
		);

		editMenu.getItems().addAll(new SeparatorMenuItem(), shuffleMenu);

		Menu drawallMenu = new Menu("Draw All");
		CheckMenuItem autoRefreshItem = new CheckMenuItem("Automatically refresh");
		autoRefreshItem.setSelected(m_bAutoRefresh);
		autoRefreshItem.setOnAction(e -> {
			m_bAutoRefresh = autoRefreshItem.isSelected();
			if (m_bAutoRefresh && m_bIsDirty) fitToScreen();
		});
		drawallMenu.getItems().addAll(autoRefreshItem,
				createMenuItem("Draw Tree Set", "redraw", new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN), () -> {
					m_viewMode = ViewMode.DRAW;
					if (m_bIsDirty) calcLines();
					SwingUtilities.invokeLater(() -> m_Panel.clearImage());
					repaint();
				})
		);

		Menu browseMenu = new Menu("Browse");
		browseMenu.getItems().addAll(
				createMenuItem("Browse First", "browsefirst", null, () -> {
					m_viewMode = ViewMode.BROWSE;
					m_iAnimateTree = 0;
					repaint();
				}),
				createMenuItem("Browse Prev", "browseprev", new KeyCodeCombination(KeyCode.P, KeyCombination.ALT_DOWN), () -> {
					m_viewMode = ViewMode.BROWSE;
					m_iAnimateTree = Math.max(0, m_iAnimateTree - 1);
					repaint();
				}),
				createMenuItem("Start", "start", new KeyCodeCombination(KeyCode.D, KeyCombination.ALT_DOWN), () -> {
					if (m_viewMode == ViewMode.ANIMATE) {
						m_viewMode = ViewMode.BROWSE;
					} else {
						if (m_viewMode != ViewMode.BROWSE) m_iAnimateTree = 0;
						m_viewMode = ViewMode.ANIMATE;
					}
					repaint();
				}),
				createMenuItem("Browse Next", "browsenext", new KeyCodeCombination(KeyCode.N, KeyCombination.ALT_DOWN), () -> {
					m_viewMode = ViewMode.BROWSE;
					m_iAnimateTree = Math.min(m_treeData.m_nTopologies - 1, m_iAnimateTree + 1);
					repaint();
				}),
				createMenuItem("Browse Last", "browselast", null, () -> {
					m_viewMode = ViewMode.BROWSE;
					m_iAnimateTree = m_treeData.m_nTopologies - 1;
					repaint();
				}),
				new SeparatorMenuItem()
		);
		CheckMenuItem animateOverwriteItem = new CheckMenuItem("Over write");
		animateOverwriteItem.setSelected(m_bAnimateOverwrite);
		animateOverwriteItem.setOnAction(e -> m_bAnimateOverwrite = animateOverwriteItem.isSelected());
		browseMenu.getItems().add(animateOverwriteItem);

		Menu windowMenu = new Menu("Window");
		CheckMenuItem viewStatusbarItem = new CheckMenuItem("View statusbar");
		viewStatusbarItem.setSelected(true);
		viewStatusbarItem.setOnAction(e -> m_jStatusBar.setVisible(viewStatusbarItem.isSelected()));

		CheckMenuItem viewToolbarItem = new CheckMenuItem("View toolbar");
		viewToolbarItem.setSelected(true);
		viewToolbarItem.setOnAction(e -> m_tbTools.setVisible(viewToolbarItem.isSelected()));

		CheckMenuItem viewSidebarItem = new CheckMenuItem("View Sidebar");
		viewSidebarItem.setSelected(true);
		viewSidebarItem.setOnAction(e -> m_tbTools2.setVisible(viewSidebarItem.isSelected()));

		CheckMenuItem viewCladeToolbarItem = new CheckMenuItem("View clade toolbar");
		viewCladeToolbarItem.setSelected(false);
		viewCladeToolbarItem.setOnAction(e -> setCladeToolsVisible(viewCladeToolbarItem.isSelected()));

		CheckMenuItem viewComparisonItem = new CheckMenuItem("View clade comparison");
		viewComparisonItem.setSelected(false);
		viewComparisonItem.setOnAction(e -> setCladeComparisonVisible(viewComparisonItem.isSelected()));

		windowMenu.getItems().addAll(
				viewStatusbarItem, viewToolbarItem, viewSidebarItem, viewCladeToolbarItem, viewComparisonItem,
				new SeparatorMenuItem(),
				createMenuItem("Zoom in", "zoomin", new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), () -> {
					m_fScale *= 1.2;
					fitToScreen();
					updateStatus("Zooming in");
				}),
				createMenuItem("Zoom out", "zoomout", new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), () -> {
					m_fScale /= 1.2;
					if (m_fScale <= 1.000001) m_fScale = 1.0f;
					fitToScreen();
					updateStatus("Zooming out");
				}),
				createMenuItem("Zoom in height", "zoominh", new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN), () -> {
					m_fTreeScale *= 1.2;
					m_fTreeOffset = m_fHeight - m_fHeight / m_fTreeScale;
					calcLines();
					SwingUtilities.invokeLater(() -> m_Panel.clearImage());
					makeDirty();
					updateStatus("Zooming in tree height");
				}),
				createMenuItem("Zoom out height", "zoomouth", new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN), () -> {
					m_fTreeScale /= 1.2;
					if (m_fTreeScale <= 1.000001) m_fTreeScale = 1.0f;
					m_fTreeOffset = m_fHeight - m_fHeight / m_fTreeScale;
					calcLines();
					SwingUtilities.invokeLater(() -> m_Panel.clearImage());
					makeDirty();
					updateStatus("Zooming out tree height");
				})
		);

		Menu helpMenu = new Menu("Help");
		helpMenu.getItems().addAll(
				createMenuItem("Help", "help", null, () -> showHelpDialog()),
				createMenuItem("View clades", "viewclades", null, () -> showCladesDialog()),
				createMenuItem("About", "about", null, () -> showAboutDialog())
		);

		mb.getMenus().addAll(fileMenu, editMenu, drawallMenu, browseMenu, windowMenu, helpMenu);
		return mb;
	}

	void doOpenAction() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Load Tree Set");
		if (m_settings.m_sDir != null && new File(m_settings.m_sDir).exists()) {
			fc.setInitialDirectory(new File(m_settings.m_sDir));
		}
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Nexus / Tree Files", "*.trees", "*.tre", "*.nex", "*.t", "*.tree", "*.nwk", "*.txt"));
		File file = fc.showOpenDialog(stage);
		if (file != null) doOpen(file.getPath());
	}

	public void doOpen(String sFileName) {
		if (sFileName.lastIndexOf('/') > 0) m_settings.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
		try {
			init(sFileName);
			m_treeData.drawMode = TreeData.MODE_CENTRE;
			m_treeData2 = null;
			calcLines();
		} catch (Exception e) {
			e.printStackTrace();
			showErrorAlert("Error loading file: " + e.getMessage());
			return;
		}
		updateStatus("Loaded " + sFileName);
		fitToScreen();
	}

	void doOpenMirrorAction() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Load Mirror Tree Set");
		if (m_settings.m_sDir != null && new File(m_settings.m_sDir).exists()) {
			fc.setInitialDirectory(new File(m_settings.m_sDir));
		}
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Nexus Trees Files", "*.trees", "*.tre", "*.nex", "*.t", "*.tree"));
		File file = fc.showOpenDialog(stage);
		if (file != null) doOpenMirror(file.getPath());
	}

	public void doOpenMirror(String sFileName) {
		if (sFileName.lastIndexOf('/') > 0) m_settings.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
		try {
			while (thread != null) Thread.sleep(100);

			m_treeData2 = new TreeData(this, this.m_settings);
			if (!m_treeData2.loadFromFile(sFileName, false)) {
				m_treeData2 = null;
				return;
			}
			m_sFileName2 = sFileName;
			m_treeData.drawMode = TreeData.MODE_LEFT;
			m_treeData2.drawMode = TreeData.MODE_RIGHT;

			thread = new MetaDataThread(m_treeData2, this);
			thread.start();
		} catch (Exception e) {
			e.printStackTrace();
			showErrorAlert("Error loading file: " + e.getMessage());
			return;
		}
		updateStatus("Loaded " + sFileName);
		fitToScreen();
	}

	void doSaveAsAction() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Save Graph");
		if (m_settings.m_sDir != null && new File(m_settings.m_sDir).exists()) {
			fc.setInitialDirectory(new File(m_settings.m_sDir));
		}
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Nexus Trees Files", "*.trees", "*.tre", "*.nex", "*.t"));
		File file = fc.showSaveDialog(stage);
		if (file != null) {
			String sFileName = file.getAbsolutePath();
			try (FileWriter outfile = new FileWriter(sFileName)) {
				StringBuilder buf = new StringBuilder();
				buf.append("#NEXUS\nBegin trees\n\tTranslate\n");
				for (int i = 0; i < m_settings.m_sLabels.size(); i++) {
					buf.append("\t\t").append(i).append(" ").append(m_settings.m_sLabels.get(i));
					if (i < m_settings.m_sLabels.size() - 1) buf.append(",");
					buf.append("\n");
				}
				buf.append(";\n");
				outfile.write(buf.toString());
				for (int i = 0; i < m_treeData.m_trees.length; i++) {
					outfile.write("tree STATE_" + i + " = " + m_treeData.m_trees[i].toString() + ";\n");
				}
				outfile.write("End;\n");
				updateStatus("Saved " + sFileName);
			} catch (Exception e) {
				e.printStackTrace();
				showErrorAlert("Error writing file: " + e.getMessage());
			}
		}
	}

	void doLoadBgImageAction() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Load Background Image");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.gif"));
		File file = fc.showOpenDialog(stage);
		if (file != null) {
			try {
				loadBGImage(file.getAbsolutePath());
				makeDirty();
			} catch (Exception e) {
				showErrorAlert("Error loading file: " + e.getMessage());
			}
		}
	}

	void loadBGImage(String sFileName) throws Exception {
		m_bgImage = ImageIO.read(new File(sFileName));
		try {
			Pattern pattern = Pattern.compile(".*\\(([0-9\\.Ee-]+),([0-9\\.Ee-]+)\\)x\\(([0-9\\.Ee-]+),([0-9\\.Ee-]+)\\).*");
			Matcher matcher = pattern.matcher(sFileName);
			matcher.find();
			m_fBGImageBox[1] = Float.parseFloat(matcher.group(1));
			m_fBGImageBox[0] = Float.parseFloat(matcher.group(2));
			m_fBGImageBox[3] = Float.parseFloat(matcher.group(3));
			m_fBGImageBox[2] = Float.parseFloat(matcher.group(4));
		} catch (Exception e) {
			m_fBGImageBox = new double[]{-180, -90, 180, 90};
		}
	}

	void doExportAction(JComponent componentToExport, String title) {
		if (componentToExport == m_cladeSetComparisonPanel && (m_treeData2 == null || !m_cladeSetComparisonPanel.isVisible())) {
			showWarningAlert("Load mirror tree set and open clade set comparison panel before exporting");
			return;
		}

		FileChooser fc = new FileChooser();
		fc.setTitle(title);
		fc.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("PNG Bitmap Files (*.png)", "*.png"),
				new FileChooser.ExtensionFilter("JPEG Bitmap Files (*.jpg)", "*.jpg"),
				new FileChooser.ExtensionFilter("Bitmap Files (*.bmp)", "*.bmp"),
				new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf")
		);
		File file = fc.showSaveDialog(stage);
		if (file != null) {
			String sFileName = file.getAbsolutePath();
			if (sFileName.toLowerCase().endsWith(".pdf")) {
				exportPDF(sFileName, componentToExport);
			} else {
				BufferedImage bi = new BufferedImage(componentToExport.getWidth(), componentToExport.getHeight(), BufferedImage.TYPE_INT_RGB);
				Graphics g = bi.getGraphics();
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, componentToExport.getWidth(), componentToExport.getHeight());
				componentToExport.printAll(g);
				try {
					String ext = sFileName.substring(sFileName.lastIndexOf('.') + 1);
					ImageIO.write(bi, ext, new File(sFileName));
				} catch (Exception e) {
					showErrorAlert(sFileName + " was not written properly: " + e.getMessage());
				}
			}
		}
	}

	void exportPDF(String sFileName, JComponent panel) {
		isExporting = true;
		try {
			com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(sFileName));
			doc.setPageSize(new com.itextpdf.text.Rectangle(panel.getWidth(), panel.getHeight()));
			doc.open();
			PdfContentByte cb = writer.getDirectContent();
			Graphics2D g = new PdfGraphics2D(cb, panel.getWidth(), panel.getHeight());
			g.setPaintMode();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, panel.getWidth(), panel.getHeight());
			panel.paint(g);
			g.dispose();
			doc.close();
		} catch (Exception e) {
			showErrorAlert("Export may have failed: " + e.getMessage());
		}
		isExporting = false;
	}

	void doPrintAction() {
		PrinterJob printJob = PrinterJob.getPrinterJob();
		printJob.setPrintable(m_Panel);
		if (printJob.printDialog()) {
			try {
				printJob.print();
			} catch (PrinterException ignored) {
			}
		}
	}

	void deleteSelected() {
		int nDeleted = 0;
		for (int i = m_treeData.m_bSelection.length - 1; i >= 0 && m_settings.m_nNrOfLabels > 2; i--) {
			if (m_treeData.m_bSelection[i]) {
				for (int j = 0; j < m_treeData.m_trees.length; j++) {
					m_treeData.m_trees[j] = deleteLeaf(m_treeData.m_trees[j], i);
					renumber(m_treeData.m_trees[j], i);
					m_treeData.m_trees[j].labelInternalNodes(m_settings.m_nNrOfLabels - 1);
				}
				for (int j = 0; j < m_treeData.m_cTrees.length; j++) {
					m_treeData.m_cTrees[j] = deleteLeaf(m_treeData.m_cTrees[j], i);
					renumber(m_treeData.m_cTrees[j], i);
					m_treeData.m_cTrees[j].labelInternalNodes(m_settings.m_nNrOfLabels - 1);
				}
				m_settings.m_sLabels.remove(i);
				m_settings.m_nNrOfLabels--;
				if (m_settings.m_fLongitude != null && m_settings.m_fLongitude.size() > i) {
					m_settings.m_fLongitude.remove(i);
					m_settings.m_fLatitude.remove(i);
				}
				int[] nOrder = new int[m_settings.m_nOrder.length - 1];
				int[] nRevOrder = new int[m_settings.m_nRevOrder.length - 1];
				int k = 0;
				for (int j = 0; j < nOrder.length; j++) {
					if (m_settings.m_nOrder[k] == i) k++;
					nOrder[j] = (m_settings.m_nOrder[k] < i ? m_settings.m_nOrder[k] : m_settings.m_nOrder[k] - 1);
					k++;
				}
				k = 0;
				for (int j = 0; j < nRevOrder.length; j++) {
					if (m_settings.m_nRevOrder[k] == i) k++;
					nRevOrder[j] = (m_settings.m_nRevOrder[k] < i ? m_settings.m_nRevOrder[k] : m_settings.m_nRevOrder[k] - 1);
					k++;
				}
				m_settings.m_nOrder = nOrder;
				m_settings.m_nRevOrder = nRevOrder;
				nDeleted++;
			}
		}
		m_treeData.m_bSelection = new boolean[m_treeData.m_bSelection.length - nDeleted];
		Arrays.fill(m_treeData.m_bSelection, true);
		fitToScreen();
		calcPositions();
		calcLines();
		SwingUtilities.invokeLater(() -> m_Panel.clearImage());
		repaint();
	}

	void renumber(Node node, int iNodeNr) {
		if (node.isLeaf()) {
			if (node.getNr() > iNodeNr) node.m_iLabel--;
		} else {
			renumber(node.m_left, iNodeNr);
			renumber(node.m_right, iNodeNr);
		}
	}

	Node deleteLeaf(Node node, int iNodeNr) {
		if (node.isLeaf()) {
			if (node.getNr() == iNodeNr) {
				Node parent = node.getParent();
				Node sibling = (parent.m_left == node ? parent.m_right : parent.m_left);
				if (parent.isRoot()) {
					sibling.m_Parent = null;
					return sibling;
				}
				Node grandparent = parent.getParent();
				if (grandparent.m_left == parent) {
					grandparent.m_left = sibling;
				} else {
					grandparent.m_right = sibling;
				}
				sibling.m_Parent = grandparent;
				sibling.m_fLength += parent.m_fLength;
			}
		} else {
			Node node2 = deleteLeaf(node.m_left, iNodeNr);
			if (node2.isRoot()) return node2;
			node2 = deleteLeaf(node.m_right, iNodeNr);
			if (node2.isRoot()) return node2;
		}
		return node;
	}

	void pasteFromClipboard() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clipboard.getContents(null);
		if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			try {
				String sResult = (String) contents.getTransferData(DataFlavor.stringFlavor);
				String sFileName = "tmp.clipboard";
				try (PrintStream out = new PrintStream(sFileName)) {
					out.print(sResult);
				}
				init(sFileName);
				calcLines();
				updateStatus("Loaded from clipboard");
				fitToScreen();
			} catch (Exception e) {
				showErrorAlert("Error pasting from clipboard: " + e.getMessage());
			}
		}
	}

	void showHelpDialog() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Help Message");
		alert.setHeaderText("DensiTree Help");
		alert.setContentText(banner() + getStatus());
		alert.showAndWait();
	}

	void showCladesDialog() {
		Dialog<Void> dlg = new Dialog<>();
		dlg.setTitle("Clades and their probabilities");
		StringBuilder b = new StringBuilder();
		for (String s : m_treeData.cladesToString()) b.append(s);
		TextArea ta = new TextArea(b.toString());
		ta.setEditable(false);
		dlg.getDialogPane().setContent(ta);
		dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
		dlg.showAndWait();
	}

	void showAboutDialog() {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("About Message");
		alert.setHeaderText("DensiTree");
		alert.setContentText(banner() + "Citation:\n" + CITATION);
		ButtonType btnCopy = new ButtonType("Copy citation to clipboard");
		alert.getButtonTypes().setAll(btnCopy, ButtonType.CLOSE);
		alert.showAndWait().ifPresent(type -> {
			if (type == btnCopy) {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(CITATION), null);
			}
		});
	}

	void showErrorAlert(String msg) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
			alert.showAndWait();
		});
	}

	void showWarningAlert(String msg) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
			alert.showAndWait();
		});
	}

	String showInputDialog(String title, String defaultValue) {
		TextInputDialog dlg = new TextInputDialog(defaultValue);
		dlg.setHeaderText(title);
		Optional<String> res = dlg.showAndWait();
		return res.orElse(null);
	}

	List<ChangeListener> m_changeListeners = new ArrayList<>();
	public void addChangeListener(ChangeListener changeListener) {
		m_changeListeners.add(changeListener);
	}

	public static DensiTree startNew(String[] args) {
		Stage stage = new Stage();
		DensiTree dt = new DensiTree(args);
		dt.stage = stage;
		Scene scene = new Scene(dt, 1000, 800);
		stage.setScene(scene);
		stage.setTitle(FRAME_TITLE);
		Image icon = getFxIcon("DensiTree");
		if (icon != null) stage.getIcons().add(icon);
		stage.show();
		dt.fitToScreen();
		return dt;
	}

	public static class DensiTreeApp extends Application {
		@Override
		public void start(Stage primaryStage) {
			List<String> rawArgs = getParameters().getRaw();
			DensiTree dt = new DensiTree(rawArgs.toArray(new String[0]));
			dt.stage = primaryStage;
			Scene scene = new Scene(dt, 1000, 800);
			primaryStage.setScene(scene);
			primaryStage.setTitle(FRAME_TITLE);
			Image icon = getFxIcon("DensiTree");
			if (icon != null) primaryStage.getIcons().add(icon);
			primaryStage.show();
			dt.fitToScreen();
		}
	}
	
	public void loadImages() {
		
	}

	public static void main(String[] args) {
		Application.launch(DensiTreeApp.class, args);
	}

	// TODO: implement following items
	public JButton a_undo = new JButton();
	public JButton a_redo = new JButton();
	public JButton a_loadMirror = new JButton();
	public JButton a_exportCladeComparison = new JButton();
	

	public void setEnabledLoadKML(boolean c) {
		// TODO Auto-generated method stub
	}

	public void loadKMLLocations() {
		// TODO Auto-generated method stub
	}
}