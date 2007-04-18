package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;
import at.dms.util.Utils;

/**
 * Create channels of appropriate type for this back end.
 * @author dimock
 *
 */

public class UniChannel  {
    /**
     * Given an edge <b>e</b>, make the correct sort of channel for it.
     * @param e  an edge.
     * @return an existing channel if one exists, else a newly made channel.
     */
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
            if (UniBackEnd.backEndBits.sliceNeedsJoinerCode(s) && 
                    !UniBackEnd.backEndBits.filterNeedsPeekBuffer((FilterSliceNode)dst)) {
                String popName = 
                    ProcessInputSliceNode.getJoinerCode((InputSliceNode)src,UniBackEnd.backEndBits).
                       getMethods()[0].getName();
                c = UnbufferredPopChannel.getChannel(e,popName);
            } else if (!UniBackEnd.backEndBits.filterNeedsPeekBuffer((FilterSliceNode)dst)) {
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
            if (UniBackEnd.backEndBits.sliceNeedsSplitterCode(s)) {
                // the channel just needs to provide the name of the splitter entry point
                // for push() from the filter.
                String pushName = 
                    ProcessOutputSliceNode.getSplitterCode((OutputSliceNode)dst,UniBackEnd.backEndBits).
                        getMethods()[0].getName();
                c = UnbufferredPushChannel.getChannel(e,pushName);
            } else if (UniBackEnd.backEndBits.sliceHasDownstreamChannel(s)) {
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
}
