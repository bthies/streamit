package streamit;

import java.util.*;

// 1 input, many output
public abstract class Splitter extends Operator
{
    public static Splitter ROUND_ROBIN_SPLITTER ()
    {
        return new RoundRobinSplitter ();
    }
    
    public static Splitter DUPLICATE_SPLITTER ()
    {
        return new DuplicateSplitter ();
    }
    
    List dest = new ArrayList ();
    List destWeight = new ArrayList ();
    int outputIndex = 0, outputCount = 0;
    public Channel input = null;
    public Channel output [] = null;
    
    public void InitIO () { }
    
    public abstract void Work ();
    
    void Add (Stream s)
    {
        Add (s, 1);
    }
    
    void Add (Stream s, int weight)
    {
        // add the destination to the list of destinations
        dest.add (s);
        destWeight.add (new Integer (weight));
    }

    public void ConnectGraph ()
    {
        // do I even have anything to do?
        ASSERT (dest.size () == destWeight.size ());
        if (dest.isEmpty ()) return;
        
        // yep, create an output array of appropriate size
        output = new Channel [dest.size ()];
        
        // go through my members and connect them all with
        // ChannelConnectFilter
        int outputIndx = 0;
        ListIterator iter = dest.listIterator ();
        while (iter.hasNext ())
        {
            // get the stream
            Stream s = (Stream) iter.next ();
            ASSERT (s != null);
            
            // connect it and retrieve its input and copy it into
            // the output array for this splitter
            s.ConnectGraph ();
            Channel channel = s.GetIOField ("input");
            output [outputIndx] = channel;

            // if it is not a source, make sure that it consumes data
            // of the same kind as everything else in this Splitter
            if (channel != null)
            {
                // handle input channel
                if (input == null)
                {
                    input = new Channel (channel);
                } else {
                    // check that the input types agree
                    ASSERT (channel.GetType ().getName ().equals (input.GetType ().getName ()));
                }
            }
            
            // now connect the in and out
            channel.SetSource (this);
            channel.SetSink (s);
            
            outputIndx ++;
        }
    }
}

