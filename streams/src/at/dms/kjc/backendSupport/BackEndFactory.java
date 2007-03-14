package at.dms.kjc.backendSupport;

import at.dms.kjc.slicegraph.*;

import java.util.Collection;

/**
 * Factor out parts of back end that need matching types.
 * A BackEndFactory should generate the parts needed for a back end.
 *
 * @author dimock
 * @param <ComputeNodesType>
 *            Instantiate to type of a collection of {@link ComputeNode}s.
 * @param <ComputeNodeType>
 *            Instantiate to type of an individual {@link ComputeNode}
 * @param <CodeStoreType>
 *            Instantiate to type of a {@link ComputeCodeStore}.
 * @param <ComputeNodeSelectorArgType>
 *            Instantiate to base type of array of extra arguments to
 *            getComputeNode. (Necessary for overriding method with reasonable
 *            types.)

 * @see BackEndAbsFactory
 */

public abstract class BackEndFactory<
     ComputeNodesType extends ComputeNodesI<?>, 
     ComputeNodeType extends ComputeNode<?>, 
     CodeStoreType extends ComputeCodeStore<?>, 
     ComputeNodeSelectorArgType extends Object>
{
    /**
     * @return Singleton to generate {@link Channel}s and
     *         {@link ComputeCodeStore}s for the {@link ComputeNodes}.
     * @param <...> needs same parameterization as this so as to be able to refer to this.
     */

    public abstract <T 
        extends BackEndScaffold<ComputeNodesType,ComputeNodeType,CodeStoreType, ComputeNodeSelectorArgType>
        > T getBackEndMain();

    /**
     * Process an input slice node: find the correct ProcElement(s) and add joiner code, and buffers.
     * please delegate work to some other object.
     * @param input           the InputSliceNode 
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param computeNodes    the available compute nodes.
     * 
     */
    public abstract void processInputSliceNode(InputSliceNode input,
            SchedulingPhase whichPhase, ComputeNodesType computeNodes);
    
    /**
     * Process all filter slice nodes in a Slice (just one in a SimpleSlice): find the correct ProcElement(s) and add filter code.
     * please delegate work to some other object.
     * @param slice           Slice containing filters
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param computeNodes    the available compute nodes.
     */
    public abstract void processFilterSlices(Slice slice, 
            SchedulingPhase whichPhase, ComputeNodesType computeNodes);

    /**
     * Process a filter slice node: find the correct ProcElement(s) and add code and buffers.
     * please delegate work to some other object.
     * @param filter          the FilterSliceNode.
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param computeNodes    the available compute nodes.
     */

    public abstract void processFilterSliceNode(FilterSliceNode filter,
            SchedulingPhase whichPhase, ComputeNodesType computeNodes);
   
    /**
     * Process an output slice node: find the correct ProcElement(s) and add splitter code, and buffers.
     * please delegate work to some other object.
     * @param output          the OutputSliceNode.
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param computeNodes    the available compute nodes.
     */
    public abstract void processOutputSliceNode(OutputSliceNode output,
            SchedulingPhase whichPhase, ComputeNodesType computeNodes);

     /**
     * @return Get the (unique) collection of nodes involved in computation.
     */
    public abstract ComputeNodesType getComputeNodes();

    /**
     * @return A (unique per parent) {@link ComputeCodeStore}.
     */
    public abstract CodeStoreType getComputeCodeStore(ComputeNodeType parent);

    /**
     * Get a specified compute node.
     * Assumes that BackEndFactory.getComputeNodes() returns a collection
     * of ComputeNode including the desired node.
     * @param specifier
     *            Different instantiations will have different number of
     *            arguments to specify which node, so a specifier (String, int, array[int]...) here.
     * @return a (unique per specifier) ComputeNode
     */
    public abstract ComputeNodeType getComputeNode(ComputeNodeSelectorArgType specifier);
    
    
    protected Layout<ComputeNodeType> layout;

    /**
     * Keep a copy of the {@link Layout}: the mapping from {@link at.dms.kjc.slicegraph.SliceNode SliceNode} to 
     * {@link ComputeNode}.
     * @param layout
     */
    public void setLayout(Layout<ComputeNodeType> layout) {
        this.layout = layout;
    }
    
    /**
     * Get saved copy of {@link Layout}.
     * @return
     */
    public Layout<ComputeNodeType> getLayout() {
        return layout;
    }

    /**
     * Back end needs to accumulate channels to pass to the code emitter.
     * This function should return that collection of channels.
     * @return some collection of Channel s for the code emitter's use.
     */
    public Collection<Channel> getChannels() {
        return Channel.getBuffers();
    }
}
