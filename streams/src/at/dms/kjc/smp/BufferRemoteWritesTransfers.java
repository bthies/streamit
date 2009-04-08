package at.dms.kjc.smp;

import at.dms.kjc.slicegraph.*;

import java.util.HashMap;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.*;

import java.util.LinkedList;
import at.dms.kjc.smp.arrayassignment.*;

import java.util.List;

public class BufferRemoteWritesTransfers extends BufferTransfers {

    /**
     * Unique id
     */
    protected static int uniqueID = 0;
    protected int myID;
	
	/** reference to tail */
    protected JExpression tail;
    /** name of variable containing head of array offset */
    protected String readTailName;
    /** definition for head */
    protected JVariableDefinition readTailDefn;
	
    /** reference to head */
    protected JExpression head;
    /** name of variable containing head of array offset */
    protected String writeHeadName;
    /** definition for head */
    protected JVariableDefinition writeHeadDefn;
    
    /** the output slice node */
    protected OutputSliceNode output;
    
    /** optimization: if this is true, then we can write directly to the remote input buffer
     * when pushing and not into the local buffer.  This only works when the src and dest are on
     * different tiles.
     */
    protected boolean directWrite = false;

    /** true if this buffer's dest is a file writer */
    protected boolean fileWrite = false;
    protected static final String FAKE_IO_VAR = "__fake_output_var__";
    
    public BufferRemoteWritesTransfers(RotatingBuffer buf) {
        super(buf);
        myID = uniqueID++;
        
        if(parent instanceof InputRotatingBuffer) {            
            //set up the tail pointer for reading
            readTailName = buf.getIdent() + "tail";
            readTailDefn = new JVariableDefinition(null,
                    at.dms.kjc.Constants.ACC_STATIC,
                    CStdType.Integer, readTailName, null);
            
            tail = new JFieldAccessExpression(readTailName);
            tail.setType(CStdType.Integer);
        	
        	readDecls.add(new JVariableDeclarationStatement(readTailDefn));
        	
        	generateReadStatements(SchedulingPhase.INIT);
        	generateReadStatements(SchedulingPhase.STEADY);
        }
        
        if(parent instanceof OutputRotatingBuffer ||
        		(parent instanceof InputRotatingBuffer &&
        				((InputRotatingBuffer)parent).hasLocalSrcFilter())) {
        	
            //set up the head pointer for writing
            writeHeadName = buf.getIdent() + "head";
            writeHeadDefn = new JVariableDefinition(null,
                    at.dms.kjc.Constants.ACC_STATIC,
                    CStdType.Integer, writeHeadName, null);
            
            head = new JFieldAccessExpression(writeHeadName);
            head.setType(CStdType.Integer);
        	
        	writeDecls.add(new JVariableDeclarationStatement(writeHeadDefn));
        	
            //if this is a shared input buffer (one we are using for output), then 
            //the output buffer we are implementing here is the upstream output buffer
            //on the same tile
            if (buf instanceof InputRotatingBuffer)
                output = ((InputRotatingBuffer)buf).getLocalSrcFilter().getParent().getTail();
            else
                output = parent.filterNode.getParent().getTail();
                
            if (output.oneOutput(SchedulingPhase.STEADY) && 
                    (output.oneOutput(SchedulingPhase.INIT) || output.noOutputs(SchedulingPhase.INIT)) &&
                    SMPBackend.scheduler.getComputeNode(parent.filterNode) !=
                        SMPBackend.scheduler.getComputeNode(output.getSingleEdge(SchedulingPhase.STEADY).getDest().getNextFilter()) &&
                        //now make sure that it is single appearance or downstream has only one input for both steady and init
                        (output.getSingleEdge(SchedulingPhase.STEADY).getDest().singleAppearance() &&
                                //check steady for only one input downstream or only one rotation
                                (output.getSingleEdge(SchedulingPhase.STEADY).getDest().totalWeights(SchedulingPhase.STEADY) == 
                                    FilterInfo.getFilterInfo(output.getSingleEdge(SchedulingPhase.STEADY).getDest().getNextFilter()).totalItemsPopped(SchedulingPhase.STEADY) ||
                                    output.getSingleEdge(SchedulingPhase.STEADY).getDest().oneInput(SchedulingPhase.INIT)) &&
                                    //check the init stage
                                    (output.noOutputs(SchedulingPhase.INIT) || (output.oneOutput(SchedulingPhase.INIT) &&
                                            output.getSingleEdge(SchedulingPhase.INIT).getDest().totalWeights(SchedulingPhase.INIT) == 
                                                FilterInfo.getFilterInfo(output.getSingleEdge(SchedulingPhase.INIT).getDest().getNextFilter()).totalItemsPopped(SchedulingPhase.INIT) ||
                                                output.getSingleEdge(SchedulingPhase.INIT).getDest().oneInput(SchedulingPhase.INIT)))))
            {
                directWrite = true;
                if (output.getSingleEdge(SchedulingPhase.STEADY).getDest().getNextFilter().isFileOutput()) {
                    fileWrite = true;
                    writeDecls.add(Util.toStmt("volatile " + buf.getType().toString() + " " + FAKE_IO_VAR + "__n" + buf.parent.getCoreID()));
                }
                
                assert !usesSharedBuffer();
            }
        	
        	generateWriteStatements(SchedulingPhase.INIT);
        	generateWriteStatements(SchedulingPhase.STEADY);
        }
    }
    
