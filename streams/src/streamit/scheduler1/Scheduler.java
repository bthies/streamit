package streamit.scheduler;

import java.util.*;
import streamit.*;

public class Scheduler extends DestroyedClass
{
    class SchedBufferRestriction
    {
        SchedFilter filter1, filter2;
        int size;
    }

    class SchedBufRestrictionMax extends SchedBufferRestriction { }
    class SchedBufRestrictionMin extends SchedBufferRestriction { }

    public final SchedStream stream;
    final Set bindedMsgs = new HashSet ();

    public Scheduler (SchedStream stream)
    {
        this.stream = stream;
        this.stream.computeSchedule ();
    }
}

