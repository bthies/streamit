package streamit;

import java.util.*;
import java.lang.reflect.*;

// the basic stream class (pipe's).  has 1 input and 1 output.
public class Stream extends Operator
{

    // CONSTRUCTORS --------------------------------------------------------------------

    public Channel input = null;
    public Channel output = null;
    LinkedList streamElements = new LinkedList ();
    
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

    public Stream(float f) 
    {
        StreamInit ();
	Init(f);
    }

    public Stream(String str) 
    {
        StreamInit ();
        Init(str);
    }
    
    public Stream(ParameterContainer params)
    {
        StreamInit ();
        Init (params);
    }

    // INIT FUNCTIONS ---------------------------------------------------------------------
    
    // initializatoin functions, to be over-ridden
    public void Init() { ASSERT (false); }

    // initializatoin functions, to be over-ridden
    public void Init(int n) { ASSERT (false); }

    // initializatoin functions, to be over-ridden
    public void Init(float f) { ASSERT (false); }

    // initializatoin functions, to be over-ridden
    public void Init(String str) {ASSERT (false); }

    // initializatoin functions, to be over-ridden
    public void Init(ParameterContainer params) {ASSERT (false); }
    
    // general initialization function for Stream class only
    
    private void StreamInit ()
    {
        InitIO ();
    }
    
    public void InitIO () { }
    
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
    
    public void ConnectGraph ()
    {
        // make sure I have some elements - not sure what to do otherwise
        ASSERT (!streamElements.isEmpty ());
        
        // go through the list and connect it together:
        try
        {
            ListIterator childIter;
            childIter = (ListIterator) streamElements.iterator ();
            Stream source = null;
        
            while (childIter.hasNext ())
            {
                // advance the iterator:
                Stream sink = (Stream) childIter.next ();
                ASSERT (sink != null);
                
                // setup the sink itself
                sink.ConnectGraph ();
                
                if (source != null && source.GetIOField ("output") != null)
                {
                    // create and connect a pass filter
                    ChannelConnectFilter connect = new ChannelConnectFilter ();
                    Channel in = source.GetIOField ("output");
                    Channel out = sink.GetIOField ("input");
                    connect.UseChannels (in, out);
                }
                source = sink;
            }
        }
        catch (NoSuchElementException error)
        {
            // this should never happen
            ASSERT (false);
        }

        // set myself up with proper input and output
        {
            input = ((Stream)streamElements.getFirst ()).GetIOField ("input");
            output = ((Stream)streamElements.getLast ()).GetIOField ("output");
        }
    }

    // get my input.
    // makes sure that I only have ONE input
    // return null if no input present
    Channel GetIOField (String fieldName)
    {
        return GetIOField (fieldName, 0);
    }
    
    void SetIOField (String fieldName, Channel newChannel)
    {
        SetIOField (fieldName, 0, newChannel);
    }
    
    
}
