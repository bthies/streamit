package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph2.FilterContent;
import at.dms.util.Utils;

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
	    /*prePeek = ((SIRTwoStageFilter)filter).getInitPeek();
	      prePush = ((SIRTwoStageFilter)filter).getInitPush();
	      prePop = ((SIRTwoStageFilter)filter).getInitPop();*/
	    prePeek = filter.getInitPeek();
	    prePush = filter.getInitPush();
	    prePop = filter.getInitPop();
	}
	
	
    }
    
    public boolean isTwoStage() 
    {
	//return (filter instanceof SIRTwoStageFilter);
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
}
