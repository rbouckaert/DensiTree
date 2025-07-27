package viz.ccd;

/**
 * This class represents an extended clade used for CCD2s
 * where 'extended' is in the sense that also the clades sibling is stored.
 * There can thus be two clades on the same taxa but with different siblings.
 *
 * @author Jonathan Klawitter
 */
public class ExtendedClade extends Clade {

    ExtendedClade sibling;

    /**
     * Construct a new Clade on the taxa specified by the given BitSet and being
     * part of the given CCD.
     *
     * @param cladeInBits BitSet representation of the clade
     * @param abstractCCD CCD this clade is part of
     */
    public ExtendedClade(BitSet cladeInBits, AbstractCCD abstractCCD) {
        super(cladeInBits, abstractCCD);
    }

    /**
     * Construct a new Clade on the taxa specified by the given BitSet and being
     * part of the given CCD.
     *
     * @param cladeInBits BitSet representation of the clade
     * @param sibling     the sibling clade of this one
     * @param abstractCCD CCD this clade is part of
     */
    public ExtendedClade(BitSet cladeInBits, ExtendedClade sibling, AbstractCCD abstractCCD) {
        super(cladeInBits, abstractCCD);
        this.sibling = sibling;
    }

    public void setSibling(ExtendedClade sibling) {
        this.sibling = sibling;
    }

    public ExtendedClade getSibling() {
        return this.sibling;
    }
}
