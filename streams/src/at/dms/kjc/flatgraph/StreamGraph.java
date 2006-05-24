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
    /** The toplevel stream container * */
    private FlatNode topLevelFlatNode;

    /** A list of all the static sub graphs * */
    private StaticStreamGraph[] staticSubGraphs;

    /** the entry point static subgraph * */
    private StaticStreamGraph topLevel;

    /** map of flat nodes to parent ssg * */
    public HashMap<FlatNode,StaticStreamGraph> parentMap;

    /**
     * Create the static stream graph for the application that has <pre>top</pre> as the
     * top level FlatNode.
     */
    public StreamGraph(FlatNode top) {
        this.topLevelFlatNode = top;
        parentMap = new HashMap<FlatNode,StaticStreamGraph>();
    }

    /**
     * This method creates the static subgraphs of the StreamGraph by cutting
     * the stream graph at dynamic rate boundaries, right now the top level flat
     * node has to be a filter.
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
            FlatNode top = (FlatNode) dynamicBoundary.remove(0);
            // we don't want to create a new SSG for something we have already
            // added
            assert !visited.contains(top);
            // dynamic boundaries can only be filters!
            assert top.isFilter() || top.isNullSplitter() : 
                "Error: Toplevel node for a static subgraph is not filter or null splitter!";
            StaticStreamGraph ssg = new StaticStreamGraph(this, top);
            // set topleve;
            if (topLevel == null)
                topLevel = ssg;
            ssgs.add(ssg);
            // search downstream for what should be added to this SSG
            searchDownstream(top, ssg, visited, dynamicBoundary);
        }

        // set up the array of sub graphs
        staticSubGraphs = (StaticStreamGraph[]) ssgs
            .toArray(new StaticStreamGraph[0]);

        // clean up the flat graph so that is can be converted to an SIR
        for (int i = 0; i < staticSubGraphs.length; i++)
            staticSubGraphs[i].cleanUp();

        // create the inter-SSG connections of the graph!!!
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
     * static rate channels or it remember entry points to new SSGs in
     * <pre>dynamicBoundary</pre>
     */
    private void searchDownstream(FlatNode current, StaticStreamGraph ssg,
                                  HashSet<FlatNode> visited, List<FlatNode> dynamicBoundary) {
        assert current.ways == current.edges.length;

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
            // if this filter has dynamic output and their is a downstream
            // neighbor, it will begin
            // a new SSG, so add it to dynamicBoundary, but only if we have not
            // seen the node yet
            // we way have visited it in an upstream walk
            if (filter.getPush().isDynamic()) {
                assert current.ways == 1 && current.edges.length == 1
                    && current.edges[0] != null;
                if (!visited.contains(current.edges[0])) {
                    dynamicBoundary.add(current.edges[0]);
                    // set the next field of the ssg for the current flatnode
                    ssg.addNext(current, current.edges[0]);
                    cutGraph(current, current.edges[0]);
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
            if (current.ways > 0 && current.edges[0].isFilter()) {
                assert current.ways == 1 && current.edges.length == 1;
                SIRFilter downstream = (SIRFilter) current.edges[0].contents;
                if ((downstream.getPeek().isDynamic() || downstream.getPop()
                     .isDynamic())) {
                    if (!visited.contains(current.edges[0])) {
                        dynamicBoundary.add(current.edges[0]);
                        // set the next field of the ssg for the current
                        // flatnode
                        ssg.addNext(current, current.edges[0]);
                        cutGraph(current, current.edges[0]);
                    }
                    return;
                }
            }
            // otherwise continue the ssg if connected to more
            if (current.edges.length > 0) {
                assert current.edges[0] != null;
                searchDownstream(current.edges[0], ssg, visited,
                                 dynamicBoundary);
            }
        } else if (current.isSplitter()) {
            // if a aplitter continue downstream search for each way...
            for (int i = 0; i < current.ways; i++) {
                assert current.edges[i] != null;
                searchDownstream(current.edges[i], ssg, visited,
                                 dynamicBoundary);
            }
        } else {
            assert current.isJoiner();
            assert current.incoming.length == current.inputs;
            // first search backwards if we haven't seen this joiner and it is
            // not a null joiner
            if (!(((SIRJoiner) current.contents).getType().isNull() || ((SIRJoiner) current.contents)
                  .getSumOfWeights() == 0)) {
                for (int i = 0; i < current.inputs; i++) {
                    assert current.incoming[i] != null;
                    searchUpstream(current.incoming[i], ssg, visited,
                                   dynamicBoundary);
                }
            }
            // now resume the downstream search
            if (current.edges.length > 0) {
                assert current.edges[0] != null;
                searchDownstream(current.edges[0], ssg, visited,
                                 dynamicBoundary);
            }
        }
    }

    /**
     * This method cuts the connections from <pre>upstream</pre> to <pre>downstream</pre> in the
     * flatgraph and in the SIR graph, sets the types of the input of
     * <pre>downstream</pre> and output of <pre>upstream</pre> to void, and sets the appropriate
     * rates to 0
     */
    private void cutGraph(FlatNode upstream, FlatNode downstream) {
        // System.out.println("*** Cut Graph ***");
        //The upstream has to be a filter or a null joiner,
        //The downstream has to be a null splitter 
        assert (upstream.isFilter() || upstream.isNullJoiner()) && 
                (downstream.isFilter() || (downstream.isNullSplitter()));
      
        if (upstream.isFilter()) {
            SIRFilter upFilter = (SIRFilter) upstream.contents;
            // reset upstream
            upFilter.setPush(new JIntLiteral(0));
            upFilter.setOutputType(CStdType.Void);
            upstream.removeEdges();
        }
        
        // reset downstream
        if (downstream.isFilter()) {
            SIRFilter downFilter = (SIRFilter) downstream.contents;
            downFilter.setPop(new JIntLiteral(0));
            downFilter.setPeek(new JIntLiteral(0));
            downFilter.setInputType(CStdType.Void);
            downstream.removeIncoming();
        }
    }

    /**
     * This method search upstream from <pre>current</pre> for nodes that it should add
     * to <pre>ssg</pre>. It will add nodes that are connected thru static rate channels.
     * <pre>dynamicBoundary</pre> stores all the entries for SSGs we haven't constructed
     * yet
     * 
     * It is necessary in order to ensure a cut across a splitjoin contruct
     */
    private void searchUpstream(FlatNode current, StaticStreamGraph ssg,
                                HashSet<FlatNode> visited, List dynamicBoundary) {
        assert current.incoming.length == current.inputs;

        // we have already added this flatnode to an ssg so just make sure and
        // exit
        if (visited.contains(current)) {
            assert !dynamicBoundary.contains(current);
            return;
        }

        // stop at unprocessed dynamic boundaries, but add the boundary node and
        // remove it
        // from the unprocess boundary list
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
     **************************************************************************/
    public boolean dynamicEntry(SIRStream stream) {
        if (stream instanceof SIRFilter) {
            return ((SIRFilter) stream).getPop().isDynamic()
                || ((SIRFilter) stream).getPeek().isDynamic();
        } else if (stream instanceof SIRPipeline) {
            // call dynamicExit on the last element of the pipeline
            return dynamicEntry(((SIRPipeline) stream).get(0));
        } else
            // right now we can never have a dynamic entryunless in the above
            // cases
            return false;
    }

    /***************************************************************************
     * Return true if the sink of this stream, <pre>stream</pre> has dynamic rate output
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
     * parent of <pre>node</pre>
     */
    public void putParentMap(FlatNode node, StaticStreamGraph ssg) {
        parentMap.put(node, ssg);
    }

    /** get the parent SSG of <pre>node</pre> * */
    public StaticStreamGraph getParentSSG(FlatNode node) {
        assert parentMap.containsKey(node) : node;
        return (StaticStreamGraph) parentMap.get(node);
    }

    /** return the array of SSGs of this Stream Graph in no particular order * */
    public StaticStreamGraph[] getStaticSubGraphs() {
        return staticSubGraphs;
    }

    /** get the toplevel (source) SSG * */
    public StaticStreamGraph getTopLevel() {
        return topLevel;
    }

    /** print out some stats on each SSG to STDOUT * */
    public void dumpStaticStreamGraph() {
        StaticStreamGraph current = topLevel;
        for (int i = 0; i < staticSubGraphs.length; i++) {
            current = staticSubGraphs[i];
            System.out.println("******* StaticStreamGraph ********");
            System.out.println("Dynamic rate input = "
                               + dynamicEntry(current.getTopLevelSIR()));
            System.out.println("InputType = "
                               + current.getTopLevelSIR().getInputType());
            System.out.println(current.toString());
            System.out.println("OutputType = "
                               + current.getTopLevelSIR().getOutputType());
            System.out.println("Dynamic rate output = "
                               + dynamicExit(current.getTopLevelSIR()));
            StreamItDot.printGraph(current.getTopLevelSIR(), current
                                   .getTopLevelSIR().getIdent()
                                   + ".dot");
            System.out.println("**********************************");
        }
    }

    /** create a stream graph with only one filter (thus one SSG) * */
    public static StreamGraph constructStreamGraph(SIRFilter filter) {
        return constructStreamGraph(new FlatNode(filter));
    }

    /**
     * create a stream graph with only one filter (thus one SSG), it's not laid
     * out yet*
     */
    public static StreamGraph constructStreamGraph(FlatNode node) {
        assert node.isFilter();

        StreamGraph streamGraph = new StreamGraph(node);
        streamGraph.createStaticStreamGraphs();
        return streamGraph;
    }

}
