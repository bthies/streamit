/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;

import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

/**
 * This class models the calculated schedule and layout using the 
 * work estimation for each filter of each slice.
 * 
 * @author mgordon
 *
 */
public class ScheduleModel {
    /** the cost of outputing one item using the gdn versus using the static net
     for the final filter of a Slice */
    public static final int GDN_PUSH_COST = 2;
    /** the cost of issue a read or a write dram command on the tiles assigned 
     * to trace enpoints for each trace.
     */
    public static final int DRAM_ISSUE_COST = 5;
    private Layout<RawTile> layout;
    private LinkedList<Slice> scheduleOrder;
    private RawChip rawChip;
    private SpaceTimeSchedule spaceTime;
    /** array of total work estimation for each tile including blocking*/
    private int[] tileCosts;
    /** Map of filter to start time on the tile they are assigned */
    private HashMap<FilterSliceNode, Integer> startTime;
    /** Map if filter to end time on the tile they are assigned */
    private HashMap<FilterSliceNode, Integer> endTime;
    /** the tile with the most work */
    private int bottleNeckTile;
    /** the amount of work for the bottleneck tile */
    private int bottleNeckCost;
    
    public ScheduleModel(SpaceTimeSchedule spaceTime, Layout layout, 
            LinkedList<Slice> scheduleOrder) {
        this.layout = layout;
        this.spaceTime = spaceTime;
        this.rawChip = spaceTime.getRawChip();
        this.scheduleOrder = scheduleOrder;
        startTime = new HashMap<FilterSliceNode, Integer>();
        endTime = new HashMap<FilterSliceNode, Integer>(); 
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
    public int getFilterStart(FilterSliceNode node) {
        //System.out.println(node);
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
    public int getFilterEnd(FilterSliceNode node) {
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
     * Return the set of tiles that are within threshold of the bottleneck tile.
     * 
     * @param threshold The threshold that defines a bottleneck tile.
     * @return the set of tiles that are within threshold of the bottleneck tile.
     */
    public HashSet<RawTile> getBottleneckTiles(double threshold) {
        HashSet<RawTile> bns = new HashSet<RawTile>();
        
        for (int i = 0; i < tileCosts.length; i++) {
            if (((double)tileCosts[i]) / ((double)bottleNeckCost) > threshold) {
                bns.add(rawChip.getTile(i));
            }
        }
        return bns;
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
        createModel(false);
    }
    
    /**
     * Calculate the amount of work that is performed by each
     * tile in the steady-state.  This calculation will use the work
     * estimation for each filter and will account for pipeline lag inside 
     * of a slice.
     */
    public void createModel(boolean debug) {
        //debug = true;
        tileCosts = new int[rawChip.getTotalTiles()];
        Iterator<Slice> slices = scheduleOrder.iterator();
        
        while (slices.hasNext()) {
            Slice slice = slices.next();
             
            //don't do anything for predefined filters...
            if (slice.getHead().getNextFilter().isPredefined()) 
                continue;
        
            int prevStart = 0;
            int prevEnd = 0;
          
            if (debug)
                System.out.println("Scheduling: " + slice);
            
            if (debug)
                System.out.println("Finding correct times for last filter: ");
            
            //find the correct starting & ending time for the last filter of the trace
            for (FilterSliceNode current : slice.getFilterNodes()) {
                
                RawTile tile = layout.getComputeNode(current);
                if (debug)
                    System.out.println("  Tile Cost of " + tile.getTileNumber() + " is " + 
                            tileCosts[tile.getTileNumber()]);
                
                //the current filter can start at the max of when the last filter
                //has produced enough data for the current to start and when its
                //tile is avail
                int currentStart =  Math.max(tileCosts[tile.getTileNumber()], 
                        prevStart + spaceTime.getSIRSlicer().getFilterStartupCost(current));
                
                if (debug)
                    System.out.println("  prev start + startUp Cost: " + 
                            (prevStart + spaceTime.getSIRSlicer().getFilterStartupCost(current)));
                
                //now the tile avail for this current tile is the max of the current
                //start plus the current occupancy and the previous end plus one iteration
                //of the current, this is because the have to give the current enough
                //cycles after the last filter completes to complete one iteration
                int tileAvail = 
                    Math.max(spaceTime.getSIRSlicer().getFilterOccupancy(current) +
                            currentStart, 
                            prevEnd + spaceTime.getSIRSlicer().getWorkEstOneFiring(current));
                if (debug)
                    System.out.println("  Occ + start = " + 
                            spaceTime.getSIRSlicer().getFilterOccupancy(current) + " " +
                            currentStart);
                if (debug)
                    System.out.println("  PrevEnd + One firing = " + 
                            prevEnd + " " + spaceTime.getSIRSlicer().getWorkEstOneFiring(current));
                
                if (debug)
                    System.out.println("Checking start of " + current + " on " + tile + 
                            "start: " + currentStart + ", tile avail: " + tileAvail);
                
                //remember the start time and end time
                startTime.put(current, new Integer(currentStart));
                //we will over write the end time below for filters 
                //downstream of bottleneck (the last guy)
                endTime.put(current, new Integer(tileAvail));

                assert tileAvail >= prevEnd : "Impossible state reached in schedule model " + 
                  tileAvail + " should be >= " + prevEnd;
                
                prevStart = currentStart; 
                prevEnd = tileAvail;
            }
            
            //when finished prev avail will have the ending time of the last filter!
            
            //the last filter is always the bottleneck of the filter, meaning
            //it finishes last and base everyone else on it
            FilterSliceNode bottleNeck = slice.getTail().getPrevFilter();
                           
            RawTile bottleNeckTile = layout.getComputeNode(bottleNeck);
            
            //calculate when the bottle neck tile will finish, 
            //and base everything off of that, traversing backward and 
            //foward in the trace
            tileCosts[bottleNeckTile.getTileNumber()] = 
                prevEnd;
            
            if (bottleNeck.getPrevious().isInputSlice()) {
                tileCosts[bottleNeckTile.getTileNumber()]+= DRAM_ISSUE_COST;
            }
            if (bottleNeck.getNext().isOutputSlice()) {
                //account for the
                //cost of sending an item over the gdn if it uses it...
                if (LogicalDramTileMapping.mustUseGdn(bottleNeckTile)) {
                    tileCosts[bottleNeckTile.getTileNumber()] += (bottleNeck.getFilter().getPushInt() * 
                            bottleNeck.getFilter().getSteadyMult() * 
                            GDN_PUSH_COST);
                }
            }
            if (debug)
                System.out.println("Setting bottleneck finish: " + bottleNeck + " " + 
                        tileCosts[bottleNeckTile.getTileNumber()]);
            
            int nextFinish = tileCosts[bottleNeckTile.getTileNumber()];
            int next1Iter = spaceTime.getSIRSlicer().getWorkEstOneFiring(bottleNeck);
            SliceNode current = bottleNeck.getPrevious();
            //record the end time for the bottleneck filter
            endTime.put(bottleNeck, new Integer(nextFinish));
            
            //traverse backwards and set the finish times of the traces...
            while (current.isFilterSlice()) {
                RawTile tile = layout.getComputeNode(current.getAsFilter());
                tileCosts[tile.getTileNumber()] = (nextFinish - next1Iter);
                
                if (debug)
                    System.out.println("Setting " + tile + " " + current + " to " + 
                            tileCosts[tile.getTileNumber()]);
                
                nextFinish = tileCosts[tile.getTileNumber()];
                //record the end time of this filter on the tile
                endTime.put(current.getAsFilter(), new Integer(nextFinish));
                //get ready for next iteration
                next1Iter = spaceTime.getSIRSlicer().getWorkEstOneFiring(current.getAsFilter());
                current = current.getPrevious();
            }
            /*
            //traverse forwards and set the finish times of the traces
            current = bottleNeck.getNext();
            int prevFinish = tileCosts[bottleNeckTile.getTileNumber()];
            
            while (current.isFilterSlice()) {
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
            */
            //some checks
            for (FilterSliceNode fsn : slice.getFilterNodes()) {
                assert getFilterStart(fsn) <=
                    (getFilterEnd(fsn) - 
                    spaceTime.getSIRSlicer().getFilterWorkSteadyMult(fsn)) :
   
                        fsn + " " + 
                        getFilterStart(fsn) + " <= " +
                            getFilterEnd(fsn) + " - " + 
                                    spaceTime.getSIRSlicer().getFilterWorkSteadyMult(fsn) +
                                        " (bottleneck: " + bottleNeck + ")";
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
    /**
    * The placement cost (energy) of the configuration.
    * 
    * @param debug Might want to do some debugging...
    * @return placement cost
    */
   public void createModelNoSWPipe() {
       int tileCosts[] = new int[rawChip.getTotalTiles()];
       
       Iterator<Slice> slices = scheduleOrder.iterator();
       //HashMap<FilterSliceNode, Double> endTime = new HashMap<FilterSliceNode, Double>();
       while (slices.hasNext()) {
           Slice slice = slices.next();
           RawTile tile = layout.getComputeNode(slice.getHead().getNextFilter());
           int traceWork = spaceTime.getSIRSlicer().getSliceBNWork(slice); 
           int myStart = 0;
           //now find the start time
           
           //find the max end times of all the traces that this trace depends on
           int maxDepStartTime = 0;
           InputSliceNode input = slice.getHead();
           Iterator<InterSliceEdge> inEdges = input.getSourceSet(SchedulingPhase.STEADY).iterator();
           while (inEdges.hasNext()) {
               InterSliceEdge edge = inEdges.next();
               if (spaceTime.getSlicer().isIO(edge.getSrc().getParent()))
                   continue;
               FilterSliceNode upStream = edge.getSrc().getPrevFilter();
               
               ComputeNode upTile = layout.getComputeNode(upStream);
               assert endTime.containsKey(upStream);
               if (endTime.get(upStream).doubleValue() > maxDepStartTime)
                   maxDepStartTime = endTime.get(upStream);
           }
           
           myStart = (int)Math.max(maxDepStartTime, tileCosts[tile.getTileNumber()]);
           
           //add the start time to the trace work (one filter)!
           tileCosts[tile.getTileNumber()] = myStart + traceWork;
           endTime.put(slice.getHead().getNextFilter(), tileCosts[tile.getTileNumber()]);
           startTime.put(slice.getHead().getNextFilter(), myStart);
       }
       
       bottleNeckCost = -1;
       for (int i = 0; i < tileCosts.length; i++) {
           System.out.println("tile " + i + " = " + tileCosts[i]);
           if (tileCosts[i] > bottleNeckCost) {
               bottleNeckCost = tileCosts[i];
               bottleNeckTile = i;
           }
       }
    }
}