package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph2.FilterContent;

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
    public int peek;

    private FilterTraceNode traceNode;
    public FilterContent filter;

    public FilterInfo(FilterTraceNode traceNode)
    {
	filter = traceNode.getFilter();
	this.traceNode = traceNode;
	this.steadyMult = traceNode.getSteadyMult();
	this.initMult = traceNode.getInitMult();
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

    private int calculateRemaining() 
    {
	//the number of times this filter fires in the initialization
	//schedule
	int initFire = initMult;
	//for now assume it is a filter trace node
	FilterTraceNode previous = (FilterTraceNode)traceNode.getPrevious();

	//if this is not a twostage, fake it by adding to initFire,
	//so we always think the preWork is called
	//if (!(filter instanceof SIRTwoStageFilter))
	if(filter.isTwoStage())
	    initFire++;
	
	//the number of items produced by the upstream filter in
	//initialization
	int upStreamItems = 0;
	
	if (previous != null) {
	    //calculate upstream items received during init  SPLITTER?
	    upStreamItems = previous.getFilter().getPushInt() * 
		previous.getInitMult();
	    //if (previous.getFilter() instanceof SIRTwoStageFilter) {
	    if (previous.getFilter().isTwoStage()) {
		/*upStreamItems -= ((SIRTwoStageFilter)previous.getFilter()).getPushInt();
		  upStreamItems += ((SIRTwoStageFilter)previous.getFilter()).getInitPush();*/
		upStreamItems -= previous.getFilter().getPushInt();
		upStreamItems += previous.getFilter().getInitPush();
	    }
	}
	
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
