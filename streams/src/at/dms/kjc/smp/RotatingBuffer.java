package at.dms.kjc.smp;

import at.dms.kjc.CType;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;

import java.util.LinkedList;
import at.dms.kjc.spacetime.*;
import at.dms.kjc.*;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;

/**
 * A rotating buffer represents a block of memory that a filter reads from or writes to that
 * is rotated because we are double buffering.  This class generates code that implements initialization
 * of all the buffers for the application including allocation, setting up the rotation structure, and
 * communicating shared addresses.
 * 
 * Note the we are not using extraCount of Channel for double buffering accounting,
 * instead we are using rotationLength.
 * 
 * @author mgordon
 *
 */
public abstract class RotatingBuffer extends Channel {

    /** the core this buffer is mapped to */
    protected Core parent;
	/** the filter this buffer is associated with */   
    protected FilterSliceNode filterNode;
    /** the filter info object for the filter that contains this buffer */
    protected FilterInfo filterInfo;
    
    /** the names of the individual buffers */
    protected String[] bufferNames;
	/** array size in elements of each buffer of the rotation*/
    protected int bufSize;
    /** type of array: array of element type */
    protected CType bufType;
    
    /** the data transfer statements that are generated for this output buffer */
    protected BufferTransfers transferCommands;
    
    /** a set of all the buffer types in the application */
    protected static HashSet<CType> types;
    /** prefix of the variable name for the rotating buffers */
    public static String rotTypeDefPrefix = "__rotating_buffer_";
	
    /** maps each FilterSliceNode to Input/OutputRotatingBuffers */
    protected static HashMap<FilterSliceNode, InputRotatingBuffer> inputBuffers;
    protected static HashMap<FilterSliceNode, RotatingBuffer> outputBuffers;

    static {
        types = new HashSet<CType>();
        inputBuffers = new HashMap<FilterSliceNode, InputRotatingBuffer>();
        outputBuffers = new HashMap<FilterSliceNode, RotatingBuffer>();
    }
    
    protected RotatingBuffer(Edge edge, FilterSliceNode fsn, Core parent) {
        super(edge);
        
        this.parent = parent;
        filterNode = fsn;
        filterInfo = FilterInfo.getFilterInfo(fsn);
    }
   
    /**
     * Create all the input and output buffers necessary for the slice graph.
     * Each filter that produces output will have an output buffer and each 
     * filter that expects input will have an input buffer.
     * 
     * This call also creates code for allocating the rotating buffers and 
     * communicating the addresses of shared buffers.
     * 
     * @param schedule  The spacetime schedule of the application
     */
    public static void createBuffers(BasicSpaceTimeSchedule schedule) {
        //have to create input buffers first because when we have a lack of a 
        //shared input buffer, we create an output buffer
        InputRotatingBuffer.createInputBuffers(schedule);
        OutputRotatingBuffer.createOutputBuffers(schedule);
        
        //now that all the buffers are created, create the pointers to them
        //that live on other cores, and create the transfer commands
        for (InputRotatingBuffer buf : inputBuffers.values()) {
            buf.createAddressBuffers();
            buf.createTransferCommands();
        }
        for (RotatingBuffer buf : outputBuffers.values()) {
            if (buf instanceof InputRotatingBuffer)
                continue;
            buf.createAddressBuffers();
            buf.createTransferCommands();
        }
        
        //now add the typedefs needed for the rotating buffers to structs.h
        rotTypeDefs();
        //now that all the buffers are allocated, we create a barrier on all the cores
        //so that we wait for all the shared memory to be allocated
        CoreCodeStore.addBufferInitBarrier();
        //generate the code for the address communication stage
        communicateAddresses();
    }
    
    public void createAddressBuffers() {
        
    }
    
    public void createTransferCommands() {
        transferCommands = new BufferRemoteWritesTransfers(this);
    }
    
