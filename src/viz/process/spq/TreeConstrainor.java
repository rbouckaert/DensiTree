package viz.process.spq;

import java.util.BitSet;
import java.util.LinkedList;

import viz.Node;

/**
 * This class represents the process to find possible sequences (represented by
 * a {@link PQTree}) that are compatible with as many as possible {@link Tree
 * trees}.
 *
 * @author Jonathan Klawitter
 */
public class TreeConstrainor extends Constrainor {

	private PQTree[] pqTrees;
	private BitSet treesCovered;

	public TreeConstrainor(final Node[] trees) {
		this.trees = trees;
		//this.taxa = beastTrees[0].getTaxaNames();
		this.numberOfLeaves = taxa.length;

		this.treesCovered = new BitSet(trees.length);
	}

	public void run() {
		// 1. create trees
		pqTrees = new PQTree[trees.length];
		for (int i = 0; i < trees.length; i++) {
			pqTrees[i] = PQTree.createPQFTreeFromBeastTree(trees[i], taxa);
		}

		// 2. set start tree
		tree = pqTrees[0];
		treesCovered.set(0);

		// 3. constrain by other trees
		for (int i = 1; i < pqTrees.length; i++) {
			boolean success = constrainTreeByTree(tree, pqTrees[i]);
			if (success) {
				treesCovered.set(i);
			}
		}
	}

	private boolean constrainTreeByTree(PQTree constrained, PQTree constrainer) {
		// 1. save current tree
		PQTree workOnTree = constrained.clone();
		boolean success = true;

		// 2. extract clade sequence
		BitSet[] clades = extractClades(constrainer);

		// 3. constrain with clades while successful
		for (int i = 0; (i < clades.length) && success; i++) {
			success &= workOnTree.constrainByClade(clades[i]);
		}

		// 4. save changes based on success
		if (success) {
			tree = workOnTree;
			return true;
		} else {
			return false;
		}
	}

	private BitSet[] extractClades(PQTree constrainer) {
		BitSet[] clades = new BitSet[constrainer.getNumberOfVertices()
				- constrainer.getNumberOfLeaves()];
		LinkedList<PQVertex> queue = new LinkedList<PQVertex>();
		queue.add(constrainer.getRoot());

		int i = clades.length - 1;
		while (!queue.isEmpty()) {
			PQVertex current = queue.poll();
			if (!current.isLeaf()) {
				clades[i--] = current.getClade();
			}
			queue.addAll(current.getChildren());
		}

		return clades;
	}

	public BitSet getTreesCoveredIndicator() {
		return treesCovered;
	}
}
