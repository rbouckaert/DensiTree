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
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 - 2013 
 */


package viz;

/**
 * Shows sets of cluster trees represented in Newick format as dendrograms and other graphs.
 * There are 2 modes of viewing a tree set
 * 1. draw all trees in the set
 * 2. browsing through the set of trees/animate through trees in the set, drawing them one by one
 * Restriction: binary trees only
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz, r.bouckaert@auckland.ac.nz)
 * @version $Revision: 2.2.7 $
 */

// the magic sentence to look for when releasing:
//RRB: not for public release





import jam.framework.DocumentFrame;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import viz.GridDrawer.GridMode;
import viz.graphics.*;
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

public class DensiTree extends JPanel implements ComponentListener {
	final static String VERSION = "2.2.7";
	final static String FRAME_TITLE = "DensiTree - Tree Set Visualizer";
//	final static String CITATION = "Remco R. Bouckaert\n"+
//		"DensiTree: making sense of sets of phylogenetic trees\n"+
//		"Bioinformatics (2010) 26 (10): 1372-1373.\n"+
//		"doi: 10.1093/bioinformatics/btq110";
	final static String CITATION = "Remco R. Bouckaert & Joseph Heled\n"+
			"DensiTree 2: Seeing Trees Through the Forest\n"+
			"bioRxiv, 2014,\n" +
			"http://dx.doi.org/10.1101/012401\n";
	static int instances = 1;
	
	public Settings settings = new Settings();

	public TreeData treeData = new TreeData(this, settings);
	public TreeData treeData2;
	
	static float GEO_OFFSET = 3.0f;
	
	/** flag for testing summary tree optimisation **/
	public String m_sOptFile = null;
	/** number of tree in tree set to use as root canal tree **/
	int m_iOptTree = -1;
	public Node m_optTree = null;
	/** user specified newick tree used for initialising the root canal tree -- lengths will be optimised **/ 
	public String m_sOptTree = null;

	static int B = 1; // frame boundary -- should be 10 on OS X, 1 otherwise
	JFrame frame;
	
	private static final long serialVersionUID = 1L;

	/** path for icons */
	public static final String ICONPATH = "viz/icons/";

	/**
	 * default tree branch length, used when that info is not in the Newick tree
	 **/
	final static double DEFAULT_LENGTH = 0.001f;
	



	/** height of highest tree **/
	public float m_fHeight = 0;
	/** scale factors for drawing to screen **/
	float m_fScaleX = 10;
	float m_fScaleY = 10;
	float m_fScaleGX = 10;
	float m_fScaleGY = 10;
	/** global scale for zooming **/
	float m_fScale = 1.0f;
	/** user scale, for scaling grid and clade heights **/
	public float m_fUserScale = 1.0f;

	/** determines which part of the tree-set is shown wrt maximum tree height **/
	float m_fTreeOffset = 0;
	float m_fTreeScale = 1;

	public GridDrawer m_gridDrawer;
	public CladeDrawer m_cladeDrawer;

	/** flag to allow leafs to be draw and dragged around.
	 * As a side effect, internal clades will not be positioned correctly,
	 * so this flag can only be set with the -allowLeafsToBeMovedIKnowThisMessesUpInternalCladePositions flag **/
	boolean m_bLeafCladeSelection = false;
	/** flag to indicate not to draw anything due to being busy initialising **/
	boolean m_bInitializing;


	/** rectangles with on screen coordinates of labels **/
	Rectangle[] m_bLabelRectangle;
	/** rectangles with geographic locations on screen **/
	Rectangle[] m_bGeoRectangle;
	/** selection rectangle drawn through dragging with left mouse button */
	Rectangle m_nSelectedRect = null;

	/**
	 * burn in = nr of trees ignored at the start of tree file, can be set by
	 * command line option
	 **/
	public int m_nBurnIn = 10;
	public boolean m_bBurnInIsPercentage = true;

	/** mean cumulative width, calculated from trees **/
	double m_w = 0;

	public static int HEIGHTCOLOR = 6,
		CONSCOLOR = 4,
		LABELCOLOR = 5,
		BGCOLOR = 7,
		GEOCOLOR = 8,
		ROOTCANALCOLOR=9;

	/** image used for the background **/
	public BufferedImage m_bgImage;
	
	/**
	 * bounding box for area contained in bfImage, derived from the image name:
	 * if the name matches (lat0,long0)x(lat1,long1) e.g. NZ(-40,140)x(-10,180)
	 * it will assume the image covers the box 40 South 140 East to 10 South,
	 * 180 East. NB first coordinate should contain lower values, and the second
	 * coordinate the higher values.
	 */
	double[] m_fBGImageBox = { -180, -90, 180, 90 };



	
	BufferedImage m_rotate;

	/** default regular expression **/
	//final static String DEFAULT_PATTERN = "theta=([0-9\\.Ee-]+)";
	// final static String DEFAULT_PATTERN = "([0-9\\.Ee-]+),([0-9\\.Ee-]+)";
	//final static String DEFAULT_PATTERN = "([0-9\\.Ee-]+),";
	final static String DEFAULT_PATTERN = ".*location=\"([^\"]*).*";
	// final static String DEFAULT_PATTERN = .*dmv=\{(.*),(.*)\}.*
	// final static String DEFAULT_PATTERN = "s=([0-9\\.Ee-]+)";
	// final static String DEFAULT_PATTERN = "([0-9\\.Ee-]+),y=([0-9\\.Ee-]+)";


	/** used to store name of tree file so that when burn-in changes, the tree set
	 * can be reloaded
	 */
	public String m_sFileName;
	

	
	/** for storing the PDF file name from CLI **/
	String m_asPDF = null;
	
	/** thread for processing meta data **/
	Thread thread = null;
	

