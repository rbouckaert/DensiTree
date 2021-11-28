package viz;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Map;

import javax.swing.JPanel;

public class CladeSetComparisonPanel extends JPanel implements MouseListener {
	private static final long serialVersionUID = 1L;
	
	DensiTree m_dt;
	
	CladeSetComparisonPanel(DensiTree dt) {
		m_dt = dt;
		addMouseListener(this);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		
		g.setColor(Color.white);
		g.clearRect(0, 0, getWidth(), getHeight());

		if (m_dt.treeData2 == null || m_dt.m_mirrorCladeToIDMap == null) {
			g.setColor(Color.blue);
			g.drawString(" Can only draw comparison", 0, getHeight()/2 - 15);
			g.drawString(" when mirror set is loaded", 0, getHeight()/2);
			g.drawString(" using File/Load mirror menu", 0, getHeight()/2 + 15);
			return;
		}
		Graphics2D g2 = (Graphics2D) g;
		
		initGraph(g2);
		
		Map<String,Integer> map = m_dt.m_mirrorCladeToIDMap;
		for (int i = 0; i < m_dt.treeData.m_cladeHeight.size(); i++) {
			output(g2, i, map, false);
		}
		
		for (int i : m_dt.treeData.getCladeSelection()) {
			output(g2, i, map, true);
		}
	}

	
	private void output(Graphics2D g2, int i, Map<String,Integer> map, boolean highlight) {
		double h1 = m_dt.treeData.m_cladeHeight.get(i);
		double lo1 = m_dt.treeData.m_cladeHeight95HPDdown.get(i);
		double hi1 = m_dt.treeData.m_cladeHeight95HPDup.get(i);
		double support1 = m_dt.treeData.m_cladeWeight.get(i);
		int [] clade = m_dt.treeData.m_clades.get(i);
		Integer j = map.get(Arrays.toString(clade));
		if (j != null) {
			double h2 = m_dt.treeData2.m_cladeHeight.get(j);
			double lo2 = m_dt.treeData2.m_cladeHeight95HPDdown.get(j);
			double hi2 = m_dt.treeData2.m_cladeHeight95HPDup.get(j);
			double support2 = m_dt.treeData2.m_cladeWeight.get(j);
			output(g2, m_dt.m_fHeight, h1, lo1, hi1, h2, lo2, hi2, support1, support2, highlight);
		} else {
			output(g2, m_dt.m_fHeight, h1, lo1, hi1, 0.0, 0.0, 0.0, support1, 0.0, highlight);
		}
	}


	private int w; // width of  panel including labels
	private int h; // height of panel including labels
	private int off; // offset space for labels

	private void initGraph(Graphics2D g) {
		g.setColor(Color.white);
		w = getWidth();
		h = getHeight();
		off = Math.min(50, w/2);;
		g.fillRect(0, 0, w, h);
		
		g.setColor(Color.black);
		g.drawRect(off, off, w-2*off, h-2*off);
		
		// diagonals
		g.drawLine(off, h-off, w-off, off);
		g.setColor(Color.blue);
		g.drawLine(off, (int)(h-off - 0.25*(h-2*off)),  (int)(w-off - 0.25*(w-2*off)), off);
		g.drawLine((int)(off + 0.25*(w-2*off)), h-off, w-off, (int)(off + 0.25*(h-2*off)));
		
		
		int y = h-off/2;
		g.drawString("0.0", (int)(off + 0.0*(w-2*off)), h-off/2);
		g.drawString("0.2", (int)(off + 0.2*(w-2*off)), y);
		g.drawString("0.4", (int)(off + 0.4*(w-2*off)), y);
		g.drawString("0.6", (int)(off + 0.6*(w-2*off)), y);
		g.drawString("0.8", (int)(off + 0.8*(w-2*off)), y);
		g.drawString("1.0",(int)(off +  1.0*(w-2*off)), y);
		
		int x = off/2;
		g.drawString("0.0", x, (int)(off + 1.0*(h-2*off)));
		g.drawString("0.2", x, (int)(off + 0.8*(h-2*off)));
		g.drawString("0.4", x, (int)(off + 0.6*(h-2*off)));
		g.drawString("0.6", x, (int)(off + 0.4*(h-2*off)));
		g.drawString("0.8", x, (int)(off + 0.2*(h-2*off)));
		g.drawString("1.0", x, (int)(off + 0.0*(h-2*off)));

		g.setColor(Color.black);
		g.setFont(new Font("Arial",Font.PLAIN, 16));
		g.drawString(m_dt.m_sFileName, off, h-2);// - g.getFontMetrics().getHeight());

		AffineTransform orig = g.getTransform();
		g.rotate(-Math.PI/2);
		g.drawString(m_dt.m_sFileName2, off-h, g.getFontMetrics().getHeight());
		g.setTransform(orig);

		g.setColor(Color.red);
		g.setComposite(AlphaComposite.SrcOver.derive(0.25f));	
	}
	
