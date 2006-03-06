package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.KjcOptions;

/**
 * An abstract class that a slice parititoner will subclass from. It holds the
 * partitioned stream graph.
 * 
 * @author mgordon
 * 
 */
public abstract class Partitioner {
    // Trace->Integer for bottleNeck work estimation
    protected HashMap traceBNWork;

    /** The startup cost of a filter when starting a slice */
    protected HashMap filterStartupCost;
    
    // the completed trace graph
    protected Trace[] traceGraph;

    protected RawChip rawChip;

    protected UnflatFilter[] topFilters;

    protected HashMap[] exeCounts;

    protected LinearAnalyzer lfa;

    // sirfilter -> work estimation
    protected WorkEstimate work;

    protected Trace[] topTraces;

    /** This hashmap maps a Trace to the FilterTraceNode that
     * has the most work;
     */ 
    protected HashMap bottleNeckFilter;
    
    public Trace[] io;

    // filtercontent -> work estimation
    protected HashMap workEstimation;

    protected int steadyMult;
    
    public Partitioner(UnflatFilter[] topFilters, HashMap[] exeCounts,
                       LinearAnalyzer lfa, WorkEstimate work, RawChip rawChip) {
        this.rawChip = rawChip;
        this.topFilters = topFilters;
        this.exeCounts = exeCounts;
        this.lfa = lfa;
        this.work = work;
        topTraces = new Trace[topFilters.length];
        traceBNWork = new HashMap();
        steadyMult = KjcOptions.steadymult;
        filterStartupCost = new HashMap();
        bottleNeckFilter = new HashMap();
    }

    /**
     * Partition the stream graph into slices (traces) and return the traces.
     * @return The slices (traces) of the partitioned graph. 
     */
    public abstract Trace[] partition();

    /**
     * @param trace
     * @return Return true if this trace is an IO trace (file reader/writer).
     */
    public boolean isIO(Trace trace) {
        for (int i = 0; i < io.length; i++) {
            if (trace == io[i])
                return true;
        }
        return false;
    }

    /**
     * @return All the traces of the trace graph. 
     */
    public Trace[] getTraceGraph() {
        assert traceGraph != null;
        return traceGraph;
    }

    /**
     * @param node The Filter 
     * @return The work estimation for the filter trace node for one steady-state
     * mult of the filter.
     */
    protected int getFilterWork(FilterTraceNode node) {
        return ((Integer) workEstimation.get(node.getFilter())).intValue();
    }

    
    /**            
     * @param node
     * @return The work estimation for the filter for one steady-state 
     * multiplied by the steady-state multiplier
     */
    public int getFilterWorkSteadyMult(FilterTraceNode node)  {
        return getFilterWork(node)  * steadyMult;
    }

    /**
     * @param trace
     * @return The work estimation for the trace (the estimation for the filter that does the
     * most work for one steady-state mult of the filter multipled by the steady state multiplier.
     */
    public int getTraceBNWork(Trace trace) {
        assert traceBNWork.containsKey(trace);
        return ((Integer) traceBNWork.get(trace)).intValue() * steadyMult;
    }
    
    /**
     * @param trace
     * @return Return the filter of trace that does the most work. 
     */
    public FilterTraceNode getTraceBNFilter(Trace trace) {
        assert bottleNeckFilter.containsKey(trace);
        return (FilterTraceNode)bottleNeckFilter.get(trace);
    }
    
    /**
     * For each filtertracenode of the slice graph, calculate the startup
     * cost.  This is essentially the time it takes to first start the filter,
     * accounting for pipeline lag.  It is calculated for a trace of 
     * filters: F0->F1->...->Fi->...->Fn
     * 
     * startupCost(F0) = 0;
     * startupCost(Fi) = startupCost(Fi-1) + 
     *      ceil(fi_pop / fi-1_push * work(fi-1)
     *      
     * where work(fi) returns the work estimation of 1 firing of the filter.
     *
     */
    public void calculateWorkStats() {
        Trace[] traces = getTraceGraph();
        for (int i = 0; i < traces.length; i++) {
            int maxWork;
            FilterTraceNode maxFilter;
            //get the first filter
            FilterTraceNode node = traces[i].getHead().getNextFilter();
            filterStartupCost.put(node, new Integer(0));
            int prevStartupCost = 0;
            FilterTraceNode prevNode = node;
            //init maxes
            maxWork = getFilterWorkSteadyMult(node);
            maxFilter = node;
            
            while (node.getNext().isFilterTrace()) {
                node = node.getNext().getAsFilter();
                
                if (getFilterWorkSteadyMult(node) > maxWork) {
                    maxWork = getFilterWorkSteadyMult(node);
                    maxFilter = node;
                }
                
                double prevPush = 
                    node.getPrevious().getAsFilter().getFilter().getPushInt();
                double myPop = node.getFilter().getPopInt();
                
                //how long it will take me to fire the first time 
                //after my upstream filter fires
                int myLag = 
                    (int)Math.ceil( myPop / prevPush * 
                            (double) getWorkEstOneFiring(prevNode)); 
                           
                
                //add the upstream's startup cost to my lag to get my startup cost
                int startupCost = prevStartupCost + 
                  myLag;
                
                //record the startup cost
                System.out.println("StartupCost: " + node + " " + startupCost);
                filterStartupCost.put(node, new Integer(startupCost));
                
                //reset the prev node and the prev startup cost...
                prevNode = node;
                prevStartupCost = startupCost;
            }
            //remember the bottle neck filter
            bottleNeckFilter.put(traces[i], maxFilter);
            //on to the next trace
        }
    }
    
    /**
     * @param node
     * @return The cost of 1 firing of the filter.
     */
    public int getWorkEstOneFiring(FilterTraceNode node) {
        return (getFilterWork(node) / node.getFilter().getSteadyMult()) * steadyMult;
    }
    
    /**
     * @param node
     * @return The startup cost for <node> 
     */
    public int getFilterStartupCost(FilterTraceNode node) {
        assert filterStartupCost.containsKey(node);
       
        return ((Integer)filterStartupCost.get(node)).intValue();
    }
    
}
