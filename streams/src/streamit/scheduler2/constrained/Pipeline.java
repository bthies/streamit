package streamit.scheduler2.constrained;

/* $Id: Pipeline.java,v 1.1 2003-02-19 23:19:44 karczma Exp $ */

import streamit.scheduler2.iriter./*persistent.*/
PipelineIter;
import streamit.scheduler2.Schedule;
import streamit.scheduler2.base.StreamFactory;
import streamit.scheduler2.hierarchical.PhasingSchedule;

public class Pipeline extends streamit.scheduler2.hierarchical.Pipeline
{
    public Pipeline(PipelineIter iterator, StreamFactory factory)
    {
        super(iterator, factory);
    }

    public void computeSchedule()
    {
        ERROR("Not implemented yet.");
    }
}
