/**
 * 
 */
package at.dms.kjc.backendSupport;

import java.util.*;

import at.dms.kjc.sir.SIRIdentity;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*; 
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SIRSlicer;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.spacetime.RawProcElements;

/**
 * This pass will break up splits or joins (OutputSliceNodes and InputSliceNodes)
 * that are wider than the number of memories attached to the chip.  It does this
 * by introducing new traces that are composed of an identity filter with width 
 * lte number of memories.  Multiple levels of these identity traces implement the
 * split or join that was too wide.
 * 
 * @author mgordon
 *
 */
public class MultiLevelSplitsJoins {
    /** The work estimate of the identity filters that we add */
    public static int IDENTITY_WORK = 3;
    /** The maximum width allowed for any split or join, it is set to the
     * number of devices*/
    private int maxWidth;
    private SIRSlicer slicer;
    
    /**
     * Create an instance of the pass that will operate on the 
     * partitioning calculated by partitioner for the number of memories
     * in RawChip.
     * 
     * @param slicer The partitioning.
     * @param maxWidth The Raw chip.
     */
    public MultiLevelSplitsJoins(SIRSlicer slicer, int maxWidth) {
        this.slicer = slicer;
        //set max width to number of devices
        this.maxWidth = maxWidth;
        System.out.println("Maxwidth: " + maxWidth);
    }
    
    /**
     * Run the pass on the entire slice graph, breaking up the 
     * wide splits and joins, creating new Slices and adding them to the 
     * partitioning.
     *
     */
    public void doit() {
        System.out.println("Break large splits / joins into multiple levels...");
        //the list of traces including any slices that are
        //created by this pass, to be installed in the partitioner at the 
        //end of this driver
        LinkedList<Slice> slices = new LinkedList<Slice>();
        int oldNumSlices = slicer.getSliceGraph().length;
        
        //cycle thru the trace graph and see if there are any 
        //splits or joins that need to be reduced...
        for (int i = 0; i < slicer.getSliceGraph().length; i++) {
            Slice slice = slicer.getSliceGraph()[i];
           
            //see if the width of the joiner is too wide and 
            //keep breaking it up until it is, adding new levels...
            while (slice.getHead().getWidth(SchedulingPhase.STEADY) > maxWidth) {
                System.out.println("Breaking up " + slice.getHead() + 
                        " width is " + slice.getHead().getWidth(SchedulingPhase.STEADY));
                breakUpJoin(slice, slices);
            }
            
            //add the original trace to the new list of traces
            slices.add(slice);
            
            //see if the width of the splitter is too wide
            while (slice.getTail().getWidth(SchedulingPhase.STEADY) > maxWidth) {
                System.out.println("Breaking up " + slice.getTail() + 
                        " width is " + slice.getTail().getWidth(SchedulingPhase.STEADY));
                breakUpSplit(slice, slices);
            }
            
        }
        //set the trace graph to the new list of traces that we have
        //calculated in this pass
        slicer.setSliceGraphNewIds(slices.toArray(new Slice[0]));
        System.out.println("Done Breaking Splits / Joins (was " + oldNumSlices + 
                " traces, now " + slicer.getSliceGraph().length + " traces).");
        
    }
    
