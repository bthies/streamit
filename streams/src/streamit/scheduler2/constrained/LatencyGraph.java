package streamit.scheduler2.constrained;

import streamit.misc.DLList;
import streamit.misc.DLList_const;
import streamit.misc.DLListIterator;
import streamit.misc.OMap;
import streamit.misc.OMapIterator;
import streamit.misc.OSet;
import streamit.misc.OSetIterator;

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
    final OMap ancestorLists = new OMap();

    DLList getAncestorList(StreamInterface stream)
    {
        OMapIterator listIter = ancestorLists.find(stream);
        ASSERT(!listIter.equals(ancestorLists.end()));

        DLList ancestors = (DLList)listIter.getData();
        ASSERT(ancestors != null);

        return ancestors;
    }

    StreamInterface findLowestCommonAncestor(
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
        ASSERT((StreamInterface)dstIter.get() == lowestAncestor);

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
            ancestorLists.insert(child, new DLList());
        }
        else
        {
            // has a parent - find parent's ancestors, copy the list
            // and add the parent - that's the child's list of ancestors
            DLList parentAncestors =
                (DLList)ancestorLists.find(parent).getData();
            DLList ancestors = parentAncestors.copy();
            ancestors.pushBack(parent);
            ancestorLists.insert(child, ancestors);
        }
    }

    LatencyNode addNode(Filter filter)
    {
        LatencyNode newNode =
            new LatencyNode(filter, getAncestorList(filter));
        nodes.pushBack(newNode);
        return newNode;
    }

    public SDEPData computeSDEP(
        LatencyNode upstreamNode,
        LatencyNode downstreamNode)
    {
        // first find all the edges that need to be traversed when figuring
        // out the dependencies between nodes
        OSet edgesToTraverse;
        {
            StreamInterface ancestor =
                findLowestCommonAncestor(upstreamNode, downstreamNode);

            edgesToTraverse =
                visitGraph(
                    upstreamNode,
                    false,
                    true,
                    ancestor,
                    false,
                    true);

            OSet edgesUpstream =
                visitGraph(
                    downstreamNode,
                    true,
                    false,
                    ancestor,
                    false,
                    true);

            // find an intersection between these two sets:
            {
                OSetIterator edgesIter = edgesToTraverse.begin();
                OSetIterator edgesLastIter = edgesToTraverse.end();
                OSetIterator upstreamEdgesLastIter = edgesUpstream.end();

                DLList uselessEdges = new DLList();

                for (; !edgesIter.equals(edgesLastIter); edgesIter.next())
                {
                    // if the edgesUpstream set doesn't have this edge,
                    // store it to be removee it from the set
                    if (edgesUpstream
                        .find(edgesIter.get())
                        .equals(upstreamEdgesLastIter))
                        uselessEdges.pushBack(edgesIter.get());
                }

                // remove useless edges
                while (!uselessEdges.empty())
                {
                    edgesToTraverse.erase(uselessEdges.front().get());
                    uselessEdges.popFront();
                }

                // now edgesToTraverse holds all edges that can be traversed
                // between upstreamNode and downstreamNode
            }

            // make sure that there are SOME edges between the two nodes
            // if this ASSERT fails, then either the srcIsUpstream is reversed
            // or there is no path between upstreamNode and downstreamNode
            // within their lowest common ancestor.
            // if you don't understand this, or think it's wrong, ask karczma 
            // (03/07/15)
            ASSERT(!edgesToTraverse.empty());
        }

        // now go through all the edges and count how many useful edges
        // arrive at each node that will be traversed
        OMap nodes2numEdges = new OMap();
        OMapIterator lastN2NEIter = nodes2numEdges.end();
        {
            OSetIterator edgeIter = edgesToTraverse.begin();
            OSetIterator lastEdgeIter = edgesToTraverse.end();

            Integer ZERO = new Integer(0);

            for (; !edgeIter.equals(lastEdgeIter); edgeIter.next())
            {
                LatencyNode nodeDst =
                    ((LatencyEdge)edgeIter.get()).getDst();
                OMapIterator n2eIter = nodes2numEdges.find(nodeDst);

                Integer useCount = ZERO;

                if (!n2eIter.equals(lastN2NEIter))
                {
                    useCount = (Integer)n2eIter.getData();
                }

                useCount = new Integer(useCount.intValue() + 1);

                nodes2numEdges.insert(nodeDst, useCount);
            }
        }

        /*
         * The following map maps LatencyNode to LatencyEdge
         * The LatencyEdge is a latency LatencyEdge (meaning that it
         * never is concerned about # of data items that travel on tapes,
         * only which execution of the src translates to an execution of
         * the dst) going from upstreamNode to the key node. 
         */
        OMap nodes2latencyEdges = new OMap();
        OMapIterator lastNodes2latencyEdgesIter = nodes2latencyEdges.end();

        // insert an identity latency edge from src to src into the map
        nodes2latencyEdges.insert(
            upstreamNode,
            new LatencyEdge(upstreamNode));

        OSet nodesToVisit = new OSet();
        nodesToVisit.insert(upstreamNode);

        // compute the dependency list for all the nodes
        // wrt to the upstreamNode
        // BUGBUG this will not deal correctly with loops!
        while (!nodesToVisit.empty())
        {
            OSetIterator nodesToVisitIter = nodesToVisit.begin();
            LatencyNode node = (LatencyNode)nodesToVisitIter.get();
            nodesToVisit.erase (nodesToVisitIter);

            DLList_const dependants = node.getDependants();
            DLListIterator dependantIter = dependants.begin();
            DLListIterator lastDependant = dependants.end();

            OSetIterator edgesLastIter = edgesToTraverse.end();
            for (;
                !dependantIter.equals(lastDependant);
                dependantIter.next())
            {
                LatencyEdge edge = (LatencyEdge)dependantIter.get();
                LatencyNode edgeSrc = edge.getSrc();
                LatencyNode edgeDst = edge.getDst();

                // if this edge doesn't need to be traversed, don't
                OSetIterator edgeDstNodeIter = edgesToTraverse.find(edge);
                if (edgeDstNodeIter.equals(edgesLastIter))
                    continue;

                // create an edge from upstreamNode to edgeDst
                {
                    // first just create an edge by combining edges:
                    // upstreamNode->edge.src and edge
                    LatencyEdge newEdge;
                    {
                        OMapIterator upstreamNode2srcIter =
                            nodes2latencyEdges.find(edgeSrc);
                        ASSERT(
                            !upstreamNode2srcIter.equals(
                                lastNodes2latencyEdgesIter));

                        newEdge =
                            new LatencyEdge(
                                (LatencyEdge)upstreamNode2srcIter.getData(),
                                edge);
                    }

                    // if there already is an edge going from upstreamNode 
                    // to edgeDst, I need to combine that edge with this 
                    // newEdge
                    {
                        OMapIterator upstreamNode2dstIter =
                            nodes2latencyEdges.find(edgeDst);
                        if (!upstreamNode2dstIter
                            .equals(lastNodes2latencyEdgesIter))
                        {
                            newEdge =
                                new LatencyEdge(
                                    newEdge,
                                    (LatencyEdge)upstreamNode2dstIter
                                        .getData());
                        }
                    }

                    // and finally, insert the new edge into the map.
                    // if an edge going to edgeDst already exists there,
                    // this will replace it, which is good

                    nodes2latencyEdges.insert(edgeDst, newEdge);
                }

                // find how many more edges need to lead to this node
                int nodeNumEdges =
                    ((Integer)nodes2numEdges.find(edge.getDst()).getData())
                        .intValue();
                ASSERT(nodeNumEdges > 0);

                // decrease the number of edges that need to lead to this node
                // by one
                nodeNumEdges--;
                nodes2numEdges.insert(
                    edge.getDst(),
                    new Integer(nodeNumEdges));

                // if there are no more edges leading into this node,
                // I'm ready to visit the node
                if (nodeNumEdges == 0)
                {
                    nodesToVisit.insert(edge.getDst());
                }
            }
        }

        return (LatencyEdge)
            (nodes2latencyEdges.find(downstreamNode).getData());
    }

    public OSet visitGraph(
        LatencyNode startNode,
        boolean travelUpstream,
        boolean travelDownstream,
        StreamInterface withinStream,
        boolean visitNodes,
        boolean visitEdges)
    {
        ASSERT(startNode);
        DLList nodesToExplore = new DLList();
        nodesToExplore.pushBack(startNode);
        OSet nodesVisited = new OSet();
        nodesVisited.insert(startNode);
        OSetIterator lastNodeVisited = nodesVisited.end();

        OSet resultNodesNEdges = new OSet();

        if (visitNodes)
            resultNodesNEdges.insert(startNode);

        while (!nodesToExplore.empty())
        {
            LatencyNode node = (LatencyNode)nodesToExplore.begin().get();
            nodesToExplore.popFront();

            if (travelUpstream)
            {
                DLList_const upstreamEdges = node.getDependecies();
                DLListIterator edgeIter = upstreamEdges.begin();
                DLListIterator lastEdgeIter = upstreamEdges.end();

                for (; !edgeIter.equals(lastEdgeIter); edgeIter.next())
                {
                    LatencyEdge edge = (LatencyEdge)edgeIter.get();
                    ASSERT(edge.getDst() == node);

                    LatencyNode upstreamNode = edge.getSrc();

                    // should I visit this node or is it not within
                    // the boundary ancestor?
                    if (withinStream != null
                        && !upstreamNode.hasAncestor(withinStream))
                        continue;

                    // visit the edge                        
                    if (visitEdges)
                        resultNodesNEdges.insert(edge);

                    // if I've visited the node already once, there's no point
                    // in visiting it again!
                    if (!nodesVisited
                        .find(upstreamNode)
                        .equals(lastNodeVisited))
                        continue;
                    nodesVisited.insert(upstreamNode);
                    nodesToExplore.pushBack(upstreamNode);

                    // visit the node
                    if (visitNodes)
                        resultNodesNEdges.insert(upstreamNode);

                }
            }

            if (travelDownstream)
            {
                DLList_const downstreamEdges = node.getDependants();
                DLListIterator edgeIter = downstreamEdges.begin();
                DLListIterator lastEdgeIter = downstreamEdges.end();

                for (; !edgeIter.equals(lastEdgeIter); edgeIter.next())
                {
                    LatencyEdge edge = (LatencyEdge)edgeIter.get();
                    ASSERT(edge.getSrc() == node);

                    LatencyNode downstreamNode = edge.getDst();

                    // should I visit this node or is it not within
                    // the boundary ancestor?
                    if (withinStream != null
                        && !downstreamNode.hasAncestor(withinStream))
                        continue;

                    // visit the edge                        
                    if (visitEdges)
                        resultNodesNEdges.insert(edge);

                    // if I've visited the node already once, there's no point
                    // in visiting it again!
                    if (!nodesVisited
                        .find(downstreamNode)
                        .equals(lastNodeVisited))
                        continue;
                    nodesVisited.insert(downstreamNode);
                    nodesToExplore.pushBack(downstreamNode);

                    // visit the node
                    if (visitNodes)
                        resultNodesNEdges.insert(downstreamNode);
                }
            }
        }

        return resultNodesNEdges;
    }
}
