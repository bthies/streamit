package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.StaticStreamGraph;
//import at.dms.kjc.flatgraph.GraphFlattener;
//import at.dms.kjc.flatgraph.SSGEdge;
import at.dms.kjc.flatgraph.*;
//import at.dms.util.IRPrinter;
//import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
//import at.dms.kjc.sir.lowering.*;
//import at.dms.kjc.sir.lowering.partition.*;
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

public class SpdStaticStreamGraph  extends at.dms.kjc.flatgraph.ScheduledStaticStreamGraph {

    // the number of tiles assigned to this subgraph
    private int numTilesAssigned;

    // the tiles assigned
    //private RawTile[] tilesAssigned;

    // the communication scheduler we are going to use for this graph...
    public Simulator simulator;


    /***************************************************************************
     * create a static stream graph with realTop as the first node that the
     * implicit splitter points to
     **************************************************************************/
     public SpdStaticStreamGraph(StreamGraph sg, FlatNode realTop) {
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


    /** set the number of tiles that this SSG should occupy on the raw chip * */
    public void setNumTiles(int i) {
        this.numTilesAssigned = i;
        //this.tilesAssigned = new RawTile[i];
    }

    /** return the number of tiles that this SSG was assigned to occupy * */
    public int getNumTiles() {
        return this.numTilesAssigned;
    }

    protected void createExecutionCounts() {
        // create execution counts, before raw considerations.
        super.createExecutionCounts();
        // now, in the above calculation, an execution of a joiner node is
        // considered one cycle of all of its inputs. For the remainder of the
        // raw backend, I would like the execution of a joiner to be defined as
        // the joiner passing one data item down stream

        HashMap[] result = {initExecutionCounts,steadyExecutionCounts};

        
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



    /**
     * count the number of nodes of the flatgraph that are assigned to tiles by
     * layout *
     */
    public int countAssignedNodes() {
        int assignedNodes = 0;

        Iterator<FlatNode> nodes = flatNodes.iterator();
        while (nodes.hasNext()) {
            if (Layout.assignToATile(nodes.next()))
                assignedNodes++;
        }
        return assignedNodes;
    }
    /** accept a stream graph visitor * */
    public void accept(StreamGraphVisitor s, HashSet<SpdStaticStreamGraph> visited, boolean newHash) {
        if (newHash)
            visited = new HashSet<SpdStaticStreamGraph>();

        if (visited.contains(this))
            return;

        visited.add(this);
        s.visitStaticStreamGraph(this);

        Iterator<StaticStreamGraph> nextsIt = nextSSGs.iterator();
        while (nextsIt.hasNext()) {
            SpdStaticStreamGraph ssg = (SpdStaticStreamGraph) nextsIt.next();
            ssg.accept(s, visited, false);
        }
    }

}
