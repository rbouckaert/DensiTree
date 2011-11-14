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
 * BufferedImageBounded.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package viz.graphics;

import java.awt.Component;
import java.awt.Graphics;

public class BufferedImageBounded extends BufferedImageF {
	/** Counts amount of probability mass collected in a pixel.
	 * This is then used to calculated HPDs, means, medians etc. **/
	float [] m_fCount;
	
	public BufferedImageBounded(int nWidth, int nHeight) {
		super(nWidth, nHeight);
		m_fCount = new float[nHeight*nWidth];
	}

	@Override
	void plot(int x, int y, float fAlpha, int nRed, int nGreen, int nBlue) {
		super.plot(x, y, fAlpha, nRed, nGreen, nBlue);
		int p = y* m_nWidth + x;
		if (p < m_fCount.length)
		m_fCount[p] += fAlpha;
	}

	@Override
	public void drawImage(Graphics g, Component component) {
		int [] rgbArray = new int [m_nWidth * m_nHeight];
		int k = 0;
		for (int y = 0; y < m_nHeight; y++) {
			for (int x = 0; x < m_nWidth; x++) {
				int nRGB =	(m_nR[k] & 0xFF0000);
				nRGB +=	(m_nG[k] & 0xFF0000)>> 8;
				nRGB += (m_nB[k] & 0xFF0000)>>16;
				rgbArray[k++] = nRGB|0xFF000000;
			}
		}
		drawBounds(rgbArray);
		m_localImage.setRGB(0, 0, m_nWidth, m_nHeight,
				rgbArray, 0, m_nWidth);
		g.drawImage(m_localImage, 0, 0, component);
	} // drawImage
	
	void drawBounds(int [] rgbArray) {
		if (m_bIsHorizontal) {
			for (int i = 0; i < m_nWidth; i++) {
				// calc total mass in cut-through
				float fSum = 0;
				for (int j = 0; j < m_nHeight; j++) {
					fSum += m_fCount[j*m_nWidth + i];
				}
				if (fSum > 0) {
					// determine 90% HPD
					float fHPD = 0.9f * fSum;
					int jOptMin = 0;
					int jOptMax = m_nHeight;
					int k = 0;
					float fPartSum = 0;
					for (int j = 0; j < m_nHeight && k < m_nHeight; j++) {
						while (fPartSum < fHPD && k < m_nHeight) {
							fPartSum += m_fCount[k*m_nWidth + i];
							k++;
						}
						if (k - j < jOptMax - jOptMin) {
							jOptMin = j;
							jOptMax = k;
						}
						fPartSum -= m_fCount[j*m_nWidth + i];
					}
					// find median
					k = 0;
					fPartSum = 0;
					while (fPartSum < fSum / 2.0f) {
						fPartSum += m_fCount[k*m_nWidth + i];
						k++;
					}
					// 'draw' the point
					jOptMin = Math.max(jOptMin, 1);
					jOptMax = Math.min(jOptMax, m_nHeight-1);
					for (int j = -1; j < 2; j++) {
						if (jOptMin + j > 0)
						rgbArray[(jOptMin + j) * m_nWidth + i] = 0xFF000000;
						if (jOptMax + j - 1 > 0)
							rgbArray[(jOptMax + j - 1) * m_nWidth + i] = 0xFF000000;
						rgbArray[(k + j) * m_nWidth + i] = 0xFF000000;
					}
				}				
			}
		}
	} // drawBounds
	
} // BufferedImageBounded
