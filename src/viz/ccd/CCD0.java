package viz.ccd;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import viz.Node;
import viz.TreeData;

/**
 * This class represents a tree distribution using the CCD graph (via the parent
 * class {@link AbstractCCD}). Tree probabilities are set proportional to the
 * product of Monte Carlo probabilities of the samples used to construct this
 * CCD0 but normalized to have a tree distribution.
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
 * The MAP tree of this distribution is the tree with highest product of clade
 * credibility. Furthermore, with {@link CCD0#getMSCCTree()} the tree with
 * highest sum of clade credibility can be retrieved.
 * </p>
 *
 * @author Jonathan Klawitter
 */
public class CCD0 extends AbstractCCD {

    /** Whether expand should use the monophyletic clade speedup. */
    private boolean useMonophyleticCladeSpeedup = false;

    /** Whether expand should run update online (instead of from scratch). */
    private boolean updateOnline = false;

    /** Clades added since last expand (for online expand). */
    private List<Clade> newClades = null;

    /** Clades organized by size for more efficient expand method. */
    private List<Set<Clade>> cladeBuckets = null;

    /** Clades already processed */
    private Set<Clade> done;

    /** Stream to report on progress of CCD0 construction. */
    private PrintStream progressStream;

    /** Progress counted of clades handled in the expand step. */
    private int progressed = 0;

    // variables for parallelization
    /** Threshold of number of clades on whether to use parallelization for expand. */
    public static final int NUM_CLADES_PARALLELIZATION_THRESHOLD = 20000;

    /** Number of worker threads used for the expand step. */
    private int threadCount = 1;
    private CountDownLatch countDown = null;

    /* -- CONSTRUCTORS & CONSTRUCTION METHODS -- */

    /**
     * Constructor for a {@link CCD0} based on the given collection of trees
     * with specified burn-in.
     *
     * @param trees  the trees whose distribution is approximated by the resulting
     *               {@link CCD0}
     * @param burnin value between 0 and 1 of what percentage of the given trees
     *               should be discarded as burn-in
     */
    public CCD0(List<Tree> trees, double burnin) {
        super(trees, burnin);
        initialize();
    }

    /**
     * Constructor for a {@link CCD0} based on the given collection of trees
     * (not containing any burnin trees).
     *
     * @param treeSet        an iterable set of trees, which contains no burnin trees,
     *                       whose distribution is approximated by the resulting
     *                       {@link CCD0}
     * @param storeBaseTrees whether to store the trees used to create this CCD
     */
//    public CCD0(TreeSet treeSet, boolean storeBaseTrees) {
//        super(treeSet, storeBaseTrees);
////        System.out.println("xx");
//        initialize();
//    }

    /**
     * Constructor for a {@link CCD0} based on the given collection of trees
     * (not containing any burnin trees) wit the given flags.
     *
     * @param treeSet                     an iterable set of trees, which contains no burnin trees,
     *                                    whose distribution is approximated by the resulting
     *                                    {@link CCD0}
     * @param storeBaseTrees              whether to store the trees used to create this CCD
     * @param useMonophyleticCladeSpeedup try to speed up expand step by considering monophyletic clades
     *                                    (mutually exclusive with online speedup)
     * @param updateOnline                speed up expand step by using an online version;
     *                                    online useful if more trees are added later
     *                                    and CCD gets reinitialized repeatedly
     *                                    (mutually exclusive with monophyletic clades speedup)
     */
//    public CCD0(TreeSet treeSet, boolean storeBaseTrees, boolean useMonophyleticCladeSpeedup, boolean updateOnline) {
//        super(treeSet, storeBaseTrees);
//        if (useMonophyleticCladeSpeedup) {
//            this.setToUseMonophyleticCladeSpeedup();
//        }
//        if (updateOnline) {
//            this.setToUpdateOnline();
//        }
//        initialize();
//    }

