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

import streamit.misc.Pair;

import streamit.library.iriter.SplitJoinIter;
import streamit.library.iriter.FeedbackLoopIter;

// many inputs, 1 output
abstract public class Joiner extends Operator
{
    public static boolean finegrained = false;

    List<Stream> srcs = new ArrayList<Stream>();

    public Channel inputChannel[] = null;
    public Channel outputChannel = null;

    public void init()
    {}

    void add(Stream s)
    {
        srcs.add(s);
    }

    public boolean isInputUsed(int index)
    {
        return true;
    }

    public void connectGraph()
    {
        // do I even have anything to do?
        if (srcs.isEmpty())
            return;

        // yep, create an input array of appropriate size
        inputChannel = new Channel[srcs.size()];

        // yep, go through my members and connect them all with
        // ChannelConnectFilter
        int inputIndx = 0;
        ListIterator<Stream> iter = srcs.listIterator();
        while (iter.hasNext())
            {
                // connect the input streams:
                Stream s = iter.next();

                // it is possible for a stream to be null - if I'm doing a
                // weighted joiner and I really don't have the stream!
                if (s != null)
                    {
                        s.setupOperator();

                        // retrieve the output of this filter, which will be an
                        // input to this joiner
                        Channel channel = s.getOutputChannel();
                        inputChannel[inputIndx] = channel;

                        // if it is not a sink, make sure that it produces data
                        // of the same kind as everything else in this Joiner
                        if (channel != null)
                            {
                                // handle input channel
                                if (outputChannel == null)
                                    {
                                        outputChannel = new Channel(channel);
                                        outputChannel.setSource(this);
                                    }
                                else
                                    {
                                        // check that the input types agree
                                        assert channel.getType().getName().equals(
                                                                                  outputChannel.getType().getName());
                                    }

                                // now connect the channel to me
                                channel.setSink(this);
                            }
                    }

                inputIndx++;
            }
        addJoiner();
    }

    public String toString()
    {
        return "joiner";
    }

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

    public boolean canFire() {
        // for the day when joiners can ever receive messages,
        // make sure that all inputs are ready before starting
        // execution, so that messages can be delivered in time
        int throughput[] =
            (sjIter != null
             ? sjIter.getJoinPopWeights(nWork)
             : flIter.getJoinPopWeights(nWork));
        for (int i=0; i<inputChannel.length; i++) {
            // input will be null for RR joiners that don't read
            // in one direction
            if (inputChannel[i]!=null && 
                inputChannel[i].myqueue.size() < throughput[i]) {
                return false;
            }
        }
        return true;
    }

    public void prepareToWork() {
        if (!Stream.scheduledRun) {
            // for the day when joiners can ever receive messages,
            // make sure that all inputs are ready before starting
            // execution, so that messages can be delivered in time
            int throughput[] =
                (sjIter != null
                 ? sjIter.getJoinPopWeights(nWork)
                 : flIter.getJoinPopWeights(nWork));
            for (int i=0; i<inputChannel.length; i++) {
                // input will be null for RR joiners that don't read
                // in one direction
                if (inputChannel[i]!=null) {
                    inputChannel[i].ensureData(throughput[i]);
                }
            }
        }
        super.prepareToWork();
    }

    int nWork = 0;
    public void work()
    {
        int throughput[] =
            (sjIter != null
             ? sjIter.getJoinPopWeights(nWork)
             : flIter.getJoinPopWeights(nWork));

        int totalWork =
            (sjIter != null
             ? sjIter.getJoinerNumWork()
             : flIter.getJoinerNumWork());

        nWork++;
        nWork = nWork % totalWork;

        for (int nCh = 0; nCh < srcs.size(); nCh++)
            {
                for (int nData = 0; nData < throughput[nCh]; nData++)
                    {
                        passOneData(inputChannel[nCh], outputChannel);
                    }

            }
    }

    Pair joinWorks[];

    public Object getWork(int nWork)
    {
        if (joinWorks == null)
            {
                joinWorks =
                    new Pair[sjIter != null
                             ? sjIter.getJoinerNumWork()
                             : flIter.getJoinerNumWork()];
            }
        if (joinWorks[nWork] == null)
            {
                joinWorks[nWork] = new Pair(this, new Integer(nWork));
            }

        return joinWorks[nWork];
    }

    // ----------------------------------------------------------------
    // This function constructs a weight distribution table
    // ----------------------------------------------------------------

    public abstract int[] getWeights();
    public abstract int getProduction();
}
