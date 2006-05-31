package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
//import at.dms.kjc.flatgraph.SSGEdge;
import at.dms.kjc.flatgraph.*;
//import at.dms.util.IRPrinter;
//import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import java.util.*;

/**
 * A StaticStreamGraph represents a subgraph of the application's StreamGraph
 * where communication within the SSG is over static rate channels. The
 * input/output (if either exists) of an SSG is dynamic, but the sources and
 * sinks have their input/output rates zeroed, repectively.
 *
 * This extension to at.dms.kjc.flatgraph.StaticStreamGraph is to allow layout
 * and scheduling information to be associated with a StatisStreamGraph.
 * 
 * It is a bit confusing casting something which already appears to be
 * of type StaticStreamGraph to StaticStreamGraph, we should have had different names.
 */

public class StaticStreamGraph  extends at.dms.kjc.flatgraph.StaticStreamGraph {

    // given a flatnode map to the execution count for desired stage
    private HashMap initExecutionCounts;

    private HashMap steadyExecutionCounts;

    // stores the multiplicities as returned by the scheduler...
    private HashMap[] executionCounts;

    // the number of tiles assigned to this subgraph
    private int numTilesAssigned;

    // the tiles assigned
    //private RawTile[] tilesAssigned;

    // set of all the flat nodes of this graph...
    private LinkedList flatNodes;

    // the communication scheduler we are going to use for this graph...
    public Simulator simulator;


    /***************************************************************************
     * create a static stream graph with realTop as the first node that the
     * implicit splitter points to
     **************************************************************************/
     public StaticStreamGraph(StreamGraph sg, FlatNode realTop) {
         super(sg,realTop);
     }

    /**
     * Schedule the static communication of this SSG, given the schedule of
     * joiner firing, <pre>js</pre> *
     */
    public void scheduleCommunication(JoinerSimulator js) {
        //if there are no overlapping routes in the graph and no
        //router tiles, we do not have to run a simulator, just
        //create the switch code directly in SwitchCode.java
        //if (!Simulator.needSimulator(this)) 
        //    simulator = new NoSimulator(this, js);
        //else if (streamGraph.isSimple())                
        //    simulator = new SimpleSimulator(this, js);        
        //else 
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
        //this.tilesAssigned = new RawTile[i];
    }

    /** return the number of tiles that this SSG was assigned to occupy * */
    public int getNumTiles() {
        return this.numTilesAssigned;
    }

    /**
     * given the multiplicities created by the scheduler, put them into a format
     * that is more easily used
     */
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

//    // debug function
//    // run me after layout please
//    public static void printCounts(HashMap counts) {
//        Iterator it = counts.keySet().iterator();
//        while (it.hasNext()) {
//            FlatNode node = (FlatNode) it.next();
//            // if (Layout.joiners.contains(node))
//            System.out.println(node.contents.getName() + " "
//                               + ((Integer) counts.get(node)).intValue());
//        }
//    }

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

}
