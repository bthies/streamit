package streamit.library.iriter;

import streamit.library.misc.DestroyedClass;

import streamit.library.scheduler2.iriter.Iterator;

import streamit.library.scheduler2.base.StreamInterface;

// switch commenting from these lines to ones below if you want to
// switch to a single-appearance instead of a minlatency scheduler.
/*
import streamit.library.scheduler2.singleappearance.Filter;
import streamit.library.scheduler2.singleappearance.Pipeline;
import streamit.library.scheduler2.singleappearance.SplitJoin;
import streamit.library.scheduler2.singleappearance.FeedbackLoop;
*/

import streamit.library.scheduler2.minlatency.Filter;
import streamit.library.scheduler2.minlatency.Pipeline;
import streamit.library.scheduler2.minlatency.SplitJoin;
import streamit.library.scheduler2.minlatency.FeedbackLoop;

/**
 * This class basically implements the StreamFactory interface.  In the 
 * current, first draft, this class will just create single appearance
 * schedule objects, so the resulting schedules will be single appearance.
 */

public class StreamFactory
    extends DestroyedClass
    implements streamit.scheduler2.base.StreamFactory
{
    public StreamInterface newFrom(Iterator streamIter, Iterator parent)
    {
        if (streamIter.isFilter() != null)
        {
            return new Filter(streamIter.isFilter(), 0);
        }

        if (streamIter.isPipeline() != null)
        {
            return new Pipeline(streamIter.isPipeline(), 0, this);
        }
        
        if (streamIter.isSplitJoin() != null)
        {
            return new SplitJoin(streamIter.isSplitJoin(), 0, this);
        }

        if (streamIter.isFeedbackLoop() != null)
        {
            return new FeedbackLoop(streamIter.isFeedbackLoop(), 0, this);
        }

        ERROR ("Unsupported type passed to StreamFactory!");
        return null;
    }
}
