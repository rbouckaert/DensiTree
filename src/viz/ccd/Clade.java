package viz.ccd;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a clade in the context of the conditional clade
 * probability & distribution of tree distribution.
 * <p>
 * For this clade, we can compute the partition that maximizes (i) the
 * conditional clade probability, which is computed locally, not recursively, or
 * (ii) the max recursive probability, that is, max probability of a subtree
 * rooted at the parent clade that realizes this partition.
 *
 * @author Jonathan Klawitter
 */
public class Clade {

    /**
     * The CCP this clade is part of.
     */
    private final AbstractCCD ccd;

    /**
     * BitSet representation of this clade. The mapping of bits to taxa is
     * implicit here, explicit in a global context.
     */
    private final BitSet cladeAsBitSet;

    /**
     * Number of taxa in this clade.
     */
    private final int size;

    /**
     * The number of times this clade occurs in the processed set of trees.
     */
    private int numOccurrences = 0;

    /**
     * Average height over all occurrences of this clade.
     */
    private double meanHeight = 0;

    /**
     * Child clades this clade is split into.
     */
    protected ArrayList<Clade> childClades;

    /**
     * Parent clades of this clade.
     */
    protected ArrayList<Clade> parentClades;

    /**
     * Observed ways this clade has been split into sub/child clades.
     */
    protected ArrayList<CladePartition> partitions;

    /**
     * The partition of this clade with max conditional clade probability
     * (locally, not in subtree).
     */
    private CladePartition maxCCPPartition = null;

    /**
     * Computed max CCP of any subtree with the root on this clade.
     */
    private double maxSubtreeCCP = -1;

    /**
     * The partition of this clade realized by the subtree rooted at this clade
     * with max CCP.
     */
    private CladePartition maxSubtreeCCPPartition = null;

    /**
     * Computed max sum of clade credibilities of any one subtree with the root
     * on this clade.
     */
    private double maxSubtreeSumCladeCredibility = -1;

    /**
     * The partition of this clade realized by the subtree rooted at this clade
     * with max sum of clade credibilities.
     */
    private CladePartition maxSubtreeSumCladeCredibilityPartition = null;

    /**
     * The sum of subtree clade credibilities of all trees rooted at this clade.
     */
    private double sumCladeCredibilities = -1;

    /**
     * The probability of this clade appearing in a tree of the respective
     * distribution.
     */
    private double probability = -1;

    /**
     * Entropy of the tree topologies with clade as root
     */
    private double entropy = -1;

    /**
     * Number of different tree topologies with this clade as root.
     */
    private BigInteger numTopologies = null;


    /* -- CONSTRUCTORS & CONSTRUCTION METHODS -- */

    /**
     * Construct a new Clade on the taxa specified by the given BitSet and being
     * part of the given CCD.
     *
     * @param cladeInBits BitSet representation of the clade
     * @param abstractCCD CCD this clade is part of
     */
    public Clade(BitSet cladeInBits, AbstractCCD abstractCCD) {
        this.ccd = abstractCCD;
        this.cladeAsBitSet = cladeInBits;
        this.size = cladeInBits.cardinality();
        this.parentClades = new ArrayList<Clade>(4);
        this.partitions = new ArrayList<CladePartition>(5);
        this.childClades = new ArrayList<Clade>(8);

        if (size == 1) {
            this.maxSubtreeCCP = 1;
            this.maxSubtreeSumCladeCredibility = 1;
            this.probability = 1;
            this.sumCladeCredibilities = 1;
        }
    }

    /**
     * Creates a (shallow) copy of this clade; CladePartitions of this Clade are
     * not set yet.
     *
     * @param targetCCD CCD to which the copy will belong to
     * @return a copy of this clade
     */
    public Clade copy(AbstractCCD targetCCD) {
        Clade copiedClade = new Clade((BitSet) this.cladeAsBitSet.clone(), targetCCD);
        copiedClade.increaseOccurrenceCountBy(getNumberOfOccurrences(), getMeanOccurredHeight());
        return copiedClade;
    }

