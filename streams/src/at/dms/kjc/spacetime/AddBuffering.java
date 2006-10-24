/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.flatgraph2.FilterContent;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRIdentity;
import at.dms.kjc.sir.lowering.RenameAll;
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterTraceNode;
import at.dms.kjc.slicegraph.InputTraceNode;
import at.dms.kjc.slicegraph.OutputTraceNode;
import at.dms.kjc.*;

/**
 * <p>After synch removal and conversion from the stream graph to the slice
 * graph, input trace nodes (joiners) or output trace nodes (splitter) may
 * not execute an integral number of times.</p>
 * <p>For an input trace node, each incoming edge may not have the same multiplicity 
 * and/or for an output trace node, the upstream filter may push a number of nodes 
 * that does not divide evenly by the total weights of the edges of the output node.</p> 
 * <p>The SpaceTime compiler assumes that there are no items buffered at input trace
 * nodes or output trace nodes, so to execute an integral number of times. To 
 * force this, this pass adds buffered at a trace by adding an identity filter to the 
 * end of a trace.  It will balance the incoming edges of an input trace node, and/or
 * make sure the output trace node executes an integral number of times.<p>
 * 
 * @author mgordon
 *
 */
public class AddBuffering {
    private SpaceTimeSchedule spaceTime;
    private HashSet<Trace> editedTraces;
    
    /**
     * Fix the input trace node and output trace node multiplicity to be
     * integral in the init stage.
     *  
     * @param spaceTime The space time schedule.
     */
    public static void doit(SpaceTimeSchedule spaceTime) {
        System.out.println("Equalizing splits and joins by buffering...");
        new AddBuffering(spaceTime).doitInternal();
    }
    
    private AddBuffering(SpaceTimeSchedule st) {
        this.spaceTime = st;
        editedTraces = new HashSet<Trace>();
    }
    
    private void doitInternal() {
        checkOutputNodes();
        checkInputNodes();
    }
    
    /**
     * Iterate over the output trace nodes of the trace graph and 
     * see if any off them don't execute an integral number of times 
     * based on the items produced by the upstream filter.  If non-integer,
     * then create an id filter to buffer items to make it integral.
     */
    private void checkOutputNodes() {
        for (int t = 0; t < spaceTime.partitioner.getTraceGraph().length; t++) {
            Trace trace = spaceTime.partitioner.getTraceGraph()[t];
            //check all the outgoing edges to see if they are balanced in the init
            //and fix them if we have to
            fixOutputNode(trace.getTail());
        }
        //don't forget to reset the filter infos after each iteration
        FilterInfo.reset();
    }
    
    /**
     * Make sure that output trace node performs an integral number of 
     * iterations in the initialization stage.
     * 
     * @param output The output trace node
     */
    private void fixOutputNode(OutputTraceNode output) {
        FilterTraceNode filterNode = output.getPrevFilter();
        FilterContent filter = filterNode.getFilter();
        
        //do nothing if nothing is pushed...
        if (filter.initItemsPushed() == 0) 
            return;
        
        if (filter.initItemsPushed() % output.totalWeights() != 0) {
            //if the number of items of produced by the filter does not
            //divide equal the total weights of the output trace node
            //we have to buffer to items by adding an upstream id filter
            int itemsToBuffer = filter.initItemsPushed() % output.totalWeights();
            int itemsIDShouldPass = filter.initItemsPushed() - itemsToBuffer;
            Trace trace = output.getParent();
            System.out.println(" * Adding buffering after " + trace.getTail().getPrevious() + 
                    " to equalize output, pass: " + itemsIDShouldPass + " buffer: " + itemsToBuffer);
            //System.out.println("   " + filter.initItemsPushed() + " % " + 
            //        output.totalWeights() + " != 0");
            addIDToTrace(trace, itemsIDShouldPass);
            
            //remember that we have edited this trace...
            editedTraces.add(trace);
        }
    }
    
    
    /**
     * Make sure that all edges of an input trace node executes an equal number
     * of times.  Do this for all input trace nodes. 
     *
     */
    private void checkInputNodes() {
        //check all the edges of the slice graph to see if they are 
        //balanced
        boolean change = false;
        do {
            //System.out.println("Iteration " + i++);
            change = false;
            for (int t = 0; t < spaceTime.partitioner.getTraceGraph().length; t++) {
                Trace trace = spaceTime.partitioner.getTraceGraph()[t];
                //check all the outgoing edges to see if they are balanced in the init
                //and fix them if we have to
                boolean currentChange 
                    = fixInputNodeBuffering(trace.getHead());
                if (currentChange)
                    change = true;
            }
            //don't forget to reset the filter infos after each iteration
            FilterInfo.reset();
            //tell the partitioner that new slices maybe have been added!
            LinkedList<Trace> newTraces = 
                DataFlowOrder.getTraversal(spaceTime.partitioner.getTraceGraph());
            spaceTime.partitioner.setTraceGraphNewIds(
                    newTraces.toArray(new Trace[newTraces.size()]));
                    
        } while (change);
    }
    
