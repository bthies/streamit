package at.dms.kjc.slicegraph;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import at.dms.kjc.sir.SIRFileReader;
import at.dms.kjc.sir.SIRFileWriter;
import at.dms.kjc.backendSupport.*;

/**
 * 
 * @author mgordon
 *
 */
public abstract class Slicer {
    
    protected UnflatFilter[] topFilters;

    protected HashMap[] exeCounts;

    protected Slice[] topSlices;

    public Slice[] io;

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
    public Slicer(UnflatFilter[] topFilters, HashMap[] exeCounts) {
        this.topFilters = topFilters;
        this.exeCounts = exeCounts;
        if (topFilters != null)
            topSlices = new Slice[topFilters.length];
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
     * Get all slices
     * @return All the slices of the slice graph. 
     */
    public Slice[] getSliceGraph() {
        //new slices may have been added so we need to reconstruct the graph each time
        LinkedList<Slice> sliceGraph = 
            DataFlowOrder.getTraversal(topSlices);
        
        return sliceGraph.toArray(new Slice[sliceGraph.size()]);
    }
    
    /**
     * Get just top level slices in the slice graph.
     * 
     * @return top level slices
     */
    public Slice[] getTopSlices() {
        assert topSlices != null;
        return topSlices;
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
  
    // dump the the completed partition to a dot file
    public void dumpGraph(String filename, Layout layout) {
        Slice[] sliceGraph = getSliceGraph();
        StringBuffer buf = new StringBuffer();
        buf.append("digraph Flattend {\n");
        buf.append("size = \"8, 10.5\";\n");

        for (int i = 0; i < sliceGraph.length; i++) {
            Slice slice = sliceGraph[i];
            assert slice != null;
            buf.append(slice.hashCode() + " [ " + 
                    sliceName(slice, layout) + 
                    "\" ];\n");
            Slice[] next = getNext(slice/* ,parent */, SchedulingPhase.STEADY);
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
    protected Slice[] getNext(Slice slice, SchedulingPhase phase) {
        SliceNode node = slice.getHead();
        if (node instanceof InputSliceNode)
            node = node.getNext();
        while (node != null && node instanceof FilterSliceNode) {
            node = node.getNext();
        }
        if (node instanceof OutputSliceNode) {
            Edge[][] dests = ((OutputSliceNode) node).getDests(phase);
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
            assert f.filter != null;
            content = new FilterContent(f);
        }
        
        return content;
    }
   
    
    //return a string with all of the names of the filterslicenodes
    // and blue if linear
    protected  String sliceName(Slice slice, Layout layout) {
        SliceNode node = slice.getHead();

        StringBuffer out = new StringBuffer();

        //do something fancy for linear slices!!!
        if (((FilterSliceNode)node.getNext()).getFilter().getArray() != null)
            out.append("color=cornflowerblue, style=filled, ");
        
        out.append("label=\"" + slice.hashCode() + "\\n" +
                    node.getAsInput().debugString(true));//toString());
        
        node = node.getNext();
        while (node != null ) {
            if (node.isFilterSlice()) {
                FilterContent f = node.getAsFilter().getFilter();
                out.append("\\n" + node.toString() + "{"
                        + "}");
                if (f.isTwoStage())
                    out.append("\\npre:(peek, pop, push): (" + 
                            f.getPreworkPeek() + ", " + f.getPreworkPop() + "," + f.getPreworkPush());
                out.append(")\\n(peek, pop, push: (" + 
                        f.getPeekInt() + ", " + f.getPopInt() + ", " + f.getPushInt() + ")");
                out.append("\\nMult: init " + f.getInitMult() + ", steady " + f.getSteadyMult());
                if (layout != null) 
                    out.append("\\nTile: " + layout.getComputeNode(slice.getFirstFilter()).getUniqueId());
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
    
   
    
    /**
     * Make sure that all the {@link Slice}s are {@link SimpleSlice}s.
     */
    
    public void ensureSimpleSlices() {
        Slice[] sliceGraph = getSliceGraph();
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
    
    /**
     * Update all the necessary state to add node to slice.
     * 
     * @param node The node to add.
     * @param slice The slice to add the node to.
     */
    public void addFilterToSlice(FilterSliceNode node, 
            Slice slice) {
    }
    

}
