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
 * TreeDrawer.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package viz.graphics;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Random;


/** this class takes care of drawing a single tree out of a tree set on 
 * a BufferedImageF. It uses a BranchDrawer to draw the actual branches
 * on the image to allow for different branch types (e.g. simple lines,
 * trapeziums, arcs, etc.)s
 */
public class TreeDrawer {
	BranchDrawer m_branchDrawer = new BranchDrawer();
	public void setBranchDrawer(BranchDrawer bd) {m_branchDrawer = bd;}
	public BranchDrawer getBranchDrawer() {return m_branchDrawer;}

	/** show trees as block tree, otherwise as triangular tree **/
	public boolean m_bViewBlockTree = false;
	/** show tree with root at the top, instead of on the left **/
	public boolean m_bRootAtTop = false;

	/** jitter of x-positions for x-coordinate **/
	int m_nJitter;
	public void setJitter(int nJitter) {m_nJitter= nJitter;}
	/** random number generator used for jitter **/
	Random m_random = new Random();
	
	/** image in memory containing tree set drawing **/
	BufferedImageF m_image;
	public void setImage(BufferedImageF image) {m_image = image;}
	
	/** width of lines used for drawing trees, etc. **/
	int m_nTreeWidth = 1;
	/** scale factor of width for meta data **/
	public float LINE_WIDTH_SCALE = 20;



	/**
	 * draw block tree using array representation of a tree. Adds jitter if
	 * required
	 **/
	void drawBlockTree(float[] nX, float[] nY, int [] color, Graphics2D g, float fScaleX, float fScaleY) {
		if (nX == null || nY == null) {
			return;
		}
		if (m_nJitter <= 0) {
			for (int i = 0; i < nX.length - 4; i+=4) {
				if (m_bRootAtTop) {
					m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY), (int) (nX[i] * fScaleX), (int) (nY[i+1] * fScaleY), 0, 0);
					m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i+1] * fScaleY), (int) (nX[i + 3] * fScaleX), (int) (nY[i + 1] * fScaleY), 0, 0);
					m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i+3] * fScaleX), (int) (nY[i+2] * fScaleY), (int) (nX[i + 3] * fScaleX), (int) (nY[i + 3] * fScaleY), 0, 0);
				} else {
					m_branchDrawer.draw(m_image, color[i], g, (int) (nY[i] * fScaleX), (int) (nX[i] * fScaleY), (int) (nY[i + 1] * fScaleX), (int) (nX[i] * fScaleY), 0, 0);
					m_branchDrawer.draw(m_image, color[i], g, (int) (nY[i+1] * fScaleX), (int) (nX[i] * fScaleY), (int) (nY[i + 1] * fScaleX), (int) (nX[i + 3] * fScaleY), 0, 0);
					m_branchDrawer.draw(m_image, color[i], g, (int) (nY[i+2] * fScaleX), (int) (nX[i+3] * fScaleY), (int) (nY[i + 3] * fScaleX), (int) (nX[i + 3] * fScaleY), 0, 0);					
				}
