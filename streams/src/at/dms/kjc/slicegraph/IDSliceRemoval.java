package at.dms.kjc.slicegraph;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Arrays;
import at.dms.kjc.slicegraph.fission.*;
import at.dms.kjc.backendSupport.FilterInfo;

public class IDSliceRemoval {
    private Slice idSlice;
    private InputSliceNode idInput;
    private OutputSliceNode idOutput;
    private InputSliceNode[] dsInputs;
    private int[] dsInputIndices;
    private HashMap<SliceNode, Integer> indexToIndex;
    private OutputSliceNode[] usOutputs;
    private int[] usOutputIndices;
    
    public static void doit(Slice slice) {
        assert slice.getFirstFilter().getFilter() instanceof IDFilterContent : 
            "Trying to remove a non ID slice";
        
        IDSliceRemoval remover = new IDSliceRemoval(slice);
        FilterInfo.reset();
    }
    
    private IDSliceRemoval(Slice s) {
        idSlice = s;
        idInput = idSlice.getHead();
        idOutput = idSlice.getTail();
        
        removeID(SchedulingPhase.INIT);
        removeID(SchedulingPhase.STEADY);
    }
    
    private void removeID(SchedulingPhase phase) {
        //unroll 
        unroll(phase);
        //remove
        remove(phase);
        //re-roll
        reroll(phase);
    }
    
    private void remove(SchedulingPhase phase) {
        assert idInput.getSources(phase).length == idOutput.getDests(phase).length;
        InterSliceEdge[] idSources = idInput.getSources(phase);
        InterSliceEdge[][] idDests = idOutput.getDests(phase);
        
        
        for (int idIndex = 0; idIndex < idSources.length; idIndex++) {
            OutputSliceNode src = idSources[idIndex].getSrc();
            
            InputSliceNode[] dests = new InputSliceNode[idDests[idIndex].length];
            for (int i = 0; i < idDests[idIndex].length; i++)
                dests[i] = idDests[idIndex][i].getDest();

            //replace the ref to the id in the inputslicenodes with the src
            for (int i = 0; i < dests.length; i++)
                replaceSrc(dests[i], src, phase);   
            
            //replace the ref to id in the outputslicenode with the dests
            replaceDest(src, dests, phase);

        }
    }
    
    private void reroll(SchedulingPhase phase) {
        for (OutputSliceNode output : usOutputs) 
            DistributionUnroller.roll(output.getParent());
        for (InputSliceNode input : dsInputs)
            DistributionUnroller.roll(input.getParent());
    }
    
    private void unroll(SchedulingPhase phase) {
        indexToIndex = new HashMap<SliceNode, Integer>();
        //unroll all the upstream output slice nodes
        LinkedList<OutputSliceNode> outputs = new LinkedList<OutputSliceNode>();
        for (InterSliceEdge edge : idInput.getSourceSet(phase)) {
            outputs.add(edge.getSrc());
            indexToIndex.put(edge.getSrc(), outputs.size() - 1);
            DistributionUnroller.unroll(edge.getSrc());
        }
        usOutputs = (OutputSliceNode[])outputs.toArray();
        usOutputIndices = new int[usOutputs.length];
        Arrays.fill(usOutputIndices, -1);
        
        //unroll all the downstream input slice nodes
        LinkedList<InputSliceNode> inputs = new LinkedList<InputSliceNode>();
        for (InterSliceEdge edge : idOutput.getDestSet(phase)) {
            inputs.add(edge.getDest());
            indexToIndex.put(edge.getDest(), inputs.size() - 1);
            DistributionUnroller.unroll(edge.getDest());
        }
        dsInputs = (InputSliceNode[])inputs.toArray();
        dsInputIndices = new int[dsInputs.length];
        Arrays.fill(dsInputIndices, -1);
        
        //unroll the input and output of the identity 
        DistributionUnroller.unroll(idInput);
        DistributionUnroller.unroll(idOutput);
    }
    
    private void replaceDest(OutputSliceNode output, InputSliceNode[] dests, 
            SchedulingPhase phase) {
        assert indexToIndex.containsKey(output);
        int index = indexToIndex.get(output);
        InterSliceEdge[][] schedule = output.getDests(phase);
        
        //create all the edges
        InterSliceEdge[] destEdges = new InterSliceEdge[dests.length];
        for (int i = 0; i < dests.length; i++) {
            InterSliceEdge edge = InterSliceEdge.getEdge(output, dests[i]);
            if (edge == null)
                edge = new InterSliceEdge(output, dests[i]);
            destEdges[i] = edge;
        }
        
        int current = usOutputIndices[index] + 1;
        for (; current < schedule.length; current++) {
            //see if ID is in the current dest []
            if (containsIDDest(schedule[current])) {
                //if so, remove id from the dest[] and add destEdges
                schedule[current] = replaceIDEdge(schedule[current], destEdges);
                //remember where we stopped
                usOutputIndices[index] = current;
                return;
            }
        }
        assert false : "Error in ID removal";
    }
    
    private InterSliceEdge[] replaceIDEdge(InterSliceEdge[] oldDests, InterSliceEdge[] toAdd) {
        assert containsIDDest(oldDests);
        InterSliceEdge[] newEdges = new InterSliceEdge[oldDests.length + toAdd.length - 1]; 
        int index = 0;
        
        //copy over all the edges except the one to the ID
        for (int i = 0; i < oldDests.length; i++) {
            if (oldDests[i].getDest() == idInput) 
                continue;
            newEdges[index++] = oldDests[i];
        }
        //now add the edges from toAdd which point to the dests bypassing the ID filter
        for (int i = 0; i < toAdd.length; i++) {
            newEdges[index++] = toAdd[i];
        }
        
        assert index == newEdges.length;
        
        return newEdges;
    }
    
    /**
     * return true if any of the edges has the ID's input node as a dest.
     */
    private boolean containsIDDest(InterSliceEdge[] edges) {
        for (InterSliceEdge edge : edges) {
            if (edge.getDest() == idInput) 
                return true;
        }
        return false;
    }
    
    
    /**
     * Replace the next edge from ID->input in input's join schedule with 
     * the edge from output->input. 
     */
    private void replaceSrc(InputSliceNode input, OutputSliceNode output, 
            SchedulingPhase phase) {
        //find the index into the index array
        assert indexToIndex.containsKey(input);
        int index = indexToIndex.get(input);
        InterSliceEdge[] srcs = input.getSources(phase);
        //we might have created this edge before, so let's check
        InterSliceEdge newEdge = InterSliceEdge.getEdge(output, input);
        if (newEdge == null)  //if not, create it
            newEdge = new InterSliceEdge(output, input);
        
        InterSliceEdge oldEdge = InterSliceEdge.getEdge(idOutput, input);
        
        int current = dsInputIndices[index] + 1;
        //find the next index into the input node that received from the ID
        //and replace it with the new edge that bypasses the ID
        for (; current < srcs.length; current++) {
            if (srcs[current] == oldEdge) {
                srcs[current] = newEdge;
                //remember where we stopped
                dsInputIndices[index] = current;
                return;
            }
        }
        assert false : "Error in ID removal";
    }
}
