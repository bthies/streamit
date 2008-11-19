/**
 * 
 */
package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.fission.StatelessDuplicate;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.*;
import java.util.*;

/**
 * @author mgordon
 *
 */
public class StreamlinedDuplicate {
    private WorkEstimate work;
    private HashMap<SIRFilter, Long> compToComm;
    private int idealWork;
    private int totalWork;
    private RawChip chip;
    private WorkList workList;
    private LinkedList<SIRFilter> filtersToSched;
    private LinkedList<SIRFilter>[] onThisTile;
    private HashSet<SIRFilter> fissedFilters;
    
    public LinkedList<SIRFilter> getFilterOnTile(int i) {
        return onThisTile[i];
    }
     
    private void calculateModel(SIRStream str) {
        work = WorkEstimate.getWorkEstimate(str);
        compToComm = new HashMap<SIRFilter, Long>();
        workList = work.getSortedFilterWork();
        totalWork = 0;
        filtersToSched = 
            new LinkedList<SIRFilter>();
        
        for (int i = workList.size() - 1; i >= 0; i--) {
            if (workList.getFilter(i) instanceof SIRPredefinedFilter)
                continue;

            long comp = work.getWork(workList.getFilter(i));
            totalWork += comp;
            filtersToSched.add(workList.getFilter(i));
            long comm = (workList.getFilter(i).getPopInt() + workList.getFilter(i).getPushInt()) * 
                ((int[])work.getExecutionCounts().get(workList.getFilter(i)))[0];
             compToComm.put(workList.getFilter(i), comp / comm);
             System.out.println(workList.getFilter(i) + ", work = " + 
                     comp);
        }
        
        idealWork = totalWork / chip.getTotalTiles();
    }
    
    private boolean fissBigUns(SIRStream str) {
        boolean changed = false;
        //fiss any filters that perform more work than idea work * 2
        for (int i = workList.size() - 1; i >= 0; i--) {
            if (workList.getFilter(i) instanceof SIRPredefinedFilter)
                continue;
            if (!StatelessDuplicate.isFissable(workList.getFilter(i)))
                continue;
            long comp = work.getWork(workList.getFilter(i));
            if (comp  >= idealWork * 2) {
                System.out.println("Fissing " + workList.getFilter(i));
                StatelessDuplicate.doit(workList.getFilter(i), chip.getTotalTiles());
                changed = true;
            }
        }
        return changed;
    }
    
