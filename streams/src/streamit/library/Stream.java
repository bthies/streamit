package streamit;

import java.util.*;
import java.lang.reflect.*;

// the basic stream class (pipe's).  has 1 input and 1 output.
public class Stream extends Operator {

    // CONSTRUCTORS --------------------------------------------------------------------

    LinkedList streamElements;
    
    public Stream() 
    {
        StreamInit ();
    	Init();
    }

    public Stream(int n) 
    {
        StreamInit ();
	    Init(n);
    }

    public Stream(String str) 
    {
        StreamInit ();
        Init(str);
    }
    
    public Stream(Stream str) 
    {
        StreamInit ();
	    Init(str);
    }

    // INIT FUNCTIONS ---------------------------------------------------------------------
    
    // initializatoin functions, to be over-ridden
    public void Init() {}

    // initializatoin functions, to be over-ridden
    public void Init(int n) {}

    // initializatoin functions, to be over-ridden
    public void Init(String str) {}

    // initializatoin functions, to be over-ridden
    public void Init(Stream str) {}
    
    // general initialization function for Stream class only
    
    private void StreamInit ()
    {
        streamElements = new LinkedList ();
    }

    // RESET FUNCTIONS (need to just call init functions) ---------------------------------

    public MessageStub Reset() 
    {
    	Init();
    	return MESSAGE_STUB;
    }

    public MessageStub Reset(int n) 
    {
    	Init(n);
    	return MESSAGE_STUB;
    }

    public MessageStub Reset(String str)
    {
    	Init(str);
    	return MESSAGE_STUB;
    }

    public MessageStub Reset(Stream str)
    {
    	Init(str);
    	return MESSAGE_STUB;
    }

    // just a runtime hook to run the stream
    public void Run() 
    {
        ConnectGraph ();
        
        // execute the stream here
    }

    // ------------------------------------------------------------------
    //                  graph handling functions
    // ------------------------------------------------------------------
    
    // tells me if this component has already been connected
    boolean isConnected = false;
    
    // adds something to the pipeline
    public void Add(Stream s) 
    {
        ASSERT (s != null);
        streamElements.add (s);
    }
    
    // ConnectGraph will walk the entire subgraph (so it should be called
    // on the "master", encapsulating Stream) and give each element
    // this function works in the following way:
    // 
    
    // goal:
    // Channels need to connect TWO separate Operators
    // 1. try to assign the same channel to both operators
    // 2. can be done by first comparing the two separate operators
    //    for matching types (error check) and then copying one of the
    //    channels over to the other operator
    // 3. should copy over the source's operator (overwrite the dest
    //    operator's channel).  reason for this is that the source's
    //    operator should know the REAL source of data (particular
    //    Filer which is producing this data)
    // 4. this operation is done in following order:
    //    - an Operator gets a proper value for its input
    //    - this Operator processes all its children
    //    - the output from the last child is copied over
    //      to the Operator and the operation is finished
    
    void ConnectGraph ()
    {
        // get my source.  If I don't have one, I'm a source.
        Channel currentSource = GetIOField ("input");
        
        // a Stream should have a list of elements that it
        // contains serially.  Go through this list and
        // setup the connections through channels:
        
        try
        {
            ListIterator childIter;
            childIter = (ListIterator) streamElements.iterator ();
        
            while (childIter.hasNext ())
            {
                Stream currentStream = (Stream) childIter.next ();
                
                // get the next stream:
                Channel currentDest = currentStream.GetIOField ("input");
                
                // make sure that the channels use the same data types
                ASSERT (currentSource == null ^ currentDest != null);
                ASSERT (currentSource == null || currentDest == null ||
                        currentSource.GetType ().equals (currentDest.GetType ()));
                
                // now copy the currentInput into the currentOutput
                if (currentDest != null)
                {
                    currentStream.SetIOField ("input", currentSource);
                }
                
                currentSource = currentStream.GetIOField ("output");
            }
            
            if (currentSource != null)
            {
            }
        }
        catch (NoSuchElementException error)
        {
            // this should never happen
            ASSERT (false);
        }
    }

    // get my input.
    // makes sure that I only have ONE input
    // return null if no input present
    Channel GetIOField (String fieldName)
    {
        Channel field = null;
        
        {
            Channel fieldInstance[];
            fieldInstance = super.GetIOFields (fieldName);
            
            if (fieldInstance != null)
            {
                ASSERT (fieldInstance.length == 1);
                field = fieldInstance [0];
            }
        }
        
        return field;
    }
    
    void SetIOField (String fieldName, Channel newChannel)
    {
        SetIOField (fieldName, 1, newChannel);
    }
    
    
}
