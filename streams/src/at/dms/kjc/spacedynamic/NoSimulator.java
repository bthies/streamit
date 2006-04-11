/**
 * 
 */
package at.dms.kjc.spacedynamic;

import java.util.HashMap;

import at.dms.kjc.flatgraph.FlatNode;

/**
 * @author mgordon
 *
 */
public class NoSimulator extends Simulator {

    /* (non-Javadoc)
     * @see at.dms.kjc.spacedynamic.Simulator#simulate()
     */
  
    public NoSimulator(StaticStreamGraph ssg, JoinerSimulator joinerSimulator) 
    {
        super(ssg, joinerSimulator);
        initJoinerCode = new HashMap();
        steadyJoinerCode = new HashMap();
        //this might have to change, but probably not!
        initSchedules = new HashMap();
        steadySchedules = new HashMap();
    }
    
        /**
         * don't do anything
         */
    public void simulate() {
       
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.spacedynamic.Simulator#canFire(at.dms.kjc.flatgraph.FlatNode, java.util.HashMap, at.dms.kjc.spacedynamic.SimulationCounter)
     */
    public boolean canFire(FlatNode node, HashMap executionCounts,
            SimulationCounter counters) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.spacedynamic.Simulator#fireJoiner(at.dms.kjc.flatgraph.FlatNode, at.dms.kjc.spacedynamic.SimulationCounter, java.util.HashMap)
     */
    protected int fireJoiner(FlatNode fire, SimulationCounter counters,
            HashMap executionCounts) {
        // TODO Auto-generated method stub
        return 0;
    }

}
