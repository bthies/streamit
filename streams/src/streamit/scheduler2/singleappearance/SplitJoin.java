package streamit.scheduler.singleappearance;

/* $Id: SplitJoin.java,v 1.1 2002-07-02 03:37:49 karczma Exp $ */

import streamit.scheduler.iriter./*persistent.*/
SplitJoinIter;
import streamit.scheduler.base.StreamFactory;
import streamit.scheduler.hierarchical.StreamInterface;
import streamit.scheduler.Schedule;
import streamit.scheduler.hierarchical.PhasingSchedule;

/**
 * This class implements a single-appearance algorithm for creating
 * schedules.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class SplitJoin extends streamit.scheduler.hierarchical.SplitJoin
{
    PhasingSchedule splitSched, joinSched;

    public SplitJoin(SplitJoinIter iterator, StreamFactory factory)
    {
        super(iterator, factory);

        // compute the splitter schedule
        {
            splitSched = new PhasingSchedule(this);
            int nPhase;
            for (nPhase = 0; nPhase < super.getNumSplitPhases(); nPhase++)
            {
                splitSched.appendPhase(super.getSplitPhase(nPhase));
            }
        }

        // compute the joiner schedule
        {
            joinSched = new PhasingSchedule(this);
            int nPhase;
            for (nPhase = 0; nPhase < super.getNumJoinPhases(); nPhase++)
            {
                joinSched.appendPhase(super.getJoinPhase(nPhase));
            }
        }
    }

    // Override the functions that deal with schedules for joiner
    // and splitter - single appearance schedules only need one
    // phase for the splitter and joiner schedules respectively

    public int getNumSplitPhases()
    {
        return 1;
    }

    public PhasingSchedule getSplitPhase(int nPhase)
    {
        // single appearance schedule has only one split phase
        ASSERT(nPhase == 0);
        return splitSched;
    }

    /**
     * @return one phase schedule for the splitter
     */
    public PhasingSchedule getSplitPhase()
    {
        return splitSched;
    }

    public int getNumJoinPhases()
    {
        return 1;
    }

    public PhasingSchedule getJoinPhase(int nPhase)
    {
        // single appearance schedule has only one join phase
        ASSERT(nPhase == 0);
        return joinSched;
    }

    /**
     * @return one phase schedule for the joiner
     */
    public PhasingSchedule getJoinPhase()
    {
        return joinSched;
    }

    // this function is basically copied from scheduler v1
    public void computeSchedule()
    {
        // compute the children's schedules and figure out
        // how many times the split needs to be executed to feed
        // all the buffers so the children can initialize (including the
        // peek - pop amounts!)
        int initSplitRunCount = 0;
        {
            // go through all the children and check how much
            int nChild;
            for (nChild = 0; nChild < getNumChildren(); nChild++)
            {
                // get the child
                StreamInterface child = getHierarchicalChild(nChild);
                ASSERT(child);

                // compute child's schedule
                child.computeSchedule();

                // get the amount of data needed to initilize this child
                int childInitDataConsumption = child.getInitPop();

                // add the amount of data needed to allow for peeking
                // this is the total amount needed to intialize this path
                // of the split join
                childInitDataConsumption
                    += (child.getSteadyPeek() - child.getSteadyPop());

                // now figure out how many times the split needs to be run in
                // initialization to accomodate this child
                int splitRunCount;
                if (childInitDataConsumption != 0)
                {
                    // just divide the amount of data needed by data received
                    // per iteration of the split
                    int splitDataSent = splitFlow.pushWeights[nChild];
                    ASSERT(splitDataSent > 0);

                    splitRunCount =
                        (childInitDataConsumption + splitDataSent - 1)
                            / splitDataSent;
                }
                else
                {
                    // the child doesn't need any data to intitialize, so I
                    // don't need to run the split for it at all
                    splitRunCount = 0;
                }

                // pick the max
                if (splitRunCount > initSplitRunCount)
                {
                    initSplitRunCount = splitRunCount;
                }
            }
        }

        // compute the init schedule
        {
            PhasingSchedule initSched = new PhasingSchedule(this);

            // run through the split an appropriate number of times
            // and append it to the init schedule
            {
                PhasingSchedule splitSched = this.getSplitPhase();

                int nRun;
                for (nRun = 0; nRun < initSplitRunCount; nRun++)
                {
                    initSched.appendPhase(splitSched);
                }
            }

            // now add the initialization schedules for all the children
            {
                int nChild;
                for (nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    StreamInterface child = getHierarchicalChild(nChild);

                    int nStage = 0;
                    for (; nStage < child.getNumInitStages(); nStage++)
                    {
                        initSched.appendPhase(
                            child.getInitScheduleStage(nStage));
                    }
                }
                algorithm.addInitScheduleStage(initSched);
            }
        }

        // compute the steady schedule
        {
            PhasingSchedule steadySched = new PhasingSchedule(this);

            // first add the split schedule the right # of times
            {
                int nReps;
                for (nReps = 0; nReps < getSplitNumRounds(); nReps++)
                {
                    steadySched.appendPhase(getSplitPhase());
                }
            }

            // add the schedule for execution of all the children
            // of the split join
            {
                int nChild;
                for (nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    StreamInterface child = getHierarchicalChild(nChild);

                    int nRun;
                    for (nRun = 0;
                        nRun < childrenNumExecs[nChild].intValue();
                        nRun++)
                    {
                        int nPhase;
                        for (nPhase = 0;
                            nPhase < child.getNumSteadyPhases();
                            nPhase++)
                        {
                            steadySched.appendPhase(
                                child.getSteadySchedulePhase(nPhase));
                        }
                    }
                }
            }

            // finally add the join schedule the right # of times
            {
                int nReps;
                for (nReps = 0; nReps < getJoinNumRounds(); nReps++)
                {
                    steadySched.appendPhase(getJoinPhase());
                }
            }

            algorithm.addSchedulePhase(steadySched);
        }
    }
}
