package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import at.dms.kjc.*;

public class IntraTraceBuffer extends OffChipBuffer 
{  
    public static IntraTraceBuffer getBuffer(FilterTraceNode src, OutputTraceNode dst) 
    {
	if (!bufferStore.containsKey(src)) {
	    bufferStore.put(src, new IntraTraceBuffer(src, dst));
	}
	assert (((IntraTraceBuffer)bufferStore.get(src)).getDest() ==
		dst) : "Src connected to different dst in buffer store";

	return (IntraTraceBuffer)bufferStore.get(src);
    }

    public static IntraTraceBuffer getBuffer(InputTraceNode src, FilterTraceNode dst) 
    {
	if (!bufferStore.containsKey(src)) {
	    bufferStore.put(src, new IntraTraceBuffer(src, dst));
	}
	assert (((IntraTraceBuffer)bufferStore.get(src)).getDest() ==
		dst) : "Src connected to different dst in buffer store";

	return (IntraTraceBuffer)bufferStore.get(src);
    }
  
    protected IntraTraceBuffer(InputTraceNode src,
			       FilterTraceNode dst) 
    {
	super(src, dst);
	calculateSize();
    }
    
    protected IntraTraceBuffer(FilterTraceNode src,
			       OutputTraceNode dst) 
    {
	super(src, dst);
	calculateSize();
    }
    
    public boolean redundant() 
    {
	//if there are no outputs for the output trace 
	//then redundant
	if (source.isFilterTrace() && dest.isOutputTrace()) {
	    if (((OutputTraceNode)dest).noOutputs())
		return true;
	} else //if the inputtrace is not necessray
	    return unnecessary((InputTraceNode)source);
	return false;
    }
    
	    
    public OffChipBuffer getNonRedundant() 
    {
	if (source.isInputTrace() && dest.isFilterTrace()) {
	    //if no inputs return null
	    if (((InputTraceNode)source).noInputs())
		return null;
	    //if redundant get the previous buffer and call getNonRedundant
	    if (redundant())
		return InterTraceBuffer.getBuffer(((InputTraceNode)source).getSingleEdge()).getNonRedundant();
	    //otherwise return this...
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
    
    protected void setType() 
    {
	if (source.isFilterTrace())
	    type = ((FilterTraceNode)source).getFilter().getOutputType();
	else if (dest.isFilterTrace()) 
	    type = ((FilterTraceNode)dest).getFilter().getInputType();
    }

    protected void calculateSize() 
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
    }
    
    
}
