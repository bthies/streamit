package streamit.scheduler.minlatency;

/* $Id: Pipeline.java,v 1.4 2002-07-19 05:09:09 karczma Exp $ */

import streamit.scheduler.iriter./*persistent.*/
PipelineIter;
import streamit.scheduler.hierarchical.StreamInterface;
import streamit.scheduler.base.StreamFactory;
import streamit.scheduler.hierarchical.PhasingSchedule;

/**
 * This class implements a minimum-latency algorithm for creating
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

    private interface PipelineSchedulingUtility
    {
        public void addSchedulePhase(PhasingSchedule phase);
        public void advanceChildSchedule(StreamInterface child);
        public PhasingSchedule getChildNextPhase(StreamInterface child);
        public PhasingSchedule getChildPhase(
            StreamInterface child,
            int phase);
    }

    private class PipelineInitSchedulingUtility
        implements PipelineSchedulingUtility
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
        implements PipelineSchedulingUtility
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
    public void computeMinLatencySchedule(
        PipelineSchedulingUtility utility,
        int childrenExecs[],
        int dataInBuffers[])
    {
        // reset the buffer below the pipeline (it's not actually used
        // for any computation)
        dataInBuffers[getNumChildren()] = 0;
        
        // calculate how much data the pipeline is supposed to push out
        // during execution of this schedule
        int pipelineOverallPush = 0;
        {
            int lastChildIndex = getNumChildren () - 1;
            StreamInterface child = getHierarchicalChild(lastChildIndex);
            int nPhase = childrenExecs [lastChildIndex] - 1;
            for ( ; nPhase >= 0; nPhase--)
            {
                PhasingSchedule phase = utility.getChildPhase(child, nPhase);
                pipelineOverallPush += phase.getOverallPush();
            }
        }

        // repeat while the last child still needs to pull some data
        while (dataInBuffers[getNumChildren()] < pipelineOverallPush)
        {
            int numChildExecs[] = new int[getNumChildren()];

            // figure out how many times each child needs to get executed
            {
                int nextChildDataNeeded = 1, nChild;
                for (nChild = getNumChildren() - 1;
                    nChild >= 0;
                    nChild--)
                {
                    StreamInterface child = getHierarchicalChild(nChild);
                    int childPeek = 0, childPop = 0;

                    // repeat while still need to provide data 
                    // for the next child
                    while (nextChildDataNeeded > 0)
                    {
                        // figure out how much this phase/stage will 
                        // push/pop/peek
                        PhasingSchedule phase =
                            utility.getChildPhase(
                                child,
                                numChildExecs[nChild]);
                        int phasePeek = phase.getOverallPeek();
                        int phasePop = phase.getOverallPop();
                        int phasePush = phase.getOverallPush();

                        // update the overall phase child peek/pop
                        childPeek = MAX(childPeek, childPop + phasePeek);
                        childPop += phasePop;

                        // reduce the amount of data still needed
                        // by the next child
                        nextChildDataNeeded -= phasePush;

                        // note that I just executed another stage/phase 
                        // of this child
                        numChildExecs[nChild]++;
                    }

                    // figure out how much data the previous child
                    // still needs to provide:
                    nextChildDataNeeded =
                        childPeek - dataInBuffers[nChild];
                }
            }

            // construct an actual stage of the schedule and execute it
            {
                PhasingSchedule steadyPhase = new PhasingSchedule(this);

                int nChild;
                for (nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    StreamInterface child = getHierarchicalChild(nChild);

                    for (;
                        numChildExecs[nChild] > 0;
                        numChildExecs[nChild]--)
                    {
                        PhasingSchedule phase =
                            utility.getChildNextPhase(child);

                        // add this stage to the init schedule
                        steadyPhase.appendPhase(phase);
                        utility.advanceChildSchedule(child);

                        // better have enough data in the buffer to execute 
                        // this phase
                        ASSERT(
                            nChild == 0
                                || dataInBuffers[nChild]
                                    >= phase.getOverallPeek());

                        // adjust buffers for this child
                        dataInBuffers[nChild] -= phase.getOverallPop();
                        dataInBuffers[nChild
                            + 1] += phase.getOverallPush();

                        // mark down that this child had another phase executed
                        childrenExecs[nChild]--;
                    }
                }

                // add the init stage to the init schedule
                utility.addSchedulePhase(steadyPhase);
            }
        }

        // it is possible that I need another phase to execute clean-up
        // phases of the children that don't output from the pipeline
        // (all but the bottom most child)
        {
            PhasingSchedule extraPhase = new PhasingSchedule(this);

            int nChild;
            for (nChild = 0; nChild < getNumChildren(); nChild++)
            {
                StreamInterface child = getHierarchicalChild(nChild);

                for (;
                    childrenExecs[nChild] > 0;
                    childrenExecs[nChild]--)
                {
                    PhasingSchedule phase =
                        utility.getChildNextPhase(child);

                    // add this stage to the init schedule
                    extraPhase.appendPhase(phase);
                    utility.advanceChildSchedule(child);

                    // better have enough data in the buffer to execute 
                    // this phase
                    ASSERT(
                        nChild == 0
                            || dataInBuffers[nChild]
                                >= phase.getOverallPeek());

                    // adjust buffers for this child
                    dataInBuffers[nChild] -= phase.getOverallPop();
                    dataInBuffers[nChild + 1] += phase.getOverallPush();
                }

                // make sure that I've executed every child the right number
                // of phases.  if this assert goes off, the error is not in
                // the code right above (the for above will never execute)
                // but in the init/steady schedule calculcation!
                ASSERT(childrenExecs[nChild] == 0);
            }

            // have I done anything?
            if (extraPhase.getNumPhases() != 0)
            {
                // yes - add the extra phase to the schedule!
                utility.addSchedulePhase(extraPhase);
            }
        }
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

        // compute the minimal amount of data necessary in buffers
        // after the initialization has taken place
        // this is a little tricky - for every child I need to figure out
        // exactly how many times I will run it in the init schedule
        // including the child's steady-state phases that I'll run to fill
        // up the buffers

        // store how many init stages and steady-state phases each child will 
        // need to be executed in order to provide enough data for the steady
        // state
        int childInitStages[] = new int[getNumChildren()];

        // store how many data each child will need in the process of 
        // initialization
        // extra element here is just to simplify the algorithm
        int childInitDataNeeded[] = new int[getNumChildren() + 1];

        // number of children that HAVE TO execute at least one init stage
        // (that includes their own init-stages)
        int numChildrenForInit = 0;

        // find out how much each child needs to be executed in order to
        // process all of the init functions
        {
            int nChild;
            for (nChild = getNumChildren() - 1; nChild >= 0; nChild--)
            {
                StreamInterface child = getHierarchicalChild(nChild);

                childInitStages[nChild] = child.getNumInitStages();

                // first figure out how much data this child pops for
                // running just the init stages
                int initPop = child.getInitPop();

                // and how much extra data is needed due to peeking in 
                // init and steady state
                int initPeek =
                    MAX(
                        child.getInitPeek() - child.getInitPop(),
                        child.getSteadyPeek() - child.getSteadyPop());

                int initPush = child.getInitPush();
                int nPhase = 0;

                // does the next child need more data?
                while (initPush < childInitDataNeeded[nChild + 1])
                {
                    // yes

                    // execute another phase
                    PhasingSchedule initStage =
                        getChildInitStage(child, childInitStages[nChild]);
                    childInitStages[nChild]++;
                    initPush += initStage.getOverallPush();

                    // and don't forget that this means that 
                    // this child may need more data!
                    initPeek =
                        MAX(
                            initPeek - initStage.getOverallPop(),
                            initStage.getOverallPeek()
                                - initStage.getOverallPop());
                    initPop += initStage.getOverallPop();

                    // next phase
                    nPhase = (nPhase + 1) % child.getNumSteadyPhases();
                }

                // store this child's needs for init data
                childInitDataNeeded[nChild] = initPop + initPeek;

                numChildrenForInit += (childInitStages[nChild] != 0 ? 1 : 0);
            }
        }

        // amount of data in buffers.  Each entry corresponds to the buffer
        // ABOVE the correspondingly numbered buffer (dataInBuffers[0] is above
        // the pipeline).  The last buffer obviously is the buffer right below
        // the last child, which is just below the pipeline.
        int dataInBuffers[] = new int[getNumChildren() + 1];

        // create the initialization schedule
        // The intialization schedule is created by executing the bottom-most
        // child, and pulling the data from upper children accordingly.
        // This is repeated until all children have had at least
        // childInitStages worth of stages or phases executed.  Reasoning for
        // this is in my (karczma) notebook, date 02/07/14.
        
        computeMinLatencySchedule(new PipelineInitSchedulingUtility(this), childInitStages, dataInBuffers);
        computeMinLatencySchedule(new PipelineSteadySchedulingUtility(this), steadyChildPhases, dataInBuffers);
    }
}
