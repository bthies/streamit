package streamit.scheduler2.constrained;

/* $Id: Filter.java,v 1.1 2003-02-19 23:19:42 karczma Exp $ */

import streamit.scheduler2.iriter./*persistent.*/
FilterIter;
import streamit.scheduler2.Schedule;
import streamit.scheduler2.hierarchical.PhasingSchedule;

public class Filter extends streamit.scheduler2.hierarchical.Filter
{
    public Filter(FilterIter iterator)
    {
        super(iterator);
    }

    public void computeSchedule()
    {
        ERROR("Not implemented yet.");
    }
}
