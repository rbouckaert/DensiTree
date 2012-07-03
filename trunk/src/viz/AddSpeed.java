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
 * AddSpeed.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/

package viz;

import java.io.FileWriter;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Add 'speed' attribute to metadata calculated over the branch from it parent
// location, its own locations (together giving the distance) and the branch length
// (for calculating the speed).
// Assumes that the metadata contains x & y coordinate in the form [x=1.23,y=4,5,6]
// Also, assumes that the geo coordinates in the labels coincide with those in the metadata,
// so if metadata x & y are from a grid, then x & y of taxa should be from a grid.
public class AddSpeed extends DensiTree {
	/** regular expression pattern for finding width information in metadata **/ 
	static Pattern g_pattern;
	final static String DEFAULT_PATTERN = "([0-9\\.Ee-]+),y=([0-9\\.Ee-]+)";
	static DensiTree g_densitree = new DensiTree();

	public static void addSpeed(Node node) {
		if (node.isRoot()) {
			addSpeed(node.m_left);
			addSpeed(node.m_right);
		} else {
			if (node.isLeaf()) {
				if (node.getParent().getMetaData() != null) {
					float fX = g_densitree.m_fLongitude.get(node.m_iLabel);//m_nCurrentPosition[node.m_iLabel]%32;
					float fY = g_densitree.m_fLatitude.get(node.m_iLabel);//m_nCurrentPosition[node.m_iLabel]/32;
					Matcher matcher2 = g_pattern.matcher(node.getParent().getMetaData());
					matcher2.find();
					float fX2 = Float.parseFloat(matcher2.group(1));
					float fY2 = Float.parseFloat(matcher2.group(2));
					node.setMetaData(node.getMetaData() + (",s="+(float)(Math.sqrt((fX-fX2)*(fX-fX2)+(fY-fY2)*(fY-fY2))/Math.abs(node.m_fLength))));
				}
			} else {
				addSpeed(node.m_left);
				addSpeed(node.m_right);
				if (node.getMetaData() != null && node.getParent().getMetaData() != null) {
					Matcher matcher = g_pattern.matcher(node.getMetaData());
					matcher.find();
					float fX = Float.parseFloat(matcher.group(1));
					float fY = Float.parseFloat(matcher.group(2));
					Matcher matcher2 = g_pattern.matcher(node.getParent().getMetaData());
					matcher2.find();
					float fX2 = Float.parseFloat(matcher2.group(1));
					float fY2 = Float.parseFloat(matcher2.group(2));
					node.setMetaData(node.getMetaData() + (",s="+(float)(Math.sqrt((fX-fX2)*(fX-fX2)+(fY-fY2)*(fY-fY2))/Math.abs(node.m_fLength))));
				}
			}
		}
	}
	
	public static void main(String[] args) {
		String sFileIn = "puma2.trees";
		String sFileOut = "puma3.trees";
		if (args.length > 0) {
			sFileIn = args[0];
		}
		if (args.length > 1) {
			sFileOut = args[1];
		}
		
		try {
			g_densitree.m_sLabels = new Vector<String>();
			g_densitree.m_fLongitude = new Vector<Float>();
			g_densitree.m_fLatitude = new Vector<Float>();
	    	g_pattern = Pattern.compile(DEFAULT_PATTERN);

	    	
	    	TreeFileParser parser = new TreeFileParser(g_densitree);
			Node [] trees = parser.parseFile(sFileIn);
			for (Node tree: trees) {
				addSpeed(tree);
			}
			FileWriter outfile = new FileWriter(sFileOut);
			StringBuffer buf = new StringBuffer();
			buf.append("#NEXUS\n");
			buf.append("Begin trees\n");
			buf.append("\tTranslate\n");
			for (int i = 0; i < g_densitree.m_sLabels.size(); i++) {
				buf.append("\t\t" + i + " " + g_densitree.m_sLabels.get(i));
				if (i < g_densitree.m_sLabels.size()-1) {
					buf.append(",");
				}
				buf.append("\n");
			}
			buf.append(";\n");
			outfile.write(buf.toString());
			for (int i = 0; i < trees.length; i++) {
				outfile.write("tree STATE_" + i + " = "	+ trees[i].toString() + ";\n");
			}
			outfile.write("End;\n");
			outfile.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
