package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;
import java.util.HashMap;


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

    static 
    {
	unique_id = 0;
	bufferStore = new HashMap();
    }
    
    protected OffChipBuffer(TraceNode source, TraceNode dest)
    {
	this.source = source;
	this.dest = dest;
	users = new Vector();
	ident = "__buf_" + owner.getIODevice().getPort() + "_" + unique_id + "__";
	unique_id++;
	
	calculateSize();
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
    
    private void calculateSize() 
    {
	//we'll make it 32 byte aligned
	if (source.isFilterTrace()) {
	    //the size is the max of the multiplicities
	    //times the push rate
	    FilterTraceNode node = (FilterTraceNode)source;
	    int maxItems = Math.max(node.getInitMult(),
				    Math.max(node.getInitMult(), node.getPrimePumpMult()));
	    maxItems *= node.getFilter().getPushInt();
	    //account for the initpush
	    if (node.getFilter().getPushInt() < node.getFilter().getInitPush())
		maxItems += (node.getFilter().getInitPush() - node.getFilter().getPushInt());
	    size = (Address.ZERO.add(maxItems)).add32Byte(0);
	}
	else if (dest.isFilterTrace())
	{
	    //this is not a perfect estimation but who cares
	    FilterTraceNode node = (FilterTraceNode)dest;
	    int maxItems = Math.max(node.getInitMult(),
				    Math.max(node.getInitMult(), node.getPrimePumpMult()));
	   
	    maxItems *= node.getFilter().getPopInt();
	    //now account for initpop, initpeek, peek
	    maxItems += (node.getFilter().getInitPeek() + node.getFilter().getInitPop() +
			node.getFilter().getPeekInt());
	    
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
    
    
    public void setOwner(RawTile tile) 
    {
	this.owner = tile;
    }

    public void addUser(RawTile tile) 
    {
	users.add(tile);
    }
    
    public RawTile[] getUsers() 
    {
	return (RawTile[])users.toArray(new RawTile[0]);
    }
}

