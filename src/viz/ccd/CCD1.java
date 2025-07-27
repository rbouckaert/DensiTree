package viz.ccd;




import java.util.List;

/**
 * This class represents a tree distribution via the conditional clade
 * distribution (CCD) as defined by
 * <a href="https://doi.org/10.1093/sysbio/syt014">B. Larget, 2013</a>. Clade
 * partition probabilities (CCP) are set based on the frequency f(C1, C2) of the
 * clade partition {C1, C2} divided by the frequency f(C) of the parent clade C,
 * i.e. f(C1,C2)/f(C). The CCD graph and computations are then handled by the
 * parent class {@link AbstractCCD}.
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
 * The MAP tree of this distribution is the tree with highest CCP.
 * </p>
 *
 * @author Jonathan Klawitter
 */
public class CCD1 extends AbstractCCD {

    /* -- CONSTRUCTORS & CONSTRUCTION METHODS -- */

    /**
     * Constructor for a {@link CCD1} based on the given collection of trees
     * with specified burn-in.
     *
     * @param trees  the trees whose distribution is approximated by the resulting
     *               {@link CCD1}
     * @param burnin value between 0 and 1 of what percentage of the given trees
     *               should be discarded as burn-in
     */
    public CCD1(List<Tree> trees, double burnin) {
        super(trees, burnin);
    }

    /**
     * Constructor for a {@link CCD1} based on the given collection of trees
     * (not containing any burnin trees).
     *
     * @param treeSet an iterable set of trees, which contains no burnin trees,
     *                whose distribution is approximated by the resulting
     *                {@link CCD1}; all of its trees are used
     */
//    public CCD1(TreeSet treeSet) {
//        this(treeSet, false);
//    }

    /**
     * Constructor for a {@link CCD1} based on the given collection of trees
     * (not containing any burnin trees).
     *
     * @param treeSet       an iterable set of trees, which contains no burnin trees,
     *                      whose distribution is approximated by the resulting {@link CCD1}
     * @param numTreesToUse the number of trees to use from the treeSet
     */
//    public CCD1(TreeSet treeSet, int numTreesToUse) {
//        super(treeSet, numTreesToUse, false);
//    }

    /**
     * Constructor for a {@link CCD1} based on the given collection of trees
     * (not containing any burnin trees).
     *
     * @param treeSet        an iterable set of trees, which contains no burnin trees,
     *                       whose distribution is approximated by the resulting
     *                       {@link CCD1}; all of its trees are used
     * @param storeBaseTrees whether to store the trees used to create this CCD
     */
//    public CCD1(TreeSet treeSet, boolean storeBaseTrees) {
//        super(treeSet, storeBaseTrees);
//    }

    /**
     * Constructor for an empty CDD. Trees can then be processed one by one.
     *
     * @param numLeaves      number of leaves of the trees that this CCD will be based on
     * @param storeBaseTrees whether to store the trees used to create this CCD;
     *                       recommended not to when huge set of trees is used
     */
    public CCD1(int numLeaves, boolean storeBaseTrees) {
        super(numLeaves, storeBaseTrees);
    }

    @Override
    public void initialize() {
        // nothing to do for CCD1s
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
    protected boolean removeCladePartitionIfNecessary(Clade clade, CladePartition partition) {
        // when a partition has no registered occurrences more, we remove it
        if (partition.getNumberOfOccurrences() <= 0) {
            clade.removePartition(partition);
            return true;
        }
        return false;
    }

    /* -- PROBABILITY, POINT ESTIMATE & SAMPLING METHODS -- */
    // all handled by parent class AbstractCCD as long as clade partition
    // probabilities are set correctly

    /* -- OTHER METHODS -- */

    @Override
    public String toString() {
        return "CCD1 " + super.toString();
    }

    @Override
    public AbstractCCD copy() {
        CCD1 copy = new CCD1(this.getSizeOfLeavesArray(), false);
        copy.baseTrees.add(this.getSomeBaseTree());
        copy.numBaseTrees = this.getNumberOfBaseTrees();

        AbstractCCD.buildCopy(this, copy);

        return copy;
    }

    @Override
    protected double getNumberOfParameters() {
        return this.getNumberOfCladePartitions();
    }

}
