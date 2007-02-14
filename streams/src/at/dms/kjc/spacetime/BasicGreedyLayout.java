/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.ComputeNode;


/**
 * Use greedy bin packing to allocate slices to compute nodes.
 * @author mgordon / dimock
 *
 */
public class BasicGreedyLayout implements Layout<ComputeNode> {
    private HashMap<FilterSliceNode, ComputeNode> assignment;
    private SpaceTimeScheduleAndPartitioner spaceTime;
    private int numBins;
    private LinkedList<FilterSliceNode>[] bins;
    private int[] binWeight;
    //private int[] searchOrder;
    private int totalWork;
    private ComputeNode[] nodes;
    
    /**
     * Constructor
     * @param spaceTime
     * @param nodes
     */
    public BasicGreedyLayout(SpaceTimeScheduleAndPartitioner spaceTime, ComputeNode[] nodes) {
        this.spaceTime = spaceTime;
        this.nodes = nodes;
        this.numBins = nodes.length;
     
        bins = new LinkedList[numBins];
        binWeight = new int[numBins];
        for (int i = 0; i < numBins; i++) {
            bins[i] = new LinkedList<FilterSliceNode>();
            binWeight[i] = 0;
        }
    }
    
    public HashMap<FilterSliceNode, ComputeNode> getAssignment() {
        return assignment;
    }
    
    
    public ComputeNode getComputeNode(FilterSliceNode node) {
        return assignment.get(node);
    }
   
    public void setComputeNode(FilterSliceNode node, ComputeNode tile) {
        assignment.put(node, tile);
    }
    public void run() {
        assignment = new HashMap<FilterSliceNode, ComputeNode>();
        pack();

        System.out.println("IdealWork = " + totalWork / numBins);
        System.out.println("Greedy max tile Work Cost = " + maxBinWeight());
    }
    
    private void pack() {
        //now sort the filters by work
        LinkedList<FilterSliceNode> sortedList = new LinkedList<FilterSliceNode>();
        LinkedList<Slice> scheduleOrder;
        
        //get the schedule order of the graph!
        if (SpaceTimeBackend.NO_SWPIPELINE) {
            //if we are not software pipelining then use then respect
            //dataflow dependencies
            scheduleOrder = DataFlowOrder.getTraversal(spaceTime.getPartitioner().getSliceGraph());
        } else {
            //if we are software pipelining then sort the traces by work
            Slice[] tempArray = (Slice[]) spaceTime.getPartitioner().getSliceGraph().clone();
            Arrays.sort(tempArray, new CompareSliceBNWork(spaceTime.getPartitioner()));
            scheduleOrder = new LinkedList<Slice>(Arrays.asList(tempArray));
            //reverse the list, we want the list in descending order!
            Collections.reverse(scheduleOrder);
        }

        
        for (int i = 0; i < scheduleOrder.size(); i++) {
            Slice slice = scheduleOrder.get(i);
            assert slice.getNumFilters() == 1 : "The greedy partitioner only works for Time!";
            sortedList.add(slice.getHead().getNextFilter());
        }
        
        Iterator<FilterSliceNode> sorted = sortedList.iterator();
        
        //perform the packing
        while (sorted.hasNext()) {
            FilterSliceNode fnode = sorted.next();
            int bin = findMinBin();
            
            bins[bin].add(fnode);
            assignment.put(fnode, nodes[bin]);
            binWeight[bin] += spaceTime.getPartitioner().getFilterWorkSteadyMult(fnode);
            totalWork += spaceTime.getPartitioner().getFilterWorkSteadyMult(fnode);
            System.out.println(" Placing: " + fnode + " work = " + 
                    spaceTime.getPartitioner().getFilterWorkSteadyMult(fnode) + 
                            " on bin " + bin + ", bin work = " + binWeight[bin]);

        }
    }
    
    private int findMinBin() {
        int minWeight = Integer.MAX_VALUE;
        int minBin = -1;
        for (int i = 0; i < numBins; i++) {
            //int index = searchOrder[i]; 
            if (binWeight[/*index*/ i] < minWeight) {
                minBin = /*index*/ i;
                minWeight = binWeight[/*index*/ i];
            }
        }
        return minBin;
    }
    
    /**
     * get the bin weights (Estimated max computation at each node).
     * @return
     */
    public int[] getBinWeights() {
        return binWeight;
    }
    
    private boolean gotMaxBinWeight = false;
    private int maxBinWeight;
    
    /**
     * get maximum bin weight (Estimated max computation at any node).
     * @return
     */
    public int maxBinWeight() {
        if (!gotMaxBinWeight) {
            maxBinWeight = -1;
            // find max bin
            for (int i = 0; i < numBins; i++) {
                if (binWeight[i] > maxBinWeight) {
                    maxBinWeight = binWeight[i];
                }
            }
            gotMaxBinWeight = true;
        }

        return maxBinWeight;
    }
}