    /********** Read code **********/
    
    private void generateReadStatements(SchedulingPhase phase) {
    	List<JStatement> statements = null;
    	
        switch (phase) {
        	case INIT: statements = readCommandsInit; break;
        	case PRIMEPUMP: assert(false); break;
        	case STEADY: statements = readCommandsSteady; break;
        	default: assert(false);
        }
        
        statements.addAll(copyDownStatements(phase));
    }
    
    /** 
     * Generate and return the statements that implement the copying of the items on 
     * a buffer to the next rotating buffer.  Only done for each primepump stage and the steady stage,
     * not done for init.
     * 
     * @return statements to implement the copy down
     */
    protected List<JStatement> copyDownStatements(SchedulingPhase phase) {
        List<JStatement> retval = new LinkedList<JStatement>();
        //if we have items on the buffer after filter execution, we must copy them 
        //to the next buffer, don't use memcopy, just generate individual statements
        
        //for the init phase we copy to the same buffer because we are not rotating
        //for the steady phase we copy to the next rotation buffer
        String dst = 
            (phase == SchedulingPhase.INIT ? parent.currentReadBufName : parent.currentReadRotName + "->next->buffer");
        String src = parent.currentReadBufName;
        
        ArrayAssignmentStatements aaStmts = new ArrayAssignmentStatements();
        
        for (int i = 0; i < parent.filterInfo.copyDown; i++)
            aaStmts.addAssignment(dst, "", i, src, "", (i + parent.filterInfo.totalItemsPopped(phase)));
        
        retval.addAll(aaStmts.toCompressedJStmts());
        return retval;
    }
    
    public JStatement zeroOutTail(SchedulingPhase phase) {
        return new JExpressionStatement(
                new JAssignmentExpression(tail, new JIntLiteral(0)));
    }
    
