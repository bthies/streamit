/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SIRSlicer;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.*;

import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.common.SimulatedAnnealing;

/**
 * An experimental class that tries to optimize the data-reordering
 * stage of the greedy layout.  It was not helpful.  Please don't 
 * look at it!
 * 
 * @author mgordon
 *
 */
public class AnnealedGreedyLayout extends SimulatedAnnealing implements Layout<RawTile> {
    private GreedyLayout greedyLayout;
    private RawChip chip;
    private SpaceTimeSchedule spaceTime;
    private BufferDRAMAssignment assignBuffers;
    private SIRSlicer slicer;
    private Router router;
    private int[] tileCosts;
    private Random rand;
    private StreamlinedDuplicate duplicate;
    
    public AnnealedGreedyLayout(SpaceTimeSchedule spaceTime, RawChip chip, 
            StreamlinedDuplicate duplicate) {
        this.chip = chip;
        this.spaceTime = spaceTime;
        this.slicer = spaceTime.getSIRSlicer();
        rand = new Random(17);
        this.duplicate = duplicate;
    }
    
    public RawTile getComputeNode(SliceNode node) {
        assert assignment.containsKey(node) : "Node not in assignment: " + node;
        assert assignment.get(node) != null : "Tile assignment null: " + node;
        return (RawTile)assignment.get(node);
    }
    
    public void setComputeNode(SliceNode node, RawTile tile) {
        assert node instanceof FilterSliceNode;
        FilterSliceNode fnode = (FilterSliceNode)node;
        if (!fnode.isPredefined()) { 
            tileCosts[getComputeNode(fnode).getTileNumber()] -= 
                slicer.getFilterWorkSteadyMult(fnode);
            tileCosts[tile.getTileNumber()] += 
                slicer.getFilterWorkSteadyMult(fnode);
            //and add the assignment
        }
        
        assignment.put(fnode, tile);
    }
    
    /** 
     * Use this function to reassign the assignment to <newAssign>, update
     * anything that needs to be updated on a new assignment.
     * @param newAssign
     */
    public void setAssignment(HashMap newAssign) {
        ////System.out.println("Calling Set Assignment!");
        this.assignment = newAssign;
    }
    
    private void recalculateBinWeights() {
        for (int i = 0; i < tileCosts.length; i++) {
            tileCosts[i] = 0;
        }
        
        Iterator<FilterSliceNode> filters = assignment.keySet().iterator();
        while (filters.hasNext()) {
            FilterSliceNode filter = filters.next();
            int bin = ((RawTile)assignment.get(filter)).getTileNumber();
            tileCosts[bin] += slicer.getFilterWorkSteadyMult(filter);
        }
    }
    
