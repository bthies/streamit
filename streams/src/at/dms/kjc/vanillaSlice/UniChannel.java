package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;
import at.dms.util.Utils;

public class UniChannel  {
    
    public static Channel getOrMakeChannel(Edge e) {
        Channel c = Channel.findChannel(e);
        if (c != null) {
            return c;
        }
        
        // TODO: lots of possibilities for channel implementations
        // add as needed.
        SliceNode src = e.getSrc();
        SliceNode dst = e.getDest();
        if (src instanceof InputSliceNode) {
            assert dst instanceof FilterSliceNode;
            // input -> filter
            Slice s = dst.getParent();
            if (sliceNeedsJoinerCode(s) && !filterNeedsPeekBuffer((FilterSliceNode)dst)) {
                String popName = 
                    ProcessInputSliceNode.getJoinerCode((InputSliceNode)src,UniBackEnd.backEndBits).
                       getMethods()[0].getName();
                c = UnbufferredPopChannel.getChannel(e,popName);
            } else if (!filterNeedsPeekBuffer((FilterSliceNode)dst)) {
                // single edge to InputSliceNode, no need for peek buffer: 
                // delegate to channel for InputSliceNode.
                Channel upstream = getOrMakeChannel(((InputSliceNode)src).getSingleEdge());
                c = DelegatingChannel.getChannel(e, upstream);
            } else {
                // make peek buffer as a channel
                if (FilterInfo.getFilterInfo((FilterSliceNode)dst).isSimple()) { 
                    // no items remain in channel between steady states.
                    c = ChannelAsArray.getChannel(e);
                } else {
                    // items remain in channel, need circular buffer (or copy-down, but circular is what we have)
                    c = ChannelAsCircularArray.getChannel(e);
                }
            }
        } else if (dst instanceof OutputSliceNode) {
            assert src instanceof FilterSliceNode;
            // filter --> output
            Slice s = dst.getParent();
            if (sliceNeedsSplitterCode(s)) {
                // the channel just needs to provide the name of the splitter entry point
                // for push() from the filter.
                String pushName = 
                    ProcessOutputSliceNode.getSplitterCode((OutputSliceNode)dst,UniBackEnd.backEndBits).
                        getMethods()[0].getName();
                c = UnbufferredPushChannel.getChannel(e,pushName);
            } else if (sliceHasDownstreamChannel(s)) {
                // there is no splitter code, this channel has no effect except delegating
                // to downstream channel.
                Channel downstream = getOrMakeChannel(((OutputSliceNode)dst).getDests()[0][0]);
                c = DelegatingChannel.getChannel(e, downstream);
            }
        } else {
            assert src instanceof OutputSliceNode && dst instanceof InputSliceNode;
            c = ChannelAsArray.getChannel(e); 
        }
        
        return c;
    }
    
    
    /**
     * Does filter need a peek buffer upstream of it?
     * Assumes 1 filter per slice.
     * Answer is <b>false</b> unless bufferring is needed to deal with
     * extra inputs between the InputliceNode and the FilterSliceNode.
     * @param filter
     * @return  whether filter needs peek buffer.
     */
    public static boolean filterNeedsPeekBuffer(FilterSliceNode filter) {
        if (! sliceHasUpstreamChannel(filter.getParent())) {
            // first filter on a slice with no input
            return false;
        }
        FilterInfo info = FilterInfo.getFilterInfo(filter);
        if (info.noBuffer()) {
            // a filter with a 0 peek rate can just pop from
            // joiner code if any.
            return false;
        }
        if (! info.isSimple() || 
                (sliceNeedsJoinerCode(filter.getParent()) && 
                 ! Utils.hasPeeks(filter.getFilter()))) {
            return true;
        } else {
            return false;
        }
    }

    /** @return true if slice has an upstream channel, false otherwise */
    public static boolean sliceHasUpstreamChannel(Slice s) {
        return s.getHead().getWidth() > 0;
        // s.getHead().getNext().getAsFilter().getFilter().getInputType() != CStdType.Void;
    }
    
    /** @return true if slice has a downstream channel, false otherwise */
    public static boolean sliceHasDownstreamChannel(Slice s) {
        return s.getTail().getWidth() > 0;
        //s.getTail().getPrevious().getAsFilter().getFilter().getOutputType() != CStdType.Void;
    }
   
    /**
     * Slice needs code for a joiner if it has input from more than one source.
     * @param s Slice
     * @return 
     */
    public static boolean sliceNeedsJoinerCode(Slice s) {
        return s.getHead().getWidth() > 1;
    }
    
    /**
     * Slice needs work function for a joiner if it has needs a joiner
     * and needs a peek buffer.  (Otherwise if it needs a joiner, it can
     * call the joiner as a function).
     * 
     * @param s Slice
     * @return
     */
    public static boolean sliceNeedsJoinerWorkFunction(Slice s) {
        // if needs peek buffer then needs joiner work function to transfer into peek buffer.
        return /*sliceNeedsJoinerCode(s) &&*/ filterNeedsPeekBuffer(s.getFilterNodes().get(0));
    }

    /**
     * Slice needs code for a splitter if it has output on more than one edge.
     * @param s
     * @return
     */
    public static boolean sliceNeedsSplitterCode(Slice s) {
        return s.getTail().getWidth() > 1;
    }
}
