package streamit.scheduler2.constrained;

/* $Id: Filter.java,v 1.3 2003-04-01 22:36:35 karczma Exp $ */

import streamit.scheduler2.iriter./*persistent.*/
FilterIter;
import streamit.scheduler2.Schedule;
import streamit.scheduler2.hierarchical.PhasingSchedule;

public class Filter
    extends streamit.scheduler2.hierarchical.Filter
    implements StreamInterface
{
    final private LatencyGraph graph;
    
    final private LatencyNode latencyNode;
    
    public Filter(FilterIter iterator, StreamFactory factory)
    {
        super(iterator);
        
        graph = factory.getLatencyGraph();
        
        latencyNode = graph.addNode (this);
    }

    public void computeSchedule()
    {
        ERROR("Not implemented yet.");
    }

    public LatencyNode getBottomLatencyNode()
    {
        return latencyNode;
    }

    public LatencyNode getTopLatencyNode()
    {
        return latencyNode;
    }
}
