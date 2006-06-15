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
        maxWidth = 8;//rawChip.getNumDev();
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
            /*
            while (trace.getTail().getWidth() > maxWidth) {
                System.out.println("Breaking up " + trace.getTail() + 
                        " width is " + trace.getTail().getWidth());
                breakUpSplit(trace, traces);
            }
           */
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
            if (i == (numNewTraces - 1))
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
        
        //create the identity filter node...
        FilterTraceNode filter = 
            new FilterTraceNode(new FilterContent(identity));
        
        //set the multiplicity of the new identity filter
        filter.getFilter().setSteadyMult(node.totalWeights());
        
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
        OutputTraceNode output = trace.getTail();
        
        //the flattened connections of this output trace node
        Edge[] connections = output.getDestList();
       
        
        //do nothing if we have less than maxwidth connections
        if (connections.length <= maxWidth)
            return;
        
        int numNewTraces = connections.length / maxWidth + 
            (connections.length % maxWidth > 0 ? 1 : 0);
        
        //these are for the new output trace that will be installed for the
        //original trace
        LinkedList<Integer> origTraceNewWeights = new LinkedList<Integer>();
        LinkedList<LinkedList<Edge>> origTraceNewDests = 
            new LinkedList<LinkedList<Edge>>();
        //the new output trace node for the original trace
        OutputTraceNode newOutput =  new OutputTraceNode();
        
        //the new output trace nodes that will be created
        OutputTraceNode[] newOutputs = createSplitOutputTraceNodes(numNewTraces,
                output);
        //create the new input trace nodes for the new trace and 
        //the new output trace of the original trace
        InputTraceNode[] newInputs = createSplitInputTraceNodes(numNewTraces,
                output, origTraceNewWeights, origTraceNewDests, newOutput);
        
        //The new traces to create
        Trace[] newTraces = new Trace[numNewTraces];
        
        
        //now create the new traces using the new outputTracennodes
        for (int n = 0; n < numNewTraces; n++) {
            newTraces[n] = fixEdgesAndCreateTrace(newOutputs[n], newInputs[n], 
                    output,  output.getType());
            //add the new trace to the trace list
            traces.add(newTraces[n]);
        }
        
        //fix the outgoing edges of the trace node by creating 
        //a new output trace node
        newOutput.set(origTraceNewWeights, origTraceNewDests);
        
        //install the new output trace node
        newOutput.setPrevious(output.getPrevious());
        newOutput.getPrevious().setNext(newOutput);
        newOutput.setParent(trace);
        trace.setTail(newOutput);
    }
    
    
    /**
     * Create all the new input trace nodes for the new traces that are 
     * created when we break up an output trace node into multiple levels.  Also
     * create the weights and pattern for the new output trace node 
     * that will be installed for the original trace.
     * 
     * @param numNewTraces The number of new traces we are creating.
     * @param output The original output trace.
     * @param origTraceNewWeights The new weights for the original traces 
     * output trace node.
     * @param origTraceNewDests The new connection pattern for the 
     * original trace's output trace node.
     * 
     * @return The new input traces.
     */
    private InputTraceNode[] createSplitInputTraceNodes(int numNewTraces,
            OutputTraceNode output, 
            LinkedList<Integer> origTraceNewWeights, 
            LinkedList<LinkedList<Edge>> origTraceNewDests, 
            OutputTraceNode origTraceNewOutput) {
        
        InputTraceNode[] newInputs = new InputTraceNode[numNewTraces];
        //the connections of the original output trace node 
        Edge[][] edges = output.getDests();
        
        //the index into the original trace's outputtracenode's output edges
        //a port is the collection of duplicated edges, 
        //so output.getWeights()[port][edge] = an edge 
        int port = 0, edge = 0;  
        
        for (int n = 0; n < numNewTraces; n++) {
            int connected = 0;
         
            
            boolean firstBroken = false;
            boolean lastBroken = false;
            
            int middleWeight = 0, firstWeight = 0, lastWeight = 0;
            
            while (true) {
                if (edge > 0) {
                    //a port had to span multiple traces
                    firstBroken = true;
                    assert firstWeight == 0;
                    firstWeight = output.getWeights()[port];
                    //remember that the first weight should not count
                    //toward the remainder of the weights, it is separate
                    middleWeight -= firstWeight;
                }
                middleWeight +=  output.getWeights()[port];
                for (int addMe = edge; addMe < edges[port].length; addMe++) {
                    //we have added an edge
                    edge++;
                    //are we done 
                    if (++connected >= maxWidth)
                        break;
                }
                //if we have completed this port, then increment port
                //and reset edge
                if (edge >= edges[port].length) {
                    port++;
                    edge = 0;
                }
                //now check to make sure that we have not filled this trace
                if (connected >= maxWidth || port >= edges.length) {
                    if (edge > 0) {
                        //a port is spanning multiple traces
                        lastBroken = true;
                        lastWeight = output.getWeights()[port];
                        middleWeight -= lastWeight;
                    }
                    break;
                }
            }
            //we are done this new trace
            //set everything up
            
            //first handle the case where we don't have a port
            //spanning multiple input trace nodes (new traces)...
            if (!firstBroken && !lastBroken) {
                assert firstWeight == 0 && lastWeight == 0;
                //nothing broken, so just create the new inputtrace
                newInputs[n] = new InputTraceNode(new int[]{1});
                Edge singleEdge = new Edge(origTraceNewOutput, newInputs[n]);
                newInputs[n].setSources(new Edge[]{singleEdge});
                //and add it to the new outputtrace node of the original trace
                origTraceNewWeights.add(new Integer(middleWeight));
                LinkedList<Edge> newPort = new LinkedList<Edge>();
                newPort.add(singleEdge);
                origTraceNewDests.add(newPort);
            }
            else {
                //if we get here, a port was broken, so the inputtrace node
                //will have multiple inputs...
                
                //the weights and the srcs of the new input trace node
                LinkedList<Integer> weights = new LinkedList<Integer>();
                LinkedList<Edge> srcs = new LinkedList<Edge>();
                InputTraceNode input = new InputTraceNode();
                
                if (firstWeight > 0) {
                    weights.add(new Integer(firstWeight));
                    Edge newEdge = new Edge(origTraceNewOutput, input);
                    srcs.add(newEdge);
                    //add edge to the last port that of the new ports for the
                    //original trace's output trace
                    origTraceNewDests.get(origTraceNewDests.size() - 1).add(newEdge);
                    //no need to set the weight, has been already done during previous
                    //input trace node 
                }
                if (middleWeight > 0) {
                    weights.add(new Integer(middleWeight));
                    Edge newEdge = new Edge(origTraceNewOutput, input);
                    srcs.add(newEdge);
                    //add the edge to a new port for the original trace's output
                    LinkedList<Edge> newPort = new LinkedList<Edge>();
                    newPort.add(newEdge);
                    origTraceNewDests.add(newPort);
                    //set the weight for this port
                    origTraceNewWeights.add(new Integer(middleWeight));
                }
                if (lastWeight > 0) {
                    weights.add(new Integer(lastWeight));
                    Edge newEdge = new Edge(origTraceNewOutput, input);
                    srcs.add(newEdge);
                    //add the edge to a new port for the original trace's output
                    LinkedList<Edge> newPort = new LinkedList<Edge>();
                    newPort.add(newEdge);
                    origTraceNewDests.add(newPort);
                    //set the weight for this port
                    origTraceNewWeights.add(new Integer(lastWeight));
                    //this last port will be appended to during the next iteration of 
                    //the loop (it will become the "first") 
                }
                
                input.set(weights, srcs);
                newInputs[n] = input;
            }
        }
        return newInputs;
    }
    
    /**
     * Create all the new output trace nodes that are needed for the
     * new traces that are created when we break an output trace node 
     * into multiple levels.
     * 
     * @param numNewTraces The number of new traces (all one level) we 
     * need to create.
     * @param output The original output trace node.
     * 
     * @return An array of the new output traces.
     */
    private OutputTraceNode[] createSplitOutputTraceNodes(int numNewTraces,
            OutputTraceNode output
           ) {
        OutputTraceNode[] newOutputs = new OutputTraceNode[numNewTraces];
        
        //the connections of the original output trace node 
        Edge[][] edges = output.getDests();
        
        //the index into the original trace's outputtracenode's output edges
        //a port is the collection of duplicated edges, 
        //so output.getWeights()[port][edge] = an edge 
        int port = 0, edge = 0;
                
        //create all the output trace nodes of the new traces
        for (int n = 0; n < numNewTraces; n++) {
            //number of edges connected to this new trace so far...
            int connected = 0;
            
            LinkedList<Integer> newWeights = new LinkedList<Integer>();  
            LinkedList<LinkedList<Edge>> newEdges = new LinkedList<LinkedList<Edge>>();
            while (true) {
                newWeights.add(new Integer(output.getWeights()[port]));
                LinkedList<Edge> portEdges = new LinkedList<Edge>();
                for (int addMe = edge; addMe < edges[port].length; addMe++) {
                    portEdges.add(edges[port][addMe]);
                    //increment the edge index because we just added one
                    edge++;
                    //check to make sure that we have not filled this new trace
                    if (++connected >= maxWidth)
                        break;
                }
                //add the "port" to the outgoing edges of this output trace 
                newEdges.add(portEdges);
                //if we have completed this port, then increment port
                //and reset edge
                if (edge >= edges[port].length) {
                    port++;
                    edge = 0;
                }
                //now check to make sure that we have not filled this trace
                if (connected >= maxWidth) 
                    break;
             
                //if we have reached the end of the original output trace, then break 
                if (port >= edges.length)
                    break;
            }
            //so now the LinkedLists are finished, so create the OutputTraceNode
            //from them...
            assert newWeights.size() == newEdges.size();
            if (newWeights.size() == 1) {
                //if we only have one port outgoing for this output trace node
                //set the single weight to 1...
                newWeights.remove(0);
                newWeights.add(new Integer(1));
            }
            newOutputs[n] = new OutputTraceNode(newWeights, newEdges);
        }
        return newOutputs;
    }
    
    
    /**
     * Create a new trace given output as its outputtracenode and src as its upstream
     * trace's outputtracenode.  The trace will have a single filter which is an 
     * identity filter of type.
     * 
     * @param output The new trace's output trace node.
     * @param input The new trace's input trace node.
     * @param src The src trace's output trace node.
     * @param type The type of the identity filter of the trace.
     */
    private Trace fixEdgesAndCreateTrace(OutputTraceNode output, InputTraceNode input,
            OutputTraceNode src, CType type) {
        
        //make sure that all of the edges of this output trace node have
        //it as their source
        for (int p = 0; p < output.getDests().length; p++) 
            for (int e = 0; e < output.getDests()[p].length; e++)
                output.getDests()[p][e].setSrc(output);

        //make sure that all of the edges of this input trace node
        //have it as there dest
        for (int i = 0; i < input.getSources().length; i++)
            input.getSources()[i].setDest(input);
        
//      create the new identity filter using an sir identity...
        SIRFilter identity = new SIRIdentity(type);
        RenameAll.renameAllFilters(identity);
        
        
        FilterTraceNode filter = 
            new FilterTraceNode(new FilterContent(identity));
        
        //set the multiplicity of the new identity filter
        filter.getFilter().setSteadyMult(output.totalWeights());
        
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
