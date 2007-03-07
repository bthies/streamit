package at.dms.kjc.flatgraph;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
//import at.dms.util.IRPrinter;
//import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import java.util.*;
import at.dms.kjc.common.CommonUtils;


/**
 * A representation of a portion of a FlatGraph where all comunication is
 * static rate.
 *
 * A StaticStreamGraph represents a subgraph of the application's StreamGraph
 * where communication within the SSG is over static rate channels. The
 * input/output (if either exists) of an SSG is dynamic, but the sources and
 * sinks have their input/output rates zeroed, repectively.
 * 
 * A SSG can be modified and replaced in the graph structure by calling 
 * setTopLevelSIR with the modified SSG.  Modification must preserve the
 * following: it must not change the number of inputs and outputs of the SSG.
 * 
 * If a SSG has multiple inputs (resp. outputs), there must be a round-robin 
 * splitter (resp. joiner) with 0 edge weights creating a single root 
 * (resp. exit) for the SSG and connecting to the actual inputs (resp. outputs).
 * 
 */

public class StaticStreamGraph {
    /** These Hashmaps map flatnode -> flatnode.
     * <br/>prevs maps an input node of this SSG to the corresponding output node of a upstream SSG
     * <br/>nexts maps an output node of this SSG to the corresponding input node of a downstream SSG
     * <br/>Note: only used in building graph.  no longer valid after setTopLevelSIR.
     * The sizes are used as a sanity check that operations on the ssh have not changed number of
     * incoming or outgoing edges, but contents should not be referenced after initial graph build.
     */
    // prevs, and nexts must survive the first graph reorganization.
    // take advantage of the fact that dynamic rate cuts are only
    // allowed between filters, which are not changed in reorganizing a SIR graph of Flat graph.
    protected HashMap<SIRFilter,SIRFilter> prevs;
    protected HashMap<SIRFilter,SIRFilter> nexts;

    /** SSGs with edges to this SSG.  Assumed immutible after SSG construction */
    private List<StaticStreamGraph> prevSSGs;
    /** SSGs that this SSG has edges to.   Assumed immutible after SSG construction */
    protected List<StaticStreamGraph> nextSSGs;

    // All of the following arrays have the same length and are filled in
    // in the same order:
    // inputs, inputSSGEdges, inputTypes, peekRates, popRates.
    
    
    // All of the following arrays have the same length and are filled in
    // in the same order:
    // outputs, outputSSGEdges, outputTypes, pushRates.
    
    /** The inter-SSG communication edges of this SSG, both incoming and outgoing. 
     *  In sync with prevs, nexts, call updateSSGEdges when prevs, nexts change. */
    private SSGEdge[] inputSSGEdges, outputSSGEdges;

    /** arrays representing the inputs and output of this ssg from top to bottom (left to right)... 
     * <br/>Note: very delicate if graph structure changes: remember to call updateIOArrays: done from setTopLevelSIR.
     */
    protected FlatNode[] inputs;
    protected FlatNode[] outputs;

    /** the output type of the ssg output
     * <br/>Note: ordered in sync with outputs; very delicate if graph structure changes.
     */
    private CType[] outputTypes;
    private JExpression[] pushRates;
    
    /**
     * Input parameters to restore when done.
     */
    private CType[] inputTypes;
    private JExpression[] peekRates;
    private JExpression[] popRates;

    /** the top level SIR node.
     * 
     * Used in debugging and scheduling from subclasses.
     */
    protected SIRStream topLevelSIR;

    /** the top level FlatNode
     *  
     * Used in debugging from subclasses.
     */
    protected FlatNode topLevel;

    /** the graph flattener used to convert SIR to FlatGraph
     * 
     */
    protected GraphFlattener graphFlattener;

    private FlatNode bottomLevel;

    // set of all the flat nodes of this graph...
    protected LinkedList<FlatNode> flatNodes;

    // used to construct a valid SIR graph
    private int splitterBalance;

    // the id of this SSG
    private static int nextID = 0;

    public int id;

    // the parent stream graph
    private StreamGraph streamGraph;
    
    // for delaying error exits found in inner classes
    //final boolean[] deferred_error = {false};

