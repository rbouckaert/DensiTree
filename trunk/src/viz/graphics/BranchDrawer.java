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
 * BranchDrawer.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package viz.graphics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;

/* Basic implementation of branch drawer.
 * This one draws branches as simple lines with the
 * width of the stroke in the Graphics2D environment.
 * Derived implementations should override the draw method in order
 * to create customized branches (like trapeziums, arcs, etc.). **/
public class BranchDrawer {
	
	final static int MAX_LINE_WIDTH = 400;


	/* Draw a branch from point (x1,y1) to (x2,y2)
	 * in theory taking top width (x1,y1) and bottom width at (x2, y2) in account.
	 * This base implementation ignores these widths and draws a line with the
	 * width defined in the stroke of graphics environment g.
	 */
	public void draw(BufferedImageF image, int color, Graphics2D g, int x1, int y1, int x2, int y2, float fBottomWidth, float fTopWidth) {
		
		if (x1 == -1 || x2 == -1 || y1 == -1 || y2 == -1) {
			x1 = Math.max(x1, 0);
			x2 = Math.max(x2, 0);
			y1 = Math.max(y1, 0);
			y2 = Math.max(y2, 0);
			if (x1 < 2 && x2 < 2 && y1 < 2 && y2 < 2) {
				return;
			}
		}
		if (Math.abs(x1-x2)<2 && Math.abs(y1-y2)<2) {
			return;
		}
			//g.drawLine(x1, y1, x2, y2);
			int nRed = (color >> 16) & 0xFF;//g.getColor().getRed();
			int nGreen = (color >> 8) & 0xFF;//g.getColor().getGreen();
			int nBlue = (color >> 0) & 0xFF;//g.getColor().getBlue();
			nRed = (nRed<<16);
			nGreen = (nGreen<<16);
			nBlue = (nBlue<<16);
			float fAlpha = ((AlphaComposite)g.getComposite()).getAlpha();
			float fLineWidth = Math.min(((BasicStroke)g.getStroke()).getLineWidth(), MAX_LINE_WIDTH);
//			nRed = (int)(nRed * fAlpha);
//			nGreen = (int)(nGreen * fAlpha);
//			nBlue = (int)(nBlue * fAlpha);
			//if (m_bDrawArc) {
			//	arcAA(x1, y1, x2, y2, fAlpha, nRed, nGreen, nBlue, fLineWidth);
			//} else {
				lineAA(image, x1, y1, x2, y2, fAlpha, nRed, nGreen, nBlue, fLineWidth);
			//}
		}

/** draws line using Wu's anti aliasing algorithm 
 * Wu, Xiaolin (July 1991). "An efficient antialiasing technique". Computer Graphics 25 (4): 143–152. doi:10.1145/127719.122734. ISBN 0-89791-436-8. http://portal.acm.org/citation.cfm?id=122734.
 * **/
	void lineAA(BufferedImageF image, int x1, int y1, int x2, int y2, float fAlpha, int nRed, int nGreen, int nBlue, float fLineWidth) {
    boolean steep = Math.abs(y2 - y1) > Math.abs(x2 - x1);
//steep = false;
    if (steep) {	
	      //swap x1, y1
    	int h = x1;x1 = y1; y1 = h;
	      //swap x2, y2
    	h = x2; x2 = y2; y2 = h;
    }
    if (x2 < x1) {
    	int h = x1;x1 = x2; x2 = h;
    	h = y1; y1 = y2; y2 = h;
	}
		float dx = x2 - x1;
		float dy = y2 - y1;
    float gradient = dy / dx;
//    // handle first end point
    int xend = round(x1);
    float yend = (int)(y1 + gradient * (xend - x1));
    float xgap = rfpart(x1 + 0.5f);
    int xpxl1 = x1;//xend;  // this will be used in the main loop
    int ypxl1 = y1;//(int)yend;
	if (steep) {
		image.plot(ypxl1, xpxl1, rfpart(yend) * xgap*fAlpha, nRed, nGreen, nBlue);
    	 for (int i = 1; i < (int) fLineWidth; i++) {
    		 image.plot(ypxl1+i,xpxl1, fAlpha, nRed, nGreen, nBlue);
    	 }
		image.plot(ypxl1 + (int) fLineWidth, xpxl1, fpart(yend) * xgap*fAlpha, nRed, nGreen, nBlue);
	} else {
	    image.plot(xpxl1, ypxl1, rfpart(yend) * xgap*fAlpha, nRed, nGreen, nBlue);
    	 for (int i = 1; i < (int) fLineWidth; i++) {
    		 image.plot(xpxl1, ypxl1+i, fAlpha, nRed, nGreen, nBlue);
    	 }
	    image.plot(xpxl1, ypxl1 + (int) fLineWidth, fpart(yend) * xgap*fAlpha, nRed, nGreen, nBlue);
	}
    float intery = y1+gradient;//yend + gradient; // first y-intersection for the main loop
//    // handle second end point
    xend = round (x2);
    yend = y2 + gradient * (xend - x2);
    xgap = fpart(x2 + 0.5f);
    int xpxl2 = x2;//xend;  // this will be used in the main loop
    int ypxl2 = y2;//(int)yend;
	if (steep) {
	    image.plot (ypxl2, xpxl2, rfpart (yend) * xgap *fAlpha, nRed, nGreen, nBlue);
    	 for (int i = 1; i < (int) fLineWidth; i++) {
    		 image.plot(ypxl2+i,xpxl2, fAlpha, nRed, nGreen, nBlue);
    	 }
	    image.plot (ypxl2, xpxl2 + (int) fLineWidth, fpart (yend) * xgap*fAlpha, nRed, nGreen, nBlue);
	} else {
		image.plot (xpxl2, ypxl2, rfpart (yend) * xgap *fAlpha, nRed, nGreen, nBlue);
    	 for (int i = 1; i < (int) fLineWidth; i++) {
    		 image.plot(xpxl2, ypxl2+i, fAlpha, nRed, nGreen, nBlue);
    	 }
		image.plot (xpxl2, ypxl2 + (int) fLineWidth, fpart (yend) * xgap*fAlpha, nRed, nGreen, nBlue);
	}
    // main loop
    //float fScale = (float)((y2-y1)/Math.sqrt(Math.abs(xpxl2-xpxl1-1)));
    for (int x = xpxl1 + 1; x < xpxl2; x++) {
    	if (steep) {
    		int y = (int) intery;
    		image.plot (y, x, rfpart (intery)*fAlpha, nRed, nGreen, nBlue);
        	 for (int i = 1; i < (int) fLineWidth; i++) {
        		 image.plot(y+i,x, fAlpha, nRed, nGreen, nBlue);
        	 }
        	 image.plot (y + (int) fLineWidth, x, fpart (intery)*fAlpha, nRed, nGreen, nBlue);
    	} else {
    		int y = (int) intery;
    		image.plot (x, (int) intery, rfpart (intery)*fAlpha, nRed, nGreen, nBlue);
        	 for (int i = 1; i < (int) fLineWidth; i++) {
        		 image.plot(x, y+i, fAlpha, nRed, nGreen, nBlue);
        	 }
        	 image.plot (x, y + (int) fLineWidth, fpart (intery)*fAlpha, nRed, nGreen, nBlue);
    	}
        intery = intery + gradient;
        //intery = y1+(float)Math.sqrt(x-xpxl1) * fScale;
    }
}
//    return fractional part of x
	float fpart(float x) {return (x - (int) x);}
//    return 1 - fpart(x)
	float rfpart(float x) {return 1.0f - fpart(x);}
//    return ipart(x + 0.5)
	int round(double x) {return (int) (x+0.5);}
	
}
