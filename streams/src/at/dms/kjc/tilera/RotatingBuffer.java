package at.dms.kjc.tilera;

import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.slicegraph.*;

import java.util.LinkedList;
import at.dms.kjc.spacetime.*;
import at.dms.kjc.*;
import at.dms.kjc.common.CommonUtils;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
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
    
    /** array size in elements of each buffer of the rotation*/
    protected int bufSize;
    /** type of array: array of element type */
    protected CType bufType;
    /** the name of the rotation structure (always points to its head) */
    protected String writeRotStructName;
    /** the name of the pointer to the current write rotation of this buffer */
    protected String currentWriteRotName;
    /** the name of the pointer to the write buffer of the current rotation */
    protected String currentWriteBufName;
    /** name of the variable that points to the rotation structure we should be transferring from */
    public String transRotName;
    /** name of the variable that points to the buffer we should be transferring from */
    public String transBufName;
    /** name of variable containing head of array offset */
    protected String writeHeadName;
    /** definition for head */
    protected JVariableDefinition writeHeadDefn;
    /** definition of boolean used during primepump to see if it is the first exection */
    protected JVariableDefinition firstExe;
    protected String firstExeName;
    /** the filter this buffer is associated with */   
    protected FilterSliceNode filterNode;
    /** the names of the individual buffers */
    protected String[] bufferNames;
    /** a set of all the buffer types in the application */
    protected static HashSet<CType> types;
    /** prefix of the variable name for the rotating buffers */
    public static String rotTypeDefPrefix = "__rotating_buffer_";
    /** the tile this buffer is mapped to */
    protected Tile parent;
    /** the filter info object for the filter that contains this buffer */
    protected FilterInfo filterInfo;
    /** the data transfer statements that are generated for this output buffer */
    protected BufferTransfers transferCommands;
    /** the address buffers that this buffer rotation uses as destinations for transfers */ 
    protected HashMap<InputRotatingBuffer, SourceAddressRotation> addressBuffers;
    protected static HashMap<FilterSliceNode, InputRotatingBuffer> inputBuffers;
    protected static HashMap<FilterSliceNode, RotatingBuffer> outputBuffers;
    protected final String temp = "__temp__";
    
    
    static {
        types = new HashSet<CType>();
        inputBuffers = new HashMap<FilterSliceNode, InputRotatingBuffer>();
        outputBuffers = new HashMap<FilterSliceNode, RotatingBuffer>();
        
    }
    
    protected RotatingBuffer(Edge edge, FilterSliceNode fsn, Tile parent) {
        super(edge);
        this.parent = parent;
        filterNode = fsn;
        filterInfo = FilterInfo.getFilterInfo(fsn);
        writeRotStructName = this.getIdent() + "write_rot_struct";
        currentWriteRotName =this.getIdent() + "_write_current";
        currentWriteBufName =this.getIdent() + "_write_buf";
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
        InputRotatingBuffer.createInputBuffers(schedule);
        OutputRotatingBuffer.createOutputBuffers(schedule);
        
        //now that all the buffers are created, create the pointers to them
        //that live on other tiles, and create the transfer commands
        for (InputRotatingBuffer buf : inputBuffers.values()) {
            buf.createAddressBuffers();
            buf.createTransferCommands();
        }
        for (RotatingBuffer buf : outputBuffers.values()) {
            buf.createAddressBuffers();
            buf.createTransferCommands();
        }
        
        //now add the typedefs needed for the rotating buffers to structs.h
        rotTypeDefs();
        //now that all the buffers are allocated, we create a barrier on all the tiles
        //so that we wait for all the shared memory to be allocated
        TileCodeStore.addBufferInitBarrier();
        //generate the code for the address communication stage
        communicateAddresses();
    }
    
    public void createAddressBuffers() {
        
    }
    
    public void createTransferCommands() {
        //generate the dma commands
        if (TileraBackend.DMA) 
            transferCommands = new BufferDMATransfers(this);
        else 
            transferCommands = new BufferRemoteWritesTransfers(this);
    }
        
    /**
     * Return the address rotation that this output rotation uses for the given input slice node
     * 
     * @param input the input slice node 
     * @return the dma address rotation used to store the address of the 
     * rotation associated with this input slice node
     */
    public SourceAddressRotation getAddressBuffer(InputSliceNode input) {
        assert addressBuffers.containsKey(InputRotatingBuffer.getInputBuffer(input.getNextFilter())) ;
        
        return addressBuffers.get(InputRotatingBuffer.getInputBuffer(input.getNextFilter()));
    }
    
    /**
     * Generate the code necessary to communicate the addresses of the shared input buffers 
     * of all input rotational structures to the sources that will write to the buffer 
     * using DMA commands.
     */
    protected static void communicateAddresses() {
        //the tag for the messages that we are using to send around the address
        int tag = 0;
        
        for (int t = 0; t < TileraBackend.chip.abstractSize(); t++) {
            Tile ownerTile = TileraBackend.chip.getTranslatedTile(t);
            TileCodeStore cs = ownerTile.getComputeCode();
            
            for (FilterSliceNode filter : cs.getFilters()) {
                InputRotatingBuffer buf = InputRotatingBuffer.getInputBuffer(filter);
                //if this filter does not have an input buffer, then continue
                if (buf == null)
                    continue;                
                for (int i = 0; i < buf.getAddressBuffers().length; i++) {
                    SourceAddressRotation addr = buf.getAddressBuffers()[i];
                    Tile srcTile = addr.parent;
                    //create the declaration of the buffers on the tile
                    addr.declareBuffers();
                    for (int b = 0; b < buf.rotationLength; b++) {
                        if (srcTile.equals(ownerTile)) {
                            //if they are on the same tile, then just assign them directly
                            srcTile.getComputeCode().addStatementToBufferInit(
                                    addr.bufferNames[b] + " = " + buf.bufferNames[b]);
                        }
                        else {
                        
                            //send the address from the home tile to this source
                            cs.addStatementToBufferInit("" +
                                    "ilib_msg_send(ILIB_GROUP_SIBLINGS, " +
                                    TileraBackend.chip.getTranslatedTileNumber(srcTile.getTileNumber()) + ", " +
                                    tag + ", " +
                                    "&" + buf.bufferNames[b] + ", " +
                                    "sizeof (" + buf.bufType + "*))");

                            //receive the addresses on the source tile
                            srcTile.getComputeCode().addStatementToBufferInit(
                                    "ilib_msg_receive(ILIB_GROUP_SIBLINGS, " +
                                    TileraBackend.chip.getTranslatedTileNumber(ownerTile.getTileNumber()) + ", " +
                                    tag + ", " +
                                    "&" + addr.bufferNames[b] + ", " +                                
                                    "sizeof (" + buf.bufType + "*), &status)");
                            tag++;
                        }
                    }
                    //set up the rotation structure at the source
                    addr.setupRotation();
                }               
            }
            //after we are all done with sending the addresses for this tile
            //append a barrier instruction to all of the tiles
            TileCodeStore.addBufferInitBarrier();
        }
    }
    
    /**
     * Create the typedef for the rotating buffer structure, one for each type 
     * we see in the program (each channel type).
     */
    protected static void rotTypeDefs() {
        for (CType type : types) {
            TileraBackend.structs_h.addLineSC("typedef struct __rotating_struct_" +
                    type.toString() + "__" + 
                    " *__rot_ptr_" + type.toString() + "__");
            TileraBackend.structs_h.addText("typedef struct __rotating_struct_" + type.toString() + "__ {\n");
            TileraBackend.structs_h.addText("\t" + type.toString() + " *buffer;\n");
            TileraBackend.structs_h.addText("\t__rot_ptr_" + type.toString() + "__ next;\n");
            TileraBackend.structs_h.addText("} " + rotTypeDefPrefix + type.toString() + ";\n");
        }
    }
    /**
     * Generate the code necessary to allocate the buffers, setup the rotation structure,
     * and communicate addresses.
     * 
     * @param input true if this is an input buffer
     */
    protected void createInitCode(boolean input) {
        this.setBufferNames();
        this.allocBuffers(input);
        this.setupRotation();
    }
    
    /**
     * Allocate the constituent buffers of this rotating buffer structure.  This is used
     * for buffers that need allocation from memory but not for dma rotational buffers
     * are just pointers to memory on another tile.
     */
    protected void allocBuffers(boolean shared) {
        for (int i = 0; i < rotationLength; i++) {
            TileCodeStore cs = this.parent.getComputeCode();
            
            //create the pointer to the this buffer constituent 
            cs.addStatementFirstToBufferInit(this.getType().toString() + "* " + 
                    bufferNames[i]);
            
            //malloc the steady buffer
            cs.addStatementToBufferInit(new JExpressionStatement(new JEmittedTextExpression(
                    bufferNames[i] + " = (" + this.getType() + 
                    "*) " +  
                    (shared ? "malloc_shared" : "malloc") +
                    "(" + 
                    this.getBufferSize() + " * sizeof(" +
                    this.getType() + "))")));
        }
    }
      
    /**
     * Generate the code to setup the structure of the rotating buffer 
     * as a circular linked list.
     */
    protected abstract void setupRotation();
    
    /** 
     * The statement returned by this method will be added to the end of the rotation setup for this
     * buffer.
     * 
     * @return The statement to add
     */
    protected JStatement endOfRotationSetup() {
       JBlock block = new JBlock();
       return block;
    }
    
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
     * Return the set of all the InputBuffers that are mapped to tile t.
     */
    public static Set<InputRotatingBuffer> getInputBuffersOnTile(Tile t) {
        HashSet<InputRotatingBuffer> set = new HashSet<InputRotatingBuffer>();
        
        for (InputRotatingBuffer b : inputBuffers.values()) {
            if (TileraBackend.backEndBits.getLayout().getComputeNode(b.getFilterNode()).equals(t))
                set.add(b);
        }
        
        return set;
    }
    /**
     * Return the set of all the InputBuffers that are mapped to tile t.
     */
    public static Set<RotatingBuffer> getOutputBuffersOnTile(Tile t) {
        HashSet<RotatingBuffer> set = new HashSet<RotatingBuffer>();
        
        for (RotatingBuffer b : outputBuffers.values()) {
            if (TileraBackend.backEndBits.getLayout().getComputeNode(b.getFilterNode()).equals(t))
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
