package at.dms.kjc.tilera;

import java.util.HashMap;

import at.dms.kjc.sir.SIRContainer;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRSplitJoin;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.lowering.fission.StatelessDuplicate;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.lowering.partition.WorkList;
import at.dms.kjc.slicegraph.*;
import java.util.LinkedList;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.fission.*;

/**
 * 
 * 
 * @author mgordon
 *
 */
public class TMD extends Scheduler {

    private double DUP_THRESHOLD = .1;
    
    public TMD() {
        super();
    }
    
    /** Get the tile for a Slice 
     * @param node the {@link at.dms.kjc.slicegraph.SliceNode} to look up. 
     * @return the tile that should execute the {@link at.dms.kjc.slicegraph.SliceNode}. 
     */
    public Tile getComputeNode(SliceNode node) {
        assert layoutMap.keySet().contains(node);
        return layoutMap.get(node);
    }
    
    
    /** Set the Tile for a Slice 
     * @param node         the {@link at.dms.kjc.slicegraph.SliceNode} to associate with ...
     * @param tile   The tile to assign the node
     */
    public void setComputeNode(SliceNode node, Tile tile) {
        assert node != null && tile != null;
        layoutMap.put(node, tile);
        //remember what filters each tile has mapped to it
        tile.getComputeCode().addFilter(node.getAsFilter());
    }
    
    /**
     * Assign the filternodes of the slice graph to tiles on the chip based on the levels
     * of the graph. 
     */
    public void runLayout() {
        assert graphSchedule != null : 
            "Must set the graph schedule (multiplicities) before running layout";
        
        //TODO Assign file readers and writers to something on the chip
        
        LevelizeSliceGraph lsg = new LevelizeSliceGraph(graphSchedule.getSlicer().getTopSlices());
        Slice[][] levels = lsg.getLevels();
        
        System.out.println("Levels: " + levels.length);
        
        for (int l = 0; l < levels.length; l++) {
            assert levels[l].length  <= TileraBackend.chip.abstractSize() : "Too many filters in level for TMD layout!";
            
            if (levels[l].length < TileraBackend.chip.abstractSize())
                System.out.println("Warning: Level " + l + " of slice graph has fewer filters than tiles.");
            
            for (int f = 0; f < levels[l].length; f++) {
                setComputeNode(levels[l][f].getFirstFilter(), TileraBackend.chip.getTranslatedTile(f));
            }
        }
    }
    
    /**
     * Run the Time-Multiplexing Data-parallel scheduler.  Right now, it assumes 
     * a pipeline of stateless filters
     */
    public void run(int tiles) {
        int factor = multiplicityFactor(tiles);
        System.out.println("Using fission steady multiplicty factor: " + factor);
        
        LinkedList<Slice> slices = DataFlowOrder.getTraversal(graphSchedule.getSlicer().getTopSlices());
        
        //go through and multiply the steady multiplicity of each filter by factor
        for (Slice slice : slices) {
            FilterContent filter = slice.getFirstFilter().getFilter();
            System.out.println(filter + " " + filter.getSteadyMult());
            filter.multSteadyMult(factor);
            System.out.println(filter + " " + filter.getSteadyMult());
        }
        
        //because we have changed the multiplicities of the FilterContents
        //we have to reset the filter info's because they cache the date of the 
        //FilterContents
        FilterInfo.reset();
        
        //fiss each slice by the number of tiles, assume pipelines for now (no TP)
        for (Slice slice: slices) {
            if (!slice.getFirstFilter().isPredefined()) {
                Fissioner.canFizz(slice, tiles, true);
                assert Fissioner.fizzSlice(slice, tiles) : "Could not fiss slice: " + slice;
            }
        }
    }
    
