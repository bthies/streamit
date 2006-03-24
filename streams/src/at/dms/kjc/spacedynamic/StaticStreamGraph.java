package at.dms.kjc.spacedynamic;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.kjc.flatgraph.*;
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import java.util.*;
import java.io.*;
import at.dms.util.Utils;

/**
 * A StaticStreamGraph represents a subgraph of the application's StreamGraph
 * where communication within the SSG is over static rate channels. The
 * input/output (if either exists) of an SSG is dynamic, but the sources and
 * sinks have their input/output rates zeroed, repectively.
 */

public class StaticStreamGraph {
    /** these Hashmaps map flatnode -> flatnode * */
    /** prevs maps the input of this SSG to the output of its upstream SSG * */
    /** nexts stores the converse * */
    private HashMap prevs, nexts;

    /** the inter-SSG communication edges of this SSG, both incoming and outgoing * */
    private SSGEdge[] inputSSGEdges, outputSSGEdges;

    private List prevSSGs, nextSSGs;

    // arrays representing the inputs and output of this ssg
    // from top to bottom (left to right)...
    private FlatNode[] inputs;

    private FlatNode[] outputs;

    // the output type of the ssg output
    private CType[] outputTypes;

    // the top level SIR node
    private SIRStream topLevelSIR;

    // the top level FlatNode
    private FlatNode topLevel;

    // the graph flattener used to convert SIR to FlatGraph
    private GraphFlattener graphFlattener;

    // given a flatnode map to the execution count for desired stage
    private HashMap initExecutionCounts;

    private HashMap steadyExecutionCounts;

    // stores the multiplicities as returned by the scheduler...
    private HashMap[] executionCounts;

    // the rate declaration of the output of this SSG
    public JExpression pushRate;

    // the rate declarations of the input of this SSG
    public JExpression popRate;

    public JExpression peekRate;

    private FlatNode bottomLevel;

    // the number of tiles assigned to this subgraph
    private int numTilesAssigned;

    // the tiles assiged
    private RawTile[] tilesAssigned;

    // set of all the flat nodes of this graph...
    private LinkedList flatNodes;

    // the communication scheduler we are going to use for this graph...
    public Simulator simulator;

    // used to construct a valid SIR graph
    private int splitterBalance;

    // the id of this SSG
    private static int nextID = 0;

    public int id;

    // the parent stream graph
    private StreamGraph streamGraph;

    /***************************************************************************
     * create a static stream graph with realTop as the first node that the
     * implicit splitter points to
     **************************************************************************/
    public StaticStreamGraph(StreamGraph sg, FlatNode realTop) {
        this.streamGraph = sg;
        id = nextID++;
        this.prevs = new HashMap();
        this.nexts = new HashMap();
        this.prevSSGs = new LinkedList();
        this.nextSSGs = new LinkedList();
        // flatNodes = new HashSet();
        flatNodes = new LinkedList();
        outputs = new FlatNode[0];
        inputs = new FlatNode[0];
        outputTypes = new CType[0];

        // a static stream graph always starts with a splitter, we
        // remove it later if ways == 1
        this.topLevel = new FlatNode(SIRSplitter.create(null,
                                                        SIRSplitType.NULL, 0));

        addTopLevelFlatNode(realTop);
    }

    /** add a source node to this SSG and add it to the toplevel * */
    public void addTopLevelFlatNode(FlatNode node) {
        // System.out.println("AddTopLevelNode " + node + " to " + id) ;
        assert node.isFilter();
        SIRFilter filter = (SIRFilter) node.contents;
        // checks on the filter
        assert filter.getPopInt() == 0 && filter.getPeekInt() == 0;
        // checks on the flatgraph
        assert node.inputs == 0 && node.incoming.length == 0;
        // add the flatnodes to various structures
        addFlatNode(node);

        // create a new toplevel with one extra connection
        FlatNode oldTopLevel = topLevel;
        flatNodes.remove(oldTopLevel);

        topLevel = new FlatNode(SIRSplitter.create(null, SIRSplitType.NULL,
                                                   oldTopLevel.ways + 1));

        addFlatNode(topLevel);

        // add edges from old top level
        assert oldTopLevel.ways == oldTopLevel.edges.length
            && oldTopLevel.ways == ((SIRSplitter) oldTopLevel.contents)
            .getWays();
        for (int i = 0; i < oldTopLevel.ways; i++) {
            FlatNode current = oldTopLevel.edges[i];
            // first remove the back edge to the old top level
            current.removeBackEdge(oldTopLevel);
            // now add new edges (forward and back)
            topLevel.addEdges(topLevel, current);
        }

        // add new edge
        topLevel.addEdges(topLevel, node);
    }

