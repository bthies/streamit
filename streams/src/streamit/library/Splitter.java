package streamit;

import java.util.*;

// 1 input, many output
public abstract class Splitter extends Operator
{
    List dest = new ArrayList ();
    int outputIndex = 0;
    public Channel input = null;
    public Channel output [] = null;
    
    public void InitIO () { }
    
    public abstract void Work ();
    
    void Add (Stream s)
    {
        ASSERT (s != null);
        dest.add (s);
    }

    public void ConnectGraph ()
    {
        // do I even have anything to do?
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
                    input.SetSink (this);
                } else {
                    // check that the input types agree
                    ASSERT (channel.GetType ().getName ().equals (input.GetType ().getName ()));
                }
                
                // now connect the channel to the Splitter
                channel.SetSource (this);
            }
            
            outputIndx ++;
        }
    }
}

