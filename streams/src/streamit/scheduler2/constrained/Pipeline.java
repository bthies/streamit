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
    
    public Pipeline(
        PipelineIter iterator,
        Iterator parent,
        streamit.scheduler2.constrained.StreamFactory factory)
    {
        super(iterator, factory);
        
        latencyGraph = factory.getLatencyGraph();
        
        if (parent == null) 
        {
            latencyGraph.registerParent(this, null);
            initiateConstrained ();
        }
    }
    
    public void initiateConstrained ()
    {
        // register all children
        for (int nChild = 0; nChild < getNumChildren (); nChild++)
        {
            StreamInterface child = getConstrainedChild (nChild);
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

            LatencyEdge edge = new LatencyEdge(topNode, 0, bottomNode, 0, 0);

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
        return getConstrainedChild(getNumChildren() - 1).getBottomLatencyNode();
    }

    public LatencyNode getTopLatencyNode()
    {
        return getConstrainedChild(0).getTopLatencyNode();
    }

    public void computeSchedule()
    {
        ERROR("Not implemented yet.");

    }
}