    /**
     * Constructor for an empty CDD. Trees can then be processed one by one.
     *
     * @param numLeaves      number of leaves of the trees that this CCD will be based on
     * @param storeBaseTrees whether to store the trees used to create this CCD;
     *                       recommended not to when huge set of trees is used
     */
    public CCD0(int numLeaves, boolean storeBaseTrees) {
        super(numLeaves, storeBaseTrees);
    }

    /**
     * Configure this CCD0 to try to speed up the expand step
     * by looking for monophyletic clades.
     */
    public void setToUseMonophyleticCladeSpeedup() {
        if (this.useMonophyleticCladeSpeedup) {
            System.err.println("Cannot use monophyletic clade speedup " +
                    "when online speedup is already used for the CCD0 expand step.");
            return;
        }
        this.useMonophyleticCladeSpeedup = true;
    }

    /**
     * Configure this CCD0 to run the expand step online,
     * meaning clades added since last expand are stored
     * and only expands containing them are checked.
     * This is mutually exclusive to using the monophyletic clades speedup.
     */
    public void setToUpdateOnline() {
        if (this.useMonophyleticCladeSpeedup) {
            System.err.println("Cannot use online speedup when monophyletic clade speedup " +
                    "is already used for the CCD0 expand step.");
            return;
        }
        this.updateOnline = true;
        this.newClades = new ArrayList<>();
    }

    /**
     * @param progressStream stream used to report on construction
     *                       (adding trees and expand steps)
     */
    public void setProgressStream(PrintStream progressStream) {
        this.progressStream = progressStream;
    }

    /** Whether a progress stream has been set. */
    private boolean hasProgressStream() {
        return (progressStream != null);
    }

    @Override
    protected Clade addNewClade(BitSet cladeInBits) {
        Clade clade = super.addNewClade(cladeInBits);
        if (updateOnline) {
            this.newClades.add(clade);
        }
        this.dirtyStructure = true;

        return clade;
    }


    /* -- STATE MANAGEMENT & INITIALIZATION -- */

    /**
     * Whether the CCD graph is expanded and CCPs renormalized.
     */
    private boolean dirtyStructure = true;

    @Override
    public void setCacheAsDirty() {
        super.setCacheAsDirty();
        this.dirtyStructure = true;
    }

    @Override
    protected void tidyUpCacheIfDirty() {
        if (dirtyStructure) {
            super.resetCache();
            this.initialize();
        } else {
            super.resetCacheIfProbabilitiesDirty();
        }
    }

    @Override
    protected void checkCladePartitionRemoval(Clade clade, CladePartition partition) {
        // nothing to do here for CCD0s
        // since we keep partitions even if they have no occurrence counts
    }

    /**
     * Set up this CCD0 after adding/removing trees. Assumes reset/not-set
     * values in CCD graph.
     */
    @Override
    public void initialize() {
        // need to find all clade partitions that could exist but were not
        // observed in base trees
        if (!updateOnline || (cladeBuckets == null)) {
            // System.out.print("Initializing CCD0 parameters ... ");
            expand();
        } else {
            // System.out.print("\nExpanding CCD graph (online) ... ");
            expandOnline();
        }

        // then need to set clade partition probabilities
        // which normalizes the product of clade probabilities
        // System.out.print("setting probabilities ... ");
        setPartitionProbabilities(this.rootClade);

        // System.out.println(" ...done.");
        if (updateOnline) {
            newClades.clear();
        }
        this.dirtyStructure = false;
        super.setCacheAsDirty();
    }

