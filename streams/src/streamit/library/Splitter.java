package streamit;

import java.util.*;

// 1 input, many output
public abstract class Splitter extends Operator
{
    public static final Splitter ROUND_ROBIN_SPLITTER = new RoundRobinSplitter ();
    public static final Splitter DUPLICATE_SPLITTER = new DuplicateSplitter ();
    
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
        Channel newOutput = (Channel) s.GetIOField ("input").clone ();
        
        // handle input channel
        if (input == null)
        {
            input = newOutput;
            newOutput = (Channel) newOutput.clone ();
        } else 
        if (newOutput != null) 
        {
            // check that the input types agree
            ASSERT (newOutput.GetType ().getName ().equals (input.GetType ().getName ()));
        }
        
        // add the destination to the list of destinations
        dest.add (s);
        destWeight.add (new Integer (weight));
        
        // and finally add an entrance in the output channel array:
        if (output == null)
        {
            output = new Channel [1];
            output [0] = newOutput;
        } else {
            // need to copy the whole thing before actually
            // appending the new entrance
            Channel [] oldOutputs = output;
            output = new Channel [oldOutputs.length + 1];
            int pos;
            for (pos = 0; pos < oldOutputs.length; pos++)
            {
                output [pos] = oldOutputs [pos];
            }
            output [pos] = newOutput;
        }
    }
    
    public void ConnectGraph ()
    {
        // do I even have anything to do?
        if (output == null || output.length == 0) return;
        ASSERT (output.length == dest.size ());
        
        // yep, go through my members and connect them all with
        // ChannelConnectFilter
        int outputIndx = 0;
        ListIterator iter = dest.listIterator ();
        while (iter.hasNext ())
        {
            // get next output and input channels
            Stream s = (Stream) iter.next ();
            ASSERT (s != null);
            
            Channel in = s.GetIOField ("input");
            ASSERT (in != null);
            
            Channel out = output [outputIndx];
            outputIndx++;
            ASSERT (out != null);
            
            // now connect the in and out
            out.SetSource (this);
            in.SetSink (s);
            ChannelConnectFilter glue = new ChannelConnectFilter ();
            glue.UseChannels (out, in);
            
            // and connect the output Stream:
            s.ConnectGraph ();
        }
    }
}