    /**
     * Returns a stored bipartition of this clade comprised of the two given
     * clades (order does not matter); returns null if partition not existent.
     *
     * @param firstChildClade  one of the two child clades forming the bipartition
     * @param secondChildClade other of the two child clades forming the bipartition
     * @return the stored partition of this clade comprised of the given child
     * clades or null if not stored
     */
    public CladePartition getCladePartition(Clade firstChildClade, Clade secondChildClade) {
        for (CladePartition cladePartition : partitions) {
            if (cladePartition.containsChildClade(firstChildClade)
                    && cladePartition.containsChildClade(secondChildClade)) {
                return cladePartition;
            }
        }
        return null;
    }

    /**
     * Creates and stores a new clade partition for this clade comprised of the
     * two given clades.
     *
     * @param firstChildClade  one of the two child clades partitioning this clade
     * @param secondChildClade other of the two child clades partitioning this clade
     * @return a new clade partition based on the two given child clades
     */
    public CladePartition createCladePartition(Clade firstChildClade, Clade secondChildClade) {
        return this.createCladePartition(firstChildClade, secondChildClade, true);
    }

    /**
     * Creates and stores a new clade partition for this clade comprised of the
     * two given clades.
     *
     * @param firstChildClade  one of the two child clades partitioning this clade
     * @param secondChildClade other of the two child clades partitioning this clade
     * @param storeParent      whether to store this clade as parent clade in the child
     *                         clades
     * @return a new clade partition based on the two given child clades
     */
    public CladePartition createCladePartition(Clade firstChildClade, Clade secondChildClade,
                                               boolean storeParent) {
        Clade[] partitioningClades = new Clade[]{firstChildClade, secondChildClade};

        CladePartition newPartition = new CladePartition(this, partitioningClades);
        partitions.add(newPartition);

        childClades.add(partitioningClades[0]);
        childClades.add(partitioningClades[1]);
        if (storeParent) {
            partitioningClades[0].parentClades.add(this);
            partitioningClades[1].parentClades.add(this);
        }

        return newPartition;
    }

    /** Removes the given partition from this clade. */
    public void removePartition(CladePartition partition) {
        if (this.partitions.remove(partition)) {
            this.childClades.remove(partition.getChildClades()[0]);
            partition.getChildClades()[0].parentClades.remove(this);
            this.childClades.remove(partition.getChildClades()[1]);
            partition.getChildClades()[1].parentClades.remove(this);
        }
    }


    /* -- STATE MANGEMENT -- */

    /**
     * Resets the cached values of this clade (and its clade partitions).
     */
    public void resetCachedValues() {
        // values for leaves do not change
        if (!this.isLeaf()) {
            if (!this.isCherry()) {
                this.maxCCPPartition = null;
                this.maxSubtreeCCPPartition = null;
                this.maxSubtreeSumCladeCredibilityPartition = null;
                this.numTopologies = null;
            }
            this.maxSubtreeCCP = -1;
            this.maxSubtreeSumCladeCredibility = -1;
            this.entropy = -1;
            this.sumCladeCredibilities = -1;
            this.probability = -1;

            for (CladePartition partition : partitions) {
                partition.resetCachedValues();
            }
        }
    }


    /* -- OCCURRENCE COUNTS & HEIGHTS -- */

    /**
     * @return the number of occurrences registered for this clade
     */
    public int getNumberOfOccurrences() {
        return numOccurrences;
    }

    /**
     * @return mean/average height for this clade over all registered
     * occurrences with height of this clade; 0 for non-leaf clade means
     * no heights registered
     */
    public double getMeanOccurredHeight() {
        return meanHeight;
    }

    /**
     * Registers an occurrence of this clade.
     */
    public void increaseOccurrenceCount() {
        this.numOccurrences++;
    }

