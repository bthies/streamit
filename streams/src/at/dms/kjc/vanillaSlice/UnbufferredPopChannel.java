package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.*;
import at.dms.util.Utils;

/**
 * A Channel with the single purpose of holding the name of a pop() routine.
 * Such a channel is used to connect a joiner to a peek buffer or directly to a filter
 * when the joiner and the filter are laid out on the same ComputeNode.
 * 
 * 
 * @author dimock
 *
 */
public class UnbufferredPopChannel extends Channel {

    private String popName;
    
    /**
     * Make a new Channel or return an already-made channel.
     * @param edge     The edge that this channel implements.
     * @param popName  The name of the pop routine that will be used.
     * @return A channel for the passed edge with a where popMethodName() returns <b>popName</b>.
     */
    public static UnbufferredPopChannel getChannel(Edge edge, String popName) {
        Channel chan = Channel.bufferStore.get(edge);
        if (chan == null) {
            chan = new UnbufferredPopChannel(edge, popName);
            Channel.bufferStore.put(edge, chan);
        } else {
            assert chan instanceof UnbufferredPopChannel
                    && chan.popMethodName().equals(popName);
        }
        return (UnbufferredPopChannel) chan;
    }
    
    private UnbufferredPopChannel(Edge edge, String popName) {
        super(edge);
        this.popName = popName;
    }
    
    /**
     * popMethodName: overridden to return string from construction time.
     */
    @Override
    public String popMethodName() {
        return popName;
    }
}