    /**
     * Expand CCD graph with clade partitions where parent and children were
     * observed, but not that clade partition.
     */
    private void expand() {
        long start = System.currentTimeMillis();

        // 1. sort clades
        Object [] clades2 = cladeMapping.values().stream().sorted(Comparator.comparingInt(x -> x.size())).toArray();
        List<Clade> clades = new ArrayList<>();
        for (Object o : clades2) {
        	clades.add((Clade) o);
        }
        if (hasProgressStream()) {
            progressStream.println("Expanding CCD0: processing " + clades.size() + " clades");
        }

        // 2. clade buckets
        // for easier matching of child clades, we want to group them by size
        // 2.i init clade buckets
        cladeBuckets = new ArrayList<Set<Clade>>(leafArraySize);
        for (int i = 0; i < leafArraySize; i++) {
            cladeBuckets.add(new HashSet<Clade>());
        }
        // 2.ii fill clade buckets
        for (Clade clade : clades) {
            cladeBuckets.get(clade.size() - 1).add(clade);
        }

        // 3. find missing clade partitions
        done = new HashSet<>();
        threadCount = Runtime.getRuntime().availableProcessors();
        if (threadCount <= 1 || clades.size() < NUM_CLADES_PARALLELIZATION_THRESHOLD
                || updateOnline) {
            threadCount = 1;
            findChildPartitions(clades);
        } else {
            try {
                System.out.println("Running expand step with " + threadCount + " threads.");
                countDown = new CountDownLatch(threadCount);
                ExecutorService exec = Executors.newFixedThreadPool(threadCount);
                int end = clades.size();
                for (int i = 0; i < threadCount; i++) {
                    ExpandWorker coreRunnable = new ExpandWorker(clades, i, end);
                    exec.execute(coreRunnable);
                }
                countDown.await();
                exec.shutdownNow();
            } catch (RejectedExecutionException | InterruptedException e) {
                // do nothing
            }
            done.clear();
        }

        // System.out.println("done.");
        long end = System.currentTimeMillis();
        // Log.warning("Expanded CCD0 in " + (end - start) / 1000 + " seconds.");
        progressStream = null;
    }

    /**
     * Like {@link CCD0#expand()}, expand CCD graph with clade partitions where parent (considering only new clades) and children were
     * observed, but not that clade partition.
     */
    private void expandOnline() {
        // System.out.println("expand online (num trees total = " + this.getNumberOfBaseTrees() + ")");

        // 0. take out clades that have no occurrences left
        // and do nothing if no new clades remain
        Object [] emptyClades2 = cladeMapping.values().stream().sorted(Comparator.comparingInt(x -> x.size())).toArray();
        List<Clade> emptyClades = new ArrayList<>();
        for (Object o : emptyClades2) {
        	emptyClades.add((Clade) o);
        }
        newClades.removeAll(emptyClades);
        if (newClades.isEmpty()) {
            return;
        }

        // 1. sort clades
        newClades.sort(Comparator.comparingInt(x -> x.size()));

        // 2. update clade buckets
        for (Clade clade : newClades) {
            cladeBuckets.get(clade.size() - 1).add(clade);
        }

        // 3.i check for clade partitions of new clades
        findChildPartitions(newClades);

        // 3.ii check if new clades can form new clade splits
        findParentPartitions(newClades);
    }

    /**
     * Find and add clade partitions for each of the given clades
     * that can be formed by pairs of child clades and that were not in any of the base trees.
     *
     * @param parentClades for which we search for new clade partitions
     */
    private void findChildPartitions(List<Clade> parentClades) {
        BitSet helperBits = BitSet.newBitSet(parentClades.get(0).getCCD().getSizeOfLeavesArray());

        // we go through clades in increasing size, then check for each clade of
        // at most half potential parent's size, whether we can find a partner
        int progressed = 0;
        int i = 0;
        for (Clade parent : parentClades) {
            findChildPartitionsOf(parent, helperBits);
            if (progressStream != null) {
                while (progressed < i * 61 / parentClades.size()) {
                    progressStream.print("*");
                    progressed++;
                }
            }
            i++;
        }

        if (progressStream != null) {
            progressStream.println();
        }
    }

