package streamit.scheduler2.constrained;

import streamit.misc.DestroyedClass;
import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.base.StreamInterface;

import java.util.Map;
import java.util.HashMap;

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

    private final Map iter2stream = new HashMap();

    public StreamInterface newFrom(Iterator streamIter, Iterator parent)
    {
        if (iter2stream.containsKey(streamIter))
        {
            return (StreamInterface)iter2stream.get(streamIter);
        }

        StreamInterface newStream;

        if (streamIter.isFilter() != null)
        {
            newStream = new Filter(streamIter.isFilter(), parent, this);
        }
        else if (streamIter.isPipeline() != null)
        {
            newStream = new Pipeline(streamIter.isPipeline(), parent, this);
        }
        else
        {
            ERROR("Unsupported type passed to StreamFactory!");
            newStream = null;
        }

        iter2stream.put(streamIter, newStream);
        return newStream;
    }

    public LatencyGraph getLatencyGraph()
    {
        return latencyGraph;
    }
}
