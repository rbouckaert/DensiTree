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
 * HierarchicalClusterer.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
*/
/**
 <!-- globalinfo-start -->
 * Hierarchical clustering class.
 * Implements a number of classic hierarchical clustering methods.
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N
 *  number of clusters
 * </pre>
 * 
 * 
 * <pre> -L
 *  Link type (Single, Complete, Average, Mean, Centroid, Ward, Adjusted complete)
 *  [SINGLE|COMPLETE|AVERAGE|MEAN|CENTROID|WARD|ADJCOMLPETE]
 * </pre>
 * 
 * <pre> -A
 * Distance function to use. (default: weka.core.EuclideanDistance)
 * </pre>
 *
 * <pre> -P
 * Print hierarchy in Newick format, which can be used for display in other programs.
 * </pre>
 *  
 *<!-- options-end -->
 *
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz, remco@cs.waikato.ac.nz)
 * @version $Revision: 1 $
 */

package viz;


import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Vector;

import viz.process.spq.CladeConstrainor;
import viz.process.spq.Util;

import java.util.List;
import java.util.ArrayList;


public class NodeOrderer {
	/** constants for ordering algorithms**/
	public final static int GEOINFO = -5; // geography determines ordering
	public final static int MANUAL = -4; // user provides ordering
	public final static int DEFAULT = -3; // optimise
	public final static int CLOSEST_OUTSIDE_FIRST = -2; // closest outside heuristic
	public final static int CLOSEST_FIRST = -1; // closest first heuristic
	/** the various link types */
	public final static int SINGLE = 0; // hierarchical clustering
	public final static int COMPLETE = 1; // hierarchical clustering
	public final static int AVERAGE = 2; // hierarchical clustering
	public final static int MEAN = 3; // hierarchical clustering
	public final static int CENTROID = 4; // hierarchical clustering
	public final static int WARD = 5; // hierarchical clustering
	public final static int ADJCOMLPETE = 6; // hierarchical clustering
	public final static int OPTIMISE = 7;
	public final static int SORT_BY_ROOT_CANAL_LENGTH = 8;
	public final static int SPQ = 9;

	
	
	
	/** calc y-coordinate by meta data info.
	 * ALL = all paths from root to leafs
	 * SUM = sum over all paths from root to leafs
	 * AVERAGE = average over all paths **/
	public final static int META_ALL     = 100;
	public final static int META_SUM     = 101;
	public final static int META_AVERAGE = 102;
	
	
	/**
	 * Holds the Link type used calculate distance between clusters
	 */
	int m_nLinkType = SINGLE;

	public NodeOrderer(int nLinkType) {
		m_nLinkType = nLinkType;
	} // c'tor

	
	/** method for calculating a 'good' order of nodes for a set of trees.
	 * 
	 * @param nNrOfLabels: total nr of labels in a tree
	 * @param trees: complete set of trees
	 * @param cTrees: consensus trees
	 * @param fTreeWeight: weight of consensus tress
	 * @param nOrder: mapping of node labels onto order [0, ... ,nNrOfLabels-1]
	 * @return order of nodes [0, ... ,nNrOfLabels-1]
	 * @throws Exception
	 */
	public int [] calcOrder(int nNrOfLabels, Node [] trees, Node [] cTrees, Node rootCanalTree, float [] fTreeWeight, /*, int [] nOrder*/
			List<int[]> clades, List<Double> cladeWeights) throws Exception {
		if (m_nLinkType == SPQ) {
			return orderBySPQTrees(trees);
		}
		
		
		double [][] fDist = new double[nNrOfLabels][nNrOfLabels];
		for (int i = 0; i < cTrees.length; i++) {
			calcDistance(cTrees[i]/*, nOrder*/, fDist, fTreeWeight[i], new Vector<Integer>(), new Vector<Float>());
		}
		//print dist matrix (Denise):
//		System.out.println("Distance matrix:");
//		for (int i = 0; i < nNrOfLabels ; i++){
//		        for (int j = 0; j < nNrOfLabels ; j++){
//		             System.out.print(Math.round(fDist[i][j]*1000)/1000.00 + " | ");
//		          }
//		    System.out.println();
//		}

		int [] nOrder = null;
		int [] nRevOrder = null;
		if (m_nLinkType == CLOSEST_OUTSIDE_FIRST) {
			nRevOrder = closestOutsideFirst(fDist);
		} else if (m_nLinkType == CLOSEST_FIRST) {
			nRevOrder = closestFirst(fDist);
		} else {
			nOrder = buildClusterer(fDist);
		}
		if (m_nLinkType == OPTIMISE) {
			nRevOrder = new int[fDist.length];
			//traverse(rootCanalTree, nRevOrder, 0);
			
			int [] cladeCount = new int[fDist.length * 2];
			countCladeSize(rootCanalTree, cladeCount);
			traverse(rootCanalTree, nRevOrder, 0, cladeCount, 0);
		}
		if (m_nLinkType == SORT_BY_ROOT_CANAL_LENGTH) {
			nRevOrder = new int[fDist.length];
			traverse2(rootCanalTree, nRevOrder, 0);  
		}
		
		if (nRevOrder != null) {
			nOrder = new int[nNrOfLabels];
		    for (int i = 0; i < nNrOfLabels ; i++) {
		    	nOrder[nRevOrder[i]] = i;
		    }
		}
//		if (m_nLinkType == OPTIMISE) {xx
//			optimise(nOrder, nRevOrder, clades, cladeWeights, fDist);
//		}
		
		
	    System.out.print("nOrder: ");
	    for (int i = 0; i < nNrOfLabels ; i++) {
	    	System.out.print(nOrder[i] + " | ");
	    }
		System.out.println("\nfinal score = " + score(nOrder, fDist));

	    return nOrder;
	}

