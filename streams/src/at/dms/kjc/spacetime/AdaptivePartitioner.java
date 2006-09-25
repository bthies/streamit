/**
 * 
 */
package at.dms.kjc.spacetime;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import at.dms.kjc.sir.*;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.flatgraph2.FileInputContent;
import at.dms.kjc.flatgraph2.FileOutputContent;
import at.dms.kjc.flatgraph2.FilterContent;
import at.dms.kjc.flatgraph2.UnflatEdge;
import at.dms.kjc.flatgraph2.UnflatFilter;
import at.dms.kjc.sir.SIRFileReader;
import at.dms.kjc.sir.SIRFileWriter;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRPredefinedFilter;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.lowering.partition.WorkList;
import java.util.*;

/**
 * @author mgordon
 * 
 */
public class AdaptivePartitioner extends Partitioner {
    
    private HashSet<SIRFilter> criticalPath;
    
    /**
     * This hashmap store the filters work plus any blocking that is
     * caused by the pipeline imbalance of the slice.
     */  
    private HashMap<UnflatFilter, Integer> unflatOccupancy;
    
    /**
     * The estimate of the amount of parallelism in the data-reorganization 
     * stage, used to determine the communication cost between two filters.
     */
    public static double DATA_REORG_PARALLEL_FACTOR;

    public AdaptivePartitioner(UnflatFilter[] topFilters, HashMap[] exeCounts,
            LinearAnalyzer lfa, WorkEstimate work, RawChip rawChip) {
        super(topFilters, exeCounts, lfa, work, rawChip);
        workEstimation = new HashMap<FilterContent, Integer>();
        
        unflatOccupancy = new HashMap<UnflatFilter, Integer>();
        
        double cpThreshold = 0.90;
        criticalPath = SpaceTimeBackend.greedyBinPacking.getCriticalpath(cpThreshold);
        
        DATA_REORG_PARALLEL_FACTOR = 2; 
            //(rawChip.getTotalTiles() - SpaceTimeBackend.greedyBinPacking.getCriticalPathTiles(cpThreshold).size());
        System.out.println("DATA_REORG_PARALLEL_FACTOR " + DATA_REORG_PARALLEL_FACTOR);
        Iterator<SIRFilter> cps = criticalPath.iterator();
        System.out.println("Greedy Critical Path (threshold = " + cpThreshold + "):");
        while (cps.hasNext()) {
            System.out.println("  " + cps.next());
        }
    }

    public boolean useSpace(SIRStream partitionedStr, SpaceTimeSchedule spaceTime,
            Layout layout) {
        //must be called after partition()
        assert traceGraph != null;
        
        ScheduleModel model = 
            new ScheduleModel(spaceTime, layout, spaceTime.getScheduleList());
        model.createModel();
        
        int spaceTimeCriticalPath = 0, spaceCriticalPath = 0;
        
        int interSliceCommCost = 0;
        
        //find the interslice communication cost for the steady state
        for (int i = 0; i < traceGraph.length; i++) {
            //for each slice find the interslice communication cost
            //first for the input
            InputTraceNode input = traceGraph[i].getHead();
            Iterator<Edge> edges = input.getSourceSet().iterator();
            while (edges.hasNext()) {
                Edge edge = edges.next();
                if (!InterTraceBuffer.getBuffer(edge).redundant()) {  
                    interSliceCommCost += (edge.steadyItems() * Util.getTypeSize(edge.getType()));
                }
            }
            //now the output
            edges = traceGraph[i].getTail().getDestSet().iterator();
            while (edges.hasNext()) {
                Edge edge = edges.next();
                               
                if (!InterTraceBuffer.getBuffer(edge).redundant()) {
                    interSliceCommCost += (edge.steadyItems() * Util.getTypeSize(edge.getType()));
                }
            }
        }
        
        //ok, now we have the total number of items transfered in the data-reorg stage
        //factor in the bandwidth and the parallelism
        interSliceCommCost = (int)(((double)(interSliceCommCost * KjcOptions.st_cyc_per_wd)) / 
                DATA_REORG_PARALLEL_FACTOR);
        
        //the critical path of the space time backend is the comm cost + the max bin (tile)
        spaceTimeCriticalPath = 
            (interSliceCommCost + model.getBottleNeckCost());
        System.out.println("SpaceTime Critical Path = " + spaceTimeCriticalPath);
        //now divide the critical path by the number of items produced!
        spaceTimeCriticalPath = spaceTimeCriticalPath / spaceTime.outputsPerSteady();
        
        //get the bottle neck for the  partitioned graph..
        WorkEstimate workEstimate = WorkEstimate.getWorkEstimate(partitionedStr);
        WorkList workList = workEstimate.getSortedFilterWork();
        spaceCriticalPath = workList.getWork(workList.size() - 1);
        System.out.println("Space Critical Path = " + spaceCriticalPath);
        spaceCriticalPath = spaceCriticalPath / 
            Util.outputsPerSteady(partitionedStr, workEstimate.getExecutionCounts());
        
        System.out.println("SpaceTime Critical Path / items = " + spaceTimeCriticalPath);
        System.out.println("Space Critical Path / items = " + spaceCriticalPath);
        //if we would be better served using space, then return true
        if (spaceCriticalPath < spaceTimeCriticalPath)
            return true;
        return false;
    }
    
