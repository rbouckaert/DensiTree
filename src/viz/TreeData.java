package viz;

import java.awt.Color;
import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;

import viz.DensiTree.LineWidthMode;
import viz.DensiTree.MetaDataType;
import viz.process.BranchLengthOptimiser;

public class TreeData {
	Settings settings;
	DensiTree m_dt;
	
	TreeData(DensiTree dt, Settings settings) {
		m_dt = dt;
		this.settings = settings;
	}

	final static int MODE_LEFT = 0;
	final static int MODE_RIGHT = 2;
	final static int MODE_CENTRE = 3;
	int drawMode = MODE_CENTRE;
	
	/** same trees, but represented as Node data structure **/
	public Node[] m_trees;
	
	public Node m_rootcanaltree;
	
	/** contains summary trees, root canal refers to one of those **/
	public List<Node> m_summaryTree = new ArrayList<Node>();

	
	RotationPoint[] m_rotationPoints = null;

	/**
	 * Trees represented as lines for drawing block trees Units are tree lengths
	 * as represented in the Newick file The lines come in quartets
	 * ((x1,y1)(x1,y2)(x3,y2),(x3,y3)) and are concatenated in a long array. The
	 * final pair contains the line to the root.
	 * **/
	float[][] m_fLinesX;
	float[][] m_fLinesY;
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
	public List<List<Double>> m_cladeHeightSetBottom;
	public List<List<Double>> m_cladeHeightSetTop;
	/** UI component for manipulating clade selection **/
	JList<String> m_cladelist;
	DefaultListModel<String> m_cladelistmodel = new DefaultListModel<String>();

	public Map<String, Integer> mapCladeToIndex;
	public Integer [] reverseindex;
	
	Comparator<Float> floatComparator = new Comparator<Float>() {
		@Override
		public int compare(Float o1, Float o2) {
			return Float.compare(Math.abs(o1), Math.abs(o2));
		}
	}; 
	
	/** represent clade as arrays of leaf indices **/
	Map<Integer, Double> m_cladePairs;
	
	List<List<ChildClade>> m_cladeChildren;
	/** X-position of the clade **/
	float[] m_cladePosition;
	
	
	/** flags whether a leaf node is selected **/
	public boolean[] m_bSelection;
	/** flag to indicate the selection was changed but image was not updated yet **/
	public boolean m_bSelectionChanged;