	void output(Graphics2D g, double maxHeight,
			double h1, double lo1, double hi1, 
			double h2, double lo2, double hi2,
			double support1, double support2,
			boolean highlight) {
		double x = (off + (w-2*off) * support1);// + Randomizer.nextInt(10) - 5);
		double y = (     h-off - (h-2*off) * support2);// + Randomizer.nextInt(10) - 5);
		double r = 1+(support1 + support2) * 10; 
		g.setColor(Color.red);
		g.setComposite(AlphaComposite.SrcOver.derive(0.25f));
		g.fillOval((int)(x-r/2), (int)(y-r/2), (int) r, (int) r);
		if (highlight) {
			g.setColor(Color.black);
			g.setComposite(AlphaComposite.SrcOver.derive(0.99f));
			BasicStroke stroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
			((Graphics2D) g).setStroke(stroke);
			g.drawOval((int)(x-r/2), (int)(y-r/2), (int) r, (int) r);
		}
		
		if (highlight) {
			g.setColor(Color.black);
		} else {
			g.setColor(Color.blue);
		}
		float alpha = (float)(0.1 + ((support1 + support2)/2.0)*0.9);
		g.setComposite(AlphaComposite.SrcOver.derive(alpha));
		x = w-off - (w-2*off) * h1 / maxHeight;
		y = off + (h-2*off) * h2/ maxHeight;
		r = 3 + Math.max(support1, support2) * 8;
		g.fillOval((int)(x-r/2), (int)(y-r/2), (int) r, (int) r);
		
		
		if ((support1 + support2) > 0.1) {
			g.setComposite(AlphaComposite.SrcOver.derive(alpha * alpha));
			int x1 = (int)(w-off - (w-2*off) * lo1 / maxHeight);
			int y1 = (int)(off + (h-2*off) * h2/ maxHeight);
			int x2 = (int)(w - off - (w-2*off) * hi1 / maxHeight);
			int y2 = y1;
			g.drawLine(x1, y1, x2, y2);
			x1 = (int)(w - off - (w-2*off) * h1 / maxHeight);
			y1 = (int)(off + (h-2*off) * lo2/ maxHeight);
			x2 = x1;
			y2 = (int)(off + (h-2*off) * hi2/ maxHeight);
			g.drawLine(x1, y1, x2, y2);
		}
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (m_dt.treeData2 == null) {
			return;
		}
		
		int x = e.getX();
		int y = e.getY();
		
		// find closest clade
		Map<String,Integer> map = m_dt.m_mirrorCladeToIDMap;
		int closestClade = -1;
		double closestDistance = Integer.MAX_VALUE;
		for (int i = 0; i < m_dt.treeData.m_cladeHeight.size(); i++) {
			double support1 = m_dt.treeData.m_cladeWeight.get(i);
			int [] clade = m_dt.treeData.m_clades.get(i);
			Integer j = map.get(Arrays.toString(clade));
			if (j != null) {
				double support2 = m_dt.treeData2.m_cladeWeight.get(j);
				double x2 = (off + (w-2*off) * support1);// + Randomizer.nextInt(10) - 5);
				double y2 = (     h-off - (h-2*off) * support2);// + Randomizer.nextInt(10) - 5);
				double d = (x-x2) * (x-x2) + (y-y2) * (y-y2);
				if (d < closestDistance) {
					closestDistance = d;
					closestClade = i;
				}
			}
		}
		
		if (closestClade >= 0 && closestDistance < 100) {
			if (m_dt.treeData.getCladeSelection().contains(closestClade)) {
				m_dt.removeCladeFromselection(closestClade, false);
			} else {
				if ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0) {
					m_dt.treeData.getCladeSelection().clear();
					m_dt.treeData2.getCladeSelection().clear();
				}
				m_dt.resetCladeSelection();
				m_dt.addCladeToSelection(closestClade, false);
			}
			m_dt.makeDirty();
		}

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
}
