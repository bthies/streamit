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
PipelineIter;
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

public class Pipeline extends streamit.scheduler2.hierarchical.Pipeline
{
    public Pipeline(PipelineIter iterator, StreamFactory factory)
    {
        super(iterator, factory);
    }

    private abstract class PipelineSchedulingUtility
    {
        abstract public void addSchedulePhase(PhasingSchedule phase);
        abstract public void advanceChildSchedule(StreamInterface child);
        abstract public PhasingSchedule getChildNextPhase(StreamInterface child);
        abstract public PhasingSchedule getChildPhase(
            StreamInterface child,
            int phase);

        public void advanceOnePhase(
            int nChild,
            int nPhase,
            int dataInBuffers[],
            int borrowedData[])
        {
            StreamInterface child = getHierarchicalChild(nChild);
            PhasingSchedule phase = getChildPhase(child, nPhase);

            // I've popped and peeked some data from the upstream
            // channel - reflect it in the buffered and borrowed data
            borrowedData[nChild] =
                MAX(
                    borrowedData[nChild],
                    -dataInBuffers[nChild] + phase.getOverallPeek());
            dataInBuffers[nChild] -= phase.getOverallPop();

            // likewise, I've pushed some data into the downstream 
            // channel - reflect it in the buffered and borrowed data
            if (borrowedData[nChild + 1] != 0)
            {
                borrowedData[nChild + 1] =
                    MAX(
                        borrowedData[nChild + 1] - phase.getOverallPush(),
                        0);
            }
            dataInBuffers[nChild + 1] += phase.getOverallPush();
        }
    }

    private class PipelineInitSchedulingUtility
        extends PipelineSchedulingUtility
    {
        Pipeline pipeline;
        PipelineInitSchedulingUtility(Pipeline _pipeline)
        {
            pipeline = _pipeline;
        }

        public void addSchedulePhase(PhasingSchedule phase)
        {
            pipeline.addInitScheduleStage(phase);
        }

        public void advanceChildSchedule(StreamInterface child)
        {
            pipeline.advanceChildInitSchedule(child);
        }

        public PhasingSchedule getChildNextPhase(StreamInterface child)
        {
            return pipeline.getChildNextInitStage(child);
        }

        public PhasingSchedule getChildPhase(
            StreamInterface child,
            int stage)
        {
            return pipeline.getChildInitStage(child, stage);
        }
    }

    private class PipelineSteadySchedulingUtility
        extends PipelineSchedulingUtility
    {
        Pipeline pipeline;
        PipelineSteadySchedulingUtility(Pipeline _pipeline)
        {
            pipeline = _pipeline;
        }

        public void addSchedulePhase(PhasingSchedule phase)
        {
            pipeline.addSteadySchedulePhase(phase);
        }

        public void advanceChildSchedule(StreamInterface child)
        {
            pipeline.advanceChildSteadySchedule(child);
        }

        public PhasingSchedule getChildNextPhase(StreamInterface child)
        {
            return pipeline.getChildNextSteadyPhase(child);
        }

        public PhasingSchedule getChildPhase(
            StreamInterface child,
            int stage)
        {
            return pipeline.getChildSteadyPhase(child, stage);
        }
    }

