package at.dms.kjc.flatgraph;

import at.dms.kjc.flatgraph.FlatNode;
//import at.dms.util.IRPrinter;
//import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.*;

/**
 * A representation of a FlatGraph as a collection of StaticStreamGraph. 
 *
 * This class represents the entire stream graph of the application we are
 * compiling. It is composed of StaticStreamGraphs.
 * 
 */

public class StreamGraph {
    /** The toplevel stream container.
     * Used as starting point for creating subgraphs
     * but may later become obsolete, not updated as SSG's change.
     */
    private FlatNode topLevelFlatNode;

    /** A list of all the static sub graphs * */
    protected StaticStreamGraph[] staticSubGraphs;

    /** the entry point static subgraph * */
    private StaticStreamGraph topLevel;

    /** map of flat nodes to parent ssg.
     * <br/>this needs to be updated whenever a node is added or deleted. */
    public HashMap<FlatNode,StaticStreamGraph> parentMap;

    /**
     * Create the static stream graph for the application that has <pre>top</pre> as the
     * top level FlatNode.
     * @param top: the entry point of the FlatGraph, used to start the first StaticStreamGraph.
     */
    public StreamGraph(FlatNode top) {
        this.topLevelFlatNode = top;
        parentMap = new HashMap<FlatNode,StaticStreamGraph>();
    }

    /**
     * Use in place of "new StaticStreamGraph" for subclassing.
     * 
     * A subclass of StreamGraph may refer to a subclass of StaticStreamGraph.
     * If we just used "new StaticStreamGraph" in this class we would
     * only be making StaticStreamGraph's of the type in this package.
     * 
     * 
     * @param sg       a StreamGraph
     * @param realTop  the top node
     * @return         a new StaticStreamGraph
     */
    protected StaticStreamGraph new_StaticStreamGraph(StreamGraph sg, FlatNode realTop) {
        return new StaticStreamGraph(sg,realTop);
    }

    
     /**
     * This method creates the static subgraphs of the StreamGraph by cutting
     * the stream graph at dynamic rate boundaries.
     * <br/>
     * The top level node may either be a filter (which would have no peeks or pops
     * if it is really the top level), or a 0-weight splitter introducing independent
     * parallel threads that are eventually joined, or a 2-way joiner with 0 weight on 
     * edge 0 for a top-level feedback loop (if the feedback loop really is at top level).
     */

    public void createStaticStreamGraphs() {
        // flat nodes at a dynamic rate boundary that may constitute the
        // beginning of a new ssg
        LinkedList<FlatNode> dynamicBoundary = new LinkedList<FlatNode>();
        HashSet<FlatNode> visited = new HashSet<FlatNode>();
        // add the toplevel to the dynamicboundary list
        dynamicBoundary.add(topLevelFlatNode);

        // create the ssgs list
        LinkedList<StaticStreamGraph> ssgs = new LinkedList<StaticStreamGraph>();

        while (!dynamicBoundary.isEmpty()) {
            FlatNode top = dynamicBoundary.remove(0);
            // we don't want to create a new SSG for something we have already
            // added
            assert !visited.contains(top);
            // dynamic boundaries can only be filters!  FALSE!
            // the top of the program is a dynamic boundary.
            // the program can start with a filter, a non-0 weight splitter
            // or a 2-way joiner (for top-level feedbackloop).
            //assert top.isTopFilter() || top.isNullSplitter():
            //    "Error: Toplevel node for a static subgraph is not filter or null splitter!";
            StaticStreamGraph ssg = this.new_StaticStreamGraph(this, top);
            // set toplevel;
            if (topLevel == null)
                topLevel = ssg;
            ssgs.add(ssg);
            // search downstream for what should be added to this SSG
            searchDownstream(top, ssg, visited, dynamicBoundary);
        }

        // set up the array of sub graphs
        staticSubGraphs = ssgs
            .toArray(new StaticStreamGraph[ssgs.size()]);

        // clean up the flat graph so that is can be converted to an SIR
        // get prevs and nexts to match up.
        for (int i = 0; i < staticSubGraphs.length; i++)
            staticSubGraphs[i].cleanUp();

        // create prevSSGs, nextSSGs data structures
        for (int i = 0; i < staticSubGraphs.length; i++)
            staticSubGraphs[i].prevAndNextSSGs();
        
        // create the inter-SSG connections of the graph,
        // that is inputSSGEdges, outputSSGEdges.
        for (int i = 0; i < staticSubGraphs.length; i++)
            staticSubGraphs[i].connect();

        // after we have created the ssg, convert their
        // flatgraphs to SIR graphs
        for (int i = 0; i < staticSubGraphs.length; i++) {
            staticSubGraphs[i].createSIRGraph();
            //staticSubGraphs[i].dumpFlatGraph();
        }
    }
    
