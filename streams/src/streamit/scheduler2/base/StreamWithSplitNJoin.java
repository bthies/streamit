package streamit.scheduler.base;

import streamit.scheduler.iriter./*persistent.*/
FeedbackLoopIter;
import streamit.scheduler.iriter./*persistent.*/
Iterator;
import java.math.BigInteger;
import streamit.misc.Fraction;

import streamit.scheduler.iriter./*persistent.*/
SplitterNJoinerIter;
import streamit.scheduler.iriter.JoinerIter;
import streamit.scheduler.iriter.SplitterIter;

/* $Id: StreamWithSplitNJoin.java,v 1.4 2002-07-18 05:34:38 karczma Exp $ */

/**
 * Computes some basic steady state data for Streams that contain
 * a split and a join.
 *
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class StreamWithSplitNJoin
    extends Stream
    implements StreamInterfaceWithSnJ
{
    StreamWithSplitNJoin(SplitterNJoinerIter snjIter)
    {
        steadySplitFlow = new SplitFlow(snjIter.getFanOut());
        steadyJoinFlow = new JoinFlow(snjIter.getFanIn());
        splitFlow = new SplitFlow[snjIter.getSplitterNumWork()];
        joinFlow = new JoinFlow[snjIter.getJoinerNumWork()];
        setupSplitFlow(snjIter);
        setupJoinFlow(snjIter);
    }

    /**
     * Stores the amount of data that the splitter transfers over all
     * executions of the splitter's work functions.
     */
    final private SplitFlow steadySplitFlow;

    /**
     * Stores the amount of data that the joiner transfers over all
     * executions of the joiner's work functions.
     */
    final private JoinFlow steadyJoinFlow;

    /**
     * stores information about data flow of each phase of the splitter.
     */
    final private SplitFlow splitFlow[];

    /**
     * stores information about data flow of each phase of the joiner.
     */
    final private JoinFlow joinFlow[];

    /**
     * calculate amount of data handled by the splitter
     * @return structure that holds amount of data 
     * handled by the splitter
     */
    private void setupSplitFlow(SplitterIter splitter)
    {
        int nSplitExec;
        for (nSplitExec = 0;
            nSplitExec < splitter.getSplitterNumWork();
            nSplitExec++)
        {
            int splitPushWeights[] = splitter.getSplitPushWeights(nSplitExec);

            // setup the steady flow
            {
                int nChild;
                for (nChild = 0; nChild < splitter.getFanOut(); nChild++)
                {
                    steadySplitFlow.setPushWeight(
                        nChild,
                        steadySplitFlow.getPushWeight(nChild)
                            + splitPushWeights[nChild]);
                }

                steadySplitFlow.setPopWeight(
                    steadySplitFlow.getPopWeight()
                        + splitter.getSplitPop(nSplitExec));
            }

            // setup the single phase flow
            {
                splitFlow[nSplitExec] = new SplitFlow(splitter.getFanOut());
                splitFlow[nSplitExec].setPopWeight(
                    splitter.getSplitPop(nSplitExec));

                int nChild;
                for (nChild = 0; nChild < splitter.getFanOut(); nChild++)
                {
                    splitFlow[nSplitExec].setPushWeight(
                        nChild,
                        splitPushWeights[nChild]);
                }
            }
        }
    }

    /**
     * calculate amount of data handled by the joiner
     * @return structure that holds amount of data 
     * handled by the joiner
     */
    private void setupJoinFlow(JoinerIter joiner)
    {
        int nJoinExec;
        for (nJoinExec = 0;
            nJoinExec < joiner.getJoinerNumWork();
            nJoinExec++)
        {
            int joinPopWeights[] = joiner.getJoinPopWeights(nJoinExec);

            // setup the steady flow 
            {
                int nChild;
                for (nChild = 0; nChild < joiner.getFanIn(); nChild++)
                {
                    steadyJoinFlow.setPopWeight(
                        nChild,
                        steadyJoinFlow.getPopWeight(nChild)
                            + joinPopWeights[nChild]);
                }

                steadyJoinFlow.setPushWeight(
                    steadyJoinFlow.getPushWeight()
                        + joiner.getJoinPush(nJoinExec));
            }

            // setup the single phase flow
            {
                joinFlow[nJoinExec] = new JoinFlow(joiner.getFanIn());
                joinFlow[nJoinExec].setPushWeight(
                    joiner.getJoinPush(nJoinExec));

                int nChild;
                for (nChild = 0; nChild < joiner.getFanIn(); nChild++)
                {
                    joinFlow[nJoinExec].setPopWeight(
                        nChild,
                        joinPopWeights[nChild]);
                }
            }
        }
    }

    public SplitFlow getSteadySplitFlow()
    {
        return steadySplitFlow;
    }

    public JoinFlow getSteadyJoinFlow()
    {
        return steadyJoinFlow;
    }

    public SplitFlow getSplitFlow(int nPhase)
    {
        ASSERT(nPhase >= 0);
        return splitFlow[nPhase];
    }

    public JoinFlow getJoinFlow(int nPhase)
    {
        ASSERT(nPhase >= 0);
        return joinFlow[nPhase];
    }
}