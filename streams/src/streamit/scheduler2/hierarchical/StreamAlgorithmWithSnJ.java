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

package streamit.scheduler2.hierarchical;

import streamit.scheduler2.base.StreamInterfaceWithSnJ.SplitFlow;
import streamit.scheduler2.base.StreamInterfaceWithSnJ.JoinFlow;

/**
 * This class provides an implementation for StreamInterface.
 * 
 * I have to make this a separate class instead of a class derrived
 * from Stream and one that Filter will derrive from because
 * Java doesn't have multiple inheritance.  Argh!
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class StreamAlgorithmWithSnJ extends StreamAlgorithm
{
    final private StreamInterfaceWithSnJ stream;

    public StreamAlgorithmWithSnJ(StreamInterfaceWithSnJ _stream)
    {
        super(_stream);

        stream = _stream;
    }

    private int nSplitPhase = 0;
    private int nJoinPhase = 0;

    /**
     * Advance the split's schedule by numPhases.
     */
    public void advanceSplitSchedule(int numPhases)
    {
        ASSERT(numPhases > 0);
        nSplitPhase += numPhases;
    }

    /**
     * Advance the join's schedule by numPhases.
     */
    public void advanceJoinSchedule(int numPhases)
    {
        ASSERT(numPhases > 0);
        nJoinPhase += numPhases;
    }
    
    /**
     * Get a phase  for the stream's Splitter.  This phase is computed relative
     * to how much of the split's schedule has already been consumed.
     * @return phase of the stream's splitter
     */
    public PhasingSchedule getSplitSteadyPhase(int nPhase)
    {
        ASSERT(nPhase >= 0);
        int phase = (nSplitPhase + nPhase) % stream.getNumSplitPhases();
        return stream.getSplitPhase(phase);
    }

    /**
     * Get a phase  for the stream's Joiner.  This phase is computed relative
     * to how much of the joiner's schedule has already been consumed.
     * @return phase of the stream's joiner
     */
    public PhasingSchedule getJoinSteadyPhase(int nPhase)
    {
        ASSERT(nPhase >= 0);
        int phase = (nJoinPhase + nPhase) % stream.getNumJoinPhases();
        return stream.getJoinPhase(phase);
    }

    /**
     * Get a number of phases of either splitter or joiner.
     * This function will use duplicatePhase function to
     * compress the phases so this construction will be fairly
     * quick and resulting schedule fairly efficient. 
     */
    private PhasingSchedule getSJPhases(int nPhases, boolean isSplitter)
    {
        PhasingSchedule phase = new PhasingSchedule (stream);
        
        int numSteadyPhases = (isSplitter ? stream.getNumSplitPhases() : stream.getNumJoinPhases());
        
        // if the splitter or joiner is NULL, it doesn't have
        // steady phases - don't even try to create a phase for it!
        if (numSteadyPhases == 0) 
        {
            ASSERT (nPhases == 0);
            return phase;
        } 
        
        // round up to a steady-state:
        // if there aren't enough executions here to make it worth
        // to do the smart compression, just append the lot
        // of phases to the phase.
        {
            int mod = nPhases % numSteadyPhases;
            
            if (nPhases / numSteadyPhases < 4) mod = nPhases;
            
            nPhases -= mod;
            
            for(;mod > 0; mod--)
            {
                if (isSplitter){
                    phase.appendPhase(getSplitSteadyPhase(0));
                    advanceSplitSchedule(1);
                } else {
                    phase.appendPhase(getJoinSteadyPhase(0));
                    advanceJoinSchedule(1);
                }
            }
        }
        
        // if I'm not actually adding more phases, quit early
        if (nPhases == 0) return phase;
                
        // create a steady-state phase:
        PhasingSchedule steadyState = new PhasingSchedule(stream);
        for (int nPhase = 0; nPhase < numSteadyPhases;nPhase++)
        {
            if (isSplitter)
            {
                steadyState.appendPhase(getSplitSteadyPhase(nPhase));
            } else{
                steadyState.appendPhase(getJoinSteadyPhase(nPhase));
            }
        }
        
        phase.appendPhase(duplicatePhase(steadyState, nPhases/numSteadyPhases));

        if(isSplitter)
        {
            advanceSplitSchedule(nPhases);
        } else {
            advanceJoinSchedule(nPhases);
        }
        
        return phase;
    }
    
    /**
     * Create a schedule consisting of nPhases of the splitter.
     */
    public PhasingSchedule getSplitterPhases(int nPhases)
    {
        return getSJPhases(nPhases, true);
    }

    /**
     * Create a schedule consisting of nPhases of the joiner.
     */
    public PhasingSchedule getJoinerPhases(int nPhases)
    {
        return getSJPhases(nPhases, false);
    }

    /**
     * Get the appropriate phase flow for the split of this SplitJoin.
     * @return phase flow of the split
     */
    public SplitFlow getSplitSteadyPhaseFlow (int nPhase)
    {
        ASSERT (nPhase >=0);
        int phase = (nSplitPhase + nPhase) % stream.getNumSplitPhases();
        return getSplitFlow(phase);
    }

    /**
     * Get the appropriate phase flow for the join of this SplitJoin.
     * @return phase flow of the join
     */
    public JoinFlow getJoinSteadyPhaseFlow (int nPhase)
    {
        ASSERT (nPhase >=0);
        int phase = (nJoinPhase + nPhase) % stream.getNumJoinPhases();
        return getJoinFlow(phase);
    }
    
    /**
     * Get flow of data through the stream's splitter.  This phase is computed
     * relative to how much of the split's schedule has already been consumed.
     * @return flow of data through the stream's splitter
     */
    public SplitFlow getSplitFlow(int nPhase)
    {
        return stream.getSplitFlow(nPhase);
    }

    /**
     * Get flow of data through the stream's joiner.  This phase is computed
     * relative to how much of the join's schedule has already been consumed.
     * @return flow of data through the stream's joiner
     */
    public JoinFlow getJoinFlow(int nPhase)
    {
        return stream.getJoinFlow(nPhase);
    }
}
