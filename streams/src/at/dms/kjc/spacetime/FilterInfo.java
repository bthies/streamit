package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph2.FilterContent;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;

/** 
    A class to hold all the various information for a filter
 */
public class FilterInfo 
{
    public int prePeek;
    public int prePop;
    public int prePush;
    public int remaining;
    public int bottomPeek;
    public int initMult;
    public int steadyMult;
    public int push;
    public int pop;
    //so here primepump is the number of times
    //the filter executes in the primepump stage
    public int primePump;
    public int peek;

    private boolean linear;
    private boolean direct;

    public FilterTraceNode traceNode;
    public FilterContent filter;

    private static HashMap filterInfos;
    
    static 
    {
	filterInfos = new HashMap();
    }
    

    public static FilterInfo getFilterInfo(FilterTraceNode traceNode) 
    {
	if (!filterInfos.containsKey(traceNode)) {
	    FilterInfo info = new FilterInfo(traceNode);
	    filterInfos.put(traceNode, info);
	    return info;
	}
	else
	    return (FilterInfo)filterInfos.get(traceNode);
    }
    

    private FilterInfo(FilterTraceNode traceNode)
    {
	filter = traceNode.getFilter();
	this.traceNode = traceNode;
	this.steadyMult = filter.getSteadyMult();
	this.initMult = filter.getInitMult();
	//multiply the primepump number by the
	//steady state multiplicity to get the true 
	//primepump multiplicity
	this.primePump = traceNode.getParent().getPrimePump() * this.steadyMult;
	prePeek = 0;
	prePush = 0;
	prePop = 0;
	linear=filter.isLinear();
	if(linear) {
	    peek=filter.getArray().length;
	    push=1;
	    pop=filter.getPopCount();
	}
	else if (traceNode.isFileInput()) {
	    push = 1;
	    pop = 0;
	    peek = 0;
	}
	else if (traceNode.isFileOutput()) {
	    push = 0;
	    pop = 1;
	    peek = 0;
	}
	else {
	    direct = DirectCommunication.testDC(this);
	    push = filter.getPushInt();
	    pop = filter.getPopInt();
	    peek = filter.getPeekInt();
	    if (isTwoStage()) {
		prePeek = filter.getInitPeek();
		prePush = filter.getInitPush();
		prePop = filter.getInitPop();
	    } 
	}
	calculateRemaining();
    }
    
    public boolean isTwoStage() 
    {
	return filter.isTwoStage();
    }

    private int calculateRemaining() 
    {
	//the number of times this filter fires in the initialization
	//schedule
	int initFire = initMult;

	//if this is not a twostage, fake it by adding to initFire,
	//so we always think the preWork is called
	//if (!(filter instanceof SIRTwoStageFilter))
	if(filter.isTwoStage())
	    initFire++;
	
	//see my thesis for an explanation of this calculation
	if (initFire  - 1 > 0) {
	    bottomPeek = Math.max(0, 
				  peek - (prePeek - prePop));
	}
	else
	    bottomPeek = 0;
	

	remaining = initItemsReceived() -
	    (prePeek + 
	     bottomPeek + 
	     Math.max((initFire - 2), 0) * pop);
	
	return remaining;
    }

    //returns true if this filter does not need a receive buffer
    public boolean isLinear() {
	return linear;
    }

    //returns true if this filter does not need a receive buffer
    //but it does receive items
    public boolean isDirect() 
    {
	return direct;
    }
    

    //does this filter require a receive buffer during code 
    //generation
    public boolean noBuffer() 
    {
	if (peek == 0 &&
	    prePeek == 0)
	    return true;
	return false;		
    }

    //can we use a simple (non-circular) receive buffer for this filter
    public boolean isSimple()
    {
 	if (noBuffer())
 	    return false;
	
 	if (peek == pop &&
 	    remaining == 0 &&
 	    (prePop == prePeek))
 	    return true;
 	return false;
    }

    //calculate the number of items produced in the primepump stage but
    //consumed in the steady state of the down stream filters...
    public int primePumpItemsNotConsumed() 
    {
	assert traceNode.getNext() instanceof OutputTraceNode :
	    "Need to call primePumpItemsNotConsumed() on last filter of trace";
	OutputTraceNode out = (OutputTraceNode)traceNode.getNext();
	int itemsSent = primePump * push;
	int itemsConsumed = 0;
	    
	Iterator it = out.getDestSet().iterator();
	while (it.hasNext()) {
	    Edge edge = (Edge)it.next();
	    FilterInfo downstream = 
		FilterInfo.getFilterInfo(((FilterTraceNode)edge.getDest().getNext()));
	    itemsConsumed += (downstream.primePump * downstream.pop);
	}
	
	assert ((itemsSent - itemsConsumed) % push == 0) :
	    "primepump items not consumed is not multiple of push!";
	return itemsSent - itemsConsumed;
    }
    

