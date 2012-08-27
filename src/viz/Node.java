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
 * Node.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package viz;


import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** class for nodes in building tree data structure **/
public class Node {
	/** length of branch in the tree **/
	public float m_fLength = -1;
	/** height of the node. This is a derived variable from m_fLength **/
	//float m_fHeight = -1;
	/** x & y coordinate of node **/
	public float m_fPosX = 0;
	public float m_fPosY = 0;
	/** label nr of node, if this is a leaf, otherwise number of clade **/
	public int m_iLabel;
	public int m_iClade;
	public int getNr() {return m_iLabel;}
	/** metadata contained in square brackers in Newick **/
	private String m_sMetaData;
	/** meta data parsed as key-value pairs **/
	private Map<String, Object> metaDataMap;
	public static Map<String, Double> g_minValue = new HashMap<String, Double>();
	public static Map<String, Double> g_maxValue = new HashMap<String, Double>();
	/** meta data parsed as list of numbers **/
	private List<Double> metaDataList;
	public static List<Double> g_minListValue = new ArrayList<Double>();
	public static List<Double> g_maxListValue = new ArrayList<Double>();
	
	/** user data generated by other applications (e.g. DensiTreeG)**/
	public Object m_data = null;
	/** list of children of this node **/
	public Node m_left;
	public Node m_right;
	//Node[] m_children;
	/** parent node in the tree, null if root **/
	Node m_Parent = null;

	/** return parent node, or null if this is root **/
	public Node getParent() {
		return m_Parent;
	}

	public void setParent(Node parent) {
		m_Parent = parent;
	}

	/** check if current node is root node **/
	public boolean isRoot() {
		return m_Parent == null;
	}

	/** check if current node is a leaf node **/
	public boolean isLeaf() {
		return m_left == null;
	}

	/** count number of nodes in tree, starting with current node **/
	int getNodeCount() {
		if (isLeaf()) {
			return 1;
		}
		return 1 + m_left.getNodeCount() + m_right.getNodeCount();
	}

	/**
	 * print tree in Newick format, without any length or meta data
	 * information
	 **/
	public String toShortNewick() {
		StringBuffer buf = new StringBuffer();
		if (m_left != null) {
			buf.append("(");
			buf.append(m_left.toShortNewick());
			if (m_right != null) {
				buf.append(',');
				buf.append(m_right.toShortNewick());
			}
			buf.append(")");
		} else {
			buf.append(m_iLabel);
		}
		return buf.toString();
	}

	/**
	 * print tree in long Newick format, with all length and meta data
	 * information
	 **/
	public String toNewick() {
		StringBuffer buf = new StringBuffer();
		if (m_left != null) {
			buf.append("(");
			buf.append(m_left.toNewick());
			if (m_right != null) {
				buf.append(',');
				buf.append(m_right.toNewick());
			}
			buf.append(")");
		} else {
			buf.append(m_iLabel);
		}
//		if (m_sMetaData != null) {
//			buf.append('[');
//			buf.append(m_sMetaData);
//			buf.append(']');
//		}
//		buf.append(":" + m_fLength);
		return buf.toString();
	}