    /**
     * recursive function to cut the graph into ssgs, it searchs downstream
     * starting at <pre>current</pre> and adds nodes to <pre>ssg</pre> that are connected by
     * static rate channels or it remembers entry points to new SSGs in
     * <pre>dynamicBoundary</pre>
     * @param current: the node currently being visited (May have rated munged if at dynamic boundary)
     * @param ssg: the StaticStreamGraph being built (updated as nodes are added)
     * @param visited: the set of FlatNode's already visited by this (updated per node)
     * @param dynamicBoundary: the unvisited nodes across a dynamic rate boundary (updated if such a node found)
     */
    private void searchDownstream(FlatNode current, StaticStreamGraph ssg,
                                  HashSet<FlatNode> visited, List<FlatNode> dynamicBoundary) {
        assert current.ways == current.getEdges().length;

        // we may have already seen this node before, if so just exit
        if (visited.contains(current)) {
            return;
        }
        // record that it has been visited...
        visited.add(current);
        // current has not been added yet
        ssg.addFlatNode(current);

        // if this flatnode is a filter, check if it has dynamic input
        if (current.isFilter()) {

            SIRFilter filter = (SIRFilter) current.contents;
            // if this filter has dynamic output and there is a downstream
            // neighbor, it will begin
            // a new SSG, so add it to dynamicBoundary, but only if we have not
            // seen the node yet: We way have visited it in an upstream walk
            if (filter.getPush().isDynamic()) {
                assert current.ways == 1 && current.getEdges().length == 1
                    && current.getEdges()[0] != null;
                if (!visited.contains(current.getEdges()[0])) {
                    dynamicBoundary.add(current.getEdges()[0]);
                    // set the next field of the ssg for the current flatnode
                    ssg.addNext(current, current.getEdges()[0]);
                    cutGraph(current, current.getEdges()[0]);
                }
                return;
            }
            // if the downstream filter of this filter has dynamic input, then
            // this is a
            // dynamic boundary, so add the downstream to dynamicBoundary to
            // start a new SSG
            // but only if we haven't visited the node yet, we may have visited
            // it in
            // upstream walk...
            if (current.ways > 0 && current.getEdges()[0].isFilter()) {
                assert current.ways == 1 && current.getEdges().length == 1;
                SIRFilter downstream = (SIRFilter) current.getEdges()[0].contents;
                if ((downstream.getPeek().isDynamic() || downstream.getPop()
                     .isDynamic())) {
                    if (!visited.contains(current.getEdges()[0])) {
                        dynamicBoundary.add(current.getEdges()[0]);
                        // set the next field of the ssg for the current
                        // flatnode
                        ssg.addNext(current, current.getEdges()[0]);
                        cutGraph(current, current.getEdges()[0]);
                    }
                    return;
                }
            }
            // otherwise continue the ssg if connected to more
            if (current.getEdges().length > 0) {
                assert current.getEdges()[0] != null;
                searchDownstream(current.getEdges()[0], ssg, visited,
                                 dynamicBoundary);
            }
        } else if (current.isSplitter()) {
            // if a splitter continue downstream search for each way...
            for (int i = 0; i < current.ways; i++) {
                //assert current.edges[i] != null;
                //case of splitting to allow some outside stream to join.
                // usual case.
                if (current.getEdges()[i] == null) continue;
                searchDownstream(current.getEdges()[i], ssg, visited,
                                 dynamicBoundary);
            }
        } else {
            assert current.isJoiner();
            assert current.incoming.length == current.inputs;
            // first search backwards if we haven't seen this joiner and it is
            // not a null joiner. (Presumably this is to take care of case where
            // a splitter had some 0 weights so the edge in the StreamIt graph is 
            // not represented in the FlatGraph, and to get connectivity, we have 
            // to search back from joiner.)
            if (!(((SIRJoiner) current.contents).getType().isNull() || ((SIRJoiner) current.contents)
                  .getSumOfWeights() == 0)) {
                for (int i = 0; i < current.inputs; i++) {
                    assert current.incoming[i] != null;
                    searchUpstream(current.incoming[i], ssg, visited,
                                   dynamicBoundary);
                }
            }
            // now resume the downstream search
            if (current.getEdges().length > 0) {
                assert current.getEdges()[0] != null;
                searchDownstream(current.getEdges()[0], ssg, visited,
                                 dynamicBoundary);
            }
        }
    }

