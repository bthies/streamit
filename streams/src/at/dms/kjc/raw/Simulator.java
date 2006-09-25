package at.dms.kjc.raw;

import java.util.HashMap;
import at.dms.kjc.flatgraph.FlatNode;

public abstract class Simulator {
    public static HashMap<Object, StringBuffer> initSchedules;
    public static HashMap<Object, StringBuffer> steadySchedules;
    
    public static HashMap<FlatNode, JoinerScheduleNode> initJoinerCode;
    public static HashMap<FlatNode, JoinerScheduleNode> steadyJoinerCode;
    
    public FlatNode toplevel;

    public abstract void simulate(FlatNode top);
    public abstract boolean canFire(FlatNode node, HashMap<FlatNode, Integer> executionCounts, 
                                    SimulationCounter counters);
}