    protected StaticStreamGraph() {}
    /**
     * create a static stream graph with realTop as the first node that the
     * implicit splitter points to
     * @param StreamGraph sg: the StreamGraph that the created StaticStreamGraph will be part of.
     * @param realTop: the top node for the StaticStreamGraph (there is an internal fake top node)
     */
    public StaticStreamGraph(StreamGraph sg, FlatNode realTop) {
        this.streamGraph = sg;
        id = nextID++;
        this.prevs = new HashMap<SIRFilter,SIRFilter>();
        this.nexts = new HashMap<SIRFilter,SIRFilter>();
        this.prevSSGs = new LinkedList<StaticStreamGraph>();
        this.nextSSGs = new LinkedList<StaticStreamGraph>();
        // flatNodes = new HashSet();
        flatNodes = new LinkedList<FlatNode>();
        outputs = new FlatNode[0];
        inputs = new FlatNode[0];

        // a static stream graph always starts with a splitter, we
        // remove it later if ways == 1
        this.topLevel = new FlatNode(SIRSplitter.create(null,
                                                        SIRSplitType.NULL, 0));

        addTopLevelFlatNode(realTop);
    }

    /** add a source node to this SSG and add it to the toplevel.
     * <br/>May be called from constructor, or may be called from StreamGraph on
     * encountering an upstream connection to a dynamic node.
     * @param node node to add.
     */
    void addTopLevelFlatNode(FlatNode node) {
        // System.out.println("AddTopLevelNode " + node + " to " + id) ;
        assert node.isFilter() || node.isNullSplitter() || node.isFeedbackJoiner();
        
        //nothing to do for a null splitter: there is no way to
        // go from a null splitter...
        if (node.isNullSplitter()) {
            topLevel = node;
            return;
        }
        
        if (node.isFeedbackJoiner()) {
            //SIRJoiner joiner = (SIRJoiner)node.contents;
            // checks on flatgraph: should have set no input to body.
            assert node.inputs == 2 && node.incoming[0] == null;
            node.currentIncoming = 0;  // attach  null splitter to null incoming edge.
        } else if (node.isFilter()) {
            // checks removed.
        }
        // add the flatnodes to various structures
        addFlatNode(node);

        // create a new toplevel with one extra connection
        FlatNode oldTopLevel = topLevel;
        flatNodes.remove(oldTopLevel);

        topLevel = new FlatNode(SIRSplitter.create(null, SIRSplitType.NULL,
                                                   oldTopLevel.ways + 1));

        addFlatNode(topLevel);

        // add edges from old top level
        assert oldTopLevel.ways == oldTopLevel.getEdges().length
            && oldTopLevel.ways == ((SIRSplitter) oldTopLevel.contents)
            .getWays();
        for (int i = 0; i < oldTopLevel.ways; i++) {
            FlatNode current = oldTopLevel.getEdges()[i];
            // first remove the back edge to the old top level
            current.removeBackEdge(oldTopLevel);
            // now add new edges (forward and back)
            FlatNode.addEdges(topLevel, current);
        }

        // add new edge
        FlatNode.addEdges(topLevel, node);
    }

    /** when constructing this SSG, add <b>node</b> to it 
     * @param FlatNode node: node to add to flatNodes in this subgraph
     */
    void addFlatNode(FlatNode node) {
        // System.out.println("Adding " + node + " to " + id);
        flatNodes.add(node);
        streamGraph.putParentMap(node, this);
        // System.out.println("Adding to parent map " + node);
    }

    /***************************************************************************
     * Remove toplevel splitter if not needed, and perform some other checks
     * and fixes on the SSG: Only for building SSGs from {@link StreamGraph}.
     * At this point we assume that the SSG has a correct collection of nodes
     * and a correct entry point.  We also assume that there is a set of SSGs 
     * partitioning the reachable nodes in the flatgraph.
     **************************************************************************/