	public void setWaitCursor() {
		if (frame != null && frame.getCursor().getType() != Cursor.WAIT_CURSOR) {
			frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		}
	}
	public void setDefaultCursor() {
		if (frame != null && frame.getCursor().getType() != Cursor.DEFAULT_CURSOR) {
			frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	/** constructors **/
	public DensiTree() {
		m_gridDrawer = new GridDrawer(this);
		m_cladeDrawer = new CladeDrawer(this);
		instances++;
	}

	public DensiTree(String[] args) {
		this();
		System.out.println(banner());
		treeData.m_bSelection = new boolean[0];
		settings.m_nRevOrder = new int[0];
		treeData.m_cTrees = new Node[0];
		treeData.m_trees = new Node[0];
		initColors();

		setSize(1000, 800);
		m_Panel = new TreeSetPanel(this);
		parseArgs(args);
		settings.m_pattern = createPattern();
		System.err.println(getSize().width + "x" + getSize().height);

		m_jScrollPane = new JScrollPane(m_Panel);
		makeToolbar(m_jTbTools);
		m_menuBar = new JMenuBar();
		makeMenuBar(m_menuBar);
		addComponentListener(this);
		this.setLayout(new BorderLayout());
		this.add(m_jScrollPane, BorderLayout.CENTER);

		a_zoomout.setEnabled(false);
		a_zoomouttree.setEnabled(false);

		m_Panel.setPreferredSize(getSize());

		java.net.URL tempURL = ClassLoader.getSystemResource(ICONPATH + "rotate.png");
		if (tempURL != null) {
			try {
				m_rotate = ImageIO.read(tempURL);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	} // c'tor

	public Pattern createPattern() {
		String sPattern = "";
		for (int i = 0; i < settings.m_iPatternForBottom; i++) {
			sPattern += "[0-9\\.Ee-]+[^0-9]+";
		}
		sPattern += "([0-9\\.Ee-]+)";
		if (settings.m_iPatternForTop > settings.m_iPatternForBottom) {
			//sPattern += "[^0-9]+";
			for (int i = settings.m_iPatternForBottom + 1; i < settings.m_iPatternForTop; i++) {
				sPattern += "[^0-9]+[0-9\\.Ee-]+";
			}
			sPattern += "[^0-9]+([0-9\\.Ee-]+)";
		}
		return Pattern.compile(sPattern);
	}

	void initColors() {
		settings.m_color = new Color[10 + 11 + 50];
		settings.m_color[0] = Color.getColor("color.1", Color.blue);
		settings.m_color[1] = Color.getColor("color.2", Color.red);
		settings.m_color[2] = Color.getColor("color.3", Color.green);
		settings.m_color[3] = Color.getColor("color.default", new Color(0, 100, 25));
		settings.m_color[CONSCOLOR] = Color.getColor("color.cons", Color.blue);
		settings.m_color[LABELCOLOR] = Color.getColor("color.label", Color.blue);
		settings.m_color[HEIGHTCOLOR] = Color.getColor("color.height", Color.gray);
		settings.m_color[BGCOLOR] = Color.getColor("color.bg", Color.white);
		settings.m_color[GEOCOLOR] = Color.getColor("color.bg", Color.orange);
		settings.m_color[ROOTCANALCOLOR] = Color.getColor("color.rootcanal", Color.blue);

		int k = GEOCOLOR + 1;
		settings.m_color[k++] = Color.blue;
		settings.m_color[k++] = Color.green;
		settings.m_color[k++] = Color.red;
		settings.m_color[k++] = Color.gray;
		settings.m_color[k++] = Color.orange;
		settings.m_color[k++] = Color.yellow;
		settings.m_color[k++] = Color.pink;
		settings.m_color[k++] = Color.black;
		settings.m_color[k++] = Color.cyan;
		settings.m_color[k++] = Color.darkGray;
		settings.m_color[k++] = Color.magenta;
		settings.m_color[k++] = new Color(100, 200, 25);
		;
//		m_color[k++] = new Color(100, 0, 25);
//		m_color[k++] = new Color(25, 0, 100);
//		m_color[k++] = new Color(0, 25, 100);
//		m_color[k++] = new Color(0, 100, 25);
//		m_color[k++] = new Color(100, 25, 100);
//		m_color[k++] = new Color(25, 100, 100);
//		m_color[k++] = new Color(100, 100, 100);

		for (float saturation = 0.9f; saturation >= 0.0f; saturation -= 0.2) {
			for (float hue = 0.0f; hue < 1.0f; hue += 0.1) {
				settings.m_color[k++] = new Color(Color.HSBtoRGB(hue+saturation/10, saturation, 0.9f));
			}
		}
	} // initColors

	/** parse command line arguments, and load file if specified **/
	void parseArgs(String[] args) {
		// check whether there is a config file to pick up arguments from
		File cfgFile = new File(".densitree");
		if (cfgFile.exists()) {
			List<String> cfgArgs = new ArrayList<String>();
			try {
				BufferedReader fin = new BufferedReader(new FileReader(cfgFile));
				//StringBuffer buf = new StringBuffer();
				String sStr = null;
				while (fin.ready()) {
					sStr = fin.readLine();
					if (sStr.length() > 0 && !sStr.matches("^\\s*$")) {
							cfgArgs.add(sStr);
					}
				}
				fin.close();
				for (String arg : args) {
					cfgArgs.add(arg);
				}
				args = cfgArgs.toArray(new String[]{});
			} catch (Exception e) {
				System.err.println(e.getMessage());
				System.err.println("WARNING: could not process cfg file");
			}
		}
				
		
		// process arguments
		int i = 0;
		try {
			while (i < args.length) {
				int iOld = i;
				if (i < args.length - 1) {
					if (args[i].equals("")) {
						i += 1;
					} else if (args[i].equals("-c")) {
						settings.m_fCTreeIntensity = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-i")) {
						settings.m_fTreeIntensity = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-j")) {
						settings.m_nJitter = (int) Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-w")) {
						settings.m_nCTreeWidth = (int) Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-v")) {
						settings.m_nTreeWidth = (int) Float.parseFloat(args[i + 1]);
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
					} else if (args[i].equals("-geo")) {
						String[] sStrs = args[i + 1].split("x");
						int nWidth = Integer.parseInt(sStrs[0]);
						int nHeight = Integer.parseInt(sStrs[1]);
						setSize(nWidth, nHeight);
						i += 2;
					} else if (args[i].equals("-geooffset")) {
						GEO_OFFSET = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-invertLongitude")) {
						settings.m_bInvertLongitude = true;
						i += 1;
					} else if (args[i].equals("-scalemode")) {
						String sMode = args[i+1].toLowerCase();
						if (sMode.equals("none")) {
							m_gridDrawer.m_nGridMode = GridMode.NONE;
						} else if (sMode.equals("short")) {
							m_gridDrawer.m_nGridMode = GridMode.SHORT;
						} else if (sMode.equals("full")) {
							m_gridDrawer.m_nGridMode = GridMode.FULL;
						} else 
							throw new Exception("expected scalemode to be NONE, SHORT or FULL");
						i += 2;
					} else if (args[i].equals("-li")) {
						settings.m_fLabelIndent = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-o")) {
						settings.m_sOutputFile = args[i + 1];
						i += 2;
					} else if (args[i].equals("-kml")) {
						settings.m_sKMLFile = args[i + 1];
						//loadKML(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-geowidth")) {
						settings.m_nGeoWidth = Integer.parseInt(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-geocolor")) {
						settings.m_color[GEOCOLOR] = Color.decode(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-bg")) {
						try {
							loadBGImage(args[i + 1]);
							// m_bgImage = ImageIO.read(new File(args[i+1]));
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
						settings.m_sPattern = args[i + 1];
						i += 2;
					} else if (args[i].equals("-colorpattern")) {
						settings.m_sColorPattern = args[i + 1];
						i += 2;
					} else if (args[i].equals("-linecolortag")) {
						settings.m_lineColorTag = args[i + 1];
						settings.m_lineColorMode = LineColorMode.COLOR_BY_METADATA_TAG;
						i += 2;
					} else if (args[i].equals("-linecolorlegend")) {
						settings.m_showLegend = true;
						i++;
					} else if (args[i].equals("-singlechild")) {
						settings.m_bAllowSingleChild = Boolean.parseBoolean(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-rotatetext")) {
						settings.m_bRotateTextWhenRootAtTop = true;
						i++;
					} else if (args[i].equals("-transform")) {
						settings.m_bUseLogScale = true;
						m_fExponent = Double.parseDouble(args[i+1]);
						i += 2;
					} else if (args[i].equals("-allowLeafsToBeMovedIKnowThisMessesUpInternalCladePositions")) {
						m_bLeafCladeSelection = true;
						i += 1;
					} else if (args[i].equals("-optfile")) {
						m_sOptFile = args[i+1];
						i += 2;
					} else if (args[i].equals("-rootcanaltree")) {
						try {
							m_iOptTree = Integer.parseInt(args[i+1]);
						} catch (NumberFormatException e) {
							m_sOptTree = args[i+1];
						}
						i += 2;
					} else if (args[i].equals("-rawrootcanaltree")) {
						m_sOptTree = args[i+1];
						settings.m_bOptimiseRootCanalTree = false;
						i += 2;
					} else if (args[i].equals("-asPDF")) {
						m_asPDF = args[i+1];
						i += 2;
					} else if (args[i].equals("-cladeThreshold")) {
						settings.m_cladeThreshold = Double.parseDouble(args[i+1]);
						i += 2;
					} else if (args[i].equals("-r")) {
						settings.m_bDrawReverse = true;
						i += 1;
					} else if (args[i].equals("-order")) {
						settings.m_sOrderFile = args[i+1];
						i += 2;
					}
					
					
					if (i == iOld) {
						if (new File(args[i]).exists()) {
							init(args[i++]);
							calcLines();

							if (i != args.length) {
								String [] args2 = new String[args.length - 1];
								for (int k = 0; k < i - 1; k++) {
									args2[k] = args[k];
								}
								for (int k = i; k < args.length; k++) {
									args2[k-1] = args[k];
								}
								startNew(args2);
							}
							return;
						}
						throw new Exception("Wrong argument");
					}
				} else {
					init(args[i++]);
					calcLines();
				}
			}
			if (m_asPDF != null) {
				new Thread() {
					@Override
					public void run() {
						try {
							Thread.sleep(5000);
						} catch (Exception e) {
							e.printStackTrace();
						}
						while (!treeData.m_bMetaDataReady) {
							try {
								Thread.sleep(100);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						exportPDF(m_asPDF);
						System.exit(0);
					};
				}.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error parsing command line arguments: " + Arrays.toString(args)
					+ "\nArguments ignored\n\n" + getStatus(), "Argument Parsing Error", JOptionPane.PLAIN_MESSAGE);
		}
	} // parseArgs

	/** print some useful info to stdout **/
	String banner() {
		return "DensiTree - Tree Set Visualizer\nVersion " + VERSION + "\n\n" + "Remco Bouckaert\n"
				+ "r.bouckaert@auckland.ac.nz\nrrb@xm.co.nz\n" + "(c) 2010-2020\n\n\n"
				+ "Key shortcuts:\n" + "c/Ctrl-c decrease/increase consensus tree intensity\n"
				+ "i/Ctrl-i decrease/increase tree intensity\n"
				+ "j/Ctrl-j decrease/increase jitter on trees (not consensus trees)\n"
				+ "w/Ctrl-w decrease/increase consensus tree line width\n"
				+ "v/Ctrl-v decrease/increase tree line width\n"
				+ "f/Ctrl-f decrease/increase animation time delay - shorter delay = faster animation\n"
				+ "t/Ctrl-t decrease/increase number of drawing threads for drawing tree set\n\n"
				+ "Arrow keys & Page-Up/Down to scroll\n";
	} // banner

	String formatColor(int iColor) {
		return " 0x" + Integer.toHexString(settings.m_color[iColor].getRGB()).substring(2) + ' ';
	}

	/** get status of internal settings **/
	String getStatus() {
		int nSelected = 0;
		if (treeData.m_bSelection != null) {
			for (boolean b : treeData.m_bSelection) {
				if (b) {
					nSelected++;
				}
			}
		}
		return "\n\nCurrent status:\n" + treeData.m_trees.length + " trees with " + treeData.m_cTrees.length + " topologies " +
				settings.m_sLabels.size() + " taxa " + nSelected + " selected \n"
				+ "Tree intensity: " + settings.m_fTreeIntensity + "\n" + "Consensus Tree intensity: " + settings.m_fCTreeIntensity
				+ "\n" + "Tree width: " + settings.m_nTreeWidth + "\n" + "Consensus Tree width: " + settings.m_nCTreeWidth + "\n"
				+ "Jitter: " + settings.m_nJitter + "\n" + "Animation delay: " + m_nAnimationDelay + "\n" + "Height: "
				+ m_fHeight + "\n" + "Zoom: " + m_fScale + "\n" + "Number of drawing threads: " + m_Panel.m_nDrawThreads + "\n"
				+ "Burn in: " + m_nBurnIn + "\n\nColor 1:" + formatColor(0) + "\tColor 2:" + formatColor(1)
				+ "\tColor 3:" + formatColor(2) + "\tDefault Color:" + formatColor(3) + "\nConsensus Color:"
				+ formatColor(CONSCOLOR) + "\tLabel color:" + formatColor(LABELCOLOR) + "\tBackground color:"
				+ formatColor(BGCOLOR) + "\tHeight color:" + formatColor(HEIGHTCOLOR);
	}

	
	class MetaDataThread extends Thread {
		TreeData treeData;
		
		MetaDataThread(TreeData treeData) {
			this.treeData = treeData;
		}
		
		@Override
		public void run() {
			m_jStatusBar.setText("Calculating clades");
			treeData.calcClades();
			treeData.m_bCladesReady = true;
			m_jStatusBar.setText("Optimising node order");
			int [] oldOrder = settings.m_nOrder.clone();
			if (!settings.m_bAllowSingleChild && treeData.drawMode != TreeData.MODE_RIGHT) {
				reshuffle(NodeOrderer.SORT_BY_ROOT_CANAL_LENGTH);
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
					m_jStatusBar.setText(statusMsg);
					setWaitCursor();
//					if (getCursor().getType() != Cursor.WAIT_CURSOR) {
//						setCursor(new Cursor(Cursor.WAIT_CURSOR));
//					}
				}
			}
			if (!settings.m_bAllowSingleChild && treeData.drawMode != TreeData.MODE_RIGHT) {
				settings.m_metaDataTags = new ArrayList<String>();
				settings.m_metaDataTypes = new ArrayList<MetaDataType>();
				collectMetaDataTags(treeData.m_trees[0]);
				if (settings.m_metaDataTags.size() > 0) {
					calcPositions();
					calcLines();
					makeDirty();
				}
			}
			treeData.m_bMetaDataReady = true;			
			notifyChangeListeners();
			m_jStatusBar.setText("Done parsing metadata");
			
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
		};
		
	};
	
	
	/**
	 * read trees from file, and process them into a set of lines This may take
	 * a while... sFile: name of Nexus or Newick tree list file or to read
	 * 
	 * @throws Exception
	 **/
	@SuppressWarnings("deprecation")
	public void init(String sFile) throws Exception {
		if (m_Panel != null) {
			setWaitCursor();
			//m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		}
		treeData2 = null;
		if (m_jStatusBar != null) {
			m_jStatusBar.setText("Initializing...");
			m_jStatusBar.repaint();
		}
		m_sFileName = sFile;
		m_bInitializing = true;
		m_viewMode = ViewMode.DRAW;
		a_animateStart.setIcon("start");
		settings.m_prevLineColorMode = null;
		LineColorMode orgLineColorMode =  settings.m_lineColorMode;
		settings.m_lineColorMode = LineColorMode.DEFAULT;
		settings.m_prevLineWidthMode = null;
		settings.m_lineWidthMode = LineWidthMode.DEFAULT;
		
		System.err.print("Initializing...");
		m_iAnimateTree = 0;
		m_fHeight = 0;
		m_fScaleX = 10;
		m_fScaleY = 10;
		m_fScale = 1;
		m_fTreeScale = 1;
		m_fTreeOffset = 0;
		m_doActions = new Vector<DoAction>();
		m_iUndo = 0;
		settings.m_random = new Random();
		m_Panel.m_drawThread = new Thread[m_Panel.m_nDrawThreads];

		treeData.m_rootcanaltree = null;
		
		try {
			if (thread != null) {
				try {
					thread.stop();
				} catch (Exception e) {
					// ignore
				}
			}
			/** contains strings with tree in Newick format **/
			settings.m_sLabels = new Vector<String>();
			settings.m_fLongitude = new Vector<Float>();
			settings.m_fLatitude = new Vector<Float>();
			settings.m_fMinLat = 360;
			settings.m_fMinLong = 360;
			settings.m_fMaxLat = 0;
			settings.m_fMaxLong = 0;
			settings.m_nOrder = null;

			treeData.loadFromFile(sFile);
			
			// initialise drawing order of x-axis according to most prevalent
			// tree
			Node tree = treeData.m_trees[0];
			// over sized, too lazy to figure out exact number of labels
			settings.m_nOrder = new int[settings.m_sLabels.size()];
			settings.m_nRevOrder = new int[settings.m_sLabels.size()];
			initOrder(tree, 0);
			// sanity check
			int nSum = 0;
			for (int i = 0; i < settings.m_nOrder.length; i++) {
				nSum += settings.m_nOrder[i];
			}
			if (nSum != settings.m_nNrOfLabels * (settings.m_nNrOfLabels - 1) / 2) {
				JOptionPane.showMessageDialog(this,
						"The tree set possibly contains non-binary trees. Expect that not all nodes are shown.");
			}

//			new Thread() {
//				public void run() {
//					calcClades();
//					m_bCladesReady = true;
//					reshuffle((settings.m_bAllowSingleChild ? NodeOrderer.DEFAULT: NodeOrderer.OPTIMISE));
//					calcPositions();
//					makeDirty();
//				};
//			}.start();

			//reshuffle((settings.m_bAllowSingleChild ? NodeOrderer.DEFAULT: NodeOrderer.OPTIMISE));
			reshuffle(NodeOrderer.DEFAULT);
			
			// calculate y-position for tree set
			calcPositions();
			
			treeData.m_bMetaDataReady = false;			
			thread = new MetaDataThread(treeData);
			thread.start();
			
			settings.m_metaDataTags = new ArrayList<String>();
			settings.m_metaDataTypes = new ArrayList<MetaDataType>();
			collectMetaDataTags(treeData.m_trees[0]);
			notifyChangeListeners();

			if (orgLineColorMode != LineColorMode.DEFAULT) {
				while (!treeData.m_bMetaDataReady) {
					Thread.sleep(100);
				}
				settings.m_lineColorMode = orgLineColorMode;
				calcColors(false);
				makeDirty();
			}

		} catch (OutOfMemoryError e) {
			clear();
			JOptionPane.showMessageDialog(null, "Not enough memory is reserved for java to process this tree. "
					+ "Try starting DensiTree with more memory\n\n(for example "
					+ "use:\njava -Xmx3g DensiTree.jar\nfrom " + "the command line) where DensiTree is in the path\n"
					+ "or subsample your tree set to create a smaller tree file.");
			setDefaultCursor();
			//m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			clear();
			setDefaultCursor();
			//m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			throw e;
		}
		m_bInitializing = false;
		if (settings.m_sColorPattern != null) {
			calcColorPattern();
		}

		addAction(new DoAction());
		if (frame != null) {
			frame.setTitle(FRAME_TITLE + " " + sFile);
		}
		if (settings.m_sKMLFile != null) {
			loadKML();
		}
		System.err.println("Done");
	} // init

	
	public float positionHeight(Node node, int fOffSet) {		
		return treeData.positionHeight(node, fOffSet);
	}
	
	public void calcLines() {
		treeData.calcLines();
		if (treeData2 != null) {
			treeData2.calcLines();
		}
	}
	
	public void calcColors(boolean forceRecalc) {
		treeData.calcColors(forceRecalc);
		if (treeData2 != null) {
			treeData2.calcColors(forceRecalc);
		}
	}
	
	public void calcPositions() {
		treeData.calcPositions();
		if (treeData2 != null) {
			treeData2.calcPositions();
		}
	}
	
	public void calcLineWidths(boolean forceRecalc) {
		treeData.calcLineWidths(forceRecalc);
		if (treeData2 != null) {
			treeData2.calcLineWidths(forceRecalc);
		}
	}
	
	float positionRest(Node node) {
		return treeData.positionRest(node);
	}
	
	private void getPosition(Node node, float[] fPosX) {
		treeData.getPosition(node, fPosX);						
	}
	
	private void setPosition(Node node, float[] fPosX) {
		treeData.setPosition(node, fPosX);			
	}

	
	void notifyChangeListeners() {
		for (ChangeListener listener : m_changeListeners) {
			listener.stateChanged(null);
		}
	}

	private boolean orderChanged(int[] oldOrder) {
		for (int i = 0; i < oldOrder.length; i++) {
			if (oldOrder[i] != settings.m_nOrder[i]) {
				return true;
			}
		}
		return false;
	}

	
	private void collectMetaDataTags(Node node) {
		Map<String, Object> metaDataMap = node.getMetaDataSet();
		if (metaDataMap != null) {
			for (String key : metaDataMap.keySet()) {
				if (!settings.m_metaDataTags.contains(key)) {
					settings.m_metaDataTags.add(key);
					Object o = metaDataMap.get(key);
					if (o instanceof Double) {
						settings.m_metaDataTypes.add(MetaDataType.NUMERIC);
					} else {
						String s = o.toString();
						if (s.length() > 0 && s.charAt(0)=='{') {
							settings.m_metaDataTypes.add(MetaDataType.SET);
						} else {
							settings.m_metaDataTypes.add(MetaDataType.STRING);
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
		treeData.updateCladeModel();
	}

	void resetCladeSelection() {
		treeData.resetCladeSelection();
	}
	
	public void calcCladeIDForNode(Node tree, Map<String, Integer> mapCladeToIndex) {
		treeData.calcCladeIDForNode(tree, mapCladeToIndex);
		
	}
	
	public void resetCladeNr(Node tree, Integer[] reverseindex) {
		treeData.resetCladeNr(tree, reverseindex);
	}

	
	
	void calcColorPattern() {
		settings.m_iColor = new int[settings.m_sLabels.size()];
		Pattern pattern = Pattern.compile(".*" + settings.m_sColorPattern + ".*");
		List<String> sPatterns = new ArrayList<String>();
		for (int i = 0; i < settings.m_sLabels.size(); i++) {
			String sLabel = settings.m_sLabels.get(i);
			Matcher matcher = pattern.matcher(sLabel);
			if (matcher.find()) {
				String sMatch = matcher.group(1);
				if (sPatterns.indexOf(sMatch) < 0) {
					sPatterns.add(sMatch);
				}
				settings.m_iColor[i] = sPatterns.indexOf(sMatch);
			}
		}
	}

	void loadKML() {
		String sFileName = settings.m_sKMLFile;
		HashMap<String, Vector<Double>> mapLabel2X = new HashMap<String, Vector<Double>>();
		HashMap<String, Vector<Double>> mapLabel2Y = new HashMap<String, Vector<Double>>();

		// sanity check
		if (!(new File(sFileName)).exists()) {
			JOptionPane.showMessageDialog(this, "Tried to read goe info, but could not find file " + sFileName );
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

			// grab styles out of the KML file
			HashMap<String, Integer> mapStyleToColor = new HashMap<String, Integer>();
			org.w3c.dom.NodeList oStyles = doc.getElementsByTagName("Style");
			for (int iNode = 0; iNode < oStyles.getLength(); iNode++) {
				org.w3c.dom.Node oStyle = oStyles.item(iNode);
				String sID = oStyle.getAttributes().getNamedItem("id").getTextContent();
				XPath xpath = XPathFactory.newInstance().newXPath();
				String expression = ".//PolyStyle/color";
				org.w3c.dom.Node oColor = (org.w3c.dom.Node) xpath.evaluate(expression, oStyles.item(iNode),
						XPathConstants.NODE);
				if (oColor != null) {
					String sColor = oColor.getTextContent();
					sColor = sColor.substring(2);
					Integer nColor = Integer.parseInt(sColor, 16);
					mapStyleToColor.put(sID, nColor);
				}
			}


			// grab polygon info from placemarks
			//List<Integer> iDistrictCenter = new ArrayList<Integer>();
			org.w3c.dom.NodeList oPlacemarks = doc.getElementsByTagName("Placemark");
			for (int iNode = 0; iNode < oPlacemarks.getLength(); iNode++) {
				String sPlacemarkName = "";
				Vector<Double> nX = new Vector<Double>();
				Vector<Double> nY = new Vector<Double>();
				org.w3c.dom.Node node = oPlacemarks.item(iNode);
				org.w3c.dom.NodeList oChildren = node.getChildNodes();
				// int color = 0x808080;
				for (int iChild = 0; iChild < oChildren.getLength(); iChild++) {
					org.w3c.dom.Node oChild = oChildren.item(iChild);
					if (oChild.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
						String sName = oChild.getNodeName();
						if (sName.equals("name")) {
							sPlacemarkName = oChild.getTextContent().trim();
						} else if (sName.equals("Style")) {
							String expression = ".//PolyStyle/color";
							XPath xpath = XPathFactory.newInstance().newXPath();
							org.w3c.dom.Node oColor = (org.w3c.dom.Node) xpath.evaluate(expression,
									oStyles.item(iNode), XPathConstants.NODE);
							if (oColor != null) {
								String sColor = oColor.getTextContent();
								sColor = sColor.substring(2);
								// color = Integer.parseInt(sColor, 16);
							}
						} else if (sName.equals("styleUrl")) {
							String sID = oChild.getTextContent();
							sID = sID.substring(1);
							if (mapStyleToColor.containsKey(sID)) {
								// color = mapStyleToColor.get(sID);
							}
							// } else if (sName.equals("description")) {
							// sDescription = oChild.getTextContent();
						} else if (sName.equals("Polygon") || sName.equals("Point") || sName.equals("LineString")) {
							XPath xpath = XPathFactory.newInstance().newXPath();
							String expression = ".//coordinates";
							org.w3c.dom.Node oCoords = (org.w3c.dom.Node) xpath.evaluate(expression, oChild,
									XPathConstants.NODE);
							String sCoord = oCoords.getTextContent();
							String[] sCoords = sCoord.split("\\s+");
							for (int i = 0; i < sCoords.length; i++) {
								String sStr = sCoords[i];
								String[] sStrs = sStr.split(",");
								if (sStrs.length > 1) {
									// Point point = new Point();
									nX.add(Double.parseDouble(sStrs[0]));// *
																			// Parser.MAX_LATITUDE_INT_UNITS
																			// /
																			// 360));
									nY.add(Double.parseDouble(sStrs[1]));// /180f)
																			// *
																			// Parser.MAX_LONGITUDE_INT_UNITS));
								}
							}
						}
					}
				}
				if (nX.size() > 0) {
					mapLabel2X.put(sPlacemarkName.toLowerCase(), nX);
					mapLabel2Y.put(sPlacemarkName.toLowerCase(), nY);
					sPlacemarkName = sPlacemarkName.replaceAll("-", "");
					sPlacemarkName = sPlacemarkName.replaceAll("_", "");
					if (!mapLabel2X.containsKey(sPlacemarkName)) {
						mapLabel2X.put(sPlacemarkName.toLowerCase(), nX);
						mapLabel2Y.put(sPlacemarkName.toLowerCase(), nY);
					}
				}
			}

		} catch (Exception e) {
			// try to process as tab-delimited txt file
			try {
				settings.m_fMinLat = 90;
				settings.m_fMinLong = 180;
				settings.m_fMaxLat = -90;
				settings.m_fMaxLong = -180;

				BufferedReader fin = new BufferedReader(new FileReader(sFileName));
				String sStr = null;
				// skip header line
				sStr = fin.readLine();
				while (fin.ready()) {
					sStr = fin.readLine();
					String [] sStrs = sStr.split("\\s+");
					if (sStrs.length >= 3) {
						String sPlacemarkName = sStrs[0];
						Vector<Double> nX = new Vector<Double>();
						Vector<Double> nY = new Vector<Double>();
						nX.add(Double.parseDouble(sStrs[2]));
						nY.add(Double.parseDouble(sStrs[1]));
						mapLabel2X.put(sPlacemarkName.toLowerCase(), nX);
						mapLabel2Y.put(sPlacemarkName.toLowerCase(), nY);
						sPlacemarkName = sPlacemarkName.replaceAll("-", "");
						sPlacemarkName = sPlacemarkName.replaceAll("_", "");
						if (!mapLabel2X.containsKey(sPlacemarkName)) {
							mapLabel2X.put(sPlacemarkName.toLowerCase(), nX);
							mapLabel2Y.put(sPlacemarkName.toLowerCase(), nY);
						}
					}
				}
				fin.close();
			
			
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}

		try {
			// grab Taxa From Objects
			settings.m_fMinLat = 90;
			settings.m_fMinLong = 180;
			settings.m_fMaxLat = -90;
			settings.m_fMaxLong = -180;
			for (int iLabel = 0; iLabel < settings.m_nNrOfLabels; iLabel++) {
				String sTaxon = settings.m_sLabels.get(iLabel).toLowerCase();
				String sTaxon2 = sTaxon.replaceAll("[-_]", "");
				if (mapLabel2X.containsKey(sTaxon) || mapLabel2X.containsKey(sTaxon2)) {
					if (!mapLabel2X.containsKey(sTaxon)) {
						sTaxon = sTaxon2;
					}
					Vector<Double> nX = mapLabel2X.get(sTaxon);
					Vector<Double> nY = mapLabel2Y.get(sTaxon);
					double fX = 0;
					double fY = 0;
					for (Double f : nX) {
						fX += f;
					}
					fX /= nX.size();
					for (Double f : nY) {
						fY += f;
					}
					fY /= nY.size();
					while (settings.m_fLatitude.size() <= iLabel) {
						settings.m_fLatitude.add(0f);
						settings.m_fLongitude.add(0f);
					}
					settings.m_fLatitude.set(iLabel, (float) fY);
					settings.m_fLongitude.set(iLabel, (float) fX);
					settings.m_fMinLat = Math.min(settings.m_fMinLat, (float) fY);
					settings.m_fMaxLat = Math.max(settings.m_fMaxLat, (float) fY);
					settings.m_fMinLong = Math.min(settings.m_fMinLong, (float) fX);
					settings.m_fMaxLong = Math.max(settings.m_fMaxLong, (float) fX);
				} else {
					System.err.println("No geo info for " + sTaxon
							+ " found (probably because taxon is missing or spelling error)");
					while (settings.m_fLatitude.size() <= iLabel) {
						settings.m_fLatitude.add(0f);
						settings.m_fLongitude.add(0f);
					}
					settings.m_fLatitude.set(iLabel, 0f);
					settings.m_fLongitude.set(iLabel, 0f);
				}
			}
			float fOffset = GEO_OFFSET;
			settings.m_fMaxLong = settings.m_fMaxLong + fOffset;
			settings.m_fMaxLat = settings.m_fMaxLat + fOffset;
			settings.m_fMinLong = settings.m_fMinLong - fOffset;
			settings.m_fMinLat = settings.m_fMinLat - fOffset;
			
			System.err.println("geo range (" +settings.m_fMinLat + "," + settings.m_fMinLong+ ")x(" + settings.m_fMaxLat+","+ settings.m_fMaxLong+")");
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	} // loadKMLFile

	/* remove all data from memory */
	void clear() {
		treeData.m_trees = new Node[0];
		treeData.m_cTrees = new Node[0];
		treeData.m_fLinesX = null;
		treeData.m_fLinesY = null;
		// m_fTLinesX = null;
		// m_fTLinesY = null;
		treeData.m_fCLinesX = null;
		treeData.m_fCLinesY = null;
		// m_fCTLinesX = null;
		// m_fCTLinesY = null;
		m_bInitializing = false;
	} // clear

	/**
	 * try to reorder the leaf nodes so that the tree layout allows
	 * investigation of some of the tree set features
	 */
	void reshuffle(int nMethod) {
		int [] oldOrder = settings.m_nOrder.clone();
		settings.m_nShuffleMode = nMethod;
		setWaitCursor();
		
		if (settings.m_sOrderFile != null) {
			if (new File(settings.m_sOrderFile).exists()) {
				nMethod = NodeOrderer.MANUAL;
			} else {
				JOptionPane.showMessageDialog(this, "Could not find file " + settings.m_sOrderFile + " for reading");
			}
		}


		//m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		try {
			switch (nMethod) {
			case NodeOrderer.DEFAULT:
				// use order of most frequently occurring tree
				initOrder(treeData.m_trees[0], 0);
				break;
			case NodeOrderer.MANUAL: {
				// use order given by user
				for (int i = 0; i < settings.m_sLabels.size(); i++) {
					System.out.print(settings.m_sLabels.elementAt(i) + " ");
				}
				
				String[] sIndex;
				if (settings.m_sOrderFile == null) {
					String sOrder = JOptionPane.showInputDialog("New node order:", "");
					if (sOrder == null) {
						return;
					}
					sIndex = sOrder.split(" ");
				} else {
					List<String> labels = new ArrayList<>();
					try {
				        BufferedReader fin = new BufferedReader(new FileReader(settings.m_sOrderFile));
				        String str = null;
				        while (fin.ready()) {
				            str = fin.readLine();
				            if (!str.startsWith("#") && str.trim().length() > 0) {
				            	labels.add(str.trim());
				            }
				        }
				        fin.close();
					} catch (IOException e) {
						e.printStackTrace();
						JOptionPane.showMessageDialog(this, "Something went wrong with file " + settings.m_sOrderFile + ": " + e.getMessage());
						
					}
					sIndex = labels.toArray(new String[]{});
					//m_sOrderFile = null;
				}
				
				if (sIndex.length != settings.m_nNrOfLabels) {
					System.err.println("Number of labels/taxa differs from given labels");
					return;
				}
				int[] nOrder = new int[settings.m_nOrder.length];
				int[] nRevOrder = new int[settings.m_nRevOrder.length];
				for (int i = 0; i < sIndex.length; i++) {
					int j = 0;
					String sTarget = sIndex[i];
					while ((j < settings.m_sLabels.size()) && !(settings.m_sLabels.elementAt(j).equals(sTarget))) {
						j++;
					}
					if (j == settings.m_sLabels.size()) {
						System.err.println("Label \"" + sTarget + "\" not found among labels");
						return;
					}
					nOrder[j] = i;
					nRevOrder[i] = j;
				}
				settings.m_nOrder = nOrder;
				settings.m_nRevOrder = nRevOrder;
			}
				break;
			case NodeOrderer.META_ALL:
				break;
			case NodeOrderer.META_SUM:
				break;
			case NodeOrderer.META_AVERAGE:
				break;
			case NodeOrderer.GEOINFO:
				break;
			default:
				// otherwise, use one of the distance based methods
				NodeOrderer h = new NodeOrderer(nMethod);
				int[] nOrder = h.calcOrder(settings.m_nNrOfLabels, treeData.m_trees, treeData.m_cTrees, treeData.m_rootcanaltree, treeData.m_fTreeWeight/*
																						 * ,
																						 * m_nOrder
																						 */, treeData.m_clades, treeData.m_cladeWeight);
				settings.m_nOrder = nOrder;
				for (int i = 0; i < settings.m_nNrOfLabels; i++) {
					settings.m_nRevOrder[settings.m_nOrder[i]] = i;
				}
				System.err.println();
				for (int i = 0; i < settings.m_nNrOfLabels; i++) {
					System.out.print(settings.m_sLabels.elementAt(settings.m_nRevOrder[i]) + " ");
				}
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		int nNodes = getNrOfNodes(treeData.m_trees[0]);
		if (nMethod < NodeOrderer.META_ALL) {
			settings.m_bShowBounds = false;
			calcPositions();
			calcLines();
			makeDirty();
			addAction(new DoAction());
		} else {
			settings.m_bShowBounds = true;
			settings.m_pattern = Pattern.compile(settings.m_sPattern);
			switch (nMethod) {
			case NodeOrderer.META_ALL: {
				double fMaxX = 0;
				for (int i = 0; i < treeData.m_trees.length; i++) {
					double fX = positionMetaAll(treeData.m_trees[i]);
					fMaxX = Math.max(fMaxX, fX);
				}
				for (int i = 0; i < treeData.m_cTrees.length; i++) {
					double fX = positionMetaAll(treeData.m_cTrees[i]);
					fMaxX = Math.max(fMaxX, fX);
				}
				fMaxX = settings.m_nNrOfLabels / fMaxX;
				for (int i = 0; i < treeData.m_trees.length; i++) {
					scaleX(treeData.m_trees[i], fMaxX);
				}
				for (int i = 0; i < treeData.m_cTrees.length; i++) {
					scaleX(treeData.m_cTrees[i], fMaxX);
				}
				calcLines();
			}
				break;
			case NodeOrderer.META_SUM:
			case NodeOrderer.META_AVERAGE: {
				for (int i = 0; i < treeData.m_trees.length; i++) {
					float[] fHeights = new float[settings.m_nNrOfLabels * 2 - 1];
					float[] fMetas = new float[settings.m_nNrOfLabels * 2 - 1];
					int[] nCounts = new int[settings.m_nNrOfLabels * 2 - 1];
					collectHeights(treeData.m_trees[i], fHeights, 0);
					Arrays.sort(fHeights);
					treeData.collectMetaData(treeData.m_trees[i], fHeights, 0.0f, 0, fMetas, nCounts);
					treeData.m_fLinesX[i] = new float[nNodes * 2 + 2];
					treeData.m_fLinesY[i] = new float[nNodes * 2 + 2];
					for (int j = 0; j < fMetas.length - 1; j++) {
						treeData.m_fLinesX[i][j * 2] = fMetas[j];
						treeData.m_fLinesY[i][j * 2] = (fHeights[j] - m_fTreeOffset) * m_fTreeScale;
						treeData.m_fLinesX[i][j * 2 + 1] = fMetas[j + 1];
						treeData.m_fLinesY[i][j * 2 + 1] = (fHeights[j + 1] - m_fTreeOffset) * m_fTreeScale;
					}
					if (nMethod == NodeOrderer.META_AVERAGE) {
						for (int j = 0; j < fMetas.length - 1; j++) {
							if (nCounts[j] > 0) {
								treeData.m_fLinesX[i][j * 2] = fMetas[j] / nCounts[j];
							}
							if (nCounts[j + 1] > 0) {
								treeData.m_fLinesX[i][j * 2 + 1] = fMetas[j + 1] / nCounts[j + 1];
							}
						}
					}
				}
				for (int i = 0; i < treeData.m_cTrees.length; i++) {
					float[] fHeights = new float[settings.m_nNrOfLabels * 2 - 1];
					float[] fMetas = new float[settings.m_nNrOfLabels * 2 - 1];
					int[] nCounts = new int[settings.m_nNrOfLabels * 2 - 1];
					collectHeights(treeData.m_cTrees[i], fHeights, 0);
					Arrays.sort(fHeights);
					treeData.collectMetaData(treeData.m_cTrees[i], fHeights, 0.0f, 0, fMetas, nCounts);
					treeData.m_fCLinesX[i] = new float[nNodes * 2 + 2];
					treeData.m_fCLinesY[i] = new float[nNodes * 2 + 2];
					for (int j = 0; j < fMetas.length - 1; j++) {
						treeData.m_fCLinesX[i][j * 2] = fMetas[j];
						treeData.m_fCLinesY[i][j * 2] = (fHeights[j] - m_fTreeOffset) * m_fTreeScale;
						treeData.m_fCLinesX[i][j * 2 + 1] = fMetas[j + 1];
						treeData.m_fCLinesY[i][j * 2 + 1] = (fHeights[j + 1] - m_fTreeOffset) * m_fTreeScale;
					}
					if (nMethod == NodeOrderer.META_AVERAGE) {
						for (int j = 0; j < fMetas.length - 1; j++) {
							if (nCounts[j] > 0) {
								treeData.m_fLinesX[i][j * 2] = fMetas[j] / nCounts[j];
							}
							if (nCounts[j + 1] > 0) {
								treeData.m_fLinesX[i][j * 2 + 1] = fMetas[j + 1] / nCounts[j + 1];
							}
						}
					}
				}
				// determine scale
				float fMaxX = 0;
				for (float[] fXs : treeData.m_fLinesX) {
					for (float f : fXs) {
						fMaxX = Math.max(f, fMaxX);
					}
				}
				for (float[] fXs : treeData.m_fCLinesX) {
					for (float f : fXs) {
						fMaxX = Math.max(f, fMaxX);
					}
				}
				float fScale = settings.m_nNrOfLabels / fMaxX;
				for (float[] fXs : treeData.m_fCLinesX) {
					for (int i = 0; i < fXs.length; i++) {
						fXs[i] *= fScale;
					}
				}
				for (float[] fXs : treeData.m_fLinesX) {
					for (int i = 0; i < fXs.length; i++) {
						fXs[i] *= fScale;
					}
				}
			}
				break;
			}
			if (orderChanged(oldOrder)) {
				makeDirty();
			}
			// addAction(new DoAction());
		}
	} // reshuffle

	/**
	 * Reorder leafs by rotating around internal node associated with
	 * iRotationPoint
	 */
	void rotateAround(int iRotationPoint) {

		Vector<Integer> iLeafs = new Vector<Integer>();
		getRotationLeafs(treeData.m_cTrees[0], -1, iLeafs, iRotationPoint);

		System.err.println("Rotating " + iRotationPoint + " " + iLeafs);
		// find rotation range
		int iMin = settings.m_nOrder.length;
		int iMax = 0;
		for (Integer i : iLeafs) {
			int j = settings.m_nOrder[i];
			iMin = Math.min(j, iMin);
			iMax = Math.max(j, iMax);
		}
		for (int i = 0; i < (iMax - iMin) / 2 + 1; i++) {
			int nTmp = settings.m_nRevOrder[iMin + i];
			settings.m_nRevOrder[iMin + i] = settings.m_nRevOrder[iMax - i];
			settings.m_nRevOrder[iMax - i] = nTmp;
		}

		for (int i = 0; i < settings.m_sLabels.size(); i++) {
			settings.m_nOrder[settings.m_nRevOrder[i]] = i;
		}

		calcPositions();
		calcLines();
		makeDirty();
		addAction(new DoAction());
	} // rotateAround

	void moveRotationPoint(int iRotationPoint, float fdH) {
		Vector<Integer> iLeafs = new Vector<Integer>();
		getRotationLeafs(treeData.m_cTrees[0], -1, iLeafs, iRotationPoint);
		boolean[] bSelection = treeData.m_bSelection;
		treeData.m_bSelection = new boolean[settings.m_sLabels.size()];
		for (int i : iLeafs) {
			treeData.m_bSelection[i] = true;
		}

		for (int i = 0; i < treeData.m_trees.length; i++) {
			moveInternalNode(fdH, treeData.m_trees[i], iLeafs.size());
		}
		for (int i = 0; i < treeData.m_cTrees.length; i++) {
			moveInternalNode(fdH, treeData.m_cTrees[i], iLeafs.size());
		}
		treeData.m_bSelection = bSelection;
		calcLines();
		makeDirty();
	}

	int moveInternalNode(float fdH, Node node, int nSelected) {
		if (node.isLeaf()) {
			return (treeData.m_bSelection[node.getNr()] ? 1 : 0);
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

	/**
	 * Determine set of leafs that are under rotation point iRotationPoint.
	 * Results stored in iLeafs as node numbers.
	 */
	int getRotationLeafs(Node node, int iPos, Vector<Integer> iLeafs, int iRotationPoint) {
		if (node.isLeaf()) {
			iLeafs.add(node.getNr());
		} else {
			iPos = getRotationLeafs(node.m_left, iPos, iLeafs, iRotationPoint);
			if (iPos == iRotationPoint) {
				return iPos;
			}
			Vector<Integer> iLeafsR = new Vector<Integer>();
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
	} // getRotationLeafs

	int getNrOfNodes(Node node) {
		if (node.isLeaf()) {
			return 1;
		} else {
			int nNodes = getNrOfNodes(node.m_left);
			if (node.m_right != null) {
				nNodes += getNrOfNodes(node.m_right);
			} else {
				// count one for the dummy node on the right
				nNodes++;
			}
			return nNodes + 1;
		}
	}




	public enum LineWidthMode {BY_METADATA_PATTERN, BY_METADATA_NUMBER, DEFAULT, BY_METADATA_TAG};

	public enum LineColorMode {COLOR_BY_CLADE, BY_METADATA_PATTERN, DEFAULT, COLOR_BY_METADATA_TAG};
	public enum MetaDataType {NUMERIC, STRING, SET};
	






	
	
	/** initialise order of leafs **/
	int initOrder(Node node, int iNr) throws Exception {
		if (node.isLeaf()) {
			settings.m_nOrder[node.m_iLabel] = iNr;
			settings.m_nRevOrder[iNr] = node.m_iLabel;
			return iNr + 1;
		} else {
			iNr = initOrder(node.m_left, iNr);
			if (node.m_right != null) {
				iNr = initOrder(node.m_right, iNr);
			}
		}
		return iNr;
	}


	/** check the selection is empty, and ask user whether this is desirable **/
	void checkSelection() {
		treeData.checkSelection();
	}

	/** check at least one, but not all labels are selected **/
	boolean moveSanityChek() {
		int nSelected = treeData.selectionSize();
		if (nSelected > 0 && nSelected < settings.m_nRevOrder.length - 1) {
			return true;
		}
		JOptionPane.showMessageDialog(null, "To move labels, select at least one, but not all of the labels",
				"Move error", JOptionPane.PLAIN_MESSAGE);
		return false;
	}

	/** move labels in selection down in ordering **/
	void moveSelectedLabelsDown() {
		for (int i = 1; i < settings.m_nRevOrder.length; i++) {
			if (treeData.m_bSelection[settings.m_nRevOrder[i]] && !treeData.m_bSelection[settings.m_nRevOrder[i - 1]]) {
				int h = settings.m_nRevOrder[i];
				settings.m_nRevOrder[i] = settings.m_nRevOrder[i - 1];
				settings.m_nRevOrder[i - 1] = h;
			}
		}
		for (int i = 0; i < settings.m_nRevOrder.length; i++) {
			settings.m_nOrder[settings.m_nRevOrder[i]] = i;
		}
		calcPositions();
		addAction(new DoAction());
	}

	/** move labels in selection up in ordering **/
	void moveSelectedLabelsUp() {
		for (int i = settings.m_nRevOrder.length - 2; i >= 0; i--) {
			if (treeData.m_bSelection[settings.m_nRevOrder[i]] && !treeData.m_bSelection[settings.m_nRevOrder[i + 1]]) {
				int h = settings.m_nRevOrder[i];
				settings.m_nRevOrder[i] = settings.m_nRevOrder[i + 1];
				settings.m_nRevOrder[i + 1] = h;
			}
		}
		for (int i = 0; i < settings.m_nRevOrder.length; i++) {
			settings.m_nOrder[settings.m_nRevOrder[i]] = i;
		}
		calcPositions();
		addAction(new DoAction());
	}


	/**
	 * return meta data value of a node as defined by the pattern (m_sPattern &
	 * m_pattern), or 1 if parsing fails.
	 */
	// int [] m_nCurrentPosition;
	float getMetaData(Node node) {
		try {
			Matcher matcher = settings.m_pattern.matcher(node.getMetaData());
			matcher.find();
			int nGroup = 1;
			int nGroups = matcher.groupCount();
			if (nGroup > nGroups) {
				nGroup = 1;
			}
			return Float.parseFloat(matcher.group(nGroup));
		} catch (Exception e) {
		}
		return 1f;
	} // getMetaData

	int getMetaDataCategory(Node node) {
		try {
			Matcher matcher = settings.m_pattern.matcher(node.getMetaData());
			matcher.find();
			int nGroup = 1;
			int nGroups = matcher.groupCount();
			if (nGroup > nGroups) {
				nGroup = 1;
			}
			String match = matcher.group(nGroup);
			if (settings.m_colorMetaDataCategories.get(match) == null) {
				settings.m_colorMetaDataCategories.put(match, settings.m_colorMetaDataCategories.size());
			}
			return settings.m_colorMetaDataCategories.get(match);
			
//			if (!m_colorMetaDataCategories.contains(match)) {
//				m_colorMetaDataCategories.add(match);
//			}
//			//System.err.println(node.m_sMetaData + ": " + match + " = " + m_metaDataCategories.indexOf(match));
//			return m_colorMetaDataCategories.indexOf(match);
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return 0;
	} // getMetaData

	double positionMetaAll(Node node) {
		node.m_fPosX = getMetaData(node);
		if (!node.isLeaf()) {
			double fX1 = positionMetaAll(node.m_left);
			double fX2 = positionMetaAll(node.m_right);
			return Math.max(fX1, Math.max(fX2, node.m_fPosX));
		}
		return node.m_fPosX;
	} // positionMetaAll

	void scaleX(Node node, double fScale) {
		node.m_fPosX = (float) (settings.m_sLabels.size() - node.m_fPosX * fScale);
		if (!node.isLeaf()) {
			scaleX(node.m_left, fScale);
			scaleX(node.m_right, fScale);
		}
	} // scaleX

	int collectHeights(Node node, float[] fHeights, int iPos) {
		fHeights[iPos++] = node.m_fPosY;// fLengthToRoot + node.m_fLength;
		if (!node.isLeaf()) {
			iPos = collectHeights(node.m_left, fHeights, iPos);
			iPos = collectHeights(node.m_right, fHeights, iPos);
		}
		return iPos;
	} // collectHeights



//	float height(Node node) {
//		if (node.isLeaf()) {
//			return node.m_fLength;
//		} else {
//			return node.m_fLength + Math.max(height(node.m_left), height(node.m_right));
//		}
//	}

	/** move y-position of a tree with offset f **/
	public void offsetHeight(Node node, float f) {
		if (!node.isLeaf()) {
			offsetHeight(node.m_left, f);
			if (node.m_right != null) {
				offsetHeight(node.m_right, f);
			}
		}
		node.m_fPosY += f;
	}


	/** draw only labels of a tree, not the branches **/
	void drawLabels(Node node, Graphics2D g, TreeData treeData) {
		if (settings.m_bHideLabels) {
			return;
		}
		g.setFont(m_font);
		if (settings.m_bShowBounds) {
			return;
		}

		if (node.isLeaf()) {
			if (treeData.m_bSelection[node.m_iLabel]) {
				if (settings.m_iColor == null) {
					g.setColor(settings.m_color[LABELCOLOR]);
				} else {
					g.setColor(settings.m_color[GEOCOLOR + settings.m_iColor[node.m_iLabel] % (settings.m_color.length - GEOCOLOR)]);
				}
			} else {
				g.setColor(Color.GRAY);
			}
			if (m_treeDrawer.m_bRootAtTop) {
				if (settings.m_bRotateTextWhenRootAtTop) {
					int x = (int) (node.m_fPosX * m_fScaleX /* m_fScale */) - g.getFontMetrics().getHeight() / 3;
					int y = getPosY(((m_bAlignLabels ? m_fHeight:node.m_fPosY) + settings.m_fLabelIndent - m_fTreeOffset) * m_fTreeScale) + 2;// ;
					g.rotate(Math.PI / 2.0);
					g.translate(y, -x);
					g.drawString(settings.m_sLabels.elementAt(node.m_iLabel), 0, 0);
					g.translate(-y, x);
					g.rotate(-Math.PI / 2.0);
					Rectangle r = m_bLabelRectangle[node.m_iLabel];
					r.x = x;
					r.y = y - 10;
					r.height = 10;
					r.width = settings.m_nLabelWidth;
					drawImage(g, x, y, node.m_iLabel);
				} else {
					String sLabel = settings.m_sLabels.elementAt(node.m_iLabel);
					int x = (int) (node.m_fPosX * m_fScaleX /* m_fScale */) - g.getFontMetrics().stringWidth(sLabel)
							/ 2;
					int y = getPosY(((m_bAlignLabels ?m_fHeight:node.m_fPosY) + settings.m_fLabelIndent - m_fTreeOffset) * m_fTreeScale)
							+ g.getFontMetrics().getHeight() + 2;
					g.drawString(sLabel, x, y);
					Rectangle r = m_bLabelRectangle[node.m_iLabel];
					r.x = x;
					r.y = y - 10;
					r.height = 10;
					r.width = settings.m_nLabelWidth;
					drawImage(g, x, y, node.m_iLabel);
				}
			} else {
				int y = (int) (node.m_fPosX * m_fScaleY/* m_fScale */) + g.getFontMetrics().getHeight() / 3;
				int x = getPosX(((m_bAlignLabels ?m_fHeight:node.m_fPosY) + settings.m_fLabelIndent - m_fTreeOffset) * m_fTreeScale) + 1;
				if (settings.m_bDrawReverse) {
					g.scale(-1.0, 1.0);
					String text = settings.m_sLabels.elementAt(node.m_iLabel);
					g.drawString(text, -x-g.getFontMetrics().stringWidth(text), y);
					g.scale(-1.0, 1.0);
				} else {
					switch (treeData.drawMode) {
						case TreeData.MODE_CENTRE:
							g.drawString(settings.m_sLabels.elementAt(node.m_iLabel), x, y);
							break;
						case TreeData.MODE_LEFT:
							String text = settings.m_sLabels.elementAt(node.m_iLabel);
							g.drawString(text, getWidth()/2 -g.getFontMetrics().stringWidth(text)/2, y);
							break;
						case TreeData.MODE_RIGHT:
							// suppress label
					}
				}
				Rectangle r = m_bLabelRectangle[node.m_iLabel];
				r.x = x;
				r.y = y - 10;
				r.height = 10;
				r.width = settings.m_nLabelWidth;
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
		if (settings.m_LabelImages != null && settings.m_LabelImages[iLabel] != null) {
			BufferedImage img = settings.m_LabelImages[iLabel];
			g.drawImage(img, x, y-settings.m_nImageSize, x+settings.m_nImageSize, y, 0, 0, img.getWidth(), img.getHeight(), null);
		}
	}
	
	/** draw lines from labels of a tree to corresponding geographic point **/
	void drawGeo(Node node, Graphics g) {
		if (node.isLeaf()) {
			if (treeData.m_bSelection[node.m_iLabel]) {
				if (settings.m_fLongitude.elementAt(node.m_iLabel) == 0 && settings.m_fLatitude.elementAt(node.m_iLabel) == 0) {
					return;
				}
				int gx = (int) ((settings.m_fLongitude.elementAt(node.m_iLabel) - settings.m_fMinLong) * m_fScaleGX * m_fScale);
				int gy = (int) ((settings.m_fMaxLat - settings.m_fLatitude.elementAt(node.m_iLabel)) * m_fScaleGY * m_fScale);
				g.setColor(settings.m_color[GEOCOLOR]);
				if (m_treeDrawer.m_bRootAtTop) {
					int x = (int) (node.m_fPosX * m_fScaleX * m_fScale) + 10;
					int y = getPosY(node.m_fPosY * m_fScale);
					g.drawLine(x, y, gx, gy);
					g.setColor(Color.BLACK);
					g.drawOval(gx - 1, gy - 1, 3, 3);
				} else {
					int y = (int) (node.m_fPosX * m_fScaleY * m_fScale);
					int x = getPosX(node.m_fPosY * m_fScale);
					if (settings.m_bInvertLongitude) {
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

	/**
	 * convert height info in tree to y position on screen -- use when root at
	 * top
	 **/
	int getPosY(float fHeight) {
		if (settings.m_bUseLogScale) {
			return (int) (m_fHeight / Math.log(m_fHeight + 1.0) * m_fScaleY * (Math.log(m_fHeight + 1.0) - Math.log(m_fHeight - fHeight + 1.0)));
		}
		return (int) (fHeight * m_fScaleY);
	}
	
	float screenPosToHeight(int nX, int nY) {
		if (settings.m_bUseLogScale) {
			return Float.NaN;
		}
		if (m_treeDrawer.m_bRootAtTop) {
			return  (m_fHeight - ((nY/ m_fScaleY) + m_fTreeOffset)) * m_fUserScale;
		} else {
			return  (m_fHeight - ((nX/ m_fScaleX) + m_fTreeOffset)) * m_fUserScale;
		}
	}

	/**
	 * convert height info in tree to x position on screen -- use when root not
	 * at top
	 **/
	int getPosX(float fHeight) {
		if (settings.m_bUseLogScale) {
			return (int) ((m_fHeight / Math.log(m_fHeight + 1.0) * m_fScaleX * (Math.log(m_fHeight + 1.0) - Math
					.log(m_fHeight - fHeight + 1.0))));
		}
		return (int) (fHeight * m_fScaleX);
	}

	/**
	 * Fits the tree to the current screen size. Call this after window has been
	 * created to get the entire tree to be in view upon launch.
	 */
	public void fitToScreen() {
		if (settings.m_sLabels == null) {
			// no trees loaded yet
			return;
		}
		m_fScaleX = 10;
		m_fScaleY = 10;
		int nW = (int) (getWidth() / m_fScale) - 24;
		int nH = (int) (getHeight() / m_fScale) - 24;
		nW = getWidth() - 24;
		nH = getHeight() - 24;
		if (m_treeDrawer.m_bRootAtTop) {
			m_fScaleX = (nW + 0.0f) / settings.m_sLabels.size();
			m_fScaleGX = (nW + 0.0f) / (settings.m_fMaxLong - settings.m_fMinLong);
			if (m_fHeight > 0) {
				if (settings.m_bRotateTextWhenRootAtTop) {
					m_fScaleY = (nH - settings.m_nLabelWidth - 0.0f) / m_fHeight;
					m_fScaleGY = (nH - settings.m_nLabelWidth - 0.0f) / (settings.m_fMaxLat - settings.m_fMinLat);
				} else {
					m_fScaleY = (nH - 10.0f) / m_fHeight;
					m_fScaleGY = (nH - 10.0f) / (settings.m_fMaxLat - settings.m_fMinLat);
				}
			}
		} else {
			if (settings.m_sLabels != null && settings.m_sLabels.size() > 0) {
				m_fScaleY = (nH + 0.0f) / settings.m_sLabels.size();
				m_fScaleGY = (nH + 0.0f) / (settings.m_fMaxLat - settings.m_fMinLat);
			}
			if (m_fHeight > 0) {
				m_fScaleX = (nW - settings.m_nLabelWidth + 0.0f) / m_fHeight;
				m_fScaleGX = (nW - settings.m_nLabelWidth + 0.0f) / (settings.m_fMaxLong - settings.m_fMinLong);
			}
		}
		m_Panel.setPreferredSize(new Dimension((int) (nW * m_fScale), (int) (nH * m_fScale)));
		m_fScaleX *= m_fScale;
		m_fScaleY *= m_fScale;
		m_jScrollPane.revalidate();
		makeDirty();
		// System.err.println("Scale " + m_fScaleX + " " + m_fScaleY);
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
		fitToScreen();
		// makeDirty();
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	/** object that draws a single tree on an image **/
	public TreeDrawer m_treeDrawer = new TreeDrawer();

	
	void selectMode(int nXmode) {
		switch (nXmode) {
		case 0: // default
			settings.m_Xmode = 0;
			settings.m_bUseAngleCorrection = false;
			m_treeDrawer.m_bViewBlockTree = false;
			m_viewClades.setEnabled(false);
			m_viewEditTree.setEnabled(true);
			break;
		case 1: // star tree
			settings.m_Xmode = 2;
			settings.m_bUseAngleCorrection = false;
			m_treeDrawer.m_bViewBlockTree = false;
			m_viewClades.setEnabled(true);
			m_viewEditTree.setEnabled(false);
			break;
		case 2: // centralised
			settings.m_Xmode = 1;
			settings.m_bUseAngleCorrection = false;
			m_treeDrawer.m_bViewBlockTree = false;
			m_viewClades.setEnabled(true);
			m_viewEditTree.setEnabled(false);
			break;
		case 3: // centralised + angle corrected
			settings.m_Xmode = 1;
			settings.m_bUseAngleCorrection = true;
			m_treeDrawer.m_bViewBlockTree = false;
			m_viewClades.setEnabled(true);
			m_viewEditTree.setEnabled(false);
			break;
		}
//		Enumeration<AbstractButton> enumeration = m_modeGroup.getElements();
//		for (int i = 0; i < nXmode; i++) {
//			enumeration.nextElement();
//		}
//		m_modeGroup.setSelected(enumeration.nextElement().getModel(), true);

		calcPositions();
		calcLines();
		makeDirty();
		for (ChangeListener listener : m_changeListeners) {
			listener.stateChanged(null);
		}
	}
 
	
	int m_nStyle = 0;
	public void resetStyle() {
		setStyle(m_nStyle);
	}
	
	void setStyle(int nStyle) {
		m_nStyle = nStyle;
		BranchDrawer bd = null;
		switch (nStyle) {
		case 0:
			if (settings.m_lineWidthMode != LineWidthMode.DEFAULT) {
				bd = new TrapeziumBranchDrawer();
			} else {
				bd = new BranchDrawer();
			}
			m_treeDrawer.m_bViewBlockTree = false;
			break;
		case 1:
			if (settings.m_lineWidthMode != LineWidthMode.DEFAULT) {
				bd = new TrapeziumBranchDrawer();
			} else {
				bd = new BranchDrawer();
			}
			m_treeDrawer.m_bViewBlockTree = true;
			break;
		case 2:
			//if (m_lineWidthMode == LineWidthMode.DEFAULT) {
				bd = new ArcBranchDrawer();
				m_treeDrawer.m_bViewBlockTree = false;
			//}
			break;
		// case 2: bd = new KoruBranchDrawer();break;
		// case 3: bd = new TrapeziumBranchDrawer();break;
		// case 3: bd = new BrownianBridgeBranchDrawer();break;
		case 3:
			//if (m_lineWidthMode == LineWidthMode.DEFAULT) {
				bd = new SteepArcBranchDrawer();
				m_treeDrawer.m_bViewBlockTree = false;
			//}
			break;
		}
		if (bd != null) {
			m_treeDrawer.setBranchDrawer(bd);
			makeDirty();
		}
	}
	
	
	
	Icon getIcon(String sIcon) {
		java.net.URL tempURL = ClassLoader.getSystemResource(ICONPATH + sIcon + ".png");
		if (tempURL != null) {
			return new ImageIcon(tempURL);
		}
		return null;
	}


	/** this contains the TreeSetPanel */
	JScrollPane m_jScrollPane;
	/** panel for drawing the trees **/
	public TreeSetPanel m_Panel = null;
	/** the menu bar for this application. */
	JMenuBar m_menuBar;
	/** status bar at bottom of window */
	final JLabel m_jStatusBar = new JLabel("Status bar");;
	/** toolbar containing buttons at top of window */
	final JToolBar m_jTbTools = new JToolBar();
	final JPanel m_jTbTools2 = new JPanel();
	final JToolBar m_jTbCladeTools = new JToolBar();
	/** font for all text being printed (e.g. labels, height info) **/
	public Font m_font = new Font("sansserif", Font.PLAIN, 12);
	public boolean m_bAlignLabels = false;
	
	/** flag to indicate consensus trees should be shown **/
	public boolean m_bViewCTrees = false;
	/** flag to indicate all individual trees should be shown **/
	public boolean m_bViewAllTrees = true;

	double m_fExponent = 1.0;



	/**
	 * flag to indicate animation should overwrite trees instead of clearing
	 * screen every time
	 **/
	boolean m_bAnimateOverwrite = false;
	/** tree currently being drawn **/
	int m_iAnimateTree;
	/** delay between drawing of two trees in animation **/
	int m_nAnimationDelay = 100;
	/** automatically refresh screen when settings are changed **/
	boolean m_bAutoRefresh = true;
	/** flag to indicate screen is out of sync with settings **/
	boolean m_bIsDirty = true;

	/**
	 * mode for viewing DRAW = draw all trees ANIMATE = animate through trees
	 * BROWSE = browse individual trees
	 */
	public enum ViewMode {
		DRAW, ANIMATE, BROWSE
	};

	ViewMode m_viewMode = ViewMode.DRAW;

	public void makeDirty() {
		m_Panel.m_rotationPoints = null;
		if (m_bAutoRefresh) {
			m_Panel.clearImage();
		} else {
			m_bIsDirty = true;
		}
		repaint();
	}

	public JMenuBar getMenuBar() {
		return m_menuBar;
	}

	/**
	 * Base class used for defining actions with a name, tool tip text, possibly
	 * an icon and accelerator key.
	 * */
	class MyAction extends AbstractAction {
		/** for serialization */
		private static final long serialVersionUID = -2038911111935517L;

		   public MyAction(String sName, String sToolTipText, String sIcon, int acceleratorKey) {
		        super(sName);
			    KeyStroke acceleratorKeystroke = KeyStroke.getKeyStroke(acceleratorKey, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
			    if ((acceleratorKey & InputEvent.ALT_DOWN_MASK) > 0) {
				    acceleratorKeystroke = KeyStroke.getKeyStroke(acceleratorKey - InputEvent.ALT_DOWN_MASK, InputEvent.ALT_DOWN_MASK);
			    }
		        // setToolTipText(sToolTipText);
		        putValue(Action.SHORT_DESCRIPTION, sToolTipText);
		        putValue(Action.LONG_DESCRIPTION, sToolTipText);
		        if (acceleratorKeystroke != null && acceleratorKeystroke.getKeyCode() >= 0) {
		            putValue(Action.ACCELERATOR_KEY, acceleratorKeystroke);
		        }
		        putValue(Action.MNEMONIC_KEY, Integer.valueOf(sName.charAt(0)));
		        java.net.URL tempURL = ClassLoader.getSystemResource("viz/icons/" + sIcon + ".png");
		        //if (true || !viz.util.Util.isMac()) {
			        if (tempURL != null) {
			            putValue(Action.SMALL_ICON, new ImageIcon(tempURL));
			        } else {
			            putValue(Action.SMALL_ICON, new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_4BYTE_ABGR)));
			        }
		        //}
		    } // c'tor

//		    public MyAction(String sName, String sToolTipText, String sIcon, String sAcceleratorKey) {
//		        this(sName, sToolTipText, sIcon, KeyStroke.getKeyStroke(sAcceleratorKey));
//		    } // c'tor

		    public MyAction(String sName, String sToolTipText, String sIcon, KeyStroke acceleratorKeystroke) {
		        super(sName);
		        // setToolTipText(sToolTipText);
		        putValue(Action.SHORT_DESCRIPTION, sToolTipText);
		        putValue(Action.LONG_DESCRIPTION, sToolTipText);
		        if (acceleratorKeystroke != null && acceleratorKeystroke.getKeyCode() >= 0) {
		            putValue(Action.ACCELERATOR_KEY, acceleratorKeystroke);
		        }
		        putValue(Action.MNEMONIC_KEY, Integer.valueOf(sName.charAt(0)));
		        java.net.URL tempURL = ClassLoader.getSystemResource("viz/icons/" + sIcon + ".png");
		        if (!viz.util.Util.isMac()) {
			        if (tempURL != null) {
			            putValue(Action.SMALL_ICON, new ImageIcon(tempURL));
			        } else {
			            putValue(Action.SMALL_ICON, new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_4BYTE_ABGR)));
			        }
		        }
		    } // c'tor
		

		void setIcon(String sIcon) {
			java.net.URL tempURL = ClassLoader.getSystemResource(ICONPATH + sIcon + ".png");
			if (tempURL != null) {
				putValue(Action.SMALL_ICON, new ImageIcon(tempURL));
			} else {
				putValue(Action.SMALL_ICON, new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_4BYTE_ABGR)));
			}
		}

		/**
		 * Place holder. Should be implemented by derived classes. (non-Javadoc)
		 */
		@Override
		public void actionPerformed(ActionEvent ae) {
		}
	} // class MyAction

	/** base class for actions that allow customisation of a color **/
	class ColorAction extends MyAction {
		private static final long serialVersionUID = 1L;
		int m_iColor;

		public ColorAction(String sName, String sToolTipText, String sIcon, int nAcceleratorKey, int iColor) {
			super(sName, sToolTipText, sIcon, nAcceleratorKey);
			m_iColor = iColor;
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			Color newColor = JColorChooser.showDialog(m_Panel, getName(), settings.m_color[m_iColor]);
			if (newColor != null) {
				settings.m_color[m_iColor] = newColor;
				makeDirty();
			}
			repaint();
		}
	} // class ColorAction

	class ShuffleAction extends MyAction {
		private static final long serialVersionUID = 1L;
		int m_nMode;

		public ShuffleAction(String sName, String sToolTipText, String sIcon, int nAcceleratorKey, int nMode) {
			super(sName, sToolTipText, sIcon, nAcceleratorKey);
			m_nMode = nMode;
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			setWaitCursor();
			//m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			reshuffle(m_nMode);
			setDefaultCursor();
			//m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}; // class ActionReshuffle

	/** base class dealing with update of internal state **/
	class SettingAction extends MyAction {
		private static final long serialVersionUID = 1L;
		String m_sName;

		public SettingAction(String sName, String sToolTipText, String sIcon, int nAcceleratorKey) {
			super(sName, sToolTipText, sIcon, nAcceleratorKey);
			m_sName = sName;
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (m_sName.equals("Jitter+")) {
				settings.m_nJitter++;
				if (settings.m_nJitter >= 0) {
					makeDirty();
				}
			}
			if (m_sName.equals("Jitter-")) {
				settings.m_nJitter--;
				if (settings.m_nJitter >= 0) {
					makeDirty();
				}
			}
			if (m_sName.equals("Intensity+")) {
				settings.m_fTreeIntensity *= 1.1;
				makeDirty();
			}
			if (m_sName.equals("Intensity-")) {
				settings.m_fTreeIntensity /= 1.1;
				makeDirty();
			}
			if (m_sName.equals("Consensus Intensity+")) {
				settings.m_fCTreeIntensity *= 1.1;
				makeDirty();
			}
			if (m_sName.equals("Consensus Intensity-")) {
				settings.m_fCTreeIntensity /= 1.1;
				makeDirty();
			}
			if (m_sName.equals("Consensus Tree Width+")) {
				settings.m_nCTreeWidth++;
				makeDirty();
			}
			if (m_sName.equals("Consensus Tree Width-")) {
				settings.m_nCTreeWidth--;
				if (settings.m_nCTreeWidth <= 1) {
					settings.m_nCTreeWidth = 1;
				}
				makeDirty();
			}
			if (m_sName.equals("Tree Width+")) {
				settings.m_nTreeWidth++;
				makeDirty();
			}
			if (m_sName.equals("Tree Width-")) {
				settings.m_nTreeWidth--;
				if (settings.m_nTreeWidth <= 1) {
					settings.m_nTreeWidth = 1;
				}
				makeDirty();
			}
			if (m_sName.equals("Drawing Threads+")) {
				m_Panel.stopDrawThreads();
				m_Panel.m_nDrawThreads++;
				m_Panel.m_drawThread = new Thread[m_Panel.m_nDrawThreads];
			}
			if (m_sName.equals("Drawing Threads-")) {
				if (m_Panel.m_nDrawThreads > 1) {
					m_Panel.stopDrawThreads();
					m_Panel.m_nDrawThreads--;
					m_Panel.m_drawThread = new Thread[m_Panel.m_nDrawThreads];
				}
			}
			if (m_sName.equals("Animation Speed-")) {
				m_nAnimationDelay += 1 + m_nAnimationDelay / 10;
			}
			if (m_sName.equals("Animation Speed+")) {
				if (m_nAnimationDelay > 0) {
					m_nAnimationDelay -= 1 + m_nAnimationDelay / 10;
				}
			}
			if (m_sName.equals("Angle Correction+")) {
				settings.m_fAngleCorrectionThresHold *= 1.1;
				if (settings.m_fAngleCorrectionThresHold > 0.999) {
					settings.m_fAngleCorrectionThresHold = 0.999;
				}
				System.err.println("Angle Correction ThresHold = " + settings.m_fAngleCorrectionThresHold);
				calcPositions();
				calcLines();
				makeDirty();
			}
			if (m_sName.equals("Angle Correction-")) {
				settings.m_fAngleCorrectionThresHold /= 1.1;
				System.err.println("Angle Correction ThresHold = " + settings.m_fAngleCorrectionThresHold);
				calcPositions();
				calcLines();
				makeDirty();
			}
			repaint();
			System.err.print(getStatus());
		} // actionPerformed
	} // class SettingAction

	/** actions triggered by GUI events */
	public Action a_quit = new MyAction("Exit", "Exit Program", "exit", -1) {
		private static final long serialVersionUID = -10;

		@Override
		public void actionPerformed(ActionEvent ae) {
			System.exit(0);
		}
	}; // class ActionQuit

	Action a_paste = new MyAction("Paste", "Paste tree(s) from clipboard", "paste", KeyEvent.VK_V) {
		private static final long serialVersionUID = -10;

		@Override
		public void actionPerformed(ActionEvent ae) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable contents = clipboard.getContents(null);
			boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
			if (hasTransferableText) {
				try {
					String sResult = (String) contents.getTransferData(DataFlavor.stringFlavor);
					String sFileName = "tmp.clipboard";
					PrintStream out = new PrintStream(sFileName);
					out.print(sResult);
					out.close();

					init(sFileName);
					calcLines();
					m_jStatusBar.setText("Loaded from clipboard");
					fitToScreen();
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Error pasting from clipboard: " + e.getMessage(),
							"File paste error", JOptionPane.PLAIN_MESSAGE);
				}
			}
		}
	}; // class ActionPaste

	abstract class MyFileFilter extends FileFilter {
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase().endsWith(getExtention());
		}

		abstract public String getExtention();
	}

	Action a_export = new MyAction("Export", "Export", "export", -1) {
		private static final long serialVersionUID = -1;

		@Override
		public void actionPerformed(ActionEvent ae) {
			JFileChooser fc = new JFileChooser(settings.m_sDir);
			fc.addChoosableFileFilter(new MyFileFilter() {
				@Override
				public String getExtention() {
					return ".bmp";
				}

				@Override
				public String getDescription() {
					return "Bitmap files (*.bmp)";
				}
			});
			fc.addChoosableFileFilter(new MyFileFilter() {
				@Override
				public String getExtention() {
					return ".jpg";
				}

				@Override
				public String getDescription() {
					return "JPEG bitmap files (*.jpg)";
				}
			});
			fc.addChoosableFileFilter(new MyFileFilter() {
				@Override
				public String getExtention() {
					return ".png";
				}

				@Override
				public String getDescription() {
					return "PNG bitmap files (*.png)";
				}
			});
			fc.addChoosableFileFilter(new MyFileFilter() {
				@Override
				public String getExtention() {
					return ".pdf";
				}

				@Override
				public String getDescription() {
					return "PDF files (*.pdf)";
				}
			});
			fc.addChoosableFileFilter(new MyFileFilter() {
				public String getExtention() {
					return ".svg";
				}

				public String getDescription() {
					return "Standard Vector Graphics files";
				}
			});
			fc.setDialogTitle("Export DensiTree As");
			int rval = fc.showSaveDialog(m_Panel);
			if (rval == JFileChooser.APPROVE_OPTION) {
				// System.out.println("Saving to file \""+
				// f.getAbsoluteFile().toString()+"\"");
				String sFileName = fc.getSelectedFile().toString();
				if (sFileName.lastIndexOf('/') > 0) {
					settings.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
				}
				if (sFileName != null && !sFileName.equals("")) {
					if (!(sFileName.toLowerCase().endsWith(".png") || sFileName.toLowerCase().endsWith(".jpg")
							|| sFileName.toLowerCase().endsWith(".pdf")
							|| sFileName.toLowerCase().endsWith(".bmp") || sFileName.toLowerCase().endsWith(".svg"))) {
						sFileName += ((MyFileFilter) fc.getFileFilter()).getExtention();
					}

                    if (sFileName.toLowerCase().endsWith(".pdf")) {
                    	exportPDF(sFileName);
                       repaint();
                    	return;
                    } else 	if (sFileName.toLowerCase().endsWith(".png") || sFileName.toLowerCase().endsWith(".jpg")
							|| sFileName.toLowerCase().endsWith(".bmp")) {
						BufferedImage bi;
						Graphics g;
						bi = new BufferedImage(m_Panel.getWidth(), m_Panel.getHeight(), BufferedImage.TYPE_INT_RGB);
						g = bi.getGraphics();
						g.setPaintMode();
						g.setColor(getBackground());
						g.fillRect(0, 0, m_Panel.getWidth(), m_Panel.getHeight());
						m_Panel.printAll(g);
						try {
							if (sFileName.toLowerCase().endsWith(".png")) {
								ImageIO.write(bi, "png", new File(sFileName));
							} else if (sFileName.toLowerCase().endsWith(".jpg")) {
								ImageIO.write(bi, "jpg", new File(sFileName));
							} else if (sFileName.toLowerCase().endsWith(".bmp")) {
								ImageIO.write(bi, "bmp", new File(sFileName));
							}
						} catch (Exception e) {
							JOptionPane.showMessageDialog(null,
									sFileName + " was not written properly: " + e.getMessage());
							e.printStackTrace();
						}
						return;
					}
					if (sFileName.toLowerCase().endsWith(".svg")) {
						m_Panel.toSVG(sFileName);
						return;
					}
					JOptionPane.showMessageDialog(null, "Extention of file " + sFileName
							+ " not recognized as png,bmp,jpg or svg file");
				}
			}
		}
	}; // class ActionExport
	
	void exportPDF(String sFileName) {
		try {
			com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(sFileName));
			doc.setPageSize(new com.itextpdf.text.Rectangle(m_Panel.getWidth(), m_Panel.getHeight()));
			doc.open();
			PdfContentByte cb = writer.getDirectContent();
			Graphics2D g = new PdfGraphics2D(cb, m_Panel.getWidth(), m_Panel.getHeight());
			 
			//BufferedImage bi;
			//bi = new BufferedImage(m_Panel.getWidth(), m_Panel.getHeight(), BufferedImage.TYPE_INT_RGB);
			//g = bi.getGraphics();
			g.setPaintMode();
			g.setColor(getBackground());
			g.fillRect(0, 0, m_Panel.getWidth(), m_Panel.getHeight());
			m_Panel.paint(g);
			//m_Panel.printAll(g);
		
			g.dispose();
			doc.close();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(m_Panel, "Export may have failed: " + e.getMessage());
		}
	}


	Action a_print = new MyAction("Print", "Print Graph", "print", KeyEvent.VK_P) {
		private static final long serialVersionUID = -20389001859354L;

		// boolean m_bIsPrinting = false;

		@Override
		public void actionPerformed(ActionEvent ae) {
			PrinterJob printJob = PrinterJob.getPrinterJob();
			printJob.setPrintable(m_Panel);
			if (printJob.printDialog())
				try {
					// m_bIsPrinting = true;
					printJob.print();
					// m_bIsPrinting = false;
				} catch (PrinterException pe) {
					// m_bIsPrinting = false;
				}
		} // actionPerformed
	}; // class ActionPrint

	Action a_new = new MyAction("New", "New instance of DensiTree", "new", KeyEvent.VK_N) {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			startNew(new String[]{});
		}
	};

	Action a_load = new MyAction("Load", "Load tree set", "open", KeyEvent.VK_O) {
		private static final long serialVersionUID = -2038911085935515L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			File [] files = Util.getFile("Load Tree Set", true, new File(settings.m_sDir), false, "Nexus trees files", "trees","tre","nex","t","tree");
			if (files != null && files.length > 0) {
				doOpen(files[0].getPath());
			}
		}
	}; // class ActionLoad

	public void doOpen(String sFileName) {
		if (sFileName.lastIndexOf('/') > 0) {
			settings.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
		}
		try {
			//m_sKMLFile = null;
			init(sFileName);
			calcLines();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error loading file: " + e.getMessage(), "File load error",
					JOptionPane.PLAIN_MESSAGE);
			return;
		}
		m_jStatusBar.setText("Loaded " + sFileName);
		fitToScreen();
	}
	
	Action a_loadMirror = new MyAction("Load mirror set", "Load mirror tree set", "open", null) {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			File [] files = Util.getFile("Load Mirror Tree Set", true, new File(settings.m_sDir), false, "Nexus trees files", "trees","tre","nex","t","tree");
			if (files != null && files.length > 0) {
				doOpenMirror(files[0].getPath());
			}
		}
	}; // class ActionLoadMirror
	
	public void doOpenMirror(String sFileName) {
		if (sFileName.lastIndexOf('/') > 0) {
			settings.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
		}
		try {
			treeData2 = new TreeData(this, this.settings);
			if (!treeData2.loadFromFile(sFileName)) {
				treeData2 = null;
				return;
			}
			treeData.drawMode = TreeData.MODE_LEFT;
			treeData2.drawMode = TreeData.MODE_RIGHT;

			if (thread != null) {
				try {
					thread.stop();
				} catch (Exception e) {
					// ignore
				}
			}
			thread = new MetaDataThread(treeData2);
			thread.start();
			
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error loading file: " + e.getMessage(), "File load error",
					JOptionPane.PLAIN_MESSAGE);
			return;
		}
		m_jStatusBar.setText("Loaded " + sFileName);
		fitToScreen();
	}

	public Action a_loadkml = new MyAction("Load locations", "Load geographic locations of taxa", "geo", -1) {
		private static final long serialVersionUID = -1L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			JFileChooser fc = new JFileChooser(settings.m_sDir);
			fc.addChoosableFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					String name = f.getName().toLowerCase();
					if (name.endsWith(".kml") || name.endsWith(".kmz")) {
						return true;
					}
					return false;
				}

				// The description of this filter
				@Override
				public String getDescription() {
					return "KML file with taxon locations";
				}
			});
			fc.addChoosableFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					String name = f.getName().toLowerCase();
					if (name.endsWith(".txt") || name.endsWith(".dat")) {
						return true;
					}
					return false;
				}

				// The description of this filter
				@Override
				public String getDescription() {
					return "text file with taxon locations, tab delimited";
				}
			});

			fc.setDialogTitle("Load Geographic Locations");
			int rval = fc.showOpenDialog(m_Panel);

			if (rval == JFileChooser.APPROVE_OPTION) {
				String sFileName = fc.getSelectedFile().toString();
				if (sFileName.lastIndexOf('/') > 0) {
					settings.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
				}
				settings.m_sKMLFile = sFileName;
				loadKML();
				m_jStatusBar.setText("Loaded " + sFileName);
				fitToScreen();
				// makeDirty();
			}
		}
	}; // class ActionLoadKML

	Action a_saveas = new MyAction("Save as", "Save as", "save", KeyEvent.VK_S) {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			JFileChooser fc = new JFileChooser(settings.m_sDir);
			fc.addChoosableFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					String name = f.getName().toLowerCase();
					if (name.endsWith(".trees")) {
						return true;
					}
					if (name.endsWith(".tre")) {
						return true;
					}
					if (name.endsWith(".nex")) {
						return true;
					}
					if (name.endsWith(".t")) {
						return true;
					}
					return false;
				}

				// The description of this filter
				@Override
				public String getDescription() {
					return "Nexus trees files";
				}
			});

			fc.setDialogTitle("Save Graph");
			int rval = fc.showSaveDialog(m_Panel);

			if (rval == JFileChooser.APPROVE_OPTION) {
				String sFileName = fc.getSelectedFile().toString();
				if (sFileName.lastIndexOf('/') > 0) {
					settings.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
				}
				try {
					FileWriter outfile = new FileWriter(sFileName);
					StringBuffer buf = new StringBuffer();
					buf.append("#NEXUS\n");
					buf.append("Begin trees\n");
					buf.append("\tTranslate\n");
					for (int i = 0; i < settings.m_sLabels.size(); i++) {
						buf.append("\t\t" + i + " " + settings.m_sLabels.get(i));
						if (i < settings.m_sLabels.size() - 1) {
							buf.append(",");
						}
						buf.append("\n");
					}
					buf.append(";\n");
					outfile.write(buf.toString());
					for (int i = 0; i < treeData.m_trees.length; i++) {
						outfile.write("tree STATE_" + i + " = " + treeData.m_trees[i].toString() + ";\n");
						System.out.println(treeData.m_trees[i].toString(settings.m_sLabels, false));
					}
					outfile.write("End;\n");
					outfile.close();
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Error writing file: " + e.getMessage(), "File save error",
							JOptionPane.PLAIN_MESSAGE);
					return;
				}
				m_jStatusBar.setText("Saved " + sFileName);
				fitToScreen();
			}
		}
	}; // class ActionSaveAs

	public void loadImages() {
		JFileChooser fc = new JFileChooser(settings.m_sDir);
		fc.setDialogTitle("Load Image Map (text file mapping taxon names on image files)");
		int rval = fc.showOpenDialog(m_Panel);

		if (rval == JFileChooser.APPROVE_OPTION) {
			String sFileName = fc.getSelectedFile().toString();
			if (sFileName.lastIndexOf('/') > 0) {
				settings.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
			}
			try {
				settings.m_LabelImages = new BufferedImage[settings.m_sLabels.size()];
		        BufferedReader fin = new BufferedReader(new FileReader(sFileName));
		        //StringBuffer buf = new StringBuffer();
		        String sStr = null;
		        // eat up the header
	            fin.readLine();
		        while (fin.ready()) {
		            sStr = fin.readLine();
			            if (!sStr.trim().equals("")) {
			            String [] sStrs = sStr.split("\\s+");
			            if (sStrs.length != 2) {
			            	JOptionPane.showMessageDialog(m_Panel, "Found \"" + sStr + "\" but expected only two words on a line");
			            	settings.m_LabelImages = null;
			            	fin.close();
			            	return;
			            }
			            String sLabel = sStrs[0].toLowerCase();
			            String imageFile = sStrs[1];
			            int k = 0;
			            while (k < settings.m_sLabels.size() && !settings.m_sLabels.get(k).toLowerCase().equals(sLabel)) {
			            	k++;
			            }
			            if (k == settings.m_sLabels.size()) {
			            	JOptionPane.showMessageDialog(m_Panel, "Taxon \"" + sLabel + "\" could not be found");
			            	settings.m_LabelImages = null;
			            	fin.close();
			            	return;
			            }
			            System.err.println("Loading " + imageFile);
			            File file = new File(imageFile);
			            if (file.exists()) {
			            	settings.m_LabelImages[k] = ImageIO.read(file);
			            } else {
			            	System.err.println("File " + imageFile + " does not exist");
			            }
		            }
		        }
		        fin.close();
				
			} catch (OutOfMemoryError e) {
				JOptionPane.showMessageDialog(null, "Error loading file: " + e.getMessage(), "File load error",
						JOptionPane.PLAIN_MESSAGE);
				settings.m_LabelImages = null;
				return;
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Error loading file: " + e.getMessage(), "File load error",
						JOptionPane.PLAIN_MESSAGE);
				settings.m_LabelImages = null;
				return;
			}
			makeDirty();
		}
		
	}

	Action a_loadimage = new MyAction("Background image ", "Load background image", "bgimage", -1) {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			JFileChooser fc = new JFileChooser(settings.m_sDir);
			fc.addChoosableFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					String name = f.getName().toLowerCase();
					if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif")) {
						return true;
					}
					return false;
				}

				// The description of this filter
				@Override
				public String getDescription() {
					return "Image files";
				}
			});

			fc.setDialogTitle("Load Background Image");
			int rval = fc.showOpenDialog(m_Panel);

			if (rval == JFileChooser.APPROVE_OPTION) {
				String sFileName = fc.getSelectedFile().toString();
				if (sFileName.lastIndexOf('/') > 0) {
					settings.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
				}
				try {
					loadBGImage(sFileName);
				} catch (OutOfMemoryError e) {
					JOptionPane.showMessageDialog(null, "Error loading file: " + e.getMessage(), "File load error",
							JOptionPane.PLAIN_MESSAGE);
					return;
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Error loading file: " + e.getMessage(), "File load error",
							JOptionPane.PLAIN_MESSAGE);
					return;
				}
				makeDirty();
			}
		}
	}; // class ActionLoadImage

	void loadBGImage(String sFileName) throws Exception {
		m_bgImage = ImageIO.read(new File(sFileName));
		try {
			Pattern pattern = Pattern
					.compile(".*\\(([0-9\\.Ee-]+),([0-9\\.Ee-]+)\\)x\\(([0-9\\.Ee-]+),([0-9\\.Ee-]+)\\).*");
			Matcher matcher = pattern.matcher(sFileName);
			matcher.find();
			m_fBGImageBox[1] = Float.parseFloat(matcher.group(1));
			m_fBGImageBox[0] = Float.parseFloat(matcher.group(2));
			m_fBGImageBox[3] = Float.parseFloat(matcher.group(3));
			m_fBGImageBox[2] = Float.parseFloat(matcher.group(4));
		} catch (Exception e) {
			final double[] fBGImageBox = { -180, -90, 180, 90 };
			m_fBGImageBox = fBGImageBox;
		}
	} // loadBGImage

	Action a_viewClades = new MyAction("View clades", "List clades and their densities", "viewclades", -1) {
		private static final long serialVersionUID = -1;

		@Override
		public void actionPerformed(ActionEvent ae) {
			JDialog dlg = new JDialog();
			dlg.setModal(true);
			dlg.setSize(400, 400);
			setWaitCursor();
			//m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			StringBuilder b = new StringBuilder();
			for (String s : treeData.cladesToString()) {
				b.append(s);
			}
			setDefaultCursor();
			//m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			JTextArea textArea = new JTextArea(b.toString());
			JScrollPane scrollPane = new JScrollPane(textArea);
			dlg.add(scrollPane);
			dlg.setTitle("Clades and their probabilities");
			dlg.setVisible(true);
		}
	}; // ActionViewClades

	Action a_help = new MyAction("Help", "DensiTree - Tree Set Visualization Help", "help", -1) {
		private static final long serialVersionUID = -20389110859354L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			String sStatus = getStatus();
			String sCmdLineOptions = "\n\nTo start with the same settings, use the following command:\njava -jar DensiTree.jar -c "
					+ settings.m_fCTreeIntensity
					+ " -i "
					+ settings.m_fTreeIntensity
					+ " -j "
					+ settings.m_nJitter
					+ " -w "
					+ settings.m_nCTreeWidth
					+ " -v " + settings.m_nTreeWidth + " -f " + m_nAnimationDelay + " -t " + m_Panel.m_nDrawThreads + " -b " + m_nBurnIn;
			System.out.println(sCmdLineOptions);
			JOptionPane.showMessageDialog(null, banner() + sStatus + sCmdLineOptions, "Help Message",
					JOptionPane.PLAIN_MESSAGE);
		}
	}; // class ActionHelp

	public Action a_about = new MyAction("About", "Help about", "about", -1) {
		private static final long serialVersionUID = -20389110859353L;
		@Override
		public void actionPerformed(ActionEvent ae) {
			if (JOptionPane.showOptionDialog(null, banner() +
					"Citation:\n" + CITATION,
					"About Message", JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE, getIcon("DensiTree"), new String[]{"Copy citation to clipboard","Close"},"Close") == 0) {
			    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(CITATION), null);			
			}
		}
	}; // class ActionAbout

	Action a_labelwidth = new MyAction("Label width", "Label width when root at left", "labelwidth", -1) {
		private static final long serialVersionUID = -2L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			String sLabeWidth = JOptionPane.showInputDialog("Labe Width:", settings.m_nLabelWidth + "");
			if (sLabeWidth != null) {
				try {
					settings.m_nLabelWidth = Integer.parseInt(sLabeWidth);
				} catch (Exception e) {
				}
				fitToScreen();
			}
		}
	}; // class ActionLabelWidth

	Action a_burnin = new MyAction("Burn in", "Burn in", "burnin", -1) {
		private static final long serialVersionUID = -2L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			String sBurnIn = JOptionPane.showInputDialog("Burn in:", m_nBurnIn + "");
			if (sBurnIn != null) {
				try {
					m_nBurnIn = Integer.parseInt(sBurnIn);
					init(m_sFileName);
					calcLines();
					fitToScreen();
				} catch (Exception e) {
				}
			}
		}
	}; // class ActionBurnin

	Action a_geolinewidth = new MyAction("Geo line width", "Geographical line width", "geolinewidth", -1) {
		private static final long serialVersionUID = -2L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			String sGeoWidth = JOptionPane.showInputDialog("Geographical line width:", settings.m_nGeoWidth + "");
			if (sGeoWidth != null) {
				try {
					settings.m_nGeoWidth = Integer.parseInt(sGeoWidth);
					m_Panel.clearImage();
					repaint();
				} catch (Exception e) {
				}
			}
		}
	}; // class ActionGeoWidth

	Action a_viewstatusbar = new MyAction("View statusbar", "View statusbar", "statusbar", -1) {
		private static final long serialVersionUID = -20389330812354L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_jStatusBar.setVisible(!m_jStatusBar.isVisible());
		} // actionPerformed
	}; // class ActionViewStatusbar

	Action a_viewtoolbar = new MyAction("View toolbar", "View toolbar", "toolbar", -1) {
		private static final long serialVersionUID = -20389110812354L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_jTbTools.setVisible(!m_jTbTools.isVisible());
		} // actionPerformed
	}; // class ActionViewToolbar

	Action a_viewtoolbar2 = new MyAction("View Sidebar", "View Sidebar", "sidebar", -1) {
		private static final long serialVersionUID = -20389110812354L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_jTbTools2.setVisible(!m_jTbTools2.isVisible());
		} // actionPerformed
	}; // class ActionViewToolbar

	Action a_viewcladetoolbar = new MyAction("View clade toolbar", "View clade toolbar", "cladetoolbar", -1) {
		private static final long serialVersionUID = -1;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_jTbCladeTools.setVisible(!m_jTbCladeTools.isVisible());
			if (m_jTbCladeTools.isVisible()) {
				JSplitPane pane = (JSplitPane) m_Panel.getParent().getParent().getParent().getParent();
				// int loc = pane.getDividerLocation();
				// Set a proportional location
				pane.setDividerLocation(0.8);
			}
			
		} // actionPerformed
	}; // class ActionViewToolbar

	Action a_zoomin = new MyAction("Zoom in", "Zoom in", "zoomin", KeyEvent.VK_EQUALS) {
		private static final long serialVersionUID = -2038911085935515L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_fScale *= 1.2;
			a_zoomout.setEnabled(true);
			fitToScreen();
			m_jStatusBar.setText("Zooming in");
		}
	}; // class ActionZoomIn

	Action a_zoomout = new MyAction("Zoom out", "Zoom out", "zoomout", KeyEvent.VK_MINUS) {
		private static final long serialVersionUID = -203891108593551L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_fScale /= 1.2;
			if (m_fScale <= 1.000001) {
				m_fScale = 1.0f;
				a_zoomout.setEnabled(false);
			}
			fitToScreen();
			m_jStatusBar.setText("Zooming out");
		}
	}; // class ActionZoomOut

	Action a_zoomintree = new MyAction("Zoom in height", "Zoom in tree height", "zoominh", KeyEvent.VK_X) {
		private static final long serialVersionUID = -1;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_fTreeScale *= 1.2;
			m_fTreeOffset = m_fHeight - m_fHeight / m_fTreeScale;
			a_zoomouttree.setEnabled(true);
			calcLines();
			m_Panel.clearImage();
			makeDirty();
			m_jStatusBar.setText("Zooming in tree height");
		}
	}; // class ActionZoomInTree

	Action a_zoomouttree = new MyAction("Zoom out height", "Zoom out tree height", "zoomouth", KeyEvent.VK_X | KeyEvent.ALT_DOWN_MASK) {
		private static final long serialVersionUID = -1;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_fTreeScale /= 1.2;
			if (m_fTreeScale <= 1.000001) {
				m_fTreeScale = 1.0f;
				a_zoomouttree.setEnabled(false);
			}
			m_fTreeOffset = m_fHeight - m_fHeight / m_fTreeScale;
			calcLines();
			m_Panel.clearImage();
			makeDirty();
			m_jStatusBar.setText("Zooming out tree height");
		}
	}; // class ActionZoomOutTree

	MyAction a_animateStart = new MyAction("Start", "Start Animation", "start", KeyEvent.VK_D|KeyEvent.ALT_DOWN_MASK) {
		private static final long serialVersionUID = -1L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (m_viewMode == ViewMode.ANIMATE) {
				m_viewMode = ViewMode.BROWSE;
				a_animateStart.setIcon("start");
			} else {
				if (m_viewMode != ViewMode.BROWSE) {
					m_iAnimateTree = 0;
				}
				m_viewMode = ViewMode.ANIMATE;
				a_animateStart.setIcon("stop");
			}
			m_Panel.repaint();
		}
	}; // class ActionAnimateStart

	Action a_drawtreeset = new MyAction("Draw Tree Set", "Draw Tree Set", "redraw", KeyEvent.VK_R) {
		private static final long serialVersionUID = -4L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			setWaitCursor();
			System.err.println("MODS=" + ae.getModifiers());
			if (ae.getModifiers() == 18) {
				// when menu item is selected & ctrl key is pressed
				System.err.println("start recording: results in /tmp");
				settings.m_nFrameNr = 0;
				settings.m_bRecord = true;
			}
			m_viewMode = ViewMode.DRAW;
			a_animateStart.setIcon("start");
			if (m_bIsDirty) {
				System.err.println("calclines");
				calcLines();
			}
			m_Panel.clearImage();
			repaint();
			System.gc();
		} // actionPerformed
	}; // class ActionDrawTreeSet

	Action a_selectAll = new MyAction("Select All", "Select All", "selectall", KeyEvent.VK_A) {
		private static final long serialVersionUID = 5L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			for (int i = 0; i < treeData.m_bSelection.length; i++) {
				treeData.m_bSelection[i] = true;
				// m_bSelection[i] = m_fLatitude.get(i)==0 &&
				// m_fLongitude.get(i)==0;
			}
			repaint();
		} // actionPerformed
	};// class ActionSelectAll
	Action a_unselectAll = new MyAction("Unselect All", "Unselect All", "unselectall", KeyEvent.VK_U) {
		private static final long serialVersionUID = 5L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			for (int i = 0; i < treeData.m_bSelection.length; i++) {
				treeData.m_bSelection[i] = false;
			}
			repaint();
		} // actionPerformed
	};// class ActionUnSelectAll
	Action a_del = new MyAction("Delete", "Delete selected", "del", -1) { //KeyEvent.VK_DELETE) {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			int nDeleted = 0;
			for (int i = treeData.m_bSelection.length - 1; i >= 0 && settings.m_nNrOfLabels > 2; i--) {
				if (treeData.m_bSelection[i]) {
					// delete node with nr i
					for (int j = 0; j < treeData.m_trees.length; j++) {
						treeData.m_trees[j] = deleteLeaf(treeData.m_trees[j], i);
						renumber(treeData.m_trees[j], i);
						treeData.m_trees[j].labelInternalNodes(settings.m_nNrOfLabels - 1);
					}
					for (int j = 0; j < treeData.m_cTrees.length; j++) {
						treeData.m_cTrees[j] = deleteLeaf(treeData.m_cTrees[j], i);
						renumber(treeData.m_cTrees[j], i);
						treeData.m_cTrees[j].labelInternalNodes(settings.m_nNrOfLabels - 1);
					}
					settings.m_sLabels.remove(i);
					settings.m_nNrOfLabels--;
					if (settings.m_fLongitude != null && settings.m_fLongitude.size() > i) {
						settings.m_fLongitude.remove(i);
						settings.m_fLatitude.remove(i);
					}
					int[] nOrder = new int[settings.m_nOrder.length - 1];
					int[] nRevOrder = new int[settings.m_nRevOrder.length - 1];
					int k = 0;
					for (int j = 0; j < nOrder.length; j++) {
						if (settings.m_nOrder[k] == i) {
							k++;
						}
						nOrder[j] = (settings.m_nOrder[k] < i ? settings.m_nOrder[k] : settings.m_nOrder[k] - 1);
						k++;
					}
					k = 0;
					for (int j = 0; j < nRevOrder.length; j++) {
						if (settings.m_nRevOrder[k] == i) {
							k++;
						}
						nRevOrder[j] = (settings.m_nRevOrder[k] < i ? settings.m_nRevOrder[k] : settings.m_nRevOrder[k] - 1);
						k++;
					}
					settings.m_nOrder = nOrder;
					settings.m_nRevOrder = nRevOrder;
					nDeleted++;
				}
			}
			System.err.println("ORDER:" + Arrays.toString(settings.m_nOrder));
			System.err.println("REVOR:" + Arrays.toString(settings.m_nRevOrder));
			treeData.m_bSelection = new boolean[treeData.m_bSelection.length - nDeleted];
			for (int i = 0; i < treeData.m_bSelection.length; i++) {
				treeData.m_bSelection[i] = true;
			}
			fitToScreen();
			calcPositions();
			calcLines();
			m_Panel.clearImage();
			repaint();
		} // actionPerformed
	};// class ActionDel

	void renumber(Node node, int iNodeNr) {
		if (node.isLeaf()) {
			if (node.getNr() > iNodeNr) {
				node.m_iLabel--;
			}
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
					// replace root by node's sibling
					sibling.m_Parent = null;
					return sibling;
				}
				// parent is not root. Link grandparent to sibling
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
			if (node2.isRoot()) {
				return node2;
			}
			node2 = deleteLeaf(node.m_right, iNodeNr);
			if (node2.isRoot()) {
				return node2;
			}
		}
		return node;
	} // deleteLeaf

	/** index to current action on undo-stack **/
	int m_iUndo = 0;
	Vector<DoAction> m_doActions = new Vector<DoAction>();

	class DoAction {
		int[] m_nOrder2;
		int[] m_nRevOrder2;
		float[] m_fPosX;

		DoAction() {
			m_nOrder2 = settings.m_nOrder.clone();
			m_nRevOrder2 = settings.m_nRevOrder.clone();
			m_fPosX = new float[settings.m_sLabels.size()];
			getPosition(treeData.m_trees[0], m_fPosX);
		}


		void doThisAction() {
			settings.m_nOrder = m_nOrder2.clone();
			settings.m_nRevOrder = m_nRevOrder2.clone();
			for (int i = 0; i < treeData.m_trees.length; i++) {
				setPosition(treeData.m_trees[i], m_fPosX);
				positionRest(treeData.m_trees[i]);
			}
			for (int i = 0; i < treeData.m_cTrees.length; i++) {
				setPosition(treeData.m_cTrees[i], m_fPosX);
				positionRest(treeData.m_cTrees[i]);
			}
			calcLines();
			makeDirty();
		} // do

	} // class DoAction

	void addAction(DoAction action) {
		while (m_iUndo < m_doActions.size()) {
			m_doActions.remove(m_iUndo);
		}
		m_doActions.add(action);
		m_iUndo++;
	} // addAction

	Action a_undo = new MyAction("Undo", "Undo", "udno", KeyEvent.VK_Z) {
		private static final long serialVersionUID = -4L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (m_iUndo > 0) {
				m_iUndo--;
				m_doActions.elementAt(m_iUndo - 1).doThisAction();
				repaint();
			}
		} // actionPerformed
	}; // class ActionUndo
	Action a_redo = new MyAction("Redo", "Redo", "reno", KeyEvent.VK_Y) {
		private static final long serialVersionUID = -4L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (m_iUndo < m_doActions.size()) {
				m_iUndo++;
				m_doActions.elementAt(m_iUndo - 1).doThisAction();
				repaint();
			}
		} // actionPerformed
	}; // class ActionRedo

	Action a_moveup = new MyAction("Move labels up", "Move selected labels up", "moveup", KeyEvent.VK_M) {
		private static final long serialVersionUID = -4L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (!moveSanityChek()) {
				return;
			}
			moveSelectedLabelsUp();
			calcLines();
			m_Panel.clearImage();
			repaint();
		} // actionPerformed
	}; // class ActionMoveUp

	Action a_movedown = new MyAction("Move labels down", "Move selected labels down", "movedown", KeyEvent.VK_M | KeyEvent.ALT_DOWN_MASK) {
		private static final long serialVersionUID = -4L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (!moveSanityChek()) {
				return;
			}
			moveSelectedLabelsDown();
			calcLines();
			m_Panel.clearImage();
			repaint();
		} // actionPerformed
	}; // class ActionMoveDown

	Action a_browsefirst = new MyAction("Browse First", "Browse First", "browsefirst", -1) {
		private static final long serialVersionUID = 5L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_viewMode = ViewMode.BROWSE;
			a_animateStart.setIcon("start");
			m_iAnimateTree = 0;
			repaint();
		} // actionPerformed
	}; // class ActionBrowseFirst

	Action a_browseprev = new MyAction("Browse Prev", "Browse Prev", "browseprev", KeyEvent.VK_P|KeyEvent.ALT_DOWN_MASK) {
		private static final long serialVersionUID = 5L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_viewMode = ViewMode.BROWSE;
			a_animateStart.setIcon("start");
			m_iAnimateTree--;
			if (m_iAnimateTree < 0) {
				m_iAnimateTree = 0;
			}
			repaint();
		} // actionPerformed
	}; // class ActionBrowsePrev

	Action a_browsenext = new MyAction("Browse Next", "Browse Next", "browsenext", KeyEvent.VK_N|KeyEvent.ALT_DOWN_MASK) {
		private static final long serialVersionUID = 5L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_viewMode = ViewMode.BROWSE;
			a_animateStart.setIcon("start");
			m_iAnimateTree++;
			if (m_iAnimateTree == treeData.m_nTopologies) {
				m_iAnimateTree = treeData.m_nTopologies - 1;
			}
			repaint();
		} // actionPerformed
	}; // class ActionBrowseNext

	Action a_browselast = new MyAction("Browse Last", "Browse Last", "browselast", -1) {
		private static final long serialVersionUID = 5L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			m_viewMode = ViewMode.BROWSE;
			a_animateStart.setIcon("start");
			m_iAnimateTree = treeData.m_nTopologies - 1;
			repaint();
		} // actionPerformed
	}; // class ActionBrowse

	Action a_setfont = new MyAction("Set Font", "Set Font", "font", -1) {
		private static final long serialVersionUID = 5L;

		// @SuppressWarnings("deprecation")
		@Override
		public void actionPerformed(ActionEvent ae) {
			JFontChooser fontChooser = new JFontChooser();
			// fontChooser.setFont(m_font);
			int result = fontChooser.showDialog(null);
			if (result == JFontChooser.OK_OPTION) {
				m_font = fontChooser.getSelectedFont();
				repaint();
			}
		} // actionPerformed
	}; // class SetFont

	SettingAction a_animationSpeedUp = new SettingAction("Animation Speed+", "Increase Animation Speed", "aspeedup",
			KeyEvent.VK_F);
	SettingAction a_animationSpeedDown = new SettingAction("Animation Speed-", "Decrease Animation Speed",
			"aspeeddown", KeyEvent.VK_F | KeyEvent.ALT_DOWN_MASK);
	SettingAction a_treeWidthUp = new SettingAction("Tree Width+", "Increase Width of Trees", "treewidthup", KeyEvent.VK_V);
	SettingAction a_treeWidthDown = new SettingAction("Tree Width-", "Decrease Width of Trees", "treewidthdown", KeyEvent.VK_V | KeyEvent.ALT_DOWN_MASK);
	SettingAction a_cTreeWidthUp = new SettingAction("Consensus Tree Width+", "Increase Width of Consensus Trees",
			"ctreewidthup", KeyEvent.VK_W);
	SettingAction a_cTreeWidthDown = new SettingAction("Consensus Tree Width-", "Decrease Width of Consensus Trees",
			"ctreewidthdown", KeyEvent.VK_W | KeyEvent.ALT_DOWN_MASK);
	SettingAction a_intensityUp = new SettingAction("Intensity+", "Increase Intensity of Trees", "intensityup",
			KeyEvent.VK_I);
	SettingAction a_intensityDown = new SettingAction("Intensity-", "Decrease Intensity of Trees", "intensitydown", KeyEvent.VK_I| KeyEvent.ALT_DOWN_MASK);
	SettingAction a_cIntensityUp = new SettingAction("Consensus Intensity+", "Increase Intensity of Consensus Trees",
			"cintensityup", KeyEvent.VK_C);
	SettingAction a_cIntensityDown = new SettingAction("Consensus Intensity-", "Decrease Intensity of Consensus Trees",
			"cintensitydown", KeyEvent.VK_C | KeyEvent.ALT_DOWN_MASK);
	SettingAction a_jitterUp = new SettingAction("Jitter+", "Increase Jitter on x-coordinate of Trees", "jitterup",
			KeyEvent.VK_J);
	SettingAction a_jitterDown = new SettingAction("Jitter-", "Decrease Jitter on x-coordinate of Trees", "jitterdown",
			KeyEvent.VK_J| KeyEvent.ALT_DOWN_MASK);
	SettingAction a_threadsUp = new SettingAction("Drawing Threads+", "Increase number of Drawing Threads",
			"threadsup", KeyEvent.VK_T);
	SettingAction a_threadsDown = new SettingAction("Drawing Threads-", "Decrease number of Drawing Threads",
			"threadsdown", KeyEvent.VK_T| KeyEvent.ALT_DOWN_MASK);
	SettingAction a_angleThresholdUp = new SettingAction("Angle Correction+",
			"Increase Threshold for angle correction", "angleup", KeyEvent.VK_N);
	SettingAction a_a_angleThresholdDown = new SettingAction("Angle Correction-",
			"Decrease Threshold for angle correction", "angledpown", KeyEvent.VK_N| KeyEvent.ALT_DOWN_MASK);


	public JCheckBoxMenuItem m_viewEditTree;
	public JCheckBoxMenuItem m_viewClades; 

	
	
	
	void makeToolbar(JToolBar m_jTbTools) {
		m_jTbTools.setFloatable(false);
		m_jTbTools.add(a_load);
		m_jTbTools.addSeparator(new Dimension(2, 2));
		m_jTbTools.add(a_drawtreeset);
		m_jTbTools.addSeparator(new Dimension(2, 2));
		m_jTbTools.add(a_browsefirst);
		m_jTbTools.add(a_browseprev);
		m_jTbTools.add(a_animateStart);
		m_jTbTools.add(a_browsenext);
		m_jTbTools.add(a_browselast);
		m_jTbTools.addSeparator(new Dimension(2, 2));
		m_jTbTools.add(a_intensityUp);
		m_jTbTools.add(a_intensityDown);
		m_jTbTools.add(a_cIntensityUp);
		m_jTbTools.add(a_cIntensityDown);
		m_jTbTools.addSeparator(new Dimension(2, 2));
		m_jTbTools.add(a_treeWidthUp);
		m_jTbTools.add(a_treeWidthDown);
		m_jTbTools.add(a_cTreeWidthUp);
		m_jTbTools.add(a_cTreeWidthDown);
		m_jTbTools.addSeparator(new Dimension(2, 2));
		m_jTbTools.add(a_animationSpeedDown);
		m_jTbTools.add(a_animationSpeedUp);
		m_jTbTools.addSeparator(new Dimension(2, 2));
		m_jTbTools.add(a_jitterUp);
		m_jTbTools.add(a_jitterDown);
		m_jTbTools.addSeparator(new Dimension(2, 2));
		m_jTbTools.add(a_help);

	
		// Create an action with an icon
		Action action = new AbstractAction("Button Label", getIcon("modedefault")) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(0);
			}
		};


		Action action3 = new AbstractAction("", getIcon("modestar")) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(1);
			}
		};

		Action action4 = new AbstractAction("", getIcon("modecentralised")) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(2);
			}
		};

		Action action5 = new AbstractAction("", getIcon("modeanglecorrected")) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(3);
			}
		};
		
		if (viz.util.Util.isMac()) {
			B = 10;
		}
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(3, B, 5, B));
		panel.setLayout(new GridLayout(0, 2));
		panel.add(createToolBarButton(action));
		panel.add(createToolBarButton(action3));
		panel.add(createToolBarButton(action4));
		panel.add(createToolBarButton(action5));

		
		JPanel toolPanel = new JPanel();
		toolPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.PAGE_START;
		gbc.gridx = 0;
		gbc.gridy = 0;
		//m_jTbTools2.setLayout(new BoxLayout(m_jTbTools2, BoxLayout.Y_AXIS));
		toolPanel.add(new ExpandablePanel("Type", panel, true), gbc);
		//toolPanel.add(panel, gbc);
				
		Action action6 = new AbstractAction("", getIcon("stylestraight")) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(0);
			}
		};
		Action action7 = new AbstractAction("", getIcon("styleblock")) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(1);
			}
		};
		Action action8 = new AbstractAction("", getIcon("stylearced")) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(2);
			}
		};
		Action action9 = new AbstractAction("", getIcon("stylesteep")) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(3);
			}
		};
		panel = new JPanel();
		panel.setBorder(new EmptyBorder(3, B, 3, B));
		panel.setLayout(new GridLayout(0, 2));
		panel.add(createToolBarButton(action6));
		panel.add(createToolBarButton(action7));
		panel.add(createToolBarButton(action8));
		panel.add(createToolBarButton(action9));
		
		gbc.gridy++;
		toolPanel.add(new ExpandablePanel("Style", panel, true), gbc);
		gbc.gridy++;
		toolPanel.add(new ExpandablePanel("Show", new ShowPanel(this)), gbc);
		gbc.gridy++;
		toolPanel.add(new ExpandablePanel("Grid", new GridPanel(this)), gbc);
		gbc.gridy++;
		toolPanel.add(new ExpandablePanel("Label", new LabelPanel(this)), gbc);
		gbc.gridy++;
		toolPanel.add(new ExpandablePanel("Geography", new GeoPanel(this)), gbc);
		gbc.gridy++;
		toolPanel.add(new ExpandablePanel("Line Width", new LineWidthPanel(this)), gbc);
		gbc.gridy++;
		toolPanel.add(new ExpandablePanel("Line Color", new ColorPanel(this)), gbc);
		gbc.gridy++;
		toolPanel.add(new ExpandablePanel("Burn in", new BurninPanel(this)), gbc);
		gbc.gridy++;
		toolPanel.add(new ExpandablePanel("Clades", new CladePanel(this)), gbc);
		//JScrollPane toolPaneScroller = new JScrollPane(toolPanel);
		m_jTbTools2.add(toolPanel);
