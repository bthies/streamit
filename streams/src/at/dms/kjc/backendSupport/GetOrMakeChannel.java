package at.dms.kjc.backendSupport;

import at.dms.kjc.slicegraph.*;

/**
 * Create channels of appropriate type for a back end.
 * Provided version creates channels in shared memory, but can be overridden as necessary.
 * @author dimock
 *
 */

public class GetOrMakeChannel  {
    /**
     * switch to turn on dumping out of channels
     */
    private static boolean debug = false;
    
    protected BackEndFactory backEndBits;
    
    /**
     * Crete a channel selector giving it a BackEndFactory to query.
     * @param backendBits
     */
    public GetOrMakeChannel(BackEndFactory backEndBits) {
        this.backEndBits = backEndBits;
    }
    
    /** You must use constructor passing BackEndFactory! */
    private GetOrMakeChannel() {}
    
    /**
     * Given an edge <b>e</b>, make the correct sort of channel for it.
     * @param e  an edge.
     * @return an existing channel if one exists, else a newly made channel.
     */
    public Channel getOrMakeChannel(Edge e) {
        Channel c = Channel.findChannel(e);
        if (c != null) {
            return c;
        }
        SliceNode src = e.getSrc();
        SliceNode dst = e.getDest();

        if (src instanceof OutputSliceNode && dst instanceof InputSliceNode) {
            c = makeInterSliceChannel((InterSliceEdge)e);
        } else {
            c = makeIntraSliceChannel(e);
        }
        
        if (debug) {
            // purely for debugging purposes:
            // dump channel type, and if array then capacity.
            System.err.print("(Channel ");
            System.err.print(c.getClass().getSimpleName());
            if (src instanceof FilterSliceNode) {
                System.err.print(" " + src.getAsFilter().getFilter().getName());
            } else if (src instanceof InputSliceNode) {
                System.err.print(" " + "joiner_"
                        + src.getNext().getAsFilter().getFilter().getName());
            } else {
                assert src instanceof OutputSliceNode;
                System.err
                        .print(" "
                                + "splitter_"
                                + src.getPrevious().getAsFilter().getFilter()
                                        .getName());
            }
            if (dst instanceof FilterSliceNode) {
                System.err.print(" " + dst.getAsFilter().getFilter().getName());
            } else if (dst instanceof InputSliceNode) {
                System.err.print(" " + "joiner_"
                        + dst.getNext().getAsFilter().getFilter().getName());
            } else {
                assert dst instanceof OutputSliceNode;
                System.err
                        .print(" "
                                + "splitter_"
                                + dst.getPrevious().getAsFilter().getFilter()
                                        .getName());
            }
            System.err.print(" " + BufferSize.calculateSize(e));
            if (c instanceof ChannelAsArray) {
                System.err.print(" " + ((ChannelAsArray) c).getBufSize());
            }
            System.err.println(")");
        }

        return c;
    }
     
