package streamit.scheduler2.constrained;

import streamit.misc.Pair;
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
                    && left.getStream().hashCode()
                        < right.getStream().hashCode());
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
     * allRestrictions holds all restrictions in a single, easy to
     * access and search data container.
     */
    final OSet allRestrictions =
        new OSet(new RestrictionPrtlFltrComperator());
        
    /**
     * streamExecutions is a map of StreamInterface to Integer. Basically
     * it stores how many times each stream has been executed so far
     */
    final OMap streamExecutions;
    final OMapIterator lastStreamExecutionIter;
    
    public Restrictions ()
    {
        streamExecutions = new OMap ();
        lastStreamExecutionIter = streamExecutions.end();
    }
    
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
        OMapIterator streamIter = streamExecutions.find(stream);
        if (streamIter.equals (lastStreamExecutionIter)) return 0;
        return ((Integer)streamIter.getData()).intValue() ;
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
        StreamInterface restrictedStream = restriction.getStream();

        OMapIterator streamRestrictionsIter =
            restrictions.find(restrictedStream);

        // first add the restriction
        {
            // has this stream had its restrictions initialized yet?
            if (streamRestrictionsIter.equals(restrictions.end()))
            {
                // nope - make sure that the stream has restrictions
                // initialized before adding :)
                Pair result =
                    restrictions.insert(
                        restrictedStream,
                        new OSet(new RestrictionExecutionsComperator()));

                // better not have anything in there - that would be 
                // a bug in the container or something
                ASSERT(((Boolean)result.getSecond()).booleanValue());
                streamRestrictionsIter = (OMapIterator)result.getFirst();
            }

            OSet streamRestrictions =
                (OSet)streamRestrictionsIter.getData();
            streamRestrictions.insert(restriction);
        }

        // now remove any old restrictions
        Restriction oldRestriction = null;
        {
            Pair result = allRestrictions.insert(restriction);

            if (((Boolean)result.getSecond()).booleanValue() == false)
            {
                // there already is a restriction for this portal/stream pair!
                // remove it from allRestrictions first
                OSetIterator oldRestrictionIter =
                    (OSetIterator)result.getFirst();
                oldRestriction = (Restriction)oldRestrictionIter.get();
                allRestrictions.erase(oldRestrictionIter);

                // insert the restriction into the allRestrictions set
                allRestrictions.insert(restriction);

                // and now remove it from from restrictions
                OSet streamRestrictions =
                    (OSet)streamRestrictionsIter.getData();
                streamRestrictions.erase(oldRestriction);
            }
        }

        return oldRestriction;
    }

    /**
     * Remove a restriction to the existing set of restrictions. If
     * restriction is not in the set of restrictions, silently don't
     * do anything.
     */
    public void remove(Restriction restriction)
    {
        // check if the restriction is still in restrictions set
        OSetIterator restrictionIter = allRestrictions.find(restriction);
        if (!restrictionIter.equals(allRestrictions.end())
            && restrictionIter.get() == restriction)
        {
            // yep, it's still there; remove it!
            allRestrictions.erase(restriction);
            OMapIterator streamRestrictionsIter =
                restrictions.find(restriction.getStream());
            ASSERT(!streamRestrictionsIter.equals(restrictions.end()));
            OSet streamRestrictions =
                (OSet)streamRestrictionsIter.getData();
            streamRestrictions.erase(restriction);
        }
    }
}
