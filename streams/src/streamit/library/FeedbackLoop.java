package streamit;

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
    public void SetDelay(int delay)
    {
        this.delay = delay;
    }

    // specifies the header
    public void SetJoiner(SplitJoin.SplitJoinType type)
    {
        ASSERT (joiner == null && type != null);
        joiner = type.GetJoiner ();
    }

    // specifies the body of the feedback loop
    public void SetBody (Stream body)
    {
        ASSERT (this.body == null && body != null);
        this.body = body;
    }

    // specifies the splitter
    public void SetSplitter (SplitJoin.SplitJoinType type)
    {
        ASSERT (splitter == null && type != null);
        splitter = type.GetSplitter ();
    }

    // specifies the feedback path stream
    public void SetLoop (Stream loop)
    {
        ASSERT (this.loop == null && loop != null);
        this.loop = loop;
    }

    // initialize the loop - push index-th element into the feedback loop channel
    public void InitPath (int index, Channel path)
    {
        ASSERT (false);
    }

    // not used here!
    public void Add(Stream s)
    {
        ASSERT (false);
    }

    // connect all the elements of this FeedbackLoop
    public void ConnectGraph ()
    {
        // make sure that I have the minimal elements to construct this loop
        ASSERT (joiner);
        ASSERT (splitter);
        ASSERT (body);

        // okay, initialize the body and figure out the type of data
        // passed around this loop
        body.SetupOperator();
        Channel bodyInput = body.GetIOField ("input");
        Channel bodyOutput = body.GetIOField ("output");
        ASSERT (bodyInput);
        ASSERT (bodyOutput);
        ASSERT (bodyOutput.GetType ().getName ().equals (bodyInput.GetType ().getName ()));

        // if I don't have a feedback path, just give myself an identity body
        // and initialize whatever the feedback path is
        if (loop == null) loop = new Identity (bodyOutput.GetType ());
        loop.SetupOperator ();
        Channel loopInput = loop.GetIOField ("input");
        Channel loopOutput = loop.GetIOField ("output");
        ASSERT (loopInput);
        ASSERT (loopOutput);
        ASSERT (loopOutput.GetType ().getName ().equals (loopInput.GetType ().getName ()));

        // create some extra Identities and give them to the split
        // and the join so they have a Filter for every input/output
        // they have to deal with
        {
            // the joiner:
            Channel channelIn = null;
            if (joiner.IsInputUsed (0))
            {
                Filter joinerIn = new Identity (bodyInput.GetType ());
                joinerIn.SetupOperator ();
                joiner.Add (joinerIn);
                channelIn = joinerIn.GetIOField ("input");
            } else {
                joiner.Add (null);
            }

            joiner.Add (loop);
            joiner.SetupOperator ();

            {
                ChannelConnectFilter connect = new ChannelConnectFilter ();
                Channel in = joiner.GetIOField ("output", 0);
                Channel out = body.GetIOField ("input");
                connect.UseChannels (in, out);
            }

            // the splitter:
            Channel channelOut = null;
            if (splitter.IsOutputUsed (0))
            {
                Filter splitterOut = new Identity (bodyOutput.GetType ());
                splitterOut.SetupOperator ();
                splitter.Add (splitterOut);
                channelOut = splitterOut.GetIOField ("output");
            } else {
                splitter.Add (null);
            }
            splitter.Add (loop);
            splitter.SetupOperator ();

            {
                ChannelConnectFilter connect = new ChannelConnectFilter ();
                Channel in = body.GetIOField ("output");
                Channel out = splitter.GetIOField ("input", 0);
                connect.UseChannels (in, out);
            }

            // copy the input/output from the identities to the input/output
            // fields of the feedback loop
            input = channelIn;
            output = channelOut;
        }

        // now fill up the feedback path with precomputed data:
        {
            Channel feedbackChannel = loop.GetIOField ("output");
            for (int index = 0; index < delay; index++)
            {
                InitPath (index, feedbackChannel);
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
}