    /**
     * Called by perturbConfiguration() to perturb the configuration.
     * perturbConfiguration() decides if we should keep the new assignment.
     * 
     */
    public void swapAssignment() {
        //find two slices to swap, remember this only work for time
        //slices are single filter
        Slice slice1, slice2;
        FilterSliceNode filter1 = null, filter2 = null;
        int bin1, bin2;
        Slice[] slices = slicer.getSliceGraph();
        
        recalculateBinWeights();
        
        while (true) {
            //while (true) {
            slice1 =  slices[rand.nextInt(slices.length)];
            //break when we have found a non-io slice
            //if (!partitioner.isIO(slice1))
            //    break;
           // }
            
           
            slice2 = slices[rand.nextInt(slices.length)];
            //if (!partitioner.isIO(slice2))
            //    break;
            
            
            if (slice1 == slice2)
                continue;
            
            filter1 = slice1.getHead().getNextFilter();
            filter2 = slice2.getHead().getNextFilter();
            
            bin1 = getComputeNode(filter1).getTileNumber();
            bin2 = getComputeNode(filter2).getTileNumber();
            
            ////System.out.println("Swap?: " + filter1 + " from  " + bin1 + 
            //        " with "+ filter2 + " from " + bin2);
            
            
            
            int oldBin1Weight = tileCosts[bin1];
            int oldBin2Weight = tileCosts[bin2];
            int oldMaxBinWeight = maxBinWeight();
            
            //System.out.println("Bin1: " + tileCosts[bin1]);
            //System.out.println("Bin2: " + tileCosts[bin2]);
            
            //System.out.println("Work1: " + filter1 + " " + 
                    //partitioner.getFilterWorkSteadyMult(filter1));
            //System.out.println("Work2: " + filter2 + " " + 
                    //partitioner.getFilterWorkSteadyMult(filter2));
            
            int oldSum = tileCosts[bin1] + tileCosts[bin2];
            //check if we should swap the assignment
      
            tileCosts[bin1] -= slicer.getFilterWorkSteadyMult(filter1); 
            tileCosts[bin1] += slicer.getFilterWorkSteadyMult(filter2); 
      
            tileCosts[bin2] -= slicer.getFilterWorkSteadyMult(filter2);
            tileCosts[bin2] += slicer.getFilterWorkSteadyMult(filter1); 
      
            //System.out.println("Bin1: " + tileCosts[bin1]);
            //System.out.println("Bin2: " + tileCosts[bin2]);
            //System.out.println("Old max: " + oldMaxBinWeight + " ? New Max: " + maxBinWeight());
            
            if (maxBinWeight() <= oldMaxBinWeight) {
                //we have found two filters that can be swapped!
                break;
            }
            else {
                //we don't want to swap these, it will increase the
                //critcal path computation!
                //restore the tileCosts
                tileCosts[bin1] = oldBin1Weight;
                tileCosts[bin2] = oldBin2Weight;
            }
        }
        assert filter1 != null && filter2 != null;
        
        //System.out.println("Swapping: " + filter1 + " into " + bin2 + 
               // ", " + filter2 + " into " + bin1);
        //when we get here we can swap the assignment
        //the bin costs have already been updated!
        assignment.put(filter1, chip.getTile(bin2));
        assignment.put(filter2, chip.getTile(bin1));
    }
    
    private int maxBinWeight() {
        int maxBinWeight = -1;
        //find max bin
        for (int i = 0; i < chip.getTotalTiles(); i++)
            if (tileCosts[i] > maxBinWeight) {
                maxBinWeight = tileCosts[i];
            }
        
        return maxBinWeight;
    }
    
    /**
     * The initial assignment, this must set up 
     * assignment so that is can be called by 
     * placemenCost() and perturbConfiguration(T);
     *
     */
    public void initialPlacement() {
    
        //dump the POV representation of the schedule
        
        /*
        Iterator<FilterSliceNode> filters = assignment.keySet().iterator();
        while (filters.hasNext()) {
            FilterSliceNode filter = filters.next();
            //System.out.println(filter + " --> " + assignment.get(filter));
        }
        */
    }
    
