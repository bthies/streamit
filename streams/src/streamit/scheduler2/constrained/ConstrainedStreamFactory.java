package streamit.scheduler2.constrained;

/* $Id: ConstrainedStreamFactory.java,v 1.1 2003-04-01 22:36:34 karczma Exp $ */

import streamit.misc.DestroyedClass;
import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.base.StreamInterface;

/**
 * This class basically implements the StreamFactory interface.  In the 
 * current, first draft, this class will just create single appearance
 * schedule objects, so the resulting schedules will be single appearance.
 */

public class ConstrainedStreamFactory
    extends DestroyedClass
    implements streamit.scheduler2.constrained.StreamFactory
{
    private LatencyGraph latencyGraph = new LatencyGraph();

    public StreamInterface newFrom(Iterator streamIter)
    {
        if (streamIter.isFilter() != null)
        {
            return new Filter(streamIter.isFilter(), this);
        }

        if (streamIter.isPipeline() != null)
        {
            return new Pipeline(streamIter.isPipeline(), this);
        }

        ERROR("Unsupported type passed to StreamFactory!");
        return null;
    }

    public LatencyGraph getLatencyGraph()
    {
        return latencyGraph;
    }
}