    public void doit(SIRStream str, RawChip chip) {
        this.chip = chip;
        fissedFilters = new HashSet<SIRFilter>();
        calculateModel(str);
     
        if (fissBigUns(str))
            calculateModel(str);
                        
        System.out.println("ideal work: " + idealWork);
        
        long workScheduledSoFar = 0;
        long workOnTile[] = new long[chip.getTotalTiles()];
        onThisTile = new LinkedList[chip.getTotalTiles()];
        for (int i = 0; i < chip.getTotalTiles(); i++ ) {
            onThisTile[i] = new LinkedList<SIRFilter>();
        }
                
        for (int tile = 0; tile < chip.getTotalTiles(); tile++) {
            long minWork = (totalWork - workScheduledSoFar) / (chip.getTotalTiles() - tile);
            long maxWork = (long)(((double)minWork) * 1.10);
            System.out.println("Tile " + tile + " starts with work: " + workOnTile[tile]);
            System.out.println("Tile " + tile + " minWork: " + minWork + " maxWork: " + maxWork);
            while (!filtersToSched.isEmpty() &&
                    workOnTile[tile] < minWork) {
                //pull off a filter and schedule it
                SIRFilter filter = filtersToSched.removeFirst();
                long filterWork = work.getWork(filter);
                System.out.println("Placing " + filter + " with work " + filterWork);
                workOnTile[tile] += filterWork;
                onThisTile[tile].add(filter);
            }
            
            System.out.println("Tile " + tile + " has (before fission) work " + workOnTile[tile]);
            
            //don't fiss for last tile
            if (tile == chip.getTotalTiles() - 1)
                break;
            
            //now fiss someone
            SIRFilter fiss = 
                fissMe(onThisTile[tile], workOnTile[tile], minWork, maxWork);
            
            if (fiss != null) {
                int[] ratio = fissAmount(fiss, workOnTile[tile], minWork, maxWork);
                
                if (ratio[0] != 0 && ratio[0] != chip.getTotalTiles()) {
                    SIRSplitJoin sj = StatelessDuplicate.doit(fiss, 2, ratio);
                    System.out.println("  * Fissing " + fiss + " ratio (stay:go) = " + ratio[0] + ":" + ratio[1]);
                                
                    onThisTile[tile].remove(fiss);
                    
                    onThisTile[tile].add((SIRFilter)sj.getChildren().get(1));
                    fissedFilters.add((SIRFilter)sj.getChildren().get(1));
                    onThisTile[tile + 1].add((SIRFilter)sj.getChildren().get(2));
                    fissedFilters.add((SIRFilter)sj.getChildren().get(2));
                    
                    workOnTile[tile] -=  (int)
                    ((((double)ratio[1]) / ((double)chip.getTotalTiles())) * ((double)work.getWork(fiss)));
                    workOnTile[tile + 1] +=  (int)
                    ((((double)ratio[1]) / ((double)chip.getTotalTiles())) * ((double)work.getWork(fiss)));
                }
            }
            
            workScheduledSoFar += (workOnTile[tile]); 
            
            
            System.out.println("Tile " + tile + " has work " + workOnTile[tile]);
            
        }
        
        for (int i = 0; i < workOnTile.length; i++)
            System.out.println("Work on tile " + i + " = " + workOnTile[i]);
        calculateModel(str);
        WorkEstimate workEst = WorkEstimate.getWorkEstimate(str);
        workEst.printGraph(str, "work-after-partition.dot");
       // SIRToStreamIt.run(str);
    }
    
    private int[] fissAmount(SIRFilter filter, long totalWork, 
            long maxWork, long minWork) {
        int ratio[] = new int[2];
        System.out.println("   Tile Work: " + totalWork + ", maxWork: " + maxWork + 
                " filterWork: " + work.getWork(filter)); 
        double block = ((double)work.getWork(filter)) / ((double)chip.getTotalTiles());
        System.out.println("   block: " + block);
        long go = (totalWork - maxWork);
        System.out.println("   goWork: " + go);
        long stay = (work.getWork(filter) - go);
        System.out.println("   stayWork: " + stay);
        
        ratio[0] = (int)(((double)stay) / block);
        ratio[1] = (int)(((double)go)/block);
        
        if (ratio[0] + ratio[1] < chip.getTotalTiles())
            ratio[0] += 1;
        
        assert (ratio[0] + ratio[1]) == chip.getTotalTiles(): ratio[0] + " : " + ratio[1] + " = 16";
        
        
        
        return ratio;
    }
    
    private SIRFilter fissMe(LinkedList<SIRFilter> filters, long totalWork, 
            long maxWork, long minWork) {
        //pick someone so that we can bring the average below maxwork
        SIRFilter fiss = null;
        long ccratio = 0;
        for (int i = 0; i < filters.size(); i++) {
            if (fissedFilters.contains(filters.get(i)))
                continue;
            if (!StatelessDuplicate.isFissable(filters.get(i)))
                continue;
            SIRFilter filter = filters.get(i);
            System.out.println("  Should fiss " + filter + "? work = " + work.getWork(filter) + 
                    " comp/comm = " + compToComm.get(filter));
            long filterWork = work.getWork(filter);
            if (totalWork - filterWork < maxWork &&
                    compToComm.get(filter) > ccratio) {
                fiss = filter;
                ccratio = compToComm.get(filter);
            }
        }
        
        if (fiss == null)
            return null;
        
        //make sure that the comp to comm ratio is above to make it work fissing
        //if (compToComm.get(fiss) < 10)
        //    return null;
        return fiss;
    }
    
}
