package at.dms.kjc.smp;

import at.dms.kjc.slicegraph.*;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.*;

import java.util.*;

/**
 * 
 * 
 * @author mgordon
 *
 */
public class InputRotatingBuffer extends RotatingBuffer {

    /** the name of the read rotation structure, (always points to its head) */
    protected String readRotStructName;
    /** the name of the pointer to the current read rotation of this buffer */
    protected String currentReadRotName;
    /** the name of the pointer to the read buffer of the current rotation */
    protected String currentReadBufName;
	
    /** all the address buffers that are on the cores that feed this input buffer */
    protected SourceAddressRotation[] addressBufs;
    /** a map from source FilterSliceNode to address buf */
    protected HashMap<FilterSliceNode, SourceAddressRotation> addrBufMap;
    
    /** true if what feeds this inputbuffer is a file reader */
    protected boolean upstreamFileReader;
    /** the name of the pointer to the current rotation of this buffer that the file reader should
     * read into*/
    protected String currentFileReaderRotName;
    /** the name of the pointer to the read buffer of the current rotation that the file reader should
     * read into */
    protected String currentFileReaderBufName;

    protected static HashSet<InputRotatingBuffer> fileWriterBuffers;

    static {
        fileWriterBuffers = new HashSet<InputRotatingBuffer>();
    }
    
    /**
     * Create all the input buffers necessary for this slice graph.  Iterate over
     * the steady-state schedule, visiting each slice and creating an input buffer
     * for the filter of the slice.  Also set the rotation lengths based on the 
     * prime pump schedule.
     * 
     * @param schedule The spacetime schedule of the slices 
     */
    public static void createInputBuffers(BasicSpaceTimeSchedule schedule) {
        for (Slice slice : schedule.getScheduleList()) {
            assert slice.getNumFilters() == 1;
            
            if (!slice.getHead().noInputs()) {
                assert slice.getHead().totalWeights(SchedulingPhase.STEADY) > 0;
                Core parent = SMPBackend.backEndBits.getLayout().getComputeNode(slice.getFirstFilter());
                
                //create the new buffer, the constructor will put the buffer in the hashmap
                InputRotatingBuffer buf = new InputRotatingBuffer(slice.getFirstFilter(), parent);

                buf.setRotationLength(schedule);
                buf.setBufferSize();
                buf.createInitCode();
                buf.createAddressBufs();
            }
        }
    }

    
    /**
     * Create a new input buffer that is associated with the filter node.
     * 
     * @param filterNode The filternode for which to create a new input buffer.
     */
    private InputRotatingBuffer(FilterSliceNode filterNode, Core parent) {
        super(filterNode.getEdgeToPrev(), filterNode, parent);
        
        bufType = filterNode.getFilter().getInputType();
        types.add(bufType);
        setInputBuffer(filterNode, this);
        
        readRotStructName =  this.getIdent() + "read_rot_struct";
        currentReadRotName = this.getIdent() + "_read_current";
        currentReadBufName = this.getIdent() + "_read_buf";
        
        currentFileReaderRotName = this.getIdent() + "_fr_current";
        currentFileReaderBufName = this.getIdent() + "_fr_buf";

        //if we have a file reader source for this filter, right now
        //we only support a single input for a filter that is feed by a file
        upstreamFileReader = filterNode.getParent().getHead().hasFileInput();
        if (upstreamFileReader) {
            //System.out.println(filterNode);
            assert filterNode.getParent().getHead().getWidth(SchedulingPhase.INIT) <= 1 &&
            filterNode.getParent().getHead().getWidth(SchedulingPhase.STEADY) <= 1;
        }
        addrBufMap = new HashMap<FilterSliceNode, SourceAddressRotation>();
    }
    
    /**
     * Must be called after setLocalSrcFilter.  This creates the address buffers that other cores
     * use when writing to this input buffer.  Each source that is mapped to a different core than 
     * this input buffer has an address buffer for this input buffer.
     */
    protected void createAddressBufs() {
       int addressBufsSize = filterNode.getParent().getHead().getSourceSlices(SchedulingPhase.STEADY).size();
       addressBufs = new SourceAddressRotation[addressBufsSize];
       
       int i = 0;
       for (Slice src : filterNode.getParent().getHead().getSourceSlices(SchedulingPhase.STEADY)) {
           Core core = SMPBackend.backEndBits.getLayout().getComputeNode(src.getFirstFilter());
           SourceAddressRotation rot = new SourceAddressRotation(core, this, filterNode, theEdge);
           addressBufs[i] = rot;
           addrBufMap.put(src.getFirstFilter(), rot);
           i++;
       }
    }
    
