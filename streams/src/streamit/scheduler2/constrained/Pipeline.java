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

package streamit.scheduler2.constrained;

import streamit.scheduler2.iriter./*persistent.*/
PipelineIter;
import streamit.scheduler2.iriter./*persistent.*/
Iterator;

import streamit.misc.Pair;
import streamit.misc.DLList;
import streamit.misc.OMap;
import streamit.misc.OMapIterator;

import streamit.scheduler2.hierarchical.PhasingSchedule;
import streamit.scheduler2.SDEPData;

/**
 * streamit.scheduler2.constrained.Pipeline is the pipeline constrained 
 * scheduler. It assumes that all streams in the program use the constrained
 * scheduler
 */

public class Pipeline
    extends streamit.scheduler2.hierarchical.Pipeline
    implements StreamInterface
{
    final private LatencyGraph latencyGraph;
    final private streamit.scheduler2.constrained.StreamFactory factory;

    public Pipeline(
        PipelineIter iterator,
        Iterator parent,
        streamit.scheduler2.constrained.StreamFactory _factory)
    {
        super(iterator, _factory);

        factory = _factory;
        latencyGraph = factory.getLatencyGraph();

        if (parent == null)
        {
            latencyGraph.registerParent(this, null);
            initiateConstrained();
        }
    }

    LatencyEdge connectingLatencyEdges[];

    public void initiateConstrained()
    {
        // register all children
        for (int nChild = 0; nChild < getNumChildren(); nChild++)
        {
            StreamInterface child = getConstrainedChild(nChild);
            latencyGraph.registerParent(child, this);
            child.initiateConstrained();
        }

        connectingLatencyEdges = new LatencyEdge[getNumChildren() - 1];

        // add all children to the latency graph
        for (int nChild = 0; nChild + 1 < getNumChildren(); nChild++)
        {
            StreamInterface topStream = getConstrainedChild(nChild);
            StreamInterface bottomStream = getConstrainedChild(nChild + 1);

            LatencyNode topNode = topStream.getBottomLatencyNode();
            LatencyNode bottomNode = bottomStream.getTopLatencyNode();

            LatencyEdge edge =
                new LatencyEdge(topNode, 0, bottomNode, 0, 0);

            // add self to the two nodes
            topNode.addDependency(edge);
            bottomNode.addDependency(edge);

            connectingLatencyEdges[nChild] = edge;
        }
    }

    StreamInterface getConstrainedChild(int nChild)
    {
        streamit.scheduler2.base.StreamInterface child;
        child = getChild(nChild);

        if (!(child instanceof StreamInterface))
        {
            ERROR("This pipeline contains a child that is not CONSTRAINED");
        }

        return (StreamInterface)child;
    }

    public LatencyNode getBottomLatencyNode()
    {
        return getConstrainedChild(getNumChildren() - 1)
            .getBottomLatencyNode();
    }

    public LatencyNode getTopLatencyNode()
    {
        return getConstrainedChild(0).getTopLatencyNode();
    }

    public StreamInterface getTopConstrainedStream()
    {
        return getConstrainedChild(0).getTopConstrainedStream();
    }

    public StreamInterface getBottomConstrainedStream()
    {
        return getConstrainedChild(getNumChildren() - 1)
            .getBottomConstrainedStream();
    }

    OMap portal2sdep = new OMap();
    OMap portal2restrictionPair = new OMap();

    public void registerConstraint(P2PPortal portal)
    {
        int minLatency = portal.getMinLatency();
        int maxLatency = portal.getMaxLatency();

        // also get the SDEP function for this portal
        SDEPData sdep = null;
        try
        {
            sdep =
                latencyGraph.computeSDEP(
                    portal.getUpstreamNode(),
                    portal.getDownstreamNode());
        }
        catch (NoPathException exception)
        {
            ERROR(
                "Need better message: No path from "
                    + portal.getUpstreamNode().toString()
                    + " to "
                    + portal.getUpstreamNode().toString()
                    + "!");
        }

        Pair oldSDEP = portal2sdep.insert(portal, sdep);

    }

    Restrictions restrictions;

    // keep track of how many restrictions I am still managing
    int numInitialRestrictions = 0;

    // also keep track of which of my children are still managing
    // some restrictions
    final DLList initRestrictedChildren = new DLList();

    public void initializeRestrictions(Restrictions _restrictions)
    {
        restrictions = _restrictions;

        OMapIterator lastIter = portal2sdep.end();
        OMapIterator iter;

        // create restrictions for myself
        for (iter = portal2sdep.begin();
            !iter.equals(lastIter);
            iter.next())
        {
            P2PPortal portal = (P2PPortal)iter.getKey();
            SDEPData sdep = (SDEPData)iter.getData();

            // create the restriction for the downstream filter
            InitDownstreamRestriction initDownstreamRestriction =
                new InitDownstreamRestriction(portal, sdep, this);
            restrictions.add(initDownstreamRestriction);

            // create the restriction for the upstream filter
            InitUpstreamRestriction initUpstreamRestriction =
                new InitUpstreamRestriction(
                    portal,
                    sdep,
                    this,
                    initDownstreamRestriction);
            restrictions.add(initUpstreamRestriction);

            numInitialRestrictions++;
        }

        // allow all my children to create restrictions
        {
            int nChild;
            for (nChild = 0; nChild < getNumChildren(); nChild++)
            {
                StreamInterface child = getConstrainedChild(nChild);
                child.initializeRestrictions(restrictions);

                initRestrictedChildren.pushBack(child);
            }
        }

        // create init restrictions for peeking filters
        {
            int nChild;
            for (nChild = 0; nChild < getNumChildren() - 1; nChild++)
            {
                StreamInterface topChild = getConstrainedChild(nChild);
                StreamInterface bottomChild =
                    getConstrainedChild(nChild + 1);
                LatencyEdge edge = connectingLatencyEdges[nChild];

                // if the steady state of the bottom node peeks no more
                // than the initialization state of the top node, I don't
                // need to provide the provide it with any extra data
                // beyond what's needed for straight initialization
                if (bottomChild.getSteadyPeek()
                    - bottomChild.getSteadyPop()
                    <= bottomChild.getInitPeek() - bottomChild.getInitPop())
                {
                    continue;
                }

                // I need the upstream node to push some extra data
                // in order to make sure that the steady state will be 
                // initialized properly!
                Restriction peekRestriction =
                    new InitPeekRestriction(edge, this);
                restrictions.add(peekRestriction);

                numInitialRestrictions++;
            }
        }
    }

    public void initRestrictionsCompleted(P2PPortal portal)
    {
        // first decrease the current count of restrictions
        numInitialRestrictions--;

        // now just create a steady-state restriction :)
        OMapIterator portalIter = portal2sdep.find(portal);

        // if this is only a local initialization restriction,
        // don't bother creating a real restriction for it (obviously)
        if (portalIter.equals(portal2sdep.end()))
            return;

        SDEPData sdep = (SDEPData)portalIter.getData();

        SteadyUpstreamRestriction upstreamRestriction =
            new SteadyUpstreamRestriction(portal, sdep, this);
        SteadyDownstreamRestriction downstreamRestriction =
            new SteadyDownstreamRestriction(portal, sdep, this);

        // these will automatically remove the initialization restrictions
        // from the restrictions queues
        restrictions.add(upstreamRestriction);
        restrictions.add(downstreamRestriction);

        portal2restrictionPair.insert(
            portal,
            new Pair(upstreamRestriction, downstreamRestriction));
    }

    public boolean isDoneInitializing()
    {
        if (numInitialRestrictions != 0)
            return false;
        while (!initRestrictedChildren.empty())
        {
            StreamInterface restrictedChild =
                (StreamInterface)initRestrictedChildren.front().get();

            if (restrictedChild.isDoneInitializing())
            {
                initRestrictedChildren.popFront();
            }
            else
            {
                return false;
            }
        }
        return true;

    }

    final DLList steadyStateRestrictedChildren = new DLList();
    boolean checkForAllMessagesNow = false;

    public void createSteadyStateRestrictions(int streamNumExecs)
    {
        for (int nChild = 0; nChild < getNumChildren(); nChild++)
        {
            StreamInterface child = getConstrainedChild(nChild);
            child.createSteadyStateRestrictions(
                streamNumExecs * getChildNumExecs(nChild));
            steadyStateRestrictedChildren.pushBack(child);
        }

        checkForAllMessagesNow = true;
    }

    public boolean isDoneSteadyState()
    {
        while (!steadyStateRestrictedChildren.empty())
        {
            StreamInterface restrictedChild =
                (StreamInterface)steadyStateRestrictedChildren
                    .front()
                    .get();
            if (restrictedChild.isDoneSteadyState())
            {
                steadyStateRestrictedChildren.popFront();
            }
            else
            {
                return false;
            }
        }

        return true;
    }

    final DLList newlyBlockedRestrictions = new DLList();

    public void registerNewlyBlockedSteadyRestriction(Restriction restriction)
    {
        newlyBlockedRestrictions.pushBack(restriction);
    }

    int internalDataAvailable[];

    public PhasingSchedule getNextPhase(
        Restrictions restrs,
        int nDataAvailable)
    {
        PhasingSchedule phase = new PhasingSchedule(this);

        if (internalDataAvailable == null)
        {
            internalDataAvailable = new int[getNumChildren() + 1];
        }

        internalDataAvailable[0] = nDataAvailable;

        if (checkForAllMessagesNow)
        {
            OMapIterator portal2restrictionPairIter;
            OMapIterator lastIter = portal2restrictionPair.end();
            for (portal2restrictionPairIter =
                portal2restrictionPair.begin();
                !portal2restrictionPairIter.equals(lastIter);
                portal2restrictionPairIter.next())
            {
                Pair pair = (Pair)portal2restrictionPairIter.getData();
                SteadyUpstreamRestriction upstreamRestr =
                    (SteadyUpstreamRestriction)pair.getFirst();
                SteadyDownstreamRestriction downstreamRestr =
                    (SteadyDownstreamRestriction)pair.getSecond();

                PhasingSchedule upstreamMsgCheck = upstreamRestr.checkMsg();
                PhasingSchedule downstreamMsgCheck =
                    downstreamRestr.checkMsg();

                if (upstreamMsgCheck != null)
                    phase.appendPhase(upstreamMsgCheck);
                if (downstreamMsgCheck != null)
                    phase.appendPhase(downstreamMsgCheck);
            }
        }

        for (int nChild = 0; nChild < getNumChildren(); nChild++)
        {
            boolean hadNewBlockedRestrictions;
            do
            {
                hadNewBlockedRestrictions = false;
                StreamInterface child = getConstrainedChild(nChild);

                PhasingSchedule childPhase =
                    child.getNextPhase(
                        restrs,
                        internalDataAvailable[nChild]);
                internalDataAvailable[nChild] -= childPhase.getOverallPop();
                internalDataAvailable[nChild
                    + 1] += childPhase.getOverallPush();

                phase.appendPhase(childPhase);

                while (!newlyBlockedRestrictions.empty())
                {
                    ERROR("not tested");
                    hadNewBlockedRestrictions = true;

                    Restriction restriction =
                        (Restriction)newlyBlockedRestrictions.front().get();
                    PhasingSchedule msgPhase = restriction.checkMsg();
                    if (msgPhase != null)
                        phase.appendPhase(msgPhase);

                    newlyBlockedRestrictions.popFront();
                }
            }
            while (hadNewBlockedRestrictions);
        }

        return phase;
    }

    public void computeSchedule()
    {
        restrictions = new Restrictions();
        initializeRestrictions(restrictions);

        {
            LatencyNode topLatencyNode = this.getTopLatencyNode();

            P2PPortal initPortal =
                new P2PPortal(
                    true,
                    topLatencyNode,
                    topLatencyNode,
                    0,
                    0,
                    this);

            InitializationSourceRestriction initRestriction =
                new InitializationSourceRestriction(initPortal);
            restrictions.add(initRestriction);
            while (!isDoneInitializing())
            {
                initRestriction.goAgain();
                PhasingSchedule initPhase = getNextPhase(restrictions, 0);
                this.addInitScheduleStage(initPhase);
            }

            restrictions.remove(initRestriction);
        }

        {
            createSteadyStateRestrictions(1);
            PhasingSchedule steadyPhase = getNextPhase(restrictions, 0);
            this.addSteadySchedulePhase(steadyPhase);
        }

        if (!isDoneSteadyState())
        {
            ERROR("This schedule is impossible in some way, or there is a bug in the scheduler");
        }
    }
}
