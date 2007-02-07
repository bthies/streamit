package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import at.dms.kjc.*;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;

/**
 * This class represents a buffer between two traces. The rotating register abstraction 
 * is implemented by have rotating buffers, so we can actually have many physical buffers 
 * for each buffer.  
 * 
 * @author mgordon
 *
 */
public class InterSliceBuffer extends OffChipBuffer {
    // the edge
    protected InterSliceEdge edge;
   
    /** 
     * A map of StreamingDrams to the number of InterTraceBuffers
     * mapped to it.  StreamingDram->Integer
     */
    protected static HashMap<StreamingDram, Integer> dramsToBuffers;
    
    
    protected InterSliceBuffer(InterSliceEdge edge) {
        super(edge.getSrc(), edge.getDest());
        this.edge = edge;
        calculateSize();
    }

    public static InterSliceBuffer getBuffer(InterSliceEdge edge) {
        if (!bufferStore.containsKey(edge)) {
            bufferStore.put(edge, new InterSliceBuffer(edge));
        }
        return (InterSliceBuffer) bufferStore.get(edge);
    }

   
    /**
     * @return True of this buffer is not used because the output intrattracebuffer
     * of the source trace performs its function.
     */
    public boolean redundant() {
        return unnecessary((OutputSliceNode) source);
    }

    public OffChipBuffer getNonRedundant() {
        if (redundant()) {
            return IntraSliceBuffer.getBuffer(
                                              (FilterSliceNode) source.getPrevious(),
                                              (OutputSliceNode) source).getNonRedundant();
        }
        return this;
    }

    protected void setType() {
        type = ((OutputSliceNode) source).getType();
    }

    protected void calculateSize() {
        // max of the buffer size in the various stages...
        int maxItems = Math.max(Util.initBufferSize(edge), Util.steadyBufferSize(edge));
        
        sizeSteady = (Address.ZERO.add(maxItems)).add32Byte(0);
    }

    public InterSliceEdge getEdge() {
        return edge;
    }

    /**
     * @param dram
     * @return The number of intertracebuffer's mapped to <pre>dram</pre>.
     * Used because each dram can at handle at most 
     * StreamingDram.STREAMING_QUEUE_SIZE number of reads and writes.
     */
    public int getNumInterTraceBuffers(StreamingDram dram) {
        assert dramsToBuffers.containsKey(dram);
        return dramsToBuffers.get(dram).intValue();
    }
    
}