	private int traverse(Node node, int[] nOrder, int i) {
		if (node.isLeaf()) {
			nOrder[i] = node.m_iLabel;
			i++;
		} else {
			i = traverse(node.m_left, nOrder, i);
			i = traverse(node.m_right, nOrder, i);
		}
		return i;
	}

	
	private int countCladeSize(Node node, int [] cladeCount) {
		if (node == null) {
			return 0;
		}
		if (node.isLeaf()) {
			cladeCount[node.m_iLabel] = 1;
			return 1;
		} else {
			int count = countCladeSize(node.m_left, cladeCount) + 
					countCladeSize(node.m_right, cladeCount) + 1;
			cladeCount[node.m_iLabel] = count;
			return count;
		}
		
		
	}
	
	private int traverse(Node node, int[] nOrder, int i, int [] cladeCount, int direction) {
		if (node.isLeaf()) {
			nOrder[i] = node.m_iLabel;
			i++;
		} else {
			if (direction == 0) {
				if (cladeCount[node.m_left.m_iLabel] > cladeCount[node.m_right.m_iLabel]) {
					i = traverse(node.m_left, nOrder, i, cladeCount, 0);
					i = traverse(node.m_right, nOrder, i, cladeCount, 1);
				} else {
					i = traverse(node.m_right, nOrder, i, cladeCount, 0);
					i = traverse(node.m_left, nOrder, i, cladeCount, 1);
				}
			} else {
				if (cladeCount[node.m_left.m_iLabel] > cladeCount[node.m_right.m_iLabel]) {
					i = traverse(node.m_right, nOrder, i, cladeCount, 0);
					i = traverse(node.m_left, nOrder, i, cladeCount, 1);
				} else {
					i = traverse(node.m_left, nOrder, i, cladeCount, 0);
					i = traverse(node.m_right, nOrder, i, cladeCount, 1);
				}
			}
		}
		return i;
	}

	private int traverse2(Node node, int[] nOrder, int i) {
		if (node.isLeaf()) {
			nOrder[i] = node.m_iLabel;
			i++;
		} else {
			if (node.m_left.m_fLength < node.m_right.m_fLength) {
				i = traverse2(node.m_left, nOrder, i);
				i = traverse2(node.m_right, nOrder, i);
			} else {
				i = traverse2(node.m_right, nOrder, i);
				i = traverse2(node.m_left, nOrder, i);
			}
		}
		return i;
	}

