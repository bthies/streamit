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

package streamit.scheduler2.constrained;

import streamit.misc.DestroyedClass;
import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.base.StreamInterface;

import java.util.Map;
import java.util.HashMap;

/**
 * This class basically implements the StreamFactory interface.  In the 
 * current, first draft, this class will just create single appearance
 * schedule objects, so the resulting schedules will be single appearance.
 */

public class ConstrainedStreamFactory
    extends DestroyedClass
    implements streamit.scheduler2.constrained.StreamFactory
{
    private LatencyGraph latencyGraph = new LatencyGraph();

    private final Map iter2stream = new HashMap();

    public StreamInterface newFrom(Iterator streamIter, Iterator parent)
    {
        if (iter2stream.containsKey(streamIter))
        {
            return (StreamInterface)iter2stream.get(streamIter);
        }

        StreamInterface newStream;

        if (streamIter.isFilter() != null)
        {
            newStream = new Filter(streamIter.isFilter(), parent, this);
        }
        else if (streamIter.isPipeline() != null)
        {
            newStream = new Pipeline(streamIter.isPipeline(), parent, this);
        }
        else if (streamIter.isSplitJoin() != null)
        {
            newStream =
                new SplitJoin(streamIter.isSplitJoin(), parent, this);
        }
        else if (streamIter.isFeedbackLoop() != null)
        {
            newStream =
                new FeedbackLoop(streamIter.isFeedbackLoop(), parent, this);
        }
        else
        {
            ERROR("Unsupported type passed to StreamFactory!");
            newStream = null;
        }

        iter2stream.put(streamIter, newStream);
        return newStream;
    }

    public LatencyGraph getLatencyGraph()
    {
        return latencyGraph;
    }
}
