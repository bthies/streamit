package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import at.dms.util.Utils;

/** 
 * This class keeps the counters for weights of the splitter/joiners
 * and performs the test to check whether the simulation is finished 
 */
public class SimulationCounter {
    
    public static HashMap maxJoinerBufferSize;
    
    private HashMap arcCountsIncoming;
    private HashMap arcCountsOutgoing;
    
    private HashMap bufferCount;
    private HashSet fired;

    private HashMap currentJoinerScheduleNode;

    private HashMap joinerBufferCounts;
    
    private HashMap joinerReceiveBuffer;
    
    static 
    {
	maxJoinerBufferSize = new HashMap();
    }
    
    public SimulationCounter(HashMap joinerSchedules) {
	joinerReceiveBuffer = new HashMap();
	arcCountsIncoming = new HashMap();
	arcCountsOutgoing = new HashMap();
	bufferCount = new HashMap();
	fired = new HashSet();
	currentJoinerScheduleNode = (HashMap)joinerSchedules.clone();
	joinerBufferCounts = new HashMap();
    }

    public String getJoinerReceiveBuffer(FlatNode node) {
	LinkedList list = (LinkedList)joinerReceiveBuffer.get(node);
	if (list == null)
	    Utils.fail("trying to pop an empty joiner receive buffer");
	return (String)list.removeFirst();
    }
    
    public void addJoinerReceiveBuffer(FlatNode node, String buffer) 
    {
	LinkedList list = (LinkedList)joinerReceiveBuffer.get(node);
	if (list == null) {
	    list = new LinkedList();
	    joinerReceiveBuffer.put(node, list);
	}
	list.add(buffer);
    }
    

    public String getJoinerBuffer(FlatNode node) 
    {
	return ((JoinerScheduleNode)currentJoinerScheduleNode.get(node)).
	    buffer;
    }
    
    public void incrementJoinerSchedule(FlatNode node) 
    {
	currentJoinerScheduleNode.put(node, ((JoinerScheduleNode)
				       currentJoinerScheduleNode.get(node)).
				      next);
    }
    
    public boolean hasFired(FlatNode node) {
	return fired.contains(node);
    }
    
    public void setFired(FlatNode node) {
	fired.add(node);
    }

    public void resetBuffers() 
    {
	Iterator it = bufferCount.keySet().iterator();
	while (it.hasNext()) {
	    FlatNode node = (FlatNode)it.next();
	    if (node.contents instanceof SIRFilter) {
		SIRFilter filter = (SIRFilter)node.contents;
		bufferCount.put(node,
				new Integer(filter.getPeekInt() - filter.getPopInt()));
	    }
	}
	
    }
    
    public int getBufferCount(FlatNode node) {
	if (!bufferCount.containsKey(node))
	    bufferCount.put(node, new Integer(0));
	return ((Integer)bufferCount.get(node)).intValue();
    }
    
    public void decrementBufferCount(FlatNode node, int val) {
	if (!bufferCount.containsKey(node))
	    Utils.fail("Cannot decrement a unseen buffer");
	if (val > getBufferCount(node))
	    Utils.fail("Cannot decrement a buffer more than it holds");
	bufferCount.put(node, 
			new Integer(getBufferCount(node) - val));
    }

    public void incrementBufferCount(FlatNode node) {
	if (!bufferCount.containsKey(node))
	    bufferCount.put(node, new Integer(0));
	bufferCount.put(node, 
			new Integer (getBufferCount(node) + 1));
    }

    
    public int getJoinerBufferCount(FlatNode node, String buf) 
    {
	if (!joinerBufferCounts.containsKey(node))
	    joinerBufferCounts.put(node, new HashMap());
	HashMap joinerBuffers = (HashMap)joinerBufferCounts.get(node);
	if (!joinerBuffers.containsKey(buf))
	    joinerBuffers.put(buf, new Integer(0));
	return ((Integer)joinerBuffers.get(buf)).intValue();
    }
    
    public void decrementJoinerBufferCount(FlatNode node, String buf) 
    {
	int old = getJoinerBufferCount(node, buf);
	if (old  <= 0)
	    Utils.fail("attempting to decrement a 0 joiner buffer");
	((HashMap)joinerBufferCounts.get(node)).put(buf, new Integer(old - 1));
    }
    
    public void incrementJoinerBufferCount(FlatNode node, String buf) 
    {
	int old = getJoinerBufferCount(node, buf);
	((HashMap)joinerBufferCounts.get(node)).put(buf, new Integer(old + 1));
	//record max buffer size
	if (!maxJoinerBufferSize.containsKey(node))
	    maxJoinerBufferSize.put(node, new Integer(1));
	else {
	    int max =  ((Integer)maxJoinerBufferSize.get(node)).intValue();
	    if (old + 1 > max)
		maxJoinerBufferSize.put(node, new Integer(old + 1));
	}
    }
    

    public int getArcCountOutgoing(FlatNode node, int way) {
	/* Create counters in the hashmap if this node has not
	   been visited already 
	*/
	if (!arcCountsOutgoing.containsKey(node)) {
	    int[] nodeCounters = new int[node.ways];
	    for (int i = 0; i < node.ways; i++) {
		nodeCounters[i] = node.weights[i];
	    }
	    arcCountsOutgoing.put(node, nodeCounters);
	}
	//Get the counter and return the count for the given way
	int[] currentArcCounts = (int[])arcCountsOutgoing.get(node);
	return currentArcCounts[way];
    }
    
    public void decrementArcCountOutgoing(FlatNode node, int way) 
    {
	int[] currentArcCounts = (int[])arcCountsOutgoing.get(node);
	if (currentArcCounts[way] > 0)
	    currentArcCounts[way]--;
	else 
	    System.err.println("Trying to decrement a way with a zero count.");
	
	arcCountsOutgoing.put(node, currentArcCounts);
    }
    
    public void resetArcCountOutgoing(FlatNode node, int way) 
    {
	int[] currentArcCounts = (int[])arcCountsOutgoing.get(node);
	if (currentArcCounts[way] == 0)
	    currentArcCounts[way] = node.weights[way];
	else
	    System.err.println("Trying to reset a non-zero counter.");
	arcCountsOutgoing.put(node, currentArcCounts);
    }
}


