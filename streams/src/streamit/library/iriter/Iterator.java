package streamit.library.iriter;

import streamit.library.FeedbackLoop;
import streamit.library.Filter;
import streamit.library.Pipeline;
import streamit.library.SplitJoin;
import streamit.library.Stream;

public class Iterator implements streamit.scheduler2.iriter.Iterator
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
    
    // members of streamit.scheduler2.iriter.Iterator

    public streamit.scheduler2.iriter.FilterIter isFilter()
    {
        if (stream instanceof Filter)
            return new streamit.library.iriter.FilterIter((Filter) stream);
        return null;
    }

    public streamit.scheduler2.iriter.PipelineIter isPipeline()
    {
        if (stream instanceof Pipeline)
            return new streamit.library.iriter.PipelineIter((Pipeline) stream);
        return null;
    }

    public streamit.scheduler2.iriter.SplitJoinIter isSplitJoin()
    {
        if (stream instanceof SplitJoin)
            return new streamit.library.iriter.SplitJoinIter((SplitJoin) stream);
        return null;
    }

    public streamit.scheduler2.iriter.FeedbackLoopIter isFeedbackLoop()
    {
        if (stream instanceof FeedbackLoop)
            return new streamit.library.iriter.FeedbackLoopIter((FeedbackLoop) stream);
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
