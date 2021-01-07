package viz.process.spq;

import java.util.BitSet;

import viz.Node;

public class Util {
	/**
	 * @param potentialSubset
	 *            BitSet to be checked of being subset
	 * @param potentialSuperset
	 *            BitSet to be checked of being superset
	 * @return whether potentialSubset is subset of potentialSuperset
	 */
	public static boolean isBitSetSubsetOf(BitSet potentialSubset, BitSet potentialSuperset) {
		if (potentialSubset.cardinality() > potentialSuperset.cardinality()) {
			return false;
		} else {
			BitSet clone = (BitSet) potentialSubset.clone();
			clone.and(potentialSuperset);
			return (potentialSubset.cardinality() == clone.cardinality());
		}
	}

	public static int findTaxaPosition(String[] taxaOrder, String taxaToFind) {
		for (int i = 0; i < taxaOrder.length; i++) {
			if (taxaOrder[i] == taxaToFind) {
				return i;
			}
		}

		throw new AssertionError("Taxa of node not found.");
	}
	

	public static int getLeafNodeCount(Node node) {
		if (node.isLeaf()) {
			return 1;
		} else {
			return getLeafNodeCount(node.m_left) + getLeafNodeCount(node.m_right);
		}
	}

	public static int getNodeCount(Node node) {
		if (node.isLeaf()) {
			return 1;
		} else {
			return 1 + getNodeCount(node.m_left) + getNodeCount(node.m_right);
		}
	}

	public static Node[] getNodesAsArray(Node node, int nodeCount) {
		Node [] nodes = new Node[nodeCount];
		collectNodes(node, nodes);
		return nodes;
	}

	private static void collectNodes(Node node, Node[] nodes) {
		nodes[node.m_iLabel] = node;
		if (!node.isLeaf()) {
			collectNodes(node.m_left, nodes);
			collectNodes(node.m_right, nodes);
		}		
	}
	
	
}