    public Trace[] partition() {
        LinkedList<UnflatFilter> queue = new LinkedList<UnflatFilter>();
        HashSet<UnflatFilter> visited = new HashSet<UnflatFilter>();
        LinkedList<Trace> traces = new LinkedList<Trace>();
        LinkedList<Trace> topTracesList = new LinkedList<Trace>(); // traces with no
        // incoming dependencies
        HashSet<UnflatFilter> topUnflat = new HashSet<UnflatFilter>();

        // map unflatEdges -> Edge?
        HashMap<UnflatEdge, Edge> edges = new HashMap<UnflatEdge, Edge>();
        // add the top filters to the queue
        for (int i = 0; i < topFilters.length; i++) {
            topUnflat.add(topFilters[i]);
            queue.add(topFilters[i]);
        }

        while (!queue.isEmpty()) {
            UnflatFilter unflatFilter = queue.removeFirst();
            if (!visited.contains(unflatFilter)) {
                visited.add(unflatFilter);
                // the filter content for the new filter
                FilterContent filterContent = getFilterContent(unflatFilter);
                // remember the work estimation based on the filter content
                int workEstimate = getWorkEstimate(unflatFilter);
                workEstimation.put(filterContent, new Integer(workEstimate));

                TraceNode node;
                Trace trace;
                int filtersInTrace = 1;

                // System.out.println("** Creating trace with first filter = "
                // + filterContent);

                // create the input trace node
                if (unflatFilter.in != null && unflatFilter.in.length > 0) {
                    Edge[] inEdges = new Edge[unflatFilter.in.length];
                    node = new InputTraceNode(unflatFilter.inWeights, inEdges);
                    for (int i = 0; i < unflatFilter.in.length; i++) {
                        UnflatEdge unflatEdge = unflatFilter.in[i];
                        // get the edge
                        Edge edge = edges.get(unflatEdge);
                        // we haven't see the edge before
                        if (edge == null) { // set dest?, wouldn't this always
                            // be the dest
                            edge = new Edge((InputTraceNode) node);
                            edges.put(unflatEdge, edge);
                        } else
                            // we've seen this edge before, set the dest to this
                            // node
                            edge.setDest((InputTraceNode) node);
                        inEdges[i] = edge;
                    }
                    trace = new Trace((InputTraceNode) node);

                    if (filterContent.isLinear()) { // Jasper's linear stuff??
                        System.out
                                .println("******** Found linear fitler, array is of length "
                                        + filterContent.getArray().length
                                        + " pop is "
                                        + filterContent.getPopCount());
                        // The number of "times" to fiss this linear filter...
                        int times = filterContent.getArray().length
                                / filterContent.getPopCount();
                        if (times > 1) {
                            assert rawChip.getTotalTiles() == 16 : "Only 4x4 layouts supported right now";

                            // for now force to execute on 16 tiles
                            if (times > rawChip.getTotalTiles())
                                times = rawChip.getTotalTiles();
                            // fiss the filter into times elements
                            FilterContent[] fissedFilters = LinearFission.fiss(
                                    filterContent, times);
                            // remove the original linear filter from the work
                            // estimation
                            workEstimation.remove(filterContent);
                            // now add the fissed filters to the trace
                            for (int i = 0; i < fissedFilters.length; i++) {
                                FilterContent fissedContent = fissedFilters[i];
                                FilterTraceNode filterNode = new FilterTraceNode(
                                        fissedContent);
                                node.setNext(filterNode);
                                filterNode.setPrevious(node);
                                node = filterNode;
                                // Dummy work estimate for now
                                workEstimation.put(fissedContent, new Integer(
                                        workEstimate / times));
                            }
                        } else {
                            FilterTraceNode filterNode = new FilterTraceNode(
                                    filterContent);
                            node.setNext(filterNode);
                            filterNode.setPrevious(node);
                            node = filterNode;
                        }
                    } else {
                        FilterTraceNode filterNode = new FilterTraceNode(
                                filterContent);
                        node.setNext(filterNode);
                        filterNode.setPrevious(node);
                        node = filterNode;
                    }
                } else { // null incoming arcs
                    node = new FilterTraceNode(filterContent);
                    trace = new Trace(node);
                }

                if (topUnflat.contains(unflatFilter)) {
                    assert unflatFilter.in == null
                            || unflatFilter.in.length == 0;
                    topTracesList.add(trace);
                } else
                    assert unflatFilter.in.length > 0;

                // should be at least one filter in the trace by now, don't
                // worry about
                // linear stuff right now...

                traces.add(trace);

                
                LinkedList<UnflatFilter> traceSoFar = new LinkedList<UnflatFilter>();
                traceSoFar.add(unflatFilter);
                int bottleNeckWork = getWorkEstimate(unflatFilter);
                unflatOccupancy.put(unflatFilter, bottleNeckWork * KjcOptions.steadymult);
                
                // try to add more filters to the trace...
                while (continueTrace(unflatFilter, filterContent.isLinear(),
                        traceSoFar, ++filtersInTrace)) { // tell continue
                    // trace you are
                    // trying to put
                    // another filter in the trace
                    UnflatFilter downstream = unflatFilter.out[0][0].dest;
                    FilterContent dsContent = getFilterContent(downstream);

                    // remember the work estimation based on the filter content
                    workEstimation.put(dsContent, new Integer(
                            getWorkEstimate(downstream)));
                    if (getWorkEstimate(downstream) > bottleNeckWork)
                        bottleNeckWork = getWorkEstimate(downstream);                
                    traceSoFar.add(downstream);
                    // if we get here we are contecting another linear filters
                    // to a
                    // previous linear filter
                    if (dsContent.isLinear()) {
                        assert false : "Trying to add a 2 different linear filters to a trace (Not supported Yet)";
                        // the code for this case is broken
                        // the number of times to fiss the linear filter
                        int times = dsContent.getArray().length
                                / dsContent.getPopCount();
                        if (times > 1) {
                            if (times > 16)
                                times = 16;
                            FilterContent[] fissedFilters = LinearFission.fiss(
                                    dsContent, times);
                            workEstimation.remove(dsContent);
                            // create filter nodes for each row of the matrix?
                            for (int i = 0; i < fissedFilters.length; i++) {
                                FilterContent fissedContent = fissedFilters[i];
                                FilterTraceNode filterNode = new FilterTraceNode(
                                        fissedContent);
                                node.setNext(filterNode);
                                filterNode.setPrevious(node);
                                node = filterNode;
                                unflatFilter = downstream;
                                // Dummy work estimate for now
                                workEstimation.put(fissedContent, new Integer(
                                        workEstimate / times));
                            }
                        } else if (!(downstream.filter instanceof SIRPredefinedFilter)) {
                            FilterTraceNode filterNode = new FilterTraceNode(
                                    dsContent);
                            node.setNext(filterNode);
                            filterNode.setPrevious(node);
                            node = filterNode;
                            unflatFilter = downstream;
                        }
                    } else if (!(downstream.filter instanceof SIRPredefinedFilter)) {
                        FilterTraceNode filterNode = new FilterTraceNode(
                                dsContent);
                        node.setNext(filterNode);
                        filterNode.setPrevious(node);
                        node = filterNode;
                        unflatFilter = downstream;
                    }
                }

                traceBNWork.put(trace, new Integer(bottleNeckWork));

                // we are finished the current trace, create the outputtracenode
                if (unflatFilter.out != null && unflatFilter.out.length > 0) {
                    Edge[][] outEdges = new Edge[unflatFilter.out.length][];
                    OutputTraceNode outNode = new OutputTraceNode(
                            unflatFilter.outWeights, outEdges);
                    node.setNext(outNode);
                    outNode.setPrevious(node);
                    for (int i = 0; i < unflatFilter.out.length; i++) {
                        UnflatEdge[] inner = unflatFilter.out[i];
                        Edge[] innerEdges = new Edge[inner.length];
                        outEdges[i] = innerEdges;
                        for (int j = 0; j < inner.length; j++) {
                            UnflatEdge unflatEdge = inner[j];
                            UnflatFilter dest = unflatEdge.dest;
                            // if we didn't visit one of the dests, add it
                            if (!visited.contains(dest))
                                queue.add(dest);
                            Edge edge = edges.get(unflatEdge);
                            if (edge == null) {
                                edge = new Edge(outNode);
                                edges.put(unflatEdge, edge);
                            } else
                                edge.setSrc(outNode);
                            innerEdges[j] = edge;
                        }
                    }
                }
                trace.finish();
            }
        }

        traceGraph = new Trace[traces.size()];
        traces.toArray(traceGraph);
        topTracesList.toArray(topTraces);
        setupIO();
        return traceGraph;
    }

