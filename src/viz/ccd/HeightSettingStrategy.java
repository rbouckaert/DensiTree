package viz.ccd;

/**
 * Different strategies to assign heights to vertices of a tree.
 */
public enum HeightSettingStrategy {
    MeanOccurredHeights("Mean (sampled) heights"), //
    CommonAncestorHeights("Mean of Least Common Ancestor heights"), //
    One("Leaves at mean heights, parent one above higher child"), //
    None("No heights set");

    String description;

    HeightSettingStrategy(String description) {
        this.description = description;
    }

//    public static HeightSettingStrategy fromName(String name) {
//        return switch (name.toUpperCase()) {
//            case "CA", "LCA", "COMMONANCESTOR", "COMMONANCESTORHEIGHTS" -> CommonAncestorHeights;
//            case "MH", "MEANOCCURRED", "MEANOCCURREDHEIGHTS" -> MeanOccurredHeights;
//            default -> One;
//        };
//    }

    @Override
    public String toString() {
        return description;
    }
}