	boolean m_bAllowCladeSelection = true;
	private Set<Integer> m_cladeSelection = new HashSet<Integer>();
	public Set<Integer> getCladeSelection() {return m_cladeSelection;}
	
	
	public void calcPositions() {
		if (settings.m_sLabels == null) {
			// no trees loaded yet
			return;
		}
		m_dt.setWaitCursor();

		if (!settings.m_bAllowSingleChild && m_bCladesReady) {
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
	
			if (settings.m_bUseAngleCorrection) {
				for (int i = 0; i < m_clades.size(); i++) {
					if (m_cladeWeight.get(i) > settings.m_fAngleCorrectionThresHold) {
						for (ChildClade child : m_cladeChildren.get(i)) {
							if (m_cladeWeight.get(child.m_iLeft) < settings.m_fAngleCorrectionThresHold) {
								m_cladePosition[child.m_iLeft] = m_cladePosition[i];
							}
							if (m_cladeWeight.get(child.m_iRight) < settings.m_fAngleCorrectionThresHold) {
								m_cladePosition[child.m_iRight] = m_cladePosition[i];
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < m_trees.length; i++) {
			if (settings.m_nShuffleMode == NodeOrderer.GEOINFO) {
				positionLeafsGeo(m_trees[i]);
			} else {
				positionLeafs(m_trees[i]);
			}
			positionRest(m_trees[i]);
		}
		for (int i = 0; i < m_cTrees.length; i++) {
			if (settings.m_nShuffleMode == NodeOrderer.GEOINFO) {
				positionLeafsGeo(m_cTrees[i]);
			} else {
				positionLeafs(m_cTrees[i]);
			}
			positionRest(m_cTrees[i]);
		}
		if (!settings.m_bAllowSingleChild && m_bCladesReady) {
			for (Node tree : m_summaryTree) {
				positionLeafs(tree);
				positionRest(tree);
			}
		}
	}

	private float positionClades(int iClade) {
		float fMin = settings.m_nNrOfLabels;
		float fMax = 0;
		for (int i : m_clades.get(iClade)) {
			fMin = Math.min(fMin, settings.m_nOrder[i]);
			fMax = Math.max(fMax, settings.m_nOrder[i]);
		}
		return (fMin + fMax) / 2.0f + 0.5f;
	}

	/**
	 * Position leafs in a tree so that x-coordinate of the leafs is fixed for
	 * all trees in the set
	 * **/
	void positionLeafs(Node node) {
		if (node.isLeaf()) {
			node.m_fPosX = settings.m_nOrder[node.m_iLabel] + 0.5f;
			if (m_cladePosition != null) {
				//node.m_fPosX += m_cladePosition[node.m_iLabel];
			}
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
			if (m_dt.m_treeDrawer.m_bRootAtTop) {
				node.m_fPosX = settings.m_nNrOfLabels * (settings.m_fLongitude.elementAt(node.m_iLabel) - settings.m_fMinLong)
						/ (settings.m_fMaxLong - settings.m_fMinLong);
			} else {
				node.m_fPosX = settings.m_nNrOfLabels * (settings.m_fMaxLat - settings.m_fLatitude.elementAt(node.m_iLabel))
						/ (settings.m_fMaxLat - settings.m_fMinLat);
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





	void calcClades() {
		if (settings.m_bAllowSingleChild) {
			return;
		}
		m_clades = new ArrayList<int[]>();
		m_cladeWeight = new ArrayList<Double>();
		m_cladeHeight = new ArrayList<Double>();
		
		m_cladeHeight95HPDup = new ArrayList<Double>();
		m_cladeHeight95HPDdown = new ArrayList<Double>();
		
		m_cladeHeightSetBottom = new ArrayList<List<Double>>();
		m_cladeHeightSetTop = new ArrayList<List<Double>>();
		m_cladeChildren = new ArrayList<List<ChildClade>>();
		mapCladeToIndex = new HashMap<String, Integer>();

		
		// add leafs as clades
		for (int i = 0; i < settings.m_nNrOfLabels; i++) {
			int[] clade = new int[1];
			clade[0] = i;
			m_clades.add(clade);
			m_cladeWeight.add(1.0);
			m_cladeHeight.add(0.0);
			m_cladeHeight95HPDup.add(0.0);
			m_cladeHeight95HPDdown.add(0.0);
			m_cladeHeightSetBottom.add(new ArrayList<Double>());
			m_cladeHeightSetTop.add(new ArrayList<Double>());
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
			List<Double> heights = new ArrayList<Double>();
			heights.addAll(m_cladeHeightSetBottom.get(i));
			Collections.sort(heights);
			int upIndex = heights.size() * 190 / 200;
			int downIndex = heights.size() * 5 / 200;
			m_cladeHeight95HPDup.set(i, heights.get(upIndex));
			m_cladeHeight95HPDdown.set(i, heights.get(downIndex));
		}
		
		double fHeight0 = m_dt.m_fHeight;
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
				if (Math.abs(m_cladeWeight.get(o1) - m_cladeWeight.get(o2)) < settings.m_cladeThreshold) {
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
		List<List<Double>> cladeHeightSetBottom = new ArrayList<List<Double>>();
		List<List<Double>> cladeHeightSetTop = new ArrayList<List<Double>>();
		List<List<ChildClade>> cladeChildren = new ArrayList<List<ChildClade>>();
		for (int i = 0; i < m_cladePosition.length; i++) {
			clades.add(m_clades.get(index[i]));
			cladeWeight.add(m_cladeWeight.get(index[i]));
			cladeHeight.add(m_cladeHeight.get(index[i]));
			cladeHeight95HPDdown.add(m_cladeHeight95HPDdown.get(index[i]));
			cladeHeight95HPDup.add(m_cladeHeight95HPDup.get(index[i]));
			cladeChildren.add(m_cladeChildren.get(index[i]));
			cladeHeightSetBottom.add(m_cladeHeightSetBottom.get(index[i]));
			cladeHeightSetTop.add(m_cladeHeightSetTop.get(index[i]));
			
		}
		m_clades = clades;
		m_cladeWeight = cladeWeight;
		m_cladeHeight = cladeHeight;
		m_cladeHeight95HPDdown = cladeHeight95HPDdown;
		m_cladeHeight95HPDup = cladeHeight95HPDup;
		m_cladeChildren = cladeChildren;
		m_cladeHeightSetBottom = cladeHeightSetBottom;
		m_cladeHeightSetTop = cladeHeightSetTop;


		reverseindex = new Integer[m_cladePosition.length];
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

		
		m_cladePairs = new HashMap<Integer, Double>();
		for (int i = 0; i < m_cTrees.length; i++) {
			calcCladePairs(m_cTrees[i], m_fTreeWeight[i]);
		}
		
		
		// find tree topology with highest product of clade support of all its clades
		int iMaxCladeProbTopology = 0;
		//int iMaxMinCladeProbTopology = 0;
		double fMaxCladeProb = cladeProb(m_cTrees[0], true);
		double fMaxMinCladeProb = cladeProb(m_cTrees[0], false);
		//int iMaxCCDProbTopology = 0;
		double fMaxCCDProb = CCDProb(m_cTrees[0]);//, index);
		for (int i = 1; i < m_cTrees.length; i++) {
			double fCladeProb = cladeProb(m_cTrees[i], true);
			if (fCladeProb > fMaxCladeProb) {
				iMaxCladeProbTopology = i;
				fMaxCladeProb = fCladeProb;
			}
		}
		for (int i = 1; i < m_cTrees.length; i++) {
			double fMinCladeProb = cladeProb(m_cTrees[i], false);
			if (fMinCladeProb > fMaxMinCladeProb) {
				//iMaxMinCladeProbTopology = i;
				fMaxMinCladeProb = fMinCladeProb;
			}
		}
		for (int i = 1; i < m_cTrees.length; i++) {
			double fCCDProb = CCDProb(m_cTrees[i]);//, index);
			if (fCCDProb > fMaxCCDProb) {
				//iMaxCCDProbTopology = i;
				fMaxCCDProb = fCCDProb;
			}
		}
		
		m_summaryTree = new ArrayList<Node>();
		m_summaryTree.add(m_cTrees[iMaxCladeProbTopology].copy());
		cleanUpSummaryTree(m_summaryTree.get(0));

		if (settings.m_bOptimiseRootCanalTree) {
			BranchLengthOptimiser optimiser = new BranchLengthOptimiser(m_dt);
			optimiser.optimiseScore(m_summaryTree.get(0));
		}
		float fHeight = positionHeight(m_summaryTree.get(0), 0);
		offsetHeight(m_summaryTree.get(0), m_dt.m_fHeight - fHeight);
		
//		m_summaryTree.add(m_cTrees[iMaxMinCladeProbTopology].copy());
//		cleanUpSummaryTree(m_summaryTree.get(1));
//
//		m_summaryTree.add(m_cTrees[iMaxCCDProbTopology].copy());
//		cleanUpSummaryTree(m_summaryTree.get(2));
//
//		// construct max. clade weight tree
//		List<Node> nodes = new ArrayList<Node>();
//		List<int[]> cladeIDs = new ArrayList<int[]>();
//		for (int i = 0; i < settings.m_sLabels.size(); i++) {
//			int [] cladeID = new int[1];
//			cladeID[0] = i;
//			cladeIDs.add(cladeID);
//			Node node = new Node();
//			node.m_iLabel = i;
//			node.m_iClade = i;
//			nodes.add(node);
//		}
//		m_summaryTree.add(constructMaxCladeTree(cladeIDs, mapCladeToIndex, nodes, false));
//		m_summaryTree.get(3).sort();
//		resetCladeNr(m_summaryTree.get(3), reverseindex);
//		m_summaryTree.add(m_summaryTree.get(3).copy());
//		cleanUpSummaryTree(m_summaryTree.get(4));		
//
//		m_summaryTree.add(constructMaxCladeTree(cladeIDs, mapCladeToIndex, nodes, true));
//		m_summaryTree.get(5).sort();
//		resetCladeNr(m_summaryTree.get(5), reverseindex);
//		cleanUpSummaryTree(m_summaryTree.get(5));		
//		setHeightByClade(m_summaryTree.get(5));
						
		// add clades to GUI component
		updateCladeModel();

//		m_summaryTree[5] = m_cTrees[iMaxCladeProbTopology].copy();
		
		if (m_dt.m_sOptTree != null) {
			TreeFileParser parser = new TreeFileParser(settings.m_sLabels, null, null, 0);
			try {
				Node tree = parser.parseNewick(m_dt.m_sOptTree);
				tree.sort();
				tree.labelInternalNodes(settings.m_nNrOfLabels);
				float fTreeHeight = positionHeight(tree, 0);
				offsetHeight(tree, m_dt.m_fHeight - fTreeHeight);
				calcCladeIDForNode(tree, mapCladeToIndex);
				resetCladeNr(tree, reverseindex);
				m_summaryTree.add(tree);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (m_dt.m_optTree != null) {
			m_summaryTree.add(m_dt.m_optTree.copy());
		}

		m_rootcanaltree = m_summaryTree.get(0);
		
		// save memory
		m_cladeHeightSetBottom = null;
		m_cladeHeightSetTop = null;
	}
	
	private void cleanUpSummaryTree(Node summaryTree) {
		setHeightByClade(summaryTree);
		summaryTree.m_fLength = (float) (m_dt.m_fHeight - m_cladeHeight.get(summaryTree.m_iClade));
		float fHeight = positionHeight(summaryTree, 0);
		offsetHeight(summaryTree, m_dt.m_fHeight - fHeight);
	}

	
	public void updateCladeModel() {
		m_cladelistmodel.clear();
		List<String> list = cladesToString();
		for (int i = 0; i < list.size(); i++) {
			m_cladelistmodel.add(i, list.get(i));
		}
	}
	
	private void calcCladePairs(Node node, double fWeight) {
		if (!node.isLeaf()) {
			calcCladePairs(node.m_left, fWeight);
			calcCladePairs(node.m_right, fWeight);
			int iCladeLeft = Math.min(node.m_left.m_iClade, node.m_right.m_iClade);
			int iCladeRight = Math.max(node.m_left.m_iClade, node.m_right.m_iClade);;
			Integer i = (iCladeRight << 16) + iCladeLeft;
			if (!m_cladePairs.containsKey(i)) {
				m_cladePairs.put(i, fWeight);
			} else {
				m_cladePairs.put(i, m_cladePairs.get(i) + fWeight);
			}
		}
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


	/** number of leafs in selection, handy for sanity checks **/
	int selectionSize() {
		int nSelected = 0;
		for (int i = 0; i < settings.m_nRevOrder.length; i++) {
			if (m_bSelection[settings.m_nRevOrder[i]]) {
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

	void resetCladeSelection() {
		m_bAllowCladeSelection = false;
		if (m_cladelist != null) {
			m_cladelist.clearSelection();
			for (int i : m_cladeSelection) {
				m_cladelist.addSelectionInterval(i, i);
				if (m_cladeSelection.size() == 1) {
					m_cladelist.ensureIndexIsVisible(i);
				}
			}
		}
		
		if (m_cladeSelection.size() > 0) {
		Arrays.fill(m_bSelection, false);
			for (int i : m_cladeSelection) {
				for (int j = 0; j < m_clades.get(i).length; j++) {
					m_bSelection[m_clades.get(i)[j]] = true;
				}
		}
		}
		m_bAllowCladeSelection = true;
	}

	
	public void resetCladeNr(Node node, Integer[] reverseindex) {
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
			if (m_cladeWeight.get(i) >= settings.m_smallestCladeSupport) {
				String sStr = "";
				//if (m_clades.get(i).length > 1) {
					sStr += format.format(m_cladeWeight.get(i) * 100) + "% ";
					sStr += format.format((m_dt.m_fHeight - m_cladeHeight95HPDup.get(i)) * m_dt.m_fUserScale) + " ";
					sStr += format.format((m_dt.m_fHeight - m_cladeHeight95HPDdown.get(i)) * m_dt.m_fUserScale) + " ";
					sStr += "[";
					int j = 0;
					for (j = 0; j < m_clades.get(i).length - 1; j++) {
						sStr += (settings.m_sLabels.get(m_clades.get(i)[j]) + ",");
					}
					sStr += (settings.m_sLabels.get(m_clades.get(i)[j]) + "]\n");
					list.add(sStr);
				//}
			}
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

	private double CCDProb(Node node) { //, Integer [] index) {
		if (node.isLeaf()) {
			return 1.0;
		} else {
//			int iClade = node.m_iClade;
//			iClade = index[iClade];
//			int iCladeLeft = Math.min(index[node.m_left.m_iClade], index[node.m_right.m_iClade]);
//			iCladeLeft = index[iCladeLeft];
			int iCladeLeft = Math.min(node.m_left.m_iClade, node.m_right.m_iClade);
			int iCladeRight = Math.max(node.m_left.m_iClade, node.m_right.m_iClade);;

			Integer i = (iCladeRight << 16) + iCladeLeft;
			Double f = m_cladePairs.get(i);
			if (f == null) {
				f = m_cladePairs.get(i);
			}
			
			double fCladeProb = f;// / m_cladeWeight.get(node.m_iClade);
			fCladeProb *= CCDProb(node.m_left);//, index);
			fCladeProb *= CCDProb(node.m_right);//, index);
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
				m_cladeHeightSetBottom.add(new ArrayList<Double>());
				m_cladeHeightSetTop.add(new ArrayList<Double>());
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

//			Integer [] cladePair = new Integer[2];
//			cladePair[0] = iClade;
//			cladePair[1] = iCladeLeft;
			return clade;
		}

	}

	private int[] calcCladeForNode2(Node node, Map<String, Integer> mapCladeToIndex, double fWeight, double fHeight) {
		if (node.isLeaf()) {
			int[] clade = new int[1];
			clade[0] = node.getNr();
			node.m_iClade = node.getNr();
			m_cladeHeightSetBottom.get(node.m_iClade).add(fHeight);
			m_cladeHeightSetTop.get(node.m_iClade).add(fHeight - node.m_fLength);
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
			m_cladeHeightSetBottom.get(iClade).add(fHeight);
			m_cladeHeightSetTop.get(iClade).add(fHeight - node.m_fLength);
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

	public int[] calcCladeIDForNode(Node node, Map<String, Integer> mapCladeToIndex) {
		if (node.isLeaf()) {
			int[] clade = new int[1];
			clade[0] = node.getNr();
			node.m_iClade = node.getNr();
			return clade;
		} else {
			int[] cladeLeft = calcCladeIDForNode(node.m_left, mapCladeToIndex);
			int[] cladeRight = calcCladeIDForNode(node.m_right, mapCladeToIndex);
			int[] clade = mergeClades(cladeLeft, cladeRight);
			String sClade = Arrays.toString(clade);
			try {
				int iClade = mapCladeToIndex.get(sClade);
				node.m_iClade = iClade;
			} catch (Exception e) {
				// ignore
				node.m_iClade = 0;
			}
			return clade;
		}
	}
	
	
	/**
	 * record position information in position array (fPosX) used for undo/redo
	 **/
	void getPosition(Node node, float[] fPosX) {
		if (node.isLeaf()) {
			fPosX[settings.m_nOrder[node.m_iLabel]] = node.m_fPosX;
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
			node.m_fPosX = fPosX[settings.m_nOrder[node.m_iLabel]];
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
	public float positionHeight(Node node, float fOffSet) {
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
	public void offsetHeight(Node node, float f) {
		if (!node.isLeaf()) {
			offsetHeight(node.m_left, f);
			if (node.m_right != null) {
				offsetHeight(node.m_right, f);
			}
		}
		node.m_fPosY += f;
	}


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
	
	public void calcColors(boolean forceRecalc) {
		if (!forceRecalc) {
			if (settings.m_lineColorMode == settings.m_prevLineColorMode && settings.m_lineColorTag != null && settings.m_lineColorTag.equals(settings.m_prevLineColorTag)
					&& settings.m_sLineColorPattern.equals(settings.m_sPrevLineColorPattern)) {
				return;
			}
		}
		if (settings.m_sLabels == null) {
			// no trees loaded
			return;
		}
		m_dt.setWaitCursor();

		settings.m_prevLineColorMode = settings.m_lineColorMode; 
		settings.m_prevLineColorTag = settings.m_lineColorTag;
		settings.m_sPrevLineColorPattern = settings.m_sLineColorPattern;
		int nNodes = getNrOfNodes(m_trees[0]);
		switch (settings.m_lineColorMode) {
		case COLOR_BY_CLADE:
			m_nLineColor = new int[m_trees.length][];
			m_nCLineColor = new int[m_cTrees.length][];
			m_nRLineColor = new int[1][];
			for (int i = 0; i < m_trees.length; i++) {
				if (settings.m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_trees[i]);
				}
				m_nLineColor[i] = new int[nNodes * 2 + 2];
				colorTree(m_trees[i], m_nLineColor[i], 0);
			}
			if (settings.m_bAllowSingleChild) {
				break;
			}
			// calculate coordinates of lines for drawing consensus trees
			for (int i = 0; i < m_cTrees.length; i++) {
				int nTopologies = 0;
				//if (settings.m_bAllowSingleChild) {
				//	nNodes = getNrOfNodes(m_cTrees[i]);
				//}
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
			//if (settings.m_bAllowSingleChild) {
			//	break;
			//}
			m_nRLineColor[0] = new int[nNodes * 2 + 2];
			Arrays.fill(m_nRLineColor[0], settings.m_color[DensiTree.ROOTCANALCOLOR].getRGB());
			break;
		case BY_METADATA_PATTERN:
			settings.m_pattern = Pattern.compile(settings.m_sLineColorPattern);
			m_nLineColor = new int[m_trees.length][];
			m_nCLineColor = new int[m_cTrees.length][];
			m_nRLineColor = new int[1][];
			//m_colorMetaDataCategories = new ArrayList<String>();
			settings.m_colorMetaDataCategories = new HashMap<String, Integer>();
			for (int i = 0; i < m_trees.length; i++) {
				if (settings.m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_trees[i]);
				}
				m_nLineColor[i] = new int[nNodes * 2 + 2];
				colorTreeByMetaData(m_trees[i], m_nLineColor[i], 0);
			}
			if (settings.m_bAllowSingleChild) {
				break;
			}
			// calculate coordinates of lines for drawing consensus trees
			for (int i = 0; i < m_cTrees.length; i++) {
				int nTopologies = 0;
				//if (settings.m_bAllowSingleChild) {
				//	nNodes = getNrOfNodes(m_cTrees[i]);
				//}
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
			//if (settings.m_bAllowSingleChild) {
			//	break;
			//}
			m_nRLineColor[0] = new int[nNodes * 2 + 2];
			Arrays.fill(m_nRLineColor[0], settings.m_color[DensiTree.ROOTCANALCOLOR].getRGB());
			break;
		case COLOR_BY_METADATA_TAG:
			settings.m_pattern = Pattern.compile(settings.m_sPattern);
			m_nLineColor = new int[m_trees.length][];
			m_nCLineColor = new int[m_cTrees.length][];
			m_nRLineColor = new int[1][];
			//m_colorMetaDataCategories = new ArrayList<String>();
			settings.m_colorMetaDataCategories = new HashMap<String, Integer>();
			boolean colorByCategory = false;
			for (int i = 0; i < settings.m_metaDataTags.size(); i++) {
				if (settings.m_metaDataTags.get(i).equals(settings.m_lineColorTag)) {
					if (settings.m_metaDataTypes.get(i).equals(MetaDataType.STRING)) {
						colorByCategory = true;
					}
					break;
				}
			}
			for (int i = 0; i < m_trees.length; i++) {
				if (settings.m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_trees[i]);
				}
				m_nLineColor[i] = new int[nNodes * 2 + 2];
				colorTreeByMetaDataTag(m_trees[i], m_nLineColor[i], 0, colorByCategory);
			}
			if (settings.m_bAllowSingleChild) {
				break;
			}
			// calculate coordinates of lines for drawing consensus trees
			for (int i = 0; i < m_cTrees.length; i++) {
				int nTopologies = 0;
				// it is known settings.m_bAllowSingleChild = false at this point
				//if (settings.m_bAllowSingleChild) {
				//	nNodes = getNrOfNodes(m_cTrees[i]);
				//}
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
			Arrays.fill(m_nRLineColor[0], settings.m_color[DensiTree.ROOTCANALCOLOR].getRGB());
			break;
		case DEFAULT:
			m_nLineColor = new int[m_trees.length][];
			m_nCLineColor = new int[m_cTrees.length][];
			m_nRLineColor = new int[1][];
			for (int i = 0; i < m_trees.length; i++) {
				if (settings.m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_trees[i]);
				}
				m_nLineColor[i] = new int[nNodes * 2 + 2];
				int color = 0;
				switch (m_nTopologyByPopularity[i]) {
				case 0:
					color = settings.m_color[0].getRGB();
					break;
				case 1:
					color = settings.m_color[1].getRGB();
					break;
				case 2:
					color = settings.m_color[2].getRGB();
					break;
				default:
					color = settings.m_color[3].getRGB();
				}
				Arrays.fill(m_nLineColor[i], color);
			}
			for (int i = 0; i < m_cTrees.length; i++) {
				int color = settings.m_color[DensiTree.CONSCOLOR].getRGB();
				if (settings.m_bViewMultiColor) {
					color = settings.m_color[9 + (i % (settings.m_color.length - 9))].getRGB();
				}
				if (settings.m_bAllowSingleChild) {
					nNodes = getNrOfNodes(m_cTrees[i]);
				}
				m_nCLineColor[i] = new int[nNodes * 2 + 2];
				Arrays.fill(m_nCLineColor[i], color);
			}
			if (settings.m_bAllowSingleChild) {
				break;
			}
			m_nRLineColor[0] = new int[nNodes * 2 + 2];
			Arrays.fill(m_nRLineColor[0], settings.m_color[DensiTree.ROOTCANALCOLOR].getRGB());
			break;
		}
	} // calcColors


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
	
	private int colorTreeByMetaData(Node node, int[] nLineColor, int iPos) {
		if (!node.isLeaf()) {
			iPos = colorTreeByMetaData(node.m_left, nLineColor, iPos);
			if (node.m_right != null) {
				iPos = colorTreeByMetaData(node.m_right, nLineColor, iPos);
			}
			int color = settings.m_color[9 + getMetaDataCategory(node.m_left) % (settings.m_color.length - 9)].getRGB();
			nLineColor[iPos++] = color;
			nLineColor[iPos++] = color;
			if (node.m_right != null) {
				color = settings.m_color[9 + getMetaDataCategory(node.m_right) % (settings.m_color.length - 9)].getRGB();
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
		Object o = node.getMetaDataSet().get(settings.m_lineColorTag);
		if (colorByCategory || settings.m_bColorByCategory) {
			if (o != null) {
				if (settings.m_colorMetaDataCategories.get(o.toString()) == null) {
					settings.m_colorMetaDataCategories.put(o.toString(), settings.m_colorMetaDataCategories.size());
				}
//				if (!m_colorMetaDataCategories.contains(o)) {
//					m_colorMetaDataCategories.add(o.toString());
//				}
//				color = m_color[9 + m_colorMetaDataCategories.indexOf(o.toString()) % (m_color.length - 9)].getRGB();
				int i = settings.m_colorMetaDataCategories.get(o.toString());
				System.err.println(i + " " + (9 + i % (settings.m_color.length - 9)) + " " + settings.m_color.length);
				color = settings.m_color[9 + i % (settings.m_color.length - 9)].getRGB();
			}
		} else {
			if (o != null) {
				double frac = (((Double) o) - Node.g_minValue.get(settings.m_lineColorTag)) /
						(Node.g_maxValue.get(settings.m_lineColorTag) - Node.g_minValue.get(settings.m_lineColorTag));
				color = Color.HSBtoRGB((float) frac, 0.5f, 0.8f); 
			}
		}
		return color;
	}
	
	
	private int colorTree(Node node, int[] nLineColor, int iPos) {
		if (node != null && !node.isLeaf()) {
			iPos = colorTree(node.m_left, nLineColor, iPos);
			iPos = colorTree(node.m_right, nLineColor, iPos);
			int color = settings.m_color[9+node.m_iClade%9].getRGB();
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
					fWidth[iPos] = getGamma(node.m_left, 1, settings.m_lineWidthMode, settings.m_lineWidthTag, settings.m_pattern);
					if (settings.m_lineWidthModeTop == LineWidthMode.DEFAULT) {
						fWidthTop[iPos] = fWidth[iPos];
					} else {
						fWidthTop[iPos] = getGamma(node.m_left, 2, settings.m_lineWidthModeTop, settings.m_lineWidthTagTop, settings.m_patternTop);						
					}
					iPos++;
//					nX[iPos] = nX[iPos - 1];
//					nY[iPos] = node.m_fPosY;
					bNeedsDrawing[0] = true;
				} else {
					fWidth[iPos] = settings.m_nTreeWidth;
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
					fWidth[iPos] = getGamma(node.m_right, 1, settings.m_lineWidthMode, settings.m_lineWidthTag, settings.m_pattern);
					if (settings.m_lineWidthModeTop == LineWidthMode.DEFAULT) {
						fWidthTop[iPos] = fWidth[iPos];
					} else {
						fWidthTop[iPos] = getGamma(node.m_right, 2, settings.m_lineWidthModeTop, settings.m_lineWidthTagTop, settings.m_patternTop);
					}
					iPos++;
//					nX[iPos] = nX[iPos - 1];
//					nY[iPos] = node.m_right.m_fPosY;
					bNeedsDrawing[0] = true;
				} else {
//					nX[iPos] = node.m_fPosX;
//					nY[iPos] = node.m_fPosY;
					fWidth[iPos] = settings.m_nTreeWidth;
					iPos++;
//					nX[iPos] = node.m_fPosX;
//					nY[iPos] = node.m_fPosY;
				}
				fWidth[iPos] = fWidth[iPos-1];
				fWidthTop[iPos] = fWidthTop[iPos-1]; 
				iPos++;
				if (settings.m_lineWidthModeTop == LineWidthMode.DEFAULT && settings.m_bCorrectTopOfBranch) {
					float fCurrentWidth = getGamma(node, 1, settings.m_lineWidthMode, settings.m_lineWidthTag, settings.m_pattern);
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

	float getGamma(Node node , int nGroup, LineWidthMode mode, String tag, Pattern pattern) {
		try {
			if (mode == LineWidthMode.BY_METADATA_PATTERN) {
				String sMetaData = node.getMetaData();
				try {
					Matcher matcher = pattern.matcher(sMetaData);
					matcher.find();
					int nGroups = matcher.groupCount();
					if (nGroup > nGroups) {
						nGroup = 1;
					}
					String sMatch = matcher.group(nGroup);
			        float f = Float.parseFloat(sMatch);
			        return f;
				} catch (Exception e) {
				}
			} else if (mode == LineWidthMode.BY_METADATA_NUMBER) {
				int index = 0;
				if (nGroup == 1) {
					index = settings.m_iPatternForBottom - 1;
				} else {
					index = settings.m_iPatternForTop - 1;
					if (index < 0) {
						index = settings.m_iPatternForBottom - 1;
					}
				}
				if (index < 0) {
					index = 0;
				}
				return (float) (node.getMetaDataList().get(index)/Node.g_maxListValue.get(index));
			} else {
				Map<String,Object> map = node.getMetaDataSet();
				if (map != null) {
					Object o = map.get(tag);
					if (o != null) {
						try {
							if (settings.m_bWidthsAreZeroBased) {
								double frac = ((Double) o) /Node.g_maxValue.get(settings.m_lineWidthTag);
								return (float) frac;
							} else {
								double frac = (((Double) o) - Node.g_minValue.get(settings.m_lineWidthTag)) /
										(Node.g_maxValue.get(settings.m_lineWidthTag) - Node.g_minValue.get(settings.m_lineWidthTag));
								return (float) frac;								
							}
						} catch (Exception e) {
							// ignore
						}
					}
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return 1f;
	}


	
	/**
	 * calculate coordinates for lines in real coordinates This initialises the
	 * m_nLines,m_nTLines, m_nCLines and m_nCTLines arrays
	 **/
	public void calcLines() {
		final float m_fTreeScale = m_dt.m_fTreeScale;
		final float m_fTreeOffset = m_dt.m_fTreeOffset;
		final float m_fHeight = m_dt.m_fHeight;
		final double m_fExponent = m_dt.m_fExponent;
		
		checkSelection();
		if (m_trees.length == 0) {
			return;
		}
		m_dt.setWaitCursor();
		
		// calculate coordinates of lines for drawing trees
		int nNodes = getNrOfNodes(m_trees[0]);

		boolean[] b = new boolean[1];
		for (int i = 0; i < m_trees.length; i++) {
			// m_fLinesX[i] = new float[nNodes * 2 + 2];
			// m_fLinesY[i] = new float[nNodes * 2 + 2];
			if (settings.m_bAllowSingleChild) {
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
			if (settings.m_bAllowSingleChild) {
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
		if (!settings.m_bAllowSingleChild && m_bCladesReady) {
			calcLinesForNode(m_rootcanaltree, m_fRLinesX[0], m_fRLinesY[0]);
		}
		
		if (settings.m_bUseLogScale) {
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
		m_dt.m_w = 0;
		for (int i = 0; i < m_cTrees.length; i++) {
			float[] fCLines = m_fCLinesX[i];
			float fWeight = m_fTreeWeight[i];
			for (int j = 0; j < fCLines.length - 3; j += 4) {
				m_dt.m_w += Math.abs(fCLines[j + 1] - fCLines[j + 2]) * fWeight;
			}
		}
		calcColors(false);
		calcLineWidths(false);
	} // calcLines
	
	
	void calcLinesForNode(Node node, float [] fLinesX, float [] fLinesY) {
		final float m_fTreeScale = m_dt.m_fTreeScale;
		final float m_fTreeOffset = m_dt.m_fTreeOffset;

		boolean[] b = new boolean[1];
		if (settings.m_bAllowSingleChild) {
			node.drawDryWithSingleChild(fLinesX, fLinesY, 0, b, m_bSelection, m_fTreeOffset,
					m_fTreeScale);
		} else {
			switch (settings.m_Xmode) {
			case 0:
				node.drawDry(fLinesX, fLinesY, 0, b, m_bSelection, m_fTreeOffset, m_fTreeScale);
				break;
			case 1:
				node.drawDryCentralised(fLinesX, fLinesY, 0, b, m_bSelection, m_fTreeOffset,
						m_fTreeScale, new float[2], new float[settings.m_nNrOfLabels * 2 - 1],
						new float[settings.m_nNrOfLabels * 2 - 1], m_cladePosition);
				break;
			case 2:
				float[] fCladeCenterX = new float[settings.m_nNrOfLabels * 2 - 1];
				float[] fCladeCenterY = new float[settings.m_nNrOfLabels * 2 - 1];
				float[] fPosX = new float[settings.m_nNrOfLabels * 2 - 1];
				float[] fPosY = new float[settings.m_nNrOfLabels * 2 - 1];
				node.getStarTreeCladeCenters(fCladeCenterX, fCladeCenterY, m_fTreeOffset, m_fTreeScale, m_cladePosition, settings.m_sLabels.size());
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
			if (settings.m_lineWidthMode == settings.m_prevLineWidthMode && settings.m_lineWidthTag != null && settings.m_lineWidthTag.equals(settings.m_prevLineWidthTag)
					&& settings.m_sLineWidthPattern.equals(settings.m_sPrevLineWidthPattern)) {
				return;
			}
		} else {
			calcPositions();
			calcLines();
		}
		settings.m_prevLineWidthMode = settings.m_lineWidthMode;
		settings.m_prevLineWidthTag = settings.m_lineWidthTag;
		settings.m_sPrevLineWidthPattern = settings.m_sLineWidthPattern;
		m_dt.setWaitCursor();

		if (settings.m_sLabels == null) {
			// no trees loaded
			return;
		}
		
		if (settings.m_lineWidthMode == LineWidthMode.DEFAULT) {
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
		int nNodes = getNrOfNodes(m_trees[0]);

		if (settings.m_lineWidthMode == LineWidthMode.BY_METADATA_PATTERN) {
			settings.m_pattern = Pattern.compile(settings.m_sLineWidthPattern);
		}
		if (settings.m_lineWidthModeTop == LineWidthMode.BY_METADATA_PATTERN) {
			settings.m_patternTop = Pattern.compile(settings.m_sLineWidthPatternTop);
		}
//		if (m_lineWidthMode == LineWidthMode.BY_METADATA_NUMBER) {
//			m_pattern = createPattern();
//		}

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

	public boolean loadFromFile(String sFile, boolean resetHeight) {
		TreeFileParser parser = new TreeFileParser(m_dt);
		try {
			m_trees = parser.parseFile(sFile);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Problem loading file: " + e.getMessage(),
					"Help Message", JOptionPane.PLAIN_MESSAGE);
			e.printStackTrace();
			return false;
		}
		m_dt.m_nBurnIn = parser.m_nBurnIn;
		if (m_dt.m_iOptTree >= 0) {
			m_dt.m_optTree = m_trees[m_dt.m_iOptTree - m_dt.m_nBurnIn];
		}

		m_dt.a_loadkml.setEnabled(true);
		float fOffset = DensiTree.GEO_OFFSET;
		settings.m_fMaxLong = parser.m_fMaxLong + fOffset;
		settings.m_fMaxLat = parser.m_fMaxLat + fOffset;
		settings.m_fMinLong = parser.m_fMinLong - fOffset;
		settings.m_fMinLat = parser.m_fMinLat - fOffset;
		settings.m_nNrOfLabels = parser.m_nNrOfLabels;

		if (m_trees.length == 0) {
			settings.m_sLabels = null;
			JOptionPane.showMessageDialog(null, "No trees found in file\nMaybe burn in is too large?",
					"Help Message", JOptionPane.PLAIN_MESSAGE);
			return false;
		}

		// set up selection
		m_bSelection = new boolean[settings.m_sLabels.size()];
		m_dt.m_bLabelRectangle = new Rectangle[settings.m_sLabels.size()];
		m_dt.m_bGeoRectangle = new Rectangle[settings.m_sLabels.size()];
		for (int i = 0; i < m_bSelection.length; i++) {
			m_bSelection[i] = true;
			m_dt.m_bLabelRectangle[i] = new Rectangle();
			m_dt.m_bGeoRectangle[i] = new Rectangle();
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
		float maxHeight = m_dt.m_fHeight;
		for (int i = 0; i < m_trees.length; i++) {
			fHeights[i] = positionHeight(m_trees[i], 0);
			maxHeight = Math.max(maxHeight, fHeights[i]);
		}
		if (resetHeight) {
			m_dt.m_fHeight = maxHeight;
		}
		for (int i = 0; i < m_trees.length; i++) {
			offsetHeight(m_trees[i], m_dt.m_fHeight - fHeights[i]);
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
		
		
		
		// reserve memory for nodes of m_cTrees
		// reserveMemory(m_nTopologies * (m_nNrOfLabels*2-1));
		// calculate consensus trees
		int i = 0;
		int iOld = 0;
		int iConsTree = 0;
		m_fTreeWeight = new float[m_nTopologies];
		m_cTrees = new Node[m_nTopologies];
		Node tree = m_trees[0];
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
			offsetHeight(consensusTree, m_dt.m_fHeight - fHeight);
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

		return true;
	}


	/**
	 * move divide y-position of a tree with factor f. Useful to calculate
	 * consensus trees.
	 **/
	private void divideLength(Node node, float f) {
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
	private void addLength(Node src, Node target) {
		// assumes same topologies for src and target
		if (!src.isLeaf()) {
			addLength(src.m_left, target.m_left);
			if (src.m_right != null) {
				addLength(src.m_right, target.m_right);
			}
		}
		target.m_fLength += src.m_fLength;
	}


	public boolean reverse() {		
		return drawMode == MODE_RIGHT;
	}
}