    /**
     * 
     * 
     * @param input The input trace node to fix, if needed.
     * 
     * @return true if buffering was added to the correct this input
     * trace node.
     */
    private boolean fixInputNodeBuffering(InputTraceNode input) {
        
        Iterator<Edge> edges = input.getSourceSet().iterator();
        HashMap<Edge, Double> mults = new HashMap<Edge, Double>();
        double minMult = Double.MAX_VALUE;

        //System.out.println("***** " + input + " " + input.getNext());
        
        if (!edges.hasNext())
            return false;
        
        //find the edge that has the smallest multiplicity in the
        //initialization stage
        while(edges.hasNext()) {
            Edge edge = edges.next();
            
            double mult = 
                (initItemsPushed(edge) / ((double)input.getWeight(edge)));
            
            //System.out.print(edge + " mult: " + mult + ": ");
            //System.out.println(initItemsPushed(edge) + " / " + input.getWeight(edge));
            
            //remember the mult of this edge
            
            //System.out.println(" * Mult for " + edge + " = " + mult +
            //        " = " + initItemsPushed(edge) + " / " + 
            //        input.getWeight(edge));
            
            mults.put(edge, new Double(mult));
            //remember the min, it will be the new multiplicity for all the edges
            if (mult < minMult)
                minMult = mult;
        }
       
        //make the target minimum multiplicity an integral number 
        minMult = Math.floor(minMult);
       
        //now add buffering so that the input trace node receives exactly the
        //right amount of data from each input to perform the minimum number
        //of iterations
              
        //re-iterate over the edges and if they don't have init mult
        //minMult then add buffering by adding an upstream identity 
        //filter
        boolean changes = false;
        
        edges = input.getSourceSet().iterator();
        while (edges.hasNext()) {
            Edge edge = edges.next();
            
            double myMult = mults.get(edge).doubleValue();
            //if the mult is not equal to the target mult, we must buffer
            if (myMult != minMult) {
                System.out.println(" * For " + input + " change " + edge + " from "  + 
                        mults.get(edge).doubleValue() + " to " + minMult);
                
                changes = true;
                Trace trace = edge.getSrc().getParent();
                //System.out.println(" * Adding output buffering to " + trace);
                //make sure that we have not already added buffering to this 
                //trace, if we try to fix it will mess up the already fixed input
                //trace node...
                assert !editedTraces.contains(trace) : 
                    "Trying to add different buffering amounts to " + trace;
                //the number of items that we should let pass in the init
                //stage for the upstream filter, the rest that it produces in the
                //init are buffered...
                //System.out.println("   Now pass " + itemsToPass(trace, edge, minMult));
                int itemsToPass = itemsToPass(trace, edge, minMult);
                if (legalToAddBufferingInSlice(trace, itemsToPass)) { 
                    addIDToTrace(trace, itemsToPass);
                    editedTraces.add(trace);
                }
                else {
                    //since we cannot legally add the buffering inside the upstream slice
                    //add a new buffering slice that will buffer all the items that
                    //are produced greater than the new mult
                    addNewBufferingSlice(trace, edge, 
                            (int)(((double)minMult) * input.getWeight(edge)));
                }
            }
        }
        
        return changes;
    }
    
    /**
     * 
     * @param trace The downstream trace
     * @param incoming
     * @param itemsToPassInit
     */
    private void addNewBufferingSlice(Trace upTrace, Edge edge, int itemsToPassInit) {
        System.out.println("Adding new buffering slice at edge: " + edge);
        CType type = edge.getType(); 
        
        //the downstream trace
        Trace downTrace = edge.getDest().getParent();
        
        //the new input of the new trace
        InputTraceNode newInput = new InputTraceNode(new int[]{1});
        //create the identity filter for the new trace
        SIRFilter identity = new SIRIdentity(type);
        RenameAll.renameAllFilters(identity);
        
        //create the identity filter node...
        FilterTraceNode filter = 
            new FilterTraceNode(new FilterContent(identity));
       
        //create the new output trace node
        OutputTraceNode newOutput = new OutputTraceNode(new int[]{1});
        //set the intra-slice connections
        newInput.setNext(filter);
        filter.setPrevious(newInput);
        filter.setNext(newOutput);
        newOutput.setPrevious(filter);
        
        //the new slice
        Trace bufferingTrace = new Trace(newInput);
        bufferingTrace.finish();
        
        //create the new edge that will exist between the new trace and the
        //downstream trace
        Edge newEdge = new Edge(newOutput, edge.getDest());
        
        //now install the edge at the input of the downstream trace instead 
        //of the old edge
        downTrace.getHead().replaceEdge(edge, newEdge);
        
        //reset the dest of the existing edge to be the new buffering slice
        edge.setDest(newInput);
                
        //set the sources and dests of the new input and new output
        newInput.setSources(new Edge[]{edge});
        newOutput.setDests(new Edge[][]{{newEdge}});
        
        System.out.println("   with new input: " + newInput);
        System.out.println("   with new output: " + newOutput);
        
        //set the mults of the new identity
        filter.getFilter().setInitMult(itemsToPassInit);
        FilterContent prev = upTrace.getTail().getPrevFilter().getFilter();
        
        //calc the number of steady items
        int steadyItems = (int) 
             (((double)(prev.getSteadyMult() * prev.getPushInt())) *  
                edge.getSrc().ratio(edge));
        
        System.out.println("   with initMult: " + itemsToPassInit + 
                ", steadyMult: " + steadyItems);
        
        filter.getFilter().setSteadyMult(steadyItems);
       
    }
    
