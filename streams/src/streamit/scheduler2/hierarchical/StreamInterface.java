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

/**
 * This interface provides the required functional interface for
 * all hierarchical scheduling objects.  Hierarchical scheduling
 * objects have the property that the schedule they produce is 
 * entirely contained by the object.  A hierarchical scheduling
 * object provides information about the particular phasing
 * schedule, such as number of data input/output by each phase, etc.
 * 
 * I have to make this an interface instead of a class because
 * Java doesn't have multiple inheritance.  Argh!
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface StreamInterface
    extends streamit.scheduler2.base.StreamInterface
{
    /**
     * Returns the object that has ability to consume/peek data
     * from the top of the stream.  For Pipeline this would be
     * the top stream in the pipeline.  For all other objects, this
     * would be self (splits and joins are considered parts of the
     * stream objects).
     */
    public streamit.scheduler2.base.StreamInterface getTop();

    /**
     * Returns the object that has ability to consume/peek data
     * from the top of the stream.  For Pipeline this would be
     * the bottom stream in the pipeline.  For all other objects, this
     * would be self (splits and joins are considered parts of the
     * stream objects).
     */
    public streamit.scheduler2.base.StreamInterface getBottom();

    /**
     * Return the number of phases that this StreamInterface's steady 
     * schedule has.
     * @return number of phases in this StreamInterface's schedule
     */
    public int getNumSteadyPhases();

    /**
     * Return the number of data that a particular phase of this
     * StreamInterface's steady schedule peeks.
     * @return amount by which a phase of this StreamInterface peeks
     */
    public int getSteadyPhaseNumPeek(int phase);

    /**
     * Return the number of data that a particular phase of this
     * StreamInterface's steady schedule pops.
     * @return amount by which a phase of this StreamInterface pops
     */
    public int getSteadyPhaseNumPop(int phase);

    /**
     * Return the number of data that a particular phase of this
     * StreamInterface's steady schedule pushes.
     * @return amount by which a phase of this StreamInterface push
     */
    public int getSteadyPhaseNumPush(int phase);

    /**
     * Return the phasing steady state schedule associated with this object.
     * @return phasing stready state schedule
     */
    public PhasingSchedule getPhasingSteadySchedule();

    /**
     * Return a phase of the steady state schedule.
     * @return a phase of the steady state schedule
     */
    public PhasingSchedule getSteadySchedulePhase(int phase);

    /**
     * Add a phase to the steady schedule
     */
    public void addSteadySchedulePhase(PhasingSchedule newPhase);

    /**
     * Return the number of stages that this StreamInterface's 
     * initialization schedule has.
     * @return number of stages in this StreamInterface's 
     * initialization schedule
     */
    public int getNumInitStages();

    /**
     * Return the number of data that a particular stage of this
     * StreamInterface's initialization schedule peeks.
     * @return amount by which a stage of this StreamInterface'
     * initialization schedule peeks
     */
    public int getInitStageNumPeek(int stage);

    /**
     * Return the number of data that a particular stage of this
     * StreamInterface's initialization schedule pops.
     * @return amount by which a stage of this StreamInterface's
     * initialization schedule pops
     */
    public int getInitStageNumPop(int stage);

    /**
     * Return the number of data that a particular stage of this
     * StreamInterface's initialization schedule pushes.
     * @return amount of data which a stage of this StreamInterface's
     * initialization schedule pushes
     */
    public int getInitStageNumPush(int stage);

    /**
     * Return the phasing initialization schedule associated with this 
     * object.
     * @return phasing intitialization schedule
     */
    public PhasingSchedule getPhasingInitSchedule();

    /**
     * Return a stage of the initialization schedule.
     * @return a stage of the initialization schedule
     */
    public PhasingSchedule getInitScheduleStage(int stage);
    
    /**
     * Thesis hack
     */
    //int getScheduleSize (PhasingSchedule sched);
}
