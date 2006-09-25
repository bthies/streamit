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
  
    public NoSimulator(SpdStaticStreamGraph ssg, JoinerSimulator joinerSimulator) 
    {
        super(ssg, joinerSimulator);
        initJoinerCode = new HashMap<FlatNode, JoinerScheduleNode>();
        steadyJoinerCode = new HashMap<FlatNode, JoinerScheduleNode>();
        //this might have to change, but probably not!
        initSchedules = new HashMap<Object, StringBuffer>();
        steadySchedules = new HashMap<Object, StringBuffer>();
    }
    
        /**
         * don't do anything
         */
    public void simulate() {
       
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.spacedynamic.Simulator#canFire(at.dms.kjc.flatgraph.FlatNode, java.util.HashMap, at.dms.kjc.spacedynamic.SimulationCounter)
     */
    public boolean canFire(FlatNode node, HashMap<FlatNode, Integer> executionCounts,
            SimulationCounter counters) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.spacedynamic.Simulator#fireJoiner(at.dms.kjc.flatgraph.FlatNode, at.dms.kjc.spacedynamic.SimulationCounter, java.util.HashMap)
     */
    protected int fireJoiner(FlatNode fire, SimulationCounter counters,
            HashMap<FlatNode, Integer> executionCounts) {
        // TODO Auto-generated method stub
        return 0;
    }

}