    /**
     * create the init schedule according to whatever limits
     * I am passed
     */
    public int computeMinLatencySchedule(
        PipelineSchedulingUtility utility,
        int childrenExecs[],
        int dataInBuffers[],
        int lastStreamMaxExecPerPhase)
    {
        /*
        // if only one child, just add all the appropriate phases!
        // BUGBUG this is a HACK!
        if (getNumChildren() == 1)
        {
            PhasingSchedule phase = new PhasingSchedule(this);
            while (childrenExecs[0] != 0)
            {
                phase.appendPhase(
                    utility.getChildNextPhase(getHierarchicalChild(0)));
                utility.advanceChildSchedule(getHierarchicalChild(0));
                childrenExecs[0]--;
            }
            utility.addSchedulePhase(phase);
            return;
        }
        */

        // compute how many child executions were requested:
        int totalChildrenExecs = 0;
        {
            for (int nChild = 0; nChild < getNumChildren(); nChild++)
            {
                totalChildrenExecs += childrenExecs[nChild];
            }
        }

        // estimate max of how many times the top child should be executed per
        // phase, based on how max of how many times the bottom child should
        // be executed per phase
        int firstStreamMaxExecPerPhase =
            (int) ((float)childrenExecs[0]
                * (float)lastStreamMaxExecPerPhase
                / (float)childrenExecs[getNumChildren()
                - 1]);
        assert firstStreamMaxExecPerPhase <= childrenExecs[0];

        // execute the children however many times is necessary
        // keep track of how many executions are "requested" and
        // how many are extra to keep the pull algorithm happy.
        int extraChildrenExecs = 0;
        while (totalChildrenExecs > 0)
        {
            // allocate the items borrowed table
            int dataBorrowed[] = new int[getNumChildren() + 1];

            // and the number of times every stream gets executed
            // per phase
            int phaseChildrenExecs[] = new int[getNumChildren()];

            // go from bottom to top, pulling data
            {
                // first the bottom-most child:
                int nChild = getNumChildren() - 1;

                // figure out how many times I want to execute the last child.
                // I want to execute the last child at least once, and no more
                // than lastStreamMaxExecPerPhase and no more than
                // childrenExecs[nChild] (unless it's 0)
                lastStreamMaxExecPerPhase =
                    MIN(childrenExecs[nChild], lastStreamMaxExecPerPhase);
                lastStreamMaxExecPerPhase =
                    MAX(lastStreamMaxExecPerPhase, 1);
                    
                // if I've already executed the last child the required
                // number of times, find the bottom-most child that still
                // needs executing 
                // (I know there is one, because totalChildrenExecs > 0)
                if (childrenExecs[nChild] == 0)
                {
                    while (childrenExecs [nChild] == 0)
                    {
                        nChild--;
                    }
                    assert nChild >= 0;
                    
                    lastStreamMaxExecPerPhase = childrenExecs [nChild];
                }
                
                int dataInBuffersBelow = dataInBuffers [nChild + 1];

                // execute this child until I've either executed it the
                // appropriate number of times, or I've produced some data
                while (phaseChildrenExecs[nChild]
                    < lastStreamMaxExecPerPhase
                    && dataInBuffers[nChild + 1] == dataInBuffersBelow)
                {
                    utility.advanceOnePhase(
                        nChild,
                        phaseChildrenExecs[nChild],
                        dataInBuffers,
                        dataBorrowed);
                    phaseChildrenExecs[nChild]++;
                }

                // now all the other children
                for (nChild--; nChild >= 0; nChild--)
                {
                    // push data into the downstream channel
                    // until no more data has been borrowed from it
                    while (dataBorrowed[nChild + 1] > 0)
                    {
                        utility.advanceOnePhase(
                            nChild,
                            phaseChildrenExecs[nChild],
                            dataInBuffers,
                            dataBorrowed);
                        phaseChildrenExecs[nChild]++;
                    }
                }
            }

            // execute the top child as many times as possible without
            // consuming more data or executing it more times than
            // childrenExecs allows me to, nor more than 
            // firstStreamMaxExecPerPhase allows me
            {
                StreamInterface topChild = getHierarchicalChild(0);

                while (phaseChildrenExecs[0]
                    < MIN(childrenExecs[0], firstStreamMaxExecPerPhase))
                {
                    PhasingSchedule phase =
                        utility.getChildPhase(
                            topChild,
                            phaseChildrenExecs[0]);
                    if (phase.getOverallPeek() > 0)
                        break;
                        
                    utility.advanceOnePhase(
                        0,
                        phaseChildrenExecs[0],
                        dataInBuffers,
                        dataBorrowed);
                    phaseChildrenExecs[0]++;
                }
            }

            // now go from top to bottom, and push the data down
            // skip the first child, in case it is a source!
            for (int nChild = 1; nChild < getNumChildren(); nChild++)
            {
                StreamInterface child = getHierarchicalChild(nChild);

                // while there's enough data in buffer to execute the next phase                
                while (dataInBuffers[nChild]
                    >= utility
                        .getChildPhase(child, phaseChildrenExecs[nChild])
                        .getOverallPeek())
                {
                    utility.advanceOnePhase(
                        nChild,
                        phaseChildrenExecs[nChild],
                        dataInBuffers,
                        dataBorrowed);
                    phaseChildrenExecs[nChild]++;
                }
            }

            // okay, now just construct the phase and be done!
            {
                PhasingSchedule phase = new PhasingSchedule(this);

                for (int nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    StreamInterface child = getHierarchicalChild(nChild);

                    phase.appendPhase(
                        getChildPhases(child, phaseChildrenExecs[nChild]));

                    if (childrenExecs[nChild]
                        >= phaseChildrenExecs[nChild])
                    {
                        childrenExecs[nChild] -= phaseChildrenExecs[nChild];
                        totalChildrenExecs -= phaseChildrenExecs[nChild];
                    }
                    else
                    {
                        totalChildrenExecs -= childrenExecs[nChild];
                        extraChildrenExecs
                            += (phaseChildrenExecs[nChild]
                                - childrenExecs[nChild]);
                        childrenExecs[nChild] = 0;
                    }
                }

                utility.addSchedulePhase(phase);
            }
        }

        return extraChildrenExecs;
    }

