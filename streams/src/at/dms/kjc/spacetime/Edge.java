package at.dms.kjc.spacetime;

import at.dms.kjc.*;

public class Edge {
    private OutputTraceNode src;
    private InputTraceNode dest;
    private CType type;
    
    public Edge(OutputTraceNode src,InputTraceNode dest) {
	assert src!=null:"Source Null!";
	assert dest!=null:"Dest Null!";
	this.src=src;
	this.dest=dest;
	type = null;
    }
    public Edge(OutputTraceNode src) {
	this.src=src;
    }

    public Edge(InputTraceNode dest) {
	this.dest=dest;
    }

    public CType getType() 
    {
	if (type != null)
	    return type;
	assert src.getPrevFilter().getFilter().getOutputType() ==
	    dest.getNextFilter().getFilter().getInputType() :
	    "Error calculating type";
	type = src.getPrevFilter().getFilter().getOutputType();
	return type;
    }
    
    
    public OutputTraceNode getSrc() {
	return src;
    }

    public InputTraceNode getDest() {
	return dest;
    }

    public void setSrc(OutputTraceNode src) {
	this.src=src;
    }

    public void setDest(InputTraceNode dest) {
	this.dest=dest;
    }

    public String toString() 
    {
	return src + "->" + dest + "(" + hashCode() + ")";
    }

    public int initItems() 
    {
	int itemsReceived, itemsSent;

	//calculate the items the input trace receives
	FilterInfo next = FilterInfo.getFilterInfo((FilterTraceNode)dest.getNext());
	itemsSent =  (int)((double)next.initItemsReceived() *
			   dest.ratio(this));
	//calculate the items the output trace sends
	FilterInfo prev = FilterInfo.getFilterInfo((FilterTraceNode)src.getPrevious());
	itemsReceived = (int)((double)prev.initItemsSent() *
			      src.ratio(this));
	
	/*
	System.out.println(out);
	for (int i = 0; i < out.getWeights().length; i++) {
	    System.out.println(" ---- Weight = " + out.getWeights()[i]);
	    for (int j = 0; j < out.getDests()[i].length; j++) 
		System.out.println(out.getDests()[i][j] + " " + out.getDests()[i][j].hashCode());
	     System.out.println(" ---- ");
	}
	//System.out.println(out.getWeight(edge)+ " / "   + out.totalWeights());
	//System.out.println(((double)out.getWeight(edge) / out.totalWeights()));	

	System.out.println(in);
	//System.out.println(in.getWeights().length + " " + in.getWeights()[0]);
	System.out.println("-------");
	for (int i = 0; i < in.getWeights().length; i++) {
	    System.out.println(in.getSources()[i] + " " + in.getWeights()[i] + " " + in.getSources()[i].hashCode());
	}
	System.out.println("-------");
	*/
	//see if they are different
	assert (itemsSent == itemsReceived) :
	    "Calculating steady state: items received != items send on buffer";
	
	return itemsSent;
    }
    
    public int steadyItems() 
    {
	int itemsReceived, itemsSent;
	
	//calculate the items the input trace receives
	FilterInfo next = FilterInfo.getFilterInfo(dest.getNextFilter());
	itemsSent = (int)((next.steadyMult * next.pop) *
	    ((double)dest.getWeight(this) / dest.totalWeights()));

	//calculate the items the output trace sends
	FilterInfo prev = FilterInfo.getFilterInfo((FilterTraceNode)src.getPrevious());
	itemsReceived = (int)((prev.steadyMult * prev.push) *
	    ((double)src.getWeight(this) / src.totalWeights()));

	assert (itemsSent == itemsReceived) :
	    "Calculating steady state: items received != items send on buffer";
	
	return itemsSent;
    }
    
    //total items sent over this edge in the primepump stage
    public int primePumpItems() 
    {
	return (int)((double)FilterInfo.getFilterInfo(src.getPrevFilter()).totalItemsSent(false, true) *
		     src.ratio(this));
    }
    
    //return the number of items sent to the init buffer for a edge
    //in the primepump stage
    public int primePumpInitItems() 
    {
	FilterInfo dst = FilterInfo.getFilterInfo(getDest().getNextFilter());
	
	int totalPPItemsRec = dst.totalItemsReceived(false, true);
	
	//I'm Rick James.
	return (int)((double)totalPPItemsRec * dest.ratio(this));
    }
    
}
