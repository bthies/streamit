package at.dms.kjc.smp;

import at.dms.kjc.slicegraph.*;

import java.util.HashMap;
import java.util.Set;

import at.dms.kjc.backendSupport.*;
import at.dms.kjc.*;

import java.util.LinkedList;
import at.dms.kjc.smp.arrayassignment.*;

import java.util.List;

public class BufferRemoteWritesTransfers extends BufferTransfers {

    /** Unique id */
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
    
    /** true if this buffer's dest is a file writer */
    protected boolean directFileWrite = false;
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
        
        if(parent instanceof OutputRotatingBuffer) {
            //set up the head pointer for writing
            writeHeadName = buf.getIdent() + "head";
            writeHeadDefn = new JVariableDefinition(null,
                    at.dms.kjc.Constants.ACC_STATIC,
                    CStdType.Integer, writeHeadName, null);
            
            head = new JFieldAccessExpression(writeHeadName);
            head.setType(CStdType.Integer);
        	
        	writeDecls.add(new JVariableDeclarationStatement(writeHeadDefn));

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
                if (output.getSingleEdge(SchedulingPhase.STEADY).getDest().getNextFilter().isFileOutput()) {
                    directFileWrite = true;
                    writeDecls.add(Util.toStmt("volatile " + buf.getType().toString() + " " + FAKE_IO_VAR + "__n" + buf.parent.getCoreID()));
                }
                
                assert ((OutputRotatingBuffer)parent).hasDirectWrite(); 
            }
        	
