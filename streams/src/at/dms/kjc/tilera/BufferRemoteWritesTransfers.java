package at.dms.kjc.tilera;

import at.dms.kjc.slicegraph.*;

import java.util.HashMap;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.*;

import java.util.List;

public class BufferRemoteWritesTransfers extends BufferTransfers {
    /** reference to head if this input buffer is shared as an output buffer */
    protected JExpression head;
    /** name of variable containing head of array offset */
    protected String writeHeadName;
    /** definition for head */
    protected JVariableDefinition writeHeadDefn;
    /** are we using an address array for writing to a shared buffer */
    boolean needAddressArray;
    /** optimization: if this is true, then we can write directly to the remote input buffer
     * when pushing and not into the local buffer.  This only works when the src and dest are on
     * different tiles.
     */
    protected boolean directWrite = false;
    
    public BufferRemoteWritesTransfers(RotatingBuffer buf) {
        super(buf);
        
        //set up the head pointer for writing
        writeHeadName = buf.getIdent() + "head";
        writeHeadDefn = new JVariableDefinition(null,
                at.dms.kjc.Constants.ACC_STATIC,
                CStdType.Integer, writeHeadName, null);
        
        head = new JFieldAccessExpression(writeHeadName);
        head.setType(CStdType.Integer);
        
        setWritingScheme();
        
        assert !needAddressArray : parent.filterNode;
        
        //optimatization opportunity if we have single output edge and the
        //downstream filter is mapped to another tile, very good for file writers
        if (output.oneOutput() && 
                output.getSingleEdge(SchedulingPhase.INIT) == 
                    output.getSingleEdge(SchedulingPhase.STEADY) &&
                TileraBackend.scheduler.getComputeNode(parent.filterNode) !=
                    TileraBackend.scheduler.getComputeNode(output.getSingleEdge(SchedulingPhase.STEADY).getDest().getNextFilter()) &&
                    output.getSingleEdge(SchedulingPhase.STEADY).getDest().singleAppearance())
        {
            directWrite = true;
            assert !usesSharedBuffer();
        }
        
        decls.add(new JVariableDeclarationStatement(writeHeadDefn));
        
        generateStatements(SchedulingPhase.INIT);
     
        generateStatements(SchedulingPhase.STEADY);     
    }

    /**
     * If these transfers have as there source a shared input buffer, we might have 
     * to use an address array for the indices of the upstream push method.  This will
     * determine if we have to use an address array.
     */
    private void setWritingScheme() {
        //if we are using a shared buffer then check if 
        if (usesSharedBuffer()) {
            FilterInfo localDest = FilterInfo.getFilterInfo(parent.filterNode);
            InputSliceNode input = parent.filterNode.getParent().getHead();
            int rotations = localDest.totalItemsReceived(SchedulingPhase.STEADY) / 
                input.totalWeights(SchedulingPhase.STEADY);
            
            if (rotations == 1 && input.singleAppearance()) {
                needAddressArray = false;
            } else {
                needAddressArray = true;
            }
            
        } else
            needAddressArray = false;
    }
    

    public boolean usesSharedBuffer() {
        return parent instanceof InputRotatingBuffer;
    }
    
