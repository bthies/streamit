package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import at.dms.kjc.*;

public class InterTraceBuffer extends OffChipBuffer 
{    
    //the edge
    protected Edge edge;

    protected InterTraceBuffer(Edge edge) 
    {
	super(edge.getSrc(), edge.getDest());
	this.edge = edge;
    }
    
    public static InterTraceBuffer getBuffer(Edge edge) 
    {
	if (!bufferStore.containsKey(edge)) {
	    bufferStore.put(edge, new InterTraceBuffer(edge));
	}
	return (InterTraceBuffer)bufferStore.get(edge);
    }
    
    public boolean redundant() 
    {
	return unnecessary((OutputTraceNode)source);
    }
    
    public OffChipBuffer getNonRedundant() 
    {
	if (redundant()) {
	    return IntraTraceBuffer.getBuffer((FilterTraceNode)source.getPrevious(), 
					      (OutputTraceNode)source).getNonRedundant();
	}
	return this;
    }

    protected void setType()
    {
	type = ((OutputTraceNode)source).getType();	
    }

    protected void calculateSize() 
    {
	//max of the buffer size in the various stages...
	int maxItems = Math.max(Util.steadyBufferSize(edge),
				Math.max(Util.initBufferSize(edge),
					 Util.primePumpBufferSize(edge)));
	size = (Address.ZERO.add(maxItems)).add32Byte(0);	
    }
    
}
