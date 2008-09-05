package at.dms.kjc.slicegraph;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.MultiLevelSplitsJoins;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.sir.SIRFileReader;
import at.dms.kjc.sir.SIRFileWriter;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;

/**
 * An abstract class that a slice partitioner will subclass from: It holds the
 * partitioned stream graph.  It contains a lot of structures that key off of SIR objects
 * for work estimation, hence the name.
 * 
 * Partitioning maps filters to slices.  This is normally not useful since most 
 * back ends will assume one filter per slice, but is sometimes used in {@link at.dms.kjc.spacetime}.
 * Partitioning also converts a SIR graph to a slice graph.
 * 
 * @author mgordon
 * 
 */
public abstract class SIRSlicer extends Slicer {
    // Slice->Integer for bottleNeck work estimation
    protected HashMap<Slice, Integer> sliceBNWork;

    /** The startup cost of a filter when starting a slice */
    protected HashMap<FilterSliceNode, Integer> filterStartupCost;
    
    // the largest number of partitions that we will allow.
    // exceeding this causes assertion error.
    protected int maxPartitions;

    protected LinearAnalyzer lfa;

    // sirfilter -> work estimation
    protected WorkEstimate work;

    protected HashMap<SIRFilter, Integer> genIdWorks; 
    
    /** This hashmap maps a Slice to the FilterSliceNode that
     * has the most work;
     */ 
    protected HashMap<Slice, FilterSliceNode> bottleNeckFilter;
    
    /**
     * This hashmap store the filters work plus any blocking that is
     * caused by the pipeline imbalance of the slice.
     */  
    protected HashMap<FilterSliceNode, Integer> filterOccupancy;
    
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
    public SIRSlicer(UnflatFilter[] topFilters, HashMap[] exeCounts,
                       LinearAnalyzer lfa, WorkEstimate work, int maxPartitions) {
        super(topFilters, exeCounts);
        this.maxPartitions = maxPartitions;
        this.topFilters = topFilters;
        this.exeCounts = exeCounts;
        this.lfa = lfa;
        this.work = work;
        if (topFilters != null)
            topSlices = new Slice[topFilters.length];
        sliceBNWork = new HashMap<Slice, Integer>();
        steadyMult = KjcOptions.steadymult;
        filterStartupCost = new HashMap<FilterSliceNode, Integer>();
        bottleNeckFilter = new HashMap<Slice, FilterSliceNode>();
        filterOccupancy = new HashMap<FilterSliceNode, Integer>();
        genIdWorks = new HashMap<SIRFilter, Integer>();
        sirToContent = new HashMap<SIRFilter, FilterContent>();
    }

    /**
     * Partition the stream graph into slices (slices) and return the slices.
     * @return The slices (slices) of the partitioned graph. 
     */
    public abstract Slice[] partition();

    /**
     * Check for I/O in slice
     * @param slice
     * @return Return true if this slice is an IO slice (file reader/writer).
     */
    public boolean isIO(Slice slice) {
        for (int i = 0; i < io.length; i++) {
            if (slice == io[i])
                return true;
        }
        return false;
    }

    /**
     *  Get just top level slices in the slice graph.
     * @return top level slices
     */
    public Slice[] getTopSlices() {
        assert topSlices != null;
        return topSlices;
    }

    /**
     * Set the slice graph to slices.
     * 
     * @param slices The slice list to install as the new slice graph.
     */
    private void setSliceGraph(Slice[] slices) {
        
        //perform some checks on the slice graph...
        for (int i = 0; i < slices.length; i++) {
            assert sliceBNWork.containsKey(slices[i]) : slices[i];
            //this doesn't get filled till later
            //assert bottleNeckFilter.containsKey(slices[i]) : slices[i];
            for (FilterSliceNode fsn : slices[i].getFilterNodes()) {
                assert workEstimation.containsKey(fsn.getFilter()) : fsn.getFilter();
            }
        }
    }
    
