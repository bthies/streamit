package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.dynamicprog.*;
import at.dms.kjc.flatgraph.*;

public class Partitioner {
    /**
     * Tries to adjust <str> into <targetCount> pieces of equal work, and
     * return new stream.
     */
    public static SIRStream doit(SIRStream str, int targetCount, boolean joinersNeedTiles) {
	// detect number of tiles we have
	int curCount = new GraphFlattener(str).getNumTiles();
	return doit(str, curCount, targetCount, joinersNeedTiles);
    }


    /**
     * Tries to adjust <str> into <targetCount> pieces of equal work,
     * given that <str> currently requires <curCount> tiles.  Indicate
     * whether or not <joinersNeedTiles> in the graph, or if they
     * count for free.  Return new stream.
     */
    public static SIRStream doit(SIRStream str, int curCount, int targetCount, boolean joinersNeedTiles) {
	// Lift filters out of pipelines if they're the only thing in
	// the pipe
	Lifter.lift(str);

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
	    str = new DynamicProgPartitioner(str, work, targetCount, joinersNeedTiles).toplevel();
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

	return str;
    }

    /**
     * Return number of tiles needed for <str>.
     */
    static int countTilesNeeded(SIRStream str, boolean joinersNeedTiles) {
	if (joinersNeedTiles) {
	    // if the joiners need tiles, use the graph flattener
	    return countTilesNeeded(str, new GraphFlattener(str), joinersNeedTiles);
	} else {
	    // otherwise count number of filters
	    return countFilters(str);
	}
    }

    /**
     * Return number of tiles needed for <str>, when you already have
     * a <flattener> handy.
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
     * Returns the number of filters in the graph.
     */
    private static int countFilters(SIRStream str) {
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
