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

import streamit.misc.DLList;
import streamit.misc.DLList_const;
import streamit.misc.DLListIterator;

import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;

import streamit.scheduler2.SDEPData;

public class LatencyGraph extends streamit.misc.AssertedClass
{
    /*
     * This is a list of all nodes (Filters, splitter and joiners)
     * in the graph. Each element is a LatencyNode.
     */
    final DLList nodes = new DLList();

    /*
     * This a map of StreamInterfaces to DLList of StreamInterfaces.
     * The second entry contains a list of ancestor of the key, 
     * ordered from top-most to bottom-most (StreamIt object downto
     * the immediate parent). A stream with no parents (out-most
     * pipeline) has an empty list.
     */
    final HashMap ancestorLists = new HashMap();

    DLList getAncestorList(StreamInterface stream)
    {
        DLList ancestors = (DLList)ancestorLists.get(stream);
        assert ancestors != null;

        return ancestors;
    }

    public StreamInterface findLowestCommonAncestor(
                                                    LatencyNode src,
                                                    LatencyNode dst)
    {
        DLList_const srcAncestors = src.getAncestors();
        DLList_const dstAncestors = dst.getAncestors();

        DLListIterator srcIter = srcAncestors.begin();
        DLListIterator dstIter = dstAncestors.begin();

        DLListIterator lastSrcIter = srcAncestors.end();
        DLListIterator lastDstIter = dstAncestors.end();

        StreamInterface lowestAncestor = (StreamInterface)srcIter.get();
        assert (StreamInterface)dstIter.get() == lowestAncestor;

        for (;
             (!srcIter.equals(lastSrcIter))
                 && (!dstIter.equals(lastDstIter));
             srcIter.next(), dstIter.next())
            {
                if (srcIter.get() != dstIter.get())
                    break;

                lowestAncestor = (StreamInterface)srcIter.get();
            }

        return lowestAncestor;
    }

    void registerParent(StreamInterface child, StreamInterface parent)
    {
        if (parent == null)
            {
                // no parent - just add an empty list
                ancestorLists.put(child, new DLList());
            }
        else
            {
                // has a parent - find parent's ancestors, copy the list
                // and add the parent - that's the child's list of ancestors
                DLList parentAncestors =(DLList)ancestorLists.get(parent);
                DLList ancestors = parentAncestors.copy();
                ancestors.pushBack(parent);
                ancestorLists.put(child, ancestors);
            }
    }

    LatencyNode addFilter(Filter filter)
    {
        LatencyNode newNode =
            new LatencyNode(filter, getAncestorList(filter));
        nodes.pushBack(newNode);
        return newNode;
    }

    LatencyNode addSplitter(SplitJoin sj)
    {
        LatencyNode newNode =
            new LatencyNode(sj, true, getAncestorList(sj));
        nodes.pushBack(newNode);
        return newNode;
    }

    LatencyNode addJoiner(SplitJoin sj)
    {
        LatencyNode newNode =
            new LatencyNode(sj, false, getAncestorList(sj));
        nodes.pushBack(newNode);
        return newNode;
    }

    LatencyNode addSplitter(FeedbackLoop fl)
    {
        LatencyNode newNode =
            new LatencyNode(fl, true, getAncestorList(fl));
        nodes.pushBack(newNode);
        return newNode;
    }

    LatencyNode addJoiner(FeedbackLoop fl)
    {
        LatencyNode newNode =
            new LatencyNode(fl, false, getAncestorList(fl));
        nodes.pushBack(newNode);
        return newNode;
    }

    /**
     * Returns whether or not this graph has a cycle, contained within
     * <ancestor>.
     */
    private boolean hasCycle(StreamInterface ancestor) {
        // do a depth-first search; if we find a back-edge, report
        // cycle
        HashSet visiting = new HashSet();  // currently being visited
        HashSet done = new HashSet();      // done being visited
        
        LatencyNode top = ancestor.getTopLatencyNode();

        return hasCycleHelper(top, visiting, done);
    }
    /**
     * Visits <node> and all its children in the depth-first
     * search for cycles.  Returns whether or not it found a cycle.
     */
    private boolean hasCycleHelper(LatencyNode node, HashSet visiting, HashSet done) {
        if (visiting.contains(node)) {
            // if we are already visiting <node>, then report cycle
            return true;
        } else if (done.contains(node)) {
            // if we already visited <node>, then no cycle here
            return false;
        }

        // otherwise, start visiting node
        visiting.add(node);

        // visit all children of <node> and return true if any of them
        // lead to a cycle
        DLList_const downstreamEdges = node.getDependants();
        DLListIterator edgeIter = downstreamEdges.begin();
        DLListIterator lastEdgeIter = downstreamEdges.end();
        for (; !edgeIter.equals(lastEdgeIter); edgeIter.next()) {
            LatencyEdge edge = (LatencyEdge)edgeIter.get();
            assert edge.getSrc() == node;
            
            LatencyNode downstreamNode = edge.getDst();
            if (hasCycleHelper(downstreamNode, visiting, done)) {
                return true;
            }
        }

        // mark as done
        visiting.remove(node);
        done.add(node);

        return false;
    }

