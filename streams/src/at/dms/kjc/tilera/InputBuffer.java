package at.dms.kjc.tilera;

import at.dms.kjc.slicegraph.*;
import java.util.*;

public class InputBuffer extends Buffer {

    /** map of all the input buffers from filter -> inputbuffer */
    protected static HashMap<FilterSliceNode, InputBuffer> buffers;
    
    static {
        buffers = new HashMap<FilterSliceNode, InputBuffer>();
    }

    
    /**
     * Create a new input buffer that is associated with the filter node.
     * 
     * @param filterNode The filternode for which to create a new input buffer.
     */
    public InputBuffer(FilterSliceNode filterNode) {
        super(filterNode.getEdgeToPrev(), filterNode);
        buffers.put(filterNode, this);
    }
    
    /**
     * Return the input buffer associated with the filter node.
     * 
     * @param fsn The filter node in question.
     * @return The input buffer of the filter node.
     */
    public static InputBuffer getInputBuffer(FilterSliceNode fsn) {
        return buffers.get(fsn);
    }
}
