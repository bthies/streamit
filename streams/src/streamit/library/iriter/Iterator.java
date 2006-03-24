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

/**
 * This is the implementation of the basic Iterator, as required by
 * scheduler2. All the stream-specific iterators must be derrived from
 * this one. It only provides some super basic capability, like verying
 * the type of the underlying object. These functions return null if
 * the iterator is not of the types inquired about, and they return
 * an iterator to the same object with the proper type if the types match.
 */

public class Iterator implements streamit.scheduler2.iriter.Iterator
{
    /**
     * Stream we are iterating over.
     */
    Stream stream;
    /**
     * Factory used to create child iterators.
     */
    IterFactory factory;
    
    /**
     * Construct an iterator for the given stream, with the default
     * factory.
     */
    public Iterator(Stream _stream)
    {
        this(_stream, new BasicIterFactory());
    }

    /**
     * Construct an iterator for the given stream, using the given
     * factory to create new iterators.
     */
    public Iterator(Stream _stream, IterFactory _factory) {
        this.stream = _stream;
        this.factory = _factory;
    }

    public Object getObject ()
    {
        return stream;
    }
    
    // members of streamit.scheduler2.iriter.Iterator

    public streamit.scheduler2.iriter.FilterIter isFilter()
    {
        if (stream instanceof Filter)
            return factory.newFrom((Filter)stream);
        return null;
    }

    public streamit.scheduler2.iriter.PipelineIter isPipeline()
    {
        if (stream instanceof Pipeline)
            return factory.newFrom((Pipeline)stream);
        return null;
    }

    public streamit.scheduler2.iriter.SplitJoinIter isSplitJoin()
    {
        if (stream instanceof SplitJoin)
            return factory.newFrom((SplitJoin)stream);
        return null;
    }

    public streamit.scheduler2.iriter.FeedbackLoopIter isFeedbackLoop()
    {
        if (stream instanceof FeedbackLoop)
            return factory.newFrom((FeedbackLoop)stream);
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
