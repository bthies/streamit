package streamit;

import streamit.scheduler.*;

// the feedback loop
public class FeedbackLoop extends Stream
{
    Joiner joiner;
    Splitter splitter;
    int delay;
    Stream body, loop;

    // constructor with delay left unspecified
    public FeedbackLoop()
    {
        super();
    }

    // set delay of feedback loop--that is, difference in original
    // stream position between items that arrive to joiner at same time,
    // assuming that all blocks in loop are 1-to-1 (otherwise we need to
    // work at defining exactly what it means).
    public void setDelay(int delay)
    {
        this.delay = delay;
    }

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

    // initialize the loop - push index-th element into the feedback loop channel
    public void initPath (int index, Channel path)
    {
        ASSERT (false);
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
        Channel bodyInput = body.getIOField ("streamInput");
        Channel bodyOutput = body.getIOField ("streamOutput");
        ASSERT (bodyInput);
        ASSERT (bodyOutput);
        ASSERT (bodyOutput.getType ().getName ().equals (bodyInput.getType ().getName ()));

        // if I don't have a feedback path, just give myself an identity body
        // and initialize whatever the feedback path is
        if (loop == null) loop = new Identity (bodyOutput.getType ());
        loop.setupOperator ();
        Channel loopInput = loop.getIOField ("streamInput");
        Channel loopOutput = loop.getIOField ("streamOutput");
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
                channelIn = joinerIn.getIOField ("streamInput");
            } else {
                joiner.add (null);
            }

            joiner.add (loop);
            joiner.setupOperator ();

            {
                ChannelConnectFilter connect = new ChannelConnectFilter ();
                Channel in = joiner.getIOField ("streamOutput", 0);
                Channel out = body.getIOField ("streamInput");
                connect.useChannels (in, out);
            }

            // the splitter:
            Channel channelOut = null;
            if (splitter.isOutputUsed (0))
            {
                Filter splitterOut = new Identity (bodyOutput.getType ());
                splitterOut.setupOperator ();
                splitter.add (splitterOut);
                channelOut = splitterOut.getIOField ("streamOutput");
            } else {
                splitter.add (null);
            }
            splitter.add (loop);
            splitter.setupOperator ();

            {
                ChannelConnectFilter connect = new ChannelConnectFilter ();
                Channel in = body.getIOField ("streamOutput");
                Channel out = splitter.getIOField ("streamInput", 0);
                connect.useChannels (in, out);
            }

            // copy the input/output from the identities to the input/output
            // fields of the feedback loop
            streamInput = channelIn;
            streamOutput = channelOut;
        }

        // now fill up the feedback path with precomputed data:
        {
            Channel feedbackChannel = loop.getIOField ("streamOutput");
            for (int index = 0; index < delay; index++)
            {
                initPath (index, feedbackChannel);
            }
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

    SchedStream constructSchedule ()
    {
        // initialize the children
        SchedStream bodySched = (body == null ? null : body.constructSchedule ());
        SchedStream loopSched = (loop == null ? null : loop.constructSchedule ());

        // create the loop
        SchedLoop stream = scheduler.newSchedLoop (this, joiner.getSchedType (scheduler), bodySched, splitter.getSchedType (scheduler), loopSched, delay);

        return stream;
    }
}

