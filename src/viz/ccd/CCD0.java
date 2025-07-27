package viz.ccd;




import java.io.File;
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
import java.util.stream.Stream;

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

    public static final boolean USE_CLADE_PARAMETERS = true;

    /** Whether expand should use the monophyletic clade speedup. */
    private boolean useMonophyleticCladeSpeedup = false;

    /** Whether expand should run update online (instead of from scratch). */
    private boolean updateOnline = false;

    /**
     * Whether CCD should be treated as CCD0 and tidying up should reinitialize
     * (expand + set CCPs) as during construction.
     */
    private boolean allowReinitializing = true;

    /** Clades added since last expand (for online expand). */
    private List<Clade> newClades = null;

    /** Clades organized by size for more efficient expand method. */
    private List<List<Clade>> cladeBuckets = null;

    /**
     * from[i][j] is the first clade in cladeBuckets[i] that has bit j set.
     * to[i][j] is the last clade in cladeBuckets[i] that has bit j set.
     * To test if cladeBuckets[i] contains sub-clades of some clade C
     * only clades cladeBuckets[i][from[i][first set bit in C]] up to and including
     * cladeBuckets[i][to[i][last set bit in C]] need to be checked.
     * All others have at least one taxon outside C: the clades below from[i][]
     * have a taxon below any taxon in C and the clades above to[i][] have one above.
     */
    private int[][] from, to;

    /** Clades already processed */
    private Set<Clade> done;

    /** Stream to report on progress of CCD0 construction. */
    private PrintStream progressStream = System.out;

    /** Progress counted of clades handled in the expand step. */
    private int progressed = 0;

    /**
     * If given, only (number of taxa) times this factor of clades are considered
     * when expanding the CCD1 graph. This speeds-up the costly expansion for large
     * datasets but leads to an approximation.
     */
    private int maxExpansionFactor = -1;

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
     * @param treeSet an iterable set of trees, which contains no burnin trees,
     *                whose distribution is approximated by the resulting
     *                {@link CCD0}; all of its trees are used
     */
//    public CCD0(TreeSet treeSet) {
//        this(treeSet, false);
//    }

    /**
     * Constructor for a {@link CCD0} based on the given collection of trees
     * (not containing any burnin trees).
     *
     * @param treeSet       an iterable set of trees, which contains no burnin trees,
     *                      whose distribution is approximated by the resulting
     *                      {@link CCD0}
     * @param numTreesToUse the number of trees to use from the treeSet
     */
//    public CCD0(TreeSet treeSet, int numTreesToUse) {
//        this(treeSet, numTreesToUse, false);
//    }

    /**
     * Constructor for a {@link CCD0} based on the given collection of trees
     * (not containing any burnin trees).
     *
     * @param treeSet        an iterable set of trees, which contains no burnin trees,
     *                       whose distribution is approximated by the resulting
     *                       {@link CCD0}; all of its trees are used
     * @param storeBaseTrees whether to store the trees used to create this CCD
     */
//    public CCD0(TreeSet treeSet, boolean storeBaseTrees) {
//        this(treeSet, treeSet.totalTrees - treeSet.burninCount, storeBaseTrees);
//    }

    /**
     * Constructor for a {@link CCD0} based on the given collection of trees
     * (not containing any burnin trees).
     *
     * @param treeSet        an iterable set of trees, which contains no burnin trees,
     *                       whose distribution is approximated by the resulting
     *                       {@link CCD0}
     * @param numTreesToUse  the number of trees to use from the treeSet
     * @param storeBaseTrees whether to store the trees used to create this CCD
     */
//    public CCD0(TreeSet treeSet, int numTreesToUse, boolean storeBaseTrees) {
//        super(treeSet, numTreesToUse, storeBaseTrees);
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
     * Constructor for a {@link CCD0} based on the given collection of trees
     * (not containing any burnin trees) with the given flags.
     *
     * @param treeSet            an iterable set of trees, which contains no burnin trees,
     *                           whose distribution is approximated by the resulting
     * @param storeBaseTrees     whether to store the trees used to create this CCD
     *                           {@link CCD0}
     * @param maxExpansionFactor
     */
