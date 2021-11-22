package viz;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Pattern;

import viz.DensiTree.LineColorMode;
import viz.DensiTree.LineWidthMode;
import viz.DensiTree.MetaDataType;

/** collection of drawing settings, like shape of tree, line width, line colour, etc. **/
public class Settings {
	/** flag indicating the attribute should be interpreted as categorial **/
	public boolean m_bColorByCategory = false;

	boolean m_bDrawReverse = false;

	/** whether to optimise branch lengths on root canal tree or not **/
	boolean m_bOptimiseRootCanalTree = false;

//	public Vector<String> m_sLabels;
	public int m_nImageSize = 20;
	public boolean m_bHideLabels = false;

	public BufferedImage[] m_LabelImages;
	/** labels of leafs **/
	/** nr of labels in dataset **/
	public int m_nNrOfLabels = 0;
	/** position information for the leafs (if available) **/
	public Vector<Float> m_fLongitude;
	public Vector<Float> m_fLatitude;
	boolean m_bInvertLongitude = false;
	/** extreme values for position information **/
	public float m_fMaxLong, m_fMaxLat, m_fMinLong, m_fMinLat;
	/** name of file containing locations **/
	String m_sKMLFile = null;

	public Vector<String> m_sLabels;

	/** smallest support for it to be considered a clade, default 1% **/
	public double m_smallestCladeSupport = 0.01;

	/** current directory for opening files **/
	String m_sDir = System.getProperty("user.dir");

	/**
	 * name of output file (if any) when batch processing. Typically used to
	 * dump a bitmap file in.
	 **/
	String m_sOutputFile = null;

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

	/** order of appearance of leafs, used to determine x-coordinates of leafs **/
	int[] m_nOrder;
	/** reverse of m_nOrder, useful for reordering **/
	int[] m_nRevOrder;
	/** file containing order of taxa -- if specified, the order will 
	 * be read from the file. If set, no other ordering is allowed **/
	String m_sOrderFile = null;


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
	public float m_fLabelIndent = 0.0f;

	/**
	 * Flag to indicate image should be recorded Frames that are drawn while
	 * refreshing screen are saved in /tmp/frame<nr>.jpg if possible.
	 */
	boolean m_bRecord = false;
	int m_nFrameNr;

	public boolean m_bViewEditTree = false;
	public boolean m_bViewClades = false;

	
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
	
	/** flag to indicate some meta data on the tree should be used for line widht **/
	//public boolean m_bMetaDataForLineWidth = false;
	/**
	 * Flag to indicate top of branch widths should be calculated from the bottom
	 * of branch lengths by distributing weight proportional to left and right
	 * bottom branches to top branch -- scaled to fit bottom of parent branch.
	 */
	public boolean m_bCorrectTopOfBranch = false;
//	/** indicator that only one group is in the pattern, so top of branch widths
//	 * should be calculated from the bottom of branch information.
//	 */
//	boolean m_bTopWidthDiffersFromBottomWidth;
	/** flag indicating meta data is zero based, instead of relative
	 * which makes minimum values at zero width.
	 */
	public boolean m_bWidthsAreZeroBased = true;
	
	/** array of various colors for color coding different topologies **/
	public Color[] m_color;

	/** variables that deal with width of lines **/
	public LineWidthMode m_lineWidthMode = LineWidthMode.DEFAULT;
	public LineWidthMode m_lineWidthModeTop = LineWidthMode.DEFAULT;
	LineWidthMode m_prevLineWidthMode = null;
	public String m_sLineWidthPattern = DensiTree.DEFAULT_PATTERN;
	public String m_sLineWidthPatternTop = DensiTree.DEFAULT_PATTERN;
	String m_sPrevLineWidthPattern = null;
	public String m_lineWidthTag;
	public String m_lineWidthTagTop;
	String m_prevLineWidthTag;

	/** variables that deal with coloring of lines **/
	public LineColorMode m_lineColorMode = LineColorMode.DEFAULT;
	public LineColorMode m_prevLineColorMode = null;
	public String m_sLineColorPattern = DensiTree.DEFAULT_PATTERN;
	String m_sPrevLineColorPattern = null;
	//List<String> m_colorMetaDataCategories = new ArrayList<String>();
	Map<String,Integer> m_colorMetaDataCategories = new HashMap<String, Integer>();
	public List<String> m_metaDataTags = new ArrayList<String>();
	public List<MetaDataType> m_metaDataTypes = new ArrayList<MetaDataType>();
	public String m_lineColorTag;
	String m_prevLineColorTag;
	public boolean m_showLegend = false;

	public boolean m_bShowRootCanalTopology = false;

	double m_cladeThreshold = 1e-4;

	/** regular expression pattern for finding width information in metadata **/
	public Pattern m_pattern;
	public Pattern m_patternTop;
	/** string containing reg exp for position matching **/
	public String m_sPattern = DensiTree.DEFAULT_PATTERN;
	public int m_iPatternForBottom = 1;
	public int m_iPatternForTop = 0;
	
	
	
}
