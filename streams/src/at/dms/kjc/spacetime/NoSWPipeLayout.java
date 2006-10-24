/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.common.*;
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterTraceNode;
import at.dms.kjc.slicegraph.InputTraceNode;

/**
 * @author mgordon
 *
 */
public class NoSWPipeLayout extends SimulatedAnnealing implements Layout {
    
    private SpaceTimeSchedule spaceTime;
    private RawChip chip;
    private LinkedList<Trace> scheduleOrder;
    private LinkedList<FilterTraceNode> assignedFilters;
    private Random rand;
    
    public NoSWPipeLayout(SpaceTimeSchedule spaceTime) {
        this.spaceTime = spaceTime;
        this.chip = spaceTime.getRawChip();
        scheduleOrder = 
            DataFlowOrder.getTraversal(spaceTime.partitioner.getTraceGraph());
        assignedFilters = new LinkedList<FilterTraceNode>();
        rand = new Random(17);
    }
    
    public RawTile getTile(FilterTraceNode node) {
        return (RawTile)assignment.get(node);
    }
    
    public void setTile(FilterTraceNode node, RawTile tile) {
        assignment.put(node, tile);
    }
    
    /** 
     * Use this function to reassign the assignment to <newAssign>, update
     * anything that needs to be updated on a new assignment.
     * @param newAssign
     */
    public void setAssignment(HashMap newAssign) {
        this.assignment = newAssign;
    }
    
    /**
     * Called by perturbConfiguration() to perturb the configuration.
     * perturbConfiguration() decides if we should keep the new assignment.
     * 
     */
    public void swapAssignment() {
        /*
        //pick two filters and swap them.
        FilterTraceNode filter1 = assignedFilters.get(rand.nextInt(assignedFilters.size()));
        FilterTraceNode filter2 = assignedFilters.get(rand.nextInt(assignedFilters.size()));
        
        RawTile tile1 = getTile(filter1);
        RawTile tile2 = getTile(filter2);
        
        assignment.put(filter1, tile2);
        assignment.put(filter2, tile1);
        */
        FilterTraceNode filter1 = assignedFilters.get(rand.nextInt(assignedFilters.size()));
        assignment.put(filter1, chip.getTile(rand.nextInt(chip.getTotalTiles())));
    }
    
    /**
     * Random initial assignment.
     */
    public void initialPlacement() {
        Iterator<Trace> traces = scheduleOrder.iterator();
        int tile = 0;
        while (traces.hasNext()) {
            
          Trace trace = traces.next();
          //System.out.println(trace.getHead().getNextFilter());
          //if (spaceTime.partitioner.isIO(trace))
          //    continue;
          assert trace.getFilterNodes().length == 1 : "NoSWPipeLayout only works for Time! "  + 
               trace;
          //System.out.println("init assiging " + trace.getHead().getNextFilter() + " to " + tile);
          assignment.put(trace.getHead().getNextFilter(), chip.getTile(tile++));
          assignedFilters.add(trace.getHead().getNextFilter());
          tile = tile % chip.getTotalTiles();
          
        }
    }
    
    
    
    /**
     * The placement cost (energy) of the configuration.
     * 
     * @param debug Might want to do some debugging...
     * @return placement cost
     */
    public double placementCost(boolean debug) {
        double tileCosts[] = new double[chip.getTotalTiles()];
        
        Iterator<Trace> traces = scheduleOrder.iterator();
        HashMap<FilterTraceNode, Double> endTime = new HashMap<FilterTraceNode, Double>();
        while (traces.hasNext()) {
            Trace trace = traces.next();
            RawTile tile = getTile(trace.getHead().getNextFilter());
            double traceWork = spaceTime.partitioner.getTraceBNWork(trace); 
            double startTime = 0;
            //now find the start time
            
            //find the max end times of all the traces that this trace depends on
            double maxDepStartTime = 0;
            InputTraceNode input = trace.getHead();
            Iterator<Edge> inEdges = input.getSourceSet().iterator();
            while (inEdges.hasNext()) {
                Edge edge = inEdges.next();
                if (spaceTime.partitioner.isIO(edge.getSrc().getParent()))
                    continue;
                FilterTraceNode upStream = edge.getSrc().getPrevFilter();
                
                RawTile upTile = getTile(upStream);
                assert endTime.containsKey(upStream);
                if (endTime.get(upStream).doubleValue() > maxDepStartTime)
                    maxDepStartTime = endTime.get(upStream).doubleValue();
            }
            
            startTime = Math.max(maxDepStartTime, tileCosts[tile.getTileNumber()]);
            
            //add the start time to the trace work (one filter)!
            tileCosts[tile.getTileNumber()] = startTime + traceWork;
            endTime.put(trace.getHead().getNextFilter(), tileCosts[tile.getTileNumber()]);
        }
        
        
        double max = -1;
        for (int i = 0; i < tileCosts.length; i++) {
            if (tileCosts[i] > max) 
                max = tileCosts[i];
        }
        
        return max;
     }
    
    /** 
     * Perform any initialization that has to be done before
     * simulated annealing. This does not include the initial placement. 
     *
     */
    public void initialize() {
        
    }
    

    /**
     * Decide if we should keep a configuration that has a 
     * cost that is EQUAL to the current minimum of the search.  
     * By default, don't keep it. Override if you want other behavior.
     * 
     * @return Should we set the min config to this config (they have the same
     * cost). 
     */
    protected boolean keepNewEqualMin() {
        return false;
    }
        
    public void run() { 
        //initialPlacement();
        simAnnealAssign(1, 2000);
        new BufferDRAMAssignment().run(spaceTime, this);
    }
}