/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.sir.SIRIdentity;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.*; 
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterTraceNode;
import at.dms.kjc.slicegraph.InputTraceNode;
import at.dms.kjc.slicegraph.OutputTraceNode;
import at.dms.kjc.slicegraph.Partitioner;

/**
 * This pass will break up splits or joins (OutputTraceNodes and InputTraceNodes)
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
    private Partitioner partitioner;
    private RawChip rawChip;
    
    /**
     * Create an instance of the pass that will operate on the 
     * partitioning calculated by partitioner for the number of memories
     * in RawChip.
     * 
     * @param partitioner The partitioning.
     * @param rawChip The Raw chip.
     */
    public MultiLevelSplitsJoins(Partitioner partitioner, RawChip rawChip) {
        this.rawChip = rawChip;
        this.partitioner = partitioner;
        //set max width to number of devices
        maxWidth = rawChip.getNumDev();
        System.out.println("Maxwidth: " + maxWidth);
    }
    
    /**
     * Run the pass on the entire slice graph, breaking up the 
     * wide splits and joins, creating new Traces and adding them to the 
     * partitioning.
     *
     */
    public void doit() {
        System.out.println("Break large splits / joins into multiple levels...");
        //the list of traces including any slices that are
        //created by this pass, to be installed in the partitioner at the 
        //end of this driver
        LinkedList<Trace> traces = new LinkedList<Trace>();
        int oldNumTraces = partitioner.getTraceGraph().length;
        
        //cycle thru the trace graph and see if there are any 
        //splits or joins that need to be reduced...
        for (int i = 0; i < partitioner.getTraceGraph().length; i++) {
            Trace trace = partitioner.getTraceGraph()[i];
           
            //see if the width of the joiner is too wide and 
            //keep breaking it up until it is, adding new levels...
            while (trace.getHead().getWidth() > maxWidth) {
                System.out.println("Breaking up " + trace.getHead() + 
                        " width is " + trace.getHead().getWidth());
                breakUpJoin(trace, traces);
            }
            
            //add the original trace to the new list of traces
            traces.add(trace);
            
            //see if the width of the splitter is too wide
            while (trace.getTail().getWidth() > maxWidth) {
                System.out.println("Breaking up " + trace.getTail() + 
                        " width is " + trace.getTail().getWidth());
                breakUpSplit(trace, traces);
            }
            
        }
        //set the trace graph to the new list of traces that we have
        //calculated in this pass
        partitioner.setTraceGraphNewIds(traces.toArray(new Trace[0]));
        System.out.println("Done Breaking Splits / Joins (was " + oldNumTraces + 
                " traces, now " + partitioner.getTraceGraph().length + " traces).");
        
    }
    
    /**
     * Break up a joiner (InputTraceNode) using multiple levels of 
     * identity traces.
     * 
     * Do not add the trace itself to the list of traces, this is done
     * by the main driver doit().
     * 
     * @param trace
     * @param traces
     */
    private void breakUpJoin(Trace trace, LinkedList<Trace> traces) {
        //the old input trace node, it will get replaced!
        InputTraceNode input = trace.getHead();
        //nothing to do, so return...
        if (input.getWidth() <= maxWidth)
            return;
        
        CType type = input.getType();
        
        //the number of new input trace nodes (and thus slices needed)...
        int numNewTraces = input.getWidth() / maxWidth +
             (input.getWidth() % maxWidth > 0 ? 1 : 0); 
        
        //create the new input trace nodes
        InputTraceNode[] newInputs = new InputTraceNode[numNewTraces];
        for (int i = 0; i < numNewTraces; i++)
            newInputs[i] = new InputTraceNode(); 
        
        //now assign each of the original edges to one of the new 
        //input trace nodes
        HashMap<Edge, Integer> assignment = new HashMap<Edge, Integer>();
        Iterator<Edge> sources = input.getSourceSequence().iterator();
        for (int i = 0; i < numNewTraces; i++) {
            int count = maxWidth;
            //account for remainder
            if (input.getWidth() % maxWidth != 0 &&
                    i == (numNewTraces - 1))
                count = input.getWidth() % maxWidth;
            for (int j = 0; j < count; j++) { 
                Edge edge = sources.next();
                //System.out.println("Assignment: " + edge + " " + i);
                assignment.put(edge, new Integer(i));
            }
        }
        //make sure there is nothing left to assign
        assert !sources.hasNext();
        
        //the weights array of each new inputs trace node
        LinkedList<Integer>[] newInputsWeights = new LinkedList[numNewTraces];
        //the source array of each new input trace node
        LinkedList<Edge>[] newInputsSources = new LinkedList[numNewTraces];
        for (int i = 0; i < numNewTraces; i++) {
            newInputsWeights[i] = new LinkedList<Integer>();
            newInputsSources[i] = new LinkedList<Edge>();
         }
            
        //the weights array of the new input trace node of the orignal trace
        LinkedList<Integer> origTraceInputWeights = new LinkedList<Integer>();
        //the source array of the new input trace node of the orignal trace
        //right now it is just the index of the new trace, because we don't have
        //the edges right now
        LinkedList<Integer> origTraceInputSources = new LinkedList<Integer>();
        
        for (int i = 0; i < input.getSources().length; i++) {
            int index = assignment.get(input.getSources()[i]).intValue();
            
            newInputsWeights[index].add(new Integer(input.getWeights()[i]));
            newInputsSources[index].add(input.getSources()[i]);
            
            origTraceInputWeights.add(new Integer(input.getWeights()[i]));
            origTraceInputSources.add(new Integer(index));
        }
        
        //set the weights and sources of the new input trace nodes
        for (int i = 0; i < numNewTraces; i++) {
            newInputs[i].set(newInputsWeights[i], newInputsSources[i]);
            newInputs[i].canonicalize();
        }
 
        //ok so now we have all the new input trace nodes, create the new traces
        Trace[] newTraces = new Trace[numNewTraces];
                
        //now we have to create the new input trace node that will replace the
        //old one of the original trace
        InputTraceNode newInput = new InputTraceNode();
                
        //we have to fix the edges and create the new traces...
        for (int i = 0; i < numNewTraces; i++) {
            newTraces[i] = fixEdgesAndCreateTrace(newInputs[i], newInput, type);
            //add the new traces to the trace list...
            traces.add(newTraces[i]);
        }
        
        //now we can create the pattern of source edges from the index list
        //we built above
        LinkedList<Edge> origTraceInputEdges = new LinkedList<Edge>();
        for (int i = 0; i < origTraceInputSources.size(); i++) {
            Trace source = newTraces[origTraceInputSources.get(i).intValue()];
            origTraceInputEdges.add(source.getTail().getSingleEdge());
        }
        //set the pattern of the new input trace
        newInput.set(origTraceInputWeights, origTraceInputEdges);
        newInput.canonicalize();
        //now install the new input trace node in the old trace
        newInput.setNext(trace.getHead().getNext());
        trace.getHead().getNext().setPrevious(newInput);
        newInput.setParent(trace);
        trace.setHead(newInput);
        
        //set the multiplicities of the new identities of the new traces
        setMultiplicitiesJoin(newTraces);
    }
    
    /**
     * Given an InputTraceNode, create a trace that contains it
     * and an Identity filter.  For this new trace, install a new   
     * output trace node, with a new edge that points to dest. 
     * 
     * @param node The input trace node of the new trace.
     * @param dest The downstream input trace node of the new trace.
     * @param type The type of the identity trace.
     * 
     * @return The new trace.
     */
    private Trace fixEdgesAndCreateTrace(InputTraceNode node, 
            InputTraceNode dest, CType type) {
        //make sure that all of the edges coming into this 
        //input point to it...
        for (int i = 0; i < node.getSources().length; i++) {
            node.getSources()[i].setDest(node);
        }
        
        
        SIRFilter identity = new SIRIdentity(type);
        RenameAll.renameAllFilters(identity);
        //System.out.println("Creating " + identity + " for joining.");
        
        //create the identity filter node...
        FilterTraceNode filter = 
            new FilterTraceNode(new FilterContent(identity));
        
        //create the outputTraceNode
        Edge edge = new Edge(dest);
        OutputTraceNode output = new OutputTraceNode(new int[]{1}, 
                new Edge[][]{{edge}});
        edge.setSrc(output);

        //set the intra trace connections
        node.setNext(filter);
        filter.setPrevious(node);
        filter.setNext(output);
        output.setPrevious(filter);
        
        //the new trace
        Trace trace = new Trace(node);
        trace.finish();
        
        return trace;
    }
    
    /**
     * Set the multiplicities (init and steady) of the new identity
     * based on the downstream filter, the original splitter trace, for the 
     * steady state, and the upstream filters for the initialization multiplicity.
     * 
     * @param traces The new traces.
     */
    private void setMultiplicitiesJoin(Trace[] traces) {
        for (int i = 0; i < traces.length; i++) {
            Edge downEdge = traces[i].getTail().getSingleEdge();
            
            //the downstream filter
            FilterContent next = downEdge.getDest().getNextFilter().getFilter();
            //System.out.println(next + " receives " + next.getSteadyMult() * next.getPopInt());
            
            //set the steady items based on the number of items the down stream 
            //filter receives
            int steadyItems = 
                (int) (((double)(next.getSteadyMult() * next.getPopInt())) * 
                       downEdge.getDest().ratio(downEdge));
                    
            //System.out.println("Setting steady items " + steadyItems + " " + 
            //        traces[i].getHead().getNextFilter().getFilter());
            traces[i].getHead().getNextFilter().getFilter().setSteadyMult(steadyItems);
            
            int initItems = 0;
            int steadyItemsOther = 0;
            //set the init items baed on the number each of the upstream 
            //filters push on to each incoming edge
            InputTraceNode input = traces[i].getHead();
            for (int s = 0; s < input.getSources().length; s++) {
               Edge upEdge = input.getSources()[s];
               FilterContent prev = upEdge.getSrc().getPrevFilter().getFilter();
               initItems += (int)(upEdge.getSrc().ratio(upEdge) * 
                       ((double)prev.initItemsPushed()));
               steadyItemsOther += (int)(upEdge.getSrc().ratio(upEdge) * 
                       ((double)(prev.getPushInt() * prev.getSteadyMult())));
            }
            traces[i].getHead().getNextFilter().getFilter().setInitMult(initItems);
            
            //this has to be greater than the requirement for the downstream filter
            //on this edge
            int initItemsNeeded = 
                (int)(downEdge.getDest().ratio(downEdge) * 
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
     * @param trace
     * @param traces
     */
    private void breakUpSplit(Trace trace, LinkedList<Trace> traces) {
        OutputTraceNode output = trace.getTail();
       
        //do nothing if we have less than maxwidth connections
        if (output.getWidth() <= maxWidth)
            return;
        
        int numNewTraces = output.getWidth() / maxWidth + 
            (output.getWidth() % maxWidth > 0 ? 1 : 0);
        
        CType type = output.getType();
        
        //here are the weights and edges lists we will build for 
        //each new trace's output trace node
        LinkedList<Integer>[] newOutputsWeights = 
            new LinkedList[numNewTraces];
        LinkedList<LinkedList<Edge>>[] newOutputsDests = 
            new LinkedList[numNewTraces];
        //now init them
        for (int i = 0; i < numNewTraces; i++) {
            newOutputsWeights[i] = new LinkedList<Integer>();
            newOutputsDests[i] = new LinkedList<LinkedList<Edge>>();
        }
        
        
        //these are for the new output trace that will be installed for the
        //original trace, we have to use the integer index at this time
        //because during construction the edges are not created yet
        LinkedList<Integer> origTraceNewWeights = new LinkedList<Integer>();
        LinkedList<LinkedList<Integer>> origTraceNewDests = 
            new LinkedList<LinkedList<Integer>>();
        //the new output trace node for the original trace
        OutputTraceNode newOutput =  new OutputTraceNode();
        
        //assign the unique edges (dests) of the original outputtrace
        //to the new outputtraces
        HashMap<Edge, Integer> assignment = new HashMap<Edge, Integer>();
        System.out.println(output.getDestSequence().size() + " ?= " + output.getWidth());
        Iterator<Edge> dests = output.getDestSequence().iterator();
        for (int i = 0; i < numNewTraces; i++) {
            int count = maxWidth;
            //account for remainder if there is one
            if (output.getWidth() % maxWidth != 0 &&
                    i == (numNewTraces - 1))
                count = output.getWidth() % maxWidth;
            for (int j = 0; j < count; j++) { 
                Edge edge = dests.next();
                //System.out.println("Assignment: " + edge + " " + i);
                assignment.put(edge, new Integer(i));
            }
        }
        
        //make sure we have assigned each of the edges
        assert !dests.hasNext();
        
        //loop through the ports and construct the weights and edges of hte
        //new output traces nodes and the new output trace node of the 
        //original trace
        for (int i = 0; i < output.getDests().length; i++) {
            //store the distribution of edges of this port to the new output
            //traces here
            HashMap<Integer, LinkedList<Edge>> newEdges = 
                new HashMap<Integer, LinkedList<Edge>>();
            //the port as we construct it for the new output trace node
            //of the original trace
            LinkedList<Integer> origTracePort = new LinkedList<Integer>();
                        
            for (int j = 0; j < output.getDests()[i].length; j++) {
                Edge edge = output.getDests()[i][j];
                Integer index = assignment.get(edge);
                if (newEdges.containsKey(index)) {
                    //add to the existing linked list
                    newEdges.get(index).add(edge);
                }
                else {
                    //create a new linked list
                    LinkedList<Edge> newlist = new LinkedList<Edge>();
                    newlist.add(edge);
                    newEdges.put(index, newlist);
                }
                //add to the new pattern splitting of the original trace
                //but only add a destination if it has not been seen 
                //already for this item, it will be duplicated as necessary
                //but the new traces' output trace node
                if (!origTracePort.contains(index))
                    origTracePort.add(index);
            }
            
            Iterator<Integer> indices = newEdges.keySet().iterator();
            while (indices.hasNext()) {
                int index = indices.next().intValue();
                newOutputsWeights[index].add(new Integer(output.getWeights()[i]));
                newOutputsDests[index].add(newEdges.get(new Integer(index)));
            }
            origTraceNewDests.add(origTracePort);
            origTraceNewWeights.add(new Integer(output.getWeights()[i]));
        }  
        
        Trace[] newTraces = new Trace[numNewTraces];
        
        //now create the new traces using the new outputTracennodes
        for (int n = 0; n < numNewTraces; n++) {
            if (n < numNewTraces -1) {
                
            }
            newTraces[n] = fixEdgesAndCreateTrace(newOutputsWeights[n],
                    newOutputsDests[n], newOutput,  type);
            //add the new trace to the trace list
            traces.add(newTraces[n]);
        }
        
        //fix the outgoing edges of the trace node by creating 
        //a new output trace node
        LinkedList<LinkedList<Edge>> origTraceNewEdges = 
            new LinkedList<LinkedList<Edge>>();
        for (int i = 0; i < origTraceNewDests.size(); i++) {
            LinkedList<Edge> port = new LinkedList<Edge>();
            for (int j = 0; j < origTraceNewDests.get(i).size(); j++) {
                //convert the index into the new trace array 
                //into the single incoming edge of the new trace
                //so that it connects to the original trace
                port.add(newTraces[origTraceNewDests.get(i).get(j).intValue()].
                        getHead().getSingleEdge());
            }
            //add the port the pattern of output of the original trace
            origTraceNewEdges.add(port);
        }
        newOutput.set(origTraceNewWeights, origTraceNewEdges);
        
        //install the new output trace node
        newOutput.setPrevious(output.getPrevious());
        newOutput.getPrevious().setNext(newOutput);
        newOutput.setParent(trace);
        trace.setTail(newOutput);

        
        //set the multiplicities of the identity filter of the new trace
        setMultiplicitiesSplit(newTraces);
    }
    
    /**
     * Set the multiplicities (init and steady) of the new identity
     * based on the upstream filter, the original splitter trace.
     * 
     * @param traces The new traces.
     */
    private void setMultiplicitiesSplit(Trace[] traces) {
        
        for (int i = 0; i < traces.length; i++) {
            Edge edge = traces[i].getHead().getSingleEdge();
     
            //the last filter of the prev (original) trace 
            FilterContent prev = 
               edge.getSrc().getPrevFilter().getFilter();
            //System.out.println("Source Total Items Steady: " + 
            //        prev.getSteadyMult() * prev.getPushInt() + " " + prev);
            
            //calculate the number of items the original trace sends to this
            //trace in the init stage
            int initItems = 
                (int) (((double) prev.initItemsPushed()) * edge.getSrc().ratio(edge));
            traces[i].getHead().getNextFilter().getFilter().setInitMult(initItems);
            
            //calc the number of steady items
            int steadyItems = (int) ((((double)prev.getSteadyMult()) * ((double)prev.getPushInt())) * 
                    (((double) edge.getSrc().getWeight(edge)) / ((double)edge.getSrc().totalWeights())));
            
            //System.out.println("Setting Steady Items: " + steadyItems + " " + 
            //        traces[i].getHead().getNextFilter().getFilter());
            traces[i].getHead().getNextFilter().getFilter().setSteadyMult(steadyItems);
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
    private Trace fixEdgesAndCreateTrace(LinkedList<Integer> weights,
            LinkedList<LinkedList<Edge>> dests,
            OutputTraceNode src, CType type) {
        //create the output trace node base on the calculated pattern
        OutputTraceNode output = new OutputTraceNode(weights, dests);
                //create the input trace node that just receive from the 
        //original trace
        InputTraceNode input = new InputTraceNode(new int[]{1});
        Edge edge = new Edge(src, input);
        input.setSources(new Edge[]{edge});
        
        //make sure that all of the edges of this output trace node have
        //it as their source
        for (int p = 0; p < output.getDests().length; p++) 
            for (int e = 0; e < output.getDests()[p].length; e++)
                output.getDests()[p][e].setSrc(output);
                
//      create the new identity filter using an sir identity...
        SIRFilter identity = new SIRIdentity(type);
        RenameAll.renameAllFilters(identity);
        //System.out.println(identity + " has " + weights.size());
        //System.out.println("Creating " + identity + " for splitting.");
        
        FilterTraceNode filter = 
            new FilterTraceNode(new FilterContent(identity));
                
        Trace trace = new Trace(input);
        
        //set up the intra-trace connections
        input.setNext(filter);
        filter.setPrevious(input);
        filter.setNext(output);
        output.setPrevious(filter);
        
        trace.finish();
        
        return trace;
    }
}