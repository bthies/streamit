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

    public void initiateConstrained()
    {
        // register all children
        for (int nChild = 0; nChild < getNumChildren(); nChild++)
        {
            StreamInterface child = getConstrainedChild(nChild);
            latencyGraph.registerParent(child, this);
            child.initiateConstrained();
        }

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

    public void registerConstraint(P2PPortal portal)
    {
        int minLatency = portal.getMinLatency();
        int maxLatency = portal.getMaxLatency();

        // also get the SDEP function for this portal
        SDEPData sdep = null;
        try
        {
            sdep =
                factory.getScheduler().computeSDEP(
                    portal.getUpstreamFilter(),
                    portal.getDownstreamFilter());
        }
        catch (NoPathException exception)
        {
            ERROR(
                "No path from "
                    + portal.getUpstreamFilter().toString()
                    + " to "
                    + portal.getUpstreamFilter().toString()
                    + "!");
        }

        Pair oldSDEP = portal2sdep.insert(portal, sdep);

    }

    Restrictions restrictions;

    // keep track of how many restrictions I am still managing
    int numInitialRestrictions = 0;

    // also keep track of which of my children are still managing
    // some restrictions
    final DLList restrictedChildren = new DLList();

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
                new InitDownstreamRestriction(
                    portal,
                    sdep,
                    this,
                    restrictions);
            restrictions.add(initDownstreamRestriction);

            // create the restriction for the upstream filter
            InitUpstreamRestriction initUpstreamRestriction =
                new InitUpstreamRestriction(
                    portal,
                    sdep,
                    this,
                    initDownstreamRestriction,
                    restrictions);
            restrictions.add(initUpstreamRestriction);

            numInitialRestrictions += 2;
        }

        // allow all my children to create restrictions
        {
            int nChild;
            for (nChild = 0; nChild < getNumChildren(); nChild++)
            {
                StreamInterface child = getConstrainedChild(nChild);
                child.initializeRestrictions(restrictions);

                restrictedChildren.pushBack(child);
            }
        }
    }

    public void initRestrictionsCompleted(P2PPortal portal)
    {
        // first decrease the current count of restrictions
        numInitialRestrictions -= 2;

        // now just create a steady-state restriction :)
        OMapIterator portalIter = portal2sdep.find(portal);
        SDEPData sdep = (SDEPData)portalIter.getData();

        SteadyUpstreamRestriction upstreamRestriction =
            new SteadyUpstreamRestriction(portal, sdep, this, restrictions);
        SteadyDownstreamRestriction downstreamRestriction =
            new SteadyDownstreamRestriction(
                portal,
                sdep,
                this,
                restrictions);

        restrictions.add(upstreamRestriction);
        restrictions.add(downstreamRestriction);

    }

    public boolean isDoneInitializing()
    {
        if (numInitialRestrictions != 0)
            return false;
        while (!restrictedChildren.empty())
        {
            StreamInterface restrictedChild =
                (StreamInterface)restrictedChildren.front().get();

            if (restrictedChild.isDoneInitializing())
            {
                restrictedChildren.popFront();
            }
            else
            {
                return false;
            }
        }
        return true;

    }

    public PhasingSchedule getNextPhase(
        Restrictions restrs,
        int nDataAvailable)
    {
        ERROR("not implemented");
        return null;
    }

    public void computeSchedule()
    {
        restrictions = new Restrictions();

        {
            P2PPortal initPortal = new P2PPortal();

            InitializationSourceRestriction initRestriction =
                new InitializationSourceRestriction(
                    this.getTopConstrainedStream(),
                    initPortal,
                    restrictions);
            restrictions.add(initRestriction);
            while (!isDoneInitializing())
            {
                PhasingSchedule initPhase = getNextPhase(restrictions, 0);
                this.addInitScheduleStage(initPhase);
            }

            restrictions.remove(initRestriction);
        }

        {
            P2PPortal steadyPortal = new P2PPortal();
            SteadyStateSourceRestriction steadyRestriction =
                new SteadyStateSourceRestriction(
                    this.getTopConstrainedStream(),
                    steadyPortal,
                    restrictions);

            restrictions.add(steadyRestriction);
            PhasingSchedule steadyPhase = getNextPhase(restrictions, 0);
            this.addSteadySchedulePhase(steadyPhase);

            ERROR("Not implemented yet.");
        }

    }
}