    /**
     * This method cuts the connections from <pre>upstream</pre> to <pre>downstream</pre> in the
     * flatgraph and in the SIR graph, sets the types of the input of
     * <pre>downstream</pre> and output of <pre>upstream</pre> to void, and sets the appropriate
     * rates to 0.
     * <br/>
     * Assume that cuts only occur between filters.
     * <br/>
     * There does not appear to be any way to restore the upstream push rate or the downstream 
     * peek and pop rates, and the edge is removed between the FlatNode's.
     * The edge is between StaticSubgraph's and can be found in the nexts (or prevs) field
     * of the appropriate StaticSubgraph, and the original type (replaced here with void)
     * can be found by calling getInputType / getOutputType for the node in the correct SSG
     * - - assuming that addNexts has been called before the call to cutGraph.
     * @param upstream: Node upstream of boundary, assumed to have 1 outgoing edge.
     * @param downstream: Node downstream of boundary, assumed to have 1 incoming edge.
     */
    private void cutGraph(FlatNode upstream, FlatNode downstream) {
        // System.out.println("*** Cut Graph ***");
        //The upstream has to be a filter or a null joiner,
        //The downstream has to be a null splitter 
        assert (upstream.isFilter() || upstream.isNullJoiner()) && 
                (downstream.isFilter() || (downstream.isNullSplitter()));

        // if node structure changes, try to at least have hook to SIR structure.
        upstream.contents.setAttribute("edge", downstream.contents);
        downstream.contents.setAttribute("incoming",upstream.contents);

        if (upstream.isFilter()) {
            SIRFilter upFilter = (SIRFilter) upstream.contents;
            // store removed data as attributes in upFilter
            upFilter.setAttribute("push",upFilter.getPush());
            upFilter.setAttribute("outtype", upFilter.getOutputType());
            // reset upstream
            upFilter.setPush(new JIntLiteral(0));
            upFilter.setOutputType(CStdType.Void);
            upstream.removeEdges();
        }
        
        // reset downstream
        if (downstream.isFilter()) {
            SIRFilter downFilter = (SIRFilter) downstream.contents;
            // store removed data as attributes in downFilter
            downFilter.setAttribute("pop",downFilter.getPop());
            downFilter.setAttribute("peek",downFilter.getPeek());
            downFilter.setAttribute("intype",downFilter.getInputType());
            downFilter.setPop(new JIntLiteral(0));
            downFilter.setPeek(new JIntLiteral(0));
            downFilter.setInputType(CStdType.Void);
            downstream.removeIncoming();
        }
    }

