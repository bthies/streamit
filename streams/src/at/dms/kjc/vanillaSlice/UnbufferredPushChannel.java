package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.slicegraph.Edge;
import java.util.*;
import at.dms.kjc.JMethodDeclaration;
/**
 * A Channel with the single purpose of holding the name of a push() routine.
 * Such a channel is used to connect a filter to a splitter
 * when the splitter and the filter are laid out on the same ComputeNode.
 * 
 * @author dimock
 *
 */
public class UnbufferredPushChannel extends Channel {

    private String pushName;
    
    /**
     * Make a new Channel or return an already-made channel.
     * @param edge     The edge that this channel implements.
     * @param pushName  The name of the push routine that will be used.
     * @return A channel for the passed edge with a where pushMethodName() returns <b>pushName</b>.
     */
    public static UnbufferredPushChannel getChannel(Edge edge, String pushName) {
        Channel oldChan = Channel.bufferStore.get(edge);
        if (oldChan == null) {
            UnbufferredPushChannel chan = new UnbufferredPushChannel(edge, pushName);
            Channel.bufferStore.put(edge, chan);
            return chan;
       } else {
            assert oldChan instanceof UnbufferredPushChannel 
                && oldChan.popMethodName().equals(pushName);
            return (UnbufferredPushChannel)oldChan;
        }
    }
    
    private UnbufferredPushChannel(Edge edge, String pushName) {
        super(edge);
        this.pushName = pushName;
    }
    
    @Override
    public String pushMethodName() {
        return pushName;
    }
    
    public void updatePushMethodName(String pushName) {
        this.pushName = pushName;
    }
    
}
