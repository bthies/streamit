package streamit;

import java.util.*;
import java.lang.reflect.*;

// the basic stream class (pipe's).  has 1 input and 1 output.
public abstract class Stream extends Operator
{

    // CONSTRUCTORS --------------------------------------------------------------------

    public Channel input = null;
    public Channel output = null;
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
        InitIO ();
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
        while (true)
        {
            RunSinks ();
            DrainChannels ();
        }
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
    // 4. this operation is done in-order:
    //    - an Operator gets a proper value for its input
    //    - this Operator processes all its children
    //    - the output from the last child is copied over
    //      to the Operator and the operation is finished
    
    void ConnectGraph ()
    {
        // get my source.  If I don't have one, I'm a source.
        Stream source = this;
        Channel currentSourceChannel = GetIOField ("input");
        
        // a Stream should have a list of elements that it
        // contains serially.  Go through this list and
        // setup the connections through channels:
        
        try
        {
            ListIterator childIter;
            childIter = (ListIterator) streamElements.iterator ();
        
            while (childIter.hasNext ())
            {
                // advance the iterator:
                Stream sink = (Stream) childIter.next ();
                
                // get the next stream:
                Channel currentDestChannel = sink.GetIOField ("input");
                
                // make sure that the channels use the same data types
                ASSERT (currentSourceChannel == null ^ currentDestChannel != null);
                ASSERT (currentSourceChannel == null || currentDestChannel == null ||
                        currentSourceChannel.GetType ().getName ().equals (currentDestChannel.GetType ().getName ()));
                
                // now copy the currentInput into the currentOutput
                if (currentSourceChannel != null)
                {
                    sink.SetIOField ("input", currentSourceChannel);
                    
                    ASSERT (sink.GetIOField ("input") == currentSourceChannel);
                    
                    // tell the channels what their sources and sinks are:
                    if (currentSourceChannel.GetSource () == null)
                        currentSourceChannel.SetSource (source);
                    currentSourceChannel.SetSink (sink);
                }
                
                // connect the subgraph
                sink.ConnectGraph ();

                // and setup for the next iteration
                source = sink;
                currentSourceChannel = source.GetIOField ("output");
                
                // if the current Operator is a sink, add it 
                // to the list:
                if (currentSourceChannel == null)
                {
                    sink.AddSink ();
                }
            }
            
            if (currentSourceChannel != null)
            {
                // get the next stream:
                Channel currentDestChannel = GetIOField ("input");
                
                // make sure that the channels use the same data types
                ASSERT (currentDestChannel != null);
                ASSERT (currentSourceChannel.GetType ().getName ().equals (currentDestChannel.GetType ().getName ()));
                
                // now copy the currentInput into the currentOutput
                SetIOField ("input", currentSourceChannel);
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
        SetIOField (fieldName, 0, newChannel);
    }
    
    
}
