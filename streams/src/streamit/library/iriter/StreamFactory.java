package streamit.iriter;

/* $Id: StreamFactory.java,v 1.7 2002-12-03 22:04:54 thies Exp $ */

import streamit.misc.DestroyedClass;
import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.base.StreamInterface;
import streamit.scheduler2.minlatency.Filter;
import streamit.scheduler2.minlatency.Pipeline;
import streamit.scheduler2.minlatency.SplitJoin;
import streamit.scheduler2.minlatency.FeedbackLoop;

/**
 * This class basically implements the StreamFactory interface.  In the 
 * current, first draft, this class will just create single appearance
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

        // not implemented yet
        ASSERT(false);
        return null;
    }
}