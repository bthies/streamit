package at.dms.kjc.spacedynamic;

import java.util.HashMap;
import at.dms.kjc.flatgraph.FlatNode;

public abstract class Simulator {
    public HashMap initSchedules;
    public HashMap steadySchedules;
    
    public HashMap initJoinerCode;
    public HashMap steadyJoinerCode;
    
    protected StaticStreamGraph ssg;

    protected FlatNode toplevel;

    protected JoinerSimulator joinerSimulator;
    

    public Simulator(StaticStreamGraph ssg, JoinerSimulator joinerSimulator) 
    {
	this.ssg = ssg;
	this.joinerSimulator = joinerSimulator;
	this.toplevel = ssg.getTopLevel();
    }

    public abstract void simulate();
    public abstract boolean canFire(FlatNode node, HashMap executionCounts, 
				    SimulationCounter counters);
}
