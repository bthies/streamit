package streamit.iriter;

import streamit.Pipeline;

public class PipelineIter
    extends streamit.misc.DestroyedClass
    implements streamit.scheduler.iriter.PipelineIter
{
    PipelineIter(Pipeline _pipeline)
    {
        pipeline = _pipeline;
    }

    Pipeline pipeline;
    
    public int getNumChildren ()
    {
        return pipeline.getNumChildren ();
    }
    
    public streamit.scheduler.iriter.Iterator getChild (int n)
    {
        return new Iterator (pipeline.getChildN (n));
    }
}

