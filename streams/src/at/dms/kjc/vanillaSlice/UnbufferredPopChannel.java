package at.dms.kjc.vanillaSlice;

import java.util.*;

import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.*;

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
    
    private Collection<Channel> produceReadHeadersFor = new LinkedList<Channel>();

    /** 
     * Add a channel to produce upstream (write) headers for:
     * @param c  a Channel connected to the splitter that this channel calls.
     */
    public void addChannelForHeaders(Channel c) {
        produceReadHeadersFor.add(c);
    }

    /**
     * popMethodName: overridden to return string from construction time.
     */
    @Override
    public String popMethodName() {
        return popName;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitRead()
     */
    @Override
    public List<JStatement> beginInitRead() {
        LinkedList<JStatement> retval = new LinkedList<JStatement>();
        for (Channel c : produceReadHeadersFor) {
            retval.addAll(c.beginInitRead());
        }
        return retval;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitRead()
     */
    @Override
   public List<JStatement> postPreworkInitRead() {
        LinkedList<JStatement> retval = new LinkedList<JStatement>();
        for (Channel c : produceReadHeadersFor) {
            retval.addAll(c.postPreworkInitRead());
        }
        return retval;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitRead()
     */
    @Override
    public List<JStatement> endInitRead() {
        LinkedList<JStatement> retval = new LinkedList<JStatement>();
        for (Channel c : produceReadHeadersFor) {
            retval.addAll(c.endInitRead());
        }
        return retval;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyRead()
     */
    @Override
    public List<JStatement> beginSteadyRead() {
        LinkedList<JStatement> retval = new LinkedList<JStatement>();
        for (Channel c : produceReadHeadersFor) {
            retval.addAll(c.beginSteadyRead());
        }
        return retval;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyRead()
     */
    @Override
    public List<JStatement> endSteadyRead() {
        LinkedList<JStatement> retval = new LinkedList<JStatement>();
        for (Channel c : produceReadHeadersFor) {
            retval.addAll(c.endSteadyRead());
        }
        return retval;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#topOfWorkSteadyRead()
     */
    @Override
    public List<JStatement> topOfWorkSteadyRead() {
        LinkedList<JStatement> retval = new LinkedList<JStatement>();
        for (Channel c : produceReadHeadersFor) {
            retval.addAll(c.topOfWorkSteadyRead());
        }
        return retval;
    }
}