    /**
     * Registers an occurrence of this clade at the given height.
     *
     * @param height at which clade occurred
     */
    protected void increaseOccurrenceCount(double height) {
        this.meanHeight = (meanHeight * numOccurrences + height) / (numOccurrences + 1);

        increaseOccurrenceCount();
    }

    /**
     * Registers multiple additional occurrences of this clade.
     *
     * @param numAdditionalOccurrences to be registered
     */
    protected void increaseOccurrenceCountBy(int numAdditionalOccurrences) {
        this.numOccurrences += numAdditionalOccurrences;
    }

    /**
     * Registers multiple additional occurrence of this clade with given mean
     * height.
     *
     * @param numAdditionalOccurrences to be registered
     * @param height                   at which clade occurred
     */
    protected void increaseOccurrenceCountBy(int numAdditionalOccurrences, double height) {
        meanHeight = (meanHeight * numOccurrences + height * numAdditionalOccurrences)
                / (numOccurrences + numAdditionalOccurrences);

        this.increaseOccurrenceCountBy(numAdditionalOccurrences);
    }

    /**
     * Removes an occurrence of this clade.
     */
    protected void decreaseOccurrenceCount() {
        this.numOccurrences--;
    }

    /**
     * Removes an occurrence of this clade at the given height.
     *
     * @param height at which clade occurred (but record gets now discarded)
     */
    protected void decreaseOccurrenceCount(double height) {
        this.meanHeight = (meanHeight * numOccurrences - height) / (numOccurrences - 1);
        // note that if no occurrences remain, then the height is set to NaN

        this.decreaseOccurrenceCount();
    }

    /**
     * Overwrites the registered number of occurrences with the given number.
     *
     * @param numOccurrences new value
     */
    public void setNumberOfOccurrences(int numOccurrences) {
        this.numOccurrences = numOccurrences;
    }


    /* -- GRAPH STRUCTURE GETTERS & BASIC GETTERS -- */

    /**
     * @return number of taxa in this clade
     */
    public int size() {
        return this.size;
    }

    /**
     * @return BitSet of this clade
     */
    public BitSet getCladeInBits() {
        return cladeAsBitSet;
    }

    /**
     * @return whether this clade represents a leaf
     */
    public boolean isLeaf() {
        return (this.size == 1);
    }

    /**
     * @return whether this clade represents a cherry
     */
    public boolean isCherry() {
        return (this.size == 2);
    }

    /**
     * @return whether this clade represents a root clade
     */
    public boolean isRoot() {
        return (this == this.ccd.getRootClade());
    }

    /**
     * @return whether this clade is monophyletic, i.e., appears in all trees
     */
    public boolean isMonophyletic() {
        return (this.getCladeCredibility() == 1.0);
    }

    @Override
    public String toString() {
        return "Clade [taxa = " + cladeAsBitSet + ", numOccurrences = " + numOccurrences
                // + ", ccd = " + ccd
                + ", num partitions = " + partitions.size() + "]";
    }

    /**
     * @return the CCD this clade is part of
     */
    public AbstractCCD getCCD() {
        return ccd;
    }

    /**
     * @return the stored partitions of this clade
     */
    public ArrayList<CladePartition> getPartitions() {
        return partitions;
    }

    /**
     * @return the number of partitions of this clade
     */
    public int getNumberOfPartitions() {
        return partitions.size();
    }

    /**
     * @return all the child clades of this clade
     */
    public ArrayList<Clade> getChildClades() {
        return childClades;
    }

    /**
     * @return the number of child clades of this clade
     */
    public int getNumberOfChildClades() {
        return this.isLeaf() ? 0 : childClades.size();
    }

    /**
     * @return all the parent clades of this clade
     */
    public ArrayList<Clade> getParentClades() {
        return parentClades;
    }

    /**
     * @return the number of child clades of this clade
     */
    public int getNumberOfParentClades() {
        return parentClades.size();
    }