        	generateWriteStatements(SchedulingPhase.INIT);
        	generateWriteStatements(SchedulingPhase.STEADY);
        }
    }
    
    /********** Read code **********/
    
    private void generateReadStatements(SchedulingPhase phase) {
    	assert (parent instanceof InputRotatingBuffer);
    	
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
    	assert (parent instanceof InputRotatingBuffer);
    	
        List<JStatement> retval = new LinkedList<JStatement>();
        //if we have items on the buffer after filter execution, we must copy them 
        //to the next buffer, don't use memcopy, just generate individual statements
        
        //for the init phase we copy to the same buffer because we are not rotating
        //for the steady phase we copy to the next rotation buffer
        String dst = 
            (phase == SchedulingPhase.INIT ? 
            		((InputRotatingBuffer)parent).currentReadBufName : 
            			((InputRotatingBuffer)parent).currentReadRotName + "->next->buffer");
        String src = ((InputRotatingBuffer)parent).currentReadBufName;
        
        ArrayAssignmentStatements aaStmts = new ArrayAssignmentStatements();
        
        System.out.println("filterNode: " + parent.filterNode + ", phase: " + phase + ", copyDown: " + parent.filterInfo.copyDown);

        for (int i = 0; i < parent.filterInfo.copyDown; i++)
            aaStmts.addAssignment(dst, "", i, src, "", (i + parent.filterInfo.totalItemsPopped(phase)));
        
        retval.addAll(aaStmts.toCompressedJStmts());
        return retval;
    }
    
    public JStatement zeroOutTail(SchedulingPhase phase) {
    	assert (parent instanceof InputRotatingBuffer);
    	
        return new JExpressionStatement(
                new JAssignmentExpression(tail, new JIntLiteral(0)));
    }
    
    public JMethodDeclaration peekMethod() {
    	assert (parent instanceof InputRotatingBuffer);
    	
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
    	assert (parent instanceof InputRotatingBuffer);
    	
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
    
    private void generateWriteStatements(SchedulingPhase phase) {
    	assert (parent instanceof OutputRotatingBuffer);
     
        if (directFileWrite && SMPBackend.FAKE_IO)
            return;
        
        List<JStatement> statements = null;        
        switch (phase) {
            case INIT: statements = writeCommandsInit; break;
            case PRIMEPUMP: assert(false); break;
            case STEADY: statements = writeCommandsSteady; break;
        }

        //if this is an input buffer shared as an output buffer, then the output
        //filter is the local src filter of this input buffer
        FilterSliceNode filter = parent.filterNode;

        FilterInfo fi = FilterInfo.getFilterInfo(filter);

        //no further code necessary if nothing is being produced
        if (fi.totalItemsSent(phase) == 0)
            return;
        
        assert fi.totalItemsSent(phase) % output.totalWeights(phase) == 0;
        
        //we might have to skip over some elements when we push into the buffer if this
        //is a shared buffer
        int writeOffset = getWriteOffset(phase);
       
        Core sourceTile = SMPBackend.scheduler.getComputeNode(filter);
        
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

                    for (InterSliceEdge dest : dests) {
                        int destElement = 
                            destIndices[destIndex.get(dest)][nextWriteIndex[destIndex.get(dest)]];
                        nextWriteIndex[destIndex.get(dest)]++;
                        Core destTile = 
                            SMPBackend.scheduler.getComputeNode(dest.getDest().getNextFilter());
                        
                        //don't do anything if this dest is on the same tiles, we are sharing the buffer with the
                        //dest, and the indices are the same.
                        if (destTile == sourceTile && destElement == sourceElement && ((OutputRotatingBuffer)parent).hasDirectWrite()) 
                            continue;
                        
                        if (destTile == sourceTile) {
                            assert !((OutputRotatingBuffer)parent).hasDirectWrite() : "Trying to reorder a single buffer! Could lead to race. " + filter;
                        }
                        
                        SourceAddressRotation addrBuf = ((OutputRotatingBuffer)parent).getAddressBuffer(dest.getDest());
                        
                        if(((OutputRotatingBuffer)parent).hasDirectWrite()) {
	                        reorderStatements.addAssignment(addrBuf.currentWriteBufName, "", destElement, 
	                        		((OutputRotatingBuffer)parent).getAddressBuffer(((OutputRotatingBuffer)parent).getDirectWriteFilter().getParent().getHead()).currentWriteBufName, "", sourceElement);
                        }
                        else {
	                        reorderStatements.addAssignment(addrBuf.currentWriteBufName, "", destElement, 
	                        		((OutputRotatingBuffer)parent).currentWriteBufName, "", sourceElement);                        	
                        }
                    }
                    
                    items++;
                }
            }
        }

        assert items == fi.totalItemsSent(phase);
        
        //add the compressed assignment statements to the appropriate stage
        //these do the remote writes and any local copying needed
        statements.addAll(reorderStatements.toCompressedJStmts());   
    }
    
    private int[] getDestIndices(InterSliceEdge edge, int outputRots, SchedulingPhase phase) {
    	assert (parent instanceof OutputRotatingBuffer);
    	
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
                    }
                }
            }
        }
        
        assert nextWriteIndex == indices.length;
        
        return indices;
    }
    
    private int getWriteOffset(SchedulingPhase phase) {
    	assert (parent instanceof OutputRotatingBuffer);
    	
        if (((OutputRotatingBuffer)parent).hasDirectWrite()) {
            //if we are directly writing then we have to get the index into the remote
            //buffer of start of this source
        	
        	Set<InterSliceEdge> edges = parent.filterNode.getParent().getTail().getDestSet(phase);
        	InterSliceEdge edge = null;
        	
        	for(InterSliceEdge e : edges) {
        		if(e.getDest().getNextFilter().equals(((OutputRotatingBuffer)parent).getDirectWriteFilter())) {
        			edge = e;
        			break;
        		}
        	}
        	
        	//first make sure we actually write in this stage
        	if(edge == null)
        		return 0;
        	
        	int offset = edge.getDest().weightBefore(edge, phase);
        	
            //if we are not in the init, we must skip over the dest's copy down            
        	if(phase != SchedulingPhase.INIT) {
        		FilterInfo destInfo = FilterInfo.getFilterInfo(edge.getDest().getNextFilter());
        		offset += destInfo.copyDown;
        	}

        	return offset;
        } 
        else {
            return 0;
        }
    }

    public JStatement zeroOutHead(SchedulingPhase phase) {
    	assert (parent instanceof OutputRotatingBuffer);
    	
        int literal = getWriteOffset(phase);

        JBlock block = new JBlock();        
        block.addStatement(new JExpressionStatement(
                new JAssignmentExpression(head, new JIntLiteral(literal))));        
        return block;
    }

    public JMethodDeclaration pushMethod() {
    	assert (parent instanceof OutputRotatingBuffer);
    	
        JBlock body = new JBlock();
        
        String valName = "__val";
        JFormalParameter val = new JFormalParameter(
                parent.getType(),
                valName);
        JLocalVariableExpression valRef = new JLocalVariableExpression(val);
        
        if (directFileWrite && SMPBackend.FAKE_IO) {
            //if we are faking the io and this writes to a file writer assign val to volatile value
            body.addStatement(Util.toStmt(FAKE_IO_VAR + "__n" + 
					  SMPBackend.scheduler.getComputeNode(parent.filterNode).getCoreID() + " = " + valName));
        } else {
            JExpression bufRef = null;
            
            //set the buffer reference to the input buffer of the remote buffer that we are writing to
            if (((OutputRotatingBuffer)parent).hasDirectWrite()) {
                bufRef = new JFieldAccessExpression(new JThisExpression(),
                        ((OutputRotatingBuffer)parent).getAddressBuffer(((OutputRotatingBuffer)parent).getDirectWriteFilter().getParent().getHead()).currentWriteBufName);
            }
            else {
                bufRef = parent.writeBufRef();
            }

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
}
