package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;
import java.util.HashMap;
import at.dms.kjc.*;

public class OffChipBuffer 
{
    private RawTile owner;
    private Vector users;
    private String ident;
    private static int unique_id;
    //the tracenode connection that this buffer is modeling
    private TraceNode source;
    private TraceNode dest;
    private static HashMap bufferStore;
    private Address size;
    private CType type;
    private StreamingDram dram;

    static 
    {
	unique_id = 0;
	bufferStore = new HashMap();
    }
    
    protected OffChipBuffer(TraceNode source, TraceNode dest)
    {
	assert source != null && dest != null : 
	    "source or dest cannot be null for an off chip buffer";

	if ((source.isFilterTrace() && !dest.isOutputTrace()) ||
	    (source.isOutputTrace() && !dest.isInputTrace()) ||
	    (source.isInputTrace() && !dest.isFilterTrace()))
	    Utils.fail("invalid buffer type");
	
	this.source = source;
	this.dest = dest;

	users = new Vector();
	ident = "__buf_" + /*owner.getIODevice().getPort() + */ "_" + unique_id + "__";
	unique_id++;
	setType();
	calculateSize();
    }

    public boolean redundant() 
    {
	if (source.isInputTrace() && dest.isFilterTrace()) {
	    //if this is a input->filter and no inputs then unnecessary
	    if (((InputTraceNode)source).noInputs()) {
		System.out.println("source: " + source + ", dest: " + dest + " size: " + size);
		return true;
	    }
	    
	    //if only one source to the input and the dram for the
	    //previous buffer is the same then redunant
	    
	    OutputTraceNode out = ((InputTraceNode)source).getParent().getDepends()[0].getTail();
	    if (((InputTraceNode)source).oneInput() &&
		OffChipBuffer.getBuffer(out, source).getDRAM() == dram)
		return true;	    
	} else if (source.isOutputTrace() && dest.isInputTrace()) {
	    //one output and the dram is the same as the previous 
	    if (((OutputTraceNode)source).oneOutput() &&
		OffChipBuffer.getBuffer(source.getPrevious(), source).getDRAM() == dram)
		return true;
	} else if (source.isFilterTrace() && dest.isOutputTrace()) {
	    //if this a filter->outputtrace and the output has no outputs
	    if (((OutputTraceNode)dest).noOutputs())
		return true;
	}
	return false;	
    }
    

    public void setDRAM(StreamingDram DRAM) 
    {
	this.dram = DRAM;
    }
    
    public StreamingDram getDRAM() 
    {
	assert dram != null: "need to assign buffer to streaming dram";
	return dram;
    }
    
    
    public String getIdent() 
    {
	return ident;
    }
    
    
    public CType getType() 
    {
	return type;
    }
    
    private void setType() 
    {
	if (source.isFilterTrace())
	    type = ((FilterTraceNode)source).getFilter().getOutputType();
	else if (dest.isFilterTrace()) 
	    type = ((FilterTraceNode)dest).getFilter().getInputType();
	else 
	    type = ((OutputTraceNode)source).getType();
    }

    //return the buffer from src, to dst
    //if it does not exist, create it
    public static OffChipBuffer getBuffer(TraceNode src, TraceNode dst) 
    {
	HashMap srcMap = (HashMap)bufferStore.get(src);
	if (srcMap == null) {
	    srcMap = new HashMap();
	    bufferStore.put(src, srcMap);
	}
	
	OffChipBuffer buf = (OffChipBuffer)srcMap.get(dst);
	if (buf == null) {
	    buf = new OffChipBuffer(src, dst);
	    srcMap.put(dst, buf);
	}
	
	return buf;
    }
    
    public Address getSize() 
    {
	return size;
    }
    

    private void calculateSize() 
    {
	//we'll make it 32 byte aligned
	if (source.isFilterTrace()) {
	    //the size is the max of the multiplicities
	    //times the push rate
	    FilterInfo fi = FilterInfo.getFilterInfo((FilterTraceNode)source);
	    int maxItems = Math.max(fi.initMult,
				    Math.max(fi.primePump, fi.steadyMult));
	    maxItems *= fi.push;
	    //account for the initpush
	    
	    if (fi.push < fi.prePush)
		maxItems += (fi.prePush - fi.push);
	    size = (Address.ZERO.add(maxItems)).add32Byte(0);
	}
	else if (dest.isFilterTrace())
	{
	    //this is not a perfect estimation but who cares
	    FilterInfo fi = FilterInfo.getFilterInfo((FilterTraceNode)dest);
	    int maxItems = Math.max(fi.steadyMult,
				    Math.max(fi.initMult, fi.primePump));
	   
	    maxItems *= fi.pop;
	    //now account for initpop, initpeek, peek
	    maxItems += (fi.prePeek + fi.prePop + fi.prePeek);
	    
	    size = (Address.ZERO.add(maxItems)).add32Byte(0);
	}
	else {
	    InputTraceNode in = (InputTraceNode)dest;
	    OutputTraceNode out = (OutputTraceNode)source;
	    //max of the buffer size in the various stages...
	    int maxItems = Math.max(Util.steadyBufferSize(in, out),
				    Math.max(Util.initBufferSize(in, out),
					     Util.primePumpBufferSize(in, out)));
	    size = (Address.ZERO.add(maxItems)).add32Byte(0);
	}
    }
    
    /**
     * return the neighboring tile of the dram this buffer is assigned to
     **/
    public RawTile getOwner() 
    {
	assert (dram != null) : 
	    "owner not set yet";
	return dram.getNeighboringTile();
    }
}

