package viz;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.text.DecimalFormat;


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
	void viewClades(Graphics g) {
		float fScaleX = m_dt.m_fScaleX;
		float fScaleY = m_dt.m_fScaleY;
		g.setFont(m_font);
		g.setColor(m_color);
		if (m_dt.m_bUseLogScale) {
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
		if (m_dt.m_Panel.m_rotationPoints == null) {
			m_dt.m_Panel.m_rotationPoints = new RotationPoint[m_dt.m_cladeHeight.size()];
			bUpdatePoints = true;
		}
		String format = (m_nSignificantDigits > 0 ? "##.": "##");
		for (int i = 0; i < m_nSignificantDigits; i++) {
			format += "#";
		}
		DecimalFormat formatter = new DecimalFormat(format);
		DecimalFormat supportFormatter = new DecimalFormat("##.#");
		for (int i = 0/* m_dt.m_sLabels.size() */; i < m_dt.m_cladeHeight.size(); i++) {
			if (m_dt.m_cladeWeight.get(i) > 0.01 && (
					(m_dt.m_Xmode == 1 && (m_dt.m_clades.get(i).length > 1 || m_dt.m_bLeafCladeSelection)) 
					|| (m_dt.m_Xmode == 2 && m_dt.m_clades.get(i).length == 1))) {
				if (!m_dt.m_treeDrawer.m_bRootAtTop) {
					x = (int) ((m_dt.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
					y = (int) (m_dt.m_cladePosition[i] * fScaleY);
					if (m_bDrawMean && (!m_bSelectedOnly || m_dt.m_cladeSelection.contains(i))) {
						g.drawLine(x, y- 3, x, y+6);
					}
					if (m_bTextMean && (!m_bSelectedOnly || m_dt.m_cladeSelection.contains(i))) {
						g.drawString(formatter.format((m_dt.m_gridDrawer.m_fGridOrigin + m_dt.m_fHeight - m_dt.m_cladeHeight.get(i)) * m_dt.m_fUserScale), x, y - 1);
					}
					if (m_bTextSupport && (!m_bSelectedOnly || m_dt.m_cladeSelection.contains(i))) {
						g.drawString(supportFormatter.format(100*m_dt.m_cladeWeight.get(i)), x, y + g.getFont().getSize() + 1);
					}
				} else {
					x = (int) (m_dt.m_cladePosition[i] * fScaleX);
					y = /*nHeight -*/ (int) ((m_dt.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
					if (m_bDrawMean && (!m_bSelectedOnly || m_dt.m_cladeSelection.contains(i))) {
						g.drawLine(x -3, y, x + 3, y);
					}
					if (m_bTextMean && (!m_bSelectedOnly || m_dt.m_cladeSelection.contains(i))) {
						g.drawString(formatter.format((m_dt.m_gridDrawer.m_fGridOrigin + m_dt.m_fHeight - m_dt.m_cladeHeight.get(i)) * m_dt.m_fUserScale), x, y - 1);
					}
					if (m_bTextSupport && (!m_bSelectedOnly || m_dt.m_cladeSelection.contains(i))) {
						g.drawString(supportFormatter.format(100*m_dt.m_cladeWeight.get(i)), x, y + g.getFont().getSize() + 1);
					}
				}
			} else {
				x = -100;
				y = -100;
			}
			w = (int)(10 +  m_dt.m_cladeWeight.get(i)*10);
			h = w;
			if (m_bDrawSupport && (!m_bSelectedOnly || m_dt.m_cladeSelection.contains(i))) {
				g.drawOval(x- w / 2, y- h / 2, w, h);
			}
			//g.drawImage(m_dt.m_rotate, x - w / 2, y - h / 2, x + h / 2, y + w / 2, 0, 0, h, w, null);
			if (bUpdatePoints) {
				m_dt.m_Panel.m_rotationPoints[i] = new RotationPoint(x, y);
			}
			
			if (!m_dt.m_treeDrawer.m_bRootAtTop) {
				x = (int) ((m_dt.m_cladeHeight95HPDdown.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
				//y = (int) (m_dt.m_cladePosition[i] * fScaleY);
				w = - x + (int) ((m_dt.m_cladeHeight95HPDup.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
				h = 3;
				if (m_bText95HPD && (!m_bSelectedOnly || m_dt.m_cladeSelection.contains(i))) {
					g.drawString(formatter.format((m_dt.m_cladeHeight95HPDup.get(i) - m_dt.m_cladeHeight95HPDdown.get(i)) * m_dt.m_fUserScale), x, y- 1);
				}
			} else {
				//x = (int) (m_dt.m_cladePosition[i] * fScaleX);
				y = (int) ((m_dt.m_cladeHeight95HPDdown.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
				w = 3;
				h = - y + (int) ((m_dt.m_cladeHeight95HPDup.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
				if (m_bText95HPD && (!m_bSelectedOnly || m_dt.m_cladeSelection.contains(i))) {
					g.drawString(formatter.format((m_dt.m_cladeHeight95HPDup.get(i) - m_dt.m_cladeHeight95HPDdown.get(i)) * m_dt.m_fUserScale), x, y- 1);
				}
			}
			if (m_bDraw95HPD && (!m_bSelectedOnly || m_dt.m_cladeSelection.contains(i))) {
				g.drawRect(x, y, w, h);
			}
			
		}

		// draw selection
		if (!m_bSelectedOnly) {
			stroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
			((Graphics2D) g).setStroke(stroke);
			((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			for (int i :  m_dt.m_cladeSelection) {
				if (!m_dt.m_treeDrawer.m_bRootAtTop) {
					x = (int) ((m_dt.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
					y = (int) (m_dt.m_cladePosition[i] * fScaleY);
				} else {
					x = (int) (m_dt.m_cladePosition[i] * fScaleX);
					y = /*nHeight -*/ (int) ((m_dt.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
				}
				w = (int)(10 +  m_dt.m_cladeWeight.get(i)*10);
				h = w;
				g.drawOval(x- w / 2, y- h / 2, w, h);
			}
		}
	
	}

}
