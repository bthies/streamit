package streamit;

import java.util.*;
import java.math.BigInteger;
import streamit.scheduler.Schedule;

/**
 *  implements a pipeline - stream already has all the functionality,
 *  so there's no need to put it in Pipeline - just inherit Stream :)
 */

public class Pipeline extends Stream
{
    /**
     * Default constructor
     */
    public Pipeline () { }

    /**
     * Constructor with an int.  Just calls the parent (as every construtor
     * should (more or less)
     */
    public Pipeline (int n) { super (n); }

    /** 
     * Constructor with a float.
     */
    public Pipeline (float x) { super(x);}

    /**
     * Constructor with two ints.
     */
    public Pipeline (int x, int y) { super (x, y); }

    /**
     * Constructor with three floats, an int and anther float.
     */
    public Pipeline (float x, float y, float z, int a, float b)
    {
        super(x,y,z,a,b);
    }

    /**
     * Another constructor.
     */
    public Pipeline( int i1, 
		     int i2, 
		     int i3, 
		     int i4, 
		     int i5, 
		     int i6, 
		     int i7, 
		     int i8, 
		     int i9, 
		     float f) {
	super(i1, i2, i3, i4, i5, i6, i7, i8, i9, f);
    }

    public Pipeline( int i1, 
		     int i2, 
		     int i3, 
		     int i4, 
		     int i5, 
		     int i6, 
		     float f) {
	super(i1, i2, i3, i4, i5, i6, f);
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

                if (source != null && source.getOutputChannel () != null)
                {
                    // create and connect a pass filter
                    ChannelConnectFilter connect = new ChannelConnectFilter ();
                    Channel in = source.getOutputChannel ();
                    Channel out = sink.getInputChannel ();
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
            input = ((Stream)streamElements.getFirst ()).getInputChannel ();
            output = ((Stream)streamElements.getLast ()).getOutputChannel ();
        }
    }

    void setupBufferLengths (Schedule schedule)
    {
        ListIterator childIter;
        childIter = (ListIterator) streamElements.iterator ();
        Stream source = null;
        Stream sink = null;

        // go through all the children
        while (childIter.hasNext ())
        {
            // advance the iterator:
            Stream child = (Stream) childIter.next ();
            ASSERT (child != null);
            child.setupBufferLengths (schedule);

            source = sink;
            sink = child;

            if (source != null)
            {
                ASSERT (sink);

                BigInteger buffSize = schedule.getBufferSizeBetween (source, sink);
                ASSERT (buffSize);

                source.getOutputChannel ().makePassThrough ();
                sink.getInputChannel ().setChannelSize (buffSize.intValue ());
            }
        }
    }
}