    /**
     * Returns a set of all descendant clades (or up to monophyletic) of this clade.
     *
     * @param untilMonophyletics whether to not collect descendants of monophyletic descendant clades as well
     * @return set of all descendant clades (or up to monophyletic) of this clade
     */
    public Set<Clade> getDescendantClades(boolean untilMonophyletics) {
        Set<Clade> descendants = new HashSet<Clade>();

        for (Clade child : this.getChildClades()) {
            child.collectDescendantClades(untilMonophyletics, descendants);
        }

        return descendants;
    }

    /* Recursive helper method */
    private void collectDescendantClades(boolean untilMonophyletics, Set<Clade> descendants) {
        if (descendants.add(this)) {
            if (!untilMonophyletics || !this.isMonophyletic()) {
                for (Clade child : this.getChildClades()) {
                    child.collectDescendantClades(untilMonophyletics, descendants);
                }
            }
        }
    }


    /* -- GETTERS RECURSIVE VALUES -- */

    /**
     * Returns the phylogenetic entropy of the subtrees under this clade
     * computed with the formula by
     * <a href="https://dx.doi.org/10.1093/sysbio/syw042">Lewis et al.,
     * 2016</a>.
     *
     * @return phylogenetic entropy of the subtrees under this clade
     */
    public double getEntropy() {
        if (this.entropy < 0) {
            // compute entropy recursively
            if (this.isLeaf()) {
                this.entropy = 0;
            } else {
                double runningEntropy = 0;

                for (CladePartition partition : this.partitions) {
                    double probability = partition.getCCP();
                    double logprobability = partition.getLogCCP();
                    double entropyFirstChild = partition.getChildClades()[0].getEntropy();
                    double entropySecondChild = partition.getChildClades()[1].getEntropy();

                    runningEntropy -= probability * (logprobability - entropyFirstChild - entropySecondChild);
                }

                this.entropy = runningEntropy;
            }
        }

        return entropy;
    }

    /**
     * @return the number of tree topologies with this clade as root
     */
    public BigInteger getNumberOfTopologies() {
        if (this.numTopologies == null) {
            if (this.isLeaf() || this.isCherry()) {
                numTopologies = BigInteger.valueOf(1);
            } else {
                numTopologies = BigInteger.valueOf(0);
                for (CladePartition partition : this.partitions) {
                    numTopologies = numTopologies.add(partition.getNumberOfTopologies());
                }
            }
        }

        return numTopologies;
    }

    /**
     * @return the partition of this clade with the max conditional clade
     * probability (locally, not recursively)
     */
    public CladePartition getMaxCCPPartition() {
        if (maxCCPPartition == null) {
            // we want to find the partition of this clade that has the maximal
            // conditional clade probability (locally, not recursively)
            double maxPartitionCCP = 0;
            for (CladePartition cladePartition : partitions) {
                if (maxPartitionCCP < cladePartition.getCCP()) {
                    maxPartitionCCP = cladePartition.getCCP();
                    maxCCPPartition = cladePartition;
                }
            }
        }

        return maxCCPPartition;
    }

    /**
     * @return max CCP of any subtree rooted at this clade
     */
    public double getMaxSubtreeCCP() {
        if (this.maxSubtreeCCP < 0) {
            this.computeMaxSubtreeCCP();
        }

        return maxSubtreeCCP;
    }

    /**
     * Returns the partition of this clade that is realized in the max
     * conditional clade probability subtree (so recursively) with the root at
     * this clade.
     *
     * @return partition realized in max CCP subtree rooted at this clade
     */
    public CladePartition getMaxSubtreeCCPPartition() {
        if ((this.maxSubtreeCCPPartition == null) || (this.maxSubtreeCCP < 0)) {
            this.computeMaxSubtreeCCP();
        }

        return maxSubtreeCCPPartition;
    }

