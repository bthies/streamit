/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.Iterator;

import at.dms.kjc.sir.SIRFeedbackLoop;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRContainer;
import at.dms.kjc.sir.SIRPipeline;
import at.dms.kjc.sir.SIRPredefinedFilter;
import at.dms.kjc.sir.SIRSplitJoin;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.lowering.fission.StatelessDuplicate;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.lowering.partition.WorkList;
import java.util.*;
import at.dms.kjc.*;

/**
 * @author mgordon
 *
 */
public class DuplicateBottleneck {
    
    private Vector<Integer> sortedWorkEsts;
    private Vector<SIRFilter> sortedFilters;
    
    public DuplicateBottleneck() {
        
    }
    
    
    public SIRStream smarterDuplicateStreamline(SIRStream str, RawChip chip) {
        double threshold = 0.9;
        SIRStream oldStr;
        //get the first work estimate
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        //bin pack the shits
        GreedyBinPacking binPacker = new GreedyBinPacking(str, chip.getTotalTiles(), work);
        binPacker.pack();
        //get the max bin weight for the packing
        int oldWork = binPacker.maxBinWeight();
        //the work of the new partitioning
        int newWork = 0;
        int oldOutputsPerSteady = Util.outputsPerSteady(str, work.getExecutionCounts());
        int newOutputsPerSteady = 0;
        //the percentage change
        double workChange;
        
        do {
            oldStr = (SIRStream)ObjectDeepCloner.deepCopy(str);
            //StreamItDot.printGraph(oldStr, "oldstr.dot");
            WorkList workList = work.getSortedFilterWork();
            SIRFilter bigDup = null;
            int bigDupWork = 0;
            for (int i = workList.size() - 1; i >= 0; i--) {
                if (StatelessDuplicate.isFissable(workList.getFilter(i))) {
                    bigDup = workList.getFilter(i);
                    break;
                }
            }
            
//          nothing to duplicate so just return the str
            if (bigDup == null) 
                return str;
            
            bigDupWork = work.getWork(bigDup);
            LinkedList<SIRFilter> duplicateMe = new LinkedList<SIRFilter>();
            for (int i = workList.size() - 1; i >= 0; i--) {
                if (!StatelessDuplicate.isFissable(workList.getFilter(i)))
                     continue;

                if ((((double)work.getWork(workList.getFilter(i))) / ((double)bigDupWork)) > .9)
                    duplicateMe.add(workList.getFilter(i));
                else 
                    break;
            }
            
            //don't break symmetry
            if (duplicateMe.size() == 16)
                return str;
            
            for (int i = 0; i < duplicateMe.size(); i++) {
                System.out.println("Duplicating " + duplicateMe.get(i) + " work: " + 
                        work.getWork(duplicateMe.get(i)));
                StatelessDuplicate.doit(duplicateMe.get(i), 16);
            }
            
            //get the new work estimate
            work = WorkEstimate.getWorkEstimate(str);
            //greedy bin pack the shits
            binPacker = new GreedyBinPacking(str, chip.getTotalTiles(), work);
            binPacker.pack();
            newWork = binPacker.maxBinWeight();
            newOutputsPerSteady = Util.outputsPerSteady(str, work.getExecutionCounts());
            
            //find the percentage change in work between the two
            double oldCyclesPerOut = (((double)oldWork) / ((double)oldOutputsPerSteady));
            double newCyclesPerOut = (((double)newWork) / ((double)newOutputsPerSteady)); 
            workChange =  newCyclesPerOut / oldCyclesPerOut; 
            
            
            //remember this as the old work for the next (possible) iteration
            System.out.println(newCyclesPerOut + " / " + oldCyclesPerOut + " = " + workChange);
            oldWork = newWork;
            oldOutputsPerSteady = newOutputsPerSteady;
        } while (workChange <= threshold);
        
        str = oldStr;
        return str;
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
                for (int i=0; i<parent.size(); i++) {
                    double work = averageWork.get(parent.get(i));
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
    
    public void duplicateFilters(SIRStream str, int reps) {
        
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        WorkList workList = work.getSortedFilterWork();
        
        for (int i = workList.size() - 1; i >= 0; i--) {
            SIRFilter filter = workList.getFilter(i);
            if (!StatelessDuplicate.isFissable(filter))
                continue;
            int filterWork = work.getWork(filter);
            int commRate = ((int[])work.getExecutionCounts().get(filter))[0] * 
                (filter.getPushInt() + filter.getPopInt());
            if (filterWork / commRate > 10) {
                StatelessDuplicate.doit(filter, reps);
                System.out.println("Dup  " + filter + " comp/comm is " + (filterWork / commRate));
            }
            else {
                System.out.println("Don't dup  " + filter + " comp/comm is " + (filterWork / commRate));
            }
        }
    }
    
    /**
     * Use the cousins 
     * @param str
     */
    public void smarterDuplicate(SIRStream str) {
        
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        WorkList workList = work.getSortedFilterWork();
        HashMap<SIRStream, Double> averageWork = 
            new HashMap<SIRStream, Double>(); 
        findAverageWork(str, work, averageWork);
        int tiles = SpaceTimeBackend.getRawChip().getTotalTiles();
        
        for (int i = workList.size() - 1; i >= 0; i--) {
            SIRFilter filter = workList.getFilter(i);
            System.out.println(filter + " work " + work.getWork(filter));
            if (!StatelessDuplicate.isFissable(filter)) {
                System.out.println("  not fissible");
                continue;
            }
            int commRate = ((int[])work.getExecutionCounts().get(filter))[0] * 
                 (filter.getPushInt() + filter.getPopInt());
            int filterWork = work.getWork(filter);
            if (filterWork / commRate <= 10) {
                continue;
            }
          
            int cousins = getNumCousins(filter); 
            if (cousins == 1) {
                StatelessDuplicate.doit(filter, tiles);
            }
            else {
                // esimate work fraction of this filter vs. cousins
                double workFraction = estimateWorkFraction(filter, averageWork);
                System.out.println("Filter " + filter + " has " + cousins + " cousins and does " + workFraction + " of the work.");
                if (cousins < tiles) {
                    int reps = (int)Math.ceil(workFraction * ((double)tiles));
                    reps = Math.min(tiles - cousins + 1, reps);
                   
                    System.out.println("Calling dup with: " + reps);
                    if (reps > 1)
                        StatelessDuplicate.doit(filter, reps);
                }
            }
        }
    }
    
    private int nextPow2(int x) {
        return (int)Math.pow(2, Math.ceil(Math.log(10) / Math.log(2)));
    }
    
    public void percentStateless(SIRStream str) {
        sortedWorkEsts = new Vector<Integer>();
        sortedFilters = new Vector<SIRFilter>();
        //get the work estimate
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        //find the ordering of filters
        walkSTR(str, work);
        
        int totalWork = 0;
        int statefulWork = 0; 
        
        for (int i = 0; i < sortedFilters.size(); i++) {
            totalWork += sortedWorkEsts.get(i).intValue();
            if (StatelessDuplicate.hasMutableState(sortedFilters.get(i)))
                    statefulWork += sortedWorkEsts.get(i).intValue();
        }
        System.out.println(" stateful work / total work = " + 
                (((double)statefulWork)) / (((double)totalWork)));
        
      
    }
    
    public SIRStream smartDuplication(SIRStream str, RawChip chip) {
        percentStateless(str);
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        WorkList workList = work.getSortedFilterWork();
                //find the total work
        int totalWork = 0;
        for (int i = 0; i < workList.size(); i++) {
            SIRFilter filter = workList.getFilter(i);
            int filterWork = work.getWork(filter); 
            System.out.println("Sorted Work " + i + ": " + filter + " work " 
                    + filterWork + ", is fissable: " + StatelessDuplicate.isFissable(filter));
            totalWork += filterWork;
        }
        //find the ideal work distribution
        int idealWork = totalWork / chip.getTotalTiles();
        boolean change = false;
        System.out.println("Ideal Work: " + idealWork);
        
        for (int i = workList.size() - 1; i >= 0; i--) {
            SIRFilter filter = workList.getFilter(i);
            int filterWork = work.getWork(filter);
            if (!StatelessDuplicate.isFissable(filter))
                continue;
            System.out.println("Analyzing " + filter + " work = " + filterWork);
            if (filterWork >= 2 * idealWork) {
                int fissAmount = (int)Math.ceil(((double)filterWork) / ((double)idealWork));
                System.out.println("Fissing " + filter  + " " + fissAmount + 
                        " times (work was " + filterWork + ")");
                StatelessDuplicate.doit(filter, fissAmount);
                change = true;
            }
            else {
                System.out.println("Stop fissing, current filter work = " + filterWork);
                //since the list is sorted, nothing else will be over 
                //the threshold
                break;
            }
        }
        
        if (!change) {
            System.exit(0);
        }
        
        return str;
    }
    
    public static void duplicateHeavyFilters(SIRStream str) { 
        while (duplicateHeavyFiltersRound(str));
    }
    
    private static boolean duplicateHeavyFiltersRound(SIRStream str) {
        boolean change = false;
        //get the work estimate
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        //find the ordering of filters
        WorkList sortedFilters = work.getSortedFilterWork();
        int totalWork = 0;
        for (int i = 0; i < sortedFilters.size(); i++) {
            totalWork += sortedFilters.getWork(i);
        }
        
        int idealWork = totalWork / SpaceTimeBackend.getRawChip().getTotalTiles();
        
        System.out.println("Ideal Work: " + idealWork);
        
        for (int i = sortedFilters.size() -1 ; i >= 0; i--) {
            if (sortedFilters.getWork(i) >= (int)(1.5 * ((double)idealWork)) &&
                    StatelessDuplicate.isFissable(sortedFilters.getFilter(i))) {
                change = true;
                int reps = Math.max(2, 
                        Math.round(sortedFilters.getWork(i) / idealWork));
                StatelessDuplicate.doit(sortedFilters.getFilter(i), reps);
                System.out.println("Duplicating " + sortedFilters.getFilter(i) + " " +
                        (((double)sortedFilters.getWork(i)) / ((double)idealWork)) + " " + reps);
            }
            else        
                break;
        }
        
        return change;
    }
    
    
    public boolean duplicateBottleneck(SIRStream str) {
        sortedFilters = new Vector<SIRFilter>();
        //get the work estimate
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        //find the ordering of filters
        WorkList sortedFilters = work.getSortedFilterWork();
        
        if (sortedFilters.size() < 2)
            return false;
        
        SIRFilter bottleNeck = sortedFilters.getFilter(sortedFilters.size() - 1);
        SIRFilter nextFilter = sortedFilters.getFilter(sortedFilters.size() - 2);
        
        if (nextFilter instanceof SIRPredefinedFilter)
            return false;
        
        //return false if we cannot duplicate the bottleneck
        if (!StatelessDuplicate.isFissable(bottleNeck))
            return false;
        
        //so the bottleneck if stateless, duplicate it as many times so 
        //that it is not the bottleneck anymore!
        
        int reps = (int)Math.round(0.5 + ((double)work.getWork(bottleNeck)) / 
                ((double)work.getWork(nextFilter)));
        
        System.out.println("Duplicating bottleneck: " + bottleNeck + " " + reps);
        StatelessDuplicate.doit(bottleNeck, reps);
                
        //might be good for another round?
        return true;
    }
    
       
    private void walkSTR(SIRStream str, WorkEstimate work) {
        if (str instanceof SIRFeedbackLoop) {
            SIRFeedbackLoop fl = (SIRFeedbackLoop) str;
            walkSTR(fl.getBody(), work);
            walkSTR(fl.getLoop(), work);
        }
        if (str instanceof SIRPipeline) {
            SIRPipeline pl = (SIRPipeline) str;
            Iterator iter = pl.getChildren().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                walkSTR(child, work);
            }
        }
        if (str instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin) str;
            Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
            while (iter.hasNext()) {
                SIRStream child = iter.next();
                walkSTR(child, work);
            }
        }
        if (str instanceof SIRFilter) {
            SIRFilter filter = (SIRFilter)str; 
            int i;
            int workEst = work.getWork(filter);
            //find the right place to add this to
            for (i = 0; i < sortedFilters.size(); i++) {
                if (workEst > sortedWorkEsts.get(i).intValue())
                    break;
            }
            sortedFilters.add(i, filter);
            sortedWorkEsts.add(i, workEst);
        }
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
