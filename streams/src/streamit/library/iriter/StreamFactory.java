package streamit.iriter;

/* $Id: StreamFactory.java,v 1.6 2002-07-27 22:43:05 karczma Exp $ */

import streamit.misc.DestroyedClass;
import streamit.scheduler.iriter.Iterator;
import streamit.scheduler.base.StreamInterface;
import streamit.scheduler.minlatency.Filter;
import streamit.scheduler.minlatency.Pipeline;
import streamit.scheduler.minlatency.SplitJoin;
import streamit.scheduler.minlatency.FeedbackLoop;

/**
 * This class basically implements the StreamFactory interface.  In the 
 * current, first draft, this class will just create single appearance
 * schedule objects, so the resulting schedules will be single appearance.
 */

public class StreamFactory
    extends DestroyedClass
    implements streamit.scheduler.base.StreamFactory
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

        // not implemented yet
        ASSERT(false);
        return null;
    }
}