    /* Helper method - do the work for one particular clade */
    private void findChildPartitionsOf(Clade parent, BitSet helperBits) {
        // we skip leaves and cherries as they have no/only one partition
        if (parent.isLeaf() || parent.isCherry()) {
            return;
        }

        BitSet parentBits = parent.getCladeInBits();

        // otherwise we check if we find a larger partner clade for any
        // smaller clade that together partition the parent clade;
        for (int j = 1; j <= parent.size() / 2; j++) {
            for (Clade smallChild : cladeBuckets.get(j - 1)) {
                if (done.contains(smallChild)) {
                    continue;
                }

                BitSet smallChildBits = smallChild.getCladeInBits();
                findPartitionHelper(smallChild, parent, helperBits, parentBits, smallChildBits);
            }
        }

        // remove clades below monophyletic clades
        if (useMonophyleticCladeSpeedup && parent.isMonophyletic()) {
            if (threadCount <= 1) {
                Set<Clade> descendants = parent.getDescendantClades(true);
                for (Clade descendant : descendants) {
                    cladeBuckets.get(descendant.size() - 1).remove(descendant);
                }
            } else {
                Set<Clade> descendants;
                try {
                    descendants = parent.getDescendantClades(true);
                } catch (ConcurrentModificationException e) {
                    try {
                        Thread.sleep(100);
                        descendants = parent.getDescendantClades(true);
                    } catch (Throwable e2) {
                        descendants = new HashSet<>();
                    }
                }
                for (Clade descendant : descendants) {
                    synchronized (this) {
                        done.add(descendant);
                    }
                }
            }
        }
    }

    /**
     * Find and add clade partitions where each of the given clades is a child clade
     * and that were not in any of the base trees.
     *
     * @param childClades for which we search for new clade partitions
     */
    private void findParentPartitions(List<Clade> childClades) {
        BitSet helperBits = BitSet.newBitSet(childClades.get(0).getCCD().getSizeOfLeavesArray());

        // we go through clades in increasing size, then check for each clade of
        // at least child's size, whether it can be a parent and if there is a partner
        for (Clade child : childClades) {
            BitSet childBits = child.getCladeInBits();

            for (int j = child.size() + 1; j <= leafArraySize; j++) {
                for (Clade parent : cladeBuckets.get(j - 1)) {
                    BitSet parentBits = parent.getCladeInBits();

                    findPartitionHelper(child, parent, helperBits, parentBits, childBits);
                }
            }
        }
    }

    /* Helper method */
    private void findPartitionHelper(Clade child, Clade parent, BitSet helperBits, BitSet parentBits, BitSet childBits) {
        // check whether child clade is contained in parent clade
        helperBits.clear();
        helperBits.or(parentBits);
        helperBits.and(childBits);

        if (helperBits.equals(childBits)
                && !child.parentClades.contains(parent)) {
            // here helperBits equal childBits, so with an XOR
            // with the parentBits we get the bits of the potential partner clade
            helperBits.xor(parentBits);
            Clade otherChild = cladeMapping.get(helperBits);
            if (otherChild != null) {
                if (threadCount > 1) {
                    synchronized (this) {
                        parent.createCladePartition(child, otherChild);
                    }
                } else {
                    parent.createCladePartition(child, otherChild);
                }
            }
        }
    }

    /* Thread worker for embarssingly parallezing parts of the expand step */
    class ExpandWorker implements Runnable {
        private List<Clade> clades;
        private int start;
        private int end;

