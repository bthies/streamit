package at.dms.kjc.tilera;

import java.util.HashMap;

import at.dms.kjc.slicegraph.FilterSliceNode;

public class OutputBuffer extends Buffer {
    
    /** map of all the output buffers from filter -> outputbuffer */
    protected static HashMap<FilterSliceNode, OutputBuffer> buffers;
    
    static {
        buffers = new HashMap<FilterSliceNode, OutputBuffer>();
    }

    
    /**
     * Create a new output buffer that is associated with the filter node.
     * 
     * @param filterNode The filternode for which to create a new output buffer.
     */
    public OutputBuffer(FilterSliceNode filterNode) {
        super(filterNode.getEdgeToNext(), filterNode);
        buffers.put(filterNode, this);
    }
    
    /**
     * Return the output buffer associated with the filter node.
     * 
     * @param fsn The filter node in question.
     * @return The output buffer of the filter node.
     */
    public static OutputBuffer getOutputBuffer(FilterSliceNode fsn) {
        return buffers.get(fsn);
    }
}
