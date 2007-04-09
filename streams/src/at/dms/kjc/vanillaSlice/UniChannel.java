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
                c = DelegatingChannel.getChannel(((OutputSliceNode)dst).getDests()[0][0], downstream);
            }
        } else {
            assert src instanceof OutputSliceNode && dst instanceof InputSliceNode;
            c = ChannelAsArray.getChannel(e); 
        }
        
        return c;
    }
    
    
    /**
     * Assumes 1 filter per slice...
     * Answer is <b>false</b> if ...
     * @param filter
     * @return
     */
    public static boolean filterNeedsPeekBuffer(FilterSliceNode filter) {
        if (! sliceHasUpstreamChannel(filter.getParent())) {
            // first filter on a slice with no input
            return false;
        }
        FilterInfo info = FilterInfo.getFilterInfo(filter);
        if (info.noBuffer()) {
            // a filter with a 0 peek rate can just pop from
            // from 
            return false;
        }
        if (! info.isSimple() || 
                (sliceNeedsJoinerCode(filter.getParent()) && 
                 ! Utils.hasPeeks(filter.getFilter()))) {
            return true;
        } else {
            return true;
        }
    }

    public static boolean sliceHasUpstreamChannel(Slice s) {
        return s.getHead().getWidth() > 0;
        // s.getHead().getNext().getAsFilter().getFilter().getInputType() != CStdType.Void;
    }
    
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
        return sliceNeedsJoinerCode(s) && filterNeedsPeekBuffer(s.getFilterNodes().get(0));
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