    private void generateStatements(SchedulingPhase phase) {
        FilterSliceNode filter;
        //if we are directly writing, then the push method does the remote writes,
        //so no other remote writes are necessary
        if (directWrite)
            return;

        //if this is an input buffer shared as an output buffer, then the output
        //filter is the local src filter of this input buffer
        if (usesSharedBuffer()) {
            filter = ((InputRotatingBuffer)parent).getLocalSrcFilter();
        }
        else  //otherwise it is an output buffer, so use the parent's filter
            filter = parent.filterNode;


        FilterInfo fi = FilterInfo.getFilterInfo(filter);
            
        //no code necessary if nothing is being produced
        if (fi.totalItemsSent(phase) == 0)
            return;
        
        assert fi.totalItemsSent(phase) % output.totalWeights(phase) == 0;
        
        //we might have to skip over some elements when we push into the buffer if this
        //is a shared buffer
        int writeOffset = getWriteOffset(phase);
        //System.out.println("Source write offset = " + writeOffset);     
        
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
                        output.weightBefore(weightIndex, phase) + curWeight + writeOffset;
                    
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
                                    assert false : "Dest: " + dest.getDest().getNextFilter() + " " + sourceElement + " < " + destElement;
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
        //System.out.println("Dest copyDown: "+dsFilter.copyDown);
        assert indices.length %  input.getWeight(edge, phase) == 0;
        
        int inputRots = indices.length / input.getWeight(edge, phase);
        int nextWriteIndex = 0;

        for (int rot = 0; rot < inputRots; rot++) {
            for (int index = 0; index < input.getWeights(phase).length; index++) {
                if (input.getSources(phase)[index] == edge) {
                    for (int item = 0; item < input.getWeights(phase)[index]; item++) {
                        indices[nextWriteIndex++] = rot * input.totalWeights(phase) +
                            input.weightBefore(index, phase) + item + dsFilter.copyDown;
                        //System.out.println("Dest index: " + indices[nextWriteIndex -1]);
                    }
                }
            }
        }
        
        assert nextWriteIndex == indices.length;
        
        return indices;
    }
    
    /** 
     * if we are using a shared buffer, then we have to start pushing over the
     * copy down and any non-local edges into the buffer
     */
    private int getWriteOffset(SchedulingPhase phase) {
        if (usesSharedBuffer()) {
            //no address array needed but we have to set the head to the copydown plus
            //the weights of any inputs that are not mapped to this tile that appear before
            //the local source
            FilterInfo localDest = FilterInfo.getFilterInfo(parent.filterNode);
            InputSliceNode input = parent.filterNode.getParent().getHead();
            FilterSliceNode localSrc = ((InputRotatingBuffer)parent).getLocalSrcFilter();
            InterSliceEdge theEdge = input.getEdgeFrom(phase, localSrc);

            int offset = localDest.copyDown + input.weightBefore(theEdge, phase);

            return offset;
        } else
            return 0;
    }

    public JStatement zeroOutHead(SchedulingPhase phase) {
        //if we have shared buffer, then we are using it for the output and input of filters
        //on the same tile, so we need to do special things to the head
        int literal = 0; 
        
        if (usesSharedBuffer()) {
            if (needAddressArray) {
                assert false;
                return null;
            } else {
                literal = getWriteOffset(phase);
            }
        } else {
            if (directWrite) {
                InterSliceEdge edge = output.getSingleEdge(phase);
                //if we are directly writing then we have to get the index into the remote
                //buffer of start of this source
                
                //first make sure we actually write in this stage
                if (edge == null || !edge.getDest().getSourceSet(phase).contains(edge)) {
                    literal = 0;
                }
                else {
                    FilterInfo destInfo = FilterInfo.getFilterInfo(edge.getDest().getNextFilter());
                    literal = 
                        edge.getDest().weightBefore(edge, phase) + destInfo.copyDown;
                }
            } else {
                //no optimizations, just zero head so that we write to beginning of output buffer
                literal = 0;
            }
        }
        return new JExpressionStatement(
                new JAssignmentExpression(head, new JIntLiteral(literal)));
    }
    
    public JMethodDeclaration pushMethod() {
        JExpression bufRef = null;
        //set the buffer reference to the input buffer of the remote buffer that we are writing to
        if (directWrite) {
            bufRef = new JFieldAccessExpression(new JThisExpression(),  
                parent.getAddressBuffer(output.getSingleEdge(SchedulingPhase.STEADY).getDest()).currentWriteBufName);
        }
        else   //not a direct write, so so the buffer ref to the write buffer of the buffer
            bufRef = parent.writeBufRef();
        
        String valName = "__val";
        JFormalParameter val = new JFormalParameter(
                parent.getType(),
                valName);
        JLocalVariableExpression valRef = new JLocalVariableExpression(val);
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                parent.pushMethodName(),
                new JFormalParameter[]{val},
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
        new JExpressionStatement(new JAssignmentExpression(
                new JArrayAccessExpression(bufRef, new JPostfixExpression(at.dms.kjc.Constants.OPE_POSTINC,
                        head)),
                valRef)));
        return retval;
    }
}
