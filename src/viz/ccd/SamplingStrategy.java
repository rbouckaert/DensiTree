package viz.ccd;

/**
 * Different strategies to sample a tree from a distribution.
 */
public enum SamplingStrategy {

    Sampling("Randomly sample tree"), //
    MAP("Tree with max probability"), //
    MaxSumCladeCredibility("Tree with max sum clade probabilities");

    String description;

    SamplingStrategy(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

}
