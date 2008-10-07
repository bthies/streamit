package at.dms.kjc.tilera;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBooleanLiteral;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIfStatement;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JLogicalComplementExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JPostfixExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;

public class OutputRotatingBuffer extends RotatingBuffer {
    /** the output slice node for this output buffer */
    protected OutputSliceNode outputNode;    
    /** the tile we are mapped to */
    protected Tile tile;
    
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
            //don't do anything for file readers or writers,
            //for file readers the output buffer is allocated in processfilereader
            if (slice.getHead().getNextFilter().isPredefined())
                continue;
            
            if (!slice.getTail().noOutputs()) {
                assert slice.getTail().totalWeights(SchedulingPhase.STEADY) > 0;
                Tile parent = TileraBackend.backEndBits.getLayout().getComputeNode(slice.getFirstFilter());
                //only create an output buffer if no downstream filter is mapped to this tile
                //if a downstream filter is mapped to this tile, then this slice will use the inputbuffer
                //for its output
                boolean createBuffer = true;
                //look to see if any downstream filters are mapped to this tile
                for (InterSliceEdge edge : slice.getTail().getDestSet(SchedulingPhase.STEADY)) {
                    if (parent == 
                        TileraBackend.backEndBits.getLayout().getComputeNode(edge.getDest().getNextFilter())) {
                        createBuffer = false;
                        break;
                    }
                }

                if (createBuffer) {
                    // create the new buffer, the constructor will put the buffer in the 
                    //hashmap
                    
                    OutputRotatingBuffer buf = new OutputRotatingBuffer(slice.getFirstFilter(), parent);
                    buf.setRotationLength(schedule);
                    buf.setBufferSize();
                    buf.createInitCode(false);

                }
            }
        }
    }
    /**
     * Create a new output buffer that is associated with the filter node.
     * 
     * @param filterNode The filternode for which to create a new output buffer.
     */
    protected OutputRotatingBuffer(FilterSliceNode filterNode, Tile parent) {
        super(filterNode.getEdgeToNext(), filterNode, parent);
        outputNode = filterNode.getParent().getTail();
        bufType = filterNode.getFilter().getOutputType();
        setOutputBuffer(filterNode, this);
       
        transRotName = this.getIdent() + "_rot_trans";
        transBufName = this.getIdent() + "_trans_buf";
              
        tile = TileraBackend.backEndBits.getLayout().getComputeNode(filterNode);
        
        firstExeName = "__first__" + this.getIdent();        
        firstExe = new JVariableDefinition(null,
                at.dms.kjc.Constants.ACC_STATIC,
                CStdType.Boolean, firstExeName, new JBooleanLiteral(true));
        
    }
   
    /** Create an array reference given an offset */   
    public JFieldAccessExpression writeBufRef() {
        return new JFieldAccessExpression(new JThisExpression(), currentWriteBufName);
     
    }
    
    public void createAddressBuffers() {
      //fill the addressbuffers array
        addressBuffers = new HashMap<InputRotatingBuffer, SourceAddressRotation>();
        for (InterSliceEdge edge : outputNode.getDestSet(SchedulingPhase.STEADY)) {
            InputRotatingBuffer input = InputRotatingBuffer.getInputBuffer(edge.getDest().getNextFilter());
            addressBuffers.put(input, input.getAddressRotation(tile));               
        }
    }
    
    public void setRotationLength(BasicSpaceTimeSchedule schedule) {
        //calculate the rotation length
        int srcMult = schedule.getPrimePumpMult(filterNode.getParent());
        int maxRotLength = 0;
        for (Slice dest : filterNode.getParent().getTail().getDestSlices(SchedulingPhase.STEADY)) {
            int diff = srcMult - schedule.getPrimePumpMult(dest);
            assert diff >= 0;
            if (diff > maxRotLength)
                maxRotLength = diff;
        }
        rotationLength = maxRotLength + 1;
    }
    

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitWrite()
     */
    public List<JStatement> endInitWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        //in the init stage we use dma to send the output to the dest filter
        //but we have to wait until the end because are not double buffering
        //also, don't rotate anything here
        list.addAll(transferCommands.transferCommands(SchedulingPhase.INIT));
        return list;
    }
    
    /**
     *  We don't want to transfer during the first execution of the primepump
     *  so guard the execution in an if statement.
     */
    public List<JStatement> beginPrimePumpWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
                
        list.add(transferCommands.zeroOutHead(SchedulingPhase.PRIMEPUMP));
        
        JBlock block = new JBlock();
        if (TileraBackend.DMA) {
            block.addAllStatements(transferCommands.transferCommands(SchedulingPhase.STEADY));


            JIfStatement guard = new JIfStatement(null, new JLogicalComplementExpression(null, 
                    new JEmittedTextExpression(firstExeName)), 
                    block , new JBlock(), null);

            list.add(guard);
        }
        return list;
    }

    public List<JStatement> endPrimePumpWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();


        if (!TileraBackend.DMA) {
            //add the transfer commands for the data that was just computed
            list.addAll(transferCommands.transferCommands(SchedulingPhase.STEADY));
            //generate the rotate statements for this output buffer
            list.addAll(rotateStatementsCurRot());
            //generate the rotate statements for the address buffers
            for (SourceAddressRotation addrRot : addressBuffers.values()) {
                list.addAll(addrRot.rotateStatements());
            }

        } else { //DMA
            //the wait for dma commands, only wait if this is not the first exec
            JBlock block1 = new JBlock();
            block1.addAllStatements(transferCommands.waitCallsSteady());
            JIfStatement guard1 = new JIfStatement(null, new JLogicalComplementExpression(null, 
                    new JEmittedTextExpression(firstExeName)), 
                    block1 , new JBlock(), null);  
            list.add(guard1);


            //generate the rotate statements for this output buffer
            list.addAll(rotateStatementsCurRot());

            JBlock block2 = new JBlock();
            //rotate the transfer buffer only when it is not first
            if (TileraBackend.DMA)
                block2.addAllStatements(rotateStatementsTransRot());
            //generate the rotation statements for the address buffers that this output
            //buffer uses, only do this after first execution
            for (SourceAddressRotation addrRot : addressBuffers.values()) {
                block2.addAllStatements(addrRot.rotateStatements());
            }
            JIfStatement guard2 = new JIfStatement(null, new JLogicalComplementExpression(null, 
                    new JEmittedTextExpression(firstExeName)), 
                    block2, new JBlock(), null);    
            list.add(guard2);

            //now we are done with the first execution to set firstExe to false
            list.add(new JExpressionStatement(
                    new JAssignmentExpression(new JEmittedTextExpression(firstExeName), 
                            new JBooleanLiteral(false))));
        }
        return list;
    }
    
    
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyWrite()
     */
    public List<JStatement> beginSteadyWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(transferCommands.zeroOutHead(SchedulingPhase.STEADY));
        if (TileraBackend.DMA)
            list.addAll(transferCommands.transferCommands(SchedulingPhase.STEADY));
        return list;
    }
    
    /**
     * The rotate statements that includes the current buffer (for output of this 
     * firing) and transfer buffer.
     */
    protected List<JStatement> rotateStatements() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        if (TileraBackend.DMA)
            list.addAll(rotateStatementsTransRot());
        list.addAll(rotateStatementsCurRot());
        return list;
    }
        

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyWrite()
     */
    public List<JStatement> endSteadyWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        
        if (TileraBackend.DMA) 
            list.addAll(transferCommands.waitCallsSteady());
        else
            list.addAll(transferCommands.transferCommands(SchedulingPhase.STEADY));
        
        //generate the rotate statements for this output buffer
        list.addAll(rotateStatements());
        
        //generate the rotation statements for the address buffers that this output
        //buffer uses
        for (SourceAddressRotation addrRot : addressBuffers.values()) {
            list.addAll(addrRot.rotateStatements());
        }
        return list;
    }
    
 
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDecls()
     */
    public List<JStatement> writeDecls() {
        List<JStatement> retval = new LinkedList<JStatement>();
        if (TileraBackend.DMA) {
            JStatement firstDecl = new JVariableDeclarationStatement(firstExe);
            retval.add(firstDecl);
        }
        retval.addAll(transferCommands.decls());
        return retval;
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
        return transferCommands.pushMethod();
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitWrite()
     */
    public List<JStatement> beginInitWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(transferCommands.zeroOutHead(SchedulingPhase.INIT));
        return list;
    }

    protected List<JStatement> rotateStatementsCurRot() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(Util.toStmt(currentWriteRotName + " = " + currentWriteRotName + "->next"));
        list.add(Util.toStmt(currentWriteBufName + " = " + currentWriteRotName + "->buffer"));
        return list;
    }
    
    protected List<JStatement> rotateStatementsTransRot() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(Util.toStmt(transRotName + " = " + transRotName + "->next"));
        list.add(Util.toStmt(transBufName + " = " + transRotName + "->buffer"));
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
        parent.getComputeCode().appendTxtToGlobal(rotType + " *" + writeRotStructName + ";\n");
        //add the declaration of the pointer that points to the current rotation in the rotation structure
        parent.getComputeCode().appendTxtToGlobal(rotType + " *" + currentWriteRotName + ";\n");
        //add the declaration of the pointer that points to the current buffer in the current rotation
        parent.getComputeCode().appendTxtToGlobal(bufType.toString() + " *" + currentWriteBufName + ";\n");

        if (TileraBackend.DMA) {
            //add the declaration of the pointer that points to the transfer rotation in the rotation structure
            parent.getComputeCode().appendTxtToGlobal(rotType + " *" + transRotName + ";\n");
            //add the declaration of the pointer that points to the transfer buffer in the current rotation
            parent.getComputeCode().appendTxtToGlobal(bufType.toString() + " *" + transBufName + ";\n");
        }
        
        JBlock block = new JBlock();
        
        //create a temp var
        if (this.rotationLength > 1)
            block.addStatement(Util.toStmt(rotType + " *" + temp));
        
        //create the first entry!!
        block.addStatement(Util.toStmt(writeRotStructName + " =  (" + rotType+ "*)" + "malloc(sizeof("
                + rotType + "))"));
        
        //modify the first entry
        block.addStatement(Util.toStmt(writeRotStructName + "->buffer = " + bufferNames[0]));
        if (this.rotationLength == 1)  {
            //loop the structure
            block.addStatement(Util.toStmt(writeRotStructName + "->next = " + writeRotStructName));
        }
        else {
            block.addStatement(Util.toStmt(temp + " = (" + rotType+ "*)" + "malloc(sizeof("
                    + rotType + "))"));    
            
            block.addStatement(Util.toStmt(writeRotStructName + "->next = " + 
                    temp));
            
            block.addStatement(Util.toStmt(temp + "->buffer = " + bufferNames[1]));
            
            for (int i = 2; i < this.rotationLength; i++) {
                block.addStatement(Util.toStmt(temp + "->next =  (" + rotType+ "*)" + "malloc(sizeof("
                        + rotType + "))"));
                block.addStatement(Util.toStmt(temp + " = " + temp + "->next"));
                block.addStatement(Util.toStmt(temp + "->buffer = " + bufferNames[i]));
            }
            
            block.addStatement(Util.toStmt(temp + "->next = " + writeRotStructName));
        }
        block.addStatement(Util.toStmt(currentWriteRotName + " = " + writeRotStructName));
        block.addStatement(Util.toStmt(currentWriteBufName + " = " + currentWriteRotName + "->buffer"));
        if (TileraBackend.DMA) {
            block.addStatement(Util.toStmt(transRotName + " = " + writeRotStructName));
            block.addStatement(Util.toStmt(transBufName + " = " + currentWriteRotName + "->buffer"));
        }
        cs.addStatementToBufferInit(block);
    }

}
