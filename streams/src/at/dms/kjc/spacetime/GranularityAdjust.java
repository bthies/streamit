/**
 * 
 */
package at.dms.kjc.spacetime;

import at.dms.kjc.ObjectDeepCloner;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.*;

/**
 * @author mgordon
 *
 */
public class GranularityAdjust {
    public final static double threshold = .90;
    
    public static SIRStream doit(SIRStream str, int numCores) {
        assert KjcOptions.partition_greedier;
        SIRStream oldStr;
        //WorkEstimate.UNROLL_FOR_WORK_EST = true;
        //get the first work estimate
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        //bin pack the shits
        GreedyBinPacking binPacker = new GreedyBinPacking(str, numCores, work);
        binPacker.pack();
        //get the max bin weight for the packing
        long oldWork = binPacker.maxBinWeight();
        //the work of the new partitioning
        long newWork = 0;
        //the percentage change
        double workChange;
        
        //KjcOptions.partition_greedier = true;
        do {
            oldStr = (SIRStream)ObjectDeepCloner.deepCopy(str);
            //StreamItDot.printGraph(oldStr, "oldstr.dot");
            int tilesNeeded = at.dms.kjc.sir.lowering.partition.Partitioner.countFilters(str);
            int minTiles = at.dms.kjc.sir.lowering.partition.Partitioner.estimateFuseAllResult(str);
            str = at.dms.kjc.sir.lowering.partition.Partitioner.doit(str,
                    tilesNeeded - 1, false, false, true);
            //StreamItDot.printGraph(str, "newstr.dot");
            work = WorkEstimate.getWorkEstimate(str);
            //greedy bin pack the shits
            binPacker = new GreedyBinPacking(str, numCores, work);
            binPacker.pack();
            newWork = binPacker.maxBinWeight();
            //find the percentage change in work between the two 
            workChange = ((double)oldWork) / ((double)newWork);
            //remember this as the old work for the next (possible) iteration
            System.out.println(oldWork + " / " + newWork + " = " + workChange);
            oldWork = newWork;
            //if tiles desired is smaller than fuseAll size, then quit since we can't do better
            if(tilesNeeded <= minTiles + 1) break;
        } while (workChange >= threshold);
        
        if (workChange < threshold) {
            str = oldStr;
        }
        //StreamItDot.printGraph(str, "str.dot");
        //KjcOptions.partition_greedier = false;
        //WorkEstimate.UNROLL_FOR_WORK_EST = false;
        return str;
    }
}