	int [] orderBySPQTrees(Node [] trees) {
		CladeConstrainor constrainor = new CladeConstrainor(trees);
		//TreeConstrainor constrainor = new TreeConstrainor(trees2);
		constrainor.run();
	
		String seq = constrainor.getPossibleSequence();
		// System.out.println("Possible sequence:" + seq);
		System.out.println("Possible sequences:" + constrainor.getPossibleSequences());
		// System.out.print("Possible sequence:");
		String [] strs = seq.split("\\s");
		int n = Util.getLeafNodeCount(trees[0]);
		int [] nRevOrder = new int[n];
		for (int i = strs.length - 1; i >= 0; i--) {
			String str = strs[i];
			int j = Integer.parseInt(str);
			//System.out.print(taxa[j] + " ");
			nRevOrder[i] = j;
		}
		
		int [] nOrder = new int[n];
	    for (int i = 0; i < n ; i++) {
	    	nOrder[nRevOrder[i]] = i;
	    }

		return nOrder;
	}


//	private void optimise(int[] nOrder, int[] nRevOrder, List<int[]> clades, List<Double> cladeWeights, double [][] fDistance) {
//		double fSumWeight = 0;
//		int nTaxa = nOrder.length;
//		for (int i = nTaxa; i < clades.size(); i++) {
//			fSumWeight += cladeWeights.get(i);
//		}
//		Random rand = new Random(1);//System.currentTimeMillis());
//		double fScore = score(nOrder, fDistance);
//		int [] nNewOrder = new int[nOrder.length];
//		int [] nNewRevOrder = new int[nOrder.length];
//		System.out.println("Start score: " + fScore);
//		for (int i = 0; i < 10000; i++) {
//			double fWeight = fSumWeight * rand.nextDouble();
//			int iClade = nTaxa;
//			while(fWeight > cladeWeights.get(iClade)) {
//				fWeight -= cladeWeights.get(iClade);
//				iClade++;
//			}
//			
//			// rotate clade
//			System.arraycopy(nOrder, 0, nNewOrder, 0, nTaxa);
//			int [] clade = clades.get(iClade);
//			boolean [] bClade = new boolean[nTaxa];
//			for (int j = 0; j < clade.length; j++) {
//				bClade[nRevOrder[j]] = true;
//			}
//			int k = 0, e = nTaxa - 1;
//			while (!bClade[k]) {
//				k++;
//			}
//			while (!bClade[e]) {
//				e--;
//			}
//			while (k < e) {
//				int tmp = nNewOrder[k];
//				nNewOrder[k] = nNewOrder[e]; 
//				nNewOrder[e] = tmp;
//				k++; e--;
//			}
//			for (int j = 0; j < nRevOrder.length; j++) {
//				nNewRevOrder[nNewOrder[j]] = j;
//			}
//			
//			// accept new order if it is doing better
//			double fNewScore = score(nOrder, fDistance);
//			if (fScore > fNewScore) {
//				fScore = fNewScore;
//				System.arraycopy(nNewOrder, 0, nOrder, 0, nTaxa);
////				double fScore2 = score(nRevOrder, fDistance);
//				for (int j = 0; j < nRevOrder.length; j++) {
//					nRevOrder[nOrder[j]] = j;
//				}
//			}
//		}
//		
//		double fScore2 = score(nOrder, fDistance);
//		System.out.println("End score: " + fScore + " " + fScore2);
//	}

	/** used for priority queue for efficient retrieval of pair of clusters to merge**/
	class Tuple {
		public Tuple(double d, int i, int j, int nSize1, int nSize2) {
			m_fDist = d;
			m_iCluster1 = i;
			m_iCluster2 = j;
			m_nClusterSize1 = nSize1;
			m_nClusterSize2 = nSize2;
		}
		double m_fDist;
		int m_iCluster1;
		int m_iCluster2;
		int m_nClusterSize1;
		int m_nClusterSize2;
	}

	/** comparator used by priority queue**/
	class TupleComparator implements Comparator<Tuple> {
		@Override
		public int compare(Tuple o1, Tuple o2) {
			if (o1.m_fDist < o2.m_fDist) {
				return -1;
			} else if (o1.m_fDist == o2.m_fDist) {
				return 0;
			}
			return 1;
		}
	} // class TupleComparator

	/** class representing node in cluster hierarchy **/
	class TreeNode {
		TreeNode m_left;
		TreeNode m_right;
		TreeNode m_parent;
		int m_iLeftInstance;
		int m_iRightInstance;
		double m_height = 0;
		int m_iLabel = -1;

		/** fill order array by traversal through tree
		 * and using leaf id in order of appearance **/
		int order(int [] order, int i) {
			if (m_left == null) {
				order[m_iLeftInstance] = i++;
			} else {
				i = m_left.order(order, i);
			}
			if (m_right == null) {
				order[m_iRightInstance] = i++;
			} else {
				i = m_right.order(order, i);
			}
			return i;
		}
		
		int label(int i) {
			if (m_left == null) {
				return i;
			} else {
				i = m_left.label(i) + 1;
				m_iLabel = i+1;
			}
			if (m_right == null) {
				return i;
			} else {
				i = m_right.label(i) + 1;
				m_iLabel = i+1;
			}
			return i;
		}
	} // class TreeNode

	
	

	/**Order nodes by starting with two closest nodes. 
	 * Then add node left or right that is closest to the
	 * most left or most right node respectively, till all nodes are ordered.
	 * @param fDist
	 * @return
	 */
	int [] closestOutsideFirst(double [][] fDist) {
		int n = fDist.length;
		int [] nOrder = new int[n];
		boolean [] bDone = new boolean[n];
		
		// find the closest pair
		int iMax = 0;
		int jMax = 1;
		double fMax = fDist[0][1];
		for (int i = 0; i < n; i++) {
			for (int j = i+1; j < n; j++) {
				if (fDist[i][j] < fMax) {
					fMax = fDist[i][j];
					iMax = i;
					jMax = j;
				}
			}
		}
		nOrder[0] = iMax;
		nOrder[1] = jMax;
		bDone[iMax] = true;
		bDone[jMax] = true;
		// find the order of remaining nodes
		for (int k = 2; k < n; k++) {
			iMax = -1;
			jMax = -1;
			fMax = Double.MAX_VALUE;
			for (int i = 0; i < n; i++) {
				if (!bDone[i]) {
					if (fDist[nOrder[k-1]][i] < fMax) {
						fMax = fDist[nOrder[k-1]][i];
						iMax = k-1;
						jMax = i;
					}
					if (fDist[nOrder[0]][i] < fMax) {
						fMax = fDist[nOrder[0]][i];
						iMax = 0;
						jMax = i;
					}
				}
			}
			if (iMax == k-1) {
				nOrder[k] = jMax;
				bDone[jMax] = true;
			} else if (iMax == 0) {
				for (int j = k; j >0; j--) {
					nOrder[j] = nOrder[j-1];
				}
				nOrder[0] = jMax;
				bDone[jMax] = true;
			} else {
				System.err.println("Something's wrong");
			}
		}	
		return nOrder;
	} // closestOutsideFirst

