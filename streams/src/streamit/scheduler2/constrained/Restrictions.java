package streamit.scheduler2.constrained;

import streamit.misc.Pair;
import streamit.misc.DLList;
import streamit.misc.DLListIterator;
import streamit.misc.OMap;
import streamit.misc.OMapIterator;
import streamit.misc.OSet;
import streamit.misc.OSetIterator;

/**
 * streamit.scheduler2.constrained.Restirctions implements the 
 * restrictions framework which is used to make sure that appropriate
 * constraints are honored.
 */

public class Restrictions extends streamit.misc.Misc
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
                    && left.getNode().hashCode() < right.getNode().hashCode());
        }
    }

    /**
     * restrictions holds all restrictions placed upon all the nodes
     * in the system. restrictions is a map. The keys in the map are
     * LatencyNodes. The entries are sets of type 
     * Restriction, sorted accroding to how many executions of the node
     * they still allow.
     */
    final OMap restrictions;
    final OMapIterator lastRestrictionsIter;

    /**
     * allRestrictions holds all restrictions in a single, easy to
     * access and search data container.
     */
    final OSet allRestrictions =
        new OSet(new RestrictionPrtlFltrComperator());

    /**
     * nodeExecutions is a map of LatencyNode to Integer. Basically
     * it stores how many times each node has been executed so far
     */
    final OMap nodeExecutions;
    final OMapIterator lastNodeExecutionIter;

    public Restrictions()
    {
        nodeExecutions = new OMap();
        lastNodeExecutionIter = nodeExecutions.end();

        restrictions = new OMap();
        lastRestrictionsIter = restrictions.end();
    }

    /**
     * Try to execute a node nTimes times. The function will return
     * the number of times the node will be executed, as allowed by
     * node's restrictions. If the node is currently blocked, this
     * function will return 0.
     */
    public int execute(LatencyNode node, int nTimes)
    {
        Restriction strongestRestr = getStrongestRestriction(node);
        int allowedExecs;

        if (strongestRestr == null)
        {
            allowedExecs = nTimes;
        }
        else
        {
            allowedExecs =
                MIN(nTimes, strongestRestr.getNumAllowedExecutions());
        }

        if (allowedExecs > 0)
        {
            // increase the # of times the node has been executed
            {
                OMapIterator nodeIter = nodeExecutions.find(node);
                if (nodeIter.equals(lastNodeExecutionIter))
                {
                    // node has not been executed yet!
                    nodeExecutions.insert(node, new Integer(allowedExecs));
                }
                else
                {
                    // node has been executed before - have to retrieve the
                    // current num execs and add allowedExecs
                    int execsSoFar =
                        ((Integer)nodeIter.getData()).intValue();
                    nodeIter.setData(
                        new Integer(execsSoFar + allowedExecs));
                }
            }

            // and notify restrictions that they're now blocking
            // execution
            {
                DLList toBeRemoved = new DLList();
                OMapIterator nodeRestrictionsIter = restrictions.find(node);

                // only have to do this if there some restrictions!
                if (!nodeRestrictionsIter.equals(lastRestrictionsIter))
                {
                    OSet nodeRestrictions =
                        (OSet)nodeRestrictionsIter.getData();
                    OSetIterator lastNodeRestrictionIter =
                        nodeRestrictions.end();
                    for (OSetIterator nodeRestrictionIter =
                        nodeRestrictions.begin();
                        !nodeRestrictionIter.equals(
                            lastNodeRestrictionIter);
                        nodeRestrictionIter.next())
                    {
                        Restriction nodeRestriction =
                            (Restriction)nodeRestrictionIter.get();
                        if (nodeRestriction.getNumAllowedExecutions() == 0)
                        {
                            // notify the restriction
                            boolean removeRestriction =
                                nodeRestriction.notifyExpired();
                            if (removeRestriction)
                            {
                                // store the restriction to be
                                // removed below!
                                toBeRemoved.pushBack(nodeRestriction);
                            }
                        }
                        else
                        {
                            // this restriction isn't blocking, so
                            // all remaining restrictions are not blocking
                            // either
                            break;
                        }
                    }
                }

                // remove any restrictions which need removing
                while (!toBeRemoved.empty())
                {
                    Restriction gonner =
                        (Restriction)toBeRemoved.front().get();
                    remove(gonner);
                    toBeRemoved.popFront();
                }
            }
        }

        return allowedExecs;
    }

    /**
     * Find out how many times a given node has executed so far. This
     * function will return the number of times the node has been
     * scheduled for execution before the function has been called. The
     * number of scheduled executions is the sum of values returned from
     * function execute() above. 
     * 
     * If execute() has not been called with this node as a parameter, 
     * this function should return 0, which is logical :)  
     */
    public int getNumExecutions(LatencyNode node)
    {
        OMapIterator nodeIter = nodeExecutions.find(node);
        if (nodeIter.equals(lastNodeExecutionIter))
            return 0;
        return ((Integer)nodeIter.getData()).intValue();
    }

    /**
     * Return the currently blocking restriction for the node. If the
     * node is currently not restricted in execution (can execute at
     * least once), return null.
     */
    public Restriction getBlockingRestriction(LatencyNode node)
    {
        Restriction strongestRestriction = getStrongestRestriction(node);

        if (strongestRestriction != null
            && strongestRestriction.getNumAllowedExecutions() == 0)
            return strongestRestriction;
        else
            return null;
    }

    /**
     * Return the strongest restriction for the node. If the node
     * currently has no restrictions, return null.
     */
    public Restriction getStrongestRestriction(LatencyNode node)
    {
        OMapIterator restrictionsIter = restrictions.find(node);
        if (restrictionsIter.equals(lastRestrictionsIter))
        {
            return null;
        }

        OSet nodeRestrictions = (OSet)restrictionsIter.getData();
        OSetIterator restrictionIter = nodeRestrictions.begin();
        if (restrictionIter.equals(nodeRestrictions.end()))
            return null;

        Restriction strongestRestriction =
            (Restriction)restrictionIter.get();
        return strongestRestriction;
    }
    
    /**
     * Return an OSet of all restrictions for a specific node. Restrictions
     * are sorted by how constraining they are first, and semi-randomly
     * second (by hashCode, which varries from execution to execution).
     * Return null if this node doesn't have any restrictions. 
     */
    public OSet getAllRestrictions (LatencyNode node)
    {
        OMapIterator restrictionsIter = restrictions.find(node);
        if (restrictionsIter.equals(lastRestrictionsIter))
        {
            return null;
        }

        OSet nodeRestrictions = (OSet)restrictionsIter.getData();
        if (nodeRestrictions.size() == 0) return null;
        
        return nodeRestrictions;
    }

    /**
     * Add a restriction to the existing set of restrictions. Allow only
     * one restriction per portal per node. Return the old restriction
     * if one existed for the particular portal/node pair, null 
     * otherwise.
     */
    public Restriction add(Restriction restriction)
    {
        restriction.useRestrictions(this);
        LatencyNode restrictedNode = restriction.getNode();

        OMapIterator nodeRestrictionsIter =
            restrictions.find(restrictedNode);

        // first add the restriction
        {
            // has this node had its restrictions initialized yet?
            if (nodeRestrictionsIter.equals(restrictions.end()))
            {
                // nope - make sure that the node has restrictions
                // initialized before adding :)
                Pair result =
                    restrictions.insert(
                        restrictedNode,
                        new OSet(new RestrictionExecutionsComperator()));

                // better not have anything in there - that would be 
                // a bug in the container or something
                ASSERT(((Boolean)result.getSecond()).booleanValue());
                nodeRestrictionsIter = (OMapIterator)result.getFirst();
            }

            OSet nodeRestrictions = (OSet)nodeRestrictionsIter.getData();
            nodeRestrictions.insert(restriction);
        }

        // now remove any old restrictions
        Restriction oldRestriction = null;
        {
            Pair result = allRestrictions.insert(restriction);

            if (((Boolean)result.getSecond()).booleanValue() == false)
            {
                // there already is a restriction for this portal/node pair!
                // remove it from allRestrictions first
                OSetIterator oldRestrictionIter =
                    (OSetIterator)result.getFirst();
                oldRestriction = (Restriction)oldRestrictionIter.get();
                allRestrictions.erase(oldRestrictionIter);

                // insert the restriction into the allRestrictions set
                allRestrictions.insert(restriction);

                // and now remove it from from restrictions
                OSet nodeRestrictions =
                    (OSet)nodeRestrictionsIter.getData();
                nodeRestrictions.erase(oldRestriction);
            }
        }

        // finally, if the restriction is now blocking, notify it
        // of its status
        if (restriction.getNumAllowedExecutions() == 0)
        {
            boolean removeRestriction = restriction.notifyExpired();
            if (removeRestriction)
                remove(restriction);
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
            OMapIterator nodeRestrictionsIter =
                restrictions.find(restriction.getNode());
            ASSERT(!nodeRestrictionsIter.equals(restrictions.end()));
            OSet nodeRestrictions = (OSet)nodeRestrictionsIter.getData();
            nodeRestrictions.erase(restriction);
        }
    }
}
