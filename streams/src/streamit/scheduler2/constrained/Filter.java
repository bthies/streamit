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

import streamit.scheduler2.iriter./*persistent.*/
FilterIter;
import streamit.scheduler2.iriter./*persistent.*/
Iterator;

public class Filter
    extends streamit.scheduler2.hierarchical.Filter
    implements StreamInterface
{
    final private LatencyGraph graph;
    
    private LatencyNode latencyNode;
    
    public Filter(FilterIter iterator, Iterator parent, StreamFactory factory)
    {
        super(iterator);
        
        graph = factory.getLatencyGraph();
    }
    
    public void initiateConstrained()
    {
        latencyNode = graph.addFilter (this);
        
        // create a schedule for this 
    }

    public void computeSchedule()
    {
        ERROR("Not implemented yet.");
    }
    
    public LatencyNode getBottomLatencyNode()
    {
        return latencyNode;
    }

    public LatencyNode getTopLatencyNode()
    {
        return latencyNode;
    }
    
    public LatencyNode getLatencyNode ()
    {
        return latencyNode;
    }
}
