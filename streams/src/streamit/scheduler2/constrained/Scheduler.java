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

import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.base.StreamInterface;
import streamit.scheduler2.SDEPData;

public class Scheduler extends streamit.scheduler2.Scheduler
{
    final StreamInterface rootStream;
    final StreamFactory factory;

    public Scheduler(Iterator _root)
    {
        super(_root);

        factory =  new ConstrainedStreamFactory(this);
        rootStream = factory.newFrom(root, null);
    }

    public void computeSchedule()
    {
        if (steadySchedule != null)
            return;

        rootStream.computeSchedule();

        initSchedule = rootStream.getInitSchedule();
        steadySchedule = rootStream.getSteadySchedule();
    }

    public SDEPData computeSDEP(Iterator src, Iterator dst)
        throws NoPathException
    {
        LatencyGraph graph = factory.getLatencyGraph();
        LatencyNode srcNode =
            ((Filter)factory.newFrom(src, null)).getLatencyNode();
        LatencyNode dstNode =
            ((Filter)factory.newFrom(dst, null)).getLatencyNode();
        return graph.computeSDEP(srcNode, dstNode);
    }

    public void addDownstreamConstraint(
        Iterator src,
        Iterator dst,
        int min,
        int max)
    {
        ERROR("Not implemented");
    }

    public void addUpstreamConstraint(
        Iterator src,
        Iterator dst,
        int min,
        int max)
    {
        ERROR("Not implemented");
    }
}