    public JMethodDeclaration peekMethod() {
        String parameterName = "__offset";
        JFormalParameter offset = new JFormalParameter(
                CStdType.Integer,
                parameterName);
        JLocalVariableExpression offsetRef = new JLocalVariableExpression(offset);
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                parent.getType(),
                parent.peekMethodName(),
                new JFormalParameter[]{offset},
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
                new JReturnStatement(null,
                        parent.readBufRef(new JAddExpression(tail, offsetRef)),null));
        return retval;
    }
    
    public JMethodDeclaration popMethod() {
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                parent.getType(),
                parent.popMethodName(),
                new JFormalParameter[0],
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
        		new JReturnStatement(null,
        				parent.readBufRef(new JPostfixExpression(at.dms.kjc.Constants.OPE_POSTINC, tail)),null));
        return retval;
    }
    
    /********** Write code **********/
    
    public boolean usesSharedBuffer() {
        return (parent instanceof InputRotatingBuffer &&
        		((InputRotatingBuffer)parent).hasLocalSrcFilter());
    }
    
    private void generateWriteStatements(SchedulingPhase phase) {
     
        //if we are directly writing, then the push method does the remote writes,
        //so no other remote writes are necessary
        if (directWrite)
            return;
        
        List<JStatement> statements = null;        
        switch (phase) {
            case INIT: statements = writeCommandsInit; break;
            case PRIMEPUMP: assert(false); break;
            case STEADY: statements = writeCommandsSteady; break;
        }

        //if this is an input buffer shared as an output buffer, then the output
        //filter is the local src filter of this input buffer
        FilterSliceNode filter;
        if (usesSharedBuffer())
            filter = ((InputRotatingBuffer)parent).getLocalSrcFilter();
        else  //otherwise it is an output buffer, so use the parent's filter
            filter = parent.filterNode;

        FilterInfo fi = FilterInfo.getFilterInfo(filter);

        //no further code necessary if nothing is being produced
        if (fi.totalItemsSent(phase) == 0)
            return;
        
        assert fi.totalItemsSent(phase) % output.totalWeights(phase) == 0;
        
        //we might have to skip over some elements when we push into the buffer if this
        //is a shared buffer
        int writeOffset = getWriteOffset(phase);
       
        Core sourceTile = SMPBackend.backEndBits.getLayout().getComputeNode(filter);
        
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
        
        ArrayAssignmentStatements reorderStatements = new ArrayAssignmentStatements();
        
        int items = 0;
        for (int rot = 0; rot < rotations; rot++) {
            for (int weightIndex = 0; weightIndex < output.getWeights(phase).length; weightIndex++) {
                InterSliceEdge[] dests = output.getDests(phase)[weightIndex];
                for (int curWeight = 0; curWeight < output.getWeights(phase)[weightIndex]; curWeight++) {
                    int sourceElement= rot * output.totalWeights(phase) + 
                        output.weightBefore(weightIndex, phase) + curWeight + writeOffset;
                        items++;
                        for (InterSliceEdge dest : dests) {
                            int destElement = 
                                destIndices[destIndex.get(dest)][nextWriteIndex[destIndex.get(dest)]];
                            nextWriteIndex[destIndex.get(dest)]++;
                            Core destTile = 
                                SMPBackend.backEndBits.getLayout().getComputeNode(dest.getDest().getNextFilter());
                            //don't do anything if this dest is on the same tiles, we are sharing the buffer with the
                            //dest, and the indices are the same.
                            
                            if (destTile == sourceTile && destElement == sourceElement && usesSharedBuffer()) 
                                continue;
                            
                            if (destTile == sourceTile) {
                                assert !usesSharedBuffer() : "Trying to reorder a single buffer! Could lead to race. " + filter;
                            }
                            
                            SourceAddressRotation addrBuf = parent.getAddressBuffer(dest.getDest());
                            reorderStatements.addAssignment(addrBuf.currentWriteBufName, "", destElement, 
                                    parent.currentWriteBufName, "", sourceElement);
                            //System.out.println("remoteWrites: " + addrBuf.currentWriteBufName + ", " + destElement + "; " + 
                            //		   parent.currentWriteBufName + ", " + sourceElement);
                           
                        }
                }
            }
        }

        assert items == fi.totalItemsSent(phase);
        
        //add the compressed assignment statements to the appropriate stage
        //these do the remote writes and any local copying needed
        statements.addAll(reorderStatements.toCompressedJStmts());   
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
                            input.weightBefore(index, phase) + item + 
                            (phase == SchedulingPhase.INIT ? 0 : dsFilter.copyDown);
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
            FilterInfo localDest = FilterInfo.getFilterInfo(parent.filterNode);
            
            //no address array needed but we have to set the head to the copydown plus
            //the weights of any inputs that are not mapped to this tile that appear before
            //the local source
            
            InputSliceNode input = parent.filterNode.getParent().getHead();
            FilterSliceNode localSrc = ((InputRotatingBuffer)parent).getLocalSrcFilter();
            //the local source and dest might not communicate in the init stage, if not
            //the offset should just be zero
            if (!input.hasEdgeFrom(phase, localSrc))
                return 0;
            
            InterSliceEdge theEdge = input.getEdgeFrom(phase, localSrc);
            int offset = input.weightBefore(theEdge, phase);
   
            //if we are not in the init, we must skip over the dest's copy down
            if (SchedulingPhase.INIT != phase) 
                offset += localDest.copyDown;
            return offset;
        } else
            return 0;
    }

    public JStatement zeroOutHead(SchedulingPhase phase) {
        //if we have shared buffer, then we are using it for the output and input of filters
        //on the same tile, so we need to do special things to the head
        int literal = 0; 
        JBlock block = new JBlock();
        if (usesSharedBuffer()) {
                literal = getWriteOffset(phase);
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
                        edge.getDest().weightBefore(edge, phase);
                    //if we are in the init, skip copy down as well
                    if (SchedulingPhase.INIT == phase)
                            literal += destInfo.copyDown;
                }
            } else {
                //no optimizations, just zero head so that we write to beginning of output buffer
                literal = 0;
            }
        }
        
        block.addStatement(new JExpressionStatement(
                new JAssignmentExpression(head, new JIntLiteral(literal))));
        
        return block;
    }

    public JMethodDeclaration pushMethod() {
        JExpression bufRef = null;
        //set the buffer reference to the input buffer of the remote buffer that we are writing to
        if (directWrite) {
            bufRef = new JFieldAccessExpression(new JThisExpression(),  
                        parent.getAddressBuffer(output.getSingleEdge(SchedulingPhase.STEADY).getDest()).currentWriteBufName);
        }
        else   //not a direct write, so the buffer ref to the write buffer of the buffer
            bufRef = parent.writeBufRef();
        
        String valName = "__val";
        JFormalParameter val = new JFormalParameter(
                parent.getType(),
                valName);
        JLocalVariableExpression valRef = new JLocalVariableExpression(val);
        JBlock body = new JBlock();

        if (fileWrite && SMPBackend.FAKE_IO) {
            //if we are faking the io and this writes to a file writer assign val to volatile value
            body.addStatement(Util.toStmt(FAKE_IO_VAR + "__n" + 
					  SMPBackend.scheduler.getComputeNode(parent.filterNode).getCoreID() + " = " + valName));
        } else {
            //otherwise generate buffer assignment
            body.addStatement(
                    new JExpressionStatement(new JAssignmentExpression(
                            new JArrayAccessExpression(bufRef, new JPostfixExpression(at.dms.kjc.Constants.OPE_POSTINC,
                                    head)),
                                    valRef)));
        }

        
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                parent.pushMethodName(),
                new JFormalParameter[]{val},
                CClassType.EMPTY,
                body, null, null);
        
        
        return retval;
    }
    
    /**
     * Do some checks to make sure we will generate correct code for this distribution pattern.
     */
    protected void checkSimple(SchedulingPhase phase) {
        assert output.singleAppearance();
        for (int w = 0; w < output.getWeights(phase).length; w++) {
            for (InterSliceEdge edge : output.getDests(phase)[w]) {
                InputSliceNode input = edge.getDest();
                //assert that we don't have a single edge appear more than once for the input slice node
                assert input.singleAppearance();
                
                int inWeight = input.getWeight(edge, phase);
                assert inWeight == output.getWeights(phase)[w];
            }
        }
    }
}
