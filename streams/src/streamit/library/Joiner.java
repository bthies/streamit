package streamit;

import java.util.*;

// many inputs, 1 output
public class Joiner extends Operator
{

    public static final Joiner ROUND_ROBIN_JOINER ()
    {
        return new RoundRobinJoiner();
    }

    public static Joiner WEIGHTED_ROUND_ROBIN(int w1, int w2)
    {
        Joiner j = new RoundRobinJoiner ();
        j.srcsWeight = new int [2];
        j.srcsWeight[0] = w1;
        j.srcsWeight[1] = w2;
        return j;
    }
    
    public static Joiner WEIGHTED_ROUND_ROBIN(int w1, int w2, int w3) { return null; }
    public static Joiner WEIGHTED_ROUND_ROBIN(int w1, int w2, int w3, int w4) { return null; }
    public static Joiner WEIGHTED_ROUND_ROBIN(int w1, int w2, int w3, int w4, int w5) { return null; }
    public static Joiner WEIGHTED_ROUND_ROBIN(int w1, int w2, int w3, int w4, int w5, int w6) { return null; }

    LinkedList srcs = new LinkedList ();
    int [] srcsWeight;
    int inputIndex = 0, inputCount = 0;
    
    public Channel input [] = null;
    public Channel output = null;
    
    public void InitIO () { }
    
    void Add (Stream s)
    {
        srcs.add (s);
    }
    
    public void ConnectGraph ()
    {
        // do I even have anything to do?
        ASSERT (srcs.size () == srcsWeight.length);
        if (srcs.isEmpty ()) return;
        
        // yep, create an input array of appropriate size
        input = new Channel [srcs.size ()];
        
        // yep, go through my members and connect them all with
        // ChannelConnectFilter
        int inputIndx = 0;
        ListIterator iter = srcs.listIterator ();
        while (iter.hasNext ())
        {
            // connect the input streams:
            Stream s = (Stream) iter.next ();
            ASSERT (s != null);
            s.ConnectGraph ();
            
            // retrieve the output of this filter, which will be an
            // input to this joiner
            Channel channel = s.GetIOField ("output");
            input [inputIndx] = channel;
            
            // if it is not a sink, make sure that it produces data
            // of the same kind as everything else in this Joiner
            if (channel != null)
            {
                // handle input channel
                if (output == null)
                {
                    output = new Channel (channel);
                    output.SetSource (this);
                } else {
                    // check that the input types agree
                    ASSERT (channel.GetType ().getName ().equals (output.GetType ().getName ()));
                }
                
                // now connect the channel to me
                channel.SetSink (this);
                
                ASSERT (srcsWeight [inputIndx] > 0);
            } else 
            {
                ASSERT (srcsWeight [inputIndx] == 0);
            }

            inputIndx ++;
        }
    }
    
    public void Work ()
    {
        ASSERT (false);
    }
}


