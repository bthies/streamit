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
    
    /** reference to whole array, prefix to element access */
    protected JExpression bufPrefix;
    /** definition for array */
    protected JVariableDefinition bufDefn;
    /** array size in elements */
    protected int bufSize;
    /** type of array: array of element type */
    protected CType bufType;
    /** array name */
    protected String bufName;
    /** the filter this buffer is associated with */
    protected FilterSliceNode filterNode;
           
    protected RotatingBuffer(Edge edge, FilterSliceNode fsn) {
        super(edge);
        filterNode = fsn;
        bufName = this.getIdent() + "buf";
        setBufferSize();
        bufDefn = CommonUtils.makeArrayVariableDefn(bufSize,edge.getType(),bufName);
                bufPrefix = new JFieldAccessExpression(bufName);
        bufPrefix.setType(edge.getType());
    }
   
    /**
     * Create all the input and output buffers necessary for the slice graph.
     * Each filter that produces output will have an output buffer and each 
     * filter that expects input will have an input buffer.
     * 
     * @param slices
     */
    public static void createBuffers(BasicSpaceTimeSchedule schedule) {
        InputRotatingBuffer.createInputBuffers(schedule);
        OutputRotatingBuffer.createOutputBuffers(schedule);
    }
    
    /**
     * Return the number of bytes that should be allocated for one rotation
     * of this buffer.
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
        return new JArrayAccessExpression(bufPrefix,offset);
    }
}