    /**
     * Break up a joiner (InputSliceNode) using multiple levels of 
     * identity traces.
     * 
     * Do not add the trace itself to the list of traces, this is done
     * by the main driver doit().
     * 
     * @param slice
     * @param slices
     */
    private void breakUpJoin(Slice slice, LinkedList<Slice> slices) {
        //the old input trace node, it will get replaced!
        InputSliceNode input = slice.getHead();
        //nothing to do, so return...
        if (input.getWidth(SchedulingPhase.STEADY) <= maxWidth)
            return;
        
        CType type = input.getType();
        
        //the number of new input trace nodes (and thus slices needed)...
        int numNewSlices = input.getWidth(SchedulingPhase.STEADY) / maxWidth +
             (input.getWidth(SchedulingPhase.STEADY) % maxWidth > 0 ? 1 : 0); 
        
        //create the new input trace nodes
        InputSliceNode[] newInputs = new InputSliceNode[numNewSlices];
        for (int i = 0; i < numNewSlices; i++)
            newInputs[i] = new InputSliceNode(); 
        
        //now assign each of the original edges to one of the new 
        //input trace nodes
        HashMap<InterSliceEdge, Integer> assignment = new HashMap<InterSliceEdge, Integer>();
        Iterator<InterSliceEdge> sources = input.getSourceSequence(SchedulingPhase.STEADY).iterator();
        for (int i = 0; i < numNewSlices; i++) {
            int count = maxWidth;
            //account for remainder
            if (input.getWidth(SchedulingPhase.STEADY) % maxWidth != 0 &&
                    i == (numNewSlices - 1))
                count = input.getWidth(SchedulingPhase.STEADY) % maxWidth;
            for (int j = 0; j < count; j++) { 
                InterSliceEdge edge = sources.next();
                //System.out.println("Assignment: " + edge + " " + i);
                assignment.put(edge, new Integer(i));
            }
        }
        //make sure there is nothing left to assign
        assert !sources.hasNext();
        
        //the weights array of each new inputs trace node
        LinkedList<Integer>[] newInputsWeights = new LinkedList[numNewSlices];
        //the source array of each new input trace node
        LinkedList<InterSliceEdge>[] newInputsSources = new LinkedList[numNewSlices];
        for (int i = 0; i < numNewSlices; i++) {
            newInputsWeights[i] = new LinkedList<Integer>();
            newInputsSources[i] = new LinkedList<InterSliceEdge>();
         }
            
        //the weights array of the new input trace node of the orignal trace
        LinkedList<Integer> origSliceInputWeights = new LinkedList<Integer>();
        //the source array of the new input trace node of the orignal trace
        //right now it is just the index of the new trace, because we don't have
        //the edges right now
        LinkedList<Integer> origSliceInputSources = new LinkedList<Integer>();
        
        for (int i = 0; i < input.getSources(SchedulingPhase.STEADY).length; i++) {
            int index = assignment.get(input.getSources(SchedulingPhase.STEADY)[i]).intValue();
            
            newInputsWeights[index].add(new Integer(input.getWeights(SchedulingPhase.STEADY)[i]));
            newInputsSources[index].add(input.getSources(SchedulingPhase.STEADY)[i]);
            
            origSliceInputWeights.add(new Integer(input.getWeights(SchedulingPhase.STEADY)[i]));
            origSliceInputSources.add(new Integer(index));
        }
        
        //set the weights and sources of the new input trace nodes
        for (int i = 0; i < numNewSlices; i++) {
            newInputs[i].set(newInputsWeights[i], newInputsSources[i]);
            newInputs[i].canonicalize();
        }
 
        //ok so now we have all the new input trace nodes, create the new traces
        Slice[] newSlices = new Slice[numNewSlices];
                
        //now we have to create the new input trace node that will replace the
        //old one of the original trace
        InputSliceNode newInput = new InputSliceNode();
                
        //we have to fix the edges and create the new traces...
        for (int i = 0; i < numNewSlices; i++) {
            newSlices[i] = fixEdgesAndCreateSlice(newInputs[i], newInput, type);
            //add the new traces to the trace list...
            slices.add(newSlices[i]);
        }
        
        //now we can create the pattern of source edges from the index list
        //we built above
        LinkedList<InterSliceEdge> origSliceInputEdges = new LinkedList<InterSliceEdge>();
        for (int i = 0; i < origSliceInputSources.size(); i++) {
            Slice source = newSlices[origSliceInputSources.get(i).intValue()];
            origSliceInputEdges.add(source.getTail().getSingleEdge(SchedulingPhase.STEADY));
        }
        //set the pattern of the new input trace
        newInput.set(origSliceInputWeights, origSliceInputEdges);
        newInput.canonicalize();
        //now install the new input trace node in the old trace
        newInput.setNext(slice.getHead().getNext());
        slice.getHead().getNext().setPrevious(newInput);
        newInput.setParent(slice);
        slice.setHead(newInput);
        
        //set the multiplicities of the new identities of the new traces
        setMultiplicitiesJoin(newSlices);
    }
    
