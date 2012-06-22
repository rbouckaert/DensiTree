package viz;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.text.DecimalFormat;

public class CladeDrawer {
	DensiTree m_dt;
	
	CladeDrawer(DensiTree dt) {
		m_dt = dt;
	}
	
	/** show all clades **/
	void viewClades(Graphics g) {
		float fScaleX = m_dt.m_fScaleX;
		float fScaleY = m_dt.m_fScaleY;
		if (m_dt.m_bUseLogScale) {
			if (m_dt.m_treeDrawer.m_bRootAtTop) {
				fScaleY *= m_dt.m_fHeight / (float) Math.log(m_dt.m_fHeight + 1.0);
			} else {
				fScaleX *= m_dt.m_fHeight / (float) Math.log(m_dt.m_fHeight + 1.0);
			}
		}
		int x = 0;
		int y = 0;
		g.setColor(Color.BLACK);
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
		int nHeight = m_dt.m_Panel.getHeight();
		DecimalFormat formatter = new DecimalFormat("##.###");
		for (int i = 0/* m_dt.m_sLabels.size() */; i < m_dt.m_cladeHeight.size(); i++) {
			if (m_dt.m_cladeWeight.get(i) > 0.01 && ((m_dt.m_Xmode == 1 && m_dt.m_clades.get(i).length > 1) || (m_dt.m_Xmode == 2 && m_dt.m_clades.get(i).length == 1))) {
				if (!m_dt.m_treeDrawer.m_bRootAtTop) {
					x = (int) ((m_dt.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
					y = (int) (m_dt.m_cladePosition[i] * fScaleY);
					g.drawLine(x, y- 3, x, y+6);
					g.drawString(formatter.format(m_dt.m_gridDrawer.m_fGridOffset + m_dt.m_cladeHeight.get(i)), x, y);
				} else {
					x = (int) (m_dt.m_cladePosition[i] * fScaleX);
					y = /*nHeight -*/ (int) ((m_dt.m_cladeHeight.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
					g.drawLine(x -3, y, x + 3, y);
					g.drawString(formatter.format(m_dt.m_gridDrawer.m_fGridOffset + m_dt.m_cladeHeight.get(i)), x, y);
				}
			} else {
				x = -100;
				y = -100;
			}
			w = (int)(10 +  m_dt.m_cladeWeight.get(i)*10);
			h = w;
			g.drawOval(x- w / 2, y- h / 2, w, h);
			//g.drawImage(m_dt.m_rotate, x - w / 2, y - h / 2, x + h / 2, y + w / 2, 0, 0, h, w, null);
			if (bUpdatePoints) {
				m_dt.m_Panel.m_rotationPoints[i] = new RotationPoint(x, y);
			}
			
			if (!m_dt.m_treeDrawer.m_bRootAtTop) {
				x = (int) ((m_dt.m_cladeHeight95HPDdown.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
				//y = (int) (m_dt.m_cladePosition[i] * fScaleY);
				w = - x + (int) ((m_dt.m_cladeHeight95HPDup.get(i) - m_dt.m_fTreeOffset) * fScaleX * m_dt.m_fTreeScale);
				h = 3;
				g.drawString(formatter.format(m_dt.m_cladeHeight95HPDup.get(i) - m_dt.m_cladeHeight95HPDdown.get(i)), x, y);
			} else {
				//x = (int) (m_dt.m_cladePosition[i] * fScaleX);
				y = (int) ((m_dt.m_cladeHeight95HPDdown.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
				w = 3;
				h = - y + (int) ((m_dt.m_cladeHeight95HPDup.get(i) - m_dt.m_fTreeOffset) * fScaleY * m_dt.m_fTreeScale);
				g.drawString(formatter.format(m_dt.m_cladeHeight95HPDup.get(i) - m_dt.m_cladeHeight95HPDdown.get(i)), x, y);
			}
			g.drawRect(x, y, w, h);
			
		}

		// draw selection
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
