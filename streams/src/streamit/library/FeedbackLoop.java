package streamit;

import streamit.scheduler2.Scheduler;
import streamit.iriter.Iterator;
import java.util.List;

// the feedback loop
public class FeedbackLoop extends Stream
{
    Joiner joiner;
    Splitter splitter;
    List enqueued;
    int delay;
    Stream body, loop;

    // constructor with delay left unspecified
    public FeedbackLoop(int N)
    {
        super(N);
        enqueued = new java.util.ArrayList();
    }

    // constructor with delay left unspecified
    public FeedbackLoop()
    {
        super();
        enqueued = new java.util.ArrayList();
    }

    public FeedbackLoop(float f)
    {
        super(f);
        enqueued = new java.util.ArrayList();
    }

    // set delay of feedback loop--that is, difference in original
    // stream position between items that arrive to joiner at same time,
    // assuming that all blocks in loop are 1-to-1 (otherwise we need to
    // work at defining exactly what it means).
    public void setDelay(int delay)
    {
        this.delay = delay;
    }
    
    public int getDelay() { return delay; }

    // specifies the header
    public void setJoiner(SplitJoin.SplitJoinType type)
    {
        ASSERT (joiner == null && type != null);
        joiner = type.getJoiner ();
    }

    // specifies the body of the feedback loop
    public void setBody (Stream body)
    {
        ASSERT (this.body == null && body != null);
        this.body = body;
    }

    // specifies the splitter
    public void setSplitter (SplitJoin.SplitJoinType type)
    {
        ASSERT (splitter == null && type != null);
        splitter = type.getSplitter ();
    }

    // specifies the feedback path stream
    public void setLoop (Stream loop)
    {
        ASSERT (this.loop == null && loop != null);
        this.loop = loop;
    }
    
    public Stream getBody () { return body; }
    public Stream getLoop () { return loop; }
    public Splitter getSplitter () { return splitter; }
    public Joiner getJoiner () { return joiner; }

    /**
     * initialize the loop - push index-th element into the feedback loop channel.
     * these initPath functions are implemented for many different types that
     * one can use - if you're missing yours, implement it!
     */

    public int initPathInt (int index)
    {
        ASSERT (false);
        return 0;
    }

    public short initPathShort (int index)
    {
        ASSERT (false);
        return 0;
    }

    public float initPathFloat (int index)
    {
        ASSERT (false);
        return 0;
    }

    public char initPathChar (int index)
    {
        ASSERT (false);
        return 0;
    }

    /**
     * initialize the path with an object.
     * If this isn't what you wanted (and it asserts here), then you
     * need to add an appropriate if-else in connectGraph (there is a comment
     * about it in the appropriate place)
     */
    public Object initPathObject (int index)
    {
        ASSERT (false);
        return null;
    }

    /*
     * alternate loop initialization -- push individual items on to
     * the output channel of the loop stream at init time.
     */

    public void enqueueInt (int value)
    {
        enqueued.add(new Integer(value));
    }
    
    public void enqueueShort (short value)
    {
        enqueued.add(new Short(value));
    }
    
    public void enqueueFloat (float value)
    {
        enqueued.add(new Float(value));
    }

    public void enqueueChar (char value)
    {
        enqueued.add(new Character(value));
    }

    public void enqueueObject (Object value)
    {
        enqueued.add(value);
    }

    // not used here!
    public void add(Stream s)
    {
        ASSERT (false);
    }