    /**
     * Returns edges between two nodes, assuming one is upstream and
     * other is downstream.
     */
    private HashSet getEdgesBetween(LatencyNode upstreamNode, LatencyNode downstreamNode) {
        StreamInterface ancestor =
            findLowestCommonAncestor(upstreamNode, downstreamNode);

        HashSet result =
            visitGraph(
                       upstreamNode,
                       false,
                       true,
                       ancestor,
                       false,
                       true);

        HashSet edgesUpstream =
            visitGraph(
                       downstreamNode,
                       true,
                       false,
                       ancestor,
                       false,
                       true);

        // find an intersection between these two sets:
	result.retainAll(edgesUpstream);

        // if there are any loops in the graph within <ancestor>,
        // remove the backward pointing edges:
        if (hasCycle(ancestor)) {
            HashSet backwardPointingEdges =
                findBackPointingEdges(upstreamNode, ancestor);
	    result.removeAll(backwardPointingEdges);
        }

        return result;
    }

    public SDEPData computeSDEP(
                                LatencyNode upstreamNode,
                                LatencyNode downstreamNode)
        throws NoPathException
    {
        // first find all the edges that need to be traversed when figuring
        // out the dependencies between nodes
        HashSet edgesToTraverse = getEdgesBetween(upstreamNode, downstreamNode);

        // make sure that there are SOME edges between the two nodes
        // if this assert fails, then either the srcIsUpstream is reversed
        // or there is no path between upstreamNode and downstreamNode
        // within their lowest common ancestor.
        // if you don't understand this, or think it's wrong, ask karczma 
        // (03/07/15)
        //assert !edgesToTraverse.isEmpty();
        if (edgesToTraverse.isEmpty()) {
            throw new NoPathException();
        }

        // now go through all the edges and count how many useful edges
        // arrive at each node that will be traversed
        HashMap nodes2numEdges = new HashMap();
        {
            Integer ONE = new Integer(1);
	    for (Iterator edgeIter = edgesToTraverse.iterator(); edgeIter.hasNext(); ) {
		LatencyNode nodeDst =
		    ((LatencyEdge)edgeIter.next()).getDst();
		
		if (nodes2numEdges.containsKey(nodeDst)) {
		    Integer useCount = (Integer)nodes2numEdges.get(nodeDst);

		    // increase the data useful edge count
		    nodes2numEdges.put(nodeDst, new Integer(useCount.intValue() + 1));
		} else {
		    nodes2numEdges.put(nodeDst, ONE);
                }
	    }
	}

        /*
         * The following map maps LatencyNode to LatencyEdge
         * The LatencyEdge is a latency LatencyEdge (meaning that it
         * never is concerned about # of data items that travel on tapes,
         * only which execution of the src translates to an execution of
         * the dst) going from upstreamNode to the key node. 
         */
        HashMap nodes2latencyEdges = new HashMap();

        // insert an identity latency edge from src to src into the map
        nodes2latencyEdges.put(upstreamNode,
			       new LatencyEdge(upstreamNode));

        HashSet nodesToVisit = new HashSet();
        nodesToVisit.add(upstreamNode);

        // compute the dependency list for all the nodes
        // wrt to the upstreamNode
        while (!nodesToVisit.isEmpty()) {
	    LatencyNode node = (LatencyNode)nodesToVisit.iterator().next();
	    nodesToVisit.remove(node);

	    DLList_const dependants = node.getDependants();
	    DLListIterator dependantIter = dependants.begin();
	    DLListIterator lastDependant = dependants.end();

	    for (;
		 !dependantIter.equals(lastDependant);
		 dependantIter.next())
		{
		    LatencyEdge edge = (LatencyEdge)dependantIter.get();
		    LatencyNode edgeSrc = edge.getSrc();
		    LatencyNode edgeDst = edge.getDst();

		    // if this edge doesn't need to be traversed, don't
		    if (!edgesToTraverse.contains(edge))
			continue;

		    // create an edge from upstreamNode to edgeDst
		    {
			// first just create an edge by combining edges:
			// upstreamNode->edge.src and edge
			LatencyEdge newEdge;
			{
			    assert nodes2latencyEdges.containsKey(edgeSrc);

			    newEdge =
				new LatencyEdge((LatencyEdge)nodes2latencyEdges.get(edgeSrc),
						edge);
			}

			// if there already is an edge going from upstreamNode 
			// to edgeDst, I need to combine that edge with this 
			// newEdge
			if (nodes2latencyEdges.containsKey(edgeDst)) {
			    newEdge =
				new LatencyEdge(newEdge,
						(LatencyEdge)nodes2latencyEdges.get(edgeDst));
			}
			
			// and finally, insert the new edge into the map.
			// if an edge going to edgeDst already exists there,
			// this will replace it, which is good
			nodes2latencyEdges.put(edgeDst, newEdge);
		    }

		    // find how many more edges need to lead to this node
		    int nodeNumEdges = ((Integer)nodes2numEdges.get(edge.getDst())).intValue();
		    assert nodeNumEdges > 0;

		    // decrease the number of edges that need to lead to this node
		    // by one
                    nodeNumEdges--;
		    nodes2numEdges.put(edge.getDst(), new Integer(nodeNumEdges));

		    // if there are no more edges leading into this node,
		    // I'm ready to visit the node
		    if (nodeNumEdges == 0) {
			nodesToVisit.add(edge.getDst());
		    }
		}
	}

        return (LatencyEdge)(nodes2latencyEdges.get(downstreamNode));
    }

