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
	
	final static double [] bounds = {0.0,	0.0,	
			0.0,	0.010408163265306124,	
			0.0,	0.030816326530612247,	
			0.0,	0.05122448979591837,	
			0.0,	0.06142857142857144,	
			0.0,	0.08183673469387756,	
			2.0408163265306194E-4,	0.09204081632653062,	
			2.0408163265306194E-4,	0.10224489795918368,	
			0.010408163265306124,	0.12265306122448981,	
			0.020612244897959188,	0.13285714285714287,	
			0.030816326530612247,	0.1430612244897959,	
			0.030816326530612247,	0.15326530612244899,	
			0.041020408163265305,	0.1736734693877551,	
			0.05122448979591837,	0.18387755102040815,	
			0.06142857142857144,	0.19408163265306122,	
			0.06142857142857144,	0.2042857142857143,	
			0.0716326530612245,	0.21448979591836734,	
			0.08183673469387756,	0.23489795918367348,	
			0.09204081632653062,	0.24510204081632653,	
			0.10224489795918368,	0.2553061224489796,	
			0.10224489795918368,	0.2655102040816326,	
			0.11244897959183675,	0.27571428571428575,	
			0.12265306122448981,	0.28591836734693876,	
			0.13285714285714287,	0.29612244897959183,	
			0.1430612244897959,	0.316530612244898,	
			0.15326530612244899,	0.32673469387755105,	
			0.16346938775510203,	0.33693877551020407,	
			0.1736734693877551,	0.34714285714285714,	
			0.1736734693877551,	0.3573469387755102,	
			0.18387755102040815,	0.36755102040816323,	
			0.19408163265306122,	0.3777551020408163,	
			0.2042857142857143,	0.38795918367346943,	
			0.21448979591836734,	0.39816326530612245,	
			0.2246938775510204,	0.4083673469387755,	
			0.23489795918367348,	0.4185714285714286,	
			0.24510204081632653,	0.4287755102040816,	
			0.2553061224489796,	0.4491836734693878,	
			0.2655102040816326,	0.45938775510204083,	
			0.27571428571428575,	0.4695918367346939,	
			0.28591836734693876,	0.479795918367347,	
			0.29612244897959183,	0.49,	
			0.29612244897959183,	0.5002040816326531,	
			0.3063265306122449,	0.5104081632653061,	
			0.316530612244898,	0.5206122448979592,	
			0.32673469387755105,	0.5308163265306123,	
			0.33693877551020407,	0.5410204081632652,	
			0.34714285714285714,	0.5512244897959183,	
			0.3573469387755102,	0.5614285714285715,	
			0.36755102040816323,	0.5716326530612245,	
			0.3777551020408163,	0.5818367346938775,	
			0.38795918367346943,	0.5920408163265306,	
			0.39816326530612245,	0.6022448979591837,	
			0.4083673469387755,	0.6124489795918368,	
			0.4185714285714286,	0.6226530612244898,	
			0.4287755102040816,	0.6328571428571429,	
			0.4389795918367347,	0.643061224489796,	
			0.4491836734693878,	0.6532653061224489,	
			0.45938775510204083,	0.6634693877551021,	
			0.4695918367346939,	0.6736734693877551,	
			0.479795918367347,	0.6838775510204081,	
			0.49,	0.6838775510204081,	
			0.5002040816326531,	0.6940816326530613,	
			0.5104081632653061,	0.7042857142857143,	
			0.5206122448979592,	0.7144897959183674,	
			0.5308163265306123,	0.7246938775510204,	
			0.5512244897959183,	0.7348979591836735,	
			0.5614285714285715,	0.7451020408163265,	
			0.5716326530612245,	0.7553061224489797,	
			0.5818367346938775,	0.7655102040816326,	
			0.5920408163265306,	0.7757142857142857,	
			0.6022448979591837,	0.7859183673469389,	
			0.6124489795918368,	0.7961224489795918,	
			0.6226530612244898,	0.8063265306122449,	
			0.6328571428571429,	0.8063265306122449,	
			0.643061224489796,	0.816530612244898,	
			0.6532653061224489,	0.826734693877551,	
			0.6634693877551021,	0.836938775510204,	
			0.6838775510204081,	0.8471428571428572,	
			0.6940816326530613,	0.8573469387755103,	
			0.7042857142857143,	0.8675510204081632,	
			0.7144897959183674,	0.8777551020408164,	
			0.7246938775510204,	0.8777551020408164,	
			0.7348979591836735,	0.8879591836734694,	
			0.7451020408163265,	0.8981632653061224,	
			0.7655102040816326,	0.9083673469387756,	
			0.7757142857142857,	0.9185714285714286,	
			0.7859183673469389,	0.9185714285714286,	
			0.7961224489795918,	0.9287755102040817,	
			0.8063265306122449,	0.9389795918367347,	
			0.826734693877551,	0.9491836734693878,	
			0.836938775510204,	0.9491836734693878,	
			0.8471428571428572,	0.9593877551020408,	
			0.8573469387755103,	0.969591836734694,	
			0.8777551020408164,	0.979795918367347,	
			0.8879591836734694,	0.979795918367347,	
			0.8981632653061224,	0.99,	
			0.9185714285714286,	0.99,	
			0.9287755102040817,	1.0,	
			0.9491836734693878,	1.0,	
			0.969591836734694,	1.0,	
			1.0,	1.0
	};
	
	private int [] boundsPolygonX = new int[bounds.length/2];
	private int [] boundsPolygonY = new int[bounds.length/2];
	
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
		
		
		g.setColor(Color.blue);
		int j = 0;
		for (int i = 0; i < boundsPolygonX.length; i++) {
			boundsPolygonX[i] = (int)(off + (w-2*off) * bounds[j++]); 
			boundsPolygonY[i] = (int)(h-off - (h-2*off) * bounds[j++]); 
		}
		g.drawPolygon(boundsPolygonX, boundsPolygonY, boundsPolygonX.length);
		j = 0;
		for (int i = 0; i < boundsPolygonX.length; i++) {
			boundsPolygonY[i] = (int)(h-off - (h-2*off) * bounds[j++]); 
			boundsPolygonX[i] = (int)(off + (w-2*off) * bounds[j++]); 
		}
		g.drawPolygon(boundsPolygonX, boundsPolygonY, boundsPolygonX.length);
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