    void cleanUp() {
        assert topLevel.isSplitter();
        assert ((SIRSplitter) topLevel.contents).getWays() > 0;
        final boolean[] deferred_errors = {false};
        // check that we have cuts in the correct places
        // remove edges that connect to flatnodes not in this ssg
        topLevel.accept(new FlatVisitor() {
            public void visitNode(FlatNode node) {
                if (!flatNodes.contains(node))
                    return;

                if (node.inputs > 0) {
                    for (int i = 0; i < node.incoming.length; i++) {
                        assert node.incoming[i] != null;
                        if (!flatNodes.contains(node.incoming[i])) {
                            node.removeBackEdge(node.incoming[i]);
                        }
                    }
                }
                if (node.ways > 0) {
                    for (int i = 0; i < node.getEdges().length; i++) {
                        if (node.getEdges()[i] == null) {
                            assert i == 0
                                    && (node.contents instanceof SIRSplitter || node.contents instanceof SIRJoiner)
                                    && node.contents.getParent() instanceof SIRFeedbackLoop;
                        } else if (!(flatNodes.contains(node.getEdges()[i]))) {
                            node.removeForwardEdge(node.getEdges()[i]);
                        }
                    }
                }
            }
        }, null, true);

        // make sure all nodes have correct number of connections...
        // seems to be all checks -- except for fixing number of joiner ways.
        topLevel.accept(new FlatVisitor() {
                public void visitNode(FlatNode node) {
                    if (node.isFilter()) {
                        // checks removed... 
                } else if (node.isJoiner()) {
                        SIRJoiner joiner = (SIRJoiner) node.contents;
                        if (joiner.getWays() != node.inputs) {
                            // System.out.println(joiner.getWays() + " != " +
                            // node.inputs);
                            assert joiner.getSumOfWeights() == 0;
                            // create a new null joiner
                            SIRJoiner newJoiner = SIRJoiner.create(null,
                                                                   SIRJoinType.NULL, node.inputs);
                            node.contents = newJoiner;
                        }
                    } else {
                        SIRSplitter splitter = (SIRSplitter) node.contents;
                        if (splitter.getWays() != node.ways) {
                            int [] weights = splitter.getWeights();
                            int nonzero_weights = 0;
                            for (int i = 0; i < weights.length; i++) {
                                if (weights[i] > 0) nonzero_weights++;
                            }
                            int nonnull_ways = 0;
                            for (int i = 0; i < node.ways; i++) {
                                if (node.getEdges()[i] != null) nonnull_ways++;
                            }
                            assert nonzero_weights == nonnull_ways : "Invalid Splitter: "
                                + node
                                + " "
                                + nonzero_weights + " (" + splitter.getWays() + ") "
                                + " != "
                                + nonnull_ways + " (" + node.ways + ") ";
                        }
                    }
                }
            }, null, true);
        
        assert ! deferred_errors[0] : "Error(s) found in dynamic rate boundary positioning." ;

        // check if the number of splitters and joiners is balanced (==)
        splitterBalance = 0;

        topLevel.accept(new FlatVisitor() {
                public void visitNode(FlatNode node) {
                    if (node.isJoiner()) {
                        splitterBalance--;
                    } else if (node.isSplitter()) {
                        splitterBalance++;
                    }
                }
            }, null, true);

        if (splitterBalance != 0)
        {
        
        // remove unbalanced splitter because it is not needed!!!
        if (((SIRSplitter) topLevel.contents).getWays() == 1) {
            FlatNode oldTopLevel = topLevel;
            assert topLevel.ways == 1 && topLevel.getEdges().length == 1
                && topLevel.getEdges()[0] != null;
            topLevel = topLevel.getEdges()[0];
            if (topLevel.isFeedbackJoiner()) {
                // old top level was feedbackloop joiner
                // restore its old configuration with
                // null first incoming edge rather than
                // removing the edge
                topLevel.incoming[0] = null;
                topLevel.currentIncoming= 2;
            } else {
                topLevel.removeBackEdge(oldTopLevel);
            }
            flatNodes.remove(oldTopLevel);
        }
        }
        
        // set up the downstream SSG's prevs hashmap for this SSG's nexts
        for (SIRFilter inThis : nexts.keySet()) {
            SIRFilter inNext = nexts.get(inThis);
            assert inNext != null : inThis.toString();
            streamGraph.getParentSSGbyFilter(inNext).addPrev(
                    streamGraph.filterToFlatNode(inNext),
                    streamGraph.filterToFlatNode(inThis));
        }


        // set up the upstream SSG's nexts hashmap for this SSG's prevs
        for (SIRFilter inThis : prevs.keySet()) {
            SIRFilter inPrev = prevs.get(inThis);
            assert inPrev != null : inThis.toString();
            streamGraph.getParentSSGbyFilter(inPrev).addNext(
                    streamGraph.filterToFlatNode(inPrev),
                    streamGraph.filterToFlatNode(inThis));
        }
        
    }