    private void setupIO() {
        int len = traceGraph.length;
        int newLen = len;
        for (int i = 0; i < len; i++)
            if (((FilterTraceNode) traceGraph[i].getHead().getNext())
                    .isPredefined())
                newLen--;
        io = new Trace[len - newLen];
        int idx = 0;
        for (int i = 0; i < len; i++) {
            Trace trace = traceGraph[i];
            if (((FilterTraceNode) trace.getHead().getNext()).isPredefined()) {
                io[idx++] = trace;
                System.out.println(trace + " is i/o trace.");
            }
        }

    }

    
    /**
     * given
     * 
     * <pre>
     * unflatFilter
     * </pre>
     * 
     * determine if we should continue the current trace we are building
     */
    private boolean continueTrace(UnflatFilter unflatFilter, boolean isLinear, 
            LinkedList<UnflatFilter> traceSoFar, 
            int newTotalFilters) {
        // if this is not connected to anything or
        // it is connected to more than one filter or one filter it is
        // connected to is joining multiple filters
        if (unflatFilter.out != null && unflatFilter.out.length == 1
                && unflatFilter.out[0].length == 1
                && unflatFilter.out[0][0].dest.in.length < 2) {
            // this is the only dest
            UnflatFilter dest = unflatFilter.out[0][0].dest;
            // put file readers and writers in there own trace, so only keep
            // going for
            // none-predefined nodes
            if (unflatFilter.filter instanceof SIRPredefinedFilter) {
                SpaceTimeBackend.println("Cannot continue trace: (Source) "
                        + unflatFilter.filter + " is predefined");
                return false;
            }

            // don't continue if the next filter is predefined
            if (dest.filter instanceof SIRPredefinedFilter) {
                SpaceTimeBackend.println("Cannot continue trace(Dest): "
                        + dest.filter + " is predefined");
                return false;
            }

            // cut out linear filters
            if (isLinear || dest.isLinear()) {
                SpaceTimeBackend
                        .println("Cannot continue trace: Source and Dest are not congruent linearly");
                return false;
            }

            // check the size of the trace, the length must be less than number
            // of tiles + 1
            if (newTotalFilters > rawChip.getTotalTiles()) {
                SpaceTimeBackend
                        .println("Cannot continue trace: Filters == number of tiles");
                return false;
            }
            
            int steadyCommCost = steadyCommCost(unflatFilter, dest);
            int wastedCycles = wastedCycles(dest, traceSoFar);
            System.out.println("Add " + dest.filter + "? " + 
                    criticalPath.contains(dest.filter) +
                    " Comm Cost: " +
                    steadyCommCost + " > " + wastedCycles);
            //ah, here's the rub!
            if (criticalPath.contains(dest.filter) && (steadyCommCost < wastedCycles)) {
                return false;
            }
            
            // everything passed
            return true;
        }

        return false;
    }
    
