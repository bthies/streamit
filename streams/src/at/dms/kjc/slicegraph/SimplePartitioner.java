package at.dms.kjc.slicegraph;

import java.util.*;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.spacetime.LinearFission;

/**
 * Partition the stream graph into slices where each slice is a pipeline of filters with
 * an inputslicenode and an outputslicenode (each filter is represented as a filterslicenode).
 * This partitioner attempts to make load balanced slices by examining the work estimation of the 
 * filters will constructing the slices.
 * 
 * @author mgordon
 *
 */
public class SimplePartitioner extends Partitioner {
    // slice work threshold, higher number, more restrictive, smaller slices
    private static double TRASHOLD;
    //if true, then each filter occupies its own slice, useful for debugging...
    private static final boolean ONE_FILTER_SLICES = false;
    
    // if true, make slices as long as possible ignoring the work balancing
    // (TRASHOLD)
    private static final boolean IGNORE_WORK_EST = false;
   
    
    public SimplePartitioner(UnflatFilter[] topFilters, HashMap[] exeCounts,
                             LinearAnalyzer lfa, WorkEstimate work, int maxPartitions) {
        super(topFilters, exeCounts, lfa, work, maxPartitions);
        workEstimation = new HashMap<FilterContent, Integer>();
        TRASHOLD = (double)KjcOptions.slicethresh / (double)100.0;
        System.out.println("Slice Work Threshold: " + TRASHOLD + "(" + KjcOptions.slicethresh + ")");
    }

