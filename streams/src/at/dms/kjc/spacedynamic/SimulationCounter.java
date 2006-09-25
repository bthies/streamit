package at.dms.kjc.spacedynamic;

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
    
    public static HashMap<FlatNode, Integer> maxJoinerBufferSize;
    
    private HashMap arcCountsIncoming;
    private HashMap<FlatNode, int[]> arcCountsOutgoing;
    
    private HashMap<FlatNode, Integer> bufferCount;
    private HashSet<FlatNode> fired;

    private HashMap<FlatNode, JoinerScheduleNode> currentJoinerScheduleNode;

    private HashMap<FlatNode, HashMap> joinerBufferCounts;
    
    private HashMap<FlatNode, LinkedList<String>> joinerReceiveBuffer;
    
    static 
    {
        maxJoinerBufferSize = new HashMap<FlatNode, Integer>();
    }
    
    public SimulationCounter(HashMap<FlatNode, JoinerScheduleNode> joinerSchedules) {
        joinerReceiveBuffer = new HashMap<FlatNode, LinkedList<String>>();
        arcCountsIncoming = new HashMap();
        arcCountsOutgoing = new HashMap<FlatNode, int[]>();
        bufferCount = new HashMap<FlatNode, Integer>();
        fired = new HashSet<FlatNode>();
        currentJoinerScheduleNode = (HashMap<FlatNode, JoinerScheduleNode>)joinerSchedules.clone();
        joinerBufferCounts = new HashMap<FlatNode, HashMap>();
    }

    public String getJoinerReceiveBuffer(FlatNode node) {
        LinkedList list = joinerReceiveBuffer.get(node);
        if (list == null)
            Utils.fail("trying to pop an empty joiner receive buffer");
        return (String)list.removeFirst();
    }
    
    public void addJoinerReceiveBuffer(FlatNode node, String buffer) 
    {
        LinkedList<String> list = joinerReceiveBuffer.get(node);
        if (list == null) {
            list = new LinkedList<String>();
            joinerReceiveBuffer.put(node, list);
        }
        list.add(buffer);
    }
    

    public String getJoinerBuffer(FlatNode node) 
    {
        return currentJoinerScheduleNode.get(node).
            buffer;
    }
    
    public void incrementJoinerSchedule(FlatNode node) 
    {
        currentJoinerScheduleNode.put(node, currentJoinerScheduleNode.get(node).
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
        Iterator<FlatNode> it = bufferCount.keySet().iterator();
        while (it.hasNext()) {
            FlatNode node = it.next();
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
        return bufferCount.get(node).intValue();
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
        HashMap<String, Integer> joinerBuffers = joinerBufferCounts.get(node);
        if (!joinerBuffers.containsKey(buf))
            joinerBuffers.put(buf, new Integer(0));
        return joinerBuffers.get(buf).intValue();
    }
    
    public void decrementJoinerBufferCount(FlatNode node, String buf) 
    {
        int old = getJoinerBufferCount(node, buf);
        if (old  <= 0)
            Utils.fail("attempting to decrement a 0 joiner buffer");
        joinerBufferCounts.get(node).put(buf, new Integer(old - 1));
    }
    
    public void incrementJoinerBufferCount(FlatNode node, String buf) 
    {
        int old = getJoinerBufferCount(node, buf);
        joinerBufferCounts.get(node).put(buf, new Integer(old + 1));
        //record max buffer size
        if (!maxJoinerBufferSize.containsKey(node))
            maxJoinerBufferSize.put(node, new Integer(1));
        else {
            int max =  maxJoinerBufferSize.get(node).intValue();
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
        int[] currentArcCounts = arcCountsOutgoing.get(node);
        return currentArcCounts[way];
    }
    
    public void decrementArcCountOutgoing(FlatNode node, int way) 
    {
        int[] currentArcCounts = arcCountsOutgoing.get(node);
        if (currentArcCounts[way] > 0)
            currentArcCounts[way]--;
        else 
            System.err.println("Trying to decrement a way with a zero count.");
    
        arcCountsOutgoing.put(node, currentArcCounts);
    }
    
    public void resetArcCountOutgoing(FlatNode node, int way) 
    {
        int[] currentArcCounts = arcCountsOutgoing.get(node);
        if (currentArcCounts[way] == 0)
            currentArcCounts[way] = node.weights[way];
        else
            System.err.println("Trying to reset a non-zero counter.");
        arcCountsOutgoing.put(node, currentArcCounts);
    }
}


