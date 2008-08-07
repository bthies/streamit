package at.dms.kjc.tilera;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JPostfixExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.slicegraph.*;


/**
 * In this rotating buffer we add a transfer pointer that is always one behind the current 
 * pointer and points to the buffer we should be transferring from.
 * 
 * @author mgordon
 *
 */
public class OutputRotatingBuffer extends RotatingBuffer {
    
    /** map of all the output buffers from filter -> outputbuffer */
    protected static HashMap<FilterSliceNode, OutputRotatingBuffer> buffers;
    /** name of the variable that points to the rotation structure we should be transferring from */
    public String transRotName;
    /** name of the variable that points to the buffer we should be transferring from */
    public String transBufName;
    /** name of variable containing head of array offset */
    protected String headName;
    /** definition for head */
    protected JVariableDefinition headDefn;
    /** the output slice node for this output buffer */
    protected OutputSliceNode outputNode;
    /** reference to head */
    protected JExpression head;      
    /** the dma commands that are generated for this output buffer */
    protected OutputBufferDMATransfers dmaCommands;
    /** the address buffers that this output rotation uses as destinations for dma commands */ 
    protected HashMap<InputRotatingBuffer, DMAAddressRotation> addressBuffers;
    /** the tile we are mapped to */
    protected Tile tile;
    
    static {
        buffers = new HashMap<FilterSliceNode, OutputRotatingBuffer>();
    }

    /**
     * Create all the output buffers necessary for this slice graph.  Iterate over
     * the steady-state schedule, visiting each slice and creating an output buffer
     * for the filter of the slice
     * 
     * @param slices The steady-state schedule of slices
     */
    public static void createOutputBuffers(BasicSpaceTimeSchedule schedule) {
        for (Slice slice : schedule.getScheduleList()) {
            assert slice.getNumFilters() == 1;
            if (!slice.getTail().noOutputs()) {
                assert slice.getTail().totalWeights() > 0;
                Tile parent = TileraBackend.backEndBits.getLayout().getComputeNode(slice.getFirstFilter());
                //create the new buffer, the constructor will put the buffer in the 
                //hashmap
                OutputRotatingBuffer buf = new OutputRotatingBuffer(slice.getFirstFilter(), parent);
                
                //calculate the rotation length
                int srcMult = schedule.getPrimePumpMult(slice);
                int maxRotLength = 0;
                for (Slice dest : slice.getTail().getDestSlices()) {
                    int diff = srcMult - schedule.getPrimePumpMult(dest);
                    assert diff >= 0;
                    if (diff > maxRotLength)
                        maxRotLength = diff;
                }
                buf.rotationLength = maxRotLength + 1;
                buf.createInitCode(false);
                System.out.println("Setting output buf " + buf.getIdent() + " to " + buf.rotationLength);    
            }
        }
    }
    
    
    /**
     * Create a new output buffer that is associated with the filter node.
     * 
     * @param filterNode The filternode for which to create a new output buffer.
     */
    private OutputRotatingBuffer(FilterSliceNode filterNode, Tile parent) {
        super(filterNode.getEdgeToNext(), filterNode, parent);
        outputNode = filterNode.getParent().getTail();
        bufType = filterNode.getFilter().getOutputType();
        buffers.put(filterNode, this);
        headName = this.getIdent() + "head";
        headDefn = new JVariableDefinition(null,
                at.dms.kjc.Constants.ACC_STATIC,
                CStdType.Integer, headName, null);
        transRotName = this.getIdent() + "_rot_trans";
        transBufName = this.getIdent() + "_trans_buf";
        
        head = new JFieldAccessExpression(headName);
        head.setType(CStdType.Integer);
        tile = TileraBackend.backEndBits.getLayout().getComputeNode(filterNode);
        
        //fill the dmaaddressbuffers array
        addressBuffers = new HashMap<InputRotatingBuffer, DMAAddressRotation>();
        for (InterSliceEdge edge : outputNode.getDestSet()) {
            InputRotatingBuffer input = InputRotatingBuffer.getInputBuffer(edge.getDest().getNextFilter());
            addressBuffers.put(input, input.getAddressRotation(tile));               
        }
        
        //generate the dma commands
        dmaCommands = new OutputBufferDMATransfers(this);
    }
   
    /**
     * Return the output buffer associated with the filter node.
     * 
     * @param fsn The filter node in question.
     * @return The output buffer of the filter node.
     */
    public static OutputRotatingBuffer getOutputBuffer(FilterSliceNode fsn) {
        return buffers.get(fsn);
    }
    
