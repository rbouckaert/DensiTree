package viz.ccd;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class provides algorithms to detect rogue taxa and rogue clades in a {@link AbstractCCD}
 * based on a {@link RogueDetectionStrategy} both to pick only a single rogue clade or
 * continuously by also using a {@link TerminationStrategy}.
 * Rogue clade here means that their removal yields the largest improvement for the given
 * strategy, but this does not necessarily mean that the found clades are "real"
 * rogues in the biological sense.<br>
 * The methods return {@link FilteredCCD}, whose filter
 * provide information on what the removed rogue clades are.
 * Ties are broken where the first found rogue clade with a certain improvement is used;
 * so this is deterministic as the same inputs will result in the same rogues found.
 *
 * @author Jonathan Klawitter
 */
public class RogueDetection {

    /**
     * This enum provides different strategies to pick rogues for continuous
     * rogue detection, meaning under which measure what potential rogue clade
     * gives the highest improvement. Works in collaboration with a specific
     * {@link TerminationStrategy}.
     */
    public enum RogueDetectionStrategy {
        /** Decrease of entropy in CCD */
        Entropy("entropy-reduction strategy"),
        /** Increase of probability of max CCP tree in CCD */
        MaxProbability("max-probability-improvement strategy"),
        /** Decrease of number of tree topologies in CCD */
        NumTopologies("number-topologies-reduction strategy");

        /** Succinct, descriptive name of strategy */
        private String name;

