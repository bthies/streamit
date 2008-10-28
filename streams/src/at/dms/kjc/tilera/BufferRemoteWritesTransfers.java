package at.dms.kjc.tilera;

import at.dms.kjc.slicegraph.*;

import java.util.HashMap;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.*;
import java.util.LinkedList;

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
    /** the address array of addresses into the local shared buffer (src buffer) in 
     * which to write for init
     */
    protected LinkedList<Integer> addressArrayInit;
    /** the address array of addresses into the local shared buffer (src buffer) in 
     * which to write for steady
     */
    protected LinkedList<Integer> addressArraySteady;
    protected static int uniqueID = 0;
    protected int myID;
    /** name of the address array pointer that is used in the push method,
     * points to either steady or init
     */
    protected String addrArrayPointer;
    /** name of the var that points to the address array for the steady */
    protected String addrArraySteadyVar;
    /** name of the var that points to the address array for the init */
    protected String addrArrayInitVar;
    
    public BufferRemoteWritesTransfers(RotatingBuffer buf) {
        super(buf);
        myID = uniqueID++;
        
        //set up the head pointer for writing
        writeHeadName = buf.getIdent() + "head";
        writeHeadDefn = new JVariableDefinition(null,
                at.dms.kjc.Constants.ACC_STATIC,
                CStdType.Integer, writeHeadName, null);
        
        head = new JFieldAccessExpression(writeHeadName);
        head.setType(CStdType.Integer);
        
        setWritingScheme();
  
        if (output.oneOutput(SchedulingPhase.STEADY) && 
                (output.oneOutput(SchedulingPhase.INIT) || output.noOutputs(SchedulingPhase.INIT)) &&
                TileraBackend.scheduler.getComputeNode(parent.filterNode) !=
                    TileraBackend.scheduler.getComputeNode(output.getSingleEdge(SchedulingPhase.STEADY).getDest().getNextFilter()) &&
                    output.getSingleEdge(SchedulingPhase.STEADY).getDest().singleAppearance())
        {
            directWrite = true;
            assert !usesSharedBuffer();
        }
        
        decls.add(new JVariableDeclarationStatement(writeHeadDefn));
        
        generateStatements(SchedulingPhase.INIT);
        if (needAddressArray) {
            addAddressArrayDecls(SchedulingPhase.INIT);
        }
        generateStatements(SchedulingPhase.STEADY);    
        if (needAddressArray) {  
            addAddressArrayDecls(SchedulingPhase.STEADY);
        }
    }

    /**
     * If these transfers have as there source a shared input buffer, we might have 
     * to use an address array for the indices of the upstream push method.  This will
     * determine if we have to use an address array.
     */
    private void setWritingScheme() {
        //if we are using a shared buffer then check if we need an address array to write into
        //the shared buffer
        if (usesSharedBuffer()) {
            FilterInfo localDest = FilterInfo.getFilterInfo(parent.filterNode);
            System.out.println(parent.filterNode);
            InputSliceNode input = parent.filterNode.getParent().getHead();
            int rotations = localDest.totalItemsReceived(SchedulingPhase.STEADY) / 
                input.totalWeights(SchedulingPhase.STEADY);
            
            if (rotations == 1 && input.singleAppearance()) {
                needAddressArray = false;
            } else {
                System.out.println(((InputRotatingBuffer)parent).localSrcFilter + " needs an address array!");
                needAddressArray = true;
                addrArrayInitVar = "__addArray_init" + myID + "__"; 
                addrArraySteadyVar = "__addArray_steady" + myID + "__"; 
                addrArrayPointer = "__addArray_" + myID + "__"; 
            }
            
        } else {
            System.out.println("non-local: " + parent.filterNode);
            needAddressArray = false;
        }
    }
    

    public boolean usesSharedBuffer() {
        return parent instanceof InputRotatingBuffer;
    }
    
    /**
     * Add the static initializer of the address array for pushing into the input buffer of the
     * downstream filter.
     */
    private void addAddressArrayDecls(SchedulingPhase phase) {
        boolean init = (phase == SchedulingPhase.INIT); 
        LinkedList<Integer> addressArray = (init ? addressArrayInit : addressArraySteady);
        String varName = "__addArray_" + phase + myID + "__"; 
        
        if (init) {
            varName = addrArrayInitVar;
            decls.add(Util.toStmt("int *" + addrArrayPointer));
        } 
        else
            varName = addrArraySteadyVar;
        
        
        StringBuffer decl = new StringBuffer("int " + varName + "[] = {");
        if (addressArray != null) {
            for (int i = 0 ; i < addressArray.size(); i++) {
                decl.append(addressArray.get(i));
                if (i != addressArray.size() - 1)
                    decl.append(", ");
            }
        }
        decl.append("}");
        decls.add(Util.toStmt(decl.toString()));
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

        LinkedList<Integer> addressArray = null;
        if (needAddressArray) {
            addressArray = new LinkedList<Integer>(); 
        }

        FilterInfo fi = FilterInfo.getFilterInfo(filter);

        List<JStatement> statements = null;
        
        switch (phase) {
            case INIT: statements = commandsInit; break;
            case PRIMEPUMP: assert false; break;
            case STEADY: statements = commandsSteady; break;
        }
        
        if (needAddressArray && phase == SchedulingPhase.INIT) {
            //during the init, we use the init address array, but after that we should use the 
            //steady addr array, so this statement is appended to the end of the statements for the
            //init to set the address array pointer to the steady for the rest of execution
            statements.add(Util.toStmt(addrArrayPointer + " = " + addrArraySteadyVar));
        }

        //no further code necessary if nothing is being produced
        if (fi.totalItemsSent(phase) == 0)
            return;
        
        assert fi.totalItemsSent(phase) % output.totalWeights(phase) == 0;
        
        //we might have to skip over some elements when we push into the buffer if this
        //is a shared buffer
        int writeOffset = getWriteOffset(phase);
        //System.out.println("Source write offset = " + writeOffset);     
          
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
                                //if we need an address array then just remember the destination address
                                if (needAddressArray) {
                                    addressArray.add(destElement);
                                } else {  //no address array, if the addresses are different, we need to move 
                                    if (destElement < sourceElement) {
                                        statements.add(Util.toStmt(parent.currentWriteBufName + "[ " + destElement + "] = " + 
                                                parent.currentWriteBufName + "[" + sourceElement + "]"));
                                    }  //bad!
                                    else if (destElement > sourceElement) {
                                        System.out.println(filter + " -> " + dest.getDest().getNextFilter());
                                        assert false : "Dest: " + dest.getDest().getNextFilter() + " " + sourceElement + " < " + 
                                        destElement + " " + phase + "\n";// + dest.getDest().debugString(false);
                                    }
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

        if (needAddressArray) {
            if (phase == SchedulingPhase.INIT) {
                addressArrayInit = addressArray;
            }
            else
                addressArraySteady = addressArray;
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
        if (usesSharedBuffer() && !needAddressArray) {
            //no address array needed but we have to set the head to the copydown plus
            //the weights of any inputs that are not mapped to this tile that appear before
            //the local source
            FilterInfo localDest = FilterInfo.getFilterInfo(parent.filterNode);
            InputSliceNode input = parent.filterNode.getParent().getHead();
            FilterSliceNode localSrc = ((InputRotatingBuffer)parent).getLocalSrcFilter();
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
            if (needAddressArray) {
                //the head is not the index into the address array, so just reset it to zero
                literal = 0;
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
        
        //if we are in the init we use the init address array, gets changed to steady after init
        if (phase == SchedulingPhase.INIT)
            block.addStatement(Util.toStmt(addrArrayPointer + " = " + addrArrayInitVar));
        
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
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                parent.pushMethodName(),
                new JFormalParameter[]{val},
                CClassType.EMPTY,
                body, null, null);
        if (needAddressArray) {
            JFieldAccessExpression addressArray = new JFieldAccessExpression(new JThisExpression(),
                    addrArrayPointer);
            body.addStatement(
                    new JExpressionStatement(new JAssignmentExpression(
                            new JArrayAccessExpression(bufRef, 
                                    new JArrayAccessExpression(
                                            addressArray, 
                                            new JPostfixExpression(at.dms.kjc.Constants.OPE_POSTINC, head))),
                                    valRef)));
            
        } else {
            body.addStatement(
                    new JExpressionStatement(new JAssignmentExpression(
                            new JArrayAccessExpression(bufRef, new JPostfixExpression(at.dms.kjc.Constants.OPE_POSTINC,
                                    head)),
                                    valRef)));
        }
        return retval;
    }
    
    
}
