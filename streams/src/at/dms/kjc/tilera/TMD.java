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

/**
 * 
 * 
 * @author mgordon
 *
 */
public class TMD extends Scheduler {

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
            assert levels[l].length  <= TileraBackend.chip.size() : "Too many filters in level for TMD layout!";
            
            if (levels[l].length < TileraBackend.chip.size())
                System.out.println("Warning: Level " + l + " of slice graph has fewer filters than tiles.");
            
            for (int f = 0; f < levels[l].length; f++) {
                setComputeNode(levels[l][f].getFirstFilter(), TileraBackend.chip.getNthComputeNode(f));
            }
        }
    }
    
    /**
     * Use fission to create a time-multiplexed, data-parallel version of the 
     * application. Use the cousins algorithm specifying the number of tiles, so that
     * we duplicate each data parallel (stateless) filter a number of times taking 
     * the task parallel work into account.  See ASPLOS 06 paper for an explanation. 
     *  
     * @param str The stream graph
     * @param tiles The number of cores of the target machine
     */
    public void run(SIRStream str, int tiles) {
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