        /* Default constructor */
        private RogueDetectionStrategy(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * This enum provides different termination (stopping) strategies for
     * continuous rogue detection. A provided or set threshold acts with respect
     * to the used {@link RogueDetectionStrategy}, so for example an entropy
     * threshold is crossed from above while a probability threshold is crossed
     * from below.
     *
     * <p>
     * All strategies should work for {@link RogueDetectionStrategy#Entropy},
     * but some do not for others.
     * </p>
     */
    public enum TerminationStrategy {
        /** Run until no further improvement possible. */
        Exhaustive("exhaustive strategy"),
        /**
         * Run until a set number of rogues (with {@link #setThreshold(double)})
         * have been detected.
         */
        NumRogues("fixed-number-rogues strategy"),
        /**
         * For {@link RogueDetectionStrategy#Entropy} only, stops when the
         * threshold computed with the following formula is passed, which
         * represents the entropy if all inner vertices of a topology had a
         * certainty of {@link #CERTAINTY_DEFAULT} p at the start:<br>
         * {@code (numberOfLeaves - 1) * (-1) * (3 * p - 2) * Math.log(p)}
         */
        AbsoluteThreshold("absolute threshold"),
        /**
         * For {@link RogueDetectionStrategy#Entropy} only, stops when a
         * maintained threshold computed with the following formula is passed,
         * which represents the entropy if all inner vertices of a topology had
         * a certainty of {@link #CERTAINTY_DEFAULT} p at the current size:<br>
         * {@code (currentNumberOfLeaves - 1) * (-1) * (3 * p - 2) * Math.log(p)}
         */
        AdaptiveThreshold("adaptive theshold"),
        /**
         * Stop when the "manually" set threshold is passed (see
         * {@link TerminationStrategy#setThreshold(double)} or other setter
         * method).
         */
        Manual("manual threshold"),
        /**
         * Stops when every clade in the max CCP tree in the current CCD has a
         * support (or clade credibility, with respect to the trees sample the
         * CCD is based on) of either {@link #SUPPORT_THRESHOLD_DEFAULT} of
         * {@value #SUPPORT_THRESHOLD_DEFAULT} or a manually set value (with
         * {@link #setThreshold(double)}
         */
        Support("all-clades-min-support strategy");

        /**
         * Default certainty used for entropy based strategies that every vertex
         * would need to have; see {@link #AbsoluteThreshold} and
         * {@link #AdaptiveThreshold}.
         */
        public static final double CERTAINTY_DEFAULT = 0.95;

        /** Default support threshold for {@link #Support} strategy. */
        public static final double SUPPORT_THRESHOLD_DEFAULT = 0.5;

        /** Succinct, descriptive name of strategy */
        private String name;

        /**
         * Threshold used by the threshold based strategies; necessary to be set
         * accordingly.
         */
        private double threshold = Double.NaN;

        /* Default constructor */
        private TerminationStrategy(String name) {
            this.name = name;
        }

        /**
         * Sets the threshold to the given values, used for any type of
         * {@link RogueDetectionStrategy}. Then returns this
         * {@link TerminationStrategy}.
         *
         * @param threshold to be set, for any type of {@link RogueDetectionStrategy}
         * @return this
         */
        public TerminationStrategy setThreshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        /** @return the threshold used by this {@link TerminationStrategy} */
        public double getThreshold() {
            return this.threshold;
        }

        /**
         * Set the threshold for entropy based strategies for the given number
         * of leaves; see {@link #AbsoluteThreshold} and
         * {@link #AdaptiveThreshold}.
         */
        public void setEntropyThreshold(int numLeaves) {
            double p = CERTAINTY_DEFAULT;
            double threshold = (numLeaves - 1) * (-1) * (3 * p - 2) * Math.log(p);
            this.setThreshold(threshold);
        }

        @Override
        public String toString() {
            if ((this == Exhaustive) || (this == AdaptiveThreshold)) {
                return this.name;
            } else {
                return this.name + " (" + this.threshold + ")";
            }
        }
    }

    /**
     * Continuously detects the roguest clades with maximal given size under the
     * given strategies in the given CCD. Returns a list of
     * {@link FilteredCCD} where the found rogue clades
     * are successively removed. If the remove of a clade of size, say, two
     * yields a larger improvement than removing two leaves in a row, then the
     * clade of size two is picked. However, overall this is optimized such that
     * the last reported CCD has the largest improvement for its number of
     * remaining taxa. (So while it might be better to remove the clade of size
     * two there, for the next removed clade it might better to continue with
     * the CCD "skipped".)
     *
     * @param ccd                    in which we try to find rogue clades
     * @param maxCladeSize           maximum size of a rogue clade we try to detect
     * @param rogueDetectionStrategy strategy to measure rogueness
     * @param terminationStrategy    strategy to decide when to stop
     * @return list of {@link FilteredCCD} where the
     * found rogue clades are successively removed
     */
    public static ArrayList<AbstractCCD> detectRoguesWhileImproving(
            AbstractCCD ccd, int maxCladeSize,
            RogueDetectionStrategy rogueDetectionStrategy,
            TerminationStrategy terminationStrategy) {

        System.out.println("# rogue detection with max clade size tested " + maxCladeSize
                + "\n# rogue detection with the " + rogueDetectionStrategy.toString()
                + "\n# and terminating based on " + terminationStrategy.toString());

        // # dynamic program
        AbstractCCD[] bestCCDs = new AbstractCCD[ccd.getSizeOfLeavesArray()];
        bestCCDs[0] = ccd;

        // we have to initialize the thresholds based on the strategies
        if ((rogueDetectionStrategy == RogueDetectionStrategy.Entropy)
                && (terminationStrategy == TerminationStrategy.AbsoluteThreshold)) {
            terminationStrategy.setEntropyThreshold(ccd.getNumberOfLeaves());
        }
        if ((terminationStrategy == TerminationStrategy.Support)
                && (terminationStrategy.threshold == Double.NaN)) {
            terminationStrategy.setThreshold(TerminationStrategy.SUPPORT_THRESHOLD_DEFAULT);
        }

        System.out.println("following is only the preliminary output of the detection "
                + "as a stated rogue clade might be replaced by the removal of a larger clade later");
        System.out.println("i, n, H_CCD, CCD_map_prob, clade count, removed taxa");

        detection:
        for (int i = 1; i < bestCCDs.length; i++) {
            if ((rogueDetectionStrategy == RogueDetectionStrategy.Entropy)
                    && (terminationStrategy == TerminationStrategy.AdaptiveThreshold)) {
                terminationStrategy.setEntropyThreshold(ccd.getNumberOfLeaves() - i);
            }

            System.out.println("\n> Rogue detection iteration " + i + ":");
            // System.out.print((i - 1) + " taxa removal; current threshold is "
            // + terminationStrategy.getThreshold() + ":");

            for (int filterSize = 1; filterSize <= maxCladeSize; filterSize++) {
                int baseIndex = i - filterSize;
                if (baseIndex >= 0) {
                    if (bestCCDs[baseIndex] == null) {
                        // can only do DP, if we have a previous solution to
                        // build on
                        continue;
                    }

                    FilteredCCD nextFCCPCandidate = detectSingleRogueClade(
                            bestCCDs[baseIndex], filterSize, rogueDetectionStrategy);
                    if (nextFCCPCandidate == null) {
                        // System.out.println("no better filtered CCD found in
                        // this round");
                        continue;
                    } else if (bestCCDs[i] == null) {
                        // System.out.println("new best filtered CCD found");
                        bestCCDs[i] = nextFCCPCandidate;
                    } else {
                        // System.out.println("another best filtered CCD
                        // found");
                        switch (rogueDetectionStrategy) {
                            case Entropy:
                                if (nextFCCPCandidate.getEntropy() < bestCCDs[i].getEntropy()) {
                                    bestCCDs[i] = nextFCCPCandidate;
                                }
                                break;
                            case MaxProbability:
                                if (nextFCCPCandidate.getMaxTreeProbability() > bestCCDs[i].getMaxTreeProbability()) {
                                    bestCCDs[i] = nextFCCPCandidate;
                                }
                                break;
                            case NumTopologies:
                                if (bestCCDs[i].getNumberOfTrees()
                                        .compareTo(nextFCCPCandidate.getNumberOfTrees()) > 0) {
                                    bestCCDs[i] = nextFCCPCandidate;

                                    if (nextFCCPCandidate.getNumberOfTrees()
                                            .compareTo(BigInteger.ONE) == 0) {
                                        break detection;
                                    }
                                }
                                break;
                        }
                    }
                }
            }

            System.out.print(i + ", ");
            if (bestCCDs[i] == null) {
                System.out.println(" -, -, -, -, - (no improvement)");
            } else {
                System.out.println(bestCCDs[i].getRootClade().getCladeInBits().cardinality() + ", " //
                        + bestCCDs[i].getEntropy() + ", " //
                        + bestCCDs[i].getMaxTreeProbability() + ", " //
                        // + ccdi.getMaxCCP() + ", " //
                        + bestCCDs[i].getNumberOfClades() + ", " //
                        + bestCCDs[0].getTaxaNames(
                        ((FilteredCCD) bestCCDs[i]).getRemovedTaxaMask()));
            }

            // # termination check
            // if we haven't found a solution in the last maxCladeSize many
            // steps, then from now on when cannot improve anymore
            if (!stillImprovingCheck(bestCCDs, i, maxCladeSize)) {
                 System.out.println("end of rogue detection - no improvement "
                        + "anymore for clades of size up to " + maxCladeSize);
                break detection;
            } else {
                switch (terminationStrategy) {
                    case NumRogues:
                        if (i == (int) terminationStrategy.getThreshold()) {
                            System.out.println("\nend of rogue detection " //
                                    + "- specified number of rogues found");
                            break detection;
                        }
                        break;
                    case AbsoluteThreshold:
                    case AdaptiveThreshold:
                    case Manual:
                        switch (rogueDetectionStrategy) {
                            case Entropy:
                                if (bestCCDs[i].getEntropy() <= terminationStrategy.getThreshold()) {
                                    System.out.println("\nend of rogue detection - entropy threshold of "
                                            + terminationStrategy.getThreshold() + " passed");
                                    break detection;
                                }
                            case MaxProbability:
                                if (bestCCDs[i].getMaxTreeProbability() >= terminationStrategy.getThreshold()) {
                                    System.out.println(
                                            "\nend of rogue detection - max probability threshold of "
                                                    + terminationStrategy.getThreshold() + " passed");
                                    break detection;
                                }
                                break;
                            default:
                                System.out.println(
                                        "\nTermination strategy not supported for this rogue detection strategy.");
                                break;
                        }
                        break;
                    case Support:
                        // TODO implement this again with beast trees
                        // Tree tree =
                        // bestCCPs[i].getMaxProbabilityTree(HeightSettingStrategy.None);
                        // double minSupport =
                        // Arrays.stream(tree.getNodesAsArray()).filter(x -> x !=
                        // null)
                        // .map(x ->
                        // x.getSupport()).min(Double::compare).orElse(0.0);
                        // if (minSupport >= terminationStrategy.threshold) {
                        // break detection;
                        // }
                        break;
                    default:
                        break;
                }
            }

        }
        System.out.println("");

        // # recover sequence of CCPs
        ArrayList<AbstractCCD> fccds = new ArrayList<>(ccd.getNumberOfLeaves());
        // so first find CCP with most rogues
        AbstractCCD bestCCD = null;
        for (int i = bestCCDs.length - 1; i >= 0; i--) {
            if (bestCCDs[i] != null) {
                bestCCD = bestCCDs[i];
                break;
            }
        }
        // then trace back sequence of CCPs
        do {
            fccds.add(bestCCD);
            if (bestCCD instanceof FilteredCCD) {
            	bestCCD = ((FilteredCCD) bestCCD).getBaseCCD();
            } else {
            	break;
            }
        } while (bestCCD != ccd);
        fccds.add(ccd);
        Collections.reverse(fccds);

        return fccds;
    }

    /**
     * @return whether any improvement has been made in the past max clade size
     * many steps
     */
    private static boolean stillImprovingCheck(AbstractCCD[] bestCCDs,
                                               int lastIndex, int maxCladeSize) {
    	if (lastIndex < maxCladeSize) {
    		return true;
    	}
        // check whether any of the past best CCDs has been set
        for (int j = lastIndex; j > (lastIndex - maxCladeSize); j++) {
            if (bestCCDs[lastIndex] != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detects a single rogue clade of given size for the give strategy in the
     * given CCD and returns {@link FilteredCCD}
     * without this rogue clade.
     *
     * @param ccd                    in which we try to find a rogue clade
     * @param cladeSize              size of rogue clade we try to detect
     * @param rogueDetectionStrategy strategy to measure rogueness
     * @return a {@link FilteredCCD} with the roguest
     * clade removed
     */
    public static FilteredCCD detectSingleRogueClade(
            AbstractCCD ccd, int cladeSize,
            RogueDetectionStrategy rogueDetectionStrategy) {

        ArrayList<BitSet> candidateFilters = new ArrayList<>();
        for (Clade clade : ccd.getClades()) {
            // only use filters based on clades
            // and if a single parent clade doesn't have 100% support
            if ((clade.size() == cladeSize) && ((clade.getNumberOfParentClades() != 1)
                    || (clade.getParentClades().get(0).getCladeCredibility() != 1))) {
                BitSet filter = (BitSet) clade.getCladeInBits().clone();
                candidateFilters.add(filter);
            }
        }

        // System.out.println("Check " + candidateFilters.size() + " clades of size " + cladeSize + " ... ");

        FilteredCCD roguestCCD = null;
        double bestProbability = ccd.getMaxTreeProbability();
        double bestEntropy = ccd.getEntropy();
        BigInteger bestNumTopologies = (rogueDetectionStrategy == RogueDetectionStrategy.NumTopologies)
                ? ccd.getNumberOfTrees()
                : null;

        // just to detect ties or no improvement
        boolean tie = false;
        // TODO: decide on tie breaking strategy / report all equally good fccp

        int i = 0;
        for (BitSet filter : candidateFilters) {
            // if ((i != 0) && (i % 10 == 0)) {
            //     System.out.print(".");
            //     System.out.flush();
            // }
            // if ((i != 0) && (i % 1000 == 0)) {
            //     System.out.println(" (" + i + ")");
            // }
            i++;
            // FilteredConditionalCladeDistribution fccd = new
            // FilteredConditionalCladeDistribution(ccd, filter);
            FilteredCCD fccd = new AttachingFilteredCCD(ccd, filter);
            boolean improvement = false;

            switch (rogueDetectionStrategy) {
                case Entropy:
                    double currentEntropy = fccd.getEntropy();

                    if (currentEntropy < bestEntropy) {
                        bestEntropy = currentEntropy;
                        improvement = true;
                    } else if ((currentEntropy == bestEntropy) && (roguestCCD != null)) {
                        tie = true;
                    }

                    break;
                case MaxProbability:
                    double currentProbability = fccd.getMaxTreeProbability();

                    if (currentProbability > bestProbability) {
                        bestProbability = currentProbability;
                        improvement = true;
                    } else if ((currentProbability == bestProbability) && (roguestCCD != null)) {
                        tie = true;
                    }

                    break;
                case NumTopologies:
                    BigInteger currentNumTopologies = fccd.getNumberOfTrees();

                    if (bestNumTopologies.compareTo(fccd.getNumberOfTrees()) > 0) {
                        bestNumTopologies = currentNumTopologies;
                        improvement = true;
                    } else if ((bestNumTopologies.compareTo(fccd.getNumberOfTrees()) == 0)
                            && (roguestCCD != null)) {
                        tie = true;
                    }

                    break;
            }

            if (improvement) {
                roguestCCD = fccd;
                tie = false;
            }
        }
        // System.out.println(" done.");

        if (tie) {
            // System.out.println("Rogue detection chose a CCD, but there were
            // ties
            // for the best.");
            // System.out.println("- Original CCD: " + ccd);
            // System.out.println("- Rogue CCD: " + roguestCCD);
        }

        if (roguestCCD != null) {
            roguestCCD = new FilteredCCD(ccd, roguestCCD.getRemovedTaxaMask());
        }

        return roguestCCD;
    }

}
