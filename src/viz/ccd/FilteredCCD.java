package viz.ccd;

import java.util.HashSet;

/**
 * This class represents a conditional clade distribution (CCD) obtained by
 * filtering another CCD, that is, removing a set of taxa and aggregating clades
 * and partitions.
 *
 * @author Jonathan Klawitter
 */
public class FilteredCCD extends AbstractCCD {

    /** The CCD this filtered one is based on. */
    protected final AbstractCCD baseCCD;

    /** The original CCD any previous filtered CCD derived from. */
    protected final AbstractCCD rootCCD;

    /** The mask representing the taxon removed from the base CCD. */
    protected final BitSet removedTaxaMask;

    /**
     * Creates a CCD based on the given CCD by taking out the taxa specified by the given mask.
     *
     * @param baseCCD      what the constructed CDD is based on
     * @param taxaToRemove mask specifying which taxa to take out
     */
    public FilteredCCD(AbstractCCD baseCCD,
                       BitSet taxaToRemove) {
        super(baseCCD.getSizeOfLeavesArray(), false);
        if (baseCCD instanceof AttachingFilteredCCD) {
            throw new IllegalArgumentException("AttachingFilteredCCD cannot be further filtered on.");
        }
        // input checks
        if (taxaToRemove.length() > baseCCD.getSizeOfLeavesArray()) {
            throw new IllegalArgumentException("Highest bit in taxa to remove mask (" + taxaToRemove.length() + ") " +
                    "is larger than the number of original taxa (data structure wise, " + baseCCD.getNumberOfLeaves() + ").");
        }
        if (taxaToRemove.cardinality() == 0) {
            throw new IllegalArgumentException("Cannot filter CCD with empty taxa to remove mask.");
        }

        this.baseCCD = baseCCD;
        AbstractCCD prevCCD = baseCCD;
        while (prevCCD instanceof FilteredCCD) {
            prevCCD = ((FilteredCCD) prevCCD).getBaseCCD();
        }
        this.rootCCD = prevCCD;
        this.removedTaxaMask = taxaToRemove;
        this.baseTrees = baseCCD.getBaseTrees();
        this.numBaseTrees = baseCCD.getNumberOfBaseTrees();

        // construction
        filter();
        initialize();
    }

    /* Helper method for class specific construction behaviour */
    protected void filter() {
        Clade originalRootClade = baseCCD.getRootClade();
        this.rootClade = this.filterRecursively(originalRootClade);
    }

    /** Helper bookkeeping set for construction */
    protected HashSet<Clade> processedClades = new HashSet<>();

    /* Recursive helper method */
    protected Clade filterRecursively(Clade clade) {
        // 0. assume that clade is not contained in filter/does not collapse
        if (clade.contained(removedTaxaMask)) {
            throw new IllegalArgumentException(
                    "Illegal call to filter CCD. Given clade is contained in filter mask:" +
                            "\n clade " + clade + "\n filter " + removedTaxaMask);
        }
        // throughout need to ensure that no two (filtered) clades on the same taxa exist

        // 1. test if clade has already been processed
        BitSet remainingTaxaBits = filterBitSet(clade.getCladeInBits());
        Clade processedClade = checkFilteringNecessity(clade, remainingTaxaBits);
        if (processedClade != null) {
            return processedClade;
        }

        // 2. recursively filter the children first
        filterChildren(clade);

        // 3. set up new filtered clade or update existing one
        Clade filteredClade = setUpFilteredClade(clade, remainingTaxaBits);

        // 4. set up partitions
        setUpPartitions(clade, filteredClade, true);

        // 5. bookkeeping
        processedClades.add(clade);

        return filteredClade;
    }

    /* Helper method */
    protected Clade checkFilteringNecessity(Clade clade, BitSet remainingTaxaBits) {
        return processedClades.contains(clade) ? cladeMapping.get(remainingTaxaBits) : null;
    }

    /* Helper method */
    protected void filterChildren(Clade clade) {
        for (Clade childClade : clade.getChildClades()) {
            // ignore collapsing children
            if (!childClade.contained(removedTaxaMask)) {
                filterRecursively(childClade);
            }
        }
    }

    /* Helper method */
    protected Clade setUpFilteredClade(Clade clade, BitSet remainingTaxaBits) {
        // filtered clade might already exist, either as an absorbing child
        // clade or from another already processed original clade;
        // existing one is used because it might already be part of partitions
        Clade filteredClade = null;

        if (cladeMapping.containsKey(remainingTaxaBits)) {
            // 3a. filtered clade already exists
            filteredClade = cladeMapping.get(remainingTaxaBits);

            // have to add the registered occurrences and heights
            int occurrences = clade.getNumberOfOccurrences();
            double meanHeight = clade.getMeanOccurredHeight();

            for (CladePartition partition : clade.getPartitions()) {
                // from the current clade
                // (which is absorbed into the already existing filtered clade)
                // we only use the occurrences & heights
                // that do not come from partitions that correspond to the absorbing clade
                if (partition.getChildClades()[0].contained(removedTaxaMask)
                        || partition.getChildClades()[1].contained(removedTaxaMask)) {
                    meanHeight = (meanHeight * occurrences
                            - partition.getMeanOccurredHeight() * partition.getNumberOfOccurrences());
                    occurrences -= partition.getNumberOfOccurrences();
                    meanHeight = (occurrences != 0) ? (meanHeight / occurrences) : 0;
                }
            }

            filteredClade.increaseOccurrenceCountBy(occurrences, meanHeight);
        } else {
            // 3b. create new filtered clade
            filteredClade = createFilteredCladeCopy(clade, remainingTaxaBits);
        }

        return filteredClade;
    }