    /**
     * Does the the slice graph contain slice (perform a simple linear
     * search).
     * 
     * @param slice The slice to query.
     * 
     * @return True if the slice graph contains slice.
     */
    public boolean containsSlice(Slice slice) {
        Slice[] sliceGraph = getSliceGraph();
        for (int i = 0; i < sliceGraph.length; i++) 
            if (sliceGraph[i] == slice)
                return true;
        return false;
    }
    
   /*
    * work estimate for filter needed in various places. 
    */
   private int workEst(FilterSliceNode node) {
       return MultiLevelSplitsJoins.IDENTITY_WORK *
       node.getFilter().getSteadyMult();
   }
    
    /**
     * Update all the necessary state to add node to slice.
     * 
     * @param node The node to add.
     * @param slice The slice to add the node to.
     */
    public void addFilterToSlice(FilterSliceNode node, 
            Slice slice) {
        int workEst = workEst(node);
        
        //add the node to the work estimation
        if (!workEstimation.containsKey(node.getFilter()))
            workEstimation.put(node.getFilter(), workEst);
        
        if (workEst > sliceBNWork.get(slice).intValue()) {
            sliceBNWork.put(slice, workEst);
            bottleNeckFilter.put(slice, node);
        }
    }
    
    /**
     * Set the slice graph to slices, where the only difference between the 
     * previous slice graph and the new slice graph is the addition of identity
     * slices (meaning slices with only an identities filter).
     *  
     * @param slices The new slice graph.
     */
    public void setSliceGraphNewIds(Slice[] slices) {
        //add the new filters to the necessary structures...
        for (int i = 0; i < slices.length; i++) {
            if (!containsSlice(slices[i])) {
                assert slices[i].getNumFilters() == 1;
                FilterSliceNode filter = slices[i].getFilterNodes().get(0);
                assert filter.toString().startsWith("Identity");
                                
                if (!workEstimation.containsKey(filter)) {
                    //for a work estimation of an identity filter
                    //multiple the estimated cost of on item by the number
                    //of items that passes through it (determined by the schedule mult).
                    workEstimation.put(filter.getFilter(), 
                            MultiLevelSplitsJoins.IDENTITY_WORK *
                            filter.getFilter().getSteadyMult());
                }
                
                //remember that that the only filter, the id, is the bottleneck..
                if (!sliceBNWork.containsKey(slices[i])) {
                    sliceBNWork.put(slices[i], 
                            workEstimation.get(filter.getFilter()));;
                }
                if (!bottleNeckFilter.containsKey(slices[i])) {
                    bottleNeckFilter.put(slices[i], filter);
                }
                
            }
        }
        //now set the new slice graph...
        setSliceGraph(slices);
    }
    
    /**
     * @param node The Filter 
     * @return The work estimation for the filter slice node for one steady-state
     * mult of the filter.
     */
    public int getFilterWork(FilterSliceNode node) {
        return workEstimation.get(node.getFilter()).intValue();
    }

    
    /**            
     * @param node
     * @return The work estimation for the filter for one steady-state 
     * multiplied by the steady-state multiplier
     */
    public int getFilterWorkSteadyMult(FilterSliceNode node)  {
        return getFilterWork(node)  * steadyMult;
    }

    /**
     * @param slice
     * @return The work estimation for the slice (the estimation for the filter that does the
     * most work for one steady-state mult of the filter multipled by the steady state multiplier.
     */
    public int getSliceBNWork(Slice slice) {
        assert sliceBNWork.containsKey(slice);
        return sliceBNWork.get(slice).intValue() * steadyMult;
    }
    
    /**
     * This hashmap store the filters work plus any blocking that is
     * caused by the pipeline imbalance of the slice. 
     */
    public int getFilterOccupancy(FilterSliceNode filter) {
        assert filterOccupancy.containsKey(filter);
        return filterOccupancy.get(filter).intValue();
    }
     
    
    /**
     * @param slice
     * @return Return the filter of slice that does the most work. 
     */
    public FilterSliceNode getSliceBNFilter(Slice slice) {
        assert bottleNeckFilter.containsKey(slice);
        return (FilterSliceNode)bottleNeckFilter.get(slice);
    }
    
    
    public void calculateWorkStats() {
        calcStartupCost();
        calcOccupancy();
    }

