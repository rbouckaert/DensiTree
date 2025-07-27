package viz.ccd;

import viz.Node;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * This class represents a tree distribution using an extended CCD graph
 * (via the parent class {@link AbstractCCD}), in particular,
 * a clade vertex is unique not only up to its clade but also its sibling clade.
 * The model is thus equivalent to the sDAG by Zhang and Matsen
 * as well as Jun et al.
 * The probability of a clade split {C1, C2} is conditional on its parent clade C
 * and the sibling clade Cs of C, so P(C1, C2 | C, Cs).
 *
 * <p>
 * It can be constructed in one go by providing a set of trees or maintained
 * (e.g. during an MCMC run) by adding and removing trees.
 * </p>
 *
 * <p>
 * It can be used to sample trees with different {@link SamplingStrategy} and
 * {@link HeightSettingStrategy}, compute their probabilities, compute the
 * entropy of the distribution, ...
 * </p>
 *
 * <p>
 * The MAP tree of this distribution is the tree with highest ccd.
 * </p>
 *
 * @author Jonathan Klawitter
 */
public class CCD2 extends AbstractCCD {

    private Map<BitSet, Map<BitSet, ExtendedClade>> extendedCladeMapping;

    private HashSet<Clade> clades;

    /* -- CONSTRUCTORS & CONSTRUCTION METHODS -- */

    /**
     * Constructor for a {@link CCD2} based on the given collection of trees
     * with specified burn-in.
     *
     * @param trees  the trees whose distribution is approximated by the resulting
     *               {@link CCD2}
     * @param burnin value between 0 and 1 of what percentage of the given trees
     *               should be discarded as burn-in
     */
    public CCD2(List<Tree> trees, double burnin) {
        this(trees.get(0).getLeafNodeCount(), true);

        this.burnin = burnin;
        List<Tree> treesToUse;
        if (burnin == 0) {
            treesToUse = trees;
        } else {
            int numDiscardedTrees = (int) (trees.size() * burnin);
            int numUsedTrees = trees.size() - numDiscardedTrees;
            this.numBaseTrees = numUsedTrees;
            treesToUse = new ArrayList<Tree>(numUsedTrees);
            treesToUse.addAll(trees.subList(numDiscardedTrees, trees.size()));
        }

        for (Tree tree : treesToUse) {
            cladifyTree(tree);
        }
    }

    /**
     * Constructor for a {@link CCD2} based on the given collection of trees
     * (not containing any burnin trees).
     *
     * @param treeSet an iterable set of trees, which contains no burnin trees,
     *                whose distribution is approximated by the resulting
     *                {@link CCD2}
     */
//    public CCD2(TreeSet treeSet) {
//        this(treeSet, false);
//    }

    /**
     * Constructor for a {@link CCD2} based on the given collection of trees
     * (not containing any burnin trees).
     *
     * @param treeSet        an iterable set of trees, which contains no burnin trees,
     *                       whose distribution is approximated by the resulting
     *                       {@link CCD2}
     * @param storeBaseTrees whether to store the trees used to create this CCD
     */
//    public CCD2(TreeSet treeSet, boolean storeBaseTrees) {
//        super(storeBaseTrees);
//
//        this.burnin = 0;
//        try {
//            treeSet.reset();
//            Tree tree = treeSet.next();
//            extendedCladeMapping = new HashMap<>(10 * tree.getLeafNodeCount());
//            clades = new HashSet<>(10 * tree.getLeafNodeCount());
//            super.initializeRootClade(tree.getLeafNodeCount());
//            clades.add(this.rootClade);
//
//            if (verbose) {
//                out.println("Constructing CCD2 with " + (treeSet.totalTrees - treeSet.burninCount) + " trees...");
//            }
//
//            while (tree != null) {
//                this.numBaseTrees++;
//                cladifyTree(tree);
//
//                // report progress
//                if (verbose) {
//                    if (numBaseTrees % 10 == 0) {
//                        System.out.print(".");
//                        System.out.flush();
//                    }
//                    if (numBaseTrees % 1000 == 0) {
//                        System.out.println(" (" + numBaseTrees + ")");
//                    }
//                }
//
//                tree = treeSet.hasNext() ? treeSet.next() : null;
//            }
//
//            if (verbose) {
//                System.out.println(" ...done.");
//            }
//
//        } catch (IOException e) {
//            System.err.println("Error reading in trees to create CCD.");
//        }
//    }

