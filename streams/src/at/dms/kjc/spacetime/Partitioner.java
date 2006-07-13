package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
import java.io.FilterWriter;

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
    protected HashMap<Trace, Integer> traceBNWork;

    /** The startup cost of a filter when starting a slice */
    protected HashMap<FilterTraceNode, Integer> filterStartupCost;
    
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
    protected HashMap<Trace, FilterTraceNode> bottleNeckFilter;
    
    /**
     * This hashmap store the filters work plus any blocking that is
     * caused by the pipeline imbalance of the slice.
     */  
    protected HashMap<FilterTraceNode, Integer> filterOccupancy;
    
    public Trace[] io;

//  filtercontent -> work estimation
    protected HashMap<FilterContent, Integer> workEstimation;

    protected int steadyMult;
    
    
    
    public Partitioner(UnflatFilter[] topFilters, HashMap[] exeCounts,
                       LinearAnalyzer lfa, WorkEstimate work, RawChip rawChip) {
        this.rawChip = rawChip;
        this.topFilters = topFilters;
        this.exeCounts = exeCounts;
        this.lfa = lfa;
        this.work = work;
        topTraces = new Trace[topFilters.length];
        traceBNWork = new HashMap<Trace, Integer>();
        steadyMult = KjcOptions.steadymult;
        filterStartupCost = new HashMap<FilterTraceNode, Integer>();
        bottleNeckFilter = new HashMap<Trace, FilterTraceNode>();
        filterOccupancy = new HashMap<FilterTraceNode, Integer>();
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
     * Set the trace graph to traces.
     * 
     * @param traces The trace list to install as the new trace graph.
     */
    private void setTraceGraph(Trace[] traces) {
        traceGraph = traces;
        
        //perform some checks on the trace graph...
        for (int i = 0; i < traces.length; i++) {
            assert traceBNWork.containsKey(traces[i]) : traces[i];
            //this doesn't get filled till later
            //assert bottleNeckFilter.containsKey(traces[i]) : traces[i];
            for (int j = 0; j < traces[i].getFilterNodes().length; j++) {
                assert workEstimation.containsKey(traces[i].getFilterNodes()[j].getFilter()) :
                    traces[i].getFilterNodes()[j].getFilter();
                
            }
        }
    }
    
    /**
     * Does the the trace graph contain trace (perform a simple linear
     * search).
     * 
     * @param trace The trace to query.
     * 
     * @return True if the trace graph contains trace.
     */
    public boolean containsTrace(Trace trace) {
        for (int i = 0; i < traceGraph.length; i++) 
            if (traceGraph[i] == trace)
                return true;
        return false;
    }
    
    /**
     * Update all the necesary state to add node to trace.
     * 
     * @param node The node to add.
     * @param trace The trace to add the node to.
     */
    public void addFilterToTrace(FilterTraceNode node, 
            Trace trace) {
        int workEst = MultiLevelSplitsJoins.IDENTITY_WORK *
               node.getFilter().getSteadyMult();
        
        //add the node to the work estimation
        if (!workEstimation.containsKey(node.getFilter()))
            workEstimation.put(node.getFilter(),
                    new Integer(workEst));
        
        if (workEst > traceBNWork.get(trace).intValue()) {
            traceBNWork.put(trace, new Integer(workEst));
            bottleNeckFilter.put(trace, node);
        }
    }
    
    /**
     * Set the trace graph to traces, where the only difference between the 
     * previous trace graph and the new trace graph is the addition of identity
     * traces (meaning traces with only an identities filter).
     *  
     * @param traces The new trace graph.
     */
    public void setTraceGraphNewIds(Trace[] traces) {
        //add the new filters to the necessary structures...
        for (int i = 0; i < traces.length; i++) {
            if (!containsTrace(traces[i])) {
                assert traces[i].getFilterNodes().length == 1;
                assert traces[i].getFilterNodes()[0].toString().startsWith("Identity");
                                
                if (!workEstimation.containsKey(traces[i].getFilterNodes()[0])) {
                    //for a work estimation of an identity filter
                    //multiple the estimated cost of on item by the number
                    //of items that passes through it (determined by the schedule mult).
                    workEstimation.put(traces[i].getFilterNodes()[0].getFilter(), 
                            MultiLevelSplitsJoins.IDENTITY_WORK *
                            traces[i].getFilterNodes()[0].getFilter().getSteadyMult());
                }
                
                //remember that that the only filter, the id, is the bottleneck..
                if (!traceBNWork.containsKey(traces[i])) {
                    traceBNWork.put(traces[i], 
                            workEstimation.get(traces[i].getFilterNodes()[0].getFilter()));;
                }
                if (!bottleNeckFilter.containsKey(traces[i])) {
                    bottleNeckFilter.put(traces[i], traces[i].getFilterNodes()[0]);
                }
                
            }
        }
        //now set the new trace graph...
        setTraceGraph(traces);
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
     * This hashmap store the filters work plus any blocking that is
     * caused by the pipeline imbalance of the slice. 
     */
    public int getFilterOccupancy(FilterTraceNode filter) {
        assert filterOccupancy.containsKey(filter);
        return filterOccupancy.get(filter).intValue();
    }
     
    
    /**
     * @param trace
     * @return Return the filter of trace that does the most work. 
     */
    public FilterTraceNode getTraceBNFilter(Trace trace) {
        assert bottleNeckFilter.containsKey(trace);
        return (FilterTraceNode)bottleNeckFilter.get(trace);
    }
    
    
    public void calculateWorkStats() {
        calcStartupCost();
        calcOccupancy();
    }

    private void calcOccupancy() {
        Trace[] traces = getTraceGraph();
        for (int i = 0; i < traces.length; i++) {
            Trace trace = traces[i];
            //start off with the bottleneck
            //the bottleneck of the trace
            FilterTraceNode bottleNeck = 
                bottleNeckFilter.get(trace);
            
            int prevWork = getFilterWorkSteadyMult(bottleNeck);
            
            //set the bottleneck
            filterOccupancy.put(bottleNeck, prevWork);
            SpaceTimeBackend.println("Setting occupancy for " + bottleNeck + " " + prevWork);
            
            //for forward from the bottleneck
            TraceNode current = bottleNeck.getNext();
            TraceNode prev = bottleNeck;
            while (current.isFilterTrace()) {
                int occ = 
                    filterOccupancy.get((FilterTraceNode)prev).intValue() - 
                    filterStartupCost.get((FilterTraceNode)current).intValue() + 
                    getWorkEstOneFiring((FilterTraceNode)current);
                
                SpaceTimeBackend.println(filterOccupancy.get((FilterTraceNode)prev).intValue() + " - " +  
                    filterStartupCost.get((FilterTraceNode)current).intValue() + " + " +  
                    getWorkEstOneFiring((FilterTraceNode)current));
                
                assert occ > 0 && occ > getFilterWorkSteadyMult((FilterTraceNode)current);
                filterOccupancy.put((FilterTraceNode)current, new Integer(occ));
                SpaceTimeBackend.println("Setting occupancy (forward) for " + current + " " + occ);
                prev = current;
                current = current.getNext();
            }
            
            //go back from the bottleNeck
            current = bottleNeck.getPrevious();
            TraceNode next = bottleNeck;
            while (current.isFilterTrace()) {
                int occ = 
                    filterOccupancy.get((FilterTraceNode)next).intValue() + 
                    filterStartupCost.get((FilterTraceNode)next).intValue() - 
                    getWorkEstOneFiring((FilterTraceNode)next);
                SpaceTimeBackend.println("Setting occupancy (back) for " + current + " " + occ);   
                assert occ > 0 && occ > getFilterWorkSteadyMult((FilterTraceNode)current);
                filterOccupancy.put((FilterTraceNode)current, new Integer(occ));
                next = current;
                current = current.getPrevious();
            }
        }
    }
    
    /**
     * For each filtertracenode of the slice graph, calculate the startup
     * cost.  This is essentially the time it takes to first start the filter,
     * accounting for pipeline lag.  It is calculated for a trace of 
     * filters: F0->F1->...->Fi->...->Fn
     * 
     * startupCost(F0) = 0;
     * startupCost(Fi) = 
     *      ceil(fi_pop / fi-1_push * work(fi-1)
     *      
     * where work(fi) returns the work estimation of 1 firing of the filter.
     *
     */
    private void calcStartupCost() {
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
                           
                
                //record the startup cost
                SpaceTimeBackend.println("StartupCost: " + node + " " + myLag);
                filterStartupCost.put(node, new Integer(myLag));
                
                //reset the prev node and the prev startup cost...
                prevNode = node;
                
            }
            //remember the bottle neck filter
            bottleNeckFilter.put(traces[i], maxFilter);
            //on to the next trace
        }
    }
    
    /**
     * The cost of 1 firing of the filter, to be run after the steady multiplier
     * has been accounted for in the steady multiplicity of each filter content.
     * 
     * @param node
     * @return 
     */
    public int getWorkEstOneFiring(FilterTraceNode node) {
        return (getFilterWork(node) / (node.getFilter().getSteadyMult() / steadyMult));
    }
    
    /**
     * @param node
     * @return The startup cost for <pre>node</pre> 
     */
    public int getFilterStartupCost(FilterTraceNode node) {
        assert filterStartupCost.containsKey(node);
       
        return ((Integer)filterStartupCost.get(node)).intValue();
    }
    
}
