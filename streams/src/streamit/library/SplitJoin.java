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
        Channel newInput = s.GetIOField ("input");
        Channel newOutput = s.GetIOField ("output");
        
        ASSERT (joiner == null);
        
        // figure out the input and output types
        if (newInput != null)
        {
            if (input == null)
            {
                input = (Channel) newInput.clone ();
                ASSERT (input != null);
            } else {
                // check that the input types agree
                ASSERT (newInput.GetType ().getName ().equals (input.GetType ().getName ()));
            }
        }
        
        if (newOutput != null)
        {
            if (output == null)
            {
                output = (Channel) newOutput.clone ();
                ASSERT (output != null);
            } else {
                // check that the output types agree
                ASSERT (newOutput.GetType ().getName ().equals (output.GetType ().getName ()));
            }
        }
        
        // add the stream to the Split
        if (splitter != null)
        {
            splitter.Add (s);
        } else {
            ASSERT (newInput == null);
        }

        if (newOutput != null) 
        {
            if (outputStreams == null)
            {
                outputStreams = new LinkedList ();
            }
            outputStreams.add (s);
        }
            
        
    }
    
    public void ConnectGraph ()
    {
        // connect the SplitJoin with the Split and the Join
        if (input != null)
        {
            ASSERT (splitter != null);
            
            splitter.SetIOField ("input", 0, input);
            splitter.ConnectGraph ();
        }
        
        if (output != null)
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