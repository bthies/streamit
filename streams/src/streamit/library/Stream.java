package streamit;

import java.util.*;
import java.lang.reflect.*;
import streamit.scheduler.*;
import streamit.scheduler.simple.SimpleHierarchicalScheduler;

// the basic stream class (pipe's).  has 1 input and 1 output.
public abstract class Stream extends Operator
{

    public Channel input = null;
    public Channel output = null;

    LinkedList streamElements = new LinkedList ();

    // CONSTRUCTORS --------------------------------------------------------------------
    public Stream(float a, float b, int c)
    {
        super(a, b, c);
    }

    public Stream(float a, float b, float c)
    {
        super(a, b, c);
    }

    public Stream(float a, float b, int c, int d)
    {
        super(a, b, c, d);
    }

    public Stream(float x, float y, float z, int a, float b)
    {
        super(x,y,z,a,b);
    }

    public Stream(float x, float y, float z, int a)
    {
        super(x,y,z,a);
    }

    public Stream ()
    {
        super ();
    }

    public Stream(char c)
    {
        super (c);
    }

    public Stream(int n)
    {
        super (n);
    }

    public Stream (int x, int y)
    {
        super (x, y);
    }

    public Stream (int x, int y, int z)
    {
        super (x, y, z);
    }

    public Stream(int n1, int n2, int n3,
		  int n4, float f1) {
      super(n1, n2, n3, n4, f1);
    }

    public Stream (int x, int y, int z,
		   int a, int b, int c)
    {
        super (x, y, z, a, b, c);
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

    Channel getOutputChannel ()
    {
        return output;
    }

    Channel getInputChannel ()
    {
        return input;
    }

    public abstract void connectGraph ();

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

    /**
     * Initialize the buffer lengths for a stream, given a schedule:
     */
    abstract void setupBufferLengths (Schedule schedule);
}
