package viz.process.spq;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import viz.Node;
import viz.process.spq.PQVertex.VertexType;

public class PQTree {

	private final int numberOfLeaves;

	/**
	 * All vertices of tree; convention that leaves first.
	 */
	private List<PQVertex> vertices;
	private PQVertex root;

	// # constructor and creators
	public PQTree(final int numberOfLeaves) {
		this.numberOfLeaves = numberOfLeaves;
		this.vertices = new ArrayList<PQVertex>(2 * numberOfLeaves);
	}

	public static PQTree createStarTree(final int numberOfLeaves) {
		PQTree tree = new PQTree(numberOfLeaves);
		PQVertex root = new PQVertex(VertexType.P);

		for (int i = 0; i < numberOfLeaves; i++) {
			PQVertex leaf = new PQVertex(VertexType.L, i);
			leaf.setParent(root);
			root.addChild(leaf);
			leaf.initClade(numberOfLeaves);
			// leaf.initDescendants(tree.getMaxNumberOfVertices());
			tree.vertices.add(leaf);
		}

		root.initClade(numberOfLeaves);
		// root.initDescendants(tree.getMaxNumberOfVertices());
		root.setAsRoot();
		tree.root = root;
		tree.vertices.add(root);

		return tree;
	}

	public static PQTree createPQFTreeFromBeastTree(Node beastTree, String[] taxaOrder) {
		// extract information from beast tree
		int numberOfLeaves = Util.getLeafNodeCount(beastTree);
		int numberOfVertices = Util.getNodeCount(beastTree);
		Node[] beastNodes = Util.getNodesAsArray(beastTree, numberOfVertices);
		//String[] beastTreeTaxa = beastTree.getTaxaNames();

		// create tree
		PQTree tree = new PQTree(numberOfLeaves);

		// create leaves
		for (int i = 0; i < numberOfLeaves; i++) {
			PQVertex leaf = new PQVertex(VertexType.L, i);
			leaf.setTaxon(taxaOrder[i]);
			leaf.initClade(numberOfLeaves);
			// leaf.initDescendants(numberOfVertices);
			tree.vertices.add(leaf);
		}

		// map from leaves to leaves of beast tree
		// i.e. leaf-i at position i in tree -> leaf-i at position j in
		// beastTree
//		Node[] beastLeavesSorted = new Node[numberOfLeaves];
//		for (int i = 0; i < numberOfLeaves; i++) {
//			int taxaPosition = Util.findTaxaPosition(taxaOrder, beastTreeTaxa[i]);
//			beastLeavesSorted[taxaPosition] = beastNodes[i];
//		}

		// create internal vertices
		for (int i = numberOfLeaves; i < numberOfVertices; i++) {
			PQVertex vertex = new PQVertex(VertexType.Q, i);
			tree.vertices.add(vertex);
		}
		tree.vertices.get(tree.vertices.size() - 1).setAsRoot();
		tree.root = tree.vertices.get(tree.vertices.size() - 1);
		tree.root.setAsRoot();

		// add edges
		for (int i = 0; i < numberOfVertices - 1; i++) {
			int parentNr;
//			if (i < numberOfLeaves) {
//				parentNr = beastLeavesSorted[i].getParent().getNr();
//			} else {
				parentNr = beastNodes[i].getParent().getNr();
//			}
			PQVertex child = tree.vertices.get(i);
			PQVertex parent = tree.vertices.get(parentNr);
			child.setParent(parent);
			parent.addChild(child);
			if (parent.getChildCount() > 1) {
				parent.initClade(numberOfLeaves);
			}
		}

		return tree;
	}

	public PQTree clone() {
		PQTree clone = new PQTree(numberOfLeaves);
		ArrayList<PQVertex> clonedVertices = new ArrayList<PQVertex>(
				vertices.size() - numberOfLeaves);

		PQVertex rootClone = root.cloneDownwards(null, numberOfLeaves);
		clone.root = rootClone;
		clone.root.setAsRoot();

		LinkedList<PQVertex> queue = new LinkedList<PQVertex>();
		queue.add(rootClone);
		while (!queue.isEmpty()) {
			PQVertex current = queue.poll();
			if (current.isLeaf()) {
				clone.vertices.add(current);
			} else {
				clonedVertices.add(current);
			}
			queue.addAll(current.getChildren());
		}
		vertices.addAll(clonedVertices);

		return clone;
	}

	// # getters
	public PQVertex getRoot() {
		return root;
	}

	public int getNumberOfLeaves() {
		return numberOfLeaves;
	}

	public int getNumberOfVertices() {
		return vertices.size();
	}

