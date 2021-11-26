package viz;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import javax.swing.JPanel;

public class CladeSetComparisonPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	DensiTree m_dt;
	
	CladeSetComparisonPanel(DensiTree dt) {
		m_dt = dt;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		
		g.setColor(Color.white);
		g.clearRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.blue);
		if (m_dt.treeData2 == null) {
			g.drawString(" Can only draw comparison", 0, getHeight()/2 - 15);
			g.drawString(" when mirror set is loaded", 0, getHeight()/2);
			g.drawString(" using File/Load mirror menu", 0, getHeight()/2 + 15);
			return;
		}
		Graphics2D g2 = (Graphics2D) g;
		
		initPNG(g2);
	}

	
	private int w; // width of  panel including labels
	private int h; // height of panel including labels
	private int off; // offset space for labels

	private void initPNG(Graphics2D g) {
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
		g.setFont(new Font("Arial",Font.PLAIN, 10));
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
			double support1, double support2) {
		double x = (off + (w-2*off) * support1);// + Randomizer.nextInt(10) - 5);
		double y = (     h-off - (h-2*off) * support2);// + Randomizer.nextInt(10) - 5);
		double r = 1+(support1 + support2) * 10; 
		g.setColor(Color.red);
		g.setComposite(AlphaComposite.SrcOver.derive(0.25f));
		g.fillOval((int)(x-r/2), (int)(y-r/2), (int) r, (int) r);
		
		g.setColor(Color.blue);
		float alpha = (float)(0.1 + ((support1 + support2)/2.0)*0.9);
		g.setComposite(AlphaComposite.SrcOver.derive(alpha));
		x = off + (w-2*off) * h1 / maxHeight;
		y = h-off - (h-2*off) * h2/ maxHeight;
		r = 3 + Math.max(support1, support2) * 13;
		g.fillOval((int)(x-r/2), (int)(y-r/2), (int) r, (int) r);
		
		
		if ((support1 + support2) > 0.1) {
			g.setComposite(AlphaComposite.SrcOver.derive(alpha * alpha));
			int x1 = (int)(off + (w-2*off) * lo1 / maxHeight);
			int y1 = (int)(h-off - (h-2*off) * h2/ maxHeight);
			int x2 = (int)(off + (w-2*off) * hi1 / maxHeight);
			int y2 = (int)(h-off - (h-2*off) * h2/ maxHeight);
			g.drawLine(x1, y1, x2, y2);
			x1 = (int)(off + (w-2*off) * h1 / maxHeight);
			y1 = (int)(h-off - (h-2*off) * lo2/ maxHeight);
			x2 = (int)(off + (w-2*off) * h1 / maxHeight);
			y2 = (int)(h-off - (h-2*off) * hi2/ maxHeight);
			g.drawLine(x1, y1, x2, y2);
		}
		
	}

}