    /*
     * Helper method. Computes the subtree with the root on this clade and that
     * maximizes the conditional clade probability; so this maximizes the
     * probability recursively.
     */
    private void computeMaxSubtreeCCP() {
        // for a single non-leaf clade, to find the max probability subtree we
        // only have to pick the partition of this clade with max probability in
        // its two subtrees
        for (CladePartition partition : partitions) {
            double partitionMaxCCP = partition.getMaxSubtreeCCP();

            if (partitionMaxCCP > maxSubtreeCCP) {
                maxSubtreeCCP = partitionMaxCCP;
                maxSubtreeCCPPartition = partition;
            } else if (partitionMaxCCP == maxSubtreeCCP) {
                // System.out.println("Tie found for computeMaxSubtreeCCP.");
                Clade smallCladeMax = maxSubtreeCCPPartition.getSmallerChild();
                Clade smallCladeCurrent = partition.getSmallerChild();

                // we pick the more balanced partition
                if (smallCladeMax.size() < smallCladeCurrent.size()) {
                    maxSubtreeCCP = partitionMaxCCP;
                    maxSubtreeCCPPartition = partition;
                } else if (smallCladeMax.size() == smallCladeCurrent.size()) {
                    // but if they are equally balanced, then decide lexicographically which partition to pick
                    // to achieve that, we work with the bitsets of the clades
                    BitSet bitsMax = smallCladeMax.getCladeInBits();
                    BitSet bitsCurrent = smallCladeCurrent.getCladeInBits();

                    if (bitsMax.equals(bitsCurrent)) {
                        System.err.println(maxSubtreeCCPPartition);
                        System.err.println(partition);
                        throw new AssertionError("Tie breaking failed - duplicate partitions detected!");
                    }

                    BitSet bitsSmaller = BitSetUtil.getLexicographicFirst(bitsMax, bitsCurrent);
                    if (bitsCurrent == bitsSmaller) {
                        maxSubtreeCCP = partitionMaxCCP;
                        maxSubtreeCCPPartition = partition;
                    }
                }
            }
        }

        // TODO consider tie breaking mechanisms
    }

    /**
     * @return clade credibility (Monte Carlo probability) of this clade
     * {@code (#occurrences / #trees)}
     */
    public double getCladeCredibility() {
        return this.getNumberOfOccurrences() / (double) this.ccd.getNumberOfBaseTrees();
    }

    /**
     * @return max sum of clade credibilities of any subtree rooted at this
     * clade
     */
    public double getMaxSubtreeSumCladeCredibility() {
        if (this.maxSubtreeSumCladeCredibility < 0) {
            this.computeMaxSubtreeSumCladeCredibility();
        }

        return maxSubtreeSumCladeCredibility;
    }

    /**
     * @return partition realized in max sum of clade credibilities subtree
     * rooted at this clade
     */
    public CladePartition getMaxSubtreeSumCladeCredibilityPartition() {
        if ((this.maxSubtreeSumCladeCredibilityPartition == null)
                || (this.maxSubtreeSumCladeCredibility < 0)) {
            this.computeMaxSubtreeSumCladeCredibility();
        }

        return maxSubtreeSumCladeCredibilityPartition;
    }

    /*
     * Helper method. Computes the subtree with the root on this clade and that
     * maximizes the sum of clade credibilities.
     */
    private void computeMaxSubtreeSumCladeCredibility() {
        double sumCladeCredibilites = this.getCladeCredibility();
        for (CladePartition partition : partitions) {
            double currentSCC = sumCladeCredibilites + partition.getMaxSubtreeSumCladeCredibility();
            if (currentSCC > maxSubtreeSumCladeCredibility) {
                maxSubtreeSumCladeCredibility = currentSCC;
                maxSubtreeSumCladeCredibilityPartition = partition;
            }
        }
    }

    /**
     * @return if computed, sum of subtree clade credibilities of all subtrees
     * rooted at this clade; use
     * {@link Clade#computeSumCladeCredibilities()} to compute new value
     */
    public double getSumCladeCredibilities() {
        return this.sumCladeCredibilities;
    }

