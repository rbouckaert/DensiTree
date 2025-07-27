package viz.ccd;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import viz.Node;

/**
 * This class provides algorithms to detect rogue taxa and rogue clades in an {@link AbstractCCD}
 * based on a {@link RogueDetectionStrategy} both to pick only a single rogue clade or
 * continuously to obtain a skeleton by also using a {@link TerminationStrategy}.
 * Rogue clade here means that their removal yields the largest improvement for the given
 * strategy, but this does not necessarily mean that the found clades are "real" rogues.<br>
 * The methods return {@link FilteredCCD}, whose filter
 * provide information on what the removed rogue clades are.
 * Ties are broken where the first found rogue clade with a certain improvement is used;
 * so this is deterministic as the same inputs will result in the same rogues found.
 *
 * @author Jonathan Klawitter
 */
public class RogueDetection {

    /**
     * This enum provides different strategies to pick rogues for single and continuous
     * rogue detection, meaning under which measure what potential rogue clade
     * gives the highest improvement.
     * For continuous removal, works in collaboration with a specific {@link TerminationStrategy}.
     */
    public enum RogueDetectionStrategy {
        /** Decrease of entropy in CCD */
        Entropy("entropy-reduction strategy"),
        /** Increase of probability of max CCP tree in CCD */
        MaxProbability("max-probability-improvement strategy"),
        /** Decrease of number of tree topologies in CCD */
        NumTopologies("number-topologies-reduction strategy");

        /** Succinct, descriptive name of strategy */
        private final String name;