	/** As closestOutsideFirst, but trying to match with *all* nodes that
	 * are already ordered. This should be more reasonable than closestOutsideFirst
	 * but does not appear to be so in practice (bug???)
	 * @param fDist
	 * @return
	 */
	int [] closestFirst(double [][] fDist) {
//		
//        int n = fDist.length;
//        int [] nOrder = new int[n];
//        boolean [] bDone = new boolean[n];
//
//        // find the closest pair
//        int iMax = 0;
//        int jMax = 1;
//        double fMax = fDist[0][1];
//        for (int i = 0; i < n; i++) {
//                for (int j = i+1; j < n; j++) {
//                        if (fDist[i][j] < fMax) {
//                                fMax = fDist[i][j];
//                                iMax = i;
//                                jMax = j;
//                        }
//                }
//        }
//nOrder[0] = iMax;
//        nOrder[1] = jMax;
//        bDone[iMax] = true;
//        bDone[jMax] = true;
//
//// find the order of remaining nodes - denise' code:
////nOrder to be filled from 2 to n-1
//for (int k = 2; k < n; k++) {
//    // find next node
//                jMax = -1;
//    boolean left = false;
//    fMax = Double.MAX_VALUE;
//
//       for (int j = 0; j < n; j++) {
//           if (!bDone[j]) {
//            // test who is closest to any ordered node
//               for (int i = 0; i < k; i++)
//                 if (j != nOrder[i] && fDist[nOrder[i]][j] < fMax){
//                    fMax = fDist[nOrder[i]][j];
//                    jMax = j;
//                    if (i < k/2) left = true;
//                        }
//       }
//       }
//    //System.out.println("jMax is " + jMax);
//    if (left){
//        for (int j = k; j > 0; j--){  nOrder[j] = nOrder[j-1]; }
//        nOrder[0] = jMax;
//    }
//    else{
//        nOrder[k] = jMax;
//    }
//    bDone[jMax] = true;
//}
//	return nOrder;
	
		int n = fDist.length;
		int [] nOrder = new int[n];
		boolean [] bDone = new boolean[n];
		
		// find the closest pair
		int iMax = 0;
		int jMax = 1;
		double fMax = fDist[0][1];
		for (int i = 0; i < n; i++) {
			for (int j = i+1; j < n; j++) {
				if (fDist[i][j] < fMax) {
					fMax = fDist[i][j];
					iMax = i;
					jMax = j;
				}
			}
		}
		nOrder[0] = iMax;
		nOrder[1] = jMax;
		bDone[iMax] = true;
		bDone[jMax] = true;
		// find the order of remaining nodes
		for (int k = 2; k < n; k++) {
			iMax = -1;
			jMax = -1;
			fMax = Double.MAX_VALUE;
			for (int j = 0; j < k; j++) {
				for (int i = 0; i < n; i++) {
					if (!bDone[i]) {
						//if (fDist[nOrder[nOrder[j]]][i] < fMax) {
						if (fDist[nOrder[j]][i] < fMax) {
							fMax = fDist[nOrder[j]][i];
							iMax = i;
							jMax = j;
						}
					}
				}
			}
			if (jMax == 0) { 
				//if (fDist[iMax][nOrder[0]]+fDist[nOrder[0]][nOrder[1]] > fDist[nOrder[0]][iMax]+fDist[iMax][nOrder[1]]) {				
				if (fDist[nOrder[0]][nOrder[1]] > fDist[iMax][nOrder[1]]) {				
					jMax++;
				}
			} else if (jMax == k-1) {
				//if (fDist[nOrder[k-2]][iMax]+fDist[iMax][nOrder[k-1]] > fDist[nOrder[k-2]][nOrder[k-1]] + fDist[nOrder[k-1]][iMax]) {
				if (fDist[nOrder[k-2]][iMax] > fDist[nOrder[k-2]][nOrder[k-1]]) {
					jMax++;
				}
			//} else if (fDist[nOrder[jMax-1]][iMax]+fDist[iMax][nOrder[jMax]]+fDist[nOrder[jMax]][nOrder[jMax+1]] >	fDist[nOrder[jMax-1]][nOrder[jMax]] + fDist[nOrder[jMax]][iMax]+fDist[iMax][nOrder[jMax+1]]) {					
			} else if (fDist[nOrder[jMax-1]][iMax] + fDist[nOrder[jMax]][nOrder[jMax+1]] >	fDist[nOrder[jMax-1]][nOrder[jMax]] + fDist[iMax][nOrder[jMax+1]]) {					
				jMax++;
			}
			for (int j = k; j > jMax; j--) {
				nOrder[j] = nOrder[j-1];
			}
			nOrder[jMax] = iMax;
			bDone[iMax] = true;
		}	
		return nOrder;
	} // closestFirst


	
	/** calculate the distance between leafs in a consensus tree
	 * and update the distance matrix weighted with the relative
	 * frequency of the tree
	 * @param node: current node
	 * @param nOrder: mapping of node label to [0,...,NrOfLeafs-1]
	 * @param fDistMatrix: distance matrix to be updated
	 * @param fWeight: relative consensus tree frequency
	 * @param iLabel: used to report set of leafs in sub tree below node
	 * @param fLength: used to report set of lengths from current node to leafs in iLabel
	 */
	void calcDistance(Node node, /*int [] nOrder, */double[][] fDistMatrix, double fWeight, Vector<Integer> iLabel, Vector<Float> fLength) {
		if (node == null) {
			return;
		}
		if (node.isLeaf()) {
			//iLabel.add(nOrder[node.m_iLabel]);
			iLabel.add(node.getNr());
			//fLength.add(node.m_fLength);
			fLength.add(node.m_fLength + 1.0f);
		} else {
			Vector<Integer> iLeft = new Vector<Integer>();
			Vector<Integer> iRight = new Vector<Integer>();
			Vector<Float> fLeft = new Vector<Float>();
			Vector<Float> fRight = new Vector<Float>();
			calcDistance(node.m_left, /*nOrder, */fDistMatrix, fWeight, iLeft, fLeft);
			calcDistance(node.m_right, /*nOrder, */fDistMatrix, fWeight, iRight, fRight);
			for (int i = 0; i < iLeft.size(); i++) {
				int i1 = iLeft.elementAt(i);
				double f = fWeight * fLeft.elementAt(i);
				for (int j = 0; j < iRight.size(); j++) {
					int i2 = iRight.elementAt(j);
					double f2 = f + fWeight * fRight.elementAt(j);
					fDistMatrix[i1][i2] += f2;
					fDistMatrix[i2][i1] += f2;
				}
			}
			for (int i = 0; i < fLeft.size(); i++) {
				iLabel.add(iLeft.elementAt(i));
				//fLength.add(fLeft.elementAt(i) + node.m_fLength);
				fLength.add(fLeft.elementAt(i) + node.m_fLength + 1.0f);
				//fLength.add(fLeft.elementAt(i) + 1.0f);
			}
			for (int i = 0; i < fRight.size(); i++) {
				iLabel.add(iRight.elementAt(i));
				//fLength.add(fRight.elementAt(i) + node.m_fLength);
				fLength.add(fRight.elementAt(i) + node.m_fLength + 1.0f);
				//fLength.add(fRight.elementAt(i) + 1.0f);
			}
		}
	} // calcDistance
	
