package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph2.FilterContent;
import at.dms.util.Utils;
import java.util.HashSet;

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
    public int primePump;
    public int peek;

    public FilterTraceNode traceNode;
    public FilterContent filter;

    public FilterInfo(FilterTraceNode traceNode)
    {
	filter = traceNode.getFilter();
	this.traceNode = traceNode;
	this.steadyMult = traceNode.getSteadyMult();
	this.initMult = traceNode.getInitMult();
	this.primePump = filter.getPrimePump();
	prePeek = 0;
	prePush = 0;
	prePop = 0;
	push = filter.getPushInt();
	pop = filter.getPopInt();
	peek = filter.getPeekInt();

	if (isTwoStage()) {
	    prePeek = filter.getInitPeek();
	    prePush = filter.getInitPush();
	    prePop = filter.getInitPop();
	}
	
	
    }
    
    public boolean isTwoStage() 
    {
	return filter.isTwoStage();
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
	}
	else { //input trace node
	    InputTraceNode input = (InputTraceNode)traceNode.getPrevious();

	    //here we assume each trace has at least one filter trace node
	    ret = new FilterTraceNode[input.getSources().length];
	    for (int i = 0; i < ret.length; i++) {
		ret[i] = (FilterTraceNode)input.getSources()[i].getPrevious();
	    }
	}    
	return ret;
    }
    
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

    private int calculateRemaining() 
    {
	//the number of times this filter fires in the initialization
	//schedule
	int initFire = initMult;
	//for now assume it is a filter trace node
	FilterTraceNode[] previous = getPreviousFilters();

	//if this is not a twostage, fake it by adding to initFire,
	//so we always think the preWork is called
	//if (!(filter instanceof SIRTwoStageFilter))
	if(filter.isTwoStage())
	    initFire++;
	
	//the number of items produced by the upstream filter in
	//initialization
	int upStreamItems = 0;
	
	if (previous.length == 1) {
	    //calculate upstream items received during init  SPLITTER?
	    upStreamItems = previous[0].getFilter().getPushInt() * 
		previous[0].getInitMult();
	    if (previous[0].getFilter().isTwoStage()) {
		/*upStreamItems -= ((SIRTwoStageFilter)previous.getFilter()).getPushInt();
		  upStreamItems += ((SIRTwoStageFilter)previous.getFilter()).getInitPush();*/
		upStreamItems -= previous[0].getFilter().getPushInt();
		upStreamItems += previous[0].getFilter().getInitPush();
	    }
	}
	else if (previous.length > 1)
	    Utils.fail("Splits/Joins not supported");
	
	//see my thesis for an explanation of this calculation
	if (initFire  - 1 > 0) {
	    bottomPeek = Math.max(0, 
				  peek - (prePeek - prePop));
	}
	else
	    bottomPeek = 0;
	
	remaining = upStreamItems -
	    (prePeek + 
	     bottomPeek + 
	     Math.max((initFire - 2), 0) * pop);
	
	return remaining;
    }

    //does this filter require a receive buffer during code 
    //generation
    public boolean noBuffer() 
    {
	//always need a buffer for rate matching.
	//	    if (KjcOptions.ratematch)
	//	return false;
	
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
}
