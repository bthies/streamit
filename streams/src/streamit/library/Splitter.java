package streamit.library;

import java.util.*;
// 1 input, many output
abstract public class Splitter extends Operator
{
    List dest = new ArrayList ();
    public Channel input = null;
    public Channel output [] = null;

    public void init () { }

    abstract public void work ();
    
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
        output = new Channel [dest.size ()];

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
                Channel channel = s.getIOField ("input");
                output [outputIndx] = channel;

                // if it is not a source, make sure that it consumes data
                // of the same kind as everything else in this Splitter
                if (channel != null)
                {
                    // handle input channel
                    if (input == null)
                    {
                        input = new Channel (channel);
                        input.setSink (this);
                    } else {
                        // check that the input types agree
                        ASSERT (channel.getType ().getName ().equals (input.getType ().getName ()),
				"input type = " + input.getType().getName() + " but channel type = " + channel.getType().getName());
                    }

                    // now connect the channel to the Splitter
                    channel.setSource (this);
                }
            }

            outputIndx ++;
        }
    }

    public String toString() {
	return "joiner";
    }

    // ----------------------------------------------------------------
    // This function constructs a weights list for the scheduler
    // ----------------------------------------------------------------
    abstract public int [] getWeights ();
    public abstract int getConsumption ();
}

