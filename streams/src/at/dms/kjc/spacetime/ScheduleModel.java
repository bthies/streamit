/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;

/**
 * This class models the calculated schedule and layout using the 
 * work estimation for each filter of each slice.
 * 
 * @author mgordon
 *
 */
public class ScheduleModel {
    /** the cost of outputing one item using the gdn versus using the static net
     for the final filter of a Trace */
    public static final int GDN_PUSH_COST = 2;
    /** the cost of issue a read or a write dram command on the tiles assigned 
     * to trace enpoints for each trace.
     */
    public static final int DRAM_ISSUE_COST = 5;
    private Layout layout;
    private LinkedList scheduleOrder;
    private RawChip rawChip;
    private SpaceTimeSchedule spaceTime;
    /** array of total work estimation for each tile including blocking*/
    private int[] tileCosts;
    /** Map of filter to start time on the tile they are assigned */
    private HashMap<FilterTraceNode, Integer> startTime;
    /** Map if filter to end time on the tile they are assigned */
    private HashMap<FilterTraceNode, Integer> endTime;
    /** the tile with the most work */
    private int bottleNeckTile;
    /** the amount of work for the bottleneck tile */
    private int bottleNeckCost;
    
    public ScheduleModel(SpaceTimeSchedule spaceTime, Layout layout, 
            LinkedList scheduleOrder) {
        this.layout = layout;
        this.spaceTime = spaceTime;
        this.rawChip = spaceTime.getRawChip();
        this.scheduleOrder = scheduleOrder;
        startTime = new HashMap<FilterTraceNode, Integer>();
        endTime = new HashMap<FilterTraceNode, Integer>(); 
    }
    
    /** 
     * Retrieve the array that stores the estimation of the work 
     * performed by each tile.
     *  
     * @return the array that stores the estimation of the work 
     * performed by each tile.
     */
    public int[] getTileCosts() {
        assert tileCosts != null;
        
        return tileCosts;
    }
    
    /**
     * Return the time estimate for when this filter will begin executing
     * in the steady state on the tile to which is it assigned.
     * 
     * @param node The filter.
     * 
     * @return the time estimate for when this filter will begin executing
     * in the steady state on the tile to which is it assigned.
     */
    public int getFilterStart(FilterTraceNode node) {
        System.out.println(node);
        return startTime.get(node).intValue();
    }
    
    /**
     * Return the time estimate for when this filter is finished executing
     * its entire steady state on the tile to which it is assigned.
     *  
     * @param node The node.
     * @return the time estimate for when this filter is finished executing
     * its entire steady state on the tile to which it is assigned.
     */
    public int getFilterEnd(FilterTraceNode node) {
        return endTime.get(node).intValue();
    }
    
    /**
     * Return the work estimate for the entire SS of the tile
     * that performs the most work. 
     * 
     * @return the work estimate for the entire SS of the tile
     * that performs the most work. 
     */
    public int getBottleNeckCost() {
        return bottleNeckCost;
    }
    
    /**
     * Return the tile number of the tile that performs the most work.
     * 
     * @return the tile number of the tile that performs the most work.
     */
    public int getBottleNeckTile() {
        return bottleNeckTile;
    }
    
