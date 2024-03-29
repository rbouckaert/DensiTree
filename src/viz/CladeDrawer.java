package viz;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Set;


public class CladeDrawer {
	DensiTree m_dt;
	public Font m_font;
	public Color m_color;
	public int m_nSignificantDigits = 3;
	
	public boolean m_bDrawMean = false;
	public boolean m_bDrawSupport = true;
	public boolean m_bDraw95HPD = false;

	public boolean m_bTextMean = false;
	public boolean m_bTextSupport = false;
	public boolean m_bText95HPD = false;

	public boolean m_bSelectedOnly = false;

	CladeDrawer(DensiTree dt) {
		m_dt = dt;
		m_font = Font.getFont("default");
		m_color = Color.black;
	}
	
	/** show all clades **/
	void viewClades(Graphics g, TreeData treeData) {
		boolean reverseText = treeData.reverse();
		float fScaleX = m_dt.m_fScaleX;
		float fScaleY = m_dt.m_fScaleY;
		g.setFont(m_font);
		g.setColor(m_color);
		if (m_dt.m_settings.m_bUseLogScale) {
			if (m_dt.m_treeDrawer.m_bRootAtTop) {
				fScaleY *= m_dt.m_fHeight / (float) Math.log(m_dt.m_fHeight + 1.0);
			} else {
				fScaleX *= m_dt.m_fHeight / (float) Math.log(m_dt.m_fHeight + 1.0);
			}
		}
		int x = 0;
		int y = 0;
		Stroke stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
		((Graphics2D) g).setStroke(stroke);
		((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

		int h = m_dt.m_rotate.getHeight(null);
		int w = m_dt.m_rotate.getWidth(null);
		boolean bUpdatePoints = false;
		if (treeData.m_rotationPoints == null) {
			treeData.m_rotationPoints = new RotationPoint[treeData.m_cladeHeight.size()];
			bUpdatePoints = true;
		}
		String format = (m_nSignificantDigits > 0 ? "##.": "##");
		for (int i = 0; i < m_nSignificantDigits; i++) {
			format += "#";
		}
		DecimalFormat formatter = new DecimalFormat(format);
		formatter.setMinimumFractionDigits(m_nSignificantDigits);
		formatter.setMaximumFractionDigits(m_nSignificantDigits);
		DecimalFormat supportFormatter = new DecimalFormat(format);//"##.#");
		Set<Integer> m_cladeSelection = treeData.getCladeSelection();
		for (int i = 0/* m_dt.m_sLabels.size() */; i < treeData.m_cladeHeight.size(); i++) {
			if (treeData.m_cladeWeight.get(i) > m_dt.m_settings.m_smallestCladeSupport && (
					(m_dt.m_settings.m_Xmode == 1 && (treeData.m_clades.get(i).length > 1 || m_dt.m_bLeafCladeSelection)) 
					|| (m_dt.m_settings.m_Xmode == 2 && treeData.m_clades.get(i).length == 1))) {
				if (!m_dt.m_treeDrawer.m_bRootAtTop) {
					x = (int) ((treeData.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
					y = (int) (treeData.m_cladePosition[i] * fScaleY);
					if (m_bDrawMean && (!m_bSelectedOnly || m_cladeSelection.contains(i))) {
						g.drawLine(x, y- 3, x, y+6);
					}
					if (m_bTextMean && (!m_bSelectedOnly || m_cladeSelection.contains(i))) {
						drawString(g, formatter.format((m_dt.m_gridDrawer.m_fGridOrigin + m_dt.m_fHeight - treeData.m_cladeHeight.get(i)) * m_dt.m_fUserScale), x, y - 1, reverseText);
					}
					if (m_bTextSupport && (!m_bSelectedOnly || m_cladeSelection.contains(i))) {
						drawString(g, supportFormatter.format(100*treeData.m_cladeWeight.get(i)), x, y + g.getFont().getSize() + 1, reverseText);
					}
				} else {
					x = (int) (treeData.m_cladePosition[i] * fScaleX);
					y = /*nHeight -*/ (int) ((treeData.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
					if (m_bDrawMean && (!m_bSelectedOnly || m_cladeSelection.contains(i))) {
						g.drawLine(x -3, y, x + 3, y);
					}
					if (m_bTextMean && (!m_bSelectedOnly || m_cladeSelection.contains(i))) {
						drawString(g, formatter.format((m_dt.m_gridDrawer.m_fGridOrigin + m_dt.m_fHeight - treeData.m_cladeHeight.get(i)) * m_dt.m_fUserScale), x, y - 1, reverseText);
					}
					if (m_bTextSupport && (!m_bSelectedOnly || m_cladeSelection.contains(i))) {
						drawString(g, supportFormatter.format(100*treeData.m_cladeWeight.get(i)), x, y + g.getFont().getSize() + 1, reverseText);
					}
				}
			} else {
				x = -100;
				y = -100;
			}
			w = (int)(10 +  treeData.m_cladeWeight.get(i)*10);
			h = w;
			if (m_bDrawSupport && (!m_bSelectedOnly || m_cladeSelection.contains(i))) {
				g.drawOval(x- w / 2, y- h / 2, w, h);
			}
			//g.drawImage(m_dt.m_rotate, x - w / 2, y - h / 2, x + h / 2, y + w / 2, 0, 0, h, w, null);
			if (bUpdatePoints) {
				if (treeData.reverse()) {
					treeData.m_rotationPoints[i] = new RotationPoint(m_dt.getWidth() - x, y);
				} else {
					treeData.m_rotationPoints[i] = new RotationPoint(x, y);
				}
			}
			
			if (!m_dt.m_treeDrawer.m_bRootAtTop) {
				x = (int) ((treeData.m_cladeHeight95HPDdown.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
				//y = (int) (m_dt.m_cladePosition[i] * fScaleY);
				w = - x + (int) ((treeData.m_cladeHeight95HPDup.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
				h = 3;
				if (m_bText95HPD && (!m_bSelectedOnly || m_cladeSelection.contains(i))) {
					drawString(g, formatter.format((treeData.m_cladeHeight95HPDup.get(i) - treeData.m_cladeHeight95HPDdown.get(i)) * m_dt.m_fUserScale), x, y- 1, reverseText);
				}
			} else {
				//x = (int) (m_dt.m_cladePosition[i] * fScaleX);
				y = (int) ((treeData.m_cladeHeight95HPDdown.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
				w = 3;
				h = - y + (int) ((treeData.m_cladeHeight95HPDup.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
				if (m_bText95HPD && (!m_bSelectedOnly || m_cladeSelection.contains(i))) {
					drawString(g, formatter.format((treeData.m_cladeHeight95HPDup.get(i) - treeData.m_cladeHeight95HPDdown.get(i)) * m_dt.m_fUserScale), x, y- 1, reverseText);
				}
			}
			if (m_bDraw95HPD && (!m_bSelectedOnly || m_cladeSelection.contains(i))) {
				g.drawRect(x, y, w, h);
			}
			
		}

		
		if (treeData == m_dt.m_treeData2) {
			int xx = 4;
			xx--;
		}
		
		
		// draw selection
		if (!m_bSelectedOnly) {
			stroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
			((Graphics2D) g).setStroke(stroke);
			((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			for (int i :  treeData.getCladeSelection()) {
				if (!m_dt.m_treeDrawer.m_bRootAtTop) {
					x = (int) ((treeData.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
					y = (int) (treeData.m_cladePosition[i] * fScaleY);
				} else {
					x = (int) (treeData.m_cladePosition[i] * fScaleX);
					y = /*nHeight -*/ (int) ((treeData.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
				}
				w = (int)(10 +  treeData.m_cladeWeight.get(i)*10);
				h = w;
				g.drawOval(x- w / 2, y- h / 2, w, h);
				
//				if (m_dt.treeData2 != null) {
//					TreeData treeData2 = m_dt.treeData2;
//					String clade = Arrays.toString(m_dt.treeData.m_clades.get(i));
//					Integer j = m_dt.m_mirrorCladeToCladeMap.get(clade);
//					if (j != null) {
//						if (!m_dt.m_treeDrawer.m_bRootAtTop) {
//							x = (int) ((treeData2.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
//							y = (int) (treeData2.m_cladePosition[i] * fScaleY);
//						} else {
//							x = (int) (treeData2.m_cladePosition[i] * fScaleX);
//							y = /*nHeight -*/ (int) ((treeData2.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
//						}
//						w = (int)(10 +  treeData2.m_cladeWeight.get(i)*10);
//						h = w;
//						((Graphics2D)g).setTransform(new AffineTransform(1,0,0,1, m_dt.getWidth()/2, 0));
//						g.drawOval(x- w / 2, y- h / 2, w, h);
//						((Graphics2D)g).setTransform(new AffineTransform(1,0,0,1, 0, 0));
//					}
//				}
			}
		}
	
	}
	
	void drawString(Graphics g, String str, int x, int y, boolean reverseText) {
		if (reverseText) {
			AffineTransform org = ((Graphics2D)g).getTransform();
			double w = org.getTranslateX();
			((Graphics2D)g).setTransform(new AffineTransform(1,0,0,1, 0, 0));
			g.drawString(str, (int)w-x, y);
			((Graphics2D)g).setTransform(org);
		} else {
			g.drawString(str, x, y);
		}
	}

}
