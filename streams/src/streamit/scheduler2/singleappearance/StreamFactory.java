package streamit.scheduler2.singleappearance;

/* $Id: StreamFactory.java,v 1.1 2003-04-01 22:36:40 karczma Exp $ */

import streamit.misc.DestroyedClass;
import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.base.StreamInterface;

/**
 * This class basically implements the StreamFactory interface.  In the 
 * current, first draft, this class will just create min latency
 * schedule objects, so the resulting schedules will be single appearance.
 */

public class StreamFactory
    extends DestroyedClass
    implements streamit.scheduler2.base.StreamFactory
{
    public StreamInterface newFrom(Iterator streamIter)
    {
        if (streamIter.isFilter() != null)
        {
            return new Filter(streamIter.isFilter());
        }

        if (streamIter.isPipeline() != null)
        {
            return new Pipeline(streamIter.isPipeline(), this);
        }
        
        if (streamIter.isSplitJoin() != null)
        {
            return new SplitJoin(streamIter.isSplitJoin(), this);
        }

        if (streamIter.isFeedbackLoop() != null)
        {
            return new FeedbackLoop(streamIter.isFeedbackLoop(), this);
        }

        ERROR ("Unsupported type passed to StreamFactory!");
        return null;
    }
}