    /**
     * The placement cost (energy) of the configuration.
     * 
     * @param debug Might want to do some debugging...
     * @return placement cost
     */
    public double placementCost(boolean debug) {
        return (int)reorgCrossRoutes();
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
    
    
    private void duplicateLayout() {
        System.out.println("Reconstructing Streamlined Duplicate Packing...");
        tileCosts = new int[16]; 
        for (int t = 0; t < chip.getTotalTiles(); t++) {
            for (int i = 0; i < duplicate.getFilterOnTile(t).size(); i++) {
                SIRFilter filter = duplicate.getFilterOnTile(t).get(i);
                FilterSliceNode node = 
                    FilterSliceNode.getFilterNode(spaceTime.getSIRSlicer().getContent(filter));
                assignment.put(node, chip.getTile(t));
                tileCosts[t] += slicer.getFilterWorkSteadyMult(node); 
            }
        }
        
        Iterator<FilterSliceNode> nodes = 
            Util.sortedFilterSlicesTime(spaceTime.getSIRSlicer()).iterator();
        while (nodes.hasNext()) {
            FilterSliceNode node = nodes.next();
            //already assigned above
            if (assignment.containsKey(node))
                continue;
            System.out.println("  *Packing additional node " + node);
            //otherwise put it in the min bin
            int tile = minIndex(tileCosts);
            assignment.put(node, chip.getTile(tile));
            tileCosts[tile] += slicer.getFilterWorkSteadyMult(node);
        }
        
    }
    
    private int minIndex(int[] arr) {
        int min = Integer.MAX_VALUE;
        int minIndex = -1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
                minIndex = i;
            }
        }
        return minIndex;
    }
    
    public void runLayout() {
        if (duplicate != null && !KjcOptions.partition_greedier)  {
            //if we have used StreamlinedDuplicate, then we want to start with its
            //layout
            duplicateLayout();
        } else {
            greedyLayout = new GreedyLayout(spaceTime, chip);
            greedyLayout.runLayout();
            //otherwise, start with a greedy bin packing!
            tileCosts = greedyLayout.getBinWeights();
            assignment = (HashMap)greedyLayout.getAssignment().clone();
        }
        
        /*
        for (int i = 0; i < tileCosts.length; i++) {
            //System.out.println("Bin " + i + " = " + tileCosts[i]);
        }
        */
        
        assignBuffers = new BufferDRAMAssignment();
        
        simAnnealAssign(4, 100);
        System.out.println("Computation Cost: " + maxBinWeight());
        assignBuffers.run(spaceTime, this);
    }
    
    /**
     * Return the number of cross routes that occur during the reorganization stage
     * between software pipelined steady states.
     * 
     * @return the number of cross routes that occur during the reorganization stage
     * between software pipelined steady states.
     */
    
    private int reorgCrossRoutes() {
        Slice[] slices = slicer.getSliceGraph();
        int crossed = 0;
        
        router = new SmarterRouter(tileCosts, chip);
        assignBuffers.run(spaceTime, this);
        
        
        //buffer edges are assigned drams by the buffer dram assignment,
        //so we can get a fairly accurate picture of the communication
        //of the graph...
        HashSet<RawComputeNode> routersUsed = new HashSet<RawComputeNode>();
        
        for (int i = 0; i < slices.length; i++) {
            Slice slice = slices[i];
            Iterator edges = slice.getTail().getDestSet(SchedulingPhase.STEADY).iterator();
            while (edges.hasNext()) {
                InterSliceEdge edge = (InterSliceEdge)edges.next();
               // //System.out.println(" Checking if " + edge + " crosses.");
                InterSliceBuffer buf = InterSliceBuffer.getBuffer(edge);
                
                //nothing is transfered for this buffer.
                if (buf.redundant())
                    continue;
                
                OutputSliceNode output = edge.getSrc();
                InputSliceNode input = edge.getDest();
                StreamingDram bufDRAM = buf.getDRAM();
               
                
                
                if (!IntraSliceBuffer.unnecessary(output)) {
                    StreamingDram outputDRAM = 
                        IntraSliceBuffer.getBuffer(output.getPrevFilter(), output).getDRAM();
                    
                    Iterator<RawComputeNode> route = router.getRoute(outputDRAM, bufDRAM).iterator();
                    while (route.hasNext()) {
                        RawComputeNode hop = route.next();
                        if (routersUsed.contains(hop)) 
                            crossed++;
                        else 
                            routersUsed.add(hop);
                    }
                }
                
                if (!IntraSliceBuffer.unnecessary(input)) {
                    StreamingDram inputDRAM = 
                        IntraSliceBuffer.getBuffer(input, input.getNextFilter()).getDRAM();
                    Iterator<RawComputeNode>route = router.getRoute(bufDRAM, inputDRAM).iterator();
                    while (route.hasNext()) {
                        RawComputeNode hop = route.next();
                        if (routersUsed.contains(hop)) 
                            crossed++;
                        else 
                            routersUsed.add(hop);
                    }
                }
            }
        }
        
        return crossed;
    }
}
