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

/**
 * A buffer represents a block of memory that a filter reads from or writes to.
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
    /** the name of the rotation structure */
    protected String rotStructName;
    /** the filter this buffer is associated with */
    protected FilterSliceNode filterNode;
    /** the names of the individual buffers */
    protected String[] bufferNames;
    /** a set of all the buffer types in the application */
    protected static HashSet<CType> types;
    /** prefix of the variable name for the rotating buffers */
    public static String rotTypeDefPrefix = "__rotating_buffer_";
           
    static {
        types = new HashSet<CType>();
    }
    
    protected RotatingBuffer(Edge edge, FilterSliceNode fsn) {
        super(edge);
        filterNode = fsn;
        rotStructName = this.getIdent() + "buf";
        setBufferSize();
    }
   
    /**
     * Create all the input and output buffers necessary for the slice graph.
     * Each filter that produces output will have an output buffer and each 
     * filter that expects input will have an input buffer.
     * 
     * @param schedule  The spacetime schedule of the application
     */
    public static void createBuffers(BasicSpaceTimeSchedule schedule) {
        InputRotatingBuffer.createInputBuffers(schedule);
        OutputRotatingBuffer.createOutputBuffers(schedule);
        //now add the typedefs needed for the rotating buffers to structs.h
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
     * Return the number of elements for each rotation of this buffer
     * 
     * @return the maximum size for this buffer for one rotation
     */
    public int getBufferSize() {
        return bufSize;
    }
    
    protected abstract void setBufferSize();
    
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
    
    /** Create an array reference given an offset */   
    protected JArrayAccessExpression bufRef(JExpression offset) {
        //return new JArrayAccessExpression(bufPrefix,offset);
        return null;
    }
}