    /**
     * Calculate the amount of work that is performed by each
     * tile in the steady-state.  This calculation will use the work
     * estimation for each filter and will account for pipeline lag inside 
     * of a slice.
     */
    public void createModel() {
        tileCosts = new int[rawChip.getTotalTiles()];
        Iterator traces = scheduleOrder.iterator();
        
        while (traces.hasNext()) {
            Trace trace = (Trace)traces.next();
             
            //don't do anything for predefined filters...
            if (trace.getHead().getNextFilter().isPredefined()) 
                continue;
            //find the bottleneck filter based on the filter work estimates
            //and the tile avail for each filter
            FilterTraceNode bottleNeck = null;
            int maxAvail = -1;
            int prevStart = 0;
            int bottleNeckStart = 0;
            
            assert trace.getFilterNodes().length == 
                trace.getNumFilters();
            
         //   System.out.println("Scheduling: " + trace);
            
            
         //   System.out.println("Finding bottleNeck: ");
            for (int f = 0; f < trace.getFilterNodes().length; f++) {
                FilterTraceNode current = trace.getFilterNodes()[f];
                
                RawTile tile = layout.getTile(current);
                int currentStart =  Math.max(tileCosts[tile.getTileNumber()], 
                        prevStart + spaceTime.partitioner.getFilterStartupCost(current));
                int tileAvail = spaceTime.partitioner.getFilterWorkSteadyMult(current) +
                   currentStart;
                //System.out.println("Trying " + current + " start: " + currentStart +
                 //       " end: " + tileAvail);
                //System.out.println("Checking start of " + current + " on " + tile + 
                //        "start: " + currentStart + ", tile avail: " + tileAvail);
                
                //remember the start time
                startTime.put(current, new Integer(currentStart));
                
                if (tileAvail > maxAvail) {
                    maxAvail = tileAvail;
                    bottleNeck = current;
                    bottleNeckStart = currentStart;
                }
                prevStart = currentStart;
            }
                
            assert bottleNeck != null : "Could not find bottleneck for " + trace ;
                
            RawTile bottleNeckTile = layout.getTile(bottleNeck);
            
            //System.out.println("Found bottleneck: " + bottleNeck + " " + bottleNeckTile);
            
            if (bottleNeck.getPrevious().isInputTrace()) {
                tileCosts[bottleNeckTile.getTileNumber()]+= DRAM_ISSUE_COST;
            }
            if (bottleNeck.getNext().isOutputTrace()) {
                //account for the
                //cost of sending an item over the gdn if it uses it...
                if (LogicalDramTileMapping.mustUseGdn(bottleNeckTile)) {
                    tileCosts[bottleNeckTile.getTileNumber()] += (bottleNeck.getFilter().getPushInt() * 
                            bottleNeck.getFilter().getSteadyMult() * 
                            GDN_PUSH_COST);
                }
            }
            
           
            
            //calculate when the bottle neck tile will finish, 
            //and base everything off of that, traversing backward and 
            //foward in the trace
            tileCosts[bottleNeckTile.getTileNumber()] = 
                (bottleNeckStart +
                spaceTime.partitioner.getFilterWorkSteadyMult(bottleNeck));
            
                        
            //System.out.println("Setting bottleneck finish: " + bottleNeck + " " + 
             //       tileCosts[bottleNeckTile.getTileNumber()]);
            
            int nextFinish = tileCosts[bottleNeckTile.getTileNumber()];
            int next1Iter = spaceTime.partitioner.getWorkEstOneFiring(bottleNeck);
            TraceNode current = bottleNeck.getPrevious();
            //record the end time for the bottleneck filter
            endTime.put(bottleNeck, new Integer(nextFinish));
            
            //traverse backwards and set the finish times of the traces...
            while (current.isFilterTrace()) {
                RawTile tile = layout.getTile(current.getAsFilter());
                tileCosts[tile.getTileNumber()] = (nextFinish - next1Iter);
                //get ready for next iteration
                //System.out.println("Setting " + tile + " " + current + " to " + 
                //        tileCosts[tile.getTileNumber()]);
                
                nextFinish = tileCosts[tile.getTileNumber()];
                //record the end time of this filter on the tile
                endTime.put(current.getAsFilter(), new Integer(nextFinish));
                next1Iter = spaceTime.partitioner.getWorkEstOneFiring(current.getAsFilter());
                current = current.getPrevious();
            }
            
            //traverse forwards and set the finish times of the traces
            current = bottleNeck.getNext();
            int prevFinish = tileCosts[bottleNeckTile.getTileNumber()];
            
            while (current.isFilterTrace()) {
                RawTile tile = layout.getTile(current.getAsFilter());
                tileCosts[tile.getTileNumber()] = 
                    (prevFinish + spaceTime.partitioner.getWorkEstOneFiring(current.getAsFilter()));
                //System.out.println("Setting " + tile + " " + current + " to " + 
                //        tileCosts[tile.getTileNumber()]);
                prevFinish = tileCosts[tile.getTileNumber()];
                //record the end time of this filter on the tile
                endTime.put(current.getAsFilter(), new Integer(prevFinish));
                current = current.getNext();
            }
        }
        //remember the bottleneck tile
        bottleNeckTile = -1;
        bottleNeckCost = -1; 
        for (int i = 0; i < tileCosts.length; i++) {
            if (tileCosts[i] > bottleNeckCost) {
             bottleNeckCost = tileCosts[i];
             bottleNeckTile = i;
            }
        }
    }
}
