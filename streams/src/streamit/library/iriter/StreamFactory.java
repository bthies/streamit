package streamit.iriter;

/* $Id: StreamFactory.java,v 1.3 2002-07-06 06:06:05 karczma Exp $ */

import streamit.misc.DestroyedClass;
import streamit.scheduler.iriter.Iterator;
import streamit.scheduler.base.StreamInterface;
import streamit.scheduler.singleappearance.Filter;
import streamit.scheduler.singleappearance.Pipeline;
import streamit.scheduler.singleappearance.SplitJoin;
import streamit.scheduler.singleappearance.FeedbackLoop;

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