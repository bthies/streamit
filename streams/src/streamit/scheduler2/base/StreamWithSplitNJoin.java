package streamit.scheduler.base;

import streamit.scheduler.iriter.FeedbackLoopIter;
import streamit.scheduler.iriter.Iterator;
import java.math.BigInteger;
import streamit.misc.Fraction;

import streamit.scheduler.iriter.SplitterIter;
import streamit.scheduler.iriter.JoinerIter;

/* $Id: StreamWithSplitNJoin.java,v 1.1 2002-05-27 03:18:53 karczma Exp $ */

/**
 * Computes some basic steady state data for Streams that contain
 * a split and a join.
 *
 * @version 2
 * @author  Michal Karczmarek
 */

abstract class StreamWithSplitNJoin extends Stream
{
    /**
     * store the amount of data distributed to and 
     * collected by the splitter
     */
    class SplitSteadyFlow
    {
        public int splitPushWeights[];
        public int splitPopWeight;
    }

    /**
     * store the amount of data distributed to and 
     * collected by the joiner
     */
    class JoinSteadyFlow
    {
        public int joinPopWeights[];
        public int joinPushWeight;
    }

    /**
     * calculate amount of data handled by the splitter
     * @return structure that holds amount of data 
     * handled by the splitter
     */
    SplitSteadyFlow getSplitSteadyFlow (SplitterIter splitter)
    {
        // not tested yet.
        ASSERT (false);
        
        SplitSteadyFlow data = new SplitSteadyFlow();
        data.splitPopWeight = 0;

        int nSplitExec;
        for (nSplitExec = 0; nSplitExec < splitter.getSplitterNumWork(); nSplitExec++)
        {
            int nChild;
            for (nChild = 0; nChild < splitter.getFanOut(); nChild++)
            {
                data.splitPushWeights[nChild] += splitter.getSplitPushWeights(nSplitExec)[nChild];
            }

            data.splitPopWeight += splitter.getSplitPop(nSplitExec);
        }
        
        return data;
    }

    /**
     * calculate amount of data handled by the joiner
     * @return structure that holds amount of data 
     * handled by the joiner
     */
    JoinSteadyFlow getJoinSteadyFlow (JoinerIter joiner)
    {
        // not tested yet.
        ASSERT (false);
        
        JoinSteadyFlow data = new JoinSteadyFlow ();
        data.joinPushWeight = 0;
        
        int nJoinExec;
        for (nJoinExec = 0; nJoinExec < joiner.getJoinerNumWork(); nJoinExec++)
        {
            int nChild;
            for (nChild = 0; nChild < joiner.getJoinerNumWork (); nChild++)
            {
                data.joinPopWeights[nChild] += joiner.getJoinPopWeights(nJoinExec)[nChild];
            }
            
            data.joinPushWeight += joiner.getJoinPush (nJoinExec);
        }
        
        return data;
    }
}