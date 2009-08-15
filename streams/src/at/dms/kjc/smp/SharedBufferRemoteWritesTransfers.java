package at.dms.kjc.smp;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.slicegraph.fission.*;
import at.dms.kjc.smp.arrayassignment.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SharedBufferRemoteWritesTransfers extends BufferTransfers {

    private static final boolean debug = false;

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
    
    public SharedBufferRemoteWritesTransfers(RotatingBuffer buf) {
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

                    if(SMPBackend.FAKE_IO)
                        writeDecls.add(Util.toStmt("volatile " + buf.getType().toString() + " " + FAKE_IO_VAR + "__n" + buf.parent.getCoreID()));
                }
                
                assert ((OutputRotatingBuffer)parent).hasDirectWrite(); 
            }
        	
        	generateWriteStatements(SchedulingPhase.INIT);
        	generateWriteStatements(SchedulingPhase.STEADY);
        }
    }
    
    private void debugPrint(String methodName, SchedulingPhase phase, FilterSliceNode filterNode,
                            String valueName, int value) {
        if(debug) {
            System.out.println(methodName + ", phase: " + phase + ", filterNode: " + filterNode +
                               ", " + valueName + ": " + value);
        }
    }


    private void debugPrint(String methodName, SchedulingPhase phase, InterSliceEdge edge,
                            String valueName, int value) {
        if(debug) {
            System.out.println(methodName + ", phase: " + phase + ", edge: " + 
                               edge.getSrc().getParent().getFirstFilter() + "->" +
                               edge.getDest().getParent().getFirstFilter() +
                               ", " + valueName + ": " + value);
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

        if(FissionGroupStore.isFizzed(parent.filterNode.getParent())) {
            if(FissionGroupStore.getFizzedSliceIndex(parent.filterNode.getParent()) == 0) {
                statements.addAll(copyDownStatements(phase));
            }
        }
        else {
            statements.addAll(copyDownStatements(phase));
        }
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

        FilterInfo fi;
        if(FissionGroupStore.isFizzed(parent.filterNode.getParent()))
            fi = FissionGroupStore.getUnfizzedFilterInfo(parent.filterNode.getParent());
        else
            fi = parent.filterInfo;

        for (int i = 0; i < fi.copyDown; i++)
            aaStmts.addAssignment(dst, "", i, src, "", (i + fi.totalItemsPopped(phase)));
        
        retval.addAll(aaStmts.toCompressedJStmts());
        return retval;
    }
    
    public JStatement zeroOutTail(SchedulingPhase phase) {
    	assert (parent instanceof InputRotatingBuffer);

        if(phase != SchedulingPhase.INIT && FissionGroupStore.isFizzed(parent.filterNode.getParent())) {
            FissionGroup group = FissionGroupStore.getFissionGroup(parent.filterNode.getParent());

            if(LoadBalancer.isLoadBalanced(group)) {
                JEmittedTextExpression startIterExpr =
                    new JEmittedTextExpression(LoadBalancer.getStartIterRef(group, parent.filterNode.getParent()));
                JIntLiteral popRate = new JIntLiteral(group.unfizzedFilterInfo.pop);

                return new JExpressionStatement(
                        new JAssignmentExpression(tail,
                                                  new JMultExpression(startIterExpr,
                                                                      popRate)));
            }
            else {
                int totalItemsReceived = group.unfizzedFilterInfo.totalItemsReceived(phase);
                int numFizzedSlices = group.fizzedSlices.length;
                int curFizzedSlice = group.getFizzedSliceIndex(parent.filterNode.getParent());
                
                assert curFizzedSlice != -1;
                assert (totalItemsReceived % numFizzedSlices) == 0;
                
                int tailOffset = curFizzedSlice * (totalItemsReceived / numFizzedSlices);

                return new JExpressionStatement(
                    new JAssignmentExpression(tail, 
                                              new JIntLiteral(tailOffset)));
            }
        }

        return new JExpressionStatement(
            new JAssignmentExpression(tail,
                                      new JIntLiteral(0)));
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

    /**
     * Calculates least common multiple between <a> and <b>
     */
    private int LCM(int a, int b) {
    	int product = a * b;
    	
    	do {
    		if(a < b) {
    			int tmp = a;
    			a = b;
    			b = tmp;
    		}
    		
    		a = a % b;
    	} while(a != 0);
    	
    	return product / b;
    }
    
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

        if(phase == SchedulingPhase.STEADY && 
           FissionGroupStore.isFizzed(parent.filterNode.getParent()) &&
           LoadBalancer.isLoadBalanced(parent.filterNode.getParent())) {

            FissionGroup group = FissionGroupStore.getFissionGroup(parent.filterNode.getParent());
            FilterSliceNode filter = group.unfizzedSlice.getFirstFilter();
            FilterInfo fi = group.unfizzedFilterInfo;
            OutputSliceNode output = filter.getParent().getTail();

            // Declare variables used for transfers
            JVariableDefinition srcBaseOffsetVar =
                new JVariableDefinition(0,
                                        CStdType.Integer,
                                        "srcBaseOffset__" + myID,
                                        null);

            JVariableDefinition destBaseOffsetVar =
                new JVariableDefinition(0,
                                        CStdType.Integer,
                                        "destBaseOffset__" + myID,
                                        null);

            JVariableDefinition iterLoopVar =
                new JVariableDefinition(0,
                                        CStdType.Integer,
                                        "iter__" + myID,
                                        null);

            JVariableDefinition frameLoopVar =
                new JVariableDefinition(0,
                                        CStdType.Integer,
                                        "frame__" + myID,
                                        null);

            JVariableDeclarationStatement srcBaseOffsetDecl =
                new JVariableDeclarationStatement(srcBaseOffsetVar);
            JVariableDeclarationStatement destBaseOffsetDecl =
                new JVariableDeclarationStatement(destBaseOffsetVar);
            JVariableDeclarationStatement iterLoopVarDecl =
                new JVariableDeclarationStatement(iterLoopVar);
            JVariableDeclarationStatement frameLoopVarDecl =
                new JVariableDeclarationStatement(frameLoopVar);

            statements.add(srcBaseOffsetDecl);
            statements.add(destBaseOffsetDecl);
            statements.add(iterLoopVarDecl);
            statements.add(frameLoopVarDecl);

            // Number of output rotations per steady-state iteration
            assert fi.push % output.totalWeights(SchedulingPhase.STEADY) == 0;
            int outputRotsPerIter = fi.push / output.totalWeights(SchedulingPhase.STEADY);

            // Src stride per steady-state iteration
            int srcStridePerIter = outputRotsPerIter * output.totalWeights(phase);

            // Set base offset for reading data elements
            statements.add(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JLocalVariableExpression(srcBaseOffsetVar),
                        getWriteOffsetExpr(phase))));

            // Iterate through dests, generate transfer code for each dest
            for(InterSliceEdge edge : output.getDestSet(SchedulingPhase.STEADY)) {
                InputSliceNode input = edge.getDest();

                int outputWeight = output.getWeight(edge, SchedulingPhase.STEADY);
                int inputWeight = input.getWeight(edge, SchedulingPhase.STEADY);

                // Dest's copyDown
                int destCopyDown;
                if(FissionGroupStore.isFizzed(input.getParent())) {
                    FissionGroup inputGroup = FissionGroupStore.getFissionGroup(input.getParent());
                    destCopyDown = inputGroup.unfizzedFilterInfo.copyDown;
                }
                else {
                    FilterInfo destInfo = FilterInfo.getFilterInfo(input.getParent().getFirstFilter());
                    destCopyDown = destInfo.copyDown;
                }

                // Dest stride per steady-state iteration
                int destStridePerIter = (outputRotsPerIter * outputWeight) / inputWeight *
                    input.totalWeights(SchedulingPhase.STEADY);

                // Set base offset for writing data elements
                statements.add(
                    new JExpressionStatement(
                        new JAssignmentExpression(
                            new JLocalVariableExpression(destBaseOffsetVar),
                            new JAddExpression(
                                new JMultExpression(
                                    new JNameExpression(null,
                                                        LoadBalancer.getStartIterRef(
                                                            group, parent.filterNode.getParent())),
                                    new JIntLiteral(destStridePerIter)),
                                new JIntLiteral(destCopyDown)))));

                /********************** 
                 * Frame calculations *
                 **********************/

                // Size of frame
                int frameWidth = LCM(outputWeight, inputWeight);

                // Number of frames per steady-state iteration
                int framesPerIter = (outputRotsPerIter * outputWeight) / frameWidth;
                assert (outputRotsPerIter * outputWeight) % frameWidth == 0;

                // Src stride per frame
                int srcStridePerFrame = (frameWidth / outputWeight) *
                    output.totalWeights(SchedulingPhase.STEADY);

                // Dest stride per frame
                int destStridePerFrame = (frameWidth / inputWeight) * 
                    input.totalWeights(SchedulingPhase.STEADY);

                // Number of output and input rotations per frame
                int outputRotsPerFrame = frameWidth / outputWeight;
                int inputRotsPerFrame = frameWidth / inputWeight;                

                // Src offsets of all data elements sent in each frame
                int srcElemOffsets[] = new int[frameWidth];
                int srcElemIndex = 0;

                for(int rot = 0 ; rot < outputRotsPerFrame ; rot++) {
                    for(int index = 0 ; index < output.getWeights(phase).length ; index++) {
                        InterSliceEdge[] dests = output.getDests(phase)[index];
                        for(InterSliceEdge dest : dests) {
                            if(dest.equals(edge)) {
                                for(int item = 0 ; item < output.getWeights(phase)[index] ; item++) {
                                    srcElemOffsets[srcElemIndex++] =
                                        rot * output.totalWeights(phase) +
                                        output.weightBefore(index, phase) + item;
                                }

                                // TODO: Check for duplicates in dests?
                                break;
                            }
                        }
                    }
                }

                // Dest offsets of all data elements received in each frame
                int destElemOffsets[] = new int[frameWidth];
                int destElemIndex = 0;

                for(int rot = 0 ; rot < inputRotsPerFrame ; rot++) {
                    for(int index = 0 ; index < input.getWeights(phase).length ; index++) {
                        if(input.getSources(phase)[index].equals(edge)) {
                            for(int item = 0 ; item < input.getWeights(phase)[index] ; item++) {
                                destElemOffsets[destElemIndex++] =
                                    rot * input.totalWeights(phase) +
                                    input.weightBefore(index, phase) + item;
                            }
                        }
                    }
                }

                /*********************
                 * Transfer commands *
                 *********************/

                // Get source and dest buffer names
                String srcBuffer;
                if(((OutputRotatingBuffer)parent).hasDirectWrite()) {
                    FilterSliceNode directWriteFilter = 
                        ((OutputRotatingBuffer)parent).getDirectWriteFilter();
                    srcBuffer = 
                        ((OutputRotatingBuffer)parent).getAddressBuffer(
                            directWriteFilter.getParent().getHead()).currentWriteBufName;
                }
                else {
                    srcBuffer = ((OutputRotatingBuffer)parent).currentWriteBufName;
                }

                String destBuffer = 
                    ((OutputRotatingBuffer)parent).getAddressBuffer(
                        edge.getDest()).currentWriteBufName;

                // Generate transfers for each frame
                JBlock transfersPerFrame = new JBlock();
                for(int frameElem = 0 ; frameElem < frameWidth ; frameElem++) {
                    JExpression srcIndex =
                        new JAddExpression(
                            new JAddExpression(
                                new JAddExpression(
                                    new JLocalVariableExpression(srcBaseOffsetVar),
                                    new JMultExpression(
                                        new JLocalVariableExpression(iterLoopVar),
                                        new JIntLiteral(srcStridePerIter))),
                                new JMultExpression(
                                    new JLocalVariableExpression(frameLoopVar),
                                    new JIntLiteral(srcStridePerFrame))),
                            new JIntLiteral(srcElemOffsets[frameElem]));

                    JExpression destIndex =
                        new JAddExpression(
                            new JAddExpression(
                                new JAddExpression(
                                    new JLocalVariableExpression(destBaseOffsetVar),
                                    new JMultExpression(
                                        new JLocalVariableExpression(iterLoopVar),
                                        new JIntLiteral(destStridePerIter))),
                                new JMultExpression(
                                    new JLocalVariableExpression(frameLoopVar),
                                    new JIntLiteral(destStridePerFrame))),
                            new JIntLiteral(destElemOffsets[frameElem]));

                    JArrayAccessExpression srcAccess =
                        new JArrayAccessExpression(
                            null,
                            //new JNameExpression(null, srcBuffer)
                            new JFieldAccessExpression(srcBuffer),
                            srcIndex);

                    JArrayAccessExpression destAccess =
                        new JArrayAccessExpression(
                            null,
                            //new JNameExpression(null, destBuffer)
                            new JFieldAccessExpression(destBuffer),
                            destIndex);

                    JStatement transfer =
                        new JExpressionStatement(
                            new JAssignmentExpression(destAccess, srcAccess));

                    transfersPerFrame.addStatement(transfer);
                }

                // Repeat frame transfers enough times to transfer data for a single
                // steady-state iteration
                JStatement frameLoopInit =
                    new JExpressionStatement(
                        new JAssignmentExpression(
                            new JLocalVariableExpression(frameLoopVar),
                            new JIntLiteral(0)));

                JExpression frameLoopCond =
                    new JRelationalExpression(null,
                                              Constants.OPE_LT,
                                              new JLocalVariableExpression(null, frameLoopVar),
                                              new JIntLiteral(framesPerIter));

                JStatement frameLoopIncr =
                    new JExpressionStatement(null,
                                             new JPostfixExpression(null,
                                                                    Constants.OPE_POSTINC,
                                                                    new JLocalVariableExpression(null,
                                                                                                 frameLoopVar)),
                                             null);

                JStatement frameLoop = 
                    new JForStatement(frameLoopInit,
                                      frameLoopCond,
                                      frameLoopIncr,
                                      transfersPerFrame);

                // Repeat for every steady-state iteration owned by core
                JStatement iterLoopInit =
                    new JExpressionStatement(
                        new JAssignmentExpression(
                            new JLocalVariableExpression(iterLoopVar),
                            new JIntLiteral(0)));

                JExpression iterLoopCond =
                    new JRelationalExpression(null,
                                              Constants.OPE_LT,
                                              new JLocalVariableExpression(null, iterLoopVar),
                                              new JEmittedTextExpression(
                                                  LoadBalancer.getNumItersRef(group,
                                                                              parent.filterNode.getParent())));

                JStatement iterLoopIncr =
                    new JExpressionStatement(null,
                                             new JPostfixExpression(null,
                                                                    Constants.OPE_POSTINC,
                                                                    new JLocalVariableExpression(null,
                                                                                                 iterLoopVar)),
                                             null);

                JStatement iterLoop =
                    new JForStatement(iterLoopInit,
                                      iterLoopCond,
                                      iterLoopIncr,
                                      frameLoop);

                statements.add(iterLoop);

                /*
                // Generate loop to transfer data to destination
                JBlock transfersPerIter = new JBlock();
                
                int destElem = 0;
                for(int rot = 0 ; rot < outputRotsPerIter ; rot++) {
                    for(int index = 0 ; index < output.getWeights(phase).length ; index++) {
                        InterSliceEdge[] dests = output.getDests(phase)[index];
                        for(InterSliceEdge dest : dests) {
                            if(dest.equals(edge)) {
                                for(int curWeight = 0 ; curWeight < output.getWeights(phase)[index] ; curWeight++) {
                                    int srcOffset = rot * output.totalWeights(phase) +
                                        output.weightBefore(index, phase) + curWeight;
                                    JExpression srcIndex = 
                                        new JAddExpression(
                                            new JAddExpression(
                                                new JLocalVariableExpression(srcBaseOffsetVar),
                                                new JMultExpression(
                                                    new JLocalVariableExpression(loopIterVar),
                                                    new JIntLiteral(outputRotsPerIter * output.totalWeights(phase)))),
                                            new JIntLiteral(srcOffset));
                                    
                                    int destOffset = destElemOffsets[destElem++];
                                    JExpression destIndex = 
                                        new JAddExpression(
                                            new JAddExpression(
                                                new JLocalVariableExpression(destBaseOffsetVar),
                                                new JMultExpression(
                                                    new JLocalVariableExpression(loopIterVar),
                                                    new JIntLiteral(destStridePerIter))),
                                            new JIntLiteral(destOffset));

                                    JArrayAccessExpression srcAccess = 
                                        new JArrayAccessExpression(
                                            null,
                                            //new JNameExpression(null, srcBuffer),
                                            new JFieldAccessExpression(srcBuffer),
                                            srcIndex);

                                    JArrayAccessExpression destAccess =
                                        new JArrayAccessExpression(
                                            null,
                                            //new JNameExpression(null, destBuffer),
                                            new JFieldAccessExpression(destBuffer),
                                            destIndex);

                                    JStatement transfer = 
                                        new JExpressionStatement(
                                            new JAssignmentExpression(
                                                destAccess, srcAccess));

                                    transfersPerIter.addStatement(transfer);
                                }
                            }
                        }
                    }
                }

                JStatement loopInit = 
                    new JExpressionStatement(
                        new JAssignmentExpression(
                            new JLocalVariableExpression(loopIterVar), new JIntLiteral(0)));

                JExpression loopCond =
                    new JRelationalExpression(null,
                                              Constants.OPE_LT,
                                              new JLocalVariableExpression(null, loopIterVar),
                                              new JEmittedTextExpression(
                                                  LoadBalancer.getNumItersRef(group,
                                                                              parent.filterNode.getParent())));

                JStatement loopIncr =
                    new JExpressionStatement(null,
                                             new JPostfixExpression(null,
                                                                    Constants.OPE_POSTINC,
                                                                    new JLocalVariableExpression(null, loopIterVar)),
                                             null);

                JStatement forLoop =
                    new JForStatement(loopInit,
                                      loopCond,
                                      loopIncr,
                                      transfersPerIter);

                statements.add(forLoop);
                */
            }
        }
        else {
            FilterSliceNode filter = parent.filterNode;
            FilterInfo fi = FilterInfo.getFilterInfo(filter);
            
            //no further code necessary if nothing is being produced
            if (fi.totalItemsSent(phase) == 0)
                return;
            
            assert fi.totalItemsSent(phase) % output.totalWeights(phase) == 0;
            int rotations = fi.totalItemsSent(phase) / output.totalWeights(phase);
            
            //we might have to skip over some elements when we push into the buffer if this
            //is a shared buffer
            int writeOffset = getWriteOffsetValue(phase);       
            
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
            
            String srcBuffer;
            if(((OutputRotatingBuffer)parent).hasDirectWrite()) {
                FilterSliceNode directWriteFilter = ((OutputRotatingBuffer)parent).getDirectWriteFilter();
                srcBuffer = ((OutputRotatingBuffer)parent).getAddressBuffer(directWriteFilter.getParent().getHead()).currentWriteBufName;
            }
            else {
                srcBuffer = ((OutputRotatingBuffer)parent).currentWriteBufName;
            }
            
            int items = 0;
            for (int rot = 0; rot < rotations; rot++) {
                for (int weightIndex = 0; weightIndex < output.getWeights(phase).length; weightIndex++) {
                    InterSliceEdge[] dests = output.getDests(phase)[weightIndex];
                    for (int curWeight = 0; curWeight < output.getWeights(phase)[weightIndex]; curWeight++) {
                        int srcElement= rot * output.totalWeights(phase) + 
                            output.weightBefore(weightIndex, phase) + curWeight + writeOffset;
                        
                        for (InterSliceEdge dest : dests) {
                            int destElement = destIndices[destIndex.get(dest)][nextWriteIndex[destIndex.get(dest)]];
                            nextWriteIndex[destIndex.get(dest)]++;
                            
                            SourceAddressRotation addrBuf = ((OutputRotatingBuffer)parent).getAddressBuffer(dest.getDest());
                            String destBuffer = addrBuf.currentWriteBufName;
                            
                            //don't do anything if src buffer and dest buffer are the same, we have
                            //direct write, and src/dest indices are the same
                            if (destBuffer.equals(srcBuffer) && destElement == srcElement && ((OutputRotatingBuffer)parent).hasDirectWrite()) 
                                continue;
                            
                            if (destBuffer.equals(srcBuffer)) {
                                assert !((OutputRotatingBuffer)parent).hasDirectWrite() : "Trying to reorder a single buffer! Could lead to race. filter: " + filter + ", phase: " + phase + ", src: " + srcElement + ", dest: " + destElement;
                            }
                            
                            reorderStatements.addAssignment(destBuffer, "", destElement,
                                                            srcBuffer, "", srcElement);
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
    }
    
    private int[] getDestIndices(InterSliceEdge edge, int outputRots, SchedulingPhase phase) {
    	assert (parent instanceof OutputRotatingBuffer);
    	
        int[] indices = new int[outputRots * output.getWeight(edge, phase)];
        InputSliceNode input = edge.getDest();

        assert indices.length % input.getWeight(edge, phase) == 0;

        int dsFilterCopyDown = 0;
        if(FissionGroupStore.isFizzed(input.getParent()))
            dsFilterCopyDown = FissionGroupStore.getUnfizzedFilterInfo(input.getParent()).copyDown;
        else
            dsFilterCopyDown = FilterInfo.getFilterInfo(input.getNextFilter()).copyDown;
        debugPrint("getDestIndices", phase, edge, "dsFilterCopyDown", dsFilterCopyDown);

        int fissionOffset = 0;
        if(phase != SchedulingPhase.INIT && FissionGroupStore.isFizzed(parent.filterNode.getParent())) {
            FissionGroup group = FissionGroupStore.getFissionGroup(parent.filterNode.getParent());

            // Calculate number of elements downstream filter has received from the fizzed
            // slices preceding this fizzed slice
            int numItersPerFizzedSlice = group.unfizzedFilterInfo.getMult(phase) / 
                group.fizzedSlices.length;
            int sourceFizzedIndex = group.getFizzedSliceIndex(parent.filterNode.getParent());

            assert (group.unfizzedFilterInfo.push * numItersPerFizzedSlice * sourceFizzedIndex) % 
                output.totalWeights(phase) == 0;
            int numPrevReceived = 
                (group.unfizzedFilterInfo.push * numItersPerFizzedSlice * sourceFizzedIndex) /
                output.totalWeights(phase) * output.getWeight(edge, phase);

            // Calculate number of previous input rotations
            assert numPrevReceived % input.getWeight(edge, phase) == 0;
            int numPrevInputRots = numPrevReceived / input.getWeight(edge, phase);

            // Calculate fission offset based upon number of previous input rotations
            fissionOffset = numPrevInputRots * input.totalWeights(phase);
        }

        debugPrint("getDestIndices", phase, edge, "fissionOffset", fissionOffset);

        int inputRots = indices.length / input.getWeight(edge, phase);
        int nextWriteIndex = 0;

        for (int rot = 0; rot < inputRots; rot++) {
            for (int index = 0; index < input.getWeights(phase).length; index++) {
                if (input.getSources(phase)[index] == edge) {
                    //debugPrint("getDestIndices", phase, edge, "index", index);
                    //debugPrint("getDestIndices", phase, edge, "weightBefore", input.weightBefore(index, phase));
                    for (int item = 0; item < input.getWeights(phase)[index]; item++) {
                        indices[nextWriteIndex++] = rot * input.totalWeights(phase) +
                            input.weightBefore(index, phase) + item + 
                            (phase == SchedulingPhase.INIT ? 0 : dsFilterCopyDown) +
                            fissionOffset;
                    }
                }
            }
        }
        
        assert nextWriteIndex == indices.length;
        
        return indices;
    }

    private int getBaseWriteOffset(SchedulingPhase phase) {
        assert (parent instanceof OutputRotatingBuffer);

        if (((OutputRotatingBuffer)parent).hasDirectWrite()) {
            Slice srcSlice = parent.filterNode.getParent();
            Slice destSlice = ((OutputRotatingBuffer)parent).getDirectWriteFilter().getParent();

            if(FissionGroupStore.isFizzed(srcSlice))
                srcSlice = FissionGroupStore.getUnfizzedSlice(srcSlice);

            if(FissionGroupStore.isFizzed(destSlice))
                destSlice = FissionGroupStore.getUnfizzedSlice(destSlice);

            //find edge from source to dest slice
        	Set<InterSliceEdge> edges = srcSlice.getTail().getDestSet(phase);
        	InterSliceEdge edge = null;
        	
        	for(InterSliceEdge e : edges) {
        		if(e.getDest().getParent().equals(destSlice)) {
        			edge = e;
        			break;
        		}
        	}

        	//make sure we actually write in this stage
        	if(edge == null)
        		return 0;
        	
        	int offset = edge.getDest().weightBefore(edge, phase);

            //if we are not in the init, we must skip over the dest's copy down
        	if(phase != SchedulingPhase.INIT) {
                if(FissionGroupStore.isFizzed(((OutputRotatingBuffer)parent).getDirectWriteFilter().getParent())) {
                    FissionGroup group = FissionGroupStore.getFissionGroup(((OutputRotatingBuffer)parent).getDirectWriteFilter().getParent());
                    offset += group.unfizzedFilterInfo.copyDown;
                }
                else {
                    offset += FilterInfo.getFilterInfo(((OutputRotatingBuffer)parent).getDirectWriteFilter()).copyDown;
                }
        	}

            return offset;
        }

        return 0;
    }
    
    private int getWriteOffsetValue(SchedulingPhase phase) {
    	assert (parent instanceof OutputRotatingBuffer);
        
        /*
        assert !FissionGroupStore.isFizzed(parent.filterNode.getParent()) ||
            !LoadBalancer.isLoadBalanced(parent.filterNode.getParent());
        */

        if (((OutputRotatingBuffer)parent).hasDirectWrite()) {
            //if we are directly writing then we have to get the index into the remote
            //buffer of start of this source

            int offset = getBaseWriteOffset(phase);
            debugPrint("getWriteOffsetValue", phase, parent.filterNode, "edgeWriteOffset", offset);

            if(phase != SchedulingPhase.INIT && FissionGroupStore.isFizzed(parent.filterNode.getParent())) {
                FissionGroup group = FissionGroupStore.getFissionGroup(parent.filterNode.getParent());
                
                int totalItemsSent = group.unfizzedFilterInfo.totalItemsSent(phase);
                int numFizzedSlices = group.fizzedSlices.length;
                int curFizzedSlice = group.getFizzedSliceIndex(parent.filterNode.getParent());
                
                assert curFizzedSlice != -1;
                assert (totalItemsSent % numFizzedSlices) == 0;
                
                offset += curFizzedSlice * (totalItemsSent / numFizzedSlices);
                debugPrint("getWriteOffsetValue", phase, parent.filterNode, "fissionOffset",
                           curFizzedSlice * (totalItemsSent / numFizzedSlices));
            }

            debugPrint("getWriteOffsetValue", phase, parent.filterNode, "offset", offset);
        	return offset;
        }
        else {
            debugPrint("getWriteOffsetValue", phase, parent.filterNode, "offset", 0);
            return 0;
        }
    }

    private JExpression getWriteOffsetExpr(SchedulingPhase phase) {
        assert (parent instanceof OutputRotatingBuffer);

        assert FissionGroupStore.isFizzed(parent.filterNode.getParent()) &&
            LoadBalancer.isLoadBalanced(parent.filterNode.getParent());

        if(((OutputRotatingBuffer)parent).hasDirectWrite()) {
            //if we are directly writing then we have to get the index into the remote
            //buffer of start of this source

            int offset = getBaseWriteOffset(phase);
            debugPrint("getWriteOffsetExpr", phase, parent.filterNode, "edgeWriteOffset", offset);

            if(phase != SchedulingPhase.INIT && FissionGroupStore.isFizzed(parent.filterNode.getParent())) {
                FissionGroup group = FissionGroupStore.getFissionGroup(parent.filterNode.getParent());
                assert LoadBalancer.isLoadBalanced(group);

                JEmittedTextExpression startIterExpr =
                    new JEmittedTextExpression(
                        LoadBalancer.getStartIterRef(group, parent.filterNode.getParent()));
                JIntLiteral pushRate = new JIntLiteral(group.unfizzedFilterInfo.push);

                return new JAddExpression(
                    new JIntLiteral(offset),
                    new JMultExpression(startIterExpr,
                                        pushRate));
            }

            return new JIntLiteral(offset);
        }
        else {
            return new JIntLiteral(0);
        }
    }

    public JStatement zeroOutHead(SchedulingPhase phase) {
    	assert (parent instanceof OutputRotatingBuffer);
    	
        if(FissionGroupStore.isFizzed(parent.filterNode.getParent()) &&
           LoadBalancer.isLoadBalanced(parent.filterNode.getParent())) {
            JExpression writeOffsetExpr = getWriteOffsetExpr(phase);
            return new JExpressionStatement(
                new JAssignmentExpression(head, writeOffsetExpr));
        }
        else {
            int writeOffsetValue = getWriteOffsetValue(phase);
            debugPrint("zeroOutHead", phase, parent.filterNode, "writeOffsetValue", writeOffsetValue);

            JBlock block = new JBlock();        
            block.addStatement(new JExpressionStatement(
                                   new JAssignmentExpression(head, 
                                                             new JIntLiteral(writeOffsetValue))));
            return block;
        }
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