    /**
     * Return the set of all the InputBuffers that are mapped to tile t.
     */
    public static Set<RotatingBuffer> getBuffersOnTile(Tile t) {
        HashSet<RotatingBuffer> set = new HashSet<RotatingBuffer>();
        
        for (RotatingBuffer b : buffers.values()) {
            if (TileraBackend.backEndBits.getLayout().getComputeNode(b.getFilterNode()).equals(t))
                set.add(b);
        }
        
        return set;
    }
    
    /**
     * Return the address rotation that this output rotation uses for the given input slice node
     * 
     * @param input the input slice node 
     * @return the dma address rotation used to store the address of the 
     * rotation associated with this input slice node
     */
    public DMAAddressRotation getAddressBuffer(InputSliceNode input) {
        assert addressBuffers.containsKey(InputRotatingBuffer.getInputBuffer(input.getNextFilter()));
        
        return addressBuffers.get(InputRotatingBuffer.getInputBuffer(input.getNextFilter()));
    }
    
    protected void setBufferSize() {
        FilterInfo fi = FilterInfo.getFilterInfo(filterNode);
        
        bufSize = Math.max(fi.totalItemsSent(SchedulingPhase.INIT),
                fi.totalItemsSent(SchedulingPhase.STEADY));
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popMethodName()
     */
    public String popMethodName() {
        assert false : "Should not call pop() method on output buffer.";
        return "";
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popMethod()
     */
    public JMethodDeclaration popMethod() {
        assert false : "Should not call pop() method on output buffer.";
        return null;
    }
    
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popManyMethodName()
     */
    public String popManyMethodName() {
        assert false : "Should not call pop() method on output buffer.";
        return "";
    }
 
        /**
     * Pop many items at once ignoring them.
     * Default method generated here to call popMethod() repeatedly.
     */
    public JMethodDeclaration popManyMethod() {
        assert false : "Should not call pop() method on output buffer.";
        return null;
     }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPopMethodName()
     */
    public String assignFromPopMethodName() {
        assert false : "Should not call pop() method on output buffer.";
        return "";
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPopMethod()
     */
    public JMethodDeclaration assignFromPopMethod() {
        assert false : "Should not call pop() method on output buffer.";
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#peekMethodName()
     */
    public String peekMethodName() {
        assert false : "Should not call peek() method on output buffer.";
        return "";
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#peekMethod()
     */
    public JMethodDeclaration peekMethod() {
        assert false : "Should not call peek() method on output buffer.";
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPeekMethodName()
     */
    public String assignFromPeekMethodName() {
        assert false : "Should not call peek() method on output buffer.";
        return "";
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPeekMethod()
     */
    public JMethodDeclaration assignFromPeekMethod() {
        assert false : "Should not call peek() method on output buffer.";
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethodName()
     */
    public String pushMethodName() {
        return "__push_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethod()
     */
    public JMethodDeclaration pushMethod() {
        String valName = "__val";
        JFormalParameter val = new JFormalParameter(
                theEdge.getType(),
                valName);
        JLocalVariableExpression valRef = new JLocalVariableExpression(val);
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                pushMethodName(),
                new JFormalParameter[]{val},
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
        new JExpressionStatement(new JAssignmentExpression(
                bufRef(new JPostfixExpression(at.dms.kjc.Constants.OPE_POSTINC,
                        head)),
                valRef)));
        return retval;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitWrite()
     */
    public List<JStatement> beginInitWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(zeroOutHead());
        return list;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitWrite()
     */
    public List<JStatement> endInitWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        //in the init stage we use dma to send the output to the dest filter
        //but we have to wait until the end because are not double buffering
        //also, don't rotate anything here
        list.addAll(dmaCommands.dmaCommands(SchedulingPhase.INIT));
        return list;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyWrite()
     */
    public List<JStatement> beginSteadyWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(zeroOutHead());
        list.addAll(dmaCommands.dmaCommands(SchedulingPhase.STEADY));
        return list;
    }
    
    protected List<JStatement> rotateStatements() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(Util.toStmt(currentRotName + " = " + currentRotName + "->next"));
        list.add(Util.toStmt(currentBufName + " = " + currentRotName + "->buffer"));
        list.add(Util.toStmt(transRotName + " = " + transRotName + "->next"));
        list.add(Util.toStmt(transBufName + " = " + transRotName + "->buffer"));
        return list;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyWrite()
     */
    public List<JStatement> endSteadyWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.addAll(dmaCommands.waitCallsSteady());
        //generate the rotate statements for this output buffer
        list.addAll(rotateStatements());
        
        //generate the rotation statements for the address buffers that this output
        //buffer uses
        for (DMAAddressRotation addrRot : addressBuffers.values()) {
            list.addAll(addrRot.rotateStatements());
        }
        return list;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#topOfWorkSteadyWrite()
     */
    public List<JStatement> topOfWorkSteadyWrite() {
        return new LinkedList<JStatement>(); 
    }
 
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#dataDeclsH()
     */
    public List<JStatement> dataDeclsH() {
        return new LinkedList<JStatement>();
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#dataDecls()
     */
    public List<JStatement> dataDecls() {
        List<JStatement> retval = new LinkedList<JStatement>();
        return retval;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDeclsExtern()
     */
    public List<JStatement> writeDeclsExtern() {
        return new LinkedList<JStatement>();
    }   
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDecls()
     */
    public List<JStatement> writeDecls() {
        JStatement tailDecl = new JVariableDeclarationStatement(headDefn);
        List<JStatement> retval = new LinkedList<JStatement>();
        retval.add(tailDecl);
        retval.addAll(dmaCommands.decls());
        return retval;
    }   

    /** Create statement zeroing out head */
    protected JStatement zeroOutHead() {
        return new JExpressionStatement(
                        new JAssignmentExpression(head, new JIntLiteral(0)));
    }

    
    /**
     * Generate the code to setup the structure of the rotating buffer 
     * as a circular linked list.
     */
    protected void setupRotation() {
        String temp = "__temp__";
        TileCodeStore cs = parent.getComputeCode();
        //this is the typedef we will use for this buffer rotation structure
        String rotType = rotTypeDefPrefix + getType().toString();
        
        //add the declaration of the rotation buffer of the appropriate rotation type
        parent.getComputeCode().appendTxtToGlobal(rotType + " *" + rotStructName + ";\n");
      //add the declaration of the pointer that points to the current rotation in the rotation structure
        parent.getComputeCode().appendTxtToGlobal(rotType + " *" + currentRotName + ";\n");
        //add the declaration of the pointer that points to the current buffer in the current rotation
        parent.getComputeCode().appendTxtToGlobal(bufType.toString() + " *" + currentBufName + ";\n");
        
        //add the declaration of the pointer that points to the transfer rotation in the rotation structure
        parent.getComputeCode().appendTxtToGlobal(rotType + " *" + transRotName + ";\n");
        //add the declaration of the pointer that points to the transfer buffer in the current rotation
        parent.getComputeCode().appendTxtToGlobal(bufType.toString() + " *" + transBufName + ";\n");
        
        JBlock block = new JBlock();
        
        //create a temp var
        if (this.rotationLength > 1)
            block.addStatement(Util.toStmt(rotType + " *" + temp));
        
        //create the first entry!!
        block.addStatement(Util.toStmt(rotStructName + " =  (" + rotType+ "*)" + "malloc(sizeof("
                + rotType + "))"));
        
        //modify the first entry
        block.addStatement(Util.toStmt(rotStructName + "->buffer = " + bufferNames[0]));
        if (this.rotationLength == 1)  {
            //we want to start the transfer buffer one behind the current buffer we are 
            //writing to, so set the transfer buffer to the last entry
            //remember that that transfer buffer pointer is not used during the init stage
            block.addStatement(Util.toStmt(transRotName + " = " + rotStructName));
            block.addStatement(Util.toStmt(transBufName + " = " + rotStructName + "->buffer"));
            //loop the structure
            block.addStatement(Util.toStmt(rotStructName + "->next = " + rotStructName));
        }
        else {
            block.addStatement(Util.toStmt(temp + " = (" + rotType+ "*)" + "malloc(sizeof("
                    + rotType + "))"));    
            
            block.addStatement(Util.toStmt(rotStructName + "->next = " + 
                    temp));
            
            block.addStatement(Util.toStmt(temp + "->buffer = " + bufferNames[1]));
            
            for (int i = 2; i < this.rotationLength; i++) {
                block.addStatement(Util.toStmt(temp + "->next =  (" + rotType+ "*)" + "malloc(sizeof("
                        + rotType + "))"));
                block.addStatement(Util.toStmt(temp + " = " + temp + "->next"));
                block.addStatement(Util.toStmt(temp + "->buffer = " + bufferNames[i]));
            }
            
            //we want to start the transfer buffer one behind the current buffer we are 
            //writing to, so set the transfer buffer to the last entry
            //remember that that transfer buffer pointer is not used during the init stage
            block.addStatement(Util.toStmt(transRotName + " = " + temp));
            block.addStatement(Util.toStmt(transBufName + " = " + temp + "->buffer"));
            

            block.addStatement(Util.toStmt(temp + "->next = " + rotStructName));
        }
        block.addStatement(Util.toStmt(currentRotName + " = " + rotStructName));
        block.addStatement(Util.toStmt(currentBufName + " = " + currentRotName + "->buffer"));
        cs.addStatementToBufferInit(block);
    }
    
}
