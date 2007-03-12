/**
 * 
 */
package at.dms.kjc.spacetime;

import at.dms.kjc.slicegraph.FilterSliceNode;

/**
 * A Layout makes the association between a {@link at.dms.kjc.backendSupport.ComputeNode ComputeNode} and a {@link at.dms.kjc.slicegraph.FilterSliceNode}.
 * A Layout is what a {@link at.dms.kjc.slicegraph.Partitioner Partitioner} returns.
 * @param T  a subtype of {@link at.dms.kjc.backendSupport.ComputeNode ComputeNode}
 * @author mgordon
 *
 */
public interface Layout<T extends at.dms.kjc.backendSupport.ComputeNode> {
    /** Get the ComputeNode for a FilterSlice 
     * @param node : the {@link at.dms.kjc.slicegraph.FilterSliceNode} to look up. 
     * @return the {@link at.dms.kjc.backendSupport.ComputeNode ComputeNode} that should execute the {@link at.dms.kjc.slicegraph.FilterSliceNode}. */
    public T getComputeNode(FilterSliceNode node);
    /** Set the ComputeNode for a FilterSlice 
     * @param node         the {@link at.dms.kjc.slicegraph.FilterSliceNode} to associate with ...
     * @param computeNode  *the {@link at.dms.kjc.backendSupport.ComputeNode ComputeNode} where the {@link at.dms.kjc.slicegraph.FilterSliceNode}  should execute.
     */
    public void setComputeNode(FilterSliceNode node, T computeNode);
    /** Do the setup for {@link #getComputeNode(FilterSliceNode) getComputeNode}. */
    public void run();
}