package streamit.scheduler2.constrained;

import streamit.scheduler2.iriter./*persistent.*/
FilterIter;
import streamit.scheduler2.iriter./*persistent.*/
Iterator;

public class Filter
    extends streamit.scheduler2.hierarchical.Filter
    implements StreamInterface
{
    final private LatencyGraph graph;
    
    private LatencyNode latencyNode;
    
    public Filter(FilterIter iterator, Iterator parent, StreamFactory factory)
    {
        super(iterator);
        
        graph = factory.getLatencyGraph();
    }
    
    public void initiateConstrained()
    {
        latencyNode = graph.addFilter (this);
        
        // create a schedule for this 
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
    
    public LatencyNode getLatencyNode ()
    {
        return latencyNode;
    }
}