	/** Perform one of the classical hierarchical clustering methods on a distance
	 * matrix. The resulting hierarchy is used to report an ordering on the node.  
	 * @param fDistance0: distance matrix
	 * @return order of leaf nodes
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public int [] buildClusterer(double [][] fDistance0) throws Exception {
		// use array of integer vectors to store cluster indices,
		// starting with one cluster per instance
		Vector<Integer> [] nClusterID = new Vector[fDistance0.length];
		for (int i = 0; i < fDistance0.length; i++) {
			nClusterID[i] = new Vector<Integer>();
			nClusterID[i].add(i);
		}
		// calculate distance matrix
		int nClusters = fDistance0.length;
		PriorityQueue<Tuple> queue = new PriorityQueue<Tuple>(nClusters*nClusters/2, new TupleComparator());
		for (int i = 0; i < nClusters; i++) {
			for (int j = i+1; j < nClusters; j++) {
				queue.add(new Tuple(fDistance0[i][j], i, j, 1, 1));
			}
		}

		int nInstances = fDistance0.length;
		
		// used for keeping track of hierarchy
		TreeNode [] clusterNodes = new TreeNode[nInstances];
		
		while (nClusters > 1) {
			// find closest two clusters
			/* simple but inefficient implementation
			double fMinDistance = Double.MAX_VALUE;
			int iMin1 = -1;
			int iMin2 = -1;
			for (int i = 0; i < nInstances; i++) {
				if (nClusterID[i] != null) {
					for (int j = i+1; j < nInstances; j++) {
						if (nClusterID[j] != null) {
							double fDist = fDistance[i][j];
							if (fDist < fMinDistance) {
								fMinDistance = fDist;
								iMin1 = i;
								iMin2 = j;
							}
						}
					}
				}
			}
			*/
			// use priority queue to find next best pair to cluster
			Tuple t = null;
			do {
				t = queue.poll();
			} while (t!=null && (nClusterID[t.m_iCluster1].size()!=t.m_nClusterSize1 || nClusterID[t.m_iCluster2].size()!=t.m_nClusterSize2));
			int iMin1 = t.m_iCluster1;
			int iMin2 = t.m_iCluster2;
			
