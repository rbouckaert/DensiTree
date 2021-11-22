package viz;

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

import javax.swing.DefaultListModel;
import javax.swing.JList;

import viz.process.BranchLengthOptimiser;

public class TreeData {
	Settings settings;
	DensiTree m_dt;
	
	TreeData(DensiTree dt, Settings settings) {
		m_dt = dt;
		this.settings = settings;
	}

	/** same trees, but represented as Node data structure **/
	public Node[] m_trees;
	
	public Node m_rootcanaltree;
	
	/** contains summary trees, root canal refers to one of those **/
	public List<Node> m_summaryTree = new ArrayList<Node>();

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
	public Set<Integer> m_cladeSelection = new HashSet<Integer>();

	
	
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


	void resetCladeSelection() {
		m_bAllowCladeSelection = false;
		m_cladelist.clearSelection();
		for (int i : m_cladeSelection) {
			m_cladelist.addSelectionInterval(i, i);
			if (m_cladeSelection.size() == 1) {
				m_cladelist.ensureIndexIsVisible(i);
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


	
	
}