    /**
     * Build nextSSGs, prevSSGs: Only for building SSGs from {@link StreamGraph}.
     * Cache types and rates across dynamic rate edges
     * and set types across dynamic edges to void, 
     * rates across dynamic edges to 0 for schedulers, partitioners.
     * Call after {@link #cleanUp()} run on all ssgs.
     */
    void prevAndNextSSGs() {
        // build the prevSSGs and nextSSGs list, the upstream and downstream
        // SSGs
        for (SIRFilter aNext : nexts.values()) {
            StaticStreamGraph ssg = streamGraph
                .getParentSSGbyFilter(aNext);
            if (!nextSSGs.contains(ssg)) {
                nextSSGs.add(ssg); }
            
        }

        for (SIRFilter aPrev : prevs.values()) {
            StaticStreamGraph ssg = streamGraph
                .getParentSSGbyFilter(aPrev);
            if (!prevSSGs.contains(ssg)) {
                prevSSGs.add(ssg); }
        }
        

        // getPrevs, getNexts order of setting up inputs / outputs
        // is not the order that we want for the inputs / outputs fields.
        // Instead, set the order that will be used by setTopLevelSIR().
        
        outputs = new FlatNode[nexts.size()];
        inputs = new FlatNode[prevs.size()];
        
        
        // recreate the graph putting joiner at bottom if more than one output.
        // then calculate inputs and outputs from the splitter and joiner in the
        // recreated graph.
        setTopLevelSIRInitial((new FlatGraphToSIR(topLevel)).getTopLevelSIR());
        

        outputTypes = new CType[outputs.length];
        pushRates = new JExpression[outputs.length];
        
        SIRDynamicRateManager.pushIdentityPolicy();
        for (int i = 0; i < outputs.length; i++)  {
            if (outputs[i].contents instanceof SIRFilter) {
                SIRFilter f = (SIRFilter)outputs[i].contents;
                outputTypes[i] = f.getOutputType();
                f.setOutputType(CStdType.Void);
                pushRates[i] = f.getPush();
                f.setPush(0);
            }
        }
        
        inputTypes = new CType[inputs.length];
        popRates = new JExpression[inputs.length];
        peekRates = new JExpression[inputs.length];
        
        for (int i = 0; i < inputs.length; i++)  {
            if (inputs[i].contents instanceof SIRFilter) {
                SIRFilter f = (SIRFilter)inputs[i].contents;
                inputTypes[i] = f.getInputType();
                f.setInputType(CStdType.Void);
                popRates[i] = f.getPop();
                f.setPop(0);
                peekRates[i] = f.getPeek();
                f.setPeek(0);
            }
        }
       
        SIRDynamicRateManager.popPolicy();
     }
    
    
    /**
     * After SSG construction is complete, create the inter-SSG connections for
     * this SSG: Only for building SSGs from {@link StreamGraph}.
     */
  
  void connect() {
        // build the inputSSGEdges and outputSSGEdges arrays
        inputSSGEdges = new SSGEdge[prevs.size()];
        outputSSGEdges = new SSGEdge[nexts.size()];

        for (int i = 0; i < outputSSGEdges.length; i++) {
            SIRFilter dest = nexts.get(outputs[i].contents);
            assert dest != null;
            StaticStreamGraph input = streamGraph.getParentSSGbyFilter(dest);

            outputSSGEdges[i] = 
                SSGEdge.createSSGEdge(this, input, i, input.getInputNum(streamGraph.filterToFlatNode(dest)));
            outputSSGEdges[i].setDownstreamNode(streamGraph.filterToFlatNode(dest));
        }

        for (int i = 0; i < inputSSGEdges.length; i++) {
            SIRFilter src = prevs.get(inputs[i].contents);
            assert src != null;
            StaticStreamGraph upstream = streamGraph.getParentSSGbyFilter(src);

            inputSSGEdges[i] = 
                SSGEdge.createSSGEdge(upstream, this, upstream.getOutputNum(streamGraph.filterToFlatNode(src)), i);
            inputSSGEdges[i].setUpstreamNode(streamGraph.filterToFlatNode(src));
        }

        // fix outputSSGEdges[i].upstreamNode, inputSSGEdges[i].downstreamNode
        // to reflect the output nodes / input nodes of this SSG.
        updateSSGEdges();

        for (int i = 0; i < outputs.length; i++)
            assert outputs[i] != null : this.toString() + " has null output "
                + i;

        for (int i = 0; i < inputs.length; i++)
            assert inputs[i] != null : this.toString() + " has null input " + i;

    }


    /**
     * after we have changed the flatgraph and updated outputs[] and inputs[],
     * update the SSGEdges to reflect the new input and output FlatNodes
     * 
     * Remember that the number of inputs and outputs has to remain the same
     */
    private void updateSSGEdges() {
        for (int i = 0; i < outputs.length; i++) {
            outputSSGEdges[i].setUpstreamNode(outputs[i]);
        }

        for (int i = 0; i < inputs.length; i++) {
            inputSSGEdges[i].setDownstreamNode(inputs[i]);
        }

    }