			// merge  clusters
			nClusterID[iMin1].addAll(nClusterID[iMin2]);
			nClusterID[iMin2].removeAllElements();
			for (int i = 0; i < nInstances; i++) {
				if (i != iMin1 && nClusterID[i].size() > 0) {
					int i1 = Math.min(iMin1,i);
					int i2 = Math.max(iMin1,i);
					double fDistance = getDistance(fDistance0, nClusterID[i1], nClusterID[i2]);
					queue.add(new Tuple(fDistance, i1, i2, nClusterID[i1].size(), nClusterID[i2].size()));
				}
			}
			
			// track hierarchy
			TreeNode node = new TreeNode();
			if (clusterNodes[iMin1] == null) {
				node.m_iLeftInstance = iMin1;
				node.m_height = 1; 
			} else {
				node.m_left = clusterNodes[iMin1];
				clusterNodes[iMin1].m_parent = node;
				node.m_height = clusterNodes[iMin1].m_height + 1; 
			}
			if (clusterNodes[iMin2] == null) {
				node.m_iRightInstance = iMin2;
				node.m_height = Math.max(1,node.m_height); 
			} else {
				node.m_right = clusterNodes[iMin2];
				clusterNodes[iMin2].m_parent = node;
				node.m_height = Math.max(clusterNodes[iMin2].m_height + 1, node.m_height); 
			}
			clusterNodes[iMin1] = node;
			
			nClusters--;
		}
		
		//  collect hierarchy
		TreeNode cluster = null;
		//int iRoot = -1;
		for (int i = 0; i < nInstances; i++) {
			if (nClusterID[i].size() > 0) {
				cluster = clusterNodes[i];
				//iRoot = i;
				break;
			}
		}
		
		
		// optimise order
		int [] order = new int[nInstances];
		cluster.label(1);
		cluster.order(order, 0);
		if (true) {
//			return order;
		}
		
		double fScore = score(order, fDistance0);
		boolean bProgress = false;
		//int [][] orderings = new int[fDistance0.length][];
		do {
			bProgress = false;
			List<Integer> label = new ArrayList<Integer>();
			List<List<Integer>> orderings = calcOrderings(cluster, label);
			// find best node to flip, i.e. node that gives the best score
			double fBestScore = fScore;
			int iBestNode = -1;
			// first ordering is original ordering
			for (int i = 1; i < orderings.size(); i++) {
				double fScore2 = score2(orderings.get(i), fDistance0);
				//System.out.println("score = " + fScore2);
				if (fScore2 < fBestScore) {
					fBestScore = fScore2;
					iBestNode = label.get(i-1);
				}
			}
			// flip  left and right on iBestNode
			if (iBestNode >= 0) {
				flip(iBestNode, cluster);
				fScore = fBestScore;
				bProgress = true;
				
				cluster.order(order, 0);
				double fScore2 = score(order, fDistance0);
				System.out.println(Arrays.toString(order) + " " + fScore + " " + fScore2);
				
			}
		} while (bProgress);

		cluster.order(order, 0);
		return order;
	} // buildClusterer

	private void flip(int iBestNode, TreeNode node) {
		if (node.m_left != null) {
			flip(iBestNode, node.m_left);
		}
		if (node.m_right != null) {
			flip(iBestNode, node.m_right);
		}
		if (iBestNode == node.m_iLabel) {
			int tmp = node.m_iLeftInstance;
			node.m_iLeftInstance = node.m_iRightInstance;
			node.m_iRightInstance = tmp;
			TreeNode tmp2 = node.m_left;
			node.m_left = node.m_right;
			node.m_right = tmp2;
		}
	}


	private List<List<Integer>> calcOrderings(TreeNode node, List<Integer> label) {
		List<List<Integer>> leftList;
		List<List<Integer>> rightList;
		List<Integer> leftLabel = new ArrayList<Integer>();
		List<Integer> rightLabel = new ArrayList<Integer>();
		if (node.m_left == null) {
			List<Integer> list = new ArrayList<Integer>();
			list.add(node.m_iLeftInstance);
			leftList = new ArrayList<List<Integer>>();
			leftList.add(list);
		} else {
			leftList= calcOrderings(node.m_left, leftLabel);
		}
		if (node.m_right == null) {
			List<Integer> list = new ArrayList<Integer>();
			list.add(node.m_iRightInstance);
			rightList = new ArrayList<List<Integer>>();
			rightList.add(list);
		} else {
			rightList= calcOrderings(node.m_right, rightLabel);
		}
		
		List<List<Integer>> list = new ArrayList<List<Integer>>();
		{
			List<Integer> newList = new ArrayList<Integer>();
			newList.addAll(leftList.get(0));
			newList.addAll(rightList.get(0));
			list.add(newList);
		}
		for (int i = 1; i < leftList.size(); i++) {
			List<Integer> newList = new ArrayList<Integer>();
			newList.addAll(leftList.get(i));
			newList.addAll(rightList.get(0));
			list.add(newList);
		}
		for (int i = 1; i < rightList.size(); i++) {
			List<Integer> newList = new ArrayList<Integer>();
			newList.addAll(leftList.get(0));
			newList.addAll(rightList.get(i));
			list.add(newList);
		}
		{
			List<Integer> newList = new ArrayList<Integer>();
			newList.addAll(rightList.get(0));
			newList.addAll(leftList.get(0));
			list.add(newList);
		}
		label.addAll(leftLabel);
		label.addAll(rightLabel);
		label.add(node.m_iLabel);
		return list;
	}

	private double score2(List<Integer> list, double[][] fDistance0) {
		int [] order = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			order[list.get(i)] = i;
		}
		return score(order, fDistance0);
