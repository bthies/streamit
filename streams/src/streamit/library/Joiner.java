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
    
    public Channel input [] = null;
    public Channel output = null;
    
    public void InitIO () { }
    
    void Add (Stream s)
    {
        Channel newOutput = s.GetIOField ("output");
        
        if (output == null)
        {
            output = newOutput;
        } else 
        if (newOutput != null)
        {
            // check that the input types agree
            ASSERT (newOutput.GetType ().getName ().equals (output.GetType ().getName ()));
        }
        
        srcs.add (s);
        
        if (newOutput != null)
        {
            if (input == null)
            {
                input = new Channel [1];
                input [0] = (Channel) newOutput.clone ();
                ASSERT (input [0] != null);
            } else {
                Channel [] oldInput = input;
                input = new Channel [oldInput.length + 1];
                
                int indx;
                for (indx = 0; indx < oldInput.length; indx++)
                {
                    input [indx] = oldInput [indx];
                }
                
                input [indx] = (Channel) newOutput.clone ();
                ASSERT (input [indx] != null);
            }
        }
    }
    
    public void ConnectGraph ()
    {
        // do I even have anything to do?
        if (input == null || input.length == 0) return;
        ASSERT (input.length == srcs.size ());
        
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
            
            // connect the joiner to me
            Channel out = s.GetIOField ("output");
            ASSERT (out != null);
            
            Channel in = input [inputIndx];
            inputIndx++;
            ASSERT (in != null);
            
            // now connect the in and out
            out.SetSource (s);
            in.SetSink (this);
            ChannelConnectFilter glue = new ChannelConnectFilter ();
            glue.UseChannels (out, in);
        }
    }
    
    public void Work ()
    {
        ASSERT (false);
    }
}


