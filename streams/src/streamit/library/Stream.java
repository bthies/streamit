package streamit;

import java.util.*;
import java.lang.reflect.*;
import streamit.scheduler.*;
import streamit.scheduler.simple.SimpleHierarchicalScheduler;

// the basic stream class (pipe's).  has 1 input and 1 output.
public class Stream extends Operator
{

    public Channel streamInput = null;
    public Channel streamOutput = null;

    LinkedList streamElements = new LinkedList ();

    // CONSTRUCTORS --------------------------------------------------------------------
    public Stream ()
    {
        super ();
    }

    public Stream(int n)
    {
        super (n);
    }

    public Stream(int n1, int n2)
    {
        super (n1, n2);
    }

    public Stream(float f)
    {
        super (f);
    }

    public Stream(String str)
    {
        super (str);
    }

    public Stream(ParameterContainer params)
    {
        super (params);
    }

    public void initIO () { }

    // RESET FUNCTIONS

    public MessageStub reset()
    {
        ASSERT (false);
        return MESSAGE_STUB;
    }

    public MessageStub reset(int n)
    {
        ASSERT (false);
        return MESSAGE_STUB;
    }

    public MessageStub reset(String str)
    {
        ASSERT (false);
        return MESSAGE_STUB;
    }

    // ------------------------------------------------------------------
    //                  graph handling functions
    // ------------------------------------------------------------------

    // tells me if this component has already been connected
    boolean isConnected = false;

    // adds something to the pipeline
    public void add(Stream s)
    {
        ASSERT (s != null);
        streamElements.add (s);
    }

    // connectGraph will walk the entire subgraph (so it should be called
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

    public void connectGraph ()
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
                sink.setupOperator ();

                if (source != null && source.getIOField ("streamOutput") != null)
                {
                    // create and connect a pass filter
                    ChannelConnectFilter connect = new ChannelConnectFilter ();
                    Channel in = source.getIOField ("streamOutput");
                    Channel out = sink.getIOField ("streamInput");
                    connect.useChannels (in, out);
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
            streamInput = ((Stream)streamElements.getFirst ()).getIOField ("streamInput");
            streamOutput = ((Stream)streamElements.getLast ()).getIOField ("streamOutput");
        }
    }

    // get my input.
    // makes sure that I only have ONE input
    // return null if no input present
    Channel getIOField (String fieldName)
    {
        return getIOField (fieldName, 0);
    }

    void setIOField (String fieldName, Channel newChannel)
    {
        setIOField (fieldName, 0, newChannel);
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    static Scheduler scheduler;

    SchedStream constructSchedule ()
    {
        // go through my children and dispatch on their
        SchedPipeline pipeline = scheduler.newSchedPipeline (this);

        ListIterator childIter;
        childIter = (ListIterator) streamElements.iterator ();

        while (childIter.hasNext ())
        {
            // advance the iterator:
            Stream child = (Stream) childIter.next ();
            ASSERT (child);

            SchedStream childStream;
            childStream = child.constructSchedule ();
            pipeline.addChild (childStream);
        }

        return pipeline;
    }

}
