package streamit.scheduler2.constrained;

/* $Id: Filter.java,v 1.2 2003-03-12 22:11:17 karczma Exp $ */

import streamit.scheduler2.iriter./*persistent.*/
FilterIter;
import streamit.scheduler2.Schedule;
import streamit.scheduler2.hierarchical.PhasingSchedule;

public class Filter
    extends streamit.scheduler2.hierarchical.Filter
    implements StreamInterface
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
