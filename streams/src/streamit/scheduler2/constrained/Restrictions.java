package streamit.scheduler2.constrained;

import streamit.misc.OMap;
import streamit.misc.OMapIterator;
import streamit.misc.OSet;
import streamit.misc.OSetIterator;

/**
 * streamit.scheduler2.constrained.Restirctions implements the 
 * restrictions framework which is used to make sure that appropriate
 * constraints are honored.
 */

public class Restrictions extends streamit.misc.AssertedClass
{
    class RestrictionExecutionsComperator
        implements streamit.misc.Comperator
    {
        public boolean isLess(Object leftObj, Object rightObj)
        {
            ASSERT(leftObj != null && rightObj != null);
            ASSERT(
                leftObj instanceof Restriction
                    && rightObj instanceof Restriction);

            Restriction left = (Restriction)leftObj;
            Restriction right = (Restriction)rightObj;

            int leftExecutions = left.getNumAllowedExecutions();
            int rightExecutions = right.getNumAllowedExecutions();

            return (leftExecutions < rightExecutions)
                || (leftExecutions == rightExecutions
                    && left.getPortal().hashCode()
                        < right.getPortal().hashCode());
        }
    }

    class RestrictionPrtlFltrComperator implements streamit.misc.Comperator
    {
        public boolean isLess(Object leftObj, Object rightObj)
        {
            ASSERT(leftObj != null && rightObj != null);
            ASSERT(
                leftObj instanceof Restriction
                    && rightObj instanceof Restriction);

            Restriction left = (Restriction)leftObj;
            Restriction right = (Restriction)rightObj;

            int leftPrtl = left.getPortal().hashCode();
            int rightPrtl = right.getPortal().hashCode();

            return (leftPrtl < rightPrtl)
                || (leftPrtl == rightPrtl
                    && left.getFilter().hashCode()
                        < right.getFilter().hashCode());
        }
    }

    /**
     * restrictions holds all restrictions placed upon all the streams
     * in the system. restrictions is a map. The keys in the map are
     * constrained StreamInterfaces. The entries are sets of type 
     * Restriction, sorted accroding to how many executions of the stream
     * they still allow.
     */
    final OMap restrictions = new OMap();

    /**
     * Try to execute a stream nTimes times. The function will return
     * the number of times the stream will be executed, as allowed by
     * stream's restrictions. If the stream is currently blocked, this
     * function will return 0.
     */
    public int execute(StreamInterface stream, int nTimes)
    {
        ERROR("not implemented");
        return -1;
    }

    /**
     * Find out how many times a given stream has executed so far. This
     * function will return the number of times the stream has been
     * scheduled for execution before the function has been called. The
     * number of scheduled executions is the sum of values returned from
     * function execute() above. 
     * 
     * If execute() has not been called with this stream as a parameter, 
     * this function should return 0, which is logical :)  
     */
    public int getNumExecutions(StreamInterface stream)
    {
        ERROR("not implemented");
        return -1;
    }

    /**
     * Return the currently blocking restriction for the stream. If the
     * stream is currently not restricted in execution (can execute at
     * least once), return null.
     */
    public Restriction getBlockingRestriction(StreamInterface stream)
    {
        ERROR("not implemented");
        return null;
    }

    /**
     * Return the strongest restriction for the stream. If the stream
     * currently has no restrictions, return null.
     */
    public Restriction getStrongestRestriction(StreamInterface stream)
    {
        ERROR("not implemented");
        return null;
    }

    /**
     * Add a restriction to the existing set of restrictions. Allow only
     * one restriction per portal per filter. Return the old restriction
     * if one existed for the particular portal/filter pair, null 
     * otherwise.
     */
    public Restriction add(Restriction restriction)
    {
        ERROR("Not implemented");
        return null;
    }

    /**
     * Remove a restriction to the existing set of restrictions. If
     * restriction is not in the set of restrictions, silently don't
     * don anything.
     */
    public void remove(Restriction restriction)
    {
        ERROR("Not implemented");
    }
}
