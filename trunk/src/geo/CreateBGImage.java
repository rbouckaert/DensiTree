package geo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class CreateBGImage {

	
	int width = 1024;
	int height = 768;
	double viewPortOffsetX = 0;
	double viewPortOffsetY = 0;
	double[] m_fBGImageBox = { -180, -90, 180, 90 };
	
	String svgFile = "/home/remco/data/map/TM_WORLD_BORDERS-0.3.svg";
	List<String> m_sLabels;
	List<Integer> m_nLabels;
	List<List<Double>> m_contours;
	int svgHeight, svgWidth;
	
	float linewidth = 1f;
	
	public BufferedImage bgImage;
	String bgFile = null;


	private void parseArgs(String[] args) {
		int i = 0;
		try {
			while (i < args.length) {
				int iOld = i;
				if (i < args.length - 1) {
					if (args[i].equals("")) {
						i += 1;
					} else if (args[i].equals("-geo")) {
						try {
							Pattern pattern = Pattern
									.compile(".*\\(([0-9\\.Ee-]+),([0-9\\.Ee-]+)\\)x\\(([0-9\\.Ee-]+),([0-9\\.Ee-]+)\\).*");
							Matcher matcher = pattern.matcher(args[i+1]);
							matcher.find();
							double x1 = Float.parseFloat(matcher.group(1));
							double x2 = Float.parseFloat(matcher.group(2));
							double x3 = Float.parseFloat(matcher.group(3));
							double x4 = Float.parseFloat(matcher.group(4));
							m_fBGImageBox[1] = Math.min(x1,x3);
							m_fBGImageBox[0] = Math.min(x2,x4);
							m_fBGImageBox[3] = Math.max(x1,x3);
							m_fBGImageBox[2] = Math.max(x2,x4);
						} catch (Exception e) {
							System.err.println("Could not parse geo argument, expected (lat1,long1)x(lat2,long2), using default (-90,-180)x(90,180)");
							final double[] fBGImageBox = { -180, -90, 180, 90 };
							m_fBGImageBox = fBGImageBox;
						}
						i += 2;
					} else if (args[i].equals("-svg")) {
						svgFile = args[i+1];
						i += 2;
					} else if (args[i].equals("-bgimage")) {
						bgFile = args[i+1];
						i += 2;
					} else if (args[i].equals("-w")) {
						width = Integer.parseInt(args[i+1]);
						i += 2;
					} else if (args[i].equals("-h")) {
						height = Integer.parseInt(args[i+1]);
						i += 2;
					} else if (args[i].equals("-lw")) {
						linewidth = Float.parseFloat(args[i+1]);
						i += 2;
					} else if (args[i].equals("-help")) {
						printUsageAndExit();
					}
					if (i == iOld) {
						throw new Exception("Wrong argument");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error parsing command line arguments: " + Arrays.toString(args)
					+ "\nArguments ignored\n\n");
		}
	}

	private void createImage() throws IOException {
		parseSVGFile();
		drawMap();
		saveFile();
	}


	private void parseSVGFile() {
		try {
			System.out.println("Reading " + svgFile);
			m_sLabels = new ArrayList<String>();
			m_nLabels = new ArrayList<Integer>();
			m_contours = new ArrayList<List<Double>>();
		    long nLength = new File(svgFile).length();
		    MappedByteBuffer in = new FileInputStream(svgFile).getChannel().map(FileChannel.MapMode.READ_ONLY, 0, nLength);
		    int i = 0;
		    while (i < nLength) {
		    	char c = (char) in.get(i++);
		    	if (c == '<') {
		    		c = (char) in.get(i++);
		    		if (c == 'g') {
			    		// we are in a <g> element
			    		do {
			    			c = (char) in.get(i++);
			    		} while (c != 'i' && c !='>');
			    		if (c == 'i' && in.get(i++) == 'd' && in.get(i++) == '=') {
			    			char delim = (char) in.get(i++);
			    			String sID = "";
			    			c = (char) in.get(i++);
			    			while (c != delim) {
			    				sID += c;
			    				c = (char) in.get(i++);
			    			}
				        	m_sLabels.add(sID);
							m_nLabels.add(m_contours.size());
			    		}
		    		} else if (c == 'p' && in.get(i++) == 'a' && in.get(i++) == 't' && in.get(i++) == 'h') {
			    		// we are in a <path> element
		    			char prev = ' ';
			    		do {
			    			prev = c;
			    			c = (char) in.get(i++);
			    		} while ((c != 'd' && c !='>') || (c == 'd' && prev == 'i'));
			    		boolean relativeMoves = false;
			    		if (c == 'd' && in.get(i++) == '=') {
			    			List<Double> contour = new ArrayList<Double>(32);
			    			char delim = (char) in.get(i++);
			    			c = (char) in.get(i++);
			    			StringBuffer buf = new StringBuffer();
			    			while (c != delim) {
			    				if (c == 'm' || c == 'l') relativeMoves = true;
			    				//if (c == 'L') relativeMoves = false;
			    				if (c == ' ' || c == 'L' || c == 'l' || c == 'z' || c == 'M' || c == 'm' || c == ',') {
			    					if (buf.length() > 0) {
			    						double f = Double.parseDouble(buf.toString());
			    						contour.add(f);
			    						if (relativeMoves && contour.size() > 2) {
			    							int k = contour.size() - 1;
			    							contour.set(k, contour.get(k - 2) + f);
			    						}
			    						buf.delete(0, buf.length());
			    					}
			    				} else {
			    					buf.append(c);
			    				}
			    				c = (char) in.get(i++);
			    			}
			    			if (contour.size() > 1) {
			    				m_contours.add(contour);
			    			}
			    		}
		    		} else if (c == 's' && in.get(i++) == 'v' && in.get(i++) == 'g') {
		    			// we are in the svg element
		    			String s = "";
		    			while ((c = (char) in.get(i++)) != '>') {
		    				s += c;
		    			}
		    			int k = s.indexOf("width=");
		    			String s2 = s.substring(k + 7);
		    			k = s2.indexOf("\""); if ( k<0) k = s2.indexOf("'");
		    			s2 = s2.substring(0, k);
		    			svgWidth = Integer.parseInt(s2);
		    			k = s.indexOf("height=");
		    			s2 = s.substring(k + 8);
		    			k = s2.indexOf("\""); if ( k<0) k = s2.indexOf("'");
		    			s2 = s2.substring(0, k);
		    			svgHeight = Integer.parseInt(s2);

		    			k = s.indexOf("viewBox=");
		    			if (k > 0) {
			    			s2 = s.substring(k + 9);
			    			k = s2.indexOf("\""); if ( k<0) k = s2.indexOf("'");
			    			s2 = s2.substring(0, k);
			    			String [] strs = s2.split(" ");
			    			viewPortOffsetX = Double.parseDouble(strs[0]);
			    			viewPortOffsetY = Double.parseDouble(strs[1]);
		    			}
}
		    	}
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void drawMap() throws IOException {
		bgImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) bgImage.getGraphics();
		g.setColor(Color.white);
		// mirror over x-axis
		g.setTransform(new AffineTransform(1.0, 0.0, 0.0, -1.0, 0.0, height));
		g.fillRect(0, 0, width, height);
		if (bgFile != null) {
			try {
				BufferedImage bg = ImageIO.read(new File(bgFile));
				int w = bg.getWidth();
				int h = bg.getHeight();
				int sx1 = (int)(w * (m_fBGImageBox[0] + 180) / 360.0);
				int sy1 = h - (int)(h * (m_fBGImageBox[1] + 90) / 180.0);
				int sx2 = (int)(w * (m_fBGImageBox[2] + 180) / 360.0);
				int sy2 = h - (int)(h * (m_fBGImageBox[3] + 90) / 180.0);
				g.drawImage(bg, 0, 0, width, height, sx1, sy1, sx2, sy2, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		g.setStroke(new BasicStroke(linewidth));
		int [] xPoints;
		int [] yPoints;
		
		double fScaleX = width  * 360.0/(m_fBGImageBox[2] - m_fBGImageBox[0]) / svgWidth;
		double fScaleY = height * 180.0/(m_fBGImageBox[3] - m_fBGImageBox[1]) / svgHeight;
		double fOffsetX = svgWidth * (m_fBGImageBox[0] + 180) / 360.0;
		double fOffsetY = svgHeight* (m_fBGImageBox[1] + 90) / 180.0;
		
		for (int iContour = 0; iContour < m_contours.size(); iContour++) {
			List<Double> contour = m_contours.get(iContour);
			int nPoints = contour.size() / 2;
//			if (nPoints >= xPoints.length) {
				xPoints = new int[nPoints];
				yPoints = new int[nPoints];
//			}
	//		double [] fPoint = new double[2];
			
			for (int j = 0; j < contour.size(); j += 2) {
				double fLong = contour.get(j);
				double fLat = contour.get(j+1);
				xPoints[j / 2] = (int) (fScaleX * (fLong - viewPortOffsetX - fOffsetX));
				yPoints[j / 2] = (int) (fScaleY * (svgHeight + viewPortOffsetY - fLat - fOffsetY));
			}
			g.setColor(Color.black);
			g.drawPolygon(xPoints, yPoints, nPoints);
		}		
	}
	
	private void saveFile() {
		
		DecimalFormat f = new DecimalFormat("###.##");
		String fileName = "bg(" + f.format(m_fBGImageBox[1]) + "," + 
				f.format(m_fBGImageBox[0]) +  ")x(" + 
				f.format(m_fBGImageBox[3]) + "," + 
				f.format(m_fBGImageBox[2]) + ").png";
		
		try {
			File file = new File(fileName);
			System.out.println("Writing file " + file.getPath());
			ImageIO.write(bgImage, "png", file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public static void printUsageAndExit() {
		System.out.println("Create background image for DensiTree from svg map file");
		System.out.println("-svg <file>: svg map file");
		System.out.println("-geo (lat1,long1)x(lat2,long2): target area of map");
		System.out.println("-w <int>: width in number of pixels in image");
		System.out.println("-h <int>: height in number of pixels in image");
		System.out.println("-lw <float>: line width");
		System.out.println("-bgimage <file>: bitmap file used as background");
		System.out.println("-help: show this message");
		System.exit(0);
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Processing");
		CreateBGImage bgImage = new CreateBGImage();
		bgImage.parseArgs(args);
		bgImage.createImage();
		System.out.println("Done!");
	}
}
