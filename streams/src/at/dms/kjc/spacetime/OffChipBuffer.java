package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
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
	if (source.isInputTrace() && dest.isFilterTrace())
	    return !necessary((InputTraceNode)source);
	else if (source.isOutputTrace() && dest.isInputTrace())
	     return !necessary((OutputTraceNode)source);
	else {//(source.isFilterTrace() && dest.isOutputTrace())
	    //if this a filter->outputtrace and the output has no outputs
	    if (((OutputTraceNode)dest).noOutputs())
		return true;
	}
	return false;
    }

    /**
     * if this buffer is redundant return the first upstream buffer
     * that is not redundant, return null if this is a input->filter 
     * buffer with no input or a filter->output buffer with no output
     **/
    public OffChipBuffer getNonRedundant() 
    {
	if (source.isInputTrace() && dest.isFilterTrace()) {
	    //if no inputs return null
	    if (((InputTraceNode)source).noInputs())
		return null;
	    //if redundant get the previous buffer and call getNonRedundant
	    if (redundant())
		return OffChipBuffer.getBuffer(((InputTraceNode)source).getSources()[0],
					       source).getNonRedundant();
	    //otherwise return this...
	    return this;
	}
	else if (source.isOutputTrace() && dest.isInputTrace()) {
	    //if this buffer is redundant, return the upstream buffer
	    //it may be a buffer with no input so call getNonRedundant on that shit
	    if (redundant())
		return OffChipBuffer.getBuffer(source, source.getPrevious()).getNonRedundant();
	    return this;
	}
	else { //(source.isFilterTrace() && dest.isOutputTrace())
	    //if no outputs return null
	    if (((OutputTraceNode)dest).noOutputs())
		return null;
	    //the only way it could be redundant (unnecesary) is for there to be no outputs
	    return this;
	}
	
    }
    
    
    //return true if the inputtracenode does anything necessary
    public static boolean necessary(InputTraceNode input) 
    {
	if (input.noInputs())
	    return false;
	if (input.oneInput() &&
	    (OffChipBuffer.getBuffer(input.getSources()[0], input).getDRAM() ==
	     OffChipBuffer.getBuffer(input, input.getNext()).getDRAM()))
	    return false;
	return true;
    }
    
    //return true if outputtracenode does anything
    public static boolean necessary(OutputTraceNode output) 
    {
	if (output.noOutputs())
	    return false;
	if (output.oneOutput() && 
	    (OffChipBuffer.getBuffer(output.getPrevious(), output).getDRAM() ==
	     OffChipBuffer.getBuffer(output, output.getDests()[0][0]).getDRAM()))
	    return false;
	return true;
    }

    public void setDRAM(StreamingDram DRAM) 
    {
	//	assert !redundant() : "calling setDRAM() on redundant buffer";
	this.dram = DRAM;
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


    //return of the buffers of this stream program    
    public static Set getBuffers() 
    {
	HashSet set = new HashSet();
	Iterator sources = bufferStore.keySet().iterator();
	while (sources.hasNext()) {
	    HashMap bufs = (HashMap)bufferStore.get(sources.next());
	    Iterator dests = bufs.keySet().iterator();
	    while (dests.hasNext()) {
		set.add(bufs.get(dests.next()));
	    }
	}
	return set;
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