    /**
     * @return the newly computed sum of subtree clade credibilities of all
     * subtrees rooted at this clade
     */
    public double computeSumCladeCredibilities() {
        if (this.isLeaf() || this.isCherry()) {
            return this.getCladeCredibility();
        } else {
            if (this.sumCladeCredibilities <= 0) {
                double sum = 0;
                for (CladePartition partition : partitions) {
                    sum += partition.getChildClades()[0].getSumCladeCredibilities()
                            * partition.getChildClades()[1].getSumCladeCredibilities();
                }
                this.sumCladeCredibilities = sum * this.getCladeCredibility();

            }
            return this.sumCladeCredibilities;
        }
    }

    /**
     * @param value sum of subtree clade credibilities of all subtrees rooted at
     *              this clade
     */
    public void setSumCladeCredibilities(double value) {
        this.sumCladeCredibilities = value;
    }

    /**
     * Better to call {@link ITreeDistribution#getCladeProbability(BitSet)} to
     * get this value. Returns the probability of this clade appearing in a tree
     * of a distribution ({@link ITreeDistribution}), if computed by the
     * distribution; otherwise returns -1;
     *
     * @return probability of this clade appearing in a tree; -1 if not computed
     * yet
     */
    public double getProbability() {
        return probability;
    }

    /**
     * Set the probability of this clade appearing in a tree of a distribution
     * ({@link ITreeDistribution}).
     *
     * @param probability
     */
    protected void setProbability(double probability) {
        this.probability = probability;
    }


    /* -- CLADE COMPARISONS -- */

    /**
     * Returns whether this clade contains the given clade as (not-necessarily
     * proper) subclade.
     *
     * @param potentialSubclade to be tested if contained in this clade
     * @return whether this clade contains the given clade as subclade
     */
    public boolean containsClade(Clade potentialSubclade) {
        return contains(potentialSubclade.getCladeInBits());
    }

    /**
     * Returns whether this clade contains the given BitSet.
     *
     * @param mask to be tested if contained in this clade
     * @return whether this clade contains the given filter
     */
    public boolean contains(BitSet mask) {
        return BitSetUtil.contains(this.cladeAsBitSet, mask);
    }

    /**
     * Returns whether this clade is contained in the given BitSet.
     *
     * @param mask to be tested if contains this clade
     * @return whether this clade is contained in the given BitSet
     */
    public boolean contained(BitSet mask) {
        return BitSetUtil.contains(mask, this.cladeAsBitSet);
    }

    /**
     * Returns whether this clade intersects the given clade.
     *
     * @param potentialIntersectedClade to be tested if intersects with this clade
     * @return whether this clade intersects the given clade
     */
    public boolean intersects(Clade potentialIntersectedClade) {
        return this.intersects(potentialIntersectedClade.getCladeInBits());
    }

    /**
     * Returns whether this clade intersects the given BitSet.
     *
     * @param mask to be tested if intersects with this clade
     * @return whether this clade intersects the given filter
     */
    public boolean intersects(BitSet mask) {
        return this.cladeAsBitSet.intersects(mask);
    }

    /**
     * Returns whether this clade (as BitSet) equals the given BitSet.
     *
     * @param mask to be tested if equals with this clade
     * @return whether this clade equals the given filter
     */
    public boolean equals(BitSet mask) {
        return this.cladeAsBitSet.equals(mask);
    }


    /* -- BASE CLADE 4 FILTERED CCDs -- */

    /**
     * Clade this one is based one; can be used for filtered clades.
     */
    private Clade baseClade;

    /**
     * @return the base clade of this clade (when clade comes from filtering)
     */
    public Clade getBaseClade() {
        return baseClade;
    }

    /**
     * @param baseClade set as base clade of this clade (when clade comes from
     *                  filtering)
     */
    protected void setBaseClade(Clade baseClade) {
        this.baseClade = baseClade;
    }

}
