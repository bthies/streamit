/**
 * 
 */
package at.dms.kjc.backendSupport;

import java.util.*;

import at.dms.kjc.slicegraph.*;
import at.dms.kjc.spacetime.CompareSliceBNWork;
import at.dms.kjc.spacetime.SpaceTimeBackend;


/**
 * Use greedy bin packing to allocate slices to compute nodes.
 * @author mgordon / dimock
 *
 */
public class BasicGreedyLayout<T extends ComputeNode> implements Layout<T> {
    private HashMap<SliceNode, T> assignment;
    private SpaceTimeScheduleAndSlicer spaceTime;
    private int numBins;
    private LinkedList<SliceNode>[] bins;
    private int[] binWeight;
    //private int[] searchOrder;
    private int totalWork;
    private T[] nodes;
    private SIRSlicer slicer;
    
    /**
     * Constructor
     * @param spaceTime
     * @param nodes
     */
    public BasicGreedyLayout(SpaceTimeScheduleAndSlicer spaceTime, T[] nodes) {
        this.slicer = (SIRSlicer)spaceTime.getSlicer();
        this.spaceTime = spaceTime;
        this.nodes = nodes;
        this.numBins = nodes.length;
     
        bins = new LinkedList[numBins];
        binWeight = new int[numBins];
        for (int i = 0; i < numBins; i++) {
            bins[i] = new LinkedList<SliceNode>();
            binWeight[i] = 0;
        }
    }
    
    public HashMap<SliceNode, T> getAssignment() {
        return assignment;
    }
    
    
    public T getComputeNode(SliceNode node) {
        return assignment.get(node);
    }
   
    public void setComputeNode(SliceNode node, T tile) {
        assignment.put(node, tile);
    }
    public void runLayout() {
        assignment = new HashMap<SliceNode, T>();
        pack();

        System.out.println("IdealWork = " + totalWork / numBins);
        System.out.println("Greedy max tile Work Cost = " + maxBinWeight());
    }
    
    private void pack() {
        //now sort the filters by work
        LinkedList<SliceNode> sortedList = new LinkedList<SliceNode>();
        LinkedList<Slice> scheduleOrder;
        
        //get the schedule order of the graph!
        if (SpaceTimeBackend.NO_SWPIPELINE) {
            //if we are not software pipelining then use then respect
            //dataflow dependencies
            scheduleOrder = DataFlowOrder.getTraversal(spaceTime.getSlicer().getSliceGraph());
        } else {
            //if we are software pipelining then sort the traces by work
            Slice[] tempArray = (Slice[]) spaceTime.getSlicer().getSliceGraph().clone();
            Arrays.sort(tempArray, new CompareSliceBNWork(slicer));
            scheduleOrder = new LinkedList<Slice>(Arrays.asList(tempArray));
            //reverse the list, we want the list in descending order!
            Collections.reverse(scheduleOrder);
        }

        
        for (int i = 0; i < scheduleOrder.size(); i++) {
            Slice slice = scheduleOrder.get(i);
            assert slice.getNumFilters() == 1 : "The greedy partitioner only works for Time!";
            sortedList.add(slice.getHead().getNextFilter());
        }
        
        Iterator<SliceNode> sorted = sortedList.iterator();
        
        //perform the packing
        while (sorted.hasNext()) {
            SliceNode snode = sorted.next();
            if (snode instanceof FilterSliceNode) {
                FilterSliceNode fnode = (FilterSliceNode) snode;
                int bin = findMinBin();

                bins[bin].add(fnode);
                assignment.put(fnode, nodes[bin]);
                binWeight[bin] += slicer
                        .getFilterWorkSteadyMult(fnode);
                totalWork += slicer
                        .getFilterWorkSteadyMult(fnode);
                System.out.println(" Placing: "
                        + fnode
                        + " work = "
                        + slicer.getFilterWorkSteadyMult(
                                fnode) + " on bin " + bin + ", bin work = "
                        + binWeight[bin]);
                if (snode.getPrevious() instanceof InputSliceNode) {
                    assignment.put(snode.getPrevious(),nodes[bin]);
                }
                if (snode.getNext() instanceof OutputSliceNode) {
                    assignment.put(snode.getNext(),nodes[bin]);
                }
            
            }

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