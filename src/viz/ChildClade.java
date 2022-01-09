package viz;

/** Each clade has a list of pairs of child clades **/
class ChildClade {
	int m_iLeft;
	int m_iRight;
	double m_fWeight;

	@Override
	public String toString() {
		return "(" + m_iLeft + "," + m_iRight + ")" + m_fWeight + " ";
	}
}