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
public class PopSimulator extends at.dms.util.Utils
{
    private static SimulationCounterPop counters;
        
    /* Given a flatnode with a filter as its contents, 
       generate the switch schedule
    */
    public static SwitchScheduleNode simulate(FlatNode top) 
    {
	counters = new SimulationCounterPop();
	
	SwitchScheduleNode first = new SwitchScheduleNode();
	SwitchScheduleNode current, temp;
	current = first;
	
	//check if this is the last downstream stream
	if (top.inputs == 0) {
	    return first;
	}
		
	do {
	    //Since top is guaranteed to be a filter, it has
	    //only one connection
	    simulateDataItem(top.incoming[0], current);
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
	else if (node.contents instanceof SIRSplitter) {
	    //just pass thru joiners they only have one downstream connection
	    simulateDataItem(node.incoming[0], scheduleNode);
	}
	else if (node.contents instanceof SIRJoiner) {
	    //here is the meat
	    SIRJoiner joiner = (SIRJoiner)node.contents;
	    //if Joiner send the item out to all arcs
	    if (joiner.getType() == SIRJoinType.COMBINE) {
		throw new RuntimeException("COMBINE");
	    }
	    else {
		//weighted round robin
		for (int i = 0; i < node.inputs; i++) {
		    if (counters.getCount(node, i) > 0) {
			counters.decrementCount(node, i);
			simulateDataItem(node.incoming[i], scheduleNode);
			return;
		    }
		}
		//none where greater than zero, reset all counters
		//and send to the first
		for (int i = 0; i < node.inputs; i++) {
		    counters.resetCount(node, i);
		}
		counters.decrementCount(node, 0);
		simulateDataItem(node.incoming[0], scheduleNode);
	    }
	    
	}
	else {
	    throw new RuntimeException("SimulateDataItem");
	}
	
    }
}