    /**
     * Given an InputSliceNode, create a trace that contains it
     * and an Identity filter.  For this new trace, install a new   
     * output trace node, with a new edge that points to dest. 
     * 
     * @param node The input trace node of the new trace.
     * @param dest The downstream input trace node of the new trace.
     * @param type The type of the identity trace.
     * 
     * @return The new trace.
     */
    private Slice fixEdgesAndCreateSlice(InputSliceNode node, 
            InputSliceNode dest, CType type) {
        //make sure that all of the edges coming into this 
        //input point to it...
        for (int i = 0; i < node.getSources(SchedulingPhase.STEADY).length; i++) {
            node.getSources(SchedulingPhase.STEADY)[i].setDest(node);
        }
        
        
        SIRFilter identity = new SIRIdentity(type);
        RenameAll.renameAllFilters(identity);
        //System.out.println("Creating " + identity + " for joining.");
        
        //create the identity filter node...
        FilterSliceNode filter = 
            new FilterSliceNode(new FilterContent(identity));
        
        //create the outputSliceNode
        InterSliceEdge edge = new InterSliceEdge(dest);
        OutputSliceNode output = new OutputSliceNode(new int[]{1}, 
                new InterSliceEdge[][]{{edge}});
        edge.setSrc(output);

        //set the intra trace connections
        node.setNext(filter);
        filter.setPrevious(node);
        filter.setNext(output);
        output.setPrevious(filter);
        
        //the new trace
        Slice slice = new Slice(node);
        slice.finish();
        
        return slice;
    }
    
    /**
     * Set the multiplicities (init and steady) of the new identity
     * based on the downstream filter, the original splitter trace, for the 
     * steady state, and the upstream filters for the initialization multiplicity.
     * 
     * @param traces The new traces.
     */
    private void setMultiplicitiesJoin(Slice[] traces) {
        for (int i = 0; i < traces.length; i++) {
            InterSliceEdge downEdge = traces[i].getTail().getSingleEdge(SchedulingPhase.STEADY);
            
            //the downstream filter
            FilterContent next = downEdge.getDest().getNextFilter().getFilter();
            //System.out.println(next + " receives " + next.getSteadyMult() * next.getPopInt());
            
            //set the steady items based on the number of items the down stream 
            //filter receives
            int steadyItems = 
                (int) (((double)(next.getSteadyMult() * next.getPopInt())) * 
                       downEdge.getDest().ratio(downEdge, SchedulingPhase.STEADY));
                    
            //System.out.println("Setting steady items " + steadyItems + " " + 
            //        traces[i].getHead().getNextFilter().getFilter());
            traces[i].getHead().getNextFilter().getFilter().setSteadyMult(steadyItems);
            
            int initItems = 0;
            int steadyItemsOther = 0;
            //set the init items baed on the number each of the upstream 
            //filters push on to each incoming edge
            InputSliceNode input = traces[i].getHead();
            for (int s = 0; s < input.getSources(SchedulingPhase.STEADY).length; s++) {
                InterSliceEdge upEdge = input.getSources(SchedulingPhase.STEADY)[s];
               FilterContent prev = upEdge.getSrc().getPrevFilter().getFilter();
               initItems += (int)(upEdge.getSrc().ratio(upEdge, SchedulingPhase.STEADY) * 
                       ((double)prev.initItemsPushed()));
               steadyItemsOther += (int)(upEdge.getSrc().ratio(upEdge, SchedulingPhase.STEADY) * 
                       ((double)(prev.getPushInt() * prev.getSteadyMult())));
            }
            traces[i].getHead().getNextFilter().getFilter().setInitMult(initItems);
            
            //this has to be greater than the requirement for the downstream filter
            //on this edge
            int initItemsNeeded = 
                (int)(downEdge.getDest().ratio(downEdge, SchedulingPhase.STEADY) * 
                        ((double)next.initItemsNeeded()));
            assert initItems >= initItemsNeeded :
               "The init mult for the Identity filter is not large enough, need " +
               initItemsNeeded + ", producing " + initItems;
        }
    }
    
