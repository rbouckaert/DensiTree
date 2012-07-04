package viz.panel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;
import javax.swing.border.TitledBorder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;


public class ExpandablePanel extends JPanel {
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

	
	public class DropDownButton extends JButton implements MouseListener {
		boolean mouseIn = false;
		boolean isOpen = false;
		void setOpen(boolean isOpen) {
			this.isOpen = isOpen;
			repaint();
		}
		String label;

		public DropDownButton(String label) {
			super(label);
			this.label = label;
	        setBorderPainted(false);
	        addMouseListener(this);
		}

		@Override
	    protected void paintComponent(Graphics g)
	    {
			Graphics2D g2 = (Graphics2D) g;
	        if (getModel().isPressed()) {
	            g.setColor(g.getColor());
	            g2.fillRect(3, 3, getWidth() - 6, getHeight() - 6);
	        }
	        g2.setFont(Font.getFont(Font.DIALOG));
	        super.paintComponent(g);
	        //g2.drawString(label, 17, getHeight() - 6);
	        int x = 5, y = 3;
			if (isOpen) {
				int w = DOWN_ICON.getWidth(null);
				int h = DOWN_ICON.getHeight(null);
				g.drawImage(DOWN_ICON, x, y, x + w, y + h, 0, 0, w, h, null);
			} else {
				int w = LEFT_ICON.getWidth(null);
				int h = LEFT_ICON.getHeight(null);
				g.drawImage(LEFT_ICON, x, y, x + w, y+ h, 0, 0, w, h, null);		
			}

	        if (mouseIn)
	            g2.setColor(Color.darkGray);
	        else
	            g2.setColor(new Color(128, 128, 128));

	        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	                RenderingHints.VALUE_ANTIALIAS_ON);
	        g2.setStroke(new BasicStroke(1.2f));
	        g2.draw(new RoundRectangle2D.Double(1, 1, (getWidth() - 3),
	                (getHeight() - 3), 12, 8));
	        g2.setStroke(new BasicStroke(1.5f));
	        g2.drawLine(4, getHeight() - 3, getWidth() - 4, getHeight() - 3);

	        g2.dispose();
	    }

		public void mouseClicked(MouseEvent e) {
	    }

	    public void mouseEntered(MouseEvent e) {
	        mouseIn = true;
	    }

	    public void mouseExited(MouseEvent e) {
	        mouseIn = false;
	    }

	    public void mousePressed(MouseEvent e) {
	    }

	    public void mouseReleased(MouseEvent e) {
	    }
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    
	JPanel m_panel;
	String m_sLabel;
	DropDownButton editButton;
	
	public ExpandablePanel(String sLabel, JPanel panel) {
//		addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseClicked(MouseEvent e) {
//				boolean isVisible = m_panel.isVisible();
//                m_panel.setVisible(!isVisible);
//                editButton.setOpen(isVisible);
//			}
//		});
//		AbstractBorder roundedLineBorder = new AbstractBorder() {
//			@Override
//			public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
//				g.setColor(Color.gray);
//				//x+=2;y+=2;width-=4;height-=4;
//				g.drawRoundRect(x, y, width, height, 14, 14);
//				x += width - 20; y-=6;
//				if (m_panel.isVisible()) {
//					int w = DOWN_ICON.getWidth(null);
//					int h = DOWN_ICON.getHeight(null);
//					g.drawImage(DOWN_ICON, x, y, x + w, y + h, 0, 0, w, h, null);
//				} else {
//					int w = LEFT_ICON.getWidth(null);
//					int h = LEFT_ICON.getHeight(null);
//					g.drawImage(LEFT_ICON, x, y, x + w, y+ h, 0, 0, w, h, null);
//					
//				}
//				Font font = Font.getFont("dialolg");
//				g.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
//				g.setColor(Color.black);
//				//g.drawString(m_sLabel, x+20, y+12);
//			}
//			
//		};
//		
//		TitledBorder roundedTitledBorder = new TitledBorder(roundedLineBorder, sLabel);
//		setBorder(roundedTitledBorder);
		m_sLabel = sLabel;
		//setBorder(new TitledBorder(null, sLabel, TitledBorder.LEADING, TitledBorder.TOP, null, null));
		m_panel = panel;
		panel.setBorder(BorderFactory.createLineBorder(Color.gray));
		
		editButton = new DropDownButton(sLabel);
		Dimension size = new Dimension(150,20);
		editButton.setPreferredSize(size);
		
        editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isVisible = m_panel.isVisible();
                m_panel.setVisible(!isVisible);
                editButton.setOpen(!isVisible);
            }
        });
        
        
        
		setLayout(new GridBagLayout());

		GridBagConstraints gbc_btnLoadLocations = new GridBagConstraints();
		gbc_btnLoadLocations.gridwidth = 1;
		gbc_btnLoadLocations.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnLoadLocations.gridx = 0;
		gbc_btnLoadLocations.gridy = 0;
		add(editButton, gbc_btnLoadLocations);

		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.gridwidth = 1;
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 1;
		add(panel, gbc_panel);
        
        
//        
//////		box.add(editButton);
//		Box box2 = Box.createVerticalBox();
////		box2.add(box);
//		box2.add(editButton);
//		box2.add(panel);
//		add(box2);
		panel.setVisible(false);
	}

}
