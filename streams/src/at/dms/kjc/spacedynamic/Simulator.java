package at.dms.kjc.spacedynamic;

import java.util.HashMap;
import at.dms.kjc.flatgraph.FlatNode;

public abstract class Simulator {
    /** ComputeNode->StringBuffer, the init switch schedule for this tile **/
    public HashMap initSchedules;
    /** ComputeNode->StringBuffer, the steady switch schedule for this tile **/
    public HashMap steadySchedules;
    /** FlatNode->JoinerScheduleNode, the receiving/sending schedule for the joiner 
	for init **/
    public HashMap initJoinerCode;
    /** FlatNode->JoinerScheduleNode, the receiving/sending schedule for the joiner
     for steady **/
    public HashMap steadyJoinerCode;
    
    protected StaticStreamGraph ssg;

    protected FlatNode toplevel;

    /** joinerSimulator gives the receiving schedule for each joiner **/
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
