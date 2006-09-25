/**
 * 
 */
package at.dms.kjc.spacedynamic;

import java.util.HashMap;

import at.dms.kjc.flatgraph.FlatNode;

/**
 * This class will create switch code (but not create the file) for a 
 * layout that has no overlapping routes.  It does not really simulate the 
 * execution of the graph, it just dumps the switch code based the properties 
 * of the filters and joiners.
 * 
 * @author mgordon
 *
 */
public class SimpleSimulator extends Simulator {

    /**
     * @param layout The layout.
     * @return True if the layout is simple (meaning no overlapping routes
     * and no empty tiles involved in routing) and we can use the simple 
     * simulator to generate switch code.
     */
    public static boolean isSimple(Layout layout) {
        return layout.getIntermediateTiles().size() == 0;
    }
    

    public SimpleSimulator(SpdStaticStreamGraph ssg, JoinerSimulator joinerSimulator) 
    {
        super(ssg, joinerSimulator);
        initJoinerCode = new HashMap<FlatNode, JoinerScheduleNode>();
        steadyJoinerCode = new HashMap<FlatNode, JoinerScheduleNode>();
        //this might have to change, but probably not!
        initSchedules = new HashMap<Object, StringBuffer>();
        steadySchedules = new HashMap<Object, StringBuffer>();
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.spacedynamic.Simulator#simulate()
     */
    public void simulate() {
        assert isSimple(layout);

        //cycle thru all the tiles and generate switch code for the 
        //assigned tiles
        for (int i = 0; i < rawChip.getTotalTiles(); i++) {
            RawTile tile = rawChip.getTile(i);
            if (layout.isAssigned(tile)) {
                FlatNode node = layout.getNode(tile);
                if (node.isFilter())
                    simpleFilter(tile, node);
                else {
                    assert node.isJoiner();
                    simpleJoiner(tile, node);
                }
            }
        }
    }

    private void simpleFilter(RawTile tile, FlatNode node) {
        
    }
    
    private void simpleJoiner(RawTile tile, FlatNode node) {
        
    }
    
    /* 
     * Unnecessary for this class!
     * (non-Javadoc)
     * @see at.dms.kjc.spacedynamic.Simulator#canFire(at.dms.kjc.flatgraph.FlatNode, java.util.HashMap, at.dms.kjc.spacedynamic.SimulationCounter)
     */
    public boolean canFire(FlatNode node, HashMap<FlatNode, Integer> executionCounts,
            SimulationCounter counters) {
        // TODO Auto-generated method stub
        return false;
    }

    /* 
     * Unnecessary for this class!
     * 
     * (non-Javadoc)
     * @see at.dms.kjc.spacedynamic.Simulator#fireJoiner(at.dms.kjc.flatgraph.FlatNode, at.dms.kjc.spacedynamic.SimulationCounter, java.util.HashMap)
     */
    protected int fireJoiner(FlatNode fire, SimulationCounter counters,
            HashMap<FlatNode, Integer> executionCounts) {
        // TODO Auto-generated method stub
        return 0;
    }

}