    /** when constructing this SSG, add <pre>node</pre> to it * */
    public void addFlatNode(FlatNode node) {
        // System.out.println("Adding " + node + " to " + id);
        flatNodes.add(node);
        streamGraph.putParentMap(node, this);
        // System.out.println("Adding to parent map " + node);
    }

    /***************************************************************************
     * remove toplevel splitter if not needed!!! and perform some other checks
     * on the SSG
     **************************************************************************/
    public void cleanUp() {
        assert topLevel.isSplitter();
        assert ((SIRSplitter) topLevel.contents).getWays() > 0;

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
                        for (int i = 0; i < node.edges.length; i++) {
                            assert node.edges[i] != null;

                            if (!(flatNodes.contains(node.edges[i]))) {
                                node.removeForwardEdge(node.edges[i]);
                            }
                        }
                    }
                }
            }, null, true);

        // make sure all nodes have correct number of connections...
        topLevel.accept(new FlatVisitor() {
                public void visitNode(FlatNode node) {
                    if (node.isFilter()) {
                        SIRFilter filter = (SIRFilter) node.contents;
                        if (filter.getPopInt() > 0)
                            assert node.inputs == 1;
                        else // doesn't pop
                            if (node.inputs == 1)
                                assert node.incoming[0].getWeight(node) == 0;

                        if (filter.getPushInt() > 0)
                            assert node.ways == 1;
                        else // doesn't push
                            if (node.ways == 1)
                                assert node.edges[0].getIncomingWeight(node) == 0;
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
                        assert splitter.getWays() == node.ways : "Invalid Splitter: "
                            + node
                            + " "
                            + splitter.getWays()
                            + " != "
                            + node.ways;

                    }
                }
            }, null, true);

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

        // splitters and joiners are balanced so return...
        if (splitterBalance == 0)
            return;

        // remove the splitter because it is not needed!!!
        if (((SIRSplitter) topLevel.contents).getWays() == 1) {
            FlatNode oldTopLevel = topLevel;
            assert topLevel.ways == 1 && topLevel.edges.length == 1
                && topLevel.edges[0] != null;
            topLevel = topLevel.edges[0];
            topLevel.removeBackEdge(oldTopLevel);
            flatNodes.remove(oldTopLevel);
        }

        // set up the downstream SSG's prevs hashmap
        Iterator nextsIt = nexts.keySet().iterator();
        while (nextsIt.hasNext()) {
            FlatNode source = (FlatNode) nextsIt.next();
            FlatNode dest = (FlatNode) nexts.get(source);
            assert dest != null : source.toString();
            streamGraph.getParentSSG(dest).addPrev(dest, source);
        }

        // build the prevSSGs and nextSSGs list, the upstream and downstream
        // SSGs
        nextsIt = nexts.values().iterator();
        while (nextsIt.hasNext()) {
            StaticStreamGraph ssg = streamGraph
                .getParentSSG(((FlatNode) nextsIt.next()));
            if (!nextSSGs.contains(ssg))
                nextSSGs.add(ssg);
        }

        Iterator prevsIt = prevs.values().iterator();
        while (prevsIt.hasNext()) {
            StaticStreamGraph ssg = streamGraph
                .getParentSSG(((FlatNode) prevsIt.next()));
            if (!prevSSGs.contains(ssg))
                prevSSGs.add(ssg);
        }
    }

    /***************************************************************************
     * after SSG construction is complete, create the inter-SSG connections for
     * this SSG
     **************************************************************************/
    public void connect() {
        // build the inputSSGEdges and outputSSGEdges arrays
        inputSSGEdges = new SSGEdge[prevs.size()];
        outputSSGEdges = new SSGEdge[nexts.size()];

        for (int i = 0; i < outputSSGEdges.length; i++) {
            // nexts.put(outputs[i], outputs[i]);
            // System.out.println(outputs[i]);
            // System.out.println(nexts.size());
            // System.out.println(nexts.keySet().toArray()[0]);
            // System.out.println((nexts.keySet().toArray()[0] == outputs[i]) +
            // " " +
            // nexts.containsKey(outputs[i]));

            FlatNode dest = (FlatNode) nexts.get(outputs[i]);
            StaticStreamGraph input = streamGraph.getParentSSG(dest);
            outputSSGEdges[i] = new SSGEdge(this, input, i, input
                                            .getInputNum(dest));
            outputSSGEdges[i].outputNode = outputs[i];
            outputSSGEdges[i].inputNode = dest;
        }

        // for input, just get the sources SSGEdges!
        for (int i = 0; i < inputSSGEdges.length; i++) {
            FlatNode source = (FlatNode) prevs.get(inputs[i]);
            StaticStreamGraph input = streamGraph.getParentSSG(source);
            inputSSGEdges[i] = input.getOutputSSGEdgeSource(source);
        }

        // update the connections
        updateSSGEdges();

        for (int i = 0; i < outputs.length; i++)
            assert outputs[i] != null : this.toString() + " has null output "
                + i;

        for (int i = 0; i < inputs.length; i++)
            assert inputs[i] != null : this.toString() + " has null input " + i;

    }

    /**
     * given an output node for this SSG, get the SSGEdge that represents the
     * connection, <pre>source</pre> is the source of the SSG edge.
     */
    public SSGEdge getOutputSSGEdgeSource(FlatNode source) {
        for (int i = 0; i < outputSSGEdges.length; i++)
            if (outputSSGEdges[i].outputNode == source)
                return outputSSGEdges[i];

        assert false;
        return null;
    }

    /***************************************************************************
     * Given <pre>dest</pre> a source of a direct downstream SSG, get the SSGEdge that
     * represents the connection
     **************************************************************************/
    public SSGEdge getOutputSSEdgeDest(FlatNode dest) {
        for (int i = 0; i < outputSSGEdges.length; i++)
            if (outputSSGEdges[i].inputNode == dest)
                return outputSSGEdges[i];
        assert false;
        return null;
    }

    /**
     * after we have changed the flatgraph and updated outputs[] and inputs[],
     * update the SSGEdges to reflect the new input and output FlatNodes
     * 
     * Remember that the number of inputs and outputs has to remain the same
     */
    private void updateSSGEdges() {
        for (int i = 0; i < outputs.length; i++) {
            outputSSGEdges[i].outputNode = outputs[i];
        }

        for (int i = 0; i < inputs.length; i++) {
            inputSSGEdges[i].inputNode = inputs[i];
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

    /***************************************************************************
     * given an input, source, for this SSG, get the input number, index to
     * inputs[] and inputSSGEdges[]
     **************************************************************************/
    public int getInputNum(FlatNode node) {
        System.out.println(inputs.length);
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] == node)
                return i;
        }
        assert false : node + " not an input";
        return -1;
    }

    /***************************************************************************
     * After the underlying flatgraph has changed, we have to update the inputs
     * and outputs arrays and the input and output edges
     **************************************************************************/
    private void updateIOArrays() {
        assert topLevel != null && bottomLevel != null;

        // set input[] to store the new inputs (sources) for this SSG
        if (prevs.size() > 0) {
            // if a filter, then just set the inputs[0] to it
            if (topLevel.isFilter()) {
                assert prevs.size() == 1 && inputs.length == 1;
                inputs[0] = topLevel;
            } else if (topLevel.isSplitter()) {
                // if a splitter, then set the input[] to the direct downstream
                // node of the splitter...
                assert prevs.size() == topLevel.edges.length
                    && topLevel.edges.length == ((SIRSplitter) topLevel.contents)
                    .getWays() && inputs.length == prevs.size() : "Partitioning problem: The partition changed the number of inputs of SSG "
                    + this.toString();
                for (int i = 0; i < inputs.length; i++) {
                    inputs[i] = topLevel.edges[i];
                }
            } else
                // can't be a joiner
                assert false : "Entry point to SSG cannot be a joiner";
        }

        // set output[] to store the new outputs of the SSG
        if (nexts.size() > 0) {
            if (bottomLevel.isFilter()) {
                assert nexts.size() == 1 && outputs.length == 1;
                outputs[0] = bottomLevel;
            } else if (bottomLevel.isJoiner()) {
                // if a joiner set outputs[] to be the upstream nodes of the
                // joiner
                assert nexts.size() == bottomLevel.incoming.length
                    && bottomLevel.incoming.length == ((SIRJoiner) bottomLevel.contents)
                    .getWays() && outputs.length == nexts.size() : "Partitioning problem: The partition changed the number of outputs of SSG "
                    + this.toString();
                for (int i = 0; i < outputs.length; i++)
                    outputs[i] = bottomLevel.incoming[i];
            } else
                // can't be a splitter
                assert false : "Exit of SSG cannot be a splitter";
        }
    }

    /** get all the SIRFilters that are either sinks or sources of this SSG * */
    public HashSet getIOFilters() {
        HashSet filters = new HashSet();

        for (int i = 0; i < inputs.length; i++) {
            assert inputs[i].isFilter();
            filters.add(inputs[i].contents);
        }
        for (int i = 0; i < outputs.length; i++) {
            assert outputs[i].isFilter();
            filters.add(outputs[i].contents);
        }
        return filters;
    }

    /**
     * set a new TopLevelSIR stream and flatten it, can only be called before
     * layout!*
     */
    public void setTopLevelSIR(SIRStream newTop) {
        // can only call this before layout!!
        assert streamGraph.getLayout() == null;

        System.out.println(" ****  CALLING SETTOPLEVELSIR **** ");

        topLevelSIR = newTop;
        // dump the graph
        StreamItDot.printGraph(topLevelSIR, SpaceDynamicBackend
                               .makeDotFileName("setTLSIR", topLevelSIR));

        // remove the old nodes from the global parent map
        Iterator fns = flatNodes.iterator();
        while (fns.hasNext()) {
            streamGraph.parentMap.remove(fns.next());
        }
        // flatten the graph
        graphFlattener = new GraphFlattener(topLevelSIR);
        topLevel = graphFlattener.top;
        // reset bottom level, the sink of this SSG
        setBottomLevel();
        // update inputs[] and outputs[] to point to the new flatnodes
        updateIOArrays();
        // update the inter-SSG connections to reference the new flatnodes
        updateSSGEdges();
        flatNodes = new LinkedList();
        // update the flatnodes of this SSG list
        topLevel.accept(new FlatVisitor() {
                public void visitNode(FlatNode node) {
                    flatNodes.add(node);
                }
            }, null, true);

        // update the global parent map
        fns = flatNodes.iterator();
        while (fns.hasNext()) {
            streamGraph.parentMap.put(fns.next(), this);
        }

    }

    /**
     * Schedule the static communication of this SSG, given the schedule of
     * joiner firing, <pre>js</pre> *
     */
    public void scheduleCommunication(JoinerSimulator js) {
        if (KjcOptions.wbs)
            simulator = new WorkBasedSimulator(this, js);
        else
            simulator = new FineGrainSimulator(this, js);
        simulator.simulate();
    }

    /**
     * Given the current toplevel flatnode, create the SIR graph, also
     * regenerating the flatgraph *
     */
    public void createSIRGraph() {
        (new DumpGraph()).dumpGraph(topLevel, SpaceDynamicBackend
                                    .makeDotFileName("beforeFGtoSIR", topLevelSIR),
                                    initExecutionCounts, steadyExecutionCounts);
        // do we want this here?!!
        setTopLevelSIR((new FlatGraphToSIR(topLevel)).getTopLevelSIR());
        // topLevelSIR = (new FlatGraphToSIR(topLevel)).getTopLevelSIR();
    }

    /** return the graph flattener object that was used to flatten * */
    public GraphFlattener getGraphFlattener() {
        return graphFlattener;
    }

    /***************************************************************************
     * returns a map of flatnodes to this SSG, so that others can remember the
     * parent mapping of flat nodes.
     **************************************************************************/
    public HashMap getParentMap() {
        // the parent map
        HashMap parentMap = new HashMap();
        // fill the parent map
        Iterator flats = flatNodes.iterator();
        while (flats.hasNext()) {
            FlatNode node = (FlatNode) flats.next();
            parentMap.put(node, this);
        }
        // return the parent map
        return parentMap;
    }

    /**
     * call the scheduler on the toplevel SIR node and create the execution
     * counts *
     */
    public void scheduleAndCreateMults() {
        // get the multiplicities from the scheduler
        executionCounts = SIRScheduler.getExecutionCounts(topLevelSIR);
        PartitionDot.printScheduleGraph(topLevelSIR, SpaceDynamicBackend
                                        .makeDotFileName("schedule", topLevelSIR), executionCounts);

        // create the multiplicity maps
        createExecutionCounts();
        // print the flat graph
        dumpFlatGraph();
    }

    /** after the underlying flatgraph has changed, find the new bottom level * */
    private void setBottomLevel() {
        bottomLevel = null;

        topLevel.accept(new FlatVisitor() {
                public void visitNode(FlatNode node) {
                    // if the node has no edges, it is a bottom level...
                    if (node.edges.length == 0) {
                        assert bottomLevel == null : node;
                        bottomLevel = node;
                    }
                }
            }, null, true);
    }

    /** dump a dot rep of the flat graph * */
    public void dumpFlatGraph() {
        // dump the flatgraph of the application, must be called after
        // createExecutionCounts
        (new DumpGraph()).dumpGraph(graphFlattener.top, SpaceDynamicBackend
                                    .makeDotFileName("flatgraph", topLevelSIR),
                                    initExecutionCounts, steadyExecutionCounts);
    }

    /** set the number of tiles that this SSG should occupy on the raw chip * */
    public void setNumTiles(int i) {
        this.numTilesAssigned = i;
        this.tilesAssigned = new RawTile[i];
    }

    /** return the number of tiles that this SSG was assigned to occupy * */
    public int getNumTiles() {
        return this.numTilesAssigned;
    }

    /***************************************************************************
     * when constructing this SSG, add a new connection from node->source to the
     * prevs hash map and add <pre>node</pre> to the inputs array
     **************************************************************************/
    public void addPrev(FlatNode node, FlatNode source) {
        assert flatNodes.contains(node);
        // create a new inputs array with the old inputs + this
        FlatNode[] oldInputs = inputs;
        inputs = new FlatNode[oldInputs.length + 1];
        for (int i = 0; i < oldInputs.length; i++)
            inputs[i] = oldInputs[i];
        inputs[inputs.length - 1] = node;

        prevs.put(node, source);
    }

    /***************************************************************************
     * when constructing this SSG, add a new connection from node->next to the
     * nexts hash map and add the <pre>node</pre> to the outputs array
     **************************************************************************/
    public void addNext(FlatNode node, FlatNode next) {
        assert flatNodes.contains(node);

        // System.out.println("Add next " + node + " -> " + next);

        // create a new outputs array with the old outputs + this
        FlatNode[] oldOutputs = outputs;
        outputs = new FlatNode[oldOutputs.length + 1];
        for (int i = 0; i < oldOutputs.length; i++)
            outputs[i] = oldOutputs[i];
        outputs[outputs.length - 1] = node;

        // create a new output type array...
        CType[] oldOutputTypes = outputTypes;
        outputTypes = new CType[oldOutputTypes.length + 1];
        for (int i = 0; i < oldOutputTypes.length; i++)
            outputTypes[i] = oldOutputTypes[i];
        outputTypes[outputTypes.length - 1] = Util.getOutputType(node);

        nexts.put(node, next);
    }

    /** does this ssg have dynamic output * */
    public boolean hasOutput() {
        return outputSSGEdges.length > 0;
    }

    /** is <pre>node</pre> a dynamic source for this SSG * */
    public boolean isInput(FlatNode node) {
        assert flatNodes.contains(node);
        for (int i = 0; i < inputs.length; i++)
            if (inputs[i] == node)
                return true;
        return false;
    }

    /** is <pre>node</pre> a dynamic sink for this SSG * */
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

    /***************************************************************************
     * given a dynamic sink for this SSG, get the node in the downstream SSG
     * that it connects to
     **************************************************************************/
    public FlatNode getNext(FlatNode flatNode) {
        assert flatNodes.contains(flatNode) : "Trying to get downstream SSG for a flatNode not in SSG";

        for (int i = 0; i < outputSSGEdges.length; i++) {
            if (flatNode == outputSSGEdges[i].outputNode)
                return outputSSGEdges[i].inputNode;
        }

        assert false : "Error: calling getNext() on non-dynamic sink of: "
            + this + " " + flatNode;
        return null;
    }

    /***************************************************************************
     * give a dynamic source for this SSG, get the node in the upstream SSG that
     * it connects to
     **************************************************************************/
    public FlatNode getPrev(FlatNode flatNode) {
        assert flatNodes.contains(flatNode) : "Trying to get upstream SSG for a FlatNode not in SSG";

        for (int i = 0; i < inputSSGEdges.length; i++) {
            if (flatNode == inputSSGEdges[i].inputNode)
                return inputSSGEdges[i].outputNode;
        }

        assert false : "Error: calling getPrev() on non-dynamic source of: "
            + this + " " + flatNode;
        return null;
    }

    /** return a list of the flatnodes of this SSG * */
    public List getFlatNodes() {
        return flatNodes;
    }

    /** get the top level flatnode of this SSG* */
    public FlatNode getTopLevel() {
        assert topLevel != null : this.toString();
        return topLevel;
    }

    /** get the toplevel SIR of this SSG * */
    public SIRStream getTopLevelSIR() {
        assert topLevelSIR != null : this.toString();
        return topLevelSIR;
    }

    public String toString() {
        // if (topLevel != null)
        // return topLevel.toString();
        return topLevelSIR.toString();
    }

    public void check() {
    }

    /***************************************************************************
     * given the multiplicities created by the scheduler, put them into a format
     * that is more easily used
     **************************************************************************/
    private void createExecutionCounts() {

        // make fresh hashmaps for results
        HashMap[] result = { initExecutionCounts = new HashMap(),
                             steadyExecutionCounts = new HashMap() };

        // then filter the results to wrap every filter in a flatnode,
        // and ignore splitters
        for (int i = 0; i < 2; i++) {
            for (Iterator it = executionCounts[i].keySet().iterator(); it
                     .hasNext();) {
                SIROperator obj = (SIROperator) it.next();
                int val = ((int[]) executionCounts[i].get(obj))[0];
                // System.err.println("execution count for " + obj + ": " +
                // val);
                /*
                 * This bug doesn't show up in the new version of FM Radio - but
                 * leaving the comment here in case we need to special case any
                 * other scheduler bugsx.
                 * 
                 * if (val==25) { System.err.println("Warning: catching
                 * scheduler bug with special-value " + "overwrite in
                 * SpaceDynamicBackend"); val=26; } if ((i == 0) &&
                 * (obj.getName().startsWith("Fused__StepSource") ||
                 * obj.getName().startsWith("Fused_FilterBank"))) val++;
                 */
                if (graphFlattener.getFlatNode(obj) != null)
                    result[i].put(graphFlattener.getFlatNode(obj), new Integer(
                                                                               val));
            }
        }

        // Schedule the new Identities and Splitters introduced by
        // GraphFlattener
        for (int i = 0; i < GraphFlattener.needsToBeSched.size(); i++) {
            FlatNode node = (FlatNode) GraphFlattener.needsToBeSched.get(i);
            int initCount = -1;
            if (node.incoming.length > 0) {
                if (initExecutionCounts.get(node.incoming[0]) != null)
                    initCount = ((Integer) initExecutionCounts
                                 .get(node.incoming[0])).intValue();
                if ((initCount == -1)
                    && (executionCounts[0].get(node.incoming[0].contents) != null))
                    initCount = ((int[]) executionCounts[0]
                                 .get(node.incoming[0].contents))[0];
            }
            int steadyCount = -1;
            if (node.incoming.length > 0) {
                if (steadyExecutionCounts.get(node.incoming[0]) != null)
                    steadyCount = ((Integer) steadyExecutionCounts
                                   .get(node.incoming[0])).intValue();
                if ((steadyCount == -1)
                    && (executionCounts[1].get(node.incoming[0].contents) != null))
                    steadyCount = ((int[]) executionCounts[1]
                                   .get(node.incoming[0].contents))[0];
            }
            if (node.contents instanceof SIRIdentity) {
                if (initCount >= 0)
                    initExecutionCounts.put(node, new Integer(initCount));
                if (steadyCount >= 0)
                    steadyExecutionCounts.put(node, new Integer(steadyCount));
            } else if (node.contents instanceof SIRSplitter) {
                // System.out.println("Splitter:"+node);
                int[] weights = node.weights;
                FlatNode[] edges = node.edges;
                int sum = 0;
                for (int j = 0; j < weights.length; j++)
                    sum += weights[j];
                for (int j = 0; j < edges.length; j++) {
                    if (initCount >= 0)
                        initExecutionCounts.put(edges[j], new Integer(
                                                                      (initCount * weights[j]) / sum));
                    if (steadyCount >= 0)
                        steadyExecutionCounts.put(edges[j], new Integer(
                                                                        (steadyCount * weights[j]) / sum));
                }
                if (initCount >= 0)
                    result[0].put(node, new Integer(initCount));
                if (steadyCount >= 0)
                    result[1].put(node, new Integer(steadyCount));
            } else if (node.contents instanceof SIRJoiner) {
                FlatNode oldNode = graphFlattener.getFlatNode(node.contents);
                if (executionCounts[0].get(node.oldContents) != null)
                    result[0].put(node, new Integer(((int[]) executionCounts[0]
                                                     .get(node.oldContents))[0]));
                if (executionCounts[1].get(node.oldContents) != null)
                    result[1].put(node, new Integer(((int[]) executionCounts[1]
                                                     .get(node.oldContents))[0]));
            }
        }

        // now, in the above calculation, an execution of a joiner node is
        // considered one cycle of all of its inputs. For the remainder of the
        // raw backend, I would like the execution of a joiner to be defined as
        // the joiner passing one data item down stream
        for (int i = 0; i < 2; i++) {
            Iterator it = result[i].keySet().iterator();
            while (it.hasNext()) {
                FlatNode node = (FlatNode) it.next();
                if (node.contents instanceof SIRJoiner) {
                    int oldVal = ((Integer) result[i].get(node)).intValue();
                    int cycles = oldVal
                        * ((SIRJoiner) node.contents).oldSumWeights;
                    if ((node.schedMult != 0) && (node.schedDivider != 0))
                        cycles = (cycles * node.schedMult) / node.schedDivider;
                    result[i].put(node, new Integer(cycles));
                }
                if (node.contents instanceof SIRSplitter) {
                    int sum = 0;
                    for (int j = 0; j < node.ways; j++)
                        sum += node.weights[j];
                    int oldVal = ((Integer) result[i].get(node)).intValue();
                    result[i].put(node, new Integer(sum * oldVal));
                    // System.out.println("SchedSplit:"+node+" "+i+" "+sum+"
                    // "+oldVal);
                }
            }
        }

        // The following code fixes an implementation quirk of two-stage-filters
        // in the *FIRST* version of the scheduler. It is no longer needed,
        // but I am keeping it around just in case we every need to go back to
        // the old
        // scheduler.

        // increment the execution count for all two-stage filters that have
        // initpop == initpush == 0, do this for the init schedule only
        // we must do this for all the two-stage filters,
        // so iterate over the keyset from the steady state
        /*
         * Iterator it = result[1].keySet().iterator(); while(it.hasNext()){
         * FlatNode node = (FlatNode)it.next(); if (node.contents instanceof
         * SIRTwoStageFilter) { SIRTwoStageFilter two = (SIRTwoStageFilter)
         * node.contents; if (two.getInitPush() == 0 && two.getInitPop() == 0) {
         * Integer old = (Integer)result[0].get(node); //if this 2-stage was not
         * in the init sched //set the oldval to 0 int oldVal = 0; if (old !=
         * null) oldVal = old.intValue(); result[0].put(node, new Integer(1 +
         * oldVal)); } } }
         */
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

    /** get the multiplicity map for the give stage * */
    public HashMap getExecutionCounts(boolean init) {
        return init ? initExecutionCounts : steadyExecutionCounts;
    }

    /**
     * get the multiplicity for <pre>node</pre> in the given stage, if <pre>init</pre> then init
     * stage *
     */
    public int getMult(FlatNode node, boolean init) {
        assert !(!init && !steadyExecutionCounts.containsKey(node)) : "Asking for steady mult for a filter that is not in the steady schedule "
            + node;

        Integer val = ((Integer) (init ? initExecutionCounts.get(node)
                                  : steadyExecutionCounts.get(node)));
        if (val == null)
            return 0;
        else
            return val.intValue();
    }

    /** accept a stream graph visitor * */
    public void accept(StreamGraphVisitor s, HashSet visited, boolean newHash) {
        if (newHash)
            visited = new HashSet();

        if (visited.contains(this))
            return;

        visited.add(this);
        s.visitStaticStreamGraph(this);

        Iterator nextsIt = nextSSGs.iterator();
        while (nextsIt.hasNext()) {
            StaticStreamGraph ssg = (StaticStreamGraph) nextsIt.next();
            ssg.accept(s, visited, false);
        }
    }

    /**
     * Called right after construction to set the rates of the endpoints of the
     * SSG (push = 0 for sink, peek = pop = 0 for source) and remember true
     * rates..
     */
    public void fixEndpoints() {
        SIRFilter source = Util.getSourceFilter(topLevelSIR);
        SIRFilter sink = Util.getSinkFilter(topLevelSIR);

        pushRate = sink.getPush();
        popRate = source.getPop();
        peekRate = source.getPeek();

        source.setPop(new JIntLiteral(0));
        source.setPeek(new JIntLiteral(0));
        sink.setPush(new JIntLiteral(0));
    }

    /** get the parent stream graph of SSG * */
    public StreamGraph getStreamGraph() {
        return streamGraph;
    }

    /**
     * count the number of nodes of the flatgraph that are assigned to tiles by
     * layout *
     */
    public int countAssignedNodes() {
        int assignedNodes = 0;

        Iterator nodes = flatNodes.iterator();
        while (nodes.hasNext()) {
            if (Layout.assignToATile((FlatNode) nodes.next()))
                assignedNodes++;
        }
        return assignedNodes;
    }

    /**
     * Get an input SSGEdge coming into this SSG given the <pre>dest</pre> of the edge,
     * so <pre>dest</pre> is in this SSG
     */
    public SSGEdge getInputSSGEdgeDest(FlatNode dest) {
        SSGEdge edge = null;

        for (int i = 0; i < inputSSGEdges.length; i++)
            if (inputSSGEdges[i].inputNode == dest)
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
            return edge.getOutput().getOutputType(edge.outputNode);
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
}
