package at.dms.kjc.slicegraph;

import java.util.*;
import java.io.FileWriter;
import java.io.FilterWriter;

import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.spacetime.MultiLevelSplitsJoins;
import at.dms.kjc.spacetime.RawChip;
import at.dms.kjc.spacetime.Trace;
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

    // the largest number of partitions that we will allow.
    // exceeding this causes assertion error.
    protected int maxPartitions;

    protected UnflatFilter[] topFilters;

    protected HashMap[] exeCounts;

    protected LinearAnalyzer lfa;

    // sirfilter -> work estimation
    protected WorkEstimate work;

    protected Trace[] topTraces;

    protected HashMap<SIRFilter, Integer> genIdWorks; 
    
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
    
    protected HashMap <SIRFilter, FilterContent> sirToContent;
    
    /**
     * Create a Partitioner.
     * 
     * The number of partitions may be limited by <i>maxPartitions</i>, but
     * some implementations ignore <i>maxPartitions</i>.
     * 
     * @param topFilters  from {@link FlattenGraph}
     * @param exeCounts  a schedule
     * @param lfa  a linearAnalyzer to convert filters to linear form if appropriate.
     * @param work a work estimate, see {@link at.dms.kjc.sir.lowering.partition}, updeted if filters are added to a slice.
     * @param maxPartitions if non-zero, a maximum number of partitions to create
     */
    public Partitioner(UnflatFilter[] topFilters, HashMap[] exeCounts,
                       LinearAnalyzer lfa, WorkEstimate work, int maxPartitions) {
        this.maxPartitions = maxPartitions;
        this.topFilters = topFilters;
        this.exeCounts = exeCounts;
        this.lfa = lfa;
        this.work = work;
        if (topFilters != null)
            topTraces = new Trace[topFilters.length];
        traceBNWork = new HashMap<Trace, Integer>();
        steadyMult = KjcOptions.steadymult;
        filterStartupCost = new HashMap<FilterTraceNode, Integer>();
        bottleNeckFilter = new HashMap<Trace, FilterTraceNode>();
        filterOccupancy = new HashMap<FilterTraceNode, Integer>();
        genIdWorks = new HashMap<SIRFilter, Integer>();
        sirToContent = new HashMap<SIRFilter, FilterContent>();
    }

    /**
     * Partition the stream graph into slices (traces) and return the traces.
     * @return The slices (traces) of the partitioned graph. 
     */
    public abstract Trace[] partition();

    /**
     * Check for I/O in slice
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
     * Get all slices
     * @return All the slices of the slice graph. 
     */
    public Trace[] getTraceGraph() {
        assert traceGraph != null;
        return traceGraph;
    }
    
    /**
     *  Get just top level slices in the slice graph.
     * @return top level slices
     */
    public Trace[] getTopTraces() {
        assert topTraces != null;
        return topTraces;
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
    public int getFilterWork(FilterTraceNode node) {
        return workEstimation.get(node.getFilter()).intValue();
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
        return traceBNWork.get(trace).intValue() * steadyMult;
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
            //start off with the first filter
            //and go forwards to find pipelining effects
            
            //FilterTraceNode bottleNeck = 
            //    bottleNeckFilter.get(trace);
            
            TraceNode prev = trace.getHead().getNextFilter();
            int prevWork = getFilterWorkSteadyMult((FilterTraceNode)prev);
            
            //set the first filter
            filterOccupancy.put((FilterTraceNode)prev, prevWork);
            
            CommonUtils.println_debugging("Setting occupancy (forward) for " + 
                    prev + " " + prevWork);
            
            //for forward from the bottleneck
            TraceNode current = prev.getNext();
            
            while (current.isFilterTrace()) {
                int occ = 
                    filterOccupancy.get((FilterTraceNode)prev).intValue() - 
                    filterStartupCost.get((FilterTraceNode)current).intValue() + 
                    getWorkEstOneFiring((FilterTraceNode)current);
                
                CommonUtils.println_debugging(filterOccupancy.get((FilterTraceNode)prev).intValue() + " - " +  
                    filterStartupCost.get((FilterTraceNode)current).intValue() + " + " +  
                    getWorkEstOneFiring((FilterTraceNode)current));
                
                assert occ > 0;
                //record either the occupany based on the previous filter, 
                //or this filter's work in the steady-state, whichever is greater
                filterOccupancy.put((FilterTraceNode)current, 
                        (getFilterWorkSteadyMult((FilterTraceNode)current) > occ) ?
                                getFilterWorkSteadyMult((FilterTraceNode)current) : 
                                    occ);
                                
                CommonUtils.println_debugging("Setting occupancy (forward) for " + current + " " + 
                        filterOccupancy.get((FilterTraceNode)current));
                
                prev = current;
                current = current.getNext();
            }
            
            //go back from the tail
            
            TraceNode next = trace.getTail().getPrevFilter();
            //if the work of the last filter is more than the occupancy calculated
            //by the forward traversal, set he occupancy to the filter's total work
            if (getFilterWorkSteadyMult((FilterTraceNode)next) > 
                getFilterOccupancy((FilterTraceNode)next))
                filterOccupancy.put((FilterTraceNode)next, 
                        getFilterWorkSteadyMult((FilterTraceNode)next));
            //set the current to the next before the last filter
            current = next.getPrevious();
            
            while (current.isFilterTrace()) {
                int occ = 
                    filterOccupancy.get((FilterTraceNode)next).intValue() + 
                    filterStartupCost.get((FilterTraceNode)next).intValue() - 
                    getWorkEstOneFiring((FilterTraceNode)next);
                
                assert occ > 0;
                //now if the backward occupancy is more than the forward occupancy, 
                //use the backward occupancy
                if (occ > getFilterOccupancy((FilterTraceNode)current)) {
                    CommonUtils.println_debugging("Setting occupancy (back) for " + current + " " + occ);   
                    filterOccupancy.put((FilterTraceNode)current, new Integer(occ));
                }
                next = current;
                current = current.getPrevious();
            }
            //check to see if everything is correct
            current = trace.getHead().getNext();
            while (current.isFilterTrace()) {
                assert  (getFilterOccupancy((FilterTraceNode)current) >=
                    getFilterWorkSteadyMult((FilterTraceNode)current)) : current;
                current = current.getNext();    
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
                CommonUtils.println_debugging("StartupCost: " + node + " " + myLag);
                filterStartupCost.put(node, new Integer(myLag));
                
                //reset the prev node and the prev startup cost...
                prevNode = node;
                
            }
            //remember the bottle neck filter
            bottleNeckFilter.put(traces[i], maxFilter);
            //on to the next trace
        }
    }
    // dump the the completed partition to a dot file
    public void dumpGraph(String filename) {
        StringBuffer buf = new StringBuffer();
        buf.append("digraph Flattend {\n");
        buf.append("size = \"8, 10.5\";\n");

        for (int i = 0; i < traceGraph.length; i++) {
            Trace trace = traceGraph[i];
            assert trace != null;
            buf.append(trace.hashCode() + " [ " + 
                    traceName(trace) + 
                    "\" ];\n");
            Trace[] next = getNext(trace/* ,parent */);
            for (int j = 0; j < next.length; j++) {
                assert next[j] != null;
                buf.append(trace.hashCode() + " -> " + next[j].hashCode()
                           + ";\n");
            }
        }

        buf.append("}\n");
        // write the file
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write(buf.toString());
            fw.close();
        } catch (Exception e) {
            System.err.println("Could not print extracted traces");
        }
    }
    
    // get the downstream traces we cannot use the edge[] of trace
    // because it is for execution order and this is not determined yet.
    protected Trace[] getNext(Trace trace) {
        TraceNode node = trace.getHead();
        if (node instanceof InputTraceNode)
            node = node.getNext();
        while (node != null && node instanceof FilterTraceNode) {
            node = node.getNext();
        }
        if (node instanceof OutputTraceNode) {
            Edge[][] dests = ((OutputTraceNode) node).getDests();
            ArrayList<Object> output = new ArrayList<Object>();
            for (int i = 0; i < dests.length; i++) {
                Edge[] inner = dests[i];
                for (int j = 0; j < inner.length; j++) {
                    // Object next=parent.get(inner[j]);
                    Object next = inner[j].getDest().getParent();
                    if (!output.contains(next))
                        output.add(next);
                }
            }
            Trace[] out = new Trace[output.size()];
            output.toArray(out);
            return out;
        }
        return new Trace[0];
    }

    protected FilterContent getFilterContent(UnflatFilter f) {
        FilterContent content;

        if (f.filter instanceof SIRFileReader)
            content = new FileInputContent(f);
        else if (f.filter instanceof SIRFileWriter)
            content = new FileOutputContent(f);
        else {
            if (f.filter == null) {
                content = new FilterContent(f);
                genIdWorks.put(f.filter, MultiLevelSplitsJoins.IDENTITY_WORK *
                        f.steadyMult);
                
            } else 
                content = new FilterContent(f);
        }
        
        sirToContent.put(f.filter, content);
        return content;
    }

    public FilterContent getContent(SIRFilter f) {
        return sirToContent.get(f);
    }
    
   
    
    //return a string with all of the names of the filtertracenodes
    // and blue if linear
    protected  String traceName(Trace trace) {
        TraceNode node = trace.getHead();

        StringBuffer out = new StringBuffer();

        //do something fancy for linear traces!!!
        if (((FilterTraceNode)node.getNext()).getFilter().getArray() != null)
            out.append("color=cornflowerblue, style=filled, ");
        
        out.append("label=\"" + node.getAsInput().debugString(true));//toString());
        
        node = node.getNext();
        while (node != null ) {
            if (node.isFilterTrace()) {
                FilterContent f = node.getAsFilter().getFilter();
                out.append("\\n" + node.toString() + "{"
                        + getWorkEstimate(f)
                        + "}");
                if (f.isTwoStage())
                    out.append("\\npre:(peek, pop, push): (" + 
                            f.getInitPeek() + ", " + f.getInitPop() + "," + f.getInitPush());
                out.append(")\\n(peek, pop, push: (" + 
                        f.getPeekInt() + ", " + f.getPopInt() + ", " + f.getPushInt() + ")");
                out.append("\\nMult: init " + f.getInitMult() + ", steady " + f.getSteadyMult());
                out.append("\\n *** ");
            }
            else {
                out.append("\\n" + node.getAsOutput().debugString(true));
            }
            /*else {
                //out.append("\\n" + node.toString());
            }*/
            node = node.getNext();
        }
        return out.toString();
    }
    
    protected int getWorkEstimate(FilterContent fc) {
        assert workEstimation.containsKey(fc);
        return workEstimation.get(fc).intValue();
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
       
        return filterStartupCost.get(node).intValue();
    }
    
}