    //return the number of items produced in the init stage
    public int initItemsSent() 
    {
	int items = push * initMult;
	if (isTwoStage()) {
	    /*upStreamItems -= ((SIRTwoStageFilter)previous.getFilter()).getPushInt();
	      upStreamItems += ((SIRTwoStageFilter)previous.getFilter()).getInitPush();*/
	    items -= push;
	    items += prePush;
	}	
	return items;
    }
    
    //return the number of items received in the init stage including
    //the remaining items on the tape that are not consumed in the 
    //schedule
    public int initItemsReceived() 
    {
	//the number of items produced by the upstream filter in
	//initialization
	int upStreamItems = 0;
	
	if (traceNode.getPrevious().isFilterTrace()) {
	    upStreamItems = 
		FilterInfo.getFilterInfo((FilterTraceNode)traceNode.getPrevious()).initItemsSent();
	}
	else { //previous is an input trace
	    InputTraceNode in = (InputTraceNode)traceNode.getPrevious();
	    
	    //add all the upstream filters items that reach this filter
	    for (int i = 0; i < in.getWeights().length; i++) {
		Edge incoming = in.getSources()[i];
		upStreamItems +=
		    (int)(FilterInfo.getFilterInfo((FilterTraceNode)incoming.getSrc().getPrevious()).initItemsSent() *
			  ((double)incoming.getSrc().getWeight(incoming) / incoming.getSrc().totalWeights()));
		//upStreamItems += (int)(FilterInfo.getFilterInfo(previous[i]).initItemsSent() *
		//   ((double)out.getWeight(in) / out.totalWeights()));
	    }
	}
	return upStreamItems;
    }

    public int totalItemsReceived(boolean init, boolean primepump) 
    {
	assert !((init) && (init && primepump)) :
	    "incorrect usage";
	int items = 0;
	//get the number of items received
	if (init) 
	    items = initItemsReceived();
	else if (primepump) 
	    items = primePump * pop;
	else
	    items = steadyMult * pop;
	
	return items;
    }
    
    public int totalItemsSent(boolean init, boolean primepump) 
    {
	assert !((init) && (init && primepump)) :
	    "incorrect usage";
	int items = 0;
	if (init) 
	    items = initItemsSent();
	else if (primepump) 
	    items = primePump * push;
	else
	    items = steadyMult * push;
	return items;
    }

    public int itemsFiring(int exeCount, boolean init) 
    {
	int items = push;
	
	if (init && exeCount == 0 && isTwoStage())
	    items = prePush;
	
	return items;
    }
    

    public int itemsNeededToFire(int exeCount, boolean init) 
    {
	int items = pop;
	
	//if we and this is the first execution we need either peek or initPeek
	if (init && exeCount == 0) {
	    if (isTwoStage())
		items = prePeek;
	    else
		items = peek;
	}
	
	return items;
    }

    public String toString() 
    {
	return traceNode.toString();
    }
    

/*  Not needed now, but needed for magic crap
      public FilterTraceNode[] getNextFilters() 
    {
	FilterTraceNode[] ret;
	
	if (traceNode.getNext() == null) 
	    return new FilterTraceNode[0];
	else if (traceNode.getNext().isFilterTrace()) {
	    ret = new FilterTraceNode[1];
	    ret[0] = (FilterTraceNode)traceNode.getNext();
	}
	else { //output trace node
	    HashSet set = new HashSet();
	    OutputTraceNode output = (OutputTraceNode)traceNode.getNext();
	    for (int i = 0; i < output.getDests().length; i++) 
		for (int j = 0; j < output.getDests()[i].length; j++)
		    set.add(output.getDests()[i][j].getNext());
	    ret = (FilterTraceNode[])set.toArray(new FilterTraceNode[0]);
	}
	return ret;
    }


     //for the filter trace node, get all upstream filter trace nodes,
    //going thru input and output trace nodes
    public FilterTraceNode[] getPreviousFilters() 
    {
	FilterTraceNode[] ret;
	
	if (traceNode.getPrevious() == null)
	    return new FilterTraceNode[0];
	
	if (traceNode.getPrevious().isFilterTrace()) {
	    ret = new FilterTraceNode[1];
	    ret[0] = (FilterTraceNode)traceNode.getPrevious();
	} else { //input trace node
	    InputTraceNode input = (InputTraceNode)traceNode.getPrevious();
	    
	    //here we assume each trace has at least one filter trace node
	    ret = new FilterTraceNode[input.getSources().length];
	    for (int i = 0; i < ret.length; i++) {
		ret[i] = (FilterTraceNode)input.getSources()[i].getSrc().getPrevious();
	    }
	}    
	return ret;
    }
*/
}
