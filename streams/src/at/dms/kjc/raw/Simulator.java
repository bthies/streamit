package at.dms.kjc.raw;

import java.util.HashMap;
import at.dms.kjc.flatgraph.FlatNode;

public abstract class Simulator {
    public static HashMap initSchedules;
    public static HashMap steadySchedules;
    
    public static HashMap initJoinerCode;
    public static HashMap steadyJoinerCode;
    
    public FlatNode toplevel;

    public abstract void simulate(FlatNode top);
    public abstract boolean canFire(FlatNode node, HashMap executionCounts, 
				    SimulationCounter counters);
}
