/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.flatgraph2.FilterContent;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRIdentity;
import at.dms.kjc.sir.lowering.RenameAll;

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
            int itemsToBuffer = output.totalWeights() % filter.initItemsPushed();
            int itemsIDShouldPass = filter.initItemsPushed() - itemsToBuffer;
            Trace trace = output.getParent();
            System.out.println(" * Adding buffering after " + trace.getTail().getPrevious() + 
                    " to equalize output, buffer: " + itemsToBuffer);
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
       
        //make the target minimum multiplicity the an integral number 
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
                addIDToTrace(trace, itemsToPass(trace, edge, minMult));
                editedTraces.add(trace);
            }
        }
        
        return changes;
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
