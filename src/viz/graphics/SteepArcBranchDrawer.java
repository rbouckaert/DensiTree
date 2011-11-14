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
 * SteepArcBranchDrawer.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package viz.graphics;

public class SteepArcBranchDrawer extends BranchDrawer {
	void lineAA(BufferedImageF image, int x1, int y1, int x2, int y2, float fAlpha, int nRed, int nGreen, int nBlue, float fLineWidth) {
	    if (x2 < x1) {
	    	int h = x1;x1 = x2; x2 = h;
	    	h = y1; y1 = y2; y2 = h;
		}
    	fAlpha /= 2.0;

		//float dx = x2 - x1;
		//float dy = y2 - y1;
	    //float gradient = dy / dx;
	    int xpxl1 = x1;//xend;  // this will be used in the main loop
	    //int ypxl1 = y1;//(int)yend;
	    float intery = y1;//+gradient;//yend + gradient; // first y-intersection for the main loop
	    int xpxl2 = x2;//xend;  // this will be used in the main loop
	    //int ypxl2 = y2;//(int)yend;
	    float fScale = (float)((y2-y1)/Math.sqrt(Math.abs(xpxl2-xpxl1-1)));
	    fScale = (float)((y2-y1));///Math.sqrt(Math.abs(xpxl2-xpxl1)));
	    float fStep = Math.min(0.25f, (xpxl2-xpxl1)/2000f);
	    for (float xf0 = xpxl1; xf0 < xpxl2; xf0 += fStep) {
	    	float xf = xpxl1 + (xf0 - xpxl1) * (xf0 - xpxl1) / (xpxl2 - xpxl1);
	    	int x = (int) xf;
	    	int y = (int) intery;
	    	image.plot (x, y, rfpart (intery)*fAlpha, nRed, nGreen, nBlue);
	        for (int i = 1; i < (int) fLineWidth; i++) {
	        	image.plot(x, y+i, fAlpha, nRed, nGreen, nBlue);
	        }
	    	image.plot (x, y + (int) fLineWidth, fpart (intery)*fAlpha, nRed, nGreen, nBlue);
	        //intery = intery + gradient;
	    	double f = Math.PI*Math.max(xf-xpxl1, 0)/(xpxl2-xpxl1);
	        intery = (f>Math.PI/2 ? y2 : y1+(float)Math.sin(f) * fScale);
	    }
	}
}
