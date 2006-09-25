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
import streamit.scheduler2.SDEPData;
import streamit.scheduler2.Schedule;

import streamit.misc.OMap;
import streamit.misc.OMapIterator;

import java.util.HashSet;
import java.util.HashMap;

public class Scheduler extends streamit.scheduler2.Scheduler
{
    final StreamInterface rootStream;
    final StreamFactory factory;

    protected Scheduler(Iterator _root, boolean needsSchedule) {
        super(_root);

        factory = new ConstrainedStreamFactory(this, needsSchedule);
        rootStream =
            (
             streamit
             .scheduler2
             .constrained
             .StreamInterface)factory
            .newFrom(
                     root,
                     null);
    }

    /**
     * In usual scenario, creating a scheduler to perform scheduling.
     */
    public static Scheduler create(Iterator _root) {
        return new Scheduler(_root, true);
    }

    /**
     * The scheduler also can do SDEP calculations, in which case it
     * does not need to build a schedule (can tolerate dynamic rates
     * in some parts of graph).
     */
    public static Scheduler createForSDEP(Iterator _root) {
        return new Scheduler(_root, false);
    }

    public void computeSchedule()
    {
        if (steadySchedule != null)
            return;

        rootStream.computeSchedule();

        initSchedule = rootStream.getInitSchedule();
        steadySchedule = rootStream.getSteadySchedule();

        initSchedule = removeMsgs(initSchedule);
        steadySchedule = removeMsgs(steadySchedule);
    }

    /**
     * Compute SDEPData given a pair of iterators.
     * 
     * @param src Iterator for upstream node.
     * @param dst Iterator for downstream node.
     * @return SDEPData
     * @throws NoPathException
     * @see streamit.scheduler2.constrained.LatencyGraph#computeSDEP(LatencyNode,HashSet)
     */
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

    /**
     * Computes SDEP from <pre>src</pre> to each Iterator in <pre>dst</pre>.  Returns
     * all SDEPData's in a map that is keyed on the members of <pre>dst</pre>.
     */
    public HashMap<Iterator, LatencyEdge> computeSDEP(Iterator src, HashSet<streamit.library.iriter.Iterator> dst)
        throws NoPathException
    {
        LatencyGraph graph = factory.getLatencyGraph();
        LatencyNode srcNode =
            ((Filter)factory.newFrom(src, null)).getLatencyNode();

        // translate <pre>dst</pre> into a new HashSet of LatencyNodes rather
        // than Iterators
        HashSet<LatencyNode> dstNodes = new HashSet<LatencyNode>();
        for (java.util.Iterator<streamit.library.iriter.Iterator> i = dst.iterator(); i.hasNext(); ) {
            Iterator dstNode = i.next();
            dstNodes.add(((Filter)factory.newFrom(dstNode, null)).getLatencyNode());
        }
            
        // compute LatencyNode -> SDEPData
        HashMap<LatencyNode, LatencyEdge> map = graph.computeSDEP(srcNode, dstNodes);

        // translate to Iterator -> SDEPData
        HashMap<Iterator, LatencyEdge> result = new HashMap<Iterator, LatencyEdge>();
        for (java.util.Iterator<LatencyNode> i = map.keySet().iterator(); i.hasNext(); ) {
            LatencyNode node = i.next();
            result.put(node.getStreamInterface().getStreamIter(), map.get(node));
        }

        return result;
    }

    public void addDownstreamConstraint(
                                        Iterator src,
                                        Iterator dst,
                                        int min,
                                        int max,
                                        Object handlerFunction)
    {
        ERROR("Not implemented");
    }

    final OMap subNoMsgs = new OMap();

    public void addUpstreamConstraint(
                                      Iterator upstream,
                                      Iterator downstream,
                                      int min,
                                      int max,
                                      Object handlerFunction)
    {
        LatencyGraph graph = factory.getLatencyGraph();
        Filter upstreamFilter = (Filter)factory.newFrom(upstream, null);
        LatencyNode srcNode = upstreamFilter.getLatencyNode();
        LatencyNode dstNode =
            ((Filter)factory.newFrom(downstream, null)).getLatencyNode();

        StreamInterface parent =
            graph.findLowestCommonAncestor(srcNode, dstNode);

        P2PPortal portal =
            new P2PPortal(
                          true,
                          srcNode,
                          dstNode,
                          min,
                          max,
                          parent,
                          upstreamFilter,
                          upstream,
                          handlerFunction);
        parent.registerConstraint(portal);
        subNoMsgs.insert(portal.getPortalMessageCheckPhase().getSchedule(), null);
    }

    Schedule removeMsgs(Schedule sched)
    {
        // maybe I've already been processed?
        OMapIterator subNoMsgsSched = subNoMsgs.find(sched);
        if (!subNoMsgsSched.equals(subNoMsgs.end()))
            {
                return (Schedule)subNoMsgsSched.getData();
            }
        
        if (sched.isBottomSchedule()) return sched;
        
        // nope - recursively go through the schedules and construct
        // a new set without any nulls :)
        Schedule newSched = new Schedule (sched.getStream());
        for (int i=0;i<sched.getNumPhases();i++)
            {
                Schedule newSubSched = removeMsgs (sched.getSubSched(i));
                if (newSubSched != null)
                    {
                        int nTimes = sched.getSubSchedNumExecs(i);
                        newSched.addSubSchedule(newSubSched, nTimes);
                    }
            }
        
        subNoMsgs.insert(sched, newSched);

        return newSched;
    }
}
