package viz.ccd;


import java.math.BigInteger;
import java.util.Arrays;

/**
 * <p>
 * This class represents the partition of a parent clade C into child subclades
 * C1 and C2; in a tree this corresponds to the branching of a vertex with clade
 * C into children with clades C1 and C2.
 * </p>
 *
 * <p>
 * For this partition, we can compute (i) its conditional clade probability,
 * which is computed locally, not recursively, and (ii) the max recursive
 * (conditional clade) probability, which is computed recursively, that is, the
 * max probability of a subtree rooted at the parent clade that realizes this
 * partition.
 * </p>
 *
 * @author Jonathan Klawitter
 */
public class CladePartition {

    /** Number of times this partition occurs in the processed set of trees. */
    private int numOccurrences = 0;

    /** Average height over all occurrences of this clade. */
    private double meanHeight = 0;

    /** Parent clade that gets partitioned. */
    private Clade parentClade;

    /** Subclades the parent clade gets partitioned into. */
    private Clade[] childClades;

    /** Whether the ccp has been manually set. */
    private boolean ccpSet = false;

    /** The conditional clade probability (ccp) of this partition. */
    private double ccp = -1;

    /**
     * The maximum log CCP of the subtree with the root on the parent clade and that
     * realizes this partition.
     */
    private double maxSubtreeLogCCP = 1;


    /** Number of different tree topologies with this partition at the root. */
    private BigInteger numTopologies = null;

    /**
     * Constructor requiring parent clade (the one split) and the child clades
     * (split into, at least two). The child clades must partition the parent
     * clade, so they are disjoint but together cover the parent clade.
     *
     * @param parentClade the parent clade that gets partition
     * @param childClades the at least two child clades that partition the parent clade
     */
    public CladePartition(Clade parentClade, Clade[] childClades) {
        // check that children are disjoint and cover parent clade
		/*- TODO turned off for now; think about whether check necessary
		BitSet union = new BitSet(parentClade.getCladeInBits().size());
		for (int i = 0; i < childClades.length; i++) {
			BitSet firstChild = childClades[i].getCladeInBits();
			for (int j = i + 1; j < childClades.length; j++) {
				BitSet secondChild = childClades[j].getCladeInBits();
		
				if (firstChild.intersects(secondChild)) {
					throw new IllegalArgumentException("Child clades overlap: "
							+ firstChild.toString() + " - " + secondChild.toString());
				}
			}
			union.or(firstChild);
		}
		union.xor(parentClade.getCladeInBits());
		if (!union.isEmpty()) {
			throw new IllegalArgumentException("Child clades to not cover parent clade.");
		}
		*/

        this.parentClade = parentClade;
        this.childClades = childClades;
    }


    /* -- STATE MANGEMENT -- */

    /** Resets cached computed values. */
    public void resetCachedValues() {
        this.maxSubtreeLogCCP = 1;
        this.numTopologies = null;
    }


    /* -- OCCURRENCE COUNTS & HEIGHTS -- */

    /** @return the number of occurrences registered for this partition */
    public int getNumberOfOccurrences() {
        return numOccurrences;
    }

    /**
     * Returns the mean/average height for this clade over all registered
     * occurrences with height of this clade.
     *
     * @return the mean height of this clade
     */
    protected double getMeanOccurredHeight() {
        return meanHeight;
    }

    /** Registers an occurrence of this partition. */
    protected void increaseOccurrenceCount() {
        this.numOccurrences++;
    }

    /**
     * Registers an occurrence of this partition at the given height.
     *
     * @param height at which partition occurred
     */
    protected void increaseOccurrenceCount(double height) {
        meanHeight = (meanHeight * numOccurrences + height) / (numOccurrences + 1);

        increaseOccurrenceCount();
    }

    /**
     * Registers multiple additional occurrences of this partition.
     *
     * @param numAdditionalOccurrences to be registered
     */
    public void increaseOccurrenceCountBy(int numAdditionalOccurrences) {
        this.numOccurrences += numAdditionalOccurrences;
    }

