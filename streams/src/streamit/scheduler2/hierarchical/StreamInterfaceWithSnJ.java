package streamit.scheduler2.hierarchical;

/**
 * This interface provides the required functional interface for
 * all hierarchical scheduling objects with splits and joins.
 * This is an extension of just regular hierarchical scheduling
 * (as you can see from the extends statement :)
 * Basically, this takes care of getting information about
 * the split and the join in the stream
 * 
 * I have to make this an interface instead of a class because
 * Java doesn't have multiple inheritance.  Argh!
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface StreamInterfaceWithSnJ
    extends StreamInterface, streamit.scheduler2.base.StreamInterfaceWithSnJ
{
    /**
     * Get the number of phases that the split of this SplitJoin has.
     * @return number of split's phases
     */
    public int getNumSplitPhases();

    /**
     * Get the appropriate phase for the split of this SplitJoin.
     * @return phase of the split
     */
    public PhasingSchedule getSplitPhase(int nPhase);

    /**
     * Get the number of phases that the join of this SplitJoin has.
     * @return number of split's join
     */
    public int getNumJoinPhases();

    /**
     * Get the appropriate phase for the join of this SplitJoin.
     * @return phase of the join
     */
    public PhasingSchedule getJoinPhase(int nPhase);

    /**
     * Create a schedule consisting of nPhases of the splitter.
     */
    public PhasingSchedule getSplitterPhases(int nPhases);
    
    /**
     * Create a schedule consisting of nPhases of the joiner.
     */
    public PhasingSchedule getJoinerPhases(int nPhases);
    
    /**
     * Get the appropriate phase flow for the split of this SplitJoin.
     * @return phase flow of the split
     */
    public SplitFlow getSplitSteadyPhaseFlow (int nPhase);

    /**
     * Get the appropriate phase flow for the join of this SplitJoin.
     * @return phase flow of the join
     */
    public JoinFlow getJoinSteadyPhaseFlow (int nPhase);
}