    /**
     * Do not add the trace itself to the list of traces, this done
     * by the main driver doit().
     * 
     * @param slice
     * @param slices
     */
    private void breakUpSplit(Slice slice, LinkedList<Slice> slices) {
        OutputSliceNode output = slice.getTail();
       
        //do nothing if we have less than maxwidth connections
        if (output.getWidth(SchedulingPhase.STEADY) <= maxWidth)
            return;
        
        int numNewSlices = output.getWidth(SchedulingPhase.STEADY) / maxWidth + 
            (output.getWidth(SchedulingPhase.STEADY) % maxWidth > 0 ? 1 : 0);
        
        CType type = output.getType();
        
        //here are the weights and edges lists we will build for 
        //each new trace's output trace node
        LinkedList<Integer>[] newOutputsWeights = 
            new LinkedList[numNewSlices];
        LinkedList<LinkedList<InterSliceEdge>>[] newOutputsDests = 
            new LinkedList[numNewSlices];
        //now init them
        for (int i = 0; i < numNewSlices; i++) {
            newOutputsWeights[i] = new LinkedList<Integer>();
            newOutputsDests[i] = new LinkedList<LinkedList<InterSliceEdge>>();
        }
        
        
        //these are for the new output trace that will be installed for the
        //original trace, we have to use the integer index at this time
        //because during construction the edges are not created yet
        LinkedList<Integer> origSliceNewWeights = new LinkedList<Integer>();
        LinkedList<LinkedList<Integer>> origSliceNewDests = 
            new LinkedList<LinkedList<Integer>>();
        //the new output trace node for the original trace
        OutputSliceNode newOutput =  new OutputSliceNode();
        
        //assign the unique edges (dests) of the original outputtrace
        //to the new outputtraces
        HashMap<InterSliceEdge, Integer> assignment = new HashMap<InterSliceEdge, Integer>();
        System.out.println(output.getDestSequence(SchedulingPhase.STEADY).size() + " ?= " + output.getWidth(SchedulingPhase.STEADY));
        Iterator<InterSliceEdge> dests = output.getDestSequence(SchedulingPhase.STEADY).iterator();
        for (int i = 0; i < numNewSlices; i++) {
            int count = maxWidth;
            //account for remainder if there is one
            if (output.getWidth(SchedulingPhase.STEADY) % maxWidth != 0 &&
                    i == (numNewSlices - 1))
                count = output.getWidth(SchedulingPhase.STEADY) % maxWidth;
            for (int j = 0; j < count; j++) { 
                InterSliceEdge edge = dests.next();
                //System.out.println("Assignment: " + edge + " " + i);
                assignment.put(edge, new Integer(i));
            }
        }
        
        //make sure we have assigned each of the edges
        assert !dests.hasNext();
        
        //loop through the ports and construct the weights and edges of hte
        //new output traces nodes and the new output trace node of the 
        //original trace
        for (int i = 0; i < output.getDests(SchedulingPhase.STEADY).length; i++) {
            //store the distribution of edges of this port to the new output
            //traces here
            HashMap<Integer, LinkedList<InterSliceEdge>> newEdges = 
                new HashMap<Integer, LinkedList<InterSliceEdge>>();
            //the port as we construct it for the new output trace node
            //of the original trace
            LinkedList<Integer> origSlicePort = new LinkedList<Integer>();
                        
            for (int j = 0; j < output.getDests(SchedulingPhase.STEADY)[i].length; j++) {
                InterSliceEdge edge = output.getDests(SchedulingPhase.STEADY)[i][j];
                Integer index = assignment.get(edge);
                if (newEdges.containsKey(index)) {
                    //add to the existing linked list
                    newEdges.get(index).add(edge);
                }
                else {
                    //create a new linked list
                    LinkedList<InterSliceEdge> newlist = new LinkedList<InterSliceEdge>();
                    newlist.add(edge);
                    newEdges.put(index, newlist);
                }
                //add to the new pattern splitting of the original trace
                //but only add a destination if it has not been seen 
                //already for this item, it will be duplicated as necessary
                //but the new traces' output trace node
                if (!origSlicePort.contains(index))
                    origSlicePort.add(index);
            }
            
            Iterator<Integer> indices = newEdges.keySet().iterator();
            while (indices.hasNext()) {
                int index = indices.next().intValue();
                newOutputsWeights[index].add(new Integer(output.getWeights(SchedulingPhase.STEADY)[i]));
                newOutputsDests[index].add(newEdges.get(new Integer(index)));
            }
            origSliceNewDests.add(origSlicePort);
            origSliceNewWeights.add(new Integer(output.getWeights(SchedulingPhase.STEADY)[i]));
        }  
        
        Slice[] newSlices = new Slice[numNewSlices];
        
        //now create the new traces using the new outputSlicennodes
        for (int n = 0; n < numNewSlices; n++) {
            if (n < numNewSlices -1) {
                
            }
            newSlices[n] = fixEdgesAndCreateSlice(newOutputsWeights[n],
                    newOutputsDests[n], newOutput,  type);
            //add the new trace to the trace list
            slices.add(newSlices[n]);
        }
        
        //fix the outgoing edges of the trace node by creating 
        //a new output trace node
        LinkedList<LinkedList<InterSliceEdge>> origSliceNewEdges = 
            new LinkedList<LinkedList<InterSliceEdge>>();
        for (int i = 0; i < origSliceNewDests.size(); i++) {
            LinkedList<InterSliceEdge> port = new LinkedList<InterSliceEdge>();
            for (int j = 0; j < origSliceNewDests.get(i).size(); j++) {
                //convert the index into the new trace array 
                //into the single incoming edge of the new trace
                //so that it connects to the original trace
                port.add(newSlices[origSliceNewDests.get(i).get(j).intValue()].
                        getHead().getSingleEdge(SchedulingPhase.STEADY));
            }
            //add the port the pattern of output of the original trace
            origSliceNewEdges.add(port);
        }
        newOutput.set(origSliceNewWeights, origSliceNewEdges);
        
        //install the new output trace node
        newOutput.setPrevious(output.getPrevious());
        newOutput.getPrevious().setNext(newOutput);
        newOutput.setParent(slice);
        slice.setTail(newOutput);

        
        //set the multiplicities of the identity filter of the new trace
        setMultiplicitiesSplit(newSlices);
    }
    
