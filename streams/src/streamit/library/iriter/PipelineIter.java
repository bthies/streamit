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

import streamit.library.Pipeline;

public class PipelineIter
    extends streamit.misc.DestroyedClass
    implements streamit.scheduler2.iriter.PipelineIter
{
    PipelineIter(Pipeline _pipeline)
    {
        pipeline = _pipeline;
    }

    Pipeline pipeline;
    
    public Object getObject ()
    {
        return pipeline;
    }
    
    public streamit.scheduler2.iriter.Iterator getUnspecializedIter()
    {
        return new Iterator(pipeline);
    }
    
    public int getNumChildren ()
    {
        return pipeline.getNumChildren ();
    }
    
    public streamit.scheduler2.iriter.Iterator getChild (int n)
    {
        return new Iterator (pipeline.getChildN (n));
    }
    
    public boolean equals(Object other)
    {
        if (!(other instanceof PipelineIter)) return false;
        PipelineIter otherPipe = (PipelineIter) other;
        return otherPipe.getObject() == this.getObject();
    }
    
    public int hashCode()
    {
        return pipeline.hashCode();
    }
}

