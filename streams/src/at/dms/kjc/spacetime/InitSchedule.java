package at.dms.kjc.spacetime;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import at.dms.util.Utils;

/** This class generates the init schedule, the execution order as 
    given by the flow of the graph 
**/

public class InitSchedule 
{
    public static LinkedList getInitSchedule(Trace[] topTraces) 
    {
	//BFS
	LinkedList schedule = new LinkedList();
	HashSet visited = new HashSet();
	LinkedList queue = new LinkedList();
	for (int i = 0; i < topTraces.length; i++) {
	    queue.add(topTraces[i]);
	    while (!queue.isEmpty()) {
		Trace trace = (Trace)queue.removeFirst();
		if (!visited.contains(trace)) {
		    System.out.println("Adding " + trace + " to init schedule.");
		    schedule.add(trace);
		    visited.add(trace);
		    Iterator dests = trace.getTail().getDestSet().iterator();
		    while (dests.hasNext()) {
			Trace current = ((Edge)dests.next()).getDest().getParent();
			if (!visited.contains(current))
			    queue.add(current);
		    }
		}
	    }
	}
	
	return schedule;
    }
}
