package streamit;

import java.util.*;

// many inputs, 1 output
public class Joiner extends Operator
{
    LinkedList srcs = new LinkedList ();
    int inputIndex = 0;

    public Channel input [] = null;
    public Channel output = null;

    public void InitIO () { }
    public void Init () { }

    void Add (Stream s)
    {
        srcs.add (s);
    }

    public boolean IsInputUsed (int index)
    {
        return true;
    }

    public void ConnectGraph ()
    {
        // do I even have anything to do?
        if (srcs.isEmpty ()) return;

        // yep, create an input array of appropriate size
        input = new Channel [srcs.size ()];

        // yep, go through my members and connect them all with
        // ChannelConnectFilter
        int inputIndx = 0;
        ListIterator iter = srcs.listIterator ();
        while (iter.hasNext ())
        {
            // connect the input streams:
            Stream s = (Stream) iter.next ();

            // it is possible for a stream to be null - if I'm doing a
            // weighted joiner and I really don't have the stream!
            if (s != null)
            {
                s.SetupOperator ();

                // retrieve the output of this filter, which will be an
                // input to this joiner
                Channel channel = s.GetIOField ("output");
                input [inputIndx] = channel;

                // if it is not a sink, make sure that it produces data
                // of the same kind as everything else in this Joiner
                if (channel != null)
                {
                    // handle input channel
                    if (output == null)
                    {
                        output = new Channel (channel);
                        output.SetSource (this);
                    } else {
                        // check that the input types agree
                        ASSERT (channel.GetType ().getName ().equals (output.GetType ().getName ()));
                    }

                    // now connect the channel to me
                    channel.SetSink (this);
                }
            }

            inputIndx ++;
        }
    }

    public void Work ()
    {
        ASSERT (false);
    }
}


