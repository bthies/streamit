package streamit.scheduler.singleappearance;

/* $Id: Pipeline.java,v 1.2 2002-06-30 04:01:20 karczma Exp $ */

import java.util.Map;
import java.util.HashMap;
import streamit.scheduler.iriter./*persistent.*/PipelineIter;
import streamit.scheduler.hierarchical.StreamInterface;
import streamit.scheduler.base.StreamFactory;
import streamit.scheduler.Schedule;
import streamit.scheduler.hierarchical.PhasingSchedule;

/**
 * This class implements a single-appearance algorithm for creating
 * schedules.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class Pipeline extends streamit.scheduler.hierarchical.Pipeline
{
    public Pipeline(PipelineIter iterator, StreamFactory factory)
    {
        super(iterator, factory);
    }

    public void computeSchedule()
    {
        // first compute schedules for all my children
        {
            int nChild;
            for (nChild = 0; nChild < getNumChildren(); nChild++)
            {
                getChild(nChild).computeSchedule();
            }
        }

        // now go through all the children again and figure out how many
        // data each child needs to be executed to fill up the buffers
        // enough to initialize the entire pipeline without producing
        // any data - this means that the last child doesn't get executed
        // even once
        // I do this by iterating from end to beggining and computing the
        // number of elements that each child needs to produce
        int numExecutionsForInit[] = new int[getNumChildren()];
        {
            int consumedByNext = 0;

            int nChild = getNumChildren() - 1;
            for (; nChild >= 0; nChild--)
            {
                StreamInterface child = getHierarchicalChild(nChild);

                int producesForInit = child.getInitPush();
                int producesPerIter = child.getSteadyPush();
                int numItersInit;
                if (producesPerIter != 0)
                {
                    // this child actually produces some data - this is
                    // the common case
                    numItersInit =
                        ((consumedByNext - producesForInit)
                            + producesPerIter
                            - 1)
                            / producesPerIter;
                    if (numItersInit < 0)
                        numItersInit = 0;
                }
                else
                {
                    // this child does not produce any data
                    // make sure that consumedByPrev is 0 (otherwise
                    // I cannot execute the next child, 'cause it will
                    // never get any input)
                    ASSERT(consumedByNext == 0);

                    // there will be no cycles executed for initialization
                    // of children downstream
                    numItersInit = 0;
                }

                // associate the child with the number of executions required
                // by the child to fill up the pipeline
                numExecutionsForInit[nChild] = numItersInit;

                // and figure out how many data this particular child
                // needs to initialize the pipeline;  that is:
                //  + number of iters * steady pop
                //  + (steady peek - steady pop)
                //  + init peek (data needed for init of this child only)
                consumedByNext =
                    numItersInit * child.getSteadyPop()
                        + (child.getSteadyPeek() - child.getSteadyPop())
                        + child.getInitPeek();
            }
        }

        // compute an initialization schedule
        // now that I know how many times each child needs to be executed,
        // it should be easy to compute this - just execute each stream
        // an appropriate number of times.
        // In this approach, I do not care about the size of the buffers, and
        // may in fact be enlarging them much more than necessary
        {
            // the entire schedule will be just a single phase, so I need
            // to wrap it in a unique PhasingSchedule myself
            PhasingSchedule initStage;
            initStage = new PhasingSchedule(this);

            int nChild;
            for (nChild = 0; nChild < getNumChildren(); nChild++)
            {
                StreamInterface child = getHierarchicalChild(nChild);
                // add the initialization schedule:
                initStage.appendPhase(child.getPhasingInitSchedule());

                // add the steady schedule an appropriate number of times:
                {
                    int n;
                    for (n = 0; n < numExecutionsForInit[nChild]; n++)
                    {
                        initStage.appendPhase(
                            child.getPhasingSteadySchedule());
                    }
                }
            }

            // now append the one init phase to my initialization phase
            algorithm.addInitScheduleStage(initStage);
        }

        // compute the steady state schedule
        // this one is quite easy - I go through the children and execute them
        // an appropriate number of times
        {
            PhasingSchedule steadyPhase;
            steadyPhase = new PhasingSchedule(this);

            int nChild;
            for (nChild = 0; nChild < getNumChildren(); nChild++)
            {
                StreamInterface child = getHierarchicalChild(nChild);

                int n;
                for (n = 0; n < getChildNumExecs(nChild); n++)
                {
                    steadyPhase.appendPhase(
                        child.getPhasingSteadySchedule());
                }
            }

            algorithm.addSchedulePhase(steadyPhase);
        }
    }
}