//				if (i % 4 != 3) {
//					m_branchDrawer.draw(m_image,g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY), 0, 0);
//				}
			}
		} else {
			int[] nXJ = new int[nX.length];
			for (int i = 0; i < nX.length; i++) {
				nXJ[i] = (int) (nX[i] * fScaleX) + m_random.nextInt(m_nJitter);
			}
			for (int i = 0; i < nX.length - 4; i += 4) {
				if (m_bRootAtTop) {
					m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY), (int) (nX[i] * fScaleX), (int) (nY[i+1] * fScaleY), 0, 0);
					m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i+1] * fScaleY), (int) (nX[i + 3] * fScaleX), (int) (nY[i + 1] * fScaleY), 0, 0);
					m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i+3] * fScaleX), (int) (nY[i+2] * fScaleY), (int) (nX[i + 3] * fScaleX), (int) (nY[i + 3] * fScaleY), 0, 0);
				} else {
					m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY), (int) (nX[i + 1] * fScaleX), (int) (nY[i] * fScaleY), 0, 0);
					m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i+1] * fScaleX), (int) (nY[i] * fScaleY), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 3] * fScaleY), 0, 0);
					m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i+2] * fScaleX), (int) (nY[i+3] * fScaleY), (int) (nX[i + 3] * fScaleX), (int) (nY[i + 3] * fScaleY), 0, 0);					
				}
			}
		}
	}

	/** draw block tree with variable line widths, where line width represents some information in the metadata **/
	void drawBlockTree(float[] nX, float[] nY, float[]fLineWidth, float [] fTopLineWidth, int [] color, Graphics2D g, float fScaleX, float fScaleY) {
		if (nX == null || nY == null) {
			return;
		}
		if (m_nJitter <= 0) {
			for (int i = 0; i < nX.length - 2; i++) {
				if (i % 4 != 3) {
					if (i % 4 == 0 || i % 4 == 2) {
						float fWidth = fLineWidth[i] * LINE_WIDTH_SCALE;
						float fTopWidth = fTopLineWidth[i] * LINE_WIDTH_SCALE;
						Stroke stroke = new BasicStroke(fWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
						g.setStroke(stroke);
						if (m_bViewBlockTree) {
							if (i % 4 == 0) {
								m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fTopWidth/2.0f), fTopWidth, fWidth);
							} else {
								// i % 4 == 2
								m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fTopWidth/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f), fWidth, fTopWidth);								
							}
						} else {
							if (i % 4 == 0) { 							
								float fTopWidth2 = fTopLineWidth[i+2] * LINE_WIDTH_SCALE;
								if (nY[i+1] <= nY[i+2]) {
									m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX)-1, (int) (((nY[i+1] + nY[i+2])/2.0) * fScaleY - (fTopWidth+fTopWidth2)/2.0f), fTopWidth, fWidth);
								} else {
									m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX)-1, (int) (((nY[i+1] + nY[i+2])/2.0) * fScaleY - (fTopWidth-fTopWidth2)/2.0f), fTopWidth, fWidth);										
								}
							} else {
								// i % 4 == 2
								float fTopWidth2 = fTopLineWidth[i-2] * LINE_WIDTH_SCALE;
								if (nY[i-1] <= nY[i]) {
									m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX)-1, (int) (((nY[i] +nY[i-1])/2.0)* fScaleY - (fTopWidth-fTopWidth2)/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f), fWidth, fTopWidth);
								} else {
									m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX)-1, (int) (((nY[i] +nY[i-1])/2.0)* fScaleY - (fTopWidth+fTopWidth2)/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f), fWidth, fTopWidth);
								}
							}
						}
					} else if (i % 4 == 1 && m_bViewBlockTree) {
						Stroke stroke = new BasicStroke(m_nTreeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
						g.setStroke(stroke);
						m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY), 0, 0);
					}
				}
			}
		} else {
			int[] nXJ = new int[nX.length];
			for (int i = 0; i < nX.length; i++) {
				nXJ[i] = (int) (nX[i] * fScaleX) + m_random.nextInt(m_nJitter);
			}
			for (int i = 0; i < nX.length - 1; i++) {
				if (i % 4 != 3) {
					m_branchDrawer.draw(m_image, color[i], g, nXJ[i], (int) (nY[i] * fScaleY), nXJ[i + 1], (int) (nY[i + 1] * fScaleY), 0, 0);
				}
				if (i % 4 == 0 || i % 4 == 2) {
					float fWidth = fLineWidth[i] * LINE_WIDTH_SCALE;
					Stroke stroke = new BasicStroke(fWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
					g.setStroke(stroke);
					m_branchDrawer.draw(m_image, color[i], g, nXJ[i], (int) (nY[i] * fScaleY - fWidth/2.0f), nXJ[i + 1], (int) (nY[i + 1] * fScaleY - fWidth/2.0f), fWidth, fTopLineWidth[i] * LINE_WIDTH_SCALE);
				} else if (i % 4 == 1) {
					Stroke stroke = new BasicStroke(m_nTreeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
					g.setStroke(stroke);
					m_branchDrawer.draw(m_image, color[i], g, nXJ[i], (int) (nY[i] * fScaleY), nXJ[i + 1], (int) (nY[i + 1] * fScaleY), 0, 0);
				}
			}
		}
	}
	
	
	/**
	 * draw triangle tree using array representation of a tree. Adds jitter
	 * if required
	 **/
	void drawTriangleTree(float[] nX, float[] nY, int [] color, Graphics2D g, float fScaleX, float fScaleY) {
		if (nX == null || nY == null) {
			return;
		}
		// ignore jitter for triangle trees
		if (m_bRootAtTop) {
			for (int i = 0; i < nX.length - 4; i++) {
				float fWidth = 0;
				float fTopWidth = 0;
				float fTopWidth2 = 0;
				if (i % 4 != 3) {
					if (i % 4 == 0 || i % 4 == 2) {
						if (i % 4 == 0) { 							
							if (nX[i+1] < nX[i+2]) {
								m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX - fWidth/2.0f), (int) (nY[i] * fScaleY), (int) (((nX[i+1] + nX[i+2])/2.0) * fScaleX - (fTopWidth+fTopWidth2)/2.0f), (int) (nY[i + 1] * fScaleY)-1, 0, 0);
							} else {
								m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX - fWidth/2.0f), (int) (nY[i] * fScaleY), (int) (((nX[i+1] + nX[i+2])/2.0) * fScaleX - (fTopWidth-fTopWidth2)/2.0f), (int) (nY[i + 1] * fScaleY)-1, 0, 0);										
							}
						} else {
							// i % 4 == 2
							if (nX[i-1] < nX[i]) {
								m_branchDrawer.draw(m_image, color[i], g, (int) (((nX[i] +nX[i-1])/2.0)* fScaleX - (fTopWidth-fTopWidth2)/2.0f), (int) (nY[i] * fScaleY)-1, (int) (nX[i + 1] * fScaleX - fWidth/2.0f), (int) (nY[i + 1] * fScaleY)-1, 0, 0);
							} else {
								m_branchDrawer.draw(m_image, color[i], g, (int) (((nX[i] +nX[i-1])/2.0)* fScaleX - (fTopWidth+fTopWidth2)/2.0f), (int) (nY[i] * fScaleY)-1, (int) (nX[i + 1] * fScaleX - fWidth/2.0f), (int) (nY[i + 1] * fScaleY)-1, 0, 0);
							}
						}
					}
				}
			}
		} else {
			for (int i = 0; i < nX.length - 4; i++) {
				float fWidth = 0;
				float fTopWidth = 0;
				float fTopWidth2 = 0;
				if (i % 4 != 3) {
					if (i % 4 == 0 || i % 4 == 2) {
						if (i % 4 == 0) { 							
							if (nY[i+1] < nY[i+2]) {
								m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX)-1, (int) (((nY[i+1] + nY[i+2])/2.0) * fScaleY - (fTopWidth+fTopWidth2)/2.0f), 0, 0);
							} else {
								m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX)-1, (int) (((nY[i+1] + nY[i+2])/2.0) * fScaleY - (fTopWidth-fTopWidth2)/2.0f), 0, 0);										
							}
						} else {
							// i % 4 == 2
							if (nY[i-1] < nY[i]) {
								m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX)-1, (int) (((nY[i] +nY[i-1])/2.0)* fScaleY - (fTopWidth-fTopWidth2)/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f), 0, 0);
							} else {
								m_branchDrawer.draw(m_image, color[i], g, (int) (nX[i] * fScaleX)-1, (int) (((nY[i] +nY[i-1])/2.0)* fScaleY - (fTopWidth+fTopWidth2)/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f), 0, 0);
							}
						}
					}
				}
			}
		}
	}

	public void draw(int i, float[][] fLinesX, float[][] fLinesY, float [][] fLineWidth, float [][] fTopLineWidth, int [][] nLineColor, Graphics2D g, float fScaleX, float fScaleY) {
		
		if (m_bViewBlockTree) {
			if (fLineWidth == null) {
//				if (m_bRootAtTop) {
					drawBlockTree(fLinesX[i], fLinesY[i], nLineColor[i], g, fScaleX, fScaleY);
//				} else {
//					drawBlockTree(fLinesY[i], fLinesX[i], g, fScaleX, fScaleY);
//				}
			} else {
				if (m_bRootAtTop) {
					drawBlockTree(fLinesX[i], fLinesY[i], fLineWidth[i], fTopLineWidth[i], nLineColor[i], g, fScaleX, fScaleY);
				} else {
					drawBlockTree(fLinesY[i], fLinesX[i], fLineWidth[i], fTopLineWidth[i], nLineColor[i], g, fScaleX, fScaleY);
				}
			}
		} else {
			if (fLineWidth == null) {
				if (m_bRootAtTop) {
					drawTriangleTree(fLinesX[i], fLinesY[i], nLineColor[i], g, fScaleX, fScaleY);
				} else {
					drawTriangleTree(fLinesY[i], fLinesX[i], nLineColor[i], g, fScaleX, fScaleY);
				}
			} else {
				if (m_bRootAtTop) {
					drawBlockTree(fLinesX[i], fLinesY[i], fLineWidth[i], fTopLineWidth[i], nLineColor[i], g, fScaleX, fScaleY);
				} else {
					drawBlockTree(fLinesY[i], fLinesX[i], fLineWidth[i], fTopLineWidth[i], nLineColor[i], g, fScaleX, fScaleY);
				}										
			}
		}
	} // draw
	
//	public void draw(Image rotate, float[][] fLinesX, float[][] fLinesY, float [][] fLineWidth, float [][] fTopLineWidth, Graphics2D g, float fScaleX, float fScaleY) {
//		if (m_bRootAtTop) {
//			drawTriangleTree(fLinesX[0], fLinesY[0], g, fScaleX, fScaleY);
//		} else {
//			drawTriangleTree(fLinesY[0], fLinesX[0], g, fScaleX, fScaleY);
//		}
//		for (int i = 2; i < fLinesX[0].length - 2; i += 4) {
//			int x = (int) (fLinesX[0][i] * fScaleX);
//			int y = (int) (fLinesY[0][i] * fScaleY);
//			g.drawImage(rotate, x, y, x + rotate.getWidth(null), y + rotate.getHeight(null), 
//					0, 0, rotate.getWidth(null), rotate.getHeight(null), null);
//		}		
//	}	
} // class BranchDrawer

