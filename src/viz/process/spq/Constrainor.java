package viz.process.spq;

import viz.Node;

public abstract class Constrainor {

	protected PQTree tree;
	protected Node[] trees;
	protected String[] taxa;
	protected int numberOfLeaves;

	public PQTree getResultingPQTree() {
		return tree;
	}

	public String getPossibleSequences() {
		return tree.getPossibleSequences();
	}

	public String getPossibleSequence() {
		return tree.getPossibleSequence();
	}

	public int[] getPossibleOrder() {
		return tree.getPossibleOrder();
	}

}