    public void computeSchedule()
    {
        int steadyChildPhases[] = new int[getNumChildren()];

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

        int dataInBuffers[] = new int[getNumChildren() + 1];

        // first compute an initialization schedule
        {
            // figure out how many phases each child needs to execute
            int childInitStages[] = new int[getNumChildren()];
            for (int nChild = 0; nChild < getNumChildren(); nChild++)
            {
                StreamInterface child = getHierarchicalChild(nChild);
                childInitStages[nChild] = child.getNumInitStages();
            }

            // also check that all children will feed their
            // downstream siblings enough data to execute their 
            // steady state
            for (int nChild = 0; nChild < getNumChildren() - 1; nChild++)
            {
                StreamInterface child = getHierarchicalChild(nChild);
                int dataNeeded =
                    getHierarchicalChild(nChild + 1).getSteadyPeek()
                        - getHierarchicalChild(nChild + 1).getSteadyPop();

                int dataPushed = 0;
                int nStages;
                for (nStages = 0; dataNeeded > dataPushed; nStages++)
                {
                    dataPushed
                        += getChildInitStage(child, nStages).getOverallPush();
                }

                childInitStages[nChild] =
                    MAX(childInitStages[nChild], nStages);
            }

            // and create a schedule that will execute enough stages
            computeMinLatencySchedule(
                new PipelineInitSchedulingUtility(this),
                childInitStages,
                dataInBuffers,
                childInitStages[getNumChildren() - 1]);
        }

        // now compute a steady state schedule
        {
            // figure out how many phases each child needs to execute
            int numChildPhases[] = new int[getNumChildren()];
            for (int nChild = 0; nChild < getNumChildren(); nChild++)
            {
                numChildPhases[nChild] =
                    getChildNumExecs(nChild)
                        * getHierarchicalChild(nChild).getNumSteadyPhases();
            }

            // figure out how many times I want to run the last child
            int lastChildNumExecPerPhase;
            {
                if (getHierarchicalChild(getNumChildren() - 1)
                    .getSteadyPush()
                    != 0)
                {
                    // the last child is not a sink
                    // execute it to the heart's content
                    lastChildNumExecPerPhase =
                        numChildPhases[getNumChildren() - 1];
                }
                else
                {
                    // the last child is a sink! use the algorithm described
                    // in karczma's thesis (page 87) to figure out how many
                    // phases of the pipeline I want
                    int numDataConsumed =
                        getChildNumExecs(getNumChildren() - 1)
                            * getHierarchicalChild(getNumChildren() - 1)
                                .getSteadyPop();
                    double numPipelinePhases = Math.sqrt(numDataConsumed);
                    if (numPipelinePhases != 0)
                        lastChildNumExecPerPhase =
                            (int)Math.ceil(
                                ((double)numChildPhases[getNumChildren()
                                    - 1])
                                    / numPipelinePhases);
                    else
                        lastChildNumExecPerPhase =
                            numChildPhases[getNumChildren() - 1];
                }
            }

            int extraExecs =
                computeMinLatencySchedule(
                    new PipelineSteadySchedulingUtility(this),
                    numChildPhases,
                    dataInBuffers,
                    lastChildNumExecPerPhase);

            assert extraExecs == 0;
        }
    }
}