    private void calcOccupancy() {
        Slice[] slices = getSliceGraph();
        for (int i = 0; i < slices.length; i++) {
            Slice slice = slices[i];
            //start off with the first filter
            //and go forwards to find pipelining effects
            
            //FilterSliceNode bottleNeck = 
            //    bottleNeckFilter.get(slice);
            
            SliceNode prev = slice.getHead().getNextFilter();
            int prevWork = getFilterWorkSteadyMult((FilterSliceNode)prev);
            
            //set the first filter
            filterOccupancy.put((FilterSliceNode)prev, prevWork);
            
            CommonUtils.println_debugging("Setting occupancy (forward) for " + 
                    prev + " " + prevWork);
            
            //for forward from the bottleneck
            SliceNode current = prev.getNext();
            
            while (current.isFilterSlice()) {
                int occ = 
                    filterOccupancy.get((FilterSliceNode)prev).intValue() - 
                    filterStartupCost.get((FilterSliceNode)current).intValue() + 
                    getWorkEstOneFiring((FilterSliceNode)current);
                
                CommonUtils.println_debugging(filterOccupancy.get((FilterSliceNode)prev).intValue() + " - " +  
                    filterStartupCost.get((FilterSliceNode)current).intValue() + " + " +  
                    getWorkEstOneFiring((FilterSliceNode)current));
                
                assert occ > 0;
                //record either the occupany based on the previous filter, 
                //or this filter's work in the steady-state, whichever is greater
                filterOccupancy.put((FilterSliceNode)current, 
                        (getFilterWorkSteadyMult((FilterSliceNode)current) > occ) ?
                                getFilterWorkSteadyMult((FilterSliceNode)current) : 
                                    occ);
                                
                CommonUtils.println_debugging("Setting occupancy (forward) for " + current + " " + 
                        filterOccupancy.get((FilterSliceNode)current));
                
                prev = current;
                current = current.getNext();
            }
            
            //go back from the tail
            
            SliceNode next = slice.getTail().getPrevFilter();
            //if the work of the last filter is more than the occupancy calculated
            //by the forward traversal, set he occupancy to the filter's total work
            if (getFilterWorkSteadyMult((FilterSliceNode)next) > 
                getFilterOccupancy((FilterSliceNode)next))
                filterOccupancy.put((FilterSliceNode)next, 
                        getFilterWorkSteadyMult((FilterSliceNode)next));
            //set the current to the next before the last filter
            current = next.getPrevious();
            
            while (current.isFilterSlice()) {
                int occ = 
                    filterOccupancy.get((FilterSliceNode)next).intValue() + 
                    filterStartupCost.get((FilterSliceNode)next).intValue() - 
                    getWorkEstOneFiring((FilterSliceNode)next);
                
                assert occ > 0;
                //now if the backward occupancy is more than the forward occupancy, 
                //use the backward occupancy
                if (occ > getFilterOccupancy((FilterSliceNode)current)) {
                    CommonUtils.println_debugging("Setting occupancy (back) for " + current + " " + occ);   
                    filterOccupancy.put((FilterSliceNode)current, new Integer(occ));
                }
                next = current;
                current = current.getPrevious();
            }
            //check to see if everything is correct
            current = slice.getHead().getNext();
            while (current.isFilterSlice()) {
                assert  (getFilterOccupancy((FilterSliceNode)current) >=
                    getFilterWorkSteadyMult((FilterSliceNode)current)) : current;
                current = current.getNext();    
            }
        }
    }
    
