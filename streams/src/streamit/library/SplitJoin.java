package streamit;

import java.util.*;
import java.lang.reflect.*;

// creates a split/join
public class SplitJoin extends Stream
{
    Splitter splitter;
    Joiner joiner;
    
    List outputStreams;

    public SplitJoin() 
    {
        super();
    }

    public SplitJoin(int n) 
    {
        super(n);
    }
    
    // initializing IO will be handled by the Add function
    public void InitIO () { }

    // specify the splitter
    public void UseSplitter(Splitter s) 
    {
        ASSERT (splitter == null && s != null);
        splitter = s;
    }

    // specify the joiner
    // must also add all the appropriate outputs to the joiner!
    public void UseJoiner(Joiner j) 
    {
        ASSERT (joiner == null && j != null);
        joiner = j;
        
        ListIterator iter;
        iter = outputStreams.listIterator ();
        while (iter.hasNext ())
        {
            Stream s = (Stream) iter.next ();
            ASSERT (s != null);
            
            joiner.Add (s);
        }
    }

    // add a stream to the parallel section between the splitter and the joiner
    public void Add(Stream s)
    {
        ASSERT (joiner == null);

        // add the stream to the Split
        if (splitter != null)
        {
            splitter.Add (s);
        }

        // save the stream to add to the Join
        if (outputStreams == null)
        {
            outputStreams = new LinkedList ();
        }
        outputStreams.add (s);
    }
    
    public void ConnectGraph ()
    {
        // connect the SplitJoin with the Split and the Join
        if (splitter != null)
        {
            ASSERT (splitter != null);
            
            splitter.ConnectGraph ();
            
            input = splitter.GetIOField ("input", 0);
        }
        
        if (joiner != null)
        {
            ASSERT (joiner != null);
            
            joiner.ConnectGraph ();
            Channel outputs [] = joiner.GetIOFields ("output");
            ASSERT (outputs != null && outputs.length == 1);
            output = outputs [0];
            ASSERT (output != null);
        }
    }
    
}