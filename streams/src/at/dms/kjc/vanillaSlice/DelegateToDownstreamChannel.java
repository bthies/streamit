package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;

/**
 * A Channel that delegates all useful work to another channel.
 * This is used to maintain one Channel per Edge in the case where
 * a SliceNode does not generate any code.
 * @author dimock
 */
public class DelegateToDownstreamChannel extends Channel {
    
    private Channel other;
    /**
     * Make a new Channel or return an already-made channel.
     * @param edge     The edge that this channel implements.
     * @param other    The channel that this delegates to.
     * @return A channel for this edge, that 
     */
    public static DelegateToDownstreamChannel getChannel(Edge edge, Channel other) {
        Channel oldChan = Channel.bufferStore.get(edge);
        if (oldChan == null) {
            DelegateToDownstreamChannel chan = new DelegateToDownstreamChannel(edge, other);
            Channel.bufferStore.put(edge, chan);
            return chan;
       } else {
            assert oldChan instanceof DelegateToDownstreamChannel; 
            return (DelegateToDownstreamChannel)oldChan;
        }
    }
    
    private DelegateToDownstreamChannel(Edge edge, Channel other) {
        super(edge);
        this.other = other;
    }

    // TODO: all methods for a channel must be implemented as delegating...
    
}
