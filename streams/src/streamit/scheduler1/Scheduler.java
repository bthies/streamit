package streamit.scheduler;

import java.util.*;
import streamit.*;

public abstract class Scheduler extends DestroyedClass
{
    class SchedBufferRestriction
    {
        SchedFilter filter1, filter2;
        int size;
    }

    class SchedBufRestrictionMax extends SchedBufferRestriction { }
    class SchedBufRestrictionMin extends SchedBufferRestriction { }

    public SchedStream stream;
    final Set bindedMsgs = new HashSet ();

    /**
     * Specify the stream to use with this scheduler.
     * This function can only be invoked once (it asserts otherwise)
     */
    public void useStream (SchedStream stream)
    {
        ASSERT (this.stream == null && stream != null);
        this.stream = stream;
        this.stream.computeSteadySchedule ();
    }

    /**
     * This function computes some schedule.
     * Every child class must implement its own scheduler
     * and use it to compute the schedule.
     */
    public abstract Schedule computeSchedule ();

    /**
     * Construct a new filter object
     */
    public SchedFilter newSchedFilter (Object stream, int push, int pop, int peek)
    {
        return new SchedFilter (stream, push, pop, peek);
    }

    /**
     * Construct a new pipeline object
     */
    public SchedPipeline newSchedPipeline (Object stream)
    {
        return new SchedPipeline (stream);
    }

    /**
     * Construct a new feedback loop object
     */
    public SchedLoop newSchedLoop (Object stream, SchedJoinType join, SchedStream body, SchedSplitType split, SchedStream loop, int delay)
    {
        return new SchedLoop (stream, join, body, split, loop, delay);
    }

    /**
     * Construct a new splitjoin object
     */
    public SchedSplitJoin newSchedSplitJoin (Object stream)
    {
        return new SchedSplitJoin (stream);
    }

    /**
     * Construct a new join type object
     */
    public SchedJoinType newSchedJoinType (int type, List joinWeights, Object joinObject)
    {
        return new SchedJoinType (type, joinWeights, joinObject);
    }

    /**
     * Construct a new split type object
     */
    public SchedSplitType newSchedSplitType (int type, List splitWeights, Object splitObject)
    {
        return new SchedSplitType (type, splitWeights, splitObject);
    }
}

