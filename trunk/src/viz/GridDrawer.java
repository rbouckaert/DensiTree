package viz;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

public class GridDrawer {

	public Font m_gridfont = Font.getFont(Font.MONOSPACED);
	public enum GridMode {NONE, SHORT, FULL};
	public GridMode m_nGridMode = GridMode.NONE;
	public float m_fGridOffset = 0;
	public boolean m_bReverseGrid = false;
	public boolean m_bAutoGrid = true;
	public float m_fGridTicks = 1;
	public float m_fGridOrigin = 0;
	public int m_nGridDigits = 2;
	
	DensiTree m_dt;

	void drawHeightInfoSVG(StringBuffer buf) {
		if (m_nGridMode != GridMode.NONE && m_dt.m_fHeight > 0) {
			DecimalFormat formatter = new DecimalFormat("##.##");
			if (m_dt.m_treeDrawer.m_bRootAtTop) {
				int nW = m_dt.getWidth();
				if (m_nGridMode == GridMode.SHORT) {
					nW = 10;
				}
				buf.append("<path " + "fill='none' " + "stroke='rgb(" + m_dt.m_color[DensiTree.HEIGHTCOLOR].getRed() + ","
						+ m_dt.m_color[DensiTree.HEIGHTCOLOR].getGreen() + "," + m_dt.m_color[DensiTree.HEIGHTCOLOR].getBlue() + ")' "
						+ "stroke-width='" + 1 + "' " + " d='");
				if (m_bAutoGrid) {
					float fHeight = (float) adjust(m_dt.m_fHeight);
					for (int i = 0; i <= m_nTicks; i++) {
						int y = m_dt.getPosY((m_dt.m_fHeight - fHeight * i / m_nTicks - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						buf.append("M" + 0 + " " + y + "L" + nW + " " + y);
					}
					
					buf.append("'/>\n");

					for (int i = 0; i <= m_nTicks; i++) {
						int y = m_dt.getPosY((m_dt.m_fHeight - fHeight * i / m_nTicks - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						String sStr = (m_bReverseGrid ?   
								formatter.format(m_fGridOffset + fHeight - fHeight * (i) / m_nTicks) :
								formatter.format(m_fGridOffset + fHeight * (i) / m_nTicks)
								);
						buf.append("<text x='"
								+ m_gridfont.getSize()
								+ "' y='"
								+ y
								+ "' font-family='" + m_gridfont.getFamily() + "' "
								+ "font-size='" + m_gridfont.getSize() + "pt' " + "font-style='"
								+ (m_gridfont.isBold() ? "oblique" : "") + (m_gridfont.isItalic() ? "italic" : "") + "' "
								+
								"stroke='rgb(" + m_dt.m_color[DensiTree.HEIGHTCOLOR].getRed() + "," + m_dt.m_color[DensiTree.HEIGHTCOLOR].getGreen()
								+ "," + m_dt.m_color[DensiTree.HEIGHTCOLOR].getBlue() + ")' " + ">" + sStr + "</text>\n");				
					}
				} else {
					float fHeight = m_fGridOrigin;
					while (fHeight < m_dt.m_fHeight) {
						int y = m_dt.getPosY((m_dt.m_fHeight - fHeight - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						buf.append("M" + 0 + " " + y + "L" + nW + " " + y);
						fHeight += Math.abs(m_fGridTicks);
					}
					buf.append("'/>\n");
					
					fHeight = m_fGridOrigin;
					while (fHeight < m_dt.m_fHeight) {

						String sStr = (m_bReverseGrid ?  
								formatter.format(m_fGridOffset + m_dt.m_fHeight - fHeight) :
								formatter.format(m_fGridOffset + fHeight)
								);
						int y = m_dt.getPosY((m_dt.m_fHeight - fHeight - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						buf.append("<text x='"
								+ m_gridfont.getSize()
								+ "' y='"
								+ y
								+ "' font-family='" + m_gridfont.getFamily() + "' "
								+ "font-size='" + m_gridfont.getSize() + "pt' " + "font-style='"
								+ (m_gridfont.isBold() ? "oblique" : "") + (m_gridfont.isItalic() ? "italic" : "") + "' "
								+
								"stroke='rgb(" + m_dt.m_color[DensiTree.HEIGHTCOLOR].getRed() + "," + m_dt.m_color[DensiTree.HEIGHTCOLOR].getGreen()
								+ "," + m_dt.m_color[DensiTree.HEIGHTCOLOR].getBlue() + ")' " + ">" + sStr + "</text>\n");				
						fHeight += Math.abs(m_fGridTicks);
					}
				}
			} else {
				int nH = m_dt.getHeight();
				if (m_nGridMode == GridMode.SHORT) {
					nH = 10;
				}
				buf.append("<path " + "fill='none' " + "stroke='rgb(" + m_dt.m_color[DensiTree.HEIGHTCOLOR].getRed() + ","
						+ m_dt.m_color[DensiTree.HEIGHTCOLOR].getGreen() + "," + m_dt.m_color[DensiTree.HEIGHTCOLOR].getBlue() + ")' "
						+ "stroke-width='" + 1 + "' " + " d='");
				if (m_bAutoGrid) {
					float fHeight = (float) adjust(m_dt.m_fHeight);
					
					for (int i = 0; i <= m_nTicks; i++) {
						int x = m_dt.getPosX((m_dt.m_fHeight - fHeight * i / m_nTicks - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						buf.append("M" + x + " " + 0 + "L" + x + " " + nH);
					}
					buf.append("'/>\n");
				
					for (int i = 0; i <= m_nTicks; i++) {
						int x = m_dt.getPosX((m_dt.m_fHeight - fHeight * i / m_nTicks - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						String sStr = (m_bReverseGrid ?   
								formatter.format(m_fGridOffset + fHeight - fHeight * (i) / m_nTicks) :
								formatter.format(m_fGridOffset + fHeight * (i) / m_nTicks)
								);
						buf.append("<text x='"
								+ x
								+ "' y='"
								+ m_gridfont.getSize()
								+ "' font-family='" + m_gridfont.getFamily() + "' "
								+ "font-size='" + m_gridfont.getSize() + "pt' " + "font-style='"
								+ (m_gridfont.isBold() ? "oblique" : "") + (m_gridfont.isItalic() ? "italic" : "") + "' "
								+
								"stroke='rgb(" + m_dt.m_color[DensiTree.HEIGHTCOLOR].getRed() + "," + m_dt.m_color[DensiTree.HEIGHTCOLOR].getGreen()
								+ "," + m_dt.m_color[DensiTree.HEIGHTCOLOR].getBlue() + ")' " + ">" + sStr + "</text>\n");				
					}
				} else {
					float fHeight = m_fGridOrigin;
					while (fHeight < m_dt.m_fHeight) {
						int x = m_dt.getPosX((m_dt.m_fHeight - fHeight - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						buf.append("M" + x + " " + 0 + "L" + x + " " + nH);
						fHeight += Math.abs(m_fGridTicks);
					}
					buf.append("'/>\n");

					fHeight = m_fGridOrigin;
					while (fHeight < m_dt.m_fHeight) {
						String sStr = (m_bReverseGrid ?   
								formatter.format(m_fGridOffset + m_dt.m_fHeight - fHeight) :
								formatter.format(m_fGridOffset + fHeight)
								);
						int x = m_dt.getPosX((m_dt.m_fHeight - fHeight - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						buf.append("<text x='"
								+ x
								+ "' y='"
								+ m_gridfont.getSize()
								+ "' font-family='" + m_gridfont.getFamily() + "' "
								+ "font-size='" + m_gridfont.getSize() + "pt' " + "font-style='"
								+ (m_gridfont.isBold() ? "oblique" : "") + (m_gridfont.isItalic() ? "italic" : "") + "' "
								+
								"stroke='rgb(" + m_dt.m_color[DensiTree.HEIGHTCOLOR].getRed() + "," + m_dt.m_color[DensiTree.HEIGHTCOLOR].getGreen()
								+ "," + m_dt.m_color[DensiTree.HEIGHTCOLOR].getBlue() + ")' " + ">" + sStr + "</text>\n");				

						fHeight += Math.abs(m_fGridTicks);
					}
				}
			}
		}
	} // drawHeightInfoSVG


	/** draw height bar and/or height grid if desired **/
	void paintHeightInfo(Graphics g) {
		if (m_nGridMode != GridMode.NONE && m_dt.m_fHeight > 0) {
			String format = "##.";
			for (int i = 0; i < m_nGridDigits; i++) {
				format += "#";
			}
			DecimalFormat formatter = new DecimalFormat(format);
			
			((Graphics2D) g).setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			if (m_gridfont == null) {
				m_gridfont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
			}
			g.setFont(m_gridfont);
			if (m_dt.m_treeDrawer.m_bRootAtTop) {
				int nW = m_dt.getWidth();
				if (m_nGridMode == GridMode.SHORT) {
					nW = 10;
				}
				g.setColor(m_dt.m_color[DensiTree.HEIGHTCOLOR]);

				if (m_bAutoGrid) {
					float fHeight = (float) adjust(m_dt.m_fHeight);
					
					for (int i = 0; i <= m_nTicks; i++) {
						String sStr = (m_bReverseGrid ?   
								formatter.format(m_fGridOffset + fHeight - fHeight * i / m_nTicks) :
								formatter.format(m_fGridOffset + fHeight * i / m_nTicks)
								);
						int y = m_dt.getPosY((m_dt.m_fHeight - fHeight * i / m_nTicks - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						g.drawString(sStr, 0, y - 2);
						g.drawLine(0, y, nW, y);
					}
				} else {
					float fHeight = m_fGridOrigin;
					while (fHeight < m_dt.m_fHeight) {
						String sStr = (m_bReverseGrid ?  
								formatter.format(m_fGridOffset + m_dt.m_fHeight - fHeight) :
								formatter.format(m_fGridOffset + fHeight)
								);
						int y = m_dt.getPosY((m_dt.m_fHeight - fHeight - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						g.drawString(sStr, 0, y - 2);
						g.drawLine(0, y, nW, y);
						fHeight += Math.abs(m_fGridTicks);
					}
				}
			} else {
				int nH = m_dt.getHeight();
				if (m_nGridMode == GridMode.SHORT) {
					nH = 10;
				}
				g.setColor(m_dt.m_color[DensiTree.HEIGHTCOLOR]);
				
				
				if (m_bAutoGrid) {
					float fHeight = (float) adjust(m_dt.m_fHeight);
					
					for (int i = 0; i <= m_nTicks; i++) {
						String sStr = (m_bReverseGrid ?   
								formatter.format(m_fGridOffset + fHeight - fHeight * i / m_nTicks) :
								formatter.format(m_fGridOffset + fHeight * i / m_nTicks)
								);
						int x = m_dt.getPosX((m_dt.m_fHeight - fHeight * i / m_nTicks - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						g.drawString(sStr, x+2, m_gridfont.getSize());
						g.drawLine(x, 0, x, nH);
					}
				} else {
					float fHeight = m_fGridOrigin;
					while (fHeight < m_dt.m_fHeight) {
						String sStr = (m_bReverseGrid ?   
								formatter.format(m_fGridOffset + m_dt.m_fHeight - fHeight) :
								formatter.format(m_fGridOffset + fHeight)
								);
						int x = m_dt.getPosX((m_dt.m_fHeight - fHeight - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
						g.drawString(sStr, x+2, m_gridfont.getSize());
						g.drawLine(x, 0, x, nH);
						fHeight += Math.abs(m_fGridTicks);
					}
				}
			}
		}
	} // paintHeightInfo

	/** maps most significant digit to nr of ticks on graph **/ 
	final int [] NR_OF_TICKS = new int [] {5,10,8,6,8,10,6,7,8,9, 10};
	int m_nTicks = 10;
	
	private double adjust(double fYMax) {
		// adjust fYMax so that the ticks come out right
		int k = 0;
		double fY = fYMax;
		while (fY > 10) {
			fY /= 10;
			k++;
		}
		while (fY < 1 && fY > 0) {
			fY *= 10;
			k--;
		}
		fY = Math.ceil(fY);
		m_nTicks = NR_OF_TICKS[(int) fY];
		m_nTicks *= (int) m_dt.m_fTreeScale;
		for (int i = 0; i < k; i++) {
			fY *= 10;
		}
		for (int i = k; i < 0; i++) {
			fY /= 10;
		}
		return fY;
	}	

}