        ExpandWorker(List<Clade> clades, int start, int end) {
            this.clades = clades;
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            int i = start;
            try {
                BitSet helperBits = BitSet.newBitSet(clades.get(0).getCCD().getSizeOfLeavesArray());
                while (i < end) {
                    findChildPartitionsOf(clades.get(i), helperBits);
                    i += threadCount;

                    if (progressStream != null) {
                        while (progressed < i * 61 / clades.size()) {
                            progressStream.print("*");
                            progressed++;
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            // progressStream.println("finished " + start + " " + i);
            countDown.countDown();
        }

    }

    /**
     * Recursively computes, sets, and returns the probabilities of all clade partitions based on the clade credibilities.
     * Method only needs to be called when a CCD0 was constructed manually,
     * e.g. by the {@link  ccp.algorithms.CCDCombiner}.
     *
     * @param clade for which the clade partition probabilities are computed
     * @return the sum of this clade's partitions probabilities times its own credibility
     */
    public static double setPartitionProbabilities(Clade clade) {
        if (clade.getSumCladeCredibilities() > 0) {
            return clade.getSumCladeCredibilities();
        }

        if (clade.isLeaf()) {
            // a leaf has no partition, sum of probabilities is 1
            return 1.0;
        } else if (clade.isCherry()) {
            // a cherry has only one partition
            if (clade.partitions.isEmpty()) {
                throw new AssertionError("Cherry should contain a clade split.");
            }
            clade.partitions.get(0).setCCP(1);
            clade.setSumCladeCredibilities(clade.getCladeCredibility());
            return clade.getCladeCredibility();

        } else {
            // other might have more partitions
            double sumSubtreeProbabilities = 0.0;
            double[] sumPartitionSubtreeProbabilities = new double[clade.getPartitions().size()];

            // compute sum of probabilities over all partitions ...
            int i = 0;
            for (CladePartition partition : clade.getPartitions()) {
                sumPartitionSubtreeProbabilities[i] =
                        setPartitionProbabilities(partition.getChildClades()[0])
                                * setPartitionProbabilities(partition.getChildClades()[1]);
                sumSubtreeProbabilities += sumPartitionSubtreeProbabilities[i];
                i++;
            }
            // ... and then normalize
            i = 0;
            for (CladePartition partition : clade.getPartitions()) {
                double probability;
                if (sumSubtreeProbabilities == 0) {
                    System.err.println("Sum of subtree probabilities for  partition is 0, which could result from an underflow;" +
                            "maybe not enough burnin used?");
                    System.err.println("- parent clade: " + clade);
                    System.err.println("- partition: " + partition);
                    probability = 0;
                } else {
                    probability = sumPartitionSubtreeProbabilities[i] / sumSubtreeProbabilities;
                }
                partition.setCCP(probability);
                i++;
            }

            // combined with probability of clade, we get sum of all subtree
            // probabilities
            double sumCladeCredibilities = sumSubtreeProbabilities * clade.getCladeCredibility();
            clade.setSumCladeCredibilities(sumCladeCredibilities);
            return sumCladeCredibilities;
        }
    }


    /* -- POINT ESTIMATE METHODS -- */

    /**
     * Returns the tree with maximum sum of clade probabilities (#occurrences /
     * #num trees) of all trees in this CCD. Note that the maximum is with
     * respect to the sum of clade credibilities, unlike the MAP tree or an MCC
     * tree, which uses the product.
     *
     * @return the tree with maximum sum of clade probabilities
     */
    public Node getMSCCTree(TreeData td) {
        return this.getMSCCTree(HeightSettingStrategy.None, td);
    }

    /**
     * Returns the tree with maximum sum of clade probabilities (#occurrences /
     * #num trees) of all trees in this CCD and heights set with the given
     * strategy. Note that the maximum is with respect to the sum of clade
     * credibilities, unlike the MAP tree or an MCC tree, which uses the
     * product. Returns the tree with maximum sum of clade credibilities
     * (#occurrences / #num trees) of all trees in this CCD and heights set with
     * the given strategy. Note that the maximum is with respect to the sum of
     * clade credibilities, unlike the MCC tree, which uses the product.
     *
     * @param heightStrategy the strategy used to set the heights of the tree vertices
     * @return the tree with maximum sum of clade probabilities
     */
    public Node getMSCCTree(HeightSettingStrategy heightStrategy, TreeData td) {
        return getTreeBasedOnStrategy(SamplingStrategy.MaxSumCladeCredibility, heightStrategy, td);
    }

    /* -- OTHER METHODS -- */
    @Override
    public AbstractCCD copy() {
        CCD0 copy = new CCD0(this.getSizeOfLeavesArray(), false);
        copy.baseTrees.add(this.getSomeBaseTree());

        AbstractCCD.buildCopy(this, copy);

        if (this.updateOnline) {
            copy.setToUpdateOnline();
        }
        if (this.useMonophyleticCladeSpeedup) {
            copy.setToUseMonophyleticCladeSpeedup();
        }
        copy.dirtyStructure = this.dirtyStructure;

        return copy;
    }

    @Override
    public String toString() {
        return "CCD0 " + super.toString();
    }

    @Override
    protected double getNumberOfParameters() {
        return this.getNumberOfClades();
    }

}