    /**
     * Determine the factor that we are going to multiple each slice by so that 
     * fission on the slice graph is legal.  Keep trying multiples of the number 
     * tiles until each slice passes the tests for legality.
     *  
     * @param tiles The number of tiles we are targeting 
     * @return the factor to multiply the steady multiplicities by
     */
    private int multiplicityFactor(int tiles) {
        int factor = 0;
        LinkedList<Slice> slices = DataFlowOrder.getTraversal(graphSchedule.getSlicer().getTopSlices());
        boolean allPassed = false;
        
        
        while (!allPassed) {
            factor += tiles;
            System.out.println("Trying Factor: " + factor);
            allPassed = true;
            for (Slice slice : slices) {
                //this works only for pipelines right now, so just that we have at most
                //one input and at most one output for the slice
                assert slice.getHead().getSourceSet().size() <= 1 &&
                    slice.getTail().getDestSet().size() <= 1;
                
                FilterInfo fi = FilterInfo.getFilterInfo(slice.getFirstFilter());
                //nothing to do for filters with no input
                if (fi.pop == 0)
                    continue;
                
                //check that we have reached the threshold for duplicated items
                if (((double)(fi.peek - fi.pop)) >= 
                        (DUP_THRESHOLD * (((double)fi.pop) * ((double)fi.steadyMult) * 
                                ((double)factor)/((double)tiles)))) {
                    allPassed = false;
                    break;
                }
                
                //check that copy down is less than the
                if (fi.copyDown >= fi.pop * fi.steadyMult * factor) {
                    allPassed = false;
                    break;
                }
            }
        }
        
        return factor;
    }
    
    /**
     * This is no longer used, it ran on the SIR graph and partitioned the graph based
     * on the ASPLOS 06 TMD scheduling policy.
     *  
     * Use fission to create a time-multiplexed, data-parallel version of the 
     * application. Use the cousins algorithm specifying the number of tiles, so that
     * we duplicate each data parallel (stateless) filter a number of times taking 
     * the task parallel work into account.  See ASPLOS 06 paper for an explanation. 
     *  
     * @param str The stream graph
     * @param tiles The number of cores of the target machine
     */
    public void oldRun(SIRStream str, int tiles) {
        System.out.println("Running TMD Partitioner...");
        
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        WorkList workList = work.getSortedFilterWork();
        HashMap<SIRStream, Double> averageWork = 
            new HashMap<SIRStream, Double>(); 
        findAverageWork(str, work, averageWork);
        
        
        int totalWork = 0;
        for (int i = 0; i < workList.size(); i++) {
            SIRFilter filter = workList.getFilter(i);
            int filterWork = work.getWork(filter); 
            System.out.println("Sorted Work " + i + ": " + filter + " work " 
                    + filterWork + ", is fissable: " + StatelessDuplicate.isFissable(filter));
            totalWork += filterWork;
        }
        
        for (int i = workList.size() - 1; i >= 0; i--) {
            SIRFilter filter = workList.getFilter(i);
            System.out.println(filter + " work: " + work.getWork(filter));
            if (!StatelessDuplicate.isFissable(filter)) {
                System.out.println("  not fissible");
                continue;
            }
            int commRate = ((int[])work.getExecutionCounts().get(filter))[0] * 
                 (filter.getPushInt() + filter.getPopInt());
            int filterWork = work.getWork(filter);
            if (filterWork / commRate <= 10) {
                System.out.println("   Comp/Comp rate too low!");
                continue;
            }
          
            int cousins = getNumCousins(filter); 
            if (cousins == 1) {
                System.out.println("   No cousins: dup " + tiles);    
                StatelessDuplicate.doit(filter, tiles);
            }
            else {
                // esimate work fraction of this filter vs. cousins
                double workFraction = estimateWorkFraction(filter, averageWork);
                System.out.println("   Filter " + filter + " has " + cousins + " cousins and does " + workFraction + " of the work.");
                if (cousins < tiles) {
                    int reps = (int)Math.ceil(workFraction * ((double)tiles));
                    reps = Math.min(tiles - cousins + 1, reps);
                   
                    System.out.println("   Calling dup with: " + reps);
                    if (reps > 1)
                        StatelessDuplicate.doit(filter, reps);
           
                }
                
            }
        }
    }
    
