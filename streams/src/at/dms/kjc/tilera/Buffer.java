package at.dms.kjc.tilera;

import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import java.util.LinkedList;
import at.dms.kjc.spacetime.*;

/**
 * A buffer represents a block of memory that a filter reads from or writes to.
 * 
 * Note the we are not using extraCount of Channel for double buffering accounting,
 * instead we are using rotationLength.
 * 
 * @author mgordon
 *
 */
public abstract class Buffer extends Channel {
    
    /** the maximum size of this buffer in bytes for one rotation */
    protected BufferSize bufSize;
    /** the filter this buffer is associated with */
    protected FilterSliceNode filterNode;
           
    protected Buffer(Edge edge, FilterSliceNode fsn) {
        super(edge);
        filterNode = fsn;
    }
   
    /**
     * Create all the input and output buffers necessary for the slice graph.
     * Each filter that produces output will have an output buffer and each 
     * filter that expects input will have an input buffer.
     * 
     * @param slices
     */
    public static void createBuffers(BasicSpaceTimeSchedule schedule) {
        InputBuffer.createInputBuffers(schedule);
        OutputBuffer.createOutputBuffers(schedule);
    }
    
    /**
     * Return the number of bytes that should be allocated for one rotation
     * of this buffer.
     * 
     * @return the maximum size for this buffer for one rotation
     */
    public BufferSize getBufferSize() {
        return bufSize;
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
}
