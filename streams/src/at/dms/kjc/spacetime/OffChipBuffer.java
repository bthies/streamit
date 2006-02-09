package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import at.dms.kjc.*;

/**
 * This abstract class represents a buffer in the partitioner slice graph.  A 
 * buffer appears between slices and inside slices between the input node and the first
 * filter and between the last filter and the output node.
 * 
 * @author mgordon
 *
 */
public abstract class OffChipBuffer {
    /** The sending or receiving tile*/
    protected RawTile owner;
    /** unique ident for the buffer */
    protected String ident;
    protected static int unique_id;
    /** the store for all OffChipBuffers, indexed by src, dest */
    protected static HashMap bufferStore;
    /** the size of the buffer in the init stage */
    protected Address sizeInit;
    /** the size of the buffer in the steady stage */ 
    protected Address sizeSteady;
    /** the type of the buffer */ 
    protected CType type;
    /** the dram that we are reading/writing */
    protected StreamingDram dram;
    /** the source slice (trace) */  
    protected TraceNode source;
    /** the destination slice (trace) */
    protected TraceNode dest;
    /** true if this buffer uses static net */
    protected boolean staticNet;
    
    static {
        unique_id = 0;
        bufferStore = new HashMap();
    }

    protected OffChipBuffer(TraceNode src, TraceNode dst) {
        source = src;
        dest = dst;

        ident = "__buf_" + /* owner.getIODevice().getPort() + */"_" + unique_id
            + "__";
        unique_id++;
        setType();
    }
    
    /**
     * @return Returns true if this buffer uses staticNet.
     */
    public boolean isStaticNet() {
        return staticNet;
    }

    /**
     * @param staticNet The staticNet to set.
     */
    public void setStaticNet(boolean staticNet) {
        this.staticNet = staticNet;
    }



    public abstract boolean redundant();

    /**
     * if this buffer is redundant return the first upstream buffer that is not
     * redundant, return null if this is a input->filter buffer with no input or
     * a filter->output buffer with no output
     */
    public abstract OffChipBuffer getNonRedundant();

    // return true if the inputtracenode does anything necessary
    public static boolean unnecessary(InputTraceNode input) {
        if (input.noInputs())
            return true;
        if (input.oneInput()
            && (InterTraceBuffer.getBuffer(input.getSingleEdge()).getDRAM() == IntraTraceBuffer
                .getBuffer(input, (FilterTraceNode) input.getNext())
                .getDRAM()))
            return true;
        return false;
    }

    // return true if outputtracenode does anything
    public static boolean unnecessary(OutputTraceNode output) {
        if (output.noOutputs())
            return true;
        if (output.oneOutput()
            && (IntraTraceBuffer.getBuffer(
                                           (FilterTraceNode) output.getPrevious(), output)
                .getDRAM() == InterTraceBuffer.getBuffer(
                                                         output.getSingleEdge()).getDRAM()))
            return true;
        return false;
    }

    public void setDRAM(StreamingDram DRAM) {
        // assert !redundant() : "calling setDRAM() on redundant buffer";
        this.dram = DRAM;
        if (source.isOutputTrace() && dest.isInputTrace() && redundant())
            SpaceTimeBackend.println("*Redundant: " + this.toString());

    }

    public boolean isAssigned() {
        return dram != null;
    }

    public StreamingDram getDRAM() {
        assert dram != null : "need to assign buffer to streaming dram "
            + this.toString();
        // assert !redundant() : "calling getDRAM() on redundant buffer";
        return dram;
    }

    public String getIdent(boolean init) {
        assert !redundant() : this.toString() + " is redundant";
        return ident + (init ? "_init__" : "_steady__");
    }

    public String getIdentPrefix() {
        assert !redundant();
        return ident;
    }

    public CType getType() {
        return type;
    }

    protected abstract void setType();

    // return of the buffers of this stream program
    public static Set getBuffers() {
        HashSet set = new HashSet();
        Iterator sources = bufferStore.keySet().iterator();
        while (sources.hasNext()) {
            set.add(bufferStore.get(sources.next()));
        }
        return set;
    }

    public Address getSize(boolean init) {
        if (init)
            return sizeInit;
        else
            return sizeSteady;
    }

    abstract protected void calculateSize();

    /**
     * return the neighboring tile of the dram this buffer is assigned to
     */
    public RawTile getOwner() {
        assert (dram != null) : "owner not set yet";
        return dram.getNeighboringTile();
    }

    public String toString() {
        return source + "->" + dest + "[" + dram + "]";
    }

    public TraceNode getSource() {
        return source;
    }

    public TraceNode getDest() {
        return dest;
    }

    public boolean isIntraTrace() {
        return (this instanceof IntraTraceBuffer);
    }

    public boolean isInterTrace() {
        return (this instanceof InterTraceBuffer);
    }

}
