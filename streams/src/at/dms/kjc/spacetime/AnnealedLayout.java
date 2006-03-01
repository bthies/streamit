/**
 * 
 */
package at.dms.kjc.spacetime;

import at.dms.kjc.common.SimulatedAnnealing;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * @author mgordon
 *
 */
public class AnnealedLayout extends SimulatedAnnealing {
    
    private RawChip rawChip;
    private SpaceTimeSchedule spaceTime;
    private LinkedList filterList;
    
    public AnnealedLayout(SpaceTimeSchedule spaceTime) {
        this.spaceTime = spaceTime;
        rawChip = spaceTime.getRawChip();
    }

    public void run() {
        //run the simulated annealing with 2 iterations, 
        //100 attempts per iteration
        simAnnealAssign(2, 100);
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.common.SimulatedAnnealing#swapAssignment()
     */
    public void swapAssignment() {
        //randomly find a filter
        FilterTraceNode filter = 
            (FilterTraceNode)filterList.get(getRandom(filterList.size()));
        
        //swap it to a random tile
        RawTile oldTile = (RawTile)assignment.get(filter);
        RawTile newTile = null;
        
        //find a new tile to swap it to...
        while (true) {
            newTile = 
                rawChip.getTile(getRandom(rawChip.getTotalTiles()));
            if (newTile == oldTile)
                continue;
            else
                break;
        }
        //commit the new assignment
        assignment.remove(filter);
        assignment.put(filter, newTile);
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.common.SimulatedAnnealing#initialPlacement()
     */
    public void initialPlacement() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see at.dms.kjc.common.SimulatedAnnealing#placementCost(boolean)
     */
    public double placementCost(boolean debug) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.common.SimulatedAnnealing#initialize()
     */
    public void initialize() {
        // TODO Auto-generated method stub
        
        //create the filter list
        filterList = new LinkedList();
        Iterator traceNodes = 
            Util.traceNodeTraversal(spaceTime.partitioner.getTraceGraph());
        while (traceNodes.hasNext()) {
            TraceNode node = (TraceNode)traceNodes.next();
            if (node.isFilterTrace())
                filterList.add(node);
        }
        
        
        
    }

}