    /**
     * For an edge within a slice, create a channel that implements that edge.
     * The provided code assumes that 
     * (1) there are no filter->filter edges.
     * (2) That there is never a need to buffer data between a filter and a following splitter
     * (i.e. the inter-slice channels connected to by the splitter will provide any necessary bufferring).
     * If this assumption changes then update this code to handle the case.
     * <p>
     * This routine may need to be overridden in the case where it is necessary to buffer
     * data off chip between a joiner and a filter or between a filter and a splitter, or in the
     * case where we are not using simple slices and there are filter->filter edges.
     * </p> 
     * @param e an Edge
     * @return a channel that implements the edge or <b>null</b> if no data passes over the edge.
     */
    protected Channel makeIntraSliceChannel(Edge e) {
        SliceNode src = e.getSrc();
        SliceNode dst = e.getDest();

        Channel c;
                
        if (src instanceof InputSliceNode) {
            assert dst instanceof FilterSliceNode;
            // input -> filter
            Slice s = dst.getParent();
            // implicit assumption here: 
            // ! sliceNeedsPeekBuffer(s) -> ! sliceNeedsJoinerWorkFunction(s)
            // so either connect to joiner code using an unbufferred channel
            // or bypass non-existent joiner code using a delegating channel.
            if (!backEndBits.sliceNeedsPeekBuffer(s)) {
                // do not make a peek buffer
                if (backEndBits.sliceNeedsJoinerCode(s)) {
                    // joiner code connected directly to filter via a channel
                    // containing no storage
                    String popName = ProcessInputSliceNode.getJoinerCode(
                            (InputSliceNode) src, backEndBits)
                            .getMethods()[0].getName();
                    c = UnbufferredPopChannel.getChannel(e, popName);
                    for (InterSliceEdge joiner_edge : ((InputSliceNode) src).getSourceList(SchedulingPhase.STEADY)) {
                        ((UnbufferredPopChannel)c).addChannelForHeaders(getOrMakeChannel(joiner_edge));
                    }
                } else if (backEndBits.sliceHasUpstreamChannel(s)) {
                    // no joiner at all: delegate to the channel for InputSliceNode.
                    Channel upstream = getOrMakeChannel(((InputSliceNode) src)
                            .getSingleEdge(SchedulingPhase.STEADY));
                    c = DelegatingChannel.getChannel(e, upstream);
                } else {
                    // no data over edge: return null channel.
                    c = null;
                }
            } else {
                // make peek buffer as a channel
                if (FilterInfo.getFilterInfo((FilterSliceNode) dst).isSimple()) {
                    // no items remain in channel between steady states.
                    c = ChannelAsArray.getChannel(e);
                } else {
                    // items remain in channel, need circular buffer (or
                    // copy-down, but circular is what we have)
                    c = ChannelAsCircularArray.getChannel(e);
                }
            }
        } else if (dst instanceof OutputSliceNode) {
            assert src instanceof FilterSliceNode;
            // filter --> output
            Slice s = dst.getParent();
            // assumes slice does not need a work function to drive splitter:
            // splitter code can be connected directly to filter via an unbufferred channel
            // or bypass non-existent splitter code delegating to interslice channel.
            if (backEndBits.sliceNeedsSplitterCode(s)) {
                // the channel just needs to provide the name of the splitter entry point
                // for push() from the filter.
                String pushName = 
                    ProcessOutputSliceNode.getSplitterCode((OutputSliceNode)dst,backEndBits).
                        getMethods()[0].getName();
                c = UnbufferredPushChannel.getChannel(e,pushName);
                for (InterSliceEdge joiner_edge : ((OutputSliceNode) dst).getDestSequence(SchedulingPhase.STEADY)) {
                    ((UnbufferredPushChannel)c).addChannelForHeaders(getOrMakeChannel(joiner_edge));
                }
            } else if (backEndBits.sliceHasDownstreamChannel(s)) {
                // there is no splitter code, this channel has no effect except delegating
                // to downstream channel.
                Channel downstream = getOrMakeChannel(((OutputSliceNode)dst).getDests(SchedulingPhase.STEADY)[0][0]);
                c = DelegatingChannel.getChannel(e, downstream);
            } else {
                c = null;
            }
        } else {
            throw new AssertionError("expecting joiner->filter or filter->splitter: " + e);
        }
        return c;
    }
    
    /**
     * Create channel between slices.
     * The default version here assumes shared memory, and implements a channel as an array.
     * <p>
     * A subclass may call super.makeInterSliceChannel(e) if both ends of the channel
     * are laid out on the same ComputeNode and if using an array in the ComputeNode's memory
     * is an appropriate way of communicating.
     * Otherwise a subclass will make a channel of the appropriate type.
     * </p>
     */
    
    protected Channel makeInterSliceChannel(InterSliceEdge e) {
        Channel c;
        if (e.initItems() > e.steadyItems()) {
            // items left on channel between steady states
            c = ChannelAsCircularArray.getChannel(e);
        } else {
            // no items left on channel between steady states
            c = ChannelAsArray.getChannel(e);
        }
        return c;
    }
}