	public int getMaxNumberOfVertices() {
		return 2 * getNumberOfLeaves() - 1;
	}

	public String getPossibleSequence() {
		return root.getPossibleSequence();
	}

	public String getPossibleSequences() {
		return root.getPossibleSequences();
	}

	public int[] getPossibleOrder() {
		int[] order = new int[numberOfLeaves];
		int[] k = new int[1];
		k[0] = 0;

		root.getPossibleOrder(order, k);

		return order;
	}

	// # constraining
	public boolean constrainByClade(BitSet clade) {
		return constrainByClade(this.getRoot(), clade, Side.NONE);
	}

	final static boolean debug = false;
	
	private boolean constrainByClade(PQVertex vertex, BitSet clade, Side side) {
		if (debug) {
			System.out.println("Constrain with " + clade + " on side " + side + " vertex " + vertex);
			System.out.println(" " + vertex.getPossibleSequences());
			if (!isValid(vertex)) {
				System.out.println("some node has misdirected parent in the tree");
			}
		}
		// makeValid(vertex);

		if (vertex.isLeaf() || vertex.hasClade(clade)) {
			return true;
		}

		// check if can go down to a child
		if (side == Side.NONE) {
			for (PQVertex child : vertex.getChildren()) {
				if (child.hasSupersetCladeOf(clade)) {
					return constrainByClade(child, clade, side);
				}
			}
		}

		// classify children
		List<PQVertex> intersectedChildren = new LinkedList<PQVertex>();
		List<PQVertex> coveredChildren = new LinkedList<PQVertex>();
		List<PQVertex> disjointChildren = new LinkedList<PQVertex>();
		if (!classifyChildren(vertex, clade, intersectedChildren, coveredChildren,
				disjointChildren)) {
			return false;
		}
		// we now know that num intersected child is less than 2
		// System.out.println("Child classification: " +
		// intersectedChildren.size() + " intersecting, "
		// + coveredChildren.size() + " covered, " + disjointChildren.size() + "
		// disjoint");

		if (coveredChildren.size() == vertex.getChildCount()) {
			return true;
		}
		// we now know that not num intersected and num disjoint children both 0

		if (intersectedChildren.size() > 0) {
			for (PQVertex intersectedChild : intersectedChildren) {
				if (!canMoveIntersectionToSide(intersectedChild, clade)) {
					return false;
				}
			}
		}
		// we now know that intersected children can move intersection to a side

		if (vertex.getVertexType() == VertexType.Q) {
			boolean success = constrainQVertex(vertex, clade, side, intersectedChildren);
			if (!success) {
				vertex.reverseChildren();
				return constrainQVertex(vertex, clade, side, intersectedChildren);
			}
		} else { // Type P
			if (intersectedChildren.size() == 0) {
				if (side == Side.NONE) {
					// zero intersected children imples that
					// there are at least two covered children 
					// and at least one clade-disjoint child.
					// only one covered child would have moved down automatically
					PQVertex newCoveredParent = new PQVertex(VertexType.P);
					moveCoveredChildren(vertex, coveredChildren, newCoveredParent);
					newCoveredParent.setParent(vertex);
					vertex.addChild(newCoveredParent);

					makeQVertexIfBinary(vertex);
					makeQVertexIfBinary(newCoveredParent);

					if (vertex.getDegreeOut() == 1) {
						throw new AssertionError("If vertex " + vertex + " has only one child it would have been fully covered" +
								 ",\n but now has only this child " + vertex.getChild(0));
					}
				} else {
					if (vertex == root) {
						throw new AssertionError("Constraining root to side nonsensical.");
					}

					// get parent
					PQVertex parent = vertex.parent;
					if (parent.getVertexType() != VertexType.Q) {
						throw new AssertionError(
								"Constraining to side under P-vertex nonsensical.");
					}
					int vertexIndex = parent.getChildren().indexOf(vertex);

					// extract covered vertices
					PQVertex coveredVertex = extractCoveredVerticesIntoVertex(vertex,
							coveredChildren);
					if (side == Side.LEFT) {
						parent.addChildAt(coveredVertex, vertexIndex);
					} else {
						parent.addChildAt(coveredVertex, vertexIndex + 1);
					}
					coveredVertex.setParent(parent);

					// fix tree depending on # of disjoint kids
					if (vertex.getChildCount() == 1) {
						// suppress old vertex
						suppressVertex(vertex);
					} else {
						makeQVertexIfBinary(vertex);
						vertex.initClade(numberOfLeaves);
					}
				}
			} else { // 1 or 2 intersected vertices
				if (side == Side.NONE) {
					PQVertex newInteresectingParent = new PQVertex(VertexType.Q);

					// add 1. intersected child
					PQVertex firstIntersectedChild = intersectedChildren.get(0);
					vertex.removeChild(firstIntersectedChild);
					newInteresectingParent.addChild(firstIntersectedChild);
					firstIntersectedChild.setParent(newInteresectingParent);

					// add covered children
					if (coveredChildren.size() != 0) {
						PQVertex coveredVertex = extractCoveredVerticesIntoVertex(vertex,
								coveredChildren);
						newInteresectingParent.addChild(coveredVertex);
						coveredVertex.setParent(newInteresectingParent);
					}

					// add 2. intersected child
					PQVertex secondIntersectedChild = null;
					if (intersectedChildren.size() == 2) {
						secondIntersectedChild = intersectedChildren.get(1);
						vertex.removeChild(secondIntersectedChild);
						newInteresectingParent.addChild(secondIntersectedChild);
						secondIntersectedChild.setParent(newInteresectingParent);
					}
					newInteresectingParent.initClade(numberOfLeaves);

					// fix tree depending on # of disjoint kids
					if (vertex.getChildCount() == 0) {
						if (vertex == root) {
							root = newInteresectingParent;
							newInteresectingParent.setAsRoot();
						} else {
							PQVertex parent = vertex.getParent();
							parent.replaceChild(vertex, newInteresectingParent);
							newInteresectingParent.setParent(parent);
						}
					} else {
						newInteresectingParent.setParent(vertex);
						vertex.addChild(newInteresectingParent);
						makeQVertexIfBinary(vertex);
						vertex.initClade(numberOfLeaves);
					}

					// progress down
					constrainByClade(firstIntersectedChild, clade, Side.RIGHT);
					if (secondIntersectedChild != null) {
						constrainByClade(secondIntersectedChild, clade, Side.LEFT);
					}
				} else {
					if (vertex == root) {
						throw new AssertionError("Constraining to side not possible for root.");
					}

					// get parent
					PQVertex parent = vertex.parent;
					if (parent.getVertexType() != VertexType.Q) {
						throw new AssertionError(
								"Constraining to side under P-vertex nonsensical.");
					}
					int vertexIndex = parent.getChildren().indexOf(vertex);

					// add covered children
					if (coveredChildren.size() > 0) {
						PQVertex coveredVertex = extractCoveredVerticesIntoVertex(vertex,
								coveredChildren);
						coveredVertex.setParent(parent);

						if (side == Side.LEFT) {
							parent.addChildAt(coveredVertex, vertexIndex);
						} else {
							parent.addChildAt(coveredVertex, vertexIndex + 1);
						}
					}

					// add intersected child
					PQVertex intersectedVertex = intersectedChildren.get(0);
					vertex.removeChild(intersectedVertex);
					intersectedVertex.setParent(parent);
					if (side == Side.LEFT) {
						parent.addChildAt(intersectedVertex, vertexIndex + 1);
					} else {
						parent.addChildAt(intersectedVertex, vertexIndex + 1);
					}

					// fix old vertex
					if (vertex.getChildCount() == 0) {
						parent.removeChild(vertex);
					} else if (vertex.getChildCount() == 1) {
						suppressVertex(vertex);
					} else {
						makeQVertexIfBinary(vertex);
						vertex.initClade(numberOfLeaves);
					}

					// progress down
					constrainByClade(intersectedVertex, clade, side);
				}
			}
		}

		return true;
	}

