package streamit.scheduler2.hierarchical;

import streamit.scheduler2.base.StreamInterfaceWithSnJ.SplitFlow;
import streamit.scheduler2.base.StreamInterfaceWithSnJ.JoinFlow;

/* $Id: StreamAlgorithmWithSnJ.java,v 1.3 2002-12-02 23:54:09 karczma Exp $ */

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
