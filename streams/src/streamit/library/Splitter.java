package streamit;

import streamit.scheduler.SchedSplitType;
import streamit.scheduler.Scheduler;

import java.util.*;

// 1 input, many output
public class Splitter extends Operator
{
    List dest = new ArrayList ();
    int outputIndex = 0;
    public Channel streamInput = null;
    public Channel streamOutput [] = null;

    public void initIO () { }
    public void init () { }

    public void work ()
    {
        ASSERT (0);
    }

    void add (Stream s)
    {
        dest.add (s);
    }

    public boolean isOutputUsed (int index)
    {
        return true;
    }

    public void connectGraph ()
    {
        // do I even have anything to do?
        if (dest.isEmpty ()) return;

        // yep, create an output array of appropriate size
        streamOutput = new Channel [dest.size ()];

        // go through my members and connect them all with
        // ChannelConnectFilter
        int outputIndx = 0;
        ListIterator iter = dest.listIterator ();
        while (iter.hasNext ())
        {
            // get the stream
            Stream s = (Stream) iter.next ();

            // it is possible that the stream will legitimately be null
            // just don't do anything in this case!
            if (s != null)
            {
                ASSERT (s != null);

                // connect it and retrieve its input and copy it into
                // the output array for this splitter
                s.setupOperator ();
                Channel channel = s.getIOField ("streamInput");
                streamOutput [outputIndx] = channel;

                // if it is not a source, make sure that it consumes data
                // of the same kind as everything else in this Splitter
                if (channel != null)
                {
                    // handle input channel
                    if (streamInput == null)
                    {
                        streamInput = new Channel (channel);
                        streamInput.setSink (this);
                    } else {
                        // check that the input types agree
                        ASSERT (channel.getType ().getName ().equals (streamInput.getType ().getName ()));
                    }

                    // now connect the channel to the Splitter
                    channel.setSource (this);
                }
            }

            outputIndx ++;
        }
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedSplitType getSchedType (Scheduler scheduler)
    {
        // you must override this function
        ASSERT (false);
        return null;
    }
}

