package viz.ccd;


import java.util.HashSet;

/**
 * This class represents a conditional clade distribution (CCD) obtained by
 * filtering another CCD, that is, removing a set of taxa and aggregating clades and partitions.
 * This class, unlike {@link FilteredCCD}, reuses parts of its base CCD that have not changed.
 *
 * @author Jonathan Klawitter
 */
public class AttachingFilteredCCD extends FilteredCCD {

    /** Helper bookkeeping set for construction */
    private HashSet<Clade> toFilterClades;

    /**
     * Creates a CCD based on the given CCD by taking out the taxa specified by the given mask.
     *
     * @param baseCCD      what the constructed CDD is based on
     * @param taxaToRemove mask specifying which taxa to remove
     */
    public AttachingFilteredCCD(AbstractCCD baseCCD, BitSet taxaToRemove) {
        super(baseCCD, taxaToRemove);
    }

    @Override
    protected void filter() {
        // when reusing parts of the base CCD, we want to identify which base clades are not affected
        // (which are exactly those that do not intersect with the filter mask;
        // the other clades (and clade partitions) can then link to these base clades
        // thus being able to reuse probability and other values efficiently
        toFilterClades = new HashSet<Clade>();
        Clade originalRootClade = baseCCD.getRootClade();
        this.identifyToFilterCladesRecursively(originalRootClade);

        processedClades.clear();
        for (Clade clade : baseCCD.getClades()) {
            if (!toFilterClades.contains(clade) && !clade.contained(removedTaxaMask)) {
                processedClades.add(clade);
                cladeMapping.put(clade.getCladeInBits(), clade);
            }
        }

        // we can then start the normal process where the method AttachingFilteredCCD#checkFilteringNecessity
        // deals with using base clades were appropriate
        this.rootClade = this.filterRecursively(originalRootClade);
    }

    /* Recursive helper method */
    private void identifyToFilterCladesRecursively(Clade clade) {
        // remember which clades we already processed as to not go down parts of CCD multiple times
        if (processedClades.contains(clade)) {
            return;
        }
        processedClades.add(clade);

        for (Clade childClade : clade.getChildClades()) {
            if (!processedClades.contains(childClade) && !childClade.contained(removedTaxaMask)) {
                identifyToFilterCladesRecursively(childClade);
            }
        }

        // we only need to look at clades that intersect the mask (contain filtered taxa)
        // but ...
        if (clade.intersects(removedTaxaMask)) {
            toFilterClades.add(clade);

            // then we also need the clade that will absorb them (if it exists)
            BitSet filteredBitSet = filterBitSet(clade.getCladeInBits());
            Clade absorbingClade = baseCCD.getClade(filteredBitSet);
            if (absorbingClade != null) {
                toFilterClades.add(absorbingClade);
            }
        }
    }

    @Override
    protected Clade checkFilteringNecessity(Clade clade, BitSet remainingTaxaBits) {
        Clade toReturn = super.checkFilteringNecessity(clade, remainingTaxaBits);
        if ((toReturn != null) && (!toFilterClades.contains(clade))) {
            processedClades.add(clade);
            cladeMapping.put(remainingTaxaBits, clade);
            toReturn = clade;
        }
        return toReturn;
    }

    @Override
    protected void setUpPartitions(Clade clade, Clade filteredClade, boolean storeParent) {
        super.setUpPartitions(clade, filteredClade, false);
    }

    @Override
    public double getEntropy() {
        return super.getEntropyLewis();
    }

    @Override
    public String toString() {
        return "Attaching" + super.toString();
    }
}
