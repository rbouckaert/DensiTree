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
 * BufferedImageF.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package viz.graphics;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;


public class BufferedImageF {
		// dimensions of image
		int m_nWidth, m_nHeight;
		// 255x255x255 color representation of image
		public BufferedImage m_localImage;
		// int array representation of image
		int [] m_nR;
		int [] m_nG;
		int [] m_nB;
		// flag to indicate both image representations are the same 
		//boolean m_bInSync;
		/** flag to indicate drawing direction **/ 
		boolean m_bIsHorizontal = true;
		// current drawing color
		int m_nRed;
		int m_nGreen;
		int m_nBlue;
		// current drawing alpha
		float m_fAlpha;
		
		public BufferedImageF(int nWidth, int nHeight) {
			m_nWidth = nWidth;
			m_nHeight = nHeight;
			m_localImage = new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_4BYTE_ABGR);
			//System.err.println(">>>>>>>>>>>>>>>>>>>>>CREATING IMAGE" + nWidth + " " + nHeight);
//			m_nR = new int[nHeight][nWidth];
//			m_nG = new int[nHeight][nWidth];
//			m_nB = new int[nHeight][nWidth];
			m_nR = new int[nHeight*nWidth];
			m_nG = new int[nHeight*nWidth];
			m_nB = new int[nHeight*nWidth];
			//m_bInSync = true;
		}
		public int getWidth() {return m_localImage.getWidth();}
		public int getHeight() {return m_localImage.getHeight();}
		public int getRGB(int i, int j) {return m_localImage.getRGB(i, j);}
		
		/** set up background **/
		public void init(Graphics2D g, Color bgColor, BufferedImage bgImage, double [] fBGImageBox,
				int nLabelWidth,
				double fMinLong, double fMaxLong, double fMinLat, double fMaxLat) {
			g.setBackground(bgColor);
			g.clearRect(0, 0, m_localImage.getWidth(), m_localImage.getHeight());
			if (bgImage != null) {
				if (fMaxLong==180) {
					g.drawImage(bgImage,
							0, 0, m_localImage.getWidth() - nLabelWidth - 20, m_localImage.getHeight(),
							0, 0, bgImage.getWidth(), bgImage.getHeight(),
							null);  
					
				} else {
					int nW = bgImage.getWidth();
					int nH = bgImage.getHeight();
//					int x0 =(int)(nW * (180+fMinLong)/360.0);
//					int x1=(int)(nW * (180+fMaxLong)/360.0);
//					int y0=(int)(nH * (90-fMaxLat)/180.0);
//					int y1=(int)(nH * (90-fMinLat)/180.0);
					int x0 =(int)(nW * (fMinLong- fBGImageBox[0])/(fBGImageBox[2] - fBGImageBox[0]));
					int x1=(int)(nW * (fMaxLong- fBGImageBox[0])/(fBGImageBox[2] - fBGImageBox[0]));
					int y0=(int)(nH * (fMaxLat- fBGImageBox[3])/(fBGImageBox[1] - fBGImageBox[3]));
					int y1=(int)(nH * (fMinLat- fBGImageBox[3])/(fBGImageBox[1] - fBGImageBox[3]));
					g.drawImage(bgImage,
							0, 0, m_localImage.getWidth() - nLabelWidth - 20, m_localImage.getHeight(),
							x0, 
							y0, 
							x1,
							y1, 
							null);
					if (x1 > nW) {
						g.drawImage(bgImage,
								0, 0, m_localImage.getWidth() - nLabelWidth - 20, m_localImage.getHeight(),
								x0-nW, 
								y0, 
								x1-nW,
								y1, 
								null);
					}
					
				}
			}
			
			
//			if (bgImage != null) {
//				g.drawImage(bgImage,
//						0, 0, m_localImage.getWidth() - nLabelWidth - 20, m_localImage.getHeight(),
//						0, 0, bgImage.getWidth(), bgImage.getHeight(),
//						null);  
//			}
			//m_bInSync = false;
		} // init
		
		public void SyncIntToRGBImage() {
			int p = 0;
			for (int y = 0; y < m_nHeight; y++) {
				for (int x = 0; x < m_nWidth; x++) {
					int nRGB = m_localImage.getRGB(x, y);
					m_nR[p] = (nRGB & 0xFF0000);
					m_nG[p] = (nRGB & 0xFF00)<<8;
					m_nB[p] = (nRGB & 0xFF)<<16;
					p++;
				}
			}
			//m_bInSync = true;
		}
		
		public Graphics2D createGraphics() {
			return m_localImage.createGraphics();
		}
		
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
			m_localImage.setRGB(0, 0, m_nWidth, m_nHeight,
					rgbArray, 0, m_nWidth);
			g.drawImage(m_localImage, 0, 0, component);
		}
		
		public void setColor(Graphics2D g, Color color) {
			g.setColor(color);
		}
		public void setOrientation(boolean bIsHorizontal) {
			m_bIsHorizontal = bIsHorizontal;
		}
		
		public void scale(Graphics2D g, double fScaleX, double fScaleY) {
			g.scale(fScaleX, fScaleY);
		} // scale

		//Graphics2D createGraphics() {return }	
