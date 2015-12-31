package viz.panel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;


public class RoundedButton extends JButton implements MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	boolean mouseIn = false;

	public RoundedButton(String label) {
		super(label);
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
        super.paintComponent(g);

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

        //g2.dispose();
    }

	@Override
	public void mouseClicked(MouseEvent e) {
    }

    @Override
	public void mouseEntered(MouseEvent e) {
        mouseIn = true;
    }

    @Override
	public void mouseExited(MouseEvent e) {
        mouseIn = false;
    }

    @Override
	public void mousePressed(MouseEvent e) {
    }

    @Override
	public void mouseReleased(MouseEvent e) {
    }
}
