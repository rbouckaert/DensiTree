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
 * BrownianBridgeBranchDrawer.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package viz.graphics;

import java.util.Random;


public class BrownianBridgeBranchDrawer extends BranchDrawer {
	Random m_random = new Random();
	final static int NR_OF_POINTS = 32;
	final static double NOISE = 1.5;
	@Override
	void lineAA(BufferedImageF image, int x1, int y1, int x2, int y2, float fAlpha, int nRed, int nGreen, int nBlue, float fLineWidth) {
		int [] nX = new int[NR_OF_POINTS];
		int [] nY = new int[NR_OF_POINTS];
		nX[0] = x1;
		nY[0] = y1;
		nX[NR_OF_POINTS-1] = x2;
		nY[NR_OF_POINTS-1] = y2;
		determinePoints(nX, nY, 0, NR_OF_POINTS-1);
		for (int i = 0; i < NR_OF_POINTS-2; i++) {
			super.lineAA(image, nX[i], nY[i], nX[i+1], nY[i+1], fAlpha, nRed, nGreen, nBlue, fLineWidth);
		}
	}
	
	void determinePoints(int [] nX, int [] nY, int iStart, int iEnd) {
		int iMid = (iStart + iEnd)/2;
		double xMid = (nX[iStart] + nX[iEnd])/2.0;
		double yMid = (nY[iStart] + nY[iEnd])/2.0;
		nX[iMid] = (int)(xMid + NOISE*(m_random.nextDouble() - 0.5)*Math.exp(-m_random.nextDouble()) * Math.abs(nX[iStart] - nX[iEnd]));
		nY[iMid] = (int)(yMid + NOISE*(m_random.nextDouble() - 0.5)*Math.exp(-m_random.nextDouble()) * Math.abs(nY[iStart] - nY[iEnd]));
		if (iMid > iStart + 1) {
			determinePoints(nX, nY, iStart, iMid);
		}
		if (iMid < iEnd - 1) {
			determinePoints(nX, nY, iMid, iEnd);
		}
	}
	
}
