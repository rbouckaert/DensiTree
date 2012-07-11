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
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
 */


package viz;

/**
 * Shows sets of cluster trees represented in Newick format as dendrograms and other graphs.
 * There are 2 modes of viewing a tree set
 * 1. draw all trees in the set
 * 2. browsing through the set of trees/animate through trees in the set, drawing them one by one
 * Restriction: binary trees only
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz, remco@cs.auckland.ac.nz, remco@cs.waikato.ac.nz)
 * @version $Revision: 2.1 $
 */

// the magic sentence to look for when releasing:
//RRB: not for public release

//TODO: truncate % of root
// log scale



import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.text.DecimalFormat;
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

public class DensiTree extends JPanel implements ComponentListener {
	final static String VERSION = "2.1.1 release candidate";
	final static String FRAME_TITLE = "DensiTree - Tree Set Visualizer";
	final static String CITATION = "Remco R. Bouckaert\n"+
		"DensiTree: making sense of sets of phylogenetic trees\n"+
		"Bioinformatics (2010) 26 (10): 1372-1373.\n"+
		"doi: 10.1093/bioinformatics/btq110";
	static int instances = 1;

	final static int B = 1;
	JFrame frame;
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
	
	private static final long serialVersionUID = 1L;
	/** path for icons */
	public static final String ICONPATH = "viz/icons/";

	/**
	 * default tree branch length, used when that info is not in the Newick tree
	 **/
	final static double DEFAULT_LENGTH = 0.001f;
	/** same trees, but represented as Node data structure **/
	Node[] m_trees;
	
	public Node m_rootcanaltree;
	// contains summary trees, root canal refers to one of those
	public Node [] m_summaryTree;

	/**
	 * Trees represented as lines for drawing block trees Units are tree lengths
	 * as represented in the Newick file The lines come in quartets
	 * ((x1,y1)(x1,y2)(x3,y2),(x3,y3)) and are concatenated in a long array. The
	 * final pair contains the line to the root.
	 * **/
	float[][] m_fLinesX;
	float[][] m_fLinesY;
	/**
	 * Trees represented as lines for drawing triangular trees Units are tree
	 * lengths as represented in the Newick file The lines come in triplets
	 * ((x1,y1)(x2,y2)(x3,y3)) and are concatenated in a long array. The final
	 * pair contains the line to the root.
	 * **/
	// float[][] m_fTLinesX;
	// float[][] m_fTLinesY;
	/**
	 * Width of individual lines, determined by some info in the metadata (if
	 * any) If specified, this only applies to block trees.
	 * **/
	public int[][] m_nLineColor;
	public float[][] m_fLineWidth;
	public float[][] m_fTopLineWidth;
	
	public int[][] m_nCLineColor;
	public float[][] m_fCLineWidth;
	public float[][] m_fTopCLineWidth;
	
	float[][] m_fRLinesX;
	float[][] m_fRLinesY;
	public int[][] m_nRLineColor;
	public float[][] m_fRLineWidth;
	public float[][] m_fRTopLineWidth;

	
	
	public Vector<String> m_sLabels;
	/** labels of leafs **/
	/** nr of labels in dataset **/
	public int m_nNrOfLabels = 0;
	/** position information for the leafs (if available) **/
	public Vector<Float> m_fLongitude;
	public Vector<Float> m_fLatitude;
	/** extreme values for position information **/
	public float m_fMaxLong, m_fMaxLat, m_fMinLong, m_fMinLat;

	/** order of appearance of leafs, used to determine x-coordinates of leafs **/
	int[] m_nOrder;
	/** reverse of m_nOrder, useful for reordering **/
	int[] m_nRevOrder;

	/** Topology number of the tree, in order of appearance in tree set **/
	int[] m_nTopology;
	/**
	 * Topology number for particular tree in order of popularity (most popular
	 * = 0, next most popular = 1, etc.) Useful for coloring trees.
	 **/
	int[] m_nTopologyByPopularity;
	/** nr of distinct topologies **/
	int m_nTopologies;
	/**
	 * relative weight of tree topology measured by its frequency of appearance
	 * in the set. Adds to unity.
	 */
	float[] m_fTreeWeight;
	/** as m_trees, but for consensus trees **/
	Node[] m_cTrees;
	/** as m_nLines, but for consensus trees **/
	float[][] m_fCLinesX;
	float[][] m_fCLinesY;
	/** as m_nTLines, but for consensus trees **/
	// float[][] m_fCTLinesX;
	// float[][] m_fCTLinesY;


	/** height of highest tree **/
	float m_fHeight = 0;
	/** scale factors for drawing to screen **/
	float m_fScaleX = 10;
	float m_fScaleY = 10;
	float m_fScaleGX = 10;
	float m_fScaleGY = 10;
	/** global scale for zooming **/
	float m_fScale = 1.0f;
	/** determines which part of the tree-set is shown wrt maximum tree height **/
	float m_fTreeOffset = 0;
	float m_fTreeScale = 1;

	public GridDrawer m_gridDrawer;
	public CladeDrawer m_cladeDrawer;
	
	/** flag to indicate not to draw anything due to being busy initialising **/
	boolean m_bInitializing;

	/** jitter of x-positions for x-coordinate **/
	int m_nJitter = 0;
	/** random nr generator used for applying jitter **/
	Random m_random = new Random();
	/** intensity with which the trees are drawn (multiplier for alpha channel) **/
	float m_fTreeIntensity = 1.0f;
	float m_fCTreeIntensity = 1.0f;
	/** width of lines used for drawing trees, etc. **/
	int m_nTreeWidth = 1;
	int m_nCTreeWidth = 4;
	public int m_nGeoWidth = 1;
	/** width of labels, when root at left **/
	public int m_nLabelWidth = 100;

	/** flags whether a leaf node is selected **/
	boolean[] m_bSelection;
	/** flag to indicate the selection was changed but image was not updated yet **/
	boolean m_bSelectionChanged;
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
	public int m_nBurnIn = 0;

	double m_w = 0;
	/** current directory for opening files **/
	String m_sDir = System.getProperty("user.dir");

	/** array of various colors for color coding different topologies **/
	public Color[] m_color;
	public static int HEIGHTCOLOR = 6,
		CONSCOLOR = 4,
		LABELCOLOR = 5,
		BGCOLOR = 7,
		GEOCOLOR = 8,
		ROOTCANALCOLOR=9;
	/** image used for the background **/
	public BufferedImage m_bgImage;
	/**
	 * bounding box for area containted in bfImage, derived from the image name:
	 * if the name matches (lat0,long0)x(lat1,long1) e.g. NZ(-40,140)x(-10,180)
	 * it will assume the image covers the box 40 South 140 East to 10 South,
	 * 180 East. NB first coordinate should contain lower values, and the second
	 * coordinate the higher values.
	 */
	double[] m_fBGImageBox = { -180, -90, 180, 90 };
	// boolean m_bSurpressMetadata = true;
	/**
	 * name of output file (if any) when batch processing. Typically used to
	 * dump a bitmap file in.
	 **/
	String m_sOutputFile = null;

	/**
	 * Flag to indicate 90% HPD and median should be shown. This only makes
	 * sense when one of the meta data based orderings is used
	 */
	boolean m_bShowBounds = false;
	/**
	 * Extra indentation to labels. This is helpful for unrooted trees since it
	 * shifts all labels with a constant amount. Units are in terms of tree
	 * height.
	 */
	float m_fLabelIndent = 0.0f;

	/**
	 * Flag to indicate image should be recorded Frames that are drawn while
	 * refreshing screen are saved in /tmp/frame<nr>.jpg if possible.
	 */
	boolean m_bRecord = false;
	int m_nFrameNr;

	public boolean m_bViewEditTree = false;
	public boolean m_bViewClades = false;
	public Set<Integer> m_cladeSelection = new HashSet<Integer>();
	
	BufferedImage m_rotate;

	/** regular expression pattern for finding width information in metadata **/
	public Pattern m_pattern;
	/** default regular expression **/
	//final static String DEFAULT_PATTERN = "theta=([0-9\\.Ee-]+)";
	// final static String DEFAULT_PATTERN = "([0-9\\.Ee-]+),([0-9\\.Ee-]+)";
	//final static String DEFAULT_PATTERN = "([0-9\\.Ee-]+),";
	final static String DEFAULT_PATTERN = ".*location=\"([^\"]*).*";
	// final static String DEFAULT_PATTERN = "s=([0-9\\.Ee-]+)";
	// final static String DEFAULT_PATTERN = "([0-9\\.Ee-]+),y=([0-9\\.Ee-]+)";
	/** string containing reg exp for position matching **/
	public String m_sPattern = DEFAULT_PATTERN;
	public int m_iPatternForBottom = 1;
	public int m_iPatternForTop = 0;

	/** string containing reg exp for grouping taxa **/
	String m_sColorPattern = null;
	/** index of color for a taxon **/
	int[] m_iColor;

	/** flag to indicate that single child nodes are allowed **/
	boolean m_bAllowSingleChild = false;

	/** flag to indicate that text should be rotated when root at top **/
	public boolean m_bRotateTextWhenRootAtTop = false;

	/**
	 * mode for determining the X location of an internal node 0 = centre of
	 * nodes below 1 = centre of all taxa below
	 */
	public int m_Xmode = 0;
	/**
	 * flag to indicate the X position needs to be corrected to prevent steep
	 * angles
	 **/
	boolean m_bUseAngleCorrection = false;
	double m_fAngleCorrectionThresHold = 0.9;

	/** method used for ordering nodes **/
	int m_nShuffleMode = NodeOrderer.DEFAULT;
	
	/** used to store name of tree file so that when burn-in changes, the tree set
	 * can be reloaded
	 */
	public String m_sFileName;
	
	/** flag to indicate some meta data on the tree should be used for line widht **/
	//public boolean m_bMetaDataForLineWidth = false;
	/**
	 * Flag to indicate top of branch widths should be calculated from the bottom
	 * of branch lengths by distributing weight proportional to left and right
	 * bottom branches to top branch -- scaled to fit bottom of parent branch.
	 */
	public boolean m_bCorrectTopOfBranch = false;
	/** indicator that only one group is in the pattern, so top of branch widths
	 * should be calculated from the bottom of branch information.
	 */
	boolean m_bGroupOverflow;	
	
	/** constructors **/
	public DensiTree() {
		m_gridDrawer = new GridDrawer(this);
		m_cladeDrawer = new CladeDrawer(this);
		instances++;
	}

	public DensiTree(String[] args) {
		this();
		System.out.println(banner());
		m_bSelection = new boolean[0];
		m_nRevOrder = new int[0];
		m_cTrees = new Node[0];
		m_trees = new Node[0];
		initColors();

		setSize(1000, 800);
		m_Panel = new TreeSetPanel(this);
		parseArgs(args);
		m_pattern = createPattern();
		System.err.println(getSize().width + "x" + getSize().height);

		m_jScrollPane = new JScrollPane(m_Panel);
		makeToolbar();
		makeMenuBar();
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
		for (int i = 0; i < m_iPatternForBottom; i++) {
			sPattern += "[0-9\\.Ee-]+[^0-9]+";
		}
		sPattern += "([0-9\\.Ee-]+)";
		if (m_iPatternForTop > m_iPatternForBottom) {
			//sPattern += "[^0-9]+";
			for (int i = m_iPatternForBottom + 1; i < m_iPatternForTop; i++) {
				sPattern += "[0-9\\.Ee-]+[^0-9]+";
			}
			sPattern += "([0-9\\.Ee-]+)";
		}
		return Pattern.compile(sPattern);
	}

	void initColors() {
		m_color = new Color[10 + 18];
		m_color[0] = Color.getColor("color.1", Color.blue);
		m_color[1] = Color.getColor("color.2", Color.red);
		m_color[2] = Color.getColor("color.3", Color.green);
		m_color[3] = Color.getColor("color.default", new Color(0, 100, 25));
		m_color[CONSCOLOR] = Color.getColor("color.cons", Color.blue);
		m_color[LABELCOLOR] = Color.getColor("color.label", Color.blue);
		m_color[HEIGHTCOLOR] = Color.getColor("color.height", Color.gray);
		m_color[BGCOLOR] = Color.getColor("color.bg", Color.white);
		m_color[GEOCOLOR] = Color.getColor("color.bg", Color.orange);
		m_color[ROOTCANALCOLOR] = Color.getColor("color.rootcanal", Color.blue);

		int k = GEOCOLOR + 1;
		m_color[k++] = Color.blue;
		m_color[k++] = Color.green;
		m_color[k++] = Color.red;
		m_color[k++] = Color.gray;
		m_color[k++] = Color.orange;
		m_color[k++] = Color.yellow;
		m_color[k++] = Color.pink;
		m_color[k++] = Color.black;
		m_color[k++] = Color.cyan;
		m_color[k++] = Color.darkGray;
		m_color[k++] = Color.magenta;
		m_color[k++] = new Color(100, 200, 25);
		;
		m_color[k++] = new Color(100, 0, 25);
		m_color[k++] = new Color(25, 0, 100);
		m_color[k++] = new Color(0, 25, 100);
		m_color[k++] = new Color(0, 100, 25);
		m_color[k++] = new Color(100, 25, 100);
		m_color[k++] = new Color(25, 100, 100);
	} // initColors

