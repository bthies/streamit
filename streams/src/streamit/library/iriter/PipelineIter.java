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

