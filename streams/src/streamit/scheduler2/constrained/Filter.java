package streamit.scheduler2.constrained;

import streamit.scheduler2.iriter./*persistent.*/
FilterIter;

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