	private void suppressVertex(PQVertex vertex) {
		if (vertex.getChildCount() != 1) {
			throw new IllegalArgumentException("Can't suppress vertex with out-degree > 1");
		}
		PQVertex parent = vertex.getParent();
		PQVertex child = vertex.getChild(0);
		parent.replaceChild(vertex, child);
		child.setParent(parent);

//		if (child.getChildCount() == 1) {
//			suppressVertex(parent);
//		}
//		if (child.getChildCount() == 1) {
//			suppressVertex(parent);
//		}
	}

	private boolean constrainQVertex(PQVertex vertex, BitSet clade, Side side,
			List<PQVertex> intersectedChildren) {
		// check that vertices are in good order
		boolean coveringStarted = (side == Side.LEFT);
		boolean coveringEnded = false;
		PQVertex toConstrainLeft = null;
		PQVertex toConstrainRight = null;

		// check if cover fits in this order
		// returns with false otherwise
		// finds out who to constrain intersected kids
		for (int i = 0; i < vertex.getChildCount(); i++) {
			PQVertex child = vertex.getChild(i);
			if (child.hasSubsetCladeOf(clade)) {
				if (coveringEnded) {
					return false;
				} else if (coveringStarted) {
					continue;
				} else {
					if (intersectedChildren.size() == 2) {
						return false;
					}
					coveringStarted = true;

				}
			} else if (child.hasIntersectingCladeWith(clade)) {
				if (coveringEnded) {
					// can't start new cover
					return false;
				} else if (coveringStarted) {
					// covering started, so intersection ends it
					if (side == Side.RIGHT) {
						// bad because not reaching right end
						return false;
					}
					toConstrainLeft = child;
					coveringEnded = true;
				} else {
					// start cover
					toConstrainRight = child;
					coveringStarted = true;
				}
			} else {
				// disjoint child
				if (coveringEnded) {
					continue;
				} else if (coveringStarted) {
					// covering started, so disjoint ends it
					if (side == Side.RIGHT) {
						// bad because not reaching right end
						return false;
					}
					coveringEnded = true;
					// might end cover without a vertex actually
					// covering anything yet
					// but then later intersecting/covering vertex
					// will return false
				} else {
					continue; // covering not started
				}
			}
		}

		// move all children up to parent
		if (side != Side.NONE) {
			PQVertex parent = vertex.getParent();
			int vertexIndex = parent.getChildren().indexOf(vertex);
			parent.getChildren().addAll(vertexIndex, vertex.getChildren());
			for (PQVertex child : vertex.getChildren()) {
				child.setParent(parent);
			}
			parent.getChildren().remove(vertex);
		}

		// progress down
		if (toConstrainLeft != null) {
			constrainByClade(toConstrainLeft, clade, Side.LEFT);
		}
		if (toConstrainRight != null) {
			constrainByClade(toConstrainRight, clade, Side.RIGHT);
		}

		return true;
	}