    /**
     * given an output (sink) for this SSG, get the output number, index to
     * outputs[] and outputSSGEdges[]
     */
    public int getOutputNum(FlatNode node) {
        for (int i = 0; i < outputs.length; i++) {
            if (outputs[i] == node)
                return i;
        }
        assert false : node + " not an output";
        return -1;
    }

    /**
     * given an input, source, for this SSG, get the input number, index to
     * inputs[] and inputSSGEdges[]
     */
    public int getInputNum(FlatNode node) {
        System.out.println(inputs.length);
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] == node)
                return i;
        }
        assert false : node + " not an input";
        return -1;
    }

    /**
     * After the underlying flatgraph has changed, we have to update the <b>inputs</b>
     * and <b>outputs</b> arrays.  Invariant: throughout the life of the SSG, the
     * number of inputs and number of outputs does not change.  Furthermore, the 
     * types passed across the dynamic boundary and the rates atthe dynamic boundary
     * do not change.
     */
    private void updateIOArrays() {
        
        // set input[] to store the new inputs (sources) for this SSG
        if (prevs.size() > 0) {
            assert topLevel != null;
            // if a filter, then just set the inputs[0] to it
            if (topLevel.isFilter()) {
                assert prevs.size() == 1 && inputs.length == 1;

//                System.err.println("inputs[0] changing from  " 
//                        + ((inputs[0] != null) ?  (inputs[0].contents.getName() + " (" 
//                        + inputs[0].contents.getIdent() + ")") : ""));
//                System.err.println("  to "
//                        + topLevel.contents.getName() + " (" 
//                        + topLevel.contents.getIdent() + ")");

                inputs[0] = topLevel;
            } else if (topLevel.isSplitter()) {
                // if a splitter, then set the input[] to the direct downstream
                // node of the splitter...
                assert prevs.size() == topLevel.getEdges().length
                    && topLevel.getEdges().length == ((SIRSplitter) topLevel.contents)
                    .getWays() && inputs.length == prevs.size() : "Partitioning problem: The partition changed the number of inputs of SSG "
                    + this.toString();
                for (int i = 0; i < inputs.length; i++) {
                    
//                    System.err.println("inputs[" + i + "] changing from  " 
//                            + ((inputs[i] != null) ?  (inputs[i].contents.getName() + " (" 
//                            + inputs[i].contents.getIdent() + ")") : ""));
//                    System.err.println("  to "
//                            + topLevel.getEdges()[i].contents.getName() + " (" 
//                            + topLevel.getEdges()[i].contents.getIdent() + ")");

                    inputs[i] = topLevel.getEdges()[i];
                }
            } else
                // can't be a joiner if there are any previous nodes!
                assert false : topLevel;
        }

        // set output[] to store the new outputs of the SSG
        if (nexts.size() > 0) {
            assert bottomLevel != null;
            if (bottomLevel.isFilter()) {
                assert nexts.size() == 1 && outputs.length == 1 : 
                    "Partitioning problem: The partition changed the number of outputs of SSG ";

//                System.err.println("outputs[0] changing from  " 
//                        + ((outputs[0] != null) ?  (outputs[0].contents.getName() + " (" 
//                        + outputs[0].contents.getIdent() + ")") : ""));
//                System.err.println("  to "
//                        + bottomLevel.contents.getName() + " (" 
//                        + bottomLevel.contents.getIdent() + ")");

                outputs[0] = bottomLevel;
            } else if (bottomLevel.isJoiner()) {
                // if a joiner set outputs[] to be the upstream nodes of the
                // joiner
                assert nexts.size() == bottomLevel.incoming.length
                    && bottomLevel.incoming.length == ((SIRJoiner) bottomLevel.contents)
                    .getWays() && outputs.length == nexts.size() : "Partitioning problem: The partition changed the number of outputs of SSG "
                    + this.toString();
                for (int i = 0; i < outputs.length; i++) {
//
//                    System.err.println("outputs[" + i + "] changing from  " 
//                            + ((outputs[i] != null) ?  (outputs[i].contents.getName() + " (" 
//                            + outputs[i].contents.getIdent() + ")") : ""));
//                    System.err.println("  to "
//                            + bottomLevel.incoming[i].contents.getName() + " (" 
//                            + bottomLevel.incoming[i].contents.getIdent() + ")");

                    outputs[i] = bottomLevel.incoming[i];
                }
            } else
                // can't be a splitter
                assert false : topLevel;
        }
    }

    /** get all the SIRFilters that are either sinks or sources of this SSG. */
    public HashSet<SIRFilter> getIOFilters() {
        HashSet<SIRFilter> filters = new HashSet<SIRFilter>();

        for (int i = 0; i < inputs.length; i++) {
            assert inputs[i].isFilter();
            filters.add((SIRFilter)(inputs[i].contents));
        }
        for (int i = 0; i < outputs.length; i++) {
            assert outputs[i].isFilter();
            filters.add((SIRFilter)(outputs[i].contents));
        }
        return filters;
    }

    /**
     * set a new TopLevelSIR stream and flatten it, can only be called before
     * layout!
     */
    public void setTopLevelSIR(SIRStream newTop) {

        topLevelSIR = newTop;

        setTopLevelSIRInitial(topLevelSIR);
        // update the inter-SSG connections to reference the new flatnodes
        updateSSGEdges();
    }

    /** Portion of setTopLevelSIR also called to set up inputs, outputs at SSC build time */
    private void setTopLevelSIRInitial(SIRStream newTop) {
        // remove the old nodes from the global parent map
        for (FlatNode fn : flatNodes) {
            streamGraph.parentMap.remove(fn);
        }
        // flatten the graph
        graphFlattener = new GraphFlattener(newTop);
        topLevel = graphFlattener.top;
        // update the flatnodes of this SSG list
        flatNodes = new LinkedList<FlatNode>();
        // update the flatnodes of this SSG list
        // and update the global parent map
        final StaticStreamGraph thiz = this;
        topLevel.accept(new FlatVisitor() {
                public void visitNode(FlatNode node) {
                    flatNodes.add(node);
                    streamGraph.parentMap.put(node,thiz);
                }
            }, null, true);

        // reset bottom level, the sink of this SSG
        setBottomLevel();
        // update inputs[] and outputs[] to point to the new flatnodes
        updateIOArrays();
    }
    
    /**
     * Given the current toplevel flatnode, create the SIR graph, also
     * regenerating the flatgraph *
     */
    public void createSIRGraph() {
//        (new DumpGraph()).dumpGraph(topLevel, SpaceDynamicBackend
//                                    .makeDotFileName("beforeFGtoSIR", topLevelSIR),
//                                    initExecutionCounts, steadyExecutionCounts);
        // do we want this here?!!
        setTopLevelSIR((new FlatGraphToSIR(topLevel)).getTopLevelSIR());
    }

    /** return the graph flattener object that was used to flatten. */
    public GraphFlattener getGraphFlattener() {
        return graphFlattener;
    }

    /**
     * returns a map of flatnodes to this SSG, so that others can remember the
     * parent mapping of flat nodes.
     */
    public Map<FlatNode,StaticStreamGraph> getParentMap() {
        // the parent map
        HashMap<FlatNode,StaticStreamGraph> parentMap = new HashMap<FlatNode,StaticStreamGraph>();
        // fill the parent map
        Iterator<FlatNode> flats = flatNodes.iterator();
        while (flats.hasNext()) {
            FlatNode node = flats.next();
            parentMap.put(node, this);
        }
        // return the parent map
        return parentMap;
    }


    /** After the underlying flatgraph has changed, find the new bottom level 
     * It should be an invariant that here is a single BottomLevel node:
     * either a single filter, or a null joiner.
     */
    private void setBottomLevel() {
        bottomLevel = null;

        topLevel.accept(new FlatVisitor() {
                public void visitNode(FlatNode node) {
                    // if the node has no edges, it is a bottom level...
                    if (node.getEdges().length == 0) {
                        assert bottomLevel == null : node;
                        bottomLevel = node;
                    }
                }
            }, null, true);
    }


    /**
     * When constructing this SSG, add a new connection from node->source to the
     * prevs hash map and add <b>node</b> to the inputs array
     */
    public void addPrev(FlatNode node, FlatNode source) {
        assert node.contents instanceof SIRFilter 
            && source.contents instanceof SIRFilter
            : "Dynamic rate edges must be between filters " + source.contents + " " + node.contents;
        if (prevs.get(node.contents) == source.contents) {
            return;
        }
        assert flatNodes.contains(node);
//        // create a new inputs array with the old inputs + this
//        FlatNode[] oldInputs = inputs;
//        inputs = new FlatNode[oldInputs.length + 1];
//        for (int i = 0; i < oldInputs.length; i++)
//            inputs[i] = oldInputs[i];
//        inputs[inputs.length - 1] = node;
//        
//        System.err.println("inputs[" + (inputs.length - 1) + "] = " 
//                + node.contents.getName() + " (" 
//                + node.contents.getIdent() + ")");
//
        prevs.put((SIRFilter)node.contents, (SIRFilter)source.contents);
    }

    /**
     * When constructing this SSG, add a new connection from node->next to the
     * nexts hash map and add the <pre>node</pre> to the outputs array.
     */
    public void addNext(FlatNode node, FlatNode next) {
        assert node.contents instanceof SIRFilter 
        && next.contents instanceof SIRFilter
        : "Dynamic rate edges must be between filters " + node.contents + " " + next.contents;
        if (nexts.get(node.contents) == next.contents) {
            return;
        }
        assert flatNodes.contains(node);

//        // System.out.println("Add next " + node + " -> " + next);
//
//        // create a new outputs array with the old outputs + this
//        FlatNode[] oldOutputs = outputs;
//        outputs = new FlatNode[oldOutputs.length + 1];
//        for (int i = 0; i < oldOutputs.length; i++)
//            outputs[i] = oldOutputs[i];
//        outputs[outputs.length - 1] = node;
//        System.err.println("outputs[" + (outputs.length - 1) + "] = " 
//                + node.contents.getName() + " (" 
//                + node.contents.getIdent() + ")");

        nexts.put((SIRFilter)node.contents, (SIRFilter)next.contents);
    }

    /** does this ssg have dynamic output * */
    public boolean hasOutput() {
        return outputSSGEdges.length > 0;
    }

    /** is <b>node</b> a dynamic source for this SSG * */
    public boolean isInput(FlatNode node) {
        assert flatNodes.contains(node);
        for (int i = 0; i < inputs.length; i++)
            if (inputs[i] == node)
                return true;
        return false;
    }

    /** is <b>node</b> a dynamic sink for this SSG * */
    public boolean isOutput(FlatNode node) {
        assert flatNodes.contains(node);
        for (int i = 0; i < outputs.length; i++)
            if (outputs[i] == node)
                return true;
        return false;
    }

    /** get the dynamic outputs of this ssg * */
    public FlatNode[] getOutputs() {
        return outputs;
    }

    /**
     * given a dynamic sink for this SSG, get the node in the downstream SSG
     * that it connects to
     */
    public FlatNode getNext(FlatNode flatNode) {
        assert flatNodes.contains(flatNode) : "Trying to get downstream SSG for a flatNode not in SSG";

        for (int i = 0; i < outputSSGEdges.length; i++) {
            if (flatNode == outputSSGEdges[i].getUpstreamNode())
                return outputSSGEdges[i].getDownstreamNode();
        }

        assert false : "Error: calling getNext() on non-dynamic sink of: "
            + this + " " + flatNode;
        return null;
    }

    /**
     * give a dynamic source for this SSG, get the node in the upstream SSG that
     * it connects to
     */
    public FlatNode getPrev(FlatNode flatNode) {
        assert flatNodes.contains(flatNode) : "Trying to get upstream SSG for a FlatNode not in SSG";

        for (int i = 0; i < inputSSGEdges.length; i++) {
            if (flatNode == inputSSGEdges[i].getDownstreamNode())
                return inputSSGEdges[i].getUpstreamNode();
        }

        assert false : "Error: calling getPrev() on non-dynamic source of: "
            + this + " " + flatNode;
        return null;
    }

    /** return a list of the flatnodes of this SSG * */
    public List<FlatNode> getFlatNodes() {
        return flatNodes;
    }

    /** get the top level flatnode of this SSG* */
    public FlatNode getTopLevel() {
        assert topLevel != null : this;
        return topLevel;
    }

    /** get the toplevel SIR of this SSG * */
    public SIRStream getTopLevelSIR() {
        assert topLevelSIR != null : this;
        return topLevelSIR;
    }

    public String toString() {
        if (topLevelSIR != null) {
            return "StaticStreamGraph_with_SIR_" + topLevelSIR.getName();
        } else {
            return "StaticStreamGraph_withoutSIR_" + topLevel.toString();
        }
    }

    public void check() {
    }


    // debug function
    // run me after layout please
    public static void printCounts(HashMap counts) {
        Iterator it = counts.keySet().iterator();
        while (it.hasNext()) {
            FlatNode node = (FlatNode) it.next();
            // if (Layout.joiners.contains(node))
            System.out.println(node.contents.getName() + " "
                               + ((Integer) counts.get(node)).intValue());
        }
    }


    /** get the parent stream graph of SSG * */
    public StreamGraph getStreamGraph() {
        return streamGraph;
    }


    /**
     * Get an input SSGEdge coming into this SSG given the <pre>dest</pre> of the edge,
     * so <pre>dest</pre> is in this SSG
     */
    private SSGEdge getInputSSGEdgeDest(FlatNode dest) {
        SSGEdge edge = null;

        for (int i = 0; i < inputSSGEdges.length; i++)
            if (inputSSGEdges[i].getDownstreamNode() == dest)
                edge = inputSSGEdges[i];
        assert edge != null : "Calling getInputSSGEdgeDest(FlatNode dest) with a node that is not an input to SSG";

        return edge;
    }

    /***************************************************************************
     * get the input type for a node of this SSG, we want to use this because we
     * set the input type of the head of this SSG to null, even though it may
     * receive data over the dynamic network
     **************************************************************************/
    public CType getInputType(FlatNode node) {
        assert flatNodes.contains(node) : "Calling getInputType(node) and node is not in SSG";
        if (isInput(node)) {
            // get the type from the source SSG of this edge
            SSGEdge edge = getInputSSGEdgeDest(node);
            return edge.getUpstream().getOutputType(edge.getUpstreamNode());
        } else
            return node.getFilter().getInputType();
    }

    /** get the output type for <pre>node</pre> of this SSG * */
    public CType getOutputType(FlatNode node) {
        assert flatNodes.contains(node) : "Calling getOutputType(node) and node is not in SSG";
        if (isOutput(node))
            return outputTypes[getOutputNum(node)];
        else
            return node.getFilter().getOutputType();
    }

    /** get the number of filters of this SSG * */
    public int filterCount() {
        final int[] filters = { 0 };

        IterFactory.createFactory().createIter(getTopLevelSIR()).accept(
                new EmptyStreamVisitor() {
                    public void visitFilter(SIRFilter self, SIRFilterIter iter) {
                        if (!(self instanceof SIRDummySource || self instanceof SIRDummySink)) {
                            filters[0]++;
                        }
                        
                    }
                    
                                                                        });
        return filters[0];
    }
    
    /** accept a stream graph visitor * */
    public void accept(StreamGraphVisitor s, HashSet<StaticStreamGraph> visited, boolean newHash) {
        if (newHash)
            visited = new HashSet<StaticStreamGraph>();

        if (visited.contains(this))
            return;

        visited.add(this);
        s.visitStaticStreamGraph(this);

        Iterator<StaticStreamGraph> nextsIt = nextSSGs.iterator();
        while (nextsIt.hasNext()) {
            StaticStreamGraph ssg = nextsIt.next();
            ssg.accept(s, visited, false);
        }
    }
    
    /** connect flatgraph with weight-1 edges to nodes in other ssg's flatgraphs based on this ssg's outputs. 
     * Restore types in filters being connected.  Restore dynamic rate info. */
    public void reconnectOutputs() {
        for (SSGEdge edge : outputSSGEdges) {
            FlatNode upstream = edge.getUpstreamNode();
            FlatNode downstream = edge.getDownstreamNode();
            if (upstream.getEdges().length > 0) {
                assert upstream.getEdges().length == 1 && upstream.getEdges()[0].isNullJoiner();
                upstream.getEdges()[0].removeBackEdge(upstream);
                upstream.removeEdges();
            }
            if (downstream.incoming.length > 0) {
                assert downstream.incoming.length == 1 && downstream.incoming[0].isNullSplitter();
                try {
                    downstream.incoming[0].removeBackEdge(downstream);
                } catch (NullPointerException e) {
                    // TODO: this shouldn't happen but it does
                }
                downstream.removeIncoming();
            }
            FlatNode.addEdges(upstream,downstream);
        }
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i].contents instanceof SIRFilter) {
                SIRFilter f = (SIRFilter)inputs[i].contents;
                f.setInputType(inputTypes[i]);
                f.setPeek(peekRates[i]);
                f.setPop(popRates[i]);
//                System.err.println("Input Filter \"" + f.getName() + " (" + f.getIdent() + ") "
//                        + inputTypes[i] + " " + peekRates[i] + " " + popRates[i]);
            }
        }
        for (int i = 0; i < outputs.length; i++) {
            if (outputs[i].contents instanceof SIRFilter) { 
                SIRFilter f = (SIRFilter)outputs[i].contents;
                f.setOutputType(outputTypes[i]);
                f.setPush(pushRates[i]);
//                System.err.println("Output Filter \"" + f.getName() + " (" + f.getIdent() + ") "
//                        + outputTypes[i] + " " + pushRates[i]);
            }
        }
    }

}
