package at.dms.kjc.smp;

import at.dms.kjc.slicegraph.*;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.*;
import at.dms.kjc.smp.arrayassignment.*;

import java.util.*;

/**
 * 
 * 
 * @author mgordon
 *
 */
public class InputRotatingBuffer extends RotatingBuffer {
    /** if this input buffer is shared as an upstream output buffer for a filter on
     * the same tile, then this references the upstream filter.
     */
    protected FilterSliceNode localSrcFilter;    

    /** all the address buffers that are on the tiles that feed this input buffer */
    protected SourceAddressRotation[] addressBufs;
    /** a map from tile to address buf */
    protected HashMap<Core, SourceAddressRotation> addrBufMap;
    
    /** true if what feeds this inputbuffer is a file reader */
    protected boolean upstreamFileReader;
    /** the name of the pointer to the current rotation of this buffer that the file reader should
     * read into*/
    protected String currentFileReaderRotName;
    /** the name of the pointer to the read buffer of the current rotation that the file reader should
     * read into */
    protected String currentFileReaderBufName;
    /** reference to head if this input buffer is shared as an output buffer */
    //protected JExpression head;
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
                //create the new buffer, the constructor will put the buffer in the 
                //hashmap
                InputRotatingBuffer buf = new InputRotatingBuffer(slice.getFirstFilter(), parent);
                                  
                buf.setRotationLength(schedule);
                buf.createInitCode(true);
                buf.createAddressBufs();
                //System.out.println("Setting input buf " + buf.getFilterNode() + " to " + buf.rotationLength);
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

        int coreNum = parent.getCoreID();
        if (filterNode.isFileOutput())
            coreNum = ProcessFileWriter.getAllocatingTile(filterNode).getCoreID();
        
        readRotStructName =  this.getIdent() + "read_rot_struct__n" + coreNum;
        currentReadRotName = this.getIdent() + "_read_current__n" + coreNum;
        currentReadBufName = this.getIdent() + "_read_buf__n" + coreNum;
        currentFileReaderRotName = this.getIdent() + "_fr_current__n" + coreNum;
        currentFileReaderBufName = this.getIdent() + "_fr_buf__n" + coreNum;

        //if we have a file reader source for this filter, right now
        //we only support a single input for a filter that is feed by a file
        upstreamFileReader = filterNode.getParent().getHead().hasFileInput();
        if (upstreamFileReader) {
            //System.out.println(filterNode);
            assert filterNode.getParent().getHead().getWidth(SchedulingPhase.INIT) <= 1 &&
            filterNode.getParent().getHead().getWidth(SchedulingPhase.STEADY) <= 1;
        }
        addrBufMap = new HashMap<Core, SourceAddressRotation>();
        localSrcFilter = null;
        