//		double fSum = 0;
//		for (int i = 0; i < list.size()-1; i++) {
//			fSum += fDistance0[list.get(i)][list.get(i+1)];
//		}
//		return fSum;
	}

	private double score(int[] order, double [][] fDistance0) {
//		System.out.println(Arrays.toString(order));

		final int K = order.length;
		double fSum = 0;
		for (int i = 0; i < order.length-1; i++) {
			for (int j = -K; j < 0; j++) {
				if (i+j>= 0)
					fSum -= fDistance0[order[i]][order[i+j]]/(j*j*j);
			}
			for (int j = 1; j <= K; j++) {
				if (i+j< order.length)
					fSum += fDistance0[order[i]][order[i+j]]/(j*j*j);
			}
		}
		return fSum;

	
		// correlation
//		final int K = order.length;
//		double fSum = 0;
//		
//		double fMeanX = 0;
//		double fMeanY = 0;
//		for (int i = 0; i < order.length; i++) {
//			for (int j = 0; j < order.length; j++) {
//				fMeanX += fDistance0[i][j];
//				fMeanY += Math.abs(i-j);
//			}
//		}
//		fMeanX /= (order.length*order.length);
//		fMeanY /= (order.length*order.length);
//
//		double fSum1 = 0;
//		double fVarX = 0;
//		double fVarY = 0;
//		for (int x = 0; x < order.length; x++) {
//			int i = order[x];
//			for (int y = 0; y < order.length; y++) {
//				int j = order[y];
//				double f = (fDistance0[i][j] - fMeanX) * (Math.abs(x-y) - fMeanY);
//				fSum1 += f;
//				fVarX += (fDistance0[i][j] - fMeanX) *(fDistance0[i][j] - fMeanX);
//				fVarY += (Math.abs(x-y) - fMeanY) * (Math.abs(x-y) - fMeanY);
//			}
//		}
//		return fSum1 / Math.sqrt(fVarX * fVarY);
	}


	/** calculate the distance between two clusters 
	 * @param cluster1 list of indices of instances in the first cluster
	 * @param cluster2 dito for second cluster
	 * @return distance between clusters based on link type
	 */
	double getDistance(double [][] fDistance, Vector<Integer> cluster1, Vector<Integer> cluster2) {
		double fBestDist = Double.MAX_VALUE;
		switch (m_nLinkType) {
		case SINGLE:
			// find single link distance aka minimum link, which is the closest distance between
			// any item in cluster1 and any item in cluster2
			fBestDist = Double.MAX_VALUE;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.elementAt(i);
				for (int j = 0; j < cluster2.size(); j++) {
					int i2  = cluster2.elementAt(j);
					double fDist = fDistance[i1][i2];
					if (fBestDist > fDist) {
						fBestDist = fDist;
					}
				}
			}
			break;
		case COMPLETE:
		case ADJCOMLPETE:
			// find complete link distance aka maximum link, which is the largest distance between
			// any item in cluster1 and any item in cluster2
			fBestDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.elementAt(i);
				for (int j = 0; j < cluster2.size(); j++) {
					int i2 = cluster2.elementAt(j);
					double fDist = fDistance[i1][i2];
					if (fBestDist < fDist) {
						fBestDist = fDist;
					}
				}
			}
			if (m_nLinkType == COMPLETE) {
				break;
			}
			// calculate adjustment, which is the largest within cluster distance
			double fMaxDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.elementAt(i);
				for (int j = i+1; j < cluster1.size(); j++) {
					int i2 = cluster1.elementAt(j);
					double fDist = fDistance[i1][i2];
					if (fMaxDist < fDist) {
						fMaxDist = fDist;
					}
				}
			}
			for (int i = 0; i < cluster2.size(); i++) {
				int i1 = cluster2.elementAt(i);
				for (int j = i+1; j < cluster2.size(); j++) {
					int i2 = cluster2.elementAt(j);
					double fDist = fDistance[i1][i2];
					if (fMaxDist < fDist) {
						fMaxDist = fDist;
					}
				}
			}
			fBestDist -= fMaxDist;
			break;
		case AVERAGE:
			// finds average distance between the elements of the two clusters
			fBestDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.elementAt(i);
				for (int j = 0; j < cluster2.size(); j++) {
					int i2 = cluster2.elementAt(j);
					fBestDist += fDistance[i1][i2];
				}
			}
			fBestDist /= (cluster1.size() * cluster2.size());
			break;
		case MEAN: 
			{
				// calculates the mean distance of a merged cluster (aka Group-average agglomerative clustering)
				Vector<Integer> merged = new Vector<Integer>();
				merged.addAll(cluster1);
				merged.addAll(cluster2);
				fBestDist = 0;
				for (int i = 0; i < merged.size(); i++) {
					int i1 = merged.elementAt(i);
					for (int j = i+1; j < merged.size(); j++) {
						int i2 = merged.elementAt(j);
						fBestDist += fDistance[i1][i2];
					}
				}
				int n = merged.size();
				fBestDist /= (n*(n-1.0)/2.0);
			}
			break;