    /**
     * Registers multiple additional occurrence of this partition with given
     * mean height.
     *
     * @param numAdditionalOccurrences to be registered
     * @param additionalMeanHeight     at which clade occurred
     */
    protected void increaseOccurrenceCountBy(int numAdditionalOccurrences,
                                             double additionalMeanHeight) {
        meanHeight = (meanHeight * numOccurrences + additionalMeanHeight * numAdditionalOccurrences)
                / (numOccurrences + numAdditionalOccurrences);

        this.increaseOccurrenceCountBy(numAdditionalOccurrences);
    }

    /** Removes an occurrence of this partition. */
    protected void decreaseOccurrenceCount() {
        this.numOccurrences--;
    }

    /**
     * Removes an occurrence of this partition at the given height.
     *
     * @param height at which partition occurred (but record gets now discarded)
     */
    protected void decreaseOccurrenceCount(double height) {
        meanHeight = (meanHeight * numOccurrences - height) / (numOccurrences - 1);

        decreaseOccurrenceCount();
    }

    /**
     * Set the number of occurrences of this partition.
     *
     * @param numOccurrences the new total number of occurrences
     */
    protected void setNumOccurrences(int numOccurrences) {
        this.numOccurrences = numOccurrences;
    }

    /**
     * Set the mean occurred height of this partition.
     *
     * @param height the new mean occurred height
     */
    public void setMeanOccurredHeight(double height) {
        this.meanHeight = height;
    }

    /* -- GRAPH STUCTURE GETTERS & BASIC GETTERS -- */
    @Override
    public String toString() {
        return "CladePartition [numOccurrences = " + numOccurrences + ", meanHeight = " + meanHeight
                + ", childClades = " + Arrays.toString(childClades) + "]";
    }

    /** @return the parent clade of this partition */
    public Clade getParentClade() {
        return parentClade;
    }

    /** @return the child clades of this partition */
    public Clade[] getChildClades() {
        return childClades;
    }

    /**
     * Returns the child clade of this partition that is not the given clade.
     *
     * @param childClade clade that is child of this partition with its sibling wanted
     * @return the child clade of this partition that is not the given clade
     */
    public Clade getOtherChildClade(Clade childClade) {
        if (!containsChildClade(childClade)) {
            throw new IllegalArgumentException(
                    "Given clade not child clade of partition; alleged child clade: " + childClade
                            + ", this partition: " + this.toString());
        }

        return (childClades[0] == childClade) ? childClades[1] : childClades[0];
    }

    /**
     * Returns whether the given clade is part of this clade partition.
     *
     * @param cladeToTest for which is checked whether it is one of the partitioning
     *                    clades of the parent clade of this partition
     * @return whether the given clade is part of this clade partition
     */
    public boolean containsChildClade(Clade cladeToTest) {
        return ((childClades[0] == cladeToTest) || (childClades[1] == cladeToTest));
    }

    /**
     * Returns whether the given clade as BitSet is part of this clade
     * partition.
     *
     * @param cladeToTest for which is checked whether it is one of the partitioning
     *                    clades of the parent clade of this partition
     * @return whether the given clade as BitSet is part of this clade partition
     */
    public boolean containsClade(BitSet cladeToTest) {
        return (childClades[0].getCladeInBits().equals(cladeToTest)
                || childClades[1].getCladeInBits().equals(cladeToTest));
    }

    /**
     * @param otherPartition clade partition to compare it to
     * @return whether this clade partition has the same clades as the given one
     */
    public boolean equivalentToPartition(CladePartition otherPartition) {
        return this.containsChildClade(otherPartition.getChildClades()[0])
                && this.containsChildClade(otherPartition.getChildClades()[1]);
    }


    /* -- GETTERS RECURSIVE VALUES -- */

    /** @return the number of tree topologies with this partition at the root */
    public BigInteger getNumberOfTopologies() {
        if (this.numTopologies == null) {
            this.numTopologies = childClades[0].getNumberOfTopologies()
                    .multiply(childClades[1].getNumberOfTopologies());
        }
        return this.numTopologies;
    }