//		for (int i = 0; i < 100; i++) {
//			gbc.gridy++;
//			m_jTbTools2.add(Box.createVerticalGlue(), gbc);
//		}

		treeData.m_cladelist = new JList<String>(treeData.m_cladelistmodel);
		treeData.m_cladelist.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (treeData.m_bAllowCladeSelection) {
	 				treeData.m_cladeSelection.clear();
					for (int i : treeData.m_cladelist.getSelectedIndices()) {
						if (treeData.m_cladeWeight.get(i) > 0.01 && ((settings.m_Xmode == 1 && treeData.m_clades.get(i).length > 1) || (settings.m_Xmode == 2 && treeData.m_clades.get(i).length == 1))) {
							treeData.m_cladeSelection.add(i);
						}
					}
					resetCladeSelection();
					System.err.println(treeData.m_cladelist.getSelectedValuesList());
					System.err.println(treeData.m_cladelist.getSelectedValuesList().size() + " items selected");
					repaint();
				}
			}
		});
		JScrollPane scrollingList = new JScrollPane(treeData.m_cladelist);
		//scrollingList.setPreferredSize(new Dimension(1200,600));
		//scrollingList.setMinimumSize(scrollingList.getPreferredSize());
		m_jTbCladeTools.setLayout(new BorderLayout());
		m_jTbCladeTools.add(scrollingList, BorderLayout.CENTER);
		m_jTbCladeTools.setFloatable(false);
		m_jTbCladeTools.setVisible(false);
	} // makeToolbar

	private JButton createToolBarButton(Action action) {
		// Add a button to the toolbar; remove the label and margin before adding
		JButton c1 = new JButton(action);
		c1.setText(null);
		c1.setMargin(new Insets(0, 0, 0, 0));
		return c1;
	}

	void setIcon(JCheckBoxMenuItem item, String sIcon) {
		java.net.URL tempURL = ClassLoader.getSystemResource(ICONPATH + sIcon + ".png");
		if (tempURL != null) {
			item.setIcon(new ImageIcon(tempURL));
		} else {
			item.setIcon(new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_4BYTE_ABGR)));
		}
	} // setIcon

	protected void makeMenuBar(JMenuBar m_menuBar) {
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		// ----------------------------------------------------------------------
		// File menu */
		m_menuBar.add(fileMenu);
		fileMenu.add(a_new);
		fileMenu.add(a_load);
		fileMenu.add(a_loadMirror);
		fileMenu.add(a_saveas);
		fileMenu.add(a_loadimage);

		fileMenu.addSeparator();
		fileMenu.add(a_print);
		// fileMenu.add(a_export);
		fileMenu.add(a_export);
		if (!viz.util.Util.isMac()) {
			fileMenu.addSeparator();
			fileMenu.add(a_quit);
		}

		// ----------------------------------------------------------------------
		// Edit menu */
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		m_menuBar.add(editMenu);
		editMenu.add(a_undo);
		editMenu.add(a_redo);
		editMenu.add(a_selectAll);
		editMenu.add(a_unselectAll);
		editMenu.add(a_del);
		editMenu.add(a_paste);
		editMenu.add(a_moveup);
		editMenu.add(a_movedown);

		m_viewEditTree = new JCheckBoxMenuItem("Show Edit Tree", settings.m_bViewEditTree);
		m_viewEditTree.setIcon(new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_4BYTE_ABGR)));
		m_viewEditTree.addActionListener(ae -> {
				boolean bPrev = settings.m_bViewEditTree;
				settings.m_bViewEditTree = m_viewEditTree.getState();
				if (bPrev != settings.m_bViewEditTree) {
					makeDirty();
				}
			});
		editMenu.add(m_viewEditTree);

		m_viewClades = new JCheckBoxMenuItem("Show Clades", settings.m_bViewClades);
		m_viewClades.setIcon(new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_4BYTE_ABGR)));
		m_viewClades.addActionListener(ae-> {
				boolean bPrev = settings.m_bViewClades;
				settings.m_bViewClades = m_viewClades.getState();
				if (bPrev != settings.m_bViewClades) {
					makeDirty();
				}
			});
		m_viewClades.setEnabled(false);
		editMenu.add(m_viewClades);

		JMenu shuffleMenu = new JMenu("Shuffle");
		shuffleMenu.setIcon(new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_4BYTE_ABGR)));
		shuffleMenu
				.add(new ShuffleAction("Most Frequent", "Use most frequent tree order", "", -1, NodeOrderer.DEFAULT));
		shuffleMenu.add(new ShuffleAction("SPQ", "Order by optimising SPQ trees", "", KeyEvent.VK_Q|InputEvent.ALT_DOWN_MASK,
				NodeOrderer.SPQ));
		shuffleMenu.add(new ShuffleAction("Closest Outside First", "Order closest to outside leaf first", "", KeyEvent.VK_S|InputEvent.ALT_DOWN_MASK,
				NodeOrderer.CLOSEST_OUTSIDE_FIRST));
		shuffleMenu.add(new ShuffleAction("Optimised root canal tree",
				"Use root canal tree, then optimise", "", KeyEvent.VK_O|InputEvent.ALT_DOWN_MASK, NodeOrderer.OPTIMISE));
		shuffleMenu.add(new ShuffleAction("Sorted root canal tree",
				"Sort by root canal tree length", "", KeyEvent.VK_R|InputEvent.ALT_DOWN_MASK, NodeOrderer.SORT_BY_ROOT_CANAL_LENGTH));
		shuffleMenu.add(new ShuffleAction("Closest First", "Order closest leaf first", "", KeyEvent.VK_1|InputEvent.ALT_DOWN_MASK,
				NodeOrderer.CLOSEST_FIRST));
		shuffleMenu.add(new ShuffleAction("Single link", "Single link hierarchical clusterer", "", KeyEvent.VK_2|InputEvent.ALT_DOWN_MASK,
				NodeOrderer.SINGLE));
		shuffleMenu.add(new ShuffleAction("Complete link", "Complete link hierarchical clusterer", "", KeyEvent.VK_3|InputEvent.ALT_DOWN_MASK,
				NodeOrderer.COMPLETE));
		shuffleMenu.add(new ShuffleAction("Average link", "Average link hierarchical clusterer", "", KeyEvent.VK_4|InputEvent.ALT_DOWN_MASK,
				NodeOrderer.AVERAGE));
		shuffleMenu.add(new ShuffleAction("Mean link", "Mean link hierarchical clusterer", "", KeyEvent.VK_5|InputEvent.ALT_DOWN_MASK, NodeOrderer.MEAN));
		shuffleMenu.add(new ShuffleAction("Adjusted complete link", "Adjusted complete link hierarchical clusterer",
				"", KeyEvent.VK_6|InputEvent.ALT_DOWN_MASK, NodeOrderer.ADJCOMLPETE));
		// RRB: not for public release
		shuffleMenu.addSeparator();
		shuffleMenu.add(new ShuffleAction("Manual", "Manual", "", -1, NodeOrderer.MANUAL));
		shuffleMenu.add(new ShuffleAction("By Geography", "By Geography", "", -1, NodeOrderer.GEOINFO));
		shuffleMenu.add(new ShuffleAction("By meta data, all", "By meta data, show all paths", "", KeyEvent.VK_7|InputEvent.ALT_DOWN_MASK,
				NodeOrderer.META_ALL));
		shuffleMenu.add(new ShuffleAction("By meta data, sum", "By meta data, sum over paths", "", KeyEvent.VK_8|InputEvent.ALT_DOWN_MASK,
				NodeOrderer.META_SUM));
		shuffleMenu.add(new ShuffleAction("By meta data, mean", "By meta data, average over paths", "", KeyEvent.VK_9|InputEvent.ALT_DOWN_MASK,
				NodeOrderer.META_AVERAGE));

		editMenu.addSeparator();
		editMenu.add(shuffleMenu);

		// ----------------------------------------------------------------------
		// Draw all menu */
		JMenu drawallMenu = new JMenu("Draw All");
		drawallMenu.setMnemonic('D');
		m_menuBar.add(drawallMenu);
		final JCheckBoxMenuItem autoRefresh = new JCheckBoxMenuItem("Automatically refresh", m_bAutoRefresh);
		autoRefresh.addActionListener(ae-> {
				m_bAutoRefresh = autoRefresh.getState();
				if (m_bAutoRefresh && m_bIsDirty) {
					fitToScreen();
					// makeDirty();
				}
			});
		drawallMenu.add(autoRefresh);
		drawallMenu.add(a_drawtreeset);

		// ----------------------------------------------------------------------
		// Browse menu */
		JMenu browseMenu = new JMenu("Browse");
		browseMenu.setMnemonic('B');
		m_menuBar.add(browseMenu);
		browseMenu.add(a_browsefirst);
		browseMenu.add(a_browseprev);
		browseMenu.add(a_animateStart);
		browseMenu.add(a_browsenext);
		browseMenu.add(a_browselast);
		browseMenu.addSeparator();
		final JCheckBoxMenuItem animateOverWrite = new JCheckBoxMenuItem("Over write", m_bAnimateOverwrite);
		animateOverWrite.addActionListener(ae-> {
				m_bAnimateOverwrite = animateOverWrite.getState();
			});
		browseMenu.add(animateOverWrite);

		// ----------------------------------------------------------------------
		// Settings menu */
		JMenu settingsMenu = new JMenu("Settings");
		settingsMenu.setMnemonic('S');
		m_menuBar.add(settingsMenu);

		settingsMenu.addSeparator();
		settingsMenu.add(a_intensityUp);
		settingsMenu.add(a_intensityDown);
		settingsMenu.add(a_cIntensityUp);
		settingsMenu.add(a_cIntensityDown);
		settingsMenu.addSeparator();
		settingsMenu.add(a_treeWidthUp);
		settingsMenu.add(a_treeWidthDown);
		settingsMenu.add(a_cTreeWidthUp);
		settingsMenu.add(a_cTreeWidthDown);
		settingsMenu.addSeparator();
		settingsMenu.add(a_animationSpeedDown);
		settingsMenu.add(a_animationSpeedUp);
		settingsMenu.addSeparator();
		settingsMenu.add(a_jitterUp);
		settingsMenu.add(a_jitterDown);
		settingsMenu.addSeparator();
		settingsMenu.add(a_threadsUp);
		settingsMenu.add(a_threadsDown);


		// ----------------------------------------------------------------------
		// Window menu */
		JMenu windowMenu = new JMenu("Window");
		windowMenu.setMnemonic('W');
		m_menuBar.add(windowMenu);
		windowMenu.add(a_viewstatusbar);
		windowMenu.add(a_viewtoolbar);
		windowMenu.add(a_viewtoolbar2);
		windowMenu.add(a_viewcladetoolbar);
		windowMenu.addSeparator();
		windowMenu.add(a_zoomin);
		windowMenu.add(a_zoomout);
		windowMenu.add(a_zoomintree);
		windowMenu.add(a_zoomouttree);

		// ----------------------------------------------------------------------
		// Help menu */
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		m_menuBar.add(helpMenu);
		helpMenu.add(a_help);
		helpMenu.add(a_viewClades);
		if (!Util.isMac()) {
			helpMenu.add(a_about);
		}
	} // makeMenuBar
	

	public static DensiTree startNew(String [] args) {

		final DensiTree a = new DensiTree(new String[]{});
		
		if (viz.util.Util.isMac()) {
			try {
				// call viz.maconly.OSXAdapter.registerMacOSXApplication(a);
				// through reflection
			//	Class<?> osx = Class.forName("viz.maconly.OSXAdapter");
	        //    Method method = osx.getMethod("registerMacOSXApplication", DensiTree.class);
	        //    method.invoke(null, a);
	            URL url = ClassLoader.getSystemResource("viz/icons/" + "DensiTree.png");
	            Icon icon = new ImageIcon(url);
				jam.framework.Application application = new jam.framework.Application(null, "DensiTree", "about" , icon) {
					
					@Override
					public void initialize() {
					}
					
					@Override
					protected JFrame getDefaultFrame() {
						return null;
					}
					
					@Override
					public void doQuit() {
						a.a_quit.actionPerformed(null);
					}
					
					@Override
					public void doAbout() {
						a.a_about.actionPerformed(null);
					}
										
					@Override
					public DocumentFrame doOpenFile(File file) {
						return null;
					}
					
					@Override
					public DocumentFrame doNew() {
						return null;
					}
				};
                if (Util.getMajorJavaVersion() >= 9) {
                	Util.macOSXRegistration(application);
                } else {
    				jam.mac.Utils.macOSXRegistration(application);
                }

			} catch (Throwable e) {
				// ignore
			}
		}
		
		JFrame f;
		f = new JFrame(FRAME_TITLE);
		a.frame = f;
		f.setVisible(true);
		a.parseArgs(args);
		JMenuBar menuBar = a.getMenuBar();
		f.setJMenuBar(menuBar);
		f.add(a.m_jTbTools, BorderLayout.NORTH);
		f.add(a.m_jTbTools2, BorderLayout.EAST);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, a, a.m_jTbCladeTools);
		splitPane.setDividerLocation(0.9);

//		JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, a.m_jTbTools2, splitPane);
		
		f.add(splitPane, BorderLayout.CENTER);
		f.add(a.m_jStatusBar, BorderLayout.SOUTH);
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Dimension dim = a.getSize();
		f.setSize(dim.width + 31, dim.height + 40 + 84);
		f.setLocation(DensiTree.instances * 10 , DensiTree.instances * 10);
		a.fitToScreen();
		java.net.URL tempURL = ClassLoader.getSystemResource(DensiTree.ICONPATH + "DensiTree.png");
		try {
			f.setIconImage(ImageIO.read(tempURL));
		} catch (Exception e) {
			// ignore
		}
		a.m_Panel.setFocusable(true);
		return a;
		// a.fitToScreen();
	} // startNew

	List<ChangeListener> m_changeListeners = new ArrayList<ChangeListener>();

	public void addChangeListener(ChangeListener changeListener) {
		m_changeListeners.add(changeListener);
	}

	/**
	 * Main method
	 */
	public static void main(String[] args) {
		viz.util.Util.loadUIManager();
		startNew(args);
	}
	
} // class DensiTree
