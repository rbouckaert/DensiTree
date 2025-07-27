package viz.ccd;



import java.math.BigInteger;
import java.util.Collection;

public interface ITreeDistribution {

    /**
     * Randomly samples a tree (without heights set) from this distribution.
     *
     * @return a tree sampled from this distribution
     */
//    public Tree sampleTree();

    /**
     * Randomly samples a tree from this distribution with heights set with the
     * given strategy.
     *
     * @param heightStrategy the strategy used to set the heights of the tree vertices
     * @return a tree sampled from this distribution
     */
//    public Tree sampleTree(HeightSettingStrategy heightStrategy);

    /**
     * Return the probability of a randomly sampled tree from this distribution.
     *
     * @return probability of randomly sampled tree
     */
//    public double sampleTreeProbability();

    /**
     * Returns the tree (without heights set) with maximum probability in this
     * distribution.
     *
     * @return the tree with maximum probability
     */
    public Tree getMAPTree();

    /**
     * Returns the tree with maximum probability in this distribution with
     * heights set with the given strategy.
     *
     * @param heightStrategy the strategy used to set the heights of the tree vertices
     * @return the tree with maximum probability
     */
    public Tree getMAPTree(HeightSettingStrategy heightStrategy);

    /** @return the maximum probability of any tree in this distribution */
    public double getMaxTreeProbability();

    /**
     * Return the probability of the given tree in this distribution.
     *
     * @param tree whose probability is requested
     * @return the probability of the given tree
     */
    public double getProbabilityOfTree(Tree tree);

    /**
     * Returns whether this distribution contains the given tree.
     *
     * @param tree tested whether contained
     * @return whether this distribution contains the given tree
     */
    public boolean containsTree(Tree tree);

    /** @return the number of trees (topologies) in this distribution */
    public BigInteger getNumberOfTrees();

    /**
     * Return the probability of the given clade.
     *
     * @param cladeInBits whose probability id requested
     * @return the probability of the given clade
     */
    public double getCladeProbability(BitSet cladeInBits);

    /** @return number of leaves/taxa of the trees in this distribution */
    public int getNumberOfLeaves();

    /** @return the number of distinct clades in this distribution */
    public int getNumberOfClades();

    /** @return all clades of this distribution */
    public Collection<Clade> getClades();

    /**
     * Return the min credible level of the given tree, that is,
     * the smallest credible set that still contains this tree.
     *
     * @param tree whose cred level is requested
     * @param type what type of information to use
     * @return min credible level of given tree
     */
//    public double getCredibleLevel(Tree tree, CredibleSetType type);

}
