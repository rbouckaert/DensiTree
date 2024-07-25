package viz.ccd;

/**
 * Different strategies to assign heights to vertices of a tree.
 */
public enum HeightSettingStrategy {
    MeanOccurredHeights("Mean (sampled) heights"), //
    MeanLCAHeight("Mean of Least Common Ancestor heights"), //
    One("Min branch length 1, contemperaneous leaves"), //
    None("No heights/branch lenghts set");

    String description;

    HeightSettingStrategy(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
