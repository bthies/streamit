package streamit.scheduler.hierarchical;

/* $Id: StreamAlgorithmWithSnJ.java,v 1.1 2002-07-16 21:41:24 karczma Exp $ */

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
     * Get a phase  for the child's Splitter.  This phase is computed relative
     * to how much of the split's schedule has already been consumed.
     * @return phase of the child's splitter
     */
    public PhasingSchedule getSplitSteadyPhase(int nPhase)
    {
        ASSERT(nPhase >= 0);
        int phase = (nSplitPhase + nPhase) % stream.getNumSplitPhases();
        return stream.getSplitPhase(phase);
    }

    /**
     * Get a phase  for the child's Joiner.  This phase is computed relative
     * to how much of the joiner's schedule has already been consumed.
     * @return phase of the child's joiner
     */
    public PhasingSchedule getJoinSteadyPhase(int nPhase)
    {
        ASSERT(nPhase >= 0);
        int phase = (nJoinPhase + nPhase) % stream.getNumJoinPhases();
        return stream.getJoinPhase(phase);
    }
}
