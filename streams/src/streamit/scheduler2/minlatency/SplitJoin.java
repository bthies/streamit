package streamit.scheduler.minlatency;

/* $Id: SplitJoin.java,v 1.1 2002-07-18 05:34:45 karczma Exp $ */

import streamit.scheduler.iriter./*persistent.*/
SplitJoinIter;
import streamit.scheduler.hierarchical.StreamInterface;
import streamit.scheduler.base.StreamFactory;
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
    public SplitJoin(SplitJoinIter iterator, StreamFactory factory)
    {
        super(iterator, factory);
    }
    
    private interface SJSchedulingUtility
    {
        public void addSchedulePhase(PhasingSchedule phase);
        public void advanceChildSchedule(StreamInterface child);
        public PhasingSchedule getChildNextPhase(StreamInterface child);
        public PhasingSchedule getChildPhase(StreamInterface child, int phase);
        public void advanceSplitSchedule ();
        public void advanceJoinSchedule ();
        
        public SplitFlow getNextSplitSteadyPhaseFlow ();
        public SplitFlow getSplitSteadyPhaseFlow (int phase);
        public PhasingSchedule getNextSplitSteadyPhase();
        public JoinFlow getNextJoinSteadyPhaseFlow ();
        public JoinFlow getJoinSteadyPhaseFlow (int phase);
        public PhasingSchedule getNextJoinSteadyPhase();
    }
    
    private class SJInitSchedulingUtility implements SJSchedulingUtility
    {
        SplitJoin sj;
        SJInitSchedulingUtility (SplitJoin _sj)
        {
            sj = _sj;
        }
        
        public void addSchedulePhase(PhasingSchedule phase)
        {
            sj.addInitScheduleStage(phase);
        }
        
        public void advanceChildSchedule(StreamInterface child)
        {
            sj.advanceChildInitSchedule(child);
        }
        
        public PhasingSchedule getChildNextPhase(StreamInterface child)
        {
            return sj.getChildNextInitStage(child);
        }
        
        public PhasingSchedule getChildPhase(StreamInterface child, int stage)
        {
            return sj.getChildInitStage(child, stage);
        }
        
        public void advanceSplitSchedule ()
        {
            sj.advanceSplitSchedule();
        }
        
        public void advanceJoinSchedule ()
        {
            sj.advanceJoinSchedule();
        }
        
        public SplitFlow getNextSplitSteadyPhaseFlow ()
        {
            return sj.getNextSplitSteadyPhaseFlow();
        }
        
        public SplitFlow getSplitSteadyPhaseFlow (int phase)
        {
            return sj.getSplitSteadyPhaseFlow(phase);
        }
        
        public PhasingSchedule getNextSplitSteadyPhase()
        {
            return sj.getNextSplitSteadyPhase();
        }
        
        public JoinFlow getNextJoinSteadyPhaseFlow ()
        {
            return sj.getNextJoinSteadyPhaseFlow();
        }
        
        public JoinFlow getJoinSteadyPhaseFlow (int phase)
        {
            return sj.getJoinSteadyPhaseFlow(phase);
        }
        
        public PhasingSchedule getNextJoinSteadyPhase()
        {
            return sj.getNextJoinSteadyPhase();
        }
    }
    
    private class SJSteadySchedulingUtility implements SJSchedulingUtility
    {
        SplitJoin sj;
        SJSteadySchedulingUtility (SplitJoin _sj)
        {
            sj = _sj;
        }
        
        public void addSchedulePhase(PhasingSchedule phase)
        {
            sj.addSteadySchedulePhase(phase);
        }
        
        public void advanceChildSchedule(StreamInterface child)
        {
            sj.advanceChildSteadySchedule(child);
        }
        
        public PhasingSchedule getChildNextPhase(StreamInterface child)
        {
            return sj.getChildNextSteadyPhase(child);
        }
        
        public PhasingSchedule getChildPhase(StreamInterface child, int stage)
        {
            return sj.getChildSteadyPhase(child, stage);
        }
        
        public void advanceSplitSchedule ()
        {
            sj.advanceSplitSchedule();
        }
        
        public void advanceJoinSchedule ()
        {
            sj.advanceJoinSchedule();
        }
        
        public SplitFlow getNextSplitSteadyPhaseFlow ()
        {
            return sj.getNextSplitSteadyPhaseFlow();
        }
        
        public SplitFlow getSplitSteadyPhaseFlow (int phase)
        {
            return sj.getSplitSteadyPhaseFlow(phase);
        }
        
        public PhasingSchedule getNextSplitSteadyPhase()
        {
            return sj.getNextSplitSteadyPhase();
        }
        
        public JoinFlow getNextJoinSteadyPhaseFlow ()
        {
            return sj.getNextJoinSteadyPhaseFlow();
        }
        
        public JoinFlow getJoinSteadyPhaseFlow (int phase)
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
    public void computeMinLatencySchedule(SJSchedulingUtility utility, int splitExecs, int joinExecs, int childrenExecs[], int postSplitBuffers[], int preJoinBuffers [])
    {
        // first pull all the data out of the split-join
        // that needs to go (drain the join)
        while (joinExecs > 0)
        {
            int splitterPhases = 0;
            int joinerPhases = 0;
            int phaseChildrenExecs [] = new int [getNumChildren()];
            
            int joinerOverallPop[] = new int [getNumChildren()];
            
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
                        joinerOverallPop [nChild] += joinFlow.getPopWeight(nChild);
                    }
                    
                } while (joinFlow.getPushWeight() == 0);
            }


            int childOverallPeek [] = new int [getNumChildren()];
            int childOverallPop [] = new int [getNumChildren()];
            int childOverallPush [] = new int [getNumChildren()];

            // figure out how many times to execute all the children
            // to provide input for the joiner
            {
                int nChild;
                for (nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    while (preJoinBuffers [nChild] - joinerOverallPop[nChild] + childOverallPush[nChild] < 0)
                    {
                        StreamInterface child = getHierarchicalChild(nChild);
                        PhasingSchedule childPhase = utility.getChildPhase(child, phaseChildrenExecs[nChild]);
                        
                        childOverallPeek [nChild] = MAX (childOverallPeek[nChild], childOverallPop [nChild] + childPhase.getOverallPeek());
                        childOverallPop [nChild] += childPhase.getOverallPop();
                        childOverallPush [nChild] += childPhase.getOverallPush();
                        
                        phaseChildrenExecs[nChild]++;
                    }
                }
            }
            
            int splitterOverallPush [] = new int [getNumChildren()];
            
            // figure out how many times to execute the splitter to
            // supply enough data for all the children to go
            {
                int nChild;
                for (nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    int childSplitterPhases = 0;
                    while (postSplitBuffers[nChild] + splitterOverallPush[nChild] - childOverallPeek [nChild]  < 0)
                    {
                        SplitFlow splitFlow = utility.getSplitSteadyPhaseFlow(childSplitterPhases );
                        childSplitterPhases ++;
                        
                        splitterOverallPush [nChild] += splitFlow.getPushWeight(nChild);
                    }
                    
                    splitterPhases = MAX (splitterPhases, childSplitterPhases);
                }
            }
            
            // okay, now I know how many times I need to run the
            // splitter, children and the joiner.  just run them :)
            {
                PhasingSchedule phase = new PhasingSchedule (this);
                
                // first run the splitter:
                for ( ; splitterPhases > 0; splitterPhases--)
                {
                    // get the appropriate flow
                    SplitFlow flow = utility.getNextSplitSteadyPhaseFlow();
                    
                    // update the buffers (add data)
                    int nChild;
                    for (nChild = 0; nChild < getNumChildren(); nChild++)
                    {
                        postSplitBuffers[nChild] += flow.getPushWeight(nChild);
                    }
                    
                    // add the phase to the schedule and 
                    // advance the phase counter
                    phase.appendPhase(utility.getNextSplitSteadyPhase());
                    utility.advanceSplitSchedule();
                            
                    // note that I've just executed a splitter init phase
                    splitExecs--;
                }
                
                // now run the children
                {
                    int nChild;
                    for (nChild = 0; nChild < getNumChildren(); nChild++)
                    {
                        StreamInterface child = getHierarchicalChild(nChild);
                        for ( ; phaseChildrenExecs [nChild] > 0; phaseChildrenExecs [nChild]--)
                        {
                            // get the phase and check that I've enough 
                            // peek data in the buffer to run it
                            PhasingSchedule childPhase = utility.getChildNextPhase(child);
                            ASSERT (postSplitBuffers [nChild] >= childPhase.getOverallPeek());
                            
                            // update the buffers
                            postSplitBuffers[nChild] -= childPhase.getOverallPop();
                            preJoinBuffers[nChild] += childPhase.getOverallPush();
                            
                            // add the phase to the schedule
                            // and advance the phase counter
                            phase.appendPhase(childPhase);
                            utility.advanceChildSchedule(child);
                            
                            // note that I've just executed a child 
                            // init phase
                            childrenExecs [nChild] --;
                        }
                    }
                }
                
                // and finally run the joiner
                for ( ; joinerPhases > 0; joinerPhases--)
                {
                    // get the appropriate flow
                    JoinFlow flow = utility.getNextJoinSteadyPhaseFlow();
                    
                    // update the buffers (add data)
                    int nChild;
                    for (nChild = 0; nChild < getNumChildren(); nChild++)
                    {
                        preJoinBuffers[nChild] -= flow.getPopWeight(nChild);
                        ASSERT (preJoinBuffers[nChild] >= 0);
                    }
                    
                    // add the phase to the schedule and 
                    // advance the phase counter
                    phase.appendPhase(utility.getNextJoinSteadyPhase());
                    utility.advanceJoinSchedule();
                            
                    // note that I've just executed a joiner init phase
                    joinExecs--;
                }
                
                utility.addSchedulePhase(phase);
            }
        }
        
        // now execute all the components (other than the join, which
        // is done initializing) until they're guaranteed to have been
        // initialized
        {
            PhasingSchedule phase = new PhasingSchedule (this);
            
            // first run the splitter:
            for ( ; splitExecs > 0; splitExecs--)
            {
                // get the appropriate flow
                SplitFlow flow = utility.getNextSplitSteadyPhaseFlow();
                
                // update the buffers (add data)
                int nChild;
                for (nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    postSplitBuffers[nChild] += flow.getPushWeight(nChild);
                }
                
                // add the phase to the schedule and 
                // advance the phase counter
                phase.appendPhase(utility.getNextSplitSteadyPhase());
                utility.advanceSplitSchedule();
            }
            
            // now run the children
            {
                int nChild;
                for (nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    StreamInterface child = getHierarchicalChild(nChild);
                    for ( ; childrenExecs [nChild] > 0; childrenExecs [nChild]--)
                    {
                        // get the phase and check that I've enough 
                        // peek data in the buffer to run it
                        PhasingSchedule childPhase = utility.getChildNextPhase(child);
                        ASSERT (postSplitBuffers [nChild] > childPhase.getOverallPeek());
                        
                        // update the buffers
                        postSplitBuffers[nChild] -= childPhase.getOverallPop();
                        preJoinBuffers[nChild] += childPhase.getOverallPush();
                        
                        // add the phase to the schedule
                        // and advance the phase counter
                        phase.appendPhase(childPhase);
                        utility.advanceChildSchedule(child);
                    }
                }
            }
                
            // add the phase to the init schedule, if necessary
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
                                child.getInitPush() - child.getSteadyPop());

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
                    numInitSplitStages = MAX (numInitSplitStages, childInitSplitStages);
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
                    numInitSplitStages = MAX (numInitSplitStages, childInitJoinStages);
                }
            }
        }

        // store amount of data in buffers internal to the splitjoin        
        int postSplitBuffers[] = new int[getNumChildren()];
        int preJoinBuffers[] = new int[getNumChildren()];
        
        // and now in two lines do the whole scheduling bit!
        
        // first the init schedule:
        computeMinLatencySchedule(new SJInitSchedulingUtility(this), numInitSplitStages, numInitJoinStages, numInitChildStages, postSplitBuffers, preJoinBuffers);
        
        // and now the steady schedule
        computeMinLatencySchedule(new SJSteadySchedulingUtility(this), steadySplitPhases, steadyJoinPhases, steadyChildPhases, postSplitBuffers, preJoinBuffers);
        
        // done!
        // (ain't that amazing?)
    }
}
