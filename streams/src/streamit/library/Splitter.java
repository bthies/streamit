package streamit;

import java.util.*;

// 1 input, many output
public class Splitter extends Operator
{
    public static final Splitter ROUND_ROBIN_SPLITTER = new Splitter ();
    public static final Splitter DUPLICATE_SPLITTER = new Splitter ();
    
    LinkedList dest = new LinkedList ();
    public Channel input = null;
    public Channel output [] = null;
    
    public void InitIO () { }
    
    void Add (Stream s)
    {
        Channel newInput = (Channel) s.GetIOField ("input").clone ();
        
        if (input == null)
        {
            input = newInput;
        } else 
        if (newInput != null) 
        {
            // check that the input types agree
            ASSERT (newInput.GetType ().getName ().equals (input.GetType ().getName ()));
        }
        
        dest.add (s);
    }
    
    void ConnectGraph ()
    {
        ASSERT (false);
    }
}

