package at.dms.kjc.backendSupport;

import at.dms.kjc.slicegraph.*;
import at.dms.util.Utils;

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

    public abstract <T extends BackEndScaffold> T getBackEndMain();

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
        
    /**
     * Back end needs to generate subclasses of channel.
     * Routine here to get a channel that implements communication over an edge.
     * @param e the edge.
     * @return a channel: preexisting or newly created.
     */
    
    public abstract Channel getChannel(Edge e);
    
    /**
     * Back end needs to generate subclasses of channel.
     * Routine here to get a channel from a source to a destination.
     * @param src
     * @param dst
     * @return a channel: preexisting or newly created.
     */
    public abstract Channel getChannel(SliceNode src, SliceNode dst);

    /**
     * Select a CodeStoreHelper subclass given a SliceNode.
     * A CodeStoreHelper generates wrapper code combining code
     * for channels with code for a SliceNode.
     * @param node the SliceNode.
     * @return an instance of CodeStoreHelper
     */
    public abstract CodeStoreHelper getCodeStoreHelper(SliceNode node);

    /**
     * Does slice need a buffer between its final filter and its splitter?
     */
    public boolean sliceNeedsPokeBuffer(Slice s) {
        return false;
    }

    /**
     * Does slice need a peek buffer before first filter?
     * Really depends on where input is coming from: filter on this ComputeNode?
     * Joiner on this ComputeNode?,  Filter on another Compute Node?  If so is there
     * shared memory? ...
     * Answer is <b>false</b> unless bufferring is needed to deal with
     * unconsumed inputs or extra peeks.
     * @param s a Slice
     * @return  whether first filter needs a peek buffer.
     */
    public boolean sliceNeedsPeekBuffer(Slice s) {
        // todo:  push this down to UniBackEndFactory.
        if (! sliceHasUpstreamChannel(s)) {
            // first filter on a slice with no input
            return false;
        }
        FilterSliceNode filter = s.getFilterNodes().get(0);
        FilterInfo info = FilterInfo.getFilterInfo(filter);
        if (info.noBuffer()) {
            // a filter with a 0 peek rate does not need
            // a peek buffer (is this redundant with !sliceHasUpstreamChannel ?)
            return false;
        }
        if (sliceNeedsJoinerCode(filter.getParent()) && 
                 Utils.hasPeeks(filter.getFilter())) {
            // if filter performs peeks and has joiner code upstream
            // then filter needs a peek buffer since it can not peek through a joiner.
            return true;
        } else {
            return info.initItemsReceived() != info.initItemsNeeded;
        }
    }

    /** @return true if slice has an upstream channel that it needs to receive data from, false otherwise.
     * May want to set false if upstream channel is to off-chip device and code for a filter controls that
     * device, but this implementation seems a good default. */
    public boolean sliceHasUpstreamChannel(Slice s) {
        return s.getHead().getWidth(SchedulingPhase.STEADY) > 0;
    }

    /** @return true if slice has a downstream channel that it needs to send data to, false otherwise 
     * May want to set false if downstream channel is to off-chip device and code for a filter controls that
     * device, but this implementation seems a good default. */
    public boolean sliceHasDownstreamChannel(Slice s) {
        return s.getTail().getWidth(SchedulingPhase.STEADY) > 0;
    }

    /**
     * Slice needs code for a joiner if it has input from more than one source.
     * @param s Slice
     * @return 
     */
    public boolean sliceNeedsJoinerCode(Slice s) {
        return s.getHead().getWidth(SchedulingPhase.STEADY) > 1;
    }

    /**
     * Slice needs work function for a joiner.
     * We distinguish this from {@link #sliceNeedsJoinerCode(Slice)} since
     * there may need to be code generated for a joiner, but the work function may be
     * rolled into the code for a filter.
     * 
     * @param s Slice
     * @return
     */
    public boolean sliceNeedsJoinerWorkFunction(Slice s) {
        // if needs peek buffer then needs joiner work function to transfer into peek buffer.
        return sliceNeedsPeekBuffer(s);
    }

    /**
     * Slice needs code for a splitter if it has output on more than one edge.
     * @param s
     * @return
     */
    public boolean sliceNeedsSplitterCode(Slice s) {
        return s.getTail().getWidth(SchedulingPhase.STEADY) > 1;
    }

    /**
     * Haven't yet found a situation where we need to buffer output to splitter
     * but may well: perhaps if prework pushes different number of items from
     * what work pushes?
     * @param s
     * @return
     */
    public boolean sliceNeedsSplitterWorkFunction(Slice s) {
        return false;
    }
}