    /**
     * Constructor for an empty CDD. Trees can then be processed one by one.
     *
     * @param numLeaves      number of leaves of the trees that this CCD will be based on
     * @param storeBaseTrees whether to store the trees used to create this CCD;
     *                       recommended not to when huge set of trees is used
     */
    public CCD2(int numLeaves, boolean storeBaseTrees) {
        super(numLeaves, storeBaseTrees);
        extendedCladeMapping = new HashMap<>(10 * numLeaves);
        clades = new HashSet<>(10 * numLeaves);
    }

    @Override
    protected void cladifyTree(Tree tree) {
        if (super.storesBaseTrees()) {
            this.baseTrees.add(tree);
        } else if (this.baseTrees.isEmpty()) {
            this.baseTrees.add(tree);
        }

        Node root = tree.getRoot();
        ExtendedClade[] children = cladifyVertices(root.getChild(0), root.getChild(1));

        CladePartition rootPartition = this.rootClade.getCladePartition(children[0], children[1]);
        if (rootPartition == null) {
            rootPartition = this.rootClade.createCladePartition(children[0], children[1], true);
        }
        rootPartition.increaseOccurrenceCount(root.getHeight());
        rootClade.increaseOccurrenceCount(root.getHeight());
    }

    /* Recursive helper method */
    private ExtendedClade[] cladifyVertices(Node leftVertex, Node rightVertex) {
        BitSet leftInBits = BitSet.newBitSet(leafArraySize);
        BitSet rightInBits = BitSet.newBitSet(leafArraySize);

        // 1. process the children: create them, return bundled, set bits in BitSet
        ExtendedClade[] leftChildren = processChildrenCladifying(leftVertex, leftInBits);
        ExtendedClade[] rightChildren = processChildrenCladifying(rightVertex, rightInBits);

        // 2. create extended clades, if they don't exist yet
        ExtendedClade leftClade = getExtendedClade(leftInBits, rightInBits);
        ExtendedClade rightClade = getExtendedClade(rightInBits, leftInBits);
        if (leftClade == null) {
            // we know that if one doesn't exist yet then also the other does not unless one is a leaf;
            // addNewClade then adds both new clades
            leftClade = addNewClade(leftInBits, leftVertex, rightClade, rightInBits, rightVertex);
            if (rightClade == null) {
                rightClade = getExtendedClade(rightInBits, leftInBits);
            }
        }
        if (rightClade == null) {
            rightClade = addNewClade(rightInBits, rightVertex, leftClade, leftInBits, leftVertex);
        }
        leftClade.increaseOccurrenceCount(leftVertex.getHeight());
        rightClade.increaseOccurrenceCount(rightVertex.getHeight());

        processCladePartitionCladifying(leftVertex, leftClade, leftChildren);
        processCladePartitionCladifying(rightVertex, rightClade, rightChildren);

        return new ExtendedClade[]{leftClade, rightClade};
    }

    /* Helper method */
    private ExtendedClade[] processChildrenCladifying(Node parent, BitSet cladeInBits) {
        ExtendedClade[] children = null;
        if (parent.isLeaf()) {
            cladeInBits.set(parent.getNr());
        } else {
            children = cladifyVertices(parent.getChild(0), parent.getChild(1));
            cladeInBits.or(children[0].getCladeInBits());
            cladeInBits.or(children[1].getCladeInBits());
        }
        return children;
    }

    /* Helper method */
    private ExtendedClade addNewClade(BitSet cladeInBits, Node vertex, ExtendedClade sibling, BitSet siblingInBits, Node siblingVertex) {
        ExtendedClade clade;
        if (vertex.isLeaf()) {
            clade = new ExtendedClade(cladeInBits, this);
        } else {
            clade = new ExtendedClade(cladeInBits, sibling, this);
        }

        if (sibling == null) {
            sibling = addNewClade(siblingInBits, siblingVertex, clade, cladeInBits, vertex);

            if (!vertex.isLeaf()) {
                clade.setSibling(sibling);
            }
        }

        clades.add(clade);
        if (vertex.isLeaf()) {
            cladeMapping.put(cladeInBits, clade);
        } else {
            Map<BitSet, ExtendedClade> map = extendedCladeMapping.get(cladeInBits);
            if (map == null) {
                map = new HashMap<>();
                extendedCladeMapping.put(cladeInBits, map);
            }
            map.put(siblingInBits, clade);
        }

        return clade;
    }

    /* Helper method */
    private static void processCladePartitionCladifying(Node vertex, ExtendedClade clade, ExtendedClade[] children) {
        if (!vertex.isLeaf()) {
            CladePartition currentPartition = clade.getCladePartition(children[0], children[1]);
            if (currentPartition == null) {
                currentPartition = clade.createCladePartition(children[0], children[1], true);
            }
            currentPartition.increaseOccurrenceCount(vertex.getHeight());
        }
    }

