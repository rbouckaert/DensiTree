package viz;

/** Each clade has a list of pairs of child clades **/
public class ChildClade {
	public int m_iLeft;
	public int m_iRight;
	public double m_fWeight;

	@Override
	public String toString() {
		return "(" + m_iLeft + "," + m_iRight + ")" + m_fWeight + " ";
	}
}