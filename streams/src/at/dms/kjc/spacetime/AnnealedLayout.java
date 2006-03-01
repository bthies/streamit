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
    private static final int ILLEGAL_COST = 10000;
    
    private RawChip rawChip;
    private SpaceTimeSchedule spaceTime;
    private LinkedList filterList;
    private Partitioner partitioner;
    
    public AnnealedLayout(SpaceTimeSchedule spaceTime) {
        super();
        this.spaceTime = spaceTime;
        rawChip = spaceTime.getRawChip();
        this.partitioner = spaceTime.partitioner;
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

    /**
     *  
     */
    public void initialPlacement() {
       //try a random layout!
        randomInitialPlacement();
    }

    
    /** 
     * The initial layout is random and probably illegal! 
     */
    private void randomInitialPlacement() {
        Iterator filters = filterList.iterator();
        
        while (filters.hasNext()) {
            FilterTraceNode filter = (FilterTraceNode)filters.next();
            
            assignment.put(filter, 
                    rawChip.getTile(getRandom(rawChip.getTotalTiles())));
        }
    }
    
    /** 
     * @return The maximum work for a tile (accounting for 
     * pipeline bubbles) plus the cost of intertrace communication.
     *  
     */
    public double placementCost(boolean debug) {
        int cost = maxTileWork() + communicationCost();
        
        if (!isLegalLayout())
            cost += ILLEGAL_COST;
        
        return cost;
    }

    /** check if the layout is legal, meaning that communicating filters of a
     * trace are placed on neighboring tiles.
     * 
     * @return
     */
    private boolean isLegalLayout() {
        Iterator filters = filterList.iterator();
        
        while (filters.hasNext()) {
            FilterTraceNode filter = (FilterTraceNode)filters.next();
            //last filter of a trace, no need to check it          
            if (filter.getNext().isOutputTrace())
                continue;
            
            RawTile myTile = (RawTile)assignment.get(filter);
            RawTile upTile = 
                (RawTile)assignment.get(filter.getNext().getAsFilter());
            if (!(rawChip.areNeighbors(myTile, upTile)))
                return false;
        }
        
        return true;
    }
    
    /**
     * @return An estimate of the cost of communication for this layout.
     */
    private int communicationCost() {
        return 0;
    }
     
    /**
     * @return The maximum amount of work that is performed on a tile
     * using a formula that accounts for startup costs of filter (pipeline lag).
     */
    private int maxTileWork() {
        int[] tileCosts = new int[rawChip.getTotalTiles()];
        
        Iterator filterNodes = assignment.keySet().iterator();
        while (filterNodes.hasNext()) {
            FilterTraceNode filterNode = (FilterTraceNode)filterNodes.next();
            //get the cost of a filter as its total work plus the pipeline lag
            //(startupcost)
            int filterCost = partitioner.getFilterWorkSteadyMult(filterNode) +
            partitioner.getFilterStartupCost(filterNode);
            //add it to the tile costs...
            tileCosts[((RawTile)assignment.get(filterNode)).getTileNumber()] += 
                filterCost;
        }
        
        //find the max
        int max = 0;
        
        for (int i = 0; i < tileCosts.length; i++) {
            if (tileCosts[i] > max)
                max = tileCosts[i];
        }
        //return the work that the max tile is estimated to do
        return max;
    }
    
    /**    
     * initalize the simulated annealing.
     */
    public void initialize() {
        //generate the startup cost of each filter...         
        partitioner.calculateStartupCosts();
        
        //create the filter list
        filterList = new LinkedList();
        Iterator traceNodes = 
            Util.traceNodeTraversal(partitioner.getTraceGraph());
        while (traceNodes.hasNext()) {
            TraceNode node = (TraceNode)traceNodes.next();
            if (node.isFilterTrace())
                filterList.add(node);
        }

    }

}