    /**
     * Set the multiplicities (init and steady) of the new identity
     * based on the upstream filter, the original splitter trace.
     * 
     * @param slices The new traces.
     */
    private void setMultiplicitiesSplit(Slice[] slices) {
        
        for (int i = 0; i < slices.length; i++) {
            InterSliceEdge edge = slices[i].getHead().getSingleEdge(SchedulingPhase.STEADY);
     
            //the last filter of the prev (original) trace 
            FilterContent prev = 
               edge.getSrc().getPrevFilter().getFilter();
            //System.out.println("Source Total Items Steady: " + 
            //        prev.getSteadyMult() * prev.getPushInt() + " " + prev);
            
            //calculate the number of items the original trace sends to this
            //trace in the init stage
            int initItems = 
                (int) (((double) prev.initItemsPushed()) * edge.getSrc().ratio(edge, SchedulingPhase.STEADY));
            slices[i].getHead().getNextFilter().getFilter().setInitMult(initItems);
            
            //calc the number of steady items
            int steadyItems = (int) ((((double)prev.getSteadyMult()) * ((double)prev.getPushInt())) * 
                    (((double) edge.getSrc().getWeight(edge, SchedulingPhase.STEADY)) / ((double)edge.getSrc().totalWeights(SchedulingPhase.STEADY))));
            
            //System.out.println("Setting Steady Items: " + steadyItems + " " + 
            //        traces[i].getHead().getNextFilter().getFilter());
            slices[i].getHead().getNextFilter().getFilter().setSteadyMult(steadyItems);
        }
        
    }

    
    /**
     * Create a new trace given output as its outputtracenode and src as its upstream
     * trace's outputtracenode.  The trace will have a single filter which is an 
     * identity filter of type.
     * 
     * @param weights The new trace's output weights.
     * @param dests The new trace's output destination pattern.
     * @param src The src trace's output trace node.
     * @param type The type of the identity filter of the trace.
     */
    private Slice fixEdgesAndCreateSlice(LinkedList<Integer> weights,
            LinkedList<LinkedList<InterSliceEdge>> dests,
            OutputSliceNode src, CType type) {
        //create the output trace node base on the calculated pattern
        OutputSliceNode output = new OutputSliceNode(weights, dests);
                //create the input trace node that just receive from the 
        //original trace
        InputSliceNode input = new InputSliceNode(new int[]{1});
        InterSliceEdge edge = new InterSliceEdge(src, input);
        input.setSources(new InterSliceEdge[]{edge});
        
        //make sure that all of the edges of this output trace node have
        //it as their source
        for (int p = 0; p < output.getDests(SchedulingPhase.STEADY).length; p++) 
            for (int e = 0; e < output.getDests(SchedulingPhase.STEADY)[p].length; e++)
                output.getDests(SchedulingPhase.STEADY)[p][e].setSrc(output);
                
//      create the new identity filter using an sir identity...
        SIRFilter identity = new SIRIdentity(type);
        RenameAll.renameAllFilters(identity);
        //System.out.println(identity + " has " + weights.size());
        //System.out.println("Creating " + identity + " for splitting.");
        
        FilterSliceNode filter = 
            new FilterSliceNode(new FilterContent(identity));
                
        Slice slice = new Slice(input);
        
        //set up the intra-trace connections
        input.setNext(filter);
        filter.setPrevious(input);
        filter.setNext(output);
        output.setPrevious(filter);
        
        slice.finish();
        
        return slice;
    }
}