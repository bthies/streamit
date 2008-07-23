package at.dms.kjc.tilera;

import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JFieldAccessExpression;
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
    protected String rotStructName;
    /** the name of the pointer to the current rotation of this buffer */
    protected String currentRotName;
    /** the name of the pointer to the buffer of the current rotation */
    protected String currentBufName;
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
    protected final String temp = "__temp__";
    
    static {
        types = new HashSet<CType>();
    }
    
    protected RotatingBuffer(Edge edge, FilterSliceNode fsn, Tile parent) {
        super(edge);
        this.parent = parent;
        filterNode = fsn;
        rotStructName = this.getIdent() + "_rot_struct";
        currentRotName =this.getIdent() + "_rot_current";
        currentBufName =this.getIdent() + "_cur_buf";
        setBufferSize();
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
        //now add the typedefs needed for the rotating buffers to structs.h
        rotTypeDefs();
        //now that all the buffers are allocated, we create a barrier on all the tiles
        //so that we wait for all the shared memory to be allocated
        for (int t = 0; t < TileraBackend.chip.abstractSize(); t++) {
            TileCodeStore cs = TileraBackend.chip.getTranslatedTile(t).getComputeCode();
            cs.addStatementToBufferInit("ilib_msg_barrier(ILIB_GROUP_SIBLINGS)");
        }
        //generate the code for the address communication stage
        communicateAddresses();
    }
    
    
    /**
     * Generate the code necessary to communicate the addresses of the shared input buffers 
     * of all input rotational structures to the sources that will write to the buffer 
     * susing DMA commands.
     */
    protected static void communicateAddresses() {
        for (int t = 0; t < TileraBackend.chip.abstractSize(); t++) {
            Tile tile = TileraBackend.chip.getTranslatedTile(t);
            TileCodeStore cs = tile.getComputeCode();
            
            for (FilterSliceNode filter : cs.getFilters()) {
                InputRotatingBuffer buf = InputRotatingBuffer.getInputBuffer(filter);
                
            }
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
     * Allocate the constituent buffers of this rotating buffer structure
     */
    protected void allocBuffers(boolean shared) {
        for (int i = 0; i < rotationLength; i++) {
            TileCodeStore cs = this.parent.getComputeCode();
            
            //create the pointer to the this buffer constituent 
            cs.addStatementToBufferInit(new JExpressionStatement(new JEmittedTextExpression(this.getType().toString() + "* " + 
                    bufferNames[i])));
            
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
        
        JBlock block = new JBlock();
        
        //create a temp var
        block.addStatement(Util.toStmt(rotType + " *" + temp));
        
        //create the first entry!!
        block.addStatement(Util.toStmt(rotStructName + " =  (" + rotType+ "*)" + "malloc(sizeof("
                + rotType + "))"));
        
        //modify the first entry
        block.addStatement(Util.toStmt(rotStructName + "->buffer = " + bufferNames[0]));
        if (this.rotationLength == 1) 
            block.addStatement(Util.toStmt(rotStructName + "->next = " + rotStructName));
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
            
            block.addStatement(Util.toStmt(temp + "->next = " + rotStructName));
        }
        block.addStatement(Util.toStmt(currentRotName + " = " + rotStructName));
        block.addStatement(Util.toStmt(currentBufName + " = " + currentRotName + "->buffer"));
        cs.addStatementToBufferInit(block);
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
    
    /** 
     * Return the filter this buffer is associated with.
     * 
     * @return Return the filter this buffer is associated with.
     */
    public FilterSliceNode getFilterNode() {
        return filterNode;
    }
    
    public static Set<RotatingBuffer> getBuffersOnTile(Tile t) {
        return null;
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
    
    protected List<JStatement> rotateStatements() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(Util.toStmt(currentRotName + " = " + currentRotName + "->next"));
        list.add(Util.toStmt(currentBufName + " = " + currentRotName + "->buffer"));
        return list;
    }
    
    /** Create an array reference given an offset */   
    protected JArrayAccessExpression bufRef(JExpression offset) {
        JFieldAccessExpression bufAccess = new JFieldAccessExpression(new JThisExpression(), currentBufName);
        return new JArrayAccessExpression(bufAccess, offset);
    }
}