    /**
     * This method search upstream from <pre>current</pre> for nodes that it should add
     * to <pre>ssg</pre>. It will add nodes that are connected through static rate channels.
     * <pre>dynamicBoundary</pre> stores all the entries for SSGs we haven't constructed
     * yet.
     * <br/>
     * It is necessary in order to ensure a cut across a splitjoin contruct.
     * @param current: the node currently being visited (May have rated munged if at dynamic boundary)
     * @param ssg: the StaticStreamGraph being built (updated as nodes are added)
     * @param visited: the set of FlatNode's already visited by this (updated per node)
     * @param dynamicBoundary: the unvisited nodes across a dynamic rate boundary (updated if such a node found)
      */
    private void searchUpstream(FlatNode current, StaticStreamGraph ssg,
                                HashSet<FlatNode> visited, List<FlatNode> dynamicBoundary) {
        assert current.incoming.length == current.inputs;

        // we have already added this flatnode to an ssg so just make sure and
        // exit
        if (visited.contains(current)) {
            assert !dynamicBoundary.contains(current);
            return;
        }

        // stop at unprocessed dynamic boundaries, but add the boundary node and
        // remove it from the unprocess boundary list
        if (dynamicBoundary.contains(current)) {
            ssg.addTopLevelFlatNode(current);
            visited.add(current);
            dynamicBoundary.remove(current);
            return;
        }

        // we have not seen this flatnode before so add it
        ssg.addFlatNode(current);
        visited.add(current);

        if (current.isFilter()) {
            SIRFilter filter = (SIRFilter) current.contents;
            // if this filter has dynamic input
            if (filter.getPop().isDynamic() || filter.getPeek().isDynamic()) {
                assert current.inputs == 1;
                // cut the graph and add another entry point for the SSG
                ssg.addPrev(current, current.incoming[0]);
                cutGraph(current.incoming[0], current);
                ssg.addTopLevelFlatNode(current);
                return;
            }
            if (current.inputs > 0) {
                assert current.inputs == 1 && current.incoming[0] != null;
                // check if the upstream filter has dynamic output
                // if so return
                if (current.incoming[0].isFilter()) {
                    SIRFilter upstream = (SIRFilter) current.incoming[0].contents;
                    if (upstream.getPush().isDynamic()) {
                        // cut the graph and add another entry point for the SSG
                        ssg.addPrev(current, current.incoming[0]);
                        cutGraph(current.incoming[0], current);
                        ssg.addTopLevelFlatNode(current);
                        return;
                    }
                }
                // if not, continue upstream walk
                searchUpstream(current.incoming[0], ssg, visited,
                               dynamicBoundary);
            }
        } else if (current.isSplitter()) {
            // continue upstream search...
            if (current.inputs > 0) {
                assert current.incoming[0] != null && current.inputs == 1;
                searchUpstream(current.incoming[0], ssg, visited,
                               dynamicBoundary);
                // for feedbackloop splitter, reachable from joiner above, 
                // also need to initiate downstream search since splitter is
                // now visited so would end a downstream search started
                // above it prematurely.
                for (int i = 0; i < current.ways; i++) {
                    if (current.getEdges()[i] != null) {
                        searchDownstream(current.getEdges()[i], ssg, visited,
                                dynamicBoundary);
                    }
                }
            }
        } else {
            assert current.isJoiner();
            // continue upstream search..
            for (int i = 0; i < current.inputs; i++) {
                assert current.incoming[i] != null;
                searchUpstream(current.incoming[i], ssg, visited,
                               dynamicBoundary);
            }
        }
    }

    /***************************************************************************
     * Return true if the source of this stream, <pre>stream</pre> has 
     * dynamic rate input
     * @param stream: 
     * @return true if stream is a filter with dynamic peek or pop rate or pipeline starting with such a filter
     **************************************************************************/
    public boolean dynamicEntry(SIRStream stream) {
        if (stream instanceof SIRFilter) {
            return ((SIRFilter) stream).getPop().isDynamic()
                || ((SIRFilter) stream).getPeek().isDynamic();
        } else if (stream instanceof SIRPipeline) {
            // call dynamicExit on the last element of the pipeline
            return dynamicEntry(((SIRPipeline) stream).get(0));
        } else
            // right now we can never have a dynamic entry except in the above
            // cases
            return false;
    }

