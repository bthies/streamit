package streamit.iriter;

import streamit.FeedbackLoop;
import streamit.Filter;
import streamit.Pipeline;
import streamit.SplitJoin;
import streamit.Stream;

public class Iterator implements streamit.scheduler.iriter.Iterator
{
    public Iterator(Stream _stream)
    {
        stream = _stream;
    }

    Stream stream;
    
    public Object getObject ()
    {
        return stream;
    }
    
    // members of streamit.scheduler.iriter.Iterator

    public streamit.scheduler.iriter.FilterIter isFilter()
    {
        if (stream instanceof Filter)
            return new streamit.iriter.FilterIter((Filter) stream);
        return null;
    }

    public streamit.scheduler.iriter.PipelineIter isPipeline()
    {
        if (stream instanceof Pipeline)
            return new streamit.iriter.PipelineIter((Pipeline) stream);
        return null;
    }

    public streamit.scheduler.iriter.SplitJoinIter isSplitJoin()
    {
        if (stream instanceof SplitJoin)
            return new streamit.iriter.SplitJoinIter((SplitJoin) stream);
        return null;
    }

    public streamit.scheduler.iriter.FeedbackLoopIter isFeedbackLoop()
    {
        if (stream instanceof FeedbackLoop)
            return new streamit.iriter.FeedbackLoopIter((FeedbackLoop) stream);
        return null;
    }
    
    public boolean equals(Object other)
    {
        if (!(other instanceof Iterator)) return false;
        Iterator otherIter = (Iterator) other;
        return otherIter.getObject() == this.getObject();
    }
    
    public int hashCode()
    {
        return stream.hashCode();
    }
}