	/** parse command line arguments, and load file if specified **/
	void parseArgs(String[] args) {
		int i = 0;
		try {
			while (i < args.length) {
				int iOld = i;
				if (i < args.length - 1) {
					if (args[i].equals("")) {
						i += 1;
					} else if (args[i].equals("-c")) {
						m_fCTreeIntensity = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-i")) {
						m_fTreeIntensity = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-j")) {
						m_nJitter = (int) Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-w")) {
						m_nCTreeWidth = (int) Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-v")) {
						m_nTreeWidth = (int) Float.parseFloat(args[i + 1]);
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
						int nWidth = (int) Integer.parseInt(sStrs[0]);
						int nHeight = (int) Integer.parseInt(sStrs[1]);
						setSize(nWidth, nHeight);
						i += 2;
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
						m_fLabelIndent = Float.parseFloat(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-o")) {
						m_sOutputFile = args[i + 1];
						i += 2;
					} else if (args[i].equals("-kml")) {
						loadKML(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-geowidth")) {
						m_nGeoWidth = Integer.parseInt(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-geocolor")) {
						m_color[GEOCOLOR] = Color.decode(args[i + 1]);
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
						m_sPattern = args[i + 1];
						i += 2;
					} else if (args[i].equals("-colorpattern")) {
						m_sColorPattern = args[i + 1];
						i += 2;
					} else if (args[i].equals("-singlechild")) {
						m_bAllowSingleChild = Boolean.parseBoolean(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-rotatetext")) {
						m_bRotateTextWhenRootAtTop = true;
						i++;
					} else if (args[i].equals("-transform")) {
						m_bUseLogScale = true;
						m_fExponent = Double.parseDouble(args[i+1]);
						i += 2;
					}
					if (i == iOld) {
						throw new Exception("Wrong argument");
					}
				} else {
					init(args[i++]);
					calcLines();
				}
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
				+ "remco@cs.waikato.ac.nz\nremco@cs.auckland.ac.nz\nrrb@xm.co.nz\n" + "(c)2010-2011\n\n\n"
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
		return " 0x" + Integer.toHexString(m_color[iColor].getRGB()).substring(2) + ' ';
	}

	/** get status of internal settings **/
	String getStatus() {
		return "\n\nCurrent status:\n" + m_trees.length + " trees with " + m_cTrees.length + " topologies\n"
				+ "Tree intensity: " + m_fTreeIntensity + "\n" + "Consensus Tree intensity: " + m_fCTreeIntensity
				+ "\n" + "Tree width: " + m_nTreeWidth + "\n" + "Consensus Tree width: " + m_nCTreeWidth + "\n"
				+ "Jitter: " + m_nJitter + "\n" + "Animation delay: " + m_nAnimationDelay + "\n" + "Height: "
				+ m_fHeight + "\n" + "Zoom: " + m_fScale + "\n" + "Number of drawing threads: " + m_Panel.m_nDrawThreads + "\n"
				+ "Burn in: " + m_nBurnIn + "\n\nColor 1:" + formatColor(0) + "\tColor 2:" + formatColor(1)
				+ "\tColor 3:" + formatColor(2) + "\tDefault Color:" + formatColor(3) + "\nConsensus Color:"
				+ formatColor(CONSCOLOR) + "\tLabel color:" + formatColor(LABELCOLOR) + "\tBackground color:"
				+ formatColor(BGCOLOR) + "\tHeight color:" + formatColor(HEIGHTCOLOR);
	}

	/**
	 * read trees from file, and process them into a set of lines This may take
	 * a while... sFile: name of Nexus or Newick tree list file or to read
	 * 
	 * @throws Exception
	 **/
	public void init(String sFile) throws Exception {
		if (m_Panel != null) {
			setWaitCursor();
			//m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		}
		if (m_jStatusBar != null) {
			m_jStatusBar.setText("Initializing...");
			m_jStatusBar.repaint();
		}
		m_sFileName = sFile;
		m_bInitializing = true;
		m_viewMode = ViewMode.DRAW;
		a_animateStart.setIcon("start");
		m_prevLineColorMode = null;
		m_lineColorMode = LineColorMode.DEFAULT;
		m_prevLineWidthMode = null;
		m_lineWidthMode = LineWidthMode.DEFAULT;
		
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
		m_random = new Random();
		m_Panel.m_drawThread = new Thread[m_Panel.m_nDrawThreads];

		m_gridDrawer.m_bReverseGrid = false;
		m_gridDrawer.m_fGridOffset = 0;

		try {
			/** contains strings with tree in Newick format **/
			// Vector<String> sNewickTrees;
			m_sLabels = new Vector<String>();
			m_fLongitude = new Vector<Float>();
			m_fLatitude = new Vector<Float>();
			m_fMinLat = 360;
			m_fMinLong = 360;
			m_fMaxLat = 0;
			m_fMaxLong = 0;
			m_nOrder = null;

			// parseFile(sFile);
			TreeFileParser parser = new TreeFileParser(this);
			m_trees = parser.parseFile(sFile);

			a_loadkml.setEnabled(true);
			// m_nOffset = parser.m_nOffset;
			float fOffset = 3f;
			m_fMaxLong = parser.m_fMaxLong + fOffset;
			m_fMaxLat = parser.m_fMaxLat + fOffset;
			m_fMinLong = parser.m_fMinLong - fOffset;
			m_fMinLat = parser.m_fMinLat - fOffset;
			m_nNrOfLabels = parser.m_nNrOfLabels;

			if (m_trees.length == 0) {
				m_sLabels = null;
				JOptionPane.showMessageDialog(null, "No trees found in file\nMaybe burn in is too large?",
						"Help Message", JOptionPane.PLAIN_MESSAGE);
				return;
			}

			// set up selection
			m_bSelection = new boolean[m_sLabels.size()];
			m_bLabelRectangle = new Rectangle[m_sLabels.size()];
			m_bGeoRectangle = new Rectangle[m_sLabels.size()];
			for (int i = 0; i < m_bSelection.length; i++) {
				m_bSelection[i] = true;
				m_bLabelRectangle[i] = new Rectangle();
				m_bGeoRectangle[i] = new Rectangle();
			}
			m_bSelectionChanged = false;

			// chop off root branch, if any
			double fMinRootLength = Double.MAX_VALUE;
			for (int i = 0; i < m_trees.length; i++) {
				fMinRootLength = Math.min(fMinRootLength, m_trees[i].m_fLength);
			}
			for (int i = 0; i < m_trees.length; i++) {
				m_trees[i].m_fLength -= fMinRootLength;
			}

			// reserve memory for nodes of m_trees
			float[] fHeights = new float[m_trees.length];
			for (int i = 0; i < m_trees.length; i++) {
				fHeights[i] = positionHeight(m_trees[i], 0);
				m_fHeight = Math.max(m_fHeight, fHeights[i]);
			}
			for (int i = 0; i < m_trees.length; i++) {
				offsetHeight(m_trees[i], m_fHeight - fHeights[i]);
			}

			// count tree topologies
			// first step is find how many different topologies are present
			m_nTopology = new int[m_trees.length];
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			for (int i = 0; i < m_trees.length; i++) {
				Node tree = m_trees[i];
				String sNewick = tree.toShortNewick();
				if (map.containsKey(sNewick)) {
					m_nTopology[i] = map.get(sNewick).intValue();
				} else {
					m_nTopology[i] = map.size();
					map.put(sNewick, map.size());
				}
			}

			// second step is find how many different tree have a particular
			// topology
			m_nTopologies = map.size();
			int[] nTopologies = new int[m_nTopologies];
			for (int i = 0; i < m_trees.length; i++) {
				nTopologies[m_nTopology[i]]++;
			}

			// sort the trees so that frequently occurring topologies go first
			// in
			// the ordering
			for (int i = 0; i < m_trees.length; i++) {
				for (int j = i + 1; j < m_trees.length; j++) {
					if (nTopologies[m_nTopology[i]] < nTopologies[m_nTopology[j]]
							|| (nTopologies[m_nTopology[i]] == nTopologies[m_nTopology[j]] && m_nTopology[i] > m_nTopology[j])) {
						int h = m_nTopology[j];
						m_nTopology[j] = m_nTopology[i];
						m_nTopology[i] = h;
						Node tree = m_trees[j];
						m_trees[j] = m_trees[i];
						m_trees[i] = tree;
					}

				}
			}

			// initialise drawing order of x-axis according to most prevalent
			// tree
			Node tree = m_trees[0];
			// over sized, too lazy to figure out exact number of labels
			m_nOrder = new int[m_sLabels.size()];
			m_nRevOrder = new int[m_sLabels.size()];
			initOrder(tree, 0);
			// sanity check
			int nSum = 0;
			for (int i = 0; i < m_nOrder.length; i++) {
				nSum += m_nOrder[i];
			}
			if (nSum != m_nNrOfLabels * (m_nNrOfLabels - 1) / 2) {
				JOptionPane.showMessageDialog(this,
						"The tree set possibly contains non-binary trees. Expect that not all nodes are shown.");
			}

			// reserve memory for nodes of m_cTrees
			// reserveMemory(m_nTopologies * (m_nNrOfLabels*2-1));
			// calculate consensus trees
			int i = 0;
			int iOld = 0;
			int iConsTree = 0;
			m_fTreeWeight = new float[m_nTopologies];
			m_cTrees = new Node[m_nTopologies];
			while (i < m_trees.length) {
				tree = m_trees[i].copy();
				Node consensusTree = tree;
				i++;
				while (i < m_trees.length && m_nTopology[i] == m_nTopology[i - 1]) {
					tree = m_trees[i];
					addLength(tree, consensusTree);
					i++;
				}
				divideLength(consensusTree, i - iOld);
				m_fTreeWeight[iConsTree] = (float) (i - iOld + 0.0) / m_trees.length;
				// position nodes of consensus trees
				// positionLeafs(consensusTree);
				// positionRest(consensusTree);
				float fHeight = positionHeight(consensusTree, 0);
				offsetHeight(consensusTree, m_fHeight - fHeight);
				m_cTrees[iConsTree] = consensusTree;
				iConsTree++;
				iOld = i;
			}
			m_nTopologyByPopularity = new int[m_trees.length];
			int nColor = 0;
			m_nTopologyByPopularity[0] = 0;
			for (i = 1; i < m_trees.length; i++) {
				if (m_nTopology[i] != m_nTopology[i - 1]) {
					nColor++;
				}
				m_nTopologyByPopularity[i] = nColor;
			}

			// calculate lines for drawing trees & consensus trees
			m_fLinesX = new float[m_trees.length][];
			m_fLinesY = new float[m_trees.length][];
			// m_fTLinesX = new float[m_trees.length][];
			// m_fTLinesY = new float[m_trees.length][];
			m_fCLinesX = new float[m_nTopologies][];
			m_fCLinesY = new float[m_nTopologies][];
			// m_fCTLinesX = new float[m_nTopologies][];
			// m_fCTLinesY = new float[m_nTopologies][];
			// calcLines();
			
			m_bCladesReady = false;
//			new Thread() {
//				public void run() {
					calcClades();
					m_bCladesReady = true;
//					reshuffle((m_bAllowSingleChild ? NodeOrderer.DEFAULT: NodeOrderer.OPTIMISE));
//					calcPositions();
//					makeDirty();
//				};
//			}.start();

			reshuffle((m_bAllowSingleChild ? NodeOrderer.DEFAULT: NodeOrderer.OPTIMISE));
			
			// calculate y-position for tree set
			calcPositions();
			
			m_bMetaDataReady = false;			
			Thread thread = new Thread() {
				public void run() {
					String statusMsg = "Parsing metadata";
					for (int k = 0; k < m_trees.length; k++) {
						parseMetaData(m_trees[k]);
						if (k % 100 == 0) {
							statusMsg += ".";
							m_jStatusBar.setText(statusMsg);
							setWaitCursor();
//							if (getCursor().getType() != Cursor.WAIT_CURSOR) {
//								setCursor(new Cursor(Cursor.WAIT_CURSOR));
//							}
						}
					}
					m_jStatusBar.setText("Parsing metadata");
					m_metaDataTags = new ArrayList<String>();
					m_metaDataTypes = new ArrayList<MetaDataType>();
					collectMetaDataTags(m_trees[0]);
					calcPositions();
					calcLines();
					makeDirty();
					m_bMetaDataReady = true;			
					for (ChangeListener listener : m_changeListeners) {
						listener.stateChanged(null);
					}
					m_jStatusBar.setText("Done parsing metadata");
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
			thread.start();
			
			m_metaDataTags = new ArrayList<String>();
			m_metaDataTypes = new ArrayList<MetaDataType>();
			collectMetaDataTags(m_trees[0]);

			for (ChangeListener listener : m_changeListeners) {
				listener.stateChanged(null);
			}
		} catch (OutOfMemoryError e) {
			clear();
			JOptionPane.showMessageDialog(null, "Not enough memory is reserved for java to process this tree. "
					+ "Try starting DensiTree with more memory\n\n(for example "
					+ "use:\njava -Xmx3g DensiTree.jar\nfrom " + "the command line) where DensiTree is in the path");
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
		if (m_sColorPattern != null) {
			calcColorPattern();
		}

		addAction(new DoAction());
		Frame[] frames = Frame.getFrames();
		if (frames.length > 0) {
			frames[0].setTitle(FRAME_TITLE + " " + sFile);
		}
		System.err.println("Done");
	} // init

	
	private void collectMetaDataTags(Node node) {
		Map<String, Object> metaDataMap = node.getMetaDataSet();
		if (metaDataMap != null) {
			for (String key : metaDataMap.keySet()) {
				if (!m_metaDataTags.contains(key)) {
					m_metaDataTags.add(key);
					Object o = metaDataMap.get(key);
					if (o instanceof Double) {
						m_metaDataTypes.add(MetaDataType.NUMERIC);
					} else {
						String s = o.toString();
						if (s.length() > 0 && s.charAt(0)=='{') {
							m_metaDataTypes.add(MetaDataType.SET);
						} else {
							m_metaDataTypes.add(MetaDataType.STRING);
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


	boolean m_bCladesReady;
	public boolean m_bMetaDataReady;
	/** represent clade as arrays of leaf indices **/
	List<int[]> m_clades;
	/** proportion of trees containing the clade **/
	List<Double> m_cladeWeight;
	/** average height of a clade **/
	List<Double> m_cladeHeight;
	List<Double> m_cladeHeight95HPDup;
	List<Double> m_cladeHeight95HPDdown;
	List<List<Double>> m_cladeHeightSet;
	/** UI component for manipulating clade selection **/
	JList m_cladelist;
	DefaultListModel m_cladelistmodel = new DefaultListModel();

	List<List<ChildClade>> m_cladeChildren;
	/** X-position of the clade **/
	float[] m_cladePosition;
	/** index of consensus tree with highest product of clade probabilities **/
	public //int m_iMaxCladeProbTopology;
	boolean m_bShowRootCanalTopology = false;
	
	/** Each clade has a list of pairs of child clades **/
	class ChildClade {
		int m_iLeft;
		int m_iRight;
		double m_fWeight;

		public String toString() {
			return "(" + m_iLeft + "," + m_iRight + ")" + m_fWeight + " ";
		}
	}

	private void calcClades() {
		if (m_bAllowSingleChild) {
			return;
		}
		m_clades = new ArrayList<int[]>();
		m_cladeWeight = new ArrayList<Double>();
		m_cladeHeight = new ArrayList<Double>();
		
		m_cladeHeight95HPDup = new ArrayList<Double>();
		m_cladeHeight95HPDdown = new ArrayList<Double>();
		
		m_cladeHeightSet = new ArrayList<List<Double>>();
		m_cladeChildren = new ArrayList<List<ChildClade>>();
		Map<String, Integer> mapCladeToIndex = new HashMap<String, Integer>();

		// add leafs as clades
		for (int i = 0; i < m_nNrOfLabels; i++) {
			int[] clade = new int[1];
			clade[0] = i;
			m_clades.add(clade);
			m_cladeWeight.add(1.0);
			m_cladeHeight.add(0.0);
			m_cladeHeight95HPDup.add(0.0);
			m_cladeHeight95HPDdown.add(0.0);
			m_cladeHeightSet.add(new ArrayList<Double>());
			m_cladeChildren.add(new ArrayList<ChildClade>());
			mapCladeToIndex.put(Arrays.toString(clade), mapCladeToIndex.size());
		}

		// collect clades
		for (int i = 0; i < m_cTrees.length; i++) {
			calcCladeForNode(m_cTrees[i], mapCladeToIndex, m_fTreeWeight[i], m_cTrees[i].m_fPosY);
		}
		for (int i = 0; i < m_trees.length; i++) {
			calcCladeForNode2(m_trees[i], mapCladeToIndex, 1.0 / m_trees.length, m_trees[i].m_fPosY);
		}

		// normalise clade heights, so m_cladeHeight represent average clade
		// height
		for (int i = 0; i < m_cladeHeight.size(); i++) {
			m_cladeHeight.set(i, m_cladeHeight.get(i) / m_cladeWeight.get(i));
		}

		for (int i = 0; i < m_cladeHeight.size(); i++) {
			List<Double> heights = m_cladeHeightSet.get(i);
			Collections.sort(heights);
			int upIndex = heights.size() * 190 / 200;
			int downIndex = heights.size() * 5 / 200;
			m_cladeHeight95HPDup.set(i, heights.get(upIndex));
			m_cladeHeight95HPDdown.set(i, heights.get(downIndex));
		}
		// save memory
		m_cladeHeightSet = null;
		
		double fHeight0 = m_fHeight;
		for (int i = 0; i < m_cladeHeight.size(); i++) {
			fHeight0 = Math.min(fHeight0, m_cladeHeight.get(i));
		}
		// for (int i = 0; i < m_cladeHeight.size(); i++) {
		// m_cladeHeight.set(i, m_cladeHeight.get(i) + fHeight0);
		// }

		m_cladePosition = new float[m_clades.size()];
		// sort clades by weight
		Integer [] index = new Integer[m_cladePosition.length];
		for (int i = 0; i < m_cladePosition.length; i++) {
			index[i] = i;
		}
		
		Arrays.sort(index, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				if (Math.abs(m_cladeWeight.get(o1) - m_cladeWeight.get(o2)) < 1e-4) {
					return (int) Math.signum(m_clades.get(o1).length- m_clades.get(o2).length);
				}
				return -Double.compare(m_cladeWeight.get(o1), m_cladeWeight.get(o2));
			}
		});
		
		List<int[]> clades = new ArrayList<int[]>();
		List<Double> cladeWeight = new ArrayList<Double>();
		List<Double> cladeHeight = new ArrayList<Double>();
		List<Double> cladeHeight95HPDup = new ArrayList<Double>();
		List<Double> cladeHeight95HPDdown = new ArrayList<Double>();
		List<List<ChildClade>> cladeChildren = new ArrayList<List<ChildClade>>();
		for (int i = 0; i < m_cladePosition.length; i++) {
			clades.add(m_clades.get(index[i]));
			cladeWeight.add(m_cladeWeight.get(index[i]));
			cladeHeight.add(m_cladeHeight.get(index[i]));
			cladeHeight95HPDdown.add(m_cladeHeight95HPDdown.get(index[i]));
			cladeHeight95HPDup.add(m_cladeHeight95HPDup.get(index[i]));
			cladeChildren.add(m_cladeChildren.get(index[i]));
		}
		m_clades = clades;
		m_cladeWeight = cladeWeight;
		m_cladeHeight = cladeHeight;
		m_cladeHeight95HPDdown = cladeHeight95HPDdown;
		m_cladeHeight95HPDup = cladeHeight95HPDup;
		m_cladeChildren = cladeChildren;


		Integer [] reverseindex = new Integer[m_cladePosition.length];
		for (int i = 0; i < m_cladePosition.length; i++) {
			reverseindex[index[i]] = i;
		}
		for (int i = 0; i < m_cladePosition.length; i++) {
			List<ChildClade> list = m_cladeChildren.get(i);
			for (ChildClade childClade : list) {
				childClade.m_iLeft = reverseindex[childClade.m_iLeft];
				childClade.m_iRight = reverseindex[childClade.m_iRight];
			}
		}		

		// reassign clade nr (after sorting) in consensus trees
		for (int i = 0; i < m_cTrees.length; i++) {
			resetCladeNr(m_cTrees[i], reverseindex);
		}

		// set clade nr for all trees, from clade nr in topology
		for (int i = 0; i < m_trees.length; i++) {
			setCladeNr(m_trees[i], m_cTrees[m_nTopologyByPopularity[i]]);
		}

		// find tree topology with highest product of clade support of all its clades
		int iMaxCladeProbTopology = 0;
		int iMaxMinCladeProbTopology = 0;
		double fMaxCladeProb = cladeProb(m_cTrees[0], true);
		double fMaxMinCladeProb = cladeProb(m_cTrees[0], false);
		for (int i = 1; i < m_cTrees.length; i++) {
			double fCladeProb = cladeProb(m_cTrees[i], true);
			if (fCladeProb > fMaxCladeProb) {
				iMaxCladeProbTopology = i;
				fMaxCladeProb = fCladeProb;
			}
			double fMinCladeProb = cladeProb(m_cTrees[i], false);
			if (fMinCladeProb > fMaxMinCladeProb) {
				iMaxMinCladeProbTopology = i;
				fMaxMinCladeProb = fMinCladeProb;
			}
		}
		
		m_summaryTree = new Node[4];
		m_summaryTree[0] = m_cTrees[iMaxCladeProbTopology].copy();
		cleanUpSummaryTree(m_summaryTree[0]);

		m_summaryTree[1] = m_cTrees[iMaxMinCladeProbTopology].copy();
		cleanUpSummaryTree(m_summaryTree[1]);

		// construct max. clade weight tree
		List<Node> nodes = new ArrayList<Node>();
		List<int[]> cladeIDs = new ArrayList<int[]>();
		for (int i = 0; i < m_sLabels.size(); i++) {
			int [] cladeID = new int[1];
			cladeID[0] = i;
			cladeIDs.add(cladeID);
			Node node = new Node();
			node.m_iLabel = i;
			node.m_iClade = i;
			nodes.add(node);
		}
		m_summaryTree[2] = constructMaxCladeTree(cladeIDs, mapCladeToIndex, nodes);
		m_summaryTree[2].sort();
		resetCladeNr(m_summaryTree[2], reverseindex);
		m_summaryTree[3] = m_summaryTree[2].copy();
		cleanUpSummaryTree(m_summaryTree[2]);
		
		//cleanUpSummaryTree(m_summaryTree[3]);
		setHeightByClade(m_summaryTree[3]);
		removeNegBranches(m_summaryTree[3]);
		m_summaryTree[3].m_fLength = (float) (m_fHeight - m_cladeHeight.get(m_summaryTree[3].m_iClade));
		float fHeight = positionHeight(m_summaryTree[3], 0);
		offsetHeight(m_summaryTree[3], m_fHeight - fHeight);
		
		m_rootcanaltree = m_summaryTree[0];
		
		// add clades to GUI component
		m_cladelistmodel.clear();
		List<String> list = cladesToString();
		for (int i = 0; i < list.size(); i++) {
			m_cladelistmodel.add(i, list.get(i));
		}
	}

	private void removeNegBranches(Node node) {
		if (!node.isLeaf()) {
			removeNegBranches(node.m_left);
			removeNegBranches(node.m_right);
			if (node.m_left.m_fLength < 0) {
				node.m_right.m_fLength += -node.m_left.m_fLength;
				node.m_fLength -= -node.m_left.m_fLength;
				node.m_left.m_fLength = 0;
			}
			if (node.m_right.m_fLength < 0) {
				node.m_left.m_fLength += -node.m_right.m_fLength;
				node.m_fLength -= -node.m_right.m_fLength;
				node.m_right.m_fLength = 0;
			}
		}		
	}

	private void cleanUpSummaryTree(Node summaryTree) {
		setHeightByClade(summaryTree);
		summaryTree.m_fLength = (float) (m_fHeight - m_cladeHeight.get(summaryTree.m_iClade));
		float fHeight = positionHeight(summaryTree, 0);
		offsetHeight(summaryTree, m_fHeight - fHeight);
	}

	private Node constructMaxCladeTree(List<int[]> cladeIDs, Map<String, Integer> mapCladeToIndex, List<Node> nodes) {
		int k = nodes.size();
		while (cladeIDs.size() > 1) {
			double maxWeight = -1;
			int maxLeft = -1;
			int maxRight = -1;
			for (int i = 0; i < cladeIDs.size(); i++) {
				int [] cladeLeft = cladeIDs.get(i);
				for (int j = i+1; j < cladeIDs.size(); j++) {
					int [] cladeRight = cladeIDs.get(j);
					int [] clade = mergeClades(cladeLeft, cladeRight);
					String sClade = Arrays.toString(clade);
					if (mapCladeToIndex.containsKey(sClade)) {
						int iClade = mapCladeToIndex.get(sClade);
						double weight = m_cladeWeight.get(iClade);
						if (weight > maxWeight) {
							maxWeight = weight;
							maxLeft = i;
							maxRight = j;
						}
					}
				}
			}
			// update clades
			int [] cladeLeft = cladeIDs.get(maxLeft);
			int [] cladeRight = cladeIDs.get(maxRight);
			int [] clade = mergeClades(cladeLeft, cladeRight);
			cladeIDs.remove(maxRight);
			cladeIDs.remove(maxLeft);
			cladeIDs.add(clade);
			// create new Node
			Node node = new Node();
			node.m_iLabel = k++;
			node.m_iClade = mapCladeToIndex.get(Arrays.toString(clade));
			node.m_left = nodes.get(maxLeft);
			nodes.get(maxLeft).m_Parent = node;
			node.m_right = nodes.get(maxRight);
			nodes.get(maxRight).m_Parent = node;
			nodes.remove(maxRight);
			nodes.remove(maxLeft);
			nodes.add(node);
		}		
		return nodes.get(0);
	}

	// merge clades, keep in sorted order
	private int[] mergeClades(int[] cladeLeft, int[] cladeRight) {
		int [] clade = new int[cladeLeft.length + cladeRight.length];
		int iLeft = 0;
		int iRight = 0;
		for (int i = 0; i < clade.length; i++) {
			if (iLeft == cladeLeft.length) {
				clade[i] = cladeRight[iRight++];
			} else if (iRight == cladeRight.length) {
				clade[i] = cladeLeft[iLeft++];
			} else if (cladeRight[iRight] > cladeLeft[iLeft]) {
				clade[i] = cladeLeft[iLeft++];
			} else {
				clade[i] = cladeRight[iRight++];
			}
		}
		return clade;
	}



	
	private void setHeightByClade(Node node) {
		if (!node.isRoot()) {
			//node.m_fLength = (float)Math.abs(m_cladeHeight.get(node.getParent().m_iClade) - m_cladeHeight.get(node.m_iClade));
			node.m_fLength = (float)(m_cladeHeight.get(node.m_iClade) - m_cladeHeight.get(node.getParent().m_iClade));
		}
		if (!node.isLeaf()) {
			setHeightByClade(node.m_left);
			setHeightByClade(node.m_right);
		}
	}

	boolean m_bAllowCladeSelection = true;

	void resetCladeSelection() {
		m_bAllowCladeSelection = false;
		m_cladelist.clearSelection();
		for (int i : m_cladeSelection) {
			m_cladelist.addSelectionInterval(i, i);
			if (m_cladeSelection.size() == 1) {
				m_cladelist.ensureIndexIsVisible(i);
			}
		}
		m_bAllowCladeSelection = true;
	}

	
	private void resetCladeNr(Node node, Integer[] reverseindex) {
		node.m_iClade = reverseindex[node.m_iClade];
		if (!node.isLeaf()) {
			resetCladeNr(node.m_left, reverseindex);
			resetCladeNr(node.m_right, reverseindex);
		}
	}

	List<String> cladesToString() {
		List<String> list = new ArrayList<String>();
		DecimalFormat format = new  DecimalFormat("###.##");
		
		for (int i = 0; i < m_cladePosition.length; i++) {
			String sStr = "";
			//if (m_clades.get(i).length > 1) {
				sStr += format.format(m_cladeWeight.get(i) * 100) + "% ";
				sStr += format.format(m_cladeHeight95HPDdown.get(i)) + " ";
				sStr += format.format(m_cladeHeight95HPDup.get(i)) + " ";
				sStr += "[";
				int j = 0;
				for (j = 0; j < m_clades.get(i).length - 1; j++) {
					sStr += (m_sLabels.get(m_clades.get(i)[j]) + ",");
				}
				sStr += (m_sLabels.get(m_clades.get(i)[j]) + "]\n");
				list.add(sStr);
			//}
		}
		return list;
	}
	
	private double cladeProb(Node node, final boolean useProduct) {
		if (node.isLeaf()) {
			return 1.0;
		} else {
			double fCladeProb = m_cladeWeight.get(node.m_iClade);
			if (useProduct) {
				fCladeProb *= cladeProb(node.m_left, useProduct);
				fCladeProb *= cladeProb(node.m_right, useProduct);
			} else {
				fCladeProb = Math.min(fCladeProb, cladeProb(node.m_left, useProduct));
				fCladeProb = Math.min(fCladeProb, cladeProb(node.m_right, useProduct));
			}
			return fCladeProb;
		}
	}

	private void setCladeNr(Node node, Node node2) {
		if (node2 == null) {
			throw new RuntimeException("node2 cannot be null");
		}
		if (!node.isLeaf()) {
			node.m_iClade = node2.m_iClade;
			setCladeNr(node.m_left, node2.m_left);
			setCladeNr(node.m_right, node2.m_right);
		}

	}

	private int[] calcCladeForNode(Node node, Map<String, Integer> mapCladeToIndex, double fWeight, double fHeight) {
		if (node.isLeaf()) {
			int[] clade = new int[1];
			clade[0] = node.getNr();
			node.m_iClade = node.getNr();
			m_cladeHeight.set(node.m_iClade, m_cladeHeight.get(node.m_iClade) + fWeight * fHeight);
			//m_cladeHeightSet.get(node.m_iClade).add(fHeight);
			return clade;
		} else {
			int[] cladeLeft = calcCladeForNode(node.m_left, mapCladeToIndex, fWeight, fHeight + node.m_left.m_fLength);
			int[] cladeRight = calcCladeForNode(node.m_right, mapCladeToIndex, fWeight, fHeight
					+ node.m_right.m_fLength);
			int[] clade = mergeClades(cladeLeft, cladeRight);
			// merge clades, keep in sorted order
//			int[] clade = new int[cladeLeft.length + cladeRight.length];
//			int iLeft = 0;
//			int iRight = 0;
//			for (int i = 0; i < clade.length; i++) {
//				if (iLeft == cladeLeft.length) {
//					clade[i] = cladeRight[iRight++];
//				} else if (iRight == cladeRight.length) {
//					clade[i] = cladeLeft[iLeft++];
//				} else if (cladeRight[iRight] > cladeLeft[iLeft]) {
//					clade[i] = cladeLeft[iLeft++];
//				} else {
//					clade[i] = cladeRight[iRight++];
//				}
//			}

			// update clade weights
			String sClade = Arrays.toString(clade);
			if (!mapCladeToIndex.containsKey(sClade)) {
				mapCladeToIndex.put(sClade, mapCladeToIndex.size());
				m_clades.add(clade);
				m_cladeWeight.add(0.0);
				m_cladeHeight.add(0.0);
				m_cladeHeight95HPDup.add(0.0);
				m_cladeHeight95HPDdown.add(0.0);
				m_cladeHeightSet.add(new ArrayList<Double>());
				m_cladeChildren.add(new ArrayList<ChildClade>());
			}
			int iClade = mapCladeToIndex.get(sClade);
			m_cladeWeight.set(iClade, m_cladeWeight.get(iClade) + fWeight);
			m_cladeHeight.set(iClade, m_cladeHeight.get(iClade) + fWeight * fHeight);
			//m_cladeHeightSet.get(iClade).add(fHeight);
			node.m_iClade = iClade;

			// update child clades
			int iCladeLeft = Math.min(node.m_left.m_iClade, node.m_right.m_iClade);
			int iCladeRight = Math.max(node.m_left.m_iClade, node.m_right.m_iClade);
			List<ChildClade> children = m_cladeChildren.get(iClade);
			boolean bFound = false;
			for (ChildClade child : children) {
				if (child.m_iLeft == iCladeLeft && child.m_iRight == iCladeRight) {
					child.m_fWeight += fWeight;
					bFound = true;
					break;
				}
			}
			if (!bFound) {
				ChildClade child = new ChildClade();
				child.m_iLeft = iCladeLeft;
				child.m_iRight = iCladeRight;
				child.m_fWeight = fWeight;
				m_cladeChildren.get(iClade).add(child);
			}

			return clade;
		}

	}

	private int[] calcCladeForNode2(Node node, Map<String, Integer> mapCladeToIndex, double fWeight, double fHeight) {
		if (node.isLeaf()) {
			int[] clade = new int[1];
			clade[0] = node.getNr();
			node.m_iClade = node.getNr();
			m_cladeHeightSet.get(node.m_iClade).add(fHeight);
			return clade;
		} else {
			int[] cladeLeft = calcCladeForNode2(node.m_left, mapCladeToIndex, fWeight, fHeight + node.m_left.m_fLength);
			int[] cladeRight = calcCladeForNode2(node.m_right, mapCladeToIndex, fWeight, fHeight
					+ node.m_right.m_fLength);
			// merge clades, keep in sorted order
			int[] clade = new int[cladeLeft.length + cladeRight.length];
			int iLeft = 0;
			int iRight = 0;
			for (int i = 0; i < clade.length; i++) {
				if (iLeft == cladeLeft.length) {
					clade[i] = cladeRight[iRight++];
				} else if (iRight == cladeRight.length) {
					clade[i] = cladeLeft[iLeft++];
				} else if (cladeRight[iRight] > cladeLeft[iLeft]) {
					clade[i] = cladeLeft[iLeft++];
				} else {
					clade[i] = cladeRight[iRight++];
				}
			}

			// update clade weights
			String sClade = Arrays.toString(clade);
//			if (!mapCladeToIndex.containsKey(sClade)) {
//				mapCladeToIndex.put(sClade, mapCladeToIndex.size());
//				m_cladeHeight95HPDup.add(0.0);
//				m_cladeHeight95HPDdown.add(0.0);
//				m_cladeHeightSet.add(new ArrayList<Double>());
//			}
			int iClade = mapCladeToIndex.get(sClade);
			m_cladeHeightSet.get(iClade).add(fHeight);
			node.m_iClade = iClade;

			// update child clades
			int iCladeLeft = Math.min(node.m_left.m_iClade, node.m_right.m_iClade);
			int iCladeRight = Math.max(node.m_left.m_iClade, node.m_right.m_iClade);
			List<ChildClade> children = m_cladeChildren.get(iClade);
			boolean bFound = false;
			for (ChildClade child : children) {
				if (child.m_iLeft == iCladeLeft && child.m_iRight == iCladeRight) {
					child.m_fWeight += fWeight;
					bFound = true;
					break;
				}
			}
			if (!bFound) {
				ChildClade child = new ChildClade();
				child.m_iLeft = iCladeLeft;
				child.m_iRight = iCladeRight;
				child.m_fWeight = fWeight;
				m_cladeChildren.get(iClade).add(child);
			}

			return clade;
		}

	}
	
	void calcColorPattern() {
		m_iColor = new int[m_sLabels.size()];
		Pattern pattern = Pattern.compile(".*" + m_sColorPattern + ".*");
		List<String> sPatterns = new ArrayList<String>();
		for (int i = 0; i < m_sLabels.size(); i++) {
			String sLabel = m_sLabels.get(i);
			Matcher matcher = pattern.matcher(sLabel);
			if (matcher.find()) {
				String sMatch = matcher.group(1);
				if (sPatterns.indexOf(sMatch) < 0) {
					sPatterns.add(sMatch);
				}
				m_iColor[i] = sPatterns.indexOf(sMatch);
			}
		}
	}

	void loadKML(String sFileName) {
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

			HashMap<String, Vector<Double>> mapLabel2X = new HashMap<String, Vector<Double>>();
			HashMap<String, Vector<Double>> mapLabel2Y = new HashMap<String, Vector<Double>>();

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
							sPlacemarkName = oChild.getTextContent();
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
				}
			}

			// grab Taxa From Objects
			m_fMinLat = 90;
			m_fMinLong = 180;
			m_fMaxLat = -90;
			m_fMaxLong = -180;
			for (int iLabel = 0; iLabel < m_nNrOfLabels; iLabel++) {
				String sTaxon = m_sLabels.get(iLabel).toLowerCase();
				if (mapLabel2X.containsKey(sTaxon)) {
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
					while (m_fLatitude.size() <= iLabel) {
						m_fLatitude.add(0f);
						m_fLongitude.add(0f);
					}
					m_fLatitude.set(iLabel, (float) fY);
					m_fLongitude.set(iLabel, (float) fX);
					m_fMinLat = Math.min(m_fMinLat, (float) fY);
					m_fMaxLat = Math.max(m_fMaxLat, (float) fY);
					m_fMinLong = Math.min(m_fMinLong, (float) fX);
					m_fMaxLong = Math.max(m_fMaxLong, (float) fX);
				} else {
					System.err.println("No geo info for " + sTaxon
							+ " found (probably because taxon is missing or spelling error)");
					while (m_fLatitude.size() <= iLabel) {
						m_fLatitude.add(0f);
						m_fLongitude.add(0f);
					}
					m_fLatitude.set(iLabel, 0f);
					m_fLongitude.set(iLabel, 0f);
				}
			}
			float fOffset = 3f;
			m_fMaxLong = m_fMaxLong + fOffset;
			m_fMaxLat = m_fMaxLat + fOffset;
			m_fMinLong = m_fMinLong - fOffset;
			m_fMinLat = m_fMinLat - fOffset;
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // loadKMLFile

	/* remove all data from memory */
	void clear() {
		m_trees = new Node[0];
		m_cTrees = new Node[0];
		m_fLinesX = null;
		m_fLinesY = null;
		// m_fTLinesX = null;
		// m_fTLinesY = null;
		m_fCLinesX = null;
		m_fCLinesY = null;
		// m_fCTLinesX = null;
		// m_fCTLinesY = null;
		m_bInitializing = false;
	} // clear

	/**
	 * try to reorder the leaf nodes so that the tree layout allows
	 * investigation of some of the tree set features
	 */
	void reshuffle(int nMethod) {
		m_nShuffleMode = nMethod;
		setWaitCursor();
		//m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		try {
			switch (nMethod) {
			case NodeOrderer.DEFAULT:
				// use order of most frequently occurring tree
				initOrder(m_trees[0], 0);
				break;
			case NodeOrderer.MANUAL: {
				// use order given by user
				for (int i = 0; i < m_sLabels.size(); i++) {
					System.out.print(m_sLabels.elementAt(i) + " ");
				}
				String sOrder = JOptionPane.showInputDialog("New node order:", "");
				if (sOrder == null) {
					return;
				}
				String[] sIndex = sOrder.split(" ");
				if (sIndex.length != m_nNrOfLabels) {
					System.err.println("Number of labels/taxa differs from given labels");
					return;
				}
				int[] nOrder = new int[m_nOrder.length];
				int[] nRevOrder = new int[m_nRevOrder.length];
				for (int i = 0; i < sIndex.length; i++) {
					int j = 0;
					String sTarget = sIndex[i];
					while ((j < m_sLabels.size()) && !(m_sLabels.elementAt(j).equals(sTarget))) {
						j++;
					}
					if (j == m_sLabels.size()) {
						System.err.println("Label \"" + sTarget + "\" not found among labels");
						return;
					}
					nOrder[j] = i;
					nRevOrder[i] = j;
				}
				m_nOrder = nOrder;
				m_nRevOrder = nRevOrder;
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
				int[] nOrder = h.calcOrder(m_nNrOfLabels, m_trees, m_cTrees, m_rootcanaltree, m_fTreeWeight/*
																						 * ,
																						 * m_nOrder
																						 */, m_clades, m_cladeWeight);
				m_nOrder = nOrder;
				for (int i = 0; i < m_nNrOfLabels; i++) {
					m_nRevOrder[m_nOrder[i]] = i;
				}
				System.err.println();
				for (int i = 0; i < m_nNrOfLabels; i++) {
					System.out.print(m_sLabels.elementAt(m_nRevOrder[i]) + " ");
				}
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		int nNodes = getNrOfNodes(m_trees[0]);
		if (nMethod < NodeOrderer.META_ALL) {
			m_bShowBounds = false;
			calcPositions();
			calcLines();
			makeDirty();
			addAction(new DoAction());
		} else {
			m_bShowBounds = true;
			m_pattern = Pattern.compile(m_sPattern);
			switch (nMethod) {
			case NodeOrderer.META_ALL: {
				double fMaxX = 0;
				for (int i = 0; i < m_trees.length; i++) {
					double fX = positionMetaAll(m_trees[i]);
					fMaxX = Math.max(fMaxX, fX);
				}
				for (int i = 0; i < m_cTrees.length; i++) {
					double fX = positionMetaAll(m_cTrees[i]);
					fMaxX = Math.max(fMaxX, fX);
				}
				fMaxX = m_nNrOfLabels / fMaxX;
				for (int i = 0; i < m_trees.length; i++) {
					scaleX(m_trees[i], fMaxX);
				}
				for (int i = 0; i < m_cTrees.length; i++) {
					scaleX(m_cTrees[i], fMaxX);
				}
				calcLines();
			}
				break;
			case NodeOrderer.META_SUM:
			case NodeOrderer.META_AVERAGE: {
				for (int i = 0; i < m_trees.length; i++) {
					float[] fHeights = new float[m_nNrOfLabels * 2 - 1];
					float[] fMetas = new float[m_nNrOfLabels * 2 - 1];
					int[] nCounts = new int[m_nNrOfLabels * 2 - 1];
					collectHeights(m_trees[i], fHeights, 0);
					Arrays.sort(fHeights);
					collectMetaData(m_trees[i], fHeights, 0.0f, 0, fMetas, nCounts);
					m_fLinesX[i] = new float[nNodes * 2 + 2];
					m_fLinesY[i] = new float[nNodes * 2 + 2];
					for (int j = 0; j < fMetas.length - 1; j++) {
						m_fLinesX[i][j * 2] = fMetas[j];
						m_fLinesY[i][j * 2] = (fHeights[j] - m_fTreeOffset) * m_fTreeScale;
						m_fLinesX[i][j * 2 + 1] = fMetas[j + 1];
						m_fLinesY[i][j * 2 + 1] = (fHeights[j + 1] - m_fTreeOffset) * m_fTreeScale;
					}
					if (nMethod == NodeOrderer.META_AVERAGE) {
						for (int j = 0; j < fMetas.length - 1; j++) {
							if (nCounts[j] > 0) {
								m_fLinesX[i][j * 2] = fMetas[j] / nCounts[j];
							}
							if (nCounts[j + 1] > 0) {
								m_fLinesX[i][j * 2 + 1] = fMetas[j + 1] / nCounts[j + 1];
							}
						}
					}
				}
				for (int i = 0; i < m_cTrees.length; i++) {
					float[] fHeights = new float[m_nNrOfLabels * 2 - 1];
					float[] fMetas = new float[m_nNrOfLabels * 2 - 1];
					int[] nCounts = new int[m_nNrOfLabels * 2 - 1];
					collectHeights(m_cTrees[i], fHeights, 0);
					Arrays.sort(fHeights);
					collectMetaData(m_cTrees[i], fHeights, 0.0f, 0, fMetas, nCounts);
					m_fCLinesX[i] = new float[nNodes * 2 + 2];
					m_fCLinesY[i] = new float[nNodes * 2 + 2];
					for (int j = 0; j < fMetas.length - 1; j++) {
						m_fCLinesX[i][j * 2] = fMetas[j];
						m_fCLinesY[i][j * 2] = (fHeights[j] - m_fTreeOffset) * m_fTreeScale;
						m_fCLinesX[i][j * 2 + 1] = fMetas[j + 1];
						m_fCLinesY[i][j * 2 + 1] = (fHeights[j + 1] - m_fTreeOffset) * m_fTreeScale;
					}
					if (nMethod == NodeOrderer.META_AVERAGE) {
						for (int j = 0; j < fMetas.length - 1; j++) {
							if (nCounts[j] > 0) {
								m_fLinesX[i][j * 2] = fMetas[j] / nCounts[j];
							}
							if (nCounts[j + 1] > 0) {
								m_fLinesX[i][j * 2 + 1] = fMetas[j + 1] / nCounts[j + 1];
							}
						}
					}
				}
				// determine scale
				float fMaxX = 0;
				for (float[] fXs : m_fLinesX) {
					for (float f : fXs) {
						fMaxX = Math.max(f, fMaxX);
					}
				}
				for (float[] fXs : m_fCLinesX) {
					for (float f : fXs) {
						fMaxX = Math.max(f, fMaxX);
					}
				}
				float fScale = m_nNrOfLabels / fMaxX;
				for (float[] fXs : m_fCLinesX) {
					for (int i = 0; i < fXs.length; i++) {
						fXs[i] *= fScale;
					}
				}
				for (float[] fXs : m_fLinesX) {
					for (int i = 0; i < fXs.length; i++) {
						fXs[i] *= fScale;
					}
				}
			}
				break;
			}
			makeDirty();
			// addAction(new DoAction());
		}
	} // reshuffle

	/**
	 * Reorder leafs by rotating around internal node associated with
	 * iRotationPoint
	 */
	void rotateAround(int iRotationPoint) {

		Vector<Integer> iLeafs = new Vector<Integer>();
		getRotationLeafs(m_cTrees[0], -1, iLeafs, iRotationPoint);

		System.err.println("Rotating " + iRotationPoint + " " + iLeafs);
		// find rotation range
		int iMin = m_nOrder.length;
		int iMax = 0;
		for (Integer i : iLeafs) {
			int j = m_nOrder[i];
			iMin = Math.min(j, iMin);
			iMax = Math.max(j, iMax);
		}
		for (int i = 0; i < (iMax - iMin) / 2 + 1; i++) {
			int nTmp = m_nRevOrder[iMin + i];
			m_nRevOrder[iMin + i] = m_nRevOrder[iMax - i];
			m_nRevOrder[iMax - i] = nTmp;
		}

		for (int i = 0; i < m_sLabels.size(); i++) {
			m_nOrder[m_nRevOrder[i]] = i;
		}

		calcPositions();
		calcLines();
		makeDirty();
		addAction(new DoAction());
	} // rotateAround

	void moveRotationPoint(int iRotationPoint, float fdH) {
		Vector<Integer> iLeafs = new Vector<Integer>();
		getRotationLeafs(m_cTrees[0], -1, iLeafs, iRotationPoint);
		boolean[] bSelection = m_bSelection;
		m_bSelection = new boolean[m_sLabels.size()];
		for (int i : iLeafs) {
			m_bSelection[i] = true;
		}

		for (int i = 0; i < m_trees.length; i++) {
			moveInternalNode(fdH, m_trees[i], iLeafs.size());
		}
		for (int i = 0; i < m_cTrees.length; i++) {
			moveInternalNode(fdH, m_cTrees[i], iLeafs.size());
		}
		m_bSelection = bSelection;
		calcLines();
		makeDirty();
	}

	int moveInternalNode(float fdH, Node node, int nSelected) {
		if (node.isLeaf()) {
			return (m_bSelection[node.getNr()] ? 1 : 0);
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

	/**
	 * calculate coordinates for lines in real coordinates This initialises the
	 * m_nLines,m_nTLines, m_nCLines and m_nCTLines arrays
	 **/
	public void calcLines() {
		checkSelection();
		if (m_trees.length == 0) {
			return;
		}
		setWaitCursor();
		
		// calculate coordinates of lines for drawing trees
		int nNodes = getNrOfNodes(m_trees[0]);

		boolean[] b = new boolean[1];
		for (int i = 0; i < m_trees.length; i++) {
			// m_fLinesX[i] = new float[nNodes * 2 + 2];
			// m_fLinesY[i] = new float[nNodes * 2 + 2];
			if (m_bAllowSingleChild) {
				nNodes = getNrOfNodes(m_trees[i]);
				m_fLinesX[i] = new float[nNodes * 2 + 2];
				m_fLinesY[i] = new float[nNodes * 2 + 2];
				m_trees[i].drawDryWithSingleChild(m_fLinesX[i], m_fLinesY[i], 0, b, m_bSelection, m_fTreeOffset,
						m_fTreeScale);
			} else {
				m_fLinesX[i] = new float[nNodes * 2 + 2];
				m_fLinesY[i] = new float[nNodes * 2 + 2];
				calcLinesForNode(m_trees[i], m_fLinesX[i], m_fLinesY[i]);
			}
		}
		// calculate coordinates of lines for drawing consensus trees
		for (int i = 0; i < m_cTrees.length; i++) {
			// m_fCLinesX[i] = new float[nNodes * 2 + 2];
			// m_fCLinesY[i] = new float[nNodes * 2 + 2];
			if (m_bAllowSingleChild) {
				nNodes = getNrOfNodes(m_cTrees[i]);
				m_fCLinesX[i] = new float[nNodes * 2 + 2];
				m_fCLinesY[i] = new float[nNodes * 2 + 2];
				m_cTrees[i].drawDryWithSingleChild(m_fCLinesX[i], m_fCLinesY[i], 0, b, m_bSelection, m_fTreeOffset,
						m_fTreeScale);
			} else {
				m_fCLinesX[i] = new float[nNodes * 2 + 2];
				m_fCLinesY[i] = new float[nNodes * 2 + 2];
				calcLinesForNode(m_cTrees[i], m_fCLinesX[i], m_fCLinesY[i]);
			}
		}

		m_fRLinesX = new float[1][nNodes * 2 + 2];
		m_fRLinesY = new float[1][nNodes * 2 + 2];
		if (!m_bAllowSingleChild) {
			calcLinesForNode(m_rootcanaltree, m_fRLinesX[0], m_fRLinesY[0]);
		}
		
		if (m_bUseLogScale) {
			System.err.println("Use log scaling");
			//float f = (float) Math.log(m_fHeight + 1.0);
			float fNormaliser = (float) (m_fHeight / Math.pow(m_fHeight, m_fExponent));
			for (int i = 0; i < m_trees.length; i++) {
				for (int j = 0; j < m_fLinesY[i].length; j++) {
					m_fLinesY[i][j] = ((float) Math.pow(m_fHeight - m_fLinesY[i][j], m_fExponent)/fNormaliser);
				}
			}
			for (int i = 0; i < m_cTrees.length; i++) {
				for (int j = 0; j < m_fCLinesY[i].length; j++) {
					m_fCLinesY[i][j] = (float) Math.pow(m_fHeight - m_fCLinesY[i][j], m_fExponent)/fNormaliser;
				}
			}
			for (int j = 0; j < m_fRLinesY[0].length; j++) {
				m_fRLinesY[0][j] = (float) Math.pow(m_fHeight - m_fRLinesY[0][j], m_fExponent)/fNormaliser;
			}
		}
		m_w = 0;
		for (int i = 0; i < m_cTrees.length; i++) {
			float[] fCLines = m_fCLinesX[i];
			float fWeight = m_fTreeWeight[i];
			for (int j = 0; j < fCLines.length - 3; j += 4) {
				m_w += Math.abs(fCLines[j + 1] - fCLines[j + 2]) * fWeight;
			}
		}
		calcColors(false);
		calcLineWidths(false);
	} // calcLines
	
	
	void calcLinesForNode(Node node, float [] fLinesX, float [] fLinesY) {
		boolean[] b = new boolean[1];
		if (m_bAllowSingleChild) {
			node.drawDryWithSingleChild(fLinesX, fLinesY, 0, b, m_bSelection, m_fTreeOffset,
					m_fTreeScale);
		} else {
			switch (m_Xmode) {
			case 0:
				node.drawDry(fLinesX, fLinesY, 0, b, m_bSelection, m_fTreeOffset, m_fTreeScale);
				break;
			case 1:
				node.drawDryCentralised(fLinesX, fLinesY, 0, b, m_bSelection, m_fTreeOffset,
						m_fTreeScale, new float[2], new float[m_nNrOfLabels * 2 - 1],
						new float[m_nNrOfLabels * 2 - 1], m_cladePosition);
				break;
			case 2:
				float[] fCladeCenterX = new float[m_nNrOfLabels * 2 - 1];
				float[] fCladeCenterY = new float[m_nNrOfLabels * 2 - 1];
				float[] fPosX = new float[m_nNrOfLabels * 2 - 1];
				float[] fPosY = new float[m_nNrOfLabels * 2 - 1];
				node.getStarTreeCladeCenters(fCladeCenterX, fCladeCenterY, m_fTreeOffset, m_fTreeScale, m_cladePosition, m_sLabels.size());
				node.drawStarTree(fLinesX, fLinesY, fPosX, fPosY, fCladeCenterX, fCladeCenterY,
						m_bSelection, m_fTreeOffset, m_fTreeScale);
				break;
			}
		}

	}
	
	/**
	 * calculate coordinates for lines in real coordinates This initialises the
	 * m_nCLines and m_nCTLines (but not m_nLines,m_nTLines), arrays
	 **/

	public void calcLineWidths(boolean forceRecalc) {
		if (!forceRecalc) {
			if (m_lineWidthMode == m_prevLineWidthMode && m_lineWidthTag == m_prevLineWidthTag
					&& m_sLineWidthPattern == m_sPrevLineWidthPattern) {
				return;
			}
		} else {
			calcPositions();
			calcLines();
		}
		setWaitCursor();

		if (m_sLabels == null) {
			// no trees loaded
			return;
		}
		
		if (m_lineWidthMode == LineWidthMode.DEFAULT) {
			m_fLineWidth = null;
			m_fCLineWidth = null;
			m_fTopLineWidth = null;
			m_fTopCLineWidth = null;
			m_fRLineWidth = null;
			m_fRTopLineWidth = null;
			return;
		}
		m_fLineWidth = new float[m_trees.length][];
		m_fCLineWidth = new float[m_cTrees.length][];
		m_fTopLineWidth = new float[m_trees.length][];
		m_fTopCLineWidth = new float[m_cTrees.length][];
		m_fRLineWidth = new float[1][];
		m_fRTopLineWidth = new float[1][];
		checkSelection();
		m_bGroupOverflow = false;
		int nNodes = getNrOfNodes(m_trees[0]);

		if (m_lineWidthMode == LineWidthMode.BY_METADATA_PATTERN) {
			m_pattern = Pattern.compile(m_sLineWidthPattern);
		}
		if (m_lineWidthMode == LineWidthMode.BY_METADATA_NUMBER) {
			m_pattern = createPattern();
		}

		// calculate coordinates of lines for drawing trees
		boolean[] b = new boolean[1];
		for (int i = 0; i < m_trees.length; i++) {
			//m_fLinesX[i] = new float[nNodes * 2 + 2];
			//m_fLinesY[i] = new float[nNodes * 2 + 2];
			m_fLineWidth[i] = new float[nNodes * 2 + 2];
			m_fTopLineWidth[i] = new float[nNodes * 2 + 2];
			drawTreeS(m_trees[i], m_fLinesX[i], m_fLinesY[i], m_fLineWidth[i], m_fTopLineWidth[i], 0, b);
		}

		// calculate coordinates of lines for drawing consensus trees
		for (int i = 0; i < m_cTrees.length; i++) {
			//m_fCLinesX[i] = new float[nNodes * 2 + 2];
			//m_fCLinesY[i] = new float[nNodes * 2 + 2];
			m_fCLineWidth[i] = new float[nNodes * 2 + 2];
			m_fTopCLineWidth[i] = new float[nNodes * 2 + 2];
			drawTreeS(m_cTrees[i], m_fCLinesX[i], m_fCLinesY[i], m_fCLineWidth[i], m_fTopCLineWidth[i], 0, b);
			int nTopologies = 0;
			float [] fCLineWidth = new float[nNodes * 2 + 2];
			float [] fTopCLineWidth = new float[nNodes * 2 + 2];
			for (int j = 0; j < m_trees.length; j++) {
				if (m_nTopologyByPopularity[j] == i) {
					for (int k = 0; k < fCLineWidth.length; k++) {
						fCLineWidth[k] += m_fLineWidth[j][k];
						fTopCLineWidth[k] += m_fTopLineWidth[j][k];
					}
					nTopologies++;
				}
				
			}
			for (int k = 0; k < fCLineWidth.length; k++) {
				fCLineWidth[k] /= nTopologies;
				fTopCLineWidth[k] /= nTopologies;
			}
			m_fCLineWidth[i] = fCLineWidth;
			m_fTopCLineWidth[i] = fTopCLineWidth;
		}

		// TODO: don't know how to set line width of root canal tree, so keep it unspecified
		m_fRLineWidth[0] = new float[nNodes * 2 + 2];
		m_fRTopLineWidth[0] = new float[nNodes * 2 + 2];
		drawTreeS(m_rootcanaltree, m_fRLinesX[0], m_fRLinesY[0], m_fRLineWidth[0], m_fRTopLineWidth[0], 0, b);
		m_fRLineWidth = null;
		m_fRTopLineWidth = null;

	} // calcLinesWidths


	/** variables that deal with width of lines **/
	public enum LineWidthMode {BY_METADATA_PATTERN, BY_METADATA_NUMBER, DEFAULT, BY_METADATA_TAG};
	public LineWidthMode m_lineWidthMode = LineWidthMode.DEFAULT;
	LineWidthMode m_prevLineWidthMode = null;
	public String m_sLineWidthPattern = DEFAULT_PATTERN;
	String m_sPrevLineWidthPattern = null;
	public String m_lineWidthTag;
	String m_prevLineWidthTag;

	/** variables that deal with coloring of lines **/
	public enum LineColorMode {COLOR_BY_CLADE, BY_METADATA_PATTERN, DEFAULT, COLOR_BY_METADATA_TAG};
	public enum MetaDataType {NUMERIC, STRING, SET};
	public LineColorMode m_lineColorMode = LineColorMode.DEFAULT;
	public LineColorMode m_prevLineColorMode = null;
	public String m_sLineColorPattern = DEFAULT_PATTERN;
	String m_sPrevLineColorPattern = null;
	List<String> m_colorMetaDataCategories = new ArrayList<String>();
	public List<String> m_metaDataTags = new ArrayList<String>();
	public List<MetaDataType> m_metaDataTypes = new ArrayList<MetaDataType>();
	public String m_lineColorTag;
	String m_prevLineColorTag;
	public boolean m_showLegend = false;
	
	
	public void calcColors(boolean forceRecalc) {
		if (!forceRecalc) {
			if (m_lineColorMode == m_prevLineColorMode && m_lineColorTag == m_prevLineColorTag
					&& m_sLineColorPattern == m_sPrevLineColorPattern) {
				return;
			}
		}
		if (m_sLabels == null) {
			// no trees loaded
			return;
		}
		setWaitCursor();

		m_prevLineColorMode = m_lineColorMode; 
		m_prevLineColorTag = m_lineColorTag;
		m_sPrevLineColorPattern = m_sLineColorPattern;
		int nNodes = getNrOfNodes(m_trees[0]);
		switch (m_lineColorMode) {
		case COLOR_BY_CLADE:
			m_nLineColor = new int[m_trees.length][];
			m_nCLineColor = new int[m_cTrees.length][];
			m_nRLineColor = new int[1][];
			for (int i = 0; i < m_trees.length; i++) {
				if (m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_trees[i]);
				}
				m_nLineColor[i] = new int[nNodes * 2 + 2];
				colorTree(m_trees[i], m_nLineColor[i], 0);
			}
			if (m_bAllowSingleChild) {
				break;
			}
			// calculate coordinates of lines for drawing consensus trees
			for (int i = 0; i < m_cTrees.length; i++) {
				int nTopologies = 0;
				if (m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_cTrees[i]);
				}
				m_nCLineColor[i] = new int[nNodes * 2 + 2];
				int [] nCLineColor = m_nCLineColor[i]; 
				for (int j = 0; j < m_trees.length; j++) {
						for (int k = 0; k < nCLineColor.length; k++) {
							nCLineColor[k] += m_nLineColor[j][k];
						}
						nTopologies++;
				}
				for (int k = 0; k < nCLineColor.length; k++) {
					nCLineColor[k] /= nTopologies;
				}
			}
			if (m_bAllowSingleChild) {
				break;
			}
			m_nRLineColor[0] = new int[nNodes * 2 + 2];
			Arrays.fill(m_nRLineColor[0], m_color[CONSCOLOR].getRGB());
			break;
		case BY_METADATA_PATTERN:
			m_pattern = Pattern.compile(m_sLineColorPattern);
			m_nLineColor = new int[m_trees.length][];
			m_nCLineColor = new int[m_cTrees.length][];
			m_nRLineColor = new int[1][];
			m_colorMetaDataCategories = new ArrayList<String>();
			for (int i = 0; i < m_trees.length; i++) {
				if (m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_trees[i]);
				}
				m_nLineColor[i] = new int[nNodes * 2 + 2];
				colorTreeByMetaData(m_trees[i], m_nLineColor[i], 0);
			}
			if (m_bAllowSingleChild) {
				break;
			}
			// calculate coordinates of lines for drawing consensus trees
			for (int i = 0; i < m_cTrees.length; i++) {
				int nTopologies = 0;
				if (m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_cTrees[i]);
				}
				m_nCLineColor[i] = new int[nNodes * 2 + 2];
				int [] nCLineColor = m_nCLineColor[i]; 
				for (int j = 0; j < m_trees.length; j++) {
						for (int k = 0; k < nCLineColor.length; k++) {
							nCLineColor[k] += m_nLineColor[j][k];
						}
						nTopologies++;
				}
				for (int k = 0; k < nCLineColor.length; k++) {
					nCLineColor[k] /= nTopologies;
				}
			}
			if (m_bAllowSingleChild) {
				break;
			}
			m_nRLineColor[0] = new int[nNodes * 2 + 2];
			Arrays.fill(m_nRLineColor[0], m_color[CONSCOLOR].getRGB());
			break;
		case COLOR_BY_METADATA_TAG:
			m_pattern = Pattern.compile(m_sPattern);
			m_nLineColor = new int[m_trees.length][];
			m_nCLineColor = new int[m_cTrees.length][];
			m_nRLineColor = new int[1][];
			m_colorMetaDataCategories = new ArrayList<String>();
			boolean colorByCategory = false;
			for (int i = 0; i < m_metaDataTags.size(); i++) {
				if (m_metaDataTags.get(i).equals(m_lineColorTag)) {
					if (m_metaDataTypes.get(i).equals(MetaDataType.STRING)) {
						colorByCategory = true;
					}
					break;
				}
			}
			for (int i = 0; i < m_trees.length; i++) {
				if (m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_trees[i]);
				}
				m_nLineColor[i] = new int[nNodes * 2 + 2];
				colorTreeByMetaDataTag(m_trees[i], m_nLineColor[i], 0, colorByCategory);
			}
			if (m_bAllowSingleChild) {
				break;
			}
			// calculate coordinates of lines for drawing consensus trees
			for (int i = 0; i < m_cTrees.length; i++) {
				int nTopologies = 0;
				if (m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_cTrees[i]);
				}
				m_nCLineColor[i] = new int[nNodes * 2 + 2];
				int [] nCLineColor = m_nCLineColor[i]; 
				for (int j = 0; j < m_trees.length; j++) {
						for (int k = 0; k < nCLineColor.length; k++) {
							nCLineColor[k] += m_nLineColor[j][k];
						}
						nTopologies++;
				}
				for (int k = 0; k < nCLineColor.length; k++) {
					nCLineColor[k] /= nTopologies;
				}
			}
			m_nRLineColor[0] = new int[nNodes * 2 + 2];
			Arrays.fill(m_nRLineColor[0], m_color[CONSCOLOR].getRGB());
			break;
		case DEFAULT:
			m_nLineColor = new int[m_trees.length][];
			m_nCLineColor = new int[m_cTrees.length][];
			m_nRLineColor = new int[1][];
			for (int i = 0; i < m_trees.length; i++) {
				if (m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_trees[i]);
				}
				m_nLineColor[i] = new int[nNodes * 2 + 2];
				int color = 0;
				switch (m_nTopologyByPopularity[i]) {
				case 0:
					color = m_color[0].getRGB();
					break;
				case 1:
					color = m_color[1].getRGB();
					break;
				case 2:
					color = m_color[2].getRGB();
					break;
				default:
					color = m_color[3].getRGB();
				}
				Arrays.fill(m_nLineColor[i], color);
			}
			for (int i = 0; i < m_cTrees.length; i++) {
				int color = m_color[CONSCOLOR].getRGB();
				if (m_bViewMultiColor) {
					color = m_color[9 + (i % (m_color.length - 9))].getRGB();
				}
				if (m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_cTrees[i]);
				}
				m_nCLineColor[i] = new int[nNodes * 2 + 2];
				Arrays.fill(m_nCLineColor[i], color);
			}
			if (m_bAllowSingleChild) {
				break;
			}
			m_nRLineColor[0] = new int[nNodes * 2 + 2];
			Arrays.fill(m_nRLineColor[0], m_color[CONSCOLOR].getRGB());
			break;
		}
	} // calcColors


	private int colorTreeByMetaData(Node node, int[] nLineColor, int iPos) {
		if (!node.isLeaf()) {
			iPos = colorTreeByMetaData(node.m_left, nLineColor, iPos);
			if (node.m_right != null) {
				iPos = colorTreeByMetaData(node.m_right, nLineColor, iPos);
			}
			int color = m_color[9 + getMetaDataCategory(node.m_left) % (m_color.length - 9)].getRGB();
			nLineColor[iPos++] = color;
			nLineColor[iPos++] = color;
			if (node.m_right != null) {
				color = m_color[9 + getMetaDataCategory(node.m_right) % (m_color.length - 9)].getRGB();
			}
			nLineColor[iPos++] = color;
			nLineColor[iPos++] = color;
			if (node.isRoot()) {
				nLineColor[iPos++] = color;
				nLineColor[iPos++] = color;
			}
		}
		return iPos;
	}

	private int colorTreeByMetaDataTag(Node node, int[] nLineColor, int iPos, boolean colorByCategory) {
		if (!node.isLeaf()) {
			iPos = colorTreeByMetaDataTag(node.m_left, nLineColor, iPos, colorByCategory);
			if (node.m_right != null) {
				iPos = colorTreeByMetaDataTag(node.m_right, nLineColor, iPos, colorByCategory);
			}
			int color = colorForNode(node.m_left, colorByCategory);
			nLineColor[iPos++] = color;
			nLineColor[iPos++] = color;
			
			if (node.m_right != null) {
				color = colorForNode(node.m_right, colorByCategory);
			}
			nLineColor[iPos++] = color;
			nLineColor[iPos++] = color;
			if (node.isRoot()) {
				nLineColor[iPos++] = color;
				nLineColor[iPos++] = color;
			}
		}
		return iPos;
	}

	int colorForNode(Node node, boolean colorByCategory) {
		int color = 0;
		Object o = node.getMetaDataSet().get(m_lineColorTag);
		if (colorByCategory) {
			if (o != null) {
				if (!m_colorMetaDataCategories.contains(o)) {
					m_colorMetaDataCategories.add(o.toString());
				}
				color = m_color[9 + m_colorMetaDataCategories.indexOf(o.toString()) % (m_color.length - 9)].getRGB();
			}
		} else {
			if (o != null) {
				double frac = (((Double) o) - Node.g_minValue.get(m_lineColorTag)) /
						(Node.g_maxValue.get(m_lineColorTag) - Node.g_minValue.get(m_lineColorTag));
				color = (int)(frac * 255.0) << 16;
			}
		}
		return color;
	}
	
	
	private int colorTree(Node node, int[] nLineColor, int iPos) {
		if (!node.isLeaf()) {
			iPos = colorTree(node.m_left, nLineColor, iPos);
			iPos = colorTree(node.m_right, nLineColor, iPos);
			int color = m_color[9+node.m_iClade%9].getRGB();
			nLineColor[iPos++] = color;
			nLineColor[iPos++] = color;
			nLineColor[iPos++] = color;
			nLineColor[iPos++] = color;
			if (node.isRoot()) {
				nLineColor[iPos++] = color;
				nLineColor[iPos++] = color;
			}
		}
		return iPos;
	}

	/**
	 * 'draw' tree into an array of x & positions. This draws the tree as
	 * block diagram.
	 * 
	 * @param nX
	 * @param nY
	 * @param iPos
	 * @return
	 */
	int drawTreeS(Node node, float[] nX, float[] nY, float[] fWidth, float[] fWidthTop, int iPos, boolean[] bNeedsDrawing) {
		if (node.isLeaf()) {
			bNeedsDrawing[0] = m_bSelection[node.m_iLabel];
		} else {
			boolean[] bChildNeedsDrawing = new boolean[2];
			iPos = drawTreeS(node.m_left, nX, nY, fWidth, fWidthTop, iPos, bNeedsDrawing);
			bChildNeedsDrawing[0] = bNeedsDrawing[0];
			if (node.m_right != null) {
				iPos = drawTreeS(node.m_right, nX, nY, fWidth, fWidthTop, iPos, bNeedsDrawing);
				bChildNeedsDrawing[1] = bNeedsDrawing[0];
			} else {
				bChildNeedsDrawing[1] = false;
			}
			bNeedsDrawing[0] = false;
				if (bChildNeedsDrawing[0]) {
//					nX[iPos] = node.m_left.m_fPosX;
//					nY[iPos] = node.m_left.m_fPosY;
					fWidth[iPos] = getGamma(node.m_left, 1);
					fWidthTop[iPos] = getGamma(node.m_left, 2);
					iPos++;
//					nX[iPos] = nX[iPos - 1];
//					nY[iPos] = node.m_fPosY;
					bNeedsDrawing[0] = true;
				} else {
					fWidth[iPos] = m_nTreeWidth;
//					nX[iPos] = node.m_fPosX;
//					nY[iPos] = node.m_fPosY;
					iPos++;
//					nX[iPos] = node.m_fPosX;
//					nY[iPos] = node.m_fPosY;
				}
				fWidth[iPos] = fWidth[iPos-1];
				fWidthTop[iPos] = fWidthTop[iPos-1]; 
				iPos++;
				if (bChildNeedsDrawing[1]) {
//					nX[iPos] = node.m_right.m_fPosX;
//					nY[iPos] = nY[iPos - 1];
					fWidth[iPos] = getGamma(node.m_right, 1);
					fWidthTop[iPos] = getGamma(node.m_right, 2);
					iPos++;
//					nX[iPos] = nX[iPos - 1];
//					nY[iPos] = node.m_right.m_fPosY;
					bNeedsDrawing[0] = true;
				} else {
//					nX[iPos] = node.m_fPosX;
//					nY[iPos] = node.m_fPosY;
					fWidth[iPos] = m_nTreeWidth;
					iPos++;
//					nX[iPos] = node.m_fPosX;
//					nY[iPos] = node.m_fPosY;
				}
				fWidth[iPos] = fWidth[iPos-1];
				fWidthTop[iPos] = fWidthTop[iPos-1]; 
				iPos++;
				if (m_bGroupOverflow && m_bCorrectTopOfBranch) {
					float fCurrentWidth = getGamma(node, 1);
					float fSumWidth = fWidth[iPos-2] + fWidth[iPos-4];
					fWidthTop[iPos-2] = fCurrentWidth * fWidth[iPos-2]/fSumWidth;
					fWidthTop[iPos-4] = fCurrentWidth * fWidth[iPos-4]/fSumWidth;
				}

				
			if (node.isRoot()) {
//				nX[iPos] = node.m_fPosX;
//				nY[iPos] = node.m_fPosY;
				fWidth[iPos] = 0;//getGamma(node, 1);
				fWidthTop[iPos] = 0;//getGamma(node, 1);
				iPos++;
//				nX[iPos] = node.m_fPosX;
//				nY[iPos] = node.m_fPosY - node.m_fLength;
				iPos++;
			}
		}
		return iPos;
	}

	float getGamma(Node node , int nGroup) {
		if (m_lineWidthMode == LineWidthMode.BY_METADATA_PATTERN) {
			String sMetaData = node.getMetaData();
			try {
				Matcher matcher = m_pattern.matcher(sMetaData);
				matcher.find();
				int nGroups = matcher.groupCount();
				if (nGroup > nGroups) {
					nGroup = 1;
					m_bGroupOverflow = true;
				}
				String sMatch = matcher.group(nGroup);
		        float f = Float.parseFloat(sMatch);
		        return f;
			} catch (Exception e) {
			}
		} else if (m_lineWidthMode == LineWidthMode.BY_METADATA_NUMBER) {
			int index = 0;
			if (nGroup == 1) {
				index = m_iPatternForBottom - 1;
			} else {
				index = m_iPatternForTop;
				if (index < 0) {
					index = m_iPatternForBottom - 1;
				}
			}
			if (index < 0) {
				index = 0;
			}
			return (float) (node.getMetaDataList().get(index)/node.g_maxListValue.get(index));
		} else {
			Object o = node.getMetaDataSet().get(m_lineWidthTag);
			if (o != null) {
				try {
					double frac = (((Double) o) - Node.g_minValue.get(m_lineWidthTag)) /
							(Node.g_maxValue.get(m_lineWidthTag) - Node.g_minValue.get(m_lineWidthTag));
					return (float) frac;
				} catch (Exception e) {
					int h = 3;
					h++;
				}
			}
		}
		return 1f;
	}

	
	
	/** initialise order of leafs **/
	int initOrder(Node node, int iNr) throws Exception {
		if (node.isLeaf()) {
			m_nOrder[node.m_iLabel] = iNr;
			m_nRevOrder[iNr] = node.m_iLabel;
			return iNr + 1;
		} else {
			iNr = initOrder(node.m_left, iNr);
			if (node.m_right != null) {
				iNr = initOrder(node.m_right, iNr);
			}
		}
		return iNr;
	}

	/** number of leafs in selection, handy for sanity checks **/
	int selectionSize() {
		int nSelected = 0;
		for (int i = 0; i < m_nRevOrder.length; i++) {
			if (m_bSelection[m_nRevOrder[i]]) {
				nSelected++;
			}
		}
		return nSelected;
	}

	/** check the selection is empty, and ask user whether this is desirable **/
	void checkSelection() {
		if (m_bSelection.length > 0 && selectionSize() == 0) {
			for (int i = 0; i < m_bSelection.length; i++) {
				m_bSelection[i] = true;
			}
		}
	}

	/** check at least one, but not all labels are selected **/
	boolean moveSanityChek() {
		int nSelected = selectionSize();
		if (nSelected > 0 && nSelected < m_nRevOrder.length - 1) {
			return true;
		}
		JOptionPane.showMessageDialog(null, "To move labels, select at least one, but not all of the labels",
				"Move error", JOptionPane.PLAIN_MESSAGE);
		return false;
	}

	/** move labels in selection down in ordering **/
	void moveSelectedLabelsDown() {
		for (int i = 1; i < m_nRevOrder.length; i++) {
			if (m_bSelection[m_nRevOrder[i]] && !m_bSelection[m_nRevOrder[i - 1]]) {
				int h = m_nRevOrder[i];
				m_nRevOrder[i] = m_nRevOrder[i - 1];
				m_nRevOrder[i - 1] = h;
			}
		}
		for (int i = 0; i < m_nRevOrder.length; i++) {
			m_nOrder[m_nRevOrder[i]] = i;
		}
		calcPositions();
		addAction(new DoAction());
	}

	/** move labels in selection up in ordering **/
	void moveSelectedLabelsUp() {
		for (int i = m_nRevOrder.length - 2; i >= 0; i--) {
			if (m_bSelection[m_nRevOrder[i]] && !m_bSelection[m_nRevOrder[i + 1]]) {
				int h = m_nRevOrder[i];
				m_nRevOrder[i] = m_nRevOrder[i + 1];
				m_nRevOrder[i + 1] = h;
			}
		}
		for (int i = 0; i < m_nRevOrder.length; i++) {
			m_nOrder[m_nRevOrder[i]] = i;
		}
		calcPositions();
		addAction(new DoAction());
	}

	void calcPositions() {
		if (m_sLabels == null) {
			// no trees loaded yet
			return;
		}
		setWaitCursor();

		if (!m_bAllowSingleChild) {
			Arrays.fill(m_cladePosition, -1);
			boolean bProgress = true;
			do {
				bProgress = false;
				for (int i = 0; i < m_clades.size(); i++) {
					if (m_cladePosition[i] < 0) {
						m_cladePosition[i] = positionClades(i);
						if (m_cladePosition[i] >= 0) {
							bProgress = true;
						}
					}
				}
			} while (bProgress);
	
			if (m_bUseAngleCorrection) {
				for (int i = 0; i < m_clades.size(); i++) {
					if (m_cladeWeight.get(i) > m_fAngleCorrectionThresHold) {
						for (ChildClade child : m_cladeChildren.get(i)) {
							if (m_cladeWeight.get(child.m_iLeft) < m_fAngleCorrectionThresHold) {
								m_cladePosition[child.m_iLeft] = m_cladePosition[i];
							}
							if (m_cladeWeight.get(child.m_iRight) < m_fAngleCorrectionThresHold) {
								m_cladePosition[child.m_iRight] = m_cladePosition[i];
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < m_trees.length; i++) {
			if (m_nShuffleMode == NodeOrderer.GEOINFO) {
				positionLeafsGeo(m_trees[i]);
			} else {
				positionLeafs(m_trees[i]);
			}
			positionRest(m_trees[i]);
		}
		for (int i = 0; i < m_cTrees.length; i++) {
			if (m_nShuffleMode == NodeOrderer.GEOINFO) {
				positionLeafsGeo(m_cTrees[i]);
			} else {
				positionLeafs(m_cTrees[i]);
			}
			positionRest(m_cTrees[i]);
		}
		if (!m_bAllowSingleChild) {
			for (Node tree : m_summaryTree) {
				positionLeafs(tree);
				positionRest(tree);
			}
		}
	}

	private float positionClades(int iClade) {
		float fMin = m_nNrOfLabels;
		float fMax = 0;
		for (int i : m_clades.get(iClade)) {
			fMin = Math.min(fMin, m_nOrder[i]);
			fMax = Math.max(fMax, m_nOrder[i]);
		}
		return (fMin + fMax) / 2.0f + 0.5f;
	}

	/**
	 * Position leafs in a tree so that x-coordinate of the leafs is fixed for
	 * all trees in the set
	 * **/
	void positionLeafs(Node node) {
		if (node.isLeaf()) {
			node.m_fPosX = m_nOrder[node.m_iLabel] + 0.5f;
			// node.m_fPosX = _posX[m_nOrder[node.m_iLabel]] + 0.5f;
		} else {
			positionLeafs(node.m_left);
			if (node.m_right != null) {
				positionLeafs(node.m_right);
			}
		}
	}

	/**
	 * Position leafs in a tree so that x-coordinate of the leafs coincides with
	 * the geographical position associated with the node
	 * **/
	void positionLeafsGeo(Node node) {
		if (node.isLeaf()) {
			if (m_treeDrawer.m_bRootAtTop) {
				node.m_fPosX = m_nNrOfLabels * (m_fLongitude.elementAt(node.m_iLabel) - m_fMinLong)
						/ (m_fMaxLong - m_fMinLong);
			} else {
				node.m_fPosX = m_nNrOfLabels * (m_fMaxLat - m_fLatitude.elementAt(node.m_iLabel))
						/ (m_fMaxLat - m_fMinLat);
			}
		} else {
			positionLeafsGeo(node.m_left);
			positionLeafsGeo(node.m_right);
		}
	}

	/**
	 * Position internal nodes to take position in between child nodes Should be
	 * called after positionLeafs to ensure leaf positions are initialized
	 * 
	 * @param node
	 * @return
	 */
	float positionRest(Node node) {
		if (node.isLeaf()) {
			return node.m_fPosX;
		} else {
			// node.m_fPosX = m_cladePosition[node.m_iClade];
			float fPosX = 0;
			fPosX += positionRest(node.m_left);
			if (node.m_right != null) {
				fPosX += positionRest(node.m_right);
				fPosX /= 2.0;
			}
			node.m_fPosX = fPosX;
			return fPosX;
		}
	}

	/**
	 * return meta data value of a node as defined by the pattern (m_sPattern &
	 * m_pattern), or 1 if parsing fails.
	 */
	// int [] m_nCurrentPosition;
	float getMetaData(Node node) {
		try {
			Matcher matcher = m_pattern.matcher(node.getMetaData());
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
			Matcher matcher = m_pattern.matcher(node.getMetaData());
			matcher.find();
			int nGroup = 1;
			int nGroups = matcher.groupCount();
			if (nGroup > nGroups) {
				nGroup = 1;
			}
			String match = matcher.group(nGroup);
			if (!m_colorMetaDataCategories.contains(match)) {
				m_colorMetaDataCategories.add(match);
			}
			//System.err.println(node.m_sMetaData + ": " + match + " = " + m_metaDataCategories.indexOf(match));
			return m_colorMetaDataCategories.indexOf(match);
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
		node.m_fPosX = (float) (m_sLabels.size() - node.m_fPosX * fScale);
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

	int collectMetaData(Node node, float[] fHeights, float fLengthToRoot, int iPos, float[] fMetas, int[] nCounts) {
		float fHeight = node.m_fPosY;// fLengthToRoot + node.m_fLength;
		float fMeta = getMetaData(node);
		int i = Arrays.binarySearch(fHeights, fHeight);
		while (i >= 0 && fHeights[i] > fLengthToRoot) {
			fMetas[i] += fMeta;
			nCounts[i]++;
			i--;
		}
		if (!node.isLeaf()) {
			iPos = collectMetaData(node.m_left, fHeights, fHeight/*
																 * fLengthToRoot
																 * +
																 * node.m_fLength
																 */, iPos, fMetas, nCounts);
			iPos = collectMetaData(node.m_right, fHeights, fHeight/*
																 * fLengthToRoot
																 * +
																 * node.m_fLength
																 */, iPos, fMetas, nCounts);
		}
		return iPos;
	} // collectMetaData

	/**
	 * record position information in position array (fPosX) used for undo/redo
	 **/
	void getPosition(Node node, float[] fPosX) {
		if (node.isLeaf()) {
			fPosX[m_nOrder[node.m_iLabel]] = node.m_fPosX;
		} else {
			getPosition(node.m_left, fPosX);
			if (node.m_right != null) {
				getPosition(node.m_right, fPosX);
			}
		}
	}

	/**
	 * set position information based on position array (fPosX) used for
	 * undo/redo
	 **/
	void setPosition(Node node, float[] fPosX) {
		if (node.isLeaf()) {
			node.m_fPosX = fPosX[m_nOrder[node.m_iLabel]];
		} else {
			setPosition(node.m_left, fPosX);
			if (node.m_right != null) {
				setPosition(node.m_right, fPosX);
			}
		}
	}

	/**
	 * position nodes so that root node is at fOffset (initially 0) and rest
	 * according to lengths of the branches
	 */
	float positionHeight(Node node, float fOffSet) {
		if (node.isLeaf()) {
			node.m_fPosY = fOffSet + node.m_fLength;
			return node.m_fPosY;
		} else {
			float fPosY = fOffSet + node.m_fLength;
			float fYMax = 0;
			fYMax = Math.max(fYMax, positionHeight(node.m_left, fPosY));
			if (node.m_right != null) {
				fYMax = Math.max(fYMax, positionHeight(node.m_right, fPosY));
			}
			node.m_fPosY = fPosY;
			return fYMax;
		}
	}

	float height(Node node) {
		if (node.isLeaf()) {
			return node.m_fLength;
		} else {
			return node.m_fLength + Math.max(height(node.m_left), height(node.m_right));
		}
	}

	/** move y-position of a tree with offset f **/
	void offsetHeight(Node node, float f) {
		if (!node.isLeaf()) {
			offsetHeight(node.m_left, f);
			if (node.m_right != null) {
				offsetHeight(node.m_right, f);
			}
		}
		node.m_fPosY += f;
	}

	/**
	 * move divide y-position of a tree with factor f. Useful to calculate
	 * consensus trees.
	 **/
	void divideLength(Node node, float f) {
		if (!node.isLeaf()) {
			divideLength(node.m_left, f);
			if (node.m_right != null) {
				divideLength(node.m_right, f);
			}
		}
		node.m_fLength /= f;
	}

	/**
	 * add length of branches in src to that of target Useful to calculate
	 * consensus trees. Assumes src and target share same topology
	 */
	void addLength(Node src, Node target) {
		// assumes same topologies for src and target
		if (!src.isLeaf()) {
			addLength(src.m_left, target.m_left);
			if (src.m_right != null) {
				addLength(src.m_right, target.m_right);
			}
		}
		target.m_fLength += src.m_fLength;
	}

	/** draw only labels of a tree, not the branches **/
	void drawLabels(Node node, Graphics2D g) {
		g.setFont(m_font);
		if (m_bShowBounds) {
			return;
		}

		if (node.isLeaf()) {
			if (m_bSelection[node.m_iLabel]) {
				if (m_iColor == null) {
					g.setColor(m_color[LABELCOLOR]);
				} else {
					g.setColor(m_color[GEOCOLOR + m_iColor[node.m_iLabel] % (m_color.length - GEOCOLOR)]);
				}
			} else {
				g.setColor(Color.GRAY);
			}
			if (m_treeDrawer.m_bRootAtTop) {
				if (m_bRotateTextWhenRootAtTop) {
					int x = (int) (node.m_fPosX * m_fScaleX /* m_fScale */) - g.getFontMetrics().getHeight() / 3;
					int y = getPosY(((m_bAlignLabels ? m_fHeight:node.m_fPosY) + m_fLabelIndent - m_fTreeOffset) * m_fTreeScale) + 2;// ;
					g.rotate(Math.PI / 2.0);
					g.translate(y, -x);
					g.drawString(m_sLabels.elementAt(node.m_iLabel), 0, 0);
					g.translate(-y, x);
					g.rotate(-Math.PI / 2.0);
					Rectangle r = m_bLabelRectangle[node.m_iLabel];
					r.x = x;
					r.y = y - 10;
					r.height = 10;
					r.width = m_nLabelWidth;
				} else {
					String sLabel = m_sLabels.elementAt(node.m_iLabel);
					int x = (int) (node.m_fPosX * m_fScaleX /* m_fScale */) - g.getFontMetrics().stringWidth(sLabel)
							/ 2;
					int y = getPosY(((m_bAlignLabels ?m_fHeight:node.m_fPosY) + m_fLabelIndent - m_fTreeOffset) * m_fTreeScale)
							+ g.getFontMetrics().getHeight() + 2;
					g.drawString(sLabel, x, y);
					Rectangle r = m_bLabelRectangle[node.m_iLabel];
					r.x = x;
					r.y = y - 10;
					r.height = 10;
					r.width = m_nLabelWidth;
				}
			} else {
				int y = (int) (node.m_fPosX * m_fScaleY/* m_fScale */) + g.getFontMetrics().getHeight() / 3;
				int x = getPosX(((m_bAlignLabels ?m_fHeight:node.m_fPosY) + m_fLabelIndent - m_fTreeOffset) * m_fTreeScale) + 1;
				g.drawString(m_sLabels.elementAt(node.m_iLabel), x, y);
				Rectangle r = m_bLabelRectangle[node.m_iLabel];
				r.x = x;
				r.y = y - 10;
				r.height = 10;
				r.width = m_nLabelWidth;
			}
		} else {
			drawLabels(node.m_left, g);
			if (node.m_right != null) {
				drawLabels(node.m_right, g);
			}
		}
	}

	/** draw lines from labels of a tree to corresponding geographic point **/
	void drawGeo(Node node, Graphics g) {
		if (node.isLeaf()) {
			if (m_bSelection[node.m_iLabel]) {
				if (m_fLongitude.elementAt(node.m_iLabel) == 0 && m_fLatitude.elementAt(node.m_iLabel) == 0) {
					return;
				}
				int gx = (int) ((m_fLongitude.elementAt(node.m_iLabel) - m_fMinLong) * m_fScaleGX * m_fScale);
				int gy = (int) ((m_fMaxLat - m_fLatitude.elementAt(node.m_iLabel)) * m_fScaleGY * m_fScale);
				g.setColor(m_color[GEOCOLOR]);
				if (m_treeDrawer.m_bRootAtTop) {
					int x = (int) (node.m_fPosX * m_fScaleX * m_fScale) + 10;
					int y = getPosY(node.m_fPosY * m_fScale);
					g.drawLine(x, y, gx, gy);
					g.setColor(Color.BLACK);
					g.drawOval(gx - 1, gy - 1, 3, 3);
				} else {
					int y = (int) (node.m_fPosX * m_fScaleY * m_fScale);
					int x = getPosX(node.m_fPosY * m_fScale);
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
		if (m_bUseLogScale) {
			return (int) (m_fHeight / Math.log(m_fHeight + 1.0) * m_fScaleY * (Math.log(m_fHeight + 1.0) - Math.log(m_fHeight - fHeight + 1.0)));
		}
		return (int) (fHeight * m_fScaleY);
	}
	
	float screenPosToHeight(int nX, int nY) {
		if (m_bUseLogScale) {
			return Float.NaN;
		}
		if (m_treeDrawer.m_bRootAtTop) {
			return  m_fHeight - ((nY/ m_fScaleY) + m_fTreeOffset);
		} else {
			return  m_fHeight - ((nX/ m_fScaleX) + m_fTreeOffset);
		}
	}

	/**
	 * convert height info in tree to x position on screen -- use when root not
	 * at top
	 **/
	int getPosX(float fHeight) {
		if (m_bUseLogScale) {
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
		if (m_sLabels == null) {
			// no trees loaded yet
			return;
		}
		m_fScaleX = 10;
		m_fScaleY = 10;
		int nW = (int) (getWidth() / m_fScale) - 24;
		int nH = (int) (getHeight() / m_fScale) - 24;
		nW = (int) getWidth() - 24;
		nH = (int) getHeight() - 24;
		if (m_treeDrawer.m_bRootAtTop) {
			m_fScaleX = (nW + 0.0f) / m_sLabels.size();
			m_fScaleGX = (nW + 0.0f) / (m_fMaxLong - m_fMinLong);
			if (m_fHeight > 0) {
				if (m_bRotateTextWhenRootAtTop) {
					m_fScaleY = (nH - m_nLabelWidth - 0.0f) / m_fHeight;
					m_fScaleGY = (nH - m_nLabelWidth - 0.0f) / (m_fMaxLat - m_fMinLat);
				} else {
					m_fScaleY = (nH - 10.0f) / m_fHeight;
					m_fScaleGY = (nH - 10.0f) / (m_fMaxLat - m_fMinLat);
				}
			}
		} else {
			if (m_sLabels != null && m_sLabels.size() > 0) {
				m_fScaleY = (nH + 0.0f) / m_sLabels.size();
				m_fScaleGY = (nH + 0.0f) / (m_fMaxLat - m_fMinLat);
			}
			if (m_fHeight > 0) {
				m_fScaleX = (nW - m_nLabelWidth + 0.0f) / m_fHeight;
				m_fScaleGX = (nW - m_nLabelWidth + 0.0f) / (m_fMaxLong - m_fMinLong);
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
			m_Xmode = 0;
			m_bUseAngleCorrection = false;
			m_treeDrawer.m_bViewBlockTree = false;
			m_viewClades.setEnabled(false);
			m_viewEditTree.setEnabled(true);
			break;
		case 1: // star tree
			m_Xmode = 2;
			m_bUseAngleCorrection = false;
			m_treeDrawer.m_bViewBlockTree = false;
			m_viewClades.setEnabled(true);
			m_viewEditTree.setEnabled(false);
			break;
		case 2: // centralised
			m_Xmode = 1;
			m_bUseAngleCorrection = false;
			m_treeDrawer.m_bViewBlockTree = false;
			m_viewClades.setEnabled(true);
			m_viewEditTree.setEnabled(false);
			break;
		case 3: // centralised + angle corrected
			m_Xmode = 1;
			m_bUseAngleCorrection = true;
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
 
	
	void setStyle(int nStyle) {
		BranchDrawer bd = null;
		switch (nStyle) {
		case 0:
			if (m_lineWidthMode != LineWidthMode.DEFAULT) {
				bd = new TrapeziumBranchDrawer();
			} else {
				bd = new BranchDrawer();
			}
			m_treeDrawer.m_bViewBlockTree = false;
			break;
		case 1:
			if (m_lineWidthMode != LineWidthMode.DEFAULT) {
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
	public TreeSetPanel m_Panel;
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
	/** use log scaling for drawing height **/
	boolean m_bUseLogScale = false;
	double m_fExponent = 1.0;


	/** show consensus tree in multiple colours, or just main colour */
	public boolean m_bViewMultiColor = false;
	/** show geographical info if available **/
	public boolean m_bDrawGeo = true;

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

		public MyAction(String sName, String sToolTipText, String sIcon, String sAcceleratorKey) {
			super(sName);
			// setToolTipText(sToolTipText);
			putValue(Action.SHORT_DESCRIPTION, sToolTipText);
			putValue(Action.LONG_DESCRIPTION, sToolTipText);
			if (sAcceleratorKey.length() > 0) {
				KeyStroke keyStroke = KeyStroke.getKeyStroke(sAcceleratorKey);
				if (sAcceleratorKey.contains("+")) {
					keyStroke = KeyStroke.getKeyStroke('+');
				}
				if (sAcceleratorKey.contains("-")) {
					keyStroke = KeyStroke.getKeyStroke('-');
				}
				putValue(Action.ACCELERATOR_KEY, keyStroke);
			}
			putValue(Action.MNEMONIC_KEY, (int) sName.charAt(0));
			setIcon(sIcon);
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
		public void actionPerformed(ActionEvent ae) {
		}
	} // class MyAction

	/** base class for actions that allow customisation of a color **/
	class ColorAction extends MyAction {
		private static final long serialVersionUID = 1L;
		int m_iColor;

		public ColorAction(String sName, String sToolTipText, String sIcon, String sAcceleratorKey, int iColor) {
			super(sName, sToolTipText, sIcon, sAcceleratorKey);
			m_iColor = iColor;
		}

		public void actionPerformed(ActionEvent ae) {
			Color newColor = JColorChooser.showDialog(m_Panel, getName(), m_color[m_iColor]);
			if (newColor != null) {
				m_color[m_iColor] = newColor;
				makeDirty();
			}
			repaint();
		}
	} // class ColorAction

	class ShuffleAction extends MyAction {
		private static final long serialVersionUID = 1L;
		int m_nMode;

		public ShuffleAction(String sName, String sToolTipText, String sIcon, String sAcceleratorKey, int nMode) {
			super(sName, sToolTipText, sIcon, sAcceleratorKey);
			m_nMode = nMode;
		}

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

		public SettingAction(String sName, String sToolTipText, String sIcon, String sAcceleratorKey) {
			super(sName, sToolTipText, sIcon, sAcceleratorKey);
			m_sName = sName;
		}

		public void actionPerformed(ActionEvent ae) {
			if (m_sName.equals("Jitter+")) {
				m_nJitter++;
				if (m_nJitter >= 0) {
					makeDirty();
				}
			}
			if (m_sName.equals("Jitter-")) {
				m_nJitter--;
				if (m_nJitter >= 0) {
					makeDirty();
				}
			}
			if (m_sName.equals("Intensity+")) {
				m_fTreeIntensity *= 1.1;
				makeDirty();
			}
			if (m_sName.equals("Intensity-")) {
				m_fTreeIntensity /= 1.1;
				makeDirty();
			}
			if (m_sName.equals("Consensus Intensity+")) {
				m_fCTreeIntensity *= 1.1;
				makeDirty();
			}
			if (m_sName.equals("Consensus Intensity-")) {
				m_fCTreeIntensity /= 1.1;
				makeDirty();
			}
			if (m_sName.equals("Consensus Tree Width+")) {
				m_nCTreeWidth++;
				makeDirty();
			}
			if (m_sName.equals("Consensus Tree Width-")) {
				m_nCTreeWidth--;
				if (m_nCTreeWidth <= 1) {
					m_nCTreeWidth = 1;
				}
				makeDirty();
			}
			if (m_sName.equals("Tree Width+")) {
				m_nTreeWidth++;
				makeDirty();
			}
			if (m_sName.equals("Tree Width-")) {
				m_nTreeWidth--;
				if (m_nTreeWidth <= 1) {
					m_nTreeWidth = 1;
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
				m_fAngleCorrectionThresHold *= 1.1;
				if (m_fAngleCorrectionThresHold > 0.999) {
					m_fAngleCorrectionThresHold = 0.999;
				}
				System.err.println("Angle Correction ThresHold = " + m_fAngleCorrectionThresHold);
				calcPositions();
				calcLines();
				makeDirty();
			}
			if (m_sName.equals("Angle Correction-")) {
				m_fAngleCorrectionThresHold /= 1.1;
				System.err.println("Angle Correction ThresHold = " + m_fAngleCorrectionThresHold);
				calcPositions();
				calcLines();
				makeDirty();
			}
			repaint();
			System.err.print(getStatus());
		} // actionPerformed
	} // class SettingAction

	/** actions triggered by GUI events */
	Action a_quit = new MyAction("Exit", "Exit Program", "exit", "") {
		private static final long serialVersionUID = -10;

		public void actionPerformed(ActionEvent ae) {
			System.exit(0);
		}
	}; // class ActionQuit

	Action a_paste = new MyAction("Paste", "Paste tree(s) from clipboard", "paste", "") {
		private static final long serialVersionUID = -10;

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
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase().endsWith(getExtention());
		}

		abstract public String getExtention();
	}

	Action a_exportVector = new MyAction("Export", "Export", "export", "") {
		private static final long serialVersionUID = -1;

		public void actionPerformed(ActionEvent ae) {
			JFileChooser fc = new JFileChooser(m_sDir);
			fc.addChoosableFileFilter(new MyFileFilter() {
				public String getExtention() {
					return ".bmp";
				}

				public String getDescription() {
					return "Bitmap files (*.bmp)";
				}
			});
			fc.addChoosableFileFilter(new MyFileFilter() {
				public String getExtention() {
					return ".jpg";
				}

				public String getDescription() {
					return "JPEG bitmap files (*.jpg)";
				}
			});
			fc.addChoosableFileFilter(new MyFileFilter() {
				public String getExtention() {
					return ".png";
				}

				public String getDescription() {
					return "PNG bitmap files (*.png)";
				}
			});
			fc.addChoosableFileFilter(new MyFileFilter() {
				public String getExtention() {
					return ".pdf";
				}

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
					m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
				}
				if (sFileName != null && !sFileName.equals("")) {
					if (!(sFileName.toLowerCase().endsWith(".png") || sFileName.toLowerCase().endsWith(".jpg")
							|| sFileName.toLowerCase().endsWith(".pdf")
							|| sFileName.toLowerCase().endsWith(".bmp") || sFileName.toLowerCase().endsWith(".svg"))) {
						sFileName += ((MyFileFilter) fc.getFileFilter()).getExtention();
					}

                    if (sFileName.toLowerCase().endsWith(".pdf")) {
                    	try {
                    	com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
                    	PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(sFileName));
                    	doc.setPageSize(new com.itextpdf.text.Rectangle(m_Panel.getWidth(), m_Panel.getHeight()));
                    	doc.open();
                    	PdfContentByte cb = writer.getDirectContent();
                    	Graphics2D g = new PdfGraphics2D(cb, m_Panel.getWidth(), m_Panel.getHeight());
                    	 
						BufferedImage bi;
						bi = new BufferedImage(m_Panel.getWidth(), m_Panel.getHeight(), BufferedImage.TYPE_INT_RGB);
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

	Action a_print = new MyAction("Print", "Print Graph", "print", "ctrl P") {
		private static final long serialVersionUID = -20389001859354L;

		// boolean m_bIsPrinting = false;

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

	Action a_new = new MyAction("New", "New instance of DensiTree", "new", "ctrl N") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent ae) {
			startNew(new String[]{});
		}
	};

	Action a_load = new MyAction("Load", "Load Graph", "open", "ctrl O") {
		private static final long serialVersionUID = -2038911085935515L;

		public void actionPerformed(ActionEvent ae) {
			JFileChooser fc = new JFileChooser(m_sDir);
			fc.addChoosableFileFilter(new FileFilter() {
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
				public String getDescription() {
					return "Nexus trees files";
				}
			});

			fc.setDialogTitle("Load Tree Set");
			int rval = fc.showOpenDialog(m_Panel);

			if (rval == JFileChooser.APPROVE_OPTION) {
				String sFileName = fc.getSelectedFile().toString();
				if (sFileName.lastIndexOf('/') > 0) {
					m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
				}
				try {
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
				// makeDirty();
			}
		}
	}; // class ActionLoad

	public Action a_loadkml = new MyAction("Load locations", "Load geographic locations of taxa", "geo", "") {
		private static final long serialVersionUID = -1L;

		public void actionPerformed(ActionEvent ae) {
			JFileChooser fc = new JFileChooser(m_sDir);
			fc.addChoosableFileFilter(new FileFilter() {
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
				public String getDescription() {
					return "KML file with taxon locations";
				}
			});

			fc.setDialogTitle("Load Geographic Locations");
			int rval = fc.showOpenDialog(m_Panel);

			if (rval == JFileChooser.APPROVE_OPTION) {
				String sFileName = fc.getSelectedFile().toString();
				if (sFileName.lastIndexOf('/') > 0) {
					m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
				}
				loadKML(sFileName);
				m_jStatusBar.setText("Loaded " + sFileName);
				fitToScreen();
				// makeDirty();
			}
		}
	}; // class ActionLoadKML

	Action a_saveas = new MyAction("Save as", "Save as", "save", "ctrl S") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent ae) {
			JFileChooser fc = new JFileChooser(m_sDir);
			fc.addChoosableFileFilter(new FileFilter() {
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
				public String getDescription() {
					return "Nexus trees files";
				}
			});

			fc.setDialogTitle("Save Graph");
			int rval = fc.showSaveDialog(m_Panel);

			if (rval == JFileChooser.APPROVE_OPTION) {
				String sFileName = fc.getSelectedFile().toString();
				if (sFileName.lastIndexOf('/') > 0) {
					m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
				}
				try {
					FileWriter outfile = new FileWriter(sFileName);
					StringBuffer buf = new StringBuffer();
					buf.append("#NEXUS\n");
					buf.append("Begin trees\n");
					buf.append("\tTranslate\n");
					for (int i = 0; i < m_sLabels.size(); i++) {
						buf.append("\t\t" + i + " " + m_sLabels.get(i));
						if (i < m_sLabels.size() - 1) {
							buf.append(",");
						}
						buf.append("\n");
					}
					buf.append(";\n");
					outfile.write(buf.toString());
					for (int i = 0; i < m_trees.length; i++) {
						outfile.write("tree STATE_" + i + " = " + m_trees[i].toString() + ";\n");
						System.out.println(m_trees[i].toString(m_sLabels, false));
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

	Action a_loadimage = new MyAction("Background image ", "Load background image", "bgimage", "") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent ae) {
			JFileChooser fc = new JFileChooser(m_sDir);
			fc.addChoosableFileFilter(new FileFilter() {
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
				public String getDescription() {
					return "Image files";
				}
			});

			fc.setDialogTitle("Load Background Image");
			int rval = fc.showOpenDialog(m_Panel);

			if (rval == JFileChooser.APPROVE_OPTION) {
				String sFileName = fc.getSelectedFile().toString();
				if (sFileName.lastIndexOf('/') > 0) {
					m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
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

	Action a_viewClades = new MyAction("View clades", "List clades and their densities", "viewclades", "") {
		private static final long serialVersionUID = -1;

		public void actionPerformed(ActionEvent ae) {
			JDialog dlg = new JDialog();
			dlg.setModal(true);
			dlg.setSize(400, 400);
			setWaitCursor();
			//m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			String sCladeText = "";
			for (String s : cladesToString()) {
				sCladeText += s;
			}
			setDefaultCursor();
			//m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			JTextArea textArea = new JTextArea(sCladeText);
			JScrollPane scrollPane = new JScrollPane(textArea);
			dlg.add(scrollPane);
			dlg.setTitle("Clades and their probabilities");
			dlg.setVisible(true);
		}
	}; // class ActionHelp

	Action a_help = new MyAction("Help", "DensiTree - Tree Set Visualization Help", "help", "") {
		private static final long serialVersionUID = -20389110859354L;

		public void actionPerformed(ActionEvent ae) {
			String sStatus = getStatus();
			String sCmdLineOptions = "\n\nTo start with the same settings, use the following command:\njava -jar DensiTree.jar -c "
					+ m_fCTreeIntensity
					+ " -i "
					+ m_fTreeIntensity
					+ " -j "
					+ m_nJitter
					+ " -w "
					+ m_nCTreeWidth
					+ " -v " + m_nTreeWidth + " -f " + m_nAnimationDelay + " -t " + m_Panel.m_nDrawThreads + " -b " + m_nBurnIn;
			System.out.println(sCmdLineOptions);
			JOptionPane.showMessageDialog(null, banner() + sStatus + sCmdLineOptions, "Help Message",
					JOptionPane.PLAIN_MESSAGE);
		}
	}; // class ActionHelp

	Action a_about = new MyAction("About", "Help about", "about", "") {
		private static final long serialVersionUID = -20389110859353L;
		public void actionPerformed(ActionEvent ae) {
			if (JOptionPane.showOptionDialog(null, "DensiTree - Tree Set Visualization\nVersion: " + VERSION
					+ "\n\nRemco Bouckaert\nremco@cs.waikato.ac.nz\nremco@cs.auckland.ac.nz\n(c) 2010-2011\n\n" +
							"Citation:\n" + CITATION,
					"About Message", JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE, getIcon("DensiTree"), new String[]{"Copy citation to clipboard","Close"},"Close") == 0) {
			    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(CITATION), null);			
			}
		}
	}; // class ActionAbout

	Action a_labelwidth = new MyAction("Label width", "Label width when root at left", "labelwidth", "") {
		private static final long serialVersionUID = -2L;

		public void actionPerformed(ActionEvent ae) {
			String sLabeWidth = JOptionPane.showInputDialog("Labe Width:", m_nLabelWidth + "");
			if (sLabeWidth != null) {
				try {
					m_nLabelWidth = Integer.parseInt(sLabeWidth);
				} catch (Exception e) {
				}
				fitToScreen();
			}
		}
	}; // class ActionBurnin

	Action a_burnin = new MyAction("Burn in", "Burn in", "burnin", "") {
		private static final long serialVersionUID = -2L;

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

	Action a_geolinewidth = new MyAction("Geo line width", "Geographical line width", "geolinewidth", "") {
		private static final long serialVersionUID = -2L;

		public void actionPerformed(ActionEvent ae) {
			String sGeoWidth = JOptionPane.showInputDialog("Geographical line width:", m_nGeoWidth + "");
			if (sGeoWidth != null) {
				try {
					m_nGeoWidth = Integer.parseInt(sGeoWidth);
					m_Panel.clearImage();
					repaint();
				} catch (Exception e) {
				}
			}
		}
	}; // class ActionGeoWidth

	Action a_viewstatusbar = new MyAction("View statusbar", "View statusbar", "statusbar", "") {
		private static final long serialVersionUID = -20389330812354L;

		public void actionPerformed(ActionEvent ae) {
			m_jStatusBar.setVisible(!m_jStatusBar.isVisible());
		} // actionPerformed
	}; // class ActionViewStatusbar

	Action a_viewtoolbar = new MyAction("View toolbar", "View toolbar", "toolbar", "") {
		private static final long serialVersionUID = -20389110812354L;

		public void actionPerformed(ActionEvent ae) {
			m_jTbTools.setVisible(!m_jTbTools.isVisible());
		} // actionPerformed
	}; // class ActionViewToolbar

	Action a_viewtoolbar2 = new MyAction("View Sidebar", "View Sidebar", "sidebar", "") {
		private static final long serialVersionUID = -20389110812354L;

		public void actionPerformed(ActionEvent ae) {
			m_jTbTools2.setVisible(!m_jTbTools2.isVisible());
		} // actionPerformed
	}; // class ActionViewToolbar

	Action a_viewcladetoolbar = new MyAction("View clade toolbar", "View clade toolbar", "cladetoolbar", "") {
		private static final long serialVersionUID = -1;

		public void actionPerformed(ActionEvent ae) {
			m_jTbCladeTools.setVisible(!m_jTbCladeTools.isVisible());
			if (m_jTbCladeTools.isVisible()) {
				JSplitPane pane = (JSplitPane) m_Panel.getParent().getParent().getParent().getParent();
				int loc = pane.getDividerLocation();
				// Set a proportional location
				pane.setDividerLocation(0.8);
			}
			
		} // actionPerformed
	}; // class ActionViewToolbar

	Action a_zoomin = new MyAction("Zoom in", "Zoom in", "zoomin", "+") {
		private static final long serialVersionUID = -2038911085935515L;

		public void actionPerformed(ActionEvent ae) {
			m_fScale *= 1.2;
			a_zoomout.setEnabled(true);
			fitToScreen();
			m_jStatusBar.setText("Zooming in");
		}
	}; // class ActionZoomIn

	Action a_zoomout = new MyAction("Zoom out", "Zoom out", "zoomout", "-") {
		private static final long serialVersionUID = -203891108593551L;

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

	Action a_zoomintree = new MyAction("Zoom in height", "Zoom in tree height", "zoominh", "H") {
		private static final long serialVersionUID = -1;

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

	Action a_zoomouttree = new MyAction("Zoom out height", "Zoom out tree height", "zoomouth", "ctrl H") {
		private static final long serialVersionUID = -1;

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

	MyAction a_animateStart = new MyAction(" Start", "Start Animation", "start", "ctrl D") {
		private static final long serialVersionUID = -1L;

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

	Action a_drawtreeset = new MyAction("Draw Tree Set", "Draw Tree Set", "redraw", "R") {
		private static final long serialVersionUID = -4L;

		public void actionPerformed(ActionEvent ae) {
			setWaitCursor();
			System.err.println("MODS=" + ae.getModifiers());
			if (ae.getModifiers() == 18) {
				// when menu item is selected & ctrl key is pressed
				System.err.println("start recording: results in /tmp");
				m_nFrameNr = 0;
				m_bRecord = true;
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

	Action a_selectAll = new MyAction("Select All", "Select All", "selectall", "ctrl A") {
		private static final long serialVersionUID = 5L;

		public void actionPerformed(ActionEvent ae) {
			for (int i = 0; i < m_bSelection.length; i++) {
				m_bSelection[i] = true;
				// m_bSelection[i] = m_fLatitude.get(i)==0 &&
				// m_fLongitude.get(i)==0;
			}
			repaint();
		} // actionPerformed
	};// class ActionSelectAll
	Action a_unselectAll = new MyAction("Unselect All", "Unselect All", "unselectall", "ctrl U") {
		private static final long serialVersionUID = 5L;

		public void actionPerformed(ActionEvent ae) {
			for (int i = 0; i < m_bSelection.length; i++) {
				m_bSelection[i] = false;
			}
			repaint();
		} // actionPerformed
	};// class ActionUnSelectAll
	Action a_del = new MyAction("Delete", "Delete selected", "del", "Del") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent ae) {
			int nDeleted = 0;
			for (int i = m_bSelection.length - 1; i >= 0 && m_nNrOfLabels > 2; i--) {
				if (m_bSelection[i]) {
					// delete node with nr i
					for (int j = 0; j < m_trees.length; j++) {
						m_trees[j] = deleteLeaf(m_trees[j], i);
						renumber(m_trees[j], i);
						m_trees[j].labelInternalNodes(m_nNrOfLabels - 1);
					}
					for (int j = 0; j < m_cTrees.length; j++) {
						m_cTrees[j] = deleteLeaf(m_cTrees[j], i);
						renumber(m_cTrees[j], i);
						m_cTrees[j].labelInternalNodes(m_nNrOfLabels - 1);
					}
					m_sLabels.remove(i);
					m_nNrOfLabels--;
					if (m_fLongitude != null && m_fLongitude.size() > i) {
						m_fLongitude.remove(i);
						m_fLatitude.remove(i);
					}
					int[] nOrder = new int[m_nOrder.length - 1];
					int[] nRevOrder = new int[m_nRevOrder.length - 1];
					int k = 0;
					for (int j = 0; j < nOrder.length; j++) {
						if (m_nOrder[k] == i) {
							k++;
						}
						nOrder[j] = (m_nOrder[k] < i ? m_nOrder[k] : m_nOrder[k] - 1);
						k++;
					}
					k = 0;
					for (int j = 0; j < nRevOrder.length; j++) {
						if (m_nRevOrder[k] == i) {
							k++;
						}
						nRevOrder[j] = (m_nRevOrder[k] < i ? m_nRevOrder[k] : m_nRevOrder[k] - 1);
						k++;
					}
					m_nOrder = nOrder;
					m_nRevOrder = nRevOrder;
					nDeleted++;
				}
			}
			System.err.println("ORDER:" + Arrays.toString(m_nOrder));
			System.err.println("REVOR:" + Arrays.toString(m_nRevOrder));
			m_bSelection = new boolean[m_bSelection.length - nDeleted];
			for (int i = 0; i < m_bSelection.length; i++) {
				m_bSelection[i] = true;
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
			m_nOrder2 = m_nOrder.clone();
			m_nRevOrder2 = m_nRevOrder.clone();
			m_fPosX = new float[m_sLabels.size()];
			getPosition(m_trees[0], m_fPosX);
		}

		void doThisAction() {
			m_nOrder = m_nOrder2.clone();
			m_nRevOrder = m_nRevOrder2.clone();
			for (int i = 0; i < m_trees.length; i++) {
				setPosition(m_trees[i], m_fPosX);
				positionRest(m_trees[i]);
			}
			for (int i = 0; i < m_cTrees.length; i++) {
				setPosition(m_cTrees[i], m_fPosX);
				positionRest(m_cTrees[i]);
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

	Action a_undo = new MyAction("Undo", "Undo", "udno", "ctrl Z") {
		private static final long serialVersionUID = -4L;

		public void actionPerformed(ActionEvent ae) {
			if (m_iUndo > 0) {
				m_iUndo--;
				m_doActions.elementAt(m_iUndo - 1).doThisAction();
				repaint();
			}
		} // actionPerformed
	}; // class ActionUndo
	Action a_redo = new MyAction("Redo", "Redo", "reno", "ctrl Y") {
		private static final long serialVersionUID = -4L;

		public void actionPerformed(ActionEvent ae) {
			if (m_iUndo < m_doActions.size()) {
				m_iUndo++;
				m_doActions.elementAt(m_iUndo - 1).doThisAction();
				repaint();
			}
		} // actionPerformed
	}; // class ActionRedo

	Action a_moveup = new MyAction("Move labels up", "Move selected labels up", "moveup", "ctrl M") {
		private static final long serialVersionUID = -4L;

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

	Action a_movedown = new MyAction("Move labels down", "Move selected labels down", "movedown", "M") {
		private static final long serialVersionUID = -4L;

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

	Action a_browsefirst = new MyAction("Browse First", "Browse First", "browsefirst", "") {
		private static final long serialVersionUID = 5L;

		public void actionPerformed(ActionEvent ae) {
			m_viewMode = ViewMode.BROWSE;
			a_animateStart.setIcon("start");
			m_iAnimateTree = 0;
			repaint();
		} // actionPerformed
	}; // class ActionBrowseFirst

	Action a_browseprev = new MyAction("Browse Prev", "Browse Prev", "browseprev", "P") {
		private static final long serialVersionUID = 5L;

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

	Action a_browsenext = new MyAction("Browse Next", "Browse Next", "browsenext", "N") {
		private static final long serialVersionUID = 5L;

		public void actionPerformed(ActionEvent ae) {
			m_viewMode = ViewMode.BROWSE;
			a_animateStart.setIcon("start");
			m_iAnimateTree++;
			if (m_iAnimateTree == m_nTopologies) {
				m_iAnimateTree = m_nTopologies - 1;
			}
			repaint();
		} // actionPerformed
	}; // class ActionBrowseNext

	Action a_browselast = new MyAction("Browse Last", "Browse Last", "browselast", "") {
		private static final long serialVersionUID = 5L;

		public void actionPerformed(ActionEvent ae) {
			m_viewMode = ViewMode.BROWSE;
			a_animateStart.setIcon("start");
			m_iAnimateTree = m_nTopologies - 1;
			repaint();
		} // actionPerformed
	}; // class ActionBrowse

	Action a_setfont = new MyAction("Set Font", "Set Font", "font", "") {
		private static final long serialVersionUID = 5L;

		// @SuppressWarnings("deprecation")
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
			"F");
	SettingAction a_animationSpeedDown = new SettingAction("Animation Speed-", "Decrease Animation Speed",
			"aspeeddown", "ctrl F");
	SettingAction a_treeWidthUp = new SettingAction("Tree Width+", "Increase Width of Trees", "treewidthup", "ctrl V");
	SettingAction a_treeWidthDown = new SettingAction("Tree Width-", "Decrease Width of Trees", "treewidthdown", "V");
	SettingAction a_cTreeWidthUp = new SettingAction("Consensus Tree Width+", "Increase Width of Consensus Trees",
			"ctreewidthup", "ctrl W");
	SettingAction a_cTreeWidthDown = new SettingAction("Consensus Tree Width-", "Decrease Width of Consensus Trees",
			"ctreewidthdown", "W");
	SettingAction a_intensityUp = new SettingAction("Intensity+", "Increase Intensity of Trees", "intensityup",
			"ctrl I");
	SettingAction a_intensityDown = new SettingAction("Intensity-", "Decrease Intensity of Trees", "intensitydown", "I");
	SettingAction a_cIntensityUp = new SettingAction("Consensus Intensity+", "Increase Intensity of Consensus Trees",
			"cintensityup", "ctrl C");
	SettingAction a_cIntensityDown = new SettingAction("Consensus Intensity-", "Decrease Intensity of Consensus Trees",
			"cintensitydown", "C");
	SettingAction a_jitterUp = new SettingAction("Jitter+", "Increase Jitter on x-coordinate of Trees", "jitterup",
			"ctrl J");
	SettingAction a_jitterDown = new SettingAction("Jitter-", "Decrease Jitter on x-coordinate of Trees", "jitterdown",
			"J");
	SettingAction a_threadsUp = new SettingAction("Drawing Threads+", "Increase number of Drawing Threads",
			"threadsup", "ctrl T");
	SettingAction a_threadsDown = new SettingAction("Drawing Threads-", "Decrease number of Drawing Threads",
			"threadsdown", "T");
	SettingAction a_angleThresholdUp = new SettingAction("Angle Correction+",
			"Increase Threshold for angle correction", "angleup", "ctrl N");
	SettingAction a_a_angleThresholdDown = new SettingAction("Angle Correction-",
			"Decrease Threshold for angle correction", "angledpown", "N");


	public JCheckBoxMenuItem m_viewEditTree;
	public JCheckBoxMenuItem m_viewClades; 

	
	
	
	void makeToolbar() {
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
			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(0);
			}
		};


		Action action3 = new AbstractAction("", getIcon("modestar")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(1);
			}
		};

		Action action4 = new AbstractAction("", getIcon("modecentralised")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(2);
			}
		};

		Action action5 = new AbstractAction("", getIcon("modeanglecorrected")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(3);
			}
		};
		
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
		toolPanel.add(panel, gbc);
				
		Action action6 = new AbstractAction("", getIcon("stylestraight")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(0);
			}
		};
		Action action7 = new AbstractAction("", getIcon("styleblock")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(1);
			}
		};
		Action action8 = new AbstractAction("", getIcon("stylearced")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(2);
			}
		};
		Action action9 = new AbstractAction("", getIcon("stylesteep")) {
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
		toolPanel.add(panel, gbc);
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
		m_jTbTools2.add(toolPanel);
//		for (int i = 0; i < 100; i++) {
//			gbc.gridy++;
//			m_jTbTools2.add(Box.createVerticalGlue(), gbc);
//		}

		m_cladelist = new JList(m_cladelistmodel);
		m_cladelist.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (m_bAllowCladeSelection) {
	 				m_cladeSelection.clear();
					for (int i : m_cladelist.getSelectedIndices()) {
						if (m_cladeWeight.get(i) > 0.01 && ((m_Xmode == 1 && m_clades.get(i).length > 1) || (m_Xmode == 2 && m_clades.get(i).length == 1))) {
							m_cladeSelection.add(i);
						}
					}
					System.err.println(Arrays.toString(m_cladelist.getSelectedValues()));
					repaint();
				}
			}
		});
		JScrollPane scrollingList = new JScrollPane(m_cladelist);
		scrollingList.setPreferredSize(new Dimension(600,30));
		scrollingList.setMinimumSize(scrollingList.getPreferredSize());
		m_jTbCladeTools.add(scrollingList);
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

	protected void makeMenuBar() {
		m_menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		// ----------------------------------------------------------------------
		// File menu */
		m_menuBar.add(fileMenu);
		fileMenu.add(a_new);
		fileMenu.add(a_load);
		fileMenu.add(a_saveas);
		fileMenu.add(a_loadimage);

		fileMenu.addSeparator();
		fileMenu.add(a_print);
		// fileMenu.add(a_export);
		fileMenu.add(a_exportVector);
		fileMenu.addSeparator();
		fileMenu.add(a_quit);

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

		m_viewEditTree = new JCheckBoxMenuItem("Show Edit Tree", m_bViewEditTree);
		m_viewEditTree.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean bPrev = m_bViewEditTree;
				m_bViewEditTree = m_viewEditTree.getState();
				if (bPrev != m_bViewEditTree) {
					makeDirty();
				}
			}
		});
		editMenu.add(m_viewEditTree);

		m_viewClades = new JCheckBoxMenuItem("Show Clades", m_bViewClades);
		m_viewClades.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean bPrev = m_bViewClades;
				m_bViewClades = m_viewClades.getState();
				if (bPrev != m_bViewClades) {
					makeDirty();
				}
			}
		});
		m_viewClades.setEnabled(false);
		editMenu.add(m_viewClades);

		JMenu shuffleMenu = new JMenu("Shuffle");
		shuffleMenu
				.add(new ShuffleAction("Most Frequent", "Use most frequent tree order", "", "", NodeOrderer.DEFAULT));
		shuffleMenu.add(new ShuffleAction("Closest Outside First", "Order closest to outside leaf first", "", "S",
				NodeOrderer.CLOSEST_OUTSIDE_FIRST));
		shuffleMenu.add(new ShuffleAction("Optimised root canal tree",
				"Use root canal tree, then optimise", "", "O", NodeOrderer.OPTIMISE));
		shuffleMenu.add(new ShuffleAction("Closest First", "Order closest leaf first", "", "ctrl 1",
				NodeOrderer.CLOSEST_FIRST));
		shuffleMenu.add(new ShuffleAction("Single link", "Single link hierarchical clusterer", "", "ctrl 2",
				NodeOrderer.SINGLE));
		shuffleMenu.add(new ShuffleAction("Complete link", "Complete link hierarchical clusterer", "", "ctrl 3",
				NodeOrderer.COMPLETE));
		shuffleMenu.add(new ShuffleAction("Average link", "Average link hierarchical clusterer", "", "ctrl 4",
				NodeOrderer.AVERAGE));
		shuffleMenu.add(new ShuffleAction("Mean link", "Mean link hierarchical clusterer", "", "ctrl 5", NodeOrderer.MEAN));
		shuffleMenu.add(new ShuffleAction("Adjusted complete link", "Adjusted complete link hierarchical clusterer",
				"", "6", NodeOrderer.ADJCOMLPETE));
		// RRB: not for public release
		shuffleMenu.addSeparator();
		shuffleMenu.add(new ShuffleAction("Manual", "Manual", "", "", NodeOrderer.MANUAL));
		shuffleMenu.add(new ShuffleAction("By Geography", "By Geography", "", "", NodeOrderer.GEOINFO));
		shuffleMenu.add(new ShuffleAction("By meta data, all", "By meta data, show all paths", "", "ctrl 7",
				NodeOrderer.META_ALL));
		shuffleMenu.add(new ShuffleAction("By meta data, sum", "By meta data, sum over paths", "", "ctrl 8",
				NodeOrderer.META_SUM));
		shuffleMenu.add(new ShuffleAction("By meta data, mean", "By meta data, average over paths", "", "ctrl 9",
				NodeOrderer.META_AVERAGE));

		editMenu.addSeparator();
		editMenu.add(shuffleMenu);

		// ----------------------------------------------------------------------
		// Draw all menu */
		JMenu drawallMenu = new JMenu("Draw All");
		drawallMenu.setMnemonic('D');
		m_menuBar.add(drawallMenu);
		final JCheckBoxMenuItem autoRefresh = new JCheckBoxMenuItem("Automatically refresh", m_bAutoRefresh);
		autoRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				m_bAutoRefresh = autoRefresh.getState();
				if (m_bAutoRefresh && m_bIsDirty) {
					fitToScreen();
					// makeDirty();
				}
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
		animateOverWrite.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				m_bAnimateOverwrite = animateOverWrite.getState();
			}
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
		helpMenu.add(a_about);
	} // makeMenuBar

	public static DensiTree startNew(String [] args) {
		DensiTree a = new DensiTree(args);
		
		try {
			String laff = UIManager.getSystemLookAndFeelClassName();
			laff = "javax.swing.plaf.metal.MetalLookAndFeel";
			System.setProperty("swing.metalTheme", "steel");
			UIManager.setLookAndFeel(laff);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		JFrame f;
		f = new JFrame(FRAME_TITLE);
		a.frame = f;
		f.setVisible(true);
		JMenuBar menuBar = a.getMenuBar();
		f.setJMenuBar(menuBar);
		f.add(a.m_jTbTools, BorderLayout.NORTH);
		f.add(a.m_jTbTools2, BorderLayout.EAST);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, a, a.m_jTbCladeTools);
		splitPane.setDividerLocation(0.9);

//		JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, a.m_jTbTools2, splitPane);
		
		f.add(splitPane, BorderLayout.CENTER);
		f.add(a.m_jStatusBar, BorderLayout.SOUTH);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
		startNew(args);
	}
	
} // class DensiTree