    private int wastedCycles(UnflatFilter filter, LinkedList<UnflatFilter>traceSoFar) {
        UnflatFilter prevFilter = traceSoFar.get(traceSoFar.size() - 1);
        int filterWorkEst = getWorkEstimate(filter.filter) * KjcOptions.steadymult;
        int proposedOccupancy = occupancyForward(filter, prevFilter);
        
        
        if (proposedOccupancy >= filterWorkEst) {
            //System.out.println(filter + "wasted work = " + proposedOccupancy + " - " +
             //       getWorkEstimate(filter.filter) * KjcOptions.steadymult);
            //simple case, this new guy does less work than the rest of the
            //slice so far, so his occupancy is based on the upstream filters
            unflatOccupancy.put(filter, proposedOccupancy);
            //and the wasted work from the addition of this 
            //filter to the pipeline is just the new filter's wasted cycles!
            return unflatOccupancy.get(filter).intValue() - (
                    getWorkEstimate(filter.filter) * KjcOptions.steadymult);
        }
        else {
            //harder case, this filter does more work then what's in the 
            //slice so far, so calculate the *additional* wasted work from 
            //adding this slice
            int newWastedWork = 0;
            int oldWastedWork = 0;
            
            unflatOccupancy.put(filter, filterWorkEst);
            
            //now cycle backwards through the filters that are in the trace so far
            //and update their occupancy and remember their wasted cycles...
            
            //add the filter temporary to the trace list to make the calculation easier
            traceSoFar.add(filter);
            for (int i = traceSoFar.size() - 2; i >= 0; i--) {
                int currentWork = getWorkEstimate(traceSoFar.get(i));
                oldWastedWork += (unflatOccupancy.get(traceSoFar.get(i)).intValue() -
                        currentWork);
                
               
                int currentOcc = 
                    occupancyBackward(traceSoFar.get(i), traceSoFar.get(i+1));
                //make sure the newly calculated occupancy is at least as great as before
                //and remember it!
                assert currentOcc >= unflatOccupancy.get(traceSoFar.get(i)).intValue(); 
                unflatOccupancy.put(traceSoFar.get(i), currentOcc);
                
                assert currentOcc >= currentWork; 
                newWastedWork += (currentOcc - currentWork);
            }
            //remove the filter from the trace list
            traceSoFar.removeLast();
            assert newWastedWork >= oldWastedWork : newWastedWork + " >= " + oldWastedWork;
            return newWastedWork - oldWastedWork;
        }
    }
    
