package geo;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;


public class ConvertWorldMap {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BufferedImage mask = ImageIO.read(new File("/tmp/image5.bmp"));

		BufferedImage img = ImageIO.read(new File("/home/remco/data/map/World.bmp"));
		int [] maskArray  = new int[10800*5400];
		mask.getRGB(0, 0, 10800, 5400, maskArray, 0, 10800);
		int [] rgbArray  = new int[10800*5400];
		img.getRGB(0, 0, 10800, 5400, rgbArray, 0, 10800);
		
		int k = 0;
		for (int i = 0; i < 10800;i++) {
			for (int j = 0; j < 5400; j++) {
				if ((maskArray[k] & 0xffffff) == 0xff) {
					rgbArray[k] = 0x85a5ab;
				}
				k++;
			}
		}
		img.setRGB(0, 0, 10800, 5400, rgbArray, 0, 10800);
		ImageIO.write(img, "bmp", new File("/tmp/world.bmp"));

	}

}
