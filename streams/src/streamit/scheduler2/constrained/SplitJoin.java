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

import streamit.misc.DLList;

import streamit.scheduler2.iriter./*persistent.*/
SplitJoinIter;
import streamit.scheduler2.iriter./*persistent.*/
Iterator;

import streamit.scheduler2.hierarchical.PhasingSchedule;

/**
 * streamit.scheduler2.constrained.Pipeline is the pipeline constrained 
 * scheduler. It assumes that all streams in the program use the constrained
 * scheduler
 */

public class SplitJoin
    extends streamit.scheduler2.hierarchical.SplitJoin
    implements StreamInterface
{
    final private LatencyGraph latencyGraph;

    LatencyNode latencySplitter, latencyJoiner;

    LatencyEdge splitterLatencyEdges[];

    public SplitJoin(
        SplitJoinIter iterator,
        Iterator parent,
        streamit.scheduler2.constrained.StreamFactory factory)
    {
        super(iterator, factory);

        latencyGraph = factory.getLatencyGraph();

        if (parent == null)
        {
            latencyGraph.registerParent(this, null);
            initiateConstrained();
        }
    }

    public void initiateConstrained()
    {
        splitterLatencyEdges = new LatencyEdge[getNumChildren()];

        latencySplitter = latencyGraph.addSplitter(this);
        latencyJoiner = latencyGraph.addJoiner(this);

        // register all children
        for (int nChild = 0; nChild < getNumChildren(); nChild++)
        {
            StreamInterface child = getConstrainedChild(nChild);
            latencyGraph.registerParent(child, this);
            child.initiateConstrained();
        }

        // add all children to the latency graph
        for (int nChild = 0; nChild < getNumChildren(); nChild++)
        {
            StreamInterface child = getConstrainedChild(nChild);

            LatencyNode topChildNode = child.getTopLatencyNode();
            LatencyNode bottomChildNode = child.getBottomLatencyNode();

            //create the appropriate edges
            LatencyEdge topEdge =
                new LatencyEdge(
                    latencySplitter,
                    nChild,
                    topChildNode,
                    0,
                    0);
            latencySplitter.addDependency(topEdge);
            topChildNode.addDependency(topEdge);

            LatencyEdge bottomEdge =
                new LatencyEdge(
                    bottomChildNode,
                    0,
                    latencyJoiner,
                    nChild,
                    0);
            latencyJoiner.addDependency(bottomEdge);
            bottomChildNode.addDependency(bottomEdge);

            splitterLatencyEdges[nChild] = topEdge;
        }
    }

    StreamInterface getConstrainedChild(int nChild)
    {
        streamit.scheduler2.base.StreamInterface child;
        child = getChild(nChild);

        if (!(child instanceof StreamInterface))
        {
            ERROR("This splitjoin contains a child that is not CONSTRAINED");
        }

        return (StreamInterface)child;
    }

    public StreamInterface getTopConstrainedStream()
    {
        return this;
    }

    public StreamInterface getBottomConstrainedStream()
    {
        return this;
    }

    public LatencyNode getBottomLatencyNode()
    {
        return latencyJoiner;
    }

    public LatencyNode getTopLatencyNode()
    {
        return latencySplitter;
    }

    // override getSplitPhase so I can ask for any phase > 0
    public PhasingSchedule getSplitPhase(int nPhase)
    {
        return super.getSplitPhase (nPhase % getNumSplitPhases ());
    }

    // override getJoingPhase so I can ask for any phase > 0
    public PhasingSchedule getJoinPhase(int nPhase)
    {
        return super.getJoinPhase (nPhase % getNumJoinPhases ());
    }



    public void computeSchedule()
    {
        ERROR("Not implemented yet.");
    }

    public void registerConstraint(P2PPortal portal)
    {
        ERROR ("You cannot have SplitJoin as the lowest common parent!");
    }
    
    
    Restrictions restrictions;
    final DLList initRestrictedChildren = new DLList();
    int numInitialRestrictions = 0;
    final DLList steadyStateRestrictedChildren = new DLList();
    int numSteadyStateRestrictions = 2;

    public void createSteadyStateRestrictions(int streamNumExecs)
    {
        // create restrictions for my children
        for (int nChild = 0; nChild < getNumChildren(); nChild++)
        {
            StreamInterface child = getConstrainedChild(nChild);
            child.createSteadyStateRestrictions(
                streamNumExecs * getChildNumExecs(nChild));
            steadyStateRestrictedChildren.pushBack(child);
        }

        // and for the splitter and joiner
        NodeSteadyRestriction splitterRestriction =
            new NodeSteadyRestriction(
                latencySplitter,
                streamNumExecs * getNumSplitPhases() * getSplitNumRounds(),
                this);
        restrictions.add(splitterRestriction);

        NodeSteadyRestriction joinerRestriction =
            new NodeSteadyRestriction(
                latencyJoiner,
                streamNumExecs * getNumJoinPhases() * getJoinNumRounds(),
                this);
        restrictions.add(joinerRestriction);

        numSteadyStateRestrictions = 2;
    }

    public void doneSteadyState(LatencyNode node)
    {
        ASSERT(node == latencySplitter || node == latencyJoiner);
        numSteadyStateRestrictions--;
        ASSERT(numSteadyStateRestrictions >= 0);
    }

    public void initRestrictionsCompleted(P2PPortal portal)
    {
        numInitialRestrictions--;
    }

    public void initializeRestrictions(Restrictions _restrictions)
    {
        restrictions = _restrictions;
        // SplitJoin cannot be a parent of any external restrictions,
        // so don't worry about them here!

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
                StreamInterface bottomChild = getConstrainedChild(nChild);
                LatencyEdge edge = splitterLatencyEdges[nChild];

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

    public boolean isDoneInitializing()
    {
        if (numInitialRestrictions > 0)
            return false;

        while (!initRestrictedChildren.empty())
        {
            StreamInterface child =
                (StreamInterface)initRestrictedChildren.front().get();
            if (child.isDoneInitializing())
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

    int postSplitterDataAvailable[], preJoinerDataAvailable[];

    int nSplitterPhase = 0, nJoinerPhase = 0;

    public PhasingSchedule getNextPhase(
        Restrictions restrs,
        int nDataAvailable)
    {
        PhasingSchedule phase = new PhasingSchedule(this);

        if (postSplitterDataAvailable == null)
        {
            postSplitterDataAvailable = new int[getNumChildren()];
        }

        if (preJoinerDataAvailable == null)
        {
            preJoinerDataAvailable = new int[getNumChildren()];
        }

        // first execute the splitter as much as possible:
        {
            Restriction strongestRestriction =
                restrictions.getStrongestRestriction(latencySplitter);

            int maxExecs =
                (strongestRestriction != null
                    ? strongestRestriction.getNumAllowedExecutions()
                    : -1);

            while (strongestRestriction == null || maxExecs > 0)
            {
                SplitFlow flow = getSplitSteadyPhaseFlow(nSplitterPhase);
                if (flow.getPopWeight() <= nDataAvailable)
                {
                    phase.appendPhase(getSplitPhase(nSplitterPhase));
                    nSplitterPhase++;

                    maxExecs--;
                    int executedTimes =
                        restrictions.execute(latencySplitter, 1);
                    ASSERT(executedTimes == 1);

                    nDataAvailable -= flow.getPopWeight();
                    for (int nChild = 0;
                        nChild < getNumChildren();
                        nChild++)
                    {
                        postSplitterDataAvailable[nChild]
                            += flow.getPushWeight(nChild);
                    }
                }
                else
                    break;
            }
        }

        // now execute all children:
        for (int nChild = 0; nChild < getNumChildren(); nChild++)
        {
            PhasingSchedule childPhase =
                getConstrainedChild(nChild).getNextPhase(
                    restrictions,
                    postSplitterDataAvailable[nChild]);
            postSplitterDataAvailable[nChild] -= childPhase.getOverallPop();
            preJoinerDataAvailable[nChild] += childPhase.getOverallPush();

            phase.appendPhase(childPhase);
        }

        // and finally execute the joiner as much as possible
        {
            Restriction strongestRestriction =
                restrictions.getStrongestRestriction(latencyJoiner);

            int maxExecs =
                (strongestRestriction != null
                    ? strongestRestriction.getNumAllowedExecutions()
                    : -1);

            execute_joiner : while (
                strongestRestriction == null || maxExecs > 0)
            {
                JoinFlow flow = getJoinSteadyPhaseFlow(nJoinerPhase);
                for (int nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    if (flow.getPopWeight(nChild)
                        > preJoinerDataAvailable[nChild])
                    {
                        break execute_joiner;
                    }
                }

                for (int nChild = 0; nChild < getNumChildren(); nChild++)
                {
                    preJoinerDataAvailable[nChild]
                        -= flow.getPopWeight(nChild);
                }

                phase.appendPhase(getJoinPhase(nJoinerPhase));
                nJoinerPhase++;

                maxExecs--;
                int executedTimes =
                    restrictions.execute(latencyJoiner, 1);
                ASSERT(executedTimes == 1);

            }
        }

        return phase;
    }

    public void registerNewlyBlockedSteadyRestriction(Restriction restriction)
    {
        ERROR("not implemented");
    }

    public boolean isDoneSteadyState()
    {
        return numSteadyStateRestrictions == 0;
    }
}
