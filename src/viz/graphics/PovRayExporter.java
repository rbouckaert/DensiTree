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
 * PovRayExporter.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package viz.graphics;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Vector;

import viz.Node;

/** class for exporting a tree to a povray scene **/
public class PovRayExporter {

	/** main method, creates header as well as tree **/
	public static void export(Node tree, String sFile, double fScaleX, double fScaleY, Vector<String> sLabels) {
		try {
		FileWriter outFile = new FileWriter(sFile);
		PrintWriter out = new PrintWriter(outFile);
		out.println("#version 3.1;");
		out.println("global_settings { assumed_gamma 2.2 }");
		out.println("#include \"densitree.inc\"");
//		out.println("camera {Camera1 translate <-400,-400,-11000>}");
//		out.println("#declare Bark=");
//		out.println(" texture{");
//		out.println("	   pigment{Brown}");
//		out.println("	   normal{");
//		out.println("	     bumps 0.6");
//		out.println("	     scale 24");
//		out.println("	   }");
//		out.println("	   finish{phong 0.3 phong_size 200}");
//		out.println("	 }");
//		out.println("	texture {");
//		out.println("   pigment {Brown}");
//		out.println("   finish {phong 1}");
//		out.println("}");
		float fTotalHeight = height(tree);
		

		out.println("box{<-1255.7326263189316, "+-fTotalHeight*fScaleX+", -300>,<1255.7326263189316, "+(-fTotalHeight*fScaleX-1000)+", 300>");
		out.println("texture{T_Grass}}");
		out.println("merge {");
		exportNode(tree, out, fScaleY, fScaleX, sLabels, fTotalHeight);
		out.println("}");
		
		out.flush();
		out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/** calculate total height of tree **/
	 static float height(Node node) {
		 if (node.isLeaf()) {
			 return node.m_fLength;
		 } else {
			 return node.m_fLength + Math.max(height(node.m_left), height(node.m_right));
		 }
	 }	
	/** recursively position branches of the tree **/
	static float exportNode(Node node, PrintWriter out, double fScaleX, double fScaleY, Vector<String> sLabels, float fTotalHeight) {
		if (node.isLeaf()) {
			out.println("text");
			out.println("{  ttf \"timrom.ttf\" \""+sLabels.elementAt(node.getNr())+"\" 0.001, 0");
			out.println("   pigment { rgb 1 }");
			out.println("   finish { ambient 1 }");
			out.println("   rotate <0,0,45>");
			out.println("   scale <50,50,50>");
			out.println("   translate <"+-node.m_fPosX* fScaleX+", "+0+", -3>");
			out.println("}");
			return node.m_fLength;
		} else {
			float fHeightL = exportNode(node.m_left, out, fScaleX, fScaleY, sLabels, fTotalHeight);
			float fHeightR = exportNode(node.m_right, out, fScaleX, fScaleY, sLabels, fTotalHeight);
			float fHeight = node.m_fLength + Math.max(fHeightL, fHeightR);
			//float fSkew = (node.m_fPosX - node.m_left.m_fPosX)/node.m_left.m_fLength;
			float fEndThickness = (float)(fScaleX/2*fHeightL/fTotalHeight);
			if (node.m_left.isLeaf()) {
				fEndThickness = (float)(fScaleX/2*0.1);
			}
			//fSkew *= fScaleX/fScaleY;
//			out.println("cone {");
//			out.println("   <0,0,0>,"+fScaleX/2*fHeight/fTotalHeight+",");
//			out.println("   <-1,0,0>,"+fEndThickness);
//			out.println("   rotate  -z*90");
//			out.println("   scale  <1," + node.m_left.m_fLength* fScaleY+",1>");
//			out.println("     matrix < 1.0, 0.0, 0.0,");
//			out.println("           "+fSkew+", 1.0, 0.0,");
//			out.println("           0.0, 0.0, 1.0,");
//			out.println("           0.0, 0.0, 0.0 >");
//			out.println("	texture {Bark}");
//			out.println("   translate <"+-node.m_fPosX* fScaleX+", "+-fHeightL* fScaleY+", -3>");
//			out.println("}");
			
			exportBranche(out, -node.m_fPosX* fScaleX, -fHeightL* fScaleY, fScaleX/2*fHeight/fTotalHeight
					,-node.m_left.m_fPosX* fScaleX , -(fHeightL+node.m_left.m_fLength)*fScaleY, fEndThickness, 2);

			//fSkew = (node.m_fPosX - node.m_right.m_fPosX)/node.m_right.m_fLength;
			//fSkew *= fScaleX/fScaleY;
			fEndThickness = (float)(fScaleX/2*fHeightR/fTotalHeight);
			if (node.m_right.isLeaf()) {
				fEndThickness = (float)(fScaleX/2*0.1);
			}
//			out.println("cone {");
//			out.println("   <0,0,0>,"+fScaleX/2*fHeight/fTotalHeight+",");
//			out.println("   <-1,0,0>,"+fEndThickness);
//			out.println("   rotate  -z*90");
//			out.println("   scale  <1," + node.m_right.m_fLength* fScaleY+",1>");
//			out.println("     matrix < 1.0, 0.0, 0.0,");
//			out.println("           "+fSkew+", 1.0, 0.0,");
//			out.println("           0.0, 0.0, 1.0,");
//			out.println("           0.0, 0.0, 0.0 >");
//			out.println("	texture {Bark}");
//			out.println("   translate <"+-node.m_fPosX* fScaleX+", "+-fHeightR* fScaleY+", -3>");
//			out.println("}");
			exportBranche(out, -node.m_fPosX* fScaleX, -fHeightR* fScaleY, fScaleX/2*fHeight/fTotalHeight
					,-node.m_right.m_fPosX* fScaleX , -(fHeightL+node.m_right.m_fLength)*fScaleY, fEndThickness, 2);
			return fHeight;
		}
	} // exportNode

	static Random m_random = new Random(); 
	
	static void exportBranche(PrintWriter out, 
			double fX0, double fY0, double fWidth0, 
			double fX1, double fY1, double fWidth1, int nDepth) {
		if (nDepth == 0) {
			exportBranche(out, fX0, fY0, fWidth0, fX1, fY1, fWidth1);
		} else {
			double fX2 = (fX0+fX1)/2 + (0.5-m_random.nextDouble()) * (fX0-fX1)/2;
			double fY2 = (fY0+fY1)/2;
			//fX2 = (fX0+fX1)/2;
			double fYDelta = 0*(0.5-m_random.nextDouble()) * (fY0-fY1)/10;
			double fWidth2 = (fWidth0+fWidth1)/2;
			exportBranche(out, fX2, fY0 + (fY0-fY1)/2.0, fWidth2, fX1, fY0, fWidth1- fYDelta, nDepth-1);
			exportBranche(out, fX0, fY0, fWidth0, fX2, fY2+ fYDelta, fWidth2, nDepth-1);
		}
		
	}
	
	static void exportBranche(PrintWriter out, 
			double fX0, double fY0, double fWidth0, 
			double fX1, double fY1, double fWidth1) {
		double fSkew = (fX1-fX0)/(fY0-fY1);
		out.println("cone {");
		out.println("   <0,0,0>,"+fWidth0+",");
		out.println("   <-1,0,0>,"+fWidth1);
		out.println("   rotate  -z*90");
		out.println("   scale  <1," +(fY0-fY1)+",1>");
		out.println("     matrix < 1.0, 0.0, 0.0,");
		out.println("           "+fSkew+", 1.0, 0.0,");
		out.println("           0.0, 0.0, 1.0,");
		out.println("           0.0, 0.0, 0.0 >");
		out.println("	texture {Bark}");
		out.println("   translate <"+fX0+", "+fY0+", -3>");
		out.println("}");
	} // exportBranch
}