    public HashSet visitGraph(
                           LatencyNode startNode,
                           boolean travelUpstream,
                           boolean travelDownstream,
                           StreamInterface withinStream,
                           boolean visitNodes,
                           boolean visitEdges)
    {
        assert startNode != null;
        DLList nodesToExplore = new DLList();
        nodesToExplore.pushBack(startNode);
        HashSet nodesVisited = new HashSet();
        nodesVisited.add(startNode);

        HashSet resultNodesNEdges = new HashSet();

        if (visitNodes)
            resultNodesNEdges.add(startNode);

        while (!nodesToExplore.empty()) {
	    LatencyNode node = (LatencyNode)nodesToExplore.begin().get();
	    nodesToExplore.popFront();
	    
	    if (travelUpstream) {
		DLList_const upstreamEdges = node.getDependecies();
		DLListIterator edgeIter = upstreamEdges.begin();
		DLListIterator lastEdgeIter = upstreamEdges.end();
		
		for (; !edgeIter.equals(lastEdgeIter); edgeIter.next()) {
		    LatencyEdge edge = (LatencyEdge)edgeIter.get();
		    assert edge.getDst() == node;
		    
		    LatencyNode upstreamNode = edge.getSrc();
		    
		    // should I visit this node or is it not within
		    // the boundary ancestor?
		    if (withinStream != null
			&& !upstreamNode.hasAncestor(withinStream))
			continue;
		    
		    // visit the edge                        
		    if (visitEdges)
			resultNodesNEdges.add(edge);
		    
		    // if I've visited the node already once, there's no point
		    // in visiting it again!
		    if (nodesVisited.contains(upstreamNode)) {
			continue;
		    }
		    nodesVisited.add(upstreamNode);
		    nodesToExplore.pushBack(upstreamNode);
		    
		    // visit the node
		    if (visitNodes)
			resultNodesNEdges.add(upstreamNode);
		}
	    }
	    
	    if (travelDownstream) {
		DLList_const downstreamEdges = node.getDependants();
		DLListIterator edgeIter = downstreamEdges.begin();
		DLListIterator lastEdgeIter = downstreamEdges.end();

		for (; !edgeIter.equals(lastEdgeIter); edgeIter.next()) {
		    LatencyEdge edge = (LatencyEdge)edgeIter.get();
		    assert edge.getSrc() == node;
		    
		    LatencyNode downstreamNode = edge.getDst();
		    
		    // should I visit this node or is it not within
		    // the boundary ancestor?
		    if (withinStream != null
			&& !downstreamNode.hasAncestor(withinStream))
			continue;
		    
		    // visit the edge                        
		    if (visitEdges)
			resultNodesNEdges.add(edge);

		    // if I've visited the node already once, there's no point
		    // in visiting it again!
		    if (nodesVisited.contains(downstreamNode)) {
			continue;
		    }
		    nodesVisited.add(downstreamNode);
		    nodesToExplore.pushBack(downstreamNode);
		    
		    // visit the node
		    if (visitNodes)
			resultNodesNEdges.add(downstreamNode);
		}
	    }
	}

        return resultNodesNEdges;
    }

