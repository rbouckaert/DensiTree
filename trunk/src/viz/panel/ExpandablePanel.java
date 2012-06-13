package viz.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;
import javax.swing.border.TitledBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class ExpandablePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    static Image  DOWN_ICON;
    static Image  LEFT_ICON;

    {
        try {
            java.net.URL downURL = ClassLoader.getSystemResource("viz/icons/down.png");
            DOWN_ICON = ImageIO.read(downURL);
            java.net.URL leftURL = ClassLoader.getSystemResource("viz/icons/left.png");
            LEFT_ICON = ImageIO.read(leftURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
	JPanel m_panel;
	String m_sLabel;

	public ExpandablePanel(String sLabel, JPanel panel) {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
                m_panel.setVisible(!m_panel.isVisible());
			}
		});
		AbstractBorder roundedLineBorder = new AbstractBorder() {
			@Override
			public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
				g.setColor(Color.gray);
				//x+=2;y+=2;width-=4;height-=4;
				g.drawRoundRect(x, y, width, height, 14, 14);
				x += width - 20; y-=6;
				if (m_panel.isVisible()) {
					int w = DOWN_ICON.getWidth(null);
					int h = DOWN_ICON.getHeight(null);
					g.drawImage(DOWN_ICON, x, y, x + w, y + h, 0, 0, w, h, null);
				} else {
					int w = LEFT_ICON.getWidth(null);
					int h = LEFT_ICON.getHeight(null);
					g.drawImage(LEFT_ICON, x, y, x + w, y+ h, 0, 0, w, h, null);
					
				}
				Font font = Font.getFont("dialolg");
				g.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
				g.setColor(Color.black);
				//g.drawString(m_sLabel, x+20, y+12);
			}
			
		};
		
		TitledBorder roundedTitledBorder = new TitledBorder(roundedLineBorder, sLabel);
		m_sLabel = sLabel;
		setBorder(roundedTitledBorder);
		//setBorder(new TitledBorder(null, sLabel, TitledBorder.LEADING, TitledBorder.TOP, null, null));
		m_panel = panel;
//		Box box = Box.createHorizontalBox();
//		box.add(Box.createHorizontalGlue());
		
//		JButton editButton = new JButton(sLabel);
//		Dimension size = new Dimension(200,20);
//		editButton.setPreferredSize(size);
//		
//        editButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//                JButton editButton = (JButton) e.getSource();
//                m_panel.setVisible(!m_panel.isVisible());
////                if (m_panel.isVisible()) {
////                    try {
////                    	editButton.setIcon(DOWN_ICON);
////                    }catch (Exception e2) {
////						// TODO: handle exception
////					}
////                } else {
////                	try {
////                		editButton.setIcon(LEFT_ICON);
////                    }catch (Exception e2) {
////						// TODO: handle exception
////					}
////                }
//            }
//        });
////		box.add(editButton);
		Box box2 = Box.createVerticalBox();
//		box2.add(box);
//		box2.add(editButton);
		box2.add(panel);
		add(box2);
		panel.setVisible(false);
	}

}
