/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

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
