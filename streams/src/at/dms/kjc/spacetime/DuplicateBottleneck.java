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
            }
        }
    }
    
  public void smarterDuplicate(SIRStream str) {
        
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
                int cousins = getNumCousins(filter); 
                System.out.println("Cousins of " + filter + ": " + cousins);
                if (cousins < 16) {
                    StatelessDuplicate.doit(filter, (int)Math.ceil(16.0 / ((double)cousins)));
                }
            }
            else {
                break;
            }
        }
        
        
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
            Iterator iter = sj.getParallelStreams().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
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
}
