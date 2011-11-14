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
 * @version $Revision: 2.01 $
 */
// the magic sentence to look for when releasing:
//RRB: not for public release

//TODO: truncate % of root
// log scale
//DONE  integrate DensiTreeS with DensiTree
//DONE suppress ill documented menu items
//DONE make root canal use clade heights
//DONE visibility of clades/edit list
//DONE make upgma default ordering
//DONE set root canal color


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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import viz.graphics.*;

public class DensiTree extends JPanel implements ComponentListener {
	final static String VERSION = "2.01";
	final static String FRAME_TITLE = "DensiTree - Tree Set Visualizer";
	final static String CITATION = "Remco R. Bouckaert\n"+
		"DensiTree: making sense of sets of phylogenetic trees\n"+
		"Bioinformatics (2010) 26 (10): 1372-1373.\n"+
		"doi: 10.1093/bioinformatics/btq110";

	private static final long serialVersionUID = 1L;
	/** path for icons */
	public static final String ICONPATH = "viz/icons/";

	/**
	 * default tree branch length, used when that info is not in the Newick tree
	 **/
	final static double DEFAULT_LENGTH = 0.001f;
	/** same trees, but represented as Node data structure **/
	Node[] m_trees;
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
	float[][] m_fLineWidth;
	float[][] m_fCLineWidth;
	float[][] m_fTopLineWidth;
	float[][] m_fTopCLineWidth;

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

	Node m_rootcanaltree;
	float[][] m_fRLinesX;
	float[][] m_fRLinesY;

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
	int m_nGeoWidth = 1;
	/** width of labels, when root at left **/
	int m_nLabelWidth = 100;

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

	/** number of threads used for drawing **/
	int m_nDrawThreads = 2;
	/** the set of actual threads **/
	Thread[] m_drawThread;
	/**
	 * burn in = nr of trees ignored at the start of tree file, can be set by
	 * command line option
	 **/
	int m_nBurnIn = 0;

	double m_w = 0;
	/** current directory for opening files **/
	String m_sDir = System.getProperty("user.dir");

	/** array of various colors for color coding different topologies **/
	Color[] m_color;
	int HEIGHTCOLOR = 6, CONSCOLOR = 4, LABELCOLOR = 5, BGCOLOR = 7, GEOCOLOR = 8, ROOTCANALCOLOR=9;
	/** image used for the background **/
	private BufferedImage m_bgImage;
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
	Pattern m_pattern;
	/** default regular expression **/
	//final static String DEFAULT_PATTERN = "theta=([0-9\\.Ee-]+)";
	// final static String DEFAULT_PATTERN = "([0-9\\.Ee-]+),([0-9\\.Ee-]+)";
	final static String DEFAULT_PATTERN = "([0-9\\.Ee-]+),";
	// final static String DEFAULT_PATTERN = "s=([0-9\\.Ee-]+)";
	// final static String DEFAULT_PATTERN = "([0-9\\.Ee-]+),y=([0-9\\.Ee-]+)";
	/** string containing reg exp for position matching **/
	String m_sPattern = DEFAULT_PATTERN;
	int m_iPatternForBottom = 0;
	int m_iPatternForTop = -1;

	/** string containing reg exp for grouping taxa **/
	String m_sColorPattern = null;
	/** index of color for a taxon **/
	int[] m_iColor;

	/** flag to indicate that single child nodes are allowed **/
	boolean m_bAllowSingleChild = false;

	/** flag to indicate that text should be rotated when root at top **/
	boolean m_bRotateTextWhenRootAtTop = false;

	/**
	 * mode for determining the X location of an internal node 0 = centre of
	 * nodes below 1 = centre of all taxa below
	 */
	int m_Xmode = 0;
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
	String m_sFileName;
	
	/** flag to indicate some meta data on the tree should be used for line widht **/
	boolean m_bMetaDataForLineWidth = false;
	/**
	 * Flag to indicate top of branch widths should be calculated from the bottom
	 * of branch lengths by distributing weight proportional to left and right
	 * bottom branches to top branch -- scaled to fit bottom of parent branch.
	 */
	boolean m_bCorrectTopOfBranch = false;
	/** indicator that only one group is in the pattern, so top of branch widths
	 * should be calculated from the bottom of branch information.
	 */
	boolean m_bGroupOverflow;	
	
	/** constructors **/
	public DensiTree() {
	}

