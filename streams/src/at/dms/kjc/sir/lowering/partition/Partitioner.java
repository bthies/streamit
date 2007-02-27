package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.partition.dynamicprog.*;
import at.dms.kjc.flatgraph.*;

public class Partitioner {
    /**
     * Tries to adjust 'str' into 'targetCount' pieces of equal work,
     * and return new stream. Indicate whether or not
     * 'joinersNeedTiles' in the graph, or if they count for free.  If
     * 'strict' is true, we will fail with an error message if we
     * can't succeed in fusing down to the target count; otherwise we
     * may produce more than <count> filters if it improves the load
     * balancing (e.g., on a multicore, additional threads might help
     * breakup the bottleneck)
     */
    public static SIRStream doit(SIRStream str, int targetCount, boolean joinersNeedTiles, boolean limitICode, boolean strict) {
        // detect number of tiles we have
        int curCount = new GraphFlattener(str).getNumTiles();
        return doit(str, curCount, targetCount, joinersNeedTiles, limitICode, strict);
    }
    /**
     * As above, with 'curCount' indicating the number of tiles that
     * 'str' currently requires.
     */
    public static SIRStream doit(SIRStream str, int curCount, int targetCount, boolean joinersNeedTiles, boolean limitICode, boolean strict) {
        return doit(str, curCount, targetCount, joinersNeedTiles, limitICode, strict, new HashSet());
    }
    /**
     * As above, with <noHorizFuse> denoting a set of SIRFilters that
     * cannot be fused horizontally.
     */
    public static SIRStream doit(SIRStream str, int curCount, int targetCount, boolean joinersNeedTiles, boolean limitICode, boolean strict, HashSet noHorizFuse) {
        // horizontal fusion constraints only respected by dynamic programming partitioner
        if (!KjcOptions.partition_dp && noHorizFuse.size()>0) {
            System.err.println("WARNING:  The partitioner you are using (greedy?) does not support horizontal\n" + 
                               "          fusion constraints.  To respect these, use dynamic programming partitioner.");
        }

        // Lift filters out of pipelines if they're the only thing in
        // the pipe
        Lifter.lift(str);

        // use identity policy for assessing fusability
        SIRDynamicRateManager.pushIdentityPolicy();

        // optimization for fusing a lot: if the number of tiles
        // requested is only one more than the number of non-fusable
        // filters, then just run fuse-all
        int fuseAllResult = estimateFuseAllResult(str);
        if (targetCount <= fuseAllResult + 1 && noHorizFuse.isEmpty()) {
            System.out.println("  Detected target is max fusion, " + (strict ? "running fuseall..." : "fusing as much as possible..."));
            str = FuseAll.fuse(str, strict);
        } else {
            // make work estimate
            WorkEstimate work = WorkEstimate.getWorkEstimate(str);
            work.printGraph(str, "work-before-partition.dot");
            work.getSortedFilterWork().writeToFile("work-before-partition.txt");
        
            System.err.println("  Found "+curCount+" tiles.");

            // for statistics gathering
            if (KjcOptions.dpscaling) {
                DynamicProgPartitioner.saveScalingStatistics(str, work, 256);
            }
        
            // do the partitioning
            if (KjcOptions.partition_dp) {
                /* // uncomment these lines if you want a partitions.dot
                // file of the partitions chosen by the DP partitioner.
                // Unfortunately we have to run partitioning twice to
                // get this.
        
                SIRStream str2 = (SIRStream)ObjectDeepCloner.deepCopy(str);
                new DynamicProgPartitioner(str2, WorkEstimate.getWorkEstimate(str2), targetCount).calcPartitions();
                */
                str = new DynamicProgPartitioner(str, work, targetCount, joinersNeedTiles, limitICode, strict, noHorizFuse).toplevel();
            } else if(KjcOptions.partition_greedier) {
                str=new GreedierPartitioner(str,work,targetCount,joinersNeedTiles).toplevel();
            } else if (KjcOptions.partition_greedy) {
                if (curCount < targetCount) {
                    // need fission
                    new GreedyPartitioner(str, work, targetCount, joinersNeedTiles).toplevelFission(curCount);
                } else {
                    // need fusion
                    new GreedyPartitioner(str, work, targetCount, joinersNeedTiles).toplevelFusion();
                }
        
            } else if (KjcOptions.partition_ilp) {
                Utils.fail("ILP Partitioner no longer supported.");
                // don't reference the ILPPartitioner because it won't
                // build without CPLEX, which is problematic for release
                // new ILPPartitioner(str, work, targetCount).toplevelFusion();
            }
        
            // lift the result
            Lifter.lift(str);
        
            // get the final work estimate
            work = WorkEstimate.getWorkEstimate(str);
            work.printGraph(str, "work-after-partition.dot");
            work.getSortedFilterWork().writeToFile("work-after-partition.txt");
            work.printWork();
        }
    
        // restore dynamic rate policy
        SIRDynamicRateManager.popPolicy();

        return str;
    }

    /**
     * Return number of tiles needed for 'str'.
     */
    public static int countTilesNeeded(SIRStream str, boolean joinersNeedTiles) {
        if (joinersNeedTiles) {
            // if the joiners need tiles, use the graph flattener
            return countTilesNeeded(str, new GraphFlattener(str), joinersNeedTiles);
        } else {
            // otherwise count number of filters
            return countFilters(str);
        }
    }

    /**
     * Return number of tiles needed for 'str', when you already have
     * a 'flattener' handy.
     */
    static int countTilesNeeded(SIRStream str, GraphFlattener flattener, boolean joinersNeedTiles) {
        if (joinersNeedTiles) {
            // if the joiners need tiles, use the graph flattener
            return flattener.getNumTiles();
        } else {
            // otherwise count number of filters
            return countFilters(str);
        }
    }

    /**
     * Estimates how many filters would be in <str> following maximal
     * fusion.  There needs to be a distinct filter for each unfusable
     * filter, and (as long as feedbackloop fusion is unimplemented)
     * there need to be at least one filter on each side of a feedback
     * loop.
     */
    static int estimateFuseAllResult(SIRStream str) {
        // Should this count identity filters or not?  Unclear,
        // depending on backend, so for now be conservative and count
        // them.
        final int[] count = { 0 };
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void preVisitFeedbackLoop(SIRFeedbackLoop self,
                                                 SIRFeedbackLoopIter iter) {
                    // this is overly conservative -- if some of the
                    // children are not fusable, will count those
                    // results twice.
                    count[0]+=2;
                }
                public void visitFilter(SIRFilter self,
                                        SIRFilterIter iter) {
                    if (!FusePipe.isFusable(self)) {
                        count[0]++;
                    }
                }});
        return count[0];
    }
    

    /**
     * Returns the number of filters in the graph.
     */
    public static int countFilters(SIRStream str) {
        // Should this count identity filters or not?  Unclear,
        // depending on backend, so for now be conservative and count
        // them.
        final int[] count = { 0 };
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void visitFilter(SIRFilter self,
                                        SIRFilterIter iter) {
                    count[0]++;
                }});
        return count[0];
    }
}
