package viz;

/**
 * Represents point over which the tree can be 'rotated' when editing
 * the tree.
 */
public class RotationPoint {
	// location of the point on screen
	int m_nX, m_nY;

	public RotationPoint(int nX, int nY) {
		m_nX = nX;
		m_nY = nY;
	}

	boolean intersects(int nX, int nY) {
		return (Math.abs(nX - m_nX) < 5 && Math.abs(nY - m_nY) < 5);
	}
	
	public String toString() {
		return "(" + m_nX + "," + m_nY + ")";
	}
}; // class RotationPoint