	/**
	 * Specified vertex has clade intersecting with specified clade. Return
	 * whether children can be sorted such that intersection is on one side.
	 * 
	 * @param vertex
	 * @param clade
	 *            whose intersection with clade of specified vertex is checked
	 *            to be moveable to side
	 * @return whether children can be sorted such that intersection is on side
	 */
	private boolean canMoveIntersectionToSide(PQVertex vertex, BitSet clade) {
		if (vertex.isLeaf() || vertex.hasClade(clade)) {
			return true;
		}

		// classify children
		List<PQVertex> intersectedChildren = new LinkedList<PQVertex>();
		List<PQVertex> coveredChildren = new LinkedList<PQVertex>();
		List<PQVertex> disjointChildren = new LinkedList<PQVertex>();
		if (!classifyChildren(vertex, clade, intersectedChildren, coveredChildren,
				disjointChildren)) {
			return false;
		}
		if (intersectedChildren.size() > 1) {
			return false;
		} else {
			// case: intersectedChildren.size() == 1
			if (vertex.getVertexType() == VertexType.P) {
				if (intersectedChildren.size() == 0) {
					return true;
				} else {
					return canMoveIntersectionToSide(intersectedChildren.get(0), clade);
				}
			} else { // Q-vertex
				boolean leftSideUsed = false;
				boolean coveringStarted = false;
				boolean coveringEnded = false;

				// check if possible start
				PQVertex child = vertex.getChild(0);
				if (child.hasSubsetCladeOf(clade)) {
					leftSideUsed = true;
					coveringStarted = true;
				} else if (child.hasIntersectingCladeWith(clade)) {
					// either start cover from here to right
					// or from left only till here
					if (!(coveredChildren.size() == 0) || (disjointChildren.size() == 0)) {
						return false;
					} else {
						return canMoveIntersectionToSide(child, clade);
					}
				}

				// continue scan
				for (int i = 1; i < vertex.getChildCount(); i++) {
					child = vertex.getChild(i);

					if (child.hasSubsetCladeOf(clade)) {
						if (coveringEnded) {
							return false;
						} else if (coveringStarted) {
							continue;
						} else {
							if (intersectedChildren.size() > 0) {
								// intersected child would need to start cover
								return false;
							} else {
								coveringStarted = true;
							}
						}
					} else if (child.hasIntersectingCladeWith(clade)) {
						if (coveringEnded) {
							return false;
						} else if (coveringStarted) {
							if (!leftSideUsed) {
								return false;
							}
							coveringEnded = true;
						} else {
							coveringStarted = true;
						}
					} else {
						if (coveringEnded || !coveringStarted) {
							continue;
						} else {
							// covering started
							// but hasn't ended yet
							if (!leftSideUsed) {
								// can't end cover that didn't start at left
								return false;
							} else {
								coveringEnded = true;
							}
						}
					}
				}

				if (intersectedChildren.size() > 0) {
					return canMoveIntersectionToSide(intersectedChildren.get(0), clade);
				} else {
					return true;
				}
			}
		}
	}
	
