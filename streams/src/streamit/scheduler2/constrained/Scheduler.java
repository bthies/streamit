package streamit.scheduler2.constrained;

import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.base.StreamInterface;
import streamit.scheduler2.SDEPData;

public class Scheduler extends streamit.scheduler2.Scheduler
{
    final StreamInterface rootStream;
    StreamFactory factory = new ConstrainedStreamFactory();

    public Scheduler(Iterator _root)
    {
        super(_root);

        rootStream = factory.newFrom(root, null);
    }

    public void computeSchedule()
    {
        if (steadySchedule != null)
            return;

        rootStream.computeSchedule();

        initSchedule = rootStream.getInitSchedule();
        steadySchedule = rootStream.getSteadySchedule();
    }

    public SDEPData computeSDEP(Iterator src, Iterator dst)
    {
        LatencyGraph graph = factory.getLatencyGraph();
        LatencyNode srcNode =
            ((Filter)factory.newFrom(src, null)).getLatencyNode();
        LatencyNode dstNode =
            ((Filter)factory.newFrom(dst, null)).getLatencyNode();
        return graph.computeSDEP(srcNode, dstNode);
    }
}
