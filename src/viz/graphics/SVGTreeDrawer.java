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
 * SVGTreeBranchDrawer.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/

package viz.graphics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Random;


/** This class takes care of drawing a single tree out of a tree set in SVG
 * into a StringBuffer 
 */
public class SVGTreeDrawer extends TreeDrawer {
//	StringBuffer m_buf;
//	
//	public SVGTreeDrawer(StringBuffer buf) {
//		m_buf = buf;
//	}
//
//	public int m_branchStyle = 0;
//	
//	private void draw(int nX1, int nY1, int nX2, int nY2, float fWidth) {
//		m_buf.append("<path " +
//				"fill='none' " +
//				"stroke='rgb(" +m_color.getRed()+ "," + m_color.getGreen() +"," + m_color.getBlue()+")' " +
//				"stroke-width='"+ fWidth+"' " +
//				"opacity='" + m_fAlpha + "' " +
//				" d='");
//		m_buf.append("M"+nX1+" "+nY1+"L"+nX2+" "+nY2);
//		m_buf.append("'/>\n");
//	}
//
//	
//	private void draw(int nX1, int nY1, int nX2, int nY2) {
//		m_buf.append("M"+nX1+" "+nY1+"L"+nX2+" "+nY2);
//	}
//	private void drawarc(int nX1, int nY1, int nX2, int nY2) {
//		m_buf.append("M"+nX1+" "+nY1+"A"+(nX2-nX1)+","+(nY2-nY1) + " 0 0,0 "
//				+nX2+","+nY2);
//	}
//
//
//	/**
//	 * draw block tree using array representation of a tree. Adds jitter if
//	 * required
//	 **/
//	void drawBlockTree(float[] nX, float[] nY, Graphics2D g, float fScaleX, float fScaleY) {
//		if (nX == null || nY == null) {
//			return;
//		}
//		float fLineWidth = ((BasicStroke)g.getStroke()).getLineWidth();
//		float fAlpha = ((AlphaComposite)g.getComposite()).getAlpha();
//		Color color = g.getColor();
//		m_buf.append("<path " +
//				"fill='none' " +
//				"stroke='rgb(" +color.getRed()+ "," + color.getGreen() +"," + color.getBlue()+")' " +
//				"stroke-width='"+ fLineWidth+"' " +
//				"opacity='" + fAlpha + "' " +
//				" d='");
//
//		if (m_nJitter <= 0) {
//			
//			for (int i = 0; i < nX.length - 1; i+=4) {
//				if (m_bRootAtTop) {
//					draw((int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY), (int) (nX[i] * fScaleX), (int) (nY[i+1] * fScaleY));
//					draw((int) (nX[i] * fScaleX), (int) (nY[i+1] * fScaleY), (int) (nX[i + 3] * fScaleX), (int) (nY[i + 1] * fScaleY));
//					draw((int) (nX[i+3] * fScaleX), (int) (nY[i+2] * fScaleY), (int) (nX[i + 3] * fScaleX), (int) (nY[i + 3] * fScaleY));
//				} else {
//					draw((int) (nY[i] * fScaleX), (int) (nX[i] * fScaleY), (int) (nY[i + 1] * fScaleX), (int) (nX[i] * fScaleY));
//					draw((int) (nY[i+1] * fScaleX), (int) (nX[i] * fScaleY), (int) (nY[i + 1] * fScaleX), (int) (nX[i + 3] * fScaleY));
//					draw((int) (nY[i+2] * fScaleX), (int) (nX[i+3] * fScaleY), (int) (nY[i + 3] * fScaleX), (int) (nX[i + 3] * fScaleY));					
//				}
//			}
///*			for (int i = 0; i < nX.length - 1; i++) {
//				if (i % 4 != 3) {
//					draw( (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY));
//				}
//			}
//*/		} else {
//			int[] nXJ = new int[nX.length];
//			for (int i = 0; i < nX.length; i++) {
//				nXJ[i] = (int) (nX[i] * fScaleX) + m_random.nextInt(m_nJitter);
//			}
//			for (int i = 0; i < nX.length - 1; i++) {
//				if (i % 4 != 3) {
//					draw( nXJ[i], (int) (nY[i] * fScaleY), nXJ[i + 1], (int) (nY[i + 1] * fScaleY));
//				}
//			}
//		}
//		m_buf.append("'/>\n");
//	}
//
//	/** draw block tree with variable line widths, where line width represents some information in the metadata **/
//	void drawBlockTree(float[] nX, float[] nY, float[]fLineWidth, float [] fTopLineWidth, Graphics2D g, float fScaleX, float fScaleY) {
//		if (nX == null || nY == null) {
//			return;
//		}
//		if (m_nJitter <= 0) {
//			for (int i = 0; i < nX.length - 2; i++) {
//				if (i % 4 != 3) {
//					if (i % 4 == 0 || i % 4 == 2) {
//						float fWidth = fLineWidth[i] * LINE_WIDTH_SCALE;
//						float fTopWidth = fTopLineWidth[i] * LINE_WIDTH_SCALE;
//						Stroke stroke = new BasicStroke(fWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
//						((Graphics2D) g).setStroke(stroke);
//						if (m_bViewBlockTree) {
//							if (i % 4 == 0) {
//								draw( (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fTopWidth/2.0f), fWidth);
//							} else {
//								// i % 4 == 2
//								draw( (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fTopWidth/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f), fWidth);								
//							}
//						} else {
//							if (i % 4 == 0) { 							
//								float fTopWidth2 = fTopLineWidth[i+2] * LINE_WIDTH_SCALE;
//								if (nY[i+1] < nY[i+2]) {
//									draw( (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX)-1, (int) (((nY[i+1] + nY[i+2])/2.0) * fScaleY - (fTopWidth+fTopWidth2)/2.0f), fWidth);
//								} else {
//									draw( (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX)-1, (int) (((nY[i+1] + nY[i+2])/2.0) * fScaleY - (fTopWidth-fTopWidth2)/2.0f), fWidth);										
//								}
//							} else {
//								// i % 4 == 2
//								float fTopWidth2 = fTopLineWidth[i-2] * LINE_WIDTH_SCALE;
//								if (nY[i-1] < nY[i]) {
//									draw( (int) (nX[i] * fScaleX)-1, (int) (((nY[i] +nY[i-1])/2.0)* fScaleY - (fTopWidth-fTopWidth2)/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f), fWidth);
//								} else {
//									draw( (int) (nX[i] * fScaleX)-1, (int) (((nY[i] +nY[i-1])/2.0)* fScaleY - (fTopWidth+fTopWidth2)/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f), fWidth);
//								}
//							}
//						}
//					} else if (i % 4 == 1 && m_bViewBlockTree) {
//						Stroke stroke = new BasicStroke(m_nTreeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
//						((Graphics2D) g).setStroke(stroke);
//						draw( (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY), m_nTreeWidth);
//					}
//				}
//			}
//		} else {
//			int[] nXJ = new int[nX.length];
//			for (int i = 0; i < nX.length; i++) {
//				nXJ[i] = (int) (nX[i] * fScaleX) + m_random.nextInt(m_nJitter);
//			}
//			for (int i = 0; i < nX.length - 1; i++) {
//				if (i % 4 != 3) {
//					draw( nXJ[i], (int) (nY[i] * fScaleY), nXJ[i + 1], (int) (nY[i + 1] * fScaleY), m_nTreeWidth);
//				}
//				if (i % 4 == 0 || i % 4 == 2) {
//					float fWidth = fLineWidth[i] * LINE_WIDTH_SCALE;
//					Stroke stroke = new BasicStroke(fWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
//					((Graphics2D) g).setStroke(stroke);
//					draw( nXJ[i], (int) (nY[i] * fScaleY - fWidth/2.0f), nXJ[i + 1], (int) (nY[i + 1] * fScaleY - fWidth/2.0f), fWidth);
//				} else if (i % 4 == 1) {
//					Stroke stroke = new BasicStroke(m_nTreeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
//					((Graphics2D) g).setStroke(stroke);
//					draw( nXJ[i], (int) (nY[i] * fScaleY), nXJ[i + 1], (int) (nY[i + 1] * fScaleY), m_nTreeWidth);
//				}
//			}
//		}
//	}
//	
//	
//	/**
//	 * draw triangle tree using array representation of a tree. Adds jitter
//	 * if required
//	 **/
//	void drawTriangleTree(float[] nX, float[] nY, Graphics2D g, float fScaleX, float fScaleY) {
//		if (nX == null || nY == null) {
//			return;
//		}
//		float fLineWidth = ((BasicStroke)g.getStroke()).getLineWidth();
//		float fAlpha = ((AlphaComposite)g.getComposite()).getAlpha();
//		Color color = g.getColor();
//		m_buf.append("<path " +
//				"fill='none' " +
//				"stroke='rgb(" +color.getRed()+ "," + color.getGreen() +"," + color.getBlue()+")' " +
//				"stroke-width='"+ fLineWidth+"' " +
//				"opacity='" + fAlpha + "' " +
//				" d='");
//
//		// ignore jitter for triangle trees
//		if (m_bRootAtTop) {
//			for (int i = 0; i < nX.length - 4; i++) {
//				float fWidth = 0;
//				float fTopWidth = 0;
//				float fTopWidth2 = 0;
//				if (i % 4 != 3) {
//					if (i % 4 == 0 || i % 4 == 2) {
//						if (m_branchStyle == 2) {
//							if (i % 4 == 0) { 							
//								if (nX[i+1] < nX[i+2]) {
//									drawarc( (int) (nX[i] * fScaleX - fWidth/2.0f), (int) (nY[i] * fScaleY), (int) (((nX[i+1] + nX[i+2])/2.0) * fScaleX - (fTopWidth+fTopWidth2)/2.0f), (int) (nY[i + 1] * fScaleY)-1);
//								} else {
//									drawarc( (int) (nX[i] * fScaleX - fWidth/2.0f), (int) (nY[i] * fScaleY), (int) (((nX[i+1] + nX[i+2])/2.0) * fScaleX - (fTopWidth-fTopWidth2)/2.0f), (int) (nY[i + 1] * fScaleY)-1);										
//								}
//							} else {
//								// i % 4 == 2
//								if (nX[i-1] < nX[i]) {
//									drawarc( (int) (((nX[i] +nX[i-1])/2.0)* fScaleX - (fTopWidth-fTopWidth2)/2.0f), (int) (nY[i] * fScaleY)-1, (int) (nX[i + 1] * fScaleX - fWidth/2.0f), (int) (nY[i + 1] * fScaleY)-1);
//								} else {
//									drawarc( (int) (((nX[i] +nX[i-1])/2.0)* fScaleX - (fTopWidth+fTopWidth2)/2.0f), (int) (nY[i] * fScaleY)-1, (int) (nX[i + 1] * fScaleX - fWidth/2.0f), (int) (nY[i + 1] * fScaleY)-1);
//								}
//							}
//						} else {
//							if (i % 4 == 0) { 							
//								if (nX[i+1] < nX[i+2]) {
//									draw( (int) (nX[i] * fScaleX - fWidth/2.0f), (int) (nY[i] * fScaleY), (int) (((nX[i+1] + nX[i+2])/2.0) * fScaleX - (fTopWidth+fTopWidth2)/2.0f), (int) (nY[i + 1] * fScaleY)-1);
//								} else {
//									draw( (int) (nX[i] * fScaleX - fWidth/2.0f), (int) (nY[i] * fScaleY), (int) (((nX[i+1] + nX[i+2])/2.0) * fScaleX - (fTopWidth-fTopWidth2)/2.0f), (int) (nY[i + 1] * fScaleY)-1);										
//								}
//							} else {
//								// i % 4 == 2
//								if (nX[i-1] < nX[i]) {
//									draw( (int) (((nX[i] +nX[i-1])/2.0)* fScaleX - (fTopWidth-fTopWidth2)/2.0f), (int) (nY[i] * fScaleY)-1, (int) (nX[i + 1] * fScaleX - fWidth/2.0f), (int) (nY[i + 1] * fScaleY)-1);
//								} else {
//									draw( (int) (((nX[i] +nX[i-1])/2.0)* fScaleX - (fTopWidth+fTopWidth2)/2.0f), (int) (nY[i] * fScaleY)-1, (int) (nX[i + 1] * fScaleX - fWidth/2.0f), (int) (nY[i + 1] * fScaleY)-1);
//								}
//							}
//						}
//					}
//				}
//			}
//		} else {
//			for (int i = 0; i < nX.length - 4; i++) {
//				float fWidth = 0;
//				float fTopWidth = 0;
//				float fTopWidth2 = 0;
//				if (i % 4 != 3) {
//					if (i % 4 == 0 || i % 4 == 2) {
//						if (m_branchStyle == 2) {
//							if (i % 4 == 0) { 							
//								if (nY[i+1] < nY[i+2]) {
//									drawarc( (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX)-1, (int) (((nY[i+1] + nY[i+2])/2.0) * fScaleY - (fTopWidth+fTopWidth2)/2.0f));
//								} else {
//									drawarc( (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX)-1, (int) (((nY[i+1] + nY[i+2])/2.0) * fScaleY - (fTopWidth-fTopWidth2)/2.0f));										
//								}
//							} else {
//								// i % 4 == 2
//								if (nY[i-1] < nY[i]) {
//									drawarc( (int) (nX[i] * fScaleX)-1, (int) (((nY[i] +nY[i-1])/2.0)* fScaleY - (fTopWidth-fTopWidth2)/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f));
//								} else {
//									drawarc( (int) (nX[i] * fScaleX)-1, (int) (((nY[i] +nY[i-1])/2.0)* fScaleY - (fTopWidth+fTopWidth2)/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f));
//								}
//							}
//						} else {
//							if (i % 4 == 0) { 							
//								if (nY[i+1] < nY[i+2]) {
//									draw( (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX)-1, (int) (((nY[i+1] + nY[i+2])/2.0) * fScaleY - (fTopWidth+fTopWidth2)/2.0f));
//								} else {
//									draw( (int) (nX[i] * fScaleX), (int) (nY[i] * fScaleY - fWidth/2.0f), (int) (nX[i + 1] * fScaleX)-1, (int) (((nY[i+1] + nY[i+2])/2.0) * fScaleY - (fTopWidth-fTopWidth2)/2.0f));										
//								}
//							} else {
//								// i % 4 == 2
//								if (nY[i-1] < nY[i]) {
//									draw( (int) (nX[i] * fScaleX)-1, (int) (((nY[i] +nY[i-1])/2.0)* fScaleY - (fTopWidth-fTopWidth2)/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f));
//								} else {
//									draw( (int) (nX[i] * fScaleX)-1, (int) (((nY[i] +nY[i-1])/2.0)* fScaleY - (fTopWidth+fTopWidth2)/2.0f), (int) (nX[i + 1] * fScaleX), (int) (nY[i + 1] * fScaleY - fWidth/2.0f));
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		m_buf.append("'/>\n");
//	}
//
//	
//	Color m_color;
//	float m_fAlpha;
} // class BranchDrawer