	public DensiTree(String[] args) {
		System.out.println(banner());
		m_bSelection = new boolean[0];
		m_nRevOrder = new int[0];
		m_cTrees = new Node[0];
		m_trees = new Node[0];
		initColors();

		setSize(1000, 800);
		m_Panel = new TreeSetPanel();
		parseArgs(args);
		m_pattern = createPattern();
		System.err.println(getSize().width + "x" + getSize().height);

		m_drawThread = new Thread[2];
		m_jScrollPane = new JScrollPane(m_Panel);
		makeToolbar();
		makeMenuBar();
		addComponentListener(this);
		this.setLayout(new BorderLayout());
		this.add(m_jScrollPane, BorderLayout.CENTER);
//		m_ctrlPanel = new ControlPanel(this);
//		this.add(m_ctrlPanel, BorderLayout.EAST);

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

	private Pattern createPattern() {
		m_sPattern = "";
		for (int i = 0; i < m_iPatternForBottom; i++) {
			m_sPattern += "[0-9\\.Ee-]+[^0-9]+";
		}
		m_sPattern += "([0-9\\.Ee-]+)";
		if (m_iPatternForTop > m_iPatternForBottom) {
			m_sPattern += "[^0-9]+";
			for (int i = m_iPatternForBottom + 1; i < m_iPatternForTop; i++) {
				m_sPattern += "[0-9\\.Ee-]+[^0-9]+";
			}
			m_sPattern += "([0-9\\.Ee-]+)";
		}
		return Pattern.compile(m_sPattern);
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
	// float [] _posX;
	void parseArgs(String[] args) {
		// String s =
		// "0.0 0.0644496624519727 0.12299647366370955 0.1828288771357618 0.24316465205713314 0.3028449582299141 0.42152474393619127 12.3484018687757 12.759547734427285 12.954877607260375 13.230334083463442 13.429915461276531 13.971822041254056 14.027609363472253 14.378202179942885 14.573394447865825 14.883272119141479 15.706679247293694 20.06935659213848 20.4884115559945 21.791709181174685 21.81735939822871 21.90854930926544 22.32272627756748 25.41919254544952 25.58907861292301 25.61961045037131 25.64894722757623 25.790188196949746 25.885354383260974 25.978148567327818 26.101344212017736 26.185331217457495 26.30434055572244 27.436803291652968 27.622851571370656 27.786418338375235 27.998656423704908 28.1890612221116 31.05949918936486 31.0903750554418 31.118216207430315 31.37773862184774 31.53781800317268 31.61654313086494 31.8186486031974 31.976062650728036 32.1076454110957 32.245417100342735 32.41364820267639 36.924327658783966 37.15009883265346 37.25582133523053 41.07961235620318 41.21854289228307 41.233788893780805 41.33591174666764 41.48919742389697 49.60963720518882 49.92539961497576 50.34285755376211 50.38418471730719 57.49225747193808 57.506750222943865 65.0679117036763 65.08585925184093 65.16616692334262 65.18065967726604 66.59015420277673 66.62455729165168 66.64617329575445 66.89485330403363 66.92126039074965 68.88226901829195 68.89676177188156 69.03964685094547 69.07581717168495 69.1024089483164 119.94775800960682 120.18097473681449 120.4024907816688 120.60068516332306 120.79182872371103 121.0034946304536 121.1965502748277 121.3864806742199 121.59708913864887 121.79647133214245 121.9939776519638 122.18833699453252 122.38530010766122 122.58324099319238 122.80380462558588 122.98492421035952 123.17830940143261 123.39499815291641 123.60840594844468 123.81513591736105 124.0074491812512 128.18457075558996 128.3692356650195 128.54248961828273 128.68918800875954 128.82993647565806 128.97564623003794 129.10998485839428 129.2304789597451 129.44206156988903 129.57809138157216 129.74273008114125 132.98851661889998 133.0151083953181 133.06612998624703 195.55173137217838 195.60526619719994 195.98648500769107 196.00260738091475 196.07478143846288 196.08927419175797 196.1838174995088 196.5417580974765 196.55786236373194 196.6205085764241 199.35983456290018 199.5330848945147 199.62915642291463 199.80924029376996 199.82587690311593 199.9415219789236 200.10731227287508 200.1332847936744 200.16139754928815 200.66806450469502 200.95405918846848 201.325087442503 201.39471205044535 201.43184203615462 201.45095566774376";
		// String [] s2 = s.split(" ");
		// _posX = new float[s2.length];
		// double fMax = 0;
		// for (int i = 0; i < s2.length; i++) {
		// _posX[i] = Float.parseFloat(s2[i]);
		// fMax = Math.max(fMax, _posX[i]);
		// }
		// for (int i = 0; i < s2.length; i++) {
		// _posX[i] *= s2.length / fMax;
		// }

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
						m_nDrawThreads = (int) Float.parseFloat(args[i + 1]);
						if (m_nDrawThreads < 1) {
							m_nDrawThreads = 1;
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
							m_nGridMode = GridMode.NONE;
						} else if (sMode.equals("short")) {
							m_nGridMode = GridMode.SHORT;
						} else if (sMode.equals("full")) {
							m_nGridMode = GridMode.FULL;
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
				+ m_fHeight + "\n" + "Zoom: " + m_fScale + "\n" + "Number of drawing threads: " + m_nDrawThreads + "\n"
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
	void init(String sFile) throws Exception {
		if (m_Panel != null) {
			m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		}
		if (m_jStatusBar != null) {
			m_jStatusBar.setText("Initializing...");
			m_jStatusBar.repaint();
		}
		m_sFileName = sFile;
		m_bInitializing = true;
		m_viewMode = ViewMode.DRAW;
		a_animateStart.setIcon("start");

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
		m_drawThread = new Thread[m_nDrawThreads];

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
			calcClades();

			reshuffle((m_bAllowSingleChild ? NodeOrderer.DEFAULT: NodeOrderer.OPTIMISE));
			
			// calculate y-position for tree set
			calcPositions();

		} catch (OutOfMemoryError e) {
			clear();
			JOptionPane.showMessageDialog(null, "Not enough memory is reserved for java to process this tree. "
					+ "Try starting DensiTree with more memory\n\n(for example "
					+ "use:\njava -Xmx3g DensiTree.jar\nfrom " + "the command line) where DensiTree is in the path");
			m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			clear();
			m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
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

	/** represent clade as arrays of leaf indices **/
	List<int[]> m_clades;
	/** proportion of trees containing the clade **/
	List<Double> m_cladeWeight;
	/** average height of a clade **/
	List<Double> m_cladeHeight;
	/** UI component for manipulating clade selection **/
	JList m_cladelist;
	DefaultListModel m_cladelistmodel = new DefaultListModel();

	List<List<ChildClade>> m_cladeChildren;
	/** X-position of the clade **/
	float[] m_cladePosition;
	/** index of consensus tree with highest product of clade probabilities **/
	//int m_iMaxCladeProbTopology;
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
		m_cladeChildren = new ArrayList<List<ChildClade>>();
		Map<String, Integer> mapCladeToIndex = new HashMap<String, Integer>();

		// add leafs as clades
		for (int i = 0; i < m_nNrOfLabels; i++) {
			int[] clade = new int[1];
			clade[0] = i;
			m_clades.add(clade);
			m_cladeWeight.add(1.0);
			m_cladeHeight.add(0.0);
			m_cladeChildren.add(new ArrayList<ChildClade>());
			mapCladeToIndex.put(Arrays.toString(clade), mapCladeToIndex.size());
		}

		// collect clades
		for (int i = 0; i < m_cTrees.length; i++) {
			calcCladeForNode(m_cTrees[i], mapCladeToIndex, m_fTreeWeight[i], m_cTrees[i].m_fPosY);
		}

		// normalise clade heights, so m_cladeHeight represent average clade
		// height
		for (int i = 0; i < m_cladeHeight.size(); i++) {
			m_cladeHeight.set(i, m_cladeHeight.get(i) / m_cladeWeight.get(i));
		}
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
		List<List<ChildClade>> cladeChildren = new ArrayList<List<ChildClade>>();
		for (int i = 0; i < m_cladePosition.length; i++) {
			clades.add(m_clades.get(index[i]));
			cladeWeight.add(m_cladeWeight.get(index[i]));
			cladeHeight.add(m_cladeHeight.get(index[i]));
			cladeChildren.add(m_cladeChildren.get(index[i]));
		}
		m_clades = clades;
		m_cladeWeight = cladeWeight;
		m_cladeHeight = cladeHeight;
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
		double fMaxCladeProb = cladeProb(m_cTrees[0]);
		for (int i = 0; i < m_cTrees.length; i++) {
			double fCladeProb = cladeProb(m_cTrees[i]);
			if (fCladeProb > fMaxCladeProb) {
				iMaxCladeProbTopology = i;
				fMaxCladeProb = fCladeProb;
			}
		}
		
		m_rootcanaltree = m_cTrees[iMaxCladeProbTopology].copy();
		setHeightByClade(m_rootcanaltree);
		m_rootcanaltree.m_fLength = (float) (m_fHeight - m_cladeHeight.get(m_rootcanaltree.m_iClade));
		float fHeight = positionHeight(m_rootcanaltree, 0);
		offsetHeight(m_rootcanaltree, m_fHeight - fHeight);
		
		// add clades to GUI component
		m_cladelistmodel.clear();
		List<String> list = cladesToString();
		for (int i = 0; i < list.size(); i++) {
			m_cladelistmodel.add(i, list.get(i));
		}
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
				sStr += (format.format(m_cladeWeight.get(i) * 100) + "% [");
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
	
	private double cladeProb(Node node) {
		if (node.isLeaf()) {
			return 1.0;
		} else {
			double fCladeProb = m_cladeWeight.get(node.m_iClade);
			fCladeProb *= cladeProb(node.m_left);
			fCladeProb *= cladeProb(node.m_right);
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
			return clade;
		} else {
			int[] cladeLeft = calcCladeForNode(node.m_left, mapCladeToIndex, fWeight, fHeight + node.m_left.m_fLength);
			int[] cladeRight = calcCladeForNode(node.m_right, mapCladeToIndex, fWeight, fHeight
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
			if (!mapCladeToIndex.containsKey(sClade)) {
				mapCladeToIndex.put(sClade, mapCladeToIndex.size());
				m_clades.add(clade);
				m_cladeWeight.add(0.0);
				m_cladeHeight.add(0.0);
				m_cladeChildren.add(new ArrayList<ChildClade>());
			}
			int iClade = mapCladeToIndex.get(sClade);
			m_cladeWeight.set(iClade, m_cladeWeight.get(iClade) + fWeight);
			m_cladeHeight.set(iClade, m_cladeHeight.get(iClade) + fWeight * fHeight);
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
		m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
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
					m_fLinesX[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
					m_fLinesY[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
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
					m_fCLinesX[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
					m_fCLinesY[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
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
			iPos = getRotationLeafs(node.m_right, iPos, iLeafsR, iRotationPoint);
			if (iPos == iRotationPoint) {
				iLeafs.removeAllElements();
				iLeafs.addAll(iLeafsR);
				return iPos;
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
	void calcLines() {
		if (m_bMetaDataForLineWidth) {
			calcLinesWithMetaData();
			return;
		}
		checkSelection();
		if (m_trees.length == 0) {
			return;
		}
		// calculate coordinates of lines for drawing trees
		int nNodes = getNrOfNodes(m_trees[0]);

		boolean[] b = new boolean[1];
		for (int i = 0; i < m_trees.length; i++) {
			// m_fLinesX[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			// m_fLinesY[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			m_fLinesX[i] = new float[nNodes * 2 + 2];
			m_fLinesY[i] = new float[nNodes * 2 + 2];
			if (m_bAllowSingleChild) {
				m_trees[i].drawDryWithSingleChild(m_fLinesX[i], m_fLinesY[i], 0, b, m_bSelection, m_fTreeOffset,
						m_fTreeScale);
			} else {
				calcLinesForNode(m_trees[i], m_fLinesX[i], m_fLinesY[i]);
//				switch (m_Xmode) {
//				case 0:
//					m_trees[i].drawDry(m_fLinesX[i], m_fLinesY[i], 0, b, m_bSelection, m_fTreeOffset, m_fTreeScale);
//					break;
//				case 1:
//					m_trees[i].drawDryCentralised(m_fLinesX[i], m_fLinesY[i], 0, b, m_bSelection, m_fTreeOffset,
//							m_fTreeScale, new float[2], new float[m_nNrOfLabels * 2 - 1],
//							new float[m_nNrOfLabels * 2 - 1], m_cladePosition);
//					break;
//				case 2:
//					float[] fCladeCenterX = new float[m_nNrOfLabels * 2 - 1];
//					float[] fCladeCenterY = new float[m_nNrOfLabels * 2 - 1];
//					float[] fPosX = new float[m_nNrOfLabels * 2 - 1];
//					float[] fPosY = new float[m_nNrOfLabels * 2 - 1];
//					m_trees[i].getStarTreeCladeCenters(fCladeCenterX, fCladeCenterY, m_fTreeOffset, m_fTreeScale, m_cladePosition, m_sLabels.size());
//					m_trees[i].drawStarTree(m_fLinesX[i], m_fLinesY[i], fPosX, fPosY, fCladeCenterX, fCladeCenterY,
//							m_bSelection, m_fTreeOffset, m_fTreeScale);
//					break;
//				}
			}
		}
		// calculate coordinates of lines for drawing consensus trees
		for (int i = 0; i < m_cTrees.length; i++) {
			// m_fCLinesX[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			// m_fCLinesY[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			m_fCLinesX[i] = new float[nNodes * 2 + 2];
			m_fCLinesY[i] = new float[nNodes * 2 + 2];
			if (m_bAllowSingleChild) {
				m_cTrees[i].drawDryWithSingleChild(m_fCLinesX[i], m_fCLinesY[i], 0, b, m_bSelection, m_fTreeOffset,
						m_fTreeScale);
			} else {
				calcLinesForNode(m_cTrees[i], m_fCLinesX[i], m_fCLinesY[i]);
//			
//				switch (m_Xmode) {
//				case 0:
//					m_cTrees[i].drawDry(m_fCLinesX[i], m_fCLinesY[i], 0, b, m_bSelection, m_fTreeOffset, m_fTreeScale);
//					break;
//				case 1:
//					m_cTrees[i].drawDryCentralised(m_fCLinesX[i], m_fCLinesY[i], 0, b, m_bSelection, m_fTreeOffset,
//							m_fTreeScale, new float[2], new float[m_nNrOfLabels * 2 - 1],
//							new float[m_nNrOfLabels * 2 - 1], m_cladePosition);
//					break;
//				case 2:
//					float[] fCladeCenterX = new float[m_nNrOfLabels * 2 - 1];
//					float[] fCladeCenterY = new float[m_nNrOfLabels * 2 - 1];
//					float[] fPosX = new float[m_nNrOfLabels * 2 - 1];
//					float[] fPosY = new float[m_nNrOfLabels * 2 - 1];
//					m_cTrees[i].getStarTreeCladeCenters(fCladeCenterX, fCladeCenterY, m_fTreeOffset, m_fTreeScale, m_cladePosition);
//					m_cTrees[i].drawStarTree(m_fCLinesX[i], m_fCLinesY[i], fPosX, fPosY, fCladeCenterX, fCladeCenterY,
//							m_bSelection, m_fTreeOffset, m_fTreeScale);
//					break;
//				}
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

	void calcLinesWithMetaData() {
		m_fLineWidth = new float[m_trees.length][];
		m_fCLineWidth = new float[m_cTrees.length][];
		m_fTopLineWidth = new float[m_trees.length][];
		m_fTopCLineWidth = new float[m_cTrees.length][];
		checkSelection();
		m_bGroupOverflow = false;

		// calculate coordinates of lines for drawing trees
		boolean[] b = new boolean[1];
		for (int i = 0; i < m_trees.length; i++) {
			m_fLinesX[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			m_fLinesY[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			m_fLineWidth[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			m_fTopLineWidth[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			drawTreeS(m_trees[i], m_fLinesX[i], m_fLinesY[i], m_fLineWidth[i], m_fTopLineWidth[i], 0, b);
		}
		// calculate coordinates of lines for drawing consensus trees
		for (int i = 0; i < m_cTrees.length; i++) {
			m_fCLinesX[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			m_fCLinesY[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			m_fCLineWidth[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			m_fTopCLineWidth[i] = new float[(m_sLabels.size() - 1) * 4 + 2];
			drawTreeS(m_cTrees[i], m_fCLinesX[i], m_fCLinesY[i], m_fCLineWidth[i], m_fTopCLineWidth[i], 0, b);
			int nTopologies = 0;
			float [] fCLineWidth = new float[(m_sLabels.size() - 1) * 4 + 2];
			float [] fTopCLineWidth = new float[(m_sLabels.size() - 1) * 4 + 2];
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
	} // calcLines


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
			iPos = drawTreeS(node.m_right, nX, nY, fWidth, fWidthTop, iPos, bNeedsDrawing);
			bChildNeedsDrawing[1] = bNeedsDrawing[0];
			bNeedsDrawing[0] = false;
				if (bChildNeedsDrawing[0]) {
					nX[iPos] = node.m_left.m_fPosX;
					nY[iPos] = node.m_left.m_fPosY;
					fWidth[iPos] = getGamma(node.m_left.m_sMetaData, 1);
					fWidthTop[iPos] = getGamma(node.m_left.m_sMetaData, 2);
					iPos++;
					nX[iPos] = nX[iPos - 1];
					nY[iPos] = node.m_fPosY;
					bNeedsDrawing[0] = true;
				} else {
					fWidth[iPos] = m_nTreeWidth;
					nX[iPos] = node.m_fPosX;
					nY[iPos] = node.m_fPosY;
					iPos++;
					nX[iPos] = node.m_fPosX;
					nY[iPos] = node.m_fPosY;
				}
				fWidth[iPos] = 0;
				iPos++;
				if (bChildNeedsDrawing[1]) {
					nX[iPos] = node.m_right.m_fPosX;
					nY[iPos] = nY[iPos - 1];
					fWidth[iPos] = getGamma(node.m_right.m_sMetaData, 1);
					fWidthTop[iPos] = getGamma(node.m_right.m_sMetaData, 2);
					iPos++;
					nX[iPos] = nX[iPos - 1];
					nY[iPos] = node.m_right.m_fPosY;
					bNeedsDrawing[0] = true;
				} else {
					nX[iPos] = node.m_fPosX;
					nY[iPos] = node.m_fPosY;
					fWidth[iPos] = m_nTreeWidth;
					iPos++;
					nX[iPos] = node.m_fPosX;
					nY[iPos] = node.m_fPosY;
				}
				iPos++;
				if (m_bGroupOverflow && m_bCorrectTopOfBranch) {
					float fCurrentWidth = getGamma(node.m_sMetaData, 1);
					float fSumWidth = fWidth[iPos-2] + fWidth[iPos-4];
					fWidthTop[iPos-2] = fCurrentWidth * fWidth[iPos-2]/fSumWidth;
					fWidthTop[iPos-4] = fCurrentWidth * fWidth[iPos-4]/fSumWidth;
				}

				
			if (node.isRoot()) {
				nX[iPos] = node.m_fPosX;
				nY[iPos] = node.m_fPosY;
				fWidth[iPos] = getGamma(node.m_sMetaData, 1);
				fWidthTop[iPos] = getGamma(node.m_sMetaData, 1);
				iPos++;
				nX[iPos] = node.m_fPosX;
				nY[iPos] = node.m_fPosY - node.m_fLength;
				iPos++;
			}
		}
		return iPos;
	}

	float getGamma(String sMetaData, int nGroup) {
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
			positionLeafs(m_rootcanaltree);
			positionRest(m_rootcanaltree);
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
			Matcher matcher = m_pattern.matcher(node.m_sMetaData);
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
					int y = getPosY((node.m_fPosY + m_fLabelIndent - m_fTreeOffset) * m_fTreeScale) + 2;// ;
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
					int y = getPosY((node.m_fPosY + m_fLabelIndent - m_fTreeOffset) * m_fTreeScale)
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
				int x = getPosX((node.m_fPosY + m_fLabelIndent - m_fTreeOffset) * m_fTreeScale) + 1;
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

	/** image in memory containing tree set drawing **/
	private BufferedImageF m_image;
	/** object that draws a single tree on an image **/
	TreeDrawer m_treeDrawer = new TreeDrawer();

	
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
		Enumeration<AbstractButton> enumeration = m_modeGroup.getElements();
		for (int i = 0; i < nXmode; i++) {
			enumeration.nextElement();
		}
		m_modeGroup.setSelected(enumeration.nextElement().getModel(), true);

		calcPositions();
		calcLines();
		makeDirty();
	}
 
	
	void setStyle(int nStyle) {
		BranchDrawer bd = null;
		switch (nStyle) {
		case 0:
			if (m_bMetaDataForLineWidth) {
				bd = new TrapeziumBranchDrawer();
			} else {
				bd = new BranchDrawer();
			}
			m_treeDrawer.m_bViewBlockTree = false;
			break;
		case 1:
			if (m_bMetaDataForLineWidth) {
				bd = new TrapeziumBranchDrawer();
			} else {
				bd = new BranchDrawer();
			}
			m_treeDrawer.m_bViewBlockTree = true;
			break;
		case 2:
			if (!m_bMetaDataForLineWidth) {
				bd = new ArcBranchDrawer();
				m_treeDrawer.m_bViewBlockTree = false;
			}
			break;
		// case 2: bd = new KoruBranchDrawer();break;
		// case 3: bd = new TrapeziumBranchDrawer();break;
		// case 3: bd = new BrownianBridgeBranchDrawer();break;
		case 3:
			if (!m_bMetaDataForLineWidth) {
				bd = new SteepArcBranchDrawer();
				m_treeDrawer.m_bViewBlockTree = false;
			}
			break;
		}
		if (bd != null) {
			m_treeDrawer.setBranchDrawer(bd);
			makeDirty();
			Enumeration<AbstractButton> enumeration = m_styleGroup.getElements();
			for (int i = 0; i < nStyle; i++) {
				enumeration.nextElement();
			}
			m_styleGroup.setSelected(enumeration.nextElement().getModel(), true);
		}
	}
	
	
//	public class ControlPanel extends JPanel {
//		private static final long serialVersionUID = 1L;
//		DensiTree m_densiTree;
//		
//		ControlPanel(DensiTree densiTree) {
//			m_densiTree = densiTree;
//			Box box = Box.createVerticalBox();
//			box.add(createModeBox());
//			box.add(createLineBox());
//			box.add(createCTreeSetBox());
//			box.add(createTreeSetBox());
//			this.add(box);
//		}
//
//		private Component createModeBox() {
//			Box box = Box.createVerticalBox();
//			JButton defaultButton = new JButton(getIcon("modedefault"));
//			defaultButton.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					selectMode(0);
//				}
//			});
//			box.add(defaultButton);
//
//
//			JButton blockStarTree = new JButton(getIcon("modestar"));
//			blockStarTree.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					selectMode(1);
//				}
//			});
//			box.add(blockStarTree);
//
//			JButton modeCentralised = new JButton(getIcon("modecentralised"));
//			modeCentralised.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					selectMode(2);
//				}
//			});
//			box.add(modeCentralised);
//
//			JButton modeCentralisedAngleCorrected = new JButton(getIcon("modeanglecorrected"));
//			modeCentralisedAngleCorrected.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					selectMode(3);
//				}
//			});
//			box.add(modeCentralisedAngleCorrected);
//			return box;
//		}
//
//		private Component createLineBox() {
//			Box box = Box.createVerticalBox();
//
//			JButton straightButton = new JButton(getIcon("stylestraight"));
//			straightButton.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					if (m_bMetaDataForLineWidth) {
//						m_treeDrawer.setBranchDrawer(new TrapeziumBranchDrawer());
//					} else {
//						m_treeDrawer.setBranchDrawer(new BranchDrawer());
//					}
//					makeDirty();
//				}
//			});
//			box.add(straightButton);
//
//			JButton arcedButton = new JButton(getIcon("stylearced"));
//			arcedButton.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					m_treeDrawer.setBranchDrawer(new ArcBranchDrawer());
//					makeDirty();
//				}
//			});
//			box.add(arcedButton);
//
//			JButton steepButton = new JButton(getIcon("stylesteep"));
//			steepButton.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					m_treeDrawer.setBranchDrawer(new SteepArcBranchDrawer());
//					makeDirty();
//				}
//			});
//			box.add(steepButton);
//			return box;
//		}
//
//		private Component createCTreeSetBox() {
//			Box box = Box.createHorizontalBox();
//			return box;
//		}
//
//		private Component createTreeSetBox() {
//			Box box = Box.createHorizontalBox();
//			return box;
//		}
//
//		
//	} // class ControlPanel
	
	Icon getIcon(String sIcon) {
		java.net.URL tempURL = ClassLoader.getSystemResource(ICONPATH + sIcon + ".png");
		if (tempURL != null) {
			return new ImageIcon(tempURL);
		}
		return null;
	}

	/**
	 * Class for drawing the tree set It uses buffer to do the actual tree
	 * drawing, then allows inspection of the image through scrolling.
	 */
	public class TreeSetPanel extends JPanel implements MouseListener, Printable, MouseMotionListener {
		private static final long serialVersionUID = 1L;

		/** constructor **/
		public TreeSetPanel() {
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		/** stop drawing thread, if any is running */
		void stopDrawThreads() {
			try {
				for (int i = 0; i < m_nDrawThreads; i++) {
					if (m_drawThread[i] != null) {
						((DrawThread) m_drawThread[i]).m_bStop = true;
					}
				}
				for (int i = 0; i < m_nDrawThreads; i++) {
					if (m_drawThread[i] != null) {
						m_drawThread[i].join();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} // stopDrawThreads

		/** reset image so that it will be redrawn on the next occasion */
		void clearImage() {
			m_image = null;
			stopDrawThreads();
		}

		/** return true if any drawing thread is active **/
		boolean isDrawing() {
			for (int i = 0; i < m_nDrawThreads; i++) {
				if (m_drawThread[i] != null) {
					return true;
				}
			}
			return false;
		}

		/** thread for drawing (part of the) tree set **/
		class DrawThread extends Thread {
			public boolean m_bStop = false;
			int m_nFrom = 0;
			int m_nTo = 1;
			int m_nEvery = 1;
			int m_iTreeTopology = -1;
			TreeDrawer m_treeDrawer;

			public DrawThread(String str, int nFrom, int nTo, int nEvery, int iTreeTopology, TreeDrawer treeDrawer) {
				super(str);
				m_treeDrawer = treeDrawer;
				m_nFrom = nFrom;
				m_nTo = nTo;
				m_nEvery = nEvery;
				m_iTreeTopology = iTreeTopology;
			} // c'tor

			public DrawThread(String str, int nFrom, int nTo, int nEvery, TreeDrawer treeDrawer) {
				super(str);
				m_nFrom = nFrom;
				m_nTo = nTo;
				m_nEvery = nEvery;
				m_treeDrawer = treeDrawer;
			} // c'tor

			public void run() {
				if (m_image == null) {
					return;
				}
				Graphics2D g = m_image.createGraphics();
				try {
					g.setClip(0, 0, m_image.getWidth(), m_image.getHeight());
					m_image.scale(g, m_fScale, m_fScale);
					float fScaleX = m_fScaleX;
					float fScaleY = m_fScaleY;
					if (m_bUseLogScale) {
						if (m_treeDrawer.m_bRootAtTop) {
							fScaleY *= m_fHeight / (float) Math.log(m_fHeight + 1.0);
						} else {
							fScaleX *= m_fHeight / (float) Math.log(m_fHeight + 1.0);
						}
					}

					// draw all individual trees if necessary
					if (m_bViewAllTrees && m_nTo >= m_nEvery) {
						int iStart = m_nTo - m_nEvery;
						float fAlpha = Math.min(1.0f, 20.0f / iStart * m_fTreeIntensity);
						((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fAlpha));
						Stroke stroke = new BasicStroke(m_nTreeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
						((Graphics2D) g).setStroke(stroke);
						m_treeDrawer.setJitter(m_nJitter);
						for (int i = iStart; i >= m_nFrom; i -= m_nEvery) {
							if (m_bStop) {
								return;
							}
							if (m_iTreeTopology < 0 || m_iTreeTopology == m_nTopologyByPopularity[i]) {
								switch (m_nTopologyByPopularity[i]) {
								case 0:
									g.setColor(m_color[0]);
									break;
								case 1:
									g.setColor(m_color[1]);
									break;
								case 2:
									g.setColor(m_color[2]);
									break;
								default:
									g.setColor(m_color[3]);
								}

								m_treeDrawer.draw(i, m_fLinesX, m_fLinesY, m_fLineWidth, m_fTopLineWidth, g, fScaleX,
										fScaleY);
								if (i % 100 == 0) {
									System.err.print('.');
									m_jStatusBar.setText("Drawing tree " + i);
								}
							}
						}
					}
					// draw consensus trees if necessary
					if (m_bViewCTrees) {
						m_jStatusBar.setText("Drawing consensus trees");
						g.setColor(m_color[CONSCOLOR]);
						Stroke stroke = new BasicStroke(m_nCTreeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
						((Graphics2D) g).setStroke(stroke);
						((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
						g.setClip(0, 0, getWidth(), getHeight());
						m_treeDrawer.setJitter(0);
						for (int i = m_nFrom; i < m_nTopologies; i += m_nEvery) {
							if (m_bStop) {
								return;
							}
							if (m_bViewMultiColor) {
								g.setColor(m_color[9 + (i % (m_color.length - 9))]);
							}
							if (m_iTreeTopology < 0 || m_iTreeTopology == i) {
								((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
										Math.min(1.0f, 0.5f * m_fCTreeIntensity * m_fTreeWeight[i])));
								m_treeDrawer.draw(i, m_fCLinesX, m_fCLinesY, m_fCLineWidth, m_fTopCLineWidth, g,
										fScaleX, fScaleY);
								if (i % 100 == 0) {
									System.err.print('x');
									m_jStatusBar.setText("Drawing consensus tree " + i);
								}
							}
						}
					}

					if (m_viewMode == ViewMode.DRAW) {
						m_drawThread[m_nFrom] = null;
						if (!isDrawing()) {
							double fEntropy = calcImageEntropy();
							m_jStatusBar.setText("Done Drawing trees ");
							System.out.println("Entropy(x100): " + fEntropy + " Mean cumulative width: " + m_w);
						}
						repaint();
					} else {
						DecimalFormat df = new DecimalFormat("##.##");
						double fSum = 0;
						for (int i = 0; i <= m_iAnimateTree; i++) {
							fSum += m_fTreeWeight[i];
						}

						m_jStatusBar.setText("Consensus tree " + (m_iAnimateTree + 1) + " out of " + m_nTopologies
								+ " covering " + df.format((m_fTreeWeight[m_iAnimateTree] * 100)) + "% of trees "
								+ df.format(fSum * 100) + "% cumultive trees");
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("DRAWING ERROR -- IGNORED");
				}
				m_drawThread[m_nFrom] = null;
			}
		} // DrawThread

		void drawHeightInfoSVG(StringBuffer buf) {
			if (m_nGridMode != GridMode.NONE && m_fHeight > 0) {
				DecimalFormat formatter = new DecimalFormat("##.##");
				if (m_treeDrawer.m_bRootAtTop) {
					int nW = getWidth();
					if (m_nGridMode == GridMode.SHORT) {
						nW = 10;
					}
					buf.append("<path " + "fill='none' " + "stroke='rgb(" + m_color[HEIGHTCOLOR].getRed() + ","
							+ m_color[HEIGHTCOLOR].getGreen() + "," + m_color[HEIGHTCOLOR].getBlue() + ")' "
							+ "stroke-width='" + 1 + "' " + " d='");
					float fHeight = (float) adjust(m_fHeight);
					for (int i = 0; i <= m_nTicks; i++) {
						int y = getPosY((m_fHeight - fHeight * i / m_nTicks - m_fTreeOffset) * m_fTreeScale);
						buf.append("M" + 0 + " " + y + "L" + nW + " " + y);
					}
					
					buf.append("'/>\n");

					for (int i = 0; i <= m_nTicks; i++) {
						int y = getPosY((m_fHeight - fHeight * i / m_nTicks - m_fTreeOffset) * m_fTreeScale);
						String sStr = formatter.format(fHeight * (i) / m_nTicks);
						buf.append("<text x='"
								+ m_gridfont.getSize()
								+ "' y='"
								+ y
								+ "' font-family='" + m_gridfont.getFamily() + "' "
								+ "font-size='" + m_gridfont.getSize() + "pt' " + "font-style='"
								+ (m_gridfont.isBold() ? "oblique" : "") + (m_gridfont.isItalic() ? "italic" : "") + "' "
								+
								"stroke='rgb(" + m_color[HEIGHTCOLOR].getRed() + "," + m_color[HEIGHTCOLOR].getGreen()
								+ "," + m_color[HEIGHTCOLOR].getBlue() + ")' " + ">" + sStr + "</text>\n");				
					}
				
				} else {
					int nH = getHeight();
					if (m_nGridMode == GridMode.SHORT) {
						nH = 10;
					}
					buf.append("<path " + "fill='none' " + "stroke='rgb(" + m_color[HEIGHTCOLOR].getRed() + ","
							+ m_color[HEIGHTCOLOR].getGreen() + "," + m_color[HEIGHTCOLOR].getBlue() + ")' "
							+ "stroke-width='" + 1 + "' " + " d='");
					float fHeight = (float) adjust(m_fHeight);
					
					for (int i = 0; i <= m_nTicks; i++) {
						int x = getPosX((m_fHeight - fHeight * i / m_nTicks - m_fTreeOffset) * m_fTreeScale);
						buf.append("M" + x + " " + 0 + "L" + x + " " + nH);
					}
					buf.append("'/>\n");
					for (int i = 0; i <= m_nTicks; i++) {
						int x = getPosX((m_fHeight - fHeight * i / m_nTicks - m_fTreeOffset) * m_fTreeScale);
						String sStr = formatter.format(fHeight * (i) / m_nTicks);
						buf.append("<text x='"
								+ x
								+ "' y='"
								+ m_gridfont.getSize()
								+ "' font-family='" + m_gridfont.getFamily() + "' "
								+ "font-size='" + m_gridfont.getSize() + "pt' " + "font-style='"
								+ (m_gridfont.isBold() ? "oblique" : "") + (m_gridfont.isItalic() ? "italic" : "") + "' "
								+
								"stroke='rgb(" + m_color[HEIGHTCOLOR].getRed() + "," + m_color[HEIGHTCOLOR].getGreen()
								+ "," + m_color[HEIGHTCOLOR].getBlue() + ")' " + ">" + sStr + "</text>\n");				
					}
				}
			}
		} // drawHeightInfoSVG

		void drawLabelsSVG(Node node, StringBuffer buf) {
			if (node.isLeaf()) {
				Color color = null;
				if (m_bSelection[node.m_iLabel]) {
					color = m_color[LABELCOLOR];
				} else {
					color = Color.GRAY;
				}
				int x = 0;
				int y = 0;
				if (m_treeDrawer.m_bRootAtTop) {
					x = (int) (node.m_fPosX * m_fScaleX /* m_fScale */) + 10;
					y = getPosY((node.m_fPosY - m_fTreeOffset) * m_fTreeScale);
				} else {
					y = (int) (node.m_fPosX * m_fScaleY/* m_fScale */);
					x = getPosX((node.m_fPosY - m_fTreeOffset) * m_fTreeScale);
				}
				buf.append("<text x='" + x + "' y='" + y + "' " + "font-family='" + m_font.getFamily() + "' "
						+ "font-size='" + m_font.getSize() + "pt' " + "font-style='"
						+ (m_font.isBold() ? "oblique" : "") + (m_font.isItalic() ? "italic" : "") + "' "
						+ "stroke='rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")' "
						+ ">" + m_sLabels.elementAt(node.m_iLabel) + "</text>\n");
			} else {
				drawLabelsSVG(node.m_left, buf);
				drawLabelsSVG(node.m_right, buf);
			}
		} // drawLabelsSVG

		void toSVG(String sFileName) {
			try {
				if (m_font == null) {
					m_font = new Font("Monospaced", Font.PLAIN, 10);
				}

				StringBuffer buf = new StringBuffer();
				SVGTreeDrawer treeDrawer = new SVGTreeDrawer(buf);
				treeDrawer.LINE_WIDTH_SCALE = m_treeDrawer.LINE_WIDTH_SCALE;
				treeDrawer.m_bRootAtTop = m_treeDrawer.m_bRootAtTop;
				treeDrawer.m_bViewBlockTree = m_treeDrawer.m_bViewBlockTree;
				if (m_treeDrawer.getBranchDrawer() instanceof SteepArcBranchDrawer) {
					treeDrawer.m_bViewBlockTree = false;
					JOptionPane.showMessageDialog(this, "Steep arcs not implemented yet for SVG export, using straigh lines instead");
				}
				if (m_treeDrawer.getBranchDrawer() instanceof ArcBranchDrawer) {
					treeDrawer.m_branchStyle = 2;
				}
				DrawThread thread = new DrawThread("draw thread", 0, m_trees.length, 1, treeDrawer);
				thread.run();
				drawLabelsSVG(m_trees[0], buf);
				drawHeightInfoSVG(buf);

				PrintStream out = new PrintStream(sFileName);
				out.println("<?xml version='1.0'?>\n" + "<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.1//EN'\n"
						+ "  'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd'>\n"
						+ "<svg xmlns='http://www.w3.org/2000/svg' version='1.1'\n" + "      width='" + getWidth()
						+ "' height='" + getHeight() + "' viewBox='0 0 " + getWidth() + " " + getHeight() + "'>\n"
						+ "<rect fill='#fff' width='" + getWidth() + "' height='" + getHeight() + "'/>");
				out.println(buf.toString());
				out.println("</svg>");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} // toSVG

		double calcImageEntropy() throws Exception {
			if (m_image == null) {
				return 0;
			}
			Thread.sleep(100);
			int[] nAlpha = new int[256];
			for (int i = 0; i < m_image.getWidth() - m_nLabelWidth; i++) {
				for (int j = 0; j < m_image.getHeight(); j++) {
					int x = m_image.getRGB(i, j);
					int y = ((x & 0xFF) + ((x & 0xFF00) >> 8) + ((x & 0xFF0000) >> 16)) / 3;
					// System.out.print(Integer.toHexString(x)+" " +
					// Integer.toHexString(y) + " ");
					nAlpha[y]++;
				}
				// System.out.println();
			}
			double fQ = 0;
			for (int i = 1; i < 255; i++) {
				fQ -= nAlpha[i] * Math.log(i / 255.0);
			}
			return 100.0 * fQ / ((m_image.getWidth() - m_nLabelWidth) * m_image.getHeight());
		} // calcImageEntropy

		
		/** maps most significant digit to nr of ticks on graph **/ 
		final int [] NR_OF_TICKS = new int [] {5,10,8,6,8,10,6,7,8,9, 10};
		int m_nTicks = 10;
		
		private double adjust(double fYMax) {
			// adjust fYMax so that the ticks come out right
			int k = 0;
			double fY = fYMax;
			while (fY > 10) {
				fY /= 10;
				k++;
			}
			while (fY < 1 && fY > 0) {
				fY *= 10;
				k--;
			}
			fY = Math.ceil(fY);
			m_nTicks = NR_OF_TICKS[(int) fY];
			for (int i = 0; i < k; i++) {
				fY *= 10;
			}
			for (int i = k; i < 0; i++) {
				fY /= 10;
			}
			return fY;
		}		
		/** draw height bar and/or height grid if desired **/
		void paintHeightInfo(Graphics g) {
			if (m_nGridMode != GridMode.NONE && m_fHeight > 0) {
				DecimalFormat formatter = new DecimalFormat("##.##");
				((Graphics2D) g).setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
				((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
				if (m_gridfont == null) {
					m_gridfont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
				}
				g.setFont(m_gridfont);
				if (m_treeDrawer.m_bRootAtTop) {
					int nW = getWidth();
					if (m_nGridMode == GridMode.SHORT) {
						nW = 10;
					}
					g.setColor(m_color[HEIGHTCOLOR]);

					float fHeight = (float) adjust(m_fHeight);
					
					for (int i = 0; i <= m_nTicks; i++) {
						int y = getPosY((m_fHeight - fHeight * i / m_nTicks - m_fTreeOffset) * m_fTreeScale);
						g.drawString(formatter.format(fHeight * (i) / m_nTicks), 0, y - 2);
						g.drawLine(0, y, nW, y);
					}
				} else {
					int nH = getHeight();
					if (m_nGridMode == GridMode.SHORT) {
						nH = 10;
					}
					g.setColor(m_color[HEIGHTCOLOR]);
					
					float fHeight = (float) adjust(m_fHeight);
					
					for (int i = 0; i <= m_nTicks; i++) {
						int x = getPosX((m_fHeight - fHeight * i / m_nTicks - m_fTreeOffset) * m_fTreeScale);
						g.drawString(formatter.format(fHeight * (i) / m_nTicks), x+2, m_gridfont.getSize());
						g.drawLine(x, 0, x, nH);
					}
				}
			}
		} // paintHeightInfo

		/**
		 * Updates the screen contents.
		 * 
		 * @param g
		 *            the drawing surface.
		 */
		public void paintComponent(Graphics g) {
			a_undo.setEnabled(m_doActions.size() > 0 && m_iUndo > 1);
			a_redo.setEnabled(m_iUndo < m_doActions.size());
			g.setFont(m_font);
			switch (m_viewMode) {
			case DRAW:
				drawTreeSet((Graphics2D) g);
				break;
			case ANIMATE:
				drawFrame(g);
				paintHeightInfo(g);
				try {
					Thread.sleep(m_nAnimationDelay);
				} catch (Exception ex) {
					// ignore
				}
				m_iAnimateTree = (m_iAnimateTree + 1) % m_nTopologies;
				repaint();
				return;
			case BROWSE:
				drawFrame(g);
				paintHeightInfo(g);
				m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				return;
			}
			if (m_sOutputFile != null && !isDrawing()) {
				// wait a second for the drawing to be finished
				try {
					ImageIO.write(m_image.m_localImage, "png", new File(m_sOutputFile));
					System.exit(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (m_bViewEditTree && m_Xmode == 0) {
				viewEditTree(g);
			}
			if (m_bViewClades && (m_Xmode == 1 || m_Xmode == 2)) {
				viewClades(g);
			}
		}

		/**
		 * Represents point over which the tree can be 'rotated' when editing
		 * the tree.
		 */
		private class RotationPoint {
			// location of the point on screen
			int m_nX, m_nY;

			RotationPoint(int nX, int nY) {
				m_nX = nX;
				m_nY = nY;
			}

			boolean intersects(int nX, int nY) {
				return (Math.abs(nX - m_nX) < 5 && Math.abs(nY - m_nY) < 5);
			}
			
			public String toString() {
				return "(" + m_nX + "," + m_nY + ")";
			}
		}; // class RotationPoint

		RotationPoint[] m_rotationPoints = null;

		/** draw tree that allows editing order **/
		void viewEditTree(Graphics g) {
			float fScaleX = m_fScaleX;
			float fScaleY = m_fScaleY;
			if (m_bUseLogScale) {
				if (m_treeDrawer.m_bRootAtTop) {
					fScaleY *= m_fHeight / (float) Math.log(m_fHeight + 1.0);
				} else {
					fScaleX *= m_fHeight / (float) Math.log(m_fHeight + 1.0);
				}
			}
			int x = 0;
			int y = 0;
			int x0 = 0;
			int y0 = 0;
			int x1 = 0;
			int y1 = 0;
			g.setColor(Color.BLACK);
			Stroke stroke = new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
			((Graphics2D) g).setStroke(stroke);
			((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

			int h = m_rotate.getHeight(null);
			int w = m_rotate.getWidth(null);
			boolean bUpdatePoints = false;
			if (m_rotationPoints == null) {
				m_rotationPoints = new RotationPoint[m_fCLinesX[0].length / 4];
				bUpdatePoints = true;
			}
			for (int i = 1; i < m_fCLinesX[0].length - 2; i += 4) {
				if (m_treeDrawer.m_bRootAtTop) {
					x = (int) ((m_fCLinesX[0][i] + m_fCLinesX[0][i + 1]) * fScaleX / 2.0f);
					y = (int) ((m_fCLinesY[0][i] + m_fCLinesY[0][i + 1]) * fScaleY / 2.0f);
					x0 = (int) ((m_fCLinesX[0][i - 1]) * fScaleX);
					y0 = (int) ((m_fCLinesY[0][i - 1]) * fScaleY);
					x1 = (int) ((m_fCLinesX[0][i + 2]) * fScaleX);
					y1 = (int) ((m_fCLinesY[0][i + 2]) * fScaleY);
				} else {
					x = (int) ((m_fCLinesY[0][i] + m_fCLinesY[0][i + 1]) * fScaleX / 2.0f);
					y = (int) ((m_fCLinesX[0][i] + m_fCLinesX[0][i + 1]) * fScaleY / 2.0f);
					x0 = (int) ((m_fCLinesY[0][i - 1]) * fScaleX);
					y0 = (int) ((m_fCLinesX[0][i - 1]) * fScaleY);
					x1 = (int) ((m_fCLinesY[0][i + 2]) * fScaleX);
					y1 = (int) ((m_fCLinesX[0][i + 2]) * fScaleY);
				}
				if (bUpdatePoints) {
					m_rotationPoints[i / 4] = new RotationPoint(x, y);
				}
				g.drawLine(x, y, x0, y0);
				g.drawLine(x, y, x1, y1);
				g.drawImage(m_rotate, x - w / 2, y - h / 2, x + h / 2, y + w / 2, 0, 0, h, w, null);

			}
		} // viewEditTree

		/** show all clades **/
		void viewClades(Graphics g) {
			float fScaleX = m_fScaleX;
			float fScaleY = m_fScaleY;
			if (m_bUseLogScale) {
				if (m_treeDrawer.m_bRootAtTop) {
					fScaleY *= m_fHeight / (float) Math.log(m_fHeight + 1.0);
				} else {
					fScaleX *= m_fHeight / (float) Math.log(m_fHeight + 1.0);
				}
			}
			int x = 0;
			int y = 0;
			g.setColor(Color.BLACK);
			Stroke stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
			((Graphics2D) g).setStroke(stroke);
			((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

			int h = m_rotate.getHeight(null);
			int w = m_rotate.getWidth(null);
			boolean bUpdatePoints = false;
			if (m_rotationPoints == null) {
				m_rotationPoints = new RotationPoint[m_cladeHeight.size()];
				bUpdatePoints = true;
			}
			int nHeight = getHeight();
			for (int i = 0/* m_sLabels.size() */; i < m_cladeHeight.size(); i++) {
				if (m_cladeWeight.get(i) > 0.01 && ((m_Xmode == 1 && m_clades.get(i).length > 1) || (m_Xmode == 2 && m_clades.get(i).length == 1))) {
					if (!m_treeDrawer.m_bRootAtTop) {
						x = (int) ((m_cladeHeight.get(i) - m_fTreeOffset) * fScaleX * m_fTreeScale);
						y = (int) (m_cladePosition[i] * fScaleY);
					} else {
						x = (int) (m_cladePosition[i] * fScaleX);
						y = /*nHeight -*/ (int) ((m_cladeHeight.get(i) - m_fTreeOffset) * fScaleY * m_fTreeScale);
					}
				} else {
					x = -100;
					y = -100;
				}
				w = (int)(10 +  m_cladeWeight.get(i)*10);
				h = w;
				g.drawOval(x- w / 2, y- h / 2, w, h);
				//g.drawImage(m_rotate, x - w / 2, y - h / 2, x + h / 2, y + w / 2, 0, 0, h, w, null);
				if (bUpdatePoints) {
					m_rotationPoints[i] = new RotationPoint(x, y);
				}
			}

			// draw selection
			stroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
			((Graphics2D) g).setStroke(stroke);
			((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			for (int i :  m_cladeSelection) {
				if (!m_treeDrawer.m_bRootAtTop) {
					x = (int) ((m_cladeHeight.get(i) - m_fTreeOffset) * fScaleX * m_fTreeScale);
					y = (int) (m_cladePosition[i] * fScaleY);
				} else {
					x = (int) (m_cladePosition[i] * fScaleX);
					y = /*nHeight -*/ (int) ((m_cladeHeight.get(i) - m_fTreeOffset) * fScaleY * m_fTreeScale);
				}
				w = (int)(10 +  m_cladeWeight.get(i)*10);
				h = w;
				g.drawOval(x- w / 2, y- h / 2, w, h);
			}
		
		}

		/** draw complete set of trees **/
		void drawTreeSet(Graphics2D g) {
			Color oldBackground = ((Graphics2D) g).getBackground();
			((Graphics2D) g).setBackground(m_color[BGCOLOR]);
			Rectangle r = g.getClipBounds();
			g.clearRect(r.x, r.y, r.width, r.height);
			((Graphics2D) g).setBackground(oldBackground);
			g.setClip(r.x, r.y, r.width, r.height);

			if (m_trees == null || m_fCLinesY == null || m_bInitializing) {
				// nothing to see
				return;
			}
			synchronized (this) {
				m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
				if (m_image == null) {
					System.err.println("Setting up new image");
					if (!m_bShowBounds) {
						m_image = new BufferedImageF((int) (getWidth() * m_fScale), (int) (getHeight() * m_fScale));
					} else {
						m_image = new BufferedImageBounded((int) (getWidth() * m_fScale),
								(int) (getHeight() * m_fScale));
					}
					m_treeDrawer.setImage(m_image);
					Graphics2D g2 = m_image.createGraphics();
					m_image.init(g2, m_color[BGCOLOR], m_bgImage, m_fBGImageBox, m_nLabelWidth, m_fMinLong, m_fMaxLong,
							m_fMinLat, m_fMaxLat);
					//drawLabels(m_trees[0], g2);
					if (m_bDrawGeo && m_fLatitude.size() > 0) {
						g2.setColor(m_color[GEOCOLOR]);
						Stroke stroke = new BasicStroke(m_nGeoWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
						((Graphics2D) g2).setStroke(stroke);
						drawGeo(m_cTrees[0], g2);
					}
					paintHeightInfo(g2);
					//drawLabels(m_trees[0], g2);
					m_image.SyncIntToRGBImage();

					int nDrawThreads = Math.min(m_nDrawThreads, m_trees.length);
					for (int i = 0; i < nDrawThreads; i++) {
						m_drawThread[i] = new DrawThread("draw thread", i, m_trees.length + i, nDrawThreads,
								m_treeDrawer);
						m_drawThread[i].start();
					}
					if (m_bShowRootCanalTopology) {
						float fScaleX = m_fScaleX;
						float fScaleY = m_fScaleY;
						if (m_bUseLogScale) {
							if (m_treeDrawer.m_bRootAtTop) {
								fScaleY *= m_fHeight / (float) Math.log(m_fHeight + 1.0);
							} else {
								fScaleX *= m_fHeight / (float) Math.log(m_fHeight + 1.0);
							}
						}

						((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
						Stroke stroke = new BasicStroke(m_nCTreeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
						((Graphics2D) g).setStroke(stroke);
						g.setColor(m_color[ROOTCANALCOLOR]);
						m_treeDrawer.draw(0, m_fRLinesX, m_fRLinesY, m_fLineWidth,
								m_fTopLineWidth, g, fScaleX, fScaleY);
					}
				}

			}
			;
			if (m_image == null) {
				return;
			}
			m_image.drawImage(g, this);
			if (m_nSelectedRect != null) {
				if (m_bViewEditTree && m_Xmode == 0) { // || m_bViewClades) {
					int h = m_rotate.getHeight(null);
					int w = m_rotate.getWidth(null);
					int x = m_nSelectedRect.x + (m_treeDrawer.m_bRootAtTop ? m_nSelectedRect.width : 0);
					int y = m_nSelectedRect.y + (m_treeDrawer.m_bRootAtTop ? 0 : m_nSelectedRect.height);
					g.drawImage(m_rotate, x - w / 2, y - h / 2, x + h / 2, y + w / 2, 0, 0, h, w, null);
				} else {
					g.drawRect((int) (m_nSelectedRect.x + Math.min(m_nSelectedRect.width, 0)),
							(int) (m_nSelectedRect.y + Math.min(m_nSelectedRect.height, 0)),
							(int) (Math.abs(m_nSelectedRect.width)), (int) (Math.abs(m_nSelectedRect.height)));
				}
			}
			// need this here so that the screen is updated when selection of
			// taxa changes
			drawLabels(m_trees[0], g);
			// ((Graphics2D) g).scale(m_fScale, m_fScale);
			if (isDrawing()) {
				try {
					Thread.sleep(m_nAnimationDelay);
				} catch (Exception ex) {
					// ignore
				}
				repaint();
				if (m_bRecord) {
					try {
						System.err.println(" writing /tmp/frame" + m_nFrameNr + ".jpg " + isDrawing());
						ImageIO.write(m_image.m_localImage, "jpg", new File("/tmp/frame" + m_nFrameNr + ".jpg"));
						m_nFrameNr++;
					} catch (Exception ex) {
						// ignore
					}
				}
			} else {
				m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				if (m_bRecord) {
					try {
						System.err.println(" writing /tmp/frame" + m_nFrameNr + ".jpg " + isDrawing());
						ImageIO.write(m_image.m_localImage, "jpg", new File("/tmp/frame" + m_nFrameNr + ".jpg"));
						m_nFrameNr++;
					} catch (Exception ex) {
						// ignore
					}
				}
				m_bRecord = false;
			}
		} // drawTreeSet

		/** draw new frame in animation or browse action **/
		void drawFrame(Graphics g) {
			Color oldBackground = ((Graphics2D) g).getBackground();
			((Graphics2D) g).setBackground(m_color[BGCOLOR]);
			Rectangle r = g.getClipBounds();
			g.clearRect(r.x, r.y, r.width, r.height);
			((Graphics2D) g).setBackground(oldBackground);
			g.setClip(r.x, r.y, r.width, r.height);

			if (m_trees == null || m_fCLinesY == null || m_bInitializing) {
				// nothing to see
				return;
			}
			m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			if (m_image == null || m_bAnimateOverwrite || m_iAnimateTree == 0) { // ||
																					// m_viewMode
																					// ==
																					// ViewMode.BROWSE)
																					// {
				if (!m_bShowBounds) {
					m_image = new BufferedImageF((int) (getWidth() * m_fScale), (int) (getHeight() * m_fScale));
				} else {
					m_image = new BufferedImageBounded((int) (getWidth() * m_fScale), (int) (getHeight() * m_fScale));
				}
				m_treeDrawer.setImage(m_image);
				Graphics2D g2 = m_image.createGraphics();
				// g2.setBackground(m_color[BGCOLOR]);
				// g2.clearRect(0, 0, m_image.getWidth(), m_image.getHeight());
				m_image.init(g2, m_color[BGCOLOR], m_bgImage, m_fBGImageBox, m_nLabelWidth, m_fMinLong, m_fMaxLong,
						m_fMinLat, m_fMaxLat);
				// drawBGImage(g2);
				// m_image.drawImage(g2 , this);
				if (m_bDrawGeo && m_fLatitude.size() > 0) {
					g2.setColor(m_color[GEOCOLOR]);
					Stroke stroke = new BasicStroke(m_nGeoWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
					((Graphics2D) g2).setStroke(stroke);
					drawGeo(m_cTrees[0], g2);
				}
				paintHeightInfo(g2);
				drawLabels(m_trees[0], g2);
				m_image.SyncIntToRGBImage();
			}

			for (int i = 0; i < m_nDrawThreads; i++) {
				m_drawThread[i] = new DrawThread("draw thread", i, m_trees.length + i, m_nDrawThreads, m_iAnimateTree,
						m_treeDrawer);
				m_drawThread[i].start();
			}

			while (isDrawing()) {
				try {
					Thread.sleep(m_nAnimationDelay);
				} catch (Exception ex) {
					// ignore
				}
				m_image.drawImage(g, this);
				System.err.print("X");
			}
			m_image.drawImage(g, this);
			m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		} // animate

		/**
		 * implementation of Printable, used for printing
		 * 
		 * @see Printable
		 */
		public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
			if (pageIndex > 0) {
				return (NO_SUCH_PAGE);
			} else {
				Graphics2D g2d = (Graphics2D) g;
				g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
				float fHeight = (float) pageFormat.getImageableHeight();
				float fWidth = (float) pageFormat.getImageableWidth();

				float fScaleX = m_fScaleX;
				m_fScaleX = fWidth / m_sLabels.size();
				float fScaleY = m_fScaleY;
				m_fScaleY = (fHeight - 10) / m_fHeight;

				// Turn off float buffering
				paint(g2d);
				m_fScaleX = fScaleX;
				m_fScaleY = fScaleY;
				// Turn float buffering back on
				return (PAGE_EXISTS);
			}
		} // print

		@Override
		public void mouseClicked(MouseEvent e) {
//			if (m_bViewEditTree && m_Xmode == 0) {
//				if (m_rotationPoints != null) {
//					if (e.getButton() == MouseEvent.BUTTON1) {
//						for (int i = 0; i < m_rotationPoints.length; i++) {
//							if (m_rotationPoints[i].intersects(e.getX(), e.getY())) {
//								rotateAround(i);
//								// repaint();
//								return;
//							}
//						}
//					}
//				}
//			} else {
//				Rectangle r = new Rectangle(e.getPoint(), new Dimension(1, 1));
//				if (e.getButton() == MouseEvent.BUTTON1) {
//					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
//						toggleSelection(r);
//					} else if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
//						addToSelection(r);
//					} else {
//						clearSelection();
//						addToSelection(r);
//					}
//					repaint();
//				}
//			}
		}

		/** remove all labels from selection **/
		void clearSelection() {
			for (int i = 0; i < m_bSelection.length; i++) {
				if (m_bSelection[i]) {
					m_bSelection[i] = false;
					m_bSelectionChanged = true;
				}
			}
			m_cladeSelection.clear();
			resetCladeSelection();			
		}

		/**
		 * remove labels overlapping rectangle r currently in selection and
		 * replace by ones not selected but overlapping with r
		 **/
		void toggleSelection(Rectangle r) {
			float f = m_fScale;
			m_fScale = 1;
			r.x = (int) (r.x / m_fScale);
			r.y = (int) (r.y / m_fScale);
			r.width = 1 + (int) (r.width / m_fScale);
			r.height = 1 + (int) (r.height / m_fScale);
			for (int i = 0; i < m_bSelection.length; i++) {
				if (m_bLabelRectangle[i].intersects(r)) {
					m_bSelection[i] = !m_bSelection[i];
					m_bSelectionChanged = true;
				}
			}
			if (m_rotationPoints != null) {
				r.x += 5;
				r.y += 5;
				Rectangle rotationPoint = new Rectangle(10, 10);
				for (int i = 0; i < m_rotationPoints.length; i++) {
					rotationPoint.x = m_rotationPoints[i].m_nX;
					rotationPoint.y = m_rotationPoints[i].m_nY;
					if (r.intersects(rotationPoint)) { 
						if (m_cladeSelection.contains(i)) {
							m_cladeSelection.remove(i);
						} else {
							m_cladeSelection.add(i);
						}
					}
				}
				resetCladeSelection();
			}
			m_fScale = f;
		}

		/** add labels overlapping rectangle r to selection **/
		void addToSelection(Rectangle r) {
			float f = m_fScale;
			m_fScale = 1;
			r.x = (int) (r.x / m_fScale);
			r.y = (int) (r.y / m_fScale);
			r.width = 1 + (int) (r.width / m_fScale);
			r.height = 1 + (int) (r.height / m_fScale);
			for (int i = 0; i < m_bSelection.length; i++) {
				if (m_bLabelRectangle[i].intersects(r)
						|| (m_bGeoRectangle[i] != null && m_bGeoRectangle[i].intersects(r))) {
					if (!m_bSelection[i]) {
						m_bSelection[i] = true;
						m_bSelectionChanged = true;
					}
				}
			}
			if (m_rotationPoints != null) {
				r.x += 5;
				r.y += 5;
				Rectangle rotationPoint = new Rectangle(10, 10);
				for (int i = 0; i < m_rotationPoints.length; i++) {
					rotationPoint.x = m_rotationPoints[i].m_nX;
					rotationPoint.y = m_rotationPoints[i].m_nY;
					if (r.intersects(rotationPoint)) { 
						m_cladeSelection.add(i);
					}
				}
				
				System.err.println(Arrays.toString(m_cladeSelection.toArray(new Integer[0])));
				resetCladeSelection();
			}
			m_fScale = f;
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		boolean m_bIsMoving = false;
		boolean m_bIsDragging = false;
		@Override
		public void mousePressed(MouseEvent e) {
			m_bIsDragging = false;
			m_nSelectedRect = new Rectangle(e.getPoint(), new Dimension(1, 1));
			if (m_bViewEditTree && m_Xmode == 0) { // && e.getButton() != MouseEvent.BUTTON1) {
				if (m_rotationPoints != null) {
					for (int i = 0; i < m_rotationPoints.length; i++) {
						if (m_rotationPoints[i].intersects(e.getPoint().x, e.getPoint().y)) {
							m_bIsMoving = true;
							m_nSelectedRect = new Rectangle(e.getPoint(), new Dimension(1, 1));
							return;
						}
					}
					m_nSelectedRect = null;
					repaint();
				}
			} else if (m_bViewClades && (m_Xmode == 1 || m_Xmode == 2)) { // && e.getButton() != MouseEvent.BUTTON1) {
//				m_nSelectedRect = new Rectangle(e.getPoint(), new Dimension(1, 1));
				if (m_rotationPoints != null) {
					for (int i = 0/* m_sLabels.size() */; i < m_rotationPoints.length; i++) {
						if (m_rotationPoints[i].intersects(e.getPoint().x, e.getPoint().y) && 
								m_cladeSelection.contains(i)) {
							m_bIsMoving = true;
							return;
						}
					}
					//m_nSelectedRect = null;
				}
				repaint();
//			} else {
//				if (!m_bViewEditTree && e.getButton() == MouseEvent.BUTTON1) {
//					m_nSelectedRect = new Rectangle(e.getPoint(), new Dimension(1, 1));
//				}
			}
		}

		/** update selection when mouse is released **/
		@Override
		public void mouseReleased(MouseEvent e) {
			if (m_bViewEditTree && m_Xmode == 0 && e.getButton() == MouseEvent.BUTTON1 && !m_bIsDragging) {
				if (m_rotationPoints != null) {
					for (int i = 0; i < m_rotationPoints.length; i++) {
						if (m_rotationPoints[i].intersects(e.getX(), e.getY())) {
							rotateAround(i);
							m_nSelectedRect = null;
							// repaint();
							return;
						}
					}
				}
			} else 	if (m_nSelectedRect != null) {
				//if (e.getButton() == MouseEvent.BUTTON1) {
				if (!m_bIsMoving) {
					// normalize rectangle
					if (m_nSelectedRect.width < 0) {
						m_nSelectedRect.x += m_nSelectedRect.width;
						m_nSelectedRect.width = -m_nSelectedRect.width;
					}
					if (m_nSelectedRect.height < 0) {
						m_nSelectedRect.y += m_nSelectedRect.height;
						m_nSelectedRect.height = -m_nSelectedRect.height;
					}
					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						toggleSelection(m_nSelectedRect);
					} else if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
						addToSelection(m_nSelectedRect);
					} else {
						clearSelection();
						addToSelection(m_nSelectedRect);
					}
					m_nSelectedRect = null;
					repaint();
				} else { // right click
					m_bIsMoving = false;
					m_bIsDragging = false;
					if (m_rotationPoints != null) {
						if (m_bViewEditTree && m_Xmode == 0) {
							for (int i = 0; i < m_rotationPoints.length; i++) {
								if (m_rotationPoints[i].intersects(m_nSelectedRect.x, m_nSelectedRect.y)) {
									moveRotationPoint(i, m_sLabels.size()
											* (m_treeDrawer.m_bRootAtTop ? (float) m_nSelectedRect.width / getWidth()
													: (float) m_nSelectedRect.height / getHeight()));
									m_nSelectedRect = null;
									repaint();
									return;
								}
							}
						} else if (m_bViewClades && (m_Xmode == 1 || m_Xmode == 2)) {
							double dF = m_sLabels.size() * (m_treeDrawer.m_bRootAtTop ? (float) m_nSelectedRect.width / getWidth()
									: (float) m_nSelectedRect.height / getHeight());
							//for (int i = 0/* m_sLabels.size() */; i < m_rotationPoints.length; i++) {
//								if (m_rotationPoints[i].intersects(m_nSelectedRect.x, m_nSelectedRect.y)) {
//								}
							for (int i : m_cladeSelection) {
								m_cladePosition[i] += dF;
							}
							calcLines();
							makeDirty();
							m_nSelectedRect = null;
							repaint();
							return;
						}
						m_nSelectedRect = null;
						repaint();
					}
				}
			}
		}

		/** update selection rectangle when mouse is dragged **/
		@Override
		public void mouseDragged(MouseEvent e) {
			if (m_nSelectedRect != null) {
				m_bIsDragging = true;
				m_nSelectedRect.width = e.getPoint().x - m_nSelectedRect.x;
				m_nSelectedRect.height = e.getPoint().y - m_nSelectedRect.y;
				repaint();
				return;
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (m_bDrawGeo) {
				for (int i = 0; i < m_bSelection.length; i++) {
					if (m_bGeoRectangle[i].contains(e.getPoint())) {
						m_jStatusBar.setText(m_sLabels.elementAt(i));
					}
				}
			}
			String sText = m_jStatusBar.getText();
			sText = sText.split("\t")[0];
			float fHeight = screenPosToHeight(e.getX(), e.getY());
			if (!Float.isNaN(fHeight)) {
				sText += "\theight=" + fHeight;
				m_jStatusBar.setText(sText);
			}
			
		}

	} // class TreeVizPanel

	/** this contains the TreeSetPanel */
	JScrollPane m_jScrollPane;
	/** panel for drawing the trees **/
	TreeSetPanel m_Panel;
	/** panel for controlling properties of DensiTree **/
	//ControlPanel m_ctrlPanel;
	ButtonGroup m_modeGroup = new ButtonGroup();
	ButtonGroup m_styleGroup = new ButtonGroup();
	/** the menu bar for this application. */
	JMenuBar m_menuBar;
	/** status bar at bottom of window */
	final JLabel m_jStatusBar = new JLabel("Status bar");;
	/** toolbar containing buttons at top of window */
	final JToolBar m_jTbTools = new JToolBar();
	final JToolBar m_jTbTools2 = new JToolBar();
	final JToolBar m_jTbCladeTools = new JToolBar();
	/** font for all text being printed (e.g. labels, height info) **/
	Font m_font = Font.getFont(Font.MONOSPACED);
	Font m_gridfont = Font.getFont(Font.MONOSPACED);
	
	/** flag to indicate consensus trees should be shown **/
	boolean m_bViewCTrees = false;
	/** flag to indicate all individual trees should be shown **/
	boolean m_bViewAllTrees = true;
	/** use log scaling for drawing height **/
	boolean m_bUseLogScale = false;
	double m_fExponent = 1.0;

	enum GridMode {NONE, SHORT, FULL};
	GridMode m_nGridMode = GridMode.NONE;

	/** show consensus tree in multiple colours, or just main colour */
	boolean m_bViewMultiColor = false;
	/** show geographical info if available **/
	boolean m_bDrawGeo = true;

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

	void makeDirty() {
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
			m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			reshuffle(m_nMode);
			m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
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
				m_nDrawThreads++;
				m_drawThread = new Thread[m_nDrawThreads];
			}
			if (m_sName.equals("Drawing Threads-")) {
				if (m_nDrawThreads > 1) {
					m_Panel.stopDrawThreads();
					m_nDrawThreads--;
					m_drawThread = new Thread[m_nDrawThreads];
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
							"File load error", JOptionPane.PLAIN_MESSAGE);
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
							|| sFileName.toLowerCase().endsWith(".bmp") || sFileName.toLowerCase().endsWith(".svg"))) {
						sFileName += ((MyFileFilter) fc.getFileFilter()).getExtention();
					}

					if (sFileName.toLowerCase().endsWith(".png") || sFileName.toLowerCase().endsWith(".jpg")
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

	Action a_loadkml = new MyAction("Load locations", "Load geographic locations of taxa", "geo", "") {
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
			m_Panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			String sCladeText = "";
			for (String s : cladesToString()) {
				sCladeText += s;
			}
			m_Panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
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
					+ " -v " + m_nTreeWidth + " -f " + m_nAnimationDelay + " -t " + m_nDrawThreads + " -b " + m_nBurnIn;
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

//	Action a_branchDrawer = new MyAction("Branch type", "Choose branch drawing type", "branchdraw", "") {
//		private static final long serialVersionUID = -2L;
//
//		public void actionPerformed(ActionEvent ae) {
//			String[] choices = { "Line", "Block", "Arced", "Steep"/* , "Trapezium" */};
//			int nChoice = JOptionPane.showOptionDialog(null, "BranchDrawer :", "Branch type",
//					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
//			BranchDrawer bd = null;
//			switch (nChoice) {
//			case 0:
//				bd = new BranchDrawer();
//				m_treeDrawer.m_bViewBlockTree = false;
//				break;
//			case 1:
//				bd = new BranchDrawer();
//				m_treeDrawer.m_bViewBlockTree = true;
//				break;
//			case 2:
//				bd = new ArcBranchDrawer();
//				m_treeDrawer.m_bViewBlockTree = false;
//				break;
//			// case 2: bd = new KoruBranchDrawer();break;
//			// case 3: bd = new TrapeziumBranchDrawer();break;
//			// case 3: bd = new BrownianBridgeBranchDrawer();break;
//			case 3:
//				bd = new SteepArcBranchDrawer();
//				m_treeDrawer.m_bViewBlockTree = false;
//				break;
//			}
//			if (bd != null) {
//				m_treeDrawer.setBranchDrawer(bd);
//				makeDirty();
//			}
//		}
//	}; // class ActionBranchDrawer

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
//
//			// Set a new location using an absolution location; center the divider
//				loc = (int)((pane.getBounds().getWidth()-pane.getDividerSize())/2);
//				pane.setDividerLocation(loc);
//
//			double propLoc = .5D;
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

	Action a_setgridfont = new MyAction("Set Grid Font", "Set Grid Font", "gridfont", "") {
		private static final long serialVersionUID = 1L;

		// @SuppressWarnings("deprecation")
		public void actionPerformed(ActionEvent ae) {
			JFontChooser fontChooser = new JFontChooser();
			// fontChooser.setFont(m_font);
			int result = fontChooser.showDialog(null);
			if (result == JFontChooser.OK_OPTION) {
				m_gridfont = fontChooser.getSelectedFont();
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
	Action a_pattern = new MyAction("Pattern", "Pattern of metadata for width - for expert users only", "pattern", "") {
		private static final long serialVersionUID = -2L;
		public void actionPerformed(ActionEvent ae) {
			String sPattern = JOptionPane.showInputDialog("Pattern on metadata for width:",m_sPattern);
			if (sPattern != null) {
				try {
				m_sPattern = sPattern;
		    	m_pattern = Pattern.compile(m_sPattern);
		    	calcLines();
		    	makeDirty();
//				JOptionPane.showMessageDialog(null,
//						"Pattern in is now set to " + m_sPattern +"\nReload file to apply to a tree set.", "Pattern message",
//						JOptionPane.PLAIN_MESSAGE);
				} catch (Exception e) {}
			}
		}
	}; // class MyAction

	Action a_patternBottom = new MyAction("Bottom Pattern Number", "Bottom Pattern Number", "patternbottom", "") {
		private static final long serialVersionUID = -2L;
		public void actionPerformed(ActionEvent ae) {
			String sPattern = JOptionPane.showInputDialog("Number of Metadata item used for bottom of branch:", (m_iPatternForBottom + 1));
			if (sPattern != null) {
				try {
					m_iPatternForBottom = Integer.parseInt(sPattern) - 1;
					if (m_iPatternForBottom < 0) {
						m_iPatternForBottom = 0;
					}
			    	m_pattern = createPattern();
			    	calcLines();
			    	makeDirty();
				} catch (Exception e) {}
			}
		}
	}; // class MyAction
	Action a_patternTop = new MyAction("Top Pattern Number", "Top Pattern Number", "patterntop", "") {
		private static final long serialVersionUID = -2L;
		public void actionPerformed(ActionEvent ae) {
			String sPattern = JOptionPane.showInputDialog("Number of Metadata item used for top of branch (if less than item used for top this is ignored):", (m_iPatternForTop + 1));
			if (sPattern != null) {
				try {
					m_iPatternForTop = Integer.parseInt(sPattern) - 1;
					if (m_iPatternForTop < 0) {
						m_iPatternForTop = 0;
					}
			    	m_pattern = createPattern();
			    	calcLines();
			    	makeDirty();
				} catch (Exception e) {}
			}
		}
	}; // class MyAction
	
	
	Action a_metadatascale = new MyAction("Meta data scale", "Meta data scale", "metadatascale", "") {
		private static final long serialVersionUID = -2L;
		public void actionPerformed(ActionEvent ae) {
			String sScale = JOptionPane.showInputDialog("Meta data scale:", m_treeDrawer.LINE_WIDTH_SCALE + "");
			if (sScale != null) {
				try {
					m_treeDrawer.LINE_WIDTH_SCALE = Float.parseFloat(sScale);
					makeDirty();
				} catch (Exception e) {}
			}
		}
	}; // class MetaDataScale


	JCheckBoxMenuItem m_viewEditTree;
	JCheckBoxMenuItem m_viewClades; 

	
	
	
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
		m_jTbTools2.add(createToolBarButton(action));
//		m_jTbTools2.add(createToolBarButton(action2));
		m_jTbTools2.add(createToolBarButton(action3));
		m_jTbTools2.add(createToolBarButton(action4));
		m_jTbTools2.add(createToolBarButton(action5));
		m_jTbTools2.addSeparator();
		
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
		m_jTbTools2.add(createToolBarButton(action6));
		m_jTbTools2.add(createToolBarButton(action7));
		m_jTbTools2.add(createToolBarButton(action8));
		m_jTbTools2.add(createToolBarButton(action9));
		m_jTbTools2.setOrientation(JToolBar.VERTICAL);

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

		m_viewClades = new JCheckBoxMenuItem("Show Clades", m_bViewEditTree);
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
		shuffleMenu.add(new ShuffleAction("Closest First", "Order closest leaf first", "", "1",
				NodeOrderer.CLOSEST_FIRST));
		shuffleMenu.add(new ShuffleAction("Single link", "Single link hierarchical clusterer", "", "2",
				NodeOrderer.SINGLE));
		shuffleMenu.add(new ShuffleAction("Complete link", "Complete link hierarchical clusterer", "", "3",
				NodeOrderer.COMPLETE));
		shuffleMenu.add(new ShuffleAction("Average link", "Average link hierarchical clusterer", "", "4",
				NodeOrderer.AVERAGE));
		shuffleMenu.add(new ShuffleAction("Mean link", "Mean link hierarchical clusterer", "", "5", NodeOrderer.MEAN));
		shuffleMenu.add(new ShuffleAction("Adjusted complete link", "Adjusted complete link hierarchical clusterer",
				"", "6", NodeOrderer.ADJCOMLPETE));
		// RRB: not for public release
		shuffleMenu.addSeparator();
		shuffleMenu.add(new ShuffleAction("Manual", "Manual", "", "", NodeOrderer.MANUAL));
		shuffleMenu.add(new ShuffleAction("By Geography", "By Geography", "", "", NodeOrderer.GEOINFO));
		shuffleMenu.add(new ShuffleAction("By meta data, all", "By meta data, show all paths", "", "7",
				NodeOrderer.META_ALL));
		shuffleMenu.add(new ShuffleAction("By meta data, sum", "By meta data, sum over paths", "", "8",
				NodeOrderer.META_SUM));
		shuffleMenu.add(new ShuffleAction("By meta data, mean", "By meta data, average over paths", "", "9",
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
		final JCheckBoxMenuItem a_viewCTrees = new JCheckBoxMenuItem("Show Consensus Trees", m_bViewCTrees);
		setIcon(a_viewCTrees, "viewctrees");
		a_viewCTrees.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean bPrev = m_bViewCTrees;
				m_bViewCTrees = a_viewCTrees.getState();
				if (bPrev != m_bViewCTrees) {
					makeDirty();
				}
			}
		});
		settingsMenu.add(a_viewCTrees);
		final JCheckBoxMenuItem viewAllTrees = new JCheckBoxMenuItem("Show All Trees", m_bViewAllTrees);
		viewAllTrees.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean bPrev = m_bViewAllTrees;
				m_bViewAllTrees = viewAllTrees.getState();
				if (bPrev != m_bViewAllTrees) {
					makeDirty();
				}
			}
		});
		settingsMenu.add(viewAllTrees);
		final JCheckBoxMenuItem viewRootCanal = new JCheckBoxMenuItem("Show Root Canal", m_bShowRootCanalTopology);
		viewRootCanal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean bPrev = m_bShowRootCanalTopology;
				m_bShowRootCanalTopology = viewRootCanal.getState();
				if (bPrev != m_bShowRootCanalTopology) {
					m_Panel.clearImage();
					makeDirty();
				}
			}
		});
		settingsMenu.add(viewRootCanal);
		
		final JCheckBoxMenuItem viewRootAtTop = new JCheckBoxMenuItem("Show Root At Top", m_treeDrawer.m_bRootAtTop);
		viewRootAtTop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean bPrev = m_treeDrawer.m_bRootAtTop;
				m_treeDrawer.m_bRootAtTop = viewRootAtTop.getState();
				if (bPrev != m_treeDrawer.m_bRootAtTop) {
					fitToScreen();
					// makeDirty();
				}
			}
		});
		settingsMenu.add(viewRootAtTop);
		
		

		JMenu modeMenu = new JMenu("Method");
		settingsMenu.add(modeMenu);

		JRadioButtonMenuItem defaultMode = new JRadioButtonMenuItem("Default");
		defaultMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(0);
			}
		});
		modeMenu.add(defaultMode);

		JRadioButtonMenuItem starTreeMode = new JRadioButtonMenuItem("Star Tree");
		starTreeMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(1);
			}
		});
		modeMenu.add(starTreeMode);

		JRadioButtonMenuItem centralisedMode = new JRadioButtonMenuItem("Centralised");
		centralisedMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(2);
			}
		});
		modeMenu.add(centralisedMode);

		JRadioButtonMenuItem centralisedCorrectedMode = new JRadioButtonMenuItem("Centralised angle corrected");
		centralisedCorrectedMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectMode(3);
			}
		});
		modeMenu.add(centralisedCorrectedMode);
		modeMenu.addSeparator();
		modeMenu.add(a_angleThresholdUp);
		modeMenu.add(a_a_angleThresholdDown);

		m_modeGroup.add(defaultMode);
		m_modeGroup.add(starTreeMode);
		m_modeGroup.add(centralisedMode);
		m_modeGroup.add(centralisedCorrectedMode);
		m_modeGroup.setSelected(defaultMode.getModel(), true);


		
		JMenu styleMenu = new JMenu("Style");
		settingsMenu.add(styleMenu);
		
		JRadioButtonMenuItem triangleStyle = new JRadioButtonMenuItem("Triangle");
		triangleStyle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(0);
			}
		});
		styleMenu.add(triangleStyle);		
		JRadioButtonMenuItem blockStyle = new JRadioButtonMenuItem("Block");
		blockStyle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(1);
			}
		});
		styleMenu.add(blockStyle);		
		JRadioButtonMenuItem arcStyle = new JRadioButtonMenuItem("Arc");
		arcStyle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(2);
			}
		});
		styleMenu.add(arcStyle);		
		JRadioButtonMenuItem steepStyle = new JRadioButtonMenuItem("Steep arc");
		steepStyle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setStyle(3);
			}
		});
		styleMenu.add(steepStyle);		
		
		m_styleGroup.add(triangleStyle);
		m_styleGroup.add(blockStyle);
		m_styleGroup.add(arcStyle);
		m_styleGroup.add(steepStyle);
		m_styleGroup.setSelected(triangleStyle.getModel(), true);

		
		
		
		
		
		
		JMenu gridMenu = new JMenu("Grid");
		settingsMenu.add(gridMenu);

		JRadioButtonMenuItem gridModeNone = new JRadioButtonMenuItem("No grid");
		gridModeNone.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_nGridMode = GridMode.NONE;
				makeDirty();
			}
		});
		gridMenu.add(gridModeNone);

		JRadioButtonMenuItem gridModeShort = new JRadioButtonMenuItem("Short grid");
		gridModeShort.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_nGridMode = GridMode.SHORT;
				makeDirty();
			}
		});
		gridMenu.add(gridModeShort);

		JRadioButtonMenuItem gridModeFull = new JRadioButtonMenuItem("Full grid");
		gridModeFull.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_nGridMode = GridMode.FULL;
				makeDirty();
			}
		});
		gridMenu.add(gridModeFull);
		gridMenu.addSeparator();
		gridMenu.add(new ColorAction("Grid color ", "Grid color ", "", "", HEIGHTCOLOR));
		gridMenu.add(a_setgridfont);
		
		ButtonGroup gridgroup = new ButtonGroup();
		gridgroup.add(gridModeNone);
		gridgroup.add(gridModeShort);
		gridgroup.add(gridModeFull);
		gridgroup.setSelected(gridModeNone.getModel(), true);

		JMenu metaDataMenu = new JMenu("Meta data");
		settingsMenu.add(metaDataMenu);
		final JCheckBoxMenuItem metaDataAsLineWidth = new JCheckBoxMenuItem("Use meta data for line width", m_bMetaDataForLineWidth);
		metaDataAsLineWidth.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean bPrev = m_bMetaDataForLineWidth;
				m_bMetaDataForLineWidth = metaDataAsLineWidth.getState();
				if (bPrev != m_bMetaDataForLineWidth) {
					if (m_bMetaDataForLineWidth) {
						m_treeDrawer.setBranchDrawer(new TrapeziumBranchDrawer());
					} else {
						m_treeDrawer.setBranchDrawer(new BranchDrawer());
					}
					m_fLineWidth = null;
					m_fCLineWidth = null;
					m_fTopLineWidth = null;
					m_fTopCLineWidth = null;
					calcLines();
					makeDirty();
				}
			}
		});
		metaDataMenu.add(metaDataAsLineWidth);
		metaDataMenu.add(a_patternBottom);
		metaDataMenu.add(a_patternTop);
		metaDataMenu.add(a_pattern);
		metaDataMenu.add(a_metadatascale);
		
		final JCheckBoxMenuItem a_calcTopOfBranch = new JCheckBoxMenuItem("Correct top of branch", m_bCorrectTopOfBranch);
		a_calcTopOfBranch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean bPrev = m_bCorrectTopOfBranch;
				m_bCorrectTopOfBranch = a_calcTopOfBranch.getState();
				if (bPrev != m_bCorrectTopOfBranch) {
					calcLines();
					makeDirty();
				}
			}
		});
		metaDataMenu.add(a_calcTopOfBranch);		
		
		JMenu geoMenu = new JMenu("Geography");
		settingsMenu.add(geoMenu);
		
		final JCheckBoxMenuItem viewGeoInfo = new JCheckBoxMenuItem("Show Geo info", m_bDrawGeo);
		viewGeoInfo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean bPrev = m_bDrawGeo;
				m_bDrawGeo = viewGeoInfo.getState();
				if (bPrev != m_bDrawGeo) {
					makeDirty();
				}
			}
		});
		geoMenu.add(viewGeoInfo);
		geoMenu.add(a_loadkml);
		a_loadkml.setEnabled(m_nNrOfLabels > 0);
		geoMenu.add(a_geolinewidth);
		geoMenu.add(new ColorAction("Line color ", "Geo line color ", "", "", GEOCOLOR));
