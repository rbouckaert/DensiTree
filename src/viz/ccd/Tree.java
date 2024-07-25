package viz.ccd;

import java.util.ArrayList;
import java.util.List;

import viz.Node;
import viz.TreeData;

public class Tree {
	TreeData td;
	Node root;
	
	public Tree(Node root) {
		this(root, null);
	}

	public Tree(Node root, TreeData td) {
		this.root = root;
		this.td = td;
	}
	
	
	public int getLeafNodeCount() {
		return td.m_dt.m_settings.m_nNrOfLabels;
	}
	
	public int getNodeCount() {
		int n = td.m_dt.m_settings.m_nNrOfLabels;
		return n * 2 -1;
	}
	
	public Node getRoot() {
		return root;
	}
	
	
	public String getID(int nr) {
		return td.m_dt.m_settings.m_sLabels.get(nr);
	}
	
	public Node getNode(int nr) {
		return getNode(root, nr);
	}

	private Node getNode(Node node, int nr) {
		if (node.isLeaf()) {
			return node.getNr() == nr ? node: null;
		} else {
			Node result = getNode(node.m_left, nr);
			if (result != null) {
				return result;
			}
			if (node.m_right != null) {
				result = getNode(node.m_right, nr);
				if (result != null) {
					return result;
				}
			}
			return null;
		}
	}

	
	public List<Node> getNodesAsArray() {
		List<Node> nodes = new ArrayList<>();
		collectNodes(root, nodes);
		return nodes;
	}

	private void collectNodes(Node node, List<Node> nodes) {
		nodes.add(node);
    	if (node.m_left != null) {
    		collectNodes(node.m_left, nodes);
    	}
    	if (node.m_right != null) {
    		collectNodes(node.m_right, nodes);
    	}

	}
	
}