	/**
	 * print tree in long Newick format, with position meta data
	 * information
	 **/
	public String toNewickWithPos(double fMinLat, double fMaxLat, double fMinLong) {
		StringBuffer buf = new StringBuffer();
		if (m_left != null) {
			buf.append("(");
			buf.append(m_left.toNewickWithPos(fMinLat, fMaxLat, fMinLong));
			buf.append(',');
			buf.append(m_right.toNewickWithPos(fMinLat, fMaxLat, fMinLong));
			buf.append(")");
		} else {
			buf.append(m_iLabel);
		}
		buf.append("[pos=");
        DecimalFormat df = new DecimalFormat("#.##");
		buf.append(df.format(toLongitude(m_fPosX, fMinLat, fMaxLat)) + "x" + df.format(toLatitude(m_fPosY, fMinLong)));
		buf.append(']');
		buf.append(":" + m_fLength);
		return buf.toString();
	}
	double toLongitude(double fPosX, double fMinLat, double fMaxLat) {
		return fMaxLat - fPosX + (fMaxLat-fMinLat) / 100.0f;
	}
	double toLatitude(double fPosY, double fMinLong) {
		return fPosY + fMinLong;
	}
	/**
	 * print tree in long Newick format, with all length and meta data
	 * information, but with leafs labelled with their names
	 **/
	public String toString(Vector<String> sLabels, boolean bShowMetaData) {
		StringBuffer buf = new StringBuffer();
		if (m_left != null) {
			buf.append("(");
			buf.append(m_left.toString(sLabels, bShowMetaData));
			if (m_right != null) {
				buf.append(',');
				buf.append(m_right.toString(sLabels, bShowMetaData));
			}
			buf.append(")");
		} else {
			buf.append(sLabels.elementAt(m_iLabel));
		}
		if (bShowMetaData && getMetaData() != null) {
			buf.append('[');
			buf.append(getMetaData());
			buf.append(']');
		}
		buf.append(":" + m_fLength);
		return buf.toString();
	}
	public String toString() {
		return toNewick();
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
	public int drawDryWithSingleChild(float[] nX, float[] nY, int iPos, boolean[] bNeedsDrawing, boolean [] bSelection, float fOffset, float fScale) {
		if (isLeaf()) {
			bNeedsDrawing[0] = bSelection[m_iLabel];
		} else {
			boolean[] bChildNeedsDrawing = new boolean[2];
			iPos = m_left.drawDryWithSingleChild(nX, nY, iPos, bNeedsDrawing, bSelection, fOffset, fScale);
			bChildNeedsDrawing[0] = bNeedsDrawing[0];
			float dX;
			if (m_right != null) {
				iPos = m_right.drawDryWithSingleChild(nX, nY, iPos, bNeedsDrawing, bSelection, fOffset, fScale);
				bChildNeedsDrawing[1] = bNeedsDrawing[0];
				dX = m_fPosX - (m_left.m_fPosX + m_right.m_fPosX)/2;
			} else {
				bChildNeedsDrawing[1] = false;
				dX = m_fPosX - m_left.m_fPosX;
			}
			bNeedsDrawing[0] = false;
				if (bChildNeedsDrawing[0]) {
					nX[iPos] = m_left.m_fPosX;
					nY[iPos] = (m_left.m_fPosY - fOffset) * fScale;
					iPos++;
					nX[iPos] = nX[iPos - 1] + dX;//nX[iPos - 1];
					nY[iPos] = (m_fPosY - fOffset) * fScale;
					bNeedsDrawing[0] = true;
				} else {
					nX[iPos] = m_fPosX;
					nY[iPos] = (m_fPosY - fOffset) * fScale;
					iPos++;
					nX[iPos] = m_fPosX;
					nY[iPos] = (m_fPosY - fOffset) * fScale;
				}
				iPos++;
				if (bChildNeedsDrawing[1]) {
					nX[iPos] = m_right.m_fPosX + dX;
					nY[iPos] = nY[iPos - 1];
					iPos++;
					nX[iPos] = m_right.m_fPosX;//nX[iPos - 1];
					nY[iPos] = (m_right.m_fPosY - fOffset) * fScale;
					bNeedsDrawing[0] = true;
				} else {
					nX[iPos] = m_fPosX;
					nY[iPos] = (m_fPosY - fOffset) * fScale;
					iPos++;
					nX[iPos] = m_fPosX;
					nY[iPos] = (m_fPosY - fOffset) * fScale;
				}
				iPos++;
			if (isRoot()) {
				nX[iPos] = m_fPosX;
				nY[iPos] = (m_fPosY - fOffset) * fScale;
				iPos++;
				nX[iPos] = m_fPosX;
				nY[iPos] = (m_fPosY - m_fLength - fOffset) * fScale;
				iPos++;
			}
		}
		return iPos;
	}
	
	public int drawDry(float[] nX, float[] nY, int iPos, boolean[] bNeedsDrawing, boolean [] bSelection, float fOffset, float fScale) {
		if (isLeaf()) {
			bNeedsDrawing[0] = bSelection[m_iLabel];
		} else {
			boolean[] bChildNeedsDrawing = new boolean[2];
			iPos = m_left.drawDry(nX, nY, iPos, bNeedsDrawing, bSelection, fOffset, fScale);
			bChildNeedsDrawing[0] = bNeedsDrawing[0];
			iPos = m_right.drawDry(nX, nY, iPos, bNeedsDrawing, bSelection, fOffset, fScale);
			bChildNeedsDrawing[1] = bNeedsDrawing[0];
			bNeedsDrawing[0] = false;
			iPos = (m_iLabel - bSelection.length) * 4;
			float dX = m_fPosX - (m_left.m_fPosX + m_right.m_fPosX)/2;
				if (bChildNeedsDrawing[0]) {
					nX[iPos] = m_left.m_fPosX;
					nY[iPos] = (m_left.m_fPosY - fOffset) * fScale;
					iPos++;
					nX[iPos] = nX[iPos - 1] + dX;//nX[iPos - 1];
					nY[iPos] = (m_fPosY - fOffset) * fScale;
					bNeedsDrawing[0] = true;
				} else {
					nX[iPos] = m_fPosX;
					nY[iPos] = (m_fPosY - fOffset) * fScale;
					iPos++;
					nX[iPos] = m_fPosX;
					nY[iPos] = (m_fPosY - fOffset) * fScale;
				}
				iPos++;
				if (bChildNeedsDrawing[1]) {
					nX[iPos] = m_right.m_fPosX + dX;
					nY[iPos] = nY[iPos - 1];
					iPos++;
					nX[iPos] = m_right.m_fPosX;//nX[iPos - 1];
					nY[iPos] = (m_right.m_fPosY - fOffset) * fScale;
					bNeedsDrawing[0] = true;
				} else {
					nX[iPos] = m_fPosX;
					nY[iPos] = (m_fPosY - fOffset) * fScale;
					iPos++;
					nX[iPos] = m_fPosX;
					nY[iPos] = (m_fPosY - fOffset) * fScale;
				}
				iPos++;
			if (isRoot()) {
				nX[iPos] = m_fPosX;
				nY[iPos] = (m_fPosY - fOffset) * fScale;
				iPos++;
				nX[iPos] = m_fPosX;
				nY[iPos] = (m_fPosY - m_fLength - fOffset) * fScale;
				iPos++;
			}
		}
		return iPos;
	}

	
	public int drawDryCentralised(float[] nX, float[] nY, int iPos, boolean[] bNeedsDrawing, boolean [] bSelection, 
			float fOffset, float fScale, float[] taxonData, float [] fPosX, float [] fNonCentralisedPosX,
			float [] cladePosition) {
		if (isLeaf()) {
			bNeedsDrawing[0] = bSelection[m_iLabel];
			m_fPosX = cladePosition[m_iLabel];
			taxonData[0] = m_fPosX;
			fPosX[m_iLabel] = m_fPosX;
			fNonCentralisedPosX[m_iLabel] = m_fPosX;
			taxonData[1] = 1;
		} else {
			boolean[] bChildNeedsDrawing = new boolean[2];
			float [] taxonDataLeft = new float[2];
			iPos = m_left.drawDryCentralised(nX, nY, iPos, bNeedsDrawing, bSelection, fOffset, fScale, taxonDataLeft, fPosX, fNonCentralisedPosX, cladePosition);
			bChildNeedsDrawing[0] = bNeedsDrawing[0];
			float [] taxonDataRight = new float[2];
			iPos = m_right.drawDryCentralised(nX, nY, iPos, bNeedsDrawing, bSelection, fOffset, fScale, taxonDataRight, fPosX, fNonCentralisedPosX, cladePosition);
			bChildNeedsDrawing[1] = bNeedsDrawing[0];

			taxonData[0] = taxonDataLeft[0] + taxonDataRight[0];
			taxonData[1] = taxonDataLeft[1] + taxonDataRight[1];
			
			bNeedsDrawing[0] = false;
			fNonCentralisedPosX[m_iLabel] = (fNonCentralisedPosX[m_left.m_iLabel] + fNonCentralisedPosX[m_right.m_iLabel])/2;
			float dX = fNonCentralisedPosX[m_iLabel] - m_fPosX;
			fPosX[m_iLabel] = cladePosition[m_iClade] - dX; //taxonData[0]/taxonData[1] - dX;
			iPos = (m_iLabel - bSelection.length) * 4;
			if (bChildNeedsDrawing[0]) {
				nX[iPos] = fPosX[m_left.m_iLabel];
				nY[iPos] = (m_left.m_fPosY - fOffset) * fScale;
				iPos++;
				nX[iPos] = fPosX[m_iLabel];//nX[iPos - 1] + dX;//nX[iPos - 1];
				nY[iPos] = (m_fPosY - fOffset) * fScale;
				bNeedsDrawing[0] = true;
			} else {
				nX[iPos] = m_fPosX;
				nY[iPos] = (m_fPosY - fOffset) * fScale;
				iPos++;
				nX[iPos] = m_fPosX;
				nY[iPos] = (m_fPosY - fOffset) * fScale;
			}
			iPos++;
			if (bChildNeedsDrawing[1]) {
				nX[iPos] = fPosX[m_iLabel];//m_right.m_fPosX + dX;
				nY[iPos] = nY[iPos - 1];
				iPos++;
				nX[iPos] = fPosX[m_right.m_iLabel];//nX[iPos - 1];
				nY[iPos] = (m_right.m_fPosY - fOffset) * fScale;
				bNeedsDrawing[0] = true;
			} else {
				nX[iPos] = m_fPosX;
				nY[iPos] = (m_fPosY - fOffset) * fScale;
				iPos++;
				nX[iPos] = m_fPosX;
				nY[iPos] = (m_fPosY - fOffset) * fScale;
			}
			iPos++;

			if (isRoot()) {
				nX[iPos] = m_fPosX;
				nY[iPos] = (m_fPosY - fOffset) * fScale;
				iPos++;
				nX[iPos] = m_fPosX;
				nY[iPos] = (m_fPosY - m_fLength - fOffset) * fScale;
				iPos++;
			}
		}
		return iPos;
	}

	public int getStarTreeCladeCenters(float[] cladeCenterX, float[] cladeCenterY, float fOffset, float fScale, float [] cladePosition, float fMaxPosition) {
		if (isLeaf()) {
			m_fPosX = cladePosition[m_iLabel];
			if (m_fPosX > fMaxPosition - 0.25) {
				m_fPosX = fMaxPosition - 0.25f;
			}
			if (m_fPosX < 0.25) {
				m_fPosX = 0.25f;
			}
			cladeCenterX[m_iLabel] = m_fPosX;
			cladeCenterY[m_iLabel] = (m_fPosY - fOffset) * fScale;
			return 1;
		} else {
			int nNrOfNodesLeft = m_left.getStarTreeCladeCenters(cladeCenterX, cladeCenterY, fOffset, fScale, cladePosition, fMaxPosition);
			int nNrOfNodesRight = m_right.getStarTreeCladeCenters(cladeCenterX, cladeCenterY, fOffset, fScale, cladePosition, fMaxPosition);
			int nNrOfNodes = nNrOfNodesLeft + nNrOfNodesRight;
			cladeCenterX[m_iLabel] = (nNrOfNodesLeft * cladeCenterX[m_left.m_iLabel] + nNrOfNodesRight * cladeCenterX[m_right.m_iLabel]) / nNrOfNodes;
			cladeCenterY[m_iLabel] = (nNrOfNodesLeft * cladeCenterY[m_left.m_iLabel] + nNrOfNodesRight * cladeCenterY[m_right.m_iLabel]) / nNrOfNodes;
			return nNrOfNodes;
		}
	}
	
	public boolean drawStarTree(
			float[] nX, float[] nY, 
			float[] cladePosX, float[] cladePosY, float[] cladeCenterX, float[] cladeCenterY, 
			boolean[] bSelection,
			float fOffset, float fScale) {
		if (isLeaf()) {
			cladePosX[m_iLabel] = m_fPosX;
			cladePosY[m_iLabel] = (m_fPosY - fOffset) * fScale;
			return bSelection[m_iLabel];
		} else {
			if (isRoot()) {
				cladePosX[m_iLabel] = cladeCenterX[m_iLabel];
				cladePosY[m_iLabel] = (m_fPosY - fOffset) * fScale;				
			} else {
				float fParentPosX = cladePosX[m_Parent.m_iLabel]; 
				float fParentPosY = cladePosY[m_Parent.m_iLabel];
				float fCladeCenterX = cladeCenterX[m_iLabel];
				float fCladeCenterY = cladeCenterY[m_iLabel];
				cladePosY[m_iLabel] = (m_fPosY - fOffset) * fScale;
				cladePosX[m_iLabel] = fParentPosX + (fCladeCenterX - fParentPosX) * (cladePosY[m_iLabel] - fParentPosY) / (fCladeCenterY - fParentPosY);
			}
			boolean[] bChildNeedsDrawing = new boolean[2];
			bChildNeedsDrawing[0] = m_left.drawStarTree(nX, nY, cladePosX, cladePosY, cladeCenterX, cladeCenterY, bSelection, fOffset, fScale);
			bChildNeedsDrawing[1] = m_right.drawStarTree(nX, nY, cladePosX, cladePosY, cladeCenterX, cladeCenterY, bSelection, fOffset, fScale);

			int iPos = (m_iLabel - bSelection.length) * 4;
//			float dX = m_fPosX - (m_left.m_fPosX + m_right.m_fPosX)/2;
				if (bChildNeedsDrawing[0]) {
					nX[iPos] = cladePosX[m_left.m_iLabel];
					nY[iPos] = cladePosY[m_left.m_iLabel];
					iPos++;
					nX[iPos] = cladePosX[m_iLabel];
					nY[iPos] = cladePosY[m_iLabel];
				} else {
					nX[iPos] = cladePosX[m_iLabel];
					nY[iPos] = cladePosY[m_iLabel];
					iPos++;
					nX[iPos] = cladePosX[m_iLabel];
					nY[iPos] = cladePosY[m_iLabel];
				}
				iPos++;
				if (bChildNeedsDrawing[1]) {
					nX[iPos] = cladePosX[m_iLabel];
					nY[iPos] = cladePosY[m_iLabel];
					iPos++;
					nX[iPos] = cladePosX[m_right.m_iLabel];
					nY[iPos] = cladePosY[m_right.m_iLabel];
				} else {
					nX[iPos] = cladePosX[m_iLabel];
					nY[iPos] = cladePosY[m_iLabel];
					iPos++;
					nX[iPos] = cladePosX[m_iLabel];
					nY[iPos] = cladePosY[m_iLabel];
				}
				iPos++;
			if (isRoot()) {
				nX[iPos] = cladePosX[m_iLabel];
				nY[iPos] = cladePosY[m_iLabel];
				iPos++;
				nX[iPos] = cladePosX[m_iLabel];
				nY[iPos] = (m_fPosY - m_fLength - fOffset) * fScale;
				iPos++;
			}
			return bChildNeedsDrawing[0] || bChildNeedsDrawing[1];
		}
	}
	
	

	
//	/**
//	 * 'draw' tree into an array of x & positions. This draws the tree using
//	 * triangles
//	 * 
//	 * @param nX
//	 * @param nY
//	 * @param iPos
//	 * @return
//	 */
//	public int drawDryTriangle(float[] nX, float[] nY, int iPos, boolean[] bNeedsDrawing, boolean [] bSelection) {
//		if (isLeaf()) {
//			bNeedsDrawing[0] = bSelection[m_iLabel];
//		} else {
//			boolean[] bChildNeedsDrawing = new boolean[2];
//			iPos = m_left.drawDryTriangle(nX, nY, iPos, bNeedsDrawing, bSelection);
//			bChildNeedsDrawing[0] = bNeedsDrawing[0];
//			iPos = m_right.drawDryTriangle(nX, nY, iPos, bNeedsDrawing, bSelection);
//			bChildNeedsDrawing[1] = bNeedsDrawing[0];
//			bNeedsDrawing[0] = false;
//				if (bChildNeedsDrawing[0]) {
//					nX[iPos] = m_left.m_fPosX;
//					nY[iPos] = m_left.m_fPosY;
//					bNeedsDrawing[0] = true;
//				} else {
//					nX[iPos] = m_fPosX;
//					nY[iPos] = m_fPosY;
//				}
//				iPos++;
//				nX[iPos] = m_fPosX;
//				nY[iPos] = m_fPosY;
//				iPos++;
//				if (bChildNeedsDrawing[1]) {
//					nX[iPos] = m_right.m_fPosX;
//					nY[iPos] = m_right.m_fPosY;
//					bNeedsDrawing[0] = true;
//				} else {
//					nX[iPos] = m_fPosX;
//					nY[iPos] = m_fPosY;
//				}
//				iPos++;
//			}
//			if (isRoot()) {
//				nX[iPos] = m_fPosX;
//				nY[iPos] = m_fPosY;
//				iPos++;
//				nX[iPos] = m_fPosX;
//				nY[iPos] = m_fPosY - m_fLength;
//				iPos++;
//			}
//		return iPos;
//	}

	/**
	 * sorts nodes in children according to lowest numbered label in subtree
	 **/
	int sort() {
		if (m_left != null) {
			int iChild1 = m_left.sort();
			if (m_right != null) {
				int iChild2 = m_right.sort();
				if (iChild1 > iChild2) {
					Node tmp = m_left;
					m_left = m_right;
					m_right = tmp;
					return iChild2;
				}
			}
			return iChild1;
		}
		// this is a leaf node, just return the label nr
		return m_iLabel;
	} // sort
	
	/** during parsing, leaf nodes are numbered 0...m_nNrOfLabels-1
	 * but internal nodes are left to zero. After labeling internal
	 * nodes, m_iLabel uniquely identifies a node in a tree.  
	 */
	int labelInternalNodes(int iLabel) {
		if (isLeaf()) {
			return iLabel;
		} else {
			iLabel = m_left.labelInternalNodes(iLabel);
			if (m_right != null) {
				iLabel = m_right.labelInternalNodes(iLabel);
			}
			m_iLabel = iLabel++;
		}
		return iLabel;
	} // labelInternalNodes

	/** create deep copy **/
	Node copy() {
		Node node = new Node();
		node.m_fLength = m_fLength;
		node.m_fPosX = m_fPosX;
		node.m_fPosY = m_fPosY;
		node.m_iLabel = m_iLabel;
		node.m_iClade = m_iClade;
		node.setMetaData(m_sMetaData);
		node.m_Parent = null;
		if (m_left != null) {
			node.m_left = m_left.copy();
			node.m_left.m_Parent = node;
			if (m_right != null) {
				node.m_right = m_right.copy();
				node.m_right.m_Parent = node;
			} else {
				node.m_right = null;
			}
		}
		return node;
	} // copy
	
	float sigmoid(float fLength, float fMaxAngle) {
		if (fLength < 1.0*fMaxAngle) {
			return 0;
		} else {
			return 1;
		}
		
		//return (float) (1.0/(1.0 + Math.exp(-1-1.0*fLength / fMaxAngle)));
		//return (float) Math.min(1.0, Math.sqrt(1*fLength / fMaxAngle));

		//return (float) Math.min(1.0, (0.5*fLength / fMaxAngle));
	}

	public int doAngleCorrection(float[] nX, float[] nY, int iPos, boolean[] bNeedsDrawing, boolean [] bSelection, float fMaxAngle) {
		if (isLeaf()) {
			bNeedsDrawing[0] = bSelection[m_iLabel];
		} else {
			boolean[] bChildNeedsDrawing = new boolean[2];
			iPos = m_left.doAngleCorrection(nX, nY, iPos, bNeedsDrawing, bSelection, fMaxAngle);
			bChildNeedsDrawing[0] = bNeedsDrawing[0];
			iPos = m_right.doAngleCorrection(nX, nY, iPos, bNeedsDrawing, bSelection, fMaxAngle);
			bChildNeedsDrawing[1] = bNeedsDrawing[0];
			bNeedsDrawing[0] = false;
			iPos = (m_iLabel - bSelection.length) * 4;
			if (bChildNeedsDrawing[0]) {
				if (!m_left.isLeaf()) {
					float fWeight = sigmoid(m_left.m_fLength/Math.abs(nX[iPos] - nX[iPos+1]), fMaxAngle);// / ( m_left.m_fLength + m_right.m_fLength);
					nX[iPos] = nX[iPos] * fWeight + nX[iPos+1] * (1.0f - fWeight);
					nX[(m_left.m_iLabel - bSelection.length) * 4 + 1] = nX[iPos];
					nX[(m_left.m_iLabel - bSelection.length) * 4 + 2] = nX[iPos];
				}
				iPos++;
				//nX[iPos] = nX[iPos - 1] + dX;//nX[iPos - 1];
				bNeedsDrawing[0] = true;
			} else {
//					nX[iPos] = m_fPosX;
//					nY[iPos] = (m_fPosY - fOffset) * fScale;
				iPos++;
//					nX[iPos] = m_fPosX;
//					nY[iPos] = (m_fPosY - fOffset) * fScale;
			}
			iPos++;
			if (bChildNeedsDrawing[1]) {
//					nX[iPos] = m_right.m_fPosX + dX;
				iPos++;
				if (!m_right.isLeaf()) {
					float fWeight = sigmoid(m_right.m_fLength/Math.abs(nX[iPos-1] - nX[iPos]), fMaxAngle);// / ( m_left.m_fLength + m_right.m_fLength);
					nX[iPos] = nX[iPos] * fWeight + nX[iPos-1] * (1.0f - fWeight);
					nX[(m_right.m_iLabel - bSelection.length) * 4 + 1] = nX[iPos];
					nX[(m_right.m_iLabel - bSelection.length) * 4 + 2] = nX[iPos];
				}
				bNeedsDrawing[0] = true;
			} else {
//					nX[iPos] = m_fPosX;
//					nY[iPos] = (m_fPosY - fOffset) * fScale;
				iPos++;
//					nX[iPos] = m_fPosX;
//					nY[iPos] = (m_fPosY - fOffset) * fScale;
			}
			iPos++;
			if (isRoot()) { // root remains in the same place
//				nX[iPos] = m_fPosX;
//				nY[iPos] = (m_fPosY - fOffset) * fScale;
				iPos++;
//				nX[iPos] = m_fPosX;
//				nY[iPos] = (m_fPosY - m_fLength - fOffset) * fScale;
				iPos++;
			}
		}
		return iPos;
	}

	public String getMetaData() {
		return m_sMetaData;
	}

	public void setMetaData(String sMetaData) {
		this.m_sMetaData = sMetaData;
	}

	public void parseMetaData() {
		if (metaDataMap == null) {
			metaDataMap	= new HashMap<String, Object>();
			metaDataList = new ArrayList<Double>();
		}
		// parse by key=value pairs
		int i = 0;
		int start = 1;
		try {
			while ((i = m_sMetaData.indexOf('=', i)) >= 0) {
				String key = m_sMetaData.substring(start, i).trim();
				String value = null;
				int k = 0;
				if ((k = m_sMetaData.indexOf('=', i+1)) >= 0) {
					int j = m_sMetaData.lastIndexOf(',', k);
					value = m_sMetaData.substring(i + 1, j);
					start = j + 1;
				} else {
					value = m_sMetaData.substring(i+1);
				}
				if (value.length() > 0 && value.charAt(0)!='{') {
					try {
						Double dvalue = Double.parseDouble(value);
						if (!g_minValue.containsKey(key)) {
							g_minValue.put(key, dvalue);
						} else {
							if (g_minValue.get(key) > dvalue) {
								g_minValue.put(key, dvalue);
							}
						}
						if (!g_maxValue.containsKey(key)) {
							g_maxValue.put(key, dvalue);
						} else {
							if (g_maxValue.get(key) < dvalue) {
								g_maxValue.put(key, dvalue);
							}
						}
						metaDataMap.put(key, dvalue);
						
					} catch (Exception e) {
						metaDataMap.put(key, value);
					}
				} else {
					metaDataMap.put(key, value);
				}
				i++;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		// parse as list of numbers
		i = 0;
		start = 1;
		int iNumber = 0;
		//Pattern pattern = Pattern.compile("^([+-0-9\\.]+)([eE]-??[0-9]+)??.*");
		Pattern pattern = Pattern.compile("((-?[0-9]+(\\.[0-9]+)?)([eE]-?[0-9]+)?)");
		try {
			while (i < m_sMetaData.length()) {
				char c = m_sMetaData.charAt(i);
				if ((c >= '0' && c <= '9') || c=='+' || c=='-') {
					String str = m_sMetaData.substring(i);
					Matcher m = pattern.matcher(str);
					m.find();
					String mantissa = m.group(1);
					i  += mantissa.length();
					double value = 0;
					try {
						value = Double.parseDouble(mantissa);
					} catch (Exception e) {
						System.err.println(e.getMessage());
					}
					metaDataList.add(value);
					if (g_maxListValue.size() < metaDataList.size()) {
						g_maxListValue.add(Double.NEGATIVE_INFINITY);
						g_minListValue.add(Double.POSITIVE_INFINITY);
					}
					g_maxListValue.set(iNumber, Math.max(g_maxListValue.get(iNumber), value));
					g_minListValue.set(iNumber, Math.min(g_minListValue.get(iNumber), value));
					iNumber++;
				} else {
					i++;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public Map<String,Object> getMetaDataSet() {
		return metaDataMap;
	}
	public List<Double> getMetaDataList() {
		return metaDataList;
	}

} // class Node