    /**
     * Estimates the percentage of work performed by 'filter' in
     * relation to all of its cousins.  Works by ascending through
     * stream graph hierarchy.  Whenever a splitjoin is encountered,
     * the average work of filters on each branch is used to guide the
     * division of work between children.  Such fractions are
     * accumulated multiplicatively until reaching the top-level
     * splitjoin.
     */
    private double estimateWorkFraction(SIRFilter filter, HashMap<SIRStream, Double> averageWork) {
        double fraction = 1.0;
        SIRContainer parent = filter.getParent();
        SIRStream child = filter;
        while (parent != null) {
            if (parent instanceof SIRSplitJoin) {
                // work done by other streams
                double otherWork = 0.0;
                // work done by stream containing <filter>
                double myWork = 0.0;    
                System.out.println(parent);
                for (int i=0; i<parent.size(); i++) {
                    System.out.println(averageWork);
                    SIRStream temp = parent.get(i);
                    System.out.println(temp);
                    Double workD = averageWork.get(temp);//parent.get(i));
                    System.out.println(workD);
                    double work = workD.doubleValue();
                    if (parent.get(i)==child) {
                        myWork = work;
                    } else {
                        otherWork += work;
                    }
                }
                // accumulate the fraction of work that we do as part of this splitjoin
                fraction *= myWork / (myWork + otherWork);
            }
            child = parent;
            parent = parent.getParent();
        }
        return fraction;
    }
    
    /**
    * Returns number of parallel streams that are at same nesting
    * depth as <filter> relative to the top-level splitjoin.  Assumes
    * that the splitjoin widths on the path from <filter> to the
    * top-level splitjoin are symmetric across other siblings.
    */
   private int getNumCousins(SIRFilter filter) {
       int cousins = 1;
       SIRContainer container = filter.getParent();
       while (container != null) {
           if (container instanceof SIRSplitJoin) {
               cousins *= (((SIRSplitJoin)container).getParallelStreams().size());
           }
           container = container.getParent();
       }
       return cousins;
   }
   
    /**
     * Mutates 'averageWork' into a mapping from filters to the
     * average amount of work for all filters deeply nested within
     * 'str'.
     */
    private void findAverageWork(SIRStream str, WorkEstimate work, 
            HashMap<SIRStream, Double> averageWork) {
        // the total amount of fissable work per container
        HashMap<SIRStream, Integer> sum = new HashMap<SIRStream, Integer>();
        // the number of fissable filters per container
        HashMap<SIRStream, Integer> count = new HashMap<SIRStream, Integer>();

        findWork(str, work, count, sum, averageWork);
    }

    /**
     * Counts the number of filters in each container and stores
     * result in 'count'.  Also accumulates 'sum' of work in
     * containers, as well as 'average' of work across all filters in
     * container.
     */
    private void findWork(SIRStream str, WorkEstimate work, 
                          HashMap<SIRStream, Integer> count,
                          HashMap<SIRStream, Integer> sum,
                          HashMap<SIRStream, Double> average) {
        if (str instanceof SIRFilter) {
            SIRFilter filter = (SIRFilter)str;
            count.put(filter, 1);
            sum.put(filter, work.getWork(filter));
            average.put(filter, (double)work.getWork(filter));
        } else {
            SIRContainer cont = (SIRContainer)str;
            int mysum = 0;
            int mycount = 0;
            // visit children to accumulate sum, count
            for (int i=0; i<cont.size(); i++) {
                SIRStream child = (SIRStream) cont.get(i);
                findWork(child, work, count, sum, average);
                mysum += sum.get(child);
                mycount += count.get(child);
            }
            // calculate average
            double myaverage= ((double)mysum) / ((double)mycount);
            // store results
            sum.put(cont, mysum);
            count.put(cont, mycount);
            average.put(cont, myaverage);
        }
    }
}