        /* Default constructor */
        RogueDetectionStrategy(String name) {
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
     * threshold is crossed from above while a probability threshold is crosses from below.
     * All of {@link #NumRogues}, {@link #Entropy}, and {@link #MaxProbability}
     * require a threshold to be set with {@link #setThreshold(double)}.
     *
     * <p>
     * All strategies should work for {@link RogueDetectionStrategy#Entropy},
     * but some other combinations do not.
     * </p>
     */
    public enum TerminationStrategy {
        /** Run until no further improvement possible. */
        Exhaustive("exhaustive"),
        /** Run until a set number of rogues (with {@link #setThreshold(double)}) have been removed. */
        NumRogues("fixed-number-rogues"),
        /** Run until an entropy threshold is passed (from above) set with {@link #setThreshold(double)}. */
        Entropy("entropy"),
        /** Run until the CCD MAP tree has at least the probability set with {@link #setThreshold(double)}. */
        MaxProbability("probability"),
        /**
         * Stops when every clade in the max CCP tree in the current CCD has a
         * support (or clade credibility, with respect to the trees sample the
         * CCD is based on) of either {@link #SUPPORT_THRESHOLD_DEFAULT} of
         * {@value #SUPPORT_THRESHOLD_DEFAULT} or a manually set value (with
         * {@link #setThreshold(double)}
         */
        Support("all-clades-min-support");

        /** Default number of rogue taxa for {@link #NumRogues} strategy. */
        public static final double NUM_ROGUES_THRESHOLD_DEFAULT = 10;

        /** Default entropy threshold for {@link #Entropy} strategy. */
        public static final double ENTROPY_THRESHOLD_DEFAULT = 10;

        /** Default threshold for probability of CCD MAP tree for {@link #MaxProbability} strategy. */
        public static final double MAX_PROB_THRESHOLD_DEFAULT = 0.1;

        /** Default support threshold for {@link #Support} strategy. */
        public static final double SUPPORT_THRESHOLD_DEFAULT = 0.5;


        /** Succinct, descriptive name of strategy */
        private final String name;

        /**
         * Threshold used by the threshold based strategies; necessary to be set
         * accordingly.
         */
        private double threshold = Double.NaN;

        /* Default constructor */
        TerminationStrategy(String name) {
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

        @Override
        public String toString() {
            return this.name;
//            if (this == Exhaustive) {
//                return this.name;
//            } else {
//                return this.name + " (" + this.threshold + ")";
//            }
        }
    }

    /**
     * Extract the rogue clades used to construct the list of {@link FilteredCCD} by {@link #detectRoguesWhileImproving}.
     *
     * @param ccds list of {@link FilteredCCD} from {@link #detectRoguesWhileImproving}
     * @return masks of rogues used to construct  {@link FilteredCCD}s
     */
    public static Set<BitSet> extractRogues(ArrayList<AbstractCCD> ccds) {
        Set<BitSet> rogues = new HashSet<>(ccds.size());
        AbstractCCD baseCCD = ccds.get(0);
        for (AbstractCCD ccd : ccds) {
            if (ccd instanceof FilteredCCD) {
                BitSet rogueMask = ((FilteredCCD) ccd).getRemovedTaxaMask();
                rogues.add(rogueMask);
            }
        }
        return rogues;
    }

    /**
     * Greedy dynamic program that continuously detects the roguest clades
     * with given maximum size under the given strategies in the given CCD.
     * Returns a list of {@link FilteredCCD} where the found rogue clades are successively removed.
     * If the remove of a clade of size, say, two yields a larger improvement
     * than removing two leaves in a row, then the clade of size two is picked.
     * However, overall the best chain of removing clades is based on a DP
     * and so intermediate CCDs might not be part of the solution list.
     *
     * @param ccd                    in which we try to find rogue clades
     * @param maxCladeSize           maximum size of a rogue clade we try to detect
     * @param rogueDetectionStrategy strategy to measure rogueness
     * @param terminationStrategy    strategy to decide when to stop
     * @return list of {@link FilteredCCD} where the found rogue clades are successively removed
     */
    public static ArrayList<AbstractCCD> detectRoguesWhileImproving(
            AbstractCCD ccd, int maxCladeSize,
            RogueDetectionStrategy rogueDetectionStrategy,
            TerminationStrategy terminationStrategy) {
        return detectRoguesWhileImproving(ccd, maxCladeSize, rogueDetectionStrategy, terminationStrategy, true);
    }

    /**
     * Greedy dynamic program that continuously detects the roguest clades
     * with given maximum size under the given strategies in the given CCD.
     * Returns a list of {@link FilteredCCD} where the found rogue clades are successively removed.
     * If the remove of a clade of size, say, two yields a larger improvement
     * than removing two leaves in a row, then the clade of size two is picked.
     * However, overall the best chain of removing clades is based on a DP
     * and so intermediate CCDs might not be part of the solution list.
     *
     * @param ccd                    in which we try to find rogue clades
     * @param maxCladeSize           maximum size of a rogue clade we try to detect
     * @param rogueDetectionStrategy strategy to measure rogueness
     * @param terminationStrategy    strategy to decide when to stop
     * @param verbose                whether to print information while running
     * @return list of {@link FilteredCCD} where the found rogue clades are successively removed
     */
    public static ArrayList<AbstractCCD> detectRoguesWhileImproving(
            AbstractCCD ccd, int maxCladeSize,
            RogueDetectionStrategy rogueDetectionStrategy,
            TerminationStrategy terminationStrategy, boolean verbose) {
        return detectRoguesWhileImproving(ccd, maxCladeSize, rogueDetectionStrategy, terminationStrategy, 0.0, verbose);
    }

    /**
     * Greedy dynamic program that continuously detects the roguest clades
     * with given maximum size under the given strategies in the given CCD.
     * Returns a list of {@link FilteredCCD} where the found rogue clades are successively removed.
     * If the remove of a clade of size, say, two yields a larger improvement
     * than removing two leaves in a row, then the clade of size two is picked.
     * However, overall the best chain of removing clades is based on a DP
     * and so intermediate CCDs might not be part of the solution list.
     *
     * @param ccd                    in which we try to find rogue clades
     * @param maxCladeSize           maximum size of a rogue clade we try to detect
     * @param rogueDetectionStrategy strategy to measure rogueness
     * @param terminationStrategy    strategy to decide when to stop
     * @param verbose                whether to print information while running
     * @return list of {@link FilteredCCD} where the found rogue clades are successively removed
     */
    public static ArrayList<AbstractCCD> detectRoguesWhileImproving(
            AbstractCCD ccd, int maxCladeSize,
            RogueDetectionStrategy rogueDetectionStrategy, TerminationStrategy terminationStrategy,
            double minCladeProbability, boolean verbose) {

        if (verbose) {
            System.out.println("> skeleton computation with max clade size " + maxCladeSize
                    + " and min clade probability " + minCladeProbability
                    + ",\n rogue detection with the " + rogueDetectionStrategy.toString()
                    + ",\n and terminating based on " + terminationStrategy.toString());
        }

        if (maxCladeSize >= ccd.getNumberOfLeaves()) {
            throw new IllegalArgumentException("Cannot remove clade of size as big root clade.");
        }

        // # dynamic program
        AbstractCCD[] bestCCDs = new AbstractCCD[ccd.getSizeOfLeavesArray()];
        bestCCDs[0] = ccd;

        // we have to initialize the thresholds based on the strategies
        if (terminationStrategy == TerminationStrategy.Support) {
            if (Double.isNaN(terminationStrategy.threshold)) {
                terminationStrategy.setThreshold(TerminationStrategy.SUPPORT_THRESHOLD_DEFAULT);
            }
        }
        if (terminationStrategy == TerminationStrategy.NumRogues) {
            if (Double.isNaN(terminationStrategy.threshold)) {
                System.err.println("Num rogue strategy has no threshold set; use max clade size (" + maxCladeSize + ") instead.");
                terminationStrategy.setThreshold(maxCladeSize);
            }
        }

        if (verbose) {
            System.out.println("The following is only the preliminary output of the skeleton algorithm\n"
                    + "as a stated rogue clade might be replaced by the removal of a larger clade later");
            System.out.println("i, n, H(CCD), p(CCD MAP tree), #clades, removed taxa");
            System.out.println(0 + ", " + ccd.getNumberOfLeaves() + ", " //
                    + ccd.getEntropy() + ", " //
                    + ccd.getMaxTreeProbability() + ", " //
                    + ccd.getNumberOfClades() + ", -");
        }

        detection:
        for (int i = 1; i < bestCCDs.length; i++) {
            filterSize:
            for (int filterSize = 1; filterSize <= maxCladeSize; filterSize++) {
                int baseIndex = i - filterSize;
                if (baseIndex >= 0) {
                    if (bestCCDs[baseIndex] == null) {
                        // can only do DP, if we have a previous solution to build on
                        continue;
                    }

                    FilteredCCD nextFCCPCandidate = detectSingleRogueClade(bestCCDs[baseIndex], filterSize, minCladeProbability, rogueDetectionStrategy);
                    if (nextFCCPCandidate != null) {
                        if (bestCCDs[i] == null) {
                            bestCCDs[i] = nextFCCPCandidate;
                        } else {
                            switch (rogueDetectionStrategy) {
                                case Entropy:
                                    if (nextFCCPCandidate.getEntropy() < bestCCDs[i].getEntropy()) {
                                        bestCCDs[i] = nextFCCPCandidate;

                                        if (nextFCCPCandidate.getEntropy() == 0) {
                                            break filterSize;
                                        }
                                    }

                                    break;
                                case MaxProbability:
                                    if (nextFCCPCandidate.getMaxTreeProbability() > bestCCDs[i].getMaxTreeProbability()) {
                                        bestCCDs[i] = nextFCCPCandidate;
                                    }
                                    break;
                                case NumTopologies:
                                    if (bestCCDs[i].getNumberOfTrees().compareTo(nextFCCPCandidate.getNumberOfTrees()) > 0) {
                                        bestCCDs[i] = nextFCCPCandidate;

                                        if (nextFCCPCandidate.getNumberOfTrees().compareTo(BigInteger.ONE) == 0) {
                                            break filterSize;
                                        }
                                    }
                                    break;
                            }
                        }
                    }
                }
            }

            if (verbose) {
                System.out.print(i + ", ");
                if (bestCCDs[i] == null) {
                    System.out.println(" -, -, -, -, - (no improvement)");
                } else {
                    System.out.println(bestCCDs[i].getRootClade().getCladeInBits().cardinality() + ", " //
                            + bestCCDs[i].getEntropy() + ", " //
                            + bestCCDs[i].getMaxTreeProbability() + ", " //
                            + bestCCDs[i].getNumberOfClades() + ", " //
                            + bestCCDs[0].getTaxaNames(((FilteredCCD) bestCCDs[i]).getRemovedTaxaMask()));
                }
            }

            // # termination check
            // if we haven't found a solution in the last maxCladeSize many steps,
            // then from now on when cannot improve anymore
            if ((bestCCDs[i] != null) && (bestCCDs[i].getEntropy() == 0)) {
                System.out.println("\nEnd of rogue detection - no uncertainty left");
                break;
            } else if (!stillImprovingCheck(bestCCDs, i, maxCladeSize)) {
                System.out.println("\nEnd of rogue detection - no improvement "
                        + "anymore for clades of size up to " + maxCladeSize);
                break;
            } else {
                if (bestCCDs[i] == null) {
                    continue;
                }

                switch (terminationStrategy) {
                    case NumRogues:
                        if (i >= (int) terminationStrategy.getThreshold()) {
                            System.out.println("\nEnd of rogue detection " //
                                    + "- specified number of rogues found");
                            break detection;
                        }
                        break;
                    case Entropy:
                        if (bestCCDs[i].getEntropy() <= terminationStrategy.getThreshold()) {
                            System.out.println("\nEnd of rogue detection - entropy threshold of "
                                    + terminationStrategy.getThreshold() + " passed");
                            break detection;
                        }
                        break;
                    case MaxProbability:
                        if (bestCCDs[i].getMaxTreeProbability() >= terminationStrategy.getThreshold()) {
                            System.out.println("\nEnd of rogue detection - probability threshold of "
                                    + terminationStrategy.getThreshold() + " passed");
                            break detection;
                        }
                        break;
                    case Support:
//                        Tree tree = bestCCDs[i].getMAPTree();
//                        double minSupport =
//                                Arrays.stream(tree.getNodesAsArray())
//                                        .filter(Objects::nonNull)
//                                        .map(x -> (Double) x.getMetaData(AbstractCCD.CLADE_SUPPORT_KEY))
//                                        .min(Double::compare).orElse(0.0);
//                        if (minSupport >= terminationStrategy.threshold) {
//                            System.out.println("\nEnd of rogue detection - support threshold of "
//                                    + terminationStrategy.getThreshold() + " passed");
//                            break detection;
//                        }
                        break;
                    default:
                        break;
                }
            }

        }
        System.out.println();

        // # recover sequence of CCPs
        ArrayList<AbstractCCD> fCCDs = new ArrayList<>(ccd.getNumberOfLeaves());
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
            fCCDs.add(bestCCD);
            if (bestCCD instanceof FilteredCCD) {
                bestCCD = ((FilteredCCD) bestCCD).getBaseCCD();
            } else {
                break;
            }
        } while (bestCCD != ccd);
        fCCDs.add(ccd);
        Collections.reverse(fCCDs);

        return fCCDs;
    }

    /**
     * @return whether any improvement has been made in the past max clade size many steps
     */
    private static boolean stillImprovingCheck(AbstractCCD[] bestCCDs, int lastIndex, int maxCladeSize) {
        // check whether enough tries were even made so far
        if ((lastIndex - maxCladeSize) < 0) {
            // haven't tried maxCladeSize yet, so there could still be improvements
            return true;
        }

        // check whether any of the past best CCDs has been set
        for (int j = lastIndex; j > (lastIndex - maxCladeSize); j--) {
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
     * @param minCladeProbability    minimum probability for a clade to be considered
     * @param rogueDetectionStrategy strategy to measure rogueness
     * @return a {@link FilteredCCD} with the roguest
     * clade removed
     */
    public static FilteredCCD detectSingleRogueClade(AbstractCCD ccd, int cladeSize, double minCladeProbability,
                                                     RogueDetectionStrategy rogueDetectionStrategy) {

        ArrayList<BitSet> candidateFilters = new ArrayList<>();
        for (Clade clade : ccd.getClades()) {
            if (clade.getProbability() < minCladeProbability) {
                continue;
            }

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

        // to detect ties
        boolean tie = false;

        for (BitSet filter : candidateFilters) {
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

        if (roguestCCD != null) {
            roguestCCD = new FilteredCCD(ccd, roguestCCD.getRemovedTaxaMask());
        }

        return roguestCCD;
    }

    /**
     * Annotates the metadata string of the given tree with rogue placement information,
     * that is, for each given rogue clade with what probability the rogue is attached to it
     * based on the probabilities derived from the given base CCD.
     *
     * <p>Assumptions: Skeleton and given tree must be consistent, or if no skeleton is given,
     * one is computed based on taxa in given tree.
     * Rogue clades may not overlap with given tree.
     *
     * @param baseCCD     on which filtered CCD and probabilities are based on
     * @param skeletonCCD CCD without rogue and containing given tree; allowed {@code null}
     * @param rogues      all clades for which rogue annotation should be added to tree metadata
     * @param tree        which gets annotated
     */
//    public static void annotateRoguePlacements(AbstractCCD baseCCD, FilteredCCD skeletonCCD, Set<BitSet> rogues, Tree tree) {
//        BitSet rogueFilter;
//        if (skeletonCCD != null) {
//            rogueFilter = (BitSet) baseCCD.getTaxaAsBitSet().clone();
//            rogueFilter.andNot(skeletonCCD.getTaxaAsBitSet());
//        } else {
//            rogueFilter = BitSet.newBitSet(baseCCD.getSizeOfLeavesArray());
//            for (Node node : tree.getExternalNodes()) {
//                rogueFilter.set(node.getNr());
//            }
//
//            skeletonCCD = new FilteredCCD(baseCCD, rogueFilter);
//        }
//
//        Map<Clade, Node> map = skeletonCCD.getCladeToNodeMap(tree);
//        for (BitSet rogue : rogues) {
//            if (rogue.disjoint(rogueFilter)) {
//                continue;
//            }
//            annotateTreeWithRoguePlacement(baseCCD, skeletonCCD, tree, rogue, map);
//        }
//    }

    /**
     * Annotates the metadata string of the given tree with rogue placement information,
     * that is, for the given rogue clade with what probability it is attached to the tree
     * based on the probabilities derived from the given base CCD.
     *
     * @param baseCCD     on which filtered CCD and probabilities are based on
     * @param skeletonCCD CCD without rogue and containing given tree
     * @param tree        which gets annotated
     * @param rogue       clade for which rogue annotation should be added to tree metadata
     * @param map         allowed {@code null}; mapping from clades corresponding to vertices of tree to those vertices
     */
//    public static void annotateTreeWithRoguePlacement(AbstractCCD baseCCD, FilteredCCD skeletonCCD, Tree tree,
//                                                      BitSet rogue, Map<Clade, Node> map) {
//        if (map == null) {
//            map = skeletonCCD.getCladeToNodeMap(tree);
//        }
//
//        String rogueString = baseCCD.getTaxaNames(rogue, "-");
//        rogueString = "rogue" + rogueString.substring(1, rogueString.length() - 1);
//
//        // construct extended skeleton, that is, skeleton but with the given rogue included
//        BitSet filter = (BitSet) baseCCD.getTaxaAsBitSet().clone();
//        filter.andNot(rogue);
//        filter.andNot(skeletonCCD.getTaxaAsBitSet());
//        AbstractCCD extendedSkeletonCCD;
//        if (filter.isEmpty()) {
//            extendedSkeletonCCD = baseCCD;
//        } else {
//            extendedSkeletonCCD = new FilteredCCD(baseCCD, filter);
//        }
//        extendedSkeletonCCD.computeCladeProbabilities();
//
//        // in the extended skeleton, we can find all placements of the rogue clade,
//        // namely all the parents of the rogue
//        Clade rogueInExtSkeleton = extendedSkeletonCCD.getClade(rogue);
//        List<Clade> parents = rogueInExtSkeleton.getParentClades();
//
//        // for each parent (placement), we have to find the mapping in the skeleton CCD, that is:
//        // - the parent from the extended skeleton CCD gets mapped to a target clade
//        // - one of the incoming edges of the target clade is the right one
//        // - which one is decided by the grandparent of the rogue clade
//        // then have to check whether that edge is present in the tree,
//        // in particular, note that only one edge between grandParent~parent->targetClade can be in the tree.
//        // however, multiple grandparents can still be in the tree (forming a chain).
//        // for each edge not contained in tree, we lose placement probability
//        BitSet filteredBits = BitSet.newBitSet(baseCCD.getSizeOfLeavesArray()); // reused BitSet
//        double lostProbability = 0;
//        double sumProbability = 0;
//        for (Clade parent : parents) {
//            filteredBits.clear();
//            filteredBits.or(parent.getCladeInBits());
//            filteredBits.andNot(rogueInExtSkeleton.getCladeInBits());
//            Clade targetClade = skeletonCCD.getClade(filteredBits);
//
//            if (!map.containsKey(targetClade)) {
//                // tree does not contain the target clade of the edge the current rogue attaches to
//                // so the probability of the path going through the parent directly to the rogue clade
//                // is not represented by an edge in the MAP tree
//                double ccp = parent.getCladePartition(rogueInExtSkeleton).getCCP();
//                lostProbability += parent.getProbability() * ccp;
//                sumProbability += parent.getProbability() * ccp;
//
//            } else {
//                for (Clade grandparent : parent.getParentClades()) {
//                    filteredBits.clear();
//                    filteredBits.or(grandparent.getCladeInBits());
//                    filteredBits.andNot(rogueInExtSkeleton.getCladeInBits());
//                    // probability of the placement is given by probability of containing grandparent
//                    // and then taking path via current parent to rogue
//                    double prob = grandparent.getProbability() * grandparent.getCladePartition(parent).getCCP()
//                            * parent.getCladePartition(rogueInExtSkeleton).getCCP();
//                    Clade targetGrandparent = skeletonCCD.getClade(filteredBits);
//                    sumProbability += prob;
//
//                    if (!map.containsKey(targetGrandparent)) {
//                        // tree does not contain the parent of the target clade (original grandparent)
//                        // of the edge the current rogue attaches to
//                        // so the probability of the path going through this grandparent via parent
//                        // to the rogue clade is not represented by an edge in the given tree
//                        lostProbability += prob;
//                    } else {
//                        // however, multiple mapped grandparents could be in the given tree
//                        // but only one as parent of our target vertex
//                        Node vertex = map.get(targetClade);
//                        Node parentVertex = map.get(targetGrandparent);
//                        if (parentVertex.getChildren().contains(vertex)) {
//                            String placementInfo = rogueString + "=" + prob;
//                            if (vertex.metaDataString != null) {
//                                vertex.metaDataString += "," + placementInfo;
//                            } else {
//                                vertex.metaDataString = placementInfo;
//                            }
//
//                        } else {
//                            lostProbability += prob;
//                        }
//                    }
//                }
//            }
//        }
//
//        // System.out.print("rogue = " + rogue);
//        // System.out.print(" = " + baseCCD.getTaxaNames(rogue));
//        // System.out.print(", lost probability not present in tree: " + lostProbability);
//        // sumProbability = (Math.abs(sumProbability - 1.0) < AbstractCCD.PROBABILITY_ROUNDING_EPSILON) ? 1.0 : sumProbability;
//        // System.out.println(", total probability: " + sumProbability);
//    }

    /**
     * Computes the clade rogue score of the given clade C with respect to the given base CCD D,
     * that is, H(D) - H(D-C) + H(C).
     *
     * @param baseCCD CCD with respect to which rogue score is computed
     * @param clade   whose clade rogue score is computed
     * @param fccd    CCD with given clade filtered out; can be {@code null}
     * @return clade rogue score of given clade
     */
//    public static double computeCladeRogueScore(AbstractCCD baseCCD, Clade clade, FilteredCCD fccd) {
//        if (clade.isRoot()) {
//            return 0;
//        }
//
//        if (fccd == null) {
//            fccd = new FilteredCCD(baseCCD, clade.getCladeInBits());
//        }
//
//        double HD = baseCCD.getEntropy();
//        double HDC = fccd.getEntropy();
//
//        double HC = 0;
//        BitSet mask = null;
//        if (!clade.isLeaf()) {
//            mask = BitSetUtil.getToggled(clade.getCladeInBits(), baseCCD.getSizeOfLeavesArray());
//            FilteredCCD cladeCCD = new FilteredCCD(baseCCD, mask);
//            HC = cladeCCD.getEntropy();
//        }
//
//        double score = HD - HDC - HC;
//
//        if ((score < 0) && (-score < AbstractCCD.PROBABILITY_ROUNDING_EPSILON)) {
//            score = 0;
//        }
//        return score;
//    }

    /**
     * Computes the simple placement rogue score given by the entropy contribution (see {@link AbstractCCD#getEntropy()})
     * of all clade partitions that contain the given to the entropy of the given base CCD.
     *
     * @param baseCCD CCD with respect to which rogue score is computed
     * @param clade   whose placement rogue score is computed
     * @return simple placement rogue score of the given clade
     */
    public static double computePlacementRogueScore(AbstractCCD baseCCD, Clade clade) {
        baseCCD.computeCladeProbabilitiesIfDirty();
        double sum = 0;
        for (Clade parent : clade.getParentClades()) {
            CladePartition partition = parent.getCladePartition(clade);
            sum += partition.getProbability() * partition.getLogCCP();
        }
        return sum;
    }
}