        setLocalSrcFilter();
        assert !(upstreamFileReader && hasLocalSrcFilter());
        setBufferSize();
        
    }
    
    public FilterSliceNode getLocalSrcFilter() {
        return localSrcFilter;
    }
    
    /**
     * This is a potential optimization.  If we have a source and dest mapped to the same
     * tile, and we have a simple reorganization scheme between them, we can write directly into the
     * downstream's input buffer.  No need for an output buffer at the source and then copying.
     * 
     * So this will set the localSrcFilter to a filter if we can apply the optimization.
     */
    protected void setLocalSrcFilter() {
       
        //if we find a candidate this will be it
        FilterSliceNode lsf = null;
        //this is the edge between the 2 local filters
        InterSliceEdge theEdge = null;
        
        for (InterSliceEdge edge : filterNode.getParent().getHead().getSourceSet(SchedulingPhase.STEADY)) {
            FilterSliceNode upstream = edge.getSrc().getPrevFilter();
            if (SMPBackend.backEndBits.getLayout().getComputeNode(upstream) == parent) {
                //System.out.println(upstream);
                assert lsf == null : "Two upstream srcs mapped to same tile ?";
                lsf = upstream;
                theEdge = edge;
            } 
         }
        
        if (lsf == null)
            return;
        
        //now we have to do some checks on the re-organization between them.
        //make sure the output node only has one output (or none in init)
        OutputSliceNode output = lsf.getParent().getTail();
        if (!(output.peekingFissionPattern(SchedulingPhase.STEADY) && 
                output.peekingFissionPattern(SchedulingPhase.INIT))) {
            //System.out.println(filterNode + " has no good local source 1.");
            return;
        }

        //now make sure the downstream consumer has a simple single appearance joiner and that
        //the lsf is the first in that joiner so nothing has to be redistributed...
        //or it is single appearance and all the upstream outputs fit in the slot in the joiner
        InputSliceNode input = filterNode.getParent().getHead();
        if (!input.singleAppearance())
            return;

        if (!(input.getWeight(theEdge, SchedulingPhase.INIT) == output.totalWeights(SchedulingPhase.INIT) &&
                input.getWeight(theEdge, SchedulingPhase.STEADY) == output.totalWeights(SchedulingPhase.STEADY)))
        {
            //System.out.println(filterNode + " has no good local source 2.");
            return;
        }

        FilterInfo consumer = FilterInfo.getFilterInfo(input.getNextFilter());
        FilterInfo producer = FilterInfo.getFilterInfo(output.getPrevFilter());
        
        //check that the 
        if ((input.getWidth(SchedulingPhase.INIT) > 1 && 
                input.totalWeights(SchedulingPhase.INIT) != consumer.totalItemsPopped(SchedulingPhase.INIT)) ||
            (input.getWidth(SchedulingPhase.STEADY) > 1 &&       
                        input.totalWeights(SchedulingPhase.STEADY) != consumer.totalItemsPopped(SchedulingPhase.STEADY)) ||
            (output.getWidth(SchedulingPhase.INIT) > 1 &&   
                output.totalWeights(SchedulingPhase.INIT) != producer.totalItemsSent(SchedulingPhase.INIT)) ||
            (output.getWidth(SchedulingPhase.STEADY) > 1 &&   
                output.totalWeights(SchedulingPhase.STEADY) != producer.totalItemsSent(SchedulingPhase.STEADY)))
        {
            return;
        }

        //if we get here, everything checked out
        localSrcFilter = lsf;        
        
        //System.out.println(filterNode + " has local source " + localSrcFilter);
        
        //if we found an upstream filter mapped to the same tile
        if (localSrcFilter != null) {
          //remember that this input buffer is the output for the src filter on the same tile
            setOutputBuffer(localSrcFilter, this);
            
            firstExeName = "__first__" + this.getIdent();        
            firstExe = new JVariableDefinition(null,
                    at.dms.kjc.Constants.ACC_STATIC,
                    CStdType.Boolean, firstExeName, new JBooleanLiteral(true));
        }
    }
    
    /**
     * Must be called after setLocalSrcFilter.  This creates the address buffers that other tiles
     * use when writing to this input buffer.  Each source that is mapped to a different tile than 
     * this input buffer has an address buffer for this input buffer.
     */
    protected void createAddressBufs() {
       int addressBufsSize = filterNode.getParent().getHead().getSourceSlices(SchedulingPhase.STEADY).size();
       //if we are using this input buffer as an output buffer, then we don't need the address buffer
       //for the output buffer that is used for the upstream filter that is mapped to this tile
       if (hasLocalSrcFilter())
           addressBufsSize--;           
       
       addressBufs = new SourceAddressRotation[addressBufsSize];
       
       int i = 0;
       for (Slice src : filterNode.getParent().getHead().getSourceSlices(SchedulingPhase.STEADY)) {
           Core tile = SMPBackend.backEndBits.getLayout().getComputeNode(src.getFirstFilter());
           if (tile == parent && hasLocalSrcFilter())
               continue;
           
           SourceAddressRotation rot = new SourceAddressRotation(tile, this, filterNode, theEdge);
           addressBufs[i] = rot;
           addrBufMap.put(tile, rot);
           i++;
       }
    }
    
    /**
     * If this input buffer is shared upstream as an output buffer, then 
     * create the commands that the upstream filter will use to transfer items to 
     * its destinations.  
     */
    public void createTransferCommands() {
        //if (!hasLocalSrcFilter())
        //    return;
        
        transferCommands = new BufferRemoteWritesTransfers(this);
    }
    
    /**
     * If we are using this input buffer as a shared buffer it is also an output buffer.  
     * Thus it needs the address buffers of any destination for the upstream filter that
     * uses the input buffer as an output buffer.
     */
    public void createAddressBuffers() {
        //do nothing for input buffers that don't act as output buffers
        if (!hasLocalSrcFilter())
            return;
        
        //fill the addressbuffers array
        addressBuffers = new HashMap<InputRotatingBuffer, SourceAddressRotation>();
        
        OutputSliceNode outputNode = localSrcFilter.getParent().getTail();
        
        for (InterSliceEdge edge : outputNode.getDestSet(SchedulingPhase.STEADY)) {
            if (edge.getDest() == this.filterNode.getParent().getHead())
                continue;
            
            InputRotatingBuffer input = InputRotatingBuffer.getInputBuffer(edge.getDest().getNextFilter());
            addressBuffers.put(input, input.getAddressRotation(parent));               
        }
    }
    
    /**
     * 
     * @param schedule
     */
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
        //if we are setting up the rotation for a file writer we have to do it on the 
        //allocating tile
        if (filterNode.isFileOutput()) {
            fileWriterBuffers.add(this);
            cs = ProcessFileWriter.getAllocatingTile(filterNode).getComputeCode();
        } else
            cs = parent.getComputeCode();
        
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
        block.addStatement(endOfRotationSetup());
        
        if (hasLocalSrcFilter()) {
            //if this has a local upstream filter, then we can set up the rotation struct for its 
            //output
            //add the declaration of the rotation buffer of the appropriate rotation type
            parent.getComputeCode().appendTxtToGlobal(rotType + " *" + writeRotStructName + ";\n");
            //add the declaration of the pointer that points to the current rotation in the rotation structure
            parent.getComputeCode().appendTxtToGlobal(rotType + " *" + currentWriteRotName + ";\n");
            //add the declaration of the pointer that points to the current buffer in the current rotation
            parent.getComputeCode().appendTxtToGlobal(bufType.toString() + " *" + currentWriteBufName + ";\n");

            
            //create the first entry!!
            block.addStatement(Util.toStmt(writeRotStructName + " =  (" + rotType+ "*)" + "malloc(sizeof("
                    + rotType + "))"));
            
            //modify the first entry
            block.addStatement(Util.toStmt(writeRotStructName + "->buffer = " + bufferNames[0]));
            if (this.rotationLength == 1) 
                block.addStatement(Util.toStmt(writeRotStructName + "->next = " + writeRotStructName));
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
        }
        
        cs.addStatementToBufferInit(block);
    }
    
    /**
     * Return the set of address buffers that are declared on tiles that feed this buffer.
     * @return the set of address buffers that are declared on tiles that feed this buffer.
     */
    public SourceAddressRotation[] getAddressBuffers() {
        return addressBufs;
    }
    
    /**
     * Return the address buffer rotation for this input buffer on the tile.
     * 
     * @param tile The tile
     * @return the address buffer for this input buffer on the tile
     */
    public SourceAddressRotation getAddressRotation(Core tile) {
        return addrBufMap.get(tile);
    }
    
    /**
     * return true if this input rotating buffer has a source mapped to the same
     * tile, if so they output for that source uses this buffer as an optimization.
     */
    public boolean hasLocalSrcFilter() {
        return localSrcFilter != null;
    }
    
    /**
     * Set the buffer size of this input buffer based on the max
     * number of items it receives.
     */
    protected void setBufferSize() {
       
        bufSize = Math.max(filterInfo.totalItemsReceived(SchedulingPhase.INIT),
                (filterInfo.totalItemsReceived(SchedulingPhase.STEADY) + filterInfo.copyDown));
        
        if (hasLocalSrcFilter()) {
            //if this filter has a local source filter, then we are using this buffer
            //for its output also, so we have to consider the amount of output in the 
            //buffer calculation
            FilterInfo srcInfo = FilterInfo.getFilterInfo(localSrcFilter);
            int output = Math.max(srcInfo.totalItemsSent(SchedulingPhase.STEADY) + filterInfo.copyDown, 
                    srcInfo.totalItemsSent(SchedulingPhase.INIT) + filterInfo.copyDown);
            bufSize = Math.max(bufSize, output);
        }
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
        assert hasLocalSrcFilter();
        return new JFieldAccessExpression(new JThisExpression(), currentWriteBufName);
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
        JLocalVariableExpression valRef = new JLocalVariableExpression(val);
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
        JLocalVariableExpression valRef = new JLocalVariableExpression(val);
        String offsetName = "__offset";
        JFormalParameter offset = new JFormalParameter(
                CStdType.Integer,
                offsetName);
        JLocalVariableExpression offsetRef = new JLocalVariableExpression(offset);
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
        assert hasLocalSrcFilter() : "Calling write method for input buffer that does not act as output buffer.";
        
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        //in the init stage for dma, we need to dma to send the output to the dest filter
        //but we have to wait until the end because are not double buffering
        //also, don't rotate anything here
        list.addAll(transferCommands.writeTransferCommands(SchedulingPhase.INIT));
        return list;
    }
    
    /**
     *  
     *       
     */
    public List<JStatement> beginPrimePumpWrite() {
        assert hasLocalSrcFilter() : "Calling write method for input buffer that does not act as output buffer.";
        
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(transferCommands.zeroOutHead(SchedulingPhase.PRIMEPUMP));
        return list;
    }
    
    public List<JStatement> endPrimePumpWrite() {
        assert hasLocalSrcFilter() : "Calling write method for input buffer that does not act as output buffer.";
        
        LinkedList<JStatement> list = new LinkedList<JStatement>();

        //add the transfer commands for the data that was just computed
        list.addAll(transferCommands.writeTransferCommands(SchedulingPhase.STEADY));
        //generate the rotate statements for this output buffer
        list.addAll(rotateStatementsWrite());
        //generate the rotate statements for the address buffers
        for (SourceAddressRotation addrRot : addressBuffers.values())
            list.addAll(addrRot.rotateStatements());

        return list;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyWrite()
     */
    public List<JStatement> beginSteadyWrite() {
        assert hasLocalSrcFilter() : "Calling write method for input buffer that does not act as output buffer.";
        
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(transferCommands.zeroOutHead(SchedulingPhase.STEADY));
        return list;
    }
    

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyWrite()
     */
    public List<JStatement> endSteadyWrite() {
        assert hasLocalSrcFilter() : "Calling write method for input buffer that does not act as output buffer.";
        
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        
        //add the transfer commands for the data that was just computed
        list.addAll(transferCommands.writeTransferCommands(SchedulingPhase.STEADY));
        //generate the rotate statements for this output buffer
        list.addAll(rotateStatementsWrite());
        //generate the rotation statements for the address buffers that this output
        //buffer uses
        for (SourceAddressRotation addrRot : addressBuffers.values())
            list.addAll(addrRot.rotateStatements());
        
        return list;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDecls()
     */
    public List<JStatement> writeDecls() {
        assert hasLocalSrcFilter() : "Calling write method for input buffer that does not act as output buffer.";
        List<JStatement> retval = new LinkedList<JStatement>();
        retval.addAll(transferCommands.writeDecls());
        return retval;
    }   
    
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethodName()
     */
    public String pushMethodName() {
        assert hasLocalSrcFilter() : "Calling write method for input buffer that does not act as output buffer.";
        return "__push_" + unique_id;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethod()
     */
    public JMethodDeclaration pushMethod() {
        assert hasLocalSrcFilter() : "Calling write method for input buffer that does not act as output buffer.";
        return transferCommands.pushMethod();
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitWrite()
     */
    public List<JStatement> beginInitWrite() {
        assert hasLocalSrcFilter() : "Calling write method for input buffer that does not act as output buffer.";
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        if (FilterInfo.getFilterInfo(localSrcFilter).totalItemsSent(SchedulingPhase.INIT) > 0)
            list.add(transferCommands.zeroOutHead(SchedulingPhase.INIT));
        return list;
    }
    
    protected List<JStatement> rotateStatementsWrite() {
        assert hasLocalSrcFilter() : "Calling write method for input buffer that does not act as output buffer.";
        
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.addAll(rotateStatementsCurRot());

        return list;
    }

    protected List<JStatement> rotateStatementsRead() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(Util.toStmt(currentReadRotName + " = " + currentReadRotName + "->next"));
        list.add(Util.toStmt(currentReadBufName + " = " + currentReadRotName + "->buffer"));
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
}
