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
SplitJoinIter;
import streamit.scheduler2.iriter./*persistent.*/
Iterator;

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
                new LatencyEdge(latencySplitter, nChild, topChildNode, 0, 0);
            latencySplitter.addDependency(topEdge);
            topChildNode.addDependency(topEdge);

            LatencyEdge bottomEdge =
                new LatencyEdge(bottomChildNode, 0, latencyJoiner, nChild, 0);
            latencyJoiner.addDependency(bottomEdge);
            bottomChildNode.addDependency(bottomEdge);
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

    public LatencyNode getBottomLatencyNode()
    {
        return latencyJoiner;
    }

    public LatencyNode getTopLatencyNode()
    {
        return latencySplitter;
    }

    public void computeSchedule()
    {
        ERROR("Not implemented yet.");

    }
}
