package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;

/**
 * This class generates a push schedule for the switch code by simulating pushes from 
 * the filter. 
 */
public class PushSimulator extends at.dms.util.Utils
{
    private static SimulationCounterPush counters;
        
    /* Given a flatnode with a filter as its contents, 
       generate the switch schedule
    */
    public static SwitchScheduleNode simulate(FlatNode top) 
    {
	counters = new SimulationCounterPush();
	
	SwitchScheduleNode first = new SwitchScheduleNode();
	SwitchScheduleNode current, temp;
	current = first;
	
	//check if this is the last downstream stream
	if (top.ways == 0) {
	    return first;
	}
		
	do {
	    //Since top is guaranteed to be a filter, it has
	    //only one connection
	    simulateDataItem(top.edges[0], current);
	    if (counters.checkAllZero())
		break;
	    temp = new SwitchScheduleNode();
	    current.next = temp;
	    current = temp;
	}while(true);
	
	return first;
    }
    

    private static void simulateDataItem (FlatNode node, SwitchScheduleNode scheduleNode) 
    {
	if (node.contents instanceof SIRFilter) {
	    scheduleNode.addDest(node);
	    return;
	}
	else if (node.contents instanceof SIRJoiner) {
	    //just pass thru joiners they only have one downstream connection
	    simulateDataItem(node.edges[0], scheduleNode);
	}
	else if (node.contents instanceof SIRSplitter) {
	    //here is the meat
	    SIRSplitter splitter = (SIRSplitter)node.contents;
	    //if splitter send the item out to all arcs
	    if (splitter.getType() == SIRSplitType.DUPLICATE) {
		for (int i = 0; i < node.ways;i++) {
		    //decrement counter on arc
		    if (counters.getCount(node, i) == 0)
			counters.resetCount(node, i);
		    counters.decrementCount(node, i);
		    simulateDataItem(node.edges[i], scheduleNode);
		}
	    }
	    else {
		//weighted round robin
		for (int i = 0; i < node.ways; i++) {
		    if (counters.getCount(node, i) > 0) {
			counters.decrementCount(node, i);
			simulateDataItem(node.edges[i], scheduleNode);
			return;
		    }
		}
		//none where greater than zero, reset all counters
		//and send to the first
		for (int i = 0; i < node.ways; i++) {
		    counters.resetCount(node, i);
		}
		counters.decrementCount(node, 0);
		simulateDataItem(node.edges[0], scheduleNode);
	    }
	    
	}
	else {
	    throw new RuntimeException("SimulateDataItem");
	}
	
    }

    private static int LCM(int a, int b) {
	return 0;
	
    }
    
}
