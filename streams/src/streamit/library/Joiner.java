package streamit;

import java.util.*;

// many inputs, 1 output
public class Joiner extends Operator 
{

    public static final Joiner ROUND_ROBIN_JOINER = new Joiner();

    public static Joiner WEIGHTED_ROUND_ROBIN(int w1, int w2) { return null; }
    public static Joiner WEIGHTED_ROUND_ROBIN(int w1, int w2, int w3) { return null; }
    public static Joiner WEIGHTED_ROUND_ROBIN(int w1, int w2, int w3, int w4) { return null; }
    public static Joiner WEIGHTED_ROUND_ROBIN(int w1, int w2, int w3, int w4, int w5) { return null; }
    public static Joiner WEIGHTED_ROUND_ROBIN(int w1, int w2, int w3, int w4, int w5, int w6) { return null; }

    LinkedList srcs = new LinkedList ();
    public Channel input [] = null;
    public Channel output = null;
    
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
    }
}


