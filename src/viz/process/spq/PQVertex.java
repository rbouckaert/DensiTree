package viz.process.spq;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public class PQVertex {

	protected int ID = -1;

	protected String taxon;

	protected VertexType vertexType;

	protected boolean root;

	protected PQVertex parent;

	private List<PQVertex> children = new ArrayList<PQVertex>();
	
	// mean height of the clade -- used in postprocessing
	private double sumHeight = 0;
	private int count = 0;

	/*
	 * Bit array to store at position i whether leaf i is descendant of this
	 * node.
	 */
	protected BitSet clade;

	public PQVertex(VertexType vertexType) {
		this.vertexType = vertexType;
	}

	public PQVertex(VertexType vertexType, final int id) {
		this(vertexType);
		this.ID = id;
	}

	public PQVertex cloneDownwards(PQVertex parent, int numberOfLeaves) {
		PQVertex clone = new PQVertex(this.getVertexType(), this.getID());
		if (parent == null) {
			clone.setAsRoot();
		} else {
			clone.setParent(parent);
		}

		clone.children = new ArrayList<PQVertex>(this.getChildCount());
		for (PQVertex child : this.getChildren()) {
			PQVertex childClone = child.cloneDownwards(clone, numberOfLeaves);
			clone.addChild(childClone);
		}

		if (this.taxon != null) {
			clone.setTaxon(this.taxon);
		}
		clone.initClade(numberOfLeaves);

		return clone;
	}

	// # properties
	public int getID() {
		return ID;
	}

	public VertexType getVertexType() {
		return vertexType;
	}

	public void setVertexType(VertexType vertexType) {
		this.vertexType = vertexType;
	}

	public boolean isLeaf() {
		return (vertexType == VertexType.L);
	}

	public String getTaxon() {
		if (isLeaf()) {
			return taxon;
		} else {
			return "";
		}
	}

	public void setTaxon(String taxon) {
		this.taxon = taxon;
	}

	public boolean isBinary() {
		return isLeaf() || (getChildCount() == 2);
	}

	public boolean isRoot() {
		return root;
	}

	public void setAsRoot() {
		this.setAsRoot(true);
	}

	public void setAsRoot(boolean root) {
		this.root = root;
	}

	// # parent
	public PQVertex getParent() {
		return parent;
	}

	public void setParent(PQVertex parent) {
		if (parent == null) {
			throw new IllegalArgumentException("Set parent vertex is null for " + this.toString());
		}
		this.parent = parent;
		setAsRoot(false);
	}

	// # children
	public int getDegreeOut() {
		return this.getChildCount();
	}

	public int getChildCount() {
		if (isLeaf()) {
			return 0;
		} else {
			return children.size();
		}
	}

	public List<PQVertex> getChildren() {
		return children;
	}

	public PQVertex getChild(int index) {
		return children.get(index);
	}

	public boolean removeChild(PQVertex childToRemove) {
		return children.remove(childToRemove);
	}

	public boolean addChild(PQVertex childToAdd) {
		return children.add(childToAdd);
	}

	public void addChildAt(PQVertex childToAdd, int index) {
		children.add(index, childToAdd);
	}

	public void replaceChild(PQVertex oldChild, PQVertex newChild) {
		int vertexIndex = this.getChildren().indexOf(oldChild);
		this.getChildren().set(vertexIndex, newChild);
	}

	public void reverseChildren() {
		if (this.vertexType == VertexType.Q) {
			Collections.reverse(children);

			for (PQVertex child : children) {
				child.reverseChildren();
			}
		}
	}

	// # clade and descendants
	public BitSet getClade() {
		return clade;
	}

	public void initClade(int numberOfLeaves) {
		clade = new BitSet(numberOfLeaves);
		if (isLeaf()) {
			clade.set(ID);
		} else {
			for (PQVertex child : children) {
				clade.or(child.clade);
			}
		}
	}

	public void setClade(BitSet clade) {
		this.clade = clade;
	}

	/**
	 * Returns whether leaf with given ID is a descendant of this vertex.
	 * 
	 * @param nodeID
	 *            ID of a leaf to check
	 * @return whether leaf with given ID is a descendant of this vertex
	 */
	public boolean hasLeafDescendant(int nodeID) {
		return this.clade.get(nodeID);
	}

	/**
	 * @param cladeToCheck
	 * @return whether given clade equals clade of this vertex
	 */
	public boolean hasClade(BitSet cladeToCheck) {
		boolean b1 = cladeToCheck.equals(clade);
//		BitSet clone = (BitSet) this.clade.clone();
//		clone.xor(cladeToCheck);
//		boolean b2 = clone.isEmpty();
//		if (b1 != b2) {
//			return b2;
//		}
		return b1;
	}

	/**
	 * @param cladeToCheck
	 * @return whether given clade is superset of own clade
	 */
	public boolean hasSubsetCladeOf(BitSet cladeToCheck) {
		return Util.isBitSetSubsetOf(this.clade, cladeToCheck);
	}

	/**
	 * @param cladeToCheck
	 * @return whether given clade is subset of clade of this vertex
	 */
	public boolean hasSupersetCladeOf(BitSet cladeToCheck) {
		return Util.isBitSetSubsetOf(cladeToCheck, this.clade);
	}

	/**
	 * @param cladeToCheck
	 * @return whether given clade intersects with own clade
	 */
	public boolean hasIntersectingCladeWith(BitSet cladeToCheck) {
		return this.clade.intersects(cladeToCheck);
	}

	// # sequences represented by vertex
	/**
	 * @return one possible sequence represented by this vertex
	 */
	public String getPossibleSequence() {
		switch (vertexType) {
		case L:
			return getID() + "";
		case Q:
		case P:
			String string = "";
			for (PQVertex child : children) {
				string += child.getPossibleSequence() + " ";
			}
			return string.trim();
		default:
			return "ERROR";
		}
	}

	/**
	 * [ ] for Q-nodes (can be mirrored), ( ) for P-nodes (can be flipped)
	 * 
	 * @return all possible sequences represented by this vertex
	 */
	public String getPossibleSequences() {
		String string;
		switch (vertexType) {
		case L:
			return getID() + "";
		case Q:
			string = "[";
			for (PQVertex child : children) {
				string += child.getPossibleSequences() + " ";
			}
			return string.trim() + "]";
		case P:
			string = "(";
			for (PQVertex child : children) {
				string += child.getPossibleSequences() + " ";
			}
			return string.trim() + ")";
		default:
			return "ERROR";
		}
	}

	public void getPossibleOrder(int[] order, int[] k) {
		switch (vertexType) {
		case L:
			order[k[0]++] = getID();
			break;
		case Q:
		case P:
			for (PQVertex child : this.getChildren()) {
				child.getPossibleOrder(order, k);
			}
			break;
		default:
			throw new AssertionError("Unkonwn vertex type.");
		}
	}

	
	public double getHeight() {
		if (count == 0) {
			return 1;
		}
		return sumHeight/count;
	}

	public void addHeight(double height) {		
		this.sumHeight += height;
		count++;
	}

	public double getLength() {
		if (parent != null) {
			return parent.getHeight() - getHeight();
		}
		return 0.0;
	}
	
	
	@Override
	public String toString() {
		String str = "";
		if (this.isLeaf()) {
			str += this.ID + ", ";
		}
//		return this.vertexType + "(" + str + "Clade " + clade.toString() + ", degree "
//				+ this.getChildCount() + ", height " + getHeight() + ")";
		return this.vertexType + "(" + str + ", degree "
		+ this.getChildCount() + ", height " + getHeight() + ")";
	}

	public void printTree(String intend) {
		System.out.println(intend + this.toString());
		for (PQVertex child : this.getChildren()) {
			child.printTree(intend + "  ");
		}
	}

	public enum VertexType {
		P, Q, L;
	}

}