    /**
     * For each filterslicenode of the slice graph, calculate the startup
     * cost.  This is essentially the time it takes to first start the filter,
     * accounting for pipeline lag.  It is calculated for a slice of 
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
        Slice[] slices = getSliceGraph();
        for (int i = 0; i < slices.length; i++) {
            int maxWork;
            FilterSliceNode maxFilter;
            //get the first filter
            FilterSliceNode node = slices[i].getHead().getNextFilter();
            filterStartupCost.put(node, new Integer(0));
            int prevStartupCost = 0;
            FilterSliceNode prevNode = node;
            //init maxes
            maxWork = getFilterWorkSteadyMult(node);
            maxFilter = node;
            
            while (node.getNext().isFilterSlice()) {
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
            bottleNeckFilter.put(slices[i], maxFilter);
            //on to the next slice
        }
    }
    // dump the the completed partition to a dot file
    public void dumpGraph(String filename) {
        Slice[] sliceGraph = getSliceGraph();
        StringBuffer buf = new StringBuffer();
        buf.append("digraph Flattend {\n");
        buf.append("size = \"8, 10.5\";\n");

        for (int i = 0; i < sliceGraph.length; i++) {
            Slice slice = sliceGraph[i];
            assert slice != null;
            buf.append(slice.hashCode() + " [ " + 
                    sliceName(slice) + 
                    "\" ];\n");
            Slice[] next = getNext(slice/* ,parent */);
            for (int j = 0; j < next.length; j++) {
                assert next[j] != null;
                buf.append(slice.hashCode() + " -> " + next[j].hashCode()
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
            System.err.println("Could not print extracted slices");
        }
    }
    
    // get the downstream slices we cannot use the edge[] of slice
    // because it is for execution order and this is not determined yet.
    protected Slice[] getNext(Slice slice) {
        SliceNode node = slice.getHead();
        if (node instanceof InputSliceNode)
            node = node.getNext();
        while (node != null && node instanceof FilterSliceNode) {
            node = node.getNext();
        }
        if (node instanceof OutputSliceNode) {
            Edge[][] dests = ((OutputSliceNode) node).getDests();
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
            Slice[] out = new Slice[output.size()];
            output.toArray(out);
            return out;
        }
        return new Slice[0];
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
    
   
    
    //return a string with all of the names of the filterslicenodes
    // and blue if linear
    protected  String sliceName(Slice slice) {
        SliceNode node = slice.getHead();

        StringBuffer out = new StringBuffer();

        //do something fancy for linear slices!!!
        if (((FilterSliceNode)node.getNext()).getFilter().getArray() != null)
            out.append("color=cornflowerblue, style=filled, ");
        
        out.append("label=\"" + node.getAsInput().debugString(true));//toString());
        
        node = node.getNext();
        while (node != null ) {
            if (node.isFilterSlice()) {
                FilterContent f = node.getAsFilter().getFilter();
                out.append("\\n" + node.toString() + "{"
                        + getWorkEstimate(f)
                        + "}");
                if (f.isTwoStage())
                    out.append("\\npre:(peek, pop, push): (" + 
                            f.getPreworkPeek() + ", " + f.getPreworkPop() + "," + f.getPreworkPush());
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
    public int getWorkEstOneFiring(FilterSliceNode node) {
        return (getFilterWork(node) / (node.getFilter().getSteadyMult() / steadyMult));
    }
    
    /**
     * @param node
     * @return The startup cost for <pre>node</pre> 
     */
    public int getFilterStartupCost(FilterSliceNode node) {
        assert filterStartupCost.containsKey(node);
       
        return filterStartupCost.get(node).intValue();
    }
    
    /**
     * Make sure that all the {@link Slice}s are {@link SimpleSlice}s.
     */
    
    public void ensureSimpleSlices() {
        // update sliceGraph, topSlices, io, sliceBNWork, bottleNeckFilter
        // Assume that topSlices, io, sliceBNWork.keys(), bottleNeckFilter.keys() 
        // are all proper subsets of sliceGraph.
        List<SimpleSlice> newSliceGraph = new LinkedList<SimpleSlice>();
        Map<Slice,SimpleSlice> newtopSlices = new HashMap<Slice,SimpleSlice>();
        for (Slice s : topSlices) {newtopSlices.put(s, null);}
        Map<Slice,SimpleSlice> newIo = new HashMap<Slice,SimpleSlice>();
        for (Slice s : io) {newIo.put(s,null);}
        
        // for each slice s, derived initial simple slice ss1, following simple slices ss2 ... ssn
        // add to ss1 ... ssn to newSliceGraph,
        // replace newtopSlices: s |-> null with s -> ss1
        // replace newIo: s |-> null with s -> ss1
        Slice[] sliceGraph = getSliceGraph();
        for (Slice s : sliceGraph) {
//            if (s.getNumFilters() == 1) {
//                SimpleSlice ss = new SimpleSlice(s.getHead(), s.getFilterNodes().get(0), s.getTail());
//                newSliceGraph.add(ss);
//                if (newtopSlices.containsKey(s)) {
//                    newtopSlices.put(s,ss);
//                }
//                if (newIo.containsKey(s)) {
//                    newIo.put(s,ss);
//                }
//            } else {
                int numFilters = s.getNumFilters();
                assert numFilters != 0 : s;
                List<FilterSliceNode> fs = s.getFilterNodes();
                OutputSliceNode prevTail = null;
                for (int i = 0; i < numFilters; i++) {
                    InputSliceNode head;
                    OutputSliceNode tail;
                    FilterSliceNode f = fs.get(i);
                    // first simpleSlice has a head, otherwise create a new one.
                    if (i == 0) {
                        head = s.getHead();
                    } else {
                       /* TODO weight should probably not be 1 */
                       head = new InputSliceNode(new int[]{1});
                       // Connect tail from last iteration with head from this iteration.
                       // prevTail will not be null here...
                       InterSliceEdge prevTailToHead = new InterSliceEdge(prevTail,head);
                       head.setSources(new InterSliceEdge[]{prevTailToHead});
                       prevTail.setDests(new InterSliceEdge[][]{{prevTailToHead}});
                    }
                   if (i == numFilters - 1) {
                       tail = s.getTail();
                   } else {
                       /* TODO weight should probably not be 1 */
                       tail = new OutputSliceNode(new int[]{1});
                   }
                   prevTail = tail;
                   SimpleSlice ss = new SimpleSlice(head, f, tail);

                   // now put these slices in crect data structures.
                   newSliceGraph.add(ss);
                   if (i == 0) {
                       if (newtopSlices.containsKey(s)) {
                           newtopSlices.put(s,ss);
                       }
                       if (newIo.containsKey(s)) {
                           // check criterion used elsewhere for inclusion in io[]
                           // it is the case that a slice in io[] only contains a single filter.
                           assert f.isPredefined();
                           newIo.put(s,ss);
                       }
                   }
                }
                
            }
            
//        }
        // update arrays of slices with new info.
        sliceGraph = newSliceGraph.toArray(new Slice[newSliceGraph.size()]);
        for (int i = 0; i < topSlices.length; i++) {
            topSlices[i] = newtopSlices.get(topSlices[i]);
            assert topSlices[i] != null;
        }
        for (int i = 0; i < io.length; i++) {
            io[i] = newIo.get(io[i]);
            assert io[i] != null;
        }
        // update bottleNeckFilter, sliceBNWork.
        bottleNeckFilter = new HashMap<Slice, FilterSliceNode>();
        sliceBNWork = new HashMap<Slice, Integer>();
        for (Slice s : sliceGraph) {
            SimpleSlice ss = (SimpleSlice)s;
            int workEst = workEst(ss.getBody());
            sliceBNWork.put(ss, workEst);
            bottleNeckFilter.put(ss,ss.getBody());
        }
    }
    
    /**
     * Force creation of kopi methods and fields for predefined filters.
     */
    public void createPredefinedContent() {
        for (Slice s : getSliceGraph()) {
            for (FilterSliceNode n : s.getFilterNodes()) {
                if (n.getFilter() instanceof PredefinedContent) {
                    ((PredefinedContent)n.getFilter()).createContent();
                }
            }
        }

    }
}