    /**
     * Generate the code necessary to communicate the addresses of the shared input buffers 
     * of all input rotational structures to the sources that will write to the buffer
     */
    protected static void communicateAddresses() {
        //handle all the filters that are mapped to compute cores
    	//this will handle all filters except file writers and file readers
        for (Core ownerCore : SMPBackend.chip.getCores()) {
            CoreCodeStore cs = ownerCore.getComputeCode();
            
            for (FilterSliceNode filter : cs.getFilters())
                communicateAddressesForFilter(filter, ownerCore);
        }
        
        //now handle the file writers
        for (FilterSliceNode fileWriter : ProcessFileWriter.getFileWriterFilters())
            communicateAddressesForFilter(fileWriter, ProcessFileWriter.getAllocatingCore(fileWriter));
        
        //now handle the file readers
        
    }
        
    private static void communicateAddressesForFilter(FilterSliceNode filter, Core ownerCore) { 
        InputRotatingBuffer buf = InputRotatingBuffer.getInputBuffer(filter);
        
        //if this filter does not have an input buffer, then continue
        if (buf == null)
            return;
        
        for (SourceAddressRotation addr : buf.getAddressBuffers()) {
            Core srcCore = addr.parent;
            
            //we might have a file reader as a source, if so, don't send the addresses to it
            if (!srcCore.isComputeNode())
                continue;

            //create declarations of the pointers to shared buffers on the source core
            addr.declareBuffers();
            
            //communicate addresses of shared buffers
            for (int b = 0; b < buf.rotationLength; b++)
                srcCore.getComputeCode().addStatementToBufferInit(
                        addr.bufferNames[b] + " = " + buf.bufferNames[b]);
            
            //setup the rotation structure at the source
            addr.setupRotation();
        }
    }

    /**
     * Create the typedef for the rotating buffer structure, one for each type 
     * we see in the program (each channel type).
     */
    protected static void rotTypeDefs() {
        for (CType type : types) {
            SMPBackend.structs_h.addLineSC("typedef struct __rotating_struct_" +
                    type.toString() + "__" + 
                    " *__rot_ptr_" + type.toString() + "__");
            SMPBackend.structs_h.addText("typedef struct __rotating_struct_" + type.toString() + "__ {\n");
            SMPBackend.structs_h.addText("\t" + type.toString() + " *buffer;\n");
            SMPBackend.structs_h.addText("\t__rot_ptr_" + type.toString() + "__ next;\n");
            SMPBackend.structs_h.addText("} " + rotTypeDefPrefix + type.toString() + ";\n");
        }
    }
    
    /**
     * Generate the code necessary to allocate the buffers, setup the rotation structure,
     * and communicate addresses.
     * 
     * @param input true if this is an input buffer
     */
    protected void createInitCode() {
        this.setBufferNames();
        this.allocBuffers();
        this.setupRotation();
    }
    
    /**
     * Allocate the constituent buffers of this rotating buffer structure
     */
    protected void allocBuffers() {
    	for (int i = 0; i < rotationLength; i++) {
    		CoreCodeStore cs;

    		//if we have a file writer then the code has to be put on the allocating core not the off chip memory!
    		//else we are dealing with a regular buffer on a core, put on parent core
    		if (filterNode.isFileOutput())
    			cs = ProcessFileWriter.getAllocatingCore(filterNode).getComputeCode();
    		else
    			cs = this.parent.getComputeCode();

    		this.parent.getMachine().getOffChipMemory().getComputeCode().appendTxtToGlobal(
    				"extern " + this.getType().toString() + " " + bufferNames[i] + "[" + 
                    this.getBufferSize() + "];\n");

            cs.appendTxtToGlobal(this.getType().toString() + " " + bufferNames[i] + "[" +
                                 this.getBufferSize() + "];\n");

            /*
    		//create pointers to constituent buffers
    		this.parent.getMachine().getOffChipMemory().getComputeCode().appendTxtToGlobal(
    				"extern " + this.getType().toString() + "* " + bufferNames[i] + ";\n");

            cs.appendTxtToGlobal(this.getType().toString() + "* " + bufferNames[i] + ";\n");

    		//malloc the steady buffer
    		cs.addStatementToBufferInit(new JExpressionStatement(new JEmittedTextExpression(
    				bufferNames[i] + " = (" + this.getType() + 
    				"*) malloc(" +  
    				this.getBufferSize() + " * sizeof(" +
    				this.getType() + "))")));
            */
        }
    }
      
