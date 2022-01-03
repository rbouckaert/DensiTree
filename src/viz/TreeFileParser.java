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
 * TreeFileParser.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package viz;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class TreeFileParser {
	/**
	 * default tree branch length, used when that info is not in the Newick tree
	 **/
	final static float DEFAULT_LENGTH = 0.001f;

	int m_nOffset = 0;
	/** labels of leafs **/
	Vector<String> m_sLabels;
	/** position information for the leafs (if available) **/
	Vector<Float> m_fLongitude;
	Vector<Float> m_fLatitude;
	/** extreme values for position information **/
	float m_fMaxLong, m_fMaxLat, m_fMinLong, m_fMinLat;
	/** nr of labels in dataset **/
	int m_nNrOfLabels;
	/** maps taxon number in mirror set to taxon number in original set **/
	int [] m_iLabelMap;
	/** burn in = nr of trees ignored at the start of tree file, can be set by command line option **/
	int m_nBurnIn = 0, m_nThin = 1;
	boolean m_bBurnInIsPercentage = true;
	//DensiTree m_densiTree;
	/** for memory saving, set to true **/
	boolean m_bSurpressMetadata = true;
	/** if there is no translate block. This solves issues where the taxa labels are numbers e.g. in generated tree data **/
	boolean m_bIsLabelledNewick = false;
	/** flag to indicate that single child nodes are allowed **/
	boolean m_bAllowSingleChild = false;
	
	public TreeFileParser(DensiTree densiTree) {
		//m_densiTree = densiTree;
		m_sLabels = densiTree.m_settings.m_sLabels;
		m_fLongitude = densiTree.m_settings.m_fLongitude;
		m_fLatitude = densiTree.m_settings.m_fLatitude;
		m_nBurnIn = densiTree.m_nBurnIn;
		m_nThin = densiTree.m_nThin;
		m_bBurnInIsPercentage = densiTree.m_bBurnInIsPercentage;
		m_fMinLat = 90; m_fMinLong = 180;
		m_fMaxLat = -90; m_fMaxLong = -180;
		m_bAllowSingleChild = densiTree.m_settings.m_bAllowSingleChild;
	} // c'tor
	
	public TreeFileParser(Vector<String> sLabels, Vector<Float> fLongitude, Vector<Float> fLatitude, int nBurnIn) {
		m_sLabels = sLabels;
		if (m_sLabels != null) {
			m_bIsLabelledNewick = true;
			m_nNrOfLabels = m_sLabels.size();
		}
		m_fLongitude = fLongitude;
		m_fLatitude = fLatitude;
		m_nBurnIn = nBurnIn;
		m_fMinLat = 90; m_fMinLong = 180;
		m_fMaxLat = -90; m_fMaxLong = -180;
	}
	
	public Node [] parseFile(String sFile) throws Exception {
		//Vector<String> sNewickTrees = new Vector<String>();
		List<Node> trees = new ArrayList<>();
		m_nOffset = 0;
		
		File file = new File(sFile);
		long nFileSize = file.length();
		
		// parse Newick tree file
		BufferedReader fin = new BufferedReader(new FileReader(sFile));
		String sStr = fin.readLine();
		nFileSize -= sStr.length();
		// grab translate block
		while (fin.ready() && sStr.toLowerCase().indexOf("translate") < 0) {
			sStr = fin.readLine();
			nFileSize -= sStr.length();
		}
		m_bIsLabelledNewick = false;
		m_nNrOfLabels = m_sLabels.size();
		boolean bAddLabels = (m_nNrOfLabels == 0);
		Vector<String> sLabelsFound  = new Vector<>();
		if (sStr.toLowerCase().indexOf("translate") < 0) {
			m_bIsLabelledNewick = true;
			// could not find translate block, assume it is a list of Newick trees instead of Nexus file
			fin.close();
			fin = new BufferedReader(new FileReader(sFile));

			int nBurnIn = m_nBurnIn;
			if (m_bBurnInIsPercentage) {
				nFileSize = file.length();
				nBurnIn = (int) (m_nBurnIn * nFileSize/ 100);
			}
			
			while (fin.ready() && m_nNrOfLabels == 0) {
				nFileSize = file.length();
				sStr = fin.readLine();
				if (m_bBurnInIsPercentage) {
					nBurnIn -= sStr.length();
				} else {
					nBurnIn--;
				}
				if (sStr.length() > 2 && sStr.indexOf("(") >= 0) {
					String sStr2 = sStr;
					sStr2 = sStr2.substring(sStr2.indexOf("("));
					while (sStr2.indexOf('[') >= 0) {
						int i0 = sStr2.indexOf('[');
						int i1 = sStr2.indexOf(']');
						sStr2 = sStr2.substring(0, i0) + sStr2.substring(i1 + 1);
					}
					sStr2 = sStr2.replaceAll("[;\\(\\),]"," ");
					sStr2 = sStr2.replaceAll(":\\s*[0-9\\.Ee-]+"," ");
					String [] sLabels = sStr2.split("\\s+");
					if (bAddLabels) {
						m_nNrOfLabels = 0;
						for (int i = 0; i < sLabels.length; i++) {
							if (sLabels[i].length() > 0) {
								m_sLabels.add(sLabels[i]);
								m_nNrOfLabels++;
							}
						}
					}
					if (nBurnIn < 0) {
						Node tree = parseNewick(sStr);
						tree.sort();
						tree.labelInternalNodes(m_nNrOfLabels);
						trees.add(tree);
					}
//					sNewickTrees.add(sStr);
				}
			}
			int iThin = 1;
			while (fin.ready()) {
				sStr = fin.readLine();
				if (sStr.length() > 2 && sStr.indexOf("(") >= 0) {
					if (iThin >= m_nThin) {
						iThin = 0;
						Node tree = parseNewick(sStr);
						tree.sort();
						tree.labelInternalNodes(m_nNrOfLabels);
						trees.add(tree);
						if (trees.size() % 100 ==0) {if (m_nNrOfLabels>=100||trees.size() % 1000 ==0) {System.err.print(trees.size() + " ");}}
					}
					iThin++;
//					sNewickTrees.add(sStr);
				}
			}
		} else {
			// read tree set from file, and store in individual strings
			sStr = fin.readLine();
			nFileSize -= sStr.length(); 
			//m_nNrOfLabels = 0;
			boolean bLastLabel = false;
			while (fin.ready() && !bLastLabel) {
				if (sStr.indexOf(";") >= 0) {
					sStr = sStr.replace(';',' ');
					sStr = sStr.trim();
					if (sStr.isEmpty()) {
						break;
					}
					bLastLabel = true;
				}
				sStr = sStr.replaceAll(",", "");
				sStr = sStr.replaceAll("^\\s+", "");
				//String[] sStrs = sStr.split("\\s+");
				
	        	// find first whitespace character in taxaTranslation
	        	int k = 0;
	        	while (k < sStr.length() && !Character.isWhitespace(sStr.charAt(k))) {
	        		k++;
	        	}
	        	String sLabel = null;
	        	if (k > 0) {
					sLabel = sStr.substring(k).trim();
					char s = sLabel.charAt(0);
					char e = sLabel.charAt(sLabel.length() - 1);
					if ((s == '\"' && e == '\"') || (s == '\'' && e == '\'')) {
						sLabel = sLabel.substring(1, sLabel.length() - 1);
					}
	        	}
				int iLabel = new Integer(sStr.substring(0, k)).intValue();
				//String sLabel = sStrs[1];
				if (m_sLabels.size() <= iLabel) {
					//m_sLabels.add("__dummy__");
					m_nOffset = 1;
				}
				// check if there is geographic info in the name
				if (sLabel.contains("(")) {
					int iStr = sLabel.indexOf('(');
					int iStr2 = sLabel.indexOf('x', iStr);
					if (iStr2 >= 0) {
						int iStr3 = sLabel.indexOf(')', iStr2);
						if (iStr3 >= 0) {
							float fLat = Float.parseFloat(sLabel.substring(iStr+1, iStr2));// + 180;
							float fLong = Float.parseFloat(sLabel.substring(iStr2+1, iStr3));// + 360)%360;
							if (fLat!=0 || fLong!=0) {
								m_fMinLat = Math.min(m_fMinLat, fLat);
								m_fMaxLat = Math.max(m_fMaxLat, fLat);
								m_fMinLong = Math.min(m_fMinLong, fLong);
								m_fMaxLong = Math.max(m_fMaxLong, fLong);
							}
							while (m_fLatitude.size() < m_sLabels.size()) {
								m_fLatitude.add(0f);
								m_fLongitude.add(0f);
							}
							m_fLatitude.add(fLat);
							m_fLongitude.add(fLong);
						}
					}
					sLabel = sLabel.substring(0, sLabel.indexOf("("));
				}
				if (bAddLabels) {
					m_sLabels.add(sLabel);
					m_nNrOfLabels++;
				} else {
					sLabelsFound.add(sLabel);
				}
				if (!bLastLabel) {
					sStr = fin.readLine();
					nFileSize -= sStr.length(); 
				}
			}
			
			m_iLabelMap = new int[m_nNrOfLabels];
			if (sLabelsFound.size() > 0) {
				for (int i = 0; i < m_nNrOfLabels; i++) {
					String label = sLabelsFound.get(i);
					boolean found = false;
					for (int j = 0; j < m_nNrOfLabels; j++) {
						if (m_sLabels.get(j).equals(label)) {
							m_iLabelMap[i] = j;
							found = true;
						}
					}
					if (!found) {
						throw new IllegalArgumentException("Taxon " + label + " found in mirror set that was not in original set");
					}
				}
			} else {
				for (int i = 0; i < m_nNrOfLabels; i++) {
					m_iLabelMap[i] = i;
				}
			}
			
			
			// read trees
			int nBurnIn = m_nBurnIn;
			if (m_bBurnInIsPercentage) {
				nBurnIn = (int) (m_nBurnIn * nFileSize/ 100);
			}
			
			//int k = 0;
			int iThin = 1;
			while (fin.ready()) {
				sStr = fin.readLine();
				if (m_bBurnInIsPercentage) {
					nBurnIn -= sStr.length();
				}
				sStr = sStr.trim();
				if (sStr.length() > 5) {
					String sTree = sStr.substring(0,5);
					if (sTree.toLowerCase().startsWith("tree ")) {
						//k++;
						if (nBurnIn <= 0) {
							if (iThin > m_nThin) {
								iThin = 0;
								int i = sStr.indexOf('(');
								if (i > 0) {
									sStr = sStr.substring(i);
								}
//						if (m_bSurpressMetadata) {
//							while (sStr.indexOf('[') >= 0) {
//								int i0 = sStr.indexOf('[');
//								int i1 = sStr.indexOf(']');
//								sStr = sStr.substring(0, i0) + sStr.substring(i1 + 1);
//							}
//						}
								Node tree = parseNewick(sStr);
								//System.err.println(k + " " + tree);
								tree.sort();
								tree.labelInternalNodes(m_nNrOfLabels);
								trees.add(tree);
								if (trees.size() % 100 ==0) {if (m_nNrOfLabels>=100||trees.size() % 1000 ==0) {System.err.print(trees.size() + " ");}}
							}
							iThin++;
							//sNewickTrees.add(sStr);
						} else {
							if (!m_bBurnInIsPercentage) {
								nBurnIn--;
							}
						}
					}
				}
			}
			fin.close();
			if (nBurnIn > 0) {
				System.err.println("WARNING: Burn-in too large, resetting burn-in to default");
				m_sLabels.clear();
				if (m_bBurnInIsPercentage) {					
					m_nBurnIn = 10;
				} else {
					m_nBurnIn = 0;
				}
				return parseFile(sFile);
			}
		}
		
		
		System.err.println();
		System.err.println("Geo: " +m_fMinLong + "x" + m_fMinLat + " " + m_fMaxLong + "x" + m_fMaxLat);
		return trees.toArray(new Node[1]);
	} // parseFile

//	/**
//	 * helper method for parsing Newick tree. It finds the split point of the
//	 * tree represented by sStr
//	 **/
//	int nextNode(String sStr, int i) {
//		int nBraces = 0;
//		char c = sStr.charAt(i);
//		do {
//			i++;
//			if (i < sStr.length()) {
//				c = sStr.charAt(i);
//				switch (c) {
//				case '(':
//					nBraces++;
//					break;
//				case ')':
//					nBraces--;
//					break;
//				default:
//					break;
//				}
//			}
//		} while (i < sStr.length() && (nBraces > 0 || (c != ',' && c != ')' && c != '(')));
//		if (i >= sStr.length() || nBraces < 0) {
//			return -1;
//		} else if (sStr.charAt(i) == ')') {
//			i++;
//			if (sStr.charAt(i) == ':') {
//				i++;
//				c = sStr.charAt(i);
//				while (i < sStr.length()
//						&& (c == '.' || c == '+' || c == '-' || Character.isDigit(c) || ((c == 'e' || c == 'E') && (sStr.charAt(i + 1) == '+' || sStr
//								.charAt(i + 1) == '-')))) {
//					i++;
//					if (i < sStr.length()) {
//						c = sStr.charAt(i);
//					}
//				}
//			}
//		}
//		return i;
//	} // nextNode
//
//	/**
//	 * convert string containing Newick tree into tree data structure but only
//	 * in the cleaned up format (no meta data allowed)
//	 * 
//	 * @param sStr
//	 * @return tree consisting of a Node
//	 */
//	Node parseNewick2(String sStr) throws Exception {
//		if (sStr == null || sStr.length() == 0) {
//			return null;
//		}
//		Node node = m_densiTree.new Node();
//		if (sStr.startsWith("(")) {
//			int i1 = nextNode(sStr, 0);
//			int i2 = nextNode(sStr, i1);
//			//node.m_children = new Node[2];
//			node.m_left = parseNewick(sStr.substring(1, i1));
//			node.m_left.m_Parent = node;
//			String sStr2 = sStr.substring(i1 + 1, (i2 > 0 ? i2 : sStr.length()));
//			node.m_right = parseNewick(sStr2);
//			node.m_right.m_Parent = node;
//			Node node2 = null;
//			if (i2 > 0 && i2 < sStr.length()-1 && sStr.charAt(i2+1)!=':'&& sStr.charAt(i2+1)!=';') {
//				// looks like it is a triple split, so we need another binaray node to represent this
//				int i3 = nextNode(sStr, i2);
//				node2 = m_densiTree.new Node();
//				String sStr3 = sStr.substring(i2 + 1, (i3 > 0 ? i3 : sStr.length()));
//				node2.m_left = parseNewick(sStr3);
//				node2.m_left.m_Parent = node2;
//				node2.m_right = node;
//				node2.m_fLength = 0;
//			}
//			
//			if (sStr.lastIndexOf('[') > sStr.lastIndexOf(')')) {
//				sStr = sStr.substring(sStr.lastIndexOf('['));
//				i2 = sStr.indexOf(']');
//				if (i2 < 0) {
//					throw new Exception("unbalanced square bracket found:" + sStr);
//				}
//				sStr2 = sStr.substring(1, i2);
//				node.m_sMetaData = sStr2;
//			}
//			if (sStr.lastIndexOf(':') > sStr.lastIndexOf(')')) {
//				sStr = sStr.substring(sStr.lastIndexOf(':'));
//				sStr = sStr.replaceAll("[,\\):;]", "");
//				try {
//					node.m_fLength = Float.parseFloat(sStr);
//				} catch (Exception e) {
//					node.m_fLength = (float) DEFAULT_LENGTH;
//				}
//			} else {
//				node.m_fLength = (float) DEFAULT_LENGTH;
//			}
//
//			if (node2 != null) {
//				node.m_Parent = node2;
//				float h1 = height(node.m_left); 
//				float h2 = height(node.m_right);
//				float h3 = height(node2.m_left); 
//				if (h1 > h2 && h1 > h3) {
//					Node n3 = node2.m_left;
//					node2.m_left = node.m_left;
//					node2.m_left.m_Parent = node2;
//					node.m_left = n3;
//					node.m_left.m_Parent = node;
//					node.m_fLength = (h1-(h1+h2+h3)/3.0f)/2.0f;
//					node2.m_left.m_fLength -= node.m_fLength; 
//				} else if (h3 > h1 && h3 > h2) {
//					node.m_fLength = (h3-(h1+h2+h3)/3.0f)/2.0f;
//					node2.m_left.m_fLength -= node.m_fLength; 
//					node = node2;
//				} else {
//					// h2 is largest of the lot
//					Node n3 = node2.m_left;
//					node2.m_left = node.m_right;
//					node2.m_left.m_Parent = node2;
//					node.m_right = n3;
//					node.m_right.m_Parent = node;
//					node.m_fLength = (h2-(h1+h2+h3)/3.0f)/2.0f;
//					node2.m_left.m_fLength -= node.m_fLength; 
//				}
//				node = node2;
//			}
//		} else {
//			// it is a leaf
//			if (sStr.contains("[")) {
//				// grab metadata
//				int i1 = sStr.indexOf('[');
//				int i2 = sStr.indexOf(']');
//				if (i2 < 0) {
//					throw new Exception("unbalanced square bracket found:" + sStr);
//				}
//				String sStr2 = sStr.substring(i1 + 1, i2);
//				sStr = sStr.substring(0, i1) + sStr.substring(i2 + 1);
//				node.m_sMetaData = sStr2;
//			}
//			if (sStr.indexOf(')') >= 0) {
//				sStr = sStr.substring(0, sStr.indexOf(')'));
//			}
//			sStr = sStr.replaceFirst("[,\\);]", "");
//			if (sStr.length() > 0) {
//				if (sStr.indexOf(':') >= 0) {
//					int iColon = sStr.indexOf(':');
//					node.m_iLabel = getLabelIndex(sStr.substring(0, iColon));
//					if (sStr.indexOf(':', iColon + 1) >= 0) {
//						int iColon2 = sStr.indexOf(':', iColon + 1);
//						node.m_fLength = Float.parseFloat(sStr.substring(iColon + 1, iColon2));
//					} else {
//						node.m_fLength = Float.parseFloat(sStr.substring(iColon + 1));
//					}
//				} else {
//					node.m_iLabel = getLabelIndex(sStr);
//					node.m_fLength = 1;
//				}
//			} else {
//				return null;
//			}
//		}
//		return node;
//	} // parseNewick
	
	/** Try to map sStr into an index. First, assume it is a number.
	 * If that does not work, look in list of labels to see whether it is there.
	 */
	private int getLabelIndex(String sStr) throws Exception {
		if (!m_bIsLabelledNewick) {
			try {
				int i = Integer.parseInt(sStr) - m_nOffset;
				return m_iLabelMap[i];
			} catch (Exception e) {
			}
		}
		for (int i = 0; i < m_nNrOfLabels; i++) {
			if (sStr.equals(m_sLabels.elementAt(i))) {
				return i;
			}
		}
		// sStr may have (double) qoutes missing
		for (int i = 0; i < m_nNrOfLabels; i++) {
			String sLabel = m_sLabels.elementAt(i);
			if (sLabel.startsWith("'") && sLabel.endsWith("'") ||
					sLabel.startsWith("\"") && sLabel.endsWith("\"")) {
				sLabel = sLabel.substring(1, sLabel.length()-1);
				if (sStr.equals(sLabel)) {
					return i;
				}
			}
		}
		// sStr may have extra (double) qoutes
		if (sStr.startsWith("'") && sStr.endsWith("'") ||
				sStr.startsWith("\"") && sStr.endsWith("\"")) {
			sStr = sStr.substring(1, sStr.length()-1);
			return getLabelIndex(sStr);
		}
		throw new Exception("Label '" + sStr + "' in Newick tree could not be identified");
	}
	

	 float height(Node node) {
		 if (node.isLeaf()) {
			 return node.m_fLength;
		 } else {
			 return node.m_fLength + Math.max(height(node.m_left), height(node.m_right));
		 }
	 }
	 
	 char [] m_chars;
	 int m_iTokenStart;
	 int m_iTokenEnd;
	 final static int COMMA = 1;
	 final static int BRACE_OPEN = 3;
	 final static int BRACE_CLOSE = 4;
	 final static int COLON = 5;
	 final static int SEMI_COLON = 8;
	 final static int META_DATA = 6;
	 final static int TEXT = 7;
	 final static int UNKNOWN = 0;
	 
	 int nextToken() {
		 m_iTokenStart = m_iTokenEnd;
		 while (m_iTokenEnd < m_chars.length) {
			 // skip spaces
			 while (m_iTokenEnd < m_chars.length && (m_chars[m_iTokenEnd] == ' ' || m_chars[m_iTokenEnd] == '\t')) {
				 m_iTokenStart++;
				 m_iTokenEnd++;
			 }
			 if (m_chars[m_iTokenEnd] == '(') {
				 m_iTokenEnd++;
				 return BRACE_OPEN;
			 }
			 if (m_chars[m_iTokenEnd] == ':') {
				 m_iTokenEnd++;
				 return COLON;
			 }
			 if (m_chars[m_iTokenEnd] == ';') {
				 m_iTokenEnd++;
				 return SEMI_COLON;
			 }
			 if (m_chars[m_iTokenEnd] == ')') {
				 m_iTokenEnd++;
				 return BRACE_CLOSE;
			 }
			 if (m_chars[m_iTokenEnd] == ',') {
				 m_iTokenEnd++;
				 return COMMA;
			 }
			 if (m_chars[m_iTokenEnd] == '[') {
				 m_iTokenEnd++;
				 while (m_iTokenEnd < m_chars.length && m_chars[m_iTokenEnd-1] != ']') {
					 m_iTokenEnd++;
				 }
				 return META_DATA;
			 }
			 while (m_iTokenEnd < m_chars.length && (m_chars[m_iTokenEnd] != ' ' && m_chars[m_iTokenEnd] != '\t'
				 && m_chars[m_iTokenEnd] != '('  && m_chars[m_iTokenEnd] != ')'  && m_chars[m_iTokenEnd] != '['
					 && m_chars[m_iTokenEnd] != ':'&& m_chars[m_iTokenEnd] != ','&& m_chars[m_iTokenEnd] != ';')) {
				 m_iTokenEnd++;
			 }
			 return TEXT;
		 }
		 return UNKNOWN;
	 }

	 public Node parseNewick(String sStr) throws Exception {
		 try {
		if (sStr == null || sStr.length() == 0) {
			return null;
		}
		
		m_chars = sStr.toCharArray();
		m_iTokenStart = sStr.indexOf('(');
		if (m_iTokenStart < 0) {
			return null;
		}
		m_iTokenEnd = m_iTokenStart;
		Vector<Node> stack = new Vector<Node>();
		Vector<Boolean> isFirstChild =  new Vector<Boolean>();
		stack.add(new Node());
		isFirstChild.add(true);
		stack.lastElement().m_fLength = DEFAULT_LENGTH;
		boolean bIsLabel = true;
		while (m_iTokenEnd < m_chars.length) {
			switch (nextToken()) {
			case BRACE_OPEN:
			{
				Node node2 = new Node();
				node2.m_fLength = DEFAULT_LENGTH;
				stack.add(node2);
				isFirstChild.add(true);
				bIsLabel = true;
			}
				break;
			case BRACE_CLOSE:
			{
				if (isFirstChild.lastElement()) {
					if (m_bAllowSingleChild) {
						// process single child nodes
						Node left = stack.lastElement();
						stack.remove(stack.size()-1);
						isFirstChild.remove(isFirstChild.size()-1);
						Node dummyparent = new Node();
						dummyparent.m_fLength = DEFAULT_LENGTH;
						dummyparent.m_left = left;
						left.m_Parent = dummyparent;
						dummyparent.m_right = null;
						Node parent = stack.lastElement();
						parent.m_left = left;
						left.m_Parent = parent;
						break;
					} else {
						// don't know how to process single child nodes
						throw new Exception("Node with single child found.");
					}
				}
				// process multi(i.e. more than 2)-child nodes by pairwise merging.
				while (isFirstChild.elementAt(isFirstChild.size()-2) == false) {
					Node right = stack.lastElement();
					stack.remove(stack.size()-1);
					isFirstChild.remove(isFirstChild.size()-1);
					Node left = stack.lastElement();
					stack.remove(stack.size()-1);
					isFirstChild.remove(isFirstChild.size()-1);
					Node dummyparent = new Node();
					dummyparent.m_fLength = DEFAULT_LENGTH;
					dummyparent.m_left = left;
					left.m_Parent = dummyparent;
					dummyparent.m_right = right;
					right.m_Parent = dummyparent;
					stack.add(dummyparent);
					isFirstChild.add(false);
				}
				// last two nodes on stack merged into single parent node 
				Node right = stack.lastElement();
				stack.remove(stack.size()-1);
				isFirstChild.remove(isFirstChild.size()-1);
				Node left = stack.lastElement();
				stack.remove(stack.size()-1);
				isFirstChild.remove(isFirstChild.size()-1);
				Node parent = stack.lastElement();
				parent.m_left = left;
				left.m_Parent = parent;
				parent.m_right = right;
				right.m_Parent = parent;
			}
				break;
			case COMMA:
			{
				Node node2 = new Node();
				node2.m_fLength = DEFAULT_LENGTH;
				stack.add(node2);
				isFirstChild.add(false);
				bIsLabel = true;
			}
				break;
			case COLON:
				bIsLabel = false;
				break;
			case TEXT:
				if (bIsLabel) {
					String sLabel = sStr.substring(m_iTokenStart, m_iTokenEnd);
					stack.lastElement().m_iLabel = getLabelIndex(sLabel); 
				} else {
					String sLength = sStr.substring(m_iTokenStart, m_iTokenEnd);
					stack.lastElement().m_fLength = Float.parseFloat(sLength); 
				}
				break;
			case META_DATA:
				if (stack.lastElement().getMetaData() == null) {
					stack.lastElement().setMetaData(sStr.substring(m_iTokenStart+1, m_iTokenEnd-1));
				} else {
					stack.lastElement().setMetaData(stack.lastElement().getMetaData() + ("," +sStr.substring(m_iTokenStart+1, m_iTokenEnd-1)));
				}
				break;
			case SEMI_COLON:
				//System.err.println(stack.lastElement().toString());
				return stack.lastElement();
			default:
				throw new Exception("parseNewick: unknown token");	
			}
		}
		return stack.lastElement();
		 } catch (Exception e) {
			 System.err.println(e.getMessage() + ": " + sStr.substring(Math.max(0, m_iTokenStart-100), m_iTokenStart) + " >>>" + sStr.substring(m_iTokenStart, m_iTokenEnd) + " <<< ...");
			 throw new Exception(e.getMessage() + ": " + sStr.substring(Math.max(0, m_iTokenStart-100), m_iTokenStart) + " >>>" + sStr.substring(m_iTokenStart, m_iTokenEnd) + " <<< ..."); 
		 }
		//return node;
	 }
	 
} // class TreeFileParser
