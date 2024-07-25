package viz.ccd;

import viz.Node;

import java.util.ArrayList;

public class WrappedBeastTree {

    private int count = 1;

    private Tree wrappedTree;

    private BitSet[] cladeOfVertex;

    /** Matrix containing distances (#edges) between pairs of leaves. */
    private double[][] pathDistanceMatrix;

    public WrappedBeastTree(Tree wrappedTree) {
        super();
        this.wrappedTree = wrappedTree;
        cladeOfVertex = new BitSet[wrappedTree.getNodeCount()];
        this.initCladeBitSet(wrappedTree.getRoot());
    }

    public Tree getWrappedTree() {
        return wrappedTree;
    }

    private BitSet initCladeBitSet(Node vertex) {
        BitSet cladeAsBitSet = BitSet.newBitSet(wrappedTree.getLeafNodeCount());
        if (vertex.isLeaf()) {
            cladeAsBitSet.set(vertex.getNr());
        } else {
            for (Node child : vertex.getChildren()) {
                BitSet childBitSet = initCladeBitSet(child);
                cladeAsBitSet.or(childBitSet);
            }
        }

        cladeOfVertex[vertex.getNr()] = cladeAsBitSet;
        return cladeAsBitSet;
    }

    public double getHeightOfClade(BitSet cladeInBits) {
        return getHeightOfClade(cladeInBits, wrappedTree.getRoot());
    }

    /* Recursive helper method */
    private double getHeightOfClade(BitSet cladeInBits, Node vertex) {
        if (vertex.isLeaf()) {
            return vertex.getHeight();
        }

        if (vertex.getChild(0) == null) {
            System.err.println("- problem -");
            System.err.println("current vertex: " + vertex);
            System.err.println("clade in bits: " + cladeInBits);
            return -1;
        }

        // if one of the children contains the clade, then recurse;
        // otherwise the current vertex is the LCA of that clade

        // test first child
        BitSet copy = (BitSet) cladeOfVertex[vertex.getChild(0).getNr()].clone();
        copy.and(cladeInBits);
        copy.xor(cladeInBits);
        if (copy.isEmpty()) {
            return getHeightOfClade(cladeInBits, vertex.getChild(0));
        }

        // otherwise test second child
        copy = (BitSet) cladeOfVertex[vertex.getChild(1).getNr()].clone();
        copy.and(cladeInBits);
        copy.xor(cladeInBits);
        if (copy.isEmpty()) {
            return getHeightOfClade(cladeInBits, vertex.getChild(1));
        }

        // otherwise return height of this vertex
        return vertex.getHeight();
    }

    public BitSet getCladeInBits(int vertexIndex) {
        return cladeOfVertex[vertexIndex];
    }

//    public boolean equals(WrappedBeastTree other) {
//        return TreeDistances.robinsonsFouldDistance(this, other) == 0;
//    }

    public boolean containsClade(BitSet cladeInBits) {
        for (BitSet treeCladeInBits : cladeOfVertex) {
            if (treeCladeInBits.equals(cladeInBits)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<BitSet> getNontrivialClades() {
        ArrayList<BitSet> clades = new ArrayList<>();
        for (int i = wrappedTree.getLeafNodeCount(); i < wrappedTree.getNodeCount(); i++) {
            clades.add(cladeOfVertex[i]);
        }
        return clades;
    }

    public ArrayList<BitSet> getClades() {
        ArrayList<BitSet> clades = new ArrayList<>();
        for (int i = 0; i < wrappedTree.getNodeCount(); i++) {
            clades.add(cladeOfVertex[i]);
        }
        return clades;
    }

    public void increaseCount() {
        this.count++;
    }

    public int getCount() {
        return count;
    }

//    /** @return distance matrix of pairwise leaf path lengths */
//    public double[][] getPathDistanceMatrix() {
//        if (pathDistanceMatrix == null) {
//            computePathDistanceMatrix();
//        }
//
//        return pathDistanceMatrix;
//    }
//
//    /** Computes the distance matrix of pairwise leaf path lengths. */
//    public void computePathDistanceMatrix() {
//        Tree tree = wrappedTree;
//        int n = tree.getLeafNodeCount();
//        pathDistanceMatrix = new double[n][n];
//
//        for (int i = 0; i < n; i++) {
//            Node prev = tree.getNode(i);
//            computePathDistanceMatrix(prev, prev.getParent(), 1, i, true);
//        }
//    }
//
//    /* Tree traversal helper method */
//    private void computePathDistanceMatrix(Node prev, Node next, int d, int indexReferenceLeaf,
//                                           boolean up) {
//        if (up) {
//            // go further up
//            if (!next.isRoot()) {
//                computePathDistanceMatrix(next, next.getParent(), d + 1, indexReferenceLeaf, true);
//            }
//
//            // go down on other paths
//            for (Node child : next.getChildren()) {
//                if (child == prev) {
//                    continue;
//                } else if (child.isLeaf()) {
//                    pathDistanceMatrix[indexReferenceLeaf][child.getNr()] = d + 1;
//                } else {
//                    computePathDistanceMatrix(next, child, d + 1, indexReferenceLeaf, false);
//                }
//            }
//
//        } else {
//            // go down all paths
//            for (Node child : next.getChildren()) {
//                if (child.isLeaf()) {
//                    pathDistanceMatrix[indexReferenceLeaf][child.getNr()] = d + 1;
//                } else {
//                    computePathDistanceMatrix(next, child, d + 1, indexReferenceLeaf, false);
//                }
//            }
//        }
//
//    }
}
