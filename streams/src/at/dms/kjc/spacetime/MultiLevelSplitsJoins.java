/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.sir.SIRIdentity;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.*;

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
    }
    
    /**
     * Run the pass on the entire slice graph, breaking up the 
     * wide splits and joins, creating new Traces and adding them to the 
     * partitioning.
     *
     */
    public void doit() {
        //the list of traces including any slices that are
        //created by this pass, to be installed in the partitioner at the 
        //end of this driver
        LinkedList<Trace> traces = new LinkedList<Trace>();
        
        //cycle thru the trace graph and see if there are any 
        //splits or joins that need to be reduced...
        for (int i = 0; i < partitioner.getTraceGraph().length; i++) {
            Trace trace = partitioner.getTraceGraph()[i];
            
            //see if the width of the joiner is too wide and 
            //keep breaking it up until it is, adding new levels...
            while (trace.getHead().getWidth() > maxWidth)
                breakUpJoin(trace, traces);
           
            //add the original trace to the new list of traces
            traces.add(trace);
            
            //see if the width of the splitter is too wide
            while (trace.getTail().getWidth() > maxWidth) 
                breakUpSplit(trace, traces);
           
        }
        //set the trace graph to the new list of traces that we have
        //calculated in this pass
        partitioner.setTraceGraphNewIds(traces.toArray(new Trace[0]));
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
        
        InputTraceNode[] newInputs = new InputTraceNode[numNewTraces];
        
        //create the new input traces nodes that are full (== to maxwidth)
        for (int i = 0; i < input.getWidth() / maxWidth; i++) {
            Edge[] edges = new Edge[maxWidth];
            int[] weights = new int[maxWidth];
            
            //fill in the edges and weights
            for (int j = 0; j < maxWidth; j++) {
                edges[j] = input.getSources()[i * maxWidth + j];
                weights[j] = input.getWeights()[i * maxWidth + j];
            }
            
            newInputs[i] = new InputTraceNode(weights, edges); 
        }
        //handle the remainder
        if (input.getWidth() % maxWidth > 0) {
            Edge[] edges = new Edge[input.getWidth() % maxWidth];
            int[] weights = new int[input.getWidth() % maxWidth];
            
            for (int i = 0; i < (input.getWidth() % maxWidth); i++) {
                edges[i] = input.getSources()[(input.getWidth() / maxWidth) * maxWidth + i];
                weights[i] = input.getWeights()[(input.getWidth() / maxWidth) * maxWidth + i];
            }
            newInputs[newInputs.length - 1] = new InputTraceNode(weights, edges);
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
        
        //connects the edges and set the weights of the new input trace node of the
        //original trace
        Edge edges[] = new Edge[numNewTraces];
        int weights[] = new int[numNewTraces];
        for (int i = 0; i < numNewTraces; i++) {
            edges[i] = newTraces[i].getTail().getSingleEdge();
            //the weight of the edge is just the sum of the incoming 
            //weights of the new upstream
            weights[i] = newTraces[i].getHead().totalWeights();
        }
        newInput.setSources(edges);
        newInput.setWeights(weights);
        //now install the new input trace node in the old trace
        newInput.setNext(trace.getHead().getNext());
        newInput.setParent(trace);
        trace.setHead(newInput);
    }
    
    /**
     * Given an InputTraceNode, create a trace that contains it
     * and an Identity filter.  For this new trace, install a new   
     * output trace node, with a new edge that points to dest. 
     * 
     * @param node The input trace node of the new trace.
     * @param dest The downstream input trace node of the new trace.
     * @param type The type of the identity trace.
     * @return
     */
    private Trace fixEdgesAndCreateTrace(InputTraceNode node, 
            InputTraceNode dest, CType type) {
        //make sure that all of the edges coming into this 
        //input point to it...
        for (int i = 0; i < node.getSources().length; i++) {
            node.getSources()[i].setDest(node);
        }
        //create the identity filter node...
        FilterTraceNode filter = 
            new FilterTraceNode(new FilterContent(new SIRIdentity(type)));
        
        
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
     * Do not add the trace itself to the list of traces, this done
     * by the main driver doit().
     * 
     * @param trace
     * @param traces
     */
    private void breakUpSplit(Trace trace, LinkedList<Trace> traces) {
        
    }
    
    
}