    /* Helper method */
    protected Clade createFilteredCladeCopy(Clade originalClade, BitSet remainingTaxaBits) {
        Clade filteredClade = new Clade(remainingTaxaBits, this);
        filteredClade.setBaseClade(originalClade);
        filteredClade.increaseOccurrenceCountBy(originalClade.getNumberOfOccurrences(),
                originalClade.getMeanOccurredHeight());

        cladeMapping.put(remainingTaxaBits, filteredClade);

        return filteredClade;
    }

    /* Helper method */
    protected void setUpPartitions(Clade clade, Clade filteredClade, boolean storeParent) {
        // if one or both children are filtered, the partition exists in filtered clade;
        // otherwise it contains a collapsing child and is ignored

        for (CladePartition partition : clade.getPartitions()) {
            Clade firstClade = partition.getChildClades()[0];
            BitSet firstBitsFiltered = filterBitSet(firstClade.getCladeInBits());
            Clade secondClade = partition.getChildClades()[1];
            BitSet secondBitsFiltered = filterBitSet(secondClade.getCladeInBits());

            if (filteredClade.equals(firstBitsFiltered)
                    || filteredClade.equals(secondBitsFiltered)) {
                // one clade of this partition equals the current (filtered) clade,
                // so the other is collapsing
                continue;
            }

            // for the new partitions, we have to use the stored clade,
            // which might have absorbed the original (even filtered) clade
            firstClade = cladeMapping.get(firstBitsFiltered);
            secondClade = cladeMapping.get(secondBitsFiltered);

            CladePartition filteredPartition = filteredClade.getCladePartition(firstClade,
                    secondClade);
            if (filteredPartition != null) {
                // case that filtered partition already exists
                filteredPartition.increaseOccurrenceCountBy(partition.getNumberOfOccurrences(),
                        partition.getMeanOccurredHeight());
            } else {
                // otherwise, create a new one
                filteredPartition = filteredClade.createCladePartition(firstClade, secondClade,
                        storeParent);
                filteredPartition.setNumOccurrences(partition.getNumberOfOccurrences());
                filteredPartition.setMeanOccurredHeight(partition.getMeanOccurredHeight());
            }
        }
    }

    /** @return the filter with which this CCD has been constructed */
    public BitSet getRemovedTaxaMask() {
        return removedTaxaMask;
    }

    /**
     * Returns a copy of the given BitSet with all bits cleared
     * that represent taxa removed for this filtered CCD.
     *
     * @param bits BitSet to be filtered
     * @return filtered copy of the given BitSet
     */
    protected BitSet filterBitSet(BitSet bits) {
        BitSet filteredBitSet = (BitSet) bits.clone();
        filteredBitSet.andNot(removedTaxaMask);
        return filteredBitSet;
    }

    /**
     * @return get the base conditional clade probability of this filtered one
     */
    public AbstractCCD getBaseCCD() {
        return baseCCD;
    }

    @Override
    public int getNumberOfLeaves() {
        return this.getRootClade().size();
    }

    @Override
    void checkCladePartitionRemoval(Clade clade, CladePartition partition) {
        // since we do not allow adding or removing trees, this should even not be called
        throw new IllegalStateException("Method should not be called on filtered CCD.");
    }

    @Override
    public Tree getSomeBaseTree() {
        return baseCCD.getSomeBaseTree();
    }

    @Override
    protected void tidyUpCacheIfDirty() {
        // nothing to do for filtered CCDs
    }

    @Override
    protected double getNumberOfParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilteredCCD copy() {
        throw new UnsupportedOperationException("Copying a filtered CCD is by design not supported.");
    }

    @Override
    public void initialize() {
        if (this.rootCCD instanceof CCD0) {
            CCD0.setPartitionProbabilities(this.rootClade);
            this.probabilitiesDirty = false;
        }
    }

    @Override
    public void addTree(Tree tree) {
        throw new UnsupportedOperationException("Adding trees not supported for filtered CCDs.");
    }

    @Override
    public void removeTree(Tree tree, boolean tidyUpCCDGraph) {
        throw new UnsupportedOperationException("Removing trees not supported for filtered CCDs.");
    }

    @Override
    public String toString() {
        String ccdToString = super.toString();
        return "FilteredCCD" + ccdToString.substring(0, ccdToString.length() - 1) + ", filter: " + removedTaxaMask + "]";
    }
}
