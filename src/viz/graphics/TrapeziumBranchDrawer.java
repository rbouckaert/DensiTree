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
 * TrapeziumBranchDrawer.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package viz.graphics;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;

public class TrapeziumBranchDrawer extends BranchDrawer {

	@Override
	public void draw(BufferedImageF image, int color, Graphics2D g, int x1, int y1, int x2, int y2, float fBottomWidth, float fTopWidth) {
		if (fBottomWidth == 0) {
			super.draw(image, color, g, x1, y1, x2, y2, fBottomWidth, fTopWidth);
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
//			nRed = (int)(nRed * fAlpha);
//			nGreen = (int)(nGreen * fAlpha);
//			nBlue = (int)(nBlue * fAlpha);
			lineAA(image, x1, y1, x2, y2, fAlpha, nRed, nGreen, nBlue, fBottomWidth, fTopWidth);
		}		
	

		
		void lineAA(BufferedImageF image, int x1, int y1, int x2, int y2, float fAlpha, int nRed, int nGreen, int nBlue, float fLineWidth, float fLineWidthTop) {
	    boolean steep = Math.abs(y2 - y1) > Math.abs(x2 - x1);
	    steep = false;
	    if (steep) {	
		      //swap x1, y1
	    	int h = x1;x1 = y1; y1 = h;
		      //swap x2, y2
	    	h = x2; x2 = y2; y2 = h;
	    }
	    if (x2 < x1) {
	    	int h = x1;x1 = x2; x2 = h;
	    	h = y1; y1 = y2; y2 = h;
	    	float f = fLineWidth;
	    	fLineWidth = fLineWidthTop;
	    	fLineWidthTop = f;
		}
			float dx = x2 - x1;
			float dy = y2 - y1;
	    float gradient = dy / dx;
//	    // handle first end point
//	    int xend = round(x1);
//	    float yend = (int)(y1 + gradient * (xend - x1));
//	    float xgap = rfpart(x1 + 0.5f);
	    int xpxl1 = x1;//xend;  // this will be used in the main loop
	    //int ypxl1 = y1;//(int)yend;
//    	if (steep) {
//    		plot(ypxl1, xpxl1, rfpart(yend) * xgap*fAlpha, nRed, nGreen, nBlue);
//        	 for (int i = 1; i < (int) fLineWidth; i++) {
//        		 plot(ypxl1+i,xpxl1, fAlpha, nRed, nGreen, nBlue);
//        	 }
//    		plot(ypxl1 + (int) fLineWidth, xpxl1, fpart(yend) * xgap*fAlpha, nRed, nGreen, nBlue);
//    	} else {
//		    plot(xpxl1, ypxl1, rfpart(yend) * xgap*fAlpha, nRed, nGreen, nBlue);
//        	 for (int i = 1; i < (int) fLineWidth; i++) {
//        		 plot(xpxl1, ypxl1+i, fAlpha, nRed, nGreen, nBlue);
//        	 }
//		    plot(xpxl1, ypxl1 + (int) fLineWidth, fpart(yend) * xgap*fAlpha, nRed, nGreen, nBlue);
//    	}
	    float intery = y1+gradient;//yend + gradient; // first y-intersection for the main loop
//	    // handle second end point
//	    xend = round (x2);
//	    yend = y2 + gradient * (xend - x2);
//	    xgap = fpart(x2 + 0.5f);
	    int xpxl2 = x2;//xend;  // this will be used in the main loop
	    //int ypxl2 = y2;//(int)yend;
//    	if (steep) {
//		    plot (ypxl2, xpxl2, rfpart (yend) * xgap *fAlpha, nRed, nGreen, nBlue);
//        	 for (int i = 1; i < (int) fLineWidth; i++) {
//        		 plot(ypxl2+i,xpxl2, fAlpha, nRed, nGreen, nBlue);
//        	 }
//		    plot (ypxl2, xpxl2 + (int) fLineWidth, fpart (yend) * xgap*fAlpha, nRed, nGreen, nBlue);
//    	} else {
//    		plot (xpxl2, ypxl2, rfpart (yend) * xgap *fAlpha, nRed, nGreen, nBlue);
//        	 for (int i = 1; i < (int) fLineWidth; i++) {
//        		 plot(xpxl2, ypxl2+i, fAlpha, nRed, nGreen, nBlue);
//        	 }
//    		plot (xpxl2, ypxl2 + (int) fLineWidth, fpart (yend) * xgap*fAlpha, nRed, nGreen, nBlue);
//    	}
	    // main loop
	    for (int x = xpxl1 + 1; x < xpxl2; x++) {
    		int nLineWidth = (int)(fLineWidthTop + (fLineWidth - fLineWidthTop) * (x-xpxl1-1)/(xpxl2-xpxl1));
	    	if (steep) {
	    		int y = (int) intery;
	    		image.plot (y, x, rfpart (intery)*fAlpha, nRed, nGreen, nBlue);
	        	 for (int i = 1; i < nLineWidth; i++) {
	        		 image.plot(y+i,x, fAlpha, nRed, nGreen, nBlue);
	        	 }
	        	 image.plot (y + nLineWidth, x, fpart (intery)*fAlpha, nRed, nGreen, nBlue);
	    	} else {
	    		int y = (int) intery;
	    		image.plot (x, (int) intery, rfpart (intery)*fAlpha, nRed, nGreen, nBlue);
	        	 for (int i = 1; i < nLineWidth; i++) {
	        		 image.plot(x, y+i, fAlpha, nRed, nGreen, nBlue);
	        	 }
	        	 image.plot (x, y + nLineWidth, fpart (intery)*fAlpha, nRed, nGreen, nBlue);
	    	}
	        intery = intery + gradient;
	    }

	}
}
