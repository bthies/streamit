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

package streamit.scheduler2.minlatency;

import streamit.scheduler2.iriter./*persistent.*/
SplitJoinIter;
import streamit.scheduler2.hierarchical.StreamInterface;
import streamit.scheduler2.base.StreamFactory;
import streamit.scheduler2.hierarchical.PhasingSchedule;

/**
 * This class implements a minimum-latency algorithm for creating
 * schedules.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class SplitJoin extends streamit.scheduler2.hierarchical.SplitJoin
{
    public SplitJoin(SplitJoinIter iterator, StreamFactory factory)
    {
        super(iterator, factory);
    }

    private abstract class SJSchedulingUtility
    {
        SplitJoin sj;
        SJSchedulingUtility(SplitJoin _sj)
        {
            sj = _sj;
        }
        abstract public void addSchedulePhase(PhasingSchedule phase);
        public void advanceChildSchedule(StreamInterface child)
        {
            advanceChildSchedule(child, 1);
        }
        abstract public void advanceChildSchedule(
            StreamInterface child,
            int nPhases);
        abstract public PhasingSchedule getChildNextPhase(StreamInterface child);
        abstract public PhasingSchedule getChildPhase(
            StreamInterface child,
            int phase);
        abstract public void advanceSplitSchedule();
        abstract public void advanceJoinSchedule();

        abstract public SplitFlow getNextSplitSteadyPhaseFlow();
        abstract public SplitFlow getSplitSteadyPhaseFlow(int phase);
        abstract public PhasingSchedule getNextSplitSteadyPhase();
        abstract public JoinFlow getNextJoinSteadyPhaseFlow();
        abstract public JoinFlow getJoinSteadyPhaseFlow(int phase);
        abstract public PhasingSchedule getNextJoinSteadyPhase();

        public PhasingSchedule createPhase(
            int nSplitPhases,
            int childrenPhases[],
            int nJoinPhases,
            int postSplitBuffers[],
            int preJoinBuffers[])
        {
            // first advance all the appropriate buffers:
            {
                // first the splitter
                for (int splitPhase = 0;
                    splitPhase < nSplitPhases;
                    splitPhase++)
                {
                    // get the appropriate flow
                    SplitFlow flow = getSplitSteadyPhaseFlow(splitPhase);

                    // update the buffers (add data)
                    int nChild;
                    for (nChild = 0; nChild < getNumChildren(); nChild++)
                    {
                        postSplitBuffers[nChild] += flow.getPushWeight(nChild);
                    }
                }

                // now the children
                for (int nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    StreamInterface child = getHierarchicalChild(nChild);
                    for (int nPhase = 0;
                        nPhase < childrenPhases[nChild];
                        nPhase++)
                    {
                        // get the phase and check that I've enough 
                        // peek data in the buffer to run it
                        PhasingSchedule childPhase = getChildPhase(child, nPhase);
                        ASSERT(
                            postSplitBuffers[nChild]
                                >= childPhase.getOverallPeek());

                        // update the buffers
                        postSplitBuffers[nChild] -= childPhase.getOverallPop();
                        preJoinBuffers[nChild] += childPhase.getOverallPush();
                    }
                }

                // and finally run the joiner
                for (int joinPhase = 0; joinPhase < nJoinPhases; joinPhase++)
                {
                    // get the appropriate flow
                    JoinFlow flow = getJoinSteadyPhaseFlow(joinPhase);

                    // update the buffers (add data)
                    int nChild;
                    for (nChild = 0; nChild < getNumChildren(); nChild++)
                    {
                        preJoinBuffers[nChild] -= flow.getPopWeight(nChild);
                        ASSERT(preJoinBuffers[nChild] >= 0);
                    }
                }
            }

            // and now create the phase
            PhasingSchedule phase = new PhasingSchedule(sj);

            // add the splitter, children and joiner phases to this phase            
            {
                phase.appendPhase(sj.getSplitterPhases(nSplitPhases));
                for (int nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    StreamInterface child = sj.getHierarchicalChild(nChild);
                    phase.appendPhase(
                        sj.getChildPhases(child, childrenPhases[nChild]));
                }
                phase.appendPhase(sj.getJoinerPhases(nJoinPhases));
            }

            return phase;
        }

    }

    private class SJInitSchedulingUtility extends SJSchedulingUtility
    {
        SJInitSchedulingUtility(SplitJoin _sj)
        {
            super(_sj);
        }

        public void addSchedulePhase(PhasingSchedule phase)
        {
            sj.addInitScheduleStage(phase);
        }

        public void advanceChildSchedule(StreamInterface child, int nPhases)
        {
            sj.advanceChildInitSchedule(child, nPhases);
        }

        public PhasingSchedule getChildNextPhase(StreamInterface child)
        {
            return sj.getChildNextInitStage(child);
        }

        public PhasingSchedule getChildPhase(StreamInterface child, int stage)
        {
            return sj.getChildInitStage(child, stage);
        }

        public void advanceSplitSchedule()
        {
            sj.advanceSplitSchedule();
        }

        public void advanceJoinSchedule()
        {
            sj.advanceJoinSchedule();
        }

        public SplitFlow getNextSplitSteadyPhaseFlow()
        {
            return sj.getNextSplitSteadyPhaseFlow();
        }

        public SplitFlow getSplitSteadyPhaseFlow(int phase)
        {
            return sj.getSplitSteadyPhaseFlow(phase);
        }

        public PhasingSchedule getNextSplitSteadyPhase()
        {
            return sj.getNextSplitSteadyPhase();
        }

        public JoinFlow getNextJoinSteadyPhaseFlow()
        {
            return sj.getNextJoinSteadyPhaseFlow();
        }

        public JoinFlow getJoinSteadyPhaseFlow(int phase)
        {
            return sj.getJoinSteadyPhaseFlow(phase);
        }

        public PhasingSchedule getNextJoinSteadyPhase()
        {
            return sj.getNextJoinSteadyPhase();
        }
    }

    private class SJSteadySchedulingUtility extends SJSchedulingUtility
    {
        SJSteadySchedulingUtility(SplitJoin _sj)
        {
            super(_sj);
        }

        public void addSchedulePhase(PhasingSchedule phase)
        {
            sj.addSteadySchedulePhase(phase);
        }

        public void advanceChildSchedule(StreamInterface child, int nPhases)
        {
            sj.advanceChildSteadySchedule(child, nPhases);
        }

        public PhasingSchedule getChildNextPhase(StreamInterface child)
        {
            return sj.getChildNextSteadyPhase(child);
        }

        public PhasingSchedule getChildPhase(StreamInterface child, int stage)
        {
            return sj.getChildSteadyPhase(child, stage);
        }

        public void advanceSplitSchedule()
        {
            sj.advanceSplitSchedule();
        }

        public void advanceJoinSchedule()
        {
            sj.advanceJoinSchedule();
        }

        public SplitFlow getNextSplitSteadyPhaseFlow()
        {
            return sj.getNextSplitSteadyPhaseFlow();
        }

        public SplitFlow getSplitSteadyPhaseFlow(int phase)
        {
            return sj.getSplitSteadyPhaseFlow(phase);
        }

        public PhasingSchedule getNextSplitSteadyPhase()
        {
            return sj.getNextSplitSteadyPhase();
        }

        public JoinFlow getNextJoinSteadyPhaseFlow()
        {
            return sj.getNextJoinSteadyPhaseFlow();
        }

        public JoinFlow getJoinSteadyPhaseFlow(int phase)
        {
            return sj.getJoinSteadyPhaseFlow(phase);
        }

        public PhasingSchedule getNextJoinSteadyPhase()
        {
            return sj.getNextJoinSteadyPhase();
        }
    }

    /**
     * create the init schedule according to whatever limits
     * I am passed
     */
    public void computeMinLatencySchedule(
        SJSchedulingUtility utility,
        int splitExecs,
        int joinExecs,
        int childrenExecs[],
        int postSplitBuffers[],
        int preJoinBuffers[])
    {
        // first pull all the data out of the split-join
        // that needs to go (drain the join)
        while (joinExecs > 0)
        {
            int splitterPhases = 0;
            int joinerPhases = 0;
            int phaseChildrenExecs[] = new int[getNumChildren()];

            int joinerOverallPop[] = new int[getNumChildren()];

            // compute how many times I need to execute the joiner
            // before I get some output - this will be my phase
            {
                JoinFlow joinFlow;
                do
                {
                    joinFlow = utility.getJoinSteadyPhaseFlow(joinerPhases);
                    joinerPhases++;

                    int nChild;
                    for (nChild = 0; nChild < getNumChildren(); nChild++)
                    {
                        joinerOverallPop[nChild]
                            += joinFlow.getPopWeight(nChild);
                    }

                }
                while (joinFlow.getPushWeight() == 0);
            }

            int childOverallPeek[] = new int[getNumChildren()];
            int childOverallPop[] = new int[getNumChildren()];
            int childOverallPush[] = new int[getNumChildren()];

            // figure out how many times to execute all the children
            // to provide input for the joiner
            {
                int nChild;
                for (nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    StreamInterface child = getHierarchicalChild(nChild);
                    while (preJoinBuffers[nChild]
                        - joinerOverallPop[nChild]
                        + childOverallPush[nChild]
                        < 0)
                    {
                        PhasingSchedule childPhase =
                            utility.getChildPhase(
                                child,
                                phaseChildrenExecs[nChild]);

                        childOverallPeek[nChild] =
                            MAX(
                                childOverallPeek[nChild],
                                childOverallPop[nChild]
                                    + childPhase.getOverallPeek());
                        childOverallPop[nChild] += childPhase.getOverallPop();
                        childOverallPush[nChild] += childPhase.getOverallPush();

                        phaseChildrenExecs[nChild]++;
                    }
                }
            }

            int splitterOverallPush[] = new int[getNumChildren()];

            // figure out how many times to execute the splitter to
            // supply enough data for all the children to go
            {
                int nChild;
                for (nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    int childSplitterPhases = 0;
                    int childSplitterPush = 0;
                    while (postSplitBuffers[nChild]
                        + childSplitterPush
                        - childOverallPeek[nChild]
                        < 0)
                    {
                        SplitFlow splitFlow =
                            utility.getSplitSteadyPhaseFlow(
                                childSplitterPhases);
                        childSplitterPhases++;

                        childSplitterPush
                            += splitFlow.getPushWeight(nChild);
                    }

                    splitterPhases = MAX(splitterPhases, childSplitterPhases);
                }
                
                for (int nPhase = 0; nPhase < splitterPhases; nPhase++)
                {
                    SplitFlow splitFlow =
                        utility.getSplitSteadyPhaseFlow(
                            splitterPhases);
                    for (nChild = 0; nChild < getNumChildren();nChild++)
                    {
                        splitterOverallPush [nChild] += splitFlow.getPushWeight(nChild);
                    }
                }
            }

            // push the data available in the splitjoin down as much
            // as possible:
            {
                // execute the children as much as possible given
                // how much data they've been given by the splitter
                {
                    for (int nChild = 0; nChild < getNumChildren(); nChild++)
                    {
                        StreamInterface child = getHierarchicalChild(nChild);

                        while (postSplitBuffers[nChild]
                            + splitterOverallPush[nChild]
                            - childOverallPop[nChild]
                            >= utility
                                .getChildPhase(
                                    child,
                                    phaseChildrenExecs[nChild])
                                .getOverallPeek()
                            && phaseChildrenExecs[nChild] < childrenExecs[nChild])
                        {
                            PhasingSchedule childPhase =
                                utility.getChildPhase(
                                    child,
                                    phaseChildrenExecs[nChild]);

                            childOverallPeek[nChild] =
                                MAX(
                                    childOverallPeek[nChild],
                                    childOverallPop[nChild]
                                        + childPhase.getOverallPeek());
                            childOverallPop[nChild]
                                += childPhase.getOverallPop();
                            childOverallPush[nChild]
                                += childPhase.getOverallPush();

                            phaseChildrenExecs[nChild]++;
                        }
                    }
                }

                // and execute the joiner as much as possible, given
                // the data available for it in the channels
                joiner_push_data : while (joinExecs > joinerPhases)
                {
                    JoinFlow joinFlow;
                    joinFlow = utility.getJoinSteadyPhaseFlow(joinerPhases);

                    int nChild;
                    for (nChild = 0; nChild < getNumChildren(); nChild++)
                    {
                        if (joinFlow.getPopWeight(nChild)
                            > preJoinBuffers[nChild]
                                + childOverallPush[nChild]
                                - joinerOverallPop[nChild])
                            break joiner_push_data;
                    }
                    for (nChild = 0; nChild < getNumChildren(); nChild++)
                    {
                        joinerOverallPop[nChild]
                            += joinFlow.getPopWeight(nChild);
                    }
                    joinerPhases++;
                }
            }

            // okay, now I know how many times I need to run the
            // splitter, children and the joiner.  just run them :)
            {
                // get the phase
                PhasingSchedule phase =
                    utility.createPhase(
                        splitterPhases,
                        phaseChildrenExecs,
                        joinerPhases,
                        postSplitBuffers,
                        preJoinBuffers);

                // and adjust counters of execution:
                splitExecs -= splitterPhases;
                for (int nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    childrenExecs[nChild] -= phaseChildrenExecs[nChild];
                }
                joinExecs -= joinerPhases;

                utility.addSchedulePhase(phase);
            }
        }

        // now execute all the components (other than the join, which
        // is done initializing) until they're guaranteed to have been
        // initialized
        {
            // create the phase
            PhasingSchedule phase =
                utility.createPhase(
                    splitExecs,
                    childrenExecs,
                    0,
                    postSplitBuffers,
                    preJoinBuffers);

            // add it to the init schedule, if necessary
            if (phase.getNumPhases() != 0)
            {
                utility.addSchedulePhase(phase);
            }
        }
    }

    public void computeSchedule()
    {
        int steadyChildPhases[] = new int[getNumChildren()];
        int steadySplitPhases = getNumSplitPhases() * getSplitNumRounds();
        int steadyJoinPhases = getNumJoinPhases() * getJoinNumRounds();

        // first compute schedules for all my children
        {
            int nChild;
            for (nChild = 0; nChild < getNumChildren(); nChild++)
            {
                StreamInterface child = getHierarchicalChild(nChild);

                // compute child's schedule
                child.computeSchedule();

                // get the # of phases that this child needs to have executed
                // to complete a steady state schedule
                steadyChildPhases[nChild] =
                    child.getNumSteadyPhases() * getChildNumExecs(nChild);
            }
        }

        int numInitSplitStages = 0;
        int numInitJoinStages = 0;
        int numInitChildStages[] = new int[getNumChildren()];

        // Figure out how many times the splitter, joiner
        // and children need to get executed for init.
        // Figuring out children is easy - just make sure that all
        // init stages get executed.  Join is also easy - make sure I
        // drain enough data so that it's a genuine pull schedule.
        // The split is a little bit more complicated - I need to make
        // sure that I provide enough data for the children to execute
        // the full steady-state schedule after.  That basically means
        // possibly enlarging their peeking.
        {
            int nChild;
            for (nChild = 0; nChild < getNumChildren(); nChild++)
            {
                StreamInterface child = getHierarchicalChild(nChild);

                // minimum number of times the child will need to
                // get executed to initialize.  Just the # of init stages
                // this child has (no extra magic)
                numInitChildStages[nChild] = child.getNumInitStages();

                // figure out how many times this child needs the splitter
                // to be executed
                {
                    int childInitSplitStages = 0;

                    int childInitPeek =
                        child.getInitPop()
                            + MAX(
                                child.getInitPeek() - child.getInitPop(),
                                child.getSteadyPeek() - child.getSteadyPop());

                    while (childInitPeek > 0)
                    {
                        // subtract appropriate amount of data flow
                        // from the amount of data still needed by the child
                        childInitPeek
                            -= getSplitSteadyPhaseFlow(
                                childInitSplitStages).getPushWeight(
                                nChild);

                        // note that I've executed another split stage
                        childInitSplitStages++;
                    }

                    // make sure that I've got the largest number of split
                    // executions needed - that's how many times I really
                    // need to execute the split
                    numInitSplitStages =
                        MAX(numInitSplitStages, childInitSplitStages);
                }

                // figure out how many times this child needs the joiner
                // to be executed
                {
                    int childInitJoinStages = 0;

                    int childInitPush = child.getInitPush();

                    while (childInitPush > 0)
                    {
                        // subtract appropriate amount of data flow
                        // from the amount of data still needed by the child
                        childInitPush
                            -= getJoinSteadyPhaseFlow(
                                childInitJoinStages).getPopWeight(
                                nChild);

                        // note that I've executed another join stage
                        childInitJoinStages++;
                    }

                    // since I don't actually want to drain all the data 
                    // (thus trigger an extra execution of the child to
                    // provide all this data), I want to execute the
                    // joiner one time less than I've just calculated above
                    childInitJoinStages--;

                    // make sure that I've got the largest number of split
                    // executions needed - that's how many times I really
                    // need to execute the split
                    // it's possible that childInitJoinStages became negative
                    // after the decrease above, but that's OK, 
                    // 'cause numIniSplitStages started as 0 anyway :)
                    numInitSplitStages =
                        MAX(numInitSplitStages, childInitJoinStages);
                }
            }
        }

        // store amount of data in buffers internal to the splitjoin        
        int postSplitBuffers[] = new int[getNumChildren()];
        int preJoinBuffers[] = new int[getNumChildren()];

        // and now in two lines do the whole scheduling bit!

        // first the init schedule:
        computeMinLatencySchedule(
            new SJInitSchedulingUtility(this),
            numInitSplitStages,
            numInitJoinStages,
            numInitChildStages,
            postSplitBuffers,
            preJoinBuffers);

        // and now the steady schedule
        computeMinLatencySchedule(
            new SJSteadySchedulingUtility(this),
            steadySplitPhases,
            steadyJoinPhases,
            steadyChildPhases,
            postSplitBuffers,
            preJoinBuffers);

        // done!
        // (ain't that amazing?)
    }
}
