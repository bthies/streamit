package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import at.dms.kjc.*;

public abstract class OffChipBuffer 
{
    protected RawTile owner;
    protected Vector users;
    protected String ident;
    protected static int unique_id;
    //the tracenode connection that this buffer is modeling
    protected static HashMap bufferStore;
    protected Address size;
    protected CType type;
    protected StreamingDram dram;
    protected TraceNode source;
    protected TraceNode dest;

    static 
    {
	unique_id = 0;
	bufferStore = new HashMap();
    }
    
    protected OffChipBuffer(TraceNode src, TraceNode dst)
    {
	source = src;
	dest = dst;

	users = new Vector();
	ident = "__buf_" + /*owner.getIODevice().getPort() + */ "_" + unique_id + "__";
	unique_id++;
	setType();
	calculateSize();
    }

    public abstract boolean redundant();

    /**
     * if this buffer is redundant return the first upstream buffer
     * that is not redundant, return null if this is a input->filter 
     * buffer with no input or a filter->output buffer with no output
     **/
    public abstract OffChipBuffer getNonRedundant();
    
    //return true if the inputtracenode does anything necessary
    public static boolean unnecessary(InputTraceNode input) 
    {
	if (input.noInputs())
	    return true;
	if (input.oneInput() &&
	    (InterTraceBuffer.getBuffer(input.getSingleEdge()).getDRAM() ==
	     IntraTraceBuffer.getBuffer(input, (FilterTraceNode)input.getNext()).getDRAM()))
	    return true;
	return false;
    }
    
    //return true if outputtracenode does anything
    public static boolean unnecessary(OutputTraceNode output) 
    {
	if (output.noOutputs())
	    return true;
	if (output.oneOutput() && 
	    (IntraTraceBuffer.getBuffer((FilterTraceNode)output.getPrevious(), output).getDRAM() ==
	     InterTraceBuffer.getBuffer(output.getSingleEdge()).getDRAM()))
	    return true;
	return false;
    }

    public void setDRAM(StreamingDram DRAM) 
    {
	//	assert !redundant() : "calling setDRAM() on redundant buffer";
	this.dram = DRAM;
	if (source.isOutputTrace() && dest.isInputTrace() && redundant())
	    SpaceTimeBackend.println("*Redundant: " + this.toString());

    }
    
    public boolean isAssigned() 
    {
	return dram != null;
    }
    
    public StreamingDram getDRAM() 
    {
	assert dram != null: "need to assign buffer to streaming dram";
	//assert !redundant() : "calling getDRAM() on redundant buffer";
	return dram;
    }
    
    
    public String getIdent() 
    {
	assert !redundant();
	return ident;
    }
    
    
    public CType getType() 
    {
	return type;
    }
    
    protected abstract void setType();

    //return of the buffers of this stream program    
    public static Set getBuffers() 
    {
	HashSet set = new HashSet();
	Iterator sources = bufferStore.keySet().iterator();
	while (sources.hasNext()) {
	    set.add(bufferStore.get(sources.next()));
	}
	return set;
    }
    

    public Address getSize() 
    {
	return size;
    }
    

    abstract protected void calculateSize();
        
    /**
     * return the neighboring tile of the dram this buffer is assigned to
     **/
    public RawTile getOwner() 
    {
	assert (dram != null) : 
	    "owner not set yet";
	return dram.getNeighboringTile();
    }

    public String toString() 
    {
	return source + "->" + dest + "[" + dram + "]";
    }
    
    public TraceNode getSource() 
    {
	return source;
    }
    
    public TraceNode getDest() 
    {
	return dest;
    }
}