    /**
     * Return the extended clade defined by itself and its sibling in a CCD2.
     *
     * @param cladeInBits   clade in bits
     * @param siblingInBits sibling in bits; can be null if requesting a leaf clade
     * @return extended clade based on itself and sibling
     */
    public ExtendedClade getExtendedClade(BitSet cladeInBits, BitSet siblingInBits) {
        if (cladeInBits.cardinality() == 1) {
            // if it is a leaf, then do not consider sibling
            return (ExtendedClade) cladeMapping.get(cladeInBits);
        } else {
            // otherwise have to use extended mapping with two get requests
            Map<BitSet, ExtendedClade> map = extendedCladeMapping.get(cladeInBits);
            return (map == null) ? null : map.get(siblingInBits);
        }
    }

    @Override
    public void initialize() {
        // nothing to do for CCD2
    }

    @Override
    public void removeTree(Tree tree, boolean tidyUpCCDGraph) {
        // throw new UnsupportedOperationException("Removing trees not supported for CCD2s yet.");
        if (super.storesBaseTrees() && !this.baseTrees.remove(tree)) {
            System.err.println("WARNING: Removing tree from CCD that was not part of it.");
        }

        Node root = tree.getRoot();
        ExtendedClade[] children = reduceCladeCount(root.getChild(0), root.getChild(1));

        if (tidyUpCCDGraph) {
            this.tidyUpCCDGraph(false);
        }

        this.setCacheAsDirty();
    }

    /* Recursive helper method */
    private ExtendedClade[] reduceCladeCount(Node leftVertex, Node rightVertex) {
        BitSet leftInBits = BitSet.newBitSet(leafArraySize);
        BitSet rightInBits = BitSet.newBitSet(leafArraySize);

        // 1. build BitSet to retrieve clade and call recursion
        ExtendedClade[] leftChildren = processChildrenRemoving(leftVertex, leftInBits);
        ExtendedClade[] rightChildren = processChildrenRemoving(rightVertex, rightInBits);

        // 2. retrieve clades and reduce count
        ExtendedClade leftClade = getExtendedClade(leftInBits, rightInBits);
        leftClade.decreaseOccurrenceCount(leftVertex.getHeight());
        ExtendedClade rightClade = getExtendedClade(rightInBits, leftInBits);
        rightClade.decreaseOccurrenceCount(rightVertex.getHeight());

        // 3. reduce counts for its clade partitions
        processCladePartitionRemoving(leftVertex, leftClade, leftChildren);
        processCladePartitionRemoving(rightVertex, rightClade, rightChildren);

        return new ExtendedClade[]{leftClade, rightClade};
    }

    /* Recursive helper method */
    private ExtendedClade[] processChildrenRemoving(Node parent, BitSet cladeInBits) {
        ExtendedClade[] children = null;
        if (parent.isLeaf()) {
            cladeInBits.set(parent.getNr());
        } else {
            children = reduceCladeCount(parent.getChild(0), parent.getChild(1));
            cladeInBits.or(children[0].getCladeInBits());
            cladeInBits.or(children[1].getCladeInBits());
        }
        return children;
    }

    /* Helper method */
    private void processCladePartitionRemoving(Node vertex, ExtendedClade clade, ExtendedClade[] children) {
        if (!vertex.isLeaf()) {
            CladePartition currentPartition = clade.getCladePartition(children[0], children[1]);
            currentPartition.decreaseOccurrenceCount(vertex.getHeight());

            removeCladePartitionIfNecessary(clade, currentPartition);
        }
    }


    /* -- GENERAL & CCD GRAPH GETTERS -- */

    @Override
    public int getNumberOfClades() {
        return clades.size();
    }

    @Override
    public Collection<Clade> getClades() {
        return clades;
    }

    @Override
    public int getNumberOfCladePartitions() {
        int count = rootClade.getNumberOfPartitions();
        for (Map<BitSet, ExtendedClade> map : extendedCladeMapping.values()) {
            for (ExtendedClade clade : map.values()) {
                count += clade.getNumberOfPartitions();
            }
        }
        return count;
    }

