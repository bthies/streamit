/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.FilterTraceNode;


/**
 * @author mgordon
 *
 */
public class GreedyLayout implements Layout {
    private HashMap<FilterTraceNode, RawTile> assignment;
    private SpaceTimeSchedule spaceTime;
    private RawChip chip;
    private int numBins;
    private LinkedList<FilterTraceNode>[] bins;
    private int[] binWeight;
    private int maxBinWeight;
    private int[] searchOrder; 
    private int totalWork;
    
    public GreedyLayout(SpaceTimeSchedule spaceTime, RawChip chip) {
        this.chip = chip;
        this.spaceTime = spaceTime;
      
        this.numBins = chip.getTotalTiles();
     
        bins = new LinkedList[numBins];
        binWeight = new int[numBins];
        for (int i = 0; i < numBins; i++) {
            bins[i] = new LinkedList<FilterTraceNode>();
            binWeight[i] = 0;
        }
        searchOrder = new int[numBins];
        if (numBins == 16) {
            searchOrder[0] = 5;
            searchOrder[1] = 1;
            searchOrder[2] = 2;
            searchOrder[3] = 3;
            searchOrder[4] = 6;
            searchOrder[5] = 7;
            searchOrder[6] = 11;
            searchOrder[7] = 15;
            searchOrder[8] = 10;
            searchOrder[9] = 14;
            searchOrder[10] = 13;
            searchOrder[11] = 12;
            searchOrder[12] = 9;
            searchOrder[13] = 8;
            searchOrder[14] = 4;
            searchOrder[15] = 0;
        }
        else {
            assert false : "Greedy Layout: Search order not defined for chip configuration.";
        }
    }
    
    public HashMap<FilterTraceNode, RawTile> getAssignment() {
        return assignment;
    }
    
    
    public RawTile getTile(FilterTraceNode node) {
        return assignment.get(node);
    }
   
    public void setTile(FilterTraceNode node, RawTile tile) {
        assignment.put(node, tile);
    }
    public void run() {
        assignment = new HashMap<FilterTraceNode, RawTile>();
        pack();
        System.out.println("IdealWork = " + totalWork / chip.getTotalTiles());
        System.out.println("Greedy max tile Work Cost = " + maxBinWeight);
        //assign buffers!
        //new BufferDRAMAssignment().run(spaceTime, this);
    }
    
    private void pack() {
        //now sort the filters by work
        LinkedList<FilterTraceNode> sortedList = new LinkedList<FilterTraceNode>();
        LinkedList<Trace> scheduleOrder;
        
        //get the schedule order of the graph!
        //System.out.println(SpaceTimeBackend.NO_SWPIPELINE);
        if (SpaceTimeBackend.NO_SWPIPELINE) {
            //if we are not software pipelining then use then respect
            //dataflow dependencies
            scheduleOrder = DataFlowOrder.getTraversal(spaceTime.partitioner.getTraceGraph());
        } else {
            //if we are software pipelining then sort the traces by work
            Trace[] tempArray = (Trace[]) spaceTime.partitioner.getTraceGraph().clone();
            Arrays.sort(tempArray, new CompareTraceBNWork(spaceTime.partitioner));
           // System.out.println(tempArray.length);
            scheduleOrder = new LinkedList<Trace>(Arrays.asList(tempArray));
            //reverse the list, we want the list in descending order!
            Collections.reverse(scheduleOrder);
        }

        
        for (int i = 0; i < scheduleOrder.size(); i++) {
            Trace trace = scheduleOrder.get(i);
            
            //don't add io traces!
            /*if (spaceTime.partitioner.isIO(trace)) {
                System.out.println("don't add " + trace.getHead().getNextFilter());
                continue;
            }*/
            assert trace.getNumFilters() == 1 : "The greedy partitioner only works for Time!";
            sortedList.add(trace.getHead().getNextFilter());
        }
        
        Iterator<FilterTraceNode> sorted = sortedList.iterator();
        
        //perform the packing
        while (sorted.hasNext()) {
            FilterTraceNode node = sorted.next();
            int bin = findMinBin();
            
            bins[bin].add(node);
            assignment.put(node, chip.getTile(bin));
            binWeight[bin] += spaceTime.partitioner.getFilterWorkSteadyMult(node);
            totalWork += spaceTime.partitioner.getFilterWorkSteadyMult(node);
            System.out.println(" Placing: " + node + " work = " + 
                    spaceTime.partitioner.getFilterWorkSteadyMult(node) + 
                            " on bin " + bin + ", bin work = " + binWeight[bin]);

        }
        
        maxBinWeight = -1;
        //find max bin
        for (int i = 0; i < numBins; i++)
            if (binWeight[i] > maxBinWeight) {
                maxBinWeight = binWeight[i];
            }
        
    }
    
    private int findMinBin() {
        int minWeight = Integer.MAX_VALUE;
        int minBin = -1;
        for (int i = 0; i < numBins; i++) {
            int index = searchOrder[i]; 
            if (binWeight[index] < minWeight) {
                minBin = index;
                minWeight = binWeight[index];
            }
        }
        return minBin;
    }
    
    public int[] getBinWeights() {
        return binWeight;
    }
    
    public int maxBinWeight() {
        return maxBinWeight;
    }
}