    private int occupancyForward(UnflatFilter filter, UnflatFilter upstream) {
        int occ = 
            unflatOccupancy.get(upstream).intValue() - 
            startupCost(filter, upstream) + 
            workEstOneFiring(filter);
        
        /*System.out.println(filter + " occupancy (foward):" + 
                unflatOccupancy.get(upstream).intValue() + " - " + 
                startupCost(filter, upstream) + " + " + 
                workEstOneFiring(filter));
        */
        assert occ > 0;
        return occ;
    }
    
    private int occupancyBackward(UnflatFilter filter, UnflatFilter downstream) {
        int occ = 
            unflatOccupancy.get(downstream).intValue() + 
            startupCost(downstream, filter) - 
            workEstOneFiring(downstream);
       /* System.out.println(filter + " Occupany (backward):" +
                unflatOccupancy.get(downstream).intValue() + " + " +
                startupCost(downstream, filter) + " - " + 
                workEstOneFiring(downstream));*/
        assert occ > 0;
        return occ;
    }
    
    private int workEstOneFiring(UnflatFilter filter) {
        return getWorkEstimate(filter.filter) / ((int[])exeCounts[1].get(filter.filter))[0];
    }
    
    private int startupCost(UnflatFilter filter, UnflatFilter upstream) {
        double prevPush = 
            upstream.filter.getPushInt();
        double myPop = filter.filter.getPopInt();
        
        int prevWorkEstOneFiring = getWorkEstimate(upstream.filter) / 
             ((int[])exeCounts[1].get(upstream.filter))[0];
        
        //how long it will take me to fire the first time 
        //after my upstream filter fires
        int myLag = 
            (int)Math.ceil( myPop / prevPush * 
                    (double) prevWorkEstOneFiring); 
                   
        return myLag;
    }
    
    private int steadyCommCost(UnflatFilter upstream, UnflatFilter downstream) {
        assert upstream.filter.getPushInt() * ((int[])exeCounts[1].get(upstream.filter))[0] == 
            downstream.filter.getPopInt() * ((int[])exeCounts[1].get(downstream.filter))[0] : 
                upstream.filter + " " + downstream.filter;
        
        int items = downstream.filter.getPopInt() * ((int[])exeCounts[1].get(downstream.filter))[0];
        items *= KjcOptions.steadymult;
        
        int cost = (int)(((double)(items * KjcOptions.st_cyc_per_wd)) / 
            DATA_REORG_PARALLEL_FACTOR);
        
        return cost;
    }
    
    // get the work estimation for a filter and multiple it by the
    // number of times a filter executes in the steady-state
    // return 0 for linear filters or predefined filters
    private int getWorkEstimate(UnflatFilter unflat) {
        if (unflat.isLinear())
            // return 0;
            return unflat.array.length * 10;
        return getWorkEstimate(unflat.filter);
    }

    private int getWorkEstimate(SIRFilter filter) {
        if (filter.getIdent().startsWith("generatedIdFilter") && 
                genIdWorks.containsKey(filter))
            return genIdWorks.get(filter).intValue();
    
        if (filter instanceof SIRPredefinedFilter) 
            return 0;
        
        assert work.getReps(filter) == ((int[]) exeCounts[1].get(filter))[0] : "Multiplicity for work estimation does not match schedule of flat graph";
        return work.getWork(filter);
    }
}
