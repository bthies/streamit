package at.dms.kjc.flatgraph;

/**
 * Interface for a private class used in {@link FlatWeights}.
 * 
 * Used to co-ordinate offsets in splitter / joiner weights
 * with offsets in {@link at.dms.kjc.flatgraph.FlatNode#edges} / {@link at.dms.kjc.flatgraph.FlatNode#incoming}. 
 * 
 * Note that these offsets may be different of there are weights of 0 in
 * a splitter or joiner, since weights of 0 do not lead to edges being
 * created in a FlatNode.
 * 
 * @author Allyn
 *
 */
public interface FlatWeight {
    /**
     * Get the weight for the current edge.
     * @return int
     */
    public int getWeight();
    /**
     * Get the node at the end of the current edge.
     * @return FlatNode
     */
    public FlatNode getNode();
    /**
     * Get offset of the current edge's weight in 
     * {@link at.dms.kjc.sir.SIRSplitter#getWeights()} or
     * {@link at.dms.kjc.sir.SIRJoiner#getWeights()}.
     * 
     * @return offset
     */
    public int getWeightsOffset();
    /**
     * Get offset of the current edge in
     * {@link at.dms.kjc.flatgraph.FlatNode#edges} / {@link at.dms.kjc.flatgraph.FlatNode#incoming}.
     * 
     * @return offset
     */
    public int getNodesOffset();
}
