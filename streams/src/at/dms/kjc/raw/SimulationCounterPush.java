package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;

/** 
 * This class keeps the counters for weights of the splitter/joiners
 * and performs the test to check whether the simulation is finished 
 */
public class SimulationCounterPush {
    
    private HashMap counts;
    
    public SimulationCounterPush() {
	counts = new HashMap();
    }


    public int getCount(FlatNode node, int way) {
	/* Create counters in the hashmap if this node has not
	   been visited already 
	*/
	if (!counts.containsKey(node)) {
	    int[] nodeCounters = new int[node.ways];
	    for (int i = 0; i < node.ways; i++) {
		nodeCounters[i] = node.weights[i];
	    }
	    counts.put(node, nodeCounters);
	}
	//Get the counter and return the count for the given way
	int[] currentCounts = (int[])counts.get(node);
	return currentCounts[way];
    }
    
    public void decrementCount(FlatNode node, int way) 
    {
	int[] currentCounts = (int[])counts.get(node);
	if (currentCounts[way] > 0)
	    currentCounts[way]--;
	else 
	    System.err.println("Trying to decrement a way with a zero count.");
	
	counts.put(node, currentCounts);
    }
    
    public void resetCount(FlatNode node, int way) 
    {
	int[] currentCounts = (int[])counts.get(node);
	if (currentCounts[way] == 0)
	    currentCounts[way] = node.weights[way];
	else
	    System.err.println("Trying to reset a non-zero counter.");
	counts.put(node, currentCounts);
    }
    
    public boolean checkAllZero() 
    {
	Iterator allCounts = counts.values().iterator();
	while (allCounts.hasNext()) {
	    int[] currentCounters = (int[])allCounts.next();
	    for (int i = 0; i < currentCounters.length; i++) {
		if (currentCounters[i] > 0) 
		    return false;
	    }
	}
	return true;
    }
    
}