    /**
     * Returns the conditional clade probability of this partition; locally (not
     * recursively), so only the probability of this partition of child clades
     * appearing under the parent clade.
     *
     * @return the conditional clade probability of this partition
     */
    public double getCCP() {
        if (ccpSet) {
            return ccp;
        } else {
            if (this.getParentClade().getNumberOfOccurrences() == 0) {
                throw new AssertionError("Clade with zero occurrences detected - tidy up?");
            }
            return this.numOccurrences / ((double) this.getParentClade().getNumberOfOccurrences());
        }
    }

    /* Precompute log values for quick lookup */
    private static double[] logTable;

    private static int logTableLength = 1024;

    static {
        logTable = new double[logTableLength];
        for (int i = 1; i < logTable.length; i++) {
            logTable[i] = Math.log(i);
        }
    }

    /**
     * Returns the log of the conditional clade probability of this partition;
     * locally (not recursively), so only for the probability of this partition
     * of child clades appearing under the parent clade.
     *
     * @return the log of the conditional clade probability of this partition
     */
    public double getLogCCP() {
        if (!this.ccpSet) {
            try {
                double logCCP = logTable[this.numOccurrences]
                        - logTable[this.getParentClade().getNumberOfOccurrences()];
                return logCCP;
            } catch (ArrayIndexOutOfBoundsException e) {
                // if our table of log wasn't long enough
                int oldLength = logTable.length;
                double[] tmp = new double[oldLength + logTableLength];
                System.arraycopy(logTable, 0, tmp, 0, oldLength);
                for (int i = oldLength; i < tmp.length; i++) {
                    tmp[i] = Math.log(i);
                }
                logTable = tmp;

                return getLogCCP();
            }
        } else {
            return Math.log(this.ccp);
        }
    }

    /**
     * Sets the conditional clade probability (CCP) of this partition to the
     * given value. Intended use is to set the CCP manually when merging CCDs.
     *
     * <p>
     * Use with care as the value will not be reflected in the stored
     * occurrences! Any changes to the CCD may thus break the probabilities!
     * Example use is when merging CCDs, but after that the CCD cannot be
     * modifiable by adding or removing trees.
     * </p>
     *
     * @param ccp conditional clade probability (ccp) of this partition
     */
    public void setCCP(double ccp) {
        if (Double.isNaN(ccp)) {
            throw new IllegalArgumentException("CCP value cannot be NaN");
        }
        this.ccp = ccp;
        this.ccpSet = true;
    }


    /**
     * Computes and returns the maximum conditional log clade probability (CCP) of
     * the subtree with the root on the parent clade and that realizes this
     * partition.
     *
     * @return maximum log probability of subtree realizing this partition
     */
    public double getMaxSubtreeLogCCP() {
        if (this.maxSubtreeLogCCP > 0) {
            maxSubtreeLogCCP = this.getLogCCP();

            for (Clade clade : childClades) {
                maxSubtreeLogCCP += clade.getMaxSubtreeLogCCP();
            }
        }

        return maxSubtreeLogCCP;
    }
    /**
     * Computes and returns the maximum sum of clade credibilities (MSCC) of the
     * subtree with the root on the parent clade and that realizes this
     * partition.
     *
     * @return maximum sum of clade credibility of subtree realizing this
     * partition
     */
    public double getMaxSubtreeSumCladeCredibility() {
        double maxSubtreeSumCladeCredibility = 0;
        for (Clade clade : childClades) {
            maxSubtreeSumCladeCredibility += clade.getMaxSubtreeSumCladeCredibility();
        }

        return maxSubtreeSumCladeCredibility;
    }

    public Clade getSmallerChild() {
        Clade childOne = this.getChildClades()[0];
        Clade childTwo = this.getChildClades()[1];
        Clade smallClade = null;
        if (childOne.size() < childTwo.size()) {
            smallClade = childOne;
        } else if (childOne.size() > childTwo.size()) {
            smallClade = childTwo;
        } else {
            BitSet bitsOne = childOne.getCladeInBits();
            BitSet bitsTwo = childTwo.getCladeInBits();
            BitSet bitsSmaller = BitSetUtil.getLexicographicFirst(bitsOne, bitsTwo);
            smallClade = (bitsSmaller == bitsOne) ? childOne : childTwo;
        }

        return smallClade;
    }
}
