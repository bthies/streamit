package streamit.scheduler;

import java.util.*;
import streamit.*;

class SchedSplitType implements Comparable
{
    static final int WEIGHTED_ROUND_ROBIN = 0;
    static final int DUPLICATE = 1;
    static final int LAST = 2;

    final int value;

    SchedSplitType (int value)
    {
        this.value = value;
    }

    public int compareTo (Object o)
    {
        SchedSplitType other = (SchedSplitType) o;
        if (other == null)
        {
            throw new ClassCastException ("Couldn't compare SchedSplitType to " + o.getClass().getName());
        }

        return value - other.value;
    }
}

class SchedJoinType
{
    static final int WEIGHTED_ROUND_ROBIN = 0;
    static final int LAST = 1;

    final int value;

    SchedJoinType (int value)
    {
        this.value = value;
    }

    public int compareTo (Object o)
    {
        SchedJoinType other = (SchedJoinType) o;
        if (other == null)
        {
            throw new ClassCastException ("Couldn't compare SchedJoinType to " + o.getClass().getName());
        }

        return value - other.value;
    }
}

public class Scheduler extends DestroyedClass
{
    public static class SchedStream
    {
        int consumes, produces, peeks;
        SchedStream parent;

        final List allChildren = new LinkedList ();

        public void addStream (SchedStream stream)
        {
            boolean result = allChildren.add (stream);
            ASSERT (result);
        }
    }

    public static class SchedFilter extends SchedStream
    {
        Operator operator;

        final List srcMsgs = new LinkedList ();
        final List dstMsgs = new LinkedList ();
    }

    public static class SchedSplitJoin extends SchedStream
    {
        SchedSplitType splitType;
        SchedJoinType joinType;
        final List allChildren = new LinkedList ();
    }

    public static class SchedLoop extends SchedStream
    {
        SchedStream body, loop;
    }

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
    }
}