    public HashSet findBackPointingEdges(
                                      LatencyNode startNode,
                                      StreamInterface withinStream)
    {
        // first find all the nodes in the stream (just to count them!)
        HashSet nodesInStream = new HashSet();

        {
            DLList nodesToVisit = new DLList();
            nodesToVisit.pushBack(startNode);

            while (!nodesToVisit.empty()) {
		LatencyNode node = (LatencyNode)nodesToVisit.front().get();
		nodesToVisit.popFront();
		
		// have I visited the node already?
		if (nodesInStream.contains(node)) {
		    continue;
		}
		
		// is the node within the minimal ancestor?
		if (withinStream != null
		    && !node.hasAncestor(withinStream))
		    continue;
		
		// add the node as in the stream
		nodesInStream.add(node);
		
		// visit all the downstream nodes of this node
		DLList_const dependants = node.getDependants();
		DLListIterator lastDependantIter = dependants.end();
		for (DLListIterator depIter = dependants.begin();
		     !depIter.equals(lastDependantIter);
		     depIter.next()) 
		    {
			nodesToVisit.pushBack(((LatencyEdge)depIter.get()).getDst());
		    }
	    }
	}
        int nNodesInStream = nodesInStream.size();
        // set up the backwards-pointing edge computation
        HashMap node2int = new HashMap();
        HashMap int2node = new HashMap();
        int nodeVectors[][] = new int[nNodesInStream][nNodesInStream];
        int nNode = 0;
	for (Iterator nodeInStreamIter = nodesInStream.iterator(); 
	     nodeInStreamIter.hasNext(); 
	     nNode++) 
	    {
		LatencyNode node = (LatencyNode)nodeInStreamIter.next();
		Integer idx = new Integer(nNode);
		node2int.put(node, idx);
		int2node.put(idx, node);
		nodeVectors[nNode][nNode] = 1;
	    }

        // find the backward pointing edges
        HashSet backwardPointingEdges = new HashSet();
        DLList nodesToVisit = new DLList();
        for (nodesToVisit.pushBack(startNode);
             !nodesToVisit.empty();
             nodesToVisit.popFront())
            {
                LatencyNode srcNode = (LatencyNode)nodesToVisit.front().get();
                DLList_const edgeList = srcNode.getDependants();
                DLListIterator edgeListIterLast = edgeList.end();
                for (DLListIterator edgeListIter = edgeList.begin();
                     !edgeListIter.equals(edgeListIterLast);
                     edgeListIter.next())
                    {
                        LatencyEdge edge = (LatencyEdge)edgeListIter.get();
                        LatencyNode dstNode = edge.getDst();

                        // is my dst within my desired stream?
                        if (!nodesInStream.contains(dstNode))
                            // no? skip it!
                            continue;

                        // get the vectors for both my src and dst nodes
                        int srcIdx =
                            ((Integer)node2int.get(srcNode)).intValue();
                        int srcVector[] = nodeVectors[srcIdx];

                        int dstIdx =
                            ((Integer)node2int.get(dstNode)).intValue();
                        int dstVector[] = nodeVectors[dstIdx];

                        // have I visited dst already?
                        if (srcVector[dstIdx] != 0)
                            {
                                // yes! add edge to backwardPointingEdges and skip it
                                backwardPointingEdges.add(edge);
                                continue;
                            }

                        // okay, propagate the visited nodes:
                        int oldTotal = 0;
                        int newTotal = 0;
                        for (int nIdx = 0; nIdx < nNodesInStream; nIdx++)
                            {
                                oldTotal += dstVector[nIdx];
                                dstVector[nIdx] |= srcVector[nIdx];
                                newTotal += dstVector[nIdx];
                            }

                        if (newTotal != oldTotal)
                            {
                                // I've added nodes to the dst node vector
                                // will have to visit this node in near future!
                                nodesToVisit.pushBack(dstNode);
                            }
                    }

            }

        return backwardPointingEdges;
    }
}