    /**
     * If this input buffer is shared upstream as an output buffer, then 
     * create the commands that the upstream filter will use to transfer items to 
     * its destinations.  
     */
    public void createTransferCommands() {
        transferCommands = new BufferRemoteWritesTransfers(this);
    }
    
    protected void setRotationLength(BasicSpaceTimeSchedule schedule) {
        //now set the rotation length
        int destMult = schedule.getPrimePumpMult(filterNode.getParent());
        //first find the max rotation length given the prime pump 
        //mults of all the sources
        int maxRotationLength = 0;
        
        for (Slice src : filterNode.getParent().getHead().getSourceSlices(SchedulingPhase.STEADY)) {
            int diff = schedule.getPrimePumpMult(src) - destMult; 
            assert diff >= 0;
            if (diff > maxRotationLength) {
                maxRotationLength = diff;
            }
        }
        rotationLength = maxRotationLength + 1;
    }
    
    /**
     * return all the input buffers of the file writers of this application
     */
    public static Set<InputRotatingBuffer> getFileWriterBuffers() {
        return fileWriterBuffers;
    }
    
    /**
     * Generate the code to setup the structure of the rotating buffer 
     * as a circular linked list.
     */
    protected void setupRotation() {
        String temp = "__temp__";
        CoreCodeStore cs; 
        
        //this is the typedef we will use for this buffer rotation structure
        String rotType = rotTypeDefPrefix + getType().toString();
        
        //if we are setting up the rotation for a file writer we have to do it on the allocating core
        if (filterNode.isFileOutput()) {
            fileWriterBuffers.add(this);
            cs = ProcessFileWriter.getAllocatingCore(filterNode).getComputeCode();
        } else {
            cs = parent.getComputeCode();
        }
        
        JBlock block = new JBlock();
                
        //add the declaration of the rotation buffer of the appropriate rotation type
        cs.appendTxtToGlobal(rotType + " *" + readRotStructName + ";\n");
        //add the declaration of the pointer that points to the current rotation in the rotation structure
        cs.appendTxtToGlobal(rotType + " *" + currentReadRotName + ";\n");
        //add the declaration of the pointer that points to the current buffer in the current rotation
        cs.appendTxtToGlobal(bufType.toString() + " *" + currentReadBufName + ";\n");

        if (upstreamFileReader) {
            //add the declaration of the pointer that points to current in the rotation structure that the file
            //reader should write into
            parent.getComputeCode().appendTxtToGlobal(rotType + " *" + currentFileReaderRotName + ";\n");
            //add the declaration of the pointer that points to the current buffer in the current rotation that
            //the file reader should write into
            parent.getComputeCode().appendTxtToGlobal(bufType.toString() + " *" + currentFileReaderBufName + ";\n");
        }

        //create a temp var
        if (this.rotationLength > 1)
            block.addStatement(Util.toStmt(rotType + " *" + temp));
        
        //create the first entry!!
        block.addStatement(Util.toStmt(readRotStructName + " =  (" + rotType+ "*)" + "malloc(sizeof("
                + rotType + "))"));
        
        //modify the first entry
        block.addStatement(Util.toStmt(readRotStructName + "->buffer = " + bufferNames[0]));
        if (this.rotationLength == 1) 
            block.addStatement(Util.toStmt(readRotStructName + "->next = " + readRotStructName));
        else {
            block.addStatement(Util.toStmt(temp + " = (" + rotType+ "*)" + "malloc(sizeof("
                    + rotType + "))"));    
            
            block.addStatement(Util.toStmt(readRotStructName + "->next = " + 
                    temp));
            
            block.addStatement(Util.toStmt(temp + "->buffer = " + bufferNames[1]));
            
            for (int i = 2; i < this.rotationLength; i++) {
                block.addStatement(Util.toStmt(temp + "->next =  (" + rotType+ "*)" + "malloc(sizeof("
                        + rotType + "))"));
                block.addStatement(Util.toStmt(temp + " = " + temp + "->next"));
                block.addStatement(Util.toStmt(temp + "->buffer = " + bufferNames[i]));
            }
            
            block.addStatement(Util.toStmt(temp + "->next = " + readRotStructName));
        }
        block.addStatement(Util.toStmt(currentReadRotName + " = " + readRotStructName));
        block.addStatement(Util.toStmt(currentReadBufName + " = " + currentReadRotName + "->buffer"));
        if (upstreamFileReader) {
            block.addStatement(Util.toStmt(currentFileReaderRotName + " = " + readRotStructName));
            block.addStatement(Util.toStmt(currentFileReaderBufName + " = " + currentReadRotName + "->buffer"));
        }
        
        cs.addStatementToBufferInit(block);
    }
    
