/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.library;

import java.util.*;

// many inputs, 1 output
abstract public class Joiner extends Operator
{
    List srcs = new ArrayList ();

    public Channel input [] = null;
    public Channel output = null;

    public void init () { }

    void add (Stream s)
    {
        srcs.add (s);
    }

    public boolean isInputUsed (int index)
    {
        return true;
    }

    public void connectGraph ()
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
                s.setupOperator ();

                // retrieve the output of this filter, which will be an
                // input to this joiner
                Channel channel = s.getOutputChannel ();
                input [inputIndx] = channel;

                // if it is not a sink, make sure that it produces data
                // of the same kind as everything else in this Joiner
                if (channel != null)
                {
                    // handle input channel
                    if (output == null)
                    {
                        output = new Channel (channel);
                        output.setSource (this);
                    } else {
                        // check that the input types agree
                        ASSERT (channel.getType ().getName ().equals (output.getType ().getName ()));
                    }

                    // now connect the channel to me
                    channel.setSink (this);
                }
            }

            inputIndx ++;
        }
    }

    public String toString() {
	return "joiner";
    }

    abstract public void work ();

    // ----------------------------------------------------------------
    // This function constructs a weight distribution table
    // ----------------------------------------------------------------

    public abstract int [] getWeights ();
    public abstract int getProduction ();
}