	private boolean isValid(PQVertex root) {
		for (PQVertex child : root.getChildren()) {
			if (child.getParent() != root) {
				return false;
			}
			if (!isValid(child)) {
				return false;
			}
		}
		return true;
	}

	private void makeValid(PQVertex root) {
		for (PQVertex child : root.getChildren()) {
			child.setParent(root);
			makeValid(child);
		}
	}

	private void makeQVertexIfBinary(PQVertex vertex) {
		if (vertex.getChildCount() == 2) {
			vertex.setVertexType(VertexType.Q);
		}
	}

	private PQVertex extractCoveredVerticesIntoVertex(PQVertex vertex,
			List<PQVertex> coveredChildren) {
		if (coveredChildren.size() == 0) {
			throw new IllegalArgumentException("Should not be called when 0 covered vertices.");
		}

		PQVertex coveredVertex;
		if (coveredChildren.size() == 1) {
			coveredVertex = coveredChildren.get(0);
			vertex.removeChild(coveredVertex);
		} else {
			coveredVertex = new PQVertex(VertexType.P);
			moveCoveredChildren(vertex, coveredChildren, coveredVertex);
			makeQVertexIfBinary(coveredVertex);
		}
		return coveredVertex;
	}

	private void moveCoveredChildren(PQVertex oldParent, List<PQVertex> coveredChildren,
			PQVertex newCoveredParent) {
		for (PQVertex coveredChild : coveredChildren) {
			oldParent.removeChild(coveredChild);
			newCoveredParent.addChild(coveredChild);
			coveredChild.setParent(newCoveredParent);
		}
		newCoveredParent.initClade(numberOfLeaves);
	}

	private boolean classifyChildren(PQVertex parent, BitSet clade,
			List<PQVertex> intersectedChildren, List<PQVertex> coveredChildren,
			List<PQVertex> disjointChildren) {
		for (PQVertex child : parent.getChildren()) {
			if (child.hasSubsetCladeOf(clade)) {
				coveredChildren.add(child);
			} else if (child.hasIntersectingCladeWith(clade)) {
				if (intersectedChildren.size() == 2) {
					// can't have 3 intersected (but not fully covered) children
					return false;
				}
				intersectedChildren.add(child);
			} else {
				disjointChildren.add(child);
			}
		}

		return true;
	}

	private enum Side {
		LEFT, RIGHT, NONE;
	}

	// public static void main(String[] args) {
	// PQTree star = createStarTree(6);
	// System.out.println(star.getPossibleSequences());
	// star.root.printTree("");
	// System.out.println();
	//
	// BitSet clade = new BitSet(6);
	// clade.set(1);
	// clade.set(3);
	// clade.set(5);
	// star.constrainByClade(clade);
	// System.out.println(star.getPossibleSequences());
	// star.root.printTree("");
	// System.out.println();
	//
	// clade.clear();
	// clade.set(2);
	// clade.set(4);
	// star.constrainByClade(clade);
	// System.out.println(star.getPossibleSequences());
	// star.root.printTree("");
	// System.out.println();
	//
	// clade.set(1);
	// clade.set(3);
	// clade.set(5);
	// star.constrainByClade(clade);
	// System.out.println(star.getPossibleSequences());
	// star.root.printTree("");
	// System.out.println();
	//
	// clade.clear();
	// clade.set(3);
	// clade.set(4);
	// star.constrainByClade(clade);
	// System.out.println(star.getPossibleSequences());
	// star.root.printTree("");
	// System.out.println();
	//
	// clade.clear();
	// clade.set(0);
	// clade.set(2);
	// System.out.println(star.constrainByClade(clade));
	// System.out.println(star.getPossibleSequences());
	// star.root.printTree("");
	// System.out.println();
	//
	// clade.clear();
	// clade.set(3);
	// clade.set(1);
	// System.out.println(star.constrainByClade(clade));
	// System.out.println(star.getPossibleSequences());
	// star.root.printTree("");
	// System.out.println();
	//
	// clade.clear();
	// clade.set(5);
	// clade.set(1);
	// System.out.println(star.constrainByClade(clade));
	// System.out.println(star.getPossibleSequences());
	// star.root.printTree("");
	// System.out.println();
	//
	// clade.clear();
	// clade.set(2);
	// clade.set(3);
	// System.out.println(star.constrainByClade(clade));
	// System.out.println(star.getPossibleSequences());
	// star.root.printTree("");
	// System.out.println();
	// }
}