    /**
     * Generate the code to setup the structure of the rotating buffer 
     * as a circular linked list.
     */
    protected abstract void setupRotation();
    
    /**
     * Return the number of buffers that comprise this rotating buffer.
     * @return the number of buffers that comprise this rotating buffer.
     */
    public int getRotationLength() {
        return rotationLength;
    }
    
    /**
     * Return the number of elements for each rotation of this buffer
     * 
     * @return the maximum size for this buffer for one rotation
     */
    public int getBufferSize() {
        return bufSize;
    }
    
    protected abstract void setBufferSize();
    
    /**
     * Set the names of the buffers that comprise this rotating buffer.
     */
    protected void setBufferNames() {
        bufferNames = new String[rotationLength];
        for (int i = 0; i < rotationLength; i++) {
            bufferNames[i] = this.getIdent() + "_Buf_" + i;
        }
    }
    
    public static void setOutputBuffer(FilterSliceNode node, RotatingBuffer buf) {
        outputBuffers.put(node, buf);
    }
    
    public static void setInputBuffer(FilterSliceNode node, InputRotatingBuffer buf) {
        inputBuffers.put(node, buf);
    }
    
    /** Create an array reference given an offset */   
    public abstract JFieldAccessExpression writeBufRef();
    public abstract JArrayAccessExpression readBufRef(JExpression offset);
    
    /**
     * Return the input buffer associated with the filter node.
     * 
     * @param fsn The filter node in question.
     * @return The input buffer of the filter node.
     */
    public static InputRotatingBuffer getInputBuffer(FilterSliceNode fsn) {
        return inputBuffers.get(fsn);
    }
  
    public static RotatingBuffer getOutputBuffer(FilterSliceNode fsn) {
        return outputBuffers.get(fsn);
    }
    
    /**
     * Return the set of all the InputBuffers that are mapped to Core t.
     */
    public static Set<InputRotatingBuffer> getInputBuffersOnCore(Core t) {
        HashSet<InputRotatingBuffer> set = new HashSet<InputRotatingBuffer>();
        
        for (InputRotatingBuffer b : inputBuffers.values()) {
            if (SMPBackend.backEndBits.getLayout().getComputeNode(b.getFilterNode()).equals(t))
                set.add(b);
        }
        
        return set;
    }
    /**
     * Return the set of all the InputBuffers that are mapped to Core t.
     */
    public static Set<RotatingBuffer> getOutputBuffersOnCore(Core t) {
        HashSet<RotatingBuffer> set = new HashSet<RotatingBuffer>();
        
        for (RotatingBuffer b : outputBuffers.values()) {
            if (SMPBackend.backEndBits.getLayout().getComputeNode(b.getFilterNode()).equals(t))
                set.add(b);
        }
        
        return set;
    }
    
    
    /** 
     * Return the filter this buffer is associated with.
     * 
     * @return Return the filter this buffer is associated with.
     */
    public FilterSliceNode getFilterNode() {
        return filterNode;
    }
    
    /**
     * DO NOT USE, WE ARE NOT USING EXTRACOUNT FOR DOUBLE BUFFERING ACCOUNTING!
     */
    public int getExtraCount() {
        assert false;
        return extraCount;
    }
    
    /**
     * DO NOT USE, WE ARE NOT USING EXTRACOUNT FOR DOUBLE BUFFERING ACCOUNTING!
     */
    public void setExtraCount(int extracount) {
        assert false;
        this.extraCount = extracount;
    }
   
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyRead()
     */
    public List<JStatement> beginPrimePumpRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyRead()
     */
    public List<JStatement> endPrimePumpRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyWrite()
     */
    public List<JStatement> beginPrimePumpWrite() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyWrite()
     */
    public List<JStatement> endPrimePumpWrite() {
        return new LinkedList<JStatement>(); 
    }
}