    /***************************************************************************
     * Return true if the sink of this stream, <pre>stream</pre> has dynamic rate output
     * @param stream: 
     * @return true if stream is a filter with dynamic push rate or pipeline ending with such a filter
     **************************************************************************/
    public boolean dynamicExit(SIRStream stream) {
        if (stream instanceof SIRFilter) {
            return ((SIRFilter) stream).getPush().isDynamic();
        } else if (stream instanceof SIRPipeline) {
            // call dynamicExit on the last element of the pipeline
            return dynamicExit(((SIRPipeline) stream)
                               .get(((SIRPipeline) stream).size() - 1));
        } else
            // right now we can never have a dynamic exit unless in the above
            // cases
            return false;
    }


    /**
     * Add the mapping node->ssg to the parent map to remember that <pre>ssg</pre> is the
     * parent of <pre>node</pre>.  Should only be called from StaticStreamGraph.
     * @param node:
     * @param ssg:
     */
    public void putParentMap(FlatNode node, StaticStreamGraph ssg) {
        parentMap.put(node, ssg);
    }

    /** get the parent SSG of <pre>node</pre> 
     * @param node for which to find the contaiing StaticStreamGraph
     * @return a StaticStreamGraph containing the node or null if none.
     */
    public StaticStreamGraph getParentSSG(FlatNode node) {
        assert parentMap.containsKey(node) : node;
        return parentMap.get(node);
    }

    /** return the array of SSGs of this Stream Graph in no particular order
     * @return  the array of SSGs of this Stream Graph in no particular order
     */
    public StaticStreamGraph[] getStaticSubGraphs() {
        return staticSubGraphs;
    }

    /** get the toplevel (source) SSG
     @return the SSG originally passed to the StreamGraph constructor
     */
    public StaticStreamGraph getTopLevel() {
        return topLevel;
    }

//    public void setTopLevel(StaticStreamGraph ssg) {
//        this.topLevel = ssg;
//    }
    
    /** print out some stats on each SSG to STDOUT * */
    public void dumpStaticStreamGraph() {
        StaticStreamGraph current = topLevel;
        for (int i = 0; i < staticSubGraphs.length; i++) {
            current = staticSubGraphs[i];
            System.out.println("Static Stream Graph " + i + ":  " + current.toString());
            System.out.println("  Type signature: " + current.getTopLevelSIR().getInputType() + " -> " + current.getTopLevelSIR().getOutputType() + ".");
            System.out.println("  " + 
                               (dynamicEntry(current.getTopLevelSIR()) ? "Dynamic" : "Static") + " input rate, " +
                               (dynamicExit(current.getTopLevelSIR()) ? "dynamic" : "static") + " output rate.");
            StreamItDot.printGraph(current.getTopLevelSIR(), current
                                   .getTopLevelSIR().getIdent()
                                   + ".dot");
        }
    }

//    /** create a stream graph with only one filter (thus one SSG) * */
//    public static StreamGraph constructStreamGraph(SIRFilter filter) {
//        return constructStreamGraph(new FlatNode(filter));
//    }

//    /**
//     * create a stream graph with only one filter (thus one SSG), it's not laid
//     * out yet*
//     */
//    public static StreamGraph constructStreamGraph(FlatNode node) {
//        assert node.isFilter();
//
//        StreamGraph streamGraph = new_StreamGraph(node);
//        streamGraph.createStaticStreamGraphs();
//        return streamGraph;
//    }

    
    /**
     * The current version pastes together the topLevelSIR graphs
     * for all StaticStreamGraphs.  Be sure that you do not want to
     * continue to use the individual graphs.
     */
    public SIRStream recreateSIR() {
        topLevel.accept(new StreamGraphVisitor(){
            public void visitStaticStreamGraph(StaticStreamGraph ssg) {
                ssg.reconnectOutputs();
            }
        }, null,true);
        return (new FlatGraphToSIR(topLevel.topLevel)).getTopLevelSIR();

    }
}
