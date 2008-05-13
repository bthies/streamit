package at.dms.kjc.tilera;

import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;

/**
 * A buffer represents a block of memory that a filter reads from or writes to.
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
}