//	    return fractional part of x
		float fpart(float x) {return (x - (int) x);}
//	    return 1 - fpart(x)
		float rfpart(float x) {return 1.0f - fpart(x);}
//	    return ipart(x + 0.5)
		int round(double x) {return (int) (x+0.5);}
		
		// plot the pixel at (x, y) with brightness alpha (where 0 ≤ alpha ≤ 1)
		void plot(int x, int y, float fAlpha, int nRed, int nGreen, int nBlue) {
			if (x == 0) {
				int h=4;
				h++;
			}
			if (y>=m_nHeight || x >= m_nWidth || x < 0 || y < 0) {return;}
//			m_nR[y][x] = (int)(m_nR[y][x]*(1-fAlpha) + nRed * fAlpha);
//			m_nG[y][x] = (int)(m_nG[y][x]*(1-fAlpha) + nGreen * fAlpha);
//			m_nB[y][x] = (int)(m_nB[y][x]*(1-fAlpha) + nBlue * fAlpha);
			int p = y* m_nWidth + x;
			m_nR[p] = (int)(m_nR[p]*(1-fAlpha) + nRed * fAlpha);
			m_nG[p] = (int)(m_nG[p]*(1-fAlpha) + nGreen * fAlpha);
			m_nB[p] = (int)(m_nB[p]*(1-fAlpha) + nBlue * fAlpha);
		}
//		void plot(int x, int y, int nRed, int nGreen, int nBlue) {
//			m_nR[y][x] += nRed;
//			m_nG[y][x] += nGreen;
//			m_nB[y][x] += nBlue;
//		}
		/** Line drawing using Digital Differential Analyzer or DDA for short 
		 * http://www.cs.unc.edu/~mcmillan/comp136/Lecture6/Lines.html
		 * **/
		void line(int x1, int y1, int x2, int y2, float fAlpha, int nRed, int nGreen, int nBlue, float fLineWidth) {
			int length,x,y,dx,dy,wx,wy,w;
			if(Math.abs(x2-x1)>=Math.abs(y2-y1)) {
				length=Math.abs(x2-x1);
			} else {
				length=Math.abs(y2-y1);
			}
			w=(int) fLineWidth;
			wx=(int)(((w-1)/2)*(Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1))/Math.abs(y2-y1)));
			wy=(int)(((w-1)/2)*(Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1))/Math.abs(x2-x1)));
			dx=(x2-x1)/length;
			dy=(y2-y1)/length;
			if(dy>dx) {
				wy=wx;
			}
			x=(int)(x1+(dx>0?0.5:-0.5));
			y=(int)(y1+(dy>0?0.5:-0.5));
			int i=1;
			while(i<=length) {
				for(int j=-wy;j<wy;j++) {
		        	 plot(x,y+j, fAlpha, nRed, nGreen, nBlue);
				}
				if (wy==0) {
					plot(x,y, fAlpha, nRed, nGreen, nBlue);
				}
				x+=dx;
				y+=dy;
				i++;
			}
		}


	
		
		/** Bresenham's line drawing algorithm
		Jack E. Bresenham, "Algorithm for computer control of a digital plotter", IBM Systems Journal, Vol. 4, No.1, January 1965, pp. 25–30
		http://www.research.ibm.com/journal/sj/041/ibmsjIVRIC.pdf
		 */
		void line2(int x0, int y0, int x1, int y1, float fAlpha, int nRed, int nGreen, int nBlue, float fLineWidth) {
		     boolean steep = Math.abs(y1 - y0) > Math.abs(x1 - x0);
		     if (steep) {
		         //swap(x0, y0)
		    	int h = x0;x0 = y0; y0 = h;
		         //swap(x1, y1)
		    	h = x1;x1 = y1; y1 = h;
		     }
		     if (x0 > x1) {
		         //swap(x0, x1)
		         //swap(y0, y1)
			    	int h = x0;x0 = x1; x1 = h;
			    	h = y0; y0 = y1; y1 = h;
		     }
		     int deltax = x1 - x0;
		     int deltay = Math.abs(y1 - y0);
		     int error = deltax / 2;
		     int ystep;
		     int y = y0;
		     if (y0 < y1) { 
		    	 ystep = 1 ;
		     } else {
		    	 ystep = -1;
		     }
		     for (int x = x0; x < x1; x++) {
		         if (steep) {
		        	 for (int i = 0; i <= (int) fLineWidth; i++) {
		        		 plot(y+i,x, fAlpha, nRed, nGreen, nBlue);
		        	 }
		         } else {
		        	 for (int i = 0; i <= (int) fLineWidth; i++) {
		        		 plot(x,y+i, fAlpha, nRed, nGreen, nBlue);
		        	 }
		         }
		         error = error - deltay;
		         if (error < 0) {
		             y = y + ystep;
		             error = error + deltax;
		         }
		     }
		}

			
			
			

	}
