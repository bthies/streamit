package streamit.scheduler2.constrained;

/* $Id: Pipeline.java,v 1.2 2003-03-12 22:11:18 karczma Exp $ */

import streamit.scheduler2.iriter./*persistent.*/
PipelineIter;
import streamit.scheduler2.Schedule;
import streamit.scheduler2.hierarchical.PhasingSchedule;

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
        streamit.scheduler2.constrained.StreamFactory factory)
    {
        super(iterator, factory);

        latencyGraph = factory.getLatencyGraph();

        // add all children to the latency graph
        for (int nChild = 0; nChild < getNumChildren() - 1; nChild++)
        {
            StreamInterface topStream = getConstrainedChild(nChild);
            StreamInterface bottomStream = getConstrainedChild(nChild + 1);

            LatencyNode topNode = topStream.getBottomLatencyNode();
            LatencyNode bottomNode = bottomStream.getTopLatencyNode();
            
            LatencyEdge edge = new LatencyEdge (topNode, 0, bottomNode, 0);
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

        return (StreamInterface) child;
    }

    public void computeSchedule()
    {
        ERROR("Not implemented yet.");

    }
}