    public Slice[] partition() {
        LinkedList<UnflatFilter> queue = new LinkedList<UnflatFilter>();
        HashSet<UnflatFilter> visited = new HashSet<UnflatFilter>();
        LinkedList<Slice> slices = new LinkedList<Slice>();
        LinkedList<Slice> topSlicesList = new LinkedList<Slice>(); // slices with no
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

                SliceNode node;
                Slice slice;
                int filtersInSlice = 1;

                //System.out.println("** Creating slice with first filter = "
                //                   + filterContent);

                // create the input slice node
                if (unflatFilter.in != null && unflatFilter.in.length > 0) {
                    Edge[] inEdges = new Edge[unflatFilter.in.length];
                    node = new InputSliceNode(unflatFilter.inWeights, inEdges);
                    for (int i = 0; i < unflatFilter.in.length; i++) {
                        UnflatEdge unflatEdge = unflatFilter.in[i];
                        // get the edge
                        Edge edge = edges.get(unflatEdge);
                        // we haven't see the edge before
                        if (edge == null) { // set dest?, wouldn't this always
                                            // be the dest
                            edge = new Edge((InputSliceNode) node);
                            edges.put(unflatEdge, edge);
                        } else
                            // we've seen this edge before, set the dest to this
                            // node
                            edge.setDest((InputSliceNode) node);
                        inEdges[i] = edge;
                    }
                    slice = new Slice((InputSliceNode) node);

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
                            //assert rawChip.getTotalTiles() == 16 : "Only 4x4 layouts supported right now";

                            // for now force to execute on 16 tiles
                            if (times > maxPartitions)
                                times = maxPartitions;
                            // fiss the filter into times elements
                            FilterContent[] fissedFilters = LinearFission.fiss(
                                                                               filterContent, times);
                            // remove the original linear filter from the work
                            // estimation
                            workEstimation.remove(filterContent);
                            // now add the fissed filters to the slice
                            for (int i = 0; i < fissedFilters.length; i++) {
                                FilterContent fissedContent = fissedFilters[i];
                                FilterSliceNode filterNode = new FilterSliceNode(
                                                                                 fissedContent);
                                node.setNext(filterNode);
                                filterNode.setPrevious(node);
                                node = filterNode;
                                // Dummy work estimate for now
                                workEstimation.put(fissedContent, new Integer(
                                                                              workEstimate / times));
                            }
                        } else {
                            FilterSliceNode filterNode = new FilterSliceNode(
                                                                             filterContent);
                            node.setNext(filterNode);
                            filterNode.setPrevious(node);
                            node = filterNode;
                        }
                    } else {
                        FilterSliceNode filterNode = new FilterSliceNode(
                                                                         filterContent);
                        node.setNext(filterNode);
                        filterNode.setPrevious(node);
                        node = filterNode;
                    }
                } else { // null incoming arcs
                    node = new FilterSliceNode(filterContent);
                    slice = new Slice(node);
                }

                if (topUnflat.contains(unflatFilter)) {
                    assert unflatFilter.in == null
                        || unflatFilter.in.length == 0;
                    topSlicesList.add(slice);
                } else
                    assert unflatFilter.in.length > 0;

                // should be at least one filter in the slice by now, don't
                // worry about
                // linear stuff right now...

                slices.add(slice);

                int bottleNeckWork = getWorkEstimate(unflatFilter);
                // try to add more filters to the slice...
                while (continueSlice(unflatFilter, filterContent.isLinear(),
                                     bottleNeckWork, ++filtersInSlice)) { // tell continue
                    // slice you are
                    // trying to put
                    // another filter in the slice
                    UnflatFilter downstream = unflatFilter.out[0][0].dest;
                    FilterContent dsContent = getFilterContent(downstream);

                    // remember the work estimation based on the filter content
                    workEstimation.put(dsContent, new Integer(
                                                              getWorkEstimate(downstream)));
                    if (getWorkEstimate(downstream) > bottleNeckWork)
                        bottleNeckWork = getWorkEstimate(downstream);
                    // if we get here we are contecting another linear filters
                    // to a
                    // previous linear filter
                    if (dsContent.isLinear()) {
                        assert false : "Trying to add a 2 different linear filters to a slice (Not supported Yet)";
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
                                FilterSliceNode filterNode = new FilterSliceNode(
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
                            FilterSliceNode filterNode = new FilterSliceNode(
                                                                             dsContent);
                            node.setNext(filterNode);
                            filterNode.setPrevious(node);
                            node = filterNode;
                            unflatFilter = downstream;
                        }
                    } else if (!(downstream.filter instanceof SIRPredefinedFilter)) {
                        FilterSliceNode filterNode = new FilterSliceNode(
                                                                         dsContent);
                        node.setNext(filterNode);
                        filterNode.setPrevious(node);
                        node = filterNode;
                        unflatFilter = downstream;
                    }
                }

                sliceBNWork.put(slice, new Integer(bottleNeckWork));

                // we are finished the current slice, create the outputslicenode
                if (unflatFilter.out != null && unflatFilter.out.length > 0) {
                    Edge[][] outEdges = new Edge[unflatFilter.out.length][];
                    OutputSliceNode outNode = new OutputSliceNode(
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
                slice.finish();
            }
        }

        sliceGraph = new Slice[slices.size()];
        slices.toArray(sliceGraph);
        topSlicesList.toArray(topSlices);
        setupIO();
        return sliceGraph;
    }

    private void setupIO() {
        int len = sliceGraph.length;
        int newLen = len;
        for (int i = 0; i < len; i++)
            if (((FilterSliceNode) sliceGraph[i].getHead().getNext())
                .isPredefined())
                newLen--;
        io = new Slice[len - newLen];
        int idx = 0;
        for (int i = 0; i < len; i++) {
            Slice slice = sliceGraph[i];
            if (((FilterSliceNode) slice.getHead().getNext()).isPredefined()) {
                io[idx++] = slice;
                System.out.println(slice + " is i/o slice.");
            }
        }
        
    }

    /**
     * given <pre>unflatFilter</pre> determine if we should continue the current slice we
     * are building
     */
    private boolean continueSlice(UnflatFilter unflatFilter, boolean isLinear,
                                  int bottleNeckWork, int newTotalFilters) {
        //always start a new slice if we only want one filter slices...
        //System.out.println("Continue Slice: " + unflatFilter.filter);
        if (ONE_FILTER_SLICES)
            return false;
        // if this is not connected to anything or
        // it is connected to more than one filter or one filter it is
        // connected to is joining multiple filters
        if (unflatFilter.out != null && unflatFilter.out.length == 1
            && unflatFilter.out[0].length == 1
            && unflatFilter.out[0][0].dest.in.length < 2) {
            // this is the only dest
            UnflatFilter dest = unflatFilter.out[0][0].dest;
            // put file readers and writers in there own slice, so only keep
            // going for
            // none-predefined nodes
            if (unflatFilter.filter instanceof SIRPredefinedFilter) {
                CommonUtils.println_debugging("Cannot continue slice: (Source) "
                                   + unflatFilter.filter + " is predefined");
                return false;
            }

            // don't continue if the next filter is predefined
            if (dest.filter instanceof SIRPredefinedFilter) {
                CommonUtils.println_debugging("Cannot continue slice(Dest): "
                                   + dest.filter + " is predefined");
                return false;
            }

            // cut out linear filters
            if (isLinear || dest.isLinear()) {
                CommonUtils
                    .println_debugging("Cannot continue slice: Source and Dest are not congruent linearly");
                return false;
            }

            // check the size of the slice, the length must be less than number
            // of tiles + 1
            if (newTotalFilters > maxPartitions) {
                CommonUtils
                    .println_debugging("Cannot continue slice: Filters > maximum alowable number of partitions");
                return false;
            }

            // check the work estimation
            int destEst = getWorkEstimate(dest);
            double ratio = (bottleNeckWork > destEst) ? 
                    (double) destEst / (double) bottleNeckWork : 
                    (double) bottleNeckWork / (double) destEst;
            ratio = Math.abs(ratio);
            // System.out.println("bottleNeckWork = " + bottleNeckWork + " / " +
            // "next = " + destEst + " = " + ratio);
            if (!IGNORE_WORK_EST && ratio < TRASHOLD) {
                //System.out.println("Cannot continue slice: " + ratio + " < " + TRASHOLD +
                //        " for " + dest);
                return false;
            }
            // everything passed
            return true;
        }

        return false;
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
        //System.out.println(filter);
        if (filter.getIdent().startsWith("generatedIdFilter") && 
                genIdWorks.containsKey(filter))
            return genIdWorks.get(filter).intValue();
                
        if (filter instanceof SIRPredefinedFilter)
            return 0;
        
        assert work.getReps(filter) == ((int[]) exeCounts[1].get(filter))[0] : "Multiplicity for work estimation does not match schedule of flat graph";
        return work.getWork(filter);
    }

   


}