    /**
     * Return true if we can legally add the buffering required for trace to the
     * inside of the trace by adding a id filter to the end of the trace.
     * 
     * This checks if there are less than num_tiles filters in the trace and if
     * the buffering required will screw up any of the filters in the init stage.
     * 
     * @param trace The trace
     * @param initItemsSent The new number of items sent to the output trace node 
     * in the init stage.
     * @return True if we can add the required buffering to the inside of the trace.
     */
    private boolean legalToAddBufferingInSlice(Trace trace, int initItemsSent) {
        if (KjcOptions.greedysched || KjcOptions.noswpipe)
            return false;
        if (trace.getFilterNodes().length >=
                spaceTime.getRawChip().getTotalTiles())
            return false;
        
        OutputTraceNode output = trace.getTail();
        
        Iterator<Edge> edges = output.getDestSet().iterator(); 
        while (edges.hasNext()) {
            Edge edge = edges.next();
            FilterInfo downstream = FilterInfo.getFilterInfo(edge.getDest().getNextFilter());
            
            int itemsRecOnEdge = (int) (((double)initItemsSent) *
                    output.ratio(edge));
            int itemsNeededOnEdge = (int) (((double)downstream.initItemsNeeded) * 
                    edge.getDest().ratio(edge));
            
            if (itemsRecOnEdge < itemsNeededOnEdge) {
                System.out.println("Cannot add buffering inside slice: " + edge);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Calculate the number of items that should be passed on to the 
     * output trace node by the new ID filter, meaning the remainder that 
     * are produced by the filter are buffered by the ID.
     * 
     * @param trace The trace we are buffering.
     * @param edge The edge that requires the buffering.
     * @param inputMult The multiplicity that is now required for the edge.
     * 
     * @return The number of items that the ID must pass to the output
     * trace node.
     */
    private int itemsToPass(Trace trace, Edge edge, double inputMult) {
        assert trace == edge.getSrc().getParent();
        OutputTraceNode output = edge.getSrc();
        InputTraceNode input = edge.getDest();
        
        //the number of items that should now flow over this edge 
        //in the init stage
        double edgeItems = (((double)inputMult) * ((double)input.getWeight(edge)));

        //System.out.println("  Edge Items = " + edgeItems + " = " + 
        //        inputMult +  " * " +  
        //                input.getWeight(edge));
        
        //System.out.println("  Items to pass = " + edgeItems + " * " +
        //        ((double)output.totalWeights() / 
        //                ((double)output.getWeight(edge))));
        
        //this is the number of items that need to be passed to the
        //output trace node inorder for edgeItems to flow on edge
        return (int)(((double)output.totalWeights() / 
                ((double)output.getWeight(edge))) *
                ((double)edgeItems));
    }
    
    /**
     * Create an identity filter and add it to the end of trace.  The
     * new identity filter will have an init multiplicity of 
     * initMult and a steady-state multiplicity equal to the number 
     * of items its upstream filter produces in the steady-state.
     *   
     * @param trace The trace to add to.
     * @param initMult The init multiplicity of the new id filter.
     */
    private void addIDToTrace(Trace trace, int initMult) {
        FilterTraceNode oldLast = trace.getTail().getPrevFilter();
     
        assert trace.getFilterNodes().length < 
            spaceTime.getRawChip().getTotalTiles() : 
                "Cannot add buffering to trace because it has (filters == tiles).";
        
        //create the identity
        SIRFilter identity = new SIRIdentity(oldLast.getFilter().getOutputType());
        RenameAll.renameAllFilters(identity);
        
        //create the identity filter node...
        FilterTraceNode filter = 
            new FilterTraceNode(new FilterContent(identity));
        
        //set the multiplicities
        filter.getFilter().setInitMult(initMult);
        filter.getFilter().setSteadyMult(oldLast.getFilter().getSteadyMult() *
                oldLast.getFilter().getPushInt());
        
        //connect and re-finish trace
        oldLast.setNext(filter);
        filter.setPrevious(oldLast);
        filter.setNext(trace.getTail());
        trace.getTail().setPrevious(filter);
        trace.finish();
        
        spaceTime.partitioner.addFilterToTrace(filter, trace);
    }
    
    /**
     * The number of items pushed onto this edge in the initialization
     * stage.
     * 
     * @param edge The edge.
     * @return The number of items pushed onto this edge in the initialization
     * stage.
     */
    private double initItemsPushed(Edge edge) {
        return ((double)edge.getSrc().getPrevFilter().getFilter().initItemsPushed()) *
        edge.getSrc().ratio(edge);
    }
}