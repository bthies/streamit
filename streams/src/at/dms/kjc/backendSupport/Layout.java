/**
 * 
 */
package at.dms.kjc.backendSupport;

import at.dms.kjc.slicegraph.SliceNode;

/**
 * A Layout makes the association between a {@link at.dms.kjc.backendSupport.ComputeNode ComputeNode} and a {@link at.dms.kjc.slicegraph.SliceNode}.
 * @param T  a subtype of {@link at.dms.kjc.backendSupport.ComputeNode ComputeNode}
 * @author mgordon
 *
 */
public interface Layout<T extends at.dms.kjc.backendSupport.ComputeNode> {
    /** Get the ComputeNode for a Slice 
     * @param node : the {@link at.dms.kjc.slicegraph.SliceNode} to look up. 
     * @return the {@link at.dms.kjc.backendSupport.ComputeNode ComputeNode} that should execute the {@link at.dms.kjc.slicegraph.SliceNode}. */
    public T getComputeNode(SliceNode node);
    /** Set the ComputeNode for a Slice 
     * @param node         the {@link at.dms.kjc.slicegraph.SliceNode} to associate with ...
     * @param computeNode  the {@link at.dms.kjc.backendSupport.ComputeNode ComputeNode} where the {@link at.dms.kjc.slicegraph.SliceNode}  should execute.
     */
    public void setComputeNode(SliceNode node, T computeNode);
    /** Do the setup for {@link #getComputeNode(SliceNode) getComputeNode}. */
    public void runLayout();
}