//    public CCD0(TreeSet treeSet, boolean storeBaseTrees, int maxExpansionFactor) {
//        super(treeSet, storeBaseTrees);
//        this.maxExpansionFactor = maxExpansionFactor;
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
     * Constructor for an empty CDD. Trees can then be processed one by one that sets .
     *
     * @param numLeaves          number of leaves of the trees that this CCD will be based on
     * @param storeBaseTrees     whether to store the trees used to create this CCD;
     *                           recommended not to when huge set of trees is used
     * @param maxExpansionFactor
     */
    public CCD0(int numLeaves, boolean storeBaseTrees, int maxExpansionFactor) {
        super(numLeaves, storeBaseTrees);
        this.maxExpansionFactor = maxExpansionFactor;
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

    /** Forbids reinitializing of CCD0 (no expand and resetting CCPs). */
    public void forbidReinitializing() {
        this.allowReinitializing = false;
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
    protected boolean removeCladePartitionIfNecessary(Clade clade, CladePartition partition) {
        // nothing to do here for CCD0s
        // since we keep partitions even if they have no occurrence counts
        return false;
    }

    /**
     * Set up this CCD0 after adding/removing trees. Assumes reset/not-set
     * values in CCD graph.
     */
    @Override
    public void initialize() {
        if (!allowReinitializing) {
            // this.dirtyStructure = false;
            return;
        }

        // need to find all clade partitions that could exist but were not
        // observed in base trees
        if (!updateOnline || (cladeBuckets == null)) {
            // out.print("Initializing CCD0 parameters ... ");
//        	try {
//            	maxExpansionFactor = 0;
//	        	PrintStream out = new PrintStream(new File("/tmp/expand.dat"));
//	        	do {
//	        		maxExpansionFactor++;
//	        		long start = System.currentTimeMillis();
//	        		expand();        		
//		    		long end = System.currentTimeMillis();
//		        	out.print(maxExpansionFactor + ",");
//		        	for (int i = 0; i < cladeBuckets.size(); i++) {
//		        		out.print(cladeBuckets.get(i).size()+",");
//		        	}
//		        	out.println(end-start);
//	        	} while (maxExpansionFactor * getNumberOfLeaves() < cladeMapping.values().size());
//        	} catch (Exception e ) {
//        		e.printStackTrace();
//        	}
            expand();
        } else {
            // out.print("\nExpanding CCD graph (online) ... ");
            expandOnline();
        }

        // then need to set clade partition probabilities
        // which normalizes the product of clade probabilities
        // out.print("setting probabilities ... ");
        try {
            setPartitionProbabilities(this.rootClade);
        } catch (UnderflowException exception) {
            System.err.println("An underflow was detected. We switch to log space.");
            this.resetSumCladeCredibilities();
            setPartitionLogProbabilities(this.rootClade);
        }

        // out.println(" ...done.");
        if (updateOnline) {
            newClades.clear();
        }
        this.dirtyStructure = false;
        super.setCacheAsDirty();
        this.probabilitiesDirty = false;
    }

    /**
     * Expand CCD graph with clade partitions where parent and children were
     * observed, but not that clade partition.
     */
    protected void expand() {
        // long start = System.currentTimeMillis();
        if (this.maxExpansionFactor == 0) {
            return;
        }

        Stream<Clade> cladesToExpand = cladeMapping.values().stream();

        // 1. we only expand the most frequent clades if necessary
        if (this.maxExpansionFactor != -1) {
            int numCladesToConsider = this.maxExpansionFactor * this.getNumberOfLeaves();
            cladesToExpand = cladesToExpand.sorted(
                    Comparator.comparingInt(x -> -x.getNumberOfOccurrences())
            ).limit(numCladesToConsider);
        }

        // 2. sort clades
        List<Clade> clades = cladesToExpand.sorted(Comparator.comparingInt(x -> x.size())).toList();
        if ((progressStream != null) && verbose) {
            progressStream.println("Expanding CCD0: processing " + clades.size() + " clades");
            if (clades.size() > 100000 && maxExpansionFactor == -1) {
                progressStream.println("If this takes too long, consider using Approximated CCD0 instead.");
                progressStream.println("This generally runs faster and gives reasonably good point estimates.");
            }
        }

        // 3. clade buckets
        // for easier matching of child clades, we want to group them by size
        cladeBuckets = processCladeBuckets(clades, leafArraySize);

        // 4. find missing clade partitions
        done = new HashSet<>();
        threadCount = Runtime.getRuntime().availableProcessors();
        if (threadCount <= 1 || clades.size() < NUM_CLADES_PARALLELIZATION_THRESHOLD
                || updateOnline) {
            threadCount = 1;
            findChildPartitions(clades);
        } else {
            try {
                if ((progressStream != null) && verbose) {
                    progressStream.println("Running expand step with " + threadCount + " threads.");
                }
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

        if ((progressStream != null) && verbose) {
            progressStream.println("... done.");
        }
        // Log.warning("Expanded CCD0 in " + (end - start) / 1000 + " seconds.");
        progressStream = null;

        // release memory
        from = null;
        to = null;
        // long end = System.currentTimeMillis();
        // System.err.println("Expanded in " + (end-start) + " ms");
    }


    /**
     * set up clade buckets so that
     * 1. each bucket contains clades of the same size
     * 2. clades are sorted by first set bit, then last set bit
     * 3. as a side effect, the from[][] and to[][] arrays are initialised
     *
     * @param clades        set of clades to distribute into clade buckets
     * @param leafArraySize = maximum clade size
     * @return clade buckets
     */
    private List<List<Clade>> processCladeBuckets(List<Clade> clades, int leafArraySize) {
        List<List<Clade>> cladeBuckets = new ArrayList<>(leafArraySize);

        // 3.i init clade buckets
        for (int i = 0; i < leafArraySize; i++) {
            cladeBuckets.add(new ArrayList<Clade>());
        }

        // 3.ii fill clade buckets
        for (Clade clade : clades) {
            cladeBuckets.get(clade.size() - 1).add(clade);
        }

        // 3.iii sort buckets by first set bit, then last set bit -- ignore intermediate bits
        for (int i = 0; i < leafArraySize; i++) {
            cladeBuckets.get(i).sort((o1, o2) -> {
                final BitSet b1 = o1.getCladeInBits();
                final BitSet b2 = o2.getCladeInBits();
                final int firstBit1 = b1.nextSetBit(0);
                final int firstBit2 = b2.nextSetBit(0);
                if (firstBit1 < firstBit2) return -1;
                if (firstBit1 > firstBit2) return 1;
                final int lastBit1 = b1.lastSetBit();
                final int lastBit2 = b2.lastSetBit();
                if (lastBit1 < lastBit2) return -1;
                if (lastBit1 > lastBit2) return 1;
                return 0;
            });
        }

        // 3.iv calculate from and to ranges
        from = new int[leafArraySize][leafArraySize];
        to = new int[leafArraySize][leafArraySize];
        for (int i = 0; i < leafArraySize; i++) {
            List<Clade> bucket = cladeBuckets.get(i);
            if (bucket.size() > 0) {
                int j = 0;
                int[] fromi = from[i];
                int[] toi = to[i];
                for (int c = 0; c < bucket.size(); c++) {
                    Clade clade = bucket.get(c);
                    int min = clade.getCladeInBits().nextSetBit(0);
                    while (j <= min) {
                        fromi[j++] = c;
                    }
                    int max = clade.getCladeInBits().lastSetBit();
//	        		if (max < 0 || max >= toi.length) {
//	        			max = clade.getCladeInBits().lastSetBit();
//	        		}
                    toi[max] = c;
                }
                while (j < leafArraySize) {
                    fromi[j] = fromi[j - 1];
                    j++;
                }

                toi[leafArraySize - 1] = bucket.size() - 1;
                for (int k = leafArraySize - 2; k >= 0; k--) {
                    if (toi[k] == 0) {
                        toi[k] = toi[k + 1];
                    }
                }
                for (int k = 1; k < leafArraySize; k++) {
                    toi[k] = Math.max(toi[k - 1], toi[k]);
                }
            }
        }
        return cladeBuckets;
    }

    /**
     * Like {@link CCD0#expand()}, expand CCD graph with clade partitions where parent (considering only new clades) and children were
     * observed, but not that clade partition.
     */
    protected void expandOnline() {
        // out.println("expand online (num trees total = " + this.getNumberOfBaseTrees() + ")");

        // 0. take out clades that have no occurrences left
        // and do nothing if no new clades remain
        List<Clade> emptyClades = newClades.stream().filter(x -> (x.getNumberOfOccurrences() != 0)).toList();
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
            if ((progressStream != null) && verbose) {
                while (progressed < (i * 61 / parentClades.size())) {
                    progressStream.print(".");
                    progressed++;
                }
            }
            i++;
        }

        if ((progressStream != null) && verbose) {
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
        int parentSize = parent.size();
        int min = parent.getCladeInBits().nextSetBit(0);
        int max = parent.getCladeInBits().lastSetBit();

        for (int j = 1; j <= parentSize / 2; j++) {
            // every clade split has a smaller child with size k_small and a larger child with
            // size k_large such that k_small <= parent.size() / 2 < k_large
            // process the smallest bucket with one of these
            // int bucketIndex = cladeBuckets.get(j - 1).size() - from[j-1][min] < cladeBuckets.get(parentSize - j - 1).size() - from[parentSize -j-1][min]? j-1 : parentSize -j-1;
            int bucketIndex = to[j - 1][max] - from[j - 1][min] < to[parentSize - j - 1][max] - from[parentSize - j - 1][min] ? j - 1 : parentSize - j - 1;
            List<Clade> bucket = cladeBuckets.get(bucketIndex);
            if (bucket.size() > 0) {
                final int start = from[bucketIndex][min];
                final int end = to[bucketIndex][max];
                for (int i = start; i <= end; i++) {
                    Clade child = bucket.get(i);
                    if (done.contains(child)) {
                        continue;
                    }

                    BitSet childBits = child.getCladeInBits();
                    findPartitionHelper(child, parent, helperBits, parentBits, childBits);
                }
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

    /* Thread worker for embarrassingly parallelizing parts of the expand step */
    class ExpandWorker implements java.lang.Runnable {
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
                        while (progressed < (i * 61 / clades.size())) {
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
     * Recursively computes, sets, and returns the log probabilities of all clade partitions based on the clade credibilities.
     * Method only needs to be called when a CCD0 was constructed manually,
     * e.g. by the {@link  ccp.algorithms.CCDCombiner}.
     *
     * @param clade for which the clade partition probabilities are computed
     * @return the sum of this clade's partitions probabilities times its own credibility
     */
    private static double setPartitionLogProbabilities(Clade clade) {
        if (clade.getLogSumCladeCredibilities() < 0) {
            return clade.getLogSumCladeCredibilities();
        }

        double logCladeValue = Math.log(clade.getCladeCredibility());

        if (clade.isLeaf()) {
            // a leaf has no partition, sum of probabilities is 1
            clade.setLogSumCladeCredibilities(0);
            return 0.0;

        } else if (clade.isCherry()) {
            // a cherry has only one partition
            if (clade.partitions.isEmpty()) {
                throw new AssertionError("Cherry should contain a clade split.");
            }
            clade.partitions.get(0).setCCP(1);
            clade.setLogSumCladeCredibilities(logCladeValue);
            return logCladeValue;

        } else {
            // other might have more partitions
            double sumSubtreeProbabilities = 0.0;
            double[] sumPartitionSubtreeLogProbabilities = new double[clade.getPartitions().size()];

            // compute log of sum of probabilities over all partitions ...
            // with log-sum-exp trick
            double max = Double.NEGATIVE_INFINITY;
            int i = 0;
            for (CladePartition partition : clade.getPartitions()) {
                sumPartitionSubtreeLogProbabilities[i] =
                        setPartitionLogProbabilities(partition.getChildClades()[0])
                                + setPartitionLogProbabilities(partition.getChildClades()[1]);
                max = Math.max(max, sumPartitionSubtreeLogProbabilities[i]);
                i++;
            }
            i = 0;
            for (CladePartition partition : clade.getPartitions()) {
                sumSubtreeProbabilities += Math.exp(sumPartitionSubtreeLogProbabilities[i] - max);
                i++;
            }
            double sumSubtreeLogProbabilities = max + Math.log(sumSubtreeProbabilities);

            // ... and then normalize
            i = 0;
            for (CladePartition partition : clade.getPartitions()) {
                double logProbability = sumPartitionSubtreeLogProbabilities[i] - sumSubtreeLogProbabilities;
                partition.setCCP(Math.exp(logProbability));
                i++;
            }

            // sumSubtreeLogProbabilities can be a very tiny positive number due to numerical issues
            // (when it should be log(1) = 0)
            // because we want non-positive log probabilities, round it down to 0 in this case
            if (1e-12 < sumSubtreeLogProbabilities) {
                throw new AssertionError("Negative probability detected.");
            } else {
                sumSubtreeLogProbabilities = Math.min(sumSubtreeLogProbabilities, 0.0);
            }

            // combined with probability of clade, we get sum of all subtree probabilities
            double sumLogCladeCredibilities = sumSubtreeLogProbabilities + logCladeValue;
            clade.setLogSumCladeCredibilities(sumLogCladeCredibilities);
            return sumLogCladeCredibilities;
        }
    }

    /**
     * Recursively computes, sets, and returns the probabilities of all clade partitions based on the clade credibilities.
     * Method only needs to be called when a CCD0 was constructed manually,
     * e.g. by the {@link  ccd.algorithms.CCDCombiner}.
     *
     * @param clade for which the clade partition probabilities are computed
     * @return the sum of this clade's partitions probabilities times its own credibility
     */
    public static void setPartitionProbabilities(Clade clade) {
        setPartitionProbabilities(clade, !USE_CLADE_PARAMETERS);
    }

    /**
     * Recursively computes, sets, and returns the probabilities of all clade partitions based on the clade credibilities.
     * Method only needs to be called when a CCD0 was constructed manually,
     * e.g. by the {@link  ccd.algorithms.CCDCombiner}.
     *
     * @param clade              for which the clade partition probabilities are computed
     * @param useCladeParameters whether to use the clade parameters or the clade credibilities
     * @return the sum of this clade's partitions probabilities times its own credibility
     */
    public static double setPartitionProbabilities(Clade clade, boolean useCladeParameters) {
        if (clade.getSumCladeCredibilities() > 0) {
            return clade.getSumCladeCredibilities();
        }

        double cladeValue = /*useCladeParameters ? clade.getCladeParameter() :*/ clade.getCladeCredibility();

        if (clade.isLeaf()) {
            // a leaf has no partition, sum of probabilities is 1
            clade.setSumCladeCredibilities(1);
            return 1.0;
        } else if (clade.isCherry()) {
            // a cherry has only one partition
            if (clade.partitions.isEmpty()) {
                throw new AssertionError("Cherry should contain a clade split.");
            }
            clade.partitions.get(0).setCCP(1);
            clade.setSumCladeCredibilities(cladeValue);
            return cladeValue;
        } else {
            // other might have more partitions
            double sumSubtreeProbabilities = 0.0;
            double[] sumPartitionSubtreeProbabilities = new double[clade.getPartitions().size()];

            // compute sum of probabilities over all partitions ...
            int i = 0;
            for (CladePartition partition : clade.getPartitions()) {
                sumPartitionSubtreeProbabilities[i] =
                        setPartitionProbabilities(partition.getChildClades()[0], useCladeParameters)
                                * setPartitionProbabilities(partition.getChildClades()[1], useCladeParameters);
                sumSubtreeProbabilities += sumPartitionSubtreeProbabilities[i];
                i++;
            }

            // ... and then normalize
            if (sumSubtreeProbabilities == 0) {
                // probability of this subtree is so small, that we have an underflow problem;
                // we can try to use log transformed probabilities to set CCPs
                // but the probability of the clade will still become zero

                // try log-sum-exp trick
                double logMax = Double.NEGATIVE_INFINITY;
                double[] logProbs = new double[clade.getPartitions().size()];
                i = 0;
                for (CladePartition partition : clade.getPartitions()) {
                    double left = partition.getChildClades()[0].getSumCladeCredibilities();
                    double right = partition.getChildClades()[1].getSumCladeCredibilities();
                    logProbs[i] = Math.log(left) + Math.log(right);
                    logMax = Math.max(logProbs[i], logMax);

                    // if (Double.isNaN(logProbs[i]) || Double.isInfinite(logProbs[i])) {
                    //     out.println("XXXXXXXXX");
                    //     out.println("logProbs[i] = " + logProbs[i]);
                    //     out.println("left = " + left);
                    //     out.println("log left = " + Math.log(left));
                    //     out.println("right = " + left);
                    //     out.println("log right = " + Math.log(right));
                    // }

                    i++;
                }

                // log sum = log pMax - log( sum exp(log pi - logMax))
                double intermSum = 0;
                for (int j = 0; j < logProbs.length; j++) {
                    if (Double.isFinite(logProbs[j])) {
                        intermSum += Math.exp(logProbs[j] - logMax);
                    }
                }
                double logSum = logMax - Math.log(intermSum);

                // normalizing with normalized log pi = log pi - log sum
                i = 0;
                double pSum = 0;
                for (CladePartition partition : clade.getPartitions()) {
                    if (!Double.isFinite(logProbs[i])) {
                        // we are still encountering underflows
                        throw new UnderflowException("An underflow has occurred.");
                    } else {
                        double logProbability = logProbs[i] - logSum;
                        double probability = Math.exp(logProbability);
                        pSum += probability;

                        if (Double.isNaN(probability)) {
                            out.println("NaN probability = " + probability);
                            out.println("logProbability = " + logProbability);
                            out.println("logProbs[i] = " + logProbs[i]);
                            out.println("logSum = " + logSum);
                        }

                        partition.setCCP(probability);
                    }
                    i++;
                }
            } else {
                i = 0;
                for (CladePartition partition : clade.getPartitions()) {
                    double probability = sumPartitionSubtreeProbabilities[i] / sumSubtreeProbabilities;
                    if (Double.isNaN(probability)) {
                        out.println("clade = " + clade);
                        out.println("partition = " + partition);
                        out.println("sumPartitionSubtreeProbabilities = " + sumPartitionSubtreeProbabilities[i]);
                        out.println("sumSubtreeProbabilities = " + sumSubtreeProbabilities);
                    }
                    partition.setCCP(probability);
                    i++;
                }
            }

            // combined with probability of clade, we get sum of all subtree probabilities
            double sumCladeCredibilities = sumSubtreeProbabilities * cladeValue;
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
//    public Tree getMSCCTree() {
//        return this.getMSCCTree(HeightSettingStrategy.None);
//    }

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
//    public Tree getMSCCTree(HeightSettingStrategy heightStrategy) {
//        return getTreeBasedOnStrategy(SamplingStrategy.MaxSumCladeCredibility, heightStrategy);
//    }

    /* -- OTHER METHODS -- */
    @Override
    public AbstractCCD copy() {
        CCD0 copy = new CCD0(this.getSizeOfLeavesArray(), false);
        copy.baseTrees.add(this.getSomeBaseTree());
        copy.numBaseTrees = this.getNumberOfBaseTrees();

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
    public double getNumberOfParameters() {
        return this.getNumberOfClades();
    }

}
