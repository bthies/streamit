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
// 1 input, many output

import streamit.misc.Pair;

import streamit.library.iriter.SplitJoinIter;
import streamit.library.iriter.FeedbackLoopIter;

abstract public class Splitter extends Operator
{
    public static boolean finegrained = false;
    public boolean duplicateSplitter = false;

    List<Stream> dest = new ArrayList<Stream>();
    public Channel inputChannel = null;
    public Channel outputChannel[] = null;

    public void init()
    {}

    SplitJoinIter sjIter;
    FeedbackLoopIter flIter;

    public void useSJ(SplitJoinIter sj)
    {
        sjIter = sj;
    }

    public void useFL(FeedbackLoopIter fl)
    {
        flIter = fl;
    }

    public void prepareToWork() {
        if (!Stream.scheduledRun) {
            // for the day when splitters can ever receive messages,
            // make sure that all inputs are ready before starting
            // execution, so that messages can be delivered in time

            // get output weights
            int throughput[] =
                (sjIter != null
                 ? sjIter.getSplitPushWeights(nWork)
                 : flIter.getSplitPushWeights(nWork));

            // sum output weights to get input weights
            int totalData = 0;
            for (int i=0; i<throughput.length; i++) {
                /* RMR { for duplicate splitters, only take the max output weight
                 * since the data on the input channel is shared 
                 */
                if (duplicateSplitter) {
                    totalData = MAX(throughput[i], totalData);
                }
                else {
                    totalData += throughput[i];
                }
                /* } RMR */
            }

            // ensure data
            inputChannel.ensureData(totalData);
        }
        super.prepareToWork();
    }

    int nWork = 0;
    public void work()
    {
        int throughput[] =
            (sjIter != null
             ? sjIter.getSplitPushWeights(nWork)
             : flIter.getSplitPushWeights(nWork));

        int totalWork =
            (sjIter != null
             ? sjIter.getSplitterNumWork()
             : flIter.getSplitterNumWork());

        nWork++;
        nWork = nWork % totalWork;

        for (int nCh = 0; nCh < dest.size(); nCh++)
            {
                for (int nData = 0; nData < throughput[nCh]; nData++)
                    {
                        passOneData(inputChannel, outputChannel[nCh]);
                    }

            }

    }

    void add(Stream s)
    {
        dest.add(s);
    }

    public boolean isOutputUsed(int index)
    {
        return true;
    }

    public void connectGraph()
    {
        // do I even have anything to do?
        if (dest.isEmpty())
            return;

        // yep, create an output array of appropriate size
        outputChannel = new Channel[dest.size()];

        // go through my members and connect them all with
        // ChannelConnectFilter
        int outputIndx = 0;
        ListIterator<Stream> iter = dest.listIterator();
        while (iter.hasNext())
            {
                // get the stream
                Stream s = iter.next();

                // it is possible that the stream will legitimately be null
                // just don't do anything in this case!
                if (s != null)
                    {
                        // connect it and retrieve its input and copy it into
                        // the output array for this splitter
                        s.setupOperator();
                        Channel channel = s.getIOField("inputChannel");
                        outputChannel[outputIndx] = channel;

                        // if it is not a source, make sure that it consumes data
                        // of the same kind as everything else in this Splitter
                        if (channel != null)
                            {
                                // handle input channel
                                if (inputChannel == null)
                                    {
                                        inputChannel = new Channel(channel);
                                        inputChannel.setSink(this);
                                    }
                                else
                                    {
                                        // check that the input types agree
                                        assert channel.getType().getName()
                                            .equals(inputChannel.getType().getName()):
                                            "input type = "
                                            + inputChannel.getType().getName()
                                            + " but channel type = "
                                            + channel.getType().getName();
                                    }

                                // now connect the channel to the Splitter
                                channel.setSource(this);
                            }
                    }

                outputIndx++;
            }
    }

    public String toString()
    {
        return "splitter";
    }

    Pair splitWorks[];

    public Object getWork(int nWork)
    {
        if (splitWorks == null)
            {
                splitWorks =
                    new Pair[sjIter != null
                             ? sjIter.getSplitterNumWork()
                             : flIter.getSplitterNumWork()];
            }
        if (splitWorks[nWork] == null)
            {
                splitWorks[nWork] = new Pair(this, new Integer(nWork));
            }

        return splitWorks[nWork];
    }

    // ----------------------------------------------------------------
    // This function constructs a weights list for the scheduler
    // ----------------------------------------------------------------
    abstract public int[] getWeights();
    public abstract int getConsumption();
}