    /**
     * Return the set of address buffers that are declared on cores that feed this buffer.
     * @return the set of address buffers that are declared on cores that feed this buffer.
     */
    public SourceAddressRotation[] getAddressBuffers() {
        return addressBufs;
    }
    
    /**
     * Return the address buffer rotation for this input buffer, to be used by a 
     * source FilterSliceNode
     * 
     * @param filterSliceNode The FilterSliceNode
     * @return the address buffer for this input buffer on the core
     */
    public SourceAddressRotation getAddressRotation(FilterSliceNode filterSliceNode) {
        return addrBufMap.get(filterSliceNode);
    }
    
    /**
     * Set the buffer size of this input buffer based on the max
     * number of items it receives.
     */
    protected void setBufferSize() {
        bufSize = Math.max(filterInfo.totalItemsReceived(SchedulingPhase.INIT),
        		(filterInfo.totalItemsReceived(SchedulingPhase.STEADY) + filterInfo.copyDown));
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popMethodName()
     */
    public String popMethodName() {
        return "__pop_" + unique_id;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popMethod()
     */
    public JMethodDeclaration popMethod() {
    	return transferCommands.popMethod();
    }
    
    /** Create an array reference given an offset */   
    public JFieldAccessExpression writeBufRef() {
    	assert(false);
    	return null;
    }
    
    /** Create an array reference given an offset */   
    public JArrayAccessExpression readBufRef(JExpression offset) {
        JFieldAccessExpression bufAccess = new JFieldAccessExpression(new JThisExpression(), currentReadBufName);
        return new JArrayAccessExpression(bufAccess, offset);
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popManyMethodName()
     */
    public String popManyMethodName() {
        return "__popN_" + unique_id;
    }
 
    JMethodDeclaration popManyCode = null;
    
    /**
     * Pop many items at once ignoring them.
     * Default method generated here to call popMethod() repeatedly.
     */
    public JMethodDeclaration popManyMethod() {
        if (popManyCode != null) {
            return popManyCode;
        }
        if (popMethod() == null) {
            return null;
        }
        
        String formalParamName = "n";
        CType formalParamType = CStdType.Integer;
        
        JVariableDefinition nPopsDef = new JVariableDefinition(formalParamType, formalParamName);
        JExpression nPops = new JLocalVariableExpression(nPopsDef);
        
        JVariableDefinition loopIndex = new JVariableDefinition(formalParamType, "i");
        
        JStatement popOne = new JExpressionStatement(
                new JMethodCallExpression(popMethodName(),new JExpression[0]));
        
        JBlock body = new JBlock();
        body.addStatement(Utils.makeForLoop(popOne, nPops, loopIndex));
        
        popManyCode = new JMethodDeclaration(CStdType.Void,
                popManyMethodName(),
                new JFormalParameter[]{new JFormalParameter(formalParamType, formalParamName)},
                body);
        return popManyCode;
     }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPopMethodName()
     */
    public String assignFromPopMethodName() {
        return "__popv_" + unique_id;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPopMethod()
     */
    public JMethodDeclaration assignFromPopMethod() {
        String parameterName = "__val";
        JFormalParameter val = new JFormalParameter(
                CStdType.Integer,
                parameterName);
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                assignFromPopMethodName(),
                new JFormalParameter[]{val},
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
                new JExpressionStatement(
                        new JEmittedTextExpression(
                                "/* assignFromPopMethod not yet implemented */")));
        return retval;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#peekMethodName()
     */
    public String peekMethodName() {
        return "__peek_" + unique_id;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#peekMethod()
     */
    public JMethodDeclaration peekMethod() {
    	return transferCommands.peekMethod();
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPeekMethodName()
     */
    public String assignFromPeekMethodName() {
        return "__peekv_" + unique_id;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPeekMethod()
     */
    public JMethodDeclaration assignFromPeekMethod() {
        String valName = "__val";
        JFormalParameter val = new JFormalParameter(
                CStdType.Integer,
                valName);
        String offsetName = "__offset";
        JFormalParameter offset = new JFormalParameter(
                CStdType.Integer,
                offsetName);
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                assignFromPeekMethodName(),
                new JFormalParameter[]{val,offset},
                CClassType.EMPTY,
                body, null, null);
         body.addStatement(
                new JExpressionStatement(
                        new JEmittedTextExpression(
                                "/* assignFromPeekMethod not yet implemented */")));
        return retval;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitRead()
     */
    public List<JStatement> beginInitRead() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(transferCommands.zeroOutTail(SchedulingPhase.INIT));
        return list;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitRead()
     */
    public List<JStatement> postPreworkInitRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitRead()
     */
    public List<JStatement> endInitRead() {
        LinkedList<JStatement> list = new LinkedList<JStatement>(); 
        list.addAll(transferCommands.readTransferCommands(SchedulingPhase.INIT));
        return list;
        //copyDownStatements(SchedulingPhase.INIT));
    }

    public List<JStatement> beginPrimePumpRead() {
    	return beginSteadyRead();
    }
    
    public List<JStatement> endPrimePumpRead() {
    	return endSteadyRead();
    }
    
    public List<JStatement> beginSteadyRead() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(transferCommands.zeroOutTail(SchedulingPhase.STEADY));
        return list;
    }
    
    public List<JStatement> endSteadyRead() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        //copy the copyDown items to the next rotation buffer
        list.addAll(transferCommands.readTransferCommands(SchedulingPhase.STEADY));
        //rotate to the next buffer
        list.addAll(rotateStatementsRead());
        return list;
        //copyDownStatements(SchedulingPhase.STEADY));
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#topOfWorkSteadyRead()
     */
    public List<JStatement> topOfWorkSteadyRead() {
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
        //declare the buffer array
        List<JStatement> retval = new LinkedList<JStatement>();
        return retval;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#readDeclsExtern()
     */
    public List<JStatement> readDeclsExtern() {
        return new LinkedList<JStatement>();
    }   
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#readDecls()
     */
    public List<JStatement> readDecls() {
    	List<JStatement> retval = new LinkedList<JStatement>();
    	retval.addAll(transferCommands.readDecls());
    	return retval;
    	/*
        //declare the tail    
        JStatement tailDecl = new JVariableDeclarationStatement(tailDefn);
        List<JStatement> retval = new LinkedList<JStatement>();
        retval.add(tailDecl);
        return retval;
        */
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitWrite()
     */
    public List<JStatement> endInitWrite() {
    	assert(false);
    	return null;
    }
    
    public List<JStatement> beginPrimePumpWrite() {
    	assert(false);
    	return null;
    }
    
    public List<JStatement> endPrimePumpWrite() {
    	assert(false);
    	return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyWrite()
     */
    public List<JStatement> beginSteadyWrite() {
    	assert(false);
    	return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyWrite()
     */
    public List<JStatement> endSteadyWrite() {
    	assert(false);
    	return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDecls()
     */
    public List<JStatement> writeDecls() {
    	assert(false);
    	return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethodName()
     */
    public String pushMethodName() {
    	assert(false);
    	return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethod()
     */
    public JMethodDeclaration pushMethod() {
    	assert(false);
    	return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitWrite()
     */
    public List<JStatement> beginInitWrite() {
    	assert(false);
    	return null;
    }
    
    protected List<JStatement> rotateStatementsWrite() {
    	assert(false);
    	return null;
    }

    protected List<JStatement> rotateStatementsRead() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(Util.toStmt(currentReadRotName + " = " + currentReadRotName + "->next"));
        list.add(Util.toStmt(currentReadBufName + " = " + currentReadRotName + "->buffer"));
        return list;
    }
}