//		final JCheckBoxMenuItem logScale = new JCheckBoxMenuItem("Use Log scale for Height", m_bUseLogScale);
//		logScale.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				boolean bPrev = m_bUseLogScale;
//				m_bUseLogScale = logScale.getState();
//				if (bPrev != m_bUseLogScale) {
//					calcLines();
//					m_Panel.clearImage();
//					repaint();
//				}
//			}
//		});
//		settingsMenu.add(logScale);
		settingsMenu.addSeparator();
		JMenu labelMenu = new JMenu("Label");
		settingsMenu.add(labelMenu);
		labelMenu.add(a_setfont);
		labelMenu.add(a_labelwidth);
		labelMenu.add(new ColorAction("Label color ", "Label color ", "", "", LABELCOLOR));
		final JCheckBoxMenuItem rotateWhenRootAtTop = new JCheckBoxMenuItem("Rotate labels", m_bRotateTextWhenRootAtTop);
		rotateWhenRootAtTop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				m_bRotateTextWhenRootAtTop = rotateWhenRootAtTop.getState();
				fitToScreen();
			}
		});
		labelMenu.add(rotateWhenRootAtTop);

		JMenu colorMenu = new JMenu("Line Color");
		final JCheckBoxMenuItem viewMultiColor = new JCheckBoxMenuItem("Multi Color Consensus trees", m_bViewMultiColor);
		viewMultiColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				m_bViewMultiColor = viewMultiColor.getState();
				makeDirty();
			}
		});
		colorMenu.add(viewMultiColor);
		colorMenu.addSeparator();
		colorMenu.add(new ColorAction("Color 1", "Color of most popular topolgy", "", "", 0));
		colorMenu.add(new ColorAction("Color 2", "Color of second most popular topolgy", "", "", 1));
		colorMenu.add(new ColorAction("Color 3", "Color of third most popular topolgy", "", "", 2));
		colorMenu.add(new ColorAction("Default color ", "Default color ", "", "", 3));
		colorMenu.add(new ColorAction("Consensus color ", "Consensus tree color ", "", "", CONSCOLOR));
		colorMenu.add(new ColorAction("Background color ", "Background color ", "", "", BGCOLOR));
		colorMenu.add(new ColorAction("Root canal color ", "Root canal color ", "", "", ROOTCANALCOLOR));
		settingsMenu.add(colorMenu);

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
		settingsMenu.addSeparator();
		settingsMenu.add(a_burnin);


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

	/**
	 * Main method
	 */
	public static void main(String[] args) {
		DensiTree a = new DensiTree(args);
		JFrame f;
		f = new JFrame(FRAME_TITLE);
		f.setVisible(true);
		JMenuBar menuBar = a.getMenuBar();
		f.setJMenuBar(menuBar);
		f.add(a.m_jTbTools, BorderLayout.NORTH);
		f.add(a.m_jTbTools2, BorderLayout.EAST);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, a, a.m_jTbCladeTools);
		splitPane.setDividerLocation(0.9);
		f.add(splitPane, BorderLayout.CENTER);
		f.add(a.m_jStatusBar, BorderLayout.SOUTH);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Dimension dim = a.getSize();
		f.setSize(dim.width + 31, dim.height + 40 + 84);
		a.fitToScreen();
		java.net.URL tempURL = ClassLoader.getSystemResource(DensiTree.ICONPATH + "DensiTree.png");
		try {
			f.setIconImage(ImageIO.read(tempURL));
		} catch (Exception e) {
			// ignore
		}
		a.m_Panel.setFocusable(true);
		// a.fitToScreen();
	} // main

} // class DensiTree