    // connect all the elements of this FeedbackLoop
    public void connectGraph ()
    {
        // make sure that I have the minimal elements to construct this loop
        ASSERT (joiner);
        ASSERT (splitter);
        ASSERT (body);

        // okay, initialize the body and figure out the type of data
        // passed around this loop
        body.setupOperator();
        Channel bodyInput = body.getInputChannel ();
        Channel bodyOutput = body.getOutputChannel ();
        ASSERT (bodyInput);
        ASSERT (bodyOutput);
        ASSERT (bodyOutput.getType ().getName ().equals (bodyInput.getType ().getName ()));

        // if I don't have a feedback path, just give myself an identity body
        // and initialize whatever the feedback path is
        if (loop == null) loop = new Identity (bodyOutput.getType ());
        loop.setupOperator ();
        Channel loopInput = loop.getInputChannel ();
        Channel loopOutput = loop.getOutputChannel ();
        ASSERT (loopInput);
        ASSERT (loopOutput);
        ASSERT (loopOutput.getType ().getName ().equals (loopInput.getType ().getName ()));

        // create some extra Identities and give them to the split
        // and the join so they have a Filter for every input/output
        // they have to deal with
        {
            // the joiner:
            Channel channelIn = null;
            if (joiner.isInputUsed (0))
            {
                Filter joinerIn = new Identity (bodyInput.getType ());
                joinerIn.setupOperator ();
                joiner.add (joinerIn);
                channelIn = joinerIn.getIOField ("input");
            } else {
                joiner.add (null);
            }

            joiner.add (loop);
            joiner.setupOperator ();

            {
                ChannelConnectFilter connect = new ChannelConnectFilter ();
                Channel in = joiner.getIOField ("output", 0);
                Channel out = body.getInputChannel ();
                connect.useChannels (in, out);
            }

            // the splitter:
            Channel channelOut = null;
            if (splitter.isOutputUsed (0))
            {
                Filter splitterOut = new Identity (bodyOutput.getType ());
                splitterOut.setupOperator ();
                splitter.add (splitterOut);
                channelOut = splitterOut.getIOField ("output");
            } else {
                splitter.add (null);
            }
            splitter.add (loop);
            splitter.setupOperator ();

            {
                ChannelConnectFilter connect = new ChannelConnectFilter ();
                Channel in = body.getOutputChannel ();
                Channel out = splitter.getIOField ("input", 0);
                connect.useChannels (in, out);
            }

            // copy the input/output from the identities to the input/output
            // fields of the feedback loop
            input = channelIn;
            output = channelOut;
        }

        // now fill up the feedback path with precomputed data:
        ASSERT(delay == 0 || enqueued.isEmpty());
        if (delay != 0)
        {
            Channel feedbackChannel = loop.getOutputChannel ();
            for (int index = 0; index < delay; index++)
            {
                Class type = feedbackChannel.getType ();
                if (type == Integer.TYPE)
                {
                    feedbackChannel.pushInt (initPathInt (index));
                } else
                if (type == Short.TYPE)
                {
                    feedbackChannel.pushShort (initPathShort (index));
                } else
                if (type == Character.TYPE)
                {
                    feedbackChannel.pushChar (initPathChar (index));
                } else
                if (type == Float.TYPE)
                {
                    feedbackChannel.pushFloat (initPathFloat (index));
                } else
                {
                    // this is essentially a default
                    // if this isn't what you want, you should implement
                    // another if-else clause!
                    feedbackChannel.push (initPathObject (index));
                }
            }
        }
        else
        {
            Channel feedbackChannel = loop.getOutputChannel ();
            for (java.util.Iterator iter = enqueued.iterator();
                 iter.hasNext(); )
            {
                Class type = feedbackChannel.getType ();
                if (type == Integer.TYPE)
                {
                    Integer value = (Integer)iter.next();
                    feedbackChannel.pushInt (value.intValue());
                } else
                if (type == Short.TYPE)
                {
                    Short value = (Short)iter.next();
                    feedbackChannel.pushShort (value.shortValue());
                } else
                if (type == Character.TYPE)
                {
                    Character value = (Character)iter.next();
                    feedbackChannel.pushChar (value.charValue());
                } else
                if (type == Float.TYPE)
                {
                    Float value = (Float)iter.next();
                    feedbackChannel.pushFloat (value.floatValue());
                } else
                {
                    // this is essentially a default
                    // if this isn't what you want, you should implement
                    // another if-else clause!
                    feedbackChannel.push (iter.next());
                }
                delay++;
            }
            // Throw out the enqueued list now.
            enqueued = null;
        }
    }


    // this is silly, but I need to put these here to make them easily
    // accessible - they're just wrappers for SplitJoin
    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN (int w1)
    {
        return SplitJoin.WEIGHTED_ROUND_ROBIN (w1);
    }

    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2)
    {
        return SplitJoin.WEIGHTED_ROUND_ROBIN (w1, w2);
    }

    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3)
    {
        return SplitJoin.WEIGHTED_ROUND_ROBIN (w1, w2, w3);
    }

    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3, int w4, int w5, int w6, int w7)
    {
        return SplitJoin.WEIGHTED_ROUND_ROBIN (w1, w2, w3, w4, w5, w6, w7);
    }

    public static SplitJoin.SplitJoinType ROUND_ROBIN ()
    {
        return SplitJoin.ROUND_ROBIN ();
    }

    public static SplitJoin.SplitJoinType DUPLICATE ()
    {
        return SplitJoin.DUPLICATE ();
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    void setupBufferLengths (Scheduler buffers)
    {
        // some of these asserts are restrictions on functionality
        // of this function and not on what a correct streamit structure would
        // be
        ASSERT (body != null);
        ASSERT (loop != null);
        ASSERT (joiner != null);
        ASSERT (splitter != null);

        // just go through and init the bloody buffers

        // start with getting body and loop to initialize their interal buffers
        body.setupBufferLengths (buffers);
        loop.setupBufferLengths (buffers);

        // now setup the buffer sizes:
        int s;

        // between joiner and body
        s = buffers.getBufferSizeBetween (new Iterator(this), new Iterator(body));
        StreamIt.totalBuffer += s;
        joiner.getIOField ("output", 0).makePassThrough ();
        body.getInputChannel ().setChannelSize (s);

        // between body and splitter
        s = buffers.getBufferSizeBetween (new Iterator(body), new Iterator(this));
        StreamIt.totalBuffer += s;
        splitter.getIOField ("input", 0).setChannelSize (s);
        body.getOutputChannel ().makePassThrough ();

        // between splitter and loop
        s = buffers.getBufferSizeBetween (new Iterator(this), new Iterator(loop));
        StreamIt.totalBuffer += s;
        loop.getInputChannel ().setChannelSize (s);

        // between loop and joiner
        s = buffers.getBufferSizeBetween (new Iterator(loop), new Iterator(this));
        StreamIt.totalBuffer += s;
        loop.getOutputChannel ().setChannelSize (s);

        // make sure that the input/output channels push data through right away:
        if (getInputChannel () != null) getInputChannel ().makePassThrough ();
        if (getOutputChannel () != null) splitter.getIOField ("output", 0).makePassThrough ();
    }
}

