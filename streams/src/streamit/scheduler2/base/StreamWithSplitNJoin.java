package streamit.scheduler.base;

import streamit.scheduler.iriter./*persistent.*/FeedbackLoopIter;
import streamit.scheduler.iriter./*persistent.*/Iterator;
import java.math.BigInteger;
import streamit.misc.Fraction;

import streamit.scheduler.iriter./*persistent.*/SplitterNJoinerIter;
import streamit.scheduler.iriter.JoinerIter;

/* $Id: StreamWithSplitNJoin.java,v 1.3 2002-07-02 03:37:44 karczma Exp $ */

/**
 * Computes some basic steady state data for Streams that contain
 * a split and a join.
 *
 * @version 2
 * @author  Michal Karczmarek
 */

abstract class StreamWithSplitNJoin extends Stream
{
    StreamWithSplitNJoin(SplitterNJoinerIter snjIter)
    {
        splitFlow = getSplitSteadyFlow(snjIter);
        joinFlow = getJoinSteadyFlow(snjIter);
    }
    
    /**
     * Stores the amount of data that the splitter transfers over all
     * executions of the splitter's work functions.
     */
    final protected SplitSteadyFlow splitFlow;

    /**
     * Stores the amount of data that the joiner transfers over all
     * executions of the joiner's work functions.
     */
    final protected JoinSteadyFlow joinFlow;
    
    /**
     * store the amount of data distributed to and 
     * collected by the splitter
     */
    public class SplitSteadyFlow
    {
        SplitSteadyFlow(int nChildren)
        {
            pushWeights = new int [nChildren];
        }
        public int pushWeights[];
        public int popWeight;
    }

    /**
     * store the amount of data distributed to and 
     * collected by the joiner
     */
    public class JoinSteadyFlow
    {
        JoinSteadyFlow(int nChildren)
        {
            popWeights = new int [nChildren];
        }
        public int popWeights[];
        public int pushWeight;
    }

    /**
     * calculate amount of data handled by the splitter
     * @return structure that holds amount of data 
     * handled by the splitter
     */
    private SplitSteadyFlow getSplitSteadyFlow (SplitterNJoinerIter splitter)
    {
        SplitSteadyFlow splitData = new SplitSteadyFlow(splitter.getFanOut());
        splitData.popWeight = 0;

        int nSplitExec;
        for (nSplitExec = 0; nSplitExec < splitter.getSplitterNumWork(); nSplitExec++)
        {
            int splitPushWeights[] = splitter.getSplitPushWeights(nSplitExec);
            int nChild;
            for (nChild = 0; nChild < splitter.getFanOut(); nChild++)
            {
                splitData.pushWeights[nChild] += splitPushWeights[nChild];
            }

            splitData.popWeight += splitter.getSplitPop(nSplitExec);
        }
        
        return splitData;
    }

    /**
     * calculate amount of data handled by the joiner
     * @return structure that holds amount of data 
     * handled by the joiner
     */
    private JoinSteadyFlow getJoinSteadyFlow (SplitterNJoinerIter joiner)
    {
        JoinSteadyFlow joinData = new JoinSteadyFlow (joiner.getFanIn());
        joinData.pushWeight = 0;
        
        int nJoinExec;
        for (nJoinExec = 0; nJoinExec < joiner.getJoinerNumWork(); nJoinExec++)
        {
            int joinPopWeights[] = joiner.getJoinPopWeights(nJoinExec);
            
            int nChild;
            for (nChild = 0; nChild < joiner.getFanIn (); nChild++)
            {
                joinData.popWeights[nChild] += joinPopWeights [nChild];
            }
            
            joinData.pushWeight += joiner.getJoinPush (nJoinExec);
        }
        
        return joinData;
    }
}