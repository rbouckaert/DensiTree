package viz;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import viz.DensiTree.LineColorMode;
import viz.DensiTree.ViewMode;
import viz.graphics.BufferedImageBounded;
import viz.graphics.BufferedImageF;
import viz.graphics.TreeDrawer;

/**
 * Class for drawing the tree set It uses buffer to do the actual tree
 * drawing, then allows inspection of the image through scrolling.
 */
public class TreeSetPanel extends JPanel implements MouseListener, Printable, MouseMotionListener {
	private static final long serialVersionUID = 1L;
	DensiTree m_dt;
	/** number of threads used for drawing **/
	int m_nDrawThreads = 2;
	/** the set of actual threads **/
	Thread[] m_drawThread;

	/** image in memory containing tree set drawing **/
	private BufferedImageF m_image;
	
	private BufferedImage m_selectedImage;

	/** constructor **/
	public TreeSetPanel(DensiTree dt) {
		m_dt = dt;
		addMouseListener(this);
		addMouseMotionListener(this);
		m_drawThread = new Thread[2];
	}

	/** stop drawing thread, if any is running */
	void stopDrawThreads() {
		try {
			for (int i = 0; i < m_nDrawThreads; i++) {
				if (m_drawThread[i] != null) {
					((DrawThread) m_drawThread[i]).m_bStop = true;
				}
			}
			for (int i = 0; i < m_nDrawThreads; i++) {
				if (m_drawThread[i] != null) {
					m_drawThread[i].join();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	} // stopDrawThreads

	/** reset image so that it will be redrawn on the next occasion */
	public void clearImage() {
		m_image = null;
		stopDrawThreads();
	}

	/** return true if any drawing thread is active **/
	boolean isDrawing() {
		for (int i = 0; i < m_nDrawThreads; i++) {
			if (m_drawThread[i] != null) {
				return true;
			}
		}
		return false;
	}

	/** thread for drawing (part of the) tree set **/
	class DrawThread extends Thread {
		public boolean m_bStop = false;
		int m_nFrom = 0;
		int m_nTo = 1;
		int m_nEvery = 1;
		int m_iTreeTopology = -1;
		TreeDrawer m_treeDrawer;

		public DrawThread(String str, int nFrom, int nTo, int nEvery, int iTreeTopology, TreeDrawer treeDrawer) {
			super(str);
			m_treeDrawer = treeDrawer;
			m_nFrom = nFrom;
			m_nTo = nTo;
			m_nEvery = nEvery;
			m_iTreeTopology = iTreeTopology;
		} // c'tor

		public DrawThread(String str, int nFrom, int nTo, int nEvery, TreeDrawer treeDrawer) {
			super(str);
			m_nFrom = nFrom;
			m_nTo = nTo;
			m_nEvery = nEvery;
			m_dt.m_treeDrawer = treeDrawer;
		} // c'tor

		@Override
		public void run() {
			if (m_image == null) {
				return;
			}
			Graphics2D g = m_image.createGraphics();
			try {
				g.setClip(0, 0, m_image.getWidth(), m_image.getHeight());
				m_image.scale(g, m_dt.m_fScale, m_dt.m_fScale);
				float fScaleX = m_dt.m_fScaleX;
				float fScaleY = m_dt.m_fScaleY;
				if (m_dt.m_bUseLogScale) {
					if (m_treeDrawer.m_bRootAtTop) {
						fScaleY *= m_dt.m_fHeight / (float) Math.log(m_dt.m_fHeight + 1.0);
					} else {
						fScaleX *= m_dt.m_fHeight / (float) Math.log(m_dt.m_fHeight + 1.0);
					}
				}

				// draw all individual trees if necessary
				if (m_dt.m_bViewAllTrees && m_nTo >= m_nEvery) {
					int iStart = m_nTo - m_nEvery;
					float fAlpha = Math.min(1.0f, 20.0f / iStart * m_dt.m_fTreeIntensity);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fAlpha));
					Stroke stroke = new BasicStroke(m_dt.m_nTreeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
					g.setStroke(stroke);
					m_dt.m_treeDrawer.setJitter(m_dt.m_nJitter);
					for (int i = iStart; i >= m_nFrom; i -= m_nEvery) {
						if (m_bStop) {
							return;
						}
						if (m_iTreeTopology < 0 || m_iTreeTopology == m_dt.m_nTopologyByPopularity[i]) {
//							switch (m_dt.m_nTopologyByPopularity[i]) {
//							case 0:
//								g.setColor(m_dt.m_color[0]);
//								break;
//							case 1:
//								g.setColor(m_dt.m_color[1]);
//								break;
//							case 2:
//								g.setColor(m_dt.m_color[2]);
//								break;
//							default:
//								g.setColor(m_dt.m_color[3]);
//							}

							m_dt.m_treeDrawer.draw(i, m_dt.m_fLinesX, m_dt.m_fLinesY, m_dt.m_fLineWidth, m_dt.m_fTopLineWidth, m_dt.m_nLineColor, g, fScaleX,
									fScaleY);
							if (i % 100 == 0) {
								System.err.print('.');
								m_dt.m_jStatusBar.setText("Drawing tree " + i);
							}
						}
					}
				}
				// draw consensus trees if necessary
				if (m_dt.m_bViewCTrees) {
					m_dt.m_jStatusBar.setText("Drawing consensus trees");
//					g.setColor(m_dt.m_color[DensiTree.CONSCOLOR]);
					Stroke stroke = new BasicStroke(m_dt.m_nCTreeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
					g.setStroke(stroke);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
					g.setClip(0, 0, getWidth(), getHeight());
					m_dt.m_treeDrawer.setJitter(0);
					for (int i = m_nFrom; i < m_dt.m_nTopologies; i += m_nEvery) {
						if (m_bStop) {
							return;
						}
//						if (m_dt.m_bViewMultiColor) {
//							g.setColor(m_dt.m_color[9 + (i % (m_dt.m_color.length - 9))]);						}
						if (m_iTreeTopology < 0 || m_iTreeTopology == i) {
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
									Math.min(1.0f, 0.5f * m_dt.m_fCTreeIntensity * m_dt.m_fTreeWeight[i])));
							m_dt.m_treeDrawer.draw(i, m_dt.m_fCLinesX, m_dt.m_fCLinesY, m_dt.m_fCLineWidth, m_dt.m_fTopCLineWidth, m_dt.m_nCLineColor, g,
									fScaleX, fScaleY);
							if (i % 100 == 0) {
								System.err.print('x');
								m_dt.m_jStatusBar.setText("Drawing consensus tree " + i);
							}
						}
					}
				}

				if (m_dt.m_viewMode == ViewMode.DRAW) {
					m_drawThread[m_nFrom] = null;
					if (!isDrawing()) {
						if (m_dt.m_bShowRootCanalTopology) {
							drawRootCanalTree(g);
						}
						double fEntropy = calcImageEntropy();
						m_dt.m_jStatusBar.setText("Done Drawing trees ");
						System.out.println("Entropy(x100): " + fEntropy + " Mean cumulative width: " + m_dt.m_w);
					}
					repaint();
				} else {
					DecimalFormat df = new DecimalFormat("##.##");
					double fSum = 0;
					for (int i = 0; i <= m_dt.m_iAnimateTree; i++) {
						fSum += m_dt.m_fTreeWeight[i];
					}

					m_dt.m_jStatusBar.setText("Consensus tree " + (m_dt.m_iAnimateTree + 1) + " out of " + m_dt.m_nTopologies
							+ " covering " + df.format((m_dt.m_fTreeWeight[m_dt.m_iAnimateTree] * 100)) + "% of trees "
							+ df.format(fSum * 100) + "% cumultive trees");
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("DRAWING ERROR -- IGNORED");
			}
			m_drawThread[m_nFrom] = null;
		}
	} // DrawThread


	void drawLabelsSVG(Node node, StringBuffer buf) {
		if (node.isLeaf()) {
			Color color = null;
			if (m_dt.m_bSelection[node.m_iLabel]) {
				color = m_dt.m_color[DensiTree.LABELCOLOR];
			} else {
				color = Color.GRAY;
			}
			int x = 0;
			int y = 0;
			if (m_dt.m_treeDrawer.m_bRootAtTop) {
				x = (int) (node.m_fPosX * m_dt.m_fScaleX /* m_dt.m_fScale */) + 10;
				y = m_dt.getPosY((node.m_fPosY - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
			} else {
				y = (int) (node.m_fPosX * m_dt.m_fScaleY/* m_dt.m_fScale */);
				x = m_dt.getPosX((node.m_fPosY - m_dt.m_fTreeOffset) * m_dt.m_fTreeScale);
			}
			buf.append("<text x='" + x + "' y='" + y + "' " + "font-family='" + m_dt.m_font.getFamily() + "' "
					+ "font-size='" + m_dt.m_font.getSize() + "pt' " + "font-style='"
					+ (m_dt.m_font.isBold() ? "oblique" : "") + (m_dt.m_font.isItalic() ? "italic" : "") + "' "
					+ "stroke='rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")' "
					+ ">" + m_dt.m_sLabels.elementAt(node.m_iLabel) + "</text>\n");
		} else {
			drawLabelsSVG(node.m_left, buf);
			drawLabelsSVG(node.m_right, buf);
		}
	} // m_dt.drawLabelsSVG

//	void toSVG(String sFileName) {
//		try {
//			if (m_dt.m_font == null) {
//				m_dt.m_font = new Font("Monospaced", Font.PLAIN, 10);
//			}
//
//			StringBuffer buf = new StringBuffer();
//			SVGTreeDrawer treeDrawer = new SVGTreeDrawer(buf);
//			treeDrawer.LINE_WIDTH_SCALE = m_dt.m_treeDrawer.LINE_WIDTH_SCALE;
//			treeDrawer.m_bRootAtTop = m_dt.m_treeDrawer.m_bRootAtTop;
//			treeDrawer.m_bViewBlockTree = m_dt.m_treeDrawer.m_bViewBlockTree;
//			if (m_dt.m_treeDrawer.getBranchDrawer() instanceof SteepArcBranchDrawer) {
//				treeDrawer.m_bViewBlockTree = false;
//				JOptionPane.showMessageDialog(this, "Steep arcs not implemented yet for SVG export, using straigh lines instead");
//			}
//			if (m_dt.m_treeDrawer.getBranchDrawer() instanceof ArcBranchDrawer) {
//				treeDrawer.m_branchStyle = 2;
//			}
//			DrawThread thread = new DrawThread("draw thread", 0, m_dt.m_trees.length, 1, treeDrawer);
//			thread.start();
//			drawLabelsSVG(m_dt.m_trees[0], buf);
//			m_dt.m_gridDrawer.drawHeightInfoSVG(buf);
//
//			PrintStream out = new PrintStream(sFileName);
//			out.println("<?xml version='1.0'?>\n" + "<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.1//EN'\n"
//					+ "  'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd'>\n"
//					+ "<svg xmlns='http://www.w3.org/2000/svg' version='1.1'\n" + "      width='" + getWidth()
//					+ "' height='" + getHeight() + "' viewBox='0 0 " + getWidth() + " " + getHeight() + "'>\n"
//					+ "<rect fill='#fff' width='" + getWidth() + "' height='" + getHeight() + "'/>");
//			out.println(buf.toString());
//			out.println("</svg>");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	} // toSVG

	double calcImageEntropy() throws Exception {
		if (m_image == null) {
			return 0;
		}
		Thread.sleep(100);
		int[] nAlpha = new int[256];
		try {
			for (int i = 0; i < m_image.getWidth() - m_dt.m_nLabelWidth; i++) {
				for (int j = 0; j < m_image.getHeight(); j++) {
					int x = m_image.getRGB(i, j);
					int y = ((x & 0xFF) + ((x & 0xFF00) >> 8) + ((x & 0xFF0000) >> 16)) / 3;
					// System.out.print(Integer.toHexString(x)+" " +
					// Integer.toHexString(y) + " ");
					nAlpha[y]++;
				}
				// System.out.println();
			}
		} catch (Exception e) {
			return 0.0;
		}
		double fQ = 0;
		for (int i = 1; i < 255; i++) {
			fQ -= nAlpha[i] * Math.log(i / 255.0);
		}
		return 100.0 * fQ / ((m_image.getWidth() - m_dt.m_nLabelWidth) * m_image.getHeight());
	} // calcImageEntropy

	
	

	/**
	 * Updates the screen contents.
	 * 
	 * @param g
	 *            the drawing surface.
	 */
	@Override
	public void paintComponent(Graphics g) {
		m_dt.a_undo.setEnabled(m_dt.m_doActions.size() > 0 && m_dt.m_iUndo > 1);
		m_dt.a_redo.setEnabled(m_dt.m_iUndo < m_dt.m_doActions.size());
		g.setFont(m_dt.m_font);
		switch (m_dt.m_viewMode) {
		case DRAW:
			drawTreeSet((Graphics2D) g);
			break;
		case ANIMATE:
			drawFrame(g);
			m_dt.m_gridDrawer.paintHeightInfo(g);
			try {
				Thread.sleep(m_dt.m_nAnimationDelay);
			} catch (Exception ex) {
				// ignore
			}
			m_dt.m_iAnimateTree = (m_dt.m_iAnimateTree + 1) % m_dt.m_nTopologies;
			repaint();
			return;
		case BROWSE:
			drawFrame(g);
			m_dt.m_gridDrawer.paintHeightInfo(g);
			m_dt.setDefaultCursor();
			//this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		if (m_dt.m_sOutputFile != null && !isDrawing()) {
			// wait a second for the drawing to be finished
			try {
				ImageIO.write(m_image.m_localImage, "png", new File(m_dt.m_sOutputFile));
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (m_dt.m_bViewEditTree && m_dt.m_Xmode == 0) {
			viewEditTree(g);
		}
		if (m_dt.m_bViewClades && m_dt.m_bCladesReady && (m_dt.m_Xmode == 1 || m_dt.m_Xmode == 2)) {
			m_dt.m_cladeDrawer.viewClades(g);
		}
		if (m_dt.m_showLegend &&
			(m_dt.m_lineColorMode == LineColorMode.BY_METADATA_PATTERN ||
				m_dt.m_lineColorMode == LineColorMode.COLOR_BY_METADATA_TAG)) {
			Font font = new Font(g.getFont().getName(), Font.BOLD, 14);
			g.setFont(font);
//			for (int k = 0; k < m_dt.m_colorMetaDataCategories.size(); k++) {
//				g.setColor(m_dt.m_color[9 + k % (m_dt.m_color.length - 9)]);
//				g.drawString(m_dt.m_colorMetaDataCategories.get(k), 10, k*15+15);
//			}
			int k = 0;
			for (String s : m_dt.m_colorMetaDataCategories.keySet()) {
				g.setColor(m_dt.m_color[9 + m_dt.m_colorMetaDataCategories.get(s) % (m_dt.m_color.length - 9)]);
				g.drawString(s, 10, k*15+15);
				k++;
			}
		}
		if (m_selectedImage != null) {
			int w = m_selectedImage.getWidth();
			int h = m_selectedImage.getHeight();
			g.drawImage(m_selectedImage, 0, 0, w, h, 0, 0, w, h, null);
		}
	}


	RotationPoint[] m_rotationPoints = null;

	/** draw tree that allows editing order **/
	void viewEditTree(Graphics g) {
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
		int x0 = 0;
		int y0 = 0;
		int x1 = 0;
		int y1 = 0;
		g.setColor(Color.BLACK);
		Stroke stroke = new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
		((Graphics2D) g).setStroke(stroke);
		((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

		int h = m_dt.m_rotate.getHeight(null);
		int w = m_dt.m_rotate.getWidth(null);
		boolean bUpdatePoints = false;
		if (m_rotationPoints == null) {
			m_rotationPoints = new RotationPoint[m_dt.m_fRLinesX[0].length / 4];
			bUpdatePoints = true;
		}
		for (int i = 1; i < m_dt.m_fRLinesX[0].length - 2; i += 4) {
			if (m_dt.m_treeDrawer.m_bRootAtTop) {
				x = (int) ((m_dt.m_fRLinesX[0][i] + m_dt.m_fRLinesX[0][i + 1]) * fScaleX / 2.0f);
				y = (int) ((m_dt.m_fCLinesY[0][i] + m_dt.m_fRLinesY[0][i + 1]) * fScaleY / 2.0f);
				x0 = (int) ((m_dt.m_fRLinesX[0][i - 1]) * fScaleX);
				y0 = (int) ((m_dt.m_fRLinesY[0][i - 1]) * fScaleY);
				x1 = (int) ((m_dt.m_fRLinesX[0][i + 2]) * fScaleX);
				y1 = (int) ((m_dt.m_fRLinesY[0][i + 2]) * fScaleY);
			} else {
				x = (int) ((m_dt.m_fRLinesY[0][i] + m_dt.m_fRLinesY[0][i + 1]) * fScaleX / 2.0f);
				y = (int) ((m_dt.m_fRLinesX[0][i] + m_dt.m_fRLinesX[0][i + 1]) * fScaleY / 2.0f);
				x0 = (int) ((m_dt.m_fRLinesY[0][i - 1]) * fScaleX);
				y0 = (int) ((m_dt.m_fRLinesX[0][i - 1]) * fScaleY);
				x1 = (int) ((m_dt.m_fRLinesY[0][i + 2]) * fScaleX);
				y1 = (int) ((m_dt.m_fRLinesX[0][i + 2]) * fScaleY);
			}
			if (bUpdatePoints) {
				m_rotationPoints[i / 4] = new RotationPoint(x, y);
			}
			g.drawLine(x, y, x0, y0);
			g.drawLine(x, y, x1, y1);
			g.drawImage(m_dt.m_rotate, x - w / 2, y - h / 2, x + h / 2, y + w / 2, 0, 0, h, w, null);

		}
	} // viewEditTree


	/** draw complete set of trees **/
	void drawTreeSet(Graphics2D g) {
		Color oldBackground = g.getBackground();
		g.setBackground(m_dt.m_color[DensiTree.BGCOLOR]);
		Rectangle r = g.getClipBounds();
		g.clearRect(r.x, r.y, r.width, r.height);
		g.setBackground(oldBackground);
		g.setClip(r.x, r.y, r.width, r.height);

		if (m_dt.m_trees == null || m_dt.m_fCLinesY == null || m_dt.m_bInitializing) {
			// nothing to see
			return;
		}
		synchronized (this) {
			m_dt.setWaitCursor();
			//this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			if (m_image == null) {
				System.err.println("Setting up new image");
				if (!m_dt.m_bShowBounds) {
					m_image = new BufferedImageF((int) (getWidth() * m_dt.m_fScale), (int) (getHeight() * m_dt.m_fScale));
				} else {
					m_image = new BufferedImageBounded((int) (getWidth() * m_dt.m_fScale),
							(int) (getHeight() * m_dt.m_fScale));
				}
				m_dt.m_treeDrawer.setImage(m_image);
				Graphics2D g2 = m_image.createGraphics();
				m_image.init(g2, m_dt.m_color[DensiTree.BGCOLOR], m_dt.m_bgImage, m_dt.m_fBGImageBox, m_dt.m_nLabelWidth, m_dt.m_fMinLong, m_dt.m_fMaxLong,
						m_dt.m_fMinLat, m_dt.m_fMaxLat);
				//m_dt.drawLabels(m_dt.m_trees[0], g2);
				m_dt.m_gridDrawer.paintHeightInfo(g2);
				//m_dt.drawLabels(m_dt.m_trees[0], g2);
				if (m_image == null) {
					return;
				} else {
					m_image.SyncIntToRGBImage();
				}

				int nDrawThreads = Math.min(m_nDrawThreads, m_dt.m_trees.length);
				for (int i = 0; i < nDrawThreads; i++) {
					m_drawThread[i] = new DrawThread("draw thread", i, m_dt.m_trees.length + i, nDrawThreads,
							m_dt.m_treeDrawer);
					m_drawThread[i].start();
				}
				if (m_dt.m_bShowRootCanalTopology) {
					drawRootCanalTree(g);
				}
			}

		}
		;
		if (m_image == null) {
			return;
		}
		m_image.drawImage(g, this);
		if (m_dt.m_nSelectedRect != null) {
			if (m_dt.m_bViewEditTree && m_dt.m_Xmode == 0) { // || m_dt.m_bViewClades) {
				int h = m_dt.m_rotate.getHeight(null);
				int w = m_dt.m_rotate.getWidth(null);
				int x = m_dt.m_nSelectedRect.x + (m_dt.m_treeDrawer.m_bRootAtTop ? m_dt.m_nSelectedRect.width : 0);
				int y = m_dt.m_nSelectedRect.y + (m_dt.m_treeDrawer.m_bRootAtTop ? 0 : m_dt.m_nSelectedRect.height);
				g.drawImage(m_dt.m_rotate, x - w / 2, y - h / 2, x + h / 2, y + w / 2, 0, 0, h, w, null);
			} else {
				g.drawRect(m_dt.m_nSelectedRect.x + Math.min(m_dt.m_nSelectedRect.width, 0),
						m_dt.m_nSelectedRect.y + Math.min(m_dt.m_nSelectedRect.height, 0),
						(Math.abs(m_dt.m_nSelectedRect.width)), (Math.abs(m_dt.m_nSelectedRect.height)));
			}
		}
		// need this here so that the screen is updated when selection of
		// taxa changes
		m_dt.drawLabels(m_dt.m_trees[0], g);
		if (m_dt.m_bDrawGeo && m_dt.m_fLatitude.size() > 0) {
			g.setColor(m_dt.m_color[DensiTree.GEOCOLOR]);
			Stroke stroke = new BasicStroke(m_dt.m_nGeoWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
			g.setStroke(stroke);
			m_dt.drawGeo(m_dt.m_cTrees[0], g);
		}
		// ((Graphics2D) g).scale(m_dt.m_fScale, m_dt.m_fScale);
		if (isDrawing()) {
			try {
				Thread.sleep(m_dt.m_nAnimationDelay);
			} catch (Exception ex) {
				// ignore
			}
			repaint();
			if (m_dt.m_bRecord) {
				try {
					System.err.println(" writing /tmp/frame" + m_dt.m_nFrameNr + ".jpg " + isDrawing());
					ImageIO.write(m_image.m_localImage, "jpg", new File("/tmp/frame" + m_dt.m_nFrameNr + ".jpg"));
					m_dt.m_nFrameNr++;
				} catch (Exception ex) {
					// ignore
				}
			}
		} else {
			m_dt.setDefaultCursor();
			//this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			if (m_dt.m_bRecord) {
				try {
					System.err.println(" writing /tmp/frame" + m_dt.m_nFrameNr + ".jpg " + isDrawing());
					ImageIO.write(m_image.m_localImage, "jpg", new File("/tmp/frame" + m_dt.m_nFrameNr + ".jpg"));
					m_dt.m_nFrameNr++;
				} catch (Exception ex) {
					// ignore
				}
			}
			m_dt.m_bRecord = false;
		}
	} // drawTreeSet
	
	
	void drawRootCanalTree(Graphics2D g) {
		float fScaleX = m_dt.m_fScaleX;
		float fScaleY = m_dt.m_fScaleY;
		if (m_dt.m_bUseLogScale) {
			if (m_dt.m_treeDrawer.m_bRootAtTop) {
				fScaleY *= m_dt.m_fHeight / (float) Math.log(m_dt.m_fHeight + 1.0);
			} else {
				fScaleX *= m_dt.m_fHeight / (float) Math.log(m_dt.m_fHeight + 1.0);
			}
		}
	
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		Stroke stroke = new BasicStroke(m_dt.m_nCTreeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
		g.setStroke(stroke);
		g.setColor(m_dt.m_color[DensiTree.ROOTCANALCOLOR]);
		m_dt.m_treeDrawer.draw(0, m_dt.m_fRLinesX, m_dt.m_fRLinesY, m_dt.m_fRLineWidth,
				m_dt.m_fRTopLineWidth, m_dt.m_nRLineColor, g, fScaleX, fScaleY);
	}

	/** draw new frame in animation or browse action **/
	void drawFrame(Graphics g) {
		Color oldBackground = ((Graphics2D) g).getBackground();
		((Graphics2D) g).setBackground(m_dt.m_color[DensiTree.BGCOLOR]);
		Rectangle r = g.getClipBounds();
		g.clearRect(r.x, r.y, r.width, r.height);
		((Graphics2D) g).setBackground(oldBackground);
		g.setClip(r.x, r.y, r.width, r.height);

		if (m_dt.m_trees == null || m_dt.m_fCLinesY == null || m_dt.m_bInitializing) {
			// nothing to see
			return;
		}
		m_dt.setWaitCursor();
		//this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		if (m_image == null || m_dt.m_bAnimateOverwrite || m_dt.m_iAnimateTree == 0) { // ||
																				// m_viewMode
																				// ==
																				// ViewMode.BROWSE)
																				// {
			if (!m_dt.m_bShowBounds) {
				m_image = new BufferedImageF((int) (getWidth() * m_dt.m_fScale), (int) (getHeight() * m_dt.m_fScale));
			} else {
				m_image = new BufferedImageBounded((int) (getWidth() * m_dt.m_fScale), (int) (getHeight() * m_dt.m_fScale));
			}
			m_dt.m_treeDrawer.setImage(m_image);
			Graphics2D g2 = m_image.createGraphics();
			// g2.setBackground(m_dt.m_color[DensiTree.BGCOLOR]);
			// g2.clearRect(0, 0, m_image.getWidth(), m_image.getHeight());
			m_image.init(g2, m_dt.m_color[DensiTree.BGCOLOR], m_dt.m_bgImage, m_dt.m_fBGImageBox, m_dt.m_nLabelWidth, m_dt.m_fMinLong, m_dt.m_fMaxLong,
				m_dt.m_fMinLat, m_dt.m_fMaxLat);
			// drawBGImage(g2);
			// m_image.drawImage(g2 , this);
			if (m_dt.m_bDrawGeo && m_dt.m_fLatitude.size() > 0) {
				g2.setColor(m_dt.m_color[DensiTree.GEOCOLOR]);
				Stroke stroke = new BasicStroke(m_dt.m_nGeoWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
				g2.setStroke(stroke);
				m_dt.drawGeo(m_dt.m_cTrees[0], g2);
			}
			m_dt.m_gridDrawer.paintHeightInfo(g2);
			m_dt.drawLabels(m_dt.m_trees[0], g2);
			m_image.SyncIntToRGBImage();
		}

		for (int i = 0; i < m_nDrawThreads; i++) {
			m_drawThread[i] = new DrawThread("draw thread", i, m_dt.m_trees.length + i, m_nDrawThreads, m_dt.m_iAnimateTree,
					m_dt.m_treeDrawer);
			m_drawThread[i].start();
		}

		while (isDrawing()) {
			try {
				Thread.sleep(m_dt.m_nAnimationDelay);
			} catch (Exception ex) {
				// ignore
			}
			m_image.drawImage(g, this);
			System.err.print("X");
		}
		m_image.drawImage(g, this);
		m_dt.setDefaultCursor();
		//this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	} // animate

	/**
	 * implementation of Printable, used for printing
	 * 
	 * @see Printable
	 */
	@Override
	public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
		if (pageIndex > 0) {
			return (NO_SUCH_PAGE);
		} else {
			Graphics2D g2d = (Graphics2D) g;
			g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
			float fHeight = (float) pageFormat.getImageableHeight();
			float fWidth = (float) pageFormat.getImageableWidth();

			float fScaleX = m_dt.m_fScaleX;
			m_dt.m_fScaleX = fWidth / m_dt.m_sLabels.size();
			float fScaleY = m_dt.m_fScaleY;
			m_dt.m_fScaleY = (fHeight - 10) / m_dt.m_fHeight;

			// Turn off float buffering
			paint(g2d);
			m_dt.m_fScaleX = fScaleX;
			m_dt.m_fScaleY = fScaleY;
			// Turn float buffering back on
			return (PAGE_EXISTS);
		}
	} // print

	@Override
	public void mouseClicked(MouseEvent e) {
//		if (m_dt.m_bViewEditTree && m_dt.m_Xmode == 0) {
//			if (m_rotationPoints != null) {
//				if (e.getButton() == MouseEvent.BUTTON1) {
//					for (int i = 0; i < m_rotationPoints.length; i++) {
//						if (m_rotationPoints[i].intersects(e.getX(), e.getY())) {
//							rotateAround(i);
//							// repaint();
//							return;
//						}
//					}
//				}
//			}
//		} else {
//			Rectangle r = new Rectangle(e.getPoint(), new Dimension(1, 1));
//			if (e.getButton() == MouseEvent.BUTTON1) {
//				if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
//					toggleSelection(r);
//				} else if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
//					addToSelection(r);
//				} else {
//					clearSelection();
//					addToSelection(r);
//				}
//				repaint();
//			}
//		}
	}

	/** remove all labels from selection **/
	void clearSelection() {
		for (int i = 0; i < m_dt.m_bSelection.length; i++) {
			if (m_dt.m_bSelection[i]) {
				m_dt.m_bSelection[i] = false;
				m_dt.m_bSelectionChanged = true;
			}
		}
		m_dt.m_cladeSelection.clear();
		m_dt.resetCladeSelection();			
	}

	/**
	 * remove labels overlapping rectangle r currently in selection and
	 * replace by ones not selected but overlapping with r
	 **/
	void toggleSelection(Rectangle r) {
		float f = m_dt.m_fScale;
		m_dt.m_fScale = 1;
		r.x = (int) (r.x / m_dt.m_fScale);
		r.y = (int) (r.y / m_dt.m_fScale);
		r.width = 1 + (int) (r.width / m_dt.m_fScale);
		r.height = 1 + (int) (r.height / m_dt.m_fScale);
		for (int i = 0; i < m_dt.m_bSelection.length; i++) {
			if (m_dt.m_bLabelRectangle[i].intersects(r)) {
				m_dt.m_bSelection[i] = !m_dt.m_bSelection[i];
				m_dt.m_bSelectionChanged = true;
			}
		}
		if (m_rotationPoints != null) {
			r.x += 5;
			r.y += 5;
			Rectangle rotationPoint = new Rectangle(10, 10);
			for (int i = 0; i < m_rotationPoints.length; i++) {
				rotationPoint.x = m_rotationPoints[i].m_nX;
				rotationPoint.y = m_rotationPoints[i].m_nY;
				if (r.intersects(rotationPoint)) { 
					if (m_dt.m_cladeSelection.contains(i)) {
						m_dt.m_cladeSelection.remove(i);
					} else {
						m_dt.m_cladeSelection.add(i);
					}
				}
			}
			m_dt.resetCladeSelection();
		}
		m_dt.m_fScale = f;
	}

	/** add labels overlapping rectangle r to selection **/
	void addToSelection(Rectangle r) {
		float f = m_dt.m_fScale;
		m_dt.m_fScale = 1;
		r.x = (int) (r.x / m_dt.m_fScale);
		r.y = (int) (r.y / m_dt.m_fScale);
		r.width = 1 + (int) (r.width / m_dt.m_fScale);
		r.height = 1 + (int) (r.height / m_dt.m_fScale);
		for (int i = 0; i < m_dt.m_bSelection.length; i++) {
			if (m_dt.m_bLabelRectangle[i].intersects(r)
					|| (m_dt.m_bGeoRectangle[i] != null && m_dt.m_bGeoRectangle[i].intersects(r))) {
				if (!m_dt.m_bSelection[i]) {
					m_dt.m_bSelection[i] = true;
					m_dt.m_bSelectionChanged = true;
				}
			}
		}
		if (m_rotationPoints != null) {
			r.x += 5;
			r.y += 5;
			Rectangle rotationPoint = new Rectangle(10, 10);
			for (int i = 0; i < m_rotationPoints.length; i++) {
				rotationPoint.x = m_rotationPoints[i].m_nX;
				rotationPoint.y = m_rotationPoints[i].m_nY;
				if (r.intersects(rotationPoint)) { 
					m_dt.m_cladeSelection.add(i);
				}
			}
			
			System.err.println(Arrays.toString(m_dt.m_cladeSelection.toArray(new Integer[0])));
			m_dt.resetCladeSelection();
		}
		m_dt.m_fScale = f;
		
		System.out.print("selected: ");
		for (int i = 0; i < m_dt.m_bSelection.length; i++) {
			if (m_dt.m_bSelection[i]) {
				System.out.println(m_dt.m_sLabels.get(i) + " ");
			}
		}
		System.out.println();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	boolean m_bIsMoving = false;
	boolean m_bIsDragging = false;
	@Override
	public void mousePressed(MouseEvent e) {
		m_bIsDragging = false;
		m_dt.m_nSelectedRect = new Rectangle(e.getPoint(), new Dimension(1, 1));
		if (m_dt.m_bViewEditTree && m_dt.m_Xmode == 0) { // && e.getButton() != MouseEvent.BUTTON1) {
			if (m_rotationPoints != null) {
				for (int i = 0; i < m_rotationPoints.length; i++) {
					if (m_rotationPoints[i].intersects(e.getPoint().x, e.getPoint().y)) {
						m_bIsMoving = true;
						m_dt.m_nSelectedRect = new Rectangle(e.getPoint(), new Dimension(1, 1));
						return;
					}
				}
				m_dt.m_nSelectedRect = null;
				repaint();
			}
		} else if (m_dt.m_bViewClades && (m_dt.m_Xmode == 1 || m_dt.m_Xmode == 2)) { // && e.getButton() != MouseEvent.BUTTON1) {
//			m_dt.m_nSelectedRect = new Rectangle(e.getPoint(), new Dimension(1, 1));
			if (m_rotationPoints != null) {
				for (int i = 0/* m_dt.m_sLabels.size() */; i < m_rotationPoints.length; i++) {
					if (m_rotationPoints[i].intersects(e.getPoint().x, e.getPoint().y) && 
							m_dt.m_cladeSelection.contains(i)) {
						m_bIsMoving = true;
						return;
					}
				}
				//m_dt.m_nSelectedRect = null;
			}
			repaint();
//		} else {
//			if (!m_dt.m_bViewEditTree && e.getButton() == MouseEvent.BUTTON1) {
//				m_dt.m_nSelectedRect = new Rectangle(e.getPoint(), new Dimension(1, 1));
//			}
		}
	}

	/** update selection when mouse is released **/
	@Override
	public void mouseReleased(MouseEvent e) {
		if (m_dt.m_bViewEditTree && m_dt.m_Xmode == 0 && e.getButton() == MouseEvent.BUTTON1 && !m_bIsDragging) {
			if (m_rotationPoints != null) {
				for (int i = 0; i < m_rotationPoints.length; i++) {
					if (m_rotationPoints[i].intersects(e.getX(), e.getY())) {
						m_dt.rotateAround(i);
						m_dt.m_nSelectedRect = null;
						// repaint();
						return;
					}
				}
			}
		} else 	if (m_dt.m_nSelectedRect != null) {
			//if (e.getButton() == MouseEvent.BUTTON1) {
			if (!m_bIsMoving) {
				// normalize rectangle
				if (m_dt.m_nSelectedRect.width < 0) {
					m_dt.m_nSelectedRect.x += m_dt.m_nSelectedRect.width;
					m_dt.m_nSelectedRect.width = -m_dt.m_nSelectedRect.width;
				}
				if (m_dt.m_nSelectedRect.height < 0) {
					m_dt.m_nSelectedRect.y += m_dt.m_nSelectedRect.height;
					m_dt.m_nSelectedRect.height = -m_dt.m_nSelectedRect.height;
				}
				if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
					toggleSelection(m_dt.m_nSelectedRect);
				} else if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
					addToSelection(m_dt.m_nSelectedRect);
				} else {
					clearSelection();
					addToSelection(m_dt.m_nSelectedRect);
				}
				m_dt.m_nSelectedRect = null;
				repaint();
			} else { // right click
				m_bIsMoving = false;
				m_bIsDragging = false;
				if (m_rotationPoints != null) {
					if (m_dt.m_bViewEditTree && m_dt.m_Xmode == 0) {
						for (int i = 0; i < m_rotationPoints.length; i++) {
							if (m_rotationPoints[i].intersects(m_dt.m_nSelectedRect.x, m_dt.m_nSelectedRect.y)) {
								m_dt.moveRotationPoint(i, m_dt.m_sLabels.size()
										* (m_dt.m_treeDrawer.m_bRootAtTop ? (float) m_dt.m_nSelectedRect.width / getWidth()
												: (float) m_dt.m_nSelectedRect.height / getHeight()));
								m_dt.m_nSelectedRect = null;
								repaint();
								return;
							}
						}
					} else if (m_dt.m_bViewClades && (m_dt.m_Xmode == 1 || m_dt.m_Xmode == 2)) {
						double dF = m_dt.m_sLabels.size() * (m_dt.m_treeDrawer.m_bRootAtTop ? (float) m_dt.m_nSelectedRect.width / getWidth()
								: (float) m_dt.m_nSelectedRect.height / getHeight());
						//for (int i = 0/* m_dt.m_sLabels.size() */; i < m_rotationPoints.length; i++) {
//							if (m_rotationPoints[i].intersects(m_dt.m_nSelectedRect.x, m_dt.m_nSelectedRect.y)) {
//							}
						for (int i : m_dt.m_cladeSelection) {
							m_dt.m_cladePosition[i] += dF;
						}
						m_dt.calcLines();
						m_dt.makeDirty();
						m_dt.m_nSelectedRect = null;
						repaint();
						return;
					}
					m_dt.m_nSelectedRect = null;
					repaint();
				}
			}
		}
	}

	/** update selection rectangle when mouse is dragged **/
	@Override
	public void mouseDragged(MouseEvent e) {
		if (m_dt.m_nSelectedRect != null) {
			m_bIsDragging = true;
			m_dt.m_nSelectedRect.width = e.getPoint().x - m_dt.m_nSelectedRect.x;
			m_dt.m_nSelectedRect.height = e.getPoint().y - m_dt.m_nSelectedRect.y;
			repaint();
			return;
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (m_dt.m_bDrawGeo) {
			for (int i = 0; i < m_dt.m_bSelection.length; i++) {
				if (m_dt.m_bGeoRectangle[i].contains(e.getPoint())) {
					m_dt.m_jStatusBar.setText(m_dt.m_sLabels.elementAt(i));
				}
			}
		}
		Point p = e.getPoint();
		boolean found = false;
		if (m_dt.m_bLabelRectangle != null) {
			for (int i = 0; i < m_dt.m_bLabelRectangle.length; i++) {
				if (m_dt.m_bLabelRectangle[i].contains(p)) {
					m_dt.m_jStatusBar.setText(m_dt.m_sLabels.elementAt(i) + " ");
					if (m_dt.m_LabelImages != null) {
						int w = 0, h = 0;
						BufferedImage old = m_selectedImage; 
						if (m_selectedImage != null) {
							w = old.getWidth();
							h = old.getHeight();
						}
						m_selectedImage = m_dt.m_LabelImages[i];
						if (m_selectedImage != null) {
							w = Math.max(w, m_selectedImage.getWidth());
							h = Math.max(h, m_selectedImage.getHeight());
						}
						if (old != m_selectedImage) {
							repaint(0, 0, w, h);
						}
					} else {
						if (m_selectedImage != null) {
							int w = m_selectedImage.getWidth();
							int h = m_selectedImage.getHeight();
							m_selectedImage = null;
							repaint(0, 0, w, h);
						}
					}
					found = true;
				}
			}
			if (!found) {
				m_dt.m_jStatusBar.setText("");
			}
		}
		String sText = m_dt.m_jStatusBar.getText();
		sText = sText.split("\t")[0];
		float fHeight = m_dt.screenPosToHeight(e.getX(), e.getY());
		if (!Float.isNaN(fHeight)) {
			sText += "\theight=" + fHeight;
			m_dt.m_jStatusBar.setText(sText);
		}
		
	}

} // class TreeVizPanel