//		case CENTROID:
//			// finds the distance of the centroids of the clusters
//			double [] fValues1 = new double[m_instances.numAttributes()];
//			double [] fValues2 = new double[m_instances.numAttributes()];
//			for (int i = 0; i < cluster1.size(); i++) {
//				Instance instance1 = m_instances.instance(cluster1.elementAt(i));
//				Instance instance2 = m_instances.instance(cluster1.elementAt(i));
//				for (int j = 0; j < m_instances.numAttributes(); j++) {
//					fValues1[j] += instance1.value(j);
//					fValues2[j] += instance2.value(j);
//				}
//			}
//			for (int j = 0; j < m_instances.numAttributes(); j++) {
//				fValues1[j] /= cluster1.size();
//				fValues2[j] /= cluster2.size();
//			}
//			// set up two instances for distance function
//			Instance instance1 = (Instance) m_instances.instance(cluster1.elementAt(0)).copy();
//			Instance instance2 = (Instance) m_instances.instance(cluster1.elementAt(0)).copy();
//			for (int j = 0; j < m_instances.numAttributes(); j++) {
//				instance1.setValue(j, fValues1[j]);
//				instance2.setValue(j, fValues1[j]);
//			}
//			fBestDist = m_DistanceFunction.distance(instance1, instance2);
//			break;
//		case WARD:
//			{
//				// finds the distance of the change in caused by merging the cluster.
//				// The information of a cluster is calculated as the error sum of squares of the
//				// centroids of the cluster and its members.
//				double ESS1 = calcESS(cluster1);
//				double ESS2 = calcESS(cluster2);
//				Vector<Integer> merged = new Vector<Integer>();
//				merged.addAll(cluster1);
//				merged.addAll(cluster2);
//				double ESS = calcESS(merged);
//				fBestDist = ESS * merged.size() - ESS1 * cluster1.size() - ESS2 * cluster2.size();
//			}
//			break;
		}
		return fBestDist;
	} // getDistance
//	/** calculated error sum-of-squares for instances wrt centroid **/
//	double calcESS(Vector<Integer> cluster) {
//		double [] fValues1 = new double[m_instances.numAttributes()];
//		for (int i = 0; i < cluster.size(); i++) {
//			Instance instance = m_instances.instance(cluster.elementAt(i));
//			for (int j = 0; j < m_instances.numAttributes(); j++) {
//				fValues1[j] += instance.value(j);
//			}
//		}
//		for (int j = 0; j < m_instances.numAttributes(); j++) {
//			fValues1[j] /= cluster.size();
//		}
//		// set up two instances for distance function
//		Instance centroid = (Instance) m_instances.instance(cluster.elementAt(0)).copy();
//		for (int j = 0; j < m_instances.numAttributes(); j++) {
//			centroid.setValue(j, fValues1[j]);
//		}
//		double fESS = 0;
//		for (int i = 0; i < cluster.size(); i++) {
//			Instance instance = m_instances.instance(cluster.elementAt(i));
//			fESS += m_DistanceFunction.distance(centroid, instance);
//		}
//		return fESS / cluster.size(); 
//	} // calcESS
	
} // class HierarchicalClusterer