    @Override
    public double getCladeProbability(BitSet cladeInBits) {
        resetCacheIfProbabilitiesDirty();

        if (cladeInBits.cardinality() == cladeInBits.length()) {
            // root clade
            return 1;
        }

        Map<BitSet, ExtendedClade> map = extendedCladeMapping.get(cladeInBits);
        if (map == null) {
            return 0;
        }

        double probability = 0;
        for (ExtendedClade clade : map.values()) {
            if (clade.getProbability() < 0) {
                computeCladeProbabilities();
            }
            probability += clade.getProbability();
        }

        return probability;
    }


    /* -- STATE MANAGEMENT - STATE MANAGEMENT -- */

    @Override
    public void setCacheAsDirty() {
        super.setCacheAsDirty();
    }

    @Override
    protected void tidyUpCacheIfDirty() {
        resetCacheIfProbabilitiesDirty();
    }

    @Override
    protected void resetCache() {
        for (Map<BitSet, ExtendedClade> map : extendedCladeMapping.values()) {
            for (ExtendedClade clade : map.values()) {
                clade.resetCachedValues();
            }
        }
        super.resetCache();
    }

    @Override
    protected boolean removeCladePartitionIfNecessary(Clade clade, CladePartition partition) {
        // when a partition has no registered occurrences more, we can remove it
        if (partition.getNumberOfOccurrences() == 0) {
            clade.removePartition(partition);
            return true;
        }
        return false;
    }


    /* -- PROBABILITY, POINT ESTIMATE & SAMPLING METHODS -- */
    // mostly handled by parent class AbstractCCD

    @Override
    public double getProbabilityOfTree(Tree tree) {
        resetCacheIfProbabilitiesDirty();

        double[] runningProbability = new double[]{1};
        Node root = tree.getRoot();
        ExtendedClade[] children = computeProbabilityOfVertices(root.getChild(0), root.getChild(1), runningProbability);
        if (runningProbability[0] == 0) {
            return 0;
        } else {
            CladePartition partition = rootClade.getCladePartition(children[0], children[1]);
            if (partition != null) {
                return runningProbability[0] * partition.getCCP();
            } else {
                return 0;
            }
        }
    }

    /* Recursive helper method */
    private ExtendedClade[] computeProbabilityOfVertices(Node leftVertex, Node rightVertex, double[] runningProbability) {
        BitSet leftInBits = BitSet.newBitSet(leafArraySize);
        BitSet rightInBits = BitSet.newBitSet(leafArraySize);

        ExtendedClade[] leftChildren = computeProbabilityOfChildren(leftVertex, leftInBits, runningProbability);
        if (!leftVertex.isLeaf() && (leftChildren == null)) {
            return null;
        }
        ExtendedClade[] rightChildren = computeProbabilityOfChildren(rightVertex, rightInBits, runningProbability);
        if (!rightVertex.isLeaf() && (rightChildren == null)) {
            return null;
        }

        ExtendedClade leftClade = getExtendedClade(leftInBits, rightInBits);
        ExtendedClade rightClade = getExtendedClade(rightInBits, leftInBits);
        if ((leftClade == null) || (rightClade == null)) {
            runningProbability[0] = 0;
            return null;
        }

        if (!leftVertex.isLeaf()) {
            CladePartition partition = leftClade.getCladePartition(leftChildren[0], leftChildren[1]);
            if (partition != null) {
                runningProbability[0] *= partition.getCCP();
            } else {
                runningProbability[0] = 0;
                return null;
            }
        }
        if (!rightVertex.isLeaf()) {
            CladePartition partition = rightClade.getCladePartition(rightChildren[0], rightChildren[1]);
            if (partition != null) {
                runningProbability[0] *= partition.getCCP();
            } else {
                runningProbability[0] = 0;
                return null;
            }
        }

        return new ExtendedClade[]{leftClade, rightClade};
    }

    /* Helper method */
    private ExtendedClade[] computeProbabilityOfChildren(Node vertex, BitSet cladeInBits, double[] runningProbability) {
        ExtendedClade[] leftChildren = null;
        if (vertex.isLeaf()) {
            int index = vertex.getNr();
            cladeInBits.set(index);
        } else {
            leftChildren = computeProbabilityOfVertices(vertex.getChild(0),
                    vertex.getChild(1), runningProbability);

            if (leftChildren == null) {
                return null;
            }

            cladeInBits.or(leftChildren[0].getCladeInBits());
            cladeInBits.or(leftChildren[1].getCladeInBits());
        }

        return leftChildren;
    }


    /* -- OTHER METHODS -- */

    @Override
    public String toString() {
        return "CCD1 " + super.toString();
    }

    @Override
    public AbstractCCD copy() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected double getNumberOfParameters() {
        return this.getNumberOfCladePartitions();
    }

}
