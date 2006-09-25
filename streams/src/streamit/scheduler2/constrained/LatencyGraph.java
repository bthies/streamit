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
import java.util.Stack;

import streamit.scheduler2.SDEPData;

/**
 * LatencyGraph provides an implementation of a graph of stream operators.
 * These operators do not have to be arranged in a structured way (StreamIt
 * operators).
 * 
 * This class also provides an algorithm for computing the SDEP 
 * relationship between an upstream filter and a set of downstream filters.
 *
 */

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
    final HashMap<StreamInterface, DLList> ancestorLists = new HashMap<StreamInterface, DLList>();

    DLList getAncestorList(StreamInterface stream)
    {
        DLList ancestors = ancestorLists.get(stream);
        assert ancestors != null;

        return ancestors;
    }

    /**
     * Returns lowest common ancestor of a set of latency nodes.
     */
    public StreamInterface findLowestCommonAncestor(HashSet<LatencyNode> nodes) {
        // explore iterators of ancestors in parallel
        DLList_const[] ancestorList = new DLList_const[nodes.size()];
        DLListIterator[] ancestorIter = new DLListIterator[nodes.size()];
        
        // get ancestors
        int k=0;
        for (Iterator<LatencyNode> iter = nodes.iterator(); iter.hasNext(); k++) {
            LatencyNode node = iter.next();
            ancestorList[k] = node.getAncestors();
            ancestorIter[k] = ancestorList[k].begin();
        }

        // walk down ancestors in parallel
        StreamInterface lowestAncestor = (StreamInterface)ancestorIter[0].get();

        outer:
        while (true) {
            // candidate for the next lowestAncestor
            StreamInterface candidate = null;

            // evaluate candidate
            for (int i=0; i<nodes.size(); i++) {
                // advance i'th node's iter
                ancestorIter[i].next();
                
                // if we reached the level of a node, break
                if (ancestorIter[i].equals(ancestorList[i].end()))
                    break outer;

                if (i==0) {
                    // if we are first node, make new candidate lowest common ancestor
                    candidate = (StreamInterface)ancestorIter[i].get();
                } else {
                    // otherwise, evaluate candidate; break if different
                    if (candidate != ancestorIter[i].get())
                        break outer;
                }
            }

            // candidate passed; accept
            lowestAncestor = candidate;
        }

        return lowestAncestor;
    }
    public StreamInterface findLowestCommonAncestor(LatencyNode n1, LatencyNode n2) {
        HashSet<LatencyNode> nodes = new HashSet<LatencyNode>();
        nodes.add(n1);
        nodes.add(n2);
        return findLowestCommonAncestor(nodes);
    }
    public StreamInterface findLowestCommonAncestor(HashSet<LatencyNode> set1, HashSet<LatencyNode> set2) {
        HashSet<LatencyNode> nodes = new HashSet<LatencyNode>();
        nodes.addAll(set1);
        nodes.addAll(set2);
        return findLowestCommonAncestor(nodes);
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
                DLList parentAncestors =ancestorLists.get(parent);
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
     * <pre>ancestor</pre>.
     */
    private boolean hasCycle(StreamInterface ancestor) {
        // do a depth-first search; if we find a back-edge, report
        // cycle
        HashSet<LatencyNode> visiting = new HashSet<LatencyNode>();  // currently being visited
        HashSet<LatencyNode> done = new HashSet<LatencyNode>();      // done being visited
        
        LatencyNode top = ancestor.getTopLatencyNode();

        return hasCycleHelper(top, visiting, done);
    }
    /**
     * Visits <pre>node</pre> and all its children in the depth-first
     * search for cycles.  Returns whether or not it found a cycle.
     */
    private boolean hasCycleHelper(LatencyNode node, HashSet<LatencyNode> visiting, HashSet<LatencyNode> done) {
        if (visiting.contains(node)) {
            // if we are already visiting <pre>node</pre>, then report cycle
            return true;
        } else if (done.contains(node)) {
            // if we already visited <pre>node</pre>, then no cycle here
            return false;
        }

        // otherwise, start visiting node
        visiting.add(node);

        // visit all children of <pre>node</pre> and return true if any of them
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
     * Returns edges between a set of <pre>upstreamNode</pre>'s and a set of
     * <pre>downstreamNode</pre>'s.  Both sets are filled with LatencyNodes.
     * An edge is "between" the two sets if it lies on a path between
     * any upstream node and any downstream node.
     */
    private HashSet<LatencyEdge> getEdgesBetween(HashSet<LatencyNode> upstreamNodes, HashSet<LatencyNode> downstreamNodes) {
        StreamInterface ancestor =
            findLowestCommonAncestor(upstreamNodes, downstreamNodes);

        // find reachable edges looking downstream and looking upstream
        HashSet<LatencyEdge> result = exploreEdges(upstreamNodes, ancestor, true);
        HashSet<LatencyEdge> edgesUpstream = exploreEdges(downstreamNodes, ancestor, false);

        // find an intersection between these two sets:
	result.retainAll(edgesUpstream);

        // if there are any loops in the graph within <pre>ancestor</pre>,
        // remove the backward pointing edges:
        if (hasCycle(ancestor)) {
            // right now this iterates over upstream nodes and removes
            // back edges for each node individually. It could
            // probably be made more efficient, to find backward
            // pointing edges for all nodes together.
            for (Iterator<LatencyNode> i = upstreamNodes.iterator(); i.hasNext(); ) {
                LatencyNode upstreamNode = i.next();
                HashSet<LatencyEdge> backwardPointingEdges =
                    findBackPointingEdges(upstreamNode, ancestor);
                result.removeAll(backwardPointingEdges);
            }
        }

        return result;
    }
    private HashSet<LatencyEdge> getEdgesBetween(LatencyNode upstreamNode, HashSet<LatencyNode> downstreamNodes) {
        HashSet<LatencyNode> upstreamNodes = new HashSet<LatencyNode>();
        upstreamNodes.add(upstreamNode);
        return getEdgesBetween(upstreamNodes, downstreamNodes);
    }
    private HashSet<LatencyEdge> getEdgesBetween(HashSet<LatencyNode> upstreamNodes, LatencyNode downstreamNode) {
        HashSet<LatencyNode> downstreamNodes = new HashSet<LatencyNode>();
        downstreamNodes.add(downstreamNode);
        return getEdgesBetween(upstreamNodes, downstreamNodes);
    }

    /**
     * Given an upstream node and a set of downstream nodes
     * (LatencyNode's), returns a mapping (LatencyNode -> SDEPData) of
     * the SDEP data from the upstream node to a given downstream
     * node.
     * 
     * This method works as follows:
     * <li> First it finds all nodes and edges that are on any path 
     * between the upstreamNode and the downstreamNodes.
     * <li> It eliminates any edges that form loops in this subgraph.
     * This doesn't modify the SDEP relationship, because loops do not
     * contribute to SDEP.
     * <li> It traverses this graph, starting from the upstreamNode
     * using a breadth-first traversal. When a joiner is reached, it is
     * not traversed across until all it incoming edges have been traversed.
     * During this traversal, an SDEP relationship is calulated between the
     * node being visited and the upstreamNode. This is done by combining
     * the SDEP between the previous node and upstreamNode and SDEP between
     * previous node and current node.
     * <li> SDEP for nodes in downsteramNodes are selected and returned.
     */
    public HashMap<LatencyNode, LatencyEdge> computeSDEP(LatencyNode upstreamNode,
                               HashSet<LatencyNode> downstreamNodes) 
        throws NoPathException {
        // first find all the edges that need to be traversed when figuring
        // out the dependencies between nodes
        HashSet<LatencyEdge> edgesToTraverse = getEdgesBetween(upstreamNode, downstreamNodes);

        // make sure that there are SOME edges between the two nodes
        // if this assert fails, then either the srcIsUpstream is reversed
        // or there is no path between upstreamNode and downstreamNode
        // within their lowest common ancestor.
        if (edgesToTraverse.isEmpty()) {
            throw new NoPathException();
        }

        // now go through all the edges and count how many useful edges
        // arrive at each node that will be traversed
        HashMap<LatencyNode, Integer> nodes2numEdges = new HashMap<LatencyNode, Integer>();
        {
            Integer ONE = new Integer(1);
	    for (Iterator<LatencyEdge> edgeIter = edgesToTraverse.iterator(); edgeIter.hasNext(); ) {
		LatencyNode nodeDst =
		    edgeIter.next().getDst();
		
		if (nodes2numEdges.containsKey(nodeDst)) {
		    Integer useCount = nodes2numEdges.get(nodeDst);

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
        HashMap<LatencyNode, LatencyEdge> nodes2latencyEdges = new HashMap<LatencyNode, LatencyEdge>();
        
        // insert an identity latency edge from src to src into the map
        nodes2latencyEdges.put(upstreamNode,
			       new LatencyEdge(upstreamNode));

        HashSet<LatencyNode> nodesToVisit = new HashSet<LatencyNode>();
        nodesToVisit.add(upstreamNode);

        // compute the dependency list for all the nodes
        // wrt to the upstreamNode
        while (!nodesToVisit.isEmpty()) {
	    LatencyNode node = nodesToVisit.iterator().next();
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
				new LatencyEdge(nodes2latencyEdges.get(edgeSrc),
						edge);
			}

			// if there already is an edge going from upstreamNode 
			// to edgeDst, I need to combine that edge with this 
			// newEdge
			if (nodes2latencyEdges.containsKey(edgeDst)) {
			    newEdge =
				new LatencyEdge(newEdge,
						nodes2latencyEdges.get(edgeDst));
			}
			
			// and finally, insert the new edge into the map.
			// if an edge going to edgeDst already exists there,
			// this will replace it, which is good
			nodes2latencyEdges.put(edgeDst, newEdge);
		    }

		    // find how many more edges need to lead to this node
		    int nodeNumEdges = nodes2numEdges.get(edge.getDst()).intValue();
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

        // build a map of what the caller requested
        HashMap<LatencyNode, LatencyEdge> result = new HashMap<LatencyNode, LatencyEdge>();
        for (Iterator<LatencyNode> i = downstreamNodes.iterator(); i.hasNext(); ) {
            LatencyNode node = i.next();
            assert nodes2latencyEdges.containsKey(node): "No data for " + node;
            result.put(node, nodes2latencyEdges.get(node));
        }

        return result;
    }

    /**
     * Compute SDEPData for a pair of LatencyNode's.
     * 
     * @param upstreamNode
     * @param downstreamNode
     * @return SDEPData
     * @throws NoPathException
     */
    public SDEPData computeSDEP(LatencyNode upstreamNode,
                                LatencyNode downstreamNode)
        throws NoPathException
    {
        HashSet<LatencyNode> downstreamNodes = new HashSet<LatencyNode>();
        downstreamNodes.add(downstreamNode);
        return computeSDEP(upstreamNode, downstreamNodes).get(downstreamNode);
    }

    /**
     * Returns all edges reachable from <pre>startNodes</pre> while staying
     * within stream container <pre>withinStream</pre>.  If <pre>travelDownstream</pre>
     * is true, then nodes are explored downstream from <pre>startNodes</pre>;
     * otherwise, nodes are explored upstream.
     */
    public HashSet<LatencyEdge> exploreEdges(HashSet<LatencyNode> startNodes,
                                StreamInterface withinStream,
                                boolean travelDownstream) {
        
        // nodes left to explore from
        Stack<LatencyNode> nodesToExplore = new Stack<LatencyNode>();
        nodesToExplore.addAll(startNodes);
        // nodes that we have visited
        HashSet<LatencyNode> nodesVisited = new HashSet<LatencyNode>();
        // result set of edges
        HashSet<LatencyEdge> result = new HashSet<LatencyEdge>();

        while (!nodesToExplore.empty()) {
            LatencyNode node = nodesToExplore.pop();
            nodesVisited.add(node);
            
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

                    // if there is no data transferred across this
                    // edge (for example, splitter/joiner weights of
                    // 0) then don't visit 
                    if (edge.getNumSrcSteadyPhases()==0 ||
                        edge.getNumDstSteadyPhases()==0)
                        continue;
		    
		    // visit the edge
                    result.add(edge);
                    
		    // if I've visited the node already once, or it is
		    // a stop node, then do not visit it again
		    if (nodesVisited.contains(downstreamNode)) {
			continue;
		    }

                    // add downstream node to worklist
		    nodesToExplore.push(downstreamNode);
                }
            }

            if (!travelDownstream) {
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
		    
                    // if there is no data transferred across this
                    // edge (for example, splitter/joiner weights of
                    // 0) then don't visit 
                    if (edge.getNumSrcSteadyPhases()==0 ||
                        edge.getNumDstSteadyPhases()==0)
                        continue;

		    // visit the edge
                    result.add(edge);
		    
		    // if I've visited the node already once, or it is
		    // a stop node, then do not visit it again
		    if (nodesVisited.contains(upstreamNode)) {
			continue;
		    }

                    // add upstream node to worklist
		    nodesToExplore.push(upstreamNode);
		}
            }
        }

        return result;
    }
                              
    /**
     * Finds edges which form loops within a certain stream, starting
     * at a particular node.
     * 
     * @param startNode
     * @param withinStream
     * @return Set of back edges
     */
    public HashSet<LatencyEdge> findBackPointingEdges(
                                      LatencyNode startNode,
                                      StreamInterface withinStream)
    {
        // first find all the nodes in the stream (just to count them!)
        HashSet<LatencyNode> nodesInStream = new HashSet<LatencyNode>();

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
        HashMap<LatencyNode, Integer> node2int = new HashMap<LatencyNode, Integer>();
        HashMap<Integer, LatencyNode> int2node = new HashMap<Integer, LatencyNode>();
        int nodeVectors[][] = new int[nNodesInStream][nNodesInStream];
        int nNode = 0;
	for (Iterator<LatencyNode> nodeInStreamIter = nodesInStream.iterator(); 
	     nodeInStreamIter.hasNext(); 
	     nNode++) 
	    {
		LatencyNode node = nodeInStreamIter.next();
		Integer idx = new Integer(nNode);
		node2int.put(node, idx);
		int2node.put(idx, node);
		nodeVectors[nNode][nNode] = 1;
	    }

        // find the backward pointing edges
        HashSet<LatencyEdge> backwardPointingEdges = new HashSet<LatencyEdge>();
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
                            node2int.get(srcNode).intValue();
                        int srcVector[] = nodeVectors[srcIdx];

                        int dstIdx =
                            node2int.get(dstNode).intValue();
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
