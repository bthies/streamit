package at.dms.kjc.tilera;

import at.dms.kjc.slicegraph.*;
import java.util.HashMap;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.JStatement;
import java.util.List;

public class BufferRemoteWritesTransfers extends BufferTransfers {
    
    public BufferRemoteWritesTransfers(RotatingBuffer buf) {
        super(buf);
        
        generateStatements(SchedulingPhase.INIT);
     
        generateStatements(SchedulingPhase.STEADY);     
    }

    private void generateStatements(SchedulingPhase phase) {
        FilterSliceNode filter;
        int copyDown = 0;
        //if this is an input buffer shared as an output buffer, then the output
        //filter is the local src filter of this input buffer
        if (parent instanceof InputRotatingBuffer) {
            filter = ((InputRotatingBuffer)parent).getLocalSrcFilter();
            //since we are using the input buffer as an output buffer, we are going
            //to be writing to locations that are offset from the copy down of the filter
            //that the input buffer is input to
            copyDown = parent.filterInfo.copyDown;
        }
        else  //otherwise it is an output buffer, so use the parent's filter
            filter = parent.filterNode;
        
       FilterInfo fi = FilterInfo.getFilterInfo(filter);
            
        //no code necessary if nothing is being produced
        if (fi.totalItemsSent(phase) == 0)
            return;
        
        
        assert fi.totalItemsSent(phase) % output.totalWeights(phase) == 0;
        
        List<JStatement> statements = null;
        
        switch (phase) {
            case INIT: statements = commandsInit; break;
            case PRIMEPUMP: assert false; break;
            case STEADY: statements = commandsSteady; break;
        }
        
        Tile sourceTile = TileraBackend.backEndBits.getLayout().getComputeNode(filter);
        
        int rotations = fi.totalItemsSent(phase) / output.totalWeights(phase);
        
        //first create an map from destinations to ints to index into the state arrays
        HashMap<InterSliceEdge, Integer> destIndex = new HashMap<InterSliceEdge, Integer>();
        int index = 0;
        int numDests = output.getDestSet(phase).size();
        int[][] destIndices = new int[numDests][];
        int[] nextWriteIndex = new int[numDests];
        
        for (InterSliceEdge edge : output.getDestSet(phase)) {
            destIndex.put(edge, index);
            destIndices[index] = getDestIndices(edge, rotations, phase);
            nextWriteIndex[index] = 0;
            index++;
        }
        
        
        for (int rot = 0; rot < rotations; rot++) {
            for (int weightIndex = 0; weightIndex < output.getWeights(phase).length; weightIndex++) {
                InterSliceEdge[] dests = output.getDests(phase)[weightIndex];
                for (int curWeight = 0; curWeight < output.getWeights(phase)[weightIndex]; curWeight++) {
                    int sourceElement= rot * output.totalWeights(phase) + 
                        output.weightBefore(weightIndex, phase) + curWeight + copyDown;
                    
                        for (InterSliceEdge dest : dests) {
                            int destElement = 
                                destIndices[destIndex.get(dest)][nextWriteIndex[destIndex.get(dest)]];
                            nextWriteIndex[destIndex.get(dest)]++;
                            Tile destTile = 
                                TileraBackend.backEndBits.getLayout().getComputeNode(dest.getDest().getNextFilter());
                            
                            if (destTile == sourceTile) {
                                if (destElement < sourceElement) {
                                    statements.add(Util.toStmt(parent.currentWriteBufName + "[ " + destElement + "] = " + 
                                            parent.currentWriteBufName + "[" + sourceElement + "]"));
                                }
                                else if (destElement > sourceElement) {
                                    assert false : "Dest: " + dest.getDest().getNextFilter();
                                }
                            } else {
                                SourceAddressRotation addrBuf = parent.getAddressBuffer(dest.getDest());
                                statements.add(Util.toStmt(addrBuf.currentWriteBufName + "[ " + destElement + "] = " + 
                                        parent.currentWriteBufName + "[" + sourceElement + "]"));
                            }
                        }
                }
            }
        }
        
    }
    
    private int[] getDestIndices(InterSliceEdge edge, int outputRots, SchedulingPhase phase) {
        int[] indices = new int[outputRots * output.getWeight(edge, phase)];
        InputSliceNode input = edge.getDest();
        FilterInfo dsFilter = FilterInfo.getFilterInfo(input.getNextFilter());
        
        assert indices.length %  input.getWeight(edge, phase) == 0;
        
        int inputRots = indices.length / input.getWeight(edge, phase);
        int nextWriteIndex = 0;

        for (int rot = 0; rot < inputRots; rot++) {
            for (int index = 0; index < input.getWeights(phase).length; index++) {
                if (input.getSources(phase)[index] == edge) {
                    for (int item = 0; item < input.getWeights(phase)[index]; item++) {
                        indices[nextWriteIndex++] = rot * input.totalWeights(phase) +
                            input.weightBefore(index, phase) + item + dsFilter.copyDown;
                    }
                }
            }
        }
        
        assert nextWriteIndex == indices.length;
        
        return indices;
    }
}
