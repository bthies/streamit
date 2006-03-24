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
 * This is a basic iterator factory that returns the most basic /
 * direct iterator for a given stream.
 */
public class BasicIterFactory implements IterFactory {

    public BasicIterFactory() {}

    /**
     * Returns a new basic iterator for <pre>filter</pre>.
     */
    public streamit.scheduler2.iriter.FilterIter newFrom(Filter filter) {
        return new streamit.library.iriter.FilterIter((Filter) filter, this);
    }

    /**
     * Returns a new basic iterator for <pre>pipeline</pre>.
     */
    public streamit.scheduler2.iriter.PipelineIter newFrom(Pipeline pipeline) {
        return new streamit.library.iriter.PipelineIter((Pipeline) pipeline, this);
    }
    
    /**
     * Returns a new basic iterator for <pre>sj</pre>.
     */
    public streamit.scheduler2.iriter.SplitJoinIter newFrom(SplitJoin sj) {
        return new streamit.library.iriter.SplitJoinIter((SplitJoin) sj, this);
    }
    
    /**
     * Returns a new basic iterator for <pre>fl</pre>.
     */
    public streamit.scheduler2.iriter.FeedbackLoopIter newFrom(FeedbackLoop fl) {
        return new streamit.library.iriter.FeedbackLoopIter((FeedbackLoop) fl, this);
    }
}
