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
    
    public static SIRStream doit(SIRStream str, RawChip chip) {
        assert KjcOptions.partition_greedier;
        SIRStream oldStr;
        //WorkEstimate.UNROLL_FOR_WORK_EST = true;
        //get the first work estimate
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        //bin pack the shits
        GreedyBinPacking binPacker = new GreedyBinPacking(str, chip.getTotalTiles(), work);
        binPacker.pack();
        //get the max bin weight for the packing
        int oldWork = binPacker.maxBinWeight();
        //the work of the new partitioning
        int newWork = 0;
        //the percentage change
        double workChange;
        
        //KjcOptions.partition_greedier = true;
        do {
            oldStr = (SIRStream)ObjectDeepCloner.deepCopy(str);
            //StreamItDot.printGraph(oldStr, "oldstr.dot");
            int tilesNeeded = at.dms.kjc.sir.lowering.partition.Partitioner.countFilters(str);
            str = at.dms.kjc.sir.lowering.partition.Partitioner.doit(str,
                    tilesNeeded - 1, false, false, true);
            //StreamItDot.printGraph(str, "newstr.dot");
            work = WorkEstimate.getWorkEstimate(str);
            //greedy bin pack the shits
            binPacker = new GreedyBinPacking(str, chip.getTotalTiles(), work);
            binPacker.pack();
            newWork = binPacker.maxBinWeight();
            //find the percentage change in work between the two 
            workChange = ((double)oldWork) / ((double)newWork);
            //remember this as the old work for the next (possible) iteration
            System.out.println(oldWork + " / " + newWork + " = " + workChange);
            oldWork = newWork;
        } while (workChange >= threshold);
        
        str = oldStr;
        //StreamItDot.printGraph(str, "str.dot");
        //KjcOptions.partition_greedier = false;
        //WorkEstimate.UNROLL_FOR_WORK_EST = false;
        return str